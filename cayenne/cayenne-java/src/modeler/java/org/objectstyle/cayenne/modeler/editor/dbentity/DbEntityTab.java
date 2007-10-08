/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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

package org.objectstyle.cayenne.modeler.editor.dbentity;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.action.CreateAttributeAction;
import org.objectstyle.cayenne.modeler.action.CreateObjEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateRelationshipAction;
import org.objectstyle.cayenne.modeler.action.DbEntitySyncAction;
import org.objectstyle.cayenne.modeler.editor.ExistingSelectionProcessor;
import org.objectstyle.cayenne.modeler.event.DbEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.modeler.util.TextAdapter;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Detail view of the DbEntity properties.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class DbEntityTab extends JPanel implements ExistingSelectionProcessor,
        DbEntityDisplayListener {

    static final String PK_DEFAULT_GENERATOR = "Default";
    static final String PK_DB_GENERATOR = "Database-Generated";
    static final String PK_CUSTOM_SEQUENCE_GENERATOR = "Custom Sequence";

    static final String[] PK_GENERATOR_TYPES = new String[] {
            PK_DEFAULT_GENERATOR, PK_DB_GENERATOR, PK_CUSTOM_SEQUENCE_GENERATOR
    };

    protected ProjectController mediator;

    protected TextAdapter name;
    protected TextAdapter schema;
    protected JComboBox parentEntities;
    protected JButton parentLabel;
    protected JLabel schemaLabel;

    protected JComboBox pkGeneratorType;
    protected JPanel pkGeneratorDetail;
    protected CardLayout pkGeneratorDetailLayout;

    public DbEntityTab(ProjectController mediator) {
        super();
        this.mediator = mediator;

        initView();
        initController();
    }

    private void initView() {

        JToolBar toolBar = new JToolBar();
        Application app = Application.getInstance();
        toolBar.add(app.getAction(CreateObjEntityAction.getActionName()).buildButton());
        toolBar.add(app.getAction(DbEntitySyncAction.getActionName()).buildButton());
        toolBar.addSeparator();

        toolBar.add(app.getAction(CreateAttributeAction.getActionName()).buildButton());
        toolBar
                .add(app
                        .getAction(CreateRelationshipAction.getActionName())
                        .buildButton());

        // create widgets
        name = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setEntityName(text);
            }
        };
        schemaLabel = new JLabel("Schema:");
        schema = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) throws ValidationException {
                setSchema(text);
            }
        };

        parentLabel = CayenneWidgetFactory.createLabelButton("Parent DbEntity:");
        parentLabel.setEnabled(false);

        parentEntities = CayenneWidgetFactory.createComboBox();
        parentEntities.setEditable(false);
        parentEntities.setEnabled(false);

        pkGeneratorType = new JComboBox();
        pkGeneratorType.setEditable(false);
        pkGeneratorType.setModel(new DefaultComboBoxModel(PK_GENERATOR_TYPES));

        pkGeneratorDetailLayout = new CardLayout();
        pkGeneratorDetail = new JPanel(pkGeneratorDetailLayout);
        pkGeneratorDetail
                .add(new PKDefaultGeneratorPanel(mediator), PK_DEFAULT_GENERATOR);
        pkGeneratorDetail.add(new PKDBGeneratorPanel(mediator), PK_DB_GENERATOR);
        pkGeneratorDetail.add(
                new PKCustomSequenceGeneratorPanel(mediator),
                PK_CUSTOM_SEQUENCE_GENERATOR);

        // assemble
        FormLayout layout = new FormLayout("right:pref, 3dlu, fill:200dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("DbEntity Configuration");
        builder.append("DbEntity Name:", name.getComponent());
        builder.append(schemaLabel, schema.getComponent());
        builder.append(parentLabel, parentEntities);

        builder.appendSeparator("Primary Key");
        builder.append("PK Generation Strategy:", pkGeneratorType);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        mainPanel.add(builder.getPanel(), BorderLayout.NORTH);
        mainPanel.add(pkGeneratorDetail, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void initController() {
        mediator.addDbEntityDisplayListener(this);

        parentEntities.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                DbEntity current = mediator.getCurrentDbEntity();

                if (current instanceof DerivedDbEntity) {
                    DerivedDbEntity derived = (DerivedDbEntity) current;
                    DbEntity parent = (DbEntity) parentEntities.getSelectedItem();

                    if (parent != derived.getParentEntity()) {
                        derived.setParentEntity(parent);
                        derived.resetToParentView();
                        ProjectUtil.cleanObjMappings(mediator.getCurrentDataMap());

                        EntityEvent event = new EntityEvent(this, current);
                        mediator.fireDbEntityEvent(event);
                    }
                }
            }
        });

        parentLabel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                DbEntity current = mediator.getCurrentDbEntity();

                if (current instanceof DerivedDbEntity) {
                    DbEntity parent = ((DerivedDbEntity) current).getParentEntity();
                    if (parent != null) {
                        DataDomain dom = mediator.getCurrentDataDomain();
                        mediator.fireDbEntityDisplayEvent(new EntityDisplayEvent(
                                this,
                                parent,
                                parent.getDataMap(),
                                dom));
                    }
                }
            }
        });

        pkGeneratorType.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                pkGeneratorDetailLayout.show(pkGeneratorDetail, (String) pkGeneratorType
                        .getSelectedItem());

                for (int i = 0; i < pkGeneratorDetail.getComponentCount(); i++) {
                    if (pkGeneratorDetail.getComponent(i).isVisible()) {

                        DbEntity entity = mediator.getCurrentDbEntity();
                        PKGeneratorPanel panel = (PKGeneratorPanel) pkGeneratorDetail
                                .getComponent(i);
                        panel.onInit(entity);
                        break;
                    }
                }
            }
        });
    }

    public void processExistingSelection(EventObject e) {
        EntityDisplayEvent ede = new EntityDisplayEvent(this, mediator
                .getCurrentDbEntity(), mediator.getCurrentDataMap(), mediator
                .getCurrentDataDomain());
        mediator.fireDbEntityDisplayEvent(ede);
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        DbEntity entity = (DbEntity) e.getEntity();

        if (entity == null) {
            return;
        }

        // if entity hasn't changed, still notify PK Generator panels, as entity PK may
        // have changed...

        for (int i = 0; i < pkGeneratorDetail.getComponentCount(); i++) {
            ((PKGeneratorPanel) pkGeneratorDetail.getComponent(i)).setDbEntity(entity);
        }

        if (!e.isEntityChanged()) {
            return;
        }

        name.setText(entity.getName());
        schema.setText(entity.getSchema());

        if (entity instanceof DerivedDbEntity) {

            updateState(true);

            // build a list consisting of non-derived entities

            DataMap map = mediator.getCurrentDataMap();
            Collection allEntities = map.getNamespace().getDbEntities();
            java.util.List entities = new ArrayList(allEntities.size());
            Iterator it = allEntities.iterator();

            while (it.hasNext()) {
                DbEntity parentEntity = (DbEntity) it.next();
                if (!(parentEntity instanceof DerivedDbEntity)) {
                    entities.add(parentEntity);
                }
            }

            DefaultComboBoxModel model = new DefaultComboBoxModel(entities.toArray());
            model.setSelectedItem(((DerivedDbEntity) entity).getParentEntity());
            parentEntities.setRenderer(CellRenderers.entityListRendererWithIcons(map));
            parentEntities.setModel(model);
        }
        else {
            String type = PK_DEFAULT_GENERATOR;

            if (entity.getPrimaryKeyGenerator() != null) {
                type = PK_CUSTOM_SEQUENCE_GENERATOR;
            }
            else {
                Iterator it = entity.getPrimaryKey().iterator();
                while (it.hasNext()) {
                    DbAttribute a = (DbAttribute) it.next();
                    if (a.isGenerated()) {
                        type = PK_DB_GENERATOR;
                        break;
                    }
                }
            }

            updateState(false);
            pkGeneratorType.setSelectedItem(type);
            pkGeneratorDetailLayout.show(pkGeneratorDetail, type);

            parentEntities.setSelectedIndex(-1);
        }
    }

    /**
     * Enables or disbales form fields depending on the type of entity shown.
     */
    protected void updateState(boolean isDerivedEntity) {
        schemaLabel.setEnabled(!isDerivedEntity);
        schema.getComponent().setEnabled(!isDerivedEntity);

        parentLabel.setEnabled(isDerivedEntity);
        parentEntities.setEnabled(isDerivedEntity);
        parentLabel.setVisible(isDerivedEntity);
        parentEntities.setVisible(isDerivedEntity);

        pkGeneratorDetail.setVisible(!isDerivedEntity);
        pkGeneratorType.setVisible(!isDerivedEntity);
    }

    void setEntityName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        DbEntity entity = mediator.getCurrentDbEntity();

        if (entity == null || Util.nullSafeEquals(newName, entity.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Entity name is required.");
        }
        else if (entity.getDataMap().getDbEntity(newName) == null) {
            // completely new name, set new name for entity
            EntityEvent e = new EntityEvent(this, entity, entity.getName());
            entity.setName(newName);
            // ProjectUtil.setDbEntityName(entity, newName);
            mediator.fireDbEntityEvent(e);
        }
        else {
            // there is an entity with the same name
            throw new ValidationException("There is another entity with name '"
                    + newName
                    + "'.");
        }
    }

    void setSchema(String text) {

        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        DbEntity ent = mediator.getCurrentDbEntity();

        if (ent != null && !Util.nullSafeEquals(ent.getSchema(), text)) {
            ent.setSchema(text);
            mediator.fireDbEntityEvent(new EntityEvent(this, ent));
        }
    }
}