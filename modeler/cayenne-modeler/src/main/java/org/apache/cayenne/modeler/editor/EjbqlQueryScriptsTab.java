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

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.JUndoableCayenneTextPane;
import org.apache.cayenne.project.validation.EJBQLStatementValidator;
import org.apache.cayenne.project.validation.EJBQLStatementValidator.PositionException;
import org.apache.cayenne.map.EJBQLQueryDescriptor;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.swing.components.textpane.JCayenneTextPane;
import org.apache.cayenne.swing.components.textpane.syntax.EJBQLSyntaxConstant;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EjbqlQueryScriptsTab extends JPanel implements DocumentListener {

    private static final Logger logger = LoggerFactory.getLogger(EjbqlQueryScriptsTab.class);

    protected ProjectController mediator;
    protected JCayenneTextPane scriptArea;
    private boolean updateDisabled;
    protected EJBQLStatementValidator ejbqlQueryValidator = new EJBQLStatementValidator();

    public EjbqlQueryScriptsTab(ProjectController mediator) {
        this.mediator = mediator;
        initView();
    }

    void displayScript() {
        scriptArea.setDocumentTextDirect(getQuery().getEjbql());
        updateDisabled = false;
    }

    private void initView() {

        scriptArea = new JUndoableCayenneTextPane(new EJBQLSyntaxConstant());
        scriptArea.getDocument().addDocumentListener(this);
        scriptArea.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    String text = scriptArea.getDocument().getText(e.getOffset(), 1);
                    if (text.equals(" ") || text.equals("\n") || text.equals("\t")) {
                        getQuery().setEjbql(scriptArea.getDocumentTextDirect());
                        validateEJBQL();
                    }
                } catch (BadLocationException ex) {
                    logger.warn("Error reading document", ex);
                }
            }

            public void removeUpdate(DocumentEvent e) {
                getQuery().setEjbql(scriptArea.getDocumentTextDirect());
                validateEJBQL();
            }
        });

        scriptArea.getPane().addFocusListener(new FocusListener() {

            EJBQLValidationThread thread;

            public void focusGained(FocusEvent e) {
                thread = new EJBQLValidationThread();
                thread.start();
            }

            public void focusLost(FocusEvent e) {
                thread.terminate();
            }
        });

        scriptArea.getPane().addKeyListener(new KeyListener() {

            boolean pasteOrCut;

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_END || e.getKeyCode() == KeyEvent.VK_HOME
                        || e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT
                        || e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_UNDO) {
                    getQuery().setEjbql(scriptArea.getText());
                    validateEJBQL();
                }
                if ((e.getKeyCode() == KeyEvent.VK_V || e.getKeyCode() == KeyEvent.VK_X) && e.isControlDown()) {
                    pasteOrCut = true;
                }
            }

            public void keyReleased(KeyEvent e) {
                if ((pasteOrCut && e.getKeyCode() == KeyEvent.VK_CONTROL) || e.getKeyCode() == KeyEvent.VK_DELETE) {
                    scriptArea.removeHighlightText();
                    getQuery().setEjbql(scriptArea.getText());
                    validateEJBQL();
                    pasteOrCut = false;
                }
            }

            public void keyTyped(KeyEvent e) {
            }
        });

        setLayout(new BorderLayout());
        add(scriptArea, BorderLayout.WEST);
        add(scriptArea.getScrollPane(), BorderLayout.CENTER);
        setVisible(true);
    }

    public void initFromModel() {
        QueryDescriptor query = mediator.getCurrentQuery();

        if (query == null || !QueryDescriptor.EJBQL_QUERY.equals(query.getType())) {
            setVisible(false);
            return;
        }
        scriptArea.setEnabled(true);
        displayScript();
        validateEJBQL();
        setVisible(true);
    }

    EJBQLQueryDescriptor getQuery() {
        QueryDescriptor query = mediator.getCurrentQuery();
        return (query != null && QueryDescriptor.EJBQL_QUERY.equals(query.getType())) ?
                (EJBQLQueryDescriptor) query : null;
    }

    void setEJBQL(DocumentEvent e) {
        Document doc = e.getDocument();
        try {
            setEJBQL(doc.getText(0, doc.getLength()));
        } catch (BadLocationException ex) {
            logger.warn("Error reading document", ex);
        }
    }

    void setEJBQL(String text) {
        EJBQLQueryDescriptor query = getQuery();
        if (query == null) {
            return;
        }

        if (text != null && text.trim().isEmpty()) {
            text = null;
        }

        // Compare the value before modifying the query - text area
        // will call "verify" even if no changes have occured....
        if (!Util.nullSafeEquals(text, query.getEjbql())) {
            query.setEjbql(text);
            mediator.fireQueryEvent(new QueryEvent(this, query));
        }

    }

    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    public void changedUpdate(DocumentEvent e) {
        if (!updateDisabled) {
            setEJBQL(e);
        }
    }

    void validateEJBQL() {
        final PositionException positionException = ejbqlQueryValidator.validateEJBQL(getQuery());
        if (positionException != null) {

            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        if (positionException.getBeginLine() != null || positionException.getBeginColumn() != null
                                || positionException.getLength() != null) {
                            scriptArea.setHighlightText(positionException.getBeginLine(),
                                    positionException.getBeginColumn(), positionException.getLength(),
                                    positionException.getMessage());
                        } else {
                            scriptArea.removeHighlightText();
                        }
                    }
                });
            }
        }
    }

    class EJBQLValidationThread extends Thread {

        volatile boolean running;
        final Object timer = new Object();
        int previousCaretPosition;
        int validateCaretPosition;

        static final int DELAY = 500;

        EJBQLValidationThread() {
            super("ejbql-validation-thread");
            setDaemon(true);
        }

        public void run() {
            running = true;
            while (running) {
                int caretPosition = scriptArea.getCaretPosition();
                if (previousCaretPosition != 0 && previousCaretPosition == caretPosition
                        && validateCaretPosition != previousCaretPosition) {
                    validateEJBQL();
                    validateCaretPosition = caretPosition;
                }
                previousCaretPosition = caretPosition;
                synchronized (timer) {
                    try {
                        timer.wait(DELAY);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }

        void terminate() {
            synchronized (timer) {
                running = false;
                timer.notify();
            }
        }
    }
}
