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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.runtime.DbAdapterFactory;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.modeler.pref.CayennePreference;
import org.apache.cayenne.modeler.pref.CayenneProjectPreferences;
import org.apache.cayenne.modeler.pref.LastProjectsPreferences;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.service.classloader.ModelerClassLoader;
import org.apache.cayenne.modeler.service.platform.PlatformInitializer;
import org.apache.cayenne.modeler.ui.ModelerController;
import org.apache.cayenne.modeler.ui.action.OpenProjectAction;
import org.apache.cayenne.modeler.ui.logconsole.LogConsoleController;
import org.apache.cayenne.modeler.ui.preferences.classpath.ClasspathPreferencesController;
import org.apache.cayenne.modeler.ui.preferences.general.GeneralPreferencesController;
import org.apache.cayenne.modeler.undo.CayenneUndoManager;
import org.apache.cayenne.project.ConfigurationNodeParentGetter;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.upgrade.UpgradeService;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.util.IDUtil;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * A main modeler application class that provides a number of services to the Modeler
 * components. Configuration properties:
 * <ul>
 * <li>cayenne.modeler.application.name - name of the application, 'CayenneModeler' is
 * default. Used to locate preferences domain among other things.</li>
 * </ul>
 */
public class Application {

    public static final String DEFAULT_MESSAGE_BUNDLE = "org.apache.cayenne.modeler.cayennemodeler-strings";

    public static final String APPLICATION_NAME_PROPERTY = "cayenne.modeler.application.name";
    public static final String DEFAULT_APPLICATION_NAME = "CayenneModeler";

    private static Application instance;

    protected LogConsoleController logConsoleController;
    protected ModelerController frameController;
    protected String name;
    protected CayenneUndoManager undoManager;
    protected CayenneProjectPreferences cayenneProjectPreferences;
    protected CayennePreference cayennePreference;

    @Inject
    protected Injector injector;

    private String newProjectTemporaryName;

    public static Application getInstance() {
        return instance;
    }

    public static void setInstance(Application instance) {
        Application.instance = instance;
    }

    public Application() {
        String configuredName = System.getProperty(APPLICATION_NAME_PROPERTY);
        this.name = (configuredName != null) ? configuredName : DEFAULT_APPLICATION_NAME;
        this.cayennePreference = new CayennePreference();
    }

    public String getNewProjectTemporaryName() {

        // TODO: andrus 4/4/2010 - should that be reset every time a new project is opened
        if (newProjectTemporaryName == null) {
            StringBuffer buffer = new StringBuffer("new_project_");
            for (byte aKey : IDUtil.pseudoUniqueByteSequence(16)) {
                IDUtil.appendFormattedByte(buffer, aKey);
            }
            newProjectTemporaryName = buffer.toString();
        }

        return newProjectTemporaryName;
    }

    public Project getProject() {
        return getFrameController().getProjectController().getProject();
    }

    public Preferences getPreferencesNode(Class<?> className, String path) {
        return cayennePreference.getNode(className, path);
    }

    public String getName() {
        return name;
    }

    public ModelerClassLoader getClassLoader() {
        return injector.getInstance(ModelerClassLoader.class);
    }

    public GlobalActions getActionManager() {
        return injector.getInstance(GlobalActions.class);
    }

    public ProjectValidator getProjectValidator() {
        return injector.getInstance(ProjectValidator.class);
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

    public PlatformInitializer getPlatformInitializer() {
        return injector.getInstance(PlatformInitializer.class);
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

    public ModelerController getFrameController() {
        return frameController;
    }

    public LogConsoleController getLogConsoleController() {
        return logConsoleController;
    }

    public void startup(File initialProject) {
        this.logConsoleController = new LogConsoleController(this);

        // TODO: should "project" preferences reside in ProjectController?
        this.cayenneProjectPreferences = new CayenneProjectPreferences();

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
        this.frameController = new ModelerController(this);

        // open up
        frameController.onStartup();

        // After prefs have been loaded, we can now show the console if needed
        logConsoleController.showConsoleIfNeeded();
        getFrameController().getView().setVisible(true);

        if (initialProject == null) {
            initialProject = initialProjectFromPreferences();
        }

        if (initialProject != null) {
            getActionManager().getAction(OpenProjectAction.class).openProject(initialProject);
        }
    }

    public CayenneProjectPreferences getCayenneProjectPreferences() {
        return cayenneProjectPreferences;
    }

    public Preferences getMainPreferenceForProject() {

        DataChannelDescriptor descriptor = (DataChannelDescriptor) getFrameController()
                .getProjectController()
                .getProject()
                .getRootNode();

        // if new project
        if (descriptor.getConfigurationSource() == null) {
            return getPreferencesNode(
                    getProject().getClass(),
                    getNewProjectTemporaryName());
        }

        String path = descriptor
                .getConfigurationSource()
                .getURL()
                .getPath()
                .replace(".xml", "");

        Preferences pref = getPreferencesNode(getProject().getClass(), "");
        return pref.node(pref.absolutePath() + path);
    }

    /**
     * Reinitializes ModelerClassLoader from preferences.
     */
    public void refreshClassLoader() {

        // init from preferences...
        Preferences classLoaderPref = getPreferencesNode(ClasspathPreferencesController.class, "");

        String[] keys;
        List<String> values = new ArrayList<>();

        try {
            keys = classLoaderPref.keys();
            for (String cpKey : keys) {
                values.add(classLoaderPref.get(cpKey, ""));
            }
        } catch (BackingStoreException ignored) {
        }

        if (!values.isEmpty()) {
            getClassLoader().setFiles(values.stream().map(File::new).collect(Collectors.toList()));
        }
    }

    private File initialProjectFromPreferences() {

        Preferences autoLoadLastProject = getPreferencesNode(GeneralPreferencesController.class, "");
        if ((autoLoadLastProject != null) && autoLoadLastProject.getBoolean(GeneralPreferencesController.AUTO_LOAD_PROJECT_PREFERENCE, false)) {
            List<File> files = LastProjectsPreferences.getFiles();
            if (!files.isEmpty()) {
                return files.get(0);
            }
        }

        return null;
    }
}
