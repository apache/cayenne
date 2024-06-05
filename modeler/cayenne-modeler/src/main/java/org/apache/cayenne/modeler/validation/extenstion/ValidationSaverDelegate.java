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
package org.apache.cayenne.modeler.validation.extenstion;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.project.extension.BaseSaverDelegate;
import org.apache.cayenne.project.validation.Inspection;
import org.apache.cayenne.project.validation.ValidationConfig;

import java.util.EnumSet;
import java.util.Set;

public class ValidationSaverDelegate extends BaseSaverDelegate {

    private final DataChannelMetaData metaData;

    ValidationSaverDelegate(DataChannelMetaData metaData) {
        this.metaData = metaData;
    }

    private Void printValidationConfig(DataChannelDescriptor dataChannelDescriptor) {
        ValidationConfig validationConfig = metaData.get(dataChannelDescriptor, ValidationConfig.class);
        if (validationConfig == null) {
            return null;
        }

        Set<Inspection> disabledInspections = EnumSet.allOf(Inspection.class);
        disabledInspections.removeAll(validationConfig.getEnabledInspections());
        encoder.start("validation").attribute("xmlns", ValidationExtension.NAMESPACE);
        for (Inspection inspection : disabledInspections) {
            encoder.simpleTag(ValidationConfigHandler.EXCLUDE_TAG, inspection.name());
        }
        encoder.end();
        return null;
    }

    @Override
    public Void visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor) {
        return printValidationConfig(channelDescriptor);
    }
}
