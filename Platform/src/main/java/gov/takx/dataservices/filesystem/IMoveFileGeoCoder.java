package gov.takx.dataservices.filesystem;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.jpeg.JpegCommentDirectory;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 */
public class IMoveFileGeoCoder extends AFileGeoCoder
{
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public ILocation geoDecode(File file)
    {
        try
        {
            Metadata metadata = JpegMetadataReader.readMetadata(file);

            Directory jpegComment = metadata.getFirstDirectoryOfType(JpegCommentDirectory.class);
            String comment = jpegComment.getString(JpegCommentDirectory.TAG_COMMENT);
            if (comment != null && comment.indexOf("<Trident>") != -1)
            {
                return parseComment(comment);
            }
        } catch (Exception e)
        {
            logger.debug("Can't geocode file: ", e);
        }
        return null;
    }

    private ILocation parseComment(String comment)
            throws ParserConfigurationException, SAXException, IOException
    {
        Document document = SafeXmlDocumentUtils.parseXML(comment);
        document.getDocumentElement().normalize();

        NodeList nList = document.getElementsByTagName("Target");
        Node targetNode = nList.item(0);

        double latitude = 0.0, longitude = 0.0, elevation = 0.0;
        boolean haveLocation = false, haveElevation = false;

        NamedNodeMap attributes = targetNode.getAttributes();

        Node node = attributes.getNamedItem("Lat");
        Node lonNode = attributes.getNamedItem("Lon");
        if (node != null && lonNode != null)
        {
            latitude = Double.parseDouble(node.getNodeValue());
            longitude = Double.parseDouble(lonNode.getNodeValue());
            haveLocation = true;
        }

        node = attributes.getNamedItem("Alt");
        if (node != null)
        {
            elevation = Double.parseDouble(node.getNodeValue());
            haveElevation = true;
        }

        logger.debug("Lat = {}, Lng = {}, El = {}", latitude, longitude, elevation);

        if (haveLocation && haveElevation)
        {
            return new RaptorLocation(latitude, longitude, elevation);
        } else if (haveLocation)
        {
            return new RaptorLocation(latitude, longitude);
        }
        return null;
    }
}
