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
package org.apache.cayenne.project;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProjectTest {

    @Test
    public void setDisabledInspections() {
        Project project = new Project();
        project.setDisabledInspections(Arrays.asList(" B ", null, "A", "", "\t", "B", "C\n"));

        assertEquals(List.of("B", "A", "C"), List.copyOf(project.getDisabledInspections()));
    }

    @Test
    public void setDisabledInspections_Replaces() {
        Project project = new Project();
        project.setDisabledInspections(List.of("A", "B"));

        project.setDisabledInspections(List.of("C"));
        assertEquals(Set.of("C"), project.getDisabledInspections());
    }

    @Test
    public void setDisabledInspections_Null() {
        Project project = new Project();
        project.setDisabledInspections(List.of("A"));

        project.setDisabledInspections(null);
        assertTrue(project.getDisabledInspections().isEmpty());
    }

    @Test
    public void getDisabledInspections_Unmodifiable() {
        Project project = new Project();
        project.setDisabledInspections(List.of("A"));

        assertThrows(UnsupportedOperationException.class, () -> project.getDisabledInspections().add("B"));
    }
}
