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

package org.apache.cayenne.access.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class SnapshotEventTest extends TestCase {

    public void testRootEvent() {
        Object source = new Object();
        Collection<?> deleted = new ArrayList<Object>();
        Collection<?> invalidated = new ArrayList<Object>();
        Map<?, ?> modified = new HashMap<Object, Object>();
        Collection<?> related = new ArrayList<Object>();

        SnapshotEvent event = new SnapshotEvent(
                source,
                source,
                modified,
                deleted,
                invalidated,
                related);
        assertSame(source, event.getSource());
        assertSame(source, event.getPostedBy());
        assertSame(deleted, event.getDeletedIds());
        assertSame(invalidated, event.getInvalidatedIds());
        assertSame(modified, event.getModifiedDiffs());
        assertSame(related, event.getIndirectlyModifiedIds());
    }
}
