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

package org.apache.cayenne.exp.property;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.path.CayennePath;

/**
 * Base interface for all types of properties
 * @since 4.2
 */
public interface Property<E> {

    /**
     * @return name of this property, can be null
     * @see #getPath()
     */
    String getName();

    /**
     * @return path that this property represents, can be empty if this is an expression-based property
     * @see #getName()
     * @since 5.0
     */
    CayennePath getPath();

    /**
     * @return alias of this property, can be null
     */
    String getAlias();

    /**
     * @return expression that defines this property, not null
     */
    Expression getExpression();

    /**
     * @return java type of this property, can be null
     */
    Class<E> getType();

}
