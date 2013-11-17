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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Query;

/**
 * Interface for callback and delegate methods allowing implementing classes to control
 * various aspects of data porting via DataPort. DataPort instance will invoke appropriate
 * delegate methods during different stages of porting process.
 * 
 * @since 1.2: Prior to 1.2 DataPort classes were a part of cayenne-examples package.
 * @deprecated since 3.2
 */
@Deprecated
public interface DataPortDelegate {

    /**
     * Allows delegate to sort or otherwise alter a list of DbEntities right before the
     * port starts.
     */
    List willPortEntities(DataPort portTool, List entities);

    /**
     * Invoked by DataPort right before the start of data port for a given entity. Allows
     * delegate to handle such things like logging, etc. Also makes it possible to
     * substitute or alter the select query used to fecth the source data, e.g. set a
     * limiting qualifier.
     */
    Query willPortEntity(DataPort portTool, DbEntity entity, Query query);

    /**
     * Invoked by DataPort right after the end of data port for a given entity. Allows
     * delegate to handle such things like logging, etc.
     */
    void didPortEntity(DataPort portTool, DbEntity entity, int rowCount);

    /**
     * Allows delegate to sort or otherwise alter a list of DbEntities right before data
     * cleanup starts.
     */
    List willCleanData(DataPort portTool, List entities);

    /**
     * Invoked by DataPort right before the start of data cleanup for a given entity.
     * Allows delegate to handle such things like logging, etc. Also makes it possible to
     * substitute or alter the delete query used to cleanup the data, e.g. set a limiting
     * qualifier.
     */
    Query willCleanData(DataPort portTool, DbEntity entity, Query query);

    /**
     * Invoked by DataPort right after the end of data cleanup for a given entity. Allows
     * delegate to handle such things like logging, etc.
     */
    void didCleanData(DataPort portTool, DbEntity entity, int rowCount);
}
