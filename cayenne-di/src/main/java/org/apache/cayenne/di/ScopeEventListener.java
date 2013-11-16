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
package org.apache.cayenne.di;


/**
 * This interface duplicates default reflection based mechanism for receiving DI events.
 * It is not fully supported and its usage are reserved for cases when for some reason
 * it is not possible to use reflection. It is used for example in
 * {@link javax.sql.DataSource} managing layer to provide compatibility with java version 5.
 *
 * @since 3.1
 */
public interface ScopeEventListener {
    /**
     * Similar to {@link BeforeScopeEnd}
     */
    void beforeScopeEnd();
}
