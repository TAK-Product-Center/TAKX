/**
 * Classes for defining import and export plugins.
 * <p>
 * Each import or export plugin relies on a graphical wizard that the user walks through to constrain and/or define
 * which data to import or export. Import and export plugins are typically used in a post-mission environment, e.g. a
 * bird collects the data for two hours, lands, and massive amounts of raw data that couldn't be sent over a narrow pipe
 * are imported into TAKX. These plugins are almost always file-based, i.e. they work with one or more externally
 * (arbitrarily) defined file formats, though this isn't a hard requirement.
 * <p>
 * When defining an import/export plugin, developers should create an import/export plugin class that extends
 * {@link gov.takx.api.plugin.io.AImportExportPlugin} and annotate the class with
 * {@link gov.takx.api.plugin.annotations.ImportExportPlugin}. One of the abstract base classes for defining an
 * import/export wizard (such as {@link gov.takx.api.plugin.io.ACancellableImportWizard} is then extended to provide
 * the necessary import/export functionality. The import/export plugin class will be responsible for instantiating the
 * necessary wizards via {@link gov.takx.api.plugin.io.IImportExportPlugin#createImporter() createImporter()} or
 * {@link gov.takx.api.plugin.io.IImportExportPlugin#createExporter() createExporter()}.
 *
 * @since 2.0
 */
package gov.takx.api.plugin.io;
