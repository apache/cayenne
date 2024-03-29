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
package org.apache.cayenne.modeler.dialog.objentity;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.DefaultWidgetFactory;
import org.apache.cayenne.modeler.util.MultiColumnBrowser;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.WidgetFactory;

public class ObjRelationshipInfoView extends JDialog{
    
    private static final Dimension BROWSER_CELL_DIM = new Dimension(130, 200);

    private static final String[] DELETE_RULES = new String[]{
            DeleteRule.deleteRuleName(DeleteRule.NO_ACTION),
            DeleteRule.deleteRuleName(DeleteRule.NULLIFY),
            DeleteRule.deleteRuleName(DeleteRule.CASCADE),
            DeleteRule.deleteRuleName(DeleteRule.DENY),
    };
    
    private MultiColumnBrowser pathBrowser;

    private Component collectionTypeLabel;
    private JComboBox<String> collectionTypeCombo;
    private Component mapKeysLabel;
    private JComboBox<String> mapKeysCombo;

    private JButton saveButton;
    private JButton cancelButton;
    private JButton newRelButton;
    
    private JTextField relationshipName;
    private JLabel semanticsLabel;
    private JLabel sourceEntityLabel;
    private JComboBox<String> targetCombo;

    private JComboBox<String> deleteRule;
    private JCheckBox usedForLocking;
    private JTextField comment;

    public ObjRelationshipInfoView() {
        super(Application.getFrame());

        WidgetFactory widgetFactory = new DefaultWidgetFactory();
        
        this.cancelButton = new JButton("Cancel");
        this.saveButton = new JButton("Done");
        this.newRelButton = new JButton("New DbRelationship");
        this.relationshipName = new JTextField(25);
        this.semanticsLabel = new JLabel();
        this.sourceEntityLabel = new JLabel();
        
        cancelButton.setEnabled(true);
        getRootPane().setDefaultButton(saveButton);
        saveButton.setEnabled(true);
        newRelButton.setEnabled(true);
        collectionTypeCombo = widgetFactory.createComboBox();
        collectionTypeCombo.setVisible(true);
        this.targetCombo = widgetFactory.createComboBox();
        targetCombo.setVisible(true);
        
        this.mapKeysCombo = widgetFactory.createComboBox();
        mapKeysCombo.setVisible(true);
      
        
        pathBrowser = new ObjRelationshipPathBrowser();
        pathBrowser.setPreferredColumnSize(BROWSER_CELL_DIM);
        pathBrowser.setDefaultRenderer();

        this.deleteRule = Application.getWidgetFactory().createComboBox(DELETE_RULES, false);
        this.usedForLocking = new JCheckBox();
        this.comment = new JTextField();
        
        setTitle("ObjRelationship Inspector");
        setLayout(new BorderLayout());
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:max(50dlu;pref), 3dlu, fill:min(150dlu;pref), 3dlu, 300dlu, 3dlu, fill:min(120dlu;pref)",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, top:14dlu, 3dlu, top:p:grow"));
        builder.setDefaultDialogBorder();

        builder.addSeparator("ObjRelationship Information", cc.xywh(1, 1, 5, 1));

        builder.addLabel("Source Entity:", cc.xy(1, 3));
        builder.add(sourceEntityLabel, cc.xywh(3, 3, 1, 1));

        builder.addLabel("Target Entity:", cc.xy(1, 5));
        builder.add(targetCombo, cc.xywh(3, 5, 1, 1));

        builder.addLabel("Relationship Name:", cc.xy(1, 7));
        builder.add(relationshipName, cc.xywh(3, 7, 1, 1));

        builder.addLabel("Semantics:", cc.xy(1, 9));
        builder.add(semanticsLabel, cc.xywh(3, 9, 5, 1));

        collectionTypeLabel = builder.addLabel("Collection Type:", cc.xy(1, 11));
        builder.add(collectionTypeCombo, cc.xywh(3, 11, 1, 1));

        mapKeysLabel = builder.addLabel("Map Key:", cc.xy(1, 13));
        builder.add(mapKeysCombo, cc.xywh(3, 13, 1, 1));

        builder.addLabel("Delete rule:", cc.xy(1, 15));
        builder.add(deleteRule, cc.xywh(3, 15, 1, 1));

        builder.addLabel("Used for locking:", cc.xy(1, 17));
        builder.add(usedForLocking, cc.xywh(3, 17, 1, 1));

        builder.addLabel("Comment:", cc.xy(1, 19));
        builder.add(comment, cc.xywh(3, 19, 1, 1));

        builder.addSeparator("Mapping to DbRelationships", cc.xywh(1, 21, 5, 1));

        JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonsPane.add(newRelButton);

        builder.add(buttonsPane, cc.xywh(1, 23, 5, 1));
        builder.add(new JScrollPane(
                pathBrowser,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), cc.xywh(1, 25, 5, 3));

        add(builder.getPanel(), BorderLayout.CENTER);
        JButton[] buttons = {cancelButton, saveButton};
        add(PanelFactory.createButtonPanel(buttons), BorderLayout.SOUTH);
    }

    public JButton getSaveButton()
    {
        return saveButton;
    }
    
    public JButton getCancelButton()
    {
        return cancelButton;
    }
    
    public JButton getNewRelButton()
    {
        return newRelButton;
    }

    public JTextField getRelationshipName()
    {
        return relationshipName;
    }
    
    public JLabel getSemanticsLabel()
    {
        return semanticsLabel;
    }
    
    public JLabel getSourceEntityLabel() {
        return sourceEntityLabel;
    }
    
    public JComboBox<String> getTargetCombo()
    {
        return targetCombo;
    }
    
    public JComboBox<String> getCollectionTypeCombo() {
        return collectionTypeCombo;
    }
    
    public JComboBox<String> getMapKeysCombo() {
        return mapKeysCombo;
    }

    public JComboBox<String> getDeleteRule() {
        return deleteRule;
    }

    public JCheckBox getUsedForLocking() {
        return usedForLocking;
    }

    public JTextField getComment() {
        return comment;
    }

    public Component getMapKeysLabel() {
        return mapKeysLabel;
    }

    public Component getCollectionTypeLabel() {
        return collectionTypeLabel;
    }

    public MultiColumnBrowser getPathBrowser() {
        return pathBrowser;
    }
}
