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
package org.apache.cayenne.access;

import org.apache.cayenne.CoreModule;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLQuery;
import org.apache.cayenne.testdo.extended_type.ExtendedTypeEntity;
import org.apache.cayenne.testdo.extended_type.StringET1;
import org.apache.cayenne.testdo.extended_type.StringET1ExtendedType;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.DataRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataContextExtendedTypeOperationsIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv
            .forProject(CayenneProjects.EXTENDED_TYPE_PROJECT)
            .withExtraModules(b ->  CoreModule.extend(b).addUserExtendedType(StringET1ExtendedType.class));

    @Test
    public void storeExtendedType() {
        ExtendedTypeEntity e1 = env.context().newObject(ExtendedTypeEntity.class);
        e1.setName(new StringET1("X"));
        env.context().commitChanges();

        SQLQuery<DataRow> checkQ = SQLQuery.query(
                ExtendedTypeEntity.class,
                "SELECT * FROM EXTENDED_TYPE_TEST WHERE NAME = 'X'").fetchDataRows();
        checkQ.columnNameCaps(CapsStrategy.UPPER);
        assertEquals(1, e1.getObjectContext().select(checkQ).size());
    }

    @Test
    public void inExpressionExtendedTypeArray() {
        ExtendedTypeEntity e1 = env.context().newObject(ExtendedTypeEntity.class);
        e1.setName(new StringET1("X"));

        ExtendedTypeEntity e2 = e1.getObjectContext().newObject(ExtendedTypeEntity.class);
        e2.setName(new StringET1("Y"));

        ExtendedTypeEntity e3 = e1.getObjectContext().newObject(ExtendedTypeEntity.class);
        e3.setName(new StringET1("Z"));

        env.context().commitChanges();

        ObjectSelect<ExtendedTypeEntity> query = ObjectSelect.query(ExtendedTypeEntity.class)
                .where(ExtendedTypeEntity.NAME.in(new StringET1("X"), new StringET1("Y")));
        assertEquals(2, e1.getObjectContext().select(query).size());
    }

    @Test
    public void inExpressionExtendedTypeList() {
        ExtendedTypeEntity e1 = env.context().newObject(ExtendedTypeEntity.class);
        e1.setName(new StringET1("X"));

        ExtendedTypeEntity e2 = e1.getObjectContext().newObject(ExtendedTypeEntity.class);
        e2.setName(new StringET1("Y"));

        ExtendedTypeEntity e3 = e1.getObjectContext().newObject(ExtendedTypeEntity.class);
        e3.setName(new StringET1("Z"));

        e1.getObjectContext().commitChanges();

        ObjectSelect<ExtendedTypeEntity> query = ObjectSelect.query(ExtendedTypeEntity.class)
                .where(ExtendedTypeEntity.NAME.in(Arrays.asList(new StringET1("X"), new StringET1("Y"))));
        assertEquals(2, e1.getObjectContext().select(query).size());
    }
}
