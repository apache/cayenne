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

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;

import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectTabbedPane;

public class SQLTemplateTabbedView extends ProjectTabbedPane {

    private static final int MAIN_TAB = 0;
    private static final int PREFETCH_TAB = 1;

    private final SQLTemplateView mainTab;
    private final SQLTemplatePrefetchTab prefetchTab;
    private int lastSelectionIndex;

    public SQLTemplateTabbedView(ProjectSession session) {
        super(session);
        this.mainTab = new SQLTemplateView(session);
        this.prefetchTab = new SQLTemplatePrefetchTab(session);
        initLayout();
        initBindings();
    }

    private void initLayout() {
        setTabPlacement(JTabbedPane.TOP);

        // not in a scroll pane, so that the SQL editor takes all the height of the tab
        addTab("SQLTemplate", mainTab);
        addTab("Prefetches", prefetchTab);
    }

    private void initBindings() {
        session.addQueryDisplayListener(e -> initFromModel());
        addChangeListener(this::stateChanged);
    }

    private void stateChanged(ChangeEvent e) {
        lastSelectionIndex = getSelectedIndex();
        updateTabs();
    }

    private void initFromModel() {
        if (!(session.getSelectedQuery() instanceof SQLTemplateDescriptor query)) {
            setVisible(false);
            return;
        }

        // the prefetches are only meaningful for a query with a root entity
        if (query.getRoot() == null) {
            lastSelectionIndex = MAIN_TAB;
        }

        // tab did not change - force update
        if (getSelectedIndex() == lastSelectionIndex) {
            updateTabs();
        }
        // change tab, this will update newly displayed tab...
        else {
            setSelectedIndex(lastSelectionIndex);
        }

        setVisible(true);
    }

    private void updateTabs() {
        // the main tab follows the query display events itself
        if (lastSelectionIndex == PREFETCH_TAB) {
            prefetchTab.initFromModel();
        }
    }

    /**
     * Returns the key of the SQL script shown in the editor.
     */
    public String getSelectedScript() {
        return mainTab.getSelectedScript();
    }

    /**
     * Shows the SQL script in the editor.
     */
    public void showScript(String key) {
        mainTab.showScript(key);
    }
}
