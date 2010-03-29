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

package org.apache.cayenne.project.validator;

import java.util.Iterator;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

/**
 */
public class DomainValidator extends TreeNodeValidator {
    /**
     * Constructor for DomainValidator.
     */
    public DomainValidator() {
        super();
    }

    @Override
    public void validateObject(ProjectPath path, Validator validator) {

        // check for empty name
        DataDomain domain = (DataDomain) path.getObject();
        String name = domain.getName();
        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed DataDomain.", path);

            // no more name assertions
            return;
        }

        Project project = (Project) path.getObjectParent();
        if (project == null) {
            return;
        }

        // check for duplicate names in the parent context
        Iterator it = project.getChildren().iterator();
        while (it.hasNext()) {
            DataDomain dom = (DataDomain) it.next();
            if (dom == domain) {
                continue;
            }

            if (name.equals(dom.getName())) {
                validator.registerError("Duplicate DataDomain name: " + name + ".", path);
                return;
            }
        }
    }
}
