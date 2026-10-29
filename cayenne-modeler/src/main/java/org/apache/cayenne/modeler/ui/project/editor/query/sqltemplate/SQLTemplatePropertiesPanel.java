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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.combobox.CMUndoableComboBox;
import org.apache.cayenne.modeler.ui.project.editor.query.LabelCapsRenderer;
import org.apache.cayenne.modeler.ui.project.editor.query.RawQueryPropertiesPanel;
import org.apache.cayenne.query.CapsStrategy;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.RowSpec;

/**
 * The properties of a SQLTemplate that are not a part of its SQL: caching, the result objects, the page size and the
 * capitalization of the result column labels.
 */
class SQLTemplatePropertiesPanel extends RawQueryPropertiesPanel<SQLTemplateDescriptor> {

    private JComboBox<CapsStrategy> labelCase;

    SQLTemplatePropertiesPanel(ProjectSession session) {
        super(session, SQLTemplateDescriptor.class);
    }

    @Override
    protected PanelBuilder createPanelBuilder() {
        labelCase = new CMUndoableComboBox<>(session.app().getUndoManager());
        labelCase.setRenderer(new LabelCapsRenderer());

        labelCase.addActionListener(event -> {
            CapsStrategy value = (CapsStrategy) labelCase.getModel().getSelectedItem();
            setQueryProperty(SQLTemplateDescriptor::getColumnNamesCapitalization,
                    SQLTemplateDescriptor::setColumnNamesCapitalization, value);
        });

        PanelBuilder builder = super.createPanelBuilder();
        for (RowSpec extraRow : RowSpec.decodeSpecs("$rgap, p")) {
            builder.appendRow(extraRow);
        }

        CellConstraints cc = new CellConstraints();
        builder.addLabel("Row Label Case:", cc.xy(1, 9));
        builder.add(labelCase, cc.xywh(3, 9, 5, 1));
        return builder;
    }

    @Override
    public void initFromModel(SQLTemplateDescriptor query) {
        super.initFromModel(query);

        DefaultComboBoxModel<CapsStrategy> labelCaseModel = new DefaultComboBoxModel<>(LabelCapsRenderer.STRATEGIES);
        CapsStrategy columnNameCapitalization = query.getColumnNamesCapitalization();
        labelCaseModel.setSelectedItem(columnNameCapitalization != null
                ? columnNameCapitalization
                : CapsStrategy.DEFAULT);
        labelCase.setModel(labelCaseModel);
    }

    @Override
    protected void setEntity(ObjEntity entity) {
        SQLTemplateDescriptor template = getQuery();
        if (template != null) {
            // the root of a query that fetches data rows is the DataMap
            template.setRoot(entity != null ? entity : session.getSelectedDataMap());
            session.fireQueryEvent(QueryEvent.ofChange(this, template));
        }
    }

    @Override
    protected ObjEntity getEntity(SQLTemplateDescriptor query) {
        return query.getRoot() instanceof ObjEntity entity ? entity : null;
    }

    @Override
    protected void setFetchingPersistentObjects(boolean fetchingPersistentObjects) {
        super.setFetchingPersistentObjects(fetchingPersistentObjects);
        if (!fetchingPersistentObjects) {
            setEntity(null);
        }
    }
}
