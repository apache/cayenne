/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cayenne.dbsync.reverse.db;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.commons.logging.Log;

/**
 * @since 4.0
 */
public class LoggingDbLoaderDelegate extends DefaultDbLoaderDelegate {

    private final Log logger;

    public LoggingDbLoaderDelegate(Log logger) {
        this.logger = logger;
    }

    @Override
    public void dbEntityAdded(DbEntity entity) {
        logger.info("  Table: " + entity.getFullyQualifiedName());
    }

    @Override
    public void dbEntityRemoved(DbEntity entity) {
        logger.info("  Table removed: " + entity.getFullyQualifiedName());
    }

    @Override
    public boolean dbRelationship(DbEntity entity) {
        if (logger.isDebugEnabled()) {
            logger.debug("    Relationships for " + entity.getFullyQualifiedName());
        }

        return true;
    }

    @Override
    public boolean dbRelationshipLoaded(DbEntity entity, DbRelationship relationship) {
        logger.info("    " + relationship);

        return true;
    }
}
