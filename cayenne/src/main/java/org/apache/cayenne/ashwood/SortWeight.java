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
package org.apache.cayenne.ashwood;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Orders the rows of an entity's table relative to the rows of other tables during a commit, when the rows don't
 * depend on each other. Tables of entities with a lower weight are inserted before tables of entities with a higher
 * weight, and deleted after them; among equal weights, the foreign key order of the tables applies. Every entity has
 * the weight 1 unless annotated. A dependency between the rows of a commit (e.g. an FK of a row referencing another
 * row of the commit) always takes precedence over the weights.
 *
 * <p>Place it on the entity class. It is honored by the runtime without any configuration.
 *
 * @since 3.1, since 4.0 moved to cayenne-server from cayenne-lifecycle
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SortWeight {

    /**
     * Returns the "weight" of the entity used for the purpose of the DB operations
     * sorting. Entities with lower values will be inserted before entities with higher
     * values. The opposite is true for the delete operations.
     */
    int value() default 1;
}
