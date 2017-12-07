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

package org.apache.cayenne.modeler.util.state;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.pref.ProjectStatePreferences;
import org.apache.cayenne.util.CayenneMapEntry;

public abstract class DisplayEventType {

    protected ProjectController controller;
    protected ProjectStatePreferences preferences;

    public DisplayEventType(ProjectController controller) {
        this.controller = controller;
        this.preferences = controller.getProjectStatePreferences();
    }

    public abstract void fireLastDisplayEvent();

    public abstract void saveLastDisplayEvent();

    protected String getObjectName(ConfigurationNode object) {
        if (object instanceof CayenneMapEntry) {
            return ((CayenneMapEntry) object).getName();
        } else if (object instanceof DataChannelDescriptor) {
            return ((DataChannelDescriptor) object).getName();
        } else if (object instanceof DataNodeDescriptor) {
            return ((DataNodeDescriptor) object).getName();
        } else if (object instanceof DataMap) {
            return ((DataMap) object).getName();
        } else if (object instanceof Embeddable) {
            return ((Embeddable) object).getClassName();
        } else if (object instanceof QueryDescriptor) {
            return ((QueryDescriptor) object).getName();
        } else {
            return "";
        }
    }

    protected String parseToString(CayenneMapEntry[] array) {
        if (array == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (CayenneMapEntry entry : array) {
            if(entry == null) {
                continue;
            }
            sb.append(entry.getName()).append(",");
        }

        return sb.toString();
    }

}
