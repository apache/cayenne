/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.RuntimeEnv;
import io.protostuff.runtime.RuntimeSchema;
import org.apache.cayenne.ObjectContextChangeLogSubListMessageFactory;
import org.apache.cayenne.access.ToManyList;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.PrefetchTreeNodeSchema;
import org.apache.cayenne.rop.ROPSerializationService;
import org.apache.cayenne.util.PersistentObjectList;
import org.apache.cayenne.util.PersistentObjectMap;
import org.apache.cayenne.util.PersistentObjectSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This {@link ROPSerializationService} implementation uses Protostuff {@link GraphIOUtil} to (de)serialize
 * Cayenne object graph.
 *
 * @since 4.0
 */
public class ProtostuffROPSerializationService implements ROPSerializationService {

    protected Schema<Wrapper> wrapperSchema;
    protected DefaultIdStrategy strategy;

    public ProtostuffROPSerializationService() {
        this.strategy = (DefaultIdStrategy) RuntimeEnv.ID_STRATEGY;
        register();
    }

    protected void register() {
        this.wrapperSchema = RuntimeSchema.getSchema(Wrapper.class);

        this.strategy.registerCollection(new ObjectContextChangeLogSubListMessageFactory());

        RuntimeSchema.register(PrefetchTreeNode.class, new PrefetchTreeNodeSchema());
        RuntimeSchema.register(PersistentObjectList.class);
        RuntimeSchema.register(PersistentObjectMap.class);
        RuntimeSchema.register(PersistentObjectSet.class);
        RuntimeSchema.register(ToManyList.class);
    }

    @Override
    public byte[] serialize(Object object) throws IOException {
        return GraphIOUtil.toByteArray(new Wrapper(object), wrapperSchema, LinkedBuffer.allocate());
    }

    @Override
    public void serialize(Object object, OutputStream outputStream) throws IOException {
        GraphIOUtil.writeTo(outputStream, new Wrapper(object), wrapperSchema, LinkedBuffer.allocate());
    }

    @Override
    public <T> T deserialize(InputStream inputStream, Class<T> objectClass) throws IOException {
        Wrapper result = wrapperSchema.newMessage();
        GraphIOUtil.mergeFrom(inputStream, result, wrapperSchema);
        return objectClass.cast(result.data);
    }

    @Override
    public <T> T deserialize(byte[] serializedObject, Class<T> objectClass) throws IOException {
        Wrapper result = wrapperSchema.newMessage();
        GraphIOUtil.mergeFrom(serializedObject, result, wrapperSchema);
        return objectClass.cast(result.data);
    }

}
