/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.dialog;

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

import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbAttribute;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.editor.DerivedAttributeParamsTableModel;
import org.objectstyle.cayenne.modeler.util.CayenneDialog;
import org.objectstyle.cayenne.modeler.util.CayenneTable;
import org.objectstyle.cayenne.modeler.util.CayenneTableModel;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.PanelFactory;

/**
 * Dialog window that alows selecting DbAttributes 
 * for derived attribute expression.
 *  
 * @author Andrei Adamchik
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
                AttributeEvent.CHANGE));

        hide();
    }

    protected void cancel() {
        hide();
    }
}
