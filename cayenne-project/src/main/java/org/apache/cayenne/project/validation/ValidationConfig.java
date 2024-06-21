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
import org.apache.cayenne.configuration.xml.DataChannelMetaData;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * @since 5.0
 */
public class ValidationConfig {

    private final Set<Inspection> enabledInspections;

    public ValidationConfig() {
        this(EnumSet.allOf(Inspection.class));
    }

    public ValidationConfig(ValidationConfig other) {
        this(other.enabledInspections);
    }

    public ValidationConfig(Set<Inspection> enabledInspections) {
        this.enabledInspections = Collections.unmodifiableSet(EnumSet.copyOf(enabledInspections));
    }

    public Set<Inspection> getEnabledInspections() {
        return enabledInspections;
    }

    public boolean isEnabled(Inspection inspection) {
        return enabledInspections.contains(inspection);
    }

    public static ValidationConfig fromMetadata(DataChannelMetaData metaData, DataChannelDescriptor dataChannel) {
        return Optional.ofNullable(metaData.get(dataChannel, ValidationConfig.class)).orElseGet(ValidationConfig::new);
    }
}
