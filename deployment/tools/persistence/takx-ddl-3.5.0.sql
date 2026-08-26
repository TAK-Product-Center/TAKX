
    create table AMedia (
       mediaType varchar(31) not null,
        id serial not null,
        color integer,
        editURL varchar(255),
        fileSize bigint,
        info varchar(255),
        jsonMetaData text,
        keyOfSource varchar(255),
        lastModified bigint,
        localFile varchar(255),
        location_elevation float(53),
        location_hasElevation boolean,
        location_latitude float(53),
        location_longitude float(53),
        name varchar(255),
        thumbnailURL varchar(255),
        time bigint,
        url varchar(255),
        height integer,
        width integer,
        duration bigint,
        primary key (id)
    );

    create table ChatMessage (
       id serial not null,
        conversationUid varchar(255) not null,
        receiveTime bigint not null,
        sendTime bigint not null,
        senderName varchar(255) not null,
        senderUid varchar(255) not null,
        status varchar(255),
        text text not null,
        uid varchar(255) not null,
        primary key (id)
    );

    create table ClientLayer (
       id serial not null,
        className varchar(255),
        configFileName varchar(255),
        deleted boolean not null,
        displayName varchar(255),
        editable boolean not null,
        iconPath varchar(255),
        key_ varchar(255),
        layerGroup varchar(255),
        maxActiveAltitude float(53) not null,
        maxActiveAltitudeEnabled boolean not null,
        minActiveAltitude float(53) not null,
        minActiveAltitudeEnabled boolean not null,
        name varchar(255),
        networkRetrievalEnabled boolean not null,
        opacity float(53) not null,
        orderkey integer,
        permanent boolean not null,
        pickable boolean not null,
        restoreStateXML text,
        splitScale float(53) not null,
        visible boolean not null,
        primary key (id)
    );

    create table ConnectingLineDefinition (
       id serial not null,
        displayName varchar(255),
        elevationThreshold float(53),
        family varchar(255),
        isUserDefined boolean,
        lineColorValue integer,
        lineOpacity float4,
        lineStyle smallint,
        lineWidth integer,
        showDirectionalArrow boolean,
        type varchar(255),
        uniqueKey varchar(255),
        parent_id integer,
        primary key (id)
    );

    create table Connection (
       id serial not null,
        family varchar(255),
        name varchar(255),
        type varchar(255),
        uniqueKey varchar(255),
        primary key (id)
    );

    create table CustomDataMonitor (
       id serial not null,
        ackReq boolean not null,
        alarmState smallint,
        alarmType varchar(255),
        clearable boolean not null,
        comparisonType smallint,
        defaultState smallint,
        displayName varchar(255),
        enabled boolean not null,
        family varchar(255),
        fieldName varchar(255),
        fieldType smallint,
        geofenceAlarmTrigger smallint,
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
       id serial not null,
        metadataAsJson text,
        defaultValue varchar(255),
        fieldLength integer,
        fieldName varchar(255),
        fieldType integer not null,
        ordinalPosition integer not null,
        structure_id integer,
        primary key (id)
    );

    create table DeletedMapObject (
       id serial not null,
        connectingLineDefinitionId integer not null,
        inProgress boolean not null,
        mapObjectId integer not null,
        mapObjectKey varchar(255),
        partialDelete boolean not null,
        timeOfDeletion bigint not null,
        trackStyleDefinitionId integer not null,
        mapEntity_id integer not null,
        primary key (id)
    );

    create table GroupContact (
       id serial not null,
        attributes text,
        membersAsJson text,
        name varchar(255) not null,
        parentUid varchar(255),
        uid varchar(255) not null,
        primary key (id)
    );

    create table IndexDefinition (
       id serial not null,
        columnNames varchar(255) array,
        uniqueIdx boolean not null,
        structure_id integer not null,
        primary key (id)
    );

    create table MapEntity (
       id serial not null,
        family varchar(255),
        type varchar(255),
        primary key (id)
    );

    create table MapObject (
       DTYPE varchar(31) not null,
        id serial not null,
        allowDeviceContainment boolean not null,
        allowDrag boolean not null,
        attributeSet text,
        cotType varchar(255),
        defaultPreferencesTab varchar(255),
        defaultTrackColor integer not null,
        description varchar(255),
        displayableType varchar(255),
        elevationMode smallint,
        geoPointMetaData_geopoint_access smallint,
        geoPointMetaData_geopoint_altitude float(53) not null,
        geoPointMetaData_geopoint_altitudeReference smallint,
        geoPointMetaData_geopoint_ce90 float(53) not null,
        geoPointMetaData_geopoint_latitude float(53) not null,
        geoPointMetaData_geopoint_le90 float(53) not null,
        geoPointMetaData_geopoint_longitude float(53) not null,
        geoPointMetaData_metadata text,
        groupName varchar(255),
        heading float(53) not null,
        iconRenderingMode varchar(255),
        iconScale float(53) not null,
        imageURI varchar(255),
        mapObjectOptions_optionFlags bigint not null,
        mostRecentCommunicationTime bigint not null,
        name varchar(255),
        oldestCommunicationTime bigint not null,
        ownerId integer,
        remarks text,
        snapIconToSurface boolean not null,
        speed float(53) not null,
        trackInfo_corners float(53) array,
        trackInfo_numberOfTracks integer not null,
        trackInfo_trackEndDate bigint not null,
        trackInfo_trackStartDate bigint not null,
        uid varchar(255),
        uniqueKey varchar(255),
        unitID varchar(255),
        visible boolean not null,
        style integer,
        containedMapObjectsIds varchar(255),
        parentContainerId integer,
        namespace varchar(255),
        relativePath varchar(255),
        backgroundColor bytea,
        borderColor bytea,
        borderRadius integer,
        borderShape integer,
        borderThickness integer,
        drawOffsetX integer,
        drawOffsetY integer,
        enableFixedHeight boolean,
        enableLeader boolean,
        fixedHeight float(53),
        font bytea,
        maxActiveAltitude float(53),
        minActiveAltitude float(53),
        opacity float(53),
        text varchar(2048),
        textAlign integer,
        textColor bytea,
        backgroundFillEnabled boolean,
        borderEnabled boolean,
        orientation float(53),
        size float(53),
        textAlignment varchar(255),
        centerAnchorId integer,
        modelJson text,
        heightMeters float(53),
        polylineMapObjectIds text,
        radiusAnchorId integer,
        connectingLineDefinition_id integer,
        stopRecordsTrackStyleDefinition_id integer,
        trackStyleDefinition_id integer,
        primary key (id)
    );

    create table PersistableSurfaceStructure (
       DTYPE varchar(31) not null,
        id serial not null,
        description varchar(255),
        location_elevation float(53),
        location_hasElevation boolean,
        location_latitude float(53),
        location_longitude float(53),
        metadata text,
        name varchar(255),
        url varchar(255),
        uuid bytea,
        blockCoordCount bytea,
        numOfVertices integer,
        numberOfBlocks integer,
        sector float(53) array,
        vertices bytea,
        altitudeMode integer,
        isExtrude boolean,
        isTesselate boolean,
        extrudeHeight float(53),
        recordNumber integer,
        layer_id integer,
        primary key (id)
    );

    create table PluginState (
       id serial not null,
        state text,
        uniqueKey varchar(255),
        primary key (id)
    );

    create table RaptorEventLog (
       id serial not null,
        annotation varchar(255),
        details varchar(255),
        logDate timestamp(6),
        mapObjectFamily varchar(255),
        mapObjectId integer,
        mapObjectType varchar(255),
        mapObjectUnitId varchar(255),
        message varchar(255),
        name varchar(255),
        state smallint,
        target varchar(255),
        type integer not null,
        primary key (id)
    );

    create table Structure (
       id serial not null,
        metadataAsJson text,
        calculatedHashCode integer not null,
        hashValue varchar(255),
        isRenderable boolean not null,
        messageType varchar(255),
        version varchar(255),
        primary key (id)
    );

    create table TrackStyleDefinition (
       id serial not null,
        bucketMode smallint,
        displayName varchar(255),
        elevationThreshold float(53),
        fadedOverTimeFlag boolean,
        family varchar(255),
        glyphConstantOpacity float4,
        glyphConstantSize integer,
        glyphResource varchar(255),
        glyphVerticalAlignment smallint,
        glyphVerticalOffset float(53),
        groupability smallint,
        isUserDefined boolean,
        linkGlyphOpacityToTrackOpacity boolean,
        linkGlyphSizeToTrackSize boolean,
        linkGlyphToHeading boolean,
        linkTrackShapeToHeading boolean,
        showHeading boolean,
        snapToSurface boolean,
        timeThreshold bigint,
        trackShape smallint,
        trackShapeConstantColor integer,
        trackShapeConstantOpacity float4,
        trackShapeConstantSize integer,
        type varchar(255),
        uniqueKey varchar(255),
        visible boolean,
        primary key (id)
    );

    alter table ChatMessage 
       add constraint UK_b6rmjogcok4ooq9kdgfvnp643 unique (uid);

    alter table ConnectingLineDefinition 
       add constraint UK_ejsfduekkibx5y2iw7789s03p unique (uniqueKey);

    alter table Connection 
       add constraint UK7e4nij7x37qqv1ohc76cvyf2y unique (uniqueKey);
create index moKey on CustomDataMonitor (moKey);

    alter table GroupContact 
       add constraint UK_a91qg9722figibctgq2e6lfhc unique (uid);

    alter table MapEntity 
       add constraint UK18hlx7wey1ch55a7yh4hvvcyy unique (family, type);

    alter table MapObject 
       add constraint MapObject_uniqueKey unique (uniqueKey);
create index idx_el_mo_id on RaptorEventLog (mapObjectId);

    alter table Structure 
       add constraint UK7jf2c7ujfb7a3r0ts0o93r0qv unique (hashValue);

    alter table TrackStyleDefinition 
       add constraint UK_faoty2clqwg11ka3a1qn6uahb unique (uniqueKey);

    alter table ConnectingLineDefinition 
       add constraint FKjaug3cgu73drngy82hi7vmqcd 
       foreign key (parent_id) 
       references ConnectingLineDefinition;

    alter table DataField 
       add constraint FKgksr7xajbyhog9g5ghqwu4fnl 
       foreign key (structure_id) 
       references Structure;

    alter table DeletedMapObject 
       add constraint FKiws4tseqrpmstqh1oshxgo1po 
       foreign key (mapEntity_id) 
       references MapEntity;

    alter table IndexDefinition 
       add constraint FKqtlg2plxsgvvlsw1jptp1jauo 
       foreign key (structure_id) 
       references Structure;

    alter table MapObject 
       add constraint FK208ovsfsay4oy229eco5rg2v0 
       foreign key (connectingLineDefinition_id) 
       references ConnectingLineDefinition;

    alter table MapObject 
       add constraint FKoyfn15dps9cyd93dc679a4d7v 
       foreign key (stopRecordsTrackStyleDefinition_id) 
       references TrackStyleDefinition;

    alter table MapObject 
       add constraint FKr4tw5t2b1gsw13515brop1qji 
       foreign key (trackStyleDefinition_id) 
       references TrackStyleDefinition;

    alter table PersistableSurfaceStructure 
       add constraint FK2c91divxtgi0k163vrdnc1mlr 
       foreign key (layer_id) 
       references ClientLayer;
