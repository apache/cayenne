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

package org.apache.cayenne.modeler;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.util.CayenneDialog;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.PanelFactory;

/**
 * @since 4.2
 */
public class DbRelationshipDialogView extends CayenneDialog {

    private JTextField name;
    private JComboBox<String> targetEntities;
    private JCheckBox fK;
    private JCheckBox toMany;
    private JTextField comment;
    private JLabel sourceName;
    private JTextField reverseName;
    private CayenneTable table;
    private TableColumnPreferences tablePreferences;
    private JButton addButton;
    private JButton removeButton;
    private  JButton saveButton;
    private JButton cancelButton;

    private boolean cancelPressed;

    public DbRelationshipDialogView() {
        super(Application.getFrame(), "Create dbRelationship", true);

        initView();
        this.pack();
        this.centerWindow();
    }

    private void initView() {
        name = new JTextField(25);
        targetEntities = new JComboBox<>();
        fK = new JCheckBox();
        toMany = new JCheckBox();
        comment = new JTextField(25);

        sourceName = new JLabel();
        reverseName = new JTextField(25);

        addButton = new JButton("Add");

        removeButton = new JButton("Remove");

        saveButton = new JButton("Done");

        cancelButton = new JButton("Cancel");

        table = new AttributeTable();

        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePreferences = new TableColumnPreferences(getClass(), "dbentity/dbjoinTable");

        getRootPane().setDefaultButton(saveButton);

        getContentPane().setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:max(50dlu;pref), 3dlu, fill:min(150dlu;pref), 3dlu, fill:min(50dlu;pref)",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, " +
                                "p, 3dlu, p, 9dlu, p, 3dlu, top:14dlu, 3dlu, top:p:grow"));
        builder.setDefaultDialogBorder();

        builder.addSeparator("Create dbRelationship", cc.xywh(1, 1, 5, 1));

        builder.addLabel("Relationship Name:", cc.xy(1, 3));
        builder.add(name, cc.xywh(3, 3, 1, 1));

        builder.addLabel("Source Entity:", cc.xy(1, 5));
        builder.add(sourceName, cc.xywh(3, 5, 1, 1));

        builder.addLabel("Target Entity:", cc.xy(1, 7));
        builder.add(targetEntities, cc.xywh(3, 7, 1, 1));

        builder.addLabel("Foreign key:", cc.xy(1, 9));
        builder.add(fK, cc.xywh(3, 9, 1, 1));

        builder.addLabel("To Many:", cc.xy(1, 11));
        builder.add(toMany, cc.xywh(3, 11, 1, 1));

        builder.addLabel("Comment:", cc.xy(1, 13));
        builder.add(comment, cc.xywh(3, 13, 1, 1));

        builder.addSeparator("DbRelationship Information", cc.xywh(1, 15, 5, 1));

        builder.addLabel("Reverse Relationship Name:", cc.xy(1, 17));
        builder.add(reverseName, cc.xywh(3, 17, 1, 1));

        builder.addSeparator("Joins", cc.xywh(1, 19, 5, 1));
        builder.add(new JScrollPane(table), cc.xywh(1, 21, 3, 3, "fill, fill"));

        JPanel joinButtons = new JPanel(new FlowLayout(FlowLayout.LEADING));
        joinButtons.add(addButton);
        joinButtons.add(removeButton);

        builder.add(joinButtons, cc.xywh(5, 21, 1, 3));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        JButton[] buttons = {cancelButton, saveButton};
        getContentPane().add(PanelFactory.createButtonPanel(buttons), BorderLayout.SOUTH);
    }

    public void enableOptions(boolean enable) {
        saveButton.setEnabled(enable);
        reverseName.setEnabled(enable);
        addButton.setEnabled(enable);
        removeButton.setEnabled(enable);
    }

    @Override
    public void setVisible(boolean b) {
        if(b && cancelPressed) {
            return;
        }
        super.setVisible(b);
    }

    public JTextField getNameField() {
        return name;
    }

    public JComboBox<String> getTargetEntities() {
        return targetEntities;
    }

    public JCheckBox getFK() {
        return fK;
    }

    public JCheckBox getToMany() {
        return toMany;
    }

    public JTextField getComment() {
        return comment;
    }

    public JLabel getSourceName() {
        return sourceName;
    }

    public JTextField getReverseName() {
        return reverseName;
    }

    public CayenneTable getTable() {
        return table;
    }

    public TableColumnPreferences getTablePreferences() {
        return tablePreferences;
    }

    public JButton getAddButton() {
        return addButton;
    }

    public JButton getRemoveButton() {
        return removeButton;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public boolean isCancelPressed() {
        return cancelPressed;
    }

    public void setCancelPressed(boolean cancelPressed) {
        this.cancelPressed = cancelPressed;
    }

    final class AttributeTable extends CayenneTable {

        final Dimension preferredSize = new Dimension(203, 100);

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return preferredSize;
        }
    }

}
