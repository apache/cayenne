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

package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class DbKeyGeneratorHandler extends NamespaceAwareNestedTagHandler {

    private static final String DB_KEY_GENERATOR_TAG = "db-key-generator";
    private static final String DB_GENERATOR_TYPE_TAG = "db-generator-type";
    private static final String DB_GENERATOR_NAME_TAG = "db-generator-name";
    private static final String DB_KEY_CACHE_SIZE_TAG = "db-key-cache-size";

    DbEntity entity;

    public DbKeyGeneratorHandler(NamespaceAwareNestedTagHandler parentHandler, DbEntity entity) {
        super(parentHandler);
        this.entity = entity;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case DB_KEY_GENERATOR_TAG:
                createDbKeyGenerator();
                return true;

            case DB_GENERATOR_NAME_TAG:
            case DB_GENERATOR_TYPE_TAG:
            case DB_KEY_CACHE_SIZE_TAG:
                return true;
        }

        return false;
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case DB_GENERATOR_TYPE_TAG:
                setDbGeneratorType(data);
                break;

            case DB_GENERATOR_NAME_TAG:
                setDbGeneratorName(data);
                break;

            case DB_KEY_CACHE_SIZE_TAG:
                setDbKeyCacheSize(data);
                break;
        }
        return true;
    }

    private void createDbKeyGenerator() {
        entity.setPrimaryKeyGenerator(new DbKeyGenerator());
    }

    private void setDbGeneratorType(String type) {
        if (entity == null) {
            return;
        }
        DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
        pkGenerator.setGeneratorType(type);
        if (pkGenerator.getGeneratorType() == null) {
            entity.setPrimaryKeyGenerator(null);
        }
    }

    private void setDbGeneratorName(String name) {
        if (entity == null) {
            return;
        }
        DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
        if (pkGenerator == null) {
            return;
        }
        pkGenerator.setGeneratorName(name);
    }

    private void setDbKeyCacheSize(String size) {
        if (entity == null) {
            return;
        }
        DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
        if (pkGenerator == null) {
            return;
        }
        try {
            pkGenerator.setKeyCacheSize(Integer.valueOf(size.trim()));
        } catch (Exception ex) {
            pkGenerator.setKeyCacheSize(null);
        }
    }
}
