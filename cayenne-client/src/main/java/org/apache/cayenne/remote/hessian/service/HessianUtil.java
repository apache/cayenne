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

package org.apache.cayenne.remote.hessian.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.HessianConfig;
import org.apache.cayenne.remote.hessian.HessianConnection;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;

/**
 * Hessian related utilities.
 * 
 * @since 1.2
 */
public class HessianUtil {

    /**
     * A utility method that clones an object using Hessian serialization/deserialization
     * mechanism, which is different from default Java serialization.
     */
    public static Object cloneViaClientServerSerialization(
            Serializable object,
            EntityResolver serverResolver) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        HessianOutput out = new HessianOutput(bytes);
        out.setSerializerFactory(HessianConfig.createFactory(
                HessianConnection.CLIENT_SERIALIZER_FACTORIES,
                null));
        out.writeObject(object);

        byte[] data = bytes.toByteArray();

        HessianInput in = new HessianInput(new ByteArrayInputStream(data));
        in.setSerializerFactory(HessianConfig.createFactory(
                HessianService.SERVER_SERIALIZER_FACTORIES,
                serverResolver));

        return in.readObject();
    }

    public static Object cloneViaServerClientSerialization(
            Serializable object,
            EntityResolver serverResolver) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        HessianOutput out = new HessianOutput(bytes);
        out.setSerializerFactory(HessianConfig.createFactory(
                HessianService.SERVER_SERIALIZER_FACTORIES,
                serverResolver));
        out.writeObject(object);

        byte[] data = bytes.toByteArray();

        HessianInput in = new HessianInput(new ByteArrayInputStream(data));
        in.setSerializerFactory(HessianConfig.createFactory(
                HessianConnection.CLIENT_SERIALIZER_FACTORIES,
                null));
        return in.readObject();
    }

    private HessianUtil() {

    }
}
