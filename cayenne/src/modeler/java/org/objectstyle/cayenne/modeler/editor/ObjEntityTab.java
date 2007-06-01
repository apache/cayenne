/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.MapObject;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.Comparators;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.modeler.util.TextAdapter;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.validation.ValidationException;
import org.scopemvc.util.convertor.StringConvertor;
import org.scopemvc.util.convertor.StringConvertors;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Detail view of the ObjEntity properties.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class ObjEntityTab extends JPanel implements ObjEntityDisplayListener,
        ExistingSelectionProcessor {

    private static final Object noInheritance = new MapObject(
            "Direct Mapping to Table/View") {

        public void encodeAsXML(XMLEncoder encoder) {
        }
    };

    protected ProjectController mediator;
    protected TextAdapter name;
    protected TextAdapter className;
    protected TextAdapter superClassName;
    protected TextAdapter qualifier;
    protected JComboBox dbEntityCombo;
    protected JComboBox superEntityCombo;
    protected JButton tableLabel;
    protected JCheckBox readOnly;
    protected JCheckBox optimisticLocking;

    public ObjEntityTab(ProjectController mediator) {
        this.mediator = mediator;
        initView();
        initController();
    }

    private void initView() {
        // create widgets
        name = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setEntityName(text);
            }
        };
        superClassName = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setSuperClassName(text);
            }
        };
        className = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setClassName(text);
            }
        };
        qualifier = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setQualifier(text);
            }
        };

        dbEntityCombo = CayenneWidgetFactory.createComboBox();
        superEntityCombo = CayenneWidgetFactory.createComboBox();

        readOnly = new JCheckBox();
        optimisticLocking = new JCheckBox();

        tableLabel = CayenneWidgetFactory.createLabelButton("Table/View:");

        // assemble
        setLayout(new BorderLayout());
        FormLayout layout = new FormLayout(
                "right:max(50dlu;pref), 3dlu, fill:max(200dlu;pref)",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("ObjEntity Configuration");
        builder.append("ObjEntity Name:", name.getComponent());
        builder.append("Inheritance:", superEntityCombo);
        builder.append(tableLabel, dbEntityCombo);

        builder.appendSeparator();

        builder.append("Java Class:", className.getComponent());
        builder.append("Superclass:", superClassName.getComponent());
        builder.append("Qualifier", qualifier.getComponent());
        builder.append("Read-Only:", readOnly);
        builder.append("Optimistic Locking:", optimisticLocking);

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

                if (dbEntity != entity.getDbEntity()) {
                    entity.setDbEntity(dbEntity);
                    mediator.fireObjEntityEvent(new EntityEvent(this, entity));
                }
            }
        });

        superEntityCombo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // Change super-entity
                MapObject superEntity = (MapObject) superEntityCombo.getSelectedItem();
                String name = (superEntity == noInheritance) ? null : superEntity
                        .getName();

                ObjEntity entity = mediator.getCurrentObjEntity();

                if (!Util.nullSafeEquals(name, entity.getSuperEntityName())) {
                    entity.setSuperEntityName(name);

                    // if a super-entity selected, disable table selection
                    // and also update parent DbEntity selection...
                    activateFields(name == null);
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
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * ObjEntity is changed.
     */
    private void initFromModel(final ObjEntity entity) {
        // TODO: this is a hack until we implement a real MVC
        qualifier.getComponent().setBackground(Color.WHITE);

        name.setText(entity.getName());
        superClassName.setText(entity.getSuperClassName() != null ? entity
                .getSuperClassName() : "");
        className.setText(entity.getClassName() != null ? entity.getClassName() : "");
        readOnly.setSelected(entity.isReadOnly());

        StringConvertor convertor = StringConvertors.forClass(Expression.class);
        qualifier.setText(convertor.valueAsString(entity.getDeclaredQualifier()));

        // TODO: fix inheritance - we should allow to select optimistic
        // lock if superclass is not already locked,
        // otherwise we must keep this checked in but not editable.
        optimisticLocking
                .setSelected(entity.getDeclaredLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC);

        // init DbEntities
        DataMap map = mediator.getCurrentDataMap();
        Object[] dbEntities = map.getNamespace().getDbEntities().toArray();
        Arrays.sort(dbEntities, Comparators.getDataMapChildrenComparator());

        DefaultComboBoxModel dbModel = new DefaultComboBoxModel(dbEntities);
        dbModel.setSelectedItem(entity.getDbEntity());
        dbEntityCombo.setRenderer(CellRenderers.entityListRendererWithIcons(map));
        dbEntityCombo.setModel(dbModel);

        // if a super-entity selected, disable table selection
        activateFields(entity.getSuperEntityName() == null);

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

    void setQualifier(String text) {
        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        ObjEntity entity = mediator.getCurrentObjEntity();
        if (entity != null) {

            StringConvertor convertor = StringConvertors.forClass(Expression.class);
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
            ProjectUtil.setObjEntityName(entity.getDataMap(), entity, newName);
            mediator.fireObjEntityEvent(e);

            // suggest to update class name
            String suggestedClassName = suggestedClassName(entity);
            if (suggestedClassName != null
                    && !suggestedClassName.equals(entity.getClassName())) {
                JOptionPane pane = new JOptionPane(
                        new Object[] {
                                "Change class name to match new entity name?",
                                "Suggested class name: " + suggestedClassName
                        },
                        JOptionPane.QUESTION_MESSAGE,
                        JOptionPane.OK_CANCEL_OPTION,
                        null,
                        new Object[] {
                                "Change", "Cancel"
                        });

                pane.createDialog(Application.getFrame(), "Update Class Name").show();
                if ("Change".equals(pane.getValue())) {
                    className.setText(suggestedClassName);
                    setClassName(suggestedClassName);
                }
            }
        }
        else {
            // there is an entity with the same name
            throw new ValidationException("There is another entity with name '"
                    + newName
                    + "'.");
        }
    }

    /**
     * Build a class name based on current entity name.
     */
    private String suggestedClassName(ObjEntity entity) {
        String name = entity.getName();
        if (name == null || name.trim().length() == 0) {
            return null;
        }

        // build suggested package...
        String oldFQN = entity.getClassName();
        String pkg = (entity.getDataMap() != null) ? entity
                .getDataMap()
                .getDefaultPackage() : null;

        if (oldFQN != null && oldFQN.lastIndexOf('.') > 0) {
            pkg = oldFQN.substring(0, oldFQN.lastIndexOf('.'));
        }

        if (pkg == null) {
            pkg = "";
        }
        else {
            pkg = pkg + '.';
        }

        // build suggested class name
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            name = name.substring(dot + 1);
        }

        return pkg + name;
    }

    void activateFields(boolean active) {
        superClassName.getComponent().setEnabled(active);
        superClassName.getComponent().setEditable(active);
        dbEntityCombo.setEnabled(active);
    }

    public void processExistingSelection() {
        EntityDisplayEvent e = new EntityDisplayEvent(this, mediator
                .getCurrentObjEntity(), mediator.getCurrentDataMap(), mediator
                .getCurrentDataDomain());
        mediator.fireObjEntityDisplayEvent(e);
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        ObjEntity entity = (ObjEntity) e.getEntity();
        if (entity == null || !e.isEntityChanged()) {
            return;
        }

        initFromModel(entity);
    }

}