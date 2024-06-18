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
package org.apache.cayenne.access.dbsync;

import org.apache.cayenne.access.DataNode;

import java.sql.SQLException;

/**
 * @since 3.0
 */
// Note that this object must be scoped by DataNode. An attempt to share it between nodes will result in ignoring
// schema update for all but one node (see CAY-2125)
public abstract class BaseSchemaUpdateStrategy implements SchemaUpdateStrategy {

    protected volatile boolean run;
    protected final ThreadLocal<Boolean> threadRunInProgress;

    public BaseSchemaUpdateStrategy() {

        // this barrier is needed to prevent stack overflow in the same thread
        // (getConnection/updateSchema/getConnection/...)
        this.threadRunInProgress = new ThreadLocal<>();
    }

    @Override
    public void updateSchema(DataNode dataNode) throws SQLException {

        Boolean inProgress = threadRunInProgress.get();
        if (!run && (inProgress == null || !inProgress)) {
            synchronized (this) {
                if (!run) {

                    try {
                        threadRunInProgress.set(true);
                        processSchemaUpdate(dataNode);
                        run = true;
                    }
                    finally {
                        threadRunInProgress.set(false);
                    }
                }
            }
        }
    }

    /**
     * @since 3.0
     */
    protected abstract void processSchemaUpdate(DataNode dataNode) throws SQLException;

}
