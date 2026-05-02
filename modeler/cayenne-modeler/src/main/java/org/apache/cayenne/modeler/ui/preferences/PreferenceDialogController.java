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

package org.apache.cayenne.modeler.ui.preferences;

import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.mvc.RootController;
import org.apache.cayenne.modeler.ui.preferences.all.AllPreferencesController;
import org.apache.cayenne.modeler.ui.preferences.classpath.ClasspathPreferencesController;
import org.apache.cayenne.modeler.ui.preferences.datasource.DataSourcePreferencesController;
import org.apache.cayenne.modeler.ui.preferences.general.GeneralPreferencesController;

import javax.swing.*;
import java.awt.*;

public class PreferenceDialogController extends ChildController<RootController> {

    private static final String GENERAL_KEY = "General";
    private static final String DATA_SOURCES_KEY = "DataSources";
    private static final String CLASSPATH_KEY = "Classpath";
    private static final String MORE_KEY = "More...";

    private static final String[] preferenceMenus = new String[]{
            GENERAL_KEY, DATA_SOURCES_KEY, CLASSPATH_KEY, MORE_KEY
    };

    // Session-only memory of which card the user last viewed. The dialog is
    // recreated on every open, so an instance field would always reset to General.
    private static String lastSelectedCard = GENERAL_KEY;

    private final PreferenceDialogView view;

    private final GeneralPreferencesController generalPrefsController;
    private final DataSourcePreferencesController dataSourcePrefsController;
    private final ClasspathPreferencesController classpathPrefsController;
    private final AllPreferencesController allPrefsController;

    public PreferenceDialogController(final RootController parent) {
        super(parent);

        Window parentView = parent.getView() instanceof Window
                ? (Window) parent.getView()
                : SwingUtilities.getWindowAncestor(parent.getView());

        this.view = (parentView instanceof Dialog)
                ? new PreferenceDialogView((Dialog) parentView)
                : new PreferenceDialogView((Frame) parentView);

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
        view.getDetailPanel().add(classpathPrefsController.getView(), CLASSPATH_KEY);

        this.allPrefsController = new AllPreferencesController(this);
        view.getDetailPanel().add(allPrefsController.getView(), MORE_KEY);
    }

    public void updateSelection() {
        final String selection = view.getList().getSelectedValue();
        if (selection != null) {
            view.getDetailLayout().show(view.getDetailPanel(), selection);
            lastSelectedCard = selection;
        }
    }

    private void cancelAction() {
        dataSourcePrefsController.discard();
        view.dispose();
    }

    private void savePreferencesAction() {
        dataSourcePrefsController.commit();
        generalPrefsController.commit();
        classpathPrefsController.commit();
        view.dispose();
    }

    public void showLastSelectedAction() {
        doShow(lastSelectedCard, controllerFor(lastSelectedCard));
    }

    public void showClassPathEditorAction() {
        doShow(CLASSPATH_KEY, classpathPrefsController);
    }

    public void showDataSourceEditorAction(Object dataSourceKey) {
        dataSourcePrefsController.editDataSourceAction(dataSourceKey);
        doShow(DATA_SOURCES_KEY, dataSourcePrefsController);
    }

    private ChildController<?> controllerFor(String cardKey) {
        switch (cardKey) {
            case DATA_SOURCES_KEY:
                return dataSourcePrefsController;
            case CLASSPATH_KEY:
                return classpathPrefsController;
            case MORE_KEY:
                return allPrefsController;
            case GENERAL_KEY:
            default:
                return generalPrefsController;
        }
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

    public ClasspathPreferencesController getClasspathPrefsController() {
        return classpathPrefsController;
    }
}
