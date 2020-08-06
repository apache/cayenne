/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.rop.http;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.remote.hessian.ClientSerializerFactory;
import org.apache.cayenne.remote.hessian.HessianConfig;
import org.apache.cayenne.rop.HessianROPSerializationService;
import org.apache.cayenne.rop.ROPSerializationService;

public class ClientHessianSerializationServiceProvider implements Provider<ROPSerializationService> {

    public static final String[] CLIENT_SERIALIZER_FACTORIES = new String[] {
            ClientSerializerFactory.class.getName()
    };

    @Override
    public ROPSerializationService get() throws DIRuntimeException {
        return new HessianROPSerializationService(
                HessianConfig.createFactory(CLIENT_SERIALIZER_FACTORIES, null));
    }
}
