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
 * Defines standard scopes supported by the DI container.
 * 
 * @since 3.1
 */
public final class Scopes {

    public static final Scope NO_SCOPE;

    public static final Scope SINGLETON;

    static {

        NO_SCOPE = new Scope() {

            public <T> Provider<T> scope(Provider<T> unscoped) {
                return unscoped;
            }

            @Override
            public String toString() {
                return "Scopes.NO_SCOPE";
            }
        };

        SINGLETON = new Scope() {

            public <T> Provider<T> scope(Provider<T> unscoped) {
                return new SingletonProvider<T>(unscoped);
            }

            @Override
            public String toString() {
                return "Scopes.SINGLETON";
            }
        };
    }
}
