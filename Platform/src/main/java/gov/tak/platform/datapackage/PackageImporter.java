package gov.tak.platform.datapackage;

import gov.tak.api.comms.security.ISslCertificateManager;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.tak.api.comms.takserver.IResource;
import gov.tak.api.util.TakPreferenceKeys;
import gov.tak.platform.cot.importer.CotImportHelper;
import gov.tak.platform.datapackage.content.DataPackageContents;
import gov.tak.platform.datapackage.content.ElementWithParameters;
import gov.tak.platform.datapackage.content.PackageManifest;
import gov.tak.platform.datapackage.content.PackageManifestContent;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.kml.IKmlLayerService;
import gov.takx.api.layer.IMobacLayerManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import jakarta.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Importer to handle the contents of a Data Package.
 *
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PackageImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IKmlLayerService kmlLayerService;

    @Inject
    private IMapObjectFinder mapObjectFinder;

    @Inject
    private IMobacLayerManager mobacLayerManager;

    @Inject
    private ISslCertificateManager certificateManager;

    @Inject
    private IExtendedDataPackageManager dataPackageManager;

    @Inject
    private CotImportHelper cotImportHelper;

    // Track certs imported via the Manifest contents.
    private final Set<String> certsImported = new HashSet<>();

    // Track attachments that need to be attached to MapObject
    private final List<PackageManifestContent> deferredAttachments = new ArrayList<>();

    // Map from cert name in the package to where it was placed by the certificate manager
    private final Map<String, File> certificateMap = new HashMap<>();

    private File packageFile;
    private final String owner;
    private final boolean makeCopy;
    private PackageReader packageReader;

    private PreferencesReader.Preferences preferences;

    /**
     * Construct an importer on the given package file.  The file will be copied into VFS after importing.
     * This constructor is used for local data package imports.
     *
     * @param packageFile Package file to process
     *
     * @see IExtendedDataPackageManager#copyPackageToVFS(File)
     */
    public PackageImporter(File packageFile)
    {
        this.packageFile = packageFile;
        owner = DataPackageContents.LOCAL_IMPORT_OWNER;
        makeCopy = true;
    }

    /**
     * Construct an importer on the given package file with the given owner.  The file will NOT be copied into VFS.
     * This constructor is used for importing downloaded data packages.
     *
     * @param packageFile Package file to process
     * @param ownerUid Data package owner's uid which can be looked up to get an up-to-date callsign.
     * In most circumstances, this value should be pulled from {@link IResource#creatorUid()}.
     *
     * @since 3.2
     */
    public PackageImporter(File packageFile, String ownerUid)
    {
        this.packageFile = packageFile;
        owner = ownerUid;
        makeCopy = false;
    }

    /**
     * Import the configured package file, copying to VFS as needed upon completion.
     *
     * @throws IOException If any operation fails
     */
    public void importPackage() throws IOException
    {
        try (PackageReader reader = new PackageReader(packageFile))
        {
            packageReader = reader;

            final PackageManifest manifest = reader.getManifest();

            logger.debug("MANIFEST: {}", manifest);

            final List<PackageManifestContent> contents = manifest.getContents().getContents().stream()
                    .filter(content -> !content.isIgnore())
                    .toList();

            for (PackageManifestContent content : contents) processContent(content);

            // Reimport any CoT events that were deferred on their initial import.
            cotImportHelper.importDeferredEvents().join();

            processDeferredAttachments();
            processAdditionalCerts();

            // Now that we've imported all certs and parsed the preferences, we can process any TAK Server connections.
            final var streamsPreference = preferences != null ? preferences.findByName(TakPreferenceKeys.COT_STREAMS) : null;
            if (streamsPreference != null) new TakServerConnectionImporter(streamsPreference, certificateMap).processServerConnections();

            packageFile = makeCopy
                    ? dataPackageManager.copyPackageToVFS(packageFile)
                    : packageFile;
        }
    }

    /**
     * Import the preferences and certificates for a TAK Server connection.
     *
     * @throws IOException If any operation fails
     * @since 3.2
     */
    public void importTakServerConnection() throws IOException
    {
        try (PackageReader reader = new PackageReader(packageFile))
        {
            packageReader = reader;

            processAdditionalCerts(); // import all cert files

            preferences = reader.getPreferences();
            if (preferences == null) throw new IOException("no preferences file found in: " + packageFile);

            // Importing connections requires the preference node to be up-to-date, so update them now
            new TakPreferencesImporter().importPreferences(preferences);

            // Now that we've imported all certs and parsed the preferences, we can process any TAK Server connections
            final var streams = preferences.findByName(TakPreferenceKeys.COT_STREAMS);
            if (streams == null) throw new IOException("No server connection configurations found: Missing 'cot_streams' preference");

            new TakServerConnectionImporter(streams, certificateMap).processServerConnections();
        }
    }

    /**
     * Some packages have incorrect references in the contents to entries in the zip. In order to be as lenient as
     * possible, we scan the whole package to find any cert files that have not already been imported, and import them.
     */
    private void processAdditionalCerts()
    {
        ZipUtil.iterate(packageFile, (in, zipEntry) -> {
            if (zipEntry.getName().toLowerCase().endsWith(".p12"))
            {
                final String name = FilenameUtils.getName(zipEntry.getName());

                if (!certsImported.contains(name))
                {
                    logger.debug("Import extra cert: {}", zipEntry);
                    createCert(zipEntry.getName(), in.readAllBytes());
                }
            }
        });
    }

    /**
     * Process attachments after we've processed all deferred CoT events as the attachments need to be attached to
     * {@link IMapObject}s that might be created by CoT messages in this package.
     *
     * @since 3.1
     */
    private void processDeferredAttachments()
    {
        for (PackageManifestContent attachment : deferredAttachments)
        {
            // If the UID for the entry is an existing MO, then we attach the file to it
            // Otherwise we skip it since I don't know what to do with it at this time
            final String targetUid = attachment.getParameterValue(ElementWithParameters.PARAMETER_UID);
            final IMapObject mapObject = targetUid != null ? mapObjectFinder.findMapObjectByUid(targetUid) : null;

            String manifestUid = attachment.getManifestUid();
            if (mapObject != null)
            {
                String attachmentName = FilenameUtils.getName(manifestUid);
                try (InputStream attachmentStream = packageReader.getInputStream(manifestUid))
                {
                    mapObject.addAttachment(attachmentName, attachmentStream);
                } catch (IOException e)
                {
                    logger.error("Failed to create filestream of package import for mapObject attachment: {} on {}", attachmentName, mapObject.getName(), e);
                }
            } else
            {
                logger.debug("Couldn't locate MO with UID {} for attachment {}", targetUid, manifestUid);
            }
        }
    }

    /**
     * Process a single manifest content element, and import it if we know how.
     *
     * @param content Content to process
     *
     * @throws IOException If processing fails
     */
    private void processContent(PackageManifestContent content) throws IOException
    {
        final String zipEntry = content.getManifestUid();
        final String suffix;
        try
        {
            suffix = FilenameUtils.getExtension(zipEntry).toLowerCase();
        } catch (IllegalArgumentException e)
        {
            final String msg = "Illegal file name: '" + zipEntry + "': " + e.getMessage();
            logger.warn(msg);
            AlwaysOnTopJOptionPane.showErrorDialog(msg, "Import failed");
            return;
        }

        switch (suffix)
        {
            case "pref" -> importPreferences(zipEntry);
            case "p12" -> importCert(zipEntry);
            case "xml" ->
                // At present, the only XML we know how to process is MOBAC layer configurations
                importPotentialMobacLayer(zipEntry);
            case "cot" ->
            {
                final String cotMessage = packageReader.readEntryAsString(zipEntry);
                logger.debug("CoT from {}: {}", zipEntry, cotMessage);
                cotImportHelper.parseAndImportCot(cotMessage).join();
            }
            case "kmz", "kml" -> importKml(zipEntry);
            default -> deferredAttachments.add(content);
        }
    }

    /**
     * See if the entry contains a MOBAC layer configuration, and create a layer from it if so.
     *
     * @since 3.1
     */
    private void importPotentialMobacLayer(String zipEntry)
    {
        try (InputStream inputStream = packageReader.getInputStream(zipEntry))
        {
            if (mobacLayerManager.isMapSourceXml(inputStream))
            {
                final String configXml = packageReader.readEntryAsString(zipEntry);
                if (!configXml.isEmpty())
                {
                    mobacLayerManager.makeLayerFromConfigXml(FilenameUtils.getName(zipEntry), configXml);
                }
            }
        } catch (IOException e)
        {
            logger.error("Failed to read {} from {}", zipEntry, packageFile, e);
        }
    }

    /**
     * Import a KML/KMZ document.
     *
     * @since 3.1
     */
    private void importKml(String zipEntry)
    {
        logger.debug("Import KML/KMZ: {}", zipEntry);

        try (InputStream inputStream = packageReader.getInputStream(zipEntry))
        {
            kmlLayerService.loadKml(inputStream, FilenameUtils.getName(zipEntry));
        } catch (IOException e)
        {
            logger.error("Failed to read {} from {}", zipEntry, packageFile, e);
        }
    }

    /**
     * Import a Preferences entry.
     *
     * @param zipEntry Path of zip entry to process
     *
     * @throws IOException If processing fails
     */
    private void importPreferences(String zipEntry) throws IOException
    {
        if (preferences != null)
        {
            logger.warn("Multiple preferences encountered in data package; ignoring {}", zipEntry);
            return;
        }

        final String preferenceXml = packageReader.readEntryAsString(zipEntry);
        preferences = new PreferencesReader().parsePreferences(preferenceXml);

        // Place entries into our preferences
        new TakPreferencesImporter().importPreferences(preferences);
    }

    /**
     * Import a certificate entry.
     *
     * @param zipEntry Path of zip entry to process
     *
     * @throws IOException If processing fails
     */
    private void importCert(String zipEntry) throws IOException
    {
        logger.debug("Read cert from {}", zipEntry);

        final byte[] bytes = packageReader.readEntry(zipEntry);

        if (bytes.length > 0) createCert(zipEntry, bytes);
    }

    /**
     * Create a certificate file from the given content.  If a certificate with the same name already exists,
     * prompt the user about overwriting it.
     *
     * @param zipEntry Entry path, from which to create the certificate name
     * @param bytes certificate content
     *
     * @throws IOException If processing fails
     */
    private void createCert(String zipEntry, byte[] bytes) throws IOException
    {
        final String name = FilenameUtils.getName(zipEntry); // Get the name from the zip entry
        final File certFile = certificateManager.createCert(name, bytes);

        certificateMap.put(name, certFile);
        certsImported.add(name);
    }
}
