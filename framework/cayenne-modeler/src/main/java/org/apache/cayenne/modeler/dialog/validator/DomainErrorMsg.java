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


package org.apache.cayenne.modeler.dialog.validator;

import javax.swing.JFrame;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.DomainDisplayEvent;
import org.apache.cayenne.project.validator.ValidationInfo;

/**
 * DataDomain validation message.
 * 
 */
public class DomainErrorMsg extends ValidationDisplayHandler {

    /**
     * Constructor for DomainErrorMsg.
     * @param result
     */
    public DomainErrorMsg(ValidationInfo result) {
        super(result);
        
        Object[] path = result.getPath().getPath();
        int len = path.length;

        if (len >= 1) {
            domain = (DataDomain) path[len - 1];
        }
    }

	public void displayField(ProjectController mediator, JFrame frame) {
		DomainDisplayEvent event;
		event = new DomainDisplayEvent(frame, domain);
		mediator.fireDomainDisplayEvent(event);
	}

}
