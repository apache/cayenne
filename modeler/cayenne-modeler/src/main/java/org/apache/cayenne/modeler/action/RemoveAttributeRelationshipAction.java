package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.editor.ObjEntityAttributePanel;
import org.apache.cayenne.modeler.editor.dbentity.DbEntityAttributePanel;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;


public class RemoveAttributeRelationshipAction extends RemoveAction implements MultipleObjectsAction {

    private RemoveAttributeAction removeAttributeAction;
    private RemoveRelationshipAction removeRelationshipAction;
    private JComponent currentSelectedPanel;

    public RemoveAttributeRelationshipAction(Application application) {
        super(application);
        removeAttributeAction = new RemoveAttributeAction(application);
        removeRelationshipAction = new RemoveRelationshipAction(application);
    }

    public JComponent getCurrentSelectedPanel() {
        return currentSelectedPanel;
    }

    public void setCurrentSelectedPanel(JComponent currentSelectedPanel) {
        this.currentSelectedPanel = currentSelectedPanel;
    }

    public String getActionName(boolean multiple) {
        if (currentSelectedPanel instanceof ObjEntityAttributePanel || currentSelectedPanel instanceof DbEntityAttributePanel) {
            return removeAttributeAction.getActionName(multiple);
        } else {
            return removeRelationshipAction.getActionName(multiple);
        }
    }

    public boolean enableForPath(ConfigurationNode object) {
        if (currentSelectedPanel instanceof ObjEntityAttributePanel || currentSelectedPanel instanceof DbEntityAttributePanel) {
            return removeAttributeAction.enableForPath(object);
        } else {
            return removeRelationshipAction.enableForPath(object);
        }
    }

    public void performAction(ActionEvent e, boolean allowAsking) {
        if (currentSelectedPanel instanceof ObjEntityAttributePanel || currentSelectedPanel instanceof DbEntityAttributePanel) {
            removeAttributeAction.performAction(e, allowAsking);
        } else {
            removeRelationshipAction.performAction(e, allowAsking);
        }
    }

}