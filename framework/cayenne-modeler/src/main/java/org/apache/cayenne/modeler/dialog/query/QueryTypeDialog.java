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

import java.awt.BorderLayout;

import javax.swing.ButtonGroup;
import javax.swing.JButton;

import org.apache.cayenne.modeler.util.PanelFactory;
import org.scopemvc.view.swing.SAction;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.SRadioButton;
import org.scopemvc.view.swing.SwingView;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A dialog to select a type of newly created query.
 * 
 */
public class QueryTypeDialog extends SPanel {
    
     public QueryTypeDialog() {
        initView();
    }

    private void initView() {
        // create widgets
     ButtonGroup buttonGroup = new ButtonGroup();
       
     SRadioButton objectSelect = new SRadioButton(
                QueryTypeController.OBJECT_QUERY_CONTROL,
                QueryTypeModel.OBJECT_SELECT_QUERY_SELECTOR);
        
      SRadioButton sqlSelect = new SRadioButton(
      QueryTypeController.SQL_QUERY_CONTROL,
      QueryTypeModel.RAW_SQL_QUERY_SELECTOR);
        
      SRadioButton procedureSelect = new SRadioButton(
      QueryTypeController.PROCEDURE_QUERY_CONTROL,
      QueryTypeModel.PROCEDURE_QUERY_SELECTOR);
        
      SRadioButton ejbqlSelect = new SRadioButton(
              QueryTypeController.EJBQL_QUERY_CONTROL,
              QueryTypeModel.EJBQL_QUERY_SELECTOR);
      
        buttonGroup.add(objectSelect);
        buttonGroup.add(sqlSelect);
        buttonGroup.add(procedureSelect);
        buttonGroup.add(ejbqlSelect);
       
        SButton saveButton = new SButton(new SAction(QueryTypeController.CREATE_CONTROL));
        saveButton.setEnabled(true);

        SButton cancelButton = new SButton(
                new SAction(QueryTypeController.CANCEL_CONTROL));
        cancelButton.setEnabled(true);
 
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
                saveButton, cancelButton
        }), BorderLayout.SOUTH);

        // decorate
        setDisplayMode(SwingView.MODAL_DIALOG);
        setTitle("Select New Query Type");
    }
}


