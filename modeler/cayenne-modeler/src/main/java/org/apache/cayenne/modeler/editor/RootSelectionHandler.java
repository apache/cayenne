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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.util.CellRenderers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Handler to user's actions with root selection combobox
 */
class RootSelectionHandler implements FocusListener, ActionListener {
    private String newName = null;
    private boolean needChangeName;
    private final BaseQueryMainTab queryTab;

    RootSelectionHandler(BaseQueryMainTab queryTab) {
        this.queryTab = queryTab;
    }

    public void actionPerformed(ActionEvent ae) {
        QueryDescriptor query = queryTab.getQuery();
        if (query != null) {
            Entity root = (Entity) queryTab.getQueryRoot().getModel().getSelectedItem();

            if (root != null) {
                query.setRoot(root);

                if (needChangeName) { //not changed by user
                    /*
                     * Doing auto name change, following CAY-888 #2
                     */
                    String newPrefix = root.getName() + "Query";
                    newName = newPrefix;

                    DataMap map = queryTab.getMediator().getCurrentDataMap();
                    long postfix = 1;

                    while (map.getQueryDescriptor(newName) != null) {
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

        QueryDescriptor query = queryTab.getQuery();
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
     *
     * We cannot follow user input because tab might be opened many times
     */
    boolean hasDefaultName(QueryDescriptor query) {
        String prefix = query.getRoot() == null ? "UntitledQuery" :
            CellRenderers.asString(query.getRoot()) + "Query";

        return queryTab.getNameField().getComponent().getText().startsWith(prefix);
    }
}
