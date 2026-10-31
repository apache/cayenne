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

package org.apache.cayenne.query.resolved;

import org.apache.cayenne.ObjectId;

import java.util.Map;

/**
 * A single row of a {@link ResolvedBatch}: the id of the object it belongs to (null for a delete), the snapshot of
 * the values identifying the row (null for an insert) and the snapshot of the values written by the row (null for a
 * delete). Which of the two snapshots feeds a given statement parameter is decided by the batch, see
 * {@link ResolvedBatch#value(BatchRow, int)}.
 *
 * @since 6.0
 */
public record BatchRow(ObjectId objectId, Map<String, Object> qualifier, Map<String, Object> values) {
}
