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
package org.apache.cayenne.modeler.graph.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.graph.DataDomainGraphTab;
import org.apache.cayenne.modeler.graph.GraphBuilder;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.jgraph.JGraph;

/**
 * Action for zooming in graph
 */
public class ZoomInAction extends CayenneAction {    
    private final DataDomainGraphTab dataDomainGraphTab;
    
    public ZoomInAction(DataDomainGraphTab dataDomainGraphTab, Application application) {
        super("Zoom In", application);
        this.dataDomainGraphTab = dataDomainGraphTab;
        setEnabled(true);
    }
    
    @Override
    public String getIconName() {
        return "icon-zoom-in.png";
    }
    
    @Override
    public void performAction(ActionEvent e) {
        JGraph graph = dataDomainGraphTab.getGraph();
        graph.setScale(graph.getScale() * GraphBuilder.ZOOM_FACTOR);
    }
}
