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
package org.apache.cayenne.commitlog.unit;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.commitlog.CommitLogListener;
import org.apache.cayenne.commitlog.model.ChangeMap;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A commit log listener that records its calls, and optionally runs a check of each call while it is made.
 */
public class RecordingCommitLogListener implements CommitLogListener {

    public record Call(ObjectContext originatingContext, ChangeMap changes) {
    }

    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private volatile BiConsumer<ObjectContext, ChangeMap> check = (context, changes) -> {};

    /**
     * Sets a check to run on each call, while the call is made.
     */
    public void onCommit(BiConsumer<ObjectContext, ChangeMap> check) {
        this.check = check;
    }

    @Override
    public void onPostCommit(ObjectContext originatingContext, ChangeMap changes) {
        calls.add(new Call(originatingContext, changes));
        check.accept(originatingContext, changes);
    }

    public List<Call> getCalls() {
        return calls;
    }

    /**
     * Checks that the listener was called exactly once, and returns that call.
     */
    public Call singleCall() {
        assertEquals(1, calls.size(), "Unexpected number of post-commit calls");
        return calls.get(0);
    }
}
