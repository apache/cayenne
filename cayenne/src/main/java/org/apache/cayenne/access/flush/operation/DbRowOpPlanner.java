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

import java.util.List;

/**
 * Plans the DB operations of a commit: returns the operations to execute, in the order to execute them. Besides
 * ordering the operations by the dependencies between their rows, a planner may replace some of them and add more,
 * e.g. to break a dependency cycle of the inserts by inserting a row without an FK and updating the FK after.
 *
 * <p>A planner must not change the operations passed to it, or the list. An operation it adds or replaces must be for
 * the same object and have the same change id as the operation it is made from, as the flush matches the operations
 * executed and the operations of the object changes by these.
 *
 * @see DefaultDbRowOpPlanner the default implementation
 * @since 6.0
 */
public interface DbRowOpPlanner {

    List<DbRowOp> plan(List<DbRowOp> ops);
}
