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


package org.apache.cayenne.map;

import java.io.Serializable;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * DbKeyGenerator is an abstraction of a primary key generator It configures the primary
 * key generation per DbEntity in a RDBMS independent manner. DbAdapter generates actual
 * key values based on the configuration. For more details see data-map.dtd
 * 
 */

public class DbKeyGenerator implements CayenneMapEntry, XMLSerializable, Serializable {

    public static final String ORACLE_TYPE = "ORACLE";
    public static final String NAMED_SEQUENCE_TABLE_TYPE = "NAMED_SEQUENCE_TABLE";

    protected String name;
    protected DbEntity dbEntity;
    protected String generatorType;
    protected Integer keyCacheSize;
    protected String generatorName;

    public DbKeyGenerator() {
    }

    public DbKeyGenerator(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getParent() {
        return getDbEntity();
    }

    public void setParent(Object parent) {
        if (parent != null && !(parent instanceof DbEntity)) {
            throw new IllegalArgumentException("Expected null or DbEntity, got: "
                    + parent);
        }

        setDbEntity((DbEntity) parent);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        if (getGeneratorType() == null) {
            return;
        }

        encoder.start("db-key-generator")
                .start("db-generator-type").cdata(getGeneratorType()).end();

        if (getGeneratorName() != null) {
            encoder.start("db-generator-name").cdata(getGeneratorName()).end();
        }
        if (getKeyCacheSize() != null) {
            encoder.start("db-key-cache-size").cdata(String.valueOf(getKeyCacheSize())).end();
        }
        encoder.end();
    }

    public DbEntity getDbEntity() {
        return dbEntity;
    }

    public void setDbEntity(DbEntity dbEntity) {
        this.dbEntity = dbEntity;
    }

    public void setGeneratorType(String generatorType) {
        this.generatorType = generatorType;
        if (this.generatorType != null) {
            this.generatorType = this.generatorType.trim().toUpperCase();
            if (!(ORACLE_TYPE.equals(this.generatorType)
                    || NAMED_SEQUENCE_TABLE_TYPE.equals(this.generatorType))) {
                this.generatorType = null;
            }
        }
    }

    public String getGeneratorType() {
        return generatorType;
    }

    public void setKeyCacheSize(Integer keyCacheSize) {
        this.keyCacheSize = keyCacheSize;
        if (this.keyCacheSize != null && this.keyCacheSize < 1) {
            this.keyCacheSize = null;
        }
    }

    public Integer getKeyCacheSize() {
        return keyCacheSize;
    }

    public void setGeneratorName(String generatorName) {
        this.generatorName = generatorName;
        if (this.generatorName != null) {
            this.generatorName = this.generatorName.trim();
            if (this.generatorName.length() == 0) {
                this.generatorName = null;
            }
        }
    }

    public String getGeneratorName() {
        return generatorName;
    }

    @Override
    public String toString() {
        return "{Type="
                + generatorType
                + ", Name="
                + generatorName
                + ", Cache="
                + keyCacheSize
                + "}";
    }
}
