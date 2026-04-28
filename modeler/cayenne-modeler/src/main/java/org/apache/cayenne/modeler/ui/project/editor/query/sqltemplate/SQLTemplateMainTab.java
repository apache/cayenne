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

package org.apache.cayenne.modeler.ui.project.editor.query.sqltemplate;

import org.apache.cayenne.modeler.ui.project.editor.query.BaseQueryMainTab;
import org.apache.cayenne.modeler.ui.project.editor.query.RawQueryPropertiesPanel;
import org.apache.cayenne.modeler.ui.project.editor.query.SelectPropertiesPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.toolkit.WidgetFactory;
import org.apache.cayenne.modeler.toolkit.text.CayenneUndoableTextField;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A main panel for editing a SQLTemplate.
 * 
 */
public class SQLTemplateMainTab extends BaseQueryMainTab {

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

    protected CayenneUndoableTextField comment;
    protected SelectPropertiesPanel properties;

    public SQLTemplateMainTab(ProjectController mediator) {
        super(mediator);

        initQueryRoot();
        initView();
    }

    private void initView() {
        // create widgets
        name = new CayenneUndoableTextField(controller.getApplication().getUndoManager());
        name.addCommitListener(this::setQueryName);

        comment = new CayenneUndoableTextField(controller.getApplication().getUndoManager());
        comment.addCommitListener(this::setQueryComment);

        properties = new SQLTemplateQueryPropertiesPanel(controller);

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, fill:max(200dlu;pref)",
                "p, 3dlu, p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("SQLTemplate Settings", cc.xywh(1, 1, 3, 1));
        builder.addLabel("Query Name:", cc.xy(1, 3));
        builder.add(name, cc.xy(3, 3));
        builder.addLabel("Comment:", cc.xy(1, 5));
        builder.add(comment, cc.xy(3, 5));
        builder.addLabel("Query Root:", cc.xy(1, 7));
        builder.add(queryRoot, cc.xy(3, 7));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.NORTH);
        this.add(properties, BorderLayout.CENTER);
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    void initFromModel() {
        QueryDescriptor query = controller.getSelectedQuery();

        if (query == null || !QueryDescriptor.SQL_TEMPLATE.equals(query.getType())) {
            setVisible(false);
            return;
        }

        name.setText(query.getName());
        properties.initFromModel(query);
        comment.setText(getQueryComment(query));

        DataMap map = controller.getSelectedDataMap();
        ObjEntity[] roots = map.getObjEntities().toArray(new ObjEntity[0]);

        if (roots.length > 1) {
            Arrays.sort(roots, Comparators.forDataMapChildren());
        }

        DefaultComboBoxModel<ObjEntity> model = new DefaultComboBoxModel<>(roots);
        model.setSelectedItem(query.getRoot());
        queryRoot.setModel(model);

        setVisible(true);
    }

    @Override
    protected QueryDescriptor getQuery() {
        QueryDescriptor query = controller.getSelectedQuery();
        return (query != null && QueryDescriptor.SQL_TEMPLATE.equals(query.getType())) ? query : null;
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
            Object root = entity != null ? entity : controller.getSelectedDataMap();
            template.setRoot(root);

            controller.fireQueryEvent(QueryEvent.ofChange(this, template));
        }
    }

    private void setQueryComment(String text) {
        QueryDescriptor query = getQuery();
        if (query == null) {
            return;
        }
        ObjectInfo.putToMetaData(controller.getApplication().getMetaData(), query, ObjectInfo.COMMENT, text);
        controller.fireQueryEvent(QueryEvent.ofChange(this, query));
    }

    private String getQueryComment(QueryDescriptor queryDescriptor) {
        return ObjectInfo.getFromMetaData(controller.getApplication().getMetaData(), queryDescriptor, ObjectInfo.COMMENT);
    }

    final class LabelCapsRenderer extends DefaultListCellRenderer {
        @Override
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

        private JComboBox<CapsStrategy> labelCase;

        SQLTemplateQueryPropertiesPanel(ProjectController mediator) {
            super(mediator);
        }

        @Override
        protected PanelBuilder createPanelBuilder() {
            labelCase = WidgetFactory.createUndoableComboBox(controller.getApplication().getUndoManager());
            labelCase.setRenderer(new LabelCapsRenderer());

            labelCase.addActionListener(event -> {
                CapsStrategy value = (CapsStrategy) labelCase.getModel().getSelectedItem();
                setQueryProperty(SQLTemplate.COLUMN_NAME_CAPITALIZATION_PROPERTY, value.name());
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

        @Override
        public void initFromModel(QueryDescriptor query) {
            super.initFromModel(query);

            if (query != null && QueryDescriptor.SQL_TEMPLATE.equals(query.getType())) {
                DefaultComboBoxModel<CapsStrategy> labelCaseModel = new DefaultComboBoxModel<>(LABEL_CAPITALIZATION);
                String columnNameCapitalization = query.getProperty(SQLTemplate.COLUMN_NAME_CAPITALIZATION_PROPERTY);

                labelCaseModel.setSelectedItem(columnNameCapitalization != null
                        ? CapsStrategy.valueOf(columnNameCapitalization)
                        : CapsStrategy.DEFAULT);
                labelCase.setModel(labelCaseModel);
            }
        }

        @Override
        protected void setEntity(ObjEntity entity) {
            SQLTemplateMainTab.this.setEntity(entity);
        }

        @Override
        public ObjEntity getEntity(QueryDescriptor query) {
            if (query != null && QueryDescriptor.SQL_TEMPLATE.equals(query.getType())) {
                return SQLTemplateMainTab.this.getEntity(query);
            }

            return null;
        }

        @Override
        protected void setFetchingPersistentObjects(boolean fetchingPersistentObjects) {
            super.setFetchingPersistentObjects(fetchingPersistentObjects);
            if(!fetchingPersistentObjects) {
                setEntity(null);
            }
        }
    }
}
