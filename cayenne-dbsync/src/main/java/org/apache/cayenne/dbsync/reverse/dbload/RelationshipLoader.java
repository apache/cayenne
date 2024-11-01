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

package org.apache.cayenne.dbsync.reverse.dbload;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelationshipLoader extends AbstractLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbLoader.class);

    private final ObjectNameGenerator nameGenerator;

    RelationshipLoader(DbLoaderConfiguration config, DbLoaderDelegate delegate, ObjectNameGenerator nameGenerator) {
        super(null, config, delegate);
        this.nameGenerator = nameGenerator;
    }

    @Override
    public void load(DatabaseMetaData metaData, DbLoadDataStore map) throws SQLException {
        if (config.isSkipRelationshipsLoading()) {
            return;
        }

        for (Map.Entry<String, Set<ExportedKey>> entry : map.getExportedKeysEntrySet()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Process keys for: " + entry.getKey());
            }

            Set<ExportedKey> exportedKeys = entry.getValue();
            ExportedKey key = exportedKeys.iterator().next();
            if (key == null) {
                throw new IllegalStateException();
            }

            ExportedKey.KeyData PK = key.getPk();
            ExportedKey.KeyData FK = key.getFk();
            DbEntity pkEntity = map.getDbEntity(PK.getTable());
            DbEntity fkEntity = map.getDbEntity(FK.getTable());
            if (pkEntity == null || fkEntity == null) {
                // Check for existence of this entities were made in creation of ExportedKey
                throw new IllegalStateException();
            }

            // forwardRelationship is a reference from table with primary key
            // it is what exactly we load from db
            DbRelationship forwardRelationship = new DbRelationship();
            forwardRelationship.setSourceEntity(pkEntity);
            forwardRelationship.setTargetEntityName(fkEntity);
            forwardRelationship.setFK(false);

            // TODO: dirty and non-transparent... using DbRelationshipDetected for the benefit of the merge package.
            // This info is available from joins....
            DbRelationshipDetected reverseRelationship = new DbRelationshipDetected();
            reverseRelationship.setFkName(FK.getName());
            reverseRelationship.setSourceEntity(fkEntity);
            reverseRelationship.setTargetEntityName(pkEntity);
            reverseRelationship.setToMany(false);
            reverseRelationship.setFK(true);
            createAndAppendJoins(exportedKeys, pkEntity, fkEntity, forwardRelationship, reverseRelationship);

            forwardRelationship.setToMany(isToMany(fkEntity, forwardRelationship));

            // set relationship names only after their joins are ready ...
            // generator logic is based on relationship state...

            setRelationshipName(pkEntity, forwardRelationship);
            setRelationshipName(fkEntity, reverseRelationship);

            checkAndAddRelationship(pkEntity, forwardRelationship);
            checkAndAddRelationship(fkEntity, reverseRelationship);
        }
    }

    private void setRelationshipName(DbEntity entity, DbRelationship relationship) {
        relationship.setName(NameBuilder
                .builder(relationship, entity)
                .baseName(nameGenerator.relationshipName(relationship))
                .name());
    }

    private void checkAndAddRelationship(DbEntity entity, DbRelationship relationship){
        TableFilter sourceTableFilter = config.getFiltersConfig()
                .tableFilter(relationship.getSourceEntity().getCatalog(), relationship.getSourceEntity().getSchema());

        TableFilter targetTableFilter = config.getFiltersConfig()
                .tableFilter(relationship.getTargetEntity().getCatalog(), relationship.getTargetEntity().getSchema());

        // check that relationship can be included
        if(sourceTableFilter != null && !sourceTableFilter.getIncludeTableRelationshipFilter(entity.getName())
                .isIncluded(relationship.getName())) {
            return;
        }

        // this can be because of filtered out columns, so next check can be excessive,
        // but still better to check everything here too, so we can assert that added relationship is valid.
        if(relationship.getJoins().isEmpty()) {
            return;
        }

        if(sourceTableFilter != null && targetTableFilter != null) {
	        // check that all join attributes are included
	        for(DbJoin join : relationship.getJoins()) {
	            if(!sourceTableFilter.getIncludeTableColumnFilter(entity.getName()).isIncluded(join.getSourceName()) ||
	                    !targetTableFilter.getIncludeTableColumnFilter(relationship.getTargetEntityName()).isIncluded(join.getTargetName())) {
	                return;
	            }
	        }
        }

        // add relationship if delegate permit it
        if (delegate.dbRelationshipLoaded(entity, relationship)) {
            entity.addRelationship(relationship);
        }
    }

    private boolean isToMany(DbEntity fkEntity, DbRelationship forwardRelationship) {
        for (DbJoin join : forwardRelationship.getJoins()) {
            if (!join.getSource().isPrimaryKey() || !join.getTarget().isPrimaryKey()) {
                return true;
            }
        }
        return fkEntity.getPrimaryKeys().size() != forwardRelationship.getJoins().size();
    }

    private void createAndAppendJoins(Set<ExportedKey> exportedKeys, DbEntity pkEntity, DbEntity fkEntity,
                                      DbRelationship forwardRelationship, DbRelationship reverseRelationship) {

        for (ExportedKey exportedKey : exportedKeys) {
            // Create and append joins
            String pkName = exportedKey.getPk().getColumn();
            String fkName = exportedKey.getFk().getColumn();

            // skip invalid joins...
            DbAttribute pkAtt = pkEntity.getAttribute(pkName);
            if (pkAtt == null) {
                LOGGER.info("no attribute for declared primary key: " + pkName);
                continue;
            }

            DbAttribute fkAtt = fkEntity.getAttribute(fkName);
            if (fkAtt == null) {
                LOGGER.info("no attribute for declared foreign key: " + fkName);
                continue;
            }

            addJoin(forwardRelationship, pkName, fkName);
            addJoin(reverseRelationship, fkName, pkName);
        }
    }

    private void addJoin(DbRelationship relationship, String sourceName, String targetName){

        for (DbJoin join : relationship.getJoins()) {
            if (join.getSourceName().equals(sourceName) && join.getTargetName().equals(targetName)) {
                return;
            }
        }

        relationship.addJoin(new DbJoin(relationship, sourceName, targetName));
    }
}