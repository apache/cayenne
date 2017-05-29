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

package org.apache.cayenne.tools;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.0
 */
public class GradlePluginTest {

    @Test
    public void apply() throws Exception {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("org.apache.cayenne");

        assertTrue(project.getTasks().getByName("cgen") instanceof CgenTask);
        assertTrue(project.getTasks().getByName("cdbimport") instanceof DbImportTask);
        assertTrue(project.getTasks().getByName("cdbgen") instanceof DbGenerateTask);

        assertTrue(project.getExtensions().getByName("cayenne") instanceof GradleCayenneExtension);
    }

}