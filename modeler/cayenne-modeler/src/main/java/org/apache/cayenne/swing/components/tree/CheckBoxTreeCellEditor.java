package org.apache.cayenne.swing.components.tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CheckBoxTreeCellEditor extends AbstractCellEditor implements TreeCellEditor {

    protected final CheckBoxTreeCellRenderer renderer;
    protected final TreeCellEditor defaultEditor;
    protected final JTree tree;

    private final AbstractAction toggleListener;

    protected CheckBoxTreeCellEditor(JTree tree) {
        this.tree = tree;
        this.renderer = new CheckBoxTreeCellRenderer();
        this.defaultEditor = new DefaultTreeCellEditor(tree, new DefaultTreeCellRenderer());

        this.toggleListener = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object cellEditorValue = getCellEditorValue();
                if (!(cellEditorValue instanceof CheckBoxNodeData) || !(e.getSource() instanceof JCheckBox)) {
                    return;
                }
                TreePath editingPath = tree.getEditingPath();
                JCheckBox checkBox = (JCheckBox) e.getSource();

                CheckBoxNodeData data = (CheckBoxNodeData) cellEditorValue;
                tree.getModel().valueForPathChanged(editingPath, data.withState(checkBox.isSelected()));
                tree.stopEditing();
            }
        };
    }

    @Override
    public Object getCellEditorValue() {
        DefaultMutableTreeNode editedNode = (DefaultMutableTreeNode) tree.getEditingPath().getLastPathComponent();
        return editedNode.getUserObject();
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
                                                boolean leaf, int row) {
        Component component = renderer.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, true);

        if (component instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) component;
            checkBox.setAction(toggleListener);
        }
        return component;
    }
}
