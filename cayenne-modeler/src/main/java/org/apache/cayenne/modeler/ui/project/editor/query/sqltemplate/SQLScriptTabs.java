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
package org.apache.cayenne.modeler.ui.project.editor.query.sqltemplate;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;

import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.modeler.dbadapter.DbAdapters;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.toolkit.text.QueryTextEditor;
import org.apache.cayenne.modeler.ui.project.editor.query.sqltemplate.SQLTemplateEditorParser.Check;

/**
 * The SQL scripts of a SQLTemplate, as tabs above the editor: the default script, used with any database, and the
 * scripts for specific DbAdapters, if there are any. A script is added with the button at the end of the tabs, and
 * removed by closing its tab.
 */
class SQLScriptTabs extends ProjectPanel {

    /**
     * The key of the default script of the query, that the other keys, the DbAdapter class names, override.
     */
    static final String DEFAULT_SCRIPT = "Default";

    static {
        QueryTextEditor.registerTokenMaker(SQLTemplateTokenMaker.SYNTAX_STYLE, SQLTemplateTokenMaker.class);
    }

    private final JTabbedPane tabs;
    private final JButton addScript;
    private final BiConsumer<String, Check> checkListener;

    // an editor per script key, created on demand and kept, as the undoable edits of a script refer to its editor
    private final Map<String, QueryTextEditor> editors;

    private SQLTemplateDescriptor query;
    private Runnable selectionListener;

    // true while the tabs are rebuilt, so that the intermediate selections aren't reported
    private boolean rebuilding;

    /**
     * @param checkListener receives the key of a script and the result of its check, after each check
     */
    SQLScriptTabs(ProjectSession session, BiConsumer<String, Check> checkListener) {
        super(session);
        this.tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        this.addScript = new JButton();
        this.checkListener = checkListener;
        this.editors = new HashMap<>();
        initLayout();
        initBindings();
    }

    private void initLayout() {
        Icon addIcon = IconFactory.buildIcon("icon-plus.png");
        addScript.setIcon(addIcon);
        addScript.setDisabledIcon(IconFactory.disabledIcon(addIcon));
        addScript.setToolTipText("Add a script for a specific database");
        addScript.putClientProperty("JButton.buttonType", "toolBarButton");

        // FlatLaf stretches the trailing component over the rest of the tab area, so the button is wrapped to keep
        // its own size, next to the last tab
        JPanel trailing = new JPanel(new BorderLayout());
        trailing.setOpaque(false);
        trailing.add(addScript, BorderLayout.WEST);
        tabs.putClientProperty("JTabbedPane.trailingComponent", trailing);

        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
    }

    private void initBindings() {
        addScript.addActionListener(e -> showAddScriptMenu());
        tabs.putClientProperty("JTabbedPane.tabCloseCallback",
                (BiConsumer<JTabbedPane, Integer>) (pane, index) -> removeScriptWithUndo(keyAt(index)));
        tabs.addChangeListener(e -> {
            if (!rebuilding && selectionListener != null) {
                selectionListener.run();
            }
        });
    }

    /**
     * Sets the listener notified when another script is shown.
     */
    void setSelectionListener(Runnable selectionListener) {
        this.selectionListener = selectionListener;
    }

    /**
     * Shows the scripts of the query: the default one, and those of the DbAdapters that have SQL. The script shown
     * before stays selected if the query has it, otherwise the default one is selected.
     */
    void setQuery(SQLTemplateDescriptor query) {
        String selected = getSelectedScript();
        this.query = query;

        rebuilding = true;
        try {
            tabs.removeAll();
            if (query != null) {
                addTab(DEFAULT_SCRIPT);
                query.getAdapterSql().entrySet().stream()
                        .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                        .map(Map.Entry::getKey)
                        .sorted(Comparator.comparing(SQLScriptTabs::label, String.CASE_INSENSITIVE_ORDER))
                        .forEach(this::addTab);
                tabs.setSelectedIndex(Math.max(0, indexOf(selected)));
            }
        } finally {
            rebuilding = false;
        }

        addScript.setEnabled(query != null);
        if (selectionListener != null) {
            selectionListener.run();
        }
    }

    /**
     * Returns the key of the script shown, or null if there's no query.
     */
    String getSelectedScript() {
        int index = tabs.getSelectedIndex();
        return index >= 0 ? keyAt(index) : null;
    }

    /**
     * Shows the script, adding its tab if the query has no such script, e.g. when the removal of a script is undone.
     */
    void showScript(String key) {
        if (query == null || key == null) {
            return;
        }

        int index = indexOf(key);
        if (index < 0) {
            addTab(key);
            index = tabs.getTabCount() - 1;
        }
        tabs.setSelectedIndex(index);
    }

    /**
     * Returns the editor of the script shown, or null if there's no query.
     */
    QueryTextEditor getSelectedEditor() {
        int index = tabs.getSelectedIndex();
        return index >= 0 ? (QueryTextEditor) tabs.getComponentAt(index) : null;
    }

    private void addTab(String key) {
        QueryTextEditor editor = editors.computeIfAbsent(key, this::createEditor);
        editor.setText(getScript(query, key));

        // the default script can't be removed
        editor.putClientProperty("JTabbedPane.tabClosable", !DEFAULT_SCRIPT.equals(key));

        tabs.addTab(label(key), null, editor, DEFAULT_SCRIPT.equals(key)
                ? "The SQL used with the databases that have no script of their own"
                : "The SQL used with " + key);
    }

    private QueryTextEditor createEditor(String key) {
        QueryTextEditor editor = new QueryTextEditor(app.getUndoManager(), SQLTemplateTokenMaker.SYNTAX_STYLE,
                new SQLTemplateEditorParser(check -> checkListener.accept(key, check)));
        editor.setName(key);
        editor.setChangeListener(text -> scriptEdited(key, text));
        return editor;
    }

    private String keyAt(int index) {
        return tabs.getComponentAt(index).getName();
    }

    private int indexOf(String key) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (keyAt(i).equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private void showAddScriptMenu() {
        JPopupMenu menu = new JPopupMenu();
        List<String> missing = new ArrayList<>();
        for (String key : DbAdapters.standardAdapters()) {
            if (indexOf(key) < 0) {
                missing.add(key);
            }
        }
        missing.sort(Comparator.comparing(SQLScriptTabs::label, String.CASE_INSENSITIVE_ORDER));

        for (String key : missing) {
            JMenuItem item = new JMenuItem(label(key));
            item.setToolTipText(key);
            item.addActionListener(e -> {
                showScript(key);
                getSelectedEditor().getTextArea().requestFocusInWindow();
            });
            menu.add(item);
        }

        if (missing.isEmpty()) {
            JMenuItem none = new JMenuItem("All the databases have scripts");
            none.setEnabled(false);
            menu.add(none);
        }

        menu.show(addScript, 0, addScript.getHeight());
    }

    private void scriptEdited(String key, String text) {
        if (query != null) {
            setScript(query, key, text);
        }
    }

    /**
     * Stores the script in the query. The text is trimmed, and a blank one is stored as null.
     */
    void setScript(SQLTemplateDescriptor query, String key, String text) {
        String sql = text != null && !text.isBlank() ? text.trim() : null;
        if (Objects.equals(sql, getScript(query, key))) {
            return;
        }

        if (DEFAULT_SCRIPT.equals(key)) {
            query.setSql(sql);
        } else {
            query.getAdapterSql().put(key, sql);
        }
        session.fireQueryEvent(QueryEvent.ofChange(this, query));
    }

    static String getScript(SQLTemplateDescriptor query, String key) {
        return DEFAULT_SCRIPT.equals(key) ? query.getSql() : query.getAdapterSql().get(key);
    }

    private void removeScriptWithUndo(String key) {
        if (query == null || DEFAULT_SCRIPT.equals(key)) {
            return;
        }

        String text = getScript(query, key);
        removeScript(query, key);
        app.getUndoManager().addEdit(new RemoveSQLScriptUndoableEdit(this, query, key, text));
    }

    /**
     * Removes the script from the query, and its tab, if the query is shown.
     */
    void removeScript(SQLTemplateDescriptor query, String key) {
        setScript(query, key, null);
        if (query == this.query) {
            int index = indexOf(key);
            if (index >= 0) {
                tabs.removeTabAt(index);
            }
        }
    }

    /**
     * Puts the script back to the query, and shows it, if the query is shown.
     */
    void restoreScript(SQLTemplateDescriptor query, String key, String text) {
        setScript(query, key, text);
        if (query == this.query) {
            showScript(key);
            editors.get(key).setText(text);
        }
    }

    /**
     * Returns the tab title of a script: the name of the database of its DbAdapter.
     */
    static String label(String key) {
        if (DEFAULT_SCRIPT.equals(key)) {
            return DEFAULT_SCRIPT;
        }

        String label = DbAdapters.getStandardAdapterLabels().get(key);
        if (label == null) {
            label = key.substring(key.lastIndexOf('.') + 1);
        }
        label = label.trim();
        return label.endsWith("Adapter") ? label.substring(0, label.length() - "Adapter".length()).trim() : label;
    }
}
