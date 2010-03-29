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

package org.apache.cayenne.remote.hessian;

import java.util.HashMap;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;

/**
 * A Hessian SerializerFactory extension that supports serializing Enums.
 * <p>
 * <i>Requires Java 1.5 or newer</i>
 * </p>
 * 
 * @since 1.2
 */
class EnumSerializerFactory extends AbstractSerializerFactory {

    private final EnumSerializer enumSerializer = new EnumSerializer();
    private HashMap<Class, Deserializer> cachedDeserializerMap;

    @Override
    public Serializer getSerializer(Class cl) throws HessianProtocolException {
        return (cl.isEnum()) ? enumSerializer : null;
    }

    @Override
    public Deserializer getDeserializer(Class cl) throws HessianProtocolException {
        if (cl.isEnum()) {
            Deserializer deserializer = null;
            
            synchronized (this) {
                
                if (cachedDeserializerMap != null) {
                    deserializer = cachedDeserializerMap.get(cl);
                }

                if (deserializer == null) {
                    deserializer = new EnumDeserializer(cl);

                    if (cachedDeserializerMap == null) {
                        cachedDeserializerMap = new HashMap<Class, Deserializer>();
                    }

                    cachedDeserializerMap.put(cl, deserializer);
                }
            }

            return deserializer;
        }

        return null;
    }
}
