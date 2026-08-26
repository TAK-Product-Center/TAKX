TAKX Core
=========
This repo contains the code for the TAKX desktop application, including the development API.

The vast majority of the plugin API is located in the Platform module. Any new class a plugin needs to directly
reference (i.e. compile-time dependency) should go in Platform. Most implementation details should go in Client.

**Readme Contents**

- [Build and Development Instructions](#build-and-development-instructions)
    - [Configuring Repository Credentials](#configuring-repository-credentials)
    - [Building TAKX Core Repo](#building-takx-core-repo)
    - [Configuring Install Location](#configuring-install-location)
    - [Copying Plugins](#copying-plugins)
    - [Running TAKX](#running-takx)
- [Repository Organization](#repository-organization)
    - [Modules](#modules)
    - [Tests](#tests)
- [Development Guidelines](#development-guidelines)
- [Change Log](#change-log)
- [Contact](#contact)

Build and Development Instructions
----------------------------------
### Current Build Status

Build Status: [Pipeline](./pipeline.svg)


### Configuring Repository Credentials

In order to pull down artifacts from the TAK Artifactory repository, you must first add your credentials to the
encrypted gradle credential store.

This project utilizes the [nu.studer.credentials](https://plugins.gradle.org/plugin/nu.studer.credentials) Gradle plugin
to maintain encrypted credentials for use in Gradle builds. A Gradle task called `addCredentials` must be executed to
set your credentials. You must do this for your TAK.gov username/password.

Follow these steps:

1. Open a terminal and navigate to the directory of the cloned repository.
2. Execute the following gradle commands to add your credentials.

   **Important**: some special characters may cause issues. Enclose your password in **double quotes for Windows** or
   **single quotes for Mac/Linux** to avoid this issue.

   Windows:
   ```
   gradlew addCredentials --key takRepoUsername --value "<TAK.gov username>"
   gradlew addCredentials --key takRepoPassword --value "<TAK.gov password>"
   ```

   macOS/Linux:
   ```
   ./gradlew addCredentials --key takRepoUsername --value '<TAK.gov username>'
   ./gradlew addCredentials --key takRepoPassword --value '<TAK.gov password>'
   ```

These steps only need to be executed once on any given development machine, regardless of how many repos are cloned.

See [here]() for troubleshooting
help if you have issues setting your credentials.


### Building TAKX Core Repo

1. Clone this repository
2. Ensure that your repo credentials have been set as described above.
3. Build using the following command from the root of the cloned repo:  
   `gradlew clean build`

### Configuring Install Location

The TAKX repos utilize a gradle build plugin to make configuration of builds more standard. This plugin is called the
BuildSupportPlugin (BSP) and docs are available [here](gradle-plugins/build-support-plugin/)
. One feature of the BSP is the ability to specify an install location for plugin jars to be copied to when the install
task is executed. To take advantage of this feature, you need an environment variable:

1. Define an environment variable called `ORG_GRADLE_PROJECT_pluginInstallDir`.
2. Set the environment variable to the plugins directory in your cloned TAKX core repo:
   "{_pathToRepo_}/deployment/bin/plugins"


### Copying Plugins

Now that you have built TAKX from source, you likely want to download several plugins that are normally included in the
installer to make TAKX more useful. All of these plugins are optional, but some are very useful.
The [Installer Support Gradle Plugin (ISP)](gradle-plugins/installer-support-plugin/)
makes this easy by publishing a bill of materials (BOM) file named <variant>-plugins.bom containing the plugin
dependencies for a particular bundle installer variant. The BOM file is published to Artifactory using the following GAV
parameters: group=`gov.takx.installers-jdk17`, artifact=`<variant>-plugins`, and version=`<projectVersion>`.

1. To copy the plugins from the standard TAKX base installer, execute the following command:  
   `gradlew installStandardPlugins`

2. To copy the plugins from a bundle installer (e.g. Voltron API), execute the following command, where `<variant>`
   is the name used for the installer variant in its installer project and `<version>` is the desired version of the
   installer for which to copy plugins:  
   `gradlew installPlugins -Pbom=<variant>:<version>`

   For example, the TAKX RF installer uses the installer variant "takxrf", so the following command can be used to
   install plugins from the 3.1.0 TAKX RF installer:  
   `gradlew installPlugins -Pbom=takxrf:3.1.0`

   Note: if you are unsure what variant is used for a given bundle installer, you can check the build.gradle file in its
   installer repo for a line like `installer('takxrf')` or you can search for the BOM file on Artifactory in the
   'gov.takx.installers-jdk17' group.

### Running TAKX

Important: Before trying to run TAKX using the `runTakx` Gradle task, make sure you have executed the Gradle `build`
task. You can verify it has already been run correctly by making sure the boot and conf directories are present in
`takx/deployment/bin`.

To launch TAKX from the development environment, execute the following command:  
`gradlew runTakx`

The runTakx task accepts several command line arguments:

```
--instance=N | -i=N  to specify the application instance number
--debug | -d         to configure the JVM for remote debugging (note: does not suspend by default; add -s to suspend.)
--suspend | -s       to debug with suspend enabled, causing the application to suspend unless a debugger is connected
--dcevm              to use DCEVM and hotswap agent (implies -d), env var HOTSWAP_AGENT_JAR must be set
```

To pass a command line argument, you must use `--args=<argument>`. For example, the following command will run TAKX with
remote debugging enabled on port 5005:  
`gradlew runTakx --args=-d`

To include multiple args, enclose them in quotes. For example, the following command will use DCEVM with suspended
debug:  
`gradlew runTakx --args="-s --dcevm"`


Repository Organization
-----------------------

### Modules

The TAKX core repo is made up of multiple modules. They are listed below in general order of relevance for most
developers:

- **Client**: contains the TAKX core. Classes in this module are _not_ part of the API and are not available for plugins
  to use. Any new classes/methods should have unit and/or integration tests added in `src/test`.

- **Platform**: defines the TAKX API for use by plugins. All classes/methods should be well documented with JavaDocs.
  Utilizes annotations like `@NoPluginImplementations`, `@Internal`, and `@Experimental` as appropriate to communicate
  to plugin developers how they may use the class and whether the API is considered stable. Classes in this module are
  typically interfaces, abstract base classes, or utilities classes, though there are cases where implementation classes
  will also be present. Care must be taken not to introduce any unintentional breaking changes in this module. Any new
  classes/methods should have unit and/or integration tests added in `src/test`.

- **TestSupport**: contains classes to support testing of TAKX and TAKX plugins. This module is provided to plugins as
  part of the API, so all classes/methods should be well documented with JavaDocs. Note: the `src/test` area of this
  module should only contains tests for verifying the test support classes in the module function as expected; it should
  not contain any unit/integration/functional tests for the other modules.

- **FunctionalTests**: contains tests for the different functional threads provided for TAKX. These tests should be at a
  higher level than the unit/integration tests included in the Client/Platform modules; they will be focused on ensuring
  specific user requirements are met, and ultimately they will probably provide traceability to those requirements.
  Note: this module is only for tests and does not contain a `src/main` directory. Anything that you would have put in a
  `src/main` directory should likely go into the **TestSupport** module and be included in the test API.

- **Raptor**: contains all of the files that are part of a TAKX distribution, such as the various config files (in
  `bin/conf`) and the JBoss modules (in `bin/modules`). The `bin` directory acts as a staging directory for a TAKX
  installation when running TAKX in the development environment using `gradlew runTakx`.

- **bin-creator**: responsible for building out the `bin` directory with needed libraries, modules, Postgres, etc. Most
  of this work is done in the `build.gradle` file. The `src` directory contains custom JBoss module definition files for
  cases where the default definitions don't include the required dependencies correctly.

- **binary-compatibility**: responsible for interfacing with the Binary Compatibility Gradle Plugin (BCP) to perform an
  analysis comparing a prior release of the API and its current state, reporting any binary incompatible changes. If a
  breaking change is necessary (and agreed upon by the TAKX leads), it will be listed
  in `accepted-public-api-changes.json`. Note: this check is currently disabled for TAKX while the API is in flux.

- **ProcessManager**: defines a stand-alone application that will monitor the main application to detect crashes and
  attempt to stop sub-processes that would otherwise be orphaned after a crash.

- **platform-bom**: publishes a bill of materials (BOM) for consuming projects so they don't have to declare the version
  on any library that TAKX provides at runtime.

- **test-bom**: publishes a bill of materials (BOM) for consuming projects so they don't have to declare the version on
  any test library that TAKX provides at runtime.

- **Bootstrap**: contains the classes necessary to allow JBoss to bootstrap the TAKX application.

### Tests

Tests focused on verifying the behavior of a specific class (i.e. unit tests) should go in `src/test/java` of the same
module as that class and have the same package. For example, `MapObjectManagerTest` is used to verify
`gov.takx.core.mapobject.MapObjectManager`, which is in the **Client** module, so it belongs in
`Client/src/test/java/gov/takx/core/mapobject/MapObjectManagerTest`. Note: we're using the term "unit test" a little
loosely here, since `MapObjectManagerTest` is actually an integration test that extends `AIntegrationTest`. We will
consider anything that is focused on the verification of one class as a unit test, even if it doesn't mock/stub out
everything outside of that class (especially since it is often easier to test the class in the context of the real
application).

Tests focused on verifying a specific user capability or requirement rather than a particular class are known as
functional tests and they belong in the **FunctionalTests** module. For example, `ShapeHolesTest` is used to verify the
different requirements on a shape with holes. For organizing these tests, keep the package names as simple as possible.
For example, the `ShapeHolesTest` is in `gov.takx.shapes`, and any other functional tests related to shapes should be
added to the same package.


Development Guidelines
----------------------
Before you commit code to any TAKX projects (including this repo), you **must** configure your IDE to use the TAKX code
style by following the instructions in the [Coding Style Guide](Coding+Style+Guide).

Additionally, you should familiarize yourself with the following pages and ensure you follow the guidelines identified.

- [Development Process](Development+Process)
- [Commit Guidelines](Commit+Guidelines)
- [Coding Best Practices](Coding+Best+Practices)

Change Log
----------
See [TAKX Release Notes](TAKX+Release+Notes).


Contact
-------
takx@tak.gov
