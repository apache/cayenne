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

package org.apache.cayenne.access;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.map.DbEntity;

/**
 * Defines API of an iterator over the records returned as a result
 * of SelectQuery execution. Usually a ResultIterator is supported by
 * an open java.sql.ResultSet, therefore most of the methods would throw
 * checked exceptions. ResultIterators must be explicitly closed when the
 * user is done working with them.
 * 
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 * @author Andrus Adamchik
 */
public interface ResultIterator {
    
    /**
     * Returns all unread data rows from ResultSet and closes this iterator
     * if asked to do so.
     */
    public List dataRows(boolean close) throws CayenneException;
           
	/** 
	 * Returns true if there is at least one more record
	 * that can be read from the iterator.
	 */
	public boolean hasNextRow() throws CayenneException;
    
    /** 
	 * Returns the next result row as a Map.
	 */
    public Map nextDataRow() throws CayenneException;
    
    /**
     * Returns a map of ObjectId values from the next result row.
     * Primary key columns are determined from the provided DbEntity.
     * 
     * @since 1.1
     */
    public Map nextObjectId(DbEntity entity) throws CayenneException;
    
    /**
     * Skips current data row instead of reading it.
     */
    public void skipDataRow() throws CayenneException;
    
    /** 
     * Closes ResultIterator and associated ResultSet. This method must be
     * called explicitly when the user is finished processing the records.
     * Otherwise unused database resources will not be released properly.
     */  
    public void close() throws CayenneException;
    
    /**
     * Returns the number of columns in the result row.
     * 
     * @since 1.0.6
     */
    public int getDataRowWidth();
}

