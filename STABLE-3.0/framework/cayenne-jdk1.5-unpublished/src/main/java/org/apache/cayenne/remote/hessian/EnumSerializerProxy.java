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

import org.apache.cayenne.util.Util;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;

/**
 * A JDK14 compatible wrapper for JDK15 EnumSerializer proxy. Under JDK 1.4 this object
 * does nothing.
 * 
 * @since 1.2
 */
public class EnumSerializerProxy extends AbstractSerializerFactory {

    static final String FACTORY_CLASS = "org.apache.cayenne.remote.hessian.EnumSerializerFactory";

    private AbstractSerializerFactory enumSerializer;

    public EnumSerializerProxy() {
        try {
            // sniff JDK 1.5
            Class.forName("java.lang.StringBuilder");

            Class factoryClass = Util.getJavaClass(FACTORY_CLASS);
            this.enumSerializer = (AbstractSerializerFactory) factoryClass.newInstance();
        }
        catch (Throwable th) {
            // ignore.. jdk 1.4
        }
    }

    @Override
    public Serializer getSerializer(Class cl) throws HessianProtocolException {
        return enumSerializer != null ? enumSerializer.getSerializer(cl) : null;
    }

    @Override
    public Deserializer getDeserializer(Class cl) throws HessianProtocolException {
        return enumSerializer != null ? enumSerializer.getDeserializer(cl) : null;
    }
}
