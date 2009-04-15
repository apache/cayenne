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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.merge.DbMerger;
import org.apache.cayenne.merge.MergerToken;

/**
 * @since 3.0
 */
public class ThrowOnPartialSchemaStrategy extends BaseSchemaUpdateStrategy {

    @Override
    public BaseSchemaUpdateStrategy getSchema() {
        return currentSchema;
    }

    /**
     * @since 3.0
     */
    public ThrowOnPartialSchemaStrategy() {
        currentSchema = this;
    }

    /**
     * @throws SQLException 
     * @since 3.0
     */
    public void updateSchema(DataNode dataNode) throws SQLException {
        super.generateUpdateSchema(dataNode);
    }

    /**
     * @since 3.0
     */
    @Override
    public void generateUpdateSchema(DataNode dataNode) {
        String errorMessage = null;
        List<String> mergerOnlyTable = new ArrayList<String>();
        DbMerger merger = new DbMerger();
        Collection<String> entityNames = new ArrayList<String>();
        Collection<DbEntity> entities = dataNode.getEntityResolver().getDbEntities();
        int entitiesSize = entities.size();
        Iterator<DbEntity> entitiesIterator = entities.iterator();
        while (entitiesIterator.hasNext()) {
            entityNames.add(entitiesIterator.next().getName());
        }

        Collection<DataMap> map = dataNode.getDataMaps();
        Iterator<DataMap> iterator = map.iterator();
        while (iterator.hasNext()) {
            List<MergerToken> mergerTokens = merger.createMergeTokens(dataNode
                    .getAdapter(), dataNode.getDataSource(), iterator.next());
            Iterator<MergerToken> tokensIt = mergerTokens.iterator();
            while (tokensIt.hasNext()) {
                MergerToken token = tokensIt.next();
                if (entityNames.contains(token.getTokenValue())
                        && !token.getTokenName().equals("Drop Table")) {
                    if (token.getTokenName().equals("Create Table")) {
                        mergerOnlyTable.add(token.getTokenValue());
                    }
                    else {
                        errorMessage = token.getTokenName()
                                + " in table "
                                + token.getTokenValue();
                        break;
                    }
                }
            }
        }
        analyser(dataNode, mergerOnlyTable, errorMessage, entitiesSize);
    }

    protected synchronized void analyser(
            DataNode dataNode,
            List<String> mergerOnlyTable,
            String errorMessage,
            int entitiesSize) {

        if (mergerOnlyTable.size() == 0 && errorMessage == null) {
        }
        else {
            String err = "Parser schema detected: ";
            if (errorMessage != null) {
                err += errorMessage;
            }
            else if (mergerOnlyTable.size() == entitiesSize) {
                err += "no schema in database";
            }
            else {
                if (mergerOnlyTable.size() > 0) {
                    err += "expect table " + mergerOnlyTable.get(0);
                }
            }
            throw new CayenneRuntimeException(err);
        }
    }
}
