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
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.reflexive.Other;
import org.apache.cayenne.testdo.reflexive.Reflexive;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.REFLEXIVE_PROJECT)
public class CayennePersistentObjectReflexiveIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private CayenneRuntime runtime;

    @Test
    public void addReflexiveParentAndChild() {
        // can add Reflexive Parent and Child, 100 times
        final int attempts = 100;
        int errors = 0;

        for (int i = 0; i < attempts; i++) {
            final Reflexive parent = context.newObject(Reflexive.class);
            parent.setName("parentA"+i);

            // and child is created and associated to "Parent"
            final Reflexive child = context.newObject(Reflexive.class);
            child.setName("childA"+i);
            child.setToParent(parent);

            try {
                context.commitChanges();

                // unset parent so that DBCleaner.clean() will work correctly
                child.setToParent(null);
                context.commitChanges();
            } catch (final Exception e) {
                errors++;
                e.printStackTrace();
                context.rollbackChanges();
            }
        }

        // then no error occurred
        assertEquals(String.format("Failed on %s of %s attempts.", errors, attempts), 0, errors);
    }

    @Test
    public void addReflexiveParentAndChildWithOtherRelationshipOnParent() {
        // can add Reflexive Parent (that belongsTo Other) and Child,
        // we will do this 100 times, because it randomly does it correctly/incorrectly

        // given some "other" Object
        final Other other = context.newObject(Other.class);
        other.setName("OtherB");
        context.commitChanges();

        final int attempts = 100;
        int errors = 0;

        for (int i = 0; i < attempts; i++) {
            // when parent is created and associated to "Other"

            final Reflexive parent = context.newObject(Reflexive.class);
            parent.setName("parentB"+i);
            parent.setToOther(other);

            // and child is created and associated to "Parent"
            final Reflexive child = context.newObject(Reflexive.class);
            child.setName("childB"+i);
            child.setToParent(parent);

            try {
                context.commitChanges();

                // unset parent so that DBCleaner.clean() will work correctly
                child.setToParent(null);
                context.commitChanges();
            } catch (final Exception e) {
                errors++;
                e.printStackTrace();
                context.rollbackChanges();
            }
        }

        // then no error occurred
        assertEquals(String.format("Failed on %s of %s attempts.", errors, attempts), 0, errors);
    }

}
