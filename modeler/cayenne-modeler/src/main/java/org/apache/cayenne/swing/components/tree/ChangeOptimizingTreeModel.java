package org.apache.cayenne.swing.components.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Objects;

public class ChangeOptimizingTreeModel extends DefaultTreeModel {

    public ChangeOptimizingTreeModel(TreeNode root) {
        super(root);
    }

    public ChangeOptimizingTreeModel(TreeNode root, boolean asksAllowsChildren) {
        super(root, asksAllowsChildren);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        if (path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            Object value = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (!Objects.equals(value, newValue)) {
                super.valueForPathChanged(path, newValue);
            }
        } else {
            super.valueForPathChanged(path, newValue);
        }
    }
}
