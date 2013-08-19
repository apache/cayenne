package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.CayenneAction;

public class CollapseTreeAction extends CayenneAction {
	private final static String COLLAPSE = "collapse";
	
	public static String getActionName() {
		return "Collapse tree";
	}
	 
	public String getIconName() {
		return "icon-tree-collapse.png";
	}

	public CollapseTreeAction(Application application) {
		super(getActionName(), application);
	}

	@Override
	public void performAction(ActionEvent e) {	
		getApplication().getFrameController().getEditorView().getFilterController().treeExpOrCollPath(COLLAPSE);		
	}
}
