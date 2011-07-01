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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.Comparators;

public class DbRelationshipTarget extends CayenneController{
    
    private DbEntity source1;
    private DbEntity source2;
    protected DbEntity relTarget;
    protected List<DbEntity> relTargets;
    
    protected DbEntity source;
    protected ProjectController mediator;
    protected boolean source1Selected;
    protected DbRelationshipTargetView view;
    protected boolean toMany;
    protected boolean savePressed;
    
    @SuppressWarnings("unchecked")
    public DbRelationshipTarget(ProjectController mediator,DbEntity source1, DbEntity source2) {
        super(mediator);
        view = new DbRelationshipTargetView(source1, source2);
        initController();
        view.getSource1Button().setSelected(true);
        view.getToManyCheckBox().setSelected(false);
        setSource(source1, true);
        this.mediator = mediator;
        this.source1 = source1;
        this.source2 = source2;
        this.relTargets = new ArrayList<DbEntity>(source1.getDataMap().getDbEntities());
        Collections.sort(relTargets, Comparators.getNamedObjectComparator());
        view.targetCombo.removeAllItems();
        for (DbEntity d : relTargets) {
            view.targetCombo.addItem(d.getName());
        }
    }
        
    private void initController() {
        view.getCancelButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                view.dispose();
            }
        });
        view.getSaveButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        view.getSource1Button().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSource(source1, true);
            }
        });     
        view.getSource2Button().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSource(source2, false);
            }
        });   
        view.getToManyCheckBox().addChangeListener(new ChangeListener() {
            
            public void stateChanged(ChangeEvent e) {
                if (view.getToManyCheckBox().isSelected()) {
                    toMany = true;
                }
                else {
                    toMany = false;
                }
            }
        });
    }
    
    @Override
    public Component getView() {
        return view;
    }
    
    protected void save() {
             
        this.relTarget = relTargets.get(view.targetCombo.getSelectedIndex());   
        DbEntity target = getTarget();
             
        if (target == null) {
            JOptionPane.showMessageDialog((Component) getView(), "Please select target entity first.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        savePressed = true;
        view.dispose();
    }
    
    /**
     * Creates and runs the dialog.
     */
    public void startupAction() {
        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }
    
    boolean isSavePressed() {
        return savePressed;
    }
    
    public List<DbEntity> getTargets() {
        return relTargets;
    }
    
    public DbEntity getTarget() {
        return relTarget;
    }
    
    public void setTarget(DbEntity newRelTarget) {
        this.relTarget = newRelTarget;
    }
    
    public boolean isSource1Selected() {
        return source1Selected;
    }
    
    public DbEntity getSource() {
        return source;
    }
    
    public void setSource(DbEntity source, boolean source1) {
        this.source = source;
        this.source1Selected = source1;
    }
    
    public boolean isToMany() {
        return toMany;
    }
}
