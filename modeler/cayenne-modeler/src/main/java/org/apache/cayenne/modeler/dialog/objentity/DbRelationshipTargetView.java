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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.DefaultWidgetFactory;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.WidgetFactory;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;

public class DbRelationshipTargetView extends JDialog {
    
    protected WidgetFactory widgetFactory;
    protected JCheckBox toManyCheckBox ;
    protected JButton saveButton;
    protected JButton cancelButton;
    protected JRadioButton source1Button;
    protected JRadioButton source2Button;
    protected JComboBox<DbEntity> targetCombo;
    
    public DbRelationshipTargetView(DbEntity source1, DbEntity source2) {
        super(Application.getFrame());

        widgetFactory = new DefaultWidgetFactory();
        
        // create widgets
        saveButton = new JButton("Continue");
        saveButton.setEnabled(true);

        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(true);
                
        targetCombo = widgetFactory.createComboBox();
        targetCombo.setVisible(true);
              
        source1Button = new JRadioButton();
        source2Button = new JRadioButton();
        source2Button.setEnabled(source2 != null);

        getRootPane().setDefaultButton(saveButton);

        ButtonGroup bg = new ButtonGroup();
        bg.add(source1Button);
        bg.add(source2Button);
        
        toManyCheckBox = new JCheckBox();
        
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
        JButton[] buttons = {cancelButton, saveButton};
        add(PanelFactory.createButtonPanel(buttons), BorderLayout.SOUTH);
    }
    
    public JRadioButton getSource1Button() {
        return source1Button;
    }
    
    public JRadioButton getSource2Button() {
        return source2Button;
    }
    
    public JButton getSaveButton()
    {
        return saveButton;
    }
    
    public JButton getCancelButton()
    {
        return cancelButton;
    }
    
    public JCheckBox getToManyCheckBox() {
        return toManyCheckBox;
    }
}
