package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.editor.ObjEntityAttributePanel;
import org.apache.cayenne.modeler.editor.dbentity.DbEntityAttributePanel;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;


public class CutAttributeRelationshipAction extends CutAction implements MultipleObjectsAction {

    private CutAttributeAction cutAttributeAction;
    private CutRelationshipAction cutRelationshipAction;
    private JComponent currentSelectedPanel;

    public CutAttributeRelationshipAction(Application application) {
        super(application);
        cutAttributeAction = new CutAttributeAction(application);
        cutRelationshipAction = new CutRelationshipAction(application);
    }

    public JComponent getCurrentSelectedPanel() {
        return currentSelectedPanel;
    }

    public void setCurrentSelectedPanel(JComponent currentSelectedPanel) {
        this.currentSelectedPanel = currentSelectedPanel;
    }

    public String getActionName(boolean multiple) {
        if (currentSelectedPanel instanceof ObjEntityAttributePanel || currentSelectedPanel instanceof DbEntityAttributePanel) {
            return cutAttributeAction.getActionName(multiple);
        } else {
            return cutRelationshipAction.getActionName(multiple);
        }
    }

    public boolean enableForPath(ConfigurationNode object) {
        if (currentSelectedPanel instanceof ObjEntityAttributePanel || currentSelectedPanel instanceof DbEntityAttributePanel) {
            return cutAttributeAction.enableForPath(object);
        } else {
            return cutRelationshipAction.enableForPath(object);
        }
    }

    public void performAction(ActionEvent e) {
        if (currentSelectedPanel instanceof ObjEntityAttributePanel || currentSelectedPanel instanceof DbEntityAttributePanel) {
            cutAttributeAction.performAction(e);
        } else {
            cutRelationshipAction.performAction(e);
        }
    }

}