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

package org.apache.cayenne.access;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.validation.ValidationException;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class NestedDataContextValidationIT extends RuntimeCase {

    @Inject
    protected CayenneRuntime runtime;
    
    @Inject
    private DataContext context;

    @Test
    public void testValidateOnCommitToParent() {
        context.setValidatingObjectsOnCommit(true);

        ObjectContext childContext = runtime.newContext(context);
        assertTrue(
                "Child context must have inherited the validation flag from parent",
                ((DataContext) childContext).isValidatingObjectsOnCommit());

        Artist a1 = childContext.newObject(Artist.class);
        try {
            childContext.commitChangesToParent();
            fail("No validation was performed");
        }
        catch (ValidationException e) {
            // expected
        }

        assertFalse(context.hasChanges());

        a1.setArtistName("T");
        childContext.commitChangesToParent();
    }
}
