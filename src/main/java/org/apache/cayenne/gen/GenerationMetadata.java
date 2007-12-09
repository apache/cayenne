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
package org.apache.cayenne.gen;

/**
 * Represents basic metadata associated with a code generation execution.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public interface GenerationMetadata {

    /**
     * Returns class name (without a package) of the sub class associated with this
     * generator.
     */
    String getSubClassName();

    /**
     * Returns the super class (without a package) of the data object class associated
     * with this generator
     */
    String getSuperClassName();

    /**
     * Returns the base class (without a package) of the data object class associated with
     * this generator. Class name must not include a package.
     */
    String getBaseClassName();

    /**
     * Returns Java package name of the class associated with this generator.
     */
    String getSubPackageName();

    /**
     * Returns <code>superPackageName</code> property that defines a superclass's
     * package name.
     */
    String getSuperPackageName();

    /**
     * Returns <code>basePackageName</code> property that defines a baseclass's
     * (superclass superclass) package name.
     */
    String getBasePackageName();
}
