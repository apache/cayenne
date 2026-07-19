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

import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.NoStemStemmer;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RelationshipsLoaderIT extends BaseLoaderIT {

    @Test
    public void relationshipLoad() throws Exception {
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
        RelationshipLoader relationshipLoader = new RelationshipLoader(EMPTY_CONFIG, delegate, new DefaultObjectNameGenerator(NoStemStemmer.getInstance()));
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

        assertNotNull(oneToOne, "No relationship to PAINTING_INFO");
        assertFalse(oneToOne.isToMany(), "Relationship to PAINTING_INFO must be to-one");
        assertTrue(oneToOne.isToDependentPK(), "Relationship to PAINTING_INFO must be to-one");
    }

    @Test
    public void compoundFkLoad() throws Exception {
        if (!accessStackAdapter.supportsFKConstraints()) {
            return;
        }

        DatabaseMetaData metaData = connection.getMetaData();
        DbLoaderDelegate delegate = new DefaultDbLoaderDelegate();

        new EntityLoader(adapter, EMPTY_CONFIG, delegate).load(metaData, store);
        new AttributeLoader(adapter, EMPTY_CONFIG, delegate).load(metaData, store);
        new PrimaryKeyLoader(EMPTY_CONFIG, delegate).load(metaData, store);
        new ExportedKeyLoader(EMPTY_CONFIG, delegate).load(metaData, store);

        new RelationshipLoader(EMPTY_CONFIG, delegate, new DefaultObjectNameGenerator(NoStemStemmer.getInstance())).load(metaData, store);

        // COMPOUND_FK_TEST has a 2-column FK (F_KEY1, F_KEY2) -> COMPOUND_PK_TEST (KEY1, KEY2)
        DbEntity fkEntity = getDbEntity("COMPOUND_FK_TEST");
        assertNotNull(fkEntity, "COMPOUND_FK_TEST entity was not loaded");

        List<DbRelationship> toCompoundPk = relationshipsTo(fkEntity, "COMPOUND_PK_TEST");
        assertEquals(1, toCompoundPk.size(),
                "The 2-column FK must produce a single to-one relationship, not one per column");
        assertEquals(2, toCompoundPk.get(0).getJoins().size(),
                "The relationship must carry both FK column joins");

        // the reverse (to-many) relationship must be a single compound relationship as well
        DbEntity pkEntity = getDbEntity("COMPOUND_PK_TEST");
        assertNotNull(pkEntity, "COMPOUND_PK_TEST entity was not loaded");

        List<DbRelationship> toCompoundFk = relationshipsTo(pkEntity, "COMPOUND_FK_TEST");
        assertEquals(1, toCompoundFk.size(),
                "The reverse of the 2-column FK must be a single to-many relationship, not one per column");
        assertEquals(2, toCompoundFk.get(0).getJoins().size(),
                "The reverse relationship must carry both FK column joins");
    }

    @Test
    public void twoIndependentFksLoad() throws Exception {
        if (!accessStackAdapter.supportsFKConstraints()) {
            return;
        }

        DatabaseMetaData metaData = connection.getMetaData();
        DbLoaderDelegate delegate = new DefaultDbLoaderDelegate();

        new EntityLoader(adapter, EMPTY_CONFIG, delegate).load(metaData, store);
        new AttributeLoader(adapter, EMPTY_CONFIG, delegate).load(metaData, store);
        new PrimaryKeyLoader(EMPTY_CONFIG, delegate).load(metaData, store);
        new ExportedKeyLoader(EMPTY_CONFIG, delegate).load(metaData, store);

        new RelationshipLoader(EMPTY_CONFIG, delegate, new DefaultObjectNameGenerator(NoStemStemmer.getInstance())).load(metaData, store);

        // TWO_FK_B has two separate single-column FKs (A_ID1, A_ID2) -> TWO_FK_A (ID)
        DbEntity bEntity = getDbEntity("TWO_FK_B");
        assertNotNull(bEntity, "TWO_FK_B entity was not loaded");

        List<DbRelationship> toA = relationshipsTo(bEntity, "TWO_FK_A");
        assertEquals(2, toA.size(), "Two independent FKs must produce two separate relationships");
        for (DbRelationship rel : toA) {
            assertEquals(1, rel.getJoins().size(), "Each single-column FK relationship must have exactly one join");
        }

        DbEntity aEntity = getDbEntity("TWO_FK_A");
        assertNotNull(aEntity, "TWO_FK_A entity was not loaded");

        List<DbRelationship> toB = relationshipsTo(aEntity, "TWO_FK_B");
        assertEquals(2, toB.size(), "Two independent FKs must produce two separate reverse relationships");
        for (DbRelationship rel : toB) {
            assertEquals(1, rel.getJoins().size(), "Each reverse relationship must have exactly one join");
        }
    }

    private List<DbRelationship> relationshipsTo(DbEntity entity, String targetName) {
        List<DbRelationship> result = new ArrayList<>();
        for (DbRelationship rel : entity.getRelationships()) {
            if (targetName.equalsIgnoreCase(rel.getTargetEntityName())) {
                result.add(rel);
            }
        }
        return result;
    }

//    private void assertUniqueConstraintsInRelationships(DataMap map) {
        // unfortunately JDBC metadata doesn't provide info for UNIQUE
        // constraints....
        // cant reengineer them...
        // upd. actually it's provided:
        // http://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getIndexInfo%28java.lang.String,%20java.lang.String,%20java.lang.String,%20boolean,%20boolean%29

        // find rel to TO_ONEFK1
        /*
         * Iterator it = getDbEntity(map,
         * "TO_ONEFK2").getRelationships().iterator(); DbRelationship rel =
         * (DbRelationship) it.next(); assertEquals("TO_ONEFK1",
         * rel.getTargetEntityName());
         * assertFalse("UNIQUE constraint was ignored...", rel.isToMany());
         */
//    }
}
