package org.apache.cayenne.swing.components.tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class CheckBoxTreeCellRenderer extends JCheckBox implements TreeCellRenderer {

    protected final TreeCellRenderer defaultRenderer;

    public CheckBoxTreeCellRenderer() {
        defaultRenderer = new DefaultTreeCellRenderer();
        setOpaque(false);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        if (!(value instanceof DefaultMutableTreeNode)) {
            return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        if (!(userObject instanceof CheckBoxNodeData)) {
            return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
        CheckBoxNodeData data = ((CheckBoxNodeData) node.getUserObject());

        if (data.getState() != CheckBoxNodeData.State.INDETERMINATE) {
            setSelected(data.isSelected());
        } else {
            setSelected(false);
        }

        return this;
    }
}
