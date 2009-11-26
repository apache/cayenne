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

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.BindingBuilder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.MapBuilder;

/**
 * @since 3.1
 */
class DefaultBinder implements Binder {

    private DefaultInjector injector;

    DefaultBinder(DefaultInjector injector) {
        this.injector = injector;
    }

    public <T> BindingBuilder<T> bind(Class<T> type) {
        return new DefaultBindingBuilder<T>(type, injector);
    }

    public <T> ListBuilder<T> bindList(Class<T> implementationType) {
        return new DefaultListBuilder<T>(implementationType, injector);
    }

    public <T> MapBuilder<T> bindMap(Class<T> implementationType) {
        return new DefaultMapBuilder<T>(implementationType, injector);
    }

}
