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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import org.apache.cayenne.map.DbEntity;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * Controller of a dialog to select source, target and cardinality for DbRelationship  
 */
public class DbRelationshipTargetController extends BasicController {
    public static final String CONTINUE_CONTROL = "cayenne.modeler.mapObjRelationship.continue.button";
    public static final String CANCEL_CONTROL = "cayenne.modeler.mapObjRelationship.cancel.button";
    
    public static final String TOMANY_CONTROL = "cayenne.modeler.mapObjRelationship.tomany.checkbox";
    
    public static final String SOURCE1_CONTROL = "cayenne.modeler.mapObjRelationship.source1.button";
    public static final String SOURCE2_CONTROL = "cayenne.modeler.mapObjRelationship.source2.button";
    
    DbEntity source1;
    DbEntity source2;
    
    boolean savePressed;
    
    public DbRelationshipTargetController(DbEntity source1, DbEntity source2) {
        this.source1 = source1;
        this.source2 = source2;
        
        DbRelationshipTargetModel model = new DbRelationshipTargetModel(source1, source2);
        setModel(model);
    }
    
    @Override
    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(CONTINUE_CONTROL)) {
            save();
        }
    }
    
    protected void save() {
        DbRelationshipTargetModel model = (DbRelationshipTargetModel) getModel();
        
        DbEntity target = model.getTarget();        
        if (target == null) {
            JOptionPane.showMessageDialog((Component) getView(), "Please select target entity first.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        savePressed = true;
        shutdown();
    }
    
    /**
     * Creates and runs the dialog.
     */
    @Override
    public void startup() {
        DbRelationshipTargetDialog view = new DbRelationshipTargetDialog(source1, source2);
        setView(view);
        
        view.getSource1Button().addActionListener(
           new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((DbRelationshipTargetModel) getModel()).setSource(source1, true);
            }
           });
        
        view.getSource2Button().addActionListener(
           new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                ((DbRelationshipTargetModel) getModel()).setSource(source2, false);
             }
           });
        
        view.getSource1Button().setSelected(true);
        ((DbRelationshipTargetModel) getModel()).setSource(source1, true);
        
        super.startup();
    }
    
    boolean isSavePressed() {
        return savePressed;
    }
}
