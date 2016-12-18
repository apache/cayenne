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

package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.codegen.CodeGeneratorController;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.Project;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

public class GenerateCodeAction extends CayenneAction {

    public static String getActionName() {
        return "Generate Classes";
    }

    public GenerateCodeAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-gen_java.gif";
    }

    public void performAction(ActionEvent e) {
        Collection<DataMap> dataMaps;
        DataMap dataMap = getProjectController().getCurrentDataMap();

        if (dataMap != null) {
            dataMaps = new ArrayList<>();
            dataMaps.add(dataMap);

            new CodeGeneratorController(getApplication().getFrameController(), dataMaps).startup();
        } else if (getProjectController().getCurrentDataNode() != null) {
            Collection<String> nodeMaps = getProjectController().getCurrentDataNode().getDataMapNames();
            Project project = getProjectController().getProject();
            dataMaps = ((DataChannelDescriptor) project.getRootNode()).getDataMaps();

            Collection<DataMap> resultMaps = new ArrayList<>();
            for (DataMap map : dataMaps) {
                if (nodeMaps.contains(map.getName())) {
                    resultMaps.add(map);
                }
            }

            new CodeGeneratorController(getApplication().getFrameController(), resultMaps).startup();
        } else {
            Project project = getProjectController().getProject();
            dataMaps = ((DataChannelDescriptor) project.getRootNode()).getDataMaps();

            new CodeGeneratorController(getApplication().getFrameController(), dataMaps).startup();
        }
    }
}
