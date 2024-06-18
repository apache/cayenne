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

package org.apache.cayenne;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.reflect.PropertyUtils;

/**
 *
 * This data object like {@link GenericPersistentObject} uses {@link Map} to store generic attributes,
 * only difference is that this Map will be created lazily at first write, thus reducing memory penalty if possible.
 * <p>
 * This class can be used as superclass for objects that have attributes created at runtime.
 * If generic runtime attributes will always be used it may be a good idea to use {@link GenericPersistentObject} instead.
 * If you don't create attributes at runtime it is better to use {@link PersistentObject} class.
 * <p>
 * Map creation is not thread safe, as PersistentObject in general not thread safe by its own.
 *
 * @see PersistentObject
 * @see GenericPersistentObject
 *
 * @since 4.1
 * @since 5.0, renamed from HybridDataObject
 */
public class HybridPersistentObject extends PersistentObject {

    private static final long serialVersionUID = 1945209973678806566L;

    protected Map<String, Object> values;

    @Override
    Object readSimpleProperty(String property) {

        // side effect - resolves HOLLOW object
        Object object = readProperty(property);

        // if a null value is returned, there is still a chance to
        // find a non-persistent property via reflection
        if (object == null && values != null && !values.containsKey(property)) {
            object = PropertyUtils.getProperty(this, property);
        }

        return object;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(values == null) {
            return null;
        }
        return values.get(propName);
    }

    @Override
    public void writePropertyDirectly(String propName, Object val) {
        if(values == null) {
            values = new HashMap<>();
        }
        values.put(propName, val);
    }

    @Override
    protected void appendProperties(StringBuffer buffer) {
        buffer.append('[');
        if(values == null) {
            buffer.append(']');
            return;
        }

        Iterator<Map.Entry<String, Object>> it = values.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();

            buffer.append(entry.getKey()).append("=>");
            Object value = entry.getValue();

            if (value instanceof Persistent) {
                buffer.append('{').append(((Persistent) value).getObjectId()).append('}');
            } else if (value instanceof Collection) {
                buffer.append("(..)");
            } else if (value instanceof Fault) {
                buffer.append('?');
            } else {
                buffer.append(value);
            }

            if (it.hasNext()) {
                buffer.append("; ");
            }
        }

        buffer.append(']');
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        values = (Map<String, Object>) in.readObject();
    }

    @Override
    protected void writeState(ObjectOutputStream out) throws IOException {
        super.writeState(out);
        out.writeObject(values);
    }

}
