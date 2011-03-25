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
package org.apache.cayenne.lifecycle.sort;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that defines the insertion sorting "weight" of an entity that is used
 * when sorting DB operations. This annotation allows to override the topological sorting
 * algorithm used by Cayenne by default in special occasions.
 * 
 * @since 3.1
 */
@Target( {
    ElementType.TYPE
})
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
