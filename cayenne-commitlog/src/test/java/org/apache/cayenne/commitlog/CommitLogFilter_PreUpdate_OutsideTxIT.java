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
package org.apache.cayenne.commitlog;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.commitlog.db.AuditLog;
import org.apache.cayenne.commitlog.db.AuditableChild5;
import org.apache.cayenne.commitlog.model.ObjectChange;
import org.apache.cayenne.commitlog.unit.AuditableServerCase;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.tx.BaseTransaction;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.*;

public class CommitLogFilter_PreUpdate_OutsideTxIT extends AuditableServerCase {

    protected ObjectContext context;
    protected CommitLogListener listener;

    @Override
    protected ServerRuntimeBuilder configureCayenne() {
        this.listener = (originatingContext, changes) -> {
            // assert we are not inside a transaction
            assertNull(BaseTransaction.getThreadTransaction());

            for (ObjectChange c : changes.getUniqueChanges()) {
                AuditLog log = runtime.newContext().newObject(AuditLog.class);
                log.setLog("DONE: " + c.getPostCommitId());
                log.getObjectContext().commitChanges();
            }
        };

        return super.configureCayenne()
                .addModule(binder ->
                        ServerModule.contributeDomainListeners(binder)
                                .add(MyPreUpdateListener.class)
                )
                .addModule(
                        CommitLogModule.extend()
                                .commitLogAnnotationEntitiesOnly()
                                .excludeFromTransaction()
                                .addListener(listener)
                                .module()
                );
    }

    @Before
    public void before() throws Exception {
        context = runtime.newContext();
        auditable5.insert(1, "yy");
        auditableChild5.insert(1, 1, "zz");
    }

    @Test
    public void testCommitLog() throws SQLException {

        AuditableChild5 auditableChild = ObjectSelect.query(AuditableChild5.class)
                .selectOne(context);
        assertEquals("zz", auditableChild.getCharProperty1());

        auditableChild.setCharProperty1("xx");

        context.commitChanges();

        List<Object[]> logs = auditLog.selectAll();
        assertEquals(2, logs.size());
    }

    public static class MyPreUpdateListener {

        @PreUpdate({ AuditableChild5.class })
        public void preUpdate( AuditableChild5 child ) {
            child.getParent().setCharProperty1("zz");
        }
    }


}
