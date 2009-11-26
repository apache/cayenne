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
 * A binding builder that helps with fluent binding creation.
 * 
 * @param <T> An interface type of the service being bound.
 * @since 3.1
 */
public interface BindingBuilder<T> {

    BindingBuilder<T> to(Class<? extends T> implementation) throws DIException;

    BindingBuilder<T> toInstance(T instance) throws DIException;

    BindingBuilder<T> toProvider(Class<? extends Provider<? extends T>> providerType)
            throws DIException;

    void in(Scope scope);
}
