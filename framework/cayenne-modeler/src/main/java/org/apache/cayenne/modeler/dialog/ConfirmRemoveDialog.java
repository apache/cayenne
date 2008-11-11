package org.apache.cayenne.modeler.dialog;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.pref.GeneralPreferences;
import org.apache.cayenne.pref.PreferenceDetail;

/**
 * Used to confirm deleting items in the model.
 *
 */
public class ConfirmRemoveDialog {
    private boolean shouldDelete = true;
    
    /**
     * If false, no question will be asked no matter what settings are
     */
    private boolean allowAsking;
    
    public ConfirmRemoveDialog(boolean allowAsking) {
        this.allowAsking = allowAsking;
    }
    
    private void showDialog(String name) {

        JCheckBox neverPromptAgainBox = new JCheckBox("Always delete without prompt.");

        Object message[] = {String.format("Are you sure you would like to delete the %s?", name), neverPromptAgainBox};

        JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);

        JDialog dialog = pane.createDialog(Application.getFrame(), "Confirm Remove");
        dialog.setVisible(true);

        Object selectedValue = pane.getValue();
        shouldDelete = selectedValue.equals(JOptionPane.YES_OPTION);

        // If the user clicks "no", we'll just ignore whatever's in the checkbox because it's non-sensical.
        if (shouldDelete) {
            PreferenceDetail pref = Application.getInstance().getPreferenceDomain().getDetail(GeneralPreferences.DELETE_PROMPT_PREFERENCE, true);
            pref.setBooleanProperty(GeneralPreferences.DELETE_PROMPT_PREFERENCE, neverPromptAgainBox.isSelected());
            Application.getInstance().getPreferenceService().savePreferences();
        }
    }
    
    public boolean shouldDelete(String type, String name) {
        return shouldDelete(String.format("%s named '%s'?", type, name));
    }

    public boolean shouldDelete(String name) {
        if (allowAsking) {
            PreferenceDetail pref = Application.getInstance().getPreferenceDomain().getDetail(GeneralPreferences.DELETE_PROMPT_PREFERENCE, true);

            // See if the user has opted not to showDialog the delete dialog.
            if ((pref == null) || (false == pref.getBooleanProperty(GeneralPreferences.DELETE_PROMPT_PREFERENCE))) {
                showDialog(name);
            }
        }

        return shouldDelete;
    }
}
