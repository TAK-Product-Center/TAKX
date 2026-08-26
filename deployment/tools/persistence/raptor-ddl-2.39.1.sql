
    create table AFileTransfer (
        transferType varchar(31) not null,
        id  serial not null,
        bytesTransferred int8 not null,
        fileName varchar(255),
        server varchar(255),
        totalBytes int8 not null,
        uuid bytea,
        vfsFile_id int4,
        primary key (id)
    );

    create table AMedia (
        mediaType varchar(31) not null,
        id  serial not null,
        color int4,
        editURL varchar(255),
        fileSize int8,
        info varchar(255),
        jsonMetaData varchar(40000),
        keyOfSource varchar(255),
        lastModified int8,
        localFile varchar(255),
        elevation float8,
        hasElevation boolean,
        latitude float8,
        longitude float8,
        name varchar(255),
        securableTypeString varchar(255),
        thumbnailURL varchar(255),
        time int8,
        url varchar(255),
        primary key (id)
    );

    create table ARigidShapeMapObject (
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

    create table AVerticalShapeMapObject (
        verticalHeight float8 not null,
        verticalHeightEnabled boolean not null,
        id int4 not null,
        primary key (id)
    );

    create table AnnotationMapObject (
        backgroundColor bytea,
        borderColor bytea,
        borderRadius int4 not null,
        borderShape int4 not null,
        borderThickness int4 not null,
        drawOffsetX int4 not null,
        drawOffsetY int4 not null,
        enableFixedHeight boolean not null,
        enableLeader boolean not null,
        fixedHeight float8 not null,
        font bytea,
        maxActiveAltitude float8 not null,
        minActiveAltitude float8 not null,
        opacity float8 not null,
        text varchar(2048),
        textAlign int4 not null,
        textColor bytea,
        id int4 not null,
        primary key (id)
    );

    create table Application (
        id  serial not null,
        family varchar(255),
        type varchar(255),
        primary key (id)
    );

    create table ClientLayer (
        id  serial not null,
        className varchar(255),
        configFileName varchar(255),
        deleted boolean not null,
        displayName varchar(255),
        editable boolean not null,
        iconPath varchar(255),
        key_ varchar(255),
        layerGroup varchar(255),
        maxActiveAltitude float8 not null,
        maxActiveAltitudeEnabled boolean not null,
        minActiveAltitude float8 not null,
        minActiveAltitudeEnabled boolean not null,
        name varchar(255),
        networkRetrievalEnabled boolean not null,
        opacity float8 not null,
        orderkey int4,
        permanent boolean not null,
        pickable boolean not null,
        restoreStateXML text,
        splitScale float8 not null,
        visible boolean not null,
        primary key (id)
    );

    create table ConnectingLineDefinition (
        id  serial not null,
        metadataAsJson text,
        displayName varchar(255),
        elevationThreshold float8,
        family varchar(255),
        isUserDefined boolean,
        lineColorValue int4,
        lineOpacity float4,
        lineStyle int4,
        lineWidth int4,
        showDirectionalArrow boolean,
        type varchar(255),
        uniqueKey varchar(255),
        parent_id int4,
        primary key (id)
    );

    create table Connection (
        id  serial not null,
        family varchar(255),
        name varchar(255),
        type varchar(255),
        uniqueKey varchar(255),
        primary key (id)
    );

    create table CustomDataMonitor (
        id  serial not null,
        ackReq boolean not null,
        alarmFilename varchar(255),
        alarmState int4,
        alarmType varchar(255),
        areAllAlarmValuesSet boolean not null,
        clearable boolean not null,
        comparisonType int4,
        defaultState int4,
        displayName varchar(255),
        enabled boolean not null,
        family varchar(255),
        fieldName varchar(255),
        fieldType int4,
        geofenceAlarmTrigger int4,
        geofenceShapeKey varchar(255),
        holdState boolean not null,
        moKey varchar(255),
        showValue boolean not null,
        structureName varchar(255),
        threshold varchar(255),
        type varchar(255),
        useAudio boolean not null,
        wasLastLocationInsideGeofence boolean not null,
        primary key (id)
    );

    create table DataField (
        id  serial not null,
        metadataAsJson text,
        defaultValue varchar(255),
        fieldLength int4,
        fieldName varchar(255),
        fieldType int4 not null,
        ordinalPosition int4 not null,
        sharedFlag boolean,
        structure_id int4,
        primary key (id)
    );

    create table DeletedMapObject (
        id  serial not null,
        connectingLineDefinitionId int4 not null,
        inProgress boolean not null,
        mapObjectId int4 not null,
        mapObjectKey varchar(255),
        partialDelete boolean not null,
        timeOfDeletion int8 not null,
        trackInfoId int4 not null,
        trackStyleDefinitionId int4 not null,
        mapEntity_id int4 not null,
        primary key (id)
    );

    create table ExceptionLog (
        id  serial not null,
        exception_ text,
        object text,
        primary key (id)
    );

    create table FileMapObject (
        id int4 not null,
        vfsFile_id int4,
        primary key (id)
    );

    create table IconMapObject (
        contained boolean not null,
        id int4 not null,
        connectingLineDefinition_id int4,
        parent_id int4,
        stopRecordsTrackStyleDefinition_id int4,
        trackStyleDefinition_id int4,
        primary key (id)
    );

    create table IndexDefinition (
        id  serial not null,
        uniqueIdx boolean not null,
        structure_id int4 not null,
        primary key (id)
    );

    create table IndexDefinitionColumn (
        id  serial not null,
        columnName varchar(255),
        idef_id int4 not null,
        primary key (id)
    );

    create table MapEntity (
        id  serial not null,
        family varchar(255),
        type varchar(255),
        version varchar(255),
        primary key (id)
    );

    create table MapObject (
        id  serial not null,
        allowDeviceContainment boolean not null,
        allowDrag boolean not null,
        bucketMode int4,
        defaultPreferencesTab varchar(255),
        defaultTrackColor bytea,
        description varchar(255),
        displayableType varchar(255),
        elevationMode int4,
        extraData text,
        groupName varchar(255),
        hasHeading boolean not null,
        heading float8 not null,
        iconRenderingMode int4,
        iconScale float8 not null,
        imageURI varchar(255),
        infoText text,
        elevation float8,
        hasElevation boolean,
        latitude float8,
        longitude float8,
        optionFlags int8 not null,
        mostRecentCommunicationTime int8 not null,
        name varchar(255),
        oldestCommunicationTime int8 not null,
        sourceNodeUuid_msb int8,
        sourceNodeUuid_lsb int8,
        ownerId int4,
        securityFilter int4,
        snapIconToSurface boolean not null,
        uniqueKey varchar(255),
        unitID varchar(255),
        visible boolean not null,
        trackInfo_id int4,
        primary key (id)
    );

    create table PersistableExtrudedLine (
        id int4 not null,
        description varchar(255),
        elevation float8,
        hasElevation boolean,
        latitude float8,
        longitude float8,
        name varchar(255),
        restorableMetadata text,
        url varchar(255),
        uuid bytea,
        layer_id int4,
        altitudeMode int4 not null,
        isExtrude boolean not null,
        isTesselate boolean not null,
        numOfVertices int4 not null,
        sector bytea,
        vertices bytea,
        primary key (id)
    );

    create table PersistableExtrudedPolygon (
        id int4 not null,
        description varchar(255),
        elevation float8,
        hasElevation boolean,
        latitude float8,
        longitude float8,
        name varchar(255),
        restorableMetadata text,
        url varchar(255),
        uuid bytea,
        layer_id int4,
        altitudeMode int4 not null,
        blockCoordCount bytea,
        extrudeHeight bytea,
        isExtrude boolean not null,
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
        hasElevation boolean,
        latitude float8,
        longitude float8,
        name varchar(255),
        restorableMetadata text,
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
        hasElevation boolean,
        latitude float8,
        longitude float8,
        name varchar(255),
        restorableMetadata text,
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

    create table PersistableShapefileShape (
        id int4 not null,
        description varchar(255),
        elevation float8,
        hasElevation boolean,
        latitude float8,
        longitude float8,
        name varchar(255),
        restorableMetadata text,
        url varchar(255),
        uuid bytea,
        layer_id int4,
        recordNumber int4 not null,
        primary key (id)
    );

    create table PersistableSurfacePolygon (
        id int4 not null,
        description varchar(255),
        elevation float8,
        hasElevation boolean,
        latitude float8,
        longitude float8,
        name varchar(255),
        restorableMetadata text,
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
        hasElevation boolean,
        latitude float8,
        longitude float8,
        name varchar(255),
        restorableMetadata text,
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
        upgraderCount int4,
        version varchar(255),
        primary key (id)
    );

    create table RaptorEventLog (
        id int4 not null,
        annotation varchar(255),
        details varchar(255),
        logDate timestamp,
        mapObjectFamily varchar(255),
        mapObjectProxyId int4,
        mapObjectType varchar(255),
        mapObjectUnitId varchar(255),
        message varchar(255),
        name varchar(255),
        state int4,
        target varchar(255),
        type int4 not null,
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
        editable boolean not null,
        fillColor int4 not null,
        lineColor int4 not null,
        lineWidth float8 not null,
        orientable boolean not null,
        orientationDegrees float8,
        published boolean not null,
        restorableMetadata text,
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
        verticalHeight float8 not null,
        verticalHeightEnabled boolean not null,
        id int4 not null,
        primary key (id)
    );

    create table ShapePolygonMapObject_ShapePolygonMapObject (
        ShapePolygonMapObject_id int4 not null,
        holes_id int4 not null,
        primary key (ShapePolygonMapObject_id, holes_id)
    );

    create table ShapePolyline3DMapObject (
        elevation float8,
        hasElevation boolean,
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

    create table ShapeRangeFanMapObject (
        interval float8 not null,
        maxDistance float8 not null,
        minDistance float8 not null,
        rangeDistances bytea,
        rangeLabels bytea,
        rangeType int4 not null,
        sampleDistance float8 not null,
        showEdges boolean not null,
        showOrientation boolean not null,
        showRangeValues boolean not null,
        sweep float8 not null,
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
        metadataAsJson text,
        calculatedHashCode int4 not null,
        hashValue varchar(255),
        isRenderable boolean not null,
        messageType varchar(255),
        version varchar(255),
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
        metadataAsJson text,
        bucketMode int4,
        displayName varchar(255),
        elevationThreshold float8,
        fadedOverTimeFlag boolean,
        family varchar(255),
        glyphConstantOpacity float4,
        glyphConstantSize int4,
        glyphResource varchar(255),
        glyphVerticalAlignment int4,
        glyphVerticalOffset float8,
        groupability int4,
        isUserDefined boolean,
        linkGlyphOpacityToTrackOpacity boolean,
        linkGlyphSizeToTrackSize boolean,
        linkGlyphToHeading boolean,
        linkTrackShapeToHeading boolean,
        showHeading boolean,
        snapToSurface boolean,
        timeThreshold int8,
        trackShape int4,
        trackShapeConstantColor int4,
        trackShapeConstantOpacity float4,
        trackShapeConstantSize int4,
        type varchar(255),
        uniqueKey varchar(255),
        visible boolean,
        primary key (id)
    );

    create table VFSFile (
        id  serial not null,
        metadataAsJson text,
        creationDate int8 not null,
        creator varchar(255),
        creator_uuid_msb int8,
        creator_uuid_lsb int8,
        hashValue varchar(255),
        packageIgnore boolean,
        pathOnDisk varchar(255),
        ready boolean not null,
        relativePath varchar(255),
        remote boolean not null,
        remoteFileSize int8 not null,
        uuid_msb int8,
        uuid_lsb int8,
        vfsNamespace_id int4,
        primary key (id)
    );

    create table VFSNamespace (
        id  serial not null,
        namespace varchar(255),
        primary key (id)
    );

    alter table Application 
        add constraint UK_frpt7wbpsuu6d06fg5xdrjdrv  unique (family, type);

    alter table ConnectingLineDefinition 
        add constraint UK_ejsfduekkibx5y2iw7789s03p  unique (uniqueKey);

    alter table Connection 
        add constraint UK_7e4nij7x37qqv1ohc76cvyf2y  unique (uniqueKey);

    create index moKey on CustomDataMonitor (moKey);

    alter table MapEntity 
        add constraint UK_18hlx7wey1ch55a7yh4hvvcyy  unique (family, type);

    alter table MapObject 
        add constraint MapObject_uniqueKey  unique (uniqueKey);

    alter table ShapePolygonMapObject_ShapePolygonMapObject 
        add constraint UK_2adyeu10g3t0enyb96homstd0  unique (holes_id);

    alter table Structure 
        add constraint UK_7jf2c7ujfb7a3r0ts0o93r0qv  unique (hashValue);

    alter table TrackStyleDefinition 
        add constraint UK_faoty2clqwg11ka3a1qn6uahb  unique (uniqueKey);

    alter table AFileTransfer 
        add constraint FK_1ljvx327o7au8glry8y0qxdq7 
        foreign key (vfsFile_id) 
        references VFSFile;

    alter table ARigidShapeMapObject 
        add constraint FK_16ssknihg5by2oyers8bt5bym 
        foreign key (id) 
        references ShapeMapObject;

    alter table AVerticalShapeMapObject 
        add constraint FK_olyn28x6u03j2cc3qve2d05gw 
        foreign key (id) 
        references ShapeMapObject;

    alter table AnnotationMapObject 
        add constraint FK_7w7v1gfuiqpvauncvjpklucdk 
        foreign key (id) 
        references MapObject;

    alter table ConnectingLineDefinition 
        add constraint FK_hs5ab6m2e1mymapyr1ky884u7 
        foreign key (parent_id) 
        references ConnectingLineDefinition;

    alter table DataField 
        add constraint FK_pd23qrjrlwks9i3rhnpfqg1a4 
        foreign key (structure_id) 
        references Structure;

    alter table DeletedMapObject 
        add constraint FK_jb1knw1tloh8g98cb7pd7xcff 
        foreign key (mapEntity_id) 
        references MapEntity;

    alter table FileMapObject 
        add constraint FK_qopickecutbedy148uyijonfs 
        foreign key (vfsFile_id) 
        references VFSFile;

    alter table FileMapObject 
        add constraint FK_8j3vqyekpmn1a8syhns8pn9fa 
        foreign key (id) 
        references IconMapObject;

    alter table IconMapObject 
        add constraint FK_auksttwtlolp7b641348jrdhq 
        foreign key (connectingLineDefinition_id) 
        references ConnectingLineDefinition;

    alter table IconMapObject 
        add constraint FK_pc1j7jqsj8q5xf874d51cerxq 
        foreign key (parent_id) 
        references IconMapObject;

    alter table IconMapObject 
        add constraint FK_267el1v9aavxq7oay5n4dgyj1 
        foreign key (stopRecordsTrackStyleDefinition_id) 
        references TrackStyleDefinition;

    alter table IconMapObject 
        add constraint FK_jggm29rjppj82h9wqc3thmupe 
        foreign key (trackStyleDefinition_id) 
        references TrackStyleDefinition;

    alter table IconMapObject 
        add constraint FK_4ts2s2necs6akqx6alijumy9p 
        foreign key (id) 
        references MapObject;

    alter table IndexDefinition 
        add constraint FK_akixmd12avvb70b2pwqqufxlx 
        foreign key (structure_id) 
        references Structure;

    alter table IndexDefinitionColumn 
        add constraint FK_6hqc6qp11m63nmt0uyv0w1tc3 
        foreign key (idef_id) 
        references IndexDefinition;

    alter table MapObject 
        add constraint FK_nf32dylspnuac0dtl6epaxwkh 
        foreign key (trackInfo_id) 
        references TrackInfo;

    alter table PersistableExtrudedLine 
        add constraint FK_5ibd30vi7fx9dydh7erprdiul 
        foreign key (layer_id) 
        references ClientLayer;

    alter table PersistableExtrudedPolygon 
        add constraint FK_sqllec8qqinfstvfy66m53tk3 
        foreign key (layer_id) 
        references ClientLayer;

    alter table PersistableLine 
        add constraint FK_ng521wk93kc7owsoafbme2le 
        foreign key (layer_id) 
        references ClientLayer;

    alter table PersistablePolygon 
        add constraint FK_f355ffurs34py6gkhrpletthk 
        foreign key (layer_id) 
        references ClientLayer;

    alter table PersistableShapefileShape 
        add constraint FK_8kq9uk2olln5lv9fsxugrpos 
        foreign key (layer_id) 
        references ClientLayer;

    alter table PersistableSurfacePolygon 
        add constraint FK_4w2n2utexjhp3esstys9xr3w8 
        foreign key (layer_id) 
        references ClientLayer;

    alter table PersistableSurfacePolyline 
        add constraint FK_t8j0j02w2bjlpxmdtg7vui1pp 
        foreign key (layer_id) 
        references ClientLayer;

    alter table ShapeBoxMapObject 
        add constraint FK_ik8qgk9gsvlff8i699110vlfd 
        foreign key (id) 
        references ARigidShapeMapObject;

    alter table ShapeCircleMapObject 
        add constraint FK_62gedr3ay34ho6aurdq0n35q0 
        foreign key (id) 
        references AVerticalShapeMapObject;

    alter table ShapeConeMapObject 
        add constraint FK_eu9o0frjd8u62p0mwpepdrbkm 
        foreign key (id) 
        references ARigidShapeMapObject;

    alter table ShapeCylinderMapObject 
        add constraint FK_2082d6de1i35fsexm3fotxdgd 
        foreign key (id) 
        references ARigidShapeMapObject;

    alter table ShapeEllipseMapObject 
        add constraint FK_8o9pwco443yvmmfld2cniaaya 
        foreign key (id) 
        references AVerticalShapeMapObject;

    alter table ShapeEllipsoidMapObject 
        add constraint FK_flreh91u7lp2dqfug39yf3rgm 
        foreign key (id) 
        references ARigidShapeMapObject;

    alter table ShapeExtrudedPolygonMapObject 
        add constraint FK_l0265f1511h8tlel8gynsopjj 
        foreign key (id) 
        references ShapePathMapObject;

    alter table ShapeMapObject 
        add constraint FK_jiups8nmuxtge7r54d806k229 
        foreign key (id) 
        references MapObject;

    alter table ShapePathMapObject 
        add constraint FK_931jcmrphhvp0e0hl482ug275 
        foreign key (id) 
        references ShapeMapObject;

    alter table ShapePolygon3DMapObject 
        add constraint FK_3yxu5ou37trvfukumwdcqn1t0 
        foreign key (id) 
        references ShapePolyline3DMapObject;

    alter table ShapePolygonMapObject 
        add constraint FK_qmqcd0gomr0v5cdf37k36wesh 
        foreign key (id) 
        references ShapePathMapObject;

    alter table ShapePolygonMapObject_ShapePolygonMapObject 
        add constraint FK_2adyeu10g3t0enyb96homstd0 
        foreign key (holes_id) 
        references ShapePolygonMapObject;

    alter table ShapePolygonMapObject_ShapePolygonMapObject 
        add constraint FK_6r5q7neaq6psppi0nwmf0od25 
        foreign key (ShapePolygonMapObject_id) 
        references ShapePolygonMapObject;

    alter table ShapePolyline3DMapObject 
        add constraint FK_oyb3vaekb5qqbc4hyb82vxs9o 
        foreign key (id) 
        references ShapePathMapObject;

    alter table ShapePyramidMapObject 
        add constraint FK_gwunvjktnayas8wgsxiw82ndy 
        foreign key (id) 
        references ARigidShapeMapObject;

    alter table ShapeRangeFanMapObject 
        add constraint FK_scrk6xbuy7yrapaq5goi5rny1 
        foreign key (id) 
        references ShapeMapObject;

    alter table ShapeRectangleMapObject 
        add constraint FK_m4tln1p9c7wllgrs9kbn1sy7q 
        foreign key (id) 
        references AVerticalShapeMapObject;

    alter table ShapeSphereMapObject 
        add constraint FK_rdh4blko997rmxlpniobxq3n6 
        foreign key (id) 
        references ShapeMapObject;

    alter table ShapeSquareMapObject 
        add constraint FK_4fngc6aargw47asl1gm97g5om 
        foreign key (id) 
        references AVerticalShapeMapObject;

    alter table ShapeWedgeMapObject 
        add constraint FK_1mo4i4yq90fgt5q5h58ifqgdg 
        foreign key (id) 
        references ARigidShapeMapObject;

    alter table SurfaceStructureRelationship 
        add constraint FK_bwa8fifofi5tqti9x4y7rqxqc 
        foreign key (type_id) 
        references SurfaceStructureRelationshipType;

    alter table VFSFile 
        add constraint FK_cov9tkw8l051g79l8nihavrsy 
        foreign key (vfsNamespace_id) 
        references VFSNamespace;

    create sequence hibernate_sequence;

    create table hibernate_sequences (
         sequence_name varchar(255),
         sequence_next_hi_value int4 
    );
