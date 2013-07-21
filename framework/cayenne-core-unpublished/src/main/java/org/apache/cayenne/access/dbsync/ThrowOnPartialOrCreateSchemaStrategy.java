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
package org.apache.cayenne.access.dbsync;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 3.0
 */
public class ThrowOnPartialOrCreateSchemaStrategy extends ThrowOnPartialSchemaStrategy {

    final static Log logger = LogFactory
            .getLog(ThrowOnPartialOrCreateSchemaStrategy.class);

    @Override
    protected void processSchemaUpdate(
            DataNode dataNode,
            List<String> mergerOnlyTable,
            String errorMessage,
            int entitiesSize) throws SQLException {

        if (mergerOnlyTable.size() == 0 && errorMessage == null) {
            logger.info("Full schema is present");
        }
        else if (mergerOnlyTable.size() == entitiesSize) {
            logger.info("No schema detected, will create mapped tables");
            generate(dataNode);
        }
        else {
            logger.info("Error - partial schema detected");

            StringBuilder buffer = new StringBuilder("Schema mismatch detected");

            if (errorMessage != null) {
                buffer.append(": ").append(errorMessage);
            }
            else {
                if (mergerOnlyTable.size() > 0) {
                    buffer
                            .append(": missing table '")
                            .append(mergerOnlyTable.get(0))
                            .append('\'');
                }
            }

            throw new CayenneRuntimeException(buffer.toString());
        }
    }

    private void generate(DataNode dataNode) {
        Collection<DataMap> map = dataNode.getDataMaps();
        Iterator<DataMap> iterator = map.iterator();
        while (iterator.hasNext()) {
            DbGenerator gen = new DbGenerator(dataNode.getAdapter(), iterator.next(), 
                    dataNode.getJdbcEventLogger());
            gen.setShouldCreateTables(true);
            gen.setShouldDropTables(false);
            gen.setShouldCreateFKConstraints(false);
            gen.setShouldCreatePKSupport(false);
            gen.setShouldDropPKSupport(false);
            try {
                gen.runGenerator(dataNode.getDataSource());
            }
            catch (Exception e) {
                throw new CayenneRuntimeException(e);
            }
        }
    }

}
