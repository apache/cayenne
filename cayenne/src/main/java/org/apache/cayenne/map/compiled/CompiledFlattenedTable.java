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
package org.apache.cayenne.map.compiled;

import org.apache.cayenne.exp.path.CayennePath;

/**
 * A table other than the entity's own that rows of the entity span, reached through a flattened attribute or
 * relationship.
 *
 * @param path     the join path to the table, relative to the root table of the entity
 * @param table the table
 * @param noDelete true if the deletion of a row of the entity must not delete the row of this table, as other rows
 *                 may reference it, or the delete rules prevent it
 * @since 6.0
 */
public record CompiledFlattenedTable(CayennePath path, CompiledTable table, boolean noDelete) {
}
