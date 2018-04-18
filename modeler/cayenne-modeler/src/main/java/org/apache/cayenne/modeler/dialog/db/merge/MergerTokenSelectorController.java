/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.dialog.db.merge;

import org.apache.cayenne.dbsync.merge.context.MergeDirection;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MergerTokenSelectorController extends CayenneController {

    protected MergerTokenSelectorView view;
    protected ObjectBinding tableBinding;

    protected MergerToken token;
    protected int permanentlyExcludedCount;
    protected Set<MergerToken> excludedTokens;
    protected List<MergerToken> selectableTokensList;
    protected MergerTokenFactory mergerTokenFactory;
    protected boolean isReverse;

    public MergerTokenSelectorController(final CayenneController parent) {
        super(parent);
        this.view = new MergerTokenSelectorView();
        this.excludedTokens = new HashSet<>();
        this.selectableTokensList = new ArrayList<>();
        initController();
    }

    public void setMergerTokenFactory(final MergerTokenFactory mergerTokenFactory) {
        this.mergerTokenFactory = mergerTokenFactory;
    }

    public void setTokens(final List<MergerToken> tokens) {
        selectableTokensList.clear();
        selectableTokensList.addAll(tokens);
        excludedTokens.addAll(tokens);
    }

    public List<MergerToken> getSelectedTokens() {
        final List<MergerToken> t = new ArrayList<>(selectableTokensList);
        t.removeAll(excludedTokens);
        return Collections.unmodifiableList(t);
    }

    public List<MergerToken> getSelectableTokens() {
        return Collections.unmodifiableList(selectableTokensList);
    }
    
    public void removeToken(final MergerToken token) {
        selectableTokensList.remove(token);
        excludedTokens.remove(token);

        final AbstractTableModel model = (AbstractTableModel) view.getTokens().getModel();
        model.fireTableDataChanged();
    }

    // ----- properties -----

    public Component getView() {
        return view;
    }

    /**
     * Called by table binding script to set current token.
     */
    public void setToken(final MergerToken token) {
        this.token = token;
    }

    /**
     * Returns {@link MergerToken}s that are excluded from DB generation.
     */
    /*
     * public Collection getExcludedTokens() { return excludedTokens; }
     */

    public boolean isIncluded() {
        if (token == null) {
            return false;
        }

        return !excludedTokens.contains(token);
    }

    public void setIncluded(final boolean b) {
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
        final int unselectedCount = excludedTokens.size() - permanentlyExcludedCount;

        if (unselectedCount == selectableTokensList.size()) {
            view.getCheckAll().setSelected(false);
        } else if (unselectedCount == 0) {
            view.getCheckAll().setSelected(true);
        }
    }

    // ------ other stuff ------

    protected void initController() {
        final BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getCheckAll(), "checkAllAction()");
        builder.bindToAction(view.getReverseAll(), "reverseAllAction()");

        final TableModel model = new MergerTokenTableModel(this);

        final MergeDirection[] dirs = new MergeDirection[] {
                MergeDirection.TO_DB, MergeDirection.TO_MODEL
        };

        view.getTokens().setModel(model);

        final TableColumnModel columnModel = view.getTokens().getColumnModel();

        // dropdown for direction column
        final JComboBox directionCombo = Application.getWidgetFactory().createComboBox(dirs, false);
        directionCombo.setEditable(false);
        final TableColumn directionColumn = columnModel.getColumn(
                MergerTokenTableModel.COL_DIRECTION);

        directionColumn.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                           final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalTextPosition(SwingConstants.LEFT);
                setIcon(ModelerUtil.buildIcon("icon-arrow-open.png"));
                return this;
            }
        });

        directionColumn.setCellEditor(new DefaultCellEditor(directionCombo));

        columnModel.getColumn(MergerTokenTableModel.COL_SELECT).setPreferredWidth(50);
        columnModel.getColumn(MergerTokenTableModel.COL_DIRECTION).setPreferredWidth(100);
        columnModel.getColumn(MergerTokenTableModel.COL_SELECT).setMaxWidth(50);
        columnModel.getColumn(MergerTokenTableModel.COL_DIRECTION).setMaxWidth(100);
    }

    public boolean isSelected(final MergerToken token) {
        return (selectableTokensList.contains(token) && !excludedTokens.contains(token));
    }

    public void select(final MergerToken token, final boolean select) {

        if (select) {
            excludedTokens.remove(token);
        } else {
            excludedTokens.add(token);
        }
    }

    public void setDirection(final MergerToken token, final MergeDirection direction) {
        if (token.getDirection().equals(direction)) {
            return;
        }

        final int i = selectableTokensList.indexOf(token);
        final MergerToken reverse = token.createReverse(mergerTokenFactory);
        selectableTokensList.set(i, reverse);
        if (excludedTokens.remove(token)) {
            excludedTokens.add(reverse);
        }
        
        // Repaint, so that "Operation" column updates properly
        view.getTokens().repaint();
    }

    public void checkAllAction() {
        stopEditing();

        final boolean isCheckAllSelected = view.getCheckAll().isSelected();

        if (isCheckAllSelected) {
            excludedTokens.clear();
        } else {
            excludedTokens.addAll(selectableTokensList);
        }

        final AbstractTableModel model = (AbstractTableModel) view.getTokens().getModel();
        model.fireTableDataChanged();
    }

    public boolean isReverse() {
        return isReverse;
    }

    public void reverseAllAction() {
        stopEditing();

        isReverse = !isReverse;

        for (int i = 0; i < selectableTokensList.size(); i++) {
            final MergerToken token = selectableTokensList.get(i);
            final MergerToken reverse = token.createReverse(mergerTokenFactory);
            selectableTokensList.set(i, reverse);
            if (excludedTokens.remove(token)) {
                excludedTokens.add(reverse);
            }
        }

        Collections.sort(selectableTokensList);
        final AbstractTableModel model = (AbstractTableModel) view.getTokens().getModel();
        model.fireTableDataChanged();
    }

    private void stopEditing() {
        // Stop cell editing before any action
        final TableCellEditor cellEditor = view.getTokens().getCellEditor();

        if (cellEditor != null) {
            cellEditor.stopCellEditing();
        }
    }
}
