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

package org.apache.cayenne.modeler.ui.project.editor.objentity.main;

import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.action.CreateAttributeAction;
import org.apache.cayenne.modeler.ui.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.ui.action.ObjEntityCounterpartAction;
import org.apache.cayenne.modeler.ui.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.toolkit.Renderers;
import org.apache.cayenne.modeler.toolkit.checkbox.CMCheckBox;
import org.apache.cayenne.modeler.toolkit.combobox.AutoCompletion;
import org.apache.cayenne.modeler.toolkit.combobox.CMComboBox;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.ExpressionConvertor;
import org.apache.cayenne.modeler.ui.project.editor.objentity.classname.ClassNameUpdaterController;
import org.apache.cayenne.modeler.ui.project.editor.objentity.duplicates.DuplicatedAttributesDialog;
import org.apache.cayenne.modeler.ui.project.editor.query.ExistingSelectionProcessor;
import org.apache.cayenne.modeler.project.ProjectComparators;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import java.util.Objects;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ObjEntityMainView extends ProjectPanel implements ObjEntityDisplayListener, ExistingSelectionProcessor {

    private static final ObjEntity NO_INHERITANCE = new ObjEntity("Direct Mapping to Table/View");

    private final CMUndoableTextField name;
    private final CMUndoableTextField className;

    private JLabel superclassLabel;
    private final CMUndoableTextField superClassName;
    private final CMUndoableTextField qualifier;
    private final CMComboBox<DbEntity> dbEntityCombo;
    private final CMComboBox<ObjEntity> superEntityCombo;
    private final CMCheckBox readOnly;
    private final CMCheckBox optimisticLocking;
    private final CMCheckBox isAbstract;
    private final CMUndoableTextField comment;
    // borderless clickable button used as a label; needs to be a field to wire its listener in initBindings()
    private final JButton tableLabel;

    public ObjEntityMainView(ProjectSession session) {
        super(session);
        name = new CMUndoableTextField(app.getUndoManager());
        superClassName = new CMUndoableTextField(app.getUndoManager());
        className = new CMUndoableTextField(app.getUndoManager());
        qualifier = new CMUndoableTextField(app.getUndoManager());
        dbEntityCombo = new CMComboBox<>();
        superEntityCombo = new CMComboBox<>();
        readOnly = new CMCheckBox(app.getUndoManager());
        optimisticLocking = new CMCheckBox(app.getUndoManager());
        isAbstract = new CMCheckBox(app.getUndoManager());
        comment = new CMUndoableTextField(app.getUndoManager());
        tableLabel = new JButton("Table/View:");
        initLayout();
        initBindings();
    }

    private void initLayout() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        GlobalActions globalActions = app.getActionManager();
        toolBar.add(globalActions.getAction(CreateAttributeAction.class).buildButton(1));
        toolBar.add(globalActions.getAction(CreateRelationshipAction.class).buildButton(3));
        toolBar.addSeparator();
        toolBar.add(globalActions.getAction(ObjEntitySyncAction.class).buildButton(1));
        toolBar.add(globalActions.getAction(ObjEntityCounterpartAction.class).buildButton(3));
        add(toolBar, BorderLayout.NORTH);

        tableLabel.setBorderPainted(false);
        tableLabel.setHorizontalAlignment(SwingConstants.LEFT);
        tableLabel.setFocusPainted(false);
        tableLabel.setMargin(new Insets(0, 0, 0, 0));
        tableLabel.setBorder(null);

        FormLayout layout = new FormLayout("right:pref, 3dlu, fill:200dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("ObjEntity Configuration");
        builder.append("ObjEntity Name:", name);
        builder.append("Inheritance:", superEntityCombo);
        builder.append(tableLabel, dbEntityCombo);
        builder.append("Comment:", comment);
        builder.appendSeparator();
        builder.append("Java Class:", className);
        superclassLabel = builder.append("Superclass:", superClassName);
        builder.append("Qualifier:", qualifier);
        builder.append("Read-Only:", readOnly);
        builder.append("Optimistic Locking:", optimisticLocking);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initBindings() {
        AutoCompletion.enable(dbEntityCombo, session::getSelectedDataMap);
        AutoCompletion.enable(superEntityCombo, session::getSelectedDataMap);

        name.addCommitListener(this::setEntityName);
        superClassName.addCommitListener(this::setSuperClassName);
        className.addCommitListener(this::setClassName);
        qualifier.addCommitListener(this::setQualifier);
        comment.addCommitListener(this::setComment);

        session.addObjEntityDisplayListener(this);

        dbEntityCombo.addActionListener(e -> {
            // Change DbEntity for current ObjEntity
            ObjEntity entity = session.getSelectedObjEntity();
            DbEntity dbEntity = (DbEntity) dbEntityCombo.getSelectedItem();
            if (dbEntity != entity.getDbEntity()) {
                entity.setDbEntity(dbEntity);
                session.fireObjEntityEvent(ObjEntityEvent.ofChange(ObjEntityMainView.this, entity));
            }
        });

        superEntityCombo.addActionListener(e -> {
            // Change super-entity
            ObjEntity superEntity = (ObjEntity) superEntityCombo.getSelectedItem();
            String name = (superEntity == null || superEntity == NO_INHERITANCE)
                    ? null
                    : superEntity.getName();

            ObjEntity entity = session.getSelectedObjEntity();

            if (!Objects.equals(name, entity.getSuperEntityName())) {
                List<ObjAttribute> duplicateAttributes = null;
                if (name != null) {
                    duplicateAttributes = getDuplicatedAttributes(superEntity);
                }

                if (duplicateAttributes != null && !duplicateAttributes.isEmpty()) {
                    DuplicatedAttributesDialog.showDialog(
                            app,
                            app.getFrame(),
                            session, duplicateAttributes, superEntity, entity);
                    if (DuplicatedAttributesDialog.getResult().equals(DuplicatedAttributesDialog.CANCEL_RESULT)) {
                        superEntityCombo.setSelectedItem(entity.getSuperEntity());
                        superClassName.setText(entity.getSuperClassName());
                        return;
                    }
                }
                entity.setSuperEntityName(name);

                // drop not valid dbAttributePath
                if (name == null) {
                    for (ObjAttribute objAttribute : entity.getAttributes()) {
                        if (objAttribute.getDbAttribute() == null) {
                            objAttribute.setDbAttributePath((String) null);
                        }
                    }
                }

                if (name == null) {
                    dbEntityCombo.setEnabled(true);
                } else {
                    dbEntityCombo.setEnabled(false);
                    dbEntityCombo.getModel().setSelectedItem(null);
                }

                // if a super-entity selected, disable table selection
                // and also update parent DbEntity selection...
                toggleEnabled(name == null);
                dbEntityCombo.getModel().setSelectedItem(entity.getDbEntity());
                superClassName.setText(entity.getSuperClassName());

                // fire both ObjEntityEvent and ObjEntityDisplayEvent;
                // the latter is to update attribute and relationship display
                DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
                DataMap map = session.getSelectedDataMap();
                session.fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
                session.displayObjEntity(new ObjEntityDisplayEvent(this, domain, map, entity));
            }
        });

        tableLabel.addActionListener(e -> {
            // Jump to DbEntity of the current ObjEntity
            DbEntity entity = session.getSelectedObjEntity().getDbEntity();
            if (entity != null) {
                DataChannelDescriptor dom = (DataChannelDescriptor) session.project().getRootNode();
                session.displayDbEntity(new DbEntityDisplayEvent(this, dom, entity.getDataMap(), entity));
            }
        });

        readOnly.addItemListener(e -> {
            ObjEntity entity = session.getSelectedObjEntity();
            if (entity != null) {
                entity.setReadOnly(readOnly.isSelected());
                session.fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
            }
        });

        optimisticLocking.addItemListener(e -> {
            ObjEntity entity = session.getSelectedObjEntity();
            if (entity != null) {
                entity.setDeclaredLockType(optimisticLocking.isSelected()
                        ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                        : ObjEntity.LOCK_TYPE_NONE);
                session.fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
            }
        });

        isAbstract.addItemListener(e -> {
            ObjEntity entity = session.getSelectedObjEntity();
            if (entity != null) {
                entity.setAbstract(isAbstract.isSelected());
                session.fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
            }
        });
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * ObjEntity is changed.
     *
     * @param entity current entity
     */
    private void initFromModel(final ObjEntity entity) {
        name.setText(entity.getName());
        superClassName.setText(entity.getSuperClassName());
        className.setText(entity.getClassName());
        readOnly.setSelected(entity.isReadOnly());

        isAbstract.setSelected(entity.isAbstract());
        comment.setText(getComment(entity));

        qualifier.setText(ExpressionConvertor.asString(entity.getDeclaredQualifier()));

        // TODO: fix inheritance - we should allow to select optimistic
        // lock if superclass is not already locked,
        // otherwise we must keep this checked in but not editable.
        optimisticLocking.setSelected(entity.getDeclaredLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC);

        // init DbEntities
        EntityResolver resolver = session.entityResolver();
        DataMap map = session.getSelectedDataMap();
        DbEntity[] dbEntities = resolver.getDbEntities().toArray(new DbEntity[0]);
        Arrays.sort(dbEntities, ProjectComparators.forDataMapChildren());

        DefaultComboBoxModel<DbEntity> dbModel = new DefaultComboBoxModel<>(dbEntities);
        dbModel.setSelectedItem(entity.getDbEntity());
        dbEntityCombo.setRenderer(Renderers.entityListRendererWithIcons(map));
        dbEntityCombo.setModel(dbModel);

        boolean isUsedInheritance = entity.getSuperEntity() != null;
        dbEntityCombo.setEnabled(!isUsedInheritance);

        // toggle visibility and editability rules
        toggleEnabled(entity.getSuperEntityName() == null);

        // do not show this entity or any of the subentities
        List<ObjEntity> objEntities = map.getObjEntities().stream()
                .filter(object -> entity != object && !object.isSubentityOf(entity))
                .sorted(ProjectComparators.forDataMapChildren())
                .collect(Collectors.toList());
        objEntities.add(0, NO_INHERITANCE);

        DefaultComboBoxModel<ObjEntity> superEntityModel = new DefaultComboBoxModel<>(objEntities.toArray(new ObjEntity[0]));
        superEntityModel.setSelectedItem(entity.getSuperEntity());
        superEntityCombo.setRenderer(Renderers.entityListRendererWithIcons(map));
        superEntityCombo.setModel(superEntityModel);
    }

    void setClassName(String className) {
        if (className != null && className.trim().isEmpty()) {
            className = null;
        }

        ObjEntity entity = session.getSelectedObjEntity();

        // "ent" may be null if we quit editing by changing tree selection
        if (entity != null && !Objects.equals(entity.getClassName(), className)) {
            entity.setClassName(className);
            session.fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
        }
    }

    void setSuperClassName(String text) {

        if (text != null && text.trim().isEmpty()) {
            text = null;
        }

        ObjEntity ent = session.getSelectedObjEntity();

        if (ent != null && !Objects.equals(ent.getSuperClassName(), text)) {
            ent.setSuperClassName(text);
            session.fireObjEntityEvent(ObjEntityEvent.ofChange(this, ent));
        }
    }

    void setQualifier(String text) {
        if (text != null && text.trim().isEmpty()) {
            text = null;
        }

        ObjEntity entity = session.getSelectedObjEntity();
        if (entity != null) {
            try {
                String oldQualifier = ExpressionConvertor.asString(entity.getDeclaredQualifier());
                if (!Objects.equals(oldQualifier, text)) {
                    entity.setDeclaredQualifier(ExpressionConvertor.fromString(text));
                    session.fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
                }
            } catch (IllegalArgumentException ex) {
                // unparsable qualifier
                throw new ValidationException(ex.getMessage());
            }
        }
    }

    void setEntityName(String newName) {
        if (newName != null && newName.trim().isEmpty()) {
            newName = null;
        }

        ObjEntity entity = session.getSelectedObjEntity();
        if (entity == null) {
            return;
        }

        if (Objects.equals(newName, entity.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Entity name is required.");
        } else if (entity.getDataMap().getObjEntity(newName) == null) {
            // completely new name, set new name for entity
            ObjEntityEvent e = ObjEntityEvent.ofChange(this, entity, entity.getName());
            entity.getDataMap().renameObjEntity(entity, newName);

            session.fireObjEntityEvent(e);

            // suggest to update class name
            ClassNameUpdaterController nameUpdater = new ClassNameUpdaterController(this, entity);

            if (nameUpdater.doNameUpdate()) {
                className.setText(entity.getClassName());
            }
        } else {
            // there is an entity with the same name
            throw new ValidationException("There is another entity with name '" + newName + "'.");
        }
    }

    void toggleEnabled(boolean directTableMapping) {
        superClassName.setEnabled(directTableMapping);
        superclassLabel.setEnabled(directTableMapping);
    }

    public void processExistingSelection(EventObject e) {

        ObjEntityDisplayEvent ede = new ObjEntityDisplayEvent(
                this,
                (DataChannelDescriptor) session.project().getRootNode(),
                session.getSelectedDataMap(),
                session.getSelectedObjEntity());
        session.displayObjEntity(ede);
    }

    public void objEntitySelected(ObjEntityDisplayEvent e) {
        ObjEntity entity = e.getEntity();
        if (entity == null) {
            return;
        }

        initFromModel(entity);
    }

    private List<ObjAttribute> getDuplicatedAttributes(ObjEntity superEntity) {
        List<ObjAttribute> result = new LinkedList<>();

        ObjEntity entity = session.getSelectedObjEntity();

        for (ObjAttribute attribute : entity.getAttributes()) {
            if (superEntity.getAttribute(attribute.getName()) != null) {
                result.add(attribute);
            }
        }

        return result;
    }

    private void setComment(String value) {
        ObjEntity entity = session.getSelectedObjEntity();
        if (entity == null) {
            return;
        }

        ObjectInfo.putToMetaData(app.getMetaData(), entity, ObjectInfo.COMMENT, value);
        session.fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
    }

    private String getComment(ObjEntity entity) {
        return ObjectInfo.getFromMetaData(app.getMetaData(), entity, ObjectInfo.COMMENT);
    }

}
