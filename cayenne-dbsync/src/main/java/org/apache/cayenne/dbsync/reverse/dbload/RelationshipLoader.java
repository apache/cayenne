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

package org.apache.cayenne.dbsync.reverse.dbload;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.util.EqualsBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RelationshipLoader extends AbstractLoader {

    private static final Log LOGGER = LogFactory.getLog(DbLoader.class);

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
            if(pkEntity == null || fkEntity == null) {
                // Check for existence of this entities were made in creation of ExportedKey
                throw new IllegalStateException();
            }

            if (!new EqualsBuilder()
                    .append(pkEntity.getCatalog(), PK.getCatalog())
                    .append(pkEntity.getSchema(), PK.getSchema()).append(fkEntity.getCatalog(), FK.getCatalog())
                    .append(fkEntity.getSchema(), PK.getSchema()).isEquals()) {

                LOGGER.info("Skip relation: '" + key + "' because it related to objects from other catalog/schema");
                LOGGER.info("     relation primary key: '" + PK.getCatalog() + "." + PK.getSchema() + "'");
                LOGGER.info("       primary key entity: '" + pkEntity.getCatalog() + "." + pkEntity.getSchema() + "'");
                LOGGER.info("     relation foreign key: '" + FK.getCatalog() + "." + FK.getSchema() + "'");
                LOGGER.info("       foreign key entity: '" + fkEntity.getCatalog() + "." + fkEntity.getSchema() + "'");
                continue;
            }

            // forwardRelationship is a reference from table with primary key
            // it is what exactly we load from db
            DbRelationship forwardRelationship = new DbRelationship();
            forwardRelationship.setSourceEntity(pkEntity);
            forwardRelationship.setTargetEntityName(fkEntity);

            // TODO: dirty and non-transparent... using DbRelationshipDetected for the benefit of the merge package.
            // This info is available from joins....
            DbRelationshipDetected reverseRelationship = new DbRelationshipDetected();
            reverseRelationship.setFkName(FK.getName());
            reverseRelationship.setSourceEntity(fkEntity);
            reverseRelationship.setTargetEntityName(pkEntity);
            reverseRelationship.setToMany(false);

            createAndAppendJoins(exportedKeys, pkEntity, fkEntity, forwardRelationship, reverseRelationship);

            boolean toDependentPK = isToDependentPK(forwardRelationship);
            boolean toMany = isToMany(toDependentPK, fkEntity, forwardRelationship);

            forwardRelationship.setToDependentPK(toDependentPK);
            forwardRelationship.setToMany(toMany);

            // set relationship names only after their joins are ready ...
            // generator logic is based on relationship state...
            forwardRelationship.setName(NameBuilder
                    .builder(forwardRelationship, pkEntity)
                    .baseName(nameGenerator.relationshipName(forwardRelationship))
                    .name());

            reverseRelationship.setName(NameBuilder
                    .builder(reverseRelationship, fkEntity)
                    .baseName(nameGenerator.relationshipName(reverseRelationship))
                    .name());

            if (delegate.dbRelationshipLoaded(fkEntity, reverseRelationship)) {
                fkEntity.addRelationship(reverseRelationship);
            }
            if (delegate.dbRelationshipLoaded(pkEntity, forwardRelationship)) {
                pkEntity.addRelationship(forwardRelationship);
            }
        }
    }

    private boolean isToMany(boolean toDependentPK, DbEntity fkEntity, DbRelationship forwardRelationship) {
        return !toDependentPK || fkEntity.getPrimaryKeys().size() != forwardRelationship.getJoins().size();
    }

    private boolean isToDependentPK(DbRelationship forwardRelationship) {
        for (DbJoin dbJoin : forwardRelationship.getJoins()) {
            if (!dbJoin.getTarget().isPrimaryKey()) {
                return false;
            }
        }

        return true;
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

            forwardRelationship.addJoin(new DbJoin(forwardRelationship, pkName, fkName));
            reverseRelationship.addJoin(new DbJoin(reverseRelationship, fkName, pkName));
        }
    }
}
