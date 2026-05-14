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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.testdo.inheritance.BaseEntity;
import org.apache.cayenne.testdo.inheritance.DirectToSubEntity;
import org.apache.cayenne.testdo.inheritance.RelatedEntity;
import org.apache.cayenne.testdo.inheritance.SubEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityInheritanceIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.INHERITANCE_PROJECT);

    /**
     * Test for CAY-1008: Reverse relationships may not be correctly set if inheritance is used.
     */
    @Disabled("This test fails")
    @Test
    public void cAY1008() {
        RelatedEntity related = env.context().newObject(RelatedEntity.class);

        BaseEntity base = env.context().newObject(BaseEntity.class);
        base.setToRelatedEntity(related);

        assertEquals(1, related.getBaseEntities().size());
        assertEquals(0, related.getSubEntities().size());

        SubEntity sub = env.context().newObject(SubEntity.class);
        sub.setToRelatedEntity(related);

        assertEquals(2, related.getBaseEntities().size());

        // TODO: andrus 2008/03/28 - this fails...
        assertEquals(1, related.getSubEntities().size());
    }

    /**
     * Test for CAY-1009: Bogus runtime relationships can mess up commit.
     */
    @Disabled("Test fails")
    @Test
    public void cAY1009() {
        // We should have only one relationship. DirectToSubEntity -> SubEntity.
        assertEquals(1, env.context()
                .getEntityResolver()
                .getObjEntity("DirectToSubEntity")
                .getRelationships()
                .size());

        DirectToSubEntity direct = env.context().newObject(DirectToSubEntity.class);

        SubEntity sub = env.context().newObject(SubEntity.class);
        sub.setToDirectToSubEntity(direct);

        assertEquals(1, direct.getSubEntities().size());

        env.context().deleteObject(sub);

        assertEquals(0, direct.getSubEntities().size());
    }

    @Test
    public void cAY2091() {
        RelatedEntity related = env.context().newObject(RelatedEntity.class);
        SubEntity subEntity = env.context().newObject(SubEntity.class);
        subEntity.setToRelatedEntity(related);
        env.context().commitChanges();

        int subEntityId = Cayenne.intPKForObject(subEntity);

        BaseEntity forPkLoadedEntity = Cayenne.objectForPK(env.context(), BaseEntity.class, subEntityId);
        assertEquals(forPkLoadedEntity.getClass(), SubEntity.class);

        BaseEntity selectLoadedEntity = SelectById.query(BaseEntity.class, subEntityId).selectOne(env.context());
        assertEquals(selectLoadedEntity.getClass(), SubEntity.class);
    }
}
