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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.apache.cayenne.configuration.CayenneServerModule;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.modeler.dialog.LogConsole;
import org.apache.cayenne.modeler.dialog.pref.ClasspathPreferences;
import org.apache.cayenne.modeler.undo.CayenneUndoManager;
import org.apache.cayenne.modeler.util.AdapterMapping;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.CayenneDialog;
import org.apache.cayenne.modeler.util.CayenneUserDir;
import org.apache.cayenne.pref.CayennePreference;
import org.apache.cayenne.pref.CayenneProjectPreferences;
import org.apache.cayenne.pref.Preference;
import org.apache.cayenne.project2.CayenneProjectModule;
import org.apache.cayenne.project2.Project;
import org.apache.cayenne.swing.BindingFactory;
import org.apache.cayenne.util.IDUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.scopemvc.controller.basic.ViewContext;
import org.scopemvc.controller.swing.SwingContext;
import org.scopemvc.core.View;
import org.scopemvc.util.UIStrings;
import org.scopemvc.view.swing.SwingView;

/**
 * A main modeler application class that provides a number of services to the Modeler
 * components. Configuration properties:
 * <ul>
 * <li>cayenne.modeler.application.name - name of the application, 'CayenneModeler' is
 * default. Used to locate preferences domain among other things.</li>
 * <li>cayenne.modeler.pref.version - a version of the preferences DB schema. Default is
 * "1.1".</li>
 * </ul>
 * 
 */
public class Application {

    public static final String PREFERENCES_VERSION = "1.2";
    public static final String PREFERENCES_DB_SUBDIRECTORY = "prefs";
    public static final String PREFERENCES_MAP_PACKAGE = "pref";
    public static final String APPLICATION_NAME_PROPERTY = "cayenne.modeler.application.name";
    public static final String PREFERENCES_VERSION_PROPERTY = "cayenne.modeler.pref.version";

    public static final String DEFAULT_APPLICATION_NAME = "CayenneModeler";

    // TODO: implement cleaner IoC approach to avoid using this singleton...
    protected static Application instance;

    protected FileClassLoadingService modelerClassLoader;
    protected ActionManager actionManager;
    protected CayenneModelerController frameController;

    protected File initialProject;
    protected String name;
    protected String preferencesDB;

    protected BindingFactory bindingFactory;
    protected AdapterMapping adapterMapping;

    protected CayenneUndoManager undoManager;

    protected CayenneProjectPreferences cayenneProjectPreferences;

    // This is for OS X support
    private boolean isQuittingApplication = false;

    protected CayennePreference cayennePreference;

    protected Injector injector;

    private static String id;

    public static Application getInstance() {
        return instance;
    }

    public static String getId() {
        if (id == null) {
            byte[] byteId = IDUtil.pseudoUniqueByteSequence(16);
            id = "new_project_" + byteId.toString();
        }
        return id;
    }

    // static methods that should probably go away eventually...
    public static CayenneModelerFrame getFrame() {
        return (CayenneModelerFrame) getInstance().getFrameController().getView();
    }

    public Injector getInjector() {
        return injector;
    }

    public static Project getProject() {
        return getInstance().getFrameController().getProjectController().getProject();
    }

    public Application(File initialProject) {
        this.initialProject = initialProject;

        Module module = new CayenneProjectModule();
        Module moduleSer = new CayenneServerModule("CayenneModeler");

        injector = DIBootstrap.createInjector(module, moduleSer);

        // configure startup settings
        String configuredName = System.getProperty(APPLICATION_NAME_PROPERTY);
        this.name = (configuredName != null) ? configuredName : DEFAULT_APPLICATION_NAME;

        String subdir = System.getProperty(PREFERENCES_VERSION_PROPERTY);

        if (subdir == null) {
            subdir = PREFERENCES_VERSION;
        }

        File dbDir = new File(CayenneUserDir.getInstance().resolveFile(
                PREFERENCES_DB_SUBDIRECTORY), subdir);
        dbDir.mkdirs();
        this.cayennePreference = new CayennePreference();

        this.preferencesDB = new File(dbDir, "db").getAbsolutePath();
    }

    public Preferences getPreferencesNode(Class className, String path) {
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
     * Returns an action for key.
     */
    public CayenneAction getAction(String key) {
        return getActionManager().getAction(key);
    }

    /**
     * Returns action controller.
     */
    public ActionManager getActionManager() {
        return actionManager;
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

        // ...Scope

        // TODO: this will go away if switch away from Scope
        // force Scope to use CayenneModeler properties
        UIStrings.setPropertiesName(ModelerConstants.DEFAULT_MESSAGE_BUNDLE);
        ViewContext.clearThreadContext();

        // init actions before the frame, as it will attempt to build menus out of
        // actions.
        this.actionManager = new ActionManager(this);
        this.undoManager = new org.apache.cayenne.modeler.undo.CayenneUndoManager(this);

        // ...create main frame
        this.frameController = new CayenneModelerController(this, initialProject);

        // update Scope to work nicely with main frame
        ViewContext.setGlobalContext(new ModelerContext(frameController.getFrame()));

        // open up
        frameController.startupAction();

        /**
         * After prefs have been loaded, we can now show the console if needed
         */
        LogConsole.getInstance().showConsoleIfNeeded();
        getFrame().setVisible(true);
    }

    public BindingFactory getBindingFactory() {
        return bindingFactory;
    }

    public CayenneProjectPreferences getCayenneProjectPreferences() {
        return cayenneProjectPreferences;
    }

    public static Preferences getMainPreferenceForProject() {

        DataChannelDescriptor descriptor = (DataChannelDescriptor) getProject()
                .getRootNode();

        // if new project
        if (descriptor.getConfigurationSource() == null) {
            return Application.getInstance().getPreferencesNode(
                    getProject().getClass(),
                    getId());
        }

        String path = CayennePreference.filePathToPrefereceNodePath(descriptor
                .getConfigurationSource()
                .getURL()
                .getPath());
        Preferences pref = Application.getInstance().getPreferencesNode(
                getProject().getClass(),
                "");
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

        Collection details = new ArrayList<String>();
        String[] keys = null;

        try {
            keys = classLoaderPreference.keys();
        }
        catch (BackingStoreException e) {
            // do nothing
        }

        for (int i = 0; i < keys.length; i++) {
            details.add(keys[i]);
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

    final class ModelerContext extends SwingContext {

        JFrame frame;

        public ModelerContext(JFrame frame) {
            this.frame = frame;
        }

        @Override
        protected void showViewInPrimaryWindow(SwingView view) {
        }

        /**
         * Creates closeable dialogs.
         */
        @Override
        protected void showViewInDialog(SwingView inView) {
            // NOTE:
            // copied from superclass, except that JDialog is substituted for
            // CayenneDialog
            // Keep in mind when upgrading Scope to the newer versions.

            // Make a JDialog to contain the view.
            Window parentWindow = getDefaultParentWindow();

            final CayenneDialog dialog;
            if (parentWindow instanceof Dialog) {
                dialog = new CayenneDialog((Dialog) parentWindow);
            }
            else {
                dialog = new CayenneDialog((Frame) parentWindow);
            }

            // Set title, modality, resizability
            if (inView.getTitle() != null) {
                dialog.setTitle(inView.getTitle());
            }
            if (inView.getDisplayMode() == SwingView.MODAL_DIALOG) {
                dialog.setModal(true);
            }
            else {
                dialog.setModal(false);
            }
            dialog.setResizable(inView.isResizable());

            setupWindow(dialog.getRootPane(), inView, true);
            dialog.toFront();
        }

        /**
         * Overrides super implementation to allow using Scope together with normal Swing
         * code that CayenneModeler already has.
         */
        @Override
        public JRootPane findRootPaneFor(View view) {
            JRootPane pane = super.findRootPaneFor(view);

            if (pane != null) {
                return pane;
            }

            if (((SwingView) view).getDisplayMode() != SwingView.PRIMARY_WINDOW) {
                return pane;
            }

            return frame.getRootPane();
        }

        @Override
        protected Window getDefaultParentWindow() {
            return frame;
        }
    }

    public boolean isQuittingApplication() {
        return isQuittingApplication;
    }

    public void setQuittingApplication(boolean isQuittingApplication) {
        this.isQuittingApplication = isQuittingApplication;
    }
}
