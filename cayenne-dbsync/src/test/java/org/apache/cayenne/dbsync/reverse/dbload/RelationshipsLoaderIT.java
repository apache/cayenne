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
import java.util.Collection;

import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.map.DbRelationship;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RelationshipsLoaderIT extends BaseLoaderIT {

    @Test
    public void testRelationshipLoad() throws Exception {
        boolean supportsFK = accessStackAdapter.supportsFKConstraints();
        if(!supportsFK) {
            return;
        }

        DatabaseMetaData metaData = connection.getMetaData();
        DbLoaderDelegate delegate = new DefaultDbLoaderDelegate();

        // We need all data to check relationships, so simply load it all
        EntityLoader entityLoader = new EntityLoader(adapter, EMPTY_CONFIG, delegate);
        AttributeLoader attributeLoader = new AttributeLoader(adapter, EMPTY_CONFIG, delegate);
        PrimaryKeyLoader primaryKeyLoader = new PrimaryKeyLoader(EMPTY_CONFIG, delegate);
        ExportedKeyLoader exportedKeyLoader = new ExportedKeyLoader(EMPTY_CONFIG, delegate);

        entityLoader.load(metaData, store);
        attributeLoader.load(metaData, store);
        primaryKeyLoader.load(metaData, store);
        exportedKeyLoader.load(metaData, store);

        // *** TESTING THIS ***
        RelationshipLoader relationshipLoader = new RelationshipLoader(EMPTY_CONFIG, delegate, new DefaultObjectNameGenerator());
        relationshipLoader.load(metaData, store);

        Collection<DbRelationship> rels = getDbEntity("ARTIST").getRelationships();
        assertNotNull(rels);
        assertTrue(!rels.isEmpty());

        // test one-to-one
        rels = getDbEntity("PAINTING").getRelationships();
        assertNotNull(rels);

        // find relationship to PAINTING_INFO
        DbRelationship oneToOne = null;
        for (DbRelationship rel : rels) {
            if ("PAINTING_INFO".equalsIgnoreCase(rel.getTargetEntityName())) {
                oneToOne = rel;
                break;
            }
        }

        assertNotNull("No relationship to PAINTING_INFO", oneToOne);
        assertFalse("Relationship to PAINTING_INFO must be to-one", oneToOne.isToMany());
        assertTrue("Relationship to PAINTING_INFO must be to-one", oneToOne.isToDependentPK());
    }

}
