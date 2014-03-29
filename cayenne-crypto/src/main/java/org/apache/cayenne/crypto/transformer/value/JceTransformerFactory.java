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
package org.apache.cayenne.crypto.transformer.value;

import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

/**
 * A {@link ValueTransformerFactory} that creates encryptors/decryptors that are
 * taking advantage of the JCE (Java Cryptography Extension) ciphers.
 * 
 * @since 3.2
 */
public class JceTransformerFactory implements ValueTransformerFactory {

    private Map<String, ToBytesConverter> toBytesConverters;
    private Map<Integer, FromBytesConverter> fromBytesConverters;

    private ConcurrentMap<DbAttribute, ValueTransformer> encryptors;

    public JceTransformerFactory() {
        this.toBytesConverters = createToBytesConverters();
        this.fromBytesConverters = createFromBytesConverters();

        this.encryptors = new ConcurrentHashMap<DbAttribute, ValueTransformer>();
    }

    @Override
    public ValueTransformer decryptor(DbAttribute a) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ValueTransformer encryptor(DbAttribute a) {
        ValueTransformer e = encryptors.get(a);

        if (e == null) {

            ValueTransformer newTransformer = createEncryptor(a);
            ValueTransformer oldTransformer = encryptors.putIfAbsent(a, newTransformer);

            e = oldTransformer != null ? oldTransformer : newTransformer;
        }

        return e;
    }

    protected Map<String, ToBytesConverter> createToBytesConverters() {
        Map<String, ToBytesConverter> map = new HashMap<String, ToBytesConverter>();

        map.put("byte[]", BytesToBytesConverter.INSTANCE);
        map.put(String.class.getName(), StringToBytesConverter.INSTANCE);

        return map;
    }

    protected Map<Integer, FromBytesConverter> createFromBytesConverters() {
        Map<Integer, FromBytesConverter> map = new HashMap<Integer, FromBytesConverter>();

        map.put(Types.BINARY, BytesToBytesConverter.INSTANCE);
        map.put(Types.BLOB, BytesToBytesConverter.INSTANCE);
        map.put(Types.VARBINARY, BytesToBytesConverter.INSTANCE);
        map.put(Types.LONGVARBINARY, BytesToBytesConverter.INSTANCE);

        map.put(Types.CHAR, Base64FromBytesConverter.INSTANCE);
        map.put(Types.CLOB, Base64FromBytesConverter.INSTANCE);
        map.put(Types.LONGNVARCHAR, Base64FromBytesConverter.INSTANCE);
        map.put(Types.VARCHAR, Base64FromBytesConverter.INSTANCE);

        return map;
    }

    protected ValueTransformer createEncryptor(DbAttribute a) {

        String type = getJavaType(a);

        ToBytesConverter toBytes = toBytesConverters.get(type);
        if (toBytes == null) {
            throw new IllegalArgumentException("The type " + type + " for attribute " + a
                    + " has no to-bytes conversion");
        }

        FromBytesConverter fromBytes = fromBytesConverters.get(a.getType());
        if (fromBytes == null) {
            throw new IllegalArgumentException("The type " + type + " for attribute " + a
                    + " has no from-bytes conversion");
        }

        return new JceValueEncryptor(toBytes, fromBytes);
    }

    // TODO: calculating Java type of ObjAttribute may become unneeded per
    // CAY-1752, as DbAttribute will have it.
    protected String getJavaType(DbAttribute a) {

        DbEntity dbEntity = a.getEntity();
        DataMap dataMap = dbEntity.getDataMap();
        Collection<ObjEntity> objEntities = dataMap.getMappedEntities(dbEntity);

        if (objEntities.size() != 1) {
            return TypesMapping.getJavaBySqlType(a.getType());
        }

        Collection<String> javaTypes = new HashSet<String>();
        ObjEntity objEntity = objEntities.iterator().next();
        for (ObjAttribute oa : objEntity.getAttributes()) {

            // TODO: this won't pick up flattened attributes
            if (a.getName().equals(oa.getDbAttributePath())) {
                javaTypes.add(oa.getType());
            }
        }

        if (javaTypes.size() != 1) {
            return TypesMapping.getJavaBySqlType(a.getType());
        }

        return javaTypes.iterator().next();
    }

}
