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
package org.apache.cayenne.lifecycle.audit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.lifecycle.changeset.ChangeSetFilter;
import org.apache.cayenne.lifecycle.db.Auditable1;
import org.apache.cayenne.lifecycle.db.Auditable2;
import org.apache.cayenne.lifecycle.db.AuditableChild1;
import org.apache.cayenne.lifecycle.db.AuditableChild2;
import org.apache.cayenne.lifecycle.db.AuditableChild3;
import org.apache.cayenne.lifecycle.db.AuditableChildUuid;
import org.apache.cayenne.lifecycle.id.IdCoder;
import org.apache.cayenne.lifecycle.relationship.ObjectIdRelationshipHandler;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;

public class AuditableFilter_InRuntime_Test extends TestCase {

    private ServerRuntime runtime;

    private TableHelper auditable1;
    private TableHelper auditableChild1;
    private TableHelper auditableChild2;

    private TableHelper auditable2;
    private TableHelper auditableChild3;
    private TableHelper auditableChildUuid;

    @Override
    protected void setUp() throws Exception {
        runtime = new ServerRuntime("cayenne-lifecycle.xml");

        DBHelper dbHelper = new DBHelper(runtime.getDataSource(null));

        auditable1 = new TableHelper(dbHelper, "AUDITABLE1").setColumns("ID", "CHAR_PROPERTY1");

        auditableChild1 = new TableHelper(dbHelper, "AUDITABLE_CHILD1").setColumns("ID", "AUDITABLE1_ID",
                "CHAR_PROPERTY1");

        auditableChild2 = new TableHelper(dbHelper, "AUDITABLE_CHILD2").setColumns("ID", "AUDITABLE1_ID",
                "CHAR_PROPERTY1");

        auditable2 = new TableHelper(dbHelper, "AUDITABLE2").setColumns("ID", "CHAR_PROPERTY1", "CHAR_PROPERTY2");

        auditableChild3 = new TableHelper(dbHelper, "AUDITABLE_CHILD3").setColumns("ID", "AUDITABLE2_ID",
                "CHAR_PROPERTY1", "CHAR_PROPERTY2");

        auditableChildUuid = new TableHelper(dbHelper, "AUDITABLE_CHILD_UUID").setColumns("ID", "UUID",
                "CHAR_PROPERTY1", "CHAR_PROPERTY2");

        auditableChild1.deleteAll();
        auditableChild2.deleteAll();
        auditable1.deleteAll();

        auditableChild3.deleteAll();
        auditable2.deleteAll();

        auditableChildUuid.deleteAll();
    }

    public void testAudit_IgnoreRuntimeRelationships() throws Exception {

        auditable1.insert(1, "xx");
        auditable1.insert(2, "yy");
        auditable1.insert(3, "aa");
        auditableChild2.insert(1, 1, "zz");

        DataDomain domain = runtime.getDataDomain();

        Processor processor = new Processor();

        AuditableFilter filter = new AuditableFilter(domain.getEntityResolver(), processor);
        domain.addFilter(filter);

        // prerequisite for BaseAuditableProcessor use
        ChangeSetFilter changeSetFilter = new ChangeSetFilter();
        domain.addFilter(changeSetFilter);

        ObjectContext context = runtime.newContext();

        Auditable1 a2 = Cayenne.objectForPK(context, Auditable1.class, 2);
        AuditableChild2 a21 = Cayenne.objectForPK(context, AuditableChild2.class, 1);

        a21.setParent(a2);
        a21.setCharProperty1("XYZA");
        context.commitChanges();

        assertEquals(0, processor.size);

        processor.reset();

        Auditable1 a3 = Cayenne.objectForPK(context, Auditable1.class, 3);
        a21.setParent(a3);
        a3.setCharProperty1("12");

        context.commitChanges();
        assertEquals(1, processor.size);
        assertTrue(processor.audited.get(AuditableOperation.UPDATE).contains(a3));
    }

    public void testAudit_IncludeToManyRelationships() throws Exception {

        auditable1.insert(1, "xx");
        auditable1.insert(2, "yy");
        auditableChild1.insert(1, 1, "zz");

        DataDomain domain = runtime.getDataDomain();

        Processor processor = new Processor();

        AuditableFilter filter = new AuditableFilter(domain.getEntityResolver(), processor);
        domain.addFilter(filter);

        // prerequisite for BaseAuditableProcessor use
        ChangeSetFilter changeSetFilter = new ChangeSetFilter();
        domain.addFilter(changeSetFilter);

        ObjectContext context = runtime.newContext();

        Auditable1 a2 = Cayenne.objectForPK(context, Auditable1.class, 2);
        AuditableChild1 a21 = Cayenne.objectForPK(context, AuditableChild1.class, 1);

        a21.setParent(a2);
        context.commitChanges();

        assertEquals(2, processor.size);

        assertTrue(processor.audited.get(AuditableOperation.UPDATE).contains(a2));
        assertTrue(processor.audited.get(AuditableOperation.UPDATE).contains(
                Cayenne.objectForPK(context, Auditable1.class, 1)));
    }

    public void testAudit_IgnoreProperties() throws Exception {

        auditable2.insert(1, "P1_1", "P2_1");
        auditable2.insert(2, "P1_2", "P2_2");
        auditable2.insert(3, "P1_3", "P2_3");

        DataDomain domain = runtime.getDataDomain();

        Processor processor = new Processor();

        AuditableFilter filter = new AuditableFilter(domain.getEntityResolver(), processor);
        domain.addFilter(filter);

        // prerequisite for BaseAuditableProcessor use
        ChangeSetFilter changeSetFilter = new ChangeSetFilter();
        domain.addFilter(changeSetFilter);

        ObjectContext context = runtime.newContext();

        Auditable2 a1 = Cayenne.objectForPK(context, Auditable2.class, 1);
        Auditable2 a2 = Cayenne.objectForPK(context, Auditable2.class, 2);
        Auditable2 a3 = Cayenne.objectForPK(context, Auditable2.class, 3);

        a1.setCharProperty1("__");
        a2.setCharProperty2("__");
        a3.setCharProperty1("__");
        a3.setCharProperty2("__");

        context.commitChanges();

        assertEquals(2, processor.size);
        assertTrue(processor.audited.get(AuditableOperation.UPDATE).contains(a2));
        assertTrue(processor.audited.get(AuditableOperation.UPDATE).contains(a3));
    }

    public void testAuditableChild_IgnoreProperties() throws Exception {

        auditable2.insert(1, "P1_1", "P2_1");
        auditable2.insert(2, "P1_2", "P2_2");
        auditableChild3.insert(1, 1, "C", "D");

        DataDomain domain = runtime.getDataDomain();

        Processor processor = new Processor();

        AuditableFilter filter = new AuditableFilter(domain.getEntityResolver(), processor);
        domain.addFilter(filter);

        // prerequisite for BaseAuditableProcessor use
        ChangeSetFilter changeSetFilter = new ChangeSetFilter();
        domain.addFilter(changeSetFilter);

        ObjectContext context = runtime.newContext();

        AuditableChild3 ac1 = Cayenne.objectForPK(context, AuditableChild3.class, 1);

        // a change to ignored property should not cause an audit event
        ac1.setCharProperty1("X_X");

        context.commitChanges();
        assertEquals(0, processor.size);

        processor.reset();
        ac1.setCharProperty2("XXXXX");
        context.commitChanges();
        assertEquals(1, processor.size);
    }

    public void testAuditableChild_objectIdRelationship() throws Exception {
        auditable1.insert(1, "xx");
        auditableChildUuid.insert(1, "Auditable1:1", "xxx", "yyy");

        DataDomain domain = runtime.getDataDomain();
        Processor processor = new Processor();

        AuditableFilter filter = new AuditableFilter(domain.getEntityResolver(), processor);
        domain.addFilter(filter);

        // prerequisite for BaseAuditableProcessor use
        ChangeSetFilter changeSetFilter = new ChangeSetFilter();
        domain.addFilter(changeSetFilter);

        ObjectContext context = runtime.newContext();
        AuditableChildUuid ac = Cayenne.objectForPK(context, AuditableChildUuid.class, 1);
        Auditable1 a1 = Cayenne.objectForPK(context, Auditable1.class, 1);
        IdCoder refHandler = new IdCoder(domain.getEntityResolver());
        ObjectIdRelationshipHandler handler = new ObjectIdRelationshipHandler(refHandler);
        handler.relate(ac, a1);

        ac.setCharProperty1("xxxx");
        context.commitChanges();
        assertEquals(1, processor.size);
        Collection<Object> auditables = processor.audited.get(AuditableOperation.UPDATE);
        assertSame(a1, auditables.toArray()[0]);

        ac.setCharProperty2("yyyy");
        context.commitChanges();
        assertEquals(2, processor.size);
        assertSame(a1, auditables.toArray()[1]);
    }

    private final class Processor implements AuditableProcessor {

        Map<AuditableOperation, Collection<Object>> audited;
        int size;

        Processor() {
            reset();
        }

        void reset() {

            audited = new EnumMap<AuditableOperation, Collection<Object>>(AuditableOperation.class);

            for (AuditableOperation op : AuditableOperation.values()) {
                audited.put(op, new ArrayList<Object>());
            }
        }

        public void audit(Persistent object, AuditableOperation operation) {
            audited.get(operation).add(object);
            size++;
        }
    }
}
