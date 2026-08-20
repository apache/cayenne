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

package org.apache.cayenne.access.sqlbuilder;

import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbEntity;

import java.util.List;

/**
 * @since 4.2
 */
public interface SQLGenerationContext {

    DbAdapter getAdapter();

    List<PSParameter<?>> getBindings();

    DbEntity getRootDbEntity();

    /**
     * Returns true if the statement being generated uses a single table, in which case table nodes
     * and column nodes omit the table alias / prefix for readability (e.g. {@code SELECT NAME FROM
     * ARTIST} rather than {@code SELECT a.NAME FROM ARTIST a}).
     *
     * @since 5.0
     */
    boolean isSingleTableSQL();
}
