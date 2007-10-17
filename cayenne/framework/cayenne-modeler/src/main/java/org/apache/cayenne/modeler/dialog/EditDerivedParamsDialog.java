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

package org.apache.cayenne.modeler.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DerivedDbAttribute;
import org.apache.cayenne.map.DerivedDbEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.editor.dbentity.DerivedAttributeParamsTableModel;
import org.apache.cayenne.modeler.util.CayenneDialog;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.PanelFactory;

/**
 * Dialog window that alows selecting DbAttributes 
 * for derived attribute expression.
 *  
 * @author Andrus Adamchik
 * @deprecated since 3.0M2 (scheduled for removal in 3.0M3).
 */
public class EditDerivedParamsDialog extends CayenneDialog implements ActionListener {

    protected DerivedDbAttribute attr;

    protected JTable table = new CayenneTable();
    protected JButton add = new JButton("Add");
    protected JButton remove = new JButton("Remove");
    protected JButton save = new JButton("Save");
    protected JButton cancel = new JButton("Cancel");

    /**
     * Constructor for EditDerivedParamsDialog.
     */
    public EditDerivedParamsDialog(DerivedDbAttribute attr) {
        super(Application.getFrame(), "Edit Derived Attribute Parameters", true);

        this.attr = attr;

        init();
        pack();
        centerWindow();
    }

    protected void init() {
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());

        buildTable();

        JPanel panel =
            PanelFactory.createTablePanel(
                table,
                new JButton[] { add, remove, save, cancel });
        pane.add(panel, BorderLayout.CENTER);

        add.addActionListener(this);
        remove.addActionListener(this);
        save.addActionListener(this);
        cancel.addActionListener(this);
    }

    protected void buildTable() {
        DerivedAttributeParamsTableModel model =
            new DerivedAttributeParamsTableModel(attr, getMediator(), this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);
        TableColumn nameCol = table.getColumnModel().getColumn(model.nameColumnInd());
        nameCol.setMinWidth(150);

        TableColumn typeCol = table.getColumnModel().getColumn(model.typeColumnInd());
        typeCol.setMinWidth(90);

        DbEntity parent = ((DerivedDbEntity) attr.getEntity()).getParentEntity();

        List list = new ArrayList(32);
        list.add("");
        list.addAll(parent.getAttributeMap().keySet());
        String[] names = (String[]) (list.toArray(new String[list.size()]));

        JComboBox comboBox = CayenneWidgetFactory.createComboBox(names, true);
        comboBox.setEditable(false);
        nameCol.setCellEditor(new DefaultCellEditor(comboBox));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == add) {
            addRow();
        }
        else if (src == remove) {
            removeRow();
        }
        else if (src == save) {
            save();
        }
        else if (src == cancel) {
            cancel();
        }
    }

    protected void removeRow() {
        DerivedAttributeParamsTableModel model =
            (DerivedAttributeParamsTableModel) table.getModel();
        model.removeRow(model.getAttribute(table.getSelectedRow()));
    }

    protected void addRow() {
        ((CayenneTableModel) table.getModel()).addRow(null);
    }

    protected void save() {
        // update parameters of the derived attribute
        attr.clearParams();
        Iterator it = ((CayenneTableModel) table.getModel()).getObjectList().iterator();
        while (it.hasNext()) {
            DbAttribute at = (DbAttribute) it.next();
            attr.addParam(at);
        }

        // notify interested parties about the changes 
        getMediator().fireDbAttributeEvent(
            new AttributeEvent(
                this,
                attr,
                attr.getEntity(),
                MapEvent.CHANGE));

        setVisible(false);
    }

    protected void cancel() {
        setVisible(false);
    }
}
