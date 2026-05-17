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
import org.apache.cayenne.modeler.pref.ClasspathPrefs;
import org.apache.cayenne.modeler.pref.DBConnectorPrefs;
import org.apache.cayenne.modeler.pref.GeneralPrefs;
import org.apache.cayenne.modeler.pref.PreferencesRepository;
import org.apache.cayenne.modeler.pref.RecentProjectsPrefs;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.service.classloader.ModelerClassLoader;
import org.apache.cayenne.modeler.platform.UIInitializer;
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

    public static void launch(String[] args, UIInitializer platformInitializer) {

        LOGGER.info("Starting CayenneModeler.");
        LOGGER.info("JRE v.{} at {}", System.getProperty("java.version"), System.getProperty("java.home"));

        // TODO: this is dirty... CoreModule is out of place inside the Modeler...
        // If we need CayenneRuntime for certain operations, those should start their own stack...
        Injector injector = DIBootstrap.createInjector(
                new CoreModule(),
                new ProjectModule(),
                new DbSyncModule(),
                new ModelerModule());

        SwingUtilities.invokeLater(() -> new Application(injector, platformInitializer).launch(initialProjectFromArgs(args)));
    }

    private static File initialProjectFromArgs(String[] args) {
        if (args != null && args.length == 1) {
            File f = new File(args[0]);

            if (f.isFile()
                    && f.getName().startsWith("cayenne")
                    && f.getName().endsWith(".xml")) {
                return f;
            }
        }

        return null;
    }

    private final Injector injector;
    private final UIInitializer platformInitializer;
    private final ModelerClassLoader classLoader;
    private final PreferencesRepository preferencesRepository;
    private final ProjectValidator projectValidator;
    private GlobalActions actionManager;
    private LogConsole logConsole;
    private MainFrame frame;
    private CayenneUndoManager undoManager;
    private DBConnectors dbConnectors;

    public Application(Injector injector, UIInitializer platformInitializer) {
        this.injector = injector;
        this.platformInitializer = platformInitializer;

        this.classLoader = new ModelerClassLoader();
        this.preferencesRepository = new PreferencesRepository(injector.getInstance(ConfigurationNameMapper.class));
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

    public UIInitializer getPlatformInitializer() {
        return platformInitializer;
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

    public void launch(File initialProject) {
        this.platformInitializer.initLookAndFeel();
        this.actionManager = new GlobalActions(
                this,
                injector.getInstance(ConfigurationNameMapper.class),
                injector.getInstance(ConfigurationNodeParentGetter.class));

        this.logConsole = new LogConsole(this);
        ModelerLogFactory.setAppender(logConsole);

        getPreferencesRepository().runMigrations();

        this.dbConnectors = new DBConnectorPrefs(getPreferencesRepository()).getConnectors();

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

        // open up
        frame.onStartup();

        // After prefs have been loaded, we can now show the console if needed
        logConsole.showConsoleIfNeeded();
        getFrame().setVisible(true);

        if (initialProject == null) {
            initialProject = initialProjectFromPreferences();
        }

        if (initialProject != null) {
            getActionManager().getAction(OpenProjectAction.class).openProject(initialProject);
        }
    }

    public DBConnectors getDbConnectors() {
        return dbConnectors;
    }

    public PreferencesRepository getPreferencesRepository() {
        return preferencesRepository;
    }

    /**
     * Reinitializes ModelerClassLoader from preferences.
     */
    public void refreshClassLoader() {
        List<String> values = new ClasspathPrefs(getPreferencesRepository()).getEntries();
        if (!values.isEmpty()) {
            getClassLoader().setFiles(values.stream().map(File::new).collect(Collectors.toList()));
        }
    }

    private File initialProjectFromPreferences() {

        if (new GeneralPrefs(getPreferencesRepository()).isAutoLoadProject()) {
            List<File> files = new RecentProjectsPrefs(getPreferencesRepository()).getFiles();
            if (!files.isEmpty()) {
                return files.get(0);
            }
        }

        return null;
    }
}
