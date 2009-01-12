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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.Util;
import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.KeywordMap;
import org.syntax.jedit.tokenmarker.PLSQLTokenMarker;
import org.syntax.jedit.tokenmarker.SQLTokenMarker;
import org.syntax.jedit.tokenmarker.Token;
import org.syntax.jedit.tokenmarker.TokenMarker;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class EjbqlQueryScriptsTab extends JPanel implements DocumentListener{

    protected ProjectController mediator;
    
    static final TokenMarker SQL_TEMPLATE_MARKER;
    static {
        KeywordMap map = PLSQLTokenMarker.getKeywordMap();
        
        //adding more keywords
        map.add("FIRST", Token.KEYWORD1);
        map.add("LIMIT", Token.KEYWORD1);
        map.add("OFFSET", Token.KEYWORD1);
        map.add("TOP", Token.KEYWORD1);
        
        //adding velocity template highlighing
        map.add("#bind", Token.KEYWORD2);
        map.add("#bindEqual", Token.KEYWORD2);
        map.add("#bindNotEqual", Token.KEYWORD2);
        map.add("#bindObjectEqual", Token.KEYWORD2);
        map.add("#bindObjectNotEqual", Token.KEYWORD2);
        map.add("#chain", Token.KEYWORD2);
        map.add("#chunk", Token.KEYWORD2);
        map.add("#end", Token.KEYWORD2);
        map.add("#result", Token.KEYWORD2);
        
        SQL_TEMPLATE_MARKER = new SQLTokenMarker(map);
    }
    
    /**
     * JEdit text component for highlighing SQL syntax (see CAY-892)
     */
    protected JEditTextArea scriptArea;
 
    private boolean updateDisabled;
    
    public EjbqlQueryScriptsTab(ProjectController mediator) {
        this.mediator = mediator;
        initView();
    }
    
    void displayScript() {
        EJBQLQuery query = getQuery();
        updateDisabled = true;
        scriptArea.setText(query.getEjbqlStatement());
        updateDisabled = false;
     }

    private void initView() {
 
        scriptArea = CayenneWidgetFactory.createJEditTextArea();     
        scriptArea.setTokenMarker(SQL_TEMPLATE_MARKER);  
        scriptArea.getDocument().addDocumentListener(this);
        CellConstraints cc = new CellConstraints();
        
        FormLayout formLayout = new FormLayout(
                "fill:0dlu:grow", 
                "fill:0dlu:grow");

        formLayout.maximumLayoutSize(scriptArea);
        PanelBuilder builder = new PanelBuilder(formLayout);
      
        builder.add(new JScrollPane(scriptArea), cc.xy(1,1));
        
        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER); 
    }


    public void initFromModel() {
        Query query = mediator.getCurrentQuery();

        if (!(query instanceof EJBQLQuery)) {
            setVisible(false);
            return;
        }
        
        scriptArea.setEnabled(true);      
        displayScript();
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

        if (text != null) {
            text = text.trim();
            if (text.length() == 0) {
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
}
