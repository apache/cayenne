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
package org.apache.cayenne.crypto.transformer.value;

import org.apache.cayenne.crypto.key.KeySource;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link ValueTransformerFactory} that creates encryptors/decryptors that are
 * taking advantage of the JCE (Java Cryptography Extension) ciphers.
 *
 * @since 4.0
 */
public class DefaultValueTransformerFactory implements ValueTransformerFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultValueTransformerFactory.class);

    public static final String DB_TO_BYTE_CONVERTERS_KEY =
            "org.apache.cayenne.crypto.transformer.value.DefaultValueTransformerFactory.dbToBytes";

    public static final String OBJECT_TO_BYTE_CONVERTERS_KEY =
            "org.apache.cayenne.crypto.transformer.value.DefaultValueTransformerFactory.objectToBytes";

    private final Key defaultKey;

    private final Map<String, BytesConverter<?>> objectToBytes;
    private final Map<Integer, BytesConverter<?>> dbToBytes;

    private final ConcurrentMap<DbAttribute, ValueEncryptor> encryptors;
    private final ConcurrentMap<DbAttribute, ValueDecryptor> decryptors;

    public DefaultValueTransformerFactory(@Inject KeySource keySource,
                      @Inject(DB_TO_BYTE_CONVERTERS_KEY) Map<String, BytesConverter<?>> dbToBytes,
                      @Inject(OBJECT_TO_BYTE_CONVERTERS_KEY) Map<String, BytesConverter<?>> objectToBytes) {

        this.defaultKey = keySource.getKey(keySource.getDefaultKeyAlias());

        this.encryptors = new ConcurrentHashMap<>();
        this.decryptors = new ConcurrentHashMap<>();

        this.objectToBytes = objectToBytes;

        Map<Integer, BytesConverter<?>> m = new HashMap<>();
        for (Map.Entry<String, BytesConverter<?>> extraConverter : dbToBytes.entrySet()) {
            m.put(Integer.valueOf(extraConverter.getKey()), extraConverter.getValue());
        }
        this.dbToBytes = m;
    }

    @Override
    public ValueDecryptor decryptor(DbAttribute a) {
        ValueDecryptor e = decryptors.get(a);

        if (e == null) {

            ValueDecryptor newTransformer = createDecryptor(a);
            ValueDecryptor oldTransformer = decryptors.putIfAbsent(a, newTransformer);

            e = oldTransformer != null ? oldTransformer : newTransformer;
        }

        return e;
    }

    @Override
    public ValueEncryptor encryptor(DbAttribute a) {
        ValueEncryptor e = encryptors.get(a);

        if (e == null) {

            ValueEncryptor newTransformer = createEncryptor(a);
            ValueEncryptor oldTransformer = encryptors.putIfAbsent(a, newTransformer);

            e = oldTransformer != null ? oldTransformer : newTransformer;
        }

        return e;
    }

    protected ValueEncryptor createEncryptor(DbAttribute a) {

        String type = getJavaType(a);

        BytesConverter<?> toBytes = objectToBytes.get(type);
        if (toBytes == null) {
            throw new IllegalArgumentException("The type " + type + " for attribute " + a
                    + " has no object-to-bytes conversion");
        }

        BytesConverter<?> fromBytes = dbToBytes.get(a.getType());
        if (fromBytes == null) {
            throw new IllegalArgumentException("The type " + TypesMapping.getSqlNameByType(a.getType())
                    + " for attribute " + a + " has no bytes-to-db conversion");
        }

        return new DefaultValueEncryptor(toBytes, fromBytes);
    }

    protected ValueDecryptor createDecryptor(DbAttribute a) {

        BytesConverter<?> toBytes = dbToBytes.get(a.getType());
        if (toBytes == null) {
            throw new IllegalArgumentException("The type " + TypesMapping.getSqlNameByType(a.getType())
                    + " for attribute " + a + " has no db-to-bytes conversion");
        }

        String type = getJavaType(a);
        BytesConverter<?> fromBytes = objectToBytes.get(type);
        if (fromBytes == null) {
            throw new IllegalArgumentException("The type " + type + " for attribute " + a
                    + " has no bytes-to-object conversion");
        }

        return new DefaultValueDecryptor(toBytes, fromBytes, defaultKey);
    }

    // TODO: calculating Java type of ObjAttribute may become unneeded per
    // CAY-1752, as DbAttribute will have it.
    protected String getJavaType(DbAttribute a) {

        DbEntity dbEntity = a.getEntity();
        DataMap dataMap = dbEntity.getDataMap();
        Collection<String> javaTypes = new HashSet<>();

        for(ObjEntity objEntity : dataMap.getMappedEntities(dbEntity)) {
            for (ObjAttribute oa : objEntity.getAttributes()) {
                if(oa.getDbAttributePath().length() > 1) {
                    // TODO: this won't pick up flattened attributes
                    continue;
                }
                if (a.getName().equals(oa.getDbAttributePath().first().value())) {
                    javaTypes.add(oa.getType());
                }
            }
        }

        if (javaTypes.size() != 1) {
            String javaType = TypesMapping.getJavaBySqlType(a);
            String attributeName = dbEntity.getName() + "." + a.getName();
            String msg = javaTypes.size() > 1 ? "ObjAttributes with different java types" : "No ObjAttributes";
            // Warn user about this problem as there is nothing else we can do
            logger.warn(msg + " bound to DbAttribute '" + attributeName + "', " + javaType + " type will be used.");
            return javaType;
        }

        return javaTypes.iterator().next();
    }

}
