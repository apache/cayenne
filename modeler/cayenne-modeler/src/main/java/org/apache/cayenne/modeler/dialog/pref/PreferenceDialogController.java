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

import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.mvc.RootController;

import javax.swing.*;
import java.awt.*;

public class PreferenceDialogController extends ChildController<RootController> {

    private static final String GENERAL_KEY = "General";
    private static final String DATA_SOURCES_KEY = "Local DataSources";
    private static final String CLASS_PATH_KEY = "ClassPath";

    private static final String[] preferenceMenus = new String[]{
            GENERAL_KEY, DATA_SOURCES_KEY, CLASS_PATH_KEY
    };

    private final PreferenceDialogView view;
    private final PreferenceDialogContext context;

    private final GeneralPreferencesController generalPrefsController;
    private final DataSourcePreferencesController dataSourcePrefsController;
    private final ClasspathPreferencesController classpathPrefsController;

    public PreferenceDialogController(final RootController parent) {
        super(parent);

        Window parentView = parent.getView() instanceof Window
                ? (Window) parent.getView()
                : SwingUtilities.getWindowAncestor(parent.getView());

        this.view = (parentView instanceof Dialog)
                ? new PreferenceDialogView((Dialog) parentView)
                : new PreferenceDialogView((Frame) parentView);


        this.context = new PreferenceDialogContext(application);

        JList<String> list = view.getList();
        list.setListData(preferenceMenus);
        list.addListSelectionListener(e -> updateSelection());

        view.getCancelButton().addActionListener(e -> cancelAction());
        view.getSaveButton().addActionListener(e -> savePreferencesAction());

        this.generalPrefsController = new GeneralPreferencesController(this);
        view.getDetailPanel().add(generalPrefsController.getView(), GENERAL_KEY);

        this.dataSourcePrefsController = new DataSourcePreferencesController(this);
        view.getDetailPanel().add(dataSourcePrefsController.getView(), DATA_SOURCES_KEY);

        this.classpathPrefsController = new ClasspathPreferencesController(this);
        view.getDetailPanel().add(classpathPrefsController.getView(), CLASS_PATH_KEY);
    }

    public void updateSelection() {
        final String selection = view.getList().getSelectedValue();
        if (selection != null) {
            view.getDetailLayout().show(view.getDetailPanel(), selection);
        }
    }

    private void cancelAction() {
        context.revert();
        view.dispose();
    }

    private void savePreferencesAction() {
        context.save();
        view.dispose();
    }

    public void showGeneralEditorAction() {
        doShow(GENERAL_KEY, generalPrefsController);
    }

    public void showClassPathEditorAction() {
        doShow(CLASS_PATH_KEY, classpathPrefsController);
    }

    public void showDataSourceEditorAction(Object dataSourceKey) {
        dataSourcePrefsController.editDataSourceAction(dataSourceKey);
        doShow(DATA_SOURCES_KEY, dataSourcePrefsController);
    }

    private void doShow(String cardKey, ChildController<?> childController) {
        view.getDetailLayout().show(view.getDetailPanel(), GENERAL_KEY);
        view.pack();

        centerView();
        makeCloseableOnEscape();

        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModalityType(Dialog.ModalityType.MODELESS);

        view.getDetailLayout().show(view.getDetailPanel(), cardKey);
        view.getList().setSelectedValue(cardKey, true);
        childController.getView().setEnabled(true);
        view.setVisible(true);
    }

    @Override
    public Component getView() {
        return view;
    }

    public PreferenceDialogContext getContext() {
        return context;
    }
}
