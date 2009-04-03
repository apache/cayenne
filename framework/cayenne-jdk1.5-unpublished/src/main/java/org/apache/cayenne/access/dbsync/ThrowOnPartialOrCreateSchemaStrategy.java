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

    final Log logObj = LogFactory.getLog(ThrowOnPartialOrCreateSchemaStrategy.class);

    /**
     * @since 3.0
     */
    public ThrowOnPartialOrCreateSchemaStrategy() {
        currentSchema = this;
    }

    @Override
    protected synchronized void analyser(
            DataNode dataNode,
            List<String> mergerOnlyTable,
            String errorMessage,
            int entitiesSize) {

        if (mergerOnlyTable.size() == 0 && errorMessage == null) {
        }
        else if (mergerOnlyTable.size() == entitiesSize) {
            generate(dataNode);
        }
        else {
            String err = "Parser schema detected: ";
            if (errorMessage != null) {
                err += errorMessage;
            }

            else {
                if (mergerOnlyTable.size() > 0) {
                    err += "expect table " + mergerOnlyTable.get(0);
                }
            }
            throw new CayenneRuntimeException(err);
        }
    }

    private synchronized void generate(DataNode dataNode) {
        Collection<DataMap> map = dataNode.getDataMaps();
        Iterator<DataMap> iterator = map.iterator();
        while (iterator.hasNext()) {
            DbGenerator gen = new DbGenerator(dataNode.getAdapter(), iterator.next());
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
