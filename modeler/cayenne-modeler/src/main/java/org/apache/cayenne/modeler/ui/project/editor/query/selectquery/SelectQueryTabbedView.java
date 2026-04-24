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

package org.apache.cayenne.modeler.ui.project.editor.query.selectquery;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;

import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.map.QueryDescriptor;

public class SelectQueryTabbedView extends JTabbedPane {

    private final ProjectController mediator;
    private final SelectQueryMainTab mainTab;
    private final SelectQueryPrefetchTab prefetchTab;
    private final SelectQueryOrderingTab orderingTab;
    private int lastSelectionIndex;

    public SelectQueryTabbedView(ProjectController mediator) {
        this.mediator = mediator;

        setTabPlacement(JTabbedPane.TOP);

        this.mainTab = new SelectQueryMainTab(mediator);
        addTab("General", new JScrollPane(mainTab));

        this.orderingTab = new SelectQueryOrderingTab(mediator);
        addTab("Orderings", orderingTab);

        this.prefetchTab = new SelectQueryPrefetchTab(mediator);
        addTab("Prefetches", prefetchTab);

        mediator.addQueryDisplayListener(e -> initFromModel());
        addChangeListener(this::stateChanged);
    }

    private void stateChanged(ChangeEvent e) {
        lastSelectionIndex = getSelectedIndex();
        updateTabs();
    }

    private void initFromModel() {
        if (!QueryDescriptor.SELECT_QUERY.equals(mediator.getSelectedQuery().getType())) {
            setVisible(false);
            return;
        }

        // if no root, reset tabs to show the first panel..
        if (mediator.getSelectedQuery().getRoot() == null) {
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
                orderingTab.initFromModel();
                break;
            case 2:
                prefetchTab.initFromModel();
                break;
        }
    }
}
