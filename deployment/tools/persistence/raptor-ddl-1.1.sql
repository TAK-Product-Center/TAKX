
    alter table ClientAnnotationMapObject 
        drop constraint FKA4146B611870E516;

    alter table ClientFileMapObject 
        drop constraint FK14F1B74EBC5A09D;

    alter table ClientIconMapObject 
        drop constraint FKF3F161171870E516;

    alter table ClientIconMapObject 
        drop constraint FKF3F16117672C4452;

    alter table ClientLineMapObject 
        drop constraint FKC22AA7FC1870E516;

    alter table ClientMapObject 
        drop constraint FKFC64B8103F685122;

    alter table ClientShapeCircleMapObject 
        drop constraint FKFFFC8F75B263B1F5;

    alter table ClientShapeEllipseMapObject 
        drop constraint FK4B9E9013B263B1F5;

    alter table ClientShapeMapObject 
        drop constraint FKAFB189E51870E516;

    alter table ClientShapePathMapObject 
        drop constraint FK5317FA40B263B1F5;

    alter table ClientShapePathPoint 
        drop constraint FK69C59A95FC38852E;

    alter table ClientShapePolyPoint 
        drop constraint FK8DE6974EFE14A81B;

    alter table ClientShapePolygonMapObject 
        drop constraint FKF0691E37B263B1F5;

    alter table ClientShapeRectangleMapObject 
        drop constraint FK55CEE422B263B1F5;

    alter table ClientShapeSquareMapObject 
        drop constraint FKE62379C8B263B1F5;

    alter table CommandMessage 
        drop constraint FKA933CA5CA4D9A9CF;

    alter table MapObject 
        drop constraint FKA606DF9BD627E761;

    alter table MapPoint 
        drop constraint FKDB11DB42CE1243D;

    alter table MapPointData 
        drop constraint FK6FE3877E93C6983A;

    alter table MapPointDataBoolean 
        drop constraint FKC14EB2CAC547ECF6;

    alter table MapPointDataByteArray 
        drop constraint FKB1D2AC53C547ECF6;

    alter table MapPointDataDate 
        drop constraint FK3BD7FC4CC547ECF6;

    alter table MapPointDataDouble 
        drop constraint FKA68FD8EFC547ECF6;

    alter table MapPointDataFloat 
        drop constraint FK3F48A97EC547ECF6;

    alter table MapPointDataInt 
        drop constraint FK9ED57F91C547ECF6;

    alter table MapPointDataList 
        drop constraint FK3BDBBD3CC547ECF6;

    alter table MapPointDataLocation 
        drop constraint FK62452D53C547ECF6;

    alter table MapPointDataLong 
        drop constraint FK3BDBD31AC547ECF6;

    alter table MapPointDataShort 
        drop constraint FK3FFE0B9EC547ECF6;

    alter table MapPointDataString 
        drop constraint FKC06DBD2FC547ECF6;

    alter table MapPointMessage 
        drop constraint FK81BB67B3A4D9A9CF;

    alter table PropertyMessage 
        drop constraint FKDD602852A4D9A9CF;

    alter table StructureAssociation 
        drop constraint FK7C19B52E13D9CBF4;

    alter table StructureAssociation 
        drop constraint FK7C19B52ED627E761;

    alter table StructureField 
        drop constraint FK2826BF677CDC86D6;

    alter table mappointdatalistjoin 
        drop constraint FK3D057026713A7E84;

    alter table mappointdatalistjoin 
        drop constraint FK3D05702674AE55E;

    drop table AppState;

    drop table ClientAnnotationMapObject;

    drop table ClientFileMapObject;

    drop table ClientIconMapObject;

    drop table ClientLayer;

    drop table ClientLineMapObject;

    drop table ClientMapObject;

    drop table ClientShapeCircleMapObject;

    drop table ClientShapeEllipseMapObject;

    drop table ClientShapeMapObject;

    drop table ClientShapePathMapObject;

    drop table ClientShapePathPoint;

    drop table ClientShapePolyPoint;

    drop table ClientShapePolygonMapObject;

    drop table ClientShapeRectangleMapObject;

    drop table ClientShapeSquareMapObject;

    drop table CommandMessage;

    drop table ExceptionLog;

    drop table MapEntity;

    drop table MapObject;

    drop table MapPoint;

    drop table MapPointData;

    drop table MapPointDataBoolean;

    drop table MapPointDataByteArray;

    drop table MapPointDataDate;

    drop table MapPointDataDouble;

    drop table MapPointDataFloat;

    drop table MapPointDataInt;

    drop table MapPointDataList;

    drop table MapPointDataLocation;

    drop table MapPointDataLong;

    drop table MapPointDataShort;

    drop table MapPointDataString;

    drop table MapPointMessage;

    drop table ProgramVersion;

    drop table PropertyMessage;

    drop table Structure;

    drop table StructureAssociation;

    drop table StructureField;

    drop table eventlog;

    drop table mappointdatalistjoin;

    drop sequence hibernate_sequence;

    create table AppState (
        id  serial not null,
        className varchar(255),
        state varchar(255),
        primary key (id)
    );

    create table ClientAnnotationMapObject (
        backgroundColor bytea,
        font bytea,
        opacity float8 not null,
        text varchar(255),
        textAlign int4 not null,
        textColor bytea,
        id int4 not null,
        primary key (id)
    );

    create table ClientFileMapObject (
        relativePath varchar(255),
        id int4 not null,
        primary key (id)
    );

    create table ClientIconMapObject (
        contained bool not null,
        deviceState text,
        objectkey varchar(255),
        id int4 not null,
        parent_id int4,
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
        visible bool not null,
        primary key (id)
    );

    create table ClientLineMapObject (
        id int4 not null,
        primary key (id)
    );

    create table ClientMapObject (
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
        imagePath varchar(255),
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
        moom_id_ int4,
        primary key (id)
    );

    create table ClientShapeCircleMapObject (
        radius float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table ClientShapeEllipseMapObject (
        majoraxis float8 not null,
        minoraxis float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table ClientShapeMapObject (
        fillColor int4,
        lineColor int4,
        lineWidth float8,
        orientationDegrees float8,
        textureHeight float8,
        textureWidth float8,
        id int4 not null,
        primary key (id)
    );

    create table ClientShapePathMapObject (
        id int4 not null,
        primary key (id)
    );

    create table ClientShapePathPoint (
        shape_id int4 not null,
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        orderkey int4 not null,
        primary key (shape_id, orderkey)
    );

    create table ClientShapePolyPoint (
        shape_id int4 not null,
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        orderkey int4 not null,
        primary key (shape_id, orderkey)
    );

    create table ClientShapePolygonMapObject (
        id int4 not null,
        primary key (id)
    );

    create table ClientShapeRectangleMapObject (
        halfheight float8 not null,
        halfwidth float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table ClientShapeSquareMapObject (
        halfwidth float8 not null,
        id int4 not null,
        primary key (id)
    );

    create table CommandMessage (
        mapObjectId varchar(255),
        mapObjectType varchar(255),
        messageType varchar(255),
        portName varchar(255),
        version varchar(255),
        id int4 not null,
        primary key (id)
    );

    create table ExceptionLog (
        id  serial not null,
        exception text,
        object text,
        primary key (id)
    );

    create table MapEntity (
        id_  serial not null,
        name_ varchar(255),
        version_ varchar(255),
        primary key (id_)
    );

    create table MapObject (
        id_  serial not null,
        mapObjectid_ varchar(255),
        subType_ varchar(255),
        topic_ varchar(255),
        type_ varchar(255),
        version_ varchar(255),
        mapentity_id_ int4 not null,
        primary key (id_)
    );

    create table MapPoint (
        id  serial not null,
        mapobject_id_ int4 not null,
        primary key (id)
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

    create table MapPointDataFloat (
        value float4,
        id int4 not null,
        primary key (id)
    );

    create table MapPointDataInt (
        value int4,
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
        mapObjectId varchar(255),
        mapObjectType varchar(255),
        messageType varchar(255),
        pitch float8 not null,
        portName varchar(255),
        receiveTime int8 not null,
        roll float8 not null,
        speed float8 not null,
        time int8 not null,
        version varchar(255),
        id int4 not null,
        primary key (id)
    );

    create table ProgramVersion (
        id  serial not null,
        version varchar(255),
        primary key (id)
    );

    create table PropertyMessage (
        creationTime int8 not null,
        mapObjectId varchar(255),
        mapObjectType varchar(255),
        messageType varchar(255),
        receiveTime int8 not null,
        version varchar(255),
        id int4 not null,
        primary key (id)
    );

    create table Structure (
        id  serial not null,
        mapObjectType varchar(255),
        messageType varchar(255),
        version varchar(255),
        primary key (id)
    );

    create table StructureAssociation (
        MapEntity_id_ int4 not null,
        dataStructures__id int4 not null
    );

    create table StructureField (
        id  serial not null,
        defaultValue_ varchar(255),
        fieldName_ varchar(255),
        fieldType_ int4 not null,
        ordinalPosition_ int4,
        structure_id int4,
        primary key (id)
    );

    create table eventlog (
        id int4 not null,
        details varchar(255),
        logDate timestamp,
        message varchar(255),
        source varchar(255),
        target varchar(255),
        type int4 not null,
        primary key (id)
    );

    create table mappointdatalistjoin (
        MapPointDataList_id int4 not null,
        value_id int4 not null,
        unique (value_id)
    );

    alter table ClientAnnotationMapObject 
        add constraint FKA4146B611870E516 
        foreign key (id) 
        references ClientMapObject;

    alter table ClientFileMapObject 
        add constraint FK14F1B74EBC5A09D 
        foreign key (id) 
        references ClientIconMapObject;

    alter table ClientIconMapObject 
        add constraint FKF3F161171870E516 
        foreign key (id) 
        references ClientMapObject;

    alter table ClientIconMapObject 
        add constraint FKF3F16117672C4452 
        foreign key (parent_id) 
        references ClientIconMapObject;

    alter table ClientLineMapObject 
        add constraint FKC22AA7FC1870E516 
        foreign key (id) 
        references ClientMapObject;

    alter table ClientMapObject 
        add constraint FKFC64B8103F685122 
        foreign key (moom_id_) 
        references MapObject;

    alter table ClientShapeCircleMapObject 
        add constraint FKFFFC8F75B263B1F5 
        foreign key (id) 
        references ClientShapeMapObject;

    alter table ClientShapeEllipseMapObject 
        add constraint FK4B9E9013B263B1F5 
        foreign key (id) 
        references ClientShapeMapObject;

    alter table ClientShapeMapObject 
        add constraint FKAFB189E51870E516 
        foreign key (id) 
        references ClientMapObject;

    alter table ClientShapePathMapObject 
        add constraint FK5317FA40B263B1F5 
        foreign key (id) 
        references ClientShapeMapObject;

    alter table ClientShapePathPoint 
        add constraint FK69C59A95FC38852E 
        foreign key (shape_id) 
        references ClientShapePathMapObject;

    alter table ClientShapePolyPoint 
        add constraint FK8DE6974EFE14A81B 
        foreign key (shape_id) 
        references ClientShapePolygonMapObject;

    alter table ClientShapePolygonMapObject 
        add constraint FKF0691E37B263B1F5 
        foreign key (id) 
        references ClientShapeMapObject;

    alter table ClientShapeRectangleMapObject 
        add constraint FK55CEE422B263B1F5 
        foreign key (id) 
        references ClientShapeMapObject;

    alter table ClientShapeSquareMapObject 
        add constraint FKE62379C8B263B1F5 
        foreign key (id) 
        references ClientShapeMapObject;

    alter table CommandMessage 
        add constraint FKA933CA5CA4D9A9CF 
        foreign key (id) 
        references MapPoint;

    alter table MapObject 
        add constraint FKA606DF9BD627E761 
        foreign key (mapentity_id_) 
        references MapEntity;

    alter table MapPoint 
        add constraint FKDB11DB42CE1243D 
        foreign key (mapobject_id_) 
        references MapObject;

    alter table MapPointData 
        add constraint FK6FE3877E93C6983A 
        foreign key (mappoint_id) 
        references MapPoint;

    alter table MapPointDataBoolean 
        add constraint FKC14EB2CAC547ECF6 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataByteArray 
        add constraint FKB1D2AC53C547ECF6 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataDate 
        add constraint FK3BD7FC4CC547ECF6 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataDouble 
        add constraint FKA68FD8EFC547ECF6 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataFloat 
        add constraint FK3F48A97EC547ECF6 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataInt 
        add constraint FK9ED57F91C547ECF6 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataList 
        add constraint FK3BDBBD3CC547ECF6 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataLocation 
        add constraint FK62452D53C547ECF6 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataLong 
        add constraint FK3BDBD31AC547ECF6 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataShort 
        add constraint FK3FFE0B9EC547ECF6 
        foreign key (id) 
        references MapPointData;

    alter table MapPointDataString 
        add constraint FKC06DBD2FC547ECF6 
        foreign key (id) 
        references MapPointData;

    create index latitude_idx on MapPointMessage (latitude);

    create index longitude_idx on MapPointMessage (longitude);

    alter table MapPointMessage 
        add constraint FK81BB67B3A4D9A9CF 
        foreign key (id) 
        references MapPoint;

    alter table PropertyMessage 
        add constraint FKDD602852A4D9A9CF 
        foreign key (id) 
        references MapPoint;

    alter table StructureAssociation 
        add constraint FK7C19B52E13D9CBF4 
        foreign key (dataStructures__id) 
        references Structure;

    alter table StructureAssociation 
        add constraint FK7C19B52ED627E761 
        foreign key (MapEntity_id_) 
        references MapEntity;

    alter table StructureField 
        add constraint FK2826BF677CDC86D6 
        foreign key (structure_id) 
        references Structure;

    alter table mappointdatalistjoin 
        add constraint FK3D057026713A7E84 
        foreign key (value_id) 
        references MapPointData;

    alter table mappointdatalistjoin 
        add constraint FK3D05702674AE55E 
        foreign key (MapPointDataList_id) 
        references MapPointDataList;

    create sequence hibernate_sequence;
