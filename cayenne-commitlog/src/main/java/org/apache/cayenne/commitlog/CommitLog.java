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

package org.apache.cayenne.commitlog;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that adds commit log behavior to Persistent objects.
 *
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CommitLog {

    /**
     * Returns an array of entity properties that should be excluded from commit log.
     */
    String[] ignoredProperties() default {};

    /**
     * Returns whether all attributes should be excluded from commit log.
     *
     * @since 4.0
     */
    boolean ignoreAttributes() default false;

    /**
     * Returns whether all to-one relationships should be excluded from commit log.
     *
     * @since 4.0
     */
    boolean ignoreToOneRelationships() default false;

    /**
     * Returns whether all to-many relationships should be excluded from commit log.
     *
     * @since 4.0
     */
    boolean ignoreToManyRelationships() default false;

    /**
     * Returns an array of properties that should be treated as confidential.
     * I.e. their change should be recorded, but their values should be hidden
     * from listeners. In practice both old and new values will be set to an
     * instance of {@link Confidential}.
     *
     * @since 4.0
     */
    String[] confidential() default {};
}
