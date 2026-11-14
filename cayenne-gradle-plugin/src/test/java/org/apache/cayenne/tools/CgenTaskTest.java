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

package org.apache.cayenne.tools;

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.CgenTemplate;
import org.apache.cayenne.map.DataMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CgenTaskTest {

    @TempDir
    File temp;

    DataMap dataMap = new DataMap();

    private CgenTask createCgenTask() {
        Project project = ProjectBuilder.builder().withProjectDir(temp).build();
        CgenTask task = project.getTasks().create("cgenTask", CgenTask.class);
        task.setDestDir(new File(temp, "out"));
        return task;
    }

    @Test
    public void generatorCreation() {
        CgenTask task = createCgenTask();
        task.setEmbeddableSuperTemplate("superTemplate");
        task.setEmbeddableTemplate("template");
        task.setEncoding("UTF-8");
        task.setExcludeEntities("entity1");
        task.setIncludeEntities("entity2");
        task.setMode("entity");
        task.setOutputPattern("pattern");
        task.setSuperPkg("org.example.model.auto");
        task.setSuperTemplate("superTemplate");
        task.setTemplate("org/apache/cayenne/tools/velotemplate.vm");
        task.setMakePairs(true);
        task.setCreatePropertyNames(true);
        task.setOverwrite(true);
        task.setUsePkgPath(true);

        for (CgenConfiguration configuration : task.buildConfigurations(dataMap)) {

            CgenTemplate cgenTemplate = configuration.getTemplate();
            assertNotNull(configuration.getEmbeddableSuperTemplate());
            assertNotNull(configuration.getEmbeddableTemplate());

            assertEquals("UTF-8", configuration.getEncoding());
            assertEquals("entity", configuration.getArtifactsGenerationMode());
            assertEquals("pattern", configuration.getOutputPattern());
            assertEquals("org.example.model.auto", configuration.getSuperPkg());
            assertTrue(cgenTemplate.isFile());
            assertEquals("org/apache/cayenne/tools/velotemplate.vm", cgenTemplate.getData());
            assertTrue(configuration.isMakePairs());
            assertTrue(configuration.isCreatePropertyNames());
            assertTrue(configuration.isOverwrite());
            assertTrue(configuration.isUsePkgPath());
        }
    }

}