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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * A main panel for editing a SQLTemplate.
 * 
 */
public class SQLTemplateMainTab extends JPanel {

    private static final String DEFAULT_CAPS_LABEL = "Database Default";
    private static final String LOWER_CAPS_LABEL = "Force Lower Case";
    private static final String UPPER_CAPS_LABEL = "Force Upper Case";

    private static final CapsStrategy[] LABEL_CAPITALIZATION = {
            CapsStrategy.DEFAULT, CapsStrategy.LOWER, CapsStrategy.UPPER
    };

    private static final Map<CapsStrategy, String> labelCapsLabels = new HashMap<>();

    static {
        labelCapsLabels.put(CapsStrategy.DEFAULT, DEFAULT_CAPS_LABEL);
        labelCapsLabels.put(CapsStrategy.LOWER, LOWER_CAPS_LABEL);
        labelCapsLabels.put(CapsStrategy.UPPER, UPPER_CAPS_LABEL);
    }

    protected ProjectController mediator;
    protected TextAdapter name;
    protected SelectPropertiesPanel properties;

    public SQLTemplateMainTab(ProjectController mediator) {
        this.mediator = mediator;

        initView();
    }

    private void initView() {
        // create widgets
        name = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setQueryName(text);
            }
        };

        properties = new SQLTemplateQueryPropertiesPanel(mediator);

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, fill:max(200dlu;pref)",
                "p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("SQLTemplate Settings", cc.xywh(1, 1, 3, 1));
        builder.addLabel("Query Name:", cc.xy(1, 3));
        builder.add(name.getComponent(), cc.xy(3, 3));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.NORTH);
        this.add(properties, BorderLayout.CENTER);
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    void initFromModel() {
        QueryDescriptor query = mediator.getCurrentQuery();

        if (query == null || !QueryDescriptor.SQL_TEMPLATE.equals(query.getType())) {
            setVisible(false);
            return;
        }

        name.setText(query.getName());
        properties.initFromModel(query);

        setVisible(true);
    }

    protected QueryDescriptor getQuery() {
        QueryDescriptor query = mediator.getCurrentQuery();
        return (query != null && QueryDescriptor.SQL_TEMPLATE.equals(query.getType())) ? query : null;
    }

    /**
     * Initializes Query name from string.
     */
    void setQueryName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        QueryDescriptor query = getQuery();

        if (query == null) {
            return;
        }

        if (Util.nullSafeEquals(newName, query.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Query name is required.");
        }

        DataMap map = mediator.getCurrentDataMap();

        if (map.getQueryDescriptor(newName) == null) {
            // completely new name, set new name for entity
            QueryEvent e = new QueryEvent(this, query, query.getName());
            ProjectUtil.setQueryName(map, query, newName);
            mediator.fireQueryEvent(e);
        }
        else {
            // there is a query with the same name
            throw new ValidationException("There is another query named '"
                    + newName
                    + "'. Use a different name.");
        }
    }

    /**
     * Returns an entity that maps to a procedure query result class.
     */
    ObjEntity getEntity(QueryDescriptor query) {
        return query != null && query.getRoot() instanceof ObjEntity ? (ObjEntity) query
                .getRoot() : null;
    }

    void setEntity(ObjEntity entity) {
        QueryDescriptor template = getQuery();
        if (template != null) {
            // in case of null entity, set root to DataMap
            Object root = entity != null ? entity : mediator.getCurrentDataMap();
            template.setRoot(root);

            mediator.fireQueryEvent(new QueryEvent(this, template));
        }
    }

    final class LabelCapsRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(
                JList list,
                Object object,
                int arg2,
                boolean arg3,
                boolean arg4) {
            object = labelCapsLabels.get(object);
            return super.getListCellRendererComponent(list, object, arg2, arg3, arg4);
        }
    }

    final class SQLTemplateQueryPropertiesPanel extends RawQueryPropertiesPanel {

        private JComboBox labelCase;

        SQLTemplateQueryPropertiesPanel(ProjectController mediator) {
            super(mediator);
        }

        protected PanelBuilder createPanelBuilder() {
            labelCase = Application.getWidgetFactory().createUndoableComboBox();
            labelCase.setRenderer(new LabelCapsRenderer());

            labelCase.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent event) {
                    CapsStrategy value = (CapsStrategy) labelCase.getModel().getSelectedItem();
                    setQueryProperty(SQLTemplate.COLUMN_NAME_CAPITALIZATION_PROPERTY, value.name());
                }
            });

            PanelBuilder builder = super.createPanelBuilder();

            RowSpec[] extraRows = RowSpec.decodeSpecs("3dlu, p");
            for (RowSpec extraRow : extraRows) {
                builder.appendRow(extraRow);
            }

            CellConstraints cc = new CellConstraints();
            builder.addLabel("Row Label Case:", cc.xy(1, 17));
            builder.add(labelCase, cc.xywh(3, 17, 5, 1));

            return builder;
        }

        public void initFromModel(QueryDescriptor query) {
            super.initFromModel(query);

            if (query != null && QueryDescriptor.SQL_TEMPLATE.equals(query.getType())) {
                DefaultComboBoxModel labelCaseModel = new DefaultComboBoxModel(
                        LABEL_CAPITALIZATION);

                String columnNameCapitalization = query.getProperty(SQLTemplate.COLUMN_NAME_CAPITALIZATION_PROPERTY);

                labelCaseModel.setSelectedItem(columnNameCapitalization != null ?
                        CapsStrategy.valueOf(columnNameCapitalization) : CapsStrategy.DEFAULT);
                labelCase.setModel(labelCaseModel);
            }
        }

        protected void setEntity(ObjEntity entity) {
            SQLTemplateMainTab.this.setEntity(entity);
        }

        public ObjEntity getEntity(QueryDescriptor query) {
            if (query != null && QueryDescriptor.SQL_TEMPLATE.equals(query.getType())) {
                return SQLTemplateMainTab.this.getEntity(query);
            }

            return null;
        }

        @Override
        protected void setFetchingDataObjects(boolean dataObjects) {
            super.setFetchingDataObjects(dataObjects);
            if(!dataObjects) {
                setEntity(null);
            }
        }
    };
}
