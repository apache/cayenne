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

import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.ui.project.ProjectController;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

public class SQLTemplateTabbedView extends JTabbedPane {

    private final ProjectController controller;
    private final SQLTemplateMainTab mainTab;
    private final SQLTemplateScriptsTab scriptsTab;
    private final SQLTemplatePrefetchTab prefetchTab;
    private int lastSelectionIndex;

    public SQLTemplateTabbedView(ProjectController controller) {
        this.controller = controller;

        setTabPlacement(JTabbedPane.TOP);

        this.mainTab = new SQLTemplateMainTab(controller);
        addTab("General", new JScrollPane(mainTab));

        this.scriptsTab = new SQLTemplateScriptsTab(controller);
        addTab("SQL Scripts", scriptsTab);

        this.prefetchTab = new SQLTemplatePrefetchTab(controller);
        addTab("Prefetches", prefetchTab);

        controller.addQueryDisplayListener(e -> initFromModel());
        addChangeListener(this::stateChanged);
    }

    private void stateChanged(ChangeEvent e) {
        lastSelectionIndex = getSelectedIndex();
        updateTabs();
    }

    private void initFromModel() {
        if (!QueryDescriptor.SQL_TEMPLATE.equals(controller.getSelectedQuery().getType())) {
            setVisible(false);
            return;
        }

        // if no root, reset tabs to show the first panel..
        if (controller.getSelectedQuery().getRoot() == null) {
            lastSelectionIndex = 0;
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
        switch (lastSelectionIndex) {
            case 0:
                mainTab.initFromModel();
                break;
            case 1:
                scriptsTab.initFromModel();
                break;
            case 2:
                prefetchTab.initFromModel();
                break;
        }
    }

    public SQLTemplateScriptsTab getScriptsTab() {
        return scriptsTab;
    }
}
