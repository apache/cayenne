/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/
package org.apache.cayenne.configuration.rop.client;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.rop.ROPSerializationService;
import org.apache.cayenne.rop.protostuff.ProtostuffROPSerializationServiceProvider;

/**
 * A DI module that uses Protostuff Object Graph Serialization as Cayenne {@link ROPSerializationService}.
 * <a href="http://www.protostuff.io/">
 *
 * Note the you usually have to add -Dprotostuff.runtime.collection_schema_on_repeated_fields=true as VM option
 * because Cayenne objects might have cyclic collection fields.
 *
 * @since 4.0
 */
public class ProtostuffModule implements Module {

    public ProtostuffModule() {
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ROPSerializationService.class).toProvider(ProtostuffROPSerializationServiceProvider.class);
    }
}
