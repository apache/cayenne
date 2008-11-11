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
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

/**
 * Validator for DataMaps.
 * 
 */
public class DataMapValidator extends TreeNodeValidator {

    /**
     * Constructor for DataMapValidator.
     */
    public DataMapValidator() {
        super();
    }

    @Override
    public void validateObject(ProjectPath path, Validator validator) {
        DataMap map = (DataMap) path.getObject();
        validateName(map, path, validator);

        // check if data map is not attached to any nodes
        validateNodeLinks(map, path, validator);
    }

    protected void validateNodeLinks(DataMap map, ProjectPath path, Validator validator) {
        DataDomain domain = (DataDomain) path.getObjectParent();
        if (domain == null) {
            return;
        }
        
        boolean unlinked = true;
        int nodeCount = 0;
        for (final DataNode node : domain.getDataNodes()) {
            nodeCount++;
            if (node.getDataMaps().contains(map)) {
                unlinked = false;
                break;
            }
        }
        
        if(unlinked && nodeCount > 0) {
        	 validator.registerWarning("DataMap is not linked to any DataNodes.", path);
        }
    }

    protected void validateName(DataMap map, ProjectPath path, Validator validator) {
        String name = map.getName();

        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed DataMap.", path);
            return;
        }

        DataDomain domain = (DataDomain) path.getObjectParent();
        if (domain == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (final DataMap otherMap : domain.getDataMaps()) {
            if (otherMap == map) {
                continue;
            }

            if (name.equals(otherMap.getName())) {
                validator.registerError("Duplicate DataMap name: " + name + ".", path);
                return;
            }
        }
    }
}
