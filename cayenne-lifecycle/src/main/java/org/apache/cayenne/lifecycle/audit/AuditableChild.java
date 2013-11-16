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
package org.apache.cayenne.lifecycle.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A built-in annotation used to tag an object that is not auditable on its own, but whose
 * changes should be tracked together with changes of another ("parent") object. This
 * annotation allows to group changes in a closely related subtree of objects. Either
 * {@link #value()} or {@link #objectIdRelationship()} must be set to a non-empty String,
 * so that a processor of AuditableChild could find the parent of the annotated object.
 * 
 * @since 3.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface AuditableChild {

    /**
     * Returns the name of a to-one relationship from an annotated object to the "parent"
     * object that should be audited when annotated object is changed.
     */
    String value() default "";

    /**
     * Returns the name of the property of the annotated entity of the relationship that
     * stores a String "FK" of a related "parent" entity.
     */
    String objectIdRelationship() default "";

    String[] ignoredProperties() default {};
}
