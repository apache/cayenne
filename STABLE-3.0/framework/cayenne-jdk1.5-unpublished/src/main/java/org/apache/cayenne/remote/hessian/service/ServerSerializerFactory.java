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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.util.PersistentObjectList;
import org.apache.cayenne.util.PersistentObjectMap;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.JavaSerializer;
import com.caucho.hessian.io.Serializer;

/**
 * An object that manages all custom (de)serializers used on the server.
 * 
 * @since 1.2
 */
class ServerSerializerFactory extends AbstractSerializerFactory {
    private ServerPersistentObjectListSerializer persistentObjectListSerializer;
    private ServerDataRowSerializer dataRowSerilaizer;
    private Serializer javaSerializer;

    ServerSerializerFactory() {
        this.persistentObjectListSerializer = new ServerPersistentObjectListSerializer();
        this.dataRowSerilaizer = new ServerDataRowSerializer();
    }

    @Override
    public Serializer getSerializer(Class cl) throws HessianProtocolException {

        if (PersistentObjectList.class.isAssignableFrom(cl)) {
            return persistentObjectListSerializer;
        }
        else if (DataRow.class.isAssignableFrom(cl)) {
            return dataRowSerilaizer;
        }
        //turns out Hessian uses its own (incorrect) serialization mechanism for maps
        else if (PersistentObjectMap.class.isAssignableFrom(cl)) {
            if (javaSerializer == null) {
                javaSerializer = new JavaSerializer(cl);
            }
            return javaSerializer;
        }

        return null;
    }

    @Override
    public Deserializer getDeserializer(Class cl) throws HessianProtocolException {
        return null;
    }
}
