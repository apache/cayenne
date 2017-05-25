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

import org.apache.cayenne.gen.ClassGenerationAction;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @since 4.0
 */
public class CgenTaskTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private CgenTask createCgenTaskMock(ClassGenerationAction action) {
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
        when(mock.newGeneratorInstance()).thenReturn(action);
        when(mock.createGenerator()).thenCallRealMethod();

        return mock;
    }

    @Test
    public void testGeneratorCreation() {
        ClassGenerationAction action = mock(ClassGenerationAction.class);
        CgenTask task = createCgenTaskMock(action);

        task.setEmbeddableSuperTemplate("superTemplate");
        task.setEmbeddableTemplate("template");
        task.setEncoding("UTF-8");
        task.setExcludeEntities("entity1");
        task.setIncludeEntities("entity2");
        task.setMode("entity");
        task.setOutputPattern("pattern");
        task.setSuperPkg("org.example.model.auto");
        task.setSuperTemplate("*.java");
        task.setTemplate("*.java");
        task.setMakePairs(true);
        task.setCreatePropertyNames(true);
        task.setOverwrite(true);
        task.setUsePkgPath(true);

        ClassGenerationAction createdAction = task.createGenerator();
        assertSame(action, createdAction);

        verify(action).setCreatePropertyNames(true);
        verify(action).setMakePairs(true);
        verify(action).setOverwrite(true);
        verify(action).setUsePkgPath(true);
        verify(action).setArtifactsGenerationMode("entity");
        verify(action).setEncoding("UTF-8");
        verify(action).setEmbeddableSuperTemplate("superTemplate");
        verify(action).setEmbeddableTemplate("template");
        verify(action).setOutputPattern("pattern");
        verify(action).setSuperPkg("org.example.model.auto");
        verify(action).setSuperTemplate("*.java");
        verify(action).setTemplate("*.java");
    }

}