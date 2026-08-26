create sequence hibernate_sequence start 1 increment 1;

    create table AMedia (
       mediaType varchar(31) not null,
        id  serial not null,
        color int4,
        editURL varchar(255),
        fileSize int8,
        info varchar(255),
        jsonMetaData text,
        keyOfSource varchar(255),
        lastModified int8,
        localFile varchar(255),
        location_elevation float8,
        location_hasElevation boolean,
        location_latitude float8,
        location_longitude float8,
        name varchar(255),
        thumbnailURL varchar(255),
        time int8,
        url varchar(255),
        height int4,
        width int4,
        duration int8,
        primary key (id)
    );

    create table AttributeSet (
       id  serial not null,
        attributesAsJson text,
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
        alarmState int4,
        alarmType varchar(255),
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
        trackStyleDefinitionId int4 not null,
        mapEntity_id int4 not null,
        primary key (id)
    );

    create table GroupContact (
       id  serial not null,
        displayName varchar(255) not null,
        membersAsJson text,
        parentUid varchar(255),
        uniqueId varchar(255) not null,
        attributes_id int4,
        primary key (id)
    );

    create table IndexDefinition (
       id  serial not null,
        columnNames bytea,
        uniqueIdx boolean not null,
        structure_id int4 not null,
        primary key (id)
    );

    create table MapEntity (
       id  serial not null,
        family varchar(255),
        type varchar(255),
        primary key (id)
    );

    create table MapObject (
       DTYPE varchar(31) not null,
        id  serial not null,
        allowDeviceContainment boolean not null,
        allowDrag boolean not null,
        cotType varchar(255),
        defaultPreferencesTab varchar(255),
        defaultTrackColor int4 not null,
        description varchar(255),
        displayableType varchar(255),
        elevationMode int4,
        extraData text,
        geoPointMetaData_geopoint_access int4,
        geoPointMetaData_geopoint_altitude float8 not null,
        geoPointMetaData_geopoint_altitudeReference int4,
        geoPointMetaData_geopoint_ce90 float8 not null,
        geoPointMetaData_geopoint_latitude float8 not null,
        geoPointMetaData_geopoint_le90 float8 not null,
        geoPointMetaData_geopoint_longitude float8 not null,
        groupName varchar(255),
        heading float8 not null,
        iconRenderingMode varchar(255),
        iconScale float8 not null,
        imageURI varchar(255),
        infoText text,
        mapObjectOptions_optionFlags int8 not null,
        mostRecentCommunicationTime int8 not null,
        name varchar(255),
        oldestCommunicationTime int8 not null,
        ownerId int4,
        snapIconToSurface boolean not null,
        speed float8 not null,
        trackInfo_corners bytea,
        trackInfo_numberOfTracks int4 not null,
        trackInfo_trackEndDate int8 not null,
        trackInfo_trackStartDate int8 not null,
        uid varchar(255),
        uniqueKey varchar(255),
        unitID varchar(255),
        visible boolean not null,
        style int4,
        containedMapObjectsIds varchar(255),
        parentContainerId int4,
        namespace varchar(255),
        relativePath varchar(255),
        backgroundColor bytea,
        borderColor bytea,
        borderRadius int4,
        borderShape int4,
        borderThickness int4,
        drawOffsetX int4,
        drawOffsetY int4,
        enableFixedHeight boolean,
        enableLeader boolean,
        fixedHeight float8,
        font bytea,
        maxActiveAltitude float8,
        minActiveAltitude float8,
        opacity float8,
        text varchar(2048),
        textAlign int4,
        textColor bytea,
        backgroundFillEnabled boolean,
        borderEnabled boolean,
        orientation float8,
        size float8,
        textAlignment varchar(255),
        centerAnchorId int4,
        modelJson text,
        heightMeters float8,
        polylineMapObjectIds text,
        controlPointUids text,
        direction varchar(255),
        method varchar(255),
        pathMapObjectId int4,
        precedence varchar(255),
        prefix varchar(255),
        waypointMapObjectIds text,
        waypointOrder varchar(255),
        radiusAnchorId int4,
        groundReference_elevation float8,
        groundReference_hasElevation boolean,
        groundReference_latitude float8,
        groundReference_longitude float8,
        lowerAltitude float8,
        lowerAltitudeReferenceMode varchar(255),
        upperAltitude float8,
        upperAltitudeReferenceMode varchar(255),
        attributeSet_id int4,
        geoPointMetaData_metadata_id int4,
        connectingLineDefinition_id int4,
        stopRecordsTrackStyleDefinition_id int4,
        trackStyleDefinition_id int4,
        primary key (id)
    );

    create table PersistableSurfaceStructure (
       DTYPE varchar(31) not null,
        id int4 not null,
        description varchar(255),
        location_elevation float8,
        location_hasElevation boolean,
        location_latitude float8,
        location_longitude float8,
        name varchar(255),
        url varchar(255),
        uuid uuid,
        blockCoordCount bytea,
        numOfVertices int4,
        numberOfBlocks int4,
        sector bytea,
        vertices bytea,
        altitudeMode int4,
        isExtrude boolean,
        isTesselate boolean,
        extrudeHeight float8,
        recordNumber int4,
        layer_id int4,
        metadata_id int4,
        primary key (id)
    );

    create table PluginState (
       id  serial not null,
        state text,
        uniqueKey varchar(255),
        primary key (id)
    );

    create table RaptorEventLog (
       id  serial not null,
        annotation varchar(255),
        details varchar(255),
        logDate timestamp,
        mapObjectFamily varchar(255),
        mapObjectId int4,
        mapObjectType varchar(255),
        mapObjectUnitId varchar(255),
        message varchar(255),
        name varchar(255),
        state int4,
        target varchar(255),
        type int4 not null,
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

    create table TrackStyleDefinition (
       id  serial not null,
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

    alter table if exists ConnectingLineDefinition 
       add constraint UK_ejsfduekkibx5y2iw7789s03p unique (uniqueKey);

    alter table if exists Connection 
       add constraint UK7e4nij7x37qqv1ohc76cvyf2y unique (uniqueKey);
create index moKey on CustomDataMonitor (moKey);

    alter table if exists GroupContact 
       add constraint UK_56goqgpgn4iu0580ti8egmkwd unique (uniqueId);

    alter table if exists MapEntity 
       add constraint UK18hlx7wey1ch55a7yh4hvvcyy unique (family, type);

    alter table if exists MapObject 
       add constraint MapObject_uniqueKey unique (uniqueKey);
create index idx_el_mo_id on RaptorEventLog (mapObjectId);

    alter table if exists Structure 
       add constraint UK7jf2c7ujfb7a3r0ts0o93r0qv unique (hashValue);

    alter table if exists TrackStyleDefinition 
       add constraint UK_faoty2clqwg11ka3a1qn6uahb unique (uniqueKey);

    alter table if exists ConnectingLineDefinition 
       add constraint FKjaug3cgu73drngy82hi7vmqcd 
       foreign key (parent_id) 
       references ConnectingLineDefinition;

    alter table if exists DataField 
       add constraint FKgksr7xajbyhog9g5ghqwu4fnl 
       foreign key (structure_id) 
       references Structure;

    alter table if exists DeletedMapObject 
       add constraint FKiws4tseqrpmstqh1oshxgo1po 
       foreign key (mapEntity_id) 
       references MapEntity;

    alter table if exists GroupContact 
       add constraint FKnwftkn5c12ae1obp2any669ni 
       foreign key (attributes_id) 
       references AttributeSet;

    alter table if exists IndexDefinition 
       add constraint FKqtlg2plxsgvvlsw1jptp1jauo 
       foreign key (structure_id) 
       references Structure;

    alter table if exists MapObject 
       add constraint FKfuespwxfc1mnotmamdmhldxjv 
       foreign key (attributeSet_id) 
       references AttributeSet;

    alter table if exists MapObject 
       add constraint FK930q4gfqh3ekbo30a3kql9i25 
       foreign key (geoPointMetaData_metadata_id) 
       references AttributeSet;

    alter table if exists MapObject 
       add constraint FK208ovsfsay4oy229eco5rg2v0 
       foreign key (connectingLineDefinition_id) 
       references ConnectingLineDefinition;

    alter table if exists MapObject 
       add constraint FKoyfn15dps9cyd93dc679a4d7v 
       foreign key (stopRecordsTrackStyleDefinition_id) 
       references TrackStyleDefinition;

    alter table if exists MapObject 
       add constraint FKr4tw5t2b1gsw13515brop1qji 
       foreign key (trackStyleDefinition_id) 
       references TrackStyleDefinition;

    alter table if exists PersistableSurfaceStructure 
       add constraint FK2c91divxtgi0k163vrdnc1mlr 
       foreign key (layer_id) 
       references ClientLayer;

    alter table if exists PersistableSurfaceStructure 
       add constraint FKe8dy566d6f4330915a1nmjifb 
       foreign key (metadata_id) 
       references AttributeSet;
