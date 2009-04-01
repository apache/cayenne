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

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.ProjectController;
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
        scriptArea.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                validateEJBQL();
            }

            public void focusLost(FocusEvent e) {
                validateEJBQL();
            }

        });

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
                scriptArea.removeHighlightText();
                validateEJBQL();
            }
        });
        setLayout(new BorderLayout());
        add(scriptArea, BorderLayout.WEST);
        add(scriptArea.scrollPane, BorderLayout.CENTER);
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
        }
    }
}
