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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.DbAdapterInfo;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.util.Util;
import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.KeywordMap;
import org.syntax.jedit.tokenmarker.PLSQLTokenMarker;
import org.syntax.jedit.tokenmarker.SQLTokenMarker;
import org.syntax.jedit.tokenmarker.Token;
import org.syntax.jedit.tokenmarker.TokenMarker;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A panel for configuring SQL scripts of a SQL template.
 * 
 */
public class SQLTemplateScriptsTab extends JPanel implements DocumentListener {

    private static final String DEFAULT_LABEL = "Default";
    
    /**
     * JEdit marker for SQL Template
     */
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

    protected ProjectController mediator;

    protected JList scripts;
    
    /**
     * JEdit text component for highlighing SQL syntax (see CAY-892)
     */
    protected JEditTextArea scriptArea;
    
    /**
     * Indication that no update should be fired
     */
    private boolean updateDisabled;
    
    protected ListSelectionListener scriptRefreshHandler;

    public SQLTemplateScriptsTab(ProjectController mediator) {
        this.mediator = mediator;

        initView();
    }

    protected void initView() {
        // create widgets, etc.

        scriptRefreshHandler = new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    displayScript();                    
                }
            }
        };

        scripts = new JList();
        scripts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scripts.setCellRenderer(new DbAdapterListRenderer(DbAdapterInfo
                .getStandardAdapterLabels()));

        List keys = new ArrayList(DbAdapterInfo.getStandardAdapters().length + 1);
        keys.addAll(Arrays.asList(DbAdapterInfo.getStandardAdapters()));
        Collections.sort(keys);
        keys.add(0, DEFAULT_LABEL);
        scripts.setModel(new DefaultComboBoxModel(keys.toArray()));
        
        scriptArea = Application.getWidgetFactory().createJEditTextArea();

        scriptArea.setTokenMarker(SQL_TEMPLATE_MARKER);
        scriptArea.getDocument().addDocumentListener(this);
        scriptArea.getPainter().setFont(new Font("Verdana", Font.PLAIN, 12));

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:100dlu, 3dlu, fill:100dlu:grow",
                "3dlu, fill:p:grow"));

        // orderings table must grow as the panel is resized
        builder.add(new JScrollPane(
                scripts,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), cc.xy(1, 2));
        builder.add(scriptArea, cc.xy(3, 2));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    void initFromModel() {
        QueryDescriptor query = mediator.getCurrentQuery();

        if (query == null || !QueryDescriptor.SQL_TEMPLATE.equals(query.getType())) {
            setVisible(false);
            return;
        }

        // select default script.. display it bypassing the listener...
        scripts.removeListSelectionListener(scriptRefreshHandler);
        scripts.setSelectedIndex(0);
        displayScript();
        scripts.addListSelectionListener(scriptRefreshHandler);
        
        scriptArea.setEnabled(true);
        setVisible(true);
    }

    /**
     * Returns SQLTemplate text for current selection.
     */
    String getSQLTemplate(String key) {
        if (key == null) {
            return null;
        }

        SQLTemplateDescriptor query = getQuery();
        if (query == null) {
            return null;
        }

        return (key.equals(DEFAULT_LABEL)) ? query.getSql() : query
                .getAdapterSql().get(key);
    }

    SQLTemplateDescriptor getQuery() {
        QueryDescriptor query = mediator.getCurrentQuery();
        return (query != null && QueryDescriptor.SQL_TEMPLATE.equals(query.getType())) ?
                (SQLTemplateDescriptor) query : null;
    }

    /**
     * Shows selected script in the editor.
     */
    void displayScript() {

        SQLTemplateDescriptor query = getQuery();
        if (query == null) {
            disableEditor();
            return;
        }

        String key = (String) scripts.getSelectedValue();
        if (key == null) {
            disableEditor();
            return;
        }

        enableEditor();

        String text = (key.equals(DEFAULT_LABEL)) ? query.getSql() : query
                .getAdapterSql().get(key);

        updateDisabled = true;
        scriptArea.setText(text);
        updateDisabled = false;
    }

    void disableEditor() {
        scriptArea.setText(null);
        scriptArea.setEnabled(false);
        scriptArea.setEditable(false);
        scriptArea.setBackground(getBackground());
    }

    void enableEditor() {
        scriptArea.setEnabled(true);
        scriptArea.setEditable(true);
        scriptArea.setBackground(Color.WHITE);
    }

    void setSQL(DocumentEvent e) {
        Document doc = e.getDocument();

        try {
            setSQL(doc.getText(0, doc.getLength()));
        }
        catch (BadLocationException e1) {
            e1.printStackTrace();
        }

    }

    /**
     * Sets the value of SQL template for the currently selected script.
     */
    void setSQL(String text) {
        SQLTemplateDescriptor query = getQuery();
        if (query == null) {
            return;
        }

        String key = (String) scripts.getSelectedValue();
        if (key == null) {
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
        if (key.equals(DEFAULT_LABEL)) {
            if (!Util.nullSafeEquals(text, query.getSql())) {
                query.setSql(text);
                mediator.fireQueryEvent(new QueryEvent(this, query));
            }
        }
        else {
            if (!Util.nullSafeEquals(text, query.getAdapterSql().get(key))) {
                query.getAdapterSql().put(key, text);
                mediator.fireQueryEvent(new QueryEvent(this, query));
            }
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
            setSQL(e);
        }
    }

    final class DbAdapterListRenderer extends DefaultListCellRenderer {

        Map adapterLabels;

        DbAdapterListRenderer(Map adapterLabels) {
            this.adapterLabels = (adapterLabels != null)
                    ? adapterLabels
                    : Collections.EMPTY_MAP;
        }

        public Component getListCellRendererComponent(
                JList list,
                Object object,
                int index,
                boolean selected,
                boolean hasFocus) {

            if (object instanceof Class) {
                object = ((Class) object).getName();
            }

            Object label = adapterLabels.get(object);
            if (label == null) {
                label = object;
            }

            Component c = super.getListCellRendererComponent(
                    list,
                    label,
                    index,
                    selected,
                    hasFocus);

            // grey out keys that have no SQL
            setForeground(selected || getSQLTemplate(object.toString()) != null
                    ? Color.BLACK
                    : Color.LIGHT_GRAY);

            return c;
        }
    }
    
    public int getSelectedIndex() {
        return scripts.getSelectedIndex();
    }
    
    public void setSelectedIndex(int index) {
        scripts.setSelectedIndex(index);
    }
}
