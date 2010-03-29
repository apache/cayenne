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

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayListener;
import org.apache.cayenne.query.SQLTemplate;

/**
 */
public class SQLTemplateTabbedView extends JTabbedPane {

    protected ProjectController mediator;
    protected SQLTemplateMainTab mainTab;
    protected SQLTemplateScriptsTab scriptsTab;
    protected int lastSelectionIndex;

    public SQLTemplateTabbedView(ProjectController mediator) {
        this.mediator = mediator;

        initView();
        initController();
    }

    private void initView() {
        setTabPlacement(JTabbedPane.TOP);

        this.mainTab = new SQLTemplateMainTab(mediator);
        addTab("General", new JScrollPane(mainTab));

        this.scriptsTab = new SQLTemplateScriptsTab(mediator);
        addTab("SQL Scripts", scriptsTab);
    }

    private void initController() {
        mediator.addQueryDisplayListener(new QueryDisplayListener() {

            public void currentQueryChanged(QueryDisplayEvent e) {
                initFromModel();
            }
        });

        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                lastSelectionIndex = getSelectedIndex();
                updateTabs();
            }
        });
    }

    void initFromModel() {
        if (!(mediator.getCurrentQuery() instanceof SQLTemplate)) {
            setVisible(false);
            return;
        }

        // if no root, reset tabs to show the first panel..
        if (((SQLTemplate) mediator.getCurrentQuery()).getRoot() == null) {
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

    void updateTabs() {
        switch (lastSelectionIndex) {
            case 0:
                mainTab.initFromModel();
                break;
            case 1:
                scriptsTab.initFromModel();
                break;
        }
    }
}
