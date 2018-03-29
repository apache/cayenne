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
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.IDUtil;
import org.apache.cayenne.util.Util;

/**
 * An object to encode/decode ObjectIds for a single mapped entity.
 * 
 * @since 3.1
 */
public class EntityIdCoder {

    static final String ID_SEPARATOR = ":";
    static final String TEMP_ID_PREFIX = ".";
    private static final int TEMP_PREFIX_LENGTH = TEMP_ID_PREFIX.length();

    private String entityName;
    private Map<String, Converter> converters;
    private int idSize;

    public static String getEntityName(String id) {
        int separator = id.indexOf(ID_SEPARATOR);
        if (separator <= 0 || separator == id.length() - 1) {
            throw new IllegalArgumentException("Invalid String id: " + id);
        }

        String name = id.substring(0, separator);

        if (name.startsWith(TEMP_ID_PREFIX)) {
            name = name.substring(TEMP_PREFIX_LENGTH);
        }

        return name;
    }

    public EntityIdCoder(ObjEntity entity) {

        this.entityName = entity.getName();
        this.converters = new HashMap<>();

        for (ObjAttribute attribute : entity.getAttributes()) {
            if (attribute.isPrimaryKey()) {
                converters.put(attribute.getDbAttributeName(),
                        create(attribute.getJavaClass()));
            }
        }

        for (DbAttribute attribute : entity.getDbEntity().getPrimaryKeys()) {
            if (!converters.containsKey(attribute.getName())) {
                String type = TypesMapping
                        .getJavaBySqlType(attribute.getType());
                try {
                    converters.put(attribute.getName(),
                            create(Util.getJavaClass(type)));
                } catch (ClassNotFoundException e) {
                    throw new CayenneRuntimeException(
                            "Can't instantiate class " + type, e);
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

        // deal with temp that have attached replacement ID as permanent IDs...
        // AuditableFilter, etc. all rely on the ability to find the temp object
        // after the transaction end

        // TODO: support encoding format for temp+replacement
        if (id.isTemporary() && !id.isReplacementIdAttached()) {
            return toTempIdString(id);
        } else {
            return toPermIdString(id);
        }
    }

    private String toTempIdString(ObjectId id) {
        StringBuilder buffer = new StringBuilder();

        buffer.append(TEMP_ID_PREFIX);

        buffer.append(id.getEntityName());

        buffer.append(ID_SEPARATOR);

        for (byte b : id.getKey()) {
            IDUtil.appendFormattedByte(buffer, b);
        }
        return buffer.toString();
    }

    private String toPermIdString(ObjectId id) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(id.getEntityName());
        Map<String, Object> idValues = id.getIdSnapshot();

        for (Entry<String, Converter> entry : converters.entrySet()) {
            Object value = idValues.get(entry.getKey());
            buffer.append(ID_SEPARATOR).append(entry.getValue().toUuid(value));
        }

        return buffer.toString();
    }

    public ObjectId toObjectId(String stringId) {

        if (stringId.startsWith(TEMP_ID_PREFIX)) {
            String idValues = stringId.substring(entityName.length() + 1
                    + TEMP_PREFIX_LENGTH);
            return new ObjectId(entityName, decodeTemp(idValues));
        }

        String idValues = stringId.substring(entityName.length() + 1);

        if (converters.size() == 1) {
            Entry<String, Converter> entry = converters.entrySet().iterator()
                    .next();

            String decoded;
            try {
                decoded = URLDecoder.decode(idValues, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // unexpected
                throw new CayenneRuntimeException("Unsupported encoding", e);
            }
            return new ObjectId(entityName, entry.getKey(), entry.getValue()
                    .fromStringId(decoded));
        }

        Map<String, Object> idMap = new HashMap<>(idSize);
        StringTokenizer toks = new StringTokenizer(idValues, ID_SEPARATOR);

        if (toks.countTokens() != converters.size()) {
            throw new IllegalArgumentException("Invalid String ID for entity "
                    + entityName + ": " + idValues);
        }

        for (Entry<String, Converter> entry : converters.entrySet()) {
            String value = toks.nextToken();

            String decoded;
            try {
                decoded = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // unexpected
                throw new CayenneRuntimeException("Unsupported encoding", e);
            }

            idMap.put(entry.getKey(), entry.getValue().fromStringId(decoded));
        }

        return new ObjectId(entityName, idMap);
    }

    private byte[] decodeTemp(String byteString) {

        byte[] bytes = new byte[byteString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;

            // this is better than Byte.parseByte which can't parse values >=
            // 128 as negative bytes
            int c1 = byteString.charAt(index);
            int c2 = byteString.charAt(index + 1);
            bytes[i] = (byte) ((Character.digit(c1, 16) << 4) + Character
                    .digit(c2, 16));
        }

        return bytes;
    }

    private Converter create(Class<?> type) {

        if (type == null) {
            throw new NullPointerException("Null type");
        }

        if (Long.class.isAssignableFrom(type)) {
            return new Converter() {

                @Override
                Object fromStringId(String stringId) {
                    return Long.valueOf(stringId);
                }
            };
        } else if (Integer.class.isAssignableFrom(type)) {
            return new Converter() {

                @Override
                Object fromStringId(String stringId) {
                    return Integer.valueOf(stringId);
                }
            };
        } else if (String.class.isAssignableFrom(type)) {
            return new Converter() {

                @Override
                Object fromStringId(String stringId) {
                    return stringId;
                }
            };
        }

        throw new IllegalArgumentException("Unsupported ID type: "
                + type.getName());
    }

    abstract class Converter {

        String toUuid(Object value) {
            try {
                return URLEncoder.encode(String.valueOf(value), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // unexpected
                throw new CayenneRuntimeException("Unsupported encoding", e);
            }
        }

        abstract Object fromStringId(String stringId);
    }
}
