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

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.relationships_to_many_fk.ToManyFkDep;
import org.apache.cayenne.testdo.relationships_to_many_fk.ToManyFkRoot;
import org.apache.cayenne.testdo.relationships_to_many_fk.ToManyRoot2;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

// TODO: this mapping scenario is really unsupported ... this is just an attempt at partial solution
@UseCayenneRuntime(CayenneProjects.RELATIONSHIPS_TO_MANY_FK_PROJECT)
public class CDOOneToManyFKIT extends RuntimeCase {

    @Inject
    protected DataContext context;

    @Test
    public void testReadRelationship() {

        ToManyRoot2 src2 = context.newObject(ToManyRoot2.class);
        ToManyFkRoot src = context.newObject(ToManyFkRoot.class);

        // this should go away when such mapping becomes fully supported
        src.setDepId(1);
        ToManyFkDep target = context.newObject(ToManyFkDep.class);

        // this should go away when such mapping becomes fully supported
        target.setDepId(1);
        target.setRoot2(src2);

        src.addToDeps(target);
        context.commitChanges();

        context.invalidateObjects(src, target, src2);

        ToManyFkRoot src1 = (ToManyFkRoot) Cayenne.objectForPK(context, src.getObjectId());
        assertNotNull(src1.getDeps());
        assertEquals(1, src1.getDeps().size());
        // resolve HOLLOW
        assertSame(src1, src1.getDeps().get(0).getRoot());

        context.invalidateObjects(src1, src1.getDeps().get(0));

        ToManyFkDep target2 = (ToManyFkDep) Cayenne.objectForPK(context, target.getObjectId());
        assertNotNull(target2.getRoot());

        // resolve HOLLOW
        assertSame(target2, target2.getRoot().getDeps().get(0));
    }

}
