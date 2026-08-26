package gov.takx.core.kml;

import de.micromata.opengis.kml.v_2_2_0.Container;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * This class handles the correlation of the FeatureSets/Features produced by the TAK Kernel with the KML model
 * produced by JAK.  The kernel only produces features for elements with actual geographic content, while the JAK
 * model contains everything in the source document.  We need to take each FeatureSet and Feature as produced
 * by the kernel and find the matching JAK model element, skipping over elements that don't have geographic extent.
 *
 * @since 5.5
 */
class FeatureCorrelator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // GDAL generates unique names for elements that have common names in the same container, by appending ' (#nn)'.
    private static final Pattern DUPLICATE_NAME_PATTERN = Pattern.compile("^(.*) \\(#\\d+\\)$");

    private final KmlModel model;
    private final Deque<SearchState> searchStateStack = new ArrayDeque<>();

    // Set to true if we enter a state from which we can never recover, so future correlations will always fail immediately
    private boolean broken;
    private int brokenReported; // To limit error deluge on large, broken files

    /**
     * @param model The KML mode to correlate against
     */
    FeatureCorrelator(@Nonnull KmlModel model)
    {
        this.model = model;

        final var rootFolder = makeFolder(model.kml().getFeature(), true);
        searchStateStack.push(new SearchState(rootFolder));
    }

    /**
     * Process the start of a new feature set, which might have more or fewer path elements, popping/pushing search levels as needed.
     *
     * @param featureSetName Name (path) of feature set
     * @param featureSetId  Feature set id
     */
    void processFeatureSet(@Nonnull String featureSetName, long featureSetId)
    {
        if (!validateState(featureSetName)) return;

        model.addFeatureSetId(featureSetId); // Record the feature set in the model

        final int currentLevel = searchStateStack.size();
        final String[] path = splitOnUnescapedSlash(featureSetName); // Split on unescaped slash characters
        final int newLevel = path.length;

        logger.debug("Find FS {}, path {}, currentLevel {}, newLevel {}, state {}", featureSetName, path, currentLevel, newLevel, searchStateStack.peek());

        if (newLevel < currentLevel)
        {
            // Pop levels
            int targetLevel = newLevel - 1;
            while (searchStateStack.size() > targetLevel) searchStateStack.pop();
        } else if (newLevel == currentLevel)
        {
            if (searchStateStack.size() == 1) return; // Initial state

            // This case is one where we're moving to the next folder on the same level, which leads to paths like:
            //    current: document/folder1
            //    next:    document/folder2
            // So we need to pop the current level and then fall through to push the new folder
            searchStateStack.pop();
        }

        // GDAL escapes any '/' characters in the name, but JAK does not, so we need to remove the escapes
        // so our name comparisons work.
        final String nameToFind = path[path.length - 1].replaceAll("\\\\/", "/");
        pushFeatureSet(nameToFind); // Push our new level
    }

    /**
     * GDAL escapes slashes in feature and folder names, so to get the folder path, we need to split on unescaped slashes.
     * E.g., {@code some/folder \/ name/more} will be split into {@code ['some', 'folder \/ name', 'more']}.
     * Empty path components are supported, so {@code some//folder} results in {@code ['some', '', 'folder']}, and
     * {@code some//folder//} results in {@code ['some', '', 'folder', '', '']}
     */
    @Nonnull
    private String[] splitOnUnescapedSlash(@Nonnull String input)
    {
        final List<String> result = new ArrayList<>();
        final int length = input.length();
        int start = 0;

        for (int i = 0; i < length; ++i)
        {
            final char c = input.charAt(i);
            if (c == '/' && i > 0 && input.charAt(i - 1) != '\\')
            {
                result.add(input.substring(start, i));
                start = i + 1;
            }
        }

        if (start <= length) result.add(input.substring(start));

        return result.toArray(new String[0]);
    }

    /**
     * Find and push a new feature set (folder/document) onto our search state stack.
     */
    private void pushFeatureSet(@Nonnull String featureSetName)
    {
        logger.debug("   push level {}", featureSetName);

        if (!validateState(featureSetName)) return;

        final var state = searchStateStack.peek();

        //noinspection DataFlowIssue - stack was validated in validateState
        final IndexedFolder indexedFolder = findIndexedElement(state, featureSetName, this::findFolderByName);
        if (indexedFolder != null)
        {
            state.featureSetIndex = indexedFolder.index;
            searchStateStack.push(new SearchState(indexedFolder.folder));

            if (logger.isDebugEnabled()) logger.debug("   FOUND: si now {}, new state {}", state.featureSetIndex, searchStateStack.peek());
        } else
        {
            logger.warn("Failed to find '{}' in KML file: {}", featureSetName, model.sourceFile());
            logger.warn("Full correlation of KML with Overlays has failed.");
            broken = true;
        }
    }

    /**
     * Find the next KML Folder/Document that has the given name and a non-empty geometry.
     *
     * @param state Current search state
     * @param folderName Name of folder to search for, may be {@code null} for an unnamed folder
     * @return Correlated folder, or {@code null} if no match was found
     */
    @Nullable
    private IndexedFolder findFolderByName(@Nonnull SearchState state, @Nullable String folderName)
    {
        final var features = state.folder.getFeature();
        int index = state.featureSetIndex;
        if (logger.isDebugEnabled()) logger.debug("   start at {} in {}, looking for {}", index, state.folder.getName(), folderName);

        while (++index < features.size())
        {
            final var feature = features.get(index);
            final var folder = makeFolder(feature, false);
            if (folder == null) continue;

            if (logger.isDebugEnabled()) logger.debug("   test '{}' == '{}'", folderName, feature.getName());

            if (Objects.equals(feature.getName(), folderName)) return new IndexedFolder(folder, index);
        }

        return null;
    }

    /**
     * Process a TME Feature within the current FeatureSet (on our search stack).  Find the KML Feature that corresponds
     * to the TME feature and update the current search state accordingly.
     *
     * @param featureName Name of the TME Feature to process
     * @return KML Feature correlated to the TME Feature, or {@code null} if no match was found
     */
    @Nullable
    public Feature processFeature(@Nonnull String featureName)
    {
        logger.debug("processFeature: {}", featureName);

        if (!validateState(featureName)) return null;

        final var state = searchStateStack.peek();

        //noinspection DataFlowIssue - stack was validated in validateState
        final IndexedFeature indexedFeature = findIndexedElement(state, featureName, this::findFeatureByName);
        if (indexedFeature != null)
        {
            logger.debug("   FOUND: @ {}", indexedFeature.index);
            state.featureIndex = indexedFeature.index;

            return indexedFeature.feature;
        } else
        {
            logger.warn("Failed to find '{}' in KML file: {}", featureName, model.sourceFile());
            return null;
        }
    }

    /**
     * Find the next KML feature that has the given name and a non-empty geometry.
     *
     * @param state Current search state
     * @param featureName Name of feature to search for, may be {@code null} for an unnamed feature
     * @return Correlated feature, or {@code null} if no match was found
     */
    @Nullable
    private IndexedFeature findFeatureByName(@Nonnull SearchState state, @Nullable String featureName)
    {
        final var features = state.folder.getFeature();
        int index = state.featureIndex;

        if (logger.isDebugEnabled()) logger.debug("   start at {} in {}, looking for {}", index, state.folder.getName(), featureName);

        while (++index < features.size())
        {
            final var feature = features.get(index);

            if (feature instanceof Container) continue; // Looking for a discrete feature, not a folder or document

            final String kmlFeatureName = feature.getName();
            final boolean hasGeometry = model.hasGeometry(feature);

            logger.debug("   test '{}' == '{}', hasGeometry {}", featureName, kmlFeatureName, hasGeometry);

            // Two special cases need to be handled, null names and empty names.
            // The use of Objects.equals handles the null case, but we need to test for empty names distinctly.
            if (hasGeometry &&
                    (Objects.equals(kmlFeatureName, featureName)
                            || (featureName != null && kmlFeatureName != null && featureName.isBlank() && kmlFeatureName.isBlank())))
            {
                return new IndexedFeature(feature, index);
            }
        }
        return null;
    }

    /**
     * Validate our current state (not broken and a non-empty state).
     *
     * @param name Name to use for error reporting
     * @return {@code true} if our state is valid
     */
    private boolean validateState(String name)
    {
        if (broken)
        {
            if (++brokenReported < 20) logger.debug("Broken: skip {}", name);
            return false;
        }

        if (searchStateStack.isEmpty())
        {
            logger.warn("Stack is empty: trying to find {}", name);
            logger.warn("Full correlation of KML with Overlays has failed.");
            broken = true;
            return false;
        }
        return true;
    }

    /**
     * Find an IndexedFolder/Feature with the given name, starting at the current search state index.
     *
     * @param state Current search state
     * @param name Name to find
     * @param findFunction Function to locate a matching element (Folder or Feature) and return its indexed value
     * @return Matching {@link IndexedFolder} or {@link IndexedFeature}, or {@code null} if the name couldn't be matched
     * @param <T> Inferred from expected return type, either {@link IndexedFolder} or {@link IndexedFeature}
     */
    private <T> T findIndexedElement(@Nonnull SearchState state, @Nonnull String name,
                                     @Nonnull BiFunction<SearchState, String, T> findFunction)
    {
        var feature = findFunction.apply(state, name); // Try exact match first
        if (feature != null) return feature;

        // If the name we're looking for has the duplicate pattern, then strip it and try the basename.
        final var matcher = DUPLICATE_NAME_PATTERN.matcher(name);
        if (matcher.matches())
        {
            feature = findFunction.apply(state, matcher.group(1)); // Try base name (without the '(#nn)')
            if (feature != null) return feature;
        }

        return findFunction.apply(state, null); // No match on name, find the next unnamed folder
    }

    /**
     * Return or create a folder for the given feature.  This is primarily needed because {@link Document documents}
     * and {@link Folder folders} are different concrete classes, and you need to use that concrete class to access
     * the list of features it contains.  The abstract base {@link Container} does not offer such a method.
     *
     * @param feature Feature from which to create a folder
     * @param allowNonContainer Pass {@code true} to create a folder from the given {@code feature} even if it is not a container
     * @return Folder or {@code null}
     */
    @Nullable
    private Folder makeFolder(@Nonnull Feature feature, boolean allowNonContainer)
    {
        if (feature instanceof Document document)
        {
            return new SyntheticFolder(document.getFeature(), document.getName());
        } else if (feature instanceof Folder folder)
        {
            return folder;
        } else
        {
            return allowNonContainer ? new SyntheticFolder(List.of(feature), feature.getName()) : null;
        }
    }

    /**
     * Simple holder for a {@link Folder} and the index at which it was found in the feature list.
     */
    private record IndexedFolder(Folder folder, int index)
    {
    }

    /**
     * Simple holder for a {@link Feature} and the index at which it was found in the folder's feature list.
     */
    private record IndexedFeature(Feature feature, int index)
    {
    }

    /**
     * State tracking a {@link Folder} and the current FeatureSet/Feature search indices.
     */
    private static class SearchState
    {
        private final Folder folder;
        private int featureSetIndex = -1;
        private int featureIndex = -1;

        SearchState(Folder folder)
        {
            this.folder = folder;
        }

        @Override
        public String toString()
        {
            return "state{folder='" + folder.getName() + "', fsi=" + featureSetIndex + ", fi =" + featureIndex + '}';
        }
    }

    /**
     * A "folder" holding the given list of features.
     */
    private static class SyntheticFolder extends Folder
    {
        private SyntheticFolder(List<Feature> feature, String name)
        {
            this.feature = feature;
            this.name = name;
        }
    }
}
