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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.LinkedList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.dialog.objentity.ClassNameUpdater;
import org.apache.cayenne.modeler.dialog.validator.DuplicatedAttributesDialog;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.ExpressionConvertor;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Detail view of the ObjEntity properties.
 */
public class ObjEntityTab extends JPanel implements ObjEntityDisplayListener,
        ExistingSelectionProcessor {

    private static final Object noInheritance = new CayenneMapEntry() {

        public String getName() {
            return "Direct Mapping to Table/View";
        }

        public Object getParent() {
            return null;
        }

        public void setParent(Object parent) {
        }
    };

    protected ProjectController mediator;
    protected TextAdapter name;
    protected TextAdapter className;

    protected JLabel superclassLabel;
    protected TextAdapter superClassName;
    protected TextAdapter qualifier;
    protected JComboBox dbEntityCombo;
    protected JButton syncWithDbEntityButton;
    protected JComboBox superEntityCombo;
    protected JButton tableLabel;
    protected JCheckBox readOnly;
    protected JCheckBox optimisticLocking;
    protected JCheckBox excludeSuperclassListeners;
    protected JCheckBox excludeDefaultListeners;

    protected JComponent clientSeparator;
    protected JLabel isAbstractLabel;
    protected JLabel serverOnlyLabel;
    protected JLabel clientClassNameLabel;
    protected JLabel clientSuperClassNameLabel;

    protected JCheckBox serverOnly;
    protected JCheckBox isAbstract;
    protected TextAdapter clientClassName;
    protected TextAdapter clientSuperClassName;

    public ObjEntityTab(ProjectController mediator) {
        this.mediator = mediator;
        initView();
        initController();
    }

    private void initView() {
        this.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        Application app = Application.getInstance();
        toolBar.add(app.getAction(ObjEntitySyncAction.getActionName()).buildButton());
        toolBar.add(app.getAction(CreateAttributeAction.getActionName()).buildButton());
        toolBar
                .add(app
                        .getAction(CreateRelationshipAction.getActionName())
                        .buildButton());
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

        dbEntityCombo = CayenneWidgetFactory.createComboBox();
        superEntityCombo = CayenneWidgetFactory.createComboBox();
        
        AutoCompletion.enable(dbEntityCombo);
        AutoCompletion.enable(superEntityCombo);

        readOnly = new JCheckBox();
        optimisticLocking = new JCheckBox();
        excludeSuperclassListeners = new JCheckBox();
        excludeDefaultListeners = new JCheckBox();

        tableLabel = CayenneWidgetFactory.createLabelButton("Table/View:");
        syncWithDbEntityButton = CayenneWidgetFactory.createButton("Sync w/DbEntity");
        syncWithDbEntityButton.setIcon(ModelerUtil.buildIcon("icon-sync.gif"));
        syncWithDbEntityButton.setToolTipText("Sync this ObjEntity with its DBEntity");

        isAbstract = new JCheckBox();
        serverOnly = new JCheckBox();
        clientClassName = new TextAdapter(new JTextField()) {

            @Override
            protected void updateModel(String text) {
                setClientClassName(text);
            }
        };
        clientSuperClassName = new TextAdapter(new JTextField()) {

            @Override
            protected void updateModel(String text) {
                setClientSuperClassName(text);
            }
        };

        // assemble
        FormLayout layout = new FormLayout(
                "right:100dlu, 3dlu, fill:135dlu, 3dlu, pref",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("ObjEntity Configuration");
        builder.append("ObjEntity Name:", name.getComponent(), 3);
        builder.append("Inheritance:", superEntityCombo, 3);
        builder.append(tableLabel, dbEntityCombo, syncWithDbEntityButton);
        isAbstractLabel = builder.append("Abstract class:", isAbstract, 3);

        builder.appendSeparator();

        builder.append("Java Class:", className.getComponent(), 3);
        superclassLabel = builder.append("Superclass:", superClassName.getComponent(), 3);
        builder.append("Qualifier:", qualifier.getComponent(), 3);
        builder.append("Read-Only:", readOnly, 3);
        builder.append("Optimistic Locking:", optimisticLocking, 3);
        // add callback-related stuff
        builder.append("Exclude superclass listeners:", excludeSuperclassListeners, 3);
        builder.append("Exclude default listeners:", excludeDefaultListeners, 3);

        clientSeparator = builder.appendSeparator("Java Client");
        serverOnlyLabel = builder.append("Not for Client Use:", serverOnly, 3);
        clientClassNameLabel = builder.append("Client Java Class:", clientClassName
                .getComponent(), 3);
        clientSuperClassNameLabel = builder.append(
                "Client Superclass:",
                clientSuperClassName.getComponent(),
                3);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initController() {
        // initialize events processing and tracking of UI updates...

        mediator.addObjEntityDisplayListener(this);

        dbEntityCombo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // Change DbEntity for current ObjEntity
                ObjEntity entity = mediator.getCurrentObjEntity();
                DbEntity dbEntity = (DbEntity) dbEntityCombo.getSelectedItem();
                syncWithDbEntityButton.setEnabled(dbEntity != null);

                if (dbEntity != entity.getDbEntity()) {
                    entity.setDbEntity(dbEntity);
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            }
        });

        superEntityCombo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // Change super-entity
                CayenneMapEntry superEntity = (CayenneMapEntry) superEntityCombo
                        .getSelectedItem();
                String name = (superEntity==null || superEntity == noInheritance) ? null : superEntity
                        .getName();

                ObjEntity entity = mediator.getCurrentObjEntity();

                if (!Util.nullSafeEquals(name, entity.getSuperEntityName())) {
                    List<ObjAttribute> duplicateAttributes = null;
                    if(name!=null){
                        duplicateAttributes = getDuplicatedAttributes((ObjEntity) superEntity);
                    }

                    if (duplicateAttributes!=null && duplicateAttributes.size() > 0) {
                        DuplicatedAttributesDialog.showDialog(Application.getFrame(), duplicateAttributes, (ObjEntity) superEntity, entity);
                        if (DuplicatedAttributesDialog.getResult().equals(DuplicatedAttributesDialog.CANCEL_RESULT)) {
                            superEntityCombo.setSelectedItem(entity.getSuperEntity());
                            superClassName.setText(entity.getSuperClassName());
                            return;
                        }
                        
                    }
                    entity.setSuperEntityName(name);

                    // if a super-entity selected, disable table selection
                    // and also update parent DbEntity selection...
                    toggleEnabled(name == null, !serverOnly.isSelected());
                    dbEntityCombo.getModel().setSelectedItem(entity.getDbEntity());
                    superClassName.setText(entity.getSuperClassName());

                    // fire both EntityEvent and EntityDisplayEvent;
                    // the later is to update attribute and relationship display

                    DataDomain domain = mediator.getCurrentDataDomain();
                    DataMap map = mediator.getCurrentDataMap();

                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                    mediator.fireObjEntityDisplayEvent(new EntityDisplayEvent(
                            this,
                            entity,
                            map,
                            domain));
                }
            }
        });

        tableLabel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // Jump to DbEntity of the current ObjEntity
                DbEntity entity = mediator.getCurrentObjEntity().getDbEntity();
                if (entity != null) {
                    DataDomain dom = mediator.getCurrentDataDomain();
                    mediator.fireDbEntityDisplayEvent(new EntityDisplayEvent(
                            this,
                            entity,
                            entity.getDataMap(),
                            dom));
                }
            }
        });

        syncWithDbEntityButton.addActionListener(new ObjEntitySyncAction(mediator
                .getApplication()));

        readOnly.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ObjEntity entity = mediator.getCurrentObjEntity();
                if (entity != null) {
                    entity.setReadOnly(readOnly.isSelected());
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            }
        });

        optimisticLocking.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ObjEntity entity = mediator.getCurrentObjEntity();
                if (entity != null) {
                    entity.setDeclaredLockType(optimisticLocking.isSelected()
                            ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                            : ObjEntity.LOCK_TYPE_NONE);
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            }
        });

        excludeSuperclassListeners.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ObjEntity entity = mediator.getCurrentObjEntity();
                if (entity != null) {
                    entity.setExcludingSuperclassListeners(excludeSuperclassListeners
                            .isSelected());
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            }
        });

        excludeDefaultListeners.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ObjEntity entity = mediator.getCurrentObjEntity();
                if (entity != null) {
                    entity.setExcludingDefaultListeners(excludeDefaultListeners
                            .isSelected());
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            }
        });

        serverOnly.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ObjEntity entity = mediator.getCurrentObjEntity();
                if (entity != null) {
                    entity.setServerOnly(serverOnly.isSelected());
                    toggleEnabled(dbEntityCombo.isEnabled(), !serverOnly.isSelected());
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            }
        });
        
        isAbstract.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ObjEntity entity = mediator.getCurrentObjEntity();
                if (entity != null) {
                    entity.setAbstract(isAbstract.isSelected());
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
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
        serverOnly.setSelected(entity.isServerOnly());
        clientClassName.setText(entity.getClientClassName());
        clientSuperClassName.setText(entity.getClientSuperClassName());

        qualifier.setText(new ExpressionConvertor().valueAsString(entity
                .getDeclaredQualifier()));

        // TODO: fix inheritance - we should allow to select optimistic
        // lock if superclass is not already locked,
        // otherwise we must keep this checked in but not editable.
        optimisticLocking
                .setSelected(entity.getDeclaredLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC);
        excludeSuperclassListeners.setSelected(entity.isExcludingSuperclassListeners());
        excludeDefaultListeners.setSelected(entity.isExcludingDefaultListeners());

        // init DbEntities
        DataMap map = mediator.getCurrentDataMap();
        Object[] dbEntities = map.getNamespace().getDbEntities().toArray();
        Arrays.sort(dbEntities, Comparators.getDataMapChildrenComparator());

        DefaultComboBoxModel dbModel = new DefaultComboBoxModel(dbEntities);
        dbModel.setSelectedItem(entity.getDbEntity());
        dbEntityCombo.setRenderer(CellRenderers.entityListRendererWithIcons(map));
        dbEntityCombo.setModel(dbModel);

        // toggle visibilty and editability rules

        toggleClientFieldsVisible(map.isClientSupported());
        toggleEnabled(entity.getSuperEntityName() == null, !entity.isServerOnly());

        // init ObjEntities for inheritance
        Predicate inheritanceFilter = new Predicate() {

            public boolean evaluate(Object object) {
                // do not show this entity or any of the subentities
                if (entity == object) {
                    return false;
                }

                if (object instanceof ObjEntity) {
                    return !((ObjEntity) object).isSubentityOf(entity);
                }

                return false;
            }
        };

        Object[] objEntities = CollectionUtils.select(
                map.getNamespace().getObjEntities(),
                inheritanceFilter).toArray();
        Arrays.sort(objEntities, Comparators.getDataMapChildrenComparator());
        Object[] finalObjEntities = new Object[objEntities.length + 1];
        finalObjEntities[0] = noInheritance;
        System.arraycopy(objEntities, 0, finalObjEntities, 1, objEntities.length);

        DefaultComboBoxModel superEntityModel = new DefaultComboBoxModel(finalObjEntities);
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

    void setClientClassName(String className) {
        if (className != null && className.trim().length() == 0) {
            className = null;
        }

        ObjEntity entity = mediator.getCurrentObjEntity();

        // "ent" may be null if we quit editing by changing tree selection
        if (entity != null
                && !Util.nullSafeEquals(entity.getClientClassName(), className)) {
            entity.setClientClassName(className);
            mediator.fireObjEntityEvent(new EntityEvent(this, entity));
        }
    }

    void setClientSuperClassName(String text) {

        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        ObjEntity ent = mediator.getCurrentObjEntity();

        if (ent != null && !Util.nullSafeEquals(ent.getClientSuperClassName(), text)) {
            ent.setClientSuperClassName(text);
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
                String oldQualifier = convertor.valueAsString(entity
                        .getDeclaredQualifier());
                if (!Util.nullSafeEquals(oldQualifier, text)) {
                    Expression exp = (Expression) convertor.stringAsValue(text);
                    entity.setDeclaredQualifier(exp);
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            }
            catch (IllegalArgumentException ex) {
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
        }
        else if (entity.getDataMap().getObjEntity(newName) == null) {
            // completely new name, set new name for entity
            EntityEvent e = new EntityEvent(this, entity, entity.getName());
            entity.setName(newName);

            mediator.fireObjEntityEvent(e);

            // suggest to update class name
            ClassNameUpdater nameUpdater = new ClassNameUpdater(Application
                    .getInstance()
                    .getFrameController(), entity);

            if (nameUpdater.doNameUpdate()) {
                className.setText(entity.getClassName());
                clientClassName.setText(entity.getClientClassName());
            }
        }
        else {
            // there is an entity with the same name
            throw new ValidationException("There is another entity with name '"
                    + newName
                    + "'.");
        }
    }

    void toggleClientFieldsVisible(boolean visible) {

        clientSeparator.setVisible(visible);
        clientSuperClassNameLabel.setVisible(visible);
        clientClassNameLabel.setVisible(visible);
        serverOnlyLabel.setVisible(visible);

        clientClassName.getComponent().setVisible(visible);
        clientSuperClassName.getComponent().setVisible(visible);
        serverOnly.setVisible(visible);
    }

    void toggleEnabled(boolean directTableMapping, boolean clientFieldsEnabled) {
        superClassName.getComponent().setEnabled(directTableMapping);
        superclassLabel.setEnabled(directTableMapping);

        syncWithDbEntityButton.setEnabled(dbEntityCombo.getSelectedItem() != null);

        clientSuperClassName.getComponent().setEnabled(
                directTableMapping && clientFieldsEnabled);
        clientSuperClassNameLabel.setEnabled(directTableMapping && clientFieldsEnabled);

        clientClassNameLabel.setEnabled(clientFieldsEnabled);
        clientClassName.getComponent().setEnabled(clientFieldsEnabled);
    }

    public void processExistingSelection(EventObject e) {

        EntityDisplayEvent ede = new EntityDisplayEvent(this, mediator
                .getCurrentObjEntity(), mediator.getCurrentDataMap(), mediator
                .getCurrentDataDomain());
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
        List<ObjAttribute> result = new LinkedList<ObjAttribute>();

        ObjEntity entity = mediator.getCurrentObjEntity();

        for (ObjAttribute attribute : entity.getAttributes()) {
            if(superEntity.getAttribute(attribute.getName()) != null) {
                result.add(attribute);
            }
        }
        
        return result;
    }

}
