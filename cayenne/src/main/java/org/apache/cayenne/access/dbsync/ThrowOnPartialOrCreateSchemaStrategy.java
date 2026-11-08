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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.map.compiled.CompiledSchema;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.sql.DataSource;
import java.util.Collection;

/**
 * @since 3.0
 */
public class ThrowOnPartialOrCreateSchemaStrategy extends ThrowOnPartialSchemaStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(ThrowOnPartialOrCreateSchemaStrategy.class);

    protected final SQLLogger sqlLogger;

    public ThrowOnPartialOrCreateSchemaStrategy(@Inject SQLLogger sqlLogger) {
        this.sqlLogger = sqlLogger;
    }

    @Override
    public void updateSchema(DbAdapter adapter, DataSource dataSource, CompiledSchema schema, String... dataMaps) {

        Collection<CompiledTable> tables = tablesOf(schema, dataMaps);
        SchemaAnalyzer analyzer = analyze(dataSource, tables);

        if (analyzer.getTableNoInDB().isEmpty() && analyzer.getErrorMessage() == null) {
            LOGGER.info("Full schema is present");
        } else if (analyzer.getTableNoInDB().size() == tables.size()) {
            LOGGER.info("No schema detected, will create mapped tables");
            generate(adapter, dataSource, schema, dataMaps);
        } else {
            LOGGER.info("Error - partial schema detected");
            throw new CayenneRuntimeException(mismatchMessage(analyzer, tables.size()));
        }
    }

    private void generate(DbAdapter adapter, DataSource dataSource, CompiledSchema schema, String... dataMaps) {
        for (String map : dataMaps) {
            DbGenerator gen = new DbGenerator(adapter, schema, map, List.of(), List.of(dataMaps), sqlLogger);
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
