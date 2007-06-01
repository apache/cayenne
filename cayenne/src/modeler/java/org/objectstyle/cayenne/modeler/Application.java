/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.util.Collection;

import javax.swing.ActionMap;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.objectstyle.cayenne.modeler.action.AboutAction;
import org.objectstyle.cayenne.modeler.action.ConfigurePreferencesAction;
import org.objectstyle.cayenne.modeler.action.CreateAttributeAction;
import org.objectstyle.cayenne.modeler.action.CreateDataMapAction;
import org.objectstyle.cayenne.modeler.action.CreateDbEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateDerivedDbEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateDomainAction;
import org.objectstyle.cayenne.modeler.action.CreateNodeAction;
import org.objectstyle.cayenne.modeler.action.CreateObjEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateProcedureAction;
import org.objectstyle.cayenne.modeler.action.CreateQueryAction;
import org.objectstyle.cayenne.modeler.action.CreateRelationshipAction;
import org.objectstyle.cayenne.modeler.action.DerivedEntitySyncAction;
import org.objectstyle.cayenne.modeler.action.ExitAction;
import org.objectstyle.cayenne.modeler.action.GenerateClassesAction;
import org.objectstyle.cayenne.modeler.action.GenerateDBAction;
import org.objectstyle.cayenne.modeler.action.ImportDataMapAction;
import org.objectstyle.cayenne.modeler.action.ImportDBAction;
import org.objectstyle.cayenne.modeler.action.ImportEOModelAction;
import org.objectstyle.cayenne.modeler.action.NewProjectAction;
import org.objectstyle.cayenne.modeler.action.ObjEntitySyncAction;
import org.objectstyle.cayenne.modeler.action.OpenProjectAction;
import org.objectstyle.cayenne.modeler.action.ProjectAction;
import org.objectstyle.cayenne.modeler.action.RemoveAction;
import org.objectstyle.cayenne.modeler.action.RevertAction;
import org.objectstyle.cayenne.modeler.action.SaveAction;
import org.objectstyle.cayenne.modeler.action.SaveAsAction;
import org.objectstyle.cayenne.modeler.action.ValidateAction;
import org.objectstyle.cayenne.modeler.util.CayenneAction;
import org.objectstyle.cayenne.modeler.util.CayenneDialog;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.DomainPreference;
import org.objectstyle.cayenne.pref.HSQLEmbeddedPreferenceEditor;
import org.objectstyle.cayenne.pref.HSQLEmbeddedPreferenceService;
import org.objectstyle.cayenne.pref.PreferenceService;
import org.objectstyle.cayenne.project.CayenneUserDir;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.swing.BindingFactory;
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
 * default. Used to locate prerferences domain among other things.</li>
 * <li>cayenne.modeler.pref.version - a version of the preferences DB schema. Default is
 * "1.1".</li>
 * </ul>
 * 
 * @author Andrei Adamchik
 */
public class Application {

    public static final String PREFERENCES_VERSION = "1.1";
    public static final String APPLICATION_NAME_PROPERTY = "cayenne.modeler.application.name";
    public static final String PREFERENCES_VERSION_PROPERTY = "cayenne.modeler.pref.version";

    public static final String DEFAULT_APPLICATION_NAME = "CayenneModeler";

    // TODO: implement cleaner IoC approach to avoid using this singleton...
    protected static Application instance;

    protected FileClassLoadingService modelerClassLoader;
    protected HSQLEmbeddedPreferenceService preferenceService;
    protected CayenneModelerController frameController;
    protected ActionMap actionMap;
    protected File initialProject;
    protected String name;
    protected String preferencesDB;
    protected BindingFactory bindingFactory;
    protected AdapterMapping adapterMapping;

    public static Application getInstance() {
        return instance;
    }

    // static methods that should probabaly go away eventually...

    public static CayenneModelerFrame getFrame() {
        return (CayenneModelerFrame) getInstance().getFrameController().getView();
    }

    public static Project getProject() {
        return getInstance().getFrameController().getProjectController().getProject();
    }

    public Application(File initialProject) {
        this.initialProject = initialProject;

        // configure startup settings
        String configuredName = System.getProperty(APPLICATION_NAME_PROPERTY);
        this.name = (configuredName != null) ? configuredName : DEFAULT_APPLICATION_NAME;

        String subdir = System.getProperty(PREFERENCES_VERSION_PROPERTY);

        if (subdir == null) {
            subdir = PREFERENCES_VERSION;
        }

        File dbDir = new File(CayenneUserDir.getInstance().resolveFile("prefs"), subdir);
        dbDir.mkdirs();
        this.preferencesDB = new File(dbDir, "db").getAbsolutePath();

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
        return (CayenneAction) actionMap.get(key);
    }

    /**
     * Returns Application ActionMap.
     */
    public ActionMap getActionMap() {
        return actionMap;
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
        initActions();
        this.bindingFactory = new BindingFactory();
        this.adapterMapping = new AdapterMapping();

        // ...Scope

        // TODO: this will go away if switch away from Scope
        // force Scope to use CayenneModeler properties
        UIStrings.setPropertiesName(ModelerConstants.DEFAULT_MESSAGE_BUNDLE);
        ViewContext.clearThreadContext();

        // ...create main frame
        this.frameController = new CayenneModelerController(this, initialProject);

        // update Scope to work nicely with main frame
        ViewContext.setGlobalContext(new ModelerContext(frameController.getFrame()));

        // open up
        frameController.startupAction();
    }

    public BindingFactory getBindingFactory() {
        return bindingFactory;
    }

    /**
     * Returns Application preferences service.
     */
    public PreferenceService getPreferenceService() {
        return preferenceService;
    }

    /**
     * Returns top preferences Domain for the application.
     */
    public Domain getPreferenceDomain() {
        return getPreferenceService().getDomain(getName(), true);
    }

    /**
     * Reinitializes ModelerClassLoader from preferences.
     */
    public void initClassLoader() {
        FileClassLoadingService classLoader = new FileClassLoadingService();

        // init from preferences...
        Domain classLoaderDomain = getPreferenceDomain().getSubdomain(
                FileClassLoadingService.class);

        Collection details = classLoaderDomain.getPreferences();
        if (details.size() > 0) {

            // transform preference to file...
            Transformer transformer = new Transformer() {

                public Object transform(Object object) {
                    DomainPreference pref = (DomainPreference) object;
                    return new File(pref.getKey());
                }
            };

            classLoader.setPathFiles(CollectionUtils.collect(details, transformer));
        }

        this.modelerClassLoader = classLoader;
    }

    protected void initPreferences() {
        HSQLEmbeddedPreferenceService service = new HSQLEmbeddedPreferenceService(
                preferencesDB,
                "pref",
                getName());
        service.stopOnShutdown();
        this.preferenceService = service;
        this.preferenceService.startService();

        // test service
        getPreferenceDomain();
    }

    protected void initActions() {
        // build action map
        actionMap = new ActionMap();

        registerAction(new ProjectAction(this));
        registerAction(new NewProjectAction(this)).setAlwaysOn(true);
        registerAction(new OpenProjectAction(this)).setAlwaysOn(true);
        registerAction(new ImportDataMapAction(this));
        registerAction(new SaveAction(this));
        registerAction(new SaveAsAction(this));
        registerAction(new RevertAction(this));
        registerAction(new ValidateAction(this));
        registerAction(new RemoveAction(this));
        registerAction(new CreateDomainAction(this));
        registerAction(new CreateNodeAction(this));
        registerAction(new CreateDataMapAction(this));
        registerAction(new GenerateClassesAction(this));
        registerAction(new CreateObjEntityAction(this));
        registerAction(new CreateDbEntityAction(this));
        registerAction(new CreateDerivedDbEntityAction(this));
        registerAction(new CreateProcedureAction(this));
        registerAction(new CreateQueryAction(this));
        registerAction(new CreateAttributeAction(this));
        registerAction(new CreateRelationshipAction(this));
        registerAction(new ObjEntitySyncAction(this));
        registerAction(new DerivedEntitySyncAction(this));
        registerAction(new ImportDBAction(this));
        registerAction(new ImportEOModelAction(this));
        registerAction(new GenerateDBAction(this));
        registerAction(new AboutAction(this)).setAlwaysOn(true);
        registerAction(new ConfigurePreferencesAction(this)).setAlwaysOn(true);
        registerAction(new ExitAction(this)).setAlwaysOn(true);
    }

    private CayenneAction registerAction(CayenneAction action) {
        actionMap.put(action.getKey(), action);
        return action;
    }

    static final class PreferencesDelegate implements
            HSQLEmbeddedPreferenceEditor.Delegate {

        static final String message = "Preferences Database is locked by another application. "
                + "Do you want to remove the lock?";
        static final String failureMessage = "Failed to remove database lock. "
                + "Preferences will we saved for this session only.";

        static final HSQLEmbeddedPreferenceEditor.Delegate sharedInstance = new PreferencesDelegate();

        public boolean deleteMasterLock(File lock) {
            int result = JOptionPane.showConfirmDialog(null, message);
            if (result == JOptionPane.YES_OPTION || result == JOptionPane.OK_OPTION) {
                if (!lock.delete()) {
                    JOptionPane.showMessageDialog(null, failureMessage);
                    return false;
                }
            }

            return true;
        }
    }

    final class ModelerContext extends SwingContext {

        JFrame frame;

        public ModelerContext(JFrame frame) {
            this.frame = frame;
        }

        protected void showViewInPrimaryWindow(SwingView view) {
        }

        /**
         * Creates closeable dialogs.
         */
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

        protected Window getDefaultParentWindow() {
            return frame;
        }
    }
}