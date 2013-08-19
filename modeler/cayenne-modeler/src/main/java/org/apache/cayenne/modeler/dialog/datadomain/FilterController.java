package org.apache.cayenne.modeler.dialog.datadomain;

import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.ProjectTreeModel;
import org.apache.cayenne.modeler.ProjectTreeView;

public class FilterController {
	
	private HashMap<String,Boolean> filterMap = new HashMap<String, Boolean>();
	private ProjectTreeView tree;
	private ProjectController eventController;
	private ProjectTreeModel treeModel;
	
	public ProjectTreeView getTree() {
		return tree;
	}

	public ProjectTreeModel getTreeModel() {
		return treeModel;
	}

	public ProjectController getEventController() {
		return eventController;
	}

	public HashMap<String, Boolean> getFilterMap() {
		return filterMap;
	}

	public FilterController(ProjectController eventController, ProjectTreeView treePanel) {
	
		this.eventController = eventController;
		this.tree = treePanel;
		this.treeModel = tree.getProjectModel();
		
		filterMap.put("dbEntity",true);
		filterMap.put("objEntity",true);
		filterMap.put("embeddable",true);
		filterMap.put("procedure",true);
		filterMap.put("query",true);
	}
	
	
	public void treeExpOrCollPath(String action) {
		TreeNode root = (TreeNode) treeModel.getRoot();
		expandAll(tree, new TreePath(root),action);
	}

	private void expandAll(JTree tree, TreePath parent, String action) {
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		
		if (node.getChildCount() >= 0) {
			for (Enumeration e = node.children(); e.hasMoreElements();) {
				TreeNode n = (TreeNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandAll(tree, path, action);
			}
		}
		if(action == "expand") {
			tree.expandPath(parent);
		}
		else if(action == "collapse") {
			tree.collapsePath(parent);
		}
	}
	
}