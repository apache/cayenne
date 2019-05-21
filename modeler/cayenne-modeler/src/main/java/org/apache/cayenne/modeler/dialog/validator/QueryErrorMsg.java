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

package org.apache.cayenne.modeler.dialog.validator;

import javax.swing.JFrame;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.validation.ValidationFailure;

/**
 * @since 1.1
 */
public class QueryErrorMsg extends ValidationDisplayHandler {

    public QueryErrorMsg(ValidationFailure result) {
        super(result);
    }

    public void displayField(ProjectController mediator, JFrame frame) {
        Object object = super.validationFailure.getSource();
        DataChannelDescriptor domain = (DataChannelDescriptor) mediator
                .getProject()
                .getRootNode();
        QueryDescriptor query = (QueryDescriptor) object;
        DataMap map = query.getDataMap();

        QueryDisplayEvent event = new QueryDisplayEvent(frame, query, map, domain);
        mediator.fireQueryDisplayEvent(event);
    }
}
