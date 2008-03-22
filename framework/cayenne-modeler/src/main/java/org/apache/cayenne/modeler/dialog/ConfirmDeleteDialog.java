package org.apache.cayenne.modeler.dialog;

import org.apache.cayenne.modeler.util.CayenneDialog;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.ConfigurePreferencesAction;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.dialog.pref.GeneralPreferences;
import org.apache.cayenne.pref.PreferenceDetail;

import javax.swing.*;
import java.awt.*;

/**
 * Used to confirm deleting items in the model.
 *
 * @author Kevin Menard
 */
public class ConfirmDeleteDialog extends CayenneDialog {
    private String type;
    private String name;
    private boolean shouldDelete = true;

    public void show() {

        PreferenceDetail pref = Application.getInstance().getPreferenceDomain().getDetail(GeneralPreferences.DELETE_PROMPT_PREFERENCE, true);

        // See if the user has opted not to show the delete dialog.
        if ((pref == null) || (false == pref.getBooleanProperty(GeneralPreferences.DELETE_PROMPT_PREFERENCE))) {
            JCheckBox neverPromptAgainBox = new JCheckBox("Always delete without prompt.");

            Object message[] = {String.format("Are you sure you would like to delete the %s named '%s'?", type, name), neverPromptAgainBox};

            JOptionPane pane =
                    new JOptionPane(message,
                            JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);

            JDialog dialog = pane.createDialog(Application.getFrame(), "File exists");
            dialog.setVisible(true);

            Object selectedValue = pane.getValue();
            shouldDelete = selectedValue.equals(JOptionPane.YES_OPTION);

            // If the user clicks "no", we'll just ignore whatever's in the checkbox because it's non-sensical.
            if (shouldDelete) {
                pref.setBooleanProperty(GeneralPreferences.DELETE_PROMPT_PREFERENCE, neverPromptAgainBox.isSelected());
                Application.getInstance().getPreferenceService().savePreferences();
            }
        }
    }

    public boolean shouldDelete(String type, String name) {
        this.type = type;
        this.name = name;

        show();

        return shouldDelete;
    }
}
