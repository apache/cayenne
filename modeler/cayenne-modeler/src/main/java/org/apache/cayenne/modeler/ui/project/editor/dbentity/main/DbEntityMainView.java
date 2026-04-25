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

package org.apache.cayenne.modeler.ui.project.editor.dbentity.main;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityFromDbAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.DbEntityCounterpartAction;
import org.apache.cayenne.modeler.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.ui.project.editor.query.ExistingSelectionProcessor;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.swing.text.CayenneUndoableTextField;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.graph.action.ShowGraphEntityAction;
import org.apache.cayenne.modeler.util.ExpressionConvertor;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.util.EventObject;

public class DbEntityMainView extends JPanel implements ExistingSelectionProcessor, DbEntityDisplayListener {

    static final String PK_DEFAULT_GENERATOR = "Cayenne-Generated (Default)";
    static final String PK_DB_GENERATOR = "Database-Generated";
    static final String PK_CUSTOM_SEQUENCE_GENERATOR = "Custom Sequence";

    static final String[] PK_GENERATOR_TYPES = { PK_DEFAULT_GENERATOR, PK_DB_GENERATOR, PK_CUSTOM_SEQUENCE_GENERATOR };

    private final ProjectController controller;

    private final TextAdapter name;
    private final CayenneUndoableTextField catalog;
    private final CayenneUndoableTextField schema;
    private final TextAdapter qualifier;
    private final CayenneUndoableTextField comment;

    private final JLabel catalogLabel;
    private final JLabel schemaLabel;

    private final JComboBox<String> pkGeneratorType;
    private final JPanel pkGeneratorDetail;
    private final CardLayout pkGeneratorDetailLayout;

    private final JToolBar toolBar;

    public DbEntityMainView(ProjectController controller) {
        super();
        this.controller = controller;

        toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        ActionManager actionManager = Application.getInstance().getActionManager();

        toolBar.add(actionManager.getAction(CreateAttributeAction.class).buildButton(1));
        toolBar.add(actionManager.getAction(CreateRelationshipAction.class).buildButton(3));
        toolBar.addSeparator();

        toolBar.add(actionManager.getAction(CreateObjEntityFromDbAction.class).buildButton(1));
        toolBar.add(actionManager.getAction(DbEntitySyncAction.class).buildButton(2));
        toolBar.add(actionManager.getAction(DbEntityCounterpartAction.class).buildButton(3));
        toolBar.addSeparator();

        toolBar.add(actionManager.getAction(ShowGraphEntityAction.class).buildButton());

        // create widgets
        name = new TextAdapter(new JTextField()) {
            protected void updateModel(String text) {
                setEntityName(text);
            }
        };

        catalogLabel = new JLabel("Catalog:");
        catalog = new CayenneUndoableTextField();
        catalog.addCommitListener(this::setCatalog);

        schemaLabel = new JLabel("Schema:");
        schema = new CayenneUndoableTextField();
        schema.addCommitListener(this::setSchema);

        qualifier = new TextAdapter(new JTextField()) {
            protected void updateModel(String qualifier) {
                setQualifier(qualifier);
            }
        };

        comment = new CayenneUndoableTextField();
        comment.addCommitListener(this::setComment);

        pkGeneratorType = new JComboBox<>();
        pkGeneratorType.setEditable(false);
        pkGeneratorType.setModel(new DefaultComboBoxModel<>(PK_GENERATOR_TYPES));

        pkGeneratorDetailLayout = new CardLayout();
        pkGeneratorDetail = new JPanel(pkGeneratorDetailLayout);
        pkGeneratorDetail.add(new PKDefaultGeneratorPanel(controller), PK_DEFAULT_GENERATOR);
        pkGeneratorDetail.add(new PKDBGeneratorPanel(controller), PK_DB_GENERATOR);
        pkGeneratorDetail.add(new PKCustomSequenceGeneratorPanel(controller), PK_CUSTOM_SEQUENCE_GENERATOR);

        // assemble
        FormLayout layout = new FormLayout("right:pref, 3dlu, fill:200dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("DbEntity Configuration");
        builder.append("DbEntity Name:", name.getComponent());
        builder.append(catalogLabel, catalog);
        builder.append(schemaLabel, schema);
        builder.append("Qualifier:", qualifier.getComponent());
        builder.append("Comment:", comment);

        builder.appendSeparator("Primary Key");
        builder.append("PK Generation Strategy:", pkGeneratorType);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        mainPanel.add(builder.getPanel(), BorderLayout.NORTH);
        mainPanel.add(pkGeneratorDetail, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        controller.addDbEntityDisplayListener(this);

        pkGeneratorType.addItemListener(e -> {
            pkGeneratorDetailLayout.show(pkGeneratorDetail, (String) pkGeneratorType.getSelectedItem());

            for (int i = 0; i < pkGeneratorDetail.getComponentCount(); i++) {
                if (pkGeneratorDetail.getComponent(i).isVisible()) {

                    DbEntity entity = controller.getSelectedDbEntity();
                    PKGeneratorPanel panel = (PKGeneratorPanel) pkGeneratorDetail.getComponent(i);
                    panel.onInit(entity);
                    break;
                }
            }
        });
    }

    public void processExistingSelection(EventObject e) {
        EntityDisplayEvent ede = new EntityDisplayEvent(this, controller.getSelectedDbEntity(),
                controller.getSelectedDataMap(), (DataChannelDescriptor) controller.getProject().getRootNode());
        controller.displayDbEntity(ede);
    }

    public void dbEntitySelected(EntityDisplayEvent e) {
        DbEntity entity = (DbEntity) e.getEntity();

        if (entity == null) {
            return;
        }

        // if entity hasn't changed, still notify PK Generator panels, as entity PK may have changed...
        for (int i = 0; i < pkGeneratorDetail.getComponentCount(); i++) {
            ((PKGeneratorPanel) pkGeneratorDetail.getComponent(i)).setDbEntity(entity);
        }

        name.setText(entity.getName());
        catalog.setText(entity.getCatalog());
        schema.setText(entity.getSchema());
        qualifier.setText(new ExpressionConvertor().valueAsString(entity.getQualifier()));
        comment.setText(getComment(entity));

        String type = PK_DEFAULT_GENERATOR;

        if (entity.getPrimaryKeyGenerator() != null) {
            type = PK_CUSTOM_SEQUENCE_GENERATOR;
        } else {
            for (DbAttribute a : entity.getPrimaryKeys()) {
                if (a.isGenerated()) {
                    type = PK_DB_GENERATOR;
                    break;
                }
            }
        }

        catalogLabel.setEnabled(true);
        catalog.setEnabled(true);

        schemaLabel.setEnabled(true);
        schema.setEnabled(true);
        pkGeneratorDetail.setVisible(true);
        pkGeneratorType.setVisible(true);

        pkGeneratorType.setSelectedItem(type);
        pkGeneratorDetailLayout.show(pkGeneratorDetail, type);

        if(entity.getDataMap().getMappedEntities(entity).isEmpty()) {
            toolBar.getComponentAtIndex(4).setEnabled(false);
            toolBar.getComponentAtIndex(5).setEnabled(false);
        } else {
            toolBar.getComponentAtIndex(4).setEnabled(true);
            toolBar.getComponentAtIndex(5).setEnabled(true);
        }
    }

    void setEntityName(String newName) {
        if (newName != null && newName.trim().isEmpty()) {
            newName = null;
        }

        DbEntity entity = controller.getSelectedDbEntity();

        if (entity == null || Util.nullSafeEquals(newName, entity.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Entity name is required.");
        } else if (entity.getDataMap().getDbEntity(newName) == null) {
            // completely new name, set new name for entity
            EntityEvent e = new EntityEvent(this, entity, entity.getName());
            entity.setName(newName);
            controller.fireDbEntityEvent(e);
        } else {
            // there is an entity with the same name
            throw new ValidationException("There is another entity with name '" + newName + "'.");
        }
    }

    void setCatalog(String text) {

        if (text != null && text.trim().isEmpty()) {
            text = null;
        }

        DbEntity ent = controller.getSelectedDbEntity();

        if (ent != null && !Util.nullSafeEquals(ent.getCatalog(), text)) {
            ent.setCatalog(text);
            controller.fireDbEntityEvent(new EntityEvent(this, ent));
        }
    }

    void setSchema(String text) {

        if (text != null && text.trim().isEmpty()) {
            text = null;
        }

        DbEntity ent = controller.getSelectedDbEntity();

        if (ent != null && !Util.nullSafeEquals(ent.getSchema(), text)) {
            ent.setSchema(text);
            controller.fireDbEntityEvent(new EntityEvent(this, ent));
        }
    }

    void setQualifier(String qualifier) {

        if (qualifier != null && qualifier.trim().isEmpty()) {
            qualifier = null;
        }

        DbEntity ent = controller.getSelectedDbEntity();

        if (ent != null && !Util.nullSafeEquals(ent.getQualifier(), qualifier)) {
            ExpressionConvertor convertor = new ExpressionConvertor();
            try {
                String oldQualifier = convertor.valueAsString(ent.getQualifier());
                if (!Util.nullSafeEquals(oldQualifier, qualifier)) {
                    Expression exp = (Expression) convertor.stringAsValue(qualifier);
                    ent.setQualifier(exp);
                    controller.fireDbEntityEvent(new EntityEvent(this, ent));
                }
            } catch (IllegalArgumentException ex) {
                // unparsable qualifier
                throw new ValidationException(ex.getMessage());
            }

        }
    }

    private String getComment(DbEntity entity) {
        return ObjectInfo.getFromMetaData(controller.getApplication().getMetaData(), entity, ObjectInfo.COMMENT);
    }

    private void setComment(String value) {
        DbEntity entity = controller.getSelectedDbEntity();

        if(entity == null) {
            return;
        }

        ObjectInfo.putToMetaData(controller.getApplication().getMetaData(), entity, ObjectInfo.COMMENT, value);
        controller.fireDbEntityEvent(new EntityEvent(this, entity));
    }
}
