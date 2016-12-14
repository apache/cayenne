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
import org.apache.cayenne.modeler.dialog.db.gen.DBGeneratorOptions;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.Project;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Action that generates database tables from a DataMap.
 */
public class GenerateDBAction extends CayenneAction {

    public static String getActionName() {
        return "Generate Database Schema";
    }

    public GenerateDBAction(Application application) {
        super(getActionName(), application);
    }

    public void performAction(ActionEvent e) {

        Collection<DataMap> dataMaps;
        DataMap dataMap = getProjectController().getCurrentDataMap();

        if (dataMap != null) {
            dataMaps = new ArrayList<>();
            dataMaps.add(dataMap);
        } else {
            Project project = getProjectController().getProject();
            dataMaps = ((DataChannelDescriptor) project.getRootNode()).getDataMaps();
        }
        new DBGeneratorOptions(getProjectController(), "Generate DB Schema: Options", dataMaps)
                .startupAction();
    }
}
