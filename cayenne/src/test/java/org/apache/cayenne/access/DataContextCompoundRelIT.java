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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.compound.CompoundFkTestEntity;
import org.apache.cayenne.testdo.compound.CompoundOrder;
import org.apache.cayenne.testdo.compound.CompoundOrderLine;
import org.apache.cayenne.testdo.compound.CompoundOrderLineInfo;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Testing relationships with compound keys.
 */
@UseCayenneRuntime(CayenneProjects.COMPOUND_PROJECT)
public class DataContextCompoundRelIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private DataContext context1;

    @Test
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

        List<CompoundPkTestEntity> objs = ObjectSelect.query(CompoundPkTestEntity.class).select(context1);

        assertEquals(1, objs.size());
        assertEquals("m1", objs.get(0).getName());

        List<CompoundFkTestEntity> details = objs.get(0).getCompoundFkArray();
        assertEquals(1, details.size());
        assertEquals("d1", details.get(0).getName());
    }

    @Test
    public void testFetchQualifyingToOne() {
        CompoundPkTestEntity master  = context.newObject(CompoundPkTestEntity.class);
        CompoundPkTestEntity master1 = context.newObject(CompoundPkTestEntity.class);
        CompoundFkTestEntity detail  = context.newObject(CompoundFkTestEntity.class);
        CompoundFkTestEntity detail1 = context.newObject(CompoundFkTestEntity.class);

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

        List<CompoundFkTestEntity> objs = ObjectSelect.query(CompoundFkTestEntity.class)
                .where(CompoundFkTestEntity.TO_COMPOUND_PK.eq(master))
                .select(context1);

        assertEquals(1, objs.size());
        assertEquals("d1", objs.get(0).getName());
    }

    @Test
    public void testToOne() {
        {
            CompoundOrder order = context.newObject(CompoundOrder.class);
            order.setInfo("order");

            CompoundOrderLine line = context.newObject(CompoundOrderLine.class);
            line.setOrder(order);
            line.setLineNumber(1);

            CompoundOrderLineInfo info = context.newObject(CompoundOrderLineInfo.class);
            info.setLine(line);
            info.setInfo("info");

            context.commitChanges();
            context.invalidateObjects(order, line, info);
        }

        {
            CompoundOrder order = ObjectSelect.query(CompoundOrder.class).selectFirst(context1);
            CompoundOrderLine line = order.getLines().get(0);
            CompoundOrderLineInfo info = line.getInfo();

            assertEquals(1, line.getLineNumber());
            assertEquals("info", info.getInfo());
        }
    }

    @Test
    public void testFetchQualifyingToMany() {
        CompoundPkTestEntity master  = context.newObject(CompoundPkTestEntity.class);
        CompoundPkTestEntity master1 = context.newObject(CompoundPkTestEntity.class);
        CompoundFkTestEntity detail  = context.newObject(CompoundFkTestEntity.class);
        CompoundFkTestEntity detail1 = context.newObject(CompoundFkTestEntity.class);

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

        List<CompoundPkTestEntity> objs = ObjectSelect.query(CompoundPkTestEntity.class)
                .where(CompoundPkTestEntity.COMPOUND_FK_ARRAY.contains(detail1))
                .select(context1);

        assertEquals(1, objs.size());
        assertEquals("m2", objs.get(0).getName());
    }
}
