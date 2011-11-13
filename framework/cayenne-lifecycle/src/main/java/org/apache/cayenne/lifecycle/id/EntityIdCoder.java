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
package org.apache.cayenne.lifecycle.id;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.Util;

/**
 * An object to encode/decode ObjectIds for a single mapped entity.
 * 
 * @since 3.1
 */
public class EntityIdCoder {

    static final String UUID_SEPARATOR = ":";

    private String entityName;
    private SortedMap<String, Converter> converters;
    private int idSize;

    public static String getEntityName(String id) {
        int separator = id.indexOf(UUID_SEPARATOR);
        if (separator <= 0 || separator == id.length() - 1) {
            throw new IllegalArgumentException("Invalid uuid: " + id);
        }

        return id.substring(0, separator);
    }

    public EntityIdCoder(ObjEntity entity) {

        this.entityName = entity.getName();
        this.converters = new TreeMap<String, Converter>();

        for (ObjAttribute attribute : entity.getAttributes()) {
            if (attribute.isPrimaryKey()) {
                converters.put(
                        attribute.getDbAttributeName(),
                        create(attribute.getJavaClass()));
            }
        }

        for (DbAttribute attribute : entity.getDbEntity().getPrimaryKeys()) {
            if (!converters.containsKey(attribute.getName())) {
                String type = TypesMapping.getJavaBySqlType(attribute.getType());
                try {
                    converters.put(attribute.getName(), create(Util.getJavaClass(type)));
                }
                catch (ClassNotFoundException e) {
                    throw new CayenneRuntimeException(
                            "Can't instantiate class " + type,
                            e);
                }
            }
        }

        if (converters.isEmpty()) {
            throw new IllegalArgumentException("Entity has no PK definied: "
                    + entity.getName());
        }

        this.idSize = (int) Math.ceil(converters.size() / 0.75d);
    }

    /**
     * Returns a consistent String representation of the ObjectId
     */
    public String toStringId(ObjectId id) {

        if (id.isTemporary() && !id.isReplacementIdAttached()) {
            throw new IllegalArgumentException(
                    "Can't create UUID for a temporary ObjectId");
        }

        Map<String, Object> idValues = id.getIdSnapshot();

        StringBuilder buffer = new StringBuilder();
        buffer.append(id.getEntityName());

        for (Entry<String, Converter> entry : converters.entrySet()) {
            Object value = idValues.get(entry.getKey());
            buffer.append(UUID_SEPARATOR).append(entry.getValue().toUuid(value));
        }

        return buffer.toString();
    }

    public ObjectId toObjectId(String uuid) {

        String uuidValues = uuid.substring(entityName.length() + 1);

        if (converters.size() == 1) {
            Entry<String, Converter> entry = converters.entrySet().iterator().next();

            String decoded;
            try {
                decoded = URLDecoder.decode(uuidValues, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                // unexpected
                throw new CayenneRuntimeException("Unsupported encoding", e);
            }
            return new ObjectId(entityName, entry.getKey(), entry.getValue().fromUuid(
                    decoded));
        }

        Map<String, Object> idMap = new HashMap<String, Object>(idSize);
        StringTokenizer toks = new StringTokenizer(uuidValues, UUID_SEPARATOR);

        if (toks.countTokens() != converters.size()) {
            throw new IllegalArgumentException("Invalid UUID for entity "
                    + entityName
                    + ": "
                    + uuidValues);
        }

        for (Entry<String, Converter> entry : converters.entrySet()) {
            String value = toks.nextToken();

            String decoded;
            try {
                decoded = URLDecoder.decode(value, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                // unexpected
                throw new CayenneRuntimeException("Unsupported encoding", e);
            }

            idMap.put(entry.getKey(), entry.getValue().fromUuid(decoded));
        }

        return new ObjectId(entityName, idMap);
    }

    private Converter create(Class<?> type) {

        if (type == null) {
            throw new NullPointerException("Null type");
        }

        if (Long.class.isAssignableFrom(type)) {
            return new Converter() {

                @Override
                Object fromUuid(String uuid) {
                    return Long.valueOf(uuid);
                }
            };
        }
        else if (Integer.class.isAssignableFrom(type)) {
            return new Converter() {

                @Override
                Object fromUuid(String uuid) {
                    return Integer.valueOf(uuid);
                }
            };
        }
        else if (String.class.isAssignableFrom(type)) {
            return new Converter() {

                @Override
                Object fromUuid(String uuid) {
                    return uuid;
                }
            };
        }

        throw new IllegalArgumentException("Unsupported UUID type: " + type.getName());
    }

    abstract class Converter {

        String toUuid(Object value) {
            try {
                return URLEncoder.encode(String.valueOf(value), "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                // unexpected
                throw new CayenneRuntimeException("Unsupported encoding", e);
            }
        }

        abstract Object fromUuid(String uuid);
    }
}
