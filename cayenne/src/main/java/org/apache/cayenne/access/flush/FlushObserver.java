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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.CayenneSqlException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.Util;

import java.util.List;

/**
 * @since 4.2
 */
class FlushObserver implements OperationObserver {

    // the latest statement reported via nextStatement, used to correlate a failure with a specific SQL statement
    private TranslatedStatement statement;

    @Override
    public void nextStatement(Query query, TranslatedStatement statement) {
        this.statement = statement;
    }

    @Override
    public void nextQueryException(Query query, Exception ex) {
        // preserve meaningful Cayenne exceptions (e.g. OptimisticLockException), even when they wrap a lower-level
        // cause; only wrap raw lower-level failures in a CayenneSqlException so they can be correlated with the
        // failing statement
        Throwable unwound = Util.unwindException(ex, CayenneRuntimeException.class);

        if (unwound instanceof CayenneRuntimeException cayenneException) {
            throw cayenneException;
        }
        throw new CayenneSqlException("Flush exception.", query, statement, unwound);
    }

    @Override
    public void nextGlobalException(Exception ex) {
        throw new CayenneRuntimeException("Flush exception.", Util.unwindException(ex));
    }

    /**
     * Processes generated keys.
     */
    @Override
    public void nextGeneratedRows(Query query, List<DataRow> keys, List<ObjectId> idsToUpdate) {

        if (!(query instanceof InsertBatchQuery batch)) {
            throw new CayenneRuntimeException("Generated keys only supported for InsertBatchQuery, instead got %s", query);
        }

        if (keys.size() != idsToUpdate.size()) {
            throw new CayenneRuntimeException("Mismatching number of generated PKs: expected %d, instead got %d", idsToUpdate.size(), keys.size());
        }
        
        for (int i = 0; i < keys.size(); i++) {
	        DataRow key = keys.get(i);
	
	        if (key.isEmpty()) {
	            throw new CayenneRuntimeException("Empty key generated.");
	        }
	
	        ObjectId idToUpdate = idsToUpdate.get(i);
	        if (idToUpdate == null || !idToUpdate.isTemporary()) {
	            // why would this happen?
	            return;
	        }

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
	                
	
	                // I guess we should override any existing value,
	                // as generated key is the latest thing that exists in the DB.
	                idToUpdate.getReplacementIdMap().put(attribute.getName(), value);
	                break;
	            }
	        }
        }
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
