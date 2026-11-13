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
package org.apache.cayenne.projecttools.validation;

import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * @since 5.0
 */
public class ValidationConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationConfig.class);

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

    /**
     * Creates a config that enables all the inspections except the ones disabled in the project.
     *
     * @since 6.0
     */
    public static ValidationConfig fromProject(Project project) {
        Set<Inspection> enabledInspections = EnumSet.allOf(Inspection.class);
        for (String name : project.getDisabledInspections()) {
            try {
                enabledInspections.remove(Inspection.valueOf(name));
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Ignoring unknown validation inspection '{}'", name);
            }
        }
        return new ValidationConfig(enabledInspections);
    }

    /**
     * Replaces the disabled inspections of the project with the ones of this config, except the deprecated ones.
     *
     * @since 6.0
     */
    public void applyTo(Project project) {
        Set<Inspection> disabledInspections = EnumSet.allOf(Inspection.class);
        disabledInspections.removeAll(enabledInspections);
        disabledInspections.removeIf(Inspection::isDeprecated);
        project.setDisabledInspections(disabledInspections.stream().map(Inspection::name).toList());
    }
}
