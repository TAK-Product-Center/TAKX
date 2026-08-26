
    alter table AnnotationMapObject 
        drop constraint FKDA9EE12C4F13C0AE;

    alter table ChannelDef 
        drop constraint FK3BBA8226F9877C8;

    alter table ChannelStats 
        drop constraint FK44C557C55479C6B;

    alter table ChannelStats 
        drop constraint FK44C557CA3E9AE7D;

    alter table ClientIconMapObject 
        drop constraint FKF3F1611717E0F035;

    alter table ConnectingLineDefinition 
        drop constraint FKA712AD5F7F1B1B37;

    alter table ConnectingLineDefinitionMetadataPair 
        drop constraint FK2960DA889F1E6C2;

    alter table FieldMetaData 
        drop constraint FK9B8A49D21165C8;

    alter table FileMapObject 
        drop constraint FK372C857F7ADFAC2A;

    alter table FileMapObject 
        drop constraint FK372C857F716EDF52;

    alter table FileMetaData 
        drop constraint FKA355610B1A295875;

    alter table FileTransfer 
        drop constraint FKA8F29A788CF9EFE;

    alter table FileTransfer 
        drop constraint FKA8F29A7716EDF52;

    alter table IconMapObject 
        drop constraint FK29CECB22603D7842;

    alter table IconMapObject 
        drop constraint FK29CECB224CC50842;

    alter table IconMapObject 
        drop constraint FK29CECB224F13C0AE;

    alter table IconMapObject 
        drop constraint FK29CECB2289F1E6C2;

    alter table IconMapObject 
        drop constraint FK29CECB22934793EA;

    alter table MapObject 
        drop constraint FKA606DF9BDBADDB12;

    alter table MapObject 
        drop constraint FKA606DF9BFACF3107;

    alter table MapObjectOwnerAppPlugin 
        drop constraint FK5FD53FBC97AD9FFB;

    alter table MapObjectOwnerFeaturePlugin 
        drop constraint FK4EBAC8D197AD9FFB;

    alter table MapObjectOwnerImportExportPlugin 
        drop constraint FK38B6724497AD9FFB;

    alter table MapObjectOwnerMapObject 
        drop constraint FK3A9206C397AD9FFB;

    alter table MapObjectOwnerMapObject 
        drop constraint FK3A9206C3B23551BA;

    alter table MapObjectProxy 
        drop constraint FKC388839387B6E7D2;

    alter table MapObjectProxy 
        drop constraint FKC3888393A3F97552;

    alter table MapObjectState 
        drop constraint FKC3B37DB655479C6B;

    alter table MapPoint 
        drop constraint FKDB11DB4501CDBC2;

    alter table MapPoint 
        drop constraint FKDB11DB494098CB2;

    alter table MapPointData 
        drop constraint FK6FE3877EAB39CBD4;

    alter table MapPointDataBoolean 
        drop constraint FKC14EB2CAED3A5352;

    alter table MapPointDataBooleanArray 
        drop constraint FKFA8CFDAFED3A5352;

    alter table MapPointDataByteArray 
        drop constraint FKB1D2AC53ED3A5352;

    alter table MapPointDataDate 
        drop constraint FK3BD7FC4CED3A5352;

    alter table MapPointDataDateArray 
        drop constraint FK1D3F556DED3A5352;

    alter table MapPointDataDouble 
        drop constraint FKA68FD8EFED3A5352;

    alter table MapPointDataDoubleArray 
        drop constraint FK4D36E6AAED3A5352;

    alter table MapPointDataFloat 
        drop constraint FK3F48A97EED3A5352;

    alter table MapPointDataFloatArray 
        drop constraint FK4B85177BED3A5352;

    alter table MapPointDataInt 
        drop constraint FK9ED57F91ED3A5352;

    alter table MapPointDataIntArray 
        drop constraint FK10FA1548ED3A5352;

    alter table MapPointDataLocation 
        drop constraint FK62452D53ED3A5352;

    alter table MapPointDataLong 
        drop constraint FK3BDBD31AED3A5352;

    alter table MapPointDataLongArray 
        drop constraint FK33B78F5FED3A5352;

    alter table MapPointDataShort 
        drop constraint FK3FFE0B9EED3A5352;

    alter table MapPointDataShortArray 
        drop constraint FKE578095BED3A5352;

    alter table MapPointDataString 
        drop constraint FKC06DBD2FED3A5352;

    alter table MapPointDataStringArray 
        drop constraint FK539DAA6AED3A5352;

    alter table PersistableExtrudedLine 
        drop constraint FKB872A8743E855450ceaa4621;

    alter table PersistableExtrudedPolygon 
        drop constraint FKB872A8743E855450b6bb73ed;

    alter table PersistableLine 
        drop constraint FKB872A8743E8554503478da22;

    alter table PersistablePolygon 
        drop constraint FKB872A8743E8554501974d44c;

    alter table PersistableSurfacePolygon 
        drop constraint FKB872A8743E855450c1989c7b;

    alter table PersistableSurfacePolyline 
        drop constraint FKB872A8743E855450717d229f;

    alter table RigidShapeMapObject 
        drop constraint FKC8CC44059B2555D;

    alter table ShapeBoxMapObject 
        drop constraint FK749C3051EFBD4047;

    alter table ShapeCircleMapObject 
        drop constraint FK9AC0D30A9B2555D;

    alter table ShapeConeMapObject 
        drop constraint FK28A951D7EFBD4047;

    alter table ShapeCylinderMapObject 
        drop constraint FKF9811B04EFBD4047;

    alter table ShapeEllipseMapObject 
        drop constraint FK962BF1E9B2555D;

    alter table ShapeEllipsoidMapObject 
        drop constraint FK6D56E159EFBD4047;

    alter table ShapeExtrudedPolygonMapObject 
        drop constraint FK2DEB8FC127A84F38;

    alter table ShapeMapObject 
        drop constraint FK3581613A4F13C0AE;

    alter table ShapePathMapObject 
        drop constraint FK33D21F159B2555D;

    alter table ShapePolygon3DMapObject 
        drop constraint FKCB5A28317ED9476C;

    alter table ShapePolygonMapObject 
        drop constraint FKAE2D4D4227A84F38;

    alter table ShapePolyline3DMapObject 
        drop constraint FK22377D4927A84F38;

    alter table ShapePyramidMapObject 
        drop constraint FK4598D5ECEFBD4047;

    alter table ShapeRectangleMapObject 
        drop constraint FKB3437C6D9B2555D;

    alter table ShapeSphereMapObject 
        drop constraint FKA12934AD9B2555D;

    alter table ShapeSquareMapObject 
        drop constraint FK80E7BD5D9B2555D;

    alter table ShapeWedgeMapObject 
        drop constraint FK9BAC548EFBD4047;

    alter table StructureAssociation 
        drop constraint FK7C19B52E94098CB2;

    alter table StructureAssociation 
        drop constraint FK7C19B52EA3F97552;

    alter table StructureField 
        drop constraint FK2826BF6794098CB2;

    alter table StructureMetaData 
        drop constraint FK919B9DC294098CB2;

    alter table SurfaceStructureRelationship 
        drop constraint FK9D8E78BE8D9D384F;

    alter table TrackStyleDefinitionMetadataPair 
        drop constraint FK19A3CA82603D7842;

    alter table VFSFile 
        drop constraint FK417E2BBF11932FC2;

    alter table channeldef_objecttype 
        drop constraint FKD2DF033688CF9EFE;

    alter table channeldef_objecttype 
        drop constraint FKD2DF0336A649D8E;

    alter table channeldef_structure 
        drop constraint FK2623299688CF9EFE;

    alter table channeldef_structure 
        drop constraint FK262329964FED63DE;

    alter table channeldef_vfsnamespace 
        drop constraint FK919B05F588CF9EFE;

    alter table channeldef_vfsnamespace 
        drop constraint FK919B05F58335F47F;

    alter table file_channeldef 
        drop constraint FKE7899EC51A295875;

    alter table file_channeldef 
        drop constraint FKE7899EC588CF9EFE;

    alter table mappointmessage_channel 
        drop constraint FKF9B46497166DA22B;

    alter table mappointmessage_channel 
        drop constraint FKF9B46497A3E9AE7D;

    alter table mappointmessage_trackstyledefinition 
        drop constraint FKB56A2059531B5B2;

    alter table mappointmessage_trackstyledefinition 
        drop constraint FKB56A205241402F5;

    drop table AnnotationMapObject cascade;

    drop table Application cascade;

    drop table ChannelDef cascade;

    drop table ChannelStats cascade;

    drop table ClientIconMapObject cascade;

    drop table ClientLayer cascade;

    drop table ConnectingLineDefinition cascade;

    drop table ConnectingLineDefinitionMetadataPair cascade;

    drop table Connection cascade;

    drop table CustomDataMonitor cascade;

    drop table ExceptionLog cascade;

    drop table FieldMetaData cascade;

    drop table FileMapObject cascade;

    drop table FileMetaData cascade;

    drop table FileSystemNamespace cascade;

    drop table FileTransfer cascade;

    drop table IconMapObject cascade;

    drop table MapEntity cascade;

    drop table MapObject cascade;

    drop table MapObjectOwner cascade;

    drop table MapObjectOwnerAppPlugin cascade;

    drop table MapObjectOwnerFeaturePlugin cascade;

    drop table MapObjectOwnerImportExportPlugin cascade;

    drop table MapObjectOwnerMapObject cascade;

    drop table MapObjectProxy cascade;

    drop table MapObjectState cascade;

    drop table MapObjectType cascade;

    drop table MapPoint cascade;

    drop table MapPointData cascade;

    drop table MapPointDataBoolean cascade;

    drop table MapPointDataBooleanArray cascade;

    drop table MapPointDataByteArray cascade;

    drop table MapPointDataDate cascade;

    drop table MapPointDataDateArray cascade;

    drop table MapPointDataDouble cascade;

    drop table MapPointDataDoubleArray cascade;

    drop table MapPointDataFloat cascade;

    drop table MapPointDataFloatArray cascade;

    drop table MapPointDataInt cascade;

    drop table MapPointDataIntArray cascade;

    drop table MapPointDataLocation cascade;

    drop table MapPointDataLong cascade;

    drop table MapPointDataLongArray cascade;

    drop table MapPointDataShort cascade;

    drop table MapPointDataShortArray cascade;

    drop table MapPointDataString cascade;

    drop table MapPointDataStringArray cascade;

    drop table PersistableExtrudedLine cascade;

    drop table PersistableExtrudedPolygon cascade;

    drop table PersistableLine cascade;

    drop table PersistablePolygon cascade;

    drop table PersistableSurfacePolygon cascade;

    drop table PersistableSurfacePolyline cascade;

    drop table PluginState cascade;

    drop table ProgramVersion cascade;

    drop table RigidShapeMapObject cascade;

    drop table SaveState cascade;

    drop table ShapeBoxMapObject cascade;

    drop table ShapeCircleMapObject cascade;

    drop table ShapeConeMapObject cascade;

    drop table ShapeCylinderMapObject cascade;

    drop table ShapeEllipseMapObject cascade;

    drop table ShapeEllipsoidMapObject cascade;

    drop table ShapeExtrudedPolygonMapObject cascade;

    drop table ShapeMapObject cascade;

    drop table ShapePathMapObject cascade;

    drop table ShapePolygon3DMapObject cascade;

    drop table ShapePolygonMapObject cascade;

    drop table ShapePolyline3DMapObject cascade;

    drop table ShapePyramidMapObject cascade;

    drop table ShapeRectangleMapObject cascade;

    drop table ShapeSphereMapObject cascade;

    drop table ShapeSquareMapObject cascade;

    drop table ShapeWedgeMapObject cascade;

    drop table Structure cascade;

    drop table StructureAssociation cascade;

    drop table StructureDef cascade;

    drop table StructureField cascade;

    drop table StructureMetaData cascade;

    drop table SurfaceStructureRelationship cascade;

    drop table SurfaceStructureRelationshipType cascade;

    drop table TrackInfo cascade;

    drop table TrackStyleDefinition cascade;

    drop table TrackStyleDefinitionMetadataPair cascade;

    drop table VFSFile cascade;

    drop table "class" cascade;

    drop table channeldef_objecttype cascade;

    drop table channeldef_structure cascade;

    drop table channeldef_vfsnamespace cascade;

    drop table eventlog cascade;

    drop table file_channeldef cascade;

    drop table mappointmessage_channel cascade;

    drop table mappointmessage_trackstyledefinition cascade;

    drop sequence hibernate_sequence;

    create table AnnotationMapObject (
        backgroundColor bytea,
        font bytea,
        maxActiveAltitude float8 not null,
        minActiveAltitude float8 not null,
        opacity float8 not null,
        text varchar(255),
        textAlign int4 not null,
        textColor bytea,
        id int4 not null,
        primary key (id)
    );

    create table Application (
        id  serial not null,
        family varchar(255),
        type varchar(255),
        primary key (id),
        unique (family, type)
    );

    create table ChannelDef (
        id  serial not null,
        description varchar(255),
        ipAddress varchar(255),
        name varchar(255),
        port int4 not null,
        remote bool,
        sequenceNumber int4 not null,
        supportsNonDevices bool,
        uuid_msb int8,
        uuid_lsb int8,
        class_id int2,
        primary key (id)
    );

    create table ChannelStats (
        id  serial not null,
        lastRPMReceivedTime int8 not null,
        lastRPMSentTime int8 not null,
        channel_id int4,
        mo_id int4,
        primary key (id),
        unique (mo_id, channel_id)
    );

    create table ClientIconMapObject (
        id int4 not null,
        primary key (id)
    );

    create table ClientLayer (
        id  serial not null,
        className varchar(255),
        configFileName varchar(255),
        deleted bool not null,
        displayName varchar(255),
        editable bool not null,
        iconPath varchar(255),
        key varchar(255),
        layerGroup varchar(255),
        maxActiveAltitude float8 not null,
        maxActiveAltitudeEnabled bool not null,
        minActiveAltitude float8 not null,
        minActiveAltitudeEnabled bool not null,
        name varchar(255),
        networkRetrievalEnabled bool not null,
        opacity float8 not null,
        orderkey int4,
        permanent bool not null,
        pickable bool not null,
        restoreStateXML text,
        splitScale float8 not null,
        visible bool not null,
        primary key (id)
    );

    create table ConnectingLineDefinition (
        id  serial not null,
        displayName varchar(255),
        elevationThreshold float8,
        family varchar(255),
        isUserDefined bool,
        lineColorValue int4,
        lineOpacity float4,
        lineStyle int4,
        lineWidth int4,
        showDirectionalArrow bool,
        type varchar(255),
        uniqueKey varchar(255),
        parent_id int4,
        primary key (id)
    );

    create table ConnectingLineDefinitionMetadataPair (
        id  serial not null,
        metadataKey varchar(255),
        metadataValue varchar(255),
        connectingLineDefinition_id int4,
        primary key (id)
    );

    create table Connection (
        id  serial not null,
        family varchar(255),
        name varchar(255),
        type varchar(255),
        uniqueKey varchar(255),
        primary key (id),
        unique (uniqueKey)
    );

    create table CustomDataMonitor (
        id  serial not null,
        ackReq bool not null,
        alarmFilename varchar(255),
        alarmState int4,
        clearable bool not null,
        comparisonType int4,
        defaultState int4,
        displayName varchar(255),
        family varchar(255),
        fieldName varchar(255),
        fieldType int4,
        holdState bool not null,
        moKey varchar(255),
        showValue bool not null,
        structureName varchar(255),
        threshold varchar(255),
        type varchar(255),
        useAudio bool not null,
        primary key (id)
    );

    create table ExceptionLog (
        id  serial not null,
        exception text,
        object text,
        primary key (id)
    );

    create table FieldMetaData (
        id  serial not null,
        metadataKey varchar(255),
        metadataValue varchar(255),
        field_id int4,
        primary key (id)
    );

    create table FileMapObject (
        id int4 not null,
        vfsFile_id int4,
        primary key (id)
    );

    create table FileMetaData (
        id  serial not null,
        metadataKey varchar(255),
        metadataValue varchar(255),
        file_id int4,
        primary key (id)
    );

    create table FileSystemNamespace (
        id  serial not null,
        namespace varchar(255),
        primary key (id)
    );

    create table FileTransfer (
        transferType varchar(31) not null,
        id  serial not null,
        bytesTransferred int8 not null,
        fileName varchar(255),
        server bytea,
        totalBytes int8 not null,
        uuid bytea,
        channelDef_id int4,
        vfsFile_id int4,
        primary key (id)
    );

    create table IconMapObject (
        contained bool not null,
        id int4 not null,
        connectingLineDefinition_id int4,
        parent_id int4,
        stopRecordsTrackStyleDefinition_id int4,
        trackStyleDefinition_id int4,
        primary key (id)
    );

    create table MapEntity (
        id  serial not null,
        family varchar(255),
        type varchar(255),
        version varchar(255),
        primary key (id),
        unique (family, type, version)
    );

    create table MapObject (
        id  serial not null,
        allowDeviceContainment bool not null,
        allowDrag bool not null,
        bucketMode int4,
        defaultTrackColor bytea,
        description varchar(255),
        displayableType varchar(255),
        groupName varchar(255),
        hasLocalElevation bool,
        hasHeading bool not null,
        iconScale float8 not null,
        imageURI varchar(255),
        infoText text,
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        mostRecentCommunicationTime int8 not null,
        name varchar(255),
        oldestCommunicationTime int8 not null,
        securityFilter int4,
        snapIconToSurface bool not null,
        unitID varchar(255),
        visible bool not null,
        owner_id int4,
        trackInfo_id int4,
        primary key (id)
    );

    create table MapObjectOwner (
        id  serial not null,
        primary key (id)
    );

    create table MapObjectOwnerAppPlugin (
        id int4 not null,
        primary key (id)
    );

    create table MapObjectOwnerFeaturePlugin (
        id int4 not null,
        primary key (id)
    );

    create table MapObjectOwnerImportExportPlugin (
        id int4 not null,
        primary key (id)
    );

    create table MapObjectOwnerMapObject (
        id int4 not null,
        owner_id int4,
        primary key (id)
    );

    create table MapObjectProxy (
        id  serial not null,
        subType varchar(255),
        topic varchar(255),
        mapentity_id int4 not null,
        mapobject_id int4,
        primary key (id)
    );

    create table MapObjectState (
        id  serial not null,
        state int4,
        mo_id int4,
        primary key (id)
    );

    create table MapObjectType (
        id  serial not null,
        family varchar(255),
        type varchar(255),
        unitid varchar(255),
        primary key (id)
    );

    create table MapPoint (
        DTYPE varchar(1) not null,
        id  serial not null,
        uuid_msb int8,
        uuid_lsb int8,
        sourceName varchar(255),
        sourceType int4,
        associatedFilePath varchar(255),
        elevation float8,
        eventName varchar(255),
        groupKey varchar(32),
        has3dOrientation bool,
        hasElevation bool,
        hasHeading bool,
        hasLocation bool,
        hasMotion bool,
        hasSpeed bool,
        hasTime bool,
        heading float8,
        isManualLocation bool,
        isRenderable bool,
        latitude float8,
        longitude float8,
        motionDuration int8,
        pitch float8,
        receiveTime int8,
        relevantTime timestamp,
        roll float8,
        speed float8,
        time int8,
        creationTime int8,
        mapObjectProxy_id int4 not null,
        structure_id int4,
        primary key (id),
        unique (uuid_msb, uuid_lsb)
    );

    create table MapPointData (
        id  serial not null,
        fieldName varchar(255),
        mappoint_id int4,
        primary key (id)
    );

    create table MapPointDataBoolean (
        value bool,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataBooleanArray (
        value bytea,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataByteArray (
        value bytea,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataDate (
        value timestamp,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataDateArray (
        value bytea,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataDouble (
        value float8,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataDoubleArray (
        value bytea,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataFloat (
        value float4,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataFloatArray (
        value bytea,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataInt (
        value int4,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataIntArray (
        value bytea,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataLocation (
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataLong (
        value int8,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataLongArray (
        value bytea,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataShort (
        value int2,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataShortArray (
        value bytea,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataString (
        value varchar(2048),
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataStringArray (
        value bytea,
        id int4 not null,
        primary key (id)
    );

    create table PersistableExtrudedLine (
        id int4 not null,
        description varchar(255),
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        name varchar(255),
        url varchar(255),
        uuid bytea,
        layer_id int4,
        altitudeMode int4 not null,
        isExtrude bool not null,
        isTesselate bool not null,
        numOfVertices int4 not null,
        sector bytea,
        vertices bytea,
        primary key (id)
    );

    create table PersistableExtrudedPolygon (
        id int4 not null,
        description varchar(255),
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        name varchar(255),
        url varchar(255),
        uuid bytea,
        layer_id int4,
        altitudeMode int4 not null,
        blockCoordCount bytea,
        extrudeHeight bytea,
        isExtrude bool not null,
        numOfVertices int4 not null,
        numberOfBlocks int4 not null,
        sector bytea,
        vertices bytea,
        primary key (id)
    );

    create table PersistableLine (
        id int4 not null,
        description varchar(255),
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        name varchar(255),
        url varchar(255),
        uuid bytea,
        layer_id int4,
        blockCoordCount bytea,
        numOfVertices int4 not null,
        numberOfBlocks int4 not null,
        sector bytea,
        vertices bytea,
        primary key (id)
    );

    create table PersistablePolygon (
        id int4 not null,
        description varchar(255),
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        name varchar(255),
        url varchar(255),
        uuid bytea,
        layer_id int4,
        blockCoordCount bytea,
        numOfVertices int4 not null,
        numberOfBlocks int4 not null,
        sector bytea,
        vertices bytea,
        primary key (id)
    );

    create table PersistableSurfacePolygon (
        id int4 not null,
        description varchar(255),
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        name varchar(255),
        url varchar(255),
        uuid bytea,
        layer_id int4,
        interior bytea,
        outerBoundary bytea,
        sector bytea,
        primary key (id)
    );

    create table PersistableSurfacePolyline (
        id int4 not null,
        description varchar(255),
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        name varchar(255),
        url varchar(255),
        uuid bytea,
        layer_id int4,
        interior bytea,
        outerBoundary bytea,
        sector bytea,
        primary key (id)
    );

    create table PluginState (
        id  serial not null,
        state text,
        uniqueKey varchar(255),
        primary key (id)
    );

    create table ProgramVersion (
        id  serial not null,
        version varchar(255),
        primary key (id)
    );

    create table RigidShapeMapObject (
        halfHeight float8 not null,
        pitch float8 not null,
        roll float8 not null,
        semiMajorAxis float8 not null,
        semiMinorAxis float8 not null,
        skewMajorAxis float8 not null,
        skewMinorAxis float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table SaveState (
        id  serial not null,
        saved bool not null,
        primary key (id)
    );

    create table ShapeBoxMapObject (
        id int4 not null,
        primary key (id)
    );

    create table ShapeCircleMapObject (
        radius float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table ShapeConeMapObject (
        id int4 not null,
        primary key (id)
    );

    create table ShapeCylinderMapObject (
        id int4 not null,
        primary key (id)
    );

    create table ShapeEllipseMapObject (
        semiMajorAxis float8 not null,
        semiMinorAxis float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table ShapeEllipsoidMapObject (
        id int4 not null,
        primary key (id)
    );

    create table ShapeExtrudedPolygonMapObject (
        height float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table ShapeMapObject (
        editable bool not null,
        fillColor int4,
        lineColor int4,
        lineWidth float8,
        orientationDegrees float8,
        published bool not null,
        id int4 not null,
        primary key (id)
    );

    create table ShapePathMapObject (
        encodedPoints bytea,
        id int4 not null,
        primary key (id)
    );

    create table ShapePolygon3DMapObject (
        id int4 not null,
        primary key (id)
    );

    create table ShapePolygonMapObject (
        id int4 not null,
        primary key (id)
    );

    create table ShapePolyline3DMapObject (
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        lowerAltitude float8 not null,
        lowerAltitudeReferenceMode varchar(255),
        upperAltitude float8 not null,
        upperAltitudeReferenceMode varchar(255),
        id int4 not null,
        primary key (id)
    );

    create table ShapePyramidMapObject (
        id int4 not null,
        primary key (id)
    );

    create table ShapeRectangleMapObject (
        halfheight float8 not null,
        halfwidth float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table ShapeSphereMapObject (
        radius float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table ShapeSquareMapObject (
        halfwidth float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table ShapeWedgeMapObject (
        wedgeAngle float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table Structure (
        id  serial not null,
        hashValue varchar(255),
        isRenderable bool not null,
        messageType varchar(255),
        version varchar(255),
        primary key (id),
        unique (hashValue)
    );

    create table StructureAssociation (
        mapentity_id int4 not null,
        structure_id int4 not null
    );

    create table StructureDef (
        id  serial not null,
        messageType varchar(255),
        version varchar(255),
        primary key (id)
    );

    create table StructureField (
        id  serial not null,
        defaultValue varchar(255),
        fieldLength int4,
        fieldName varchar(255),
        fieldType int4 not null,
        ordinalPosition int4 not null,
        sharedFlag bool,
        structure_id int4,
        primary key (id)
    );

    create table StructureMetaData (
        id  serial not null,
        metadataKey varchar(255),
        metadataValue varchar(255),
        structure_id int4,
        primary key (id)
    );

    create table SurfaceStructureRelationship (
        id  serial not null,
        source_id int4,
        target_id int4,
        type_id int4,
        primary key (id)
    );

    create table SurfaceStructureRelationshipType (
        id  serial not null,
        forward varchar(255),
        reverse varchar(255),
        primary key (id)
    );

    create table TrackInfo (
        id  serial not null,
        corners bytea,
        numberOfTracks int4 not null,
        originText varchar(255),
        trackEndDate int8 not null,
        trackStartDate int8 not null,
        primary key (id)
    );

    create table TrackStyleDefinition (
        id  serial not null,
        bucketMode int4,
        displayName varchar(255),
        elevationThreshold float8,
        fadedOverTimeFlag bool,
        family varchar(255),
        glyphConstantOpacity float4,
        glyphConstantSize int4,
        glyphResource varchar(255),
        glyphVerticalAlignment int4,
        groupability int4,
        isUserDefined bool,
        linkGlyphOpacityToTrackOpacity bool,
        linkGlyphSizeToTrackSize bool,
        linkGlyphToHeading bool,
        linkTrackShapeToHeading bool,
        showHeading bool,
        snapToSurface bool,
        timeThreshold int8,
        trackShape int4,
        trackShapeConstantColor int4,
        trackShapeConstantOpacity float4,
        trackShapeConstantSize int4,
        type varchar(255),
        uniqueKey varchar(255) unique,
        visible bool,
        primary key (id)
    );

    create table TrackStyleDefinitionMetadataPair (
        id  serial not null,
        metadataKey varchar(255),
        metadataValue varchar(255),
        trackStyleDefinition_id int4,
        primary key (id)
    );

    create table VFSFile (
        id  serial not null,
        creationDate int8 not null,
        creator varchar(255),
        creator_uuid_msb int8,
        creator_uuid_lsb int8,
        hashValue varchar(255),
        pathOnDisk varchar(255),
        ready bool not null,
        relativePath varchar(255),
        remote bool not null,
        remoteFileSize int8 not null,
        uuid_msb int8,
        uuid_lsb int8,
        vfsNamespace_id int4,
        primary key (id)
    );

    create table "class" (
        id int2 not null,
        name varchar(255),
        primary key (id)
    );

    create table channeldef_objecttype (
        channeldef_id int4 not null,
        objecttype_id int4 not null,
        primary key (channeldef_id, objecttype_id)
    );

    create table channeldef_structure (
        channeldef_id int4 not null,
        structuredef_id int4 not null,
        primary key (channeldef_id, structuredef_id)
    );

    create table channeldef_vfsnamespace (
        channeldef_id int4 not null,
        namespace_id int4 not null,
        primary key (channeldef_id, namespace_id)
    );

    create table eventlog (
        id int4 not null,
        details varchar(255),
        logDate timestamp,
        mapObjectFamily varchar(255),
        mapObjectType varchar(255),
        mapObjectUnitId varchar(255),
        message varchar(255),
        name varchar(255),
        state int4,
        target varchar(255),
        type int4 not null,
        primary key (id)
    );

    create table file_channeldef (
        file_id int4 not null,
        channeldef_id int4 not null
    );

    create table mappointmessage_channel (
        id  serial not null,
        sequenceNumber int4 not null,
        channel_id int4,
        message_id int4,
        primary key (id)
    );

    create table mappointmessage_trackstyledefinition (
        id  serial not null,
        message_id int4,
        trackStyle_id int4,
        primary key (id)
    );

    alter table AnnotationMapObject 
        add constraint FKDA9EE12C4F13C0AE 
        foreign key (id) 
        references MapObject;

    alter table ChannelDef 
        add constraint FK3BBA8226F9877C8 
        foreign key (class_id) 
        references "class";

    alter table ChannelStats 
        add constraint FK44C557C55479C6B 
        foreign key (mo_id) 
        references MapObject;

    alter table ChannelStats 
        add constraint FK44C557CA3E9AE7D 
        foreign key (channel_id) 
        references ChannelDef;

    alter table ClientIconMapObject 
        add constraint FKF3F1611717E0F035 
        foreign key (id) 
        references IconMapObject;

    alter table ConnectingLineDefinition 
        add constraint FKA712AD5F7F1B1B37 
        foreign key (parent_id) 
        references ConnectingLineDefinition;

    alter table ConnectingLineDefinitionMetadataPair 
        add constraint FK2960DA889F1E6C2 
        foreign key (connectingLineDefinition_id) 
        references ConnectingLineDefinition;

    create index moKey on CustomDataMonitor (moKey);

    alter table FieldMetaData 
        add constraint FK9B8A49D21165C8 
        foreign key (field_id) 
        references StructureField;

    alter table FileMapObject 
        add constraint FK372C857F7ADFAC2A 
        foreign key (id) 
        references ClientIconMapObject;

    alter table FileMapObject 
        add constraint FK372C857F716EDF52 
        foreign key (vfsFile_id) 
        references VFSFile;

    alter table FileMetaData 
        add constraint FKA355610B1A295875 
        foreign key (file_id) 
        references VFSFile;

    alter table FileTransfer 
        add constraint FKA8F29A788CF9EFE 
        foreign key (channelDef_id) 
        references ChannelDef;

    alter table FileTransfer 
        add constraint FKA8F29A7716EDF52 
        foreign key (vfsFile_id) 
        references VFSFile;

    alter table IconMapObject 
        add constraint FK29CECB22603D7842 
        foreign key (trackStyleDefinition_id) 
        references TrackStyleDefinition;

    alter table IconMapObject 
        add constraint FK29CECB224CC50842 
        foreign key (stopRecordsTrackStyleDefinition_id) 
        references TrackStyleDefinition;

    alter table IconMapObject 
        add constraint FK29CECB224F13C0AE 
        foreign key (id) 
        references MapObject;

    alter table IconMapObject 
        add constraint FK29CECB2289F1E6C2 
        foreign key (connectingLineDefinition_id) 
        references ConnectingLineDefinition;

    alter table IconMapObject 
        add constraint FK29CECB22934793EA 
        foreign key (parent_id) 
        references IconMapObject;

    alter table MapObject 
        add constraint FKA606DF9BDBADDB12 
        foreign key (trackInfo_id) 
        references TrackInfo;

    alter table MapObject 
        add constraint FKA606DF9BFACF3107 
        foreign key (owner_id) 
        references MapObjectOwner;

    alter table MapObjectOwnerAppPlugin 
        add constraint FK5FD53FBC97AD9FFB 
        foreign key (id) 
        references MapObjectOwner;

    alter table MapObjectOwnerFeaturePlugin 
        add constraint FK4EBAC8D197AD9FFB 
        foreign key (id) 
        references MapObjectOwner;

    alter table MapObjectOwnerImportExportPlugin 
        add constraint FK38B6724497AD9FFB 
        foreign key (id) 
        references MapObjectOwner;

    alter table MapObjectOwnerMapObject 
        add constraint FK3A9206C397AD9FFB 
        foreign key (id) 
        references MapObjectOwner;

    alter table MapObjectOwnerMapObject 
        add constraint FK3A9206C3B23551BA 
        foreign key (owner_id) 
        references MapObject;

    alter table MapObjectProxy 
        add constraint FKC388839387B6E7D2 
        foreign key (mapobject_id) 
        references MapObject;

    alter table MapObjectProxy 
        add constraint FKC3888393A3F97552 
        foreign key (mapentity_id) 
        references MapEntity;

    alter table MapObjectState 
        add constraint FKC3B37DB655479C6B 
        foreign key (mo_id) 
        references MapObject;

    create index rdm_receivetime_idx on MapPoint (receiveTime);

    create index rdm_longitude_idx on MapPoint (longitude);

    create index rdm_groupKey_idx on MapPoint (groupKey);

    create index rdm_devicetime_idx on MapPoint (time);

    create index rdm_latitude_idx on MapPoint (latitude);

    alter table MapPoint 
        add constraint FKDB11DB4501CDBC2 
        foreign key (mapObjectProxy_id) 
        references MapObjectProxy;

    alter table MapPoint 
        add constraint FKDB11DB494098CB2 
        foreign key (structure_id) 
        references Structure;

    alter table MapPointData 
        add constraint FK6FE3877EAB39CBD4 
        foreign key (mappoint_id) 
        references MapPoint;

    alter table MapPointDataBoolean 
        add constraint FKC14EB2CAED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataBooleanArray 
        add constraint FKFA8CFDAFED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataByteArray 
        add constraint FKB1D2AC53ED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataDate 
        add constraint FK3BD7FC4CED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataDateArray 
        add constraint FK1D3F556DED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataDouble 
        add constraint FKA68FD8EFED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataDoubleArray 
        add constraint FK4D36E6AAED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataFloat 
        add constraint FK3F48A97EED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataFloatArray 
        add constraint FK4B85177BED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataInt 
        add constraint FK9ED57F91ED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataIntArray 
        add constraint FK10FA1548ED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataLocation 
        add constraint FK62452D53ED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataLong 
        add constraint FK3BDBD31AED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataLongArray 
        add constraint FK33B78F5FED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataShort 
        add constraint FK3FFE0B9EED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataShortArray 
        add constraint FKE578095BED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataString 
        add constraint FKC06DBD2FED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataStringArray 
        add constraint FK539DAA6AED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table PersistableExtrudedLine 
        add constraint FKB872A8743E855450ceaa4621 
        foreign key (layer_id) 
        references ClientLayer;

    alter table PersistableExtrudedPolygon 
        add constraint FKB872A8743E855450b6bb73ed 
        foreign key (layer_id) 
        references ClientLayer;

    alter table PersistableLine 
        add constraint FKB872A8743E8554503478da22 
        foreign key (layer_id) 
        references ClientLayer;

    alter table PersistablePolygon 
        add constraint FKB872A8743E8554501974d44c 
        foreign key (layer_id) 
        references ClientLayer;

    alter table PersistableSurfacePolygon 
        add constraint FKB872A8743E855450c1989c7b 
        foreign key (layer_id) 
        references ClientLayer;

    alter table PersistableSurfacePolyline 
        add constraint FKB872A8743E855450717d229f 
        foreign key (layer_id) 
        references ClientLayer;

    alter table RigidShapeMapObject 
        add constraint FKC8CC44059B2555D 
        foreign key (id) 
        references ShapeMapObject;

    alter table ShapeBoxMapObject 
        add constraint FK749C3051EFBD4047 
        foreign key (id) 
        references RigidShapeMapObject;

    alter table ShapeCircleMapObject 
        add constraint FK9AC0D30A9B2555D 
        foreign key (id) 
        references ShapeMapObject;

    alter table ShapeConeMapObject 
        add constraint FK28A951D7EFBD4047 
        foreign key (id) 
        references RigidShapeMapObject;

    alter table ShapeCylinderMapObject 
        add constraint FKF9811B04EFBD4047 
        foreign key (id) 
        references RigidShapeMapObject;

    alter table ShapeEllipseMapObject 
        add constraint FK962BF1E9B2555D 
        foreign key (id) 
        references ShapeMapObject;

    alter table ShapeEllipsoidMapObject 
        add constraint FK6D56E159EFBD4047 
        foreign key (id) 
        references RigidShapeMapObject;

    alter table ShapeExtrudedPolygonMapObject 
        add constraint FK2DEB8FC127A84F38 
        foreign key (id) 
        references ShapePathMapObject;

    alter table ShapeMapObject 
        add constraint FK3581613A4F13C0AE 
        foreign key (id) 
        references MapObject;

    alter table ShapePathMapObject 
        add constraint FK33D21F159B2555D 
        foreign key (id) 
        references ShapeMapObject;

    alter table ShapePolygon3DMapObject 
        add constraint FKCB5A28317ED9476C 
        foreign key (id) 
        references ShapePolyline3DMapObject;

    alter table ShapePolygonMapObject 
        add constraint FKAE2D4D4227A84F38 
        foreign key (id) 
        references ShapePathMapObject;

    alter table ShapePolyline3DMapObject 
        add constraint FK22377D4927A84F38 
        foreign key (id) 
        references ShapePathMapObject;

    alter table ShapePyramidMapObject 
        add constraint FK4598D5ECEFBD4047 
        foreign key (id) 
        references RigidShapeMapObject;

    alter table ShapeRectangleMapObject 
        add constraint FKB3437C6D9B2555D 
        foreign key (id) 
        references ShapeMapObject;

    alter table ShapeSphereMapObject 
        add constraint FKA12934AD9B2555D 
        foreign key (id) 
        references ShapeMapObject;

    alter table ShapeSquareMapObject 
        add constraint FK80E7BD5D9B2555D 
        foreign key (id) 
        references ShapeMapObject;

    alter table ShapeWedgeMapObject 
        add constraint FK9BAC548EFBD4047 
        foreign key (id) 
        references RigidShapeMapObject;

    alter table StructureAssociation 
        add constraint FK7C19B52E94098CB2 
        foreign key (structure_id) 
        references Structure;

    alter table StructureAssociation 
        add constraint FK7C19B52EA3F97552 
        foreign key (mapentity_id) 
        references MapEntity;

    alter table StructureField 
        add constraint FK2826BF6794098CB2 
        foreign key (structure_id) 
        references Structure;

    alter table StructureMetaData 
        add constraint FK919B9DC294098CB2 
        foreign key (structure_id) 
        references Structure;

    alter table SurfaceStructureRelationship 
        add constraint FK9D8E78BE8D9D384F 
        foreign key (type_id) 
        references SurfaceStructureRelationshipType;

    alter table TrackStyleDefinitionMetadataPair 
        add constraint FK19A3CA82603D7842 
        foreign key (trackStyleDefinition_id) 
        references TrackStyleDefinition;

    alter table VFSFile 
        add constraint FK417E2BBF11932FC2 
        foreign key (vfsNamespace_id) 
        references FileSystemNamespace;

    alter table channeldef_objecttype 
        add constraint FKD2DF033688CF9EFE 
        foreign key (channeldef_id) 
        references ChannelDef;

    alter table channeldef_objecttype 
        add constraint FKD2DF0336A649D8E 
        foreign key (objecttype_id) 
        references MapObjectType;

    alter table channeldef_structure 
        add constraint FK2623299688CF9EFE 
        foreign key (channeldef_id) 
        references ChannelDef;

    alter table channeldef_structure 
        add constraint FK262329964FED63DE 
        foreign key (structuredef_id) 
        references StructureDef;

    alter table channeldef_vfsnamespace 
        add constraint FK919B05F588CF9EFE 
        foreign key (channeldef_id) 
        references ChannelDef;

    alter table channeldef_vfsnamespace 
        add constraint FK919B05F58335F47F 
        foreign key (namespace_id) 
        references FileSystemNamespace;

    alter table file_channeldef 
        add constraint FKE7899EC51A295875 
        foreign key (file_id) 
        references VFSFile;

    alter table file_channeldef 
        add constraint FKE7899EC588CF9EFE 
        foreign key (channeldef_id) 
        references ChannelDef;

    create index cr_sequence_idx on mappointmessage_channel (sequenceNumber);

    alter table mappointmessage_channel 
        add constraint FKF9B46497166DA22B 
        foreign key (message_id) 
        references MapPoint;

    alter table mappointmessage_channel 
        add constraint FKF9B46497A3E9AE7D 
        foreign key (channel_id) 
        references ChannelDef;

    alter table mappointmessage_trackstyledefinition 
        add constraint FKB56A2059531B5B2 
        foreign key (message_id) 
        references MapPoint;

    alter table mappointmessage_trackstyledefinition 
        add constraint FKB56A205241402F5 
        foreign key (trackStyle_id) 
        references TrackStyleDefinition;

    create sequence hibernate_sequence;
