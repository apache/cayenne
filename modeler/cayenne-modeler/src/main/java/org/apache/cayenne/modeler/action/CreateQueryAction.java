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

import java.awt.event.ActionEvent;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.query.QueryType;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.query.QueryDescriptor;

/**
 * @since 1.1
 */
public class CreateQueryAction extends CayenneAction {

    

    public static String getActionName() {
        return "Create Query";
    }

    /**
     * Constructor for CreateQueryAction.
     */
    public CreateQueryAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-query.gif";
    }

    public void performAction(ActionEvent e) {
        createQuery();
    }

    protected void createQuery() {
        new QueryType(getProjectController(),getProjectController().getCurrentDataMap()).startupAction();
    }
    
    public void createQuery(DataChannelDescriptor domain, DataMap dataMap, QueryDescriptor query) {
        dataMap.addQueryDescriptor(query);
        // notify listeners
        fireQueryEvent(this, getProjectController(), domain, dataMap, query);
    }
     
    /**
     * Fires events when a query was added
     */
    public static void fireQueryEvent(Object src, ProjectController mediator, DataChannelDescriptor domain,
            DataMap dataMap, QueryDescriptor query) {
        mediator.fireQueryEvent(new QueryEvent(src, query, MapEvent.ADD, dataMap));
        mediator.fireQueryDisplayEvent(new QueryDisplayEvent(src, query, dataMap, domain));
    }
}
