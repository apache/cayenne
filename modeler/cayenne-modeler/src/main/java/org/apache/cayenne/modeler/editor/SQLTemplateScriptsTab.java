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
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.DbAdapterInfo;
import org.apache.cayenne.modeler.util.JUndoableCayenneTextPane;
import org.apache.cayenne.swing.components.textpane.JCayenneTextPane;
import org.apache.cayenne.swing.components.textpane.syntax.SQLSyntaxConstants;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A panel for configuring SQL scripts of a SQL template.
 *
 */
public class SQLTemplateScriptsTab extends JPanel {

    private static final String DEFAULT_LABEL = "Default";
    private static final Logger logger = LoggerFactory.getLogger(SQLTemplateScriptsTab.class);

    protected ProjectController mediator;

    protected JList<String> scripts;
    protected List<String> keys;
    protected PanelBuilder builder;
    protected CellConstraints cc;
    protected JCayenneTextPane textPane;
    protected List<JCayenneTextPane> panes;
    protected ListSelectionListener scriptRefreshHandler;

    public SQLTemplateScriptsTab(ProjectController mediator) {
        this.mediator = mediator;

        initView();
    }

    private void prepareScriptAreas() {
        for(String key : DbAdapterInfo.getStandardAdapters()) {
            JCayenneTextPane currPane = new JUndoableCayenneTextPane(new SQLSyntaxConstants());
            currPane.setName(key);
            currPane.getDocument().addDocumentListener(new CustomListener(currPane.getName()));
            builder.add(currPane.getScrollPane(), cc.xy(3, 2));
            panes.add(currPane);
        }
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

        scripts = new JList<>();
        scripts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scripts.setCellRenderer(new DbAdapterListRenderer(DbAdapterInfo.getStandardAdapterLabels()));

        keys = new ArrayList<>(DbAdapterInfo.getStandardAdapters().length + 1);
        keys.addAll(Arrays.asList(DbAdapterInfo.getStandardAdapters()));
        Collections.sort(keys);
        keys.add(0, DEFAULT_LABEL);
        scripts.setModel(new DefaultComboBoxModel<>(keys.toArray(new String[0])));

        // assemble
        cc = new CellConstraints();

        textPane = new JUndoableCayenneTextPane(new SQLSyntaxConstants());
        textPane.setName(DEFAULT_LABEL);
        textPane.getDocument().addDocumentListener(new CustomListener(textPane.getName()));

        panes = new ArrayList<>();
        panes.add(textPane);

        builder = new PanelBuilder(new FormLayout(
                "fill:100dlu, 3dlu, fill:100dlu:grow",
                "3dlu, fill:100dlu:grow"));

        // orderings table must grow as the panel is resized
        builder.add(new JScrollPane(
                scripts,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), cc.xy(1, 2));
        builder.add(textPane.getScrollPane(), cc.xy(3, 2));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        prepareScriptAreas();
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
            return;
        }

        String key = scripts.getSelectedValue();
        if (key == null) {
            return;
        }

        final String text = (key.equals(DEFAULT_LABEL)) ? query.getSql() : query
                .getAdapterSql().get(key);

        for (final JCayenneTextPane textPane : panes) {
            if (key.equals(textPane.getName())) {
                textPane.setDocumentTextDirect(text);
                textPane.getScrollPane().setVisible(true);
                textPane.getPane().requestFocusInWindow();
            } else {
                textPane.getScrollPane().setVisible(false);
            }
        }
    }

    void setSQL(DocumentEvent e, String key) {
        if (key == null) {
            return;
        }

        SQLTemplateDescriptor query = getQuery();
        if (query == null) {
            return;
        }

        Document doc = e.getDocument();
        String text = null;

        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException ex) {
            logger.warn("Error reading document", ex);
        }

        if (text != null) {
            text = text.trim();
            if (text.length() == 0) {
                text = null;
            }
        }

        // Compare the value before modifying the query - text pane
        // will call "verify" even if no changes have occured....
        if (key.equals(DEFAULT_LABEL)) {
            if (!Util.nullSafeEquals(text, query.getSql())) {
                query.setSql(text);
                mediator.fireQueryEvent(new QueryEvent(this, query));
            }
        } else {
            if (!Util.nullSafeEquals(text, query.getAdapterSql().get(key))) {
                query.getAdapterSql().put(key, text);
                mediator.fireQueryEvent(new QueryEvent(this, query));
            }
        }
    }

    final class CustomListener implements DocumentListener{

        private String key;

        public CustomListener(String key) {
            this.key = key;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            changedUpdate(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changedUpdate(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            setSQL(e, key);
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
