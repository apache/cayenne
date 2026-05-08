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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.model.DbEntityEvent;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.ui.action.CreateAttributeAction;
import org.apache.cayenne.modeler.ui.action.CreateObjEntityFromDbAction;
import org.apache.cayenne.modeler.ui.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.ui.action.DbEntityCounterpartAction;
import org.apache.cayenne.modeler.ui.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.ExpressionConvertor;
import org.apache.cayenne.modeler.ui.project.editor.query.ExistingSelectionProcessor;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.EventObject;

public class DbEntityMainView extends ProjectPanel implements ExistingSelectionProcessor, DbEntityDisplayListener {

    static final String PK_DEFAULT_GENERATOR = "Cayenne-Generated (Default)";
    static final String PK_DB_GENERATOR = "Database-Generated";
    static final String PK_CUSTOM_SEQUENCE_GENERATOR = "Custom Sequence";

    static final String[] PK_GENERATOR_TYPES = { PK_DEFAULT_GENERATOR, PK_DB_GENERATOR, PK_CUSTOM_SEQUENCE_GENERATOR };

    private final CMUndoableTextField name;
    private final CMUndoableTextField catalog;
    private final CMUndoableTextField schema;
    private final CMUndoableTextField qualifier;
    private final CMUndoableTextField comment;

    private final JLabel catalogLabel;
    private final JLabel schemaLabel;

    private final JComboBox<String> pkGeneratorType;
    private final JPanel pkGeneratorDetail;
    private final CardLayout pkGeneratorDetailLayout;

    private final JToolBar toolBar;

    public DbEntityMainView(ProjectSession session) {
        super(session);
        toolBar = new JToolBar();
        name = new CMUndoableTextField(app.getUndoManager());
        catalogLabel = new JLabel("Catalog:");
        catalog = new CMUndoableTextField(app.getUndoManager());
        schemaLabel = new JLabel("Schema:");
        schema = new CMUndoableTextField(app.getUndoManager());
        qualifier = new CMUndoableTextField(app.getUndoManager());
        comment = new CMUndoableTextField(app.getUndoManager());
        pkGeneratorType = new JComboBox<>();
        pkGeneratorDetailLayout = new CardLayout();
        pkGeneratorDetail = new JPanel(pkGeneratorDetailLayout);
        initLayout();
        initBindings();
    }

    private void initLayout() {
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        GlobalActions globalActions = app.getActionManager();
        toolBar.add(globalActions.getAction(CreateAttributeAction.class).buildButton(1));
        toolBar.add(globalActions.getAction(CreateRelationshipAction.class).buildButton(3));
        toolBar.addSeparator();
        toolBar.add(globalActions.getAction(CreateObjEntityFromDbAction.class).buildButton(1));
        toolBar.add(globalActions.getAction(DbEntitySyncAction.class).buildButton(2));
        toolBar.add(globalActions.getAction(DbEntityCounterpartAction.class).buildButton(3));

        pkGeneratorType.setEditable(false);
        pkGeneratorType.setModel(new DefaultComboBoxModel<>(PK_GENERATOR_TYPES));
        pkGeneratorDetail.add(new PKDefaultGeneratorPanel(session), PK_DEFAULT_GENERATOR);
        pkGeneratorDetail.add(new PKDBGeneratorPanel(session), PK_DB_GENERATOR);
        pkGeneratorDetail.add(new PKCustomSequenceGeneratorPanel(session), PK_CUSTOM_SEQUENCE_GENERATOR);

        FormLayout layout = new FormLayout("right:pref, 3dlu, fill:200dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.appendSeparator("DbEntity Configuration");
        builder.append("DbEntity Name:", name);
        builder.append(catalogLabel, catalog);
        builder.append(schemaLabel, schema);
        builder.append("Qualifier:", qualifier);
        builder.append("Comment:", comment);
        builder.appendSeparator("Primary Key");
        builder.append("PK Generation Strategy:", pkGeneratorType);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(builder.getPanel(), BorderLayout.NORTH);
        mainPanel.add(pkGeneratorDetail, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void initBindings() {
        name.addCommitListener(this::setEntityName);
        catalog.addCommitListener(this::setCatalog);
        schema.addCommitListener(this::setSchema);
        qualifier.addCommitListener(this::setQualifier);
        comment.addCommitListener(this::setComment);
        session.addDbEntityDisplayListener(this);
        pkGeneratorType.addItemListener(e -> {
            pkGeneratorDetailLayout.show(pkGeneratorDetail, (String) pkGeneratorType.getSelectedItem());
            for (int i = 0; i < pkGeneratorDetail.getComponentCount(); i++) {
                if (pkGeneratorDetail.getComponent(i).isVisible()) {
                    DbEntity entity = session.getSelectedDbEntity();
                    PKGeneratorPanel panel = (PKGeneratorPanel) pkGeneratorDetail.getComponent(i);
                    panel.onInit(entity);
                    break;
                }
            }
        });
    }

    public void processExistingSelection(EventObject e) {
        DbEntityDisplayEvent ede = new DbEntityDisplayEvent(this,
                (DataChannelDescriptor) session.project().getRootNode(),
                session.getSelectedDataMap(),
                session.getSelectedDbEntity());
        session.displayDbEntity(ede);
    }

    public void dbEntitySelected(DbEntityDisplayEvent e) {
        DbEntity entity = e.getEntity();

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
        qualifier.setText(ExpressionConvertor.asString(entity.getQualifier()));
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

        DbEntity entity = session.getSelectedDbEntity();

        if (entity == null || Util.nullSafeEquals(newName, entity.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Entity name is required.");
        } else if (entity.getDataMap().getDbEntity(newName) == null) {
            // completely new name, set new name for entity
            DbEntityEvent e = DbEntityEvent.ofChange(this, entity, entity.getName());
            entity.getDataMap().renameDbEntity(entity, newName);
            session.fireDbEntityEvent(e);
        } else {
            // there is an entity with the same name
            throw new ValidationException("There is another entity with name '" + newName + "'.");
        }
    }

    void setCatalog(String text) {

        if (text != null && text.trim().isEmpty()) {
            text = null;
        }

        DbEntity ent = session.getSelectedDbEntity();

        if (ent != null && !Util.nullSafeEquals(ent.getCatalog(), text)) {
            ent.setCatalog(text);
            session.fireDbEntityEvent(DbEntityEvent.ofChange(this, ent));
        }
    }

    void setSchema(String text) {

        if (text != null && text.trim().isEmpty()) {
            text = null;
        }

        DbEntity ent = session.getSelectedDbEntity();

        if (ent != null && !Util.nullSafeEquals(ent.getSchema(), text)) {
            ent.setSchema(text);
            session.fireDbEntityEvent(DbEntityEvent.ofChange(this, ent));
        }
    }

    void setQualifier(String qualifier) {

        if (qualifier != null && qualifier.trim().isEmpty()) {
            qualifier = null;
        }

        DbEntity ent = session.getSelectedDbEntity();

        if (ent != null && !Util.nullSafeEquals(ent.getQualifier(), qualifier)) {
            try {
                String oldQualifier = ExpressionConvertor.asString(ent.getQualifier());
                if (!Util.nullSafeEquals(oldQualifier, qualifier)) {
                    ent.setQualifier(ExpressionConvertor.fromString(qualifier));
                    session.fireDbEntityEvent(DbEntityEvent.ofChange(this, ent));
                }
            } catch (IllegalArgumentException ex) {
                // unparsable qualifier
                throw new ValidationException(ex.getMessage());
            }

        }
    }

    private String getComment(DbEntity entity) {
        return ObjectInfo.getFromMetaData(app.getMetaData(), entity, ObjectInfo.COMMENT);
    }

    private void setComment(String value) {
        DbEntity entity = session.getSelectedDbEntity();

        if(entity == null) {
            return;
        }

        ObjectInfo.putToMetaData(app.getMetaData(), entity, ObjectInfo.COMMENT, value);
        session.fireDbEntityEvent(DbEntityEvent.ofChange(this, entity));
    }
}
