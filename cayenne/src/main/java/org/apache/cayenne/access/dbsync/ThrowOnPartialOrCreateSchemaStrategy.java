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
package org.apache.cayenne.access.dbsync;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Collection;

/**
 * @since 3.0
 */
public class ThrowOnPartialOrCreateSchemaStrategy extends ThrowOnPartialSchemaStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(ThrowOnPartialOrCreateSchemaStrategy.class);

    @Override
    public void updateSchema(DbAdapter adapter, DataSource dataSource, Collection<DataMap> dataMaps, SQLLogger logger) {

        Collection<DbEntity> entities = dbEntities(dataMaps);
        SchemaAnalyzer analyzer = analyze(dataSource, entities);

        if (analyzer.getTableNoInDB().isEmpty() && analyzer.getErrorMessage() == null) {
            LOGGER.info("Full schema is present");
        } else if (analyzer.getTableNoInDB().size() == entities.size()) {
            LOGGER.info("No schema detected, will create mapped tables");
            generate(adapter, dataSource, dataMaps, logger);
        } else {
            LOGGER.info("Error - partial schema detected");
            throw new CayenneRuntimeException(mismatchMessage(analyzer, entities.size()));
        }
    }

    private void generate(DbAdapter adapter, DataSource dataSource, Collection<DataMap> dataMaps, SQLLogger logger) {
        for (DataMap map : dataMaps) {
            DbGenerator gen = new DbGenerator(adapter, map, logger);
            gen.setShouldCreateTables(true);
            gen.setShouldDropTables(false);
            gen.setShouldCreateFKConstraints(false);
            gen.setShouldCreatePKSupport(false);
            gen.setShouldDropPKSupport(false);
            try {
                gen.runGenerator(dataSource);
            } catch (Exception e) {
                throw new CayenneRuntimeException(e);
            }
        }
    }
}
