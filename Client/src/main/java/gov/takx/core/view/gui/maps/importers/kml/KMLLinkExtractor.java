package gov.takx.core.view.gui.maps.importers.kml;

import gov.takx.commons.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class KMLLinkExtractor
{
    private static final Pattern HREF_PATTERN = Pattern.compile("<href>(.+?)</href>");

    /**
     * Iterates through the text found in a KML file and extracts all links found between "href" tags.  This only
     * works with text-based KML files.  Binary KMZ files will not be parsed.
     * <p>
     * Note that any whitespace around a given link (but still within the "href" tags) will be trimmed during extraction.
     *
     * @param kmlFile KML file from which to extract href links.
     * @return List of extracted link Strings.
     */
    public static List<String> getKMLLinks(File kmlFile)
    {
        if (kmlFile == null || !kmlFile.canRead()) return Collections.emptyList();

        if (!kmlFile.getName().toLowerCase().endsWith(".kml")) return Collections.emptyList();

        final List<String> links = new ArrayList<>();

        try
        {
            final String kml = Files.readString(kmlFile.toPath());
            final Matcher matcher = HREF_PATTERN.matcher(kml);

            while (matcher.find())
            {
                final String match = matcher.group(1);

                if (!StringUtils.isEmpty(match, true)) links.add(match);
            }
        } catch (IOException ignore)
        {
        }

        return links;
    }
}
