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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureQueryDescriptor;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayListener;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class ProcedureQueryView extends JPanel {

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
    protected TextAdapter comment;
    protected JComboBox<Procedure> queryRoot;
    protected SelectPropertiesPanel properties;

    public ProcedureQueryView(ProjectController mediator) {
        this.mediator = mediator;

        initView();
        initController();
    }

    private void initView() {
        // create widgets
        name = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) {
                setQueryName(text);
            }
        };

        comment = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) {
                setQueryComment(text);
            }
        };

        queryRoot = Application.getWidgetFactory().createUndoableComboBox();
        queryRoot.setRenderer(CellRenderers.listRendererWithIcons());
        properties = new ProcedureQueryPropertiesPanel(mediator);

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, fill:max(200dlu;pref)",
                "p, 3dlu, p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("ProcedureQuery Settings", cc.xywh(1, 1, 3, 1));
        builder.addLabel("Query Name:", cc.xy(1, 3));
        builder.add(name.getComponent(), cc.xy(3, 3));
        builder.addLabel("Procedure:", cc.xy(1, 5));
        builder.add(queryRoot, cc.xy(3, 5));
        builder.addLabel("Comment:", cc.xy(1, 7));
        builder.add(comment.getComponent(), cc.xy(3, 7));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.NORTH);
        this.add(properties, BorderLayout.CENTER);
    }

    private void initController() {

        queryRoot.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                QueryDescriptor query = mediator.getCurrentQuery();
                if (query != null) {
                    query.setRoot(queryRoot.getModel().getSelectedItem());
                    mediator.fireQueryEvent(new QueryEvent(this, query));
                }
            }
        });

        mediator.addQueryDisplayListener(e -> initFromModel());
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    void initFromModel() {
        QueryDescriptor query = mediator.getCurrentQuery();

        if (query == null || !QueryDescriptor.PROCEDURE_QUERY.equals(query.getType())) {
            setVisible(false);
            return;
        }

        properties.setEnabled(true);
        name.setText(query.getName());
        comment.setText(getQueryComment(query));

        // init root choices and title label..

        // - ProcedureQuery supports Procedure roots

        // TODO: now we only allow roots from the current map,
        // since query root is fully resolved during map loading,
        // making it impossible to reference other DataMaps.

        DataMap map = mediator.getCurrentDataMap();
        Procedure[] roots = map.getProcedures().toArray(new Procedure[0]);

        if (roots.length > 1) {
            Arrays.sort(roots, Comparators.getDataMapChildrenComparator());
        }

        DefaultComboBoxModel<Procedure> model = new DefaultComboBoxModel<>(roots);
        model.setSelectedItem(query.getRoot());
        queryRoot.setModel(model);

        properties.initFromModel(query);
        setVisible(true);
    }

    /**
     * Initializes Query name from string.
     */
    void setQueryName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        QueryDescriptor query = mediator.getCurrentQuery();
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
            QueryEvent e = new QueryEvent(this, query, query.getName(), map);
            ProjectUtil.setQueryName(map, query, newName);
            mediator.fireQueryEvent(e);
        } else {
            // there is a query with the same name
            throw new ValidationException("There is another query named '" + newName + "'. Use a different name.");
        }
    }

    /**
     * Returns an entity that maps to a procedure query result class.
     */
    ObjEntity getEntity(ProcedureQueryDescriptor query) {
        String entityName = query.getResultEntityName();
        if (entityName == null) {
            return null;
        }

        DataMap map = mediator.getCurrentDataMap();
        if (map == null) {
            return null;
        }

        return map.getObjEntity(entityName);
    }

    void setEntity(ObjEntity entity) {
        QueryDescriptor query = mediator.getCurrentQuery();
        if (query != null && QueryDescriptor.PROCEDURE_QUERY.equals(query.getType())) {
            ((ProcedureQueryDescriptor) query).setResultEntityName(entity != null ? entity.getName() : null);
            mediator.fireQueryEvent(new QueryEvent(this, query));
        }
    }

    private void setQueryComment(String text) {
        QueryDescriptor query = mediator.getCurrentQuery();
        if (query == null) {
            return;
        }
        ObjectInfo.putToMetaData(mediator.getApplication().getMetaData(), query, ObjectInfo.COMMENT, text);
        mediator.fireQueryEvent(new QueryEvent(this, query));
    }

    private String getQueryComment(QueryDescriptor queryDescriptor) {
        return ObjectInfo.getFromMetaData(mediator.getApplication().getMetaData(), queryDescriptor, ObjectInfo.COMMENT);
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

    final class ProcedureQueryPropertiesPanel extends RawQueryPropertiesPanel {

        private JComboBox<CapsStrategy> labelCase;

        ProcedureQueryPropertiesPanel(ProjectController mediator) {
            super(mediator);
        }

        protected PanelBuilder createPanelBuilder() {
            labelCase = Application.getWidgetFactory().createUndoableComboBox();
            labelCase.setRenderer(new LabelCapsRenderer());
            labelCase.addActionListener(event -> {
                String value = labelCase.getModel().getSelectedItem().toString();
                setQueryProperty(ProcedureQuery.COLUMN_NAME_CAPITALIZATION_PROPERTY, value);
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

            if (query != null && QueryDescriptor.PROCEDURE_QUERY.equals(query.getType())) {
                DefaultComboBoxModel<CapsStrategy> labelCaseModel = new DefaultComboBoxModel<>(LABEL_CAPITALIZATION);
                String columnNameCapitalization = query.getProperty(ProcedureQuery.COLUMN_NAME_CAPITALIZATION_PROPERTY);
                labelCaseModel.setSelectedItem(columnNameCapitalization != null ?
                        CapsStrategy.valueOf(columnNameCapitalization) : CapsStrategy.DEFAULT);
                labelCase.setModel(labelCaseModel);
            }
        }

        protected void setEntity(ObjEntity entity) {
            ProcedureQueryView.this.setEntity(entity);
        }

        public ObjEntity getEntity(QueryDescriptor query) {
            if (query != null && QueryDescriptor.PROCEDURE_QUERY.equals(query.getType())) {
                return ProcedureQueryView.this.getEntity((ProcedureQueryDescriptor) query);
            }

            return null;
        }
    }
}
