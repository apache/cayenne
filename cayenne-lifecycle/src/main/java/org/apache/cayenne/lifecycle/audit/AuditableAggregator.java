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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cayenne.Persistent;

/**
 * Aggregates audit events per audit root object, and passes them to delegate
 * processor at the end of the transaction.
 * 
 * @since 3.1
 */
class AuditableAggregator {

    private static final int[] OP_PRECEDENCE;

    static {
        OP_PRECEDENCE = new int[AuditableOperation.values().length];

        // decreasing precedence of operations when recording audits is DELETE,
        // INSERT, UPDATE
        OP_PRECEDENCE[AuditableOperation.DELETE.ordinal()] = 3;
        OP_PRECEDENCE[AuditableOperation.INSERT.ordinal()] = 2;
        OP_PRECEDENCE[AuditableOperation.UPDATE.ordinal()] = 1;
    }

    private AuditableProcessor delegate;

    private Map<Persistent, AuditableOperation> ops;

    AuditableAggregator(AuditableProcessor delegate) {
        this.delegate = delegate;
        this.ops = new IdentityHashMap<Persistent, AuditableOperation>();
    }

    void audit(Persistent object, AuditableOperation operation) {
        AuditableOperation oldOp = ops.put(object, operation);
        if (oldOp != null) {
            if (OP_PRECEDENCE[operation.ordinal()] < OP_PRECEDENCE[oldOp.ordinal()]) {
                ops.put(object, oldOp);
            }
        }
    }

    void postSync() {
        for (Entry<Persistent, AuditableOperation> op : ops.entrySet()) {
            delegate.audit(op.getKey(), op.getValue());
        }
    }

}
