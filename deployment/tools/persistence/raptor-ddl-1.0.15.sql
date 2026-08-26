
    alter table ClientAnnotationMapObject 
        drop constraint FKA4146B617DBC0B81;

    alter table ClientIconMapObject 
        drop constraint FKF3F161177DBC0B81;

    alter table ClientIconMapObject 
        drop constraint FKF3F161174C583D3D;

    alter table ClientLineMapObject 
        drop constraint FKC22AA7FC7DBC0B81;

    alter table ClientMapObject 
        drop constraint FKFC64B8103F685122;

    alter table ClientShapeMapObject 
        drop constraint FKAFB189E57DBC0B81;

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

    alter table MapPointDataLong 
        drop constraint FK3BDBD31AC547ECF6;

    alter table MapPointDataShort 
        drop constraint FK3FFE0B9EC547ECF6;

    alter table MapPointDataString 
        drop constraint FKC06DBD2FC547ECF6;

    alter table MapPointMessage 
        drop constraint FK81BB67B3A4D9A9CF;

    alter table StructureAssociation 
        drop constraint FK7C19B52E13D9CBF4;

    alter table StructureAssociation 
        drop constraint FK7C19B52ED627E761;

    alter table StructureField 
        drop constraint FK2826BF677CDC86D6;

    drop table ClientAnnotationMapObject;

    drop table ClientIconMapObject;

    drop table ClientLayer;

    drop table ClientLineMapObject;

    drop table ClientMapObject;

    drop table ClientShapeMapObject;

    drop table CommandMessage;

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

    drop table MapPointDataLong;

    drop table MapPointDataShort;

    drop table MapPointDataString;

    drop table MapPointMessage;

    drop table ProgramVersion;

    drop table Structure;

    drop table StructureAssociation;

    drop table StructureField;

    drop table eventlog;

    drop sequence hibernate_sequence;

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

    create table ClientIconMapObject (
        contained bool not null,
        objectkey varchar(255),
        id int4 not null,
        parent_id int4,
        primary key (id)
    );

    create table ClientLayer (
        id  serial not null,
        className varchar(255),
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
        elevation float8,
        hasElevation bool,
        latitude float8,
        longitude float8,
        mostRecentCommunicationTime int8 not null,
        oldestCommunicationTime int8 not null,
        overrideProgramTracksTimeThreshold bool not null,
        securityFilter int4,
        showHeading bool not null,
        showTracks bool not null,
        snapTracksToSurface bool not null,
        tracksTimeThreshold int8 not null,
        unitID varchar(255),
        visible bool not null,
        moom_id_ int4,
        primary key (id)
    );

    create table ClientShapeMapObject (
        restoreStateXML text,
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
        structure_id int4,
        primary key (id)
    );

    create table eventlog (
        id int4 not null,
        details varchar(255),
        logDate timestamp,
        message varchar(255),
        source varchar(255),
        type int4 not null,
        primary key (id)
    );

    alter table ClientAnnotationMapObject 
        add constraint FKA4146B617DBC0B81 
        foreign key (id) 
        references ClientMapObject;

    alter table ClientIconMapObject 
        add constraint FKF3F161177DBC0B81 
        foreign key (id) 
        references ClientMapObject;

    alter table ClientIconMapObject 
        add constraint FKF3F161174C583D3D 
        foreign key (parent_id) 
        references ClientIconMapObject;

    alter table ClientLineMapObject 
        add constraint FKC22AA7FC7DBC0B81 
        foreign key (id) 
        references ClientMapObject;

    alter table ClientMapObject 
        add constraint FKFC64B8103F685122 
        foreign key (moom_id_) 
        references MapObject;

    alter table ClientShapeMapObject 
        add constraint FKAFB189E57DBC0B81 
        foreign key (id) 
        references ClientMapObject;

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

    create sequence hibernate_sequence;
