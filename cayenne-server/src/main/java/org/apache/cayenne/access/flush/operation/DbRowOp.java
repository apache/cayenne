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

package org.apache.cayenne.access.flush.operation;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbEntity;

/**
 * Object that represents some change on DB level.
 * Common cases are insert/update/delete of single DB row.
 *
 * @since 4.2
 */
public interface DbRowOp {

    <T> T accept(DbRowOpVisitor<T> visitor);

    DbEntity getEntity();

    ObjectId getChangeId();

    Persistent getObject();

    /**
     * @param rowOp to check
     * @return is this and rowOp operations belong to same sql batch
     */
    boolean isSameBatch(DbRowOp rowOp);
}
