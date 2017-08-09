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
package org.apache.cayenne.dbsync.merge.builders;

import org.apache.cayenne.datafactory.DictionaryValueProvider;
import org.apache.cayenne.datafactory.ValueProvider;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.util.Util;

/**
 * @since 4.0.
 */
public class DbAttributeBuilder extends DefaultBuilder<DbAttribute> {

    private static final ValueProvider<String> TYPES_RANDOM = new DictionaryValueProvider<String>(ValueProvider.RANDOM) {
        @Override
        protected String[] values() {
            return TypesMapping.getDatabaseTypes();
        }
    };

    public DbAttributeBuilder() {
        super(new DbAttribute());
    }

    public DbAttributeBuilder name() {
        return name(getRandomJavaName());
    }

    public DbAttributeBuilder name(String name) {
        obj.setName(name);

        return this;
    }

    public DbAttributeBuilder type() {
        return type(TYPES_RANDOM.randomValue());
    }

    public DbAttributeBuilder type(String item) {
        obj.setType(TypesMapping.getSqlTypeByName(item));

        return this;
    }

    public DbAttributeBuilder typeInt() {
        return type(TypesMapping.SQL_INTEGER);
    }

    public DbAttributeBuilder typeBigInt() {
        return type(TypesMapping.SQL_BIGINT);
    }

    public DbAttributeBuilder typeVarchar(int length) {
        type(TypesMapping.SQL_VARCHAR);
        length(length);

        return this;
    }

    private DbAttributeBuilder length(int length) {
        obj.setMaxLength(length);

        return this;
    }

    public DbAttributeBuilder primaryKey() {
        obj.setPrimaryKey(true);

        return this;
    }

    public DbAttributeBuilder mandatory() {
        obj.setMandatory(true);

        return this;
    }

    public DbAttributeBuilder generated() {
        obj.setGenerated(true);
        return this;
    }

    @Override
    public DbAttribute build() {
        if (Util.isEmptyString(obj.getName())) {
            name();
        }

        if (obj.getType() == TypesMapping.NOT_DEFINED) {
            type();
        }

        return obj;
    }

    @Override
    public DbAttribute random() {
        return build();
    }

}
