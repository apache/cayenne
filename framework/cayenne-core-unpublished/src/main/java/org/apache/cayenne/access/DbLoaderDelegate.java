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

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;

/**
 * DbLoaderDelegate defines API that allows to control the behavior of DbLoader
 * during the database reverse-engineering. Delegate is also notified of the
 * progress of reverse-engineering.
 */
public interface DbLoaderDelegate {

    /**
     * Returns true to tell DbLoader that it is OK to overwrite DbEntity that
     * already exists in the model. If loading process should be stopped
     * immediately, an exception is thrown.
     */
    public boolean overwriteDbEntity(DbEntity entity) throws CayenneException;

    public void dbEntityAdded(DbEntity entity);

    public void dbEntityRemoved(DbEntity entity);

    public void objEntityAdded(ObjEntity entity);

    public void objEntityRemoved(ObjEntity entity);
}
