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
package org.apache.cayenne.project2.validation;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.util.Util;

class DataMapValidator {

    void validate(Object object, ValidationVisitor validationVisitor) {
        DataMap map = (DataMap) object;
        validateName(map, object, validationVisitor);

        // check if data map is not attached to any nodes
        validateNodeLinks(map, object, validationVisitor);
    }

    void validateNodeLinks(DataMap map, Object object, ValidationVisitor validationVisitor) {
        DataChannelDescriptor domain = (DataChannelDescriptor) validationVisitor
                .getProject()
                .getRootNode();
        if (domain == null) {
            return;
        }

        boolean unlinked = true;
        int nodeCount = 0;
        for (final DataNodeDescriptor node : domain.getNodeDescriptors()) {
            nodeCount++;
            if (node.getDataMapNames().contains(map.getName())) {
                unlinked = false;
                break;
            }
        }

        if (unlinked && nodeCount > 0) {
            validationVisitor.registerWarning("DataMap is not linked to any DataNodes.", object);
        }
    }

    void validateName(DataMap map, Object object, ValidationVisitor validationVisitor) {
        String name = map.getName();

        if (Util.isEmptyString(name)) {
            validationVisitor.registerError("Unnamed DataMap.", object);
            return;
        }

        DataChannelDescriptor domain = (DataChannelDescriptor) validationVisitor
                .getProject()
                .getRootNode();
        if (domain == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (final DataMap otherMap : domain.getDataMaps()) {
            if (otherMap == map) {
                continue;
            }

            if (name.equals(otherMap.getName())) {
                validationVisitor.registerError("Duplicate DataMap name: " + name + ".", object);
                return;
            }
        }
    }

}
