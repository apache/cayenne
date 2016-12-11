/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.joda;

import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.joda.access.types.DateTimeType;
import org.apache.cayenne.joda.access.types.LocalDateTimeType;
import org.apache.cayenne.joda.access.types.LocalDateType;
import org.apache.cayenne.joda.access.types.LocalTimeType;

/**
 * Auto-loadable Cayenne module that adds support for Joda {@link org.apache.cayenne.access.types.ExtendedType} types.
 *
 * @since 4.0
 */
public class JodaModule implements Module {

    @Override
    public void configure(Binder binder) {
        ServerModule.contributeDefaultTypes(binder)
                .add(new DateTimeType())
                .add(new LocalDateType())
                .add(new LocalTimeType())
                .add(new LocalDateTimeType());
    }
}
