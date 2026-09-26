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
package org.apache.cayenne;

import org.apache.cayenne.query.Query;

/**
 * A single item of a multipart query result. QueryResults are returned as a list from {@link ObjectContext#execute(Query)}.
 * The goal is to expose a specific combination of ResultSets, update counts and procedure OUT parameters produced by a
 * JDBC statement. The user can process each QueryResult like this:
 * <pre>{@code
 * for (QueryResult item : SQLExec.query(sql).execute(context)) {
 *     switch (item) {
 *         case SelectResult<?> select -> process(select.objects());
 *         case UpdateResult update -> process(update.counts());
 *         case ResultIterator<?> iterator -> process(iterator);
 *         case OutParametersResult out -> process(out.values());
 *     }
 * }
 * }</pre>
 */
public sealed interface QueryResult permits SelectResult, UpdateResult, ResultIterator, OutParametersResult {
}
