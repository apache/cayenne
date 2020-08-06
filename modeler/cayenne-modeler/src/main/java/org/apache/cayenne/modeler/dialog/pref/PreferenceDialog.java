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

package org.apache.cayenne.modeler.dialog.pref;

import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.PreferenceEditor;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;

/**
 * A controller for editing Modeler preferences.
 * 
 */
public class PreferenceDialog extends CayenneController {

    public static final String GENERAL_KEY = "General";
    public static final String DATA_SOURCES_KEY = "Local DataSources";
    public static final String CLASS_PATH_KEY = "ClassPath";
    public static final String TEMPLATES_KEY = "Templates";

    private static final String[] preferenceMenus = new String[] {
            GENERAL_KEY, DATA_SOURCES_KEY, CLASS_PATH_KEY, TEMPLATES_KEY
    };

    protected PreferenceDialogView view;
    protected Map<String, CayenneController> detailControllers;
    protected PreferenceEditor editor;

    public PreferenceDialog(final CayenneController parent) {
        super(parent);

        final Window parentView = parent.getView() instanceof Window
                ? (Window) parent.getView()
                : SwingUtilities.getWindowAncestor(parent.getView());
        this.view = (parentView instanceof Dialog)
                ? new PreferenceDialogView((Dialog) parentView)
                : new PreferenceDialogView((Frame) parentView);
        this.detailControllers = new HashMap<>();

        // editor must be configured before startup for "showDetailViewAction()" to work
        this.editor = new CayenneModelerPreferenceEditor(application);

        initBindings();
    }

    protected void initBindings() {
        final JList<String> list = view.getList();
        list.setListData(preferenceMenus);
        list.addListSelectionListener(e -> updateSelection());

        view.getCancelButton().addActionListener(e -> cancelAction());
        view.getSaveButton().addActionListener(e -> savePreferencesAction());
    }

    public void updateSelection() {
        final String selection = view.getList().getSelectedValue();
        if (selection != null) {
            view.getDetailLayout().show(view.getDetailPanel(), selection);
        }
    }

    public void cancelAction() {
        editor.revert();
        view.dispose();
    }

    public void savePreferencesAction() {
        editor.save();
        view.dispose();
    }

    /**
     * Configures preferences dialog to display an editor for a local DataSource with
     * specified name.
     */
    public void showDataSourceEditorAction(final Object dataSourceKey) {
        configure();

        // this will install needed controller
        view.getDetailLayout().show(view.getDetailPanel(), DATA_SOURCES_KEY);

        final DataSourcePreferences controller = (DataSourcePreferences) detailControllers
                .get(DATA_SOURCES_KEY);
        controller.editDataSourceAction(dataSourceKey);
        view.setVisible(true);
    }

    /**
     * Configures preferences dialog to display an editor for a local DataSource with
     * specified name.
     */
    public void showClassPathEditorAction() {
        configure();

        // this will install needed controller
        view.getDetailLayout().show(view.getDetailPanel(), CLASS_PATH_KEY);

        ClasspathPreferences controller = (ClasspathPreferences) detailControllers
                .get(CLASS_PATH_KEY);
        controller.getView().setEnabled(true);
        view.setVisible(true);
    }

    public void startupAction(final String key) {
        configure();
        view.getList().setSelectedValue(key == null ? GENERAL_KEY : key, true);
        view.setVisible(true);
    }

    public void startupToCreateTemplate(String template, String superTemplate) {
        configure();
        ((TemplatePreferences) detailControllers.get(TEMPLATES_KEY)).addTemplateAction(template, superTemplate);
    }

    protected void configure() {
        // init known panels
        registerPanel(GENERAL_KEY, new GeneralPreferences(this));
        registerPanel(DATA_SOURCES_KEY, new DataSourcePreferences(this));
        registerPanel(CLASS_PATH_KEY, new ClasspathPreferences(this));
        registerPanel(TEMPLATES_KEY, new TemplatePreferences(this));
        view.getDetailLayout().show(view.getDetailPanel(), GENERAL_KEY);
        view.pack();

        // show
        centerView();
        makeCloseableOnEscape();

        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModal(true);
    }

    protected void registerPanel(final String name, final CayenneController panelController) {
        detailControllers.put(name, panelController);
        view.getDetailPanel().add(panelController.getView(), name);
    }

    public Component getView() {
        return view;
    }

    public PreferenceEditor getEditor() {
        return editor;
    }
}
