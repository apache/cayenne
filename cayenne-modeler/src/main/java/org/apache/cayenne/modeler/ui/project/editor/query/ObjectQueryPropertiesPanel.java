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

package org.apache.cayenne.modeler.ui.project.editor.query;

import java.awt.BorderLayout;

import javax.swing.JCheckBox;

import org.apache.cayenne.modeler.toolkit.checkbox.CMCheckBox;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.modeler.ui.project.editor.EditorForm;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel that supports editing the properties of an ObjectSelect query that are not a part of its query String.
 */
public class ObjectQueryPropertiesPanel extends SelectPropertiesPanel<SelectQueryDescriptor> {

    private static final String DATA_ROWS_TOOLTIP = "A column query or a query rooted in a DbEntity fetches no objects";

    protected JCheckBox dataRows;
    protected CMUndoableTextField statementFetchSize;

    public ObjectQueryPropertiesPanel(ProjectSession session) {
        super(session, SelectQueryDescriptor.class);
    }

    protected void initView() {
        super.initView();
        // create widgets

        dataRows = new CMCheckBox(session.app().getUndoManager());

        statementFetchSize = new CMUndoableTextField(session.app().getUndoManager(), 7);
        statementFetchSize.addCommitListener(this::setStatementFetchSize);

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                EditorForm.LABEL_COLUMN + ", $lcgap, left:max(50dlu;pref), fill:max(150dlu;pref)",
                "p, $rgap, p, $rgap, p, $rgap, p, $rgap, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(EditorForm.sectionBorder());
        builder.addLabel("Result Caching:", cc.xy(1, 1));
        builder.add(cacheStrategy, cc.xywh(3, 1, 2, 1));
        cacheGroupsLabel = builder.addLabel("Cache Group:", cc.xy(1, 3));
        builder.add(cacheGroups, cc.xywh(3, 3, 2, 1));
        builder.addLabel("Fetch Data Rows:", cc.xy(1, 5));
        builder.add(dataRows, cc.xy(3, 5));
        builder.addLabel("Page Size:", cc.xy(1, 7));
        builder.add(pageSize, cc.xy(3, 7));
        builder.addLabel("Statement Fetch Size:", cc.xy(1, 9));
        builder.add(statementFetchSize, cc.xy(3, 9));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    protected void initController() {
        super.initController();

        dataRows.addItemListener(e -> setQueryProperty(QueryDescriptor::isFetchingDataRows,
                QueryDescriptor::setFetchingDataRows, dataRows.isSelected()));
    }

    void setStatementFetchSize(String string) {
        setQueryProperty(QueryDescriptor::getStatementFetchSize, QueryDescriptor::setStatementFetchSize,
                intValue(string, "Statement fetch size"));
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    @Override
    public void initFromModel(SelectQueryDescriptor query) {
        super.initFromModel(query);

        dataRows.setSelected(query.isFetchingDataRows());
        statementFetchSize.setText(String.valueOf(query.getStatementFetchSize()));
        updateDataRowsEnabled(query);
    }

    /**
     * Enables the "Fetch Data Rows" option only for the queries that fetch objects, which the query String defines.
     */
    public void updateDataRowsEnabled(SelectQueryDescriptor query) {
        boolean fetchesObjects = !query.isColumnQuery() && !query.isDbQuery();
        dataRows.setEnabled(fetchesObjects);
        dataRows.setToolTipText(fetchesObjects ? null : DATA_ROWS_TOOLTIP);
    }
}
