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

package org.apache.cayenne.modeler.ui.dbmerge;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.dbsync.merge.context.MergeDirection;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.modeler.toolkit.combobox.CMComboBox;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reusable panel for selecting which {@link MergerToken}s should be applied during a
 * DB merge, with per-row check + direction (TO_DB / TO_MODEL) controls and bulk
 * "check all" / "reverse all" actions.
 */
public class MergerTokenSelector extends JPanel {

    private final JTable tokensTable;
    private final JCheckBox checkAll;
    private final JLabel checkAllLabel;
    private final JButton reverseAll;

    private final Set<MergerToken> excludedTokens;
    private final List<MergerToken> selectableTokensList;

    private MergerToken token;
    private MergerTokenFactory mergerTokenFactory;
    private boolean isReverse;

    public MergerTokenSelector() {
        this.tokensTable = new JTable();
        this.checkAll = new JCheckBox();
        this.checkAllLabel = new JLabel("Check All Operations");
        this.reverseAll = new JButton("Reverse All Operations");
        this.excludedTokens = new HashSet<>();
        this.selectableTokensList = new ArrayList<>();

        initLayout();
        initBindings();
    }

    public void setMergerTokenFactory(MergerTokenFactory mergerTokenFactory) {
        this.mergerTokenFactory = mergerTokenFactory;
    }

    public void setTokens(List<MergerToken> tokens) {
        selectableTokensList.clear();
        selectableTokensList.addAll(tokens);
        excludedTokens.addAll(tokens);
    }

    public List<MergerToken> getSelectedTokens() {
        List<MergerToken> t = new ArrayList<>(selectableTokensList);
        t.removeAll(excludedTokens);
        return Collections.unmodifiableList(t);
    }

    public List<MergerToken> getSelectableTokens() {
        return Collections.unmodifiableList(selectableTokensList);
    }

    public void removeToken(MergerToken token) {
        selectableTokensList.remove(token);
        excludedTokens.remove(token);

        AbstractTableModel model = (AbstractTableModel) tokensTable.getModel();
        model.fireTableDataChanged();
    }

    /**
     * Called by table binding script to set current token.
     */
    public void setToken(MergerToken token) {
        this.token = token;
    }

    public boolean isIncluded() {
        return token != null && !excludedTokens.contains(token);
    }

    public void setIncluded(boolean b) {
        if (token == null) {
            return;
        }
        if (b) {
            excludedTokens.remove(token);
        } else {
            excludedTokens.add(token);
        }
        tableSelectedAction();
    }

    /**
     * A callback action that updates the state of Select All checkbox.
     */
    public void tableSelectedAction() {
        int unselectedCount = excludedTokens.size();

        if (unselectedCount == selectableTokensList.size()) {
            checkAll.setSelected(false);
        } else if (unselectedCount == 0) {
            checkAll.setSelected(true);
        }
    }

    public boolean isSelected(MergerToken token) {
        return selectableTokensList.contains(token) && !excludedTokens.contains(token);
    }

    public void select(MergerToken token, boolean select) {
        if (select) {
            excludedTokens.remove(token);
        } else {
            excludedTokens.add(token);
        }
    }

    public void setDirection(MergerToken token, MergeDirection direction) {
        if (token.getDirection().equals(direction)) {
            return;
        }

        int i = selectableTokensList.indexOf(token);
        MergerToken reverse = token.createReverse(mergerTokenFactory);
        selectableTokensList.set(i, reverse);
        if (excludedTokens.remove(token)) {
            excludedTokens.add(reverse);
        }

        // Repaint, so that "Operation" column updates properly
        tokensTable.repaint();
    }

    public boolean isReverse() {
        return isReverse;
    }

    private void initLayout() {
        tokensTable.setRowHeight(25);
        tokensTable.setRowMargin(3);

        JPanel checkAllPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        checkAllPanel.add(checkAll);
        checkAllPanel.add(checkAllLabel);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(checkAllPanel, BorderLayout.WEST);
        topPanel.add(ButtonBarFactory.buildRightAlignedBar(reverseAll), BorderLayout.EAST);

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(50dlu;pref):grow",
                "p, $rgap, fill:40dlu:grow"));
        builder.setDefaultDialogBorder();
        builder.addSeparator("Select Operations", cc.xy(1, 1));
        builder.add(new JScrollPane(
                tokensTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), cc.xy(1, 3));

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initBindings() {
        checkAll.addItemListener(e -> {
            checkAllLabel.setText(checkAll.isSelected() ? "Uncheck All Operations" : "Check All Operations");
        });
        checkAll.addActionListener(e -> checkAllAction());
        reverseAll.addActionListener(e -> reverseAllAction());

        TableModel model = new MergerTokenTableModel(this);
        tokensTable.setModel(model);

        MergeDirection[] dirs = new MergeDirection[]{MergeDirection.TO_DB, MergeDirection.TO_MODEL};

        TableColumnModel columnModel = tokensTable.getColumnModel();

        // dropdown for direction column
        JComboBox<MergeDirection> directionCombo = new CMComboBox<>(dirs);
        directionCombo.setEditable(false);
        TableColumn directionColumn = columnModel.getColumn(MergerTokenTableModel.COL_DIRECTION);

        directionColumn.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalTextPosition(SwingConstants.LEFT);
                setIcon(IconFactory.buildIcon("icon-arrow-open.png"));
                return this;
            }
        });
        directionColumn.setCellEditor(new DefaultCellEditor(directionCombo));

        columnModel.getColumn(MergerTokenTableModel.COL_SELECT).setPreferredWidth(50);
        columnModel.getColumn(MergerTokenTableModel.COL_DIRECTION).setPreferredWidth(100);
        columnModel.getColumn(MergerTokenTableModel.COL_SELECT).setMaxWidth(50);
        columnModel.getColumn(MergerTokenTableModel.COL_DIRECTION).setMaxWidth(100);
    }

    private void checkAllAction() {
        stopEditing();

        if (checkAll.isSelected()) {
            excludedTokens.clear();
        } else {
            excludedTokens.addAll(selectableTokensList);
        }

        AbstractTableModel model = (AbstractTableModel) tokensTable.getModel();
        model.fireTableDataChanged();
    }

    private void reverseAllAction() {
        stopEditing();

        isReverse = !isReverse;

        for (int i = 0; i < selectableTokensList.size(); i++) {
            MergerToken token = selectableTokensList.get(i);
            MergerToken reverse = token.createReverse(mergerTokenFactory);
            selectableTokensList.set(i, reverse);
            if (excludedTokens.remove(token)) {
                excludedTokens.add(reverse);
            }
        }

        Collections.sort(selectableTokensList);
        AbstractTableModel model = (AbstractTableModel) tokensTable.getModel();
        model.fireTableDataChanged();
    }

    private void stopEditing() {
        TableCellEditor cellEditor = tokensTable.getCellEditor();
        if (cellEditor != null) {
            cellEditor.stopCellEditing();
        }
    }
}
