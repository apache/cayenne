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
package org.apache.cayenne.modeler.ui.project.editor.query.ejbql;

import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.toolkit.ProjectTabbedPane;
import org.apache.cayenne.modeler.project.ProjectSession;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;

public class EjbqlTabbedView extends ProjectTabbedPane {

    private final EjbqlQueryMainTab mainTab;
    private final EjbqlQueryScriptsTab scriptsTab;
    private int lastTabIndex;

    public EjbqlTabbedView(ProjectSession session) {
        super(session);

        setTabPlacement(JTabbedPane.TOP);

        this.mainTab = new EjbqlQueryMainTab(session);
        addTab("General", new JScrollPane(mainTab));

        this.scriptsTab = new EjbqlQueryScriptsTab(session);
        addTab("EJBQL", scriptsTab);

        session.addQueryDisplayListener(e -> initFromModel());
        addChangeListener(this::stateChanged);
    }

    private void stateChanged(ChangeEvent e) {
        lastTabIndex = getSelectedIndex();
        updateTabs();
    }

    private void initFromModel() {
        if (!QueryDescriptor.EJBQL_QUERY.equals(session().getSelectedQuery().getType())) {
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
