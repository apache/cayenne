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

package org.apache.cayenne.unit.jira;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.testdo.relationship.FkOfDifferentType;
import org.apache.cayenne.unit.RelationshipCase;

public class CAY_191Test extends RelationshipCase {

    public void testResolveToOneOverFKOfDifferentNumType() throws Exception {
        // this is mostly for legacy schemas, as on many dbs you won;t be able to even
        // create the FK constraint...

        deleteTestData();
        createTestData("testResolveToOneOverFKOfDifferentNumType");

        DataContext context = createDataContext();
        FkOfDifferentType root = DataObjectUtils.objectForPK(
                context,
                FkOfDifferentType.class,
                1);

        assertNotNull(root);
        assertNotNull(root.getRelationshipHelper());
        assertEquals("RH1", root.getRelationshipHelper().getName());
    }
}
