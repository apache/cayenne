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
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.toolkit.text.QueryTextEditor;

/**
 * A text editor of the query String of an ObjectSelect query, with the syntax highlighted, and the problems of the
 * query marked. Each edit is stored in the query as is, even if the String can't be parsed.
 */
class QLEditor extends ProjectPanel {

    static {
        QueryTextEditor.registerTokenMaker(QLTokenMaker.SYNTAX_STYLE, QLTokenMaker.class);
    }

    private final QueryTextEditor editor;

    private SelectQueryDescriptor query;

    /**
     * @param problemsListener receives the problems of the query after each check, empty if there are none
     */
    QLEditor(ProjectSession session, Consumer<List<String>> problemsListener) {
        super(session);
        this.editor = new QueryTextEditor(app.getUndoManager(), QLTokenMaker.SYNTAX_STYLE,
                new QLEditorParser(() -> query, problemsListener));
        initLayout();
        initBindings();
    }

    private void initLayout() {
        setLayout(new BorderLayout());
        add(editor, BorderLayout.CENTER);
    }

    private void initBindings() {
        editor.setChangeListener(this::textChanged);
    }

    /**
     * Shows the query String of the query, or clears the editor if the query is null.
     */
    void setQuery(SelectQueryDescriptor query) {
        this.query = query;
        editor.setText(query != null ? query.getQl() : null);
    }

    /**
     * Inserts the text at the caret, in place of the selection.
     */
    void insert(String text) {
        editor.insert(text);
    }

    private void textChanged(String text) {
        if (query == null) {
            return;
        }

        String ql = text.isBlank() ? null : text;
        if (Objects.equals(ql, query.getQl())) {
            return;
        }

        query.setQl(ql);
        session.fireQueryEvent(QueryEvent.ofChange(this, query));
    }
}
