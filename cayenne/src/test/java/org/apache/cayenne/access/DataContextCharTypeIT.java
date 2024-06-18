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

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.return_types.ReturnTypesMap1;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.RETURN_TYPES_PROJECT)
public class DataContextCharTypeIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    private UnitDbAdapter unitDbAdapter;

    @Test
    public void testCharTrimming() {
        if (unitDbAdapter.supportsLobs()) {
            ReturnTypesMap1 map1 = context.newObject(ReturnTypesMap1.class);
            map1.setCharColumn("  text   ");
            ReturnTypesMap1 map2 = context.newObject(ReturnTypesMap1.class);
            map2.setCharColumn("  text");
            ReturnTypesMap1 map3 = context.newObject(ReturnTypesMap1.class);
            map3.setCharColumn("text     ");

            context.commitChanges();

            List<ReturnTypesMap1> result =  ObjectSelect.query(ReturnTypesMap1.class)
                    .where(ReturnTypesMap1.CHAR_COLUMN.eq("  text"))
                    .select(context);

            assertTrue("CHAR type trimming is not valid.", result.get(0).getCharColumn().startsWith("  text"));
            assertTrue("CHAR type trimming is not valid.", result.get(1).getCharColumn().startsWith("  text"));

            result =  ObjectSelect.query(ReturnTypesMap1.class)
                    .where(ReturnTypesMap1.CHAR_COLUMN.eq("text"))
                    .select(context);

            assertTrue("CHAR type trimming is not valid.", result.get(0).getCharColumn().startsWith("text"));
        }
    }
}
