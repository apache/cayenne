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
package org.apache.cayenne.modeler.ui.confirmremove;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.adapters.GeneralPrefs;

import javax.swing.*;

/**
 * Used to confirm deleting items in the model.
 *
 */
public class ConfirmRemoveDialog {

    private static final String DELETE = "Delete";

    private final Application application;
    private final boolean allowAsking;

    private boolean shouldDelete = true;

    /**
     * @param allowAsking if false, no question will be asked no matter what settings are
     */
    public ConfirmRemoveDialog(Application application, boolean allowAsking) {
        this.application = application;
        this.allowAsking = allowAsking;
    }

    private void showDialog(String name) {

        JCheckBox neverPromptAgainBox = new JCheckBox("Always delete without prompt.");

        Object message[] = {
                String.format("Are you sure you would like to delete the %s?", name),
                neverPromptAgainBox
        };

        JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE);
        pane.setOptions(new Object[]{DELETE, "Cancel"});
        pane.setInitialValue(DELETE);
        pane.createDialog(application.getFrame(), "Confirm Delete").setVisible(true);

        shouldDelete = DELETE.equals(pane.getValue());

        // If the user clicks "cancel" or window close button, we'll just ignore whatever's in the checkbox because
        // it's non-sensical.
        if (shouldDelete) {
            new GeneralPrefs(application.getPrefsLocator().appNode(GeneralPrefs.NODE)).setNoDeletePrompt(neverPromptAgainBox.isSelected());
        }
    }

    public boolean shouldDelete(String type, String name) {
        return shouldDelete(String.format("%s named '%s'", type, name));
    }

    public boolean shouldDelete(String name) {
        if (allowAsking && !new GeneralPrefs(application.getPrefsLocator().appNode(GeneralPrefs.NODE)).isNoDeletePrompt()) {
            showDialog(name);
        }

        return shouldDelete;
    }
}
