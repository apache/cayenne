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
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.util.PersistentObjectList;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.JavaDeserializer;
import com.caucho.hessian.io.Serializer;

/**
 * An object that manages all custom (de)serializers used on the client.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ClientSerializerFactory extends AbstractSerializerFactory {

    private Map<Class, Deserializer> deserializers;
    private Deserializer dataRowDeserializer;

    @Override
    public Serializer getSerializer(Class cl) throws HessianProtocolException {
        return null;
    }

    @Override
    public Deserializer getDeserializer(Class cl) throws HessianProtocolException {
        Deserializer deserializer = null;

        if (PersistentObjectList.class.isAssignableFrom(cl)) {

            synchronized (this) {

                if (deserializers != null) {
                    deserializer = deserializers.get(cl);
                }

                if (deserializer == null) {
                    deserializer = new JavaDeserializer(cl);

                    if (deserializers == null) {
                        deserializers = new HashMap<Class, Deserializer>();
                    }

                    deserializers.put(cl, deserializer);
                }
            }
        }
        else if(DataRow.class.isAssignableFrom(cl)) {
            if(dataRowDeserializer == null) {
                dataRowDeserializer = new DataRowDeserializer();
            }
            
            return dataRowDeserializer;
        }

        return deserializer;
    }
}
