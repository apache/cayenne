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
 * A factory of delete row ops used by the commit pipeline.
 *
 * @since 5.0
 */
public interface DeleteDbRowOpFactory {

    /**
     * Creates a delete op for a given object and target table.
     *
     * @param object the object being deleted
     * @param entity the target DbEntity (may be the root table or an additional / flattened table)
     * @param id     the id of the row being deleted
     * @return a delete op
     */
    DeleteDbRowOp createOp(Persistent object, DbEntity entity, ObjectId id);
}
