package gov.takx.core.preferences.migrations;

import gov.takx.commons.utils.SafeXmlDocumentUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handles the application of a preferences migration script to a given document.
 *
 * @since 5.1
 */
public class MigrationProcessor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final XPath XPATH = XPathFactory.newInstance().newXPath();

    private final String scriptName;
    private final Document scriptDocument;

    /**
     * @param scriptName Name of script in resources to load
     * @throws MigrationException If the migration script can't be read or parsed
     */
    public MigrationProcessor(@Nonnull String scriptName) throws MigrationException
    {
        this.scriptName = scriptName;

        final URL scriptUrl = getClass().getResource(scriptName);
        if (scriptUrl == null) throw new MigrationException("Migration script not found: " + scriptName);

        try (InputStream in = scriptUrl.openStream())
        {
            final String migrationScript = new String(in.readAllBytes());
            scriptDocument = SafeXmlDocumentUtils.parseXML(migrationScript);
        } catch (ParserConfigurationException | IOException | SAXException e)
        {
            throw new MigrationException("Failed to parse migration script: " + scriptName, e);
        }
    }

    /**
     * @return The document version that this migration is targeted to operate upon (i.e., the version AFTER application of this migration)
     */
    public String getFromVersion()
    {
        return scriptDocument.getDocumentElement().getAttribute("from");
    }

    /**
     * @return The document version that this migration produces (i.e., the version AFTER application of this migration)
     */
    public String getToVersion()
    {
        return scriptDocument.getDocumentElement().getAttribute("to");
    }

    /**
     * Process the given {@code document} according to the migration script configured during construction.
     *
     * @param document Document to process
     */
    public void processMigration(@Nonnull Document document) throws MigrationException
    {
        logger.debug("Process migration: {}", scriptName);

        try
        {
            final Node versionAttribute = validateDocumentVersion(document);

            for (AOperation operation : compileOperations())
            {
                logger.debug("   apply: {}", operation);
                operation.process(document);
            }

            versionAttribute.setTextContent(getToVersion()); // Update the schema version of the document
        } catch (XPathExpressionException | ParserConfigurationException e)
        {
            throw new MigrationException("Failed to apply migration operations from: " + scriptName, e);
        }
    }

    /**
     * Validate that our target document is at the version this migration is supposed to process.
     *
     * @return The version attribute node in the document being migrated
     */
    @Nonnull
    private Node validateDocumentVersion(Document document) throws XPathExpressionException, MigrationException
    {
        final Node versionAttribute = (Node) XPATH.compile(ProgramPropertiesMigrationManager.VERSION_XPATH)
                .evaluate(document, XPathConstants.NODE);

        if (versionAttribute == null) throw new MigrationException("Missing schema version!");

        final String fromVersion = getFromVersion();
        final String documentVersion = versionAttribute.getTextContent();

        if (!fromVersion.equals(documentVersion))
        {
            throw new MigrationException("Migration '" + scriptName + "' targets the wrong version; expected '" +
                    fromVersion + "' got '" + documentVersion + "'");
        }
        return versionAttribute;
    }

    /**
     * @return List of {@link AOperation operations} defined in the migration script
     */
    @Nonnull
    private List<AOperation> compileOperations()
    {
        return getChildrenAsList(scriptDocument.getDocumentElement()).stream()
                .filter(Element.class::isInstance) // Ignore comments and whitespace
                .map(Element.class::cast)
                .map(opNode -> switch (opNode.getNodeName())
                {
                    case "add" -> new AddOperation(opNode);
                    case "modify" -> new ModifyOperation(opNode);
                    case "remove" -> new RemoveOperation(opNode);
                    default ->
                    {
                        logger.warn("Unknown migration operation: {}", opNode.getNodeName());
                        yield null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * @return Children of the given node as a List.
     */
    @Nonnull
    private static List<Node> getChildrenAsList(@Nonnull Node node)
    {
        final var childNodes = node.getChildNodes();
        final int length = childNodes.getLength();
        final List<Node> childList = new ArrayList<>(length);

        for (int i = 0; i < length; i++) childList.add(childNodes.item(i));

        return childList;
    }

    /**
     * Operation that adds a set of new Nodes before/after a reference node.  The {@code where} attribute on the
     * {@code add} node determines where the new nodes are added, relative to the reference node located by the
     * reference XPath expression, legal values are:
     * <ul>
     * <li>{@code "before"} - add the nodes before the reference node, note that any comment before the reference node
     *     will be skipped and the new nodes will be added before the comment</li>
     * <li>{@code "after"} - add the nodes after the reference node</li>
     * <li>{@code "append"} - add the nodes at the end of the children of the reference node</li>
     * <li>{@code "prepend"} - add the nodes at the beginning of the children of the reference node</li>
     * </ul>
     */
    private static class AddOperation extends AOperation
    {
        // Supported locations for adding nodes
        private enum Where
        {
            after, append, before, prepend
        }

        private final Where where;

        AddOperation(@Nonnull Element opElement)
        {
            super(opElement);

            where = Where.valueOf(opElement.getAttribute("where").trim().toLowerCase());
        }

        @Override
        protected void process(@Nonnull Document document) throws XPathExpressionException, ParserConfigurationException, MigrationException
        {
            final Node referenceNode = getReferenceNode(document);

            // If we can't find our reference node, then we can't apply this operation. This will leave the file in an
            // inconsistent state, so we need to throw an exception so our manager can respond accordingly.
            if (referenceNode == null) throw new MigrationException("Missing reference node in operation: " + this);

            final Node parentNode;
            final Node targetNode;

            switch (where)
            {
                case after ->
                {
                    parentNode = referenceNode.getParentNode();

                    // The DOM API doesn't have an insertAfter method, so we need to get the next node and use
                    // insertBefore on that, taking care to not over-index the child node list.
                    final int refNodeIndex = indexOf(parentNode, referenceNode);
                    final var childNodes = parentNode.getChildNodes();

                    targetNode = refNodeIndex < childNodes.getLength() - 1
                            ? childNodes.item(refNodeIndex + 1)
                            : null; // null means append after to existing children
                }
                case before ->
                {
                    parentNode = referenceNode.getParentNode();

                    // Skip any comment that precedes the target element so we insert before the comment
                    final var comment = findComment(referenceNode);
                    targetNode = comment != null ? comment : referenceNode;
                }
                case append ->
                {
                    // Appending to the content of the reference node, so switch it to be the parent
                    parentNode = referenceNode;
                    targetNode = null; // null means append after all existing children
                }
                case prepend ->
                {
                    // Prepending to the content of the reference node, so switch it to be the parent
                    parentNode = referenceNode;
                    targetNode = referenceNode.getFirstChild();
                }
                default -> throw new IllegalStateException("Missing handling for 'where' value of [" + where + "]");
            }

            // Insert all the children of the operation element at the determined location in our target document
            getChildrenAsList(opElement).forEach(child -> parentNode.insertBefore(document.importNode(child, true), targetNode));
        }

        @Override
        public String toString()
        {
            return "AddOperation{" +
                    "where='" + where + '\'' +
                    ", referenceXpath='" + referenceXpath + '\'' +
                    '}';
        }
    }

    /**
     * Operation that removes the element specified in the reference XPath.  Any comment associated with the
     * referenced node will also be removed.
     */
    private static class RemoveOperation extends AOperation
    {
        RemoveOperation(@Nonnull Element opElement)
        {
            super(opElement);
        }

        @Override
        protected void process(@Nonnull Document document) throws XPathExpressionException
        {
            final Node referenceNode = getReferenceNode(document);

            // If the node is already gone (presumably deleted by the user), just skip it
            if (referenceNode != null)
            {
                // Must find the comment node prior to removing the reference node
                final Node parentNode = referenceNode.getParentNode();
                final Node comment = findComment(referenceNode);

                parentNode.removeChild(referenceNode);
                if (comment != null) parentNode.removeChild(comment);
            } else
            {
                logger.debug("Not modifying {}; user deleted", referenceXpath);
            }
        }
    }

    /**
     * Operation that modifies an element's {@code value} attribute. If the current value of the attribute is not
     * equal to our expected {@code oldValue}, then the modification is NOT performed.
     */
    private static class ModifyOperation extends AOperation
    {
        private final String expectedValue;
        private final String newValue;

        ModifyOperation(@Nonnull Element opElement)
        {
            super(opElement);

            expectedValue = opElement.getAttribute("oldValue");
            newValue = opElement.getAttribute("newValue");
        }

        @Override
        protected void process(@Nonnull Document document) throws XPathExpressionException
        {
            final var referenceNode = (Element) getReferenceNode(document);

            // If the reference node is null, the user has deleted if from the file, so skip it
            if (referenceNode != null)
            {
                // If the current value is different from our expected value, the user has modified it, so don't apply the op
                final String currentValue = referenceNode.getAttribute("value");

                if (expectedValue.equals(currentValue))
                {
                    referenceNode.setAttribute("value", newValue);
                } else
                {
                    logger.debug("Not modifying {}; value changed '{}' != '{}'", referenceXpath, expectedValue, currentValue);
                }
            } else
            {
                logger.debug("Not modifying {}; user deleted", referenceXpath);
            }
        }

        @Override
        public String toString()
        {
            return "ModifyOperation{" +
                    "expectedValue='" + expectedValue + '\'' +
                    ", newValue='" + newValue + '\'' +
                    ", referenceXpath='" + referenceXpath + '\'' +
                    '}';
        }
    }

    /**
     * Abstract base for XML edit operations.
     */
    private static abstract class AOperation
    {
        private static final String REF_XPATH = "refXpath";

        protected final String referenceXpath;
        protected final XPathExpression referenceXpathExpression;
        protected final Element opElement;

        /**
         * @param opElement The Element from which we are being constructed
         */
        protected AOperation(@Nonnull Element opElement)
        {
            this.opElement = opElement;

            // If the XPath given is absolute, then use it as is, otherwise prepend a "match anywhere" pattern
            final String xpathAttribute = opElement.getAttribute(REF_XPATH);

            if (xpathAttribute.isBlank()) throw new IllegalArgumentException(REF_XPATH + " must have a value; op = " + this);

            referenceXpath = xpathAttribute.charAt(0) == '/'
                    ? xpathAttribute
                    : "//" + xpathAttribute;

            try
            {
                referenceXpathExpression = XPATH.compile(referenceXpath);
            } catch (XPathExpressionException e)
            {
                throw new IllegalArgumentException("Invalid XPath expression: " + referenceXpath, e);
            }
        }

        /**
         * @return The Node within the given document corresponding to our configured {@code refXpath}
         */
        @Nullable
        protected Node getReferenceNode(@Nonnull Document document) throws XPathExpressionException
        {
            return (Node) referenceXpathExpression.evaluate(document, XPathConstants.NODE);
        }

        /**
         * @return The index of the {@code child} node within the {@code parent}, else {@code -1}
         */
        protected int indexOf(@Nonnull Node parent, @Nonnull Node child)
        {
            final NodeList nodes = parent.getChildNodes();
            for (int i = 0, length = nodes.getLength(); i < length; i++)
            {
                if (nodes.item(i).isSameNode(child)) return i;
            }
            return -1;
        }

        /**
         * @return The comment node preceding the given {@code node}, or else {@code null}
         */
        @Nullable
        protected Node findComment(@Nonnull Node node)
        {
            Node previousSibling = node.getPreviousSibling();
            while (previousSibling != null && !(previousSibling instanceof Element))
            {
                if (previousSibling instanceof Comment) return previousSibling;
                previousSibling = previousSibling.getPreviousSibling();
            }
            return null;
        }

        /**
         * Process the migration operation.
         *
         * @param document Source document to process
         */
        protected abstract void process(@Nonnull Document document) throws XPathExpressionException, ParserConfigurationException, MigrationException;

        @Override
        public String toString()
        {
            return getClass().getSimpleName() + "{referenceXpath='" + referenceXpath + '\'' + '}';
        }
    }
}
