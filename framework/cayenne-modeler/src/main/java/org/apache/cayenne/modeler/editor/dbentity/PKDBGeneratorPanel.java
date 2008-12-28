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
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.MutableComboBoxModel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.ProjectController;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class PKDBGeneratorPanel extends PKGeneratorPanel {

    private JComboBox attributes;

    public PKDBGeneratorPanel(ProjectController mediator) {
        super(mediator);
        initView();
    }

    private void initView() {

        attributes = new JComboBox();
        attributes.setEditable(false);
        attributes.setRenderer(new AttributeRenderer());

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
                "right:70dlu, 3dlu, fill:200dlu",
                ""));
        builder.setDefaultDialogBorder();
        builder.append("Auto Incremented:", attributes);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void setDbEntity(DbEntity entity) {
        // refresh only if this entity
        if (isVisible()) {
            updateView(entity);
        }
    }

    public void onInit(DbEntity entity) {
        resetStrategy(entity, true, false);

        Collection pkAttributes = entity.getPrimaryKeys();

        // by default check the only numeric PK
        if (pkAttributes.size() == 1) {
            DbAttribute pk = (DbAttribute) pkAttributes.iterator().next();
            if (TypesMapping.isNumeric(pk.getType()) && !pk.isGenerated()) {
                pk.setGenerated(true);
                mediator.fireDbEntityEvent(new EntityEvent(this, entity));
            }
        }

        updateView(entity);
    }

    void updateView(final DbEntity entity) {
        for (ItemListener listener : attributes.getItemListeners()) {
            attributes.removeItemListener(listener);
        }

        Collection<DbAttribute> pkAttributes = entity.getPrimaryKeys();
        if (pkAttributes.isEmpty()) {
            attributes.removeAllItems();
            attributes.addItem("<Entity has no PK columns>");
            attributes.setSelectedIndex(0);
            attributes.setEnabled(false);
        }
        else {

            attributes.setEnabled(true);
            MutableComboBoxModel model = new DefaultComboBoxModel(pkAttributes.toArray());
            String noSelection = "<Select Generated Column>";
            model.insertElementAt(noSelection, 0);
            model.setSelectedItem(noSelection);
            attributes.setModel(model);

            for (DbAttribute a : pkAttributes) {
                if (a.isGenerated()) {
                    model.setSelectedItem(a);
                    break;
                }
            }

            // listen for selection changes of the new entity
            attributes.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    Object item = e.getItem();
                    if (item instanceof DbAttribute) {

                        boolean generated = e.getStateChange() == ItemEvent.SELECTED;
                        DbAttribute a = (DbAttribute) item;

                        if (a.isGenerated() != generated) {
                            a.setGenerated(generated);
                            mediator.fireDbEntityEvent(new EntityEvent(this, entity));
                        }
                    }
                }
            });
        }

        // revalidate as children layout has changed...
        revalidate();
    }

    class AttributeRenderer extends BasicComboBoxRenderer {

        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            if (value instanceof DbAttribute) {
                DbAttribute a = (DbAttribute) value;
                String type = TypesMapping.getSqlNameByType(a.getType());
                value = a.getName() + " (" + (type != null ? type : "?") + ")";
            }

            return super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus);
        }
    }
}
