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

import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.CompoundFkTestEntity;
import org.apache.cayenne.testdo.testmap.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Testing relationships with compound keys.
 */
@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextCompoundRelTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DataContext context1;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("COMPOUND_FK_TEST");
        dbHelper.deleteAll("COMPOUND_PK_TEST");
    }

    public void testInsert() {

        CompoundPkTestEntity master = context.newObject(CompoundPkTestEntity.class);
        CompoundFkTestEntity detail = context.newObject(CompoundFkTestEntity.class);
        master.addToCompoundFkArray(detail);
        master.setName("m1");
        master.setKey1("key11");
        master.setKey2("key21");
        detail.setName("d1");

        context.commitChanges();
        context.invalidateObjects(master, detail);

        SelectQuery q = new SelectQuery(CompoundPkTestEntity.class);
        List<?> objs = context1.performQuery(q);
        assertEquals(1, objs.size());

        master = (CompoundPkTestEntity) objs.get(0);
        assertEquals("m1", master.getName());

        List<?> details = master.getCompoundFkArray();
        assertEquals(1, details.size());
        detail = (CompoundFkTestEntity) details.get(0);

        assertEquals("d1", detail.getName());
    }

    public void testFetchQualifyingToOne() {
        CompoundPkTestEntity master = (CompoundPkTestEntity) context
                .newObject("CompoundPkTestEntity");
        CompoundPkTestEntity master1 = (CompoundPkTestEntity) context
                .newObject("CompoundPkTestEntity");
        CompoundFkTestEntity detail = (CompoundFkTestEntity) context
                .newObject("CompoundFkTestEntity");
        CompoundFkTestEntity detail1 = (CompoundFkTestEntity) context
                .newObject("CompoundFkTestEntity");
        master.addToCompoundFkArray(detail);
        master1.addToCompoundFkArray(detail1);

        master.setName("m1");
        master.setKey1("key11");
        master.setKey2("key21");

        master1.setName("m2");
        master1.setKey1("key12");
        master1.setKey2("key22");

        detail.setName("d1");

        detail1.setName("d2");

        context.commitChanges();
        context.invalidateObjects(master, master1, detail, detail1);

        Expression qual = ExpressionFactory.matchExp("toCompoundPk", master);
        SelectQuery q = new SelectQuery(CompoundFkTestEntity.class, qual);
        List<?> objs = context1.performQuery(q);
        assertEquals(1, objs.size());

        detail = (CompoundFkTestEntity) objs.get(0);
        assertEquals("d1", detail.getName());
    }

    public void testFetchQualifyingToMany() throws Exception {
        CompoundPkTestEntity master = (CompoundPkTestEntity) context
                .newObject("CompoundPkTestEntity");
        CompoundPkTestEntity master1 = (CompoundPkTestEntity) context
                .newObject("CompoundPkTestEntity");
        CompoundFkTestEntity detail = (CompoundFkTestEntity) context
                .newObject("CompoundFkTestEntity");
        CompoundFkTestEntity detail1 = (CompoundFkTestEntity) context
                .newObject("CompoundFkTestEntity");
        master.addToCompoundFkArray(detail);
        master1.addToCompoundFkArray(detail1);

        master.setName("m1");
        master.setKey1("key11");
        master.setKey2("key21");

        master1.setName("m2");
        master1.setKey1("key12");
        master1.setKey2("key22");

        detail.setName("d1");

        detail1.setName("d2");

        context.commitChanges();
        context.invalidateObjects(master, master1, detail, detail1);

        Expression qual = ExpressionFactory.matchExp("compoundFkArray", detail1);
        SelectQuery q = new SelectQuery(CompoundPkTestEntity.class, qual);
        List<?> objs = context1.performQuery(q);
        assertEquals(1, objs.size());

        master = (CompoundPkTestEntity) objs.get(0);
        assertEquals("m2", master.getName());
    }
}
