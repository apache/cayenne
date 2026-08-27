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

import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectTabbedPane;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

public class SelectQueryTabbedView extends ProjectTabbedPane {

    private final SelectQueryMainTab mainTab;
    private final SelectQueryPrefetchTab prefetchTab;
    private final SelectQueryOrderingTab orderingTab;
    private int lastSelectionIndex;

    public SelectQueryTabbedView(ProjectSession session) {
        super(session);
        this.mainTab = new SelectQueryMainTab(session);
        this.orderingTab = new SelectQueryOrderingTab(session);
        this.prefetchTab = new SelectQueryPrefetchTab(session);
        initLayout();
        initBindings();
    }

    private void initLayout() {
        setTabPlacement(JTabbedPane.TOP);
        addTab("General", new JScrollPane(mainTab));
        addTab("Orderings", orderingTab);
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
        if (!QueryDescriptor.SELECT_QUERY.equals(session.getSelectedQuery().getType())) {
            setVisible(false);
            return;
        }

        // if no root, reset tabs to show the first panel..
        if (session.getSelectedQuery().getRoot() == null) {
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
