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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.testdo.relationships.MeaningfulFK;
import org.apache.cayenne.testdo.relationships.RelationshipHelper;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.validation.ValidationResult;
import org.junit.Test;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.RELATIONSHIPS_PROJECT)
public class MeaningfulFKIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Test
    public void testValidateForSave1() throws Exception {
        MeaningfulFK testObject = context.newObject(MeaningfulFK.class);

        ValidationResult validation = new ValidationResult();
        testObject.validateForSave(validation);
        assertTrue(
                "Must fail validation due to missing required relationship",
                validation.hasFailures());
        assertEquals(
                "Must fail validation due to missing required relationship",
                1,
                validation.getFailures().size());
    }

    @Test
    public void testValidateForSave2() throws Exception {
        MeaningfulFK testObject = context.newObject(MeaningfulFK.class);

        RelationshipHelper related = context.newObject(RelationshipHelper.class);
        testObject.setToRelationshipHelper(related);

        ValidationResult validation = new ValidationResult();
        testObject.validateForSave(validation);
        assertFalse(validation.hasFailures());
    }

    @Test
    public void testMeaningfulFKSet() {
        MeaningfulFK testObject = context.newObject(MeaningfulFK.class);

        RelationshipHelper related = context.newObject(RelationshipHelper.class);
        testObject.setToRelationshipHelper(related);

        context.commitChanges();

        MeaningfulFK testObject2 = SelectById.query(MeaningfulFK.class, testObject.getObjectId()).selectOne(context);
        assertNotEquals(0, testObject2.getRelationshipHelperID());
    }
}
