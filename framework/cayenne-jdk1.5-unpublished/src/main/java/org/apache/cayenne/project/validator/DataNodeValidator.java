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
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

/**
 */
public class DataNodeValidator extends TreeNodeValidator {

    /**
     * Constructor for DataNodeValidator.
     */
    public DataNodeValidator() {
        super();
    }

    @Override
    public void validateObject(ProjectPath path, Validator validator) {
        DataNode node = (DataNode) path.getObject();
        validateName(node, path, validator);
        validateConnection(node, path, validator);
    }

    protected void validateConnection(DataNode node, ProjectPath path, Validator validator) {
        String factory = node.getDataSourceFactory();

        // If direct factory, make sure the location is a valid file name.
        if (Util.isEmptyString(factory)) {
            validator.registerError("No DataSource factory.", path);
        }
        else if (!DriverDataSourceFactory.class.getName().equals(factory)) {
            String location = node.getDataSourceLocation();
            if (Util.isEmptyString(location)) {
                validator.registerError("DataNode has no location parameter.", path);
            }
        }
    }

    protected void validateName(DataNode node, ProjectPath path, Validator validator) {
        String name = node.getName();

        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed DataNode.", path);
            return;
        }

        DataDomain domain = (DataDomain) path.getObjectParent();
        if (domain == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (final DataNode otherNode : domain.getDataNodes()) {
            if (otherNode == node) {
                continue;
            }

            if (name.equals(otherNode.getName())) {
                validator.registerError("Duplicate DataNode name: " + name + ".", path);
                break;
            }
        }
    }
}
