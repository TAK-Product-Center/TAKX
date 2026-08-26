package gov.takx.core.database;

import gov.takx.api.persist.IDatabaseProvider;
import gov.takx.core.chat.ChatMessage;
import gov.takx.core.chat.Conversation;
import gov.takx.core.database.provider.DatabaseProviderType;
import gov.takx.core.persist.breadcrumb.Breadcrumb;
import gov.takx.core.persist.breadcrumb.Track;
import gov.takx.dataservices.persist.AMedia;
import gov.takx.dataservices.persist.ARegularShapeMapObject;
import gov.takx.dataservices.persist.AShapeMapObject;
import gov.takx.dataservices.persist.AnnotationMapObject;
import gov.takx.dataservices.persist.Audio;
import gov.takx.dataservices.persist.ConnectingLineDefinition;
import gov.takx.dataservices.persist.Connection;
import gov.takx.dataservices.persist.CustomDataMonitor;
import gov.takx.dataservices.persist.DataField;
import gov.takx.dataservices.persist.DeletedMapObject;
import gov.takx.dataservices.persist.FileMapObject;
import gov.takx.dataservices.persist.FlatAnnotationMapObject;
import gov.takx.dataservices.persist.IconMapObject;
import gov.takx.dataservices.persist.Image;
import gov.takx.dataservices.persist.IndexDefinition;
import gov.takx.dataservices.persist.MapEntity;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.MarkerMapObject;
import gov.takx.dataservices.persist.MultiPolylineMapObject;
import gov.takx.dataservices.persist.PersistableExtrudedLine;
import gov.takx.dataservices.persist.PersistableExtrudedPolygon;
import gov.takx.dataservices.persist.PersistableLine;
import gov.takx.dataservices.persist.PersistablePolygon;
import gov.takx.dataservices.persist.PersistableShapefileShape;
import gov.takx.dataservices.persist.PersistableSurfaceStructure;
import gov.takx.dataservices.persist.PluginState;
import gov.takx.dataservices.persist.PointMapObject;
import gov.takx.dataservices.persist.RaptorEventLog;
import gov.takx.dataservices.persist.RaptorLayer;
import gov.takx.dataservices.persist.RouteMapObject;
import gov.takx.dataservices.persist.ShapeArrowMapObject;
import gov.takx.dataservices.persist.ShapeBullseyeMapObject;
import gov.takx.dataservices.persist.ShapeCircleMapObject;
import gov.takx.dataservices.persist.ShapeEllipseMapObject;
import gov.takx.dataservices.persist.ShapePathMapObject;
import gov.takx.dataservices.persist.ShapePolygonMapObject;
import gov.takx.dataservices.persist.ShapeRangeFanMapObject;
import gov.takx.dataservices.persist.ShapeRectangleMapObject;
import gov.takx.dataservices.persist.ShapeSquareMapObject;
import gov.takx.dataservices.persist.Structure;
import gov.takx.dataservices.persist.TrackInfo;
import gov.takx.dataservices.persist.TrackStyleDefinition;
import gov.takx.dataservices.persist.Video;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class manages access to database providers, and holds the collection of mapped classes for hibernate.
 *
 * @since 3.0
 */
@ApplicationScoped
public class DatabaseManager
{
    private final Instance<IDatabaseProvider> providers;

    @Inject
    DatabaseManager(@Any Instance<IDatabaseProvider> providers)
    {
        this.providers = providers;
    }

    /**
     * Get the DB provider for the given DB type name.
     *
     * @param databaseType DB type name
     * @return selected provider
     */
    public IDatabaseProvider getDatabaseProvider(String databaseType)
    {
        return providers.select(new DatabaseProviderType.Literal(databaseType)).get();
    }

    /**
     * @return The list of classes to be mapped and managed by hibernate
     */
    public static List<Class<?>> getMappedClasses()
    {
        return Collections.unmodifiableList(ENTITY_CLASS_LIST);
    }

    /**
     * The list of classes to add to the mapped entities in the hibernate session factory.
     */
    private static final List<Class<?>> ENTITY_CLASS_LIST = Arrays.asList(
            AMedia.class,
            AnnotationMapObject.class,
            Audio.class,
            ARegularShapeMapObject.class,
            Breadcrumb.class,
            ChatMessage.class,
            ConnectingLineDefinition.class,
            Connection.class,
            Conversation.class,
            CustomDataMonitor.class,
            DataField.class,
            DeletedMapObject.class,
            FileMapObject.class,
            FlatAnnotationMapObject.class,
            IconMapObject.class,
            Image.class,
            IndexDefinition.class,
            MapEntity.class,
            MapObject.class,
            MarkerMapObject.class,
            MultiPolylineMapObject.class,
            PersistableExtrudedLine.class,
            PersistableExtrudedPolygon.class,
            PersistableLine.class,
            PersistablePolygon.class,
            PersistableShapefileShape.class,
            PersistableSurfaceStructure.class,
            PluginState.class,
            PointMapObject.class,
            RaptorEventLog.class,
            RaptorLayer.class,
            RouteMapObject.class,
            ShapeArrowMapObject.class,
            ShapeBullseyeMapObject.class,
            ShapeCircleMapObject.class,
            ShapeEllipseMapObject.class,
            AShapeMapObject.class,
            ShapePathMapObject.class,
            ShapePolygonMapObject.class,
            ShapeRangeFanMapObject.class,
            ShapeRectangleMapObject.class,
            ShapeSquareMapObject.class,
            Structure.class,
            Track.class,
            TrackInfo.class,
            TrackStyleDefinition.class,
            Video.class,
            gov.tak.platform.contact.GroupContact.class);
}
