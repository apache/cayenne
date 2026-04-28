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
package org.apache.cayenne.modeler.ui.project.editor.objentity.relinfo;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.MultiColumnBrowser;
import org.apache.cayenne.modeler.toolkit.buttons.CMButtonPanel;
import org.apache.cayenne.modeler.toolkit.combobox.CMComboBox;

import javax.swing.*;
import java.awt.*;

public class ObjRelationshipInfoView extends JDialog{
    
    private static final Dimension BROWSER_CELL_DIM = new Dimension(130, 200);

    private static final String[] DELETE_RULES = new String[]{
            DeleteRule.deleteRuleName(DeleteRule.NO_ACTION),
            DeleteRule.deleteRuleName(DeleteRule.NULLIFY),
            DeleteRule.deleteRuleName(DeleteRule.CASCADE),
            DeleteRule.deleteRuleName(DeleteRule.DENY),
    };
    
    private final MultiColumnBrowser pathBrowser;

    private final Component collectionTypeLabel;
    private final JComboBox<String> collectionTypeCombo;
    private final Component mapKeysLabel;
    private final JComboBox<String> mapKeysCombo;

    private final JButton saveButton;
    private final JButton cancelButton;
    private final JButton newRelButton;
    
    private final JTextField relationshipName;
    private final JLabel semanticsLabel;
    private final JLabel sourceEntityLabel;
    private final JComboBox<String> targetCombo;

    private final JComboBox<String> deleteRule;
    private final JCheckBox usedForLocking;
    private final JTextField comment;

    public ObjRelationshipInfoView(Application application) {
        super(application.getFrameController().getView());

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
        collectionTypeCombo = new CMComboBox<>();
        collectionTypeCombo.setVisible(true);
        this.targetCombo = new CMComboBox<>();
        targetCombo.setVisible(true);

        this.mapKeysCombo = new CMComboBox<>();
        mapKeysCombo.setVisible(true);
      
        
        pathBrowser = new ObjRelationshipPathBrowser();
        pathBrowser.setPreferredColumnSize(BROWSER_CELL_DIM);
        pathBrowser.setDefaultRenderer();

        this.deleteRule = new CMComboBox<>(DELETE_RULES);
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
        add(new CMButtonPanel(cancelButton, saveButton), BorderLayout.SOUTH);
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
