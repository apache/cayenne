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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.testdo.inheritance.BaseEntity;
import org.apache.cayenne.testdo.inheritance.DirectToSubEntity;
import org.apache.cayenne.testdo.inheritance.RelatedEntity;
import org.apache.cayenne.testdo.inheritance.SubEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.INHERITANCE_PROJECT)
public class EntityInheritanceIT extends RuntimeCase {

    @Inject
    private DataContext context;

    /**
     * Test for CAY-1008: Reverse relationships may not be correctly set if inheritance is used.
     */
    @Test
    @Ignore("This test fails")
    public void testCAY1008() {
        RelatedEntity related = context.newObject(RelatedEntity.class);

        BaseEntity base = context.newObject(BaseEntity.class);
        base.setToRelatedEntity(related);

        assertEquals(1, related.getBaseEntities().size());
        assertEquals(0, related.getSubEntities().size());

        SubEntity sub = context.newObject(SubEntity.class);
        sub.setToRelatedEntity(related);

        assertEquals(2, related.getBaseEntities().size());

        // TODO: andrus 2008/03/28 - this fails...
        assertEquals(1, related.getSubEntities().size());
    }

    /**
     * Test for CAY-1009: Bogus runtime relationships can mess up commit.
     */
    @Test
    @Ignore("Test fails")
    public void testCAY1009() {
        // We should have only one relationship. DirectToSubEntity -> SubEntity.
        assertEquals(1, context
                .getEntityResolver()
                .getObjEntity("DirectToSubEntity")
                .getRelationships()
                .size());

        DirectToSubEntity direct = context.newObject(DirectToSubEntity.class);

        SubEntity sub = context.newObject(SubEntity.class);
        sub.setToDirectToSubEntity(direct);

        assertEquals(1, direct.getSubEntities().size());

        context.deleteObject(sub);

        assertEquals(0, direct.getSubEntities().size());
    }

    @Test
    public void testCAY2091() {
        RelatedEntity related = context.newObject(RelatedEntity.class);
        SubEntity subEntity = context.newObject(SubEntity.class);
        subEntity.setToRelatedEntity(related);
        context.commitChanges();

        int subEntityId = Cayenne.intPKForObject(subEntity);

        BaseEntity forPkLoadedEntity = Cayenne.objectForPK(context, BaseEntity.class, subEntityId);
        assertEquals(forPkLoadedEntity.getClass(), SubEntity.class);

        BaseEntity selectLoadedEntity = SelectById.query(BaseEntity.class, subEntityId).selectOne(context);
        assertEquals(selectLoadedEntity.getClass(), SubEntity.class);
    }
}
