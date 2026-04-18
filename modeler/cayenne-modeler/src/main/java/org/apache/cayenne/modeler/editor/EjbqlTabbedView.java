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

import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.ProjectController;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

public class EjbqlTabbedView extends JTabbedPane {

    private final ProjectController controller;
    private final EjbqlQueryMainTab mainTab;
    private final EjbqlQueryScriptsTab scriptsTab;
    private int lastTabIndex;

    public EjbqlTabbedView(ProjectController controller) {
        this.controller = controller;

        setTabPlacement(JTabbedPane.TOP);

        this.mainTab = new EjbqlQueryMainTab(controller);
        addTab("General", new JScrollPane(mainTab));

        this.scriptsTab = new EjbqlQueryScriptsTab(controller);
        addTab("EJBQL", scriptsTab);

        controller.addQueryDisplayListener(e -> initFromModel());
        addChangeListener(this::stateChanged);
    }

    private void stateChanged(ChangeEvent e) {
        lastTabIndex = getSelectedIndex();
        updateTabs();
    }

    private void initFromModel() {
        if (!QueryDescriptor.EJBQL_QUERY.equals(controller.getSelectedQuery().getType())) {
            setVisible(false);
            return;
        }

        // tab did not change - force update
        if (getSelectedIndex() == lastTabIndex) {
            updateTabs();
        }
        // change tab, this will update newly displayed tab...
        else {
            setSelectedIndex(lastTabIndex);
        }

        setVisible(true);
    }

    private void updateTabs() {
        switch (lastTabIndex) {
            case 0:
                mainTab.initFromModel();
                break;
            case 1:
                scriptsTab.initFromModel();
                break;
        }
    }
}
