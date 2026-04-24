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

package org.apache.cayenne.modeler.editor;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.event.display.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProcedureDisplayListener;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.modeler.event.model.ProcedureEvent;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.swing.components.JCayenneCheckBox;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.util.EventObject;

/**
 * A panel for editing stored procedure general settings, such as name, schema, etc.
 */
public class ProcedureTab extends JPanel implements ProcedureDisplayListener, ExistingSelectionProcessor {

    protected ProjectController eventController;
    protected TextAdapter name;
    protected TextAdapter schema;
    protected TextAdapter catalog;
    protected TextAdapter comment;
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
            @Override
            protected void updateModel(String text) {
                setProcedureName(text);
            }
        };

        this.schema = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) {
                setSchema(text);
            }
        };

        this.catalog = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) {
                setCatalog(text);
            }
        };

        this.comment = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) {
                setComment(text);
            }
        };

        this.returnsValue = new JCayenneCheckBox();
        this.returnsValue.setToolTipText("first parameter will be used as return value");

        FormLayout layout = new FormLayout("right:pref, 3dlu, fill:200dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("Stored Procedure Configuration");
        builder.append("Procedure Name:", name.getComponent());
        builder.append("Catalog:", catalog.getComponent());
        builder.append("Schema:", schema.getComponent());
        builder.append("Returns Value:", returnsValue);
        builder.append("Comment:", comment.getComponent());

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initController() {
        returnsValue.addItemListener(e -> {
            Procedure procedure = eventController.getSelectedProcedure();
            if (procedure != null && !ignoreChange) {
                procedure.setReturningValue(returnsValue.isSelected());
                eventController.fireProcedureEvent(new ProcedureEvent(ProcedureTab.this, procedure));
            }
        });

        eventController.addProcedureDisplayListener(this);
    }

    public void processExistingSelection(EventObject e) {
        ProcedureDisplayEvent pde = new ProcedureDisplayEvent(this, eventController.getSelectedProcedure(),
                eventController.getSelectedDataMap(), (DataChannelDescriptor) eventController.getProject().getRootNode());
        eventController.displayProcedure(pde);
    }

    /**
     * Invoked when currently selected Procedure object is changed.
     */
    public void procedureSelected(ProcedureDisplayEvent e) {
        Procedure procedure = e.getProcedure();
        if (procedure == null) {
            return;
        }

        name.setText(procedure.getName());
        schema.setText(procedure.getSchema());
        catalog.setText(procedure.getCatalog());
        comment.setText(getComment(procedure));

        ignoreChange = true;
        returnsValue.setSelected(procedure.isReturningValue());
        ignoreChange = false;
    }

    void setProcedureName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        Procedure procedure = eventController.getSelectedProcedure();

        if (procedure == null || Util.nullSafeEquals(newName, procedure.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Procedure name is required.");
        } else if (procedure.getDataMap().getProcedure(newName) == null) {
            // completely new name, set new name for entity
            String oldName = procedure.getName();
            ProcedureEvent e = new ProcedureEvent(this, procedure, oldName);
            DataMap map = procedure.getDataMap();
            procedure.setName(newName);
            map.removeProcedure(oldName);
            map.addProcedure(procedure);
            MappingNamespace ns = map.getNamespace();
            if (ns instanceof EntityResolver) {
                ((EntityResolver) ns).refreshMappingCache();
            }
            eventController.fireProcedureEvent(e);
        } else {
            // there is an entity with the same name
            throw new ValidationException("There is another procedure with name '" + newName + "'.");
        }
    }

    void setSchema(String text) {
        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        Procedure procedure = eventController.getSelectedProcedure();

        if (procedure != null && !Util.nullSafeEquals(procedure.getSchema(), text)) {
            procedure.setSchema(text);
            eventController.fireProcedureEvent(new ProcedureEvent(this, procedure));
        }
    }

    void setCatalog(String text) {
        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        Procedure procedure = eventController.getSelectedProcedure();

        if (procedure != null && !Util.nullSafeEquals(procedure.getCatalog(), text)) {
            procedure.setCatalog(text);
            eventController.fireProcedureEvent(new ProcedureEvent(this, procedure));
        }
    }

    void setComment(String comment) {
        Procedure procedure = eventController.getSelectedProcedure();

        if (procedure == null) {
            return;
        }

        ObjectInfo.putToMetaData(eventController.getApplication().getMetaData(), procedure, ObjectInfo.COMMENT, comment);
        eventController.fireProcedureEvent(new ProcedureEvent(this, procedure));
    }

    String getComment(Procedure procedure) {
        return ObjectInfo.getFromMetaData(eventController.getApplication().getMetaData(), procedure, ObjectInfo.COMMENT);
    }
}
