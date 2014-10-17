package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.editor.ObjEntityAttributePanel;
import org.apache.cayenne.modeler.editor.dbentity.DbEntityAttributePanel;
import javax.swing.JComponent;

public class CopyAttributeRelationshipAction extends CopyAction implements MultipleObjectsAction {

    private CopyAttributeAction copyAttributeAction;
    private CopyRelationshipAction copyRelationshipAction;
    private JComponent currentSelectedPanel;

    protected CopyAttributeRelationshipAction(Application application) {
        super(application);
        copyAttributeAction = new CopyAttributeAction(application);
        copyRelationshipAction = new CopyRelationshipAction(application);
    }

    public JComponent getCurrentSelectedPanel() {
        return currentSelectedPanel;
    }

    public void setCurrentSelectedPanel(JComponent currentSelectedPanel) {
        this.currentSelectedPanel = currentSelectedPanel;
    }

    public String getActionName(boolean multiple) {
        if (currentSelectedPanel instanceof ObjEntityAttributePanel || currentSelectedPanel instanceof DbEntityAttributePanel) {
            return copyAttributeAction.getActionName(multiple);
        } else {
            return copyRelationshipAction.getActionName(multiple);
        }
    }

    public boolean enableForPath(ConfigurationNode object) {
        if (currentSelectedPanel instanceof ObjEntityAttributePanel || currentSelectedPanel instanceof DbEntityAttributePanel) {
            return copyAttributeAction.enableForPath(object);
        } else {
            return copyRelationshipAction.enableForPath(object);
        }
    }

    public Object copy(ProjectController mediator) {
        if (currentSelectedPanel instanceof ObjEntityAttributePanel || currentSelectedPanel instanceof DbEntityAttributePanel) {
            return copyAttributeAction.copy(mediator);
        } else {
            return copyRelationshipAction.copy(mediator);
        }
    }

}
