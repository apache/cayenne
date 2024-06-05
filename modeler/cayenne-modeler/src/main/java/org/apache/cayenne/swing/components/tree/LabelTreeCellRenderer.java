package org.apache.cayenne.swing.components.tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class LabelTreeCellRenderer extends JLabel implements TreeCellRenderer {

    protected final TreeCellRenderer defaultRenderer;

    LabelTreeCellRenderer() {
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
        if (!(node.getUserObject() instanceof CheckBoxNodeData)) {
            return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
        CheckBoxNodeData data = ((CheckBoxNodeData) node.getUserObject());

        setText(data.getLabel() + " [" + data.getState() + "]");
        return this;
    }
}
