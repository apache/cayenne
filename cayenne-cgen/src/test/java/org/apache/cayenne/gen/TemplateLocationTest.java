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

package org.apache.cayenne.gen;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

public class TemplateLocationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private CgenConfiguration cgenConfiguration;
    private ClassGenerationAction action;
    private TemplateType templateType;

    @Before
    public void setUp() {
        cgenConfiguration = new CgenConfiguration();
        action = new ClassGenerationAction(cgenConfiguration);
        templateType = TemplateType.ENTITY_SUBCLASS;
    }

    @Test
    public void upperLevel() throws Exception {
        cgenConfiguration.setRootPath(tempFolder.newFolder().toPath());
        tempFolder.newFile("testTemplate.vm");
        cgenConfiguration.setTemplate("../testTemplate.vm");
        assertNotNull(action.getTemplate(templateType));
    }

    @Test
    public void sameLevel() throws Exception {
        cgenConfiguration.setRootPath(tempFolder.getRoot().toPath());
        tempFolder.newFile("testTemplate2.vm");
        cgenConfiguration.setTemplate("testTemplate2.vm");
        assertNotNull(action.getTemplate(templateType));
    }

    @Test
    public void aboveLevel() throws Exception {
        cgenConfiguration.setRootPath(Paths.get(tempFolder.getRoot().getParent()));
        tempFolder.newFile("testTemplate3.vm");
        cgenConfiguration.setTemplate(tempFolder.getRoot() + "/testTemplate3.vm");
        assertNotNull(action.getTemplate(templateType));
    }
}
