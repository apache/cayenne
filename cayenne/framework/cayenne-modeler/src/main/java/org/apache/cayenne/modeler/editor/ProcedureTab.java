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

package org.apache.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventObject;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.event.ProcedureEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayListener;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel for editing stored procedure general settings, such as name, schema, etc.
 * 
 */
public class ProcedureTab extends JPanel implements ProcedureDisplayListener,
        ExistingSelectionProcessor {

    protected ProjectController eventController;
    protected TextAdapter name;
    protected TextAdapter schema;
    protected JCheckBox returnsValue;
    protected boolean ignoreChange;

    public ProcedureTab(ProjectController eventController) {
        this.eventController = eventController;

        initView();
        initController();
    }

    private void initView() {
        // create widgets

        this.name = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setProcedureName(text);
            }
        };

        this.schema = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setSchema(text);
            }
        };

        this.returnsValue = new JCheckBox();

        JLabel returnValueHelp = new JLabel("(first parameter will be used as return value)");
        returnValueHelp.setFont(returnValueHelp.getFont().deriveFont(10));

        // assemble
        FormLayout layout = new FormLayout(
                "right:max(50dlu;pref), 3dlu, left:max(20dlu;pref), 3dlu, fill:150dlu",
                "p, 3dlu, p, 3dlu, p, 3dlu, p");

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("Stored Procedure Configuration", cc.xywh(1, 1, 5, 1));
        builder.addLabel("Procedure Name:", cc.xy(1, 3));
        builder.add(name.getComponent(), cc.xywh(3, 3, 3, 1));
        builder.addLabel("Schema:", cc.xy(1, 5));
        builder.add(schema.getComponent(), cc.xywh(3, 5, 3, 1));
        builder.addLabel("Returns Value:", cc.xy(1, 7));
        builder.add(returnsValue, cc.xy(3, 7));
        builder.add(returnValueHelp, cc.xy(5, 7));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initController() {
        returnsValue.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                Procedure procedure = eventController.getCurrentProcedure();
                if (procedure != null && !ignoreChange) {
                    procedure.setReturningValue(returnsValue.isSelected());
                    eventController.fireProcedureEvent(new ProcedureEvent(
                            ProcedureTab.this,
                            procedure));
                }
            }
        });

        eventController.addProcedureDisplayListener(this);
    }
    
    public void processExistingSelection(EventObject e) {
        ProcedureDisplayEvent pde = new ProcedureDisplayEvent(
                this,
                eventController.getCurrentProcedure(),
                eventController.getCurrentDataMap(),
                eventController.getCurrentDataDomain());
        eventController.fireProcedureDisplayEvent(pde);
    }

    /**
     * Invoked when currently selected Procedure object is changed.
     */
    public synchronized void currentProcedureChanged(ProcedureDisplayEvent e) {
        Procedure procedure = e.getProcedure();
        if (procedure == null || !e.isProcedureChanged()) {
            return;
        }

        name.setText(procedure.getName());
        schema.setText(procedure.getSchema());

        ignoreChange = true;
        returnsValue.setSelected(procedure.isReturningValue());
        ignoreChange = false;
    }

    void setProcedureName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        Procedure procedure = eventController.getCurrentProcedure();

        if (procedure == null || Util.nullSafeEquals(newName, procedure.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Procedure name is required.");
        }
        else if (procedure.getDataMap().getProcedure(newName) == null) {
            // completely new name, set new name for entity
            ProcedureEvent e = new ProcedureEvent(this, procedure, procedure.getName());
            ProjectUtil.setProcedureName(procedure.getDataMap(), procedure, newName);
            eventController.fireProcedureEvent(e);
        }
        else {
            // there is an entity with the same name
            throw new ValidationException("There is another procedure with name '"
                    + newName
                    + "'.");
        }
    }

    void setSchema(String text) {
        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        Procedure procedure = eventController.getCurrentProcedure();

        if (procedure != null && !Util.nullSafeEquals(procedure.getSchema(), text)) {
            procedure.setSchema(text);
            eventController.fireProcedureEvent(new ProcedureEvent(this, procedure));
        }
    }
}
