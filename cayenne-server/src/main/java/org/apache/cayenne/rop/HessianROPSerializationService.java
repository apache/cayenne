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
package org.apache.cayenne.rop;

import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.SerializerFactory;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.remote.hessian.HessianConfig;
import org.apache.cayenne.remote.hessian.service.HessianService;

import java.io.*;

public class HessianROPSerializationService implements ROPSerializationService {

    protected SerializerFactory serializerFactory;
    
    public HessianROPSerializationService(SerializerFactory serializerFactory) {
        this.serializerFactory = serializerFactory;
    }
    
    @Override
    public byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        HessianOutput out = new HessianOutput(bytes);
        out.setSerializerFactory(serializerFactory);
        out.writeObject(object);

        return bytes.toByteArray();
    }

	@Override
	public void serialize(Object object, OutputStream outputStream) throws IOException {
		Hessian2Output out = new Hessian2Output(outputStream);
		out.setSerializerFactory(serializerFactory);
		out.writeObject(object);
	}

	@Override
	public <T> T deserialize(byte[] serializedObject, Class<T> objectClass) throws IOException {
        HessianInput in = new HessianInput(new ByteArrayInputStream(serializedObject));
        in.setSerializerFactory(serializerFactory);

        return objectClass.cast(in.readObject());
	}

	@Override
    public <T> T deserialize(InputStream input, Class<T> objectClass) throws IOException {
        HessianInput in = new HessianInput(input);
        in.setSerializerFactory(serializerFactory);

        return objectClass.cast(in.readObject());
    }
}
