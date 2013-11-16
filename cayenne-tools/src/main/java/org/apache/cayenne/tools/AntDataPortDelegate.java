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

package org.apache.cayenne.tools;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.cayenne.access.DataPort;
import org.apache.cayenne.access.DataPortDelegate;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Query;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * DataPortDelegate implementation that works in the context of Ant DataPortTask
 * task execution, performing entity filtering and logging functions.
 * 
 * @since 1.2: Prior to 1.2 DataPort classes were a part of cayenne-examples
 *        package.
 * @deprecated since 3.2
 */
@Deprecated
class AntDataPortDelegate implements DataPortDelegate {

    protected Task parentTask;

    protected Pattern[] mapFilters;

    protected long timestamp;
    protected DbEntity lastEntity;

    protected NamePatternMatcher namePatternMatcher;

    // exists for testing and such
    AntDataPortDelegate() {
        mapFilters = new Pattern[] {};
    }

    AntDataPortDelegate(Task parentTask, String mapsPattern,
            String includeEntitiesPattern, String excludeEntitiesPattern) {
        this.parentTask = parentTask;

        this.namePatternMatcher = new NamePatternMatcher(new AntLogger(
                parentTask), includeEntitiesPattern, excludeEntitiesPattern);

        this.mapFilters = namePatternMatcher.createPatterns(mapsPattern);
    }

    /**
     * Applies preconfigured list of filters to the list, removing entities that
     * do not pass the filter.
     */
    protected List filterEntities(List entities) {
        if (entities == null || entities.isEmpty()) {
            return entities;
        }

        Iterator it = entities.iterator();
        while (it.hasNext()) {
            DbEntity entity = (DbEntity) it.next();

            if (!passedDataMapFilter(entity.getDataMap())) {
                it.remove();
            }
        }

        namePatternMatcher.filter(entities);

        return entities;
    }

    /**
     * Returns true if the DataMap passes a set of DataMap filters or if there
     * is no DataMap filters.
     */
    protected boolean passedDataMapFilter(DataMap map) {
        if (mapFilters.length == 0) {
            return true;
        }

        if (map == null) {
            return true;
        }

        String mapName = map.getName();
        for (Pattern mapFilter : mapFilters) {
            if (mapFilter.matcher(mapName).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Implements the delegate method to filter the list of entities applying
     * filtering rules encapsulated by this object.
     */
    public List willPortEntities(DataPort portTool, List entities) {
        return filterEntities(entities);
    }

    /**
     * Logs entity porting event using Ant logger.
     */
    public Query willPortEntity(DataPort portTool, DbEntity entity, Query query) {
        parentTask.log("Porting '" + entity.getName() + "'");
        lastEntity = entity;
        timestamp = System.currentTimeMillis();
        return query;
    }

    public void didPortEntity(DataPort portTool, DbEntity entity, int rowCount) {
        String timestampLabel = "";
        if (lastEntity == entity) {
            timestampLabel = " in " + (System.currentTimeMillis() - timestamp)
                    + " ms.";
        }

        String label = (rowCount == 1) ? "1 row transferred" : rowCount
                + " rows transferred";
        parentTask.log("Done porting " + entity.getName() + ", " + label
                + timestampLabel, Project.MSG_VERBOSE);
    }

    public List willCleanData(DataPort portTool, List entities) {
        return filterEntities(entities);
    }

    public Query willCleanData(DataPort portTool, DbEntity entity, Query query) {
        parentTask.log("Deleting " + entity.getName(), Project.MSG_VERBOSE);
        lastEntity = entity;
        timestamp = System.currentTimeMillis();
        return query;
    }

    public void didCleanData(DataPort portTool, DbEntity entity, int rowCount) {
        String timestampLabel = "";
        if (lastEntity == entity) {
            timestampLabel = " in " + (System.currentTimeMillis() - timestamp)
                    + " ms.";
        }

        String label = (rowCount == 1) ? "1 row deleted" : rowCount
                + " rows deleted";
        parentTask.log("Done deleting " + entity.getName() + ", " + label
                + timestampLabel, Project.MSG_VERBOSE);
    }
}
