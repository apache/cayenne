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
package org.apache.cayenne.modeler.ui.project.editor.procedure;

import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectTabbedPane;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

/**
 * A single-tab container for the {@link ProcedureQueryView}, so that the editor looks like the other
 * query editors, which are tabbed.
 */
public class ProcedureQueryTabbedView extends ProjectTabbedPane {

    public ProcedureQueryTabbedView(ProjectSession session) {
        super(session);
        setTabPlacement(JTabbedPane.TOP);
        addTab("ProcedureQuery", new JScrollPane(new ProcedureQueryView(session)));
    }
}
