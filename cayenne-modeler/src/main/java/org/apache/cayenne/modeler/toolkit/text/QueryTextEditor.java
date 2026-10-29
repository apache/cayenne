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
package org.apache.cayenne.modeler.toolkit.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.cayenne.modeler.undo.CayenneUndoManager;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * A text editor of a query String, with line numbers, the syntax highlighted, and the problems found by an optional
 * parser marked. The edits are recorded in the Modeler undo manager.
 */
public class QueryTextEditor extends JPanel {

    private static final int PARSER_DELAY_MS = 300;

    private final CMUndoableSyntaxTextArea textArea;
    private final Parser parser;

    private Consumer<String> changeListener;

    // true while the text is loaded from the model, so that it isn't reported as an edit
    private boolean loading;

    /**
     * @param syntaxStyle the key of the token maker, see {@link #registerTokenMaker(String, Class)}
     * @param parser      checks the text after the edits, may be null
     */
    public QueryTextEditor(CayenneUndoManager undoManager, String syntaxStyle, Parser parser) {
        this.textArea = new CMUndoableSyntaxTextArea(undoManager);
        this.parser = parser;
        initLayout(syntaxStyle);
        initBindings();
    }

    /**
     * Registers a token maker of a language in RSyntaxTextArea, under the syntax style key used by the editors.
     */
    public static void registerTokenMaker(String syntaxStyle, Class<? extends TokenMaker> tokenMaker) {
        AbstractTokenMakerFactory factory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        factory.putMapping(syntaxStyle, tokenMaker.getName());
    }

    private void initLayout(String syntaxStyle) {
        textArea.setSyntaxEditingStyle(syntaxStyle);
        textArea.setRows(8);
        textArea.setHighlightCurrentLine(false);
        textArea.setCodeFoldingEnabled(false);
        textArea.setAntiAliasingEnabled(true);
        if (parser != null) {
            textArea.setParserDelay(PARSER_DELAY_MS);
            textArea.addParser(parser);
        }

        setLayout(new BorderLayout());
        RTextScrollPane scrollPane = new RTextScrollPane(textArea, true);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void initBindings() {
        textArea.getDocument().addDocumentListener(new TextChangeListener());
    }

    /**
     * Sets the listener that receives the text after each edit. It is not called when the text is loaded with
     * {@link #setText(String)}.
     */
    public void setChangeListener(Consumer<String> changeListener) {
        this.changeListener = changeListener;
    }

    /**
     * Shows a text loaded from the model, with the caret at the start, without recording an undoable edit, and
     * checks it right away.
     */
    public void setText(String text) {
        loading = true;
        try {
            textArea.setText(text != null ? text : "");
            textArea.setCaretPosition(0);
        } finally {
            loading = false;
        }

        // check right away, not after the delay that is meant for typing
        reparse();
    }

    public String getText() {
        return textArea.getText();
    }

    /**
     * Checks the text with the parser right away.
     */
    public void reparse() {
        if (parser != null) {
            textArea.forceReparsing(parser);
        }
    }

    /**
     * Inserts the text at the caret, in place of the selection.
     */
    public void insert(String text) {
        textArea.replaceSelection(text);
        textArea.requestFocusInWindow();
    }

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    private void textChanged() {
        if (!loading && changeListener != null) {
            changeListener.accept(textArea.getText());
        }
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
