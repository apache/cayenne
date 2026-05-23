/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.configuration.runtime.DbAdapterFactory;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.modeler.dbconnector.DBConnectors;
import org.apache.cayenne.modeler.log.ModelerLogFactory;
import org.apache.cayenne.modeler.platform.UIInitializer;
import org.apache.cayenne.modeler.toolkit.filechooser.FileChooserFactory;
import org.apache.cayenne.modeler.pref.adapters.ClasspathPrefs;
import org.apache.cayenne.modeler.pref.adapters.DBConnectorPrefs;
import org.apache.cayenne.modeler.pref.adapters.GeneralPrefs;
import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.apache.cayenne.modeler.pref.PrefsManager;
import org.apache.cayenne.modeler.pref.adapters.RecentProjectsPrefs;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.service.classloader.ModelerClassLoader;
import org.apache.cayenne.modeler.service.validator.ConfigurableProjectValidator;
import org.apache.cayenne.modeler.ui.MainFrame;
import org.apache.cayenne.modeler.ui.action.OpenProjectAction;
import org.apache.cayenne.modeler.ui.logconsole.LogConsole;
import org.apache.cayenne.modeler.undo.CayenneUndoManager;
import org.apache.cayenne.project.ConfigurationNodeParentGetter;
import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.ProjectModule;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.upgrade.UpgradeService;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A main modeler application class that manages the main app frame and everything below it.
 */
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void launch(String[] args, UIInitializer platformInit) {

        platformInit.beforeSwingLaunch();

        CliArgs cli = CliArgs.parse(args);

        LOGGER.info("Starting CayenneModeler.");
        LOGGER.info("JRE v.{} at {}", System.getProperty("java.version"), System.getProperty("java.home"));

        // TODO: this is dirty... CoreModule is out of place inside the Modeler...
        // If we need CayenneRuntime for certain operations, those should start their own stack...
        Injector injector = DIBootstrap.createInjector(
                new CoreModule(),
                new ProjectModule(),
                new DbSyncModule(),
                new ModelerModule());

        SwingUtilities.invokeLater(() ->
                new Application(injector, platformInit, cli).launch(cli.initialProject()));
    }

    private final Injector injector;
    private final UIInitializer platformInit;
    private final FileChooserFactory fileChooserFactory;
    private final ModelerClassLoader classLoader;
    private final PrefsLocator prefsLocator;
    private final PrefsManager prefsManager;
    private final ProjectValidator projectValidator;
    private final CliArgs cli;
    private GlobalActions actionManager;
    private LogConsole logConsole;
    private MainFrame frame;
    private CayenneUndoManager undoManager;
    private DBConnectors dbConnectors;

    public Application(Injector injector, UIInitializer platformInit, CliArgs cli) {
        this.injector = injector;
        this.platformInit = platformInit;
        this.fileChooserFactory = platformInit.fileChooserFactory();
        this.cli = cli;

        this.classLoader = new ModelerClassLoader();
        this.prefsLocator = new PrefsLocator();
        this.prefsManager = new PrefsManager(injector.getInstance(ConfigurationNameMapper.class), prefsLocator);
        this.projectValidator = new ConfigurableProjectValidator(this);
    }


    public ModelerClassLoader getClassLoader() {
        return classLoader;
    }

    public GlobalActions getActionManager() {
        return actionManager;
    }

    public ProjectValidator getProjectValidator() {
        return projectValidator;
    }

    public ProjectSaver getProjectSaver() {
        return injector.getInstance(ProjectSaver.class);
    }

    public ProjectLoader getProjectLoader() {
        return injector.getInstance(ProjectLoader.class);
    }

    public DataMapLoader getDataMapLoader() {
        return injector.getInstance(DataMapLoader.class);
    }

    public UpgradeService getUpgradeService() {
        return injector.getInstance(UpgradeService.class);
    }

    public FileChooserFactory getFileChooserFactory() {
        return fileChooserFactory;
    }

    public ConfigurationNodeParentGetter getConfigurationNodeParentGetter() {
        return injector.getInstance(ConfigurationNodeParentGetter.class);
    }

    public DbAdapterFactory getDbAdapterFactory() {
        return injector.getInstance(DbAdapterFactory.class);
    }

    public DataChannelMetaData getMetaData() {
        return injector.getInstance(DataChannelMetaData.class);
    }

    public MergerTokenFactoryProvider getMergerTokenFactoryProvider() {
        return injector.getInstance(MergerTokenFactoryProvider.class);
    }

    public CayenneUndoManager getUndoManager() {
        return undoManager;
    }

    public MainFrame getFrame() {
        return frame;
    }

    public LogConsole getLogConsole() {
        return logConsole;
    }

    public CliArgs getCli() {
        return cli;
    }

    public void launch(File initialProject) {

        this.actionManager = new GlobalActions(
                this,
                injector.getInstance(ConfigurationNameMapper.class),
                injector.getInstance(ConfigurationNodeParentGetter.class));

        this.logConsole = new LogConsole(this);
        ModelerLogFactory.setAppender(logConsole);

        getPrefsManager().runMigrations();

        this.dbConnectors = new DBConnectorPrefs(prefsLocator.appNode(DBConnectorPrefs.NODE)).getConnectors();

        refreshClassLoader();

        // TODO: is this used by DB Import and CGen? If so, the corresponding actions must set it in a proper scope.
        //  Or a better idea - get rid of thread-bound classloaders everywhere
        ModelerClassLoader classLoader = getClassLoader();
        if (SwingUtilities.isEventDispatchThread()) {
            Thread.currentThread().setContextClassLoader(classLoader.getClassLoader());
        } else {
            SwingUtilities.invokeLater(() -> Thread.currentThread().setContextClassLoader(classLoader.getClassLoader()));
        }

        this.undoManager = new CayenneUndoManager(this);
        this.frame = new MainFrame(this);
        this.platformInit.afterFrameCreated(this);
        
        // open up
        frame.onStartup();

        // After prefs have been loaded, we can now show the console if needed
        logConsole.showConsoleIfNeeded();
        getFrame().setVisible(true);

        if (initialProject == null) {
            initialProject = initialProjectFromPreferences();
        }

        if (initialProject != null) {
            getActionManager().getAction(OpenProjectAction.class)
                    .openProject(initialProject, cli.mcpHandshakeNonce());
        }
    }

    public DBConnectors getDbConnectors() {
        return dbConnectors;
    }

    public PrefsManager getPrefsManager() {
        return prefsManager;
    }

    public PrefsLocator getPrefsLocator() {
        return prefsLocator;
    }

    /**
     * Reinitializes ModelerClassLoader from preferences.
     */
    public void refreshClassLoader() {
        List<String> values = new ClasspathPrefs(prefsLocator.appNode(ClasspathPrefs.NODE)).getEntries();
        if (!values.isEmpty()) {
            getClassLoader().setFiles(values.stream().map(File::new).collect(Collectors.toList()));
        }
    }

    private File initialProjectFromPreferences() {

        if (new GeneralPrefs(prefsLocator.appNode(GeneralPrefs.NODE)).isAutoLoadProject()) {
            List<File> files = new RecentProjectsPrefs(prefsLocator.appNode(RecentProjectsPrefs.NODE)).getFiles();
            if (!files.isEmpty()) {
                return files.getFirst();
            }
        }

        return null;
    }
}
