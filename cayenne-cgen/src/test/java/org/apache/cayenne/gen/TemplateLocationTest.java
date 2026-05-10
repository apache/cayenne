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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TemplateLocationTest {

    @TempDir
    public File tempFolder;

    private CgenConfiguration cgenConfiguration;
    private ClassGenerationAction action;
    private TemplateType templateType;

    @BeforeEach
    public void setUp() {
        cgenConfiguration = new CgenConfiguration();
        action = new ClassGenerationAction(cgenConfiguration);
        templateType = TemplateType.ENTITY_SUBCLASS;
    }

    @Test
    public void upperLevel() throws Exception {
        File subFolder = new File(tempFolder, "sub");
        subFolder.mkdir();
        cgenConfiguration.setRootPath(subFolder.toPath());
        new File(tempFolder, "testTemplate.vm").createNewFile();
        cgenConfiguration.setTemplate(new CgenTemplate("../testTemplate.vm", false, TemplateType.ENTITY_SUBCLASS));
        assertNotNull(action.getTemplate(templateType));
    }

    @Test
    public void sameLevel() throws Exception {
        cgenConfiguration.setRootPath(tempFolder.toPath());
        new File(tempFolder, "testTemplate2.vm").createNewFile();
        cgenConfiguration.setTemplate(new CgenTemplate("testTemplate2.vm", false, TemplateType.ENTITY_SUBCLASS));
        assertNotNull(action.getTemplate(templateType));
    }

    @Test
    public void aboveLevel() throws Exception {
        cgenConfiguration.setRootPath(Paths.get(tempFolder.getParent()));
        new File(tempFolder, "testTemplate3.vm").createNewFile();
        cgenConfiguration.setTemplate(new CgenTemplate(tempFolder + "/testTemplate3.vm", false, TemplateType.ENTITY_SUBCLASS));
        assertNotNull(action.getTemplate(templateType));
    }
}
