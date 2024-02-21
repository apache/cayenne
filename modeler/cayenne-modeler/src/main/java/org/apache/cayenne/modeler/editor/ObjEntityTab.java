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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.util.Arrays;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.swing.components.JCayenneCheckBox;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.ObjEntityCounterpartAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.dialog.objentity.ClassNameUpdater;
import org.apache.cayenne.modeler.dialog.validator.DuplicatedAttributesDialog;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.graph.action.ShowGraphEntityAction;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.ExpressionConvertor;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Detail view of the ObjEntity properties.
 */
public class ObjEntityTab extends JPanel implements ObjEntityDisplayListener, ExistingSelectionProcessor {

    private static final ObjEntity NO_INHERITANCE = new ObjEntity("Direct Mapping to Table/View");

    protected ProjectController mediator;
    protected TextAdapter name;
    protected TextAdapter className;

    protected JLabel superclassLabel;
    protected TextAdapter superClassName;
    protected TextAdapter qualifier;
    protected JComboBox<DbEntity> dbEntityCombo;
    protected JComboBox<ObjEntity> superEntityCombo;
    protected JButton tableLabel;
    protected JCheckBox readOnly;
    protected JCheckBox optimisticLocking;

    protected JLabel isAbstractLabel;

    protected JCheckBox isAbstract;
    protected TextAdapter comment;

    public ObjEntityTab(ProjectController mediator) {
        this.mediator = mediator;
        initView();
        initController();
    }

    private void initView() {
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
        tableLabel = new JButton("Table/View:");
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
        isAbstractLabel = builder.append("Abstract class:", isAbstract);
        builder.append("Comment:", comment.getComponent());
        builder.appendSeparator();

        builder.append("Java Class:", className.getComponent());
     
        superclassLabel = builder.append("Superclass:", superClassName.getComponent());
        builder.append("Qualifier:", qualifier.getComponent());
        builder.append("Read-Only:", readOnly);
        builder.append("Optimistic Locking:", optimisticLocking);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initController() {
        // initialize events processing and tracking of UI updates...

        mediator.addObjEntityDisplayListener(this);

        dbEntityCombo.addActionListener(e -> {
            // Change DbEntity for current ObjEntity
            ObjEntity entity = mediator.getCurrentObjEntity();
            DbEntity dbEntity = (DbEntity) dbEntityCombo.getSelectedItem();


            if (dbEntity != entity.getDbEntity()) {
                entity.setDbEntity(dbEntity);
                mediator.fireObjEntityEvent(new EntityEvent(ObjEntityTab.this, entity));
            }
        });

        superEntityCombo.addActionListener(e -> {
            // Change super-entity
            ObjEntity superEntity = (ObjEntity) superEntityCombo.getSelectedItem();
            String name = (superEntity == null || superEntity == NO_INHERITANCE)
                    ? null
                    : superEntity.getName();

            ObjEntity entity = mediator.getCurrentObjEntity();

            if (!Util.nullSafeEquals(name, entity.getSuperEntityName())) {
                List<ObjAttribute> duplicateAttributes = null;
                if (name != null) {
                    duplicateAttributes = getDuplicatedAttributes(superEntity);
                }

                if (duplicateAttributes != null && duplicateAttributes.size() > 0) {
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
                // the later is to update attribute and relationship display

                DataChannelDescriptor domain = (DataChannelDescriptor) mediator.getProject().getRootNode();
                DataMap map = mediator.getCurrentDataMap();

                mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                mediator.fireObjEntityDisplayEvent(new EntityDisplayEvent(this, entity, map, domain));
            }
        });

        tableLabel.addActionListener(e -> {
            // Jump to DbEntity of the current ObjEntity
            DbEntity entity = mediator.getCurrentObjEntity().getDbEntity();
            if (entity != null) {
                DataChannelDescriptor dom = (DataChannelDescriptor) mediator.getProject().getRootNode();
                mediator.fireDbEntityDisplayEvent(new EntityDisplayEvent(this, entity, entity.getDataMap(), dom));
            }
        });

        readOnly.addItemListener(e -> {
            ObjEntity entity = mediator.getCurrentObjEntity();
            if (entity != null) {
                entity.setReadOnly(readOnly.isSelected());
                mediator.fireObjEntityEvent(new EntityEvent(this, entity));
            }
        });

        optimisticLocking.addItemListener(e -> {
            ObjEntity entity = mediator.getCurrentObjEntity();
            if (entity != null) {
                entity.setDeclaredLockType(optimisticLocking.isSelected()
                        ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                        : ObjEntity.LOCK_TYPE_NONE);
                mediator.fireObjEntityEvent(new EntityEvent(this, entity));
            }
        });

        isAbstract.addItemListener(e -> {
            ObjEntity entity = mediator.getCurrentObjEntity();
            if (entity != null) {
                entity.setAbstract(isAbstract.isSelected());
                mediator.fireObjEntityEvent(new EntityEvent(this, entity));
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
        EntityResolver resolver = mediator.getEntityResolver();
        DataMap map = mediator.getCurrentDataMap();
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
        if (className != null && className.trim().length() == 0) {
            className = null;
        }

        ObjEntity entity = mediator.getCurrentObjEntity();

        // "ent" may be null if we quit editing by changing tree selection
        if (entity != null && !Util.nullSafeEquals(entity.getClassName(), className)) {
            entity.setClassName(className);
            mediator.fireObjEntityEvent(new EntityEvent(this, entity));
        }
    }

    void setSuperClassName(String text) {

        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        ObjEntity ent = mediator.getCurrentObjEntity();

        if (ent != null && !Util.nullSafeEquals(ent.getSuperClassName(), text)) {
            ent.setSuperClassName(text);
            mediator.fireObjEntityEvent(new EntityEvent(this, ent));
        }
    }

    void setQualifier(String text) {
        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        ObjEntity entity = mediator.getCurrentObjEntity();
        if (entity != null) {
            ExpressionConvertor convertor = new ExpressionConvertor();
            try {
                String oldQualifier = convertor.valueAsString(entity.getDeclaredQualifier());
                if (!Util.nullSafeEquals(oldQualifier, text)) {
                    Expression exp = (Expression) convertor.stringAsValue(text);
                    entity.setDeclaredQualifier(exp);
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            } catch (IllegalArgumentException ex) {
                // unparsable qualifier
                throw new ValidationException(ex.getMessage());
            }
        }
    }

    void setEntityName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        ObjEntity entity = mediator.getCurrentObjEntity();
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

            mediator.fireObjEntityEvent(e);

            // suggest to update class name
            ClassNameUpdater nameUpdater = new ClassNameUpdater(Application.getInstance().getFrameController(), entity);

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
                mediator.getCurrentObjEntity(),
                mediator.getCurrentDataMap(),
                (DataChannelDescriptor) mediator.getProject().getRootNode());
        mediator.fireObjEntityDisplayEvent(ede);
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        ObjEntity entity = (ObjEntity) e.getEntity();
        if (entity == null || !e.isEntityChanged()) {
            return;
        }

        initFromModel(entity);
    }

    private List<ObjAttribute> getDuplicatedAttributes(ObjEntity superEntity) {
        List<ObjAttribute> result = new LinkedList<>();

        ObjEntity entity = mediator.getCurrentObjEntity();

        for (ObjAttribute attribute : entity.getAttributes()) {
            if (superEntity.getAttribute(attribute.getName()) != null) {
                result.add(attribute);
            }
        }

        return result;
    }

    private void setComment(String value) {
        ObjEntity entity = mediator.getCurrentObjEntity();
        if (entity == null) {
            return;
        }

        ObjectInfo.putToMetaData(mediator.getApplication().getMetaData(), entity, ObjectInfo.COMMENT, value);
        mediator.fireObjEntityEvent(new EntityEvent(this, entity));
    }

    private String getComment(ObjEntity entity) {
        return ObjectInfo.getFromMetaData(mediator.getApplication().getMetaData(), entity, ObjectInfo.COMMENT);
    }

}
