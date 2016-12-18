/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.dialog.LogConsole;
import org.apache.cayenne.modeler.dialog.pref.ClasspathPreferences;
import org.apache.cayenne.modeler.undo.CayenneUndoManager;
import org.apache.cayenne.modeler.util.AdapterMapping;
import org.apache.cayenne.modeler.util.WidgetFactory;
import org.apache.cayenne.pref.CayennePreference;
import org.apache.cayenne.pref.CayenneProjectPreferences;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.swing.BindingFactory;
import org.apache.cayenne.util.IDUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A main modeler application class that provides a number of services to the Modeler
 * components. Configuration properties:
 * <ul>
 * <li>cayenne.modeler.application.name - name of the application, 'CayenneModeler' is
 * default. Used to locate preferences domain among other things.</li>
 * <li>cayenne.modeler.pref.version - a version of the preferences DB schema. Default is
 * "1.1".</li>
 * </ul>
 */
public class Application {

    public static final String DEFAULT_MESSAGE_BUNDLE = "org.apache.cayenne.modeler.cayennemodeler-strings";

    public static final String APPLICATION_NAME_PROPERTY = "cayenne.modeler.application.name";
    public static final String DEFAULT_APPLICATION_NAME = "CayenneModeler";

    private static Application instance;

    protected FileClassLoadingService modelerClassLoader;

    protected CayenneModelerController frameController;

    protected String name;

    protected BindingFactory bindingFactory;
    protected AdapterMapping adapterMapping;

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

    // TODO: must be injectable directly in components
    public static WidgetFactory getWidgetFactory() {
        return instance.getInjector().getInstance(WidgetFactory.class);
    }

    // static methods that should probably go away eventually...
    public static CayenneModelerFrame getFrame() {
        return (CayenneModelerFrame) getInstance().getFrameController().getView();
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

    public Application() {
        String configuredName = System.getProperty(APPLICATION_NAME_PROPERTY);
        this.name = (configuredName != null) ? configuredName : DEFAULT_APPLICATION_NAME;
        this.cayennePreference = new CayennePreference();
    }

    public Injector getInjector() {
        return injector;
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

    public ClassLoadingService getClassLoadingService() {
        return modelerClassLoader;
    }

    public AdapterMapping getAdapterMapping() {
        return adapterMapping;
    }

    /**
     * Returns action controller.
     */
    public ActionManager getActionManager() {
        return injector.getInstance(ActionManager.class);
    }

    /**
     * Returns undo-edits controller.
     */
    public CayenneUndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * Returns controller for the main frame.
     */
    public CayenneModelerController getFrameController() {
        return frameController;
    }

    /**
     * Starts the application.
     */
    public void startup() {
        // init subsystems
        initPreferences();
        initClassLoader();

        this.bindingFactory = new BindingFactory();
        this.adapterMapping = new AdapterMapping();

        this.undoManager = new CayenneUndoManager(this);

        this.frameController = new CayenneModelerController(this);

        // open up
        frameController.startupAction();

        // After prefs have been loaded, we can now show the console if needed
        LogConsole.getInstance().showConsoleIfNeeded();
        getFrame().setVisible(true);
    }

    public BindingFactory getBindingFactory() {
        return bindingFactory;
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

        String path = CayennePreference.filePathToPrefereceNodePath(descriptor
                .getConfigurationSource()
                .getURL()
                .getPath());
        Preferences pref = getPreferencesNode(getProject().getClass(), "");
        return pref.node(pref.absolutePath() + path);
    }

    /**
     * Returns a new instance of CodeTemplateManager.
     */
    public CodeTemplateManager getCodeTemplateManager() {
        return new CodeTemplateManager(this);
    }

    /**
     * Reinitializes ModelerClassLoader from preferences.
     */
    @SuppressWarnings("unchecked")
    public void initClassLoader() {
        final FileClassLoadingService classLoader = new FileClassLoadingService();

        // init from preferences...
        Preferences classLoaderPreference = Application.getInstance().getPreferencesNode(
                ClasspathPreferences.class,
                "");

        Collection details = new ArrayList<>();
        String[] keys = null;
        ArrayList<String> values = new ArrayList<>();

        try {
            keys = classLoaderPreference.keys();
            for (String cpKey : keys) {
            	values.add(classLoaderPreference.get(cpKey, ""));
            }
        }
        catch (BackingStoreException e) {
            // do nothing
        }

        for (int i = 0; i < values.size(); i++) {
            details.add(values.get(i));
        }

        if (details.size() > 0) {

            // transform preference to file...
            Transformer transformer = new Transformer() {

                public Object transform(Object object) {
                    String pref = (String) object;
                    return new File(pref);
                }
            };

            classLoader.setPathFiles(CollectionUtils.collect(details, transformer));
        }

        this.modelerClassLoader = classLoader;

        // set as EventDispatch thread default class loader
        if (SwingUtilities.isEventDispatchThread()) {
            Thread.currentThread().setContextClassLoader(classLoader.getClassLoader());
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    Thread.currentThread().setContextClassLoader(
                            classLoader.getClassLoader());
                }
            });
        }
    }

    protected void initPreferences() {
        this.cayenneProjectPreferences = new CayenneProjectPreferences();
    }
}
