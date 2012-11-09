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

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class CayenneGeneratorMojoTest extends AbstractMojoTestCase {

    public void testCgenExecution() throws Exception {

        File pom = getTestFile("src/test/resources/cgen/project-to-test/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        CayenneGeneratorMojo myMojo = (CayenneGeneratorMojo) lookupMojo("cgen",
                pom);
        assertNotNull(myMojo);
        myMojo.execute();

        File superTestEntity = new File(
                "target/cayenneGeneratedClasses/superPkg/_TestEntity.txt");
        File testEntity = new File(
                "target/cayenneGeneratedClasses/pack/TestEntity.txt");

        File superEmbeddable = new File(
                "target/cayenneGeneratedClasses/superPkg/_Embeddable.txt");
        File embeddable = new File(
                "target/cayenneGeneratedClasses/pack/Embeddable.txt");

        File superNotIncludedEntity = new File(
                "target/cayenneGeneratedClasses/pack/_NotIncludedEntity.txt");

        File notIncludedEntity = new File(
                "target/cayenneGeneratedClasses/pack/NotIncludedEntity.txt");

        File superExcludedEntity = new File(
                "target/cayenneGeneratedClasses/pack/_TestExcludedEntity.txt");
        File excludedEntity = new File(
                "target/cayenneGeneratedClasses/pack/TestExcludedEntity.txt");

        assertTrue(superTestEntity.exists());
        assertTrue(testEntity.exists());

        assertTrue(superEmbeddable.exists());
        assertTrue(embeddable.exists());

        assertFalse(superNotIncludedEntity.exists());
        assertFalse(notIncludedEntity.exists());

        assertFalse(superExcludedEntity.exists());
        assertFalse(excludedEntity.exists());

    }
}
