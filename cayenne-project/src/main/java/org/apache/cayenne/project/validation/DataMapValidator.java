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
package org.apache.cayenne.project.validation;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

class DataMapValidator extends ConfigurationNodeValidator {

    void validate(DataMap map, ValidationResult validationResult) {
        validateName(map, validationResult);
        validateNodeLinks(map, validationResult);
    }

    void validateNodeLinks(DataMap map, ValidationResult validationResult) {
        DataChannelDescriptor domain = map.getDataChannelDescriptor();
        if (domain == null) {
            return;
        }

        boolean unlinked = true;
        int nodeCount = 0;
        for (DataNodeDescriptor node : domain.getNodeDescriptors()) {
            nodeCount++;
            if (node.getDataMapNames().contains(map.getName())) {
                unlinked = false;
                break;
            }
        }

        if (unlinked && nodeCount > 0) {
            addFailure(validationResult, map, "DataMap is not linked to any DataNodes");
        }
    }

    void validateName(DataMap map, ValidationResult validationResult) {
        String name = map.getName();

        if (Util.isEmptyString(name)) {
            addFailure(validationResult, map, "Unnamed DataMap");
            return;
        }

        DataChannelDescriptor domain = map.getDataChannelDescriptor();
        if (domain == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (DataMap otherMap : domain.getDataMaps()) {
            if (otherMap == map) {
                continue;
            }

            if (name.equals(otherMap.getName())) {
                addFailure(validationResult, map, "Duplicate DataMap name: %s", name);
                return;
            }
        }
    }
}
