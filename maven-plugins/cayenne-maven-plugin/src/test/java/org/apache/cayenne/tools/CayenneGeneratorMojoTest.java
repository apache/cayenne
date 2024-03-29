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
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.map.DataMap;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CayenneGeneratorMojoTest extends AbstractMojoTestCase {

    public static final String TEST_TEMPLATE_PATH = "src/test/resources/cgen/project-to-test/testTemplate.vm";

    public void testCgenExecution() throws Exception {

        File pom = getTestFile("src/test/resources/cgen/project-to-test/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        CayenneGeneratorMojo myMojo = (CayenneGeneratorMojo) lookupMojo("cgen", pom);
        assertNotNull(myMojo);
        myMojo.execute();

        File superTestEntity = new File("target/cayenneGeneratedClasses/superPkg/_TestEntity.txt");
        File testEntity = new File("target/cayenneGeneratedClasses/pack/TestEntity.txt");

        File superEmbeddable = new File("target/cayenneGeneratedClasses/superPkg/_Embeddable.txt");
        File embeddable = new File("target/cayenneGeneratedClasses/pack/Embeddable.txt");

        File superNotIncludedEntity = new File("target/cayenneGeneratedClasses/pack/_NotIncludedEntity.txt");

        File notIncludedEntity = new File("target/cayenneGeneratedClasses/pack/NotIncludedEntity.txt");

        File superExcludedEntity = new File("target/cayenneGeneratedClasses/pack/_TestExcludedEntity.txt");
        File excludedEntity = new File("target/cayenneGeneratedClasses/pack/TestExcludedEntity.txt");

        assertTrue(superTestEntity.exists());
        assertTrue(testEntity.exists());

        assertTrue(superEmbeddable.exists());
        assertTrue(embeddable.exists());

        assertFalse(superNotIncludedEntity.exists());
        assertFalse(notIncludedEntity.exists());

        assertFalse(superExcludedEntity.exists());
        assertFalse(excludedEntity.exists());

        String content = FileUtils.readFileToString(superTestEntity);
        assertTrue(content.contains("public static final ListProperty<TestRelEntity> ADDITIONAL_REL = PropertyFactory.createList(\"additionalRel\", TestRelEntity.class);"));
        assertTrue(content.contains("public void addToAdditionalRel(TestRelEntity obj)"));
        assertTrue(content.contains("public void removeFromAdditionalRel(TestRelEntity obj)"));
    }

    public void testTransferPluginDataToCgenConfiguration() throws Exception {
        File pom = getTestFile("src/test/resources/cgen/project-to-test/pomTransferPluginToCgen.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        CayenneGeneratorMojo myMojo = (CayenneGeneratorMojo) lookupMojo("cgen", pom);
        assertNotNull(myMojo);

        myMojo.execute();
        List<CgenConfiguration> cgenConfigurations = myMojo.buildConfigurations(new DataMap());
        for (CgenConfiguration cgenConfiguration : cgenConfigurations) {
            assertEquals("UTF-8", cgenConfiguration.getEncoding());
            assertEquals("all", cgenConfiguration.getArtifactsGenerationMode());
            assertEquals("pattern", cgenConfiguration.getOutputPattern());
            assertEquals("superPkg", cgenConfiguration.getSuperPkg());
            assertTrue(cgenConfiguration.isMakePairs());
            assertTrue(cgenConfiguration.isCreatePropertyNames());
            assertTrue(cgenConfiguration.isOverwrite());
            assertTrue(cgenConfiguration.isUsePkgPath());

            assertTrue(cgenConfiguration.getTemplate().isFile());
            assertEquals(TEST_TEMPLATE_PATH, cgenConfiguration.getTemplate().getData());
            assertEquals(TemplateType.ENTITY_SUBCLASS, cgenConfiguration.getTemplate().getType());

            assertTrue(cgenConfiguration.getSuperTemplate().isFile());
            assertEquals(TEST_TEMPLATE_PATH, cgenConfiguration.getSuperTemplate().getData());
            assertEquals(TemplateType.ENTITY_SUPERCLASS, cgenConfiguration.getSuperTemplate().getType());

            assertTrue(cgenConfiguration.getEmbeddableTemplate().isFile());
            assertEquals(TEST_TEMPLATE_PATH, cgenConfiguration.getEmbeddableTemplate().getData());
            assertEquals(TemplateType.EMBEDDABLE_SUBCLASS, cgenConfiguration.getEmbeddableTemplate().getType());

            assertTrue(cgenConfiguration.getEmbeddableSuperTemplate().isFile());
            assertEquals(TEST_TEMPLATE_PATH, cgenConfiguration.getEmbeddableSuperTemplate().getData());
            assertEquals(TemplateType.EMBEDDABLE_SUPERCLASS, cgenConfiguration.getEmbeddableSuperTemplate().getType());

            assertTrue(cgenConfiguration.getDataMapTemplate().isFile());
            assertEquals(TEST_TEMPLATE_PATH, cgenConfiguration.getDataMapTemplate().getData());
            assertEquals(TemplateType.DATAMAP_SUBCLASS, cgenConfiguration.getDataMapTemplate().getType());

            assertTrue(cgenConfiguration.getDataMapSuperTemplate().isFile());
            assertEquals(TEST_TEMPLATE_PATH, cgenConfiguration.getDataMapSuperTemplate().getData());
            assertEquals(TemplateType.DATAMAP_SUPERCLASS, cgenConfiguration.getDataMapSuperTemplate().getType());
        }
    }

    public void testCgenDataMapConfig() throws Exception {
        File pom = getTestFile("src/test/resources/cgen/project-to-test/cgen-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        CayenneGeneratorMojo myMojo = (CayenneGeneratorMojo) lookupMojo("cgen", pom);
        assertNotNull(myMojo);
        myMojo.execute();

        File testEntity = new File("target/cgenClasses/ObjEntity1.txt");
        File notIncludedDataMapEntity = new File("target/cgenClasses/TestCgenMap.txt");

        File notIncludedEntity = new File("target/cgenClasses/ObjEntity.txt");
        File notIncludedEmbeddable = new File("target/cgenClasses/Embeddable.txt");
        File notIncludedSuperDataMap = new File("target/cgenClasses/_TestCgenMap.txt");

        assertTrue(testEntity.exists());
        assertFalse(notIncludedDataMapEntity.exists());

        assertFalse(notIncludedEntity.exists());
        assertFalse(notIncludedSuperDataMap.exists());
        assertFalse(notIncludedEmbeddable.exists());
    }

    public void testCgenWithDmAndPomConfigs() throws Exception {
        File pom = getTestFile("src/test/resources/cgen/project-to-test/datamap-and-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        CayenneGeneratorMojo myMojo = (CayenneGeneratorMojo) lookupMojo("cgen", pom);
        assertNotNull(myMojo);
        myMojo.execute();

        File objEntity1 = new File("target/resultClasses/ObjEntity1.txt");
        assertTrue(objEntity1.exists());
        File embeddable = new File("target/resultClasses/Embeddable.txt");
        assertTrue(embeddable.exists());
        File dataMap = new File("target/resultClasses/TestCgen.txt");
        assertTrue(dataMap.exists());

        File objEntity = new File("target/resultClasses/ObjEntity.txt");
        assertFalse(objEntity.exists());
        File superObjEntity1 = new File("target/resultClasses/auto/_ObjEntity.txt");
        assertFalse(superObjEntity1.exists());
        File superDataMap = new File("target/resultClasses/auto/_TestCgen.txt");
        assertFalse(superDataMap.exists());
    }

    public void testDatamapModeReplace() throws Exception {
        File pom = getTestFile("src/test/resources/cgen/project-to-test/replaceDatamapMode-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        CayenneGeneratorMojo myMojo = (CayenneGeneratorMojo) lookupMojo("cgen", pom);
        assertNotNull(myMojo);
        myMojo.execute();

        File objEntity1 = new File("target/testForMode/ObjEntity1.txt");
        assertFalse(objEntity1.exists());
        File embeddable = new File("target/testForMode/Embeddable.txt");
        assertFalse(embeddable.exists());
        File objEntity = new File("target/testForMode/ObjEntity.txt");
        assertFalse(objEntity.exists());
        File dataMap = new File("target/testForMode/TestCgen.txt");
        assertTrue(dataMap.exists());

        File superDataMap = new File("target/testForMode/superPkg/_TestCgen.txt");
        assertTrue(superDataMap.exists());
    }
}
