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
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.gradle.api.logging.Logging;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @since 4.0
 */
public class CgenTaskTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    DataMap dataMap = new DataMap();

    private CgenTask createCgenTaskMock() {
        CgenTask mock = mock(CgenTask.class);

        doCallRealMethod().when(mock).setClient(anyBoolean());
        doCallRealMethod().when(mock).setAdditionalMaps(any(File.class));
        doCallRealMethod().when(mock).setCreatePropertyNames(anyBoolean());
        doCallRealMethod().when(mock).setEmbeddableSuperTemplate(anyString());
        doCallRealMethod().when(mock).setEmbeddableTemplate(anyString());
        doCallRealMethod().when(mock).setEncoding(anyString());
        doCallRealMethod().when(mock).setExcludeEntities(anyString());
        doCallRealMethod().when(mock).setIncludeEntities(anyString());
        doCallRealMethod().when(mock).setMakePairs(anyBoolean());
        doCallRealMethod().when(mock).setMode(anyString());
        doCallRealMethod().when(mock).setOutputPattern(anyString());
        doCallRealMethod().when(mock).setSuperPkg(anyString());
        doCallRealMethod().when(mock).setSuperTemplate(anyString());
        doCallRealMethod().when(mock).setOverwrite(anyBoolean());
        doCallRealMethod().when(mock).setUsePkgPath(anyBoolean());
        doCallRealMethod().when(mock).setTemplate(anyString());
        when(mock.buildConfiguration(dataMap)).thenCallRealMethod();
        when(mock.createGenerator(dataMap)).thenCallRealMethod();
        when(mock.getLogger()).thenReturn(Logging.getLogger(CgenTaskTest.class));

        return mock;
    }

    @Test
    public void testGeneratorCreation() {
        CgenTask task = createCgenTaskMock();
        task.setEmbeddableSuperTemplate("superTemplate");
        task.setEmbeddableTemplate("template");
        task.setEncoding("UTF-8");
        task.setExcludeEntities("entity1");
        task.setIncludeEntities("entity2");
        task.setMode("entity");
        task.setOutputPattern("pattern");
        task.setSuperPkg("org.example.model.auto");
        task.setSuperTemplate("superTemplate");
        task.setTemplate("template");
        task.setMakePairs(true);
        task.setCreatePropertyNames(true);
        task.setOverwrite(true);
        task.setUsePkgPath(true);

        CgenConfiguration configuration = task.buildConfiguration(dataMap);
        ClassGenerationAction createdAction = new ClassGenerationAction(configuration);

        CgenConfiguration cgenConfiguration = createdAction.getCgenConfiguration();
        assertEquals(cgenConfiguration.getEmbeddableSuperTemplate(), "superTemplate");
        assertEquals(cgenConfiguration.getEmbeddableTemplate(), "template");
        assertEquals(cgenConfiguration.getEncoding(), "UTF-8");
        assertEquals(cgenConfiguration.getArtifactsGenerationMode(), "entity");
        assertEquals(cgenConfiguration.getOutputPattern(), "pattern");
        assertEquals(cgenConfiguration.getSuperPkg(), "org.example.model.auto");
        assertEquals(cgenConfiguration.getSuperTemplate(), "superTemplate");
        assertEquals(cgenConfiguration.getTemplate(), "template");
        assertTrue(cgenConfiguration.isMakePairs());
        assertTrue(cgenConfiguration.isCreatePropertyNames());
        assertTrue(cgenConfiguration.isOverwrite());
        assertTrue(cgenConfiguration.isUsePkgPath());

    }

}