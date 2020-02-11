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

import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.Util;

/**
 * @since 4.2
 */
class FlushObserver implements OperationObserver {

    private JdbcEventLogger logger;

    FlushObserver(JdbcEventLogger logger) {
        this.logger = logger;
    }

    @Override
    public void nextQueryException(Query query, Exception ex) {
        throw new CayenneRuntimeException("Raising from query exception.", Util.unwindException(ex));
    }

    @Override
    public void nextGlobalException(Exception ex) {
        throw new CayenneRuntimeException("Raising from underlyingQueryEngine exception.", Util.unwindException(ex));
    }

    /**
     * Processes generated keys.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void nextGeneratedRows(Query query, ResultIterator<?> keysIterator, List<ObjectId> idsToUpdate) {

        // read and close the iterator before doing anything else
        List<DataRow> keys;
        try {
            keys = (List<DataRow>) keysIterator.allRows();
        } finally {
            keysIterator.close();
        }

        if (!(query instanceof InsertBatchQuery)) {
            throw new CayenneRuntimeException("Generated keys only supported for InsertBatchQuery, instead got %s", query);
        }

        if (keys.size() != idsToUpdate.size()) {
            throw new CayenneRuntimeException("Mismatching number of generated PKs: expected %d, instead got %d", idsToUpdate.size(), keys.size());
        }
        
        for (int i = 0; i < keys.size(); i++) {
	        DataRow key = keys.get(i);
	
	        // empty key?
	        if (key.size() == 0) {
	            throw new CayenneRuntimeException("Empty key generated.");
	        }
	
	        ObjectId idToUpdate = idsToUpdate.get(i);
	        if (idToUpdate == null || !idToUpdate.isTemporary()) {
	            // why would this happen?
	            return;
	        }

	        BatchQuery batch = (BatchQuery) query;
	        for (DbAttribute attribute : batch.getDbEntity().getGeneratedAttributes()) {
	
	            // batch can have generated attributes that are not PKs, e.g.
	            // columns with
	            // DB DEFAULT values. Ignore those.
	            if (attribute.isPrimaryKey()) {
	            	
	                Object value = key.get(attribute.getName());
	                
	    	        // As of now (01/2005) many tested drivers don't provide decent
	    	        // descriptors of
	    	        // identity result sets, so a data row may contain garbage labels.
	                if (value == null) {
	                	value = key.values().iterator().next();
	                }
	                
	                // Log the generated PK
	                logger.logGeneratedKey(attribute, value);
	
	                // I guess we should override any existing value,
	                // as generated key is the latest thing that exists in the DB.
	                idToUpdate.getReplacementIdMap().put(attribute.getName(), value);
	                break;
	            }
	        }
        }
    }

    public void setJdbcEventLogger(JdbcEventLogger logger) {
        this.logger = logger;
    }

    public JdbcEventLogger getJdbcEventLogger() {
        return this.logger;
    }

    @Override
    public void nextBatchCount(Query query, int[] resultCount) {
    }

    @Override
    public void nextCount(Query query, int resultCount) {
    }

    @Override
    public void nextRows(Query query, List<?> dataRows) {
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void nextRows(Query q, ResultIterator it) {
        throw new UnsupportedOperationException("'nextDataRows(Query,ResultIterator)' is unsupported (and unexpected) on commit.");
    }

    @Override
    public boolean isIteratedResult() {
        return false;
    }
}
