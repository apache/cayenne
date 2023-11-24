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

package org.apache.cayenne;

import java.sql.Types;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.lazy.Lazyblob;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @since 4.2
 */
@UseCayenneRuntime(CayenneProjects.LAZY_ATTRIBUTES_PROJECT)
public class LazyAttributesIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Before
    public void setup() throws Exception {
        TableHelper th = new TableHelper(dbHelper, "LAZYBLOB")
                .setColumns("ID", "NAME", "LAZY_DATA")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.VARBINARY);
        th.insert(1, "test", new byte[]{1, 2, 3, 4, 5});
    }

    @Test
    public void testRead() {
        Lazyblob lazyblob = ObjectSelect.query(Lazyblob.class).selectOne(context);
        byte[] expected = {1, 2, 3, 4, 5};

        assertThat(lazyblob.readPropertyDirectly("lazyData"), instanceOf(Fault.class));
        assertArrayEquals(expected, (byte[])lazyblob.readProperty("lazyData"));
        assertArrayEquals(expected, lazyblob.getLazyData());
    }

    @Test
    public void testReadColumn() {
        byte[] lazyData = ObjectSelect.columnQuery(Lazyblob.class, Lazyblob.LAZY_DATA).selectOne(context);
        byte[] expected = {1, 2, 3, 4, 5};
        assertArrayEquals(expected, lazyData);
    }

    @Test
    public void testWrite() {
        Lazyblob lazyblob = ObjectSelect.query(Lazyblob.class).selectOne(context);
        byte[] expected = {5, 4, 3, 2, 1};

        // this cause resolve of the fault
        lazyblob.setLazyData(expected);

        context.commitChanges();

        assertThat(lazyblob.readPropertyDirectly("lazyData"), instanceOf(byte[].class));
        assertArrayEquals(expected, (byte[])lazyblob.readProperty("lazyData"));
        assertArrayEquals(expected, lazyblob.getLazyData());

        Lazyblob lazyblob2 = ObjectSelect.query(Lazyblob.class).selectOne(context);
        assertArrayEquals(expected, lazyblob2.getLazyData());
    }

    @Test
    public void testUpdateNoFetch() {
        Lazyblob lazyblob = ObjectSelect.query(Lazyblob.class).selectOne(context);
        lazyblob.setName("updated_name");

        context.commitChanges();

        Lazyblob lazyblob2 = ObjectSelect.query(Lazyblob.class).selectOne(context);
        assertEquals("updated_name", lazyblob2.getName());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, lazyblob2.getLazyData());
    }

    @Test
    public void testUpdateFetch() {
        Lazyblob lazyblob = ObjectSelect.query(Lazyblob.class).selectOne(context);
        lazyblob.setName("updated_name");
        lazyblob.getLazyData();

        context.commitChanges();

        Lazyblob lazyblob2 = ObjectSelect.query(Lazyblob.class).selectOne(context);
        assertEquals("updated_name", lazyblob2.getName());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, lazyblob2.getLazyData());
    }
}
