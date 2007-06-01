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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.dba.JdbcPkGenerator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbKeyGenerator;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
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
        DbEntityDisplayListener, ActionListener {

    protected ProjectController mediator;

    protected TextAdapter name;
    protected JTextField schema;
    protected JComboBox parentEntities;
    protected JButton parentLabel;
    protected JLabel schemaLabel;

    protected JCheckBox customPKGenerator;
    protected JLabel customPKGeneratorLabel;
    protected JLabel customPKGeneratorNote;
    protected JLabel customPKGeneratorNameLabel;
    protected JLabel customPKSizeLabel;
    protected JTextField customPKName;
    protected JTextField customPKSize;

    public DbEntityTab(ProjectController mediator) {
        super();
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
        schemaLabel = CayenneWidgetFactory.createLabel("Schema:");
        schema = CayenneWidgetFactory.createTextField();

        parentLabel = CayenneWidgetFactory.createLabelButton("Parent DbEntity:");
        parentLabel.setEnabled(false);

        parentEntities = CayenneWidgetFactory.createComboBox();
        parentEntities.setEditable(false);
        parentEntities.setEnabled(false);

        customPKGenerator = new JCheckBox();
        customPKGeneratorLabel = CayenneWidgetFactory
                .createLabel("Customize primary key generation");
        customPKGeneratorNote = CayenneWidgetFactory
                .createLabel("(currently ignored by all adapters except Oracle)");
        customPKGeneratorNote.setFont(customPKGeneratorNote.getFont().deriveFont(10));
        customPKGeneratorNameLabel = CayenneWidgetFactory
                .createLabel("Database object name: ");
        customPKSizeLabel = CayenneWidgetFactory.createLabel("Cached PK Size: ");

        customPKName = CayenneWidgetFactory.createTextField();
        customPKSize = CayenneWidgetFactory.createTextField();

        // assemble
        setLayout(new BorderLayout());
        FormLayout layout = new FormLayout(
                "right:max(50dlu;pref), 3dlu, fill:max(200dlu;pref)",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("DbEntity Configuration");
        builder.append("DbEntity name:", name.getComponent());
        builder.append(schemaLabel, schema);
        builder.append(parentLabel, parentEntities);

        builder.appendSeparator("PK Generation");
        builder.append(customPKGenerator, customPKGeneratorLabel);
        builder.append("", customPKGeneratorNote);
        builder.append(customPKGeneratorNameLabel, customPKName);
        builder.append(customPKSizeLabel, customPKSize);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initController() {
        mediator.addDbEntityDisplayListener(this);
        InputVerifier inputCheck = new FieldVerifier();
        schema.setInputVerifier(inputCheck);
        customPKName.setInputVerifier(inputCheck);
        customPKSize.setInputVerifier(inputCheck);

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

        parentLabel.addActionListener(this);
        customPKGenerator.addActionListener(this);
    }

    public void processExistingSelection() {
        EntityDisplayEvent e = new EntityDisplayEvent(
                this,
                mediator.getCurrentDbEntity(),
                mediator.getCurrentDataMap(),
                mediator.getCurrentDataDomain());
        mediator.fireDbEntityDisplayEvent(e);
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        DbEntity entity = (DbEntity) e.getEntity();
        if (null == entity || !e.isEntityChanged()) {
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
            updateState(false);
            parentEntities.setSelectedIndex(-1);
        }
    }

    /**
     * Enables or disbales form fields depending on the type of entity shown.
     */
    protected void updateState(boolean isDerivedEntity) {
        schemaLabel.setEnabled(!isDerivedEntity);
        schema.setEnabled(!isDerivedEntity);

        parentLabel.setEnabled(isDerivedEntity);
        parentEntities.setEnabled(isDerivedEntity);
        parentLabel.setVisible(isDerivedEntity);
        parentEntities.setVisible(isDerivedEntity);

        DbEntity entity = mediator.getCurrentDbEntity();
        updatePrimaryKeyGeneratorView(entity);
    }

    protected void updatePrimaryKeyGeneratorView(DbEntity entity) {
        DbKeyGenerator generator = entity.getPrimaryKeyGenerator();
        boolean isPKGeneratorCustomized = generator != null;

        customPKGenerator.setSelected(isPKGeneratorCustomized);

        customPKGeneratorNameLabel.setEnabled(isPKGeneratorCustomized);
        customPKSizeLabel.setEnabled(isPKGeneratorCustomized);

        customPKName.setEnabled(isPKGeneratorCustomized);
        customPKSize.setEnabled(isPKGeneratorCustomized);

        if (isPKGeneratorCustomized) {
            customPKName.setText(generator.getGeneratorName());
            customPKSize.setText(generator.getKeyCacheSize() != null ? generator
                    .getKeyCacheSize()
                    .toString() : "0");
        }
        else {
            customPKName.setText("");
            customPKSize.setText("");
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (parentLabel == e.getSource()) {
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
        else if (customPKGenerator == e.getSource()) {
            DbEntity entity = mediator.getCurrentDbEntity();
            if (entity == null) {
                return;
            }

            if (customPKGenerator.isSelected()) {
                if (entity.getPrimaryKeyGenerator() == null) {
                    DbKeyGenerator generator = new DbKeyGenerator();
                    generator.setGeneratorType(DbKeyGenerator.ORACLE_TYPE);
                    generator.setKeyCacheSize(new Integer(
                            JdbcPkGenerator.DEFAULT_PK_CACHE_SIZE));
                    entity.setPrimaryKeyGenerator(generator);
                }

                updatePrimaryKeyGeneratorView(entity);
            }
            else {
                entity.setPrimaryKeyGenerator(null);
                updatePrimaryKeyGeneratorView(entity);
            }
        }
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
            ProjectUtil.setDbEntityName(entity, newName);
            mediator.fireDbEntityEvent(e);
        }
        else {
            // there is an entity with the same name
            throw new ValidationException("There is another entity with name '"
                    + newName
                    + "'.");
        }
    }

    class FieldVerifier extends InputVerifier {

        public boolean verify(JComponent input) {
            if (input == schema) {
                return verifySchema();
            }
            else if (input == customPKSize) {
                return verifyCustomPKSize();
            }
            else if (input == customPKName) {
                return verifyCustomPKName();
            }
            else {
                return true;
            }
        }

        protected boolean verifySchema() {
            String text = schema.getText();
            if (text != null && text.trim().length() == 0) {
                text = null;
            }

            DbEntity ent = mediator.getCurrentDbEntity();

            if (ent != null && !Util.nullSafeEquals(ent.getSchema(), text)) {
                ent.setSchema(text);
                mediator.fireDbEntityEvent(new EntityEvent(this, ent));
            }

            return true;
        }

        protected boolean verifyCustomPKSize() {
            String text = customPKSize.getText();
            int cacheSize = 0;

            if (text != null && text.trim().length() > 0) {
                try {
                    cacheSize = Integer.parseInt(text);
                }
                catch (NumberFormatException nfex) {
                }
            }

            // erase any incorrect input
            if (cacheSize == 0) {
                customPKSize.setText("0");
            }

            DbEntity entity = mediator.getCurrentDbEntity();
            DbKeyGenerator generator = entity.getPrimaryKeyGenerator();

            if (generator != null
                    && (generator.getKeyCacheSize() == null || generator
                            .getKeyCacheSize()
                            .intValue() != cacheSize)) {
                generator.setKeyCacheSize(new Integer(cacheSize));
                mediator.fireDbEntityEvent(new EntityEvent(this, entity));
            }

            return true;
        }

        protected boolean verifyCustomPKName() {
            String text = customPKName.getText();
            if (text != null && text.trim().length() == 0) {
                text = null;
            }

            DbEntity entity = mediator.getCurrentDbEntity();
            DbKeyGenerator generator = entity.getPrimaryKeyGenerator();

            if (generator != null && (!Util.nullSafeEquals(text, generator.getName()))) {
                generator.setGeneratorName(text);
                mediator.fireDbEntityEvent(new EntityEvent(this, entity));
            }
            return true;
        }
    }
}