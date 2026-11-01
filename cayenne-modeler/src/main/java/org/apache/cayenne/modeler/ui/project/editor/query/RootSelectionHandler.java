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

package org.apache.cayenne.modeler.ui.project.editor.query;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.NamedProcedureQuery;
import org.apache.cayenne.map.NamedQuery;
import org.apache.cayenne.map.NamedSQLQuery;
import org.apache.cayenne.map.NamedSelectQuery;
import org.apache.cayenne.modeler.toolkit.Renderers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Handler to user's actions with root selection combobox
 */
class RootSelectionHandler implements FocusListener, ActionListener {

    private String newName;
    private boolean needChangeName;
    private final BaseQueryMainTab queryTab;

    RootSelectionHandler(BaseQueryMainTab queryTab) {
        this.queryTab = queryTab;
    }

    public void actionPerformed(ActionEvent ae) {
        NamedQuery query = queryTab.getQuery();
        if (query != null) {
            Entity<?, ?, ?> root = (Entity<?, ?, ?>) queryTab.getQueryRoot().getModel().getSelectedItem();

            if (root != null) {
                setRoot(query, root);

                if (needChangeName) { //not changed by user
                    /*
                     * Doing auto name change, following CAY-888 #2
                     */
                    String newPrefix = root.getName() + "Query";
                    newName = newPrefix;

                    DataMap map = queryTab.getSession().getSelectedDataMap();
                    long postfix = 1;

                    while (map.getNamedQuery(newName) != null) {
                        newName = newPrefix + (postfix++);
                    }

                    queryTab.getNameField().setText(newName);
                }
            }
        }
    }

    public void focusGained(FocusEvent e) {
        //reset new name tracking
        newName = null;

        NamedQuery query = queryTab.getQuery();
        if (query != null) {
            needChangeName = hasDefaultName(query);
        } else {
            needChangeName = false;
        }
    }

    public void focusLost(FocusEvent e) {
        if (newName != null) {
            queryTab.setQueryName(newName);
        }

        newName = null;
        needChangeName = false;
    }

    /**
     * @return whether specified's query name is 'default' i.e. Cayenne generated
     * A query's name is 'default' if it starts with 'UntitledQuery' or with root name.
     * <p>
     * We cannot follow user input because tab might be opened many times
     */
    boolean hasDefaultName(NamedQuery query) {
        Object root = rootOf(query);
        String prefix = root == null ? "UntitledQuery" :
                Renderers.asString(root, queryTab.getSession().getSelectedDataMap()) + "Query";

        return queryTab.getNameField().getText().startsWith(prefix);
    }

    // a select query has no root of its own, its root is the "from" clause of its String
    private static Object rootOf(NamedQuery query) {
        return switch (query) {
            case NamedSQLQuery sql -> sql.getRoot();
            case NamedProcedureQuery procedure -> procedure.getRoot();
            case NamedSelectQuery ignored -> null;
        };
    }

    private static void setRoot(NamedQuery query, Object root) {
        switch (query) {
            case NamedSQLQuery sql -> sql.setRoot(root);
            case NamedProcedureQuery procedure -> procedure.setRoot(root);
            case NamedSelectQuery ignored -> {
            }
        }
    }
}
