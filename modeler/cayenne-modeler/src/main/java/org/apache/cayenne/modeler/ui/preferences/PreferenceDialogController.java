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
import org.apache.cayenne.modeler.ui.preferences.more.MorePreferencesController;
import org.apache.cayenne.modeler.ui.preferences.classpath.ClasspathPreferencesController;
import org.apache.cayenne.modeler.ui.preferences.dbconnector.DBConnectorPreferencesController;
import org.apache.cayenne.modeler.ui.preferences.general.GeneralPreferencesController;

import javax.swing.*;
import java.awt.*;

public class PreferenceDialogController extends ChildController<RootController> {

    private static final String GENERAL_KEY = "General";
    private static final String DB_CONNECTORS_KEY = "DB Connectors";
    private static final String CLASSPATH_KEY = "Classpath";
    private static final String MORE_KEY = "More...";

    private static final String[] preferenceMenus = new String[]{
            GENERAL_KEY, DB_CONNECTORS_KEY, CLASSPATH_KEY, MORE_KEY
    };

    // Session-only memory of which card the user last viewed. The dialog is
    // recreated on every open, so an instance field would always reset to General.
    private static String lastSelectedCard = GENERAL_KEY;

    private final PreferenceDialogView view;

    private final GeneralPreferencesController generalPrefsController;
    private final DBConnectorPreferencesController dbConnectorPrefsController;
    private final ClasspathPreferencesController classpathPrefsController;
    private final MorePreferencesController allPrefsController;

    public PreferenceDialogController(RootController parent) {
        super(parent);

        Window parentView = parent.getView() instanceof Window
                ? (Window) parent.getView()
                : SwingUtilities.getWindowAncestor(parent.getView());

        this.view = (parentView instanceof Dialog)
                ? new PreferenceDialogView(this, (Dialog) parentView)
                : new PreferenceDialogView(this, (Frame) parentView);

        view.setMenuItems(preferenceMenus);

        this.generalPrefsController = new GeneralPreferencesController(this);
        view.addCard(GENERAL_KEY, generalPrefsController.getView());

        this.dbConnectorPrefsController = new DBConnectorPreferencesController(this);
        view.addCard(DB_CONNECTORS_KEY, dbConnectorPrefsController.getView());

        this.classpathPrefsController = new ClasspathPreferencesController(this);
        view.addCard(CLASSPATH_KEY, classpathPrefsController.getView());

        this.allPrefsController = new MorePreferencesController(this);
        view.addCard(MORE_KEY, allPrefsController.getView());
    }

    void cardSelected(String name) {
        lastSelectedCard = name;
        view.showCard(name);
    }

    void cancelClicked() {
        dbConnectorPrefsController.discard();
        view.dispose();
    }

    void saveClicked() {
        dbConnectorPrefsController.commit();
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

    public void showDBConnectorEditorAction(Object connectorKey) {
        dbConnectorPrefsController.editConnectorAction(connectorKey);
        doShow(DB_CONNECTORS_KEY, dbConnectorPrefsController);
    }

    private ChildController<?> controllerFor(String cardKey) {
        switch (cardKey) {
            case DB_CONNECTORS_KEY:
                return dbConnectorPrefsController;
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
        view.showCard(GENERAL_KEY);
        view.pack();

        centerView();
        makeCloseableOnEscape();

        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModalityType(Dialog.ModalityType.MODELESS);

        view.showCard(cardKey);
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
