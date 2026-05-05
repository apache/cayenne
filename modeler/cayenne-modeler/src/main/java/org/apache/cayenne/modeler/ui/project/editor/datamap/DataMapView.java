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
package org.apache.cayenne.modeler.ui.project.editor.datamap;

import org.apache.cayenne.modeler.event.display.DataMapDisplayEvent;
import org.apache.cayenne.modeler.toolkit.ProjectTabbedPane;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.datamap.cgen.CgenPanel;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportView;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.DataMapMainView;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

public class DataMapView extends ProjectTabbedPane {

    public DataMapView(ProjectSession session) {
        super(session);
        initLayout();
        initBindings();
    }

    private void initLayout() {
        setTabPlacement(JTabbedPane.TOP);
        addTab("DataMap", new JScrollPane(new DataMapMainView(session())));
        addTab("DB Import", new JScrollPane(new DbImportView(session())));
        addTab("Class Generation", new JScrollPane(new CgenPanel(app(), session())));
    }

    private void initBindings() {
        session().addDataMapDisplayListener(this::currentDataMapChanged);
    }

    private void currentDataMapChanged(DataMapDisplayEvent e) {

        if (e.isMainTabFocus()) {
            setSelectedIndex(0);
        }
    }
}
