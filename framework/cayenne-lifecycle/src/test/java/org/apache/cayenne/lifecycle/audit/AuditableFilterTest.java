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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import junit.framework.TestCase;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.graph.GraphDiff;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class AuditableFilterTest extends TestCase {

    public void testInsertAudit() {
        AuditableProcessor processor = mock(AuditableProcessor.class);

        AuditableFilter filter = new AuditableFilter(processor);
        Object audited = new Object();
        filter.insertAudit(audited);
        filter.postSync();

        verify(processor).audit(audited, AuditableOperation.INSERT);
    }

    public void testDeleteAudit() {
        AuditableProcessor processor = mock(AuditableProcessor.class);

        AuditableFilter filter = new AuditableFilter(processor);
        Object audited = new Object();
        filter.deleteAudit(audited);
        filter.postSync();

        verify(processor).audit(audited, AuditableOperation.DELETE);
    }

    public void testUpdateAudit() {
        AuditableProcessor processor = mock(AuditableProcessor.class);

        AuditableFilter filter = new AuditableFilter(processor);
        Object audited = new Object();
        filter.updateAudit(audited);
        filter.postSync();

        verify(processor).audit(audited, AuditableOperation.UPDATE);
    }

    public void testUpdateAuditChild() {
        AuditableProcessor processor = mock(AuditableProcessor.class);

        AuditableFilter filter = new AuditableFilter(processor);

        Object auditedParent = new Object();
        DataObject audited = new MockAuditableChild();
        audited.writeProperty("parent", auditedParent);
        filter.updateAuditChild(audited);
        filter.postSync();

        verify(processor).audit(auditedParent, AuditableOperation.UPDATE);
    }

    public void testOnSyncPassThrough() {
        AuditableProcessor processor = mock(AuditableProcessor.class);

        AuditableFilter filter = new AuditableFilter(processor);
        ObjectContext context = mock(ObjectContext.class);
        GraphDiff changes = mock(GraphDiff.class);

        DataChannelFilterChain chain = mock(DataChannelFilterChain.class);

        filter.onSync(context, changes, DataChannel.FLUSH_CASCADE_SYNC, chain);
        verify(chain).onSync(context, changes, DataChannel.FLUSH_CASCADE_SYNC);

        filter.onSync(context, changes, DataChannel.ROLLBACK_CASCADE_SYNC, chain);
        verify(chain).onSync(context, changes, DataChannel.ROLLBACK_CASCADE_SYNC);
    }

    public void testOnSyncAuditEventsCollapse() {
        AuditableProcessor processor = mock(AuditableProcessor.class);

        final AuditableFilter filter = new AuditableFilter(processor);
        ObjectContext context = mock(ObjectContext.class);
        GraphDiff changes = mock(GraphDiff.class);

        final Object auditedParent1 = new Object();
        final DataObject audited11 = new MockAuditableChild();
        audited11.writeProperty("parent", auditedParent1);
        final DataObject audited12 = new MockAuditableChild();
        audited12.writeProperty("parent", auditedParent1);
        final DataObject audited13 = new MockAuditableChild();
        audited13.writeProperty("parent", auditedParent1);

        DataChannelFilterChain chain = mock(DataChannelFilterChain.class);
        when(chain.onSync(context, changes, DataChannel.FLUSH_CASCADE_SYNC)).thenAnswer(
                new Answer<GraphDiff>() {

                    public GraphDiff answer(InvocationOnMock invocation) throws Throwable {
                        filter.updateAudit(auditedParent1);
                        filter.updateAuditChild(audited11);
                        filter.updateAuditChild(audited12);
                        filter.updateAuditChild(audited13);
                        return mock(GraphDiff.class);
                    }
                });

        filter.onSync(context, changes, DataChannel.FLUSH_CASCADE_SYNC, chain);

        verify(chain).onSync(context, changes, DataChannel.FLUSH_CASCADE_SYNC);
        verify(processor).audit(auditedParent1, AuditableOperation.UPDATE);
    }
}
