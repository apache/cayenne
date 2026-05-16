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

package org.apache.cayenne;

import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.testdo.relationships.MeaningfulFK;
import org.apache.cayenne.testdo.relationships.RelationshipHelper;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.validation.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

public class MeaningfulFKIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.RELATIONSHIPS_PROJECT);

    @Test
    public void validateForSave1() throws Exception {
        MeaningfulFK testObject = env.context().newObject(MeaningfulFK.class);

        ValidationResult validation = new ValidationResult();
        testObject.validateForSave(validation);
        assertTrue(
                validation.hasFailures(),
                "Must fail validation due to missing required relationship");
        assertEquals(
                1,
                validation.getFailures().size(),
                "Must fail validation due to missing required relationship");
    }

    @Test
    public void validateForSave2() throws Exception {
        MeaningfulFK testObject = env.context().newObject(MeaningfulFK.class);

        RelationshipHelper related = env.context().newObject(RelationshipHelper.class);
        testObject.setToRelationshipHelper(related);

        ValidationResult validation = new ValidationResult();
        testObject.validateForSave(validation);
        assertFalse(validation.hasFailures());
    }

    @Test
    public void testMeaningfulFKSet() {
        MeaningfulFK testObject = env.context().newObject(MeaningfulFK.class);

        RelationshipHelper related = env.context().newObject(RelationshipHelper.class);
        testObject.setToRelationshipHelper(related);

        env.context().commitChanges();

        MeaningfulFK testObject2 = SelectById.query(MeaningfulFK.class, testObject.getObjectId()).selectOne(env.context());
        assertNotEquals(0, testObject2.getRelationshipHelperID());
    }
}
