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

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.pref.adapters.SplitPanePrefs;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.toolkit.text.QueryStatusLine;
import org.apache.cayenne.modeler.ui.project.editor.EditorForm;
import org.apache.cayenne.projecttools.extension.info.ObjectInfo;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A base editor of a query that is a text: the text editor with a status line under it, and an optional side panel
 * next to it, take all the height that the form below them doesn't need. The form has the query name, the properties
 * of the query that are not a part of the text, and the comment.
 */
public abstract class TextQueryView<Q extends QueryDescriptor> extends BaseQueryMainTab {

    private static final int DEFAULT_SPLIT_LOCATION = 520;

    private final CMUndoableTextField comment;

    protected TextQueryView(ProjectSession session) {
        super(session);
        this.name = new CMUndoableTextField(app.getUndoManager());
        this.comment = new CMUndoableTextField(app.getUndoManager());
        name.addCommitListener(this::setQueryName);
        comment.addCommitListener(this::setQueryComment);
    }

    /**
     * Lays out the editor. Called once by the subclass constructor, after it created its components.
     *
     * @param editor        the editor of the query text
     * @param statusLine    the line shown under the editor
     * @param sidePanel     a panel shown on the right of the editor, in a split pane, or null for no side panel
     * @param splitPrefsKey the preferences node of the split pane location, not used if there's no side panel
     * @param properties    the sections of the query properties, shown between the name and the comment
     */
    protected void initLayout(JComponent editor, QueryStatusLine statusLine, JComponent sidePanel,
                              String splitPrefsKey, JComponent properties) {
        CellConstraints cc = new CellConstraints();

        JPanel query = new JPanel(new BorderLayout());
        query.add(editor, BorderLayout.CENTER);
        query.add(statusLine, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(EditorForm.editorAreaBorder());
        if (sidePanel != null) {
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, query, sidePanel);
            split.setResizeWeight(1.0);

            // the split pane has no surface of its own, the editor and the side panel have their borders
            split.setBorder(null);
            split.setBackground(UIManager.getColor("Panel.background"));
            center.add(split, BorderLayout.CENTER);

            new SplitPanePrefs(app.getPrefsManager().uiNode(splitPrefsKey)).bind(split, DEFAULT_SPLIT_LOCATION);
        } else {
            center.add(query, BorderLayout.CENTER);
        }

        // the name opens the list of the query properties, below the query text
        PanelBuilder nameBuilder = new PanelBuilder(
                new FormLayout(EditorForm.LABEL_COLUMN + ", $lcgap, fill:200dlu", "p"));
        nameBuilder.setBorder(EditorForm.sectionBorder());
        nameBuilder.addLabel("Name:", cc.xy(1, 1));
        nameBuilder.add(name, cc.xy(3, 1));

        // the comment closes the form, below the query properties
        PanelBuilder commentBuilder = new PanelBuilder(
                new FormLayout(EditorForm.LABEL_COLUMN + ", $lcgap, fill:200dlu", "p"));
        commentBuilder.setBorder(EditorForm.lastSectionBorder());
        commentBuilder.addLabel("Comment:", cc.xy(1, 1));
        commentBuilder.add(comment, cc.xy(3, 1));

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(nameBuilder.getPanel(), BorderLayout.NORTH);
        footer.add(properties, BorderLayout.CENTER);
        footer.add(commentBuilder.getPanel(), BorderLayout.SOUTH);

        // the query text takes all the height that the rest of the form doesn't need
        setLayout(new BorderLayout());
        add(center, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    /**
     * Shows the name and the comment of the query.
     */
    protected void initNameAndComment(Q query) {
        name.setText(query.getName());
        comment.setText(ObjectInfo.getFromMetaData(app.getMetaData(), query, ObjectInfo.COMMENT));
    }

    private void setQueryComment(String text) {
        Q query = getQuery();
        if (query == null) {
            return;
        }
        ObjectInfo.putToMetaData(app.getMetaData(), query, ObjectInfo.COMMENT, text);
        session.fireQueryEvent(QueryEvent.ofChange(this, query));
    }

    /**
     * Returns the query shown in the editor, or null if the selected query is of another type.
     */
    @Override
    protected abstract Q getQuery();
}
