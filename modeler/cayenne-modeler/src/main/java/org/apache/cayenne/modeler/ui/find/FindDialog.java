/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.modeler.ui.find;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppDialog;
import org.apache.cayenne.modeler.toolkit.border.TopBorder;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.toolkit.table.IconCellRenderer;
import org.apache.cayenne.modeler.ui.action.FindAction;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

/**
 * Modal dialog displaying search results and navigating to the selected entity on click or Enter.
 */
public class FindDialog extends AppDialog {

    private final List<FindAction.SearchResultEntry> results;
    private final JTable table;
    private final JButton okButton;

    public FindDialog(Application app, Window owner, List<FindAction.SearchResultEntry> results) {
        super(app, owner, "Search results", ModalityType.APPLICATION_MODAL);
        this.results = results;
        this.table = new JTable(buildTableModel(results));
        this.okButton = new JButton("OK");

        initLayout();
        initBindings();
    }

    private void initLayout() {
        table.getColumnModel().getColumn(0).setCellRenderer(new IconCellRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setRowMargin(3);
        table.getSelectionModel().setSelectionInterval(0, 0);

        // Suppress default Enter handling so it can be used to "open result"
        InputMap im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        InputMap imParent = im.getParent();
        imParent.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        im.setParent(imParent);
        im.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        table.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, im);

        getRootPane().setDefaultButton(okButton);

        JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        okPanel.setBorder(TopBorder.create());
        okPanel.add(okButton);

        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createEmptyBorder());
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JComponent contentPane = (JComponent) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(scrollPane);
        contentPane.add(okPanel, BorderLayout.SOUTH);
        contentPane.setPreferredSize(new Dimension(400, 325));
    }

    private void initBindings() {
        okButton.addActionListener(e -> dispose());

        JumpToResultListener listener = new JumpToResultListener();
        table.addKeyListener(listener);
        table.addMouseListener(listener);
    }

    private static DefaultTableModel buildTableModel(List<FindAction.SearchResultEntry> entries) {
        JLabel[][] data = new JLabel[entries.size()][1];
        int i = 0;
        for (FindAction.SearchResultEntry entry : entries) {
            JLabel label = new JLabel();
            label.setIcon(IconFactory.iconForObject(entry.getObject()));
            label.setText(entry.getName());
            data[i++] = new JLabel[]{label};
        }
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        model.setDataVector(data, new Object[]{""});
        return model;
    }

    private class JumpToResultListener implements MouseListener, KeyListener {

        @Override
        public void mouseReleased(MouseEvent e) {
            openResult(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                openResult(e);
            }
        }

        private void openResult(InputEvent e) {
            JTable source = (JTable) e.getSource();
            int selected = source.getSelectionModel().getLeadSelectionIndex();
            if (selected >= 0 && selected < results.size()) {
                FindAction.jumpToResult(results.get(selected), app);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }
    }
}
