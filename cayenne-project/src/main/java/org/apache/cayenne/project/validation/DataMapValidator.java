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
package org.apache.cayenne.project.validation;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

import java.util.function.Supplier;

public class DataMapValidator extends ConfigurationNodeValidator<DataMap> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public DataMapValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(DataMap node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.DATA_MAP_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.DATA_MAP_NAME_DUPLICATE, this::checkForNameDuplicates)
                .performIfEnabled(Inspection.DATA_MAP_NODE_LINKAGE, this::checkForNodeLinkage)
                .performIfEnabled(Inspection.DATA_MAP_JAVA_PACKAGE, this::validateJavaPackage);
    }

    private void checkForName(DataMap map, ValidationResult validationResult) {
        String name = map.getName();
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, map, "Unnamed DataMap");
        }
    }

    private void checkForNameDuplicates(DataMap map, ValidationResult validationResult) {
        String name = map.getName();
        DataChannelDescriptor domain = map.getDataChannelDescriptor();
        if (domain == null || Util.isEmptyString(name)) {
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

    private void checkForNodeLinkage(DataMap map, ValidationResult validationResult) {
        DataChannelDescriptor domain = map.getDataChannelDescriptor();
        if (domain == null) {
            return;
        }

        boolean linked = false;
        int nodeCount = 0;
        for (DataNodeDescriptor node : domain.getNodeDescriptors()) {
            nodeCount++;
            if (node.getDataMapNames().contains(map.getName())) {
                linked = true;
                break;
            }
        }

        if (!linked && nodeCount > 0) {
            addFailure(validationResult, map, "DataMap is not linked to any DataNodes");
        }
    }

    private void validateJavaPackage(DataMap map, ValidationResult validationResult) {
        String javaPackage = map.getDefaultPackage();
        if (Util.isEmptyString(javaPackage)) {
            addFailure(validationResult, map, "Java package is not set in DataMap '%s'", map.getName());
            return;
        }

        NameValidationHelper helper = NameValidationHelper.getInstance();
        String invalidChars = helper.invalidCharsInJavaClassName(javaPackage);
        if (invalidChars != null) {
            addFailure(validationResult, map, "DataMap '%s' Java package '%s' contains invalid characters: %s",
                    map.getName(), javaPackage, invalidChars);
        }
    }
}
