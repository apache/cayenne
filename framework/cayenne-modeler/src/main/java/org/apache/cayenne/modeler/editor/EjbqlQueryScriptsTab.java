/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.undo.JTextFieldUndoListener;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.project.validator.EJBQLQueryValidator;
import org.apache.cayenne.project.validator.EJBQLQueryValidator.PositionException;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.swing.components.textpane.JCayenneTextPane;
import org.apache.cayenne.util.Util;

public class EjbqlQueryScriptsTab extends JPanel implements DocumentListener {

    protected ProjectController mediator;
    protected JCayenneTextPane scriptArea;
    private boolean updateDisabled;
    protected EJBQLQueryValidator ejbqlQueryValidator = new EJBQLQueryValidator();

    public EjbqlQueryScriptsTab(ProjectController mediator) {
        this.mediator = mediator;
        initView();
    }

    void displayScript() {
        EJBQLQuery query = getQuery();
        scriptArea.setText(query.getEjbqlStatement());
        updateDisabled = false;
    }

    private void initView() {

        scriptArea = CayenneWidgetFactory.createJEJBQLTextPane();
         
        scriptArea.getDocument().addDocumentListener(this);

        scriptArea.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    String text = scriptArea
                            .getDocument()
                            .getText(e.getOffset(), 1)
                            .toString();
                    if (text.equals(" ") || text.equals("\n") || text.equals("\t")) {
                        getQuery().setEjbqlStatement(scriptArea.getText());
                        validateEJBQL();
                    }
                }
                catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }

            public void removeUpdate(DocumentEvent e) {
                getQuery().setEjbqlStatement(scriptArea.getText());
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
                if (e.getKeyCode() == KeyEvent.VK_END
                        || e.getKeyCode() == KeyEvent.VK_HOME
                        || e.getKeyCode() == KeyEvent.VK_LEFT
                        || e.getKeyCode() == KeyEvent.VK_RIGHT
                        || e.getKeyCode() == KeyEvent.VK_UP
                        || e.getKeyCode() == KeyEvent.VK_UNDO) {
                    getQuery().setEjbqlStatement(scriptArea.getText());
                    validateEJBQL();
                }
                if ((e.getKeyCode() == KeyEvent.VK_V || e.getKeyCode() == KeyEvent.VK_X)
                        && e.isControlDown()) {
                    pasteOrCut = true;
                }
            }

            public void keyReleased(KeyEvent e) {

                if ((pasteOrCut && e.getKeyCode() == KeyEvent.VK_CONTROL)
                        || e.getKeyCode() == KeyEvent.VK_DELETE) {
                    scriptArea.removeHighlightText();
                    getQuery().setEjbqlStatement(scriptArea.getText());
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
        Query query = mediator.getCurrentQuery();

        if (!(query instanceof EJBQLQuery)) {
            setVisible(false);
            return;
        }
        scriptArea.setEnabled(true);
        displayScript();
        validateEJBQL();
        setVisible(true);
    }

    EJBQLQuery getQuery() {
        Query query = mediator.getCurrentQuery();
        return (query instanceof EJBQLQuery) ? (EJBQLQuery) query : null;
    }

    void setEJBQL(DocumentEvent e) {
        Document doc = e.getDocument();
        try {
            setEJBQL(doc.getText(0, doc.getLength()));
        }
        catch (BadLocationException e1) {
            e1.printStackTrace();
        }
    }

    void setEJBQL(String text) {
        EJBQLQuery query = getQuery();
        if (query == null) {
            return;
        }

        String testTemp = null;
        if (text != null) {
            testTemp = text.trim();
            if (testTemp.length() == 0) {
                text = null;
            }
        }

        // Compare the value before modifying the query - text area
        // will call "verify" even if no changes have occured....
        if (!Util.nullSafeEquals(text, query.getEjbqlStatement())) {
            query.setEjbqlStatement(text);
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
        PositionException positionException = ejbqlQueryValidator.validateEJBQL(
                getQuery(),
                mediator.getCurrentDataDomain());
        if (positionException != null) {
            if (positionException.getBeginLine() != null
                    || positionException.getBeginColumn() != null
                    || positionException.getLength() != null) {
                scriptArea.setHighlightText(
                        positionException.getBeginLine(),
                        positionException.getBeginColumn(),
                        positionException.getLength(),
                        positionException.getMessage());
            }
            else {
                scriptArea.removeHighlightText();
            }
        }
    }

    class EJBQLValidationThread extends Thread {

        boolean running;
        Object timer = new Object();
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
                if (previousCaretPosition != 0
                        && previousCaretPosition == caretPosition
                        && validateCaretPosition != previousCaretPosition) {
                    validateEJBQL();
                    validateCaretPosition = caretPosition;
                }
                previousCaretPosition = caretPosition;
                synchronized (timer) {
                    try {
                        timer.wait(DELAY);
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
        }

        public void terminate() {
            synchronized (timer) {
                running = false;
                timer.notify();
            }
        }
    }
}
