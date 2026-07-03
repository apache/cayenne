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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * A value that is not known until a preceding row of the same transaction has been executed, e.g. a generated PK
 * propagated to a dependent PK or to a foreign key. Such a value is captured when a batch is planned and resolved
 * lazily via {@link #resolve(Object)} when the owning row is bound, after the rows it depends on have been executed.
 *
 * @since 5.0
 */
@FunctionalInterface
public interface DeferredValue {

    int MAX_NESTED_LEVEL = 1000;

    /**
     * Returns the current value, which may itself be another {@link DeferredValue}. Use {@link #resolve(Object)} to
     * collapse a full chain.
     */
    Object get();

    /**
     * Fully resolves a value that may be a {@link DeferredValue}, following nested chains all the way down. A value
     * that is not a {@code DeferredValue} is returned as is. Throws if the chain does not terminate, guarding against
     * a value that (incorrectly) resolves to itself.
     */
    static Object resolve(Object value) {
        int safeguard = 0;
        while (value instanceof DeferredValue deferred) {
            if (++safeguard > MAX_NESTED_LEVEL) {
                throw new CayenneRuntimeException("Possible recursive deferred value chain: %s", value);
            }
            value = deferred.get();
        }
        return value;
    }
}
