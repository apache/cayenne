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

package org.apache.cayenne.access.translator.batch;

import org.apache.cayenne.query.BatchQueryRow;

/**
 * Superclass of batch query translators.
 * 
 * @since 3.2
 */
public interface BatchTranslator {

    /**
     * Returns SQL String that can be used to init a PreparedStatement.
     */
    String getSql();

    /**
     * Returns the widest possible array of bindings for this query.
     */
    BatchParameterBinding[] getBindings();

    /**
     * Updates internal bindings to be used with a given row, returning updated
     * bindings array. This method guarantees that the returned array contains
     * the same bindings in the same order as in the array returned from
     * {@link #getBindings()} (but in a state corresponding to the 'row'
     * parameter). Usually the returned array is actually the same object reused
     * for every iteration, only with changed object state.
     */
    BatchParameterBinding[] updateBindings(BatchQueryRow row);
}
