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
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.apache.cayenne.validation.ValidationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NestedDataContextValidationIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void validateOnCommitToParent() {
        env.context().setValidatingObjectsOnCommit(true);

        ObjectContext childContext = env.runtime().newContext(env.context());
        assertTrue(
                ((DataContext) childContext).isValidatingObjectsOnCommit(),
                "Child context must have inherited the validation flag from parent");

        Artist a1 = childContext.newObject(Artist.class);
        assertThrows(ValidationException.class, childContext::commitChangesToParent);

        assertFalse(env.context().hasChanges());

        a1.setArtistName("T");
        childContext.commitChangesToParent();
    }
}
