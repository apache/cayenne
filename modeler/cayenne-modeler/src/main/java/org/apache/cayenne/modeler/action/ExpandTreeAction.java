package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.modeler.Application;

import org.apache.cayenne.modeler.util.CayenneAction;

public class ExpandTreeAction extends CayenneAction {
	private final static String EXPAND = "expand";
	
    public static String getActionName() {
        return "Expand tree";
    }
    
    @Override
    public String getIconName() {
        return "icon-tree-expand.png";
    }

    public ExpandTreeAction(Application application) {
        super(getActionName(), application);
    }

	@Override
	public void performAction(ActionEvent e) {	
		getApplication().getFrameController().getEditorView().getFilterController().treeExpOrCollPath(EXPAND);	
	}
}
