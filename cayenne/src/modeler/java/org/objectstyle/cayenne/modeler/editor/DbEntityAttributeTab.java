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
package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbAttribute;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.map.event.DbAttributeListener;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.EditDerivedParamsDialog;
import org.objectstyle.cayenne.modeler.event.AttributeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DbEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.util.CayenneTable;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.PanelFactory;
import org.objectstyle.cayenne.modeler.util.UIUtil;

/** 
 * Detail view of the DbEntity attributes. 
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class DbEntityAttributeTab
    extends JPanel
    implements
        DbEntityDisplayListener,
        ListSelectionListener,
        DbAttributeListener,
        ExistingSelectionProcessor,
        ActionListener {

    protected ProjectController mediator;
    protected CayenneTable table;
    protected JButton editParams;

    public DbEntityAttributeTab(ProjectController temp_mediator) {
        super();
        mediator = temp_mediator;
        mediator.addDbEntityDisplayListener(this);
        mediator.addDbAttributeListener(this);

        // Create and layout components
        init();

        editParams.addActionListener(this);
    }

    private void init() {
        setLayout(new BorderLayout());

        // Create table with two columns and no rows.
        table = new CayenneTable();
        editParams = new JButton("Edit Parameters");
        JPanel panel = PanelFactory.createTablePanel(table, new JButton[] { editParams });
        add(panel, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == editParams) {
            int row = table.getSelectedRow();
            if (row >= 0) {
                DbAttribute attr =
                    ((DbAttributeTableModel) table.getModel()).getAttribute(row);

                EditDerivedParamsDialog dialog =
                    new EditDerivedParamsDialog((DerivedDbAttribute) attr);
                dialog.show();
                dialog.dispose();
            }
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        processExistingSelection();
    }

    /**
      * Selects a specified attribute.
      */
    public void selectAttribute(DbAttribute attr) {
        if (attr == null) {
            return;
        }

        DbAttributeTableModel model = (DbAttributeTableModel) table.getModel();
        java.util.List attrs = model.getObjectList();
        int attrPos = attrs.indexOf(attr);
        if (attrPos >= 0) {
            table.select(attrPos);
        }
    }

    public void processExistingSelection() {
        DbAttribute att = null;
        if (table.getSelectedRow() >= 0) {
            DbAttributeTableModel model = (DbAttributeTableModel) table.getModel();
            att = model.getAttribute(table.getSelectedRow());
            editParams.setEnabled(att instanceof DerivedDbAttribute);

            // scroll table
            UIUtil.scrollToSelectedRow(table);
        }

        mediator.fireDbAttributeDisplayEvent(
            new AttributeDisplayEvent(
                this,
                att,
                mediator.getCurrentDbEntity(),
                mediator.getCurrentDataMap(),
                mediator.getCurrentDataDomain()));
    }

    public void dbAttributeChanged(AttributeEvent e) {
        table.select(e.getAttribute());
    }

    public void dbAttributeAdded(AttributeEvent e) {
        rebuildTable((DbEntity) e.getEntity());
        table.select(e.getAttribute());
    }

    public void dbAttributeRemoved(AttributeEvent e) {
        DbAttributeTableModel model = (DbAttributeTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getAttribute());
        model.removeRow(e.getAttribute());
        table.select(ind);
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        DbEntity entity = (DbEntity) e.getEntity();
        if (entity != null && e.isEntityChanged()) {
            rebuildTable(entity);
        }

        // if an entity was selected on a tree, 
        // unselect currently selected row
        if (e.isUnselectAttributes()) {
            table.clearSelection();
        }
    }

    protected void rebuildTable(DbEntity ent) {
        editParams.setVisible(ent instanceof DerivedDbEntity);
        editParams.setEnabled(false);

        DbAttributeTableModel model =
            (ent instanceof DerivedDbEntity)
                ? new DerivedDbAttributeTableModel(ent, mediator, this)
                : new DbAttributeTableModel(ent, mediator, this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);
        TableColumn col = table.getColumnModel().getColumn(model.nameColumnInd());
        col.setMinWidth(150);

        col = table.getColumnModel().getColumn(model.typeColumnInd());
        col.setMinWidth(90);

        String[] types = TypesMapping.getDatabaseTypes();
        JComboBox comboBox = CayenneWidgetFactory.createComboBox(types, true);
        comboBox.setEditable(true);
        col.setCellEditor(new DefaultCellEditor(comboBox));

        table.getSelectionModel().addListSelectionListener(this);
    }
}
