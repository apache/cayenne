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

package org.apache.cayenne.modeler.editor.dbentity;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventObject;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.editor.ExistingSelectionProcessor;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.util.ExpressionConvertor;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Detail view of the DbEntity properties.
 * 
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
    protected TextAdapter qualifier;
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
        qualifier = new TextAdapter(new JTextField()) {

            protected void updateModel(String qualifier) {
                setQualifier(qualifier);
            }
        };

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
        builder.append("Qualifier", qualifier.getComponent());

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
            //name.getComponent().requestFocusInWindow();
            return;
        }

        name.setText(entity.getName());
        schema.setText(entity.getSchema());
        qualifier.setText(new ExpressionConvertor().valueAsString(entity.getQualifier()));

        String type = PK_DEFAULT_GENERATOR;

        if (entity.getPrimaryKeyGenerator() != null) {
            type = PK_CUSTOM_SEQUENCE_GENERATOR;
        }
        else {
            for (DbAttribute a : entity.getPrimaryKeys()) {
                if (a.isGenerated()) {
                    type = PK_DB_GENERATOR;
                    break;
                }
            }
        }

        schemaLabel.setEnabled(true);
        schema.getComponent().setEnabled(true);
        pkGeneratorDetail.setVisible(true);
        pkGeneratorType.setVisible(true);

        pkGeneratorType.setSelectedItem(type);
        pkGeneratorDetailLayout.show(pkGeneratorDetail, type);

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
    
    void setQualifier(String qualifier) {

        if (qualifier != null && qualifier.trim().length() == 0) {
            qualifier = null;
        }

        DbEntity ent = mediator.getCurrentDbEntity();

        if (ent != null && !Util.nullSafeEquals(ent.getQualifier(), qualifier)) {
            ExpressionConvertor convertor = new ExpressionConvertor();
            try {
                String oldQualifier = convertor.valueAsString(ent.getQualifier());
                if (!Util.nullSafeEquals(oldQualifier, qualifier)) {
                    Expression exp = (Expression) convertor.stringAsValue(qualifier);
                    ent.setQualifier(exp);
                    mediator.fireDbEntityEvent(new EntityEvent(this, ent));
                }
            }
            catch (IllegalArgumentException ex) {
                // unparsable qualifier
                throw new ValidationException(ex.getMessage());
            }
            
        }
    }
}
