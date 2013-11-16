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

import java.util.Collection;

import org.apache.cayenne.ConfigurationException;

/**
 * A binding builder for list configurations.
 * 
 * @param <T> A type of list values.
 * @since 3.1
 */
public interface ListBuilder<T> {

    ListBuilder<T> add(Class<? extends T> interfaceType) throws ConfigurationException;

    ListBuilder<T> add(T value) throws ConfigurationException;
    
    ListBuilder<T> addAll(Collection<T> values) throws ConfigurationException;

    void in(Scope scope);
}
