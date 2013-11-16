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
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.Scope;

/**
 * A binding encapsulates DI provider scoping settings and allows to change them as many
 * times as needed.
 * 
 * @since 3.1
 */
class Binding<T> {

    private Provider<T> unscoped;
    private Provider<T> scoped;

    Binding(Provider<T> provider, Scope initialScope) {
        this.unscoped = provider;
        this.scoped = initialScope != null ? initialScope.scope(provider) : provider;
    }

    void changeScope(Scope scope) {
        if (scope == null) {
            scope = NoScope.INSTANCE;
        }

        scoped = scope.scope(unscoped);
    }

    Provider<T> getUnscoped() {
        return unscoped;
    }

    Provider<T> getScoped() {
        return scoped;
    }
}
