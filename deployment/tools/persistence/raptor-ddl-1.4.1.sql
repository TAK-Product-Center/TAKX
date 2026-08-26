
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

    alter table CommandMessage 
        drop constraint FKA933CA5C7B340EF3;

    alter table FileMapObject 
        drop constraint FK372C857F7ADFAC2A;

    alter table FileMapObject 
        drop constraint FK372C857F4AFE4474;

    alter table IconMapObject 
        drop constraint FK29CECB224F13C0AE;

    alter table IconMapObject 
        drop constraint FK29CECB22934793EA;

    alter table MapObject 
        drop constraint FKA606DF9BFACF3107;

    alter table MapObjectOwnerAppPlugin 
        drop constraint FK5FD53FBC97AD9FFB;

    alter table MapObjectOwnerImportExportPlugin 
        drop constraint FK38B6724497AD9FFB;

    alter table MapObjectOwnerMapObject 
        drop constraint FK3A9206C397AD9FFB;

    alter table MapObjectOwnerMapObject 
        drop constraint FK3A9206C3B23551BA;

    alter table MapObjectOwnerRaptorFeature 
        drop constraint FK593DB78897AD9FFB;

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
        drop constraint FK6FE3877E6A20FD5E;

    alter table MapPointDataBoolean 
        drop constraint FKC14EB2CAED3A5352;

    alter table MapPointDataByteArray 
        drop constraint FKB1D2AC53ED3A5352;

    alter table MapPointDataDate 
        drop constraint FK3BD7FC4CED3A5352;

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

    alter table MapPointDataList 
        drop constraint FK3BDBBD3CED3A5352;

    alter table MapPointDataLocation 
        drop constraint FK62452D53ED3A5352;

    alter table MapPointDataLong 
        drop constraint FK3BDBD31AED3A5352;

    alter table MapPointDataShort 
        drop constraint FK3FFE0B9EED3A5352;

    alter table MapPointDataString 
        drop constraint FKC06DBD2FED3A5352;

    alter table MapPointMessage 
        drop constraint FK81BB67B37B340EF3;

    alter table PropertyMessage 
        drop constraint FKDD6028527B340EF3;

    alter table ShapeCircleMapObject 
        drop constraint FK9AC0D30A9B2555D;

    alter table ShapeEllipseMapObject 
        drop constraint FK962BF1E9B2555D;

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

    alter table ShapeRectangleMapObject 
        drop constraint FKB3437C6D9B2555D;

    alter table ShapeSphereMapObject 
        drop constraint FKA12934AD9B2555D;

    alter table ShapeSquareMapObject 
        drop constraint FK80E7BD5D9B2555D;

    alter table StructureAssociation 
        drop constraint FK7C19B52E94098CB2;

    alter table StructureAssociation 
        drop constraint FK7C19B52EA3F97552;

    alter table StructureField 
        drop constraint FK2826BF6794098CB2;

    alter table channeldef_objecttype 
        drop constraint FKD2DF033688CF9EFE;

    alter table channeldef_objecttype 
        drop constraint FKD2DF0336A649D8E;

    alter table channeldef_structure 
        drop constraint FK2623299688CF9EFE;

    alter table channeldef_structure 
        drop constraint FK262329964FED63DE;

    alter table mappointdatalistjoin 
        drop constraint FK3D057026992CE4E0;

    alter table mappointdatalistjoin 
        drop constraint FK3D057026B0A09182;

    alter table mappointmessage_channel 
        drop constraint FKF9B46497166DA22B;

    alter table mappointmessage_channel 
        drop constraint FKF9B46497A3E9AE7D;

    drop table AnnotationMapObject;

    drop table Application;

    drop table ChannelDef;

    drop table ChannelStats;

    drop table ClientIconMapObject;

    drop table ClientLayer;

    drop table CommandMessage;

    drop table Connection;

    drop table ExceptionLog;

    drop table FileMapObject;

    drop table FileSystemNamespace;

    drop table IconMapObject;

    drop table MapEntity;

    drop table MapObject;

    drop table MapObjectOwner;

    drop table MapObjectOwnerAppPlugin;

    drop table MapObjectOwnerImportExportPlugin;

    drop table MapObjectOwnerMapObject;

    drop table MapObjectOwnerRaptorFeature;

    drop table MapObjectProxy;

    drop table MapObjectState;

    drop table MapObjectType;

    drop table MapPoint;

    drop table MapPointData;

    drop table MapPointDataBoolean;

    drop table MapPointDataByteArray;

    drop table MapPointDataDate;

    drop table MapPointDataDouble;

    drop table MapPointDataDoubleArray;

    drop table MapPointDataFloat;

    drop table MapPointDataFloatArray;

    drop table MapPointDataInt;

    drop table MapPointDataIntArray;

    drop table MapPointDataList;

    drop table MapPointDataLocation;

    drop table MapPointDataLong;

    drop table MapPointDataShort;

    drop table MapPointDataString;

    drop table MapPointMessage;

    drop table PluginState;

    drop table ProgramVersion;

    drop table PropertyMessage;

    drop table SaveState;

    drop table ShapeCircleMapObject;

    drop table ShapeEllipseMapObject;

    drop table ShapeMapObject;

    drop table ShapePathMapObject;

    drop table ShapePolygon3DMapObject;

    drop table ShapePolygonMapObject;

    drop table ShapePolyline3DMapObject;

    drop table ShapeRectangleMapObject;

    drop table ShapeSphereMapObject;

    drop table ShapeSquareMapObject;

    drop table Structure;

    drop table StructureAssociation;

    drop table StructureDef;

    drop table StructureField;

    drop table "class";

    drop table channeldef_objecttype;

    drop table channeldef_structure;

    drop table eventlog;

    drop table mappointdatalistjoin;

    drop table mappointmessage_channel;

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
        displayName varchar(255),
        editable bool not null,
        iconPath varchar(255),
        key varchar(255),
        layerGroup int4,
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

    create table CommandMessage (
        sourceName varchar(255),
        sourceType int4,
        id int4 not null,
        primary key (id)
    );

    create table Connection (
        id  serial not null,
        family varchar(255),
        name varchar(255),
        type varchar(255),
        primary key (id),
        unique (family, name)
    );

    create table ExceptionLog (
        id  serial not null,
        exception text,
        object text,
        primary key (id)
    );

    create table FileMapObject (
        relativePath varchar(255),
        id int4 not null,
        fsNamespace_id int4,
        primary key (id)
    );

    create table FileSystemNamespace (
        id  serial not null,
        namespace varchar(255),
        rootDir varchar(255),
        primary key (id)
    );

    create table IconMapObject (
        contained bool not null,
        id int4 not null,
        parent_id int4,
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
        defaultTracksColor bytea,
        description varchar(255),
        displayableType varchar(255),
        fadeTracksOverTime bool not null,
        groupName varchar(255),
        hasLocalElevation bool,
        hasHeading bool not null,
        hasTracksTimeThreshold bool not null,
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
        overrideProgramTracksPreferences bool not null,
        securityFilter int4,
        showHeading bool not null,
        showTracks bool not null,
        snapIconToSurface bool not null,
        snapTracksToSurface bool not null,
        tracksTimeThreshold int8 not null,
        unitID varchar(255),
        visible bool not null,
        owner_id int4,
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

    create table MapObjectOwnerImportExportPlugin (
        id int4 not null,
        primary key (id)
    );

    create table MapObjectOwnerMapObject (
        id int4 not null,
        owner_id int4,
        primary key (id)
    );

    create table MapObjectOwnerRaptorFeature (
        id int4 not null,
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
        id  serial not null,
        uuid_msb int8,
        uuid_lsb int8,
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

    create table MapPointDataList (
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

    create table MapPointDataShort (
        value int2,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataString (
        value varchar(2048),
        id int4 not null,
        primary key (id)
    );

    create table MapPointMessage (
        elevation float8 not null,
        groupKey varchar(32),
        has3dOrientation bool not null,
        hasElevation bool not null,
        hasHeading bool not null,
        hasLocation bool not null,
        hasMotion bool not null,
        hasSpeed bool not null,
        hasTime bool not null,
        heading float8 not null,
        isManualLocation bool not null,
        latitude float8 not null,
        longitude float8 not null,
        pitch float8 not null,
        receiveTime int8 not null,
        roll float8 not null,
        sourceName varchar(255),
        sourceType int4,
        speed float8 not null,
        time int8 not null,
        id int4 not null,
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

    create table PropertyMessage (
        creationTime int8 not null,
        receiveTime int8 not null,
        id int4 not null,
        primary key (id)
    );

    create table SaveState (
        id  serial not null,
        saved bool not null,
        primary key (id)
    );

    create table ShapeCircleMapObject (
        radius float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table ShapeEllipseMapObject (
        semiMajorAxis float8 not null,
        semiMinorAxis float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table ShapeMapObject (
        editable bool not null,
        fillColor int4,
        isGeofence bool not null,
        lineColor int4,
        lineWidth float8,
        orientationDegrees float8,
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

    create table Structure (
        id  serial not null,
        hashValue varchar(255),
        messageType varchar(255),
        version varchar(255),
        primary key (id),
        unique (hashValue)
    );

    create table StructureAssociation (
        mapentity_id int4 not null,
        structure_id int4 not null,
        primary key (mapentity_id, structure_id)
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
        fieldName varchar(255),
        fieldType int4 not null,
        ordinalPosition int4 not null,
        sharedFlag bool,
        structure_id int4,
        primary key (id)
    );

    create table "class" (
        id int2 not null,
        name varchar(255),
        primary key (id)
    );

    create table channeldef_objecttype (
        channeldef_id int4 not null,
        objecttype_id int4 not null
    );

    create table channeldef_structure (
        channeldef_id int4 not null,
        structuredef_id int4 not null
    );

    create table eventlog (
        id int4 not null,
        details varchar(255),
        logDate timestamp,
        mapObjectFamily varchar(255),
        mapObjectType varchar(255),
        mapObjectUnitId varchar(255),
        message varchar(255),
        target varchar(255),
        type int4 not null,
        primary key (id)
    );

    create table mappointdatalistjoin (
        MapPointDataList_id int4 not null,
        value_id int4 not null,
        order_key int4 not null,
        primary key (MapPointDataList_id, order_key),
        unique (value_id)
    );

    create table mappointmessage_channel (
        id  serial not null,
        sequenceNumber int4 not null,
        channel_id int4,
        message_id int4,
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

    alter table CommandMessage 
        add constraint FKA933CA5C7B340EF3 
        foreign key (id) 
        references MapPoint;

    alter table FileMapObject 
        add constraint FK372C857F7ADFAC2A 
        foreign key (id) 
        references ClientIconMapObject;

    alter table FileMapObject 
        add constraint FK372C857F4AFE4474 
        foreign key (fsNamespace_id) 
        references FileSystemNamespace;

    alter table IconMapObject 
        add constraint FK29CECB224F13C0AE 
        foreign key (id) 
        references MapObject;

    alter table IconMapObject 
        add constraint FK29CECB22934793EA 
        foreign key (parent_id) 
        references IconMapObject;

    alter table MapObject 
        add constraint FKA606DF9BFACF3107 
        foreign key (owner_id) 
        references MapObjectOwner;

    alter table MapObjectOwnerAppPlugin 
        add constraint FK5FD53FBC97AD9FFB 
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

    alter table MapObjectOwnerRaptorFeature 
        add constraint FK593DB78897AD9FFB 
        foreign key (id) 
        references MapObjectOwner;

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

    alter table MapPoint 
        add constraint FKDB11DB4501CDBC2 
        foreign key (mapObjectProxy_id) 
        references MapObjectProxy;

    alter table MapPoint 
        add constraint FKDB11DB494098CB2 
        foreign key (structure_id) 
        references Structure;

    alter table MapPointData 
        add constraint FK6FE3877E6A20FD5E 
        foreign key (mappoint_id) 
        references MapPoint;

    alter table MapPointDataBoolean 
        add constraint FKC14EB2CAED3A5352 
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

    alter table MapPointDataList 
        add constraint FK3BDBBD3CED3A5352 
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

    alter table MapPointDataShort 
        add constraint FK3FFE0B9EED3A5352 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataString 
        add constraint FKC06DBD2FED3A5352 
        foreign key (id) 
        references MapPointData;

    create index rdm_receivetime_idx on MapPointMessage (receiveTime);

    create index rdm_longitude_idx on MapPointMessage (longitude);

    create index rdm_devicetime_idx on MapPointMessage (time);

    create index groupKey_idx on MapPointMessage (groupKey);

    create index rdm_latitude_idx on MapPointMessage (latitude);

    alter table MapPointMessage 
        add constraint FK81BB67B37B340EF3 
        foreign key (id) 
        references MapPoint;

    alter table PropertyMessage 
        add constraint FKDD6028527B340EF3 
        foreign key (id) 
        references MapPoint;

    alter table ShapeCircleMapObject 
        add constraint FK9AC0D30A9B2555D 
        foreign key (id) 
        references ShapeMapObject;

    alter table ShapeEllipseMapObject 
        add constraint FK962BF1E9B2555D 
        foreign key (id) 
        references ShapeMapObject;

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

    alter table mappointdatalistjoin 
        add constraint FK3D057026992CE4E0 
        foreign key (value_id) 
        references MapPointData;

    alter table mappointdatalistjoin 
        add constraint FK3D057026B0A09182 
        foreign key (MapPointDataList_id) 
        references MapPointDataList;

    create index cr_sequence_idx on mappointmessage_channel (sequenceNumber);

    alter table mappointmessage_channel 
        add constraint FKF9B46497166DA22B 
        foreign key (message_id) 
        references MapPoint;

    alter table mappointmessage_channel 
        add constraint FKF9B46497A3E9AE7D 
        foreign key (channel_id) 
        references ChannelDef;

    create sequence hibernate_sequence;
