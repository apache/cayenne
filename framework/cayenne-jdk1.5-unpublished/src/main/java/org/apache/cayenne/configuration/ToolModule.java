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
package org.apache.cayenne.configuration;

import org.apache.cayenne.access.jdbc.BatchQueryBuilderFactory;
import org.apache.cayenne.access.jdbc.DefaultBatchQueryBuilderFactory;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;

/**
 * A DI module containing basic Cayenne configuration.
 * 
 * @since 3.1
 */
public class ToolModule implements Module {
    
public void configure(Binder binder) {
        
        // configure empty global stack properties
        binder.bindMap(Constants.PROPERTIES_MAP);
        
        binder.bindList(Constants.SERVER_DEFAULT_TYPES_LIST);
        binder.bindList(Constants.SERVER_USER_TYPES_LIST);
        binder.bindList(Constants.SERVER_TYPE_FACTORIES_LIST);

        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
        binder.bind(BatchQueryBuilderFactory.class).to(DefaultBatchQueryBuilderFactory.class);
        binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
    }

}
