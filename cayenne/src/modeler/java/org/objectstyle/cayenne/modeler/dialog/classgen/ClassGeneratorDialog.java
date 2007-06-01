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
package org.objectstyle.cayenne.modeler.dialog.classgen;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.objectstyle.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.scopemvc.core.PropertyManager;
import org.scopemvc.core.Selector;
import org.scopemvc.view.swing.SAction;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SCheckBox;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.STable;
import org.scopemvc.view.swing.STableModel;
import org.scopemvc.view.swing.STextField;
import org.scopemvc.view.swing.SwingView;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for generating Java classes from the DataMap.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class ClassGeneratorDialog extends SPanel {

    protected STable table;

    public ClassGeneratorDialog() {

        // create widgets
        final STextField superClassPackage = new STextField(30);
        superClassPackage.setSelector("superClassPackage");

        final STextField superClassTemplate = new STextField(30);
        superClassTemplate.setSelector("customSuperclassTemplate");

        STextField classTemplate = new STextField(30);
        classTemplate.setSelector("customClassTemplate");

        STextField folder = new STextField(30);
        folder.setSelector("outputDir");

        SButton chooseButton = new SButton(new SAction(
                ClassGeneratorController.CHOOSE_LOCATION_CONTROL));
        chooseButton.setEnabled(true);

        SButton chooseTemplateButton = new SButton(new SAction(
                ClassGeneratorController.CHOOSE_TEMPLATE_CONTROL));
        chooseTemplateButton.setEnabled(true);

        final SButton chooseSuperTemplateButton = new SButton(new SAction(
                ClassGeneratorController.CHOOSE_SUPERTEMPLATE_CONTROL));
        chooseSuperTemplateButton.setEnabled(true);

        SButton selectAllButton = new SButton(new SAction(
                ClassGeneratorController.SELECT_ALL_CONTROL));
        selectAllButton.setEnabled(true);

        SButton generateButton = new SButton(new SAction(
                ClassGeneratorController.GENERATE_CLASSES_CONTROL));
        generateButton.setEnabled(true);

        SButton cancelButton = new SButton(new SAction(
                ClassGeneratorController.CANCEL_CONTROL));
        cancelButton.setEnabled(true);

        SCheckBox generateSuperclass = new SCheckBox() {

            // (de)activate the text fields
            public void itemStateChanged(ItemEvent inEvent) {
                boolean enabled = inEvent.getStateChange() == ItemEvent.SELECTED;
                superClassPackage.setEnabled(enabled);
                superClassTemplate.setEnabled(enabled);
                chooseSuperTemplateButton.setEnabled(enabled);

                super.itemStateChanged(inEvent);
            }
        };
        generateSuperclass.setSelector("pairs");

        // **** build entity table
        table = new ClassGeneratorTable();
        table.setRowHeight(25);
        table.setRowMargin(3);
        ClassGeneratorModel model = new ClassGeneratorModel(table);
        model.setSelector("entities");
        model.setColumnNames(new String[] {
                "Entity", "Class", "Generate", "Problems"
        });
        model.setColumnSelectors(new String[] {
                "entity.name", "entity.className", "selected", "validationMessage"
        });

        table.setModel(model);
        table.getColumnModel().getColumn(2).setPreferredWidth(30);

        // assemble..

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:max(50dlu;pref), 3dlu, fill:pref:grow, 3dlu, left:70",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, fill:100dlu:grow"));
        builder.setDefaultDialogBorder();

        builder.addSeparator("Generated Class Settings", cc.xywh(1, 1, 5, 1));
        builder.addLabel("Output Directory:", cc.xy(1, 3));
        builder.add(folder, cc.xy(3, 3));
        builder.add(chooseButton, cc.xy(5, 3));
        builder.addLabel("Custom Template:", cc.xy(1, 5));
        builder.add(classTemplate, cc.xy(3, 5));
        builder.add(chooseTemplateButton, cc.xy(5, 5));
        builder.addSeparator("Generated Superclass Settings", cc.xywh(1, 7, 5, 1));
        builder.addLabel("Generate Superclass:", cc.xy(1, 9));
        builder.add(generateSuperclass, cc.xywh(3, 9, 3, 1));
        builder.addLabel("Superclass Package:", cc.xy(1, 11));
        builder.add(superClassPackage, cc.xy(3, 11));
        builder.addLabel("Custom Template:", cc.xy(1, 13));
        builder.add(superClassTemplate, cc.xy(3, 13));
        builder.add(chooseSuperTemplateButton, cc.xy(5, 13));

        builder.add(new JScrollPane(
                table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), cc.xywh(1, 15, 5, 1));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(selectAllButton);
        buttons.add(Box.createHorizontalStrut(20));
        buttons.add(cancelButton);
        buttons.add(generateButton);

        setDisplayMode(SwingView.MODAL_DIALOG);
        setTitle("Generate Java Classes");
        setLayout(new BorderLayout());

        add(builder.getPanel(), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    public STable getTable() {
        return table;
    }

    class ClassGeneratorModel extends STableModel {

        Selector enabledSelector = Selector.fromString("enabled");

        /**
         * Constructor for TableModel.
         * 
         * @param table
         */
        public ClassGeneratorModel(JTable table) {
            super(table);
        }

        public boolean isEnabledRow(int rowIndex) {
            // check if this is a failed row
            Object row = getElementAt(rowIndex);
            PropertyManager manager = getItemsManager();
            if (manager == null || row == null) {
                return false;
            }

            try {
                Boolean enabled = (Boolean) manager.get(row, enabledSelector);
                return enabled != null && enabled.booleanValue();
            }
            catch (Exception e) {
                return false;
            }
        }

        /**
         * @see javax.swing.table.TableModel#isCellEditable(int, int)
         */
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // only checkbox is editable
            if (columnIndex != 2) {
                return false;
            }

            return isEnabledRow(rowIndex);
        }
    }

    class ClassGeneratorTable extends STable {

        final Dimension preferredSize = new Dimension(500, 300);

        DefaultTableCellRenderer problemRenderer;

        ClassGeneratorTable() {
            problemRenderer = new ClassGeneratorProblemRenderer();
            problemRenderer.setBackground(ValidatorDialog.WARNING_COLOR);
        }

        public TableCellRenderer getCellRenderer(int row, int column) {
            ClassGeneratorModel model = (ClassGeneratorModel) getModel();

            return (model.isEnabledRow(row))
                    ? super.getCellRenderer(row, column)
                    : problemRenderer;
        }

        public Dimension getPreferredScrollableViewportSize() {
            return preferredSize;
        }
    }

    class ClassGeneratorProblemRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            if (value instanceof Boolean) {
                value = "";
            }

            return super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);
        }

    }

}