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

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;

// This class is an ugly workaround for Hessian 4 bug with not loading custom deserializers.
// TODO: once it is fixed in Hessian, remove this class
class CayenneSerializerFactory extends com.caucho.hessian.io.SerializerFactory {
    @Override
    public Deserializer getDeserializer(Class cl) throws HessianProtocolException {
        for (int i = 0; _factories != null && i < _factories.size(); i++) {
            AbstractSerializerFactory factory;
            factory = (AbstractSerializerFactory) _factories.get(i);

            Deserializer deserializer = factory.getDeserializer(cl);
            if (deserializer != null) {
                return deserializer;
            }
        }
        
        return super.getDeserializer(cl);
    }

}
