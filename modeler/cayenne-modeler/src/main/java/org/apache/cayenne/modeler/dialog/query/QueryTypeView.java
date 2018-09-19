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
package org.apache.cayenne.modeler.dialog.query;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.util.PanelFactory;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;

public class QueryTypeView extends JDialog {
    
    protected ButtonGroup buttonGroup;
    protected JRadioButton objectSelect;
    protected JRadioButton sqlSelect;
    protected JRadioButton procedureSelect;
    protected JRadioButton ejbqlSelect;
    protected JButton createButton;
    protected JButton cancelButton;
    
    public QueryTypeView() {
        initView();
    }
    
    private void initView() {
        
        // create widgets
        ButtonGroup buttonGroup = new ButtonGroup();
        objectSelect = new JRadioButton("Object Select Query");
        sqlSelect = new JRadioButton("SQLTemplate Query");
        procedureSelect = new JRadioButton("Stored Procedure Query");
        ejbqlSelect = new JRadioButton("EJBQL Query");
        objectSelect.setSelected(true);
        buttonGroup.add(objectSelect);
        buttonGroup.add(sqlSelect);
        buttonGroup.add(procedureSelect);
        buttonGroup.add(ejbqlSelect);

        createButton = new JButton("Create"); 
        createButton.setEnabled(true);

        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(true);

        getRootPane().setDefaultButton(createButton);

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "left:max(180dlu;pref)",
                "p, 4dlu, p, 4dlu, p, 4dlu, p, 4dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.add(objectSelect, cc.xy(1, 1));
        builder.add(sqlSelect, cc.xy(1, 3));
        builder.add(procedureSelect, cc.xy(1, 5));
        builder.add(ejbqlSelect, cc.xy(1, 7));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        add(PanelFactory.createButtonPanel(new JButton[] {
                cancelButton, createButton
        }), BorderLayout.SOUTH);

        setTitle("Select New Query Type");
    }
    
    public JButton getSaveButton()
    {
        return createButton;
    }
    
    public JButton getCancelButton()
    {
        return cancelButton;
    }
    
    public JRadioButton getObjectSelect()
    {
        return objectSelect;
    }
    
    public JRadioButton getSqlSelect()
    {
        return sqlSelect;
    }
    
    public JRadioButton getProcedureSelect()
    {
        return procedureSelect;
    }
    
    public JRadioButton getEjbqlSelect()
    {
        return ejbqlSelect;
    }
}
