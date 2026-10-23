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
package org.apache.cayenne.modeler.ui.project.editor.query.selectquery;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.pref.adapters.SplitPanePrefs;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.ui.project.editor.EditorForm;
import org.apache.cayenne.modeler.ui.project.editor.query.BaseQueryMainTab;
import org.apache.cayenne.modeler.ui.project.editor.query.ObjectQueryPropertiesPanel;
import org.apache.cayenne.projecttools.extension.info.ObjectInfo;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * An editor of an ObjectSelect query: its name, its query String, and the properties that are not a part of the String.
 */
public class ObjectSelectView extends BaseQueryMainTab {

    private static final int DEFAULT_SPLIT_LOCATION = 520;

    private final QLEditor editor;
    private final QLStatusLine statusLine;
    private final QLPathBrowser pathBrowser;
    private final JSplitPane split;
    private final ObjectQueryPropertiesPanel properties;
    private final CMUndoableTextField comment;

    public ObjectSelectView(ProjectSession session) {
        super(session);
        this.name = new CMUndoableTextField(app.getUndoManager());
        this.editor = new QLEditor(session, this::queryChecked);
        this.statusLine = new QLStatusLine();
        this.pathBrowser = new QLPathBrowser(editor::insert);
        this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.properties = new ObjectQueryPropertiesPanel(session);
        this.comment = new CMUndoableTextField(app.getUndoManager());
        initLayout();
        initBindings();
    }

    private void initLayout() {
        CellConstraints cc = new CellConstraints();

        PanelBuilder nameBuilder = new PanelBuilder(
                new FormLayout(EditorForm.LABEL_COLUMN + ", $lcgap, fill:200dlu", "p"));
        nameBuilder.setBorder(EditorForm.formBorder());
        nameBuilder.addLabel("Name:", cc.xy(1, 1));
        nameBuilder.add(name, cc.xy(3, 1));

        JPanel header = new JPanel(new BorderLayout());
        header.add(EditorForm.toolBarSpacer(), BorderLayout.NORTH);
        header.add(nameBuilder.getPanel(), BorderLayout.CENTER);

        JPanel query = new JPanel(new BorderLayout());
        query.add(editor, BorderLayout.CENTER);
        query.add(statusLine, BorderLayout.SOUTH);

        split.setLeftComponent(query);
        split.setRightComponent(pathBrowser);
        split.setResizeWeight(1.0);

        // the split pane has no surface of its own, the editor and the tree have their borders
        split.setBorder(null);
        split.setBackground(UIManager.getColor("Panel.background"));

        JPanel queryAndPaths = new JPanel(new BorderLayout());
        queryAndPaths.setBorder(EditorForm.sectionBorder());
        queryAndPaths.add(split, BorderLayout.CENTER);

        // the comment closes the form, below the query properties
        PanelBuilder commentBuilder = new PanelBuilder(
                new FormLayout(EditorForm.LABEL_COLUMN + ", $lcgap, fill:200dlu", "p"));
        commentBuilder.setBorder(EditorForm.lastSectionBorder());
        commentBuilder.addLabel("Comment:", cc.xy(1, 1));
        commentBuilder.add(comment, cc.xy(3, 1));

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(properties, BorderLayout.NORTH);
        footer.add(commentBuilder.getPanel(), BorderLayout.CENTER);

        // the query String takes all the height that the rest of the form doesn't need
        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(queryAndPaths, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private void initBindings() {
        name.addCommitListener(this::setQueryName);
        comment.addCommitListener(this::setQueryComment);
        session.addQueryDisplayListener(e -> initFromModel());
        new SplitPanePrefs(app.getPrefsManager().uiNode("objectSelect/editorSplit"))
                .bind(split, DEFAULT_SPLIT_LOCATION);
    }

    private void initFromModel() {
        SelectQueryDescriptor query = getQuery();
        if (query == null) {
            editor.setQuery(null);
            statusLine.update(null, List.of());
            pathBrowser.setEntity(null);
            return;
        }

        name.setText(query.getName());
        comment.setText(ObjectInfo.getFromMetaData(app.getMetaData(), query, ObjectInfo.COMMENT));
        properties.initFromModel(query);
        editor.setQuery(query);

        // the entity may have changed since the tree was built
        pathBrowser.refresh();
    }

    // a reaction to each check of the query String by the editor
    private void queryChecked(List<String> problems) {
        SelectQueryDescriptor query = getQuery();
        statusLine.update(query, problems);
        if (query != null) {
            // a String that can't be parsed keeps the last tree, so that it doesn't flicker while typing
            if (query.isValid()) {
                pathBrowser.setEntity(query.getRoot() instanceof Entity<?, ?, ?> entity ? entity : null);
            }
            properties.updateDataRowsEnabled(query);
        }
    }

    private void setQueryComment(String text) {
        QueryDescriptor query = getQuery();
        if (query == null) {
            return;
        }
        ObjectInfo.putToMetaData(app.getMetaData(), query, ObjectInfo.COMMENT, text);
        session.fireQueryEvent(QueryEvent.ofChange(this, query));
    }

    @Override
    protected SelectQueryDescriptor getQuery() {
        return session.getSelectedQuery() instanceof SelectQueryDescriptor query ? query : null;
    }
}
