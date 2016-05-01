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
package org.apache.cayenne.rop.protostuff;

import io.protostuff.GraphIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.apache.cayenne.rop.ROPSerializationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProtostuffROPSerializationService implements ROPSerializationService {

    @Override
    public byte[] serialize(Object object) throws IOException {
        Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
        return GraphIOUtil.toByteArray(new Wrapper(object), schema, LinkedBuffer.allocate());
    }

    @Override
    public void serialize(Object object, OutputStream outputStream) throws IOException {
        Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
        GraphIOUtil.writeTo(outputStream, new Wrapper(object), schema, LinkedBuffer.allocate());
    }

    @Override
    public <T> T deserialize(InputStream inputStream, Class<T> objectClass) throws IOException {
        Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
        Wrapper result = schema.newMessage();
        GraphIOUtil.mergeFrom(inputStream, result, schema, LinkedBuffer.allocate());
        return objectClass.cast(result.data);

    }

    @Override
    public <T> T deserialize(byte[] serializedObject, Class<T> objectClass) throws IOException {
        Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
        Wrapper result = schema.newMessage();
        GraphIOUtil.mergeFrom(serializedObject, result, schema);
        return objectClass.cast(result.data);

    }

    private class Wrapper {
        public Object data;

        public Wrapper(Object data) {
            this.data = data;
        }
    }
}
