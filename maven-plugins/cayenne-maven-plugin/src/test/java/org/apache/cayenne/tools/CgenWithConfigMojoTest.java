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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;

/**
 * @since 4.1
 */
public class CgenWithConfigMojoTest extends AbstractMojoTestCase {

    public void testCgen() throws Exception {
        File pom = getTestFile("src/test/resources/cgen/project-to-test/cgen-pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        CayenneGeneratorMojo myMojo = (CayenneGeneratorMojo) lookupMojo("cgen", pom);
        assertNotNull(myMojo);
        myMojo.execute();

        File testEntity = new File("target/cgenClasses/ObjEntity1.txt");
        File notIncludedDataMapEntity = new File("target/cgenClasses/TestCgenMap.txt");

        File notIncludedEntity = new File("target/cgenClasses/ObjEntity.txt");
        File notIncludedSuperDataMap = new File("target/cgenClasses/_TestCgenMap.txt");

        assertTrue(testEntity.exists());
        assertFalse(notIncludedDataMapEntity.exists());

        assertFalse(notIncludedEntity.exists());
        assertFalse(notIncludedSuperDataMap.exists());
    }
}
