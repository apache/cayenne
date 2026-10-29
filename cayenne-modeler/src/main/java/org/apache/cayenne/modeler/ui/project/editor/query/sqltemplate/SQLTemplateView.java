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

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.modeler.project.ProjectComparators;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.text.QueryStatusLine;
import org.apache.cayenne.modeler.toolkit.text.QueryTextEditor;
import org.apache.cayenne.modeler.ui.project.editor.EditorForm;
import org.apache.cayenne.modeler.ui.project.editor.query.TextQueryView;
import org.apache.cayenne.modeler.ui.project.editor.query.sqltemplate.SQLTemplateEditorParser.Check;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * An editor of a SQLTemplate query: its name, its SQL scripts, and the properties that are not a part of the SQL.
 */
public class SQLTemplateView extends TextQueryView<SQLTemplateDescriptor> {

    private final SQLScriptTabs scripts;
    private final QueryStatusLine statusLine;
    private final SQLTemplatePropertiesPanel properties;

    public SQLTemplateView(ProjectSession session) {
        super(session);
        initQueryRoot();
        this.scripts = new SQLScriptTabs(session, this::scriptChecked);
        this.statusLine = new QueryStatusLine();
        this.properties = new SQLTemplatePropertiesPanel(session);
        initLayout(scripts, statusLine, null, null, propertiesPanel());
        initBindings();
    }

    // the root of the query, followed by the rest of its properties
    private JPanel propertiesPanel() {
        CellConstraints cc = new CellConstraints();
        PanelBuilder rootBuilder = new PanelBuilder(
                new FormLayout(EditorForm.LABEL_COLUMN + ", $lcgap, fill:200dlu", "p"));
        rootBuilder.setBorder(EditorForm.sectionBorder());
        rootBuilder.addLabel("Root:", cc.xy(1, 1));
        rootBuilder.add(queryRoot, cc.xy(3, 1));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(rootBuilder.getPanel(), BorderLayout.NORTH);
        panel.add(properties, BorderLayout.CENTER);
        return panel;
    }

    private void initBindings() {
        session.addQueryDisplayListener(e -> initFromModel());

        // the status line is about the script shown, so it is updated with a check of that script
        scripts.setSelectionListener(() -> {
            QueryTextEditor editor = scripts.getSelectedEditor();
            if (editor != null) {
                editor.reparse();
            } else {
                statusLine.clear();
            }
        });
    }

    void initFromModel() {
        SQLTemplateDescriptor query = getQuery();
        if (query == null) {
            scripts.setQuery(null);
            return;
        }

        initNameAndComment(query);

        List<ObjEntity> roots = new ArrayList<>(session.getSelectedDataMap().getObjEntities());
        roots.sort(ProjectComparators.forDataMapChildren());
        DefaultComboBoxModel<ObjEntity> model = new DefaultComboBoxModel<>(roots.toArray(new ObjEntity[0]));
        model.setSelectedItem(query.getRoot());
        queryRoot.setModel(model);

        properties.initFromModel(query);
        scripts.setQuery(query);
    }

    // a reaction to each check of a script by its editor
    private void scriptChecked(String key, Check check) {
        if (!Objects.equals(key, scripts.getSelectedScript())) {
            return;
        }

        if (!check.errors().isEmpty()) {
            statusLine.showErrors(check.errors());
        } else if (!check.warnings().isEmpty()) {
            statusLine.showWarnings(check.warnings());
        } else {
            statusLine.showOk("Parameters: " + (check.parameters().isEmpty()
                    ? "none"
                    : check.parameters().stream().map(p -> "$" + p).collect(Collectors.joining(", "))));
        }
    }

    /**
     * Returns the key of the script shown: {@link SQLScriptTabs#DEFAULT_SCRIPT}, or a DbAdapter class name.
     */
    public String getSelectedScript() {
        return scripts.getSelectedScript();
    }

    /**
     * Shows the script of the query, e.g. the one changed by an undoable edit.
     */
    public void showScript(String key) {
        scripts.showScript(key);
    }

    @Override
    protected SQLTemplateDescriptor getQuery() {
        return session.getSelectedQuery() instanceof SQLTemplateDescriptor query ? query : null;
    }
}
