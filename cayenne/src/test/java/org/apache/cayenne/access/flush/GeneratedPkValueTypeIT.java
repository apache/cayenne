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
package org.apache.cayenne.access.flush;

import org.apache.cayenne.CayenneRuntime;
import org.apache.cayenne.CoreModule;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.SQLQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Checks that a generated primary key is converted to the Java type of the ObjAttribute mapped to it, when that
 * type is a value object type.
 */
public class GeneratedPkValueTypeIT {

    public record PkWrapper(long value) {
    }

    // converts from Long, while the PK column below is an INTEGER, so the generated key must be widened before
    // the conversion
    public static class PkWrapperType implements ValueObjectType<PkWrapper, Long> {

        @Override
        public Class<Long> getTargetType() {
            return Long.class;
        }

        @Override
        public Class<PkWrapper> getValueType() {
            return PkWrapper.class;
        }

        @Override
        public PkWrapper toJavaObject(Long value) {
            return new PkWrapper(value);
        }

        @Override
        public Long fromJavaObject(PkWrapper object) {
            return object.value();
        }

        @Override
        public String toCacheKey(PkWrapper object) {
            return String.valueOf(object.value());
        }
    }

    private CayenneRuntime runtime;

    @AfterEach
    public void stopRuntime() {
        if (runtime != null) {
            runtime.shutdown();
        }
    }

    @Test
    public void generatedPkConvertedToAttributeType() throws Exception {

        // a dedicated DB, with a config-free runtime and a programmatic mapping
        DataSource dataSource = CayenneDataSource.of("jdbc:hsqldb:mem:generated_pk_value_type")
                .driverClass("org.hsqldb.jdbcDriver")
                .userName("sa")
                .build();
        runtime = CayenneRuntime.of()
                .defaultDataNode(dataSource)
                .addModule(b -> CoreModule.extend(b).addValueObjectType(PkWrapperType.class))
                .build();

        DbEntity dbEntity = new DbEntity("VO_PK");
        DbAttribute pk = new DbAttribute("ID", Types.INTEGER, dbEntity);
        pk.setPrimaryKey(true);
        pk.setMandatory(true);
        dbEntity.addAttribute(pk);

        // a meaningful PK: the ObjAttribute of the wrapper type is mapped to the PK column
        ObjEntity objEntity = new ObjEntity("VoPk");
        objEntity.setDbEntity(dbEntity);
        ObjAttribute id = new ObjAttribute("id", PkWrapper.class.getName(), objEntity);
        id.setDbAttributePath("ID");
        objEntity.addAttribute(id);

        DataMap map = new DataMap("vo_pk");
        map.addDbEntity(dbEntity);
        map.addObjEntity(objEntity);

        DataDomain domain = runtime.getDataDomain();
        domain.addDataMap(map);

        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE VO_PK (ID INTEGER NOT NULL PRIMARY KEY)");
        }
        DataNode node = domain.getDefaultNode();
        node.getPkGenerator().createAutoPk(node, List.of(dbEntity));

        ObjectContext context = runtime.newContext();
        Persistent object = context.newObject("VoPk");
        context.commitChanges();

        Object generated = object.getObjectId().getIdSnapshot().get("ID");
        PkWrapper wrapper = assertInstanceOf(PkWrapper.class, generated);

        Integer stored = SQLQuery.scalarQuery("SELECT ID FROM VO_PK", Integer.class).selectOne(context);
        assertEquals(wrapper.value(), stored.longValue());
    }
}
