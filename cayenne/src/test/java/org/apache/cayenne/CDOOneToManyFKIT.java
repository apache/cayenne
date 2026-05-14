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

import org.apache.cayenne.testdo.relationships_to_many_fk.ToManyFkDep;
import org.apache.cayenne.testdo.relationships_to_many_fk.ToManyFkRoot;
import org.apache.cayenne.testdo.relationships_to_many_fk.ToManyRoot2;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

// TODO: this mapping scenario is really unsupported ... this is just an attempt at partial solution
public class CDOOneToManyFKIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.RELATIONSHIPS_TO_MANY_FK_PROJECT);

    @Test
    public void readRelationship() {

        ToManyRoot2 src2 = env.context().newObject(ToManyRoot2.class);
        ToManyFkRoot src = env.context().newObject(ToManyFkRoot.class);

        // this should go away when such mapping becomes fully supported
        src.setDepId(1);
        ToManyFkDep target = env.context().newObject(ToManyFkDep.class);

        // this should go away when such mapping becomes fully supported
        target.setDepId(1);
        target.setRoot2(src2);

        src.addToDeps(target);
        env.context().commitChanges();

        env.context().invalidateObjects(src, target, src2);

        ToManyFkRoot src1 = (ToManyFkRoot) Cayenne.objectForPK(env.context(), src.getObjectId());
        assertNotNull(src1.getDeps());
        assertEquals(1, src1.getDeps().size());
        // resolve HOLLOW
        assertSame(src1, src1.getDeps().get(0).getRoot());

        env.context().invalidateObjects(src1, src1.getDeps().get(0));

        ToManyFkDep target2 = (ToManyFkDep) Cayenne.objectForPK(env.context(), target.getObjectId());
        assertNotNull(target2.getRoot());

        // resolve HOLLOW
        assertSame(target2, target2.getRoot().getDeps().get(0));
    }

}
