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

package org.apache.cayenne.access.translator.select;

import java.sql.Types;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class BaseColumnExtractorTest {

    DbEntity createMockDbEntity(String entityName) {
        DbEntity entity = new DbEntity(entityName);
        DbAttribute id = new DbAttribute("id");
        id.setPrimaryKey(true);
        id.setType(Types.BIGINT);
        DbAttribute name = new DbAttribute("name");
        name.setType(Types.VARBINARY);
        entity.addAttribute(id);
        entity.addAttribute(name);
        return entity;
    }
}
