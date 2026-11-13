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
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidationConfigTest {

    @Test
    public void fromProject() {
        Project project = new Project();
        project.setDisabledInspections(List.of("SQL_TEMPLATE_NO_ROOT", "PROJECT_NO_NAME"));

        ValidationConfig config = ValidationConfig.fromProject(project);
        assertEquals(EnumSet.complementOf(EnumSet.of(Inspection.SQL_TEMPLATE_NO_ROOT, Inspection.PROJECT_NO_NAME)),
                config.getEnabledInspections());
    }

    @Test
    public void fromProject_UnknownName() {
        Project project = new Project();
        project.setDisabledInspections(List.of("NO_SUCH_INSPECTION", "DB_ENTITY_NO_PK"));

        ValidationConfig config = ValidationConfig.fromProject(project);
        assertEquals(EnumSet.complementOf(EnumSet.of(Inspection.DB_ENTITY_NO_PK)), config.getEnabledInspections());
    }

    @Test
    public void fromProject_Empty() {
        ValidationConfig config = ValidationConfig.fromProject(new Project());
        assertEquals(EnumSet.allOf(Inspection.class), config.getEnabledInspections());
    }

    @Test
    public void applyTo() {
        Project project = new Project();
        project.setDisabledInspections(List.of("NO_SUCH_INSPECTION"));

        ValidationConfig config = new ValidationConfig(
                EnumSet.complementOf(EnumSet.of(Inspection.SQL_TEMPLATE_NO_ROOT, Inspection.PROJECT_NO_NAME)));
        config.applyTo(project);

        assertEquals(List.of("PROJECT_NO_NAME", "SQL_TEMPLATE_NO_ROOT"), List.copyOf(project.getDisabledInspections()));
    }

    @Test
    public void applyTo_AllEnabled() {
        Project project = new Project();
        project.setDisabledInspections(List.of("PROJECT_NO_NAME"));

        new ValidationConfig().applyTo(project);
        assertTrue(project.getDisabledInspections().isEmpty());
    }

    @Test
    public void roundTrip() {
        Project project = new Project();
        project.setDisabledInspections(List.of("DB_ENTITY_NO_PK", "PROJECT_NO_NAME"));

        ValidationConfig config = ValidationConfig.fromProject(project);
        Project copy = new Project();
        config.applyTo(copy);

        assertEquals(config.getEnabledInspections(), ValidationConfig.fromProject(copy).getEnabledInspections());
        assertEquals(List.of("PROJECT_NO_NAME", "DB_ENTITY_NO_PK"), List.copyOf(copy.getDisabledInspections()));
    }
}
