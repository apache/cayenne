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

import org.apache.art.CompoundFkTestEntity;
import org.apache.art.CompoundPkTestEntity;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Testing relationships with compound keys.
 * 
 */
public class DataContextCompoundRelTest extends CayenneCase {

    protected DataContext ctxt;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        ctxt = createDataContext();
    }

    public void testInsert() {
        CompoundPkTestEntity master = ctxt
                .newObject(CompoundPkTestEntity.class);
        CompoundFkTestEntity detail = ctxt
                .newObject(CompoundFkTestEntity.class);
        master.addToCompoundFkArray(detail);
        master.setName("m1");
        master.setKey1("key11");
        master.setKey2("key21");
        detail.setName("d1");

        ctxt.commitChanges();

        // reset context
        ctxt = createDataContext();

        SelectQuery q = new SelectQuery(CompoundPkTestEntity.class);
        List<?> objs = ctxt.performQuery(q);
        assertEquals(1, objs.size());

        master = (CompoundPkTestEntity) objs.get(0);
        assertEquals("m1", master.getName());

        List<?> details = master.getCompoundFkArray();
        assertEquals(1, details.size());
        detail = (CompoundFkTestEntity) details.get(0);

        assertEquals("d1", detail.getName());
    }

    public void testFetchQualifyingToOne() {
        CompoundPkTestEntity master = (CompoundPkTestEntity) ctxt
                .newObject("CompoundPkTestEntity");
        CompoundPkTestEntity master1 = (CompoundPkTestEntity) ctxt
                .newObject("CompoundPkTestEntity");
        CompoundFkTestEntity detail = (CompoundFkTestEntity) ctxt
                .newObject("CompoundFkTestEntity");
        CompoundFkTestEntity detail1 = (CompoundFkTestEntity) ctxt
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

        ctxt.commitChanges();

        // reset context
        ctxt = createDataContext();

        Expression qual = ExpressionFactory.matchExp("toCompoundPk", master);
        SelectQuery q = new SelectQuery(CompoundFkTestEntity.class, qual);
        List<?> objs = ctxt.performQuery(q);
        assertEquals(1, objs.size());

        detail = (CompoundFkTestEntity) objs.get(0);
        assertEquals("d1", detail.getName());
    }

    public void testFetchQualifyingToMany() throws Exception {
        CompoundPkTestEntity master = (CompoundPkTestEntity) ctxt
                .newObject("CompoundPkTestEntity");
        CompoundPkTestEntity master1 = (CompoundPkTestEntity) ctxt
                .newObject("CompoundPkTestEntity");
        CompoundFkTestEntity detail = (CompoundFkTestEntity) ctxt
                .newObject("CompoundFkTestEntity");
        CompoundFkTestEntity detail1 = (CompoundFkTestEntity) ctxt
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

        ctxt.commitChanges();

        // reset context
        ctxt = createDataContext();

        Expression qual = ExpressionFactory.matchExp("compoundFkArray", detail1);
        SelectQuery q = new SelectQuery(CompoundPkTestEntity.class, qual);
        List<?> objs = ctxt.performQuery(q);
        assertEquals(1, objs.size());

        master = (CompoundPkTestEntity) objs.get(0);
        assertEquals("m2", master.getName());
    }
}
