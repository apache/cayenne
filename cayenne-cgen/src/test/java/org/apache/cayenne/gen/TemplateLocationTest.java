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
import org.slf4j.helpers.NOPLogger;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TemplateLocationTest extends CgenCase {

    @TempDir
    public File tempFolder;

    private CgenConfiguration configuration;

    @BeforeEach
    public void setUp() {
        configuration = new CgenConfiguration();
    }

    @Test
    public void upperLevel() throws Exception {
        File subFolder = new File(tempFolder, "sub");
        subFolder.mkdir();
        configuration.setRootPath(subFolder.toPath());
        new File(tempFolder, "testTemplate.vm").createNewFile();
        configuration.setTemplate(new CgenTemplate("../testTemplate.vm", true, TemplateType.ENTITY_SUBCLASS));

        assertNotNull(createAction().getTemplate(TemplateType.ENTITY_SUBCLASS));
    }

    @Test
    public void sameLevel() throws Exception {
        configuration.setRootPath(tempFolder.toPath());
        new File(tempFolder, "testTemplate2.vm").createNewFile();
        configuration.setTemplate(new CgenTemplate("testTemplate2.vm", true, TemplateType.ENTITY_SUBCLASS));

        assertNotNull(createAction().getTemplate(TemplateType.ENTITY_SUBCLASS));
    }

    @Test
    public void aboveLevel() throws Exception {
        configuration.setRootPath(Paths.get(tempFolder.getParent()));
        new File(tempFolder, "testTemplate3.vm").createNewFile();
        configuration.setTemplate(
                new CgenTemplate(tempFolder + "/testTemplate3.vm", true, TemplateType.ENTITY_SUBCLASS));

        assertNotNull(createAction().getTemplate(TemplateType.ENTITY_SUBCLASS));
    }

    private ClassGenerationAction createAction() {
        return new ClassGenerationAction(
                configuration,
                getUnitTestInjector().getInstance(ToolsUtilsFactory.class),
                getUnitTestInjector().getInstance(MetadataUtils.class),
                NOPLogger.NOP_LOGGER);
    }
}
