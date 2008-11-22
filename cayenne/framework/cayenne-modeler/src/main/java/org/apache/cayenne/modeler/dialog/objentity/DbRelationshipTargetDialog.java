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
package org.apache.cayenne.modeler.dialog.objentity;

import java.awt.BorderLayout;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.scopemvc.view.swing.SAction;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SCheckBox;
import org.scopemvc.view.swing.SComboBox;
import org.scopemvc.view.swing.SListCellRenderer;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.SwingView;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog to select source, target and cardinality for DbRelationship  
 */
public class DbRelationshipTargetDialog extends SPanel {
    JRadioButton source1Button;
    
    JRadioButton source2Button;
    
    public DbRelationshipTargetDialog(DbEntity source1, DbEntity source2) {
        init(source1, source2);
    }
    
    protected void init(DbEntity source1, DbEntity source2) {
        // create widgets
        SButton saveButton = new SButton(new SAction(
                DbRelationshipTargetController.CONTINUE_CONTROL));
        saveButton.setEnabled(true);

        SButton cancelButton = new SButton(new SAction(
                DbRelationshipTargetController.CANCEL_CONTROL));
        cancelButton.setEnabled(true);
        
        SComboBox targetCombo = new SComboBox();
        targetCombo.setSelector(DbRelationshipTargetModel.TARGETS_SELECTOR);
        targetCombo.setSelectionSelector(DbRelationshipTargetModel.TARGET_SELECTOR);
        SListCellRenderer renderer = (SListCellRenderer) targetCombo.getRenderer();
        renderer.setTextSelector("name");
        
        source1Button = new JRadioButton();
        source2Button = new JRadioButton();
        source2Button.setEnabled(source2 != null);
        
        ButtonGroup bg = new ButtonGroup();
        bg.add(source1Button);
        bg.add(source2Button);
        
        SCheckBox toManyCheckBox = new SCheckBox();
        toManyCheckBox.setSelector(DbRelationshipTargetModel.TO_MANY_SELECTOR);
        
        setDisplayMode(SwingView.MODAL_DIALOG);
        setTitle("Create New DbRelationship");
        setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:max(100dlu;pref), 3dlu, fill:min(150dlu;pref)",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, top:p:grow"));
        builder.setDefaultDialogBorder();
        
        builder.addLabel("Source: " + source1.getName(), cc.xy(1, 1));
        builder.add(source1Button, cc.xy(3, 1));
        
        builder.addLabel("Source: " + (source2 == null ? "" : source2.getName()), cc.xy(1, 3));
        builder.add(source2Button, cc.xy(3, 3));
        
        builder.addLabel("Target:", cc.xy(1, 5));
        builder.add(targetCombo, cc.xywh(3, 5, 1, 1));
        
        builder.addLabel("To Many:", cc.xy(1, 7));
        builder.add(toManyCheckBox, cc.xywh(3, 7, 1, 1));
        
        add(builder.getPanel(), BorderLayout.CENTER);
        add(PanelFactory.createButtonPanel(new JButton[] {
                saveButton, cancelButton
            }), BorderLayout.SOUTH);
    }
    
    JRadioButton getSource1Button() {
        return source1Button;
    }
    
    JRadioButton getSource2Button() {
        return source2Button;
    }
}
