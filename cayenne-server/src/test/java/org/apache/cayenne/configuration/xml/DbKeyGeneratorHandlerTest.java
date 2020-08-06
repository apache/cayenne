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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
public class DbKeyGeneratorHandlerTest extends BaseHandlerTest {

    @Test
    public void testParsing()throws Exception {
        final DbEntity dbEntity = new DbEntity("TEST");

        parse("db-key-generator", new HandlerFactory() {
            @Override
            public NamespaceAwareNestedTagHandler createHandler(NamespaceAwareNestedTagHandler parent) {
                return new DbKeyGeneratorHandler(parent, dbEntity);
            }
        });

        assertNotNull(dbEntity.getPrimaryKeyGenerator());
        assertEquals("gallery_seq", dbEntity.getPrimaryKeyGenerator().getGeneratorName());
        assertEquals(20, (int)dbEntity.getPrimaryKeyGenerator().getKeyCacheSize());
        assertEquals(DbKeyGenerator.ORACLE_TYPE, dbEntity.getPrimaryKeyGenerator().getGeneratorType());
    }


}