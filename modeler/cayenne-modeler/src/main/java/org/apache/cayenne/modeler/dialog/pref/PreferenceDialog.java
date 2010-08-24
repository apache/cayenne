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

package org.apache.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.PreferenceEditor;

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
    protected Map detailControllers;
    protected PreferenceEditor editor;

    public PreferenceDialog(CayenneController parent) {
        super(parent);

        Window parentView = parent.getView() instanceof Window ? (Window) parent.getView() : 
            SwingUtilities.getWindowAncestor(parent.getView());
        this.view = (parentView instanceof Dialog) ? new PreferenceDialogView(
                (Dialog) parentView) : new PreferenceDialogView((Frame) parentView);
        this.detailControllers = new HashMap();

        // editor must be configured before startup for "showDetailViewAction()" to work
        this.editor = new CayenneModelerPreferenceEditor(application);

        initBindings();
    }

    protected void initBindings() {
        final JList list = view.getList();
        list.setListData(preferenceMenus);
        list.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                Object selection = list.getSelectedValue();
                if (selection != null) {
                    view.getDetailLayout().show(
                            view.getDetailPanel(),
                            selection.toString());
                }
            }
        });

        view.getCancelButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelAction();
            }
        });

        view.getSaveButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                savePreferencesAction();
            }
        });
    }

    public void cancelAction() {
        editor.revert();
        view.dispose();
    }

    public void savePreferencesAction() {
        editor.save();

        // update

        view.dispose();
    }

    /**
     * Configures preferences dialog to display an editor for a local DataSource with
     * specified name.
     */
    public void showDataSourceEditorAction(Object dataSourceKey) {
        configure();

        // this will install needed controller
        view.getDetailLayout().show(view.getDetailPanel(), DATA_SOURCES_KEY);

        DataSourcePreferences controller = (DataSourcePreferences) detailControllers
                .get(DATA_SOURCES_KEY);
        controller.editDataSourceAction(dataSourceKey);
        view.setVisible(true);
    }

    public void startupAction(String key) {

        if (key == null) {
            key = GENERAL_KEY;
        }

        configure();
        view.getList().setSelectedValue(key, true);
        view.setVisible(true);
    }

    protected void configure() {
        // init known panels
        registerPanel(GENERAL_KEY, new GeneralPreferences(this));
        registerPanel(DATA_SOURCES_KEY, new DataSourcePreferences(this));
        registerPanel(CLASS_PATH_KEY, new ClasspathPreferences(this));
        registerPanel(TEMPLATES_KEY, new TemplatePreferences(this));
        view.getDetailLayout().show(view.getDetailPanel(), GENERAL_KEY);
        // view.getSplit().setDividerLocation(150);
        view.pack();

        // show
        centerView();
        makeCloseableOnEscape();

        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModal(true);
    }

    protected void registerPanel(String name, CayenneController panelController) {
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
