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

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inheritance_vertical.IvImpl;
import org.apache.cayenne.testdo.inheritance_vertical.IvOther;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 4.1
 */
@UseCayenneRuntime(CayenneProjects.INHERITANCE_VERTICAL_PROJECT)
public class VerticalInheritanceMultipleAttributesIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected CayenneRuntime runtime;

    TableHelper ivOtherTable, ivBaseTable, ivImplTable;

    @Before
    public void setupTableHelpers() throws Exception {
        ivOtherTable = new TableHelper(dbHelper, "IV_OTHER");
        ivOtherTable.setColumns("ID", "NAME")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR);

        ivBaseTable = new TableHelper(dbHelper, "IV_BASE");
        ivBaseTable.setColumns("ID", "NAME", "TYPE")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);

        ivImplTable = new TableHelper(dbHelper, "IV_IMPL");
        ivImplTable.setColumns("ID", "ATTR1", "ATTR2", "OTHER1_ID", "OTHER2_ID")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER);

        ivImplTable.deleteAll();
        ivBaseTable.deleteAll();
        ivOtherTable.deleteAll();
    }

    /**
     * @link https://issues.apache.org/jira/browse/CAY-2282
     */
    @Test
    public void testUpdateTwoObjects() throws SQLException {
        // Insert records we want to update
        ivOtherTable.insert(1, "other1");
        ivOtherTable.insert(2, "other2");

        ivBaseTable.insert(1, "Impl 1", "I");
        ivBaseTable.insert(2, "Impl 2", "I");

        ivImplTable.insert(1, "attr1", "attr2", 1, 2);
        ivImplTable.insert(2, "attr1", "attr2", 1, 2);

        // Fetch and update the records
        IvOther other1 = ObjectSelect.query(IvOther.class).where(IvOther.NAME.eq("other1")).selectOne(context);
        IvOther other2 = ObjectSelect.query(IvOther.class).where(IvOther.NAME.eq("other2")).selectOne(context);

        List<IvImpl> implResult = ObjectSelect.query(IvImpl.class).select(context);
        assertEquals(2, implResult.size());
        for(IvImpl record : implResult) {
            record.setName(record.getName() + "-Change");
            record.setAttr1(record.getAttr1() + "-Change");
            record.setAttr2(record.getAttr2() + "-Change");
            record.setOther1(other2);
            record.setOther2(other1);
        }

        context.commitChanges();

        // Check result via clean context
        ObjectContext cleanContext = runtime.newContext();
        implResult = ObjectSelect.query(IvImpl.class).select(cleanContext);
        assertEquals(2, implResult.size());
        for(IvImpl record : implResult) {
            assertTrue(record.getName().endsWith("-Change"));
            assertTrue(record.getAttr1().endsWith("-Change"));
            assertTrue(record.getAttr2().endsWith("-Change"));
            assertEquals(other2.getObjectId(), record.getOther1().getObjectId());
            assertEquals(other1.getObjectId(), record.getOther2().getObjectId());
        }
    }

    @Test
    public void testCreateObjectsWithData() throws SQLException {
        ivOtherTable.insert(1, "other1");
        ivOtherTable.insert(2, "other2");

        IvOther other1 = ObjectSelect.query(IvOther.class).where(IvOther.NAME.eq("other1")).selectOne(context);
        IvOther other2 = ObjectSelect.query(IvOther.class).where(IvOther.NAME.eq("other2")).selectOne(context);

        IvImpl impl1 = context.newObject(IvImpl.class);
        impl1.setName("name");
        impl1.setAttr1("attr1");
        impl1.setAttr2("attr2");
        impl1.setOther1(other1);
        impl1.setOther2(other2);

        IvImpl impl2 = context.newObject(IvImpl.class);
        impl2.setName("name");
        impl2.setAttr1("attr1");
        impl2.setAttr2("attr2");
        impl2.setOther1(other1);
        impl2.setOther2(other2);

        context.commitChanges();

        // Check result via clean context
        ObjectContext cleanContext = runtime.newContext();
        List<IvImpl> implResult = ObjectSelect.query(IvImpl.class).select(cleanContext);
        assertEquals(2, implResult.size());
        for(IvImpl record : implResult) {
            assertEquals("name", record.getName());
            assertEquals("attr1", record.getAttr1());
            assertEquals("attr2", record.getAttr2());
            assertEquals(other1.getObjectId(), record.getOther1().getObjectId());
            assertEquals(other2.getObjectId(), record.getOther2().getObjectId());
        }
    }

    @Test
    public void testCreateEmptyObjects() throws SQLException {
        IvImpl impl1 = context.newObject(IvImpl.class);
        impl1.setName("name");

        IvImpl impl2 = context.newObject(IvImpl.class);
        impl2.setName("name");

        context.commitChanges();

        ObjectContext cleanContext = runtime.newContext();
        List<IvImpl> implResult = ObjectSelect.query(IvImpl.class).select(cleanContext);
        assertEquals(2, implResult.size());
        for(IvImpl record : implResult) {
            assertEquals("name", record.getName());
            assertNull(record.getAttr1());
            assertNull(record.getAttr2());
            assertNull(record.getOther1());
            assertNull(record.getOther2());
        }
    }

    @Test
    public void testCreateEmptyObjectsWithUpdate() throws SQLException {
        ivOtherTable.insert(1, "other1");
        ivOtherTable.insert(2, "other2");

        IvOther other1 = ObjectSelect.query(IvOther.class).where(IvOther.NAME.eq("other1")).selectOne(context);
        IvOther other2 = ObjectSelect.query(IvOther.class).where(IvOther.NAME.eq("other2")).selectOne(context);

        IvImpl impl1 = context.newObject(IvImpl.class);
        impl1.setName("name");

        IvImpl impl2 = context.newObject(IvImpl.class);
        impl2.setName("name");

        context.commitChanges();

        ObjectContext cleanContext = runtime.newContext();
        List<IvImpl> implResult = ObjectSelect.query(IvImpl.class).select(cleanContext);
        assertEquals(2, implResult.size());
        for(IvImpl record : implResult) {
            assertEquals("name", record.getName());
            assertNull(record.getAttr1());
            assertNull(record.getAttr2());
            assertNull(record.getOther1());
            assertNull(record.getOther2());
        }

        impl1.setAttr1("attr1");
        impl1.setAttr2("attr2");
        impl1.setOther1(other1);
        impl1.setOther2(other2);

        impl2.setAttr1("attr1");
        impl2.setAttr2("attr2");
        impl2.setOther1(other1);
        impl2.setOther2(other2);

        context.commitChanges();

        cleanContext = runtime.newContext();
        implResult = ObjectSelect.query(IvImpl.class).select(cleanContext);
        assertEquals(2, implResult.size());
        for(IvImpl record : implResult) {
            assertEquals("name", record.getName());
            assertEquals("attr1", record.getAttr1());
            assertEquals("attr2", record.getAttr2());
            assertEquals(other1.getObjectId(), record.getOther1().getObjectId());
            assertEquals(other2.getObjectId(), record.getOther2().getObjectId());
        }
    }

    @Test
    public void testPartialCreateObjectsWithUpdate() throws SQLException {
        ivOtherTable.insert(1, "other1");
        ivOtherTable.insert(2, "other2");

        IvOther other1 = ObjectSelect.query(IvOther.class).where(IvOther.NAME.eq("other1")).selectOne(context);
        IvOther other2 = ObjectSelect.query(IvOther.class).where(IvOther.NAME.eq("other2")).selectOne(context);

        IvImpl impl1 = context.newObject(IvImpl.class);
        impl1.setName("name");
        impl1.setAttr1("attr1");

        IvImpl impl2 = context.newObject(IvImpl.class);
        impl2.setName("name");
        impl2.setAttr1("attr1");

        context.commitChanges();

        ObjectContext cleanContext = runtime.newContext();
        List<IvImpl> implResult = ObjectSelect.query(IvImpl.class).select(cleanContext);
        assertEquals(2, implResult.size());
        for(IvImpl record : implResult) {
            assertEquals("name", record.getName());
            assertEquals("attr1", record.getAttr1());
            assertNull(record.getAttr2());
            assertNull(record.getOther1());
            assertNull(record.getOther2());
        }

        impl1.setAttr1("attr1");
        impl1.setAttr2("attr2");
        impl1.setOther1(other1);
        impl1.setOther2(other2);

        impl2.setAttr1("attr1");
        impl2.setAttr2("attr2");
        impl2.setOther1(other1);
        impl2.setOther2(other2);

        context.commitChanges();

        cleanContext = runtime.newContext();
        implResult = ObjectSelect.query(IvImpl.class).select(cleanContext);
        assertEquals(2, implResult.size());
        for(IvImpl record : implResult) {
            assertEquals("name", record.getName());
            assertEquals("attr1", record.getAttr1());
            assertEquals("attr2", record.getAttr2());
            assertEquals(other1.getObjectId(), record.getOther1().getObjectId());
            assertEquals(other2.getObjectId(), record.getOther2().getObjectId());
        }
    }

    @Test
    public void testDeleteObjects() throws SQLException {
        // Insert records we want to update
        ivOtherTable.insert(1, "other1");
        ivOtherTable.insert(2, "other2");

        ivBaseTable.insert(1, "Impl 1", "I");
        ivBaseTable.insert(2, "Impl 2", "I");

        ivImplTable.insert(1, "attr1", "attr2", 1, 2);
        ivImplTable.insert(2, "attr1", "attr2", 1, 2);

        List<IvImpl> implResult = ObjectSelect.query(IvImpl.class).select(context);
        assertEquals(2, implResult.size());

        for(IvImpl iv : implResult) {
            context.deleteObject(iv);
        }

        context.commitChanges();

        assertEquals(0L, ObjectSelect.query(IvImpl.class).selectCount(context));
    }
}
