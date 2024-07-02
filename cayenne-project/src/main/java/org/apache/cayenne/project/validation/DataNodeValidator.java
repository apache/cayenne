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
import org.apache.cayenne.configuration.runtime.XMLPoolingDataSourceFactory;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

import java.util.function.Supplier;

class DataNodeValidator extends ConfigurationNodeValidator<DataNodeDescriptor> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public DataNodeValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(DataNodeDescriptor node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.DATA_NODE_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.DATA_NODE_NAME_DUPLICATE, this::checkForNameDuplicates)
                .performIfEnabled(Inspection.DATA_NODE_CONNECTION_PARAMS, this::validateConnection);
    }

    private void checkForName(DataNodeDescriptor node, ValidationResult validationResult) {
        String name = node.getName();
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, node, "Unnamed DataNode");
        }

    }

    private void checkForNameDuplicates(DataNodeDescriptor node, ValidationResult validationResult) {
        String name = node.getName();
        if (Util.isEmptyString(name)) {
            return;
        }
        DataChannelDescriptor dataChannelDescriptor = node.getDataChannelDescriptor();

        // check for duplicate names in the parent context
        for (DataNodeDescriptor otherNode : dataChannelDescriptor.getNodeDescriptors()) {
            if (otherNode == node) {
                continue;
            }

            if (name.equals(otherNode.getName())) {
                addFailure(validationResult, node, "Duplicate DataNode name: %s", name);
                return;
            }
        }
    }

    private void validateConnection(DataNodeDescriptor node, ValidationResult validationResult) {
        String factory = node.getDataSourceFactoryType();

        // TODO: andrus 03/10/2010 - null factory is allowed, however
        //  'getDataSourceDescriptor' must ne not null in this case

        if (factory == null || XMLPoolingDataSourceFactory.class.getName().equals(factory)) {
            return;
        }
        String parameters = node.getParameters();
        if (Util.isEmptyString(parameters)) {
            addFailure(validationResult, node, "DataNode has empty 'parameters' string");
        }
    }
}
