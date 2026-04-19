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
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.ObjEntityCounterpartAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.dialog.objentity.ClassNameUpdaterController;
import org.apache.cayenne.modeler.dialog.validator.DuplicatedAttributesDialog;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.graph.action.ShowGraphEntityAction;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.ExpressionConvertor;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.swing.components.JCayenneCheckBox;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ObjEntityTab extends JPanel implements ObjEntityDisplayListener, ExistingSelectionProcessor {

    private static final ObjEntity NO_INHERITANCE = new ObjEntity("Direct Mapping to Table/View");

    private final ProjectController controller;
    private final TextAdapter name;
    private final TextAdapter className;

    private final JLabel superclassLabel;
    private final TextAdapter superClassName;
    private final TextAdapter qualifier;
    private final JComboBox<DbEntity> dbEntityCombo;
    private final JComboBox<ObjEntity> superEntityCombo;
    private final JCheckBox readOnly;
    private final JCheckBox optimisticLocking;

    private final JCheckBox isAbstract;
    private final TextAdapter comment;

    public ObjEntityTab(ProjectController controller) {
        this.controller = controller;

        this.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        ActionManager actionManager = Application.getInstance().getActionManager();

        toolBar.add(actionManager.getAction(CreateAttributeAction.class).buildButton(1));
        toolBar.add(actionManager.getAction(CreateRelationshipAction.class).buildButton(3));
        toolBar.addSeparator();
        toolBar.add(actionManager.getAction(ObjEntitySyncAction.class).buildButton(1));
        toolBar.add(actionManager.getAction(ObjEntityCounterpartAction.class).buildButton(3));
        toolBar.addSeparator();
        toolBar.add(actionManager.getAction(ShowGraphEntityAction.class).buildButton());

        add(toolBar, BorderLayout.NORTH);

        // create widgets
        name = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) {
                setEntityName(text);
            }
        };
        superClassName = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) {
                setSuperClassName(text);
            }
        };
        className = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) {
                setClassName(text);
            }
        };
        qualifier = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) {
                setQualifier(text);
            }
        };

        dbEntityCombo = Application.getWidgetFactory().createComboBox();
        superEntityCombo = Application.getWidgetFactory().createComboBox();

        AutoCompletion.enable(dbEntityCombo);
        AutoCompletion.enable(superEntityCombo);

        readOnly = new JCayenneCheckBox();

        optimisticLocking = new JCayenneCheckBox();

        // borderless clickable button used as a label
        JButton tableLabel = new JButton("Table/View:");
        tableLabel.setBorderPainted(false);
        tableLabel.setHorizontalAlignment(SwingConstants.LEFT);
        tableLabel.setFocusPainted(false);
        tableLabel.setMargin(new Insets(0, 0, 0, 0));
        tableLabel.setBorder(null);


        isAbstract = new JCayenneCheckBox();

        comment = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) throws ValidationException {
                setComment(text);
            }
        };

        // assemble
        FormLayout layout = new FormLayout("right:pref, 3dlu, fill:200dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("ObjEntity Configuration");
        builder.append("ObjEntity Name:", name.getComponent());
        builder.append("Inheritance:", superEntityCombo);
        builder.append(tableLabel, dbEntityCombo);
        builder.append("Comment:", comment.getComponent());
        builder.appendSeparator();

        builder.append("Java Class:", className.getComponent());
     
        superclassLabel = builder.append("Superclass:", superClassName.getComponent());
        builder.append("Qualifier:", qualifier.getComponent());
        builder.append("Read-Only:", readOnly);
        builder.append("Optimistic Locking:", optimisticLocking);

        add(builder.getPanel(), BorderLayout.CENTER);

        // initialize events processing and tracking of UI updates...

        controller.addObjEntityDisplayListener(this);

        dbEntityCombo.addActionListener(e -> {
            // Change DbEntity for current ObjEntity
            ObjEntity entity = controller.getSelectedObjEntity();
            DbEntity dbEntity = (DbEntity) dbEntityCombo.getSelectedItem();


            if (dbEntity != entity.getDbEntity()) {
                entity.setDbEntity(dbEntity);
                controller.fireObjEntityEvent(new EntityEvent(ObjEntityTab.this, entity));
            }
        });

        superEntityCombo.addActionListener(e -> {
            // Change super-entity
            ObjEntity superEntity = (ObjEntity) superEntityCombo.getSelectedItem();
            String name = (superEntity == null || superEntity == NO_INHERITANCE)
                    ? null
                    : superEntity.getName();

            ObjEntity entity = controller.getSelectedObjEntity();

            if (!Util.nullSafeEquals(name, entity.getSuperEntityName())) {
                List<ObjAttribute> duplicateAttributes = null;
                if (name != null) {
                    duplicateAttributes = getDuplicatedAttributes(superEntity);
                }

                if (duplicateAttributes != null && !duplicateAttributes.isEmpty()) {
                    DuplicatedAttributesDialog.showDialog(
                            Application.getFrame(), duplicateAttributes, superEntity, entity);
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
                            objAttribute.setDbAttributePath((String)null);
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

                // fire both EntityEvent and EntityDisplayEvent;
                // the latter is to update attribute and relationship display

                DataChannelDescriptor domain = (DataChannelDescriptor) controller.getProject().getRootNode();
                DataMap map = controller.getSelectedDataMap();

                controller.fireObjEntityEvent(new EntityEvent(this, entity));
                controller.displayObjEntity(new EntityDisplayEvent(this, entity, map, domain));
            }
        });

        tableLabel.addActionListener(e -> {
            // Jump to DbEntity of the current ObjEntity
            DbEntity entity = controller.getSelectedObjEntity().getDbEntity();
            if (entity != null) {
                DataChannelDescriptor dom = (DataChannelDescriptor) controller.getProject().getRootNode();
                controller.displayDbEntity(new EntityDisplayEvent(this, entity, entity.getDataMap(), dom));
            }
        });

        readOnly.addItemListener(e -> {
            ObjEntity entity = controller.getSelectedObjEntity();
            if (entity != null) {
                entity.setReadOnly(readOnly.isSelected());
                controller.fireObjEntityEvent(new EntityEvent(this, entity));
            }
        });

        optimisticLocking.addItemListener(e -> {
            ObjEntity entity = controller.getSelectedObjEntity();
            if (entity != null) {
                entity.setDeclaredLockType(optimisticLocking.isSelected()
                        ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                        : ObjEntity.LOCK_TYPE_NONE);
                controller.fireObjEntityEvent(new EntityEvent(this, entity));
            }
        });

        isAbstract.addItemListener(e -> {
            ObjEntity entity = controller.getSelectedObjEntity();
            if (entity != null) {
                entity.setAbstract(isAbstract.isSelected());
                controller.fireObjEntityEvent(new EntityEvent(this, entity));
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
        // TODO: this is a hack until we implement a real MVC
        qualifier.getComponent().setBackground(Color.WHITE);

        name.setText(entity.getName());
        superClassName.setText(entity.getSuperClassName());
        className.setText(entity.getClassName());
        readOnly.setSelected(entity.isReadOnly());

        isAbstract.setSelected(entity.isAbstract());
        comment.setText(getComment(entity));

        qualifier.setText(new ExpressionConvertor().valueAsString(entity.getDeclaredQualifier()));

        // TODO: fix inheritance - we should allow to select optimistic
        // lock if superclass is not already locked,
        // otherwise we must keep this checked in but not editable.
        optimisticLocking.setSelected(entity.getDeclaredLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC);

        // init DbEntities
        EntityResolver resolver = controller.getEntityResolver();
        DataMap map = controller.getSelectedDataMap();
        DbEntity[] dbEntities = resolver.getDbEntities().toArray(new DbEntity[0]);
        Arrays.sort(dbEntities, Comparators.getDataMapChildrenComparator());

        DefaultComboBoxModel<DbEntity> dbModel = new DefaultComboBoxModel<>(dbEntities);
        dbModel.setSelectedItem(entity.getDbEntity());
        dbEntityCombo.setRenderer(CellRenderers.entityListRendererWithIcons(map));
        dbEntityCombo.setModel(dbModel);

        boolean isUsedInheritance = entity.getSuperEntity() != null;
        dbEntityCombo.setEnabled(!isUsedInheritance);

        // toggle visibility and editability rules
        toggleEnabled(entity.getSuperEntityName() == null);

        // do not show this entity or any of the subentities
        List<ObjEntity> objEntities = map.getObjEntities().stream()
                .filter(object -> entity != object && !object.isSubentityOf(entity))
                .sorted(Comparators.getDataMapChildrenComparator())
                .collect(Collectors.toList());
        objEntities.add(0, NO_INHERITANCE);

        DefaultComboBoxModel<ObjEntity> superEntityModel = new DefaultComboBoxModel<>(objEntities.toArray(new ObjEntity[0]));
        superEntityModel.setSelectedItem(entity.getSuperEntity());
        superEntityCombo.setRenderer(CellRenderers.entityListRendererWithIcons(map));
        superEntityCombo.setModel(superEntityModel);
    }

    void setClassName(String className) {
        if (className != null && className.trim().isEmpty()) {
            className = null;
        }

        ObjEntity entity = controller.getSelectedObjEntity();

        // "ent" may be null if we quit editing by changing tree selection
        if (entity != null && !Util.nullSafeEquals(entity.getClassName(), className)) {
            entity.setClassName(className);
            controller.fireObjEntityEvent(new EntityEvent(this, entity));
        }
    }

    void setSuperClassName(String text) {

        if (text != null && text.trim().isEmpty()) {
            text = null;
        }

        ObjEntity ent = controller.getSelectedObjEntity();

        if (ent != null && !Util.nullSafeEquals(ent.getSuperClassName(), text)) {
            ent.setSuperClassName(text);
            controller.fireObjEntityEvent(new EntityEvent(this, ent));
        }
    }

    void setQualifier(String text) {
        if (text != null && text.trim().isEmpty()) {
            text = null;
        }

        ObjEntity entity = controller.getSelectedObjEntity();
        if (entity != null) {
            ExpressionConvertor convertor = new ExpressionConvertor();
            try {
                String oldQualifier = convertor.valueAsString(entity.getDeclaredQualifier());
                if (!Util.nullSafeEquals(oldQualifier, text)) {
                    Expression exp = (Expression) convertor.stringAsValue(text);
                    entity.setDeclaredQualifier(exp);
                    controller.fireObjEntityEvent(new EntityEvent(this, entity));
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

        ObjEntity entity = controller.getSelectedObjEntity();
        if (entity == null) {
            return;
        }

        if (Util.nullSafeEquals(newName, entity.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Entity name is required.");
        } else if (entity.getDataMap().getObjEntity(newName) == null) {
            // completely new name, set new name for entity
            EntityEvent e = new EntityEvent(this, entity, entity.getName());
            entity.setName(newName);

            controller.fireObjEntityEvent(e);

            // suggest to update class name
            ClassNameUpdaterController nameUpdater = new ClassNameUpdaterController(Application.getInstance().getFrameController(), entity);

            if (nameUpdater.doNameUpdate()) {
                className.setText(entity.getClassName());
            }
        } else {
            // there is an entity with the same name
            throw new ValidationException("There is another entity with name '" + newName + "'.");
        }
    }

    void toggleEnabled(boolean directTableMapping) {
        superClassName.getComponent().setEnabled(directTableMapping);
        superclassLabel.setEnabled(directTableMapping);
    }

    public void processExistingSelection(EventObject e) {

        EntityDisplayEvent ede = new EntityDisplayEvent(
                this,
                controller.getSelectedObjEntity(),
                controller.getSelectedDataMap(),
                (DataChannelDescriptor) controller.getProject().getRootNode());
        controller.displayObjEntity(ede);
    }

    public void objEntitySelected(EntityDisplayEvent e) {
        ObjEntity entity = (ObjEntity) e.getEntity();
        if (entity == null) {
            return;
        }

        initFromModel(entity);
    }

    private List<ObjAttribute> getDuplicatedAttributes(ObjEntity superEntity) {
        List<ObjAttribute> result = new LinkedList<>();

        ObjEntity entity = controller.getSelectedObjEntity();

        for (ObjAttribute attribute : entity.getAttributes()) {
            if (superEntity.getAttribute(attribute.getName()) != null) {
                result.add(attribute);
            }
        }

        return result;
    }

    private void setComment(String value) {
        ObjEntity entity = controller.getSelectedObjEntity();
        if (entity == null) {
            return;
        }

        ObjectInfo.putToMetaData(controller.getApplication().getMetaData(), entity, ObjectInfo.COMMENT, value);
        controller.fireObjEntityEvent(new EntityEvent(this, entity));
    }

    private String getComment(ObjEntity entity) {
        return ObjectInfo.getFromMetaData(controller.getApplication().getMetaData(), entity, ObjectInfo.COMMENT);
    }

}
