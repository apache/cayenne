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
package org.apache.cayenne.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the order in which the rows of an entity's table are inserted and deleted relative to other tables
 * during a commit. By default, the order follows the foreign keys: a table is inserted after the tables it
 * references and deleted before them. Tables of entities with a lower weight are inserted before tables of entities
 * with a higher weight, whatever their foreign keys say, and deleted after them; among equal weights the foreign
 * key order applies. Every entity has the weight 1 unless annotated.
 *
 * <p>Place it on the entity class. It is honored by the runtime without any configuration.
 *
 * @since 6.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SortWeight {

    /**
     * The weight of the entity. Entities with lower values are inserted before entities with higher values, and
     * deleted after them.
     */
    int value() default 1;
}
