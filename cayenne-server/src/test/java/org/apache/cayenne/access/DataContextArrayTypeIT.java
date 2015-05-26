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

package org.apache.cayenne.access;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.array_type.ArrayTestEntity;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;


@UseServerRuntime(CayenneProjects.ARRAY_TYPE_PROJECT)
public class DataContextArrayTypeIT extends ServerCase {

    @Inject
    private DataContext context;

    @Test
    public void testDoubleArray() {
        ArrayTestEntity arrayTest = context.newObject(ArrayTestEntity.class);
        Double[] doubleArray = {1.0, 2.0, 3.0};

        arrayTest.setDoubleArray(doubleArray);
        context.commitChanges();

        List<ArrayTestEntity> res = context.performQuery(new SelectQuery(ArrayTestEntity.class));
        ArrayTestEntity arrayRes = res.get(0);

        assertNotNull(arrayRes);
        assertNotNull(arrayRes.getDoubleArray());
        assertArrayEquals(doubleArray, arrayRes.getDoubleArray());
    }

}