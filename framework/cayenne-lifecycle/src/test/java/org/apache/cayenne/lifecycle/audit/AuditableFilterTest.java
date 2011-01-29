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
import junit.framework.TestCase;

import org.apache.cayenne.DataObject;

public class AuditableFilterTest extends TestCase {

    public void testInsertAudit() {
        AuditableProcessor processor = mock(AuditableProcessor.class);

        AuditableFilter filter = new AuditableFilter(processor);
        Object audited = new Object();
        filter.insertAudit(audited);

        verify(processor).audit(audited, audited, AuditableOperation.INSERT);
    }

    public void testDeleteAudit() {
        AuditableProcessor processor = mock(AuditableProcessor.class);

        AuditableFilter filter = new AuditableFilter(processor);
        Object audited = new Object();
        filter.deleteAudit(audited);

        verify(processor).audit(audited, audited, AuditableOperation.DELETE);
    }

    public void testUpdateAudit() {
        AuditableProcessor processor = mock(AuditableProcessor.class);

        AuditableFilter filter = new AuditableFilter(processor);
        Object audited = new Object();
        filter.updateAudit(audited);

        verify(processor).audit(audited, audited, AuditableOperation.UPDATE);
    }

    public void testUpdateAuditChild() {
        AuditableProcessor processor = mock(AuditableProcessor.class);

        AuditableFilter filter = new AuditableFilter(processor);

        Object auditedParent = new Object();
        DataObject audited = new MockAuditableChild();
        audited.writeProperty("parent", auditedParent);
        filter.updateAuditChild(audited);

        verify(processor).audit(auditedParent, audited, AuditableOperation.UPDATE);
    }
}
