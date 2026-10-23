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
import java.awt.Color;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * A text editor of the query String of an ObjectSelect query, with the syntax highlighted, and the problems of the
 * query marked. Each edit is stored in the query as is, even if the String can't be parsed.
 */
class QLEditor extends ProjectPanel {

    private static final int PARSER_DELAY_MS = 300;

    static {
        AbstractTokenMakerFactory factory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        factory.putMapping(QLTokenMaker.SYNTAX_STYLE, QLTokenMaker.class.getName());
    }

    private final CMUndoableSyntaxTextArea textArea;
    private final QLEditorParser parser;

    private SelectQueryDescriptor query;

    // true while the text is loaded from the model, so that it isn't written back
    private boolean loading;

    /**
     * @param problemsListener receives the problems of the query after each check, empty if there are none
     */
    QLEditor(ProjectSession session, Consumer<List<String>> problemsListener) {
        super(session);
        this.textArea = new CMUndoableSyntaxTextArea(app.getUndoManager());
        this.parser = new QLEditorParser(() -> query, problemsListener);
        initLayout();
        initBindings();
    }

    private void initLayout() {
        textArea.setSyntaxEditingStyle(QLTokenMaker.SYNTAX_STYLE);
        textArea.setRows(8);
        textArea.setHighlightCurrentLine(false);
        textArea.setCodeFoldingEnabled(false);
        textArea.setAntiAliasingEnabled(true);
        textArea.setParserDelay(PARSER_DELAY_MS);
        textArea.addParser(parser);

        setLayout(new BorderLayout());
        RTextScrollPane scrollPane = new RTextScrollPane(textArea, true);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void initBindings() {
        textArea.getDocument().addDocumentListener(new TextChangeListener());
    }

    /**
     * Shows the query String of the query, or clears the editor if the query is null.
     */
    void setQuery(SelectQueryDescriptor query) {
        this.query = query;

        loading = true;
        try {
            textArea.setText(query != null && query.getQl() != null ? query.getQl() : "");
            textArea.setCaretPosition(0);
        } finally {
            loading = false;
        }

        // check right away, not after the delay that is meant for typing
        textArea.forceReparsing(parser);
    }

    /**
     * Inserts the text at the caret, in place of the selection.
     */
    void insert(String text) {
        textArea.replaceSelection(text);
        textArea.requestFocusInWindow();
    }

    private void textChanged() {
        if (loading || query == null) {
            return;
        }

        String text = textArea.getText();
        String ql = text.isBlank() ? null : text;
        if (Objects.equals(ql, query.getQl())) {
            return;
        }

        query.setQl(ql);
        session.fireQueryEvent(QueryEvent.ofChange(this, query));
    }

    private final class TextChangeListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            textChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            textChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            // style changes only
        }
    }
}
