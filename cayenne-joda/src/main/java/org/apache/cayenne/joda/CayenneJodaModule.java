package org.apache.cayenne.joda;

/**
 * **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * **************************************************************
 */

import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.joda.access.types.DateTimeType;
import org.apache.cayenne.joda.access.types.LocalDateTimeType;
import org.apache.cayenne.joda.access.types.LocalDateType;
import org.apache.cayenne.joda.access.types.LocalTimeType;

/**
 * Include this module when creating a ServerRuntime in order to add support for
 * joda-time ObjAttributes.
 *
 * @since 4.0
 */
public class CayenneJodaModule implements Module {

    public CayenneJodaModule() {
    }

    @Override
    public void configure(Binder binder) {
        ServerModule.contributeDefaultTypes(binder)
                .add(new DateTimeType())
                .add(new LocalDateType())
                .add(new LocalTimeType())
                .add(new LocalDateTimeType());
    }
}
