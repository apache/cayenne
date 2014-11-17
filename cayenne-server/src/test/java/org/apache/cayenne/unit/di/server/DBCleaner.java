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

package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.unit.UnitDbAdapter;

import java.sql.SQLException;

public class DBCleaner {

    private FlavoredDBHelper dbHelper;
    private String location;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    public DBCleaner(FlavoredDBHelper dbHelper, String location) {
        this.dbHelper = dbHelper;
        this.location = location;
    }

    public void clean() throws SQLException {
        if (location.equals(CayenneProjects.TESTMAP_PROJECT)) {
            dbHelper.deleteAll("ARTGROUP");
            dbHelper.deleteAll("ARTIST");
            dbHelper.deleteAll("ARTIST_CT");
            dbHelper.deleteAll("ARTIST_EXHIBIT");
            dbHelper.deleteAll("ARTIST_GROUP");
            dbHelper.deleteAll("EXHIBIT");
            dbHelper.deleteAll("GALLERY");
            dbHelper.deleteAll("GENERATED_COLUMN");
            dbHelper.deleteAll("NULL_TEST");
            dbHelper.deleteAll("PAINTING");
            dbHelper.deleteAll("PAINTING1");
            dbHelper.deleteAll("PAINTING_INFO");
        } else if (location.equals(CayenneProjects.MULTI_TIER_PROJECT)) {
            dbHelper.deleteAll("MT_JOIN45");
            dbHelper.deleteAll("MT_TABLE1");
            dbHelper.deleteAll("MT_TABLE2");
            dbHelper.deleteAll("MT_TABLE3");
            dbHelper.deleteAll("MT_TABLE4");
            dbHelper.deleteAll("MT_TABLE5");
        } else if (location.equals(CayenneProjects.COMPOUND_PROJECT)) {
            dbHelper.deleteAll("CHAR_FK_TEST");
            dbHelper.deleteAll("CHAR_PK_TEST");
            dbHelper.deleteAll("COMPOUND_FK_TEST");
            dbHelper.deleteAll("COMPOUND_PK_TEST");
        } else if (location.equals(CayenneProjects.PEOPLE_PROJECT)) {
            dbHelper.deleteAll("ADDRESS");
            dbHelper.deleteAll("CLIENT_COMPANY");
            dbHelper.deleteAll("DEPARTMENT");
            dbHelper.deleteAll("PERSON");
            dbHelper.deleteAll("PERSON_NOTES");
        } else if(location.equals(CayenneProjects.BINARY_PK_PROJECT)) {
            dbHelper.deleteAll("BINARY_PK_TEST1");
            dbHelper.deleteAll("BINARY_PK_TEST2");
        } else if (location.equals(CayenneProjects.DATE_TIME_PROJECT)) {
            dbHelper.deleteAll("CALENDAR_TEST");
            dbHelper.deleteAll("DATE_TEST");
        } else if (location.equals(CayenneProjects.DELETE_RULES_PROJECT)) {
            dbHelper.deleteAll("DELETE_CASCADE");
            dbHelper.deleteAll("DELETE_DENY");
            dbHelper.deleteAll("DELETE_NULLIFY");
            dbHelper.deleteAll("DELETE_RULE");
        } else if (location.equals(CayenneProjects.EMBEDDABLE_PROJECT)) {
            dbHelper.deleteAll("EMBED_ENTITY1");
        } else if (location.equals(CayenneProjects.EMPTY_PROJECT)) {
            return;
        } else if (location.equals(CayenneProjects.ENUM_PROJECT)) {
            dbHelper.deleteAll("ENUM_ENTITY");
        } else if (location.equals(CayenneProjects.EXTENDED_TYPE_PROJECT)) {
            dbHelper.deleteAll("EXTENDED_TYPE_TEST");
        } else if (location.equals(CayenneProjects.GENERATED_PROJECT)) {
            dbHelper.deleteAll("GENERATED_COLUMN_COMP_KEY");
            dbHelper.deleteAll("GENERATED_COLUMN_COMP_M");
            dbHelper.deleteAll("GENERATED_COLUMN_DEP");
            dbHelper.deleteAll("GENERATED_COLUMN_TEST");
            dbHelper.deleteAll("GENERATED_COLUMN_TEST2");
            dbHelper.deleteAll("GENERATED_F1");
            dbHelper.deleteAll("GENERATED_F2");
            dbHelper.deleteAll("GENERATED_JOIN");
        } else if (location.equals(CayenneProjects.GENERIC_PROJECT)) {
            dbHelper.deleteAll("GENERIC1");
            dbHelper.deleteAll("GENERIC2");
        } else if (location.equals(CayenneProjects.INHERITANCE_PROJECT)) {
            dbHelper.deleteAll("BASE_ENTITY");
            dbHelper.deleteAll("DIRECT_TO_SUB_ENTITY");
            dbHelper.deleteAll("RELATED_ENTITY");
        } else if (location.equals(CayenneProjects.INHERITANCE_SINGLE_TABLE1_PROJECT)) {
            dbHelper.deleteAll("GROUP_MEMBERS");
            dbHelper.deleteAll("GROUP_PROPERTIES");
            dbHelper.deleteAll("ROLES");
            dbHelper.deleteAll("USER_PROPERTIES");
        } else if (location.equals(CayenneProjects.INHERITANCE_VERTICAL_PROJECT)) {
            dbHelper.deleteAll("IV1_ROOT");
            dbHelper.deleteAll("IV1_SUB1");
            dbHelper.deleteAll("IV2_ROOT");
            dbHelper.deleteAll("IV2_SUB1");
            dbHelper.deleteAll("IV2_X");
            dbHelper.deleteAll("IV_ROOT");
            dbHelper.deleteAll("IV_SUB1");
            dbHelper.deleteAll("IV_SUB1_SUB1");
            dbHelper.deleteAll("IV_SUB2");
        } else if (location.equals(CayenneProjects.LIFECYCLES_PROJECT)) {
            dbHelper.deleteAll("LIFECYCLES");
        } else if (location.equals(CayenneProjects.LOB_PROJECT)) {
            if (accessStackAdapter.supportsLobs()) {
                dbHelper.deleteAll("BLOB_TEST");
                dbHelper.deleteAll("CLOB_TEST");
            }
            dbHelper.deleteAll("CLOB_TEST_RELATION");
            dbHelper.deleteAll("TEST");
        } else if (location.equals(CayenneProjects.LOCKING_PROJECT)) {
            dbHelper.deleteAll("LOCKING_HELPER");
            dbHelper.deleteAll("REL_LOCKING_TEST");
            dbHelper.deleteAll("SIMPLE_LOCKING_TEST");
        } else if (location.equals(CayenneProjects.MAP_TO_MANY_PROJECT)) {
            dbHelper.deleteAll("ID_MAP_TO_MANY");
            dbHelper.deleteAll("ID_MAP_TO_MANY_TARGET");
            dbHelper.deleteAll("MAP_TO_MANY");
            dbHelper.deleteAll("MAP_TO_MANY_TARGET");
        } else if (location.equals(CayenneProjects.MEANINGFUL_PK_PROJECT)) {
            dbHelper.deleteAll("MEANINGFUL_PK");
            dbHelper.deleteAll("MEANINGFUL_PK_DEP");
            dbHelper.deleteAll("MEANINGFUL_PK_TEST1");
        } else if (location.equals(CayenneProjects.MISC_TYPES_PROJECT)) {
            dbHelper.deleteAll("ARRAYS_ENTITY");
            dbHelper.deleteAll("CHARACTER_ENTITY");
            if(accessStackAdapter.supportsLobs()) {
                dbHelper.deleteAll("SERIALIZABLE_ENTITY");
            }
        } else if (location.equals(CayenneProjects.MIXED_PERSISTENCE_STRATEGY_PROJECT)) {
            dbHelper.deleteAll("MIXED_PERSISTENCE_STRATEGY");
            dbHelper.deleteAll("MIXED_PERSISTENCE_STRATEGY2");
        } else if (location.equals(CayenneProjects.MULTINODE_PROJECT)) {
            dbHelper.deleteAll("CROSSDB_M1E1");
            dbHelper.deleteAll("CROSSDB_M2E1");
            dbHelper.deleteAll("CROSSDB_M2E2");
        } else if (location.equals(CayenneProjects.NO_PK_PROJECT)) {
            dbHelper.deleteAll("NO_PK_TEST");
        } else if (location.equals(CayenneProjects.NUMERIC_TYPES_PROJECT)) {
            dbHelper.deleteAll("BIGDECIMAL_ENTITY");
            dbHelper.deleteAll("BIGINTEGER_ENTITY");
            dbHelper.deleteAll("BIT_TEST");
            dbHelper.deleteAll("BOOLEAN_TEST");
            dbHelper.deleteAll("DECIMAL_PK_TST");
            dbHelper.deleteAll("FLOAT_TEST");
            dbHelper.deleteAll("LONG_ENTITY");
            dbHelper.deleteAll("SMALLINT_TEST");
            dbHelper.deleteAll("TINYINT_TEST");
        } else if (location.equals(CayenneProjects.ONEWAY_PROJECT)) {
            dbHelper.deleteAll("oneway_table1");
            dbHelper.deleteAll("oneway_table2");
            dbHelper.deleteAll("oneway_table3");
            dbHelper.deleteAll("oneway_table4");
        } else if (location.equals(CayenneProjects.PERSISTENT_PROJECT)) {
            dbHelper.deleteAll("CONTINENT");
            dbHelper.deleteAll("COUNTRY");
        } else if (location.equals(CayenneProjects.PRIMITIVE_PROJECT)) {
            dbHelper.deleteAll("PRIMITIVES_TEST");
        } else if (location.equals(CayenneProjects.QUALIFIED_PROJECT)) {
            dbHelper.deleteAll("TEST_QUALIFIED1");
            dbHelper.deleteAll("TEST_QUALIFIED2");
        } else if (location.equals(CayenneProjects.QUOTED_IDENTIFIERS_PROJECT)) {
            dbHelper.deleteAll("QUOTED_ADDRESS");
            dbHelper.deleteAll("quote Person");
        } else if (location.equals(CayenneProjects.REFLEXIVE_PROJECT)) {
            dbHelper.deleteAll("REFLEXIVE");
        } else if (location.equals(CayenneProjects.RELATIONSHIPS_PROJECT)) {
            dbHelper.deleteAll("FK_OF_DIFFERENT_TYPE");
            dbHelper.deleteAll("MEANINGFUL_FK");
            dbHelper.deleteAll("REFLEXIVE_AND_TO_ONE");
            dbHelper.deleteAll("RELATIONSHIP_HELPER");
        } else if (location.equals(CayenneProjects.RELATIONSHIPS_ACTIVITY_PROJECT)) {
            dbHelper.deleteAll("ACTIVITY");
            dbHelper.deleteAll("RESULT");
        } else if (location.equals(CayenneProjects.RELATIONSHIPS_CHILD_MASTER_PROJECT)) {
            dbHelper.deleteAll("CHILD");
            dbHelper.deleteAll("MASTER");
        } else if (location.equals(CayenneProjects.RELATIONSHIPS_CLOB_PROJECT)) {
            dbHelper.deleteAll("CLOB_DETAIL");
            dbHelper.deleteAll("CLOB_MASTER");
        } else if (location.equals(CayenneProjects.RELATIONSHIPS_COLLECTION_TO_MANY_PROJECT)) {
            dbHelper.deleteAll("COLLECTION_TO_MANY");
            dbHelper.deleteAll("COLLECTION_TO_MANY_TARGET");
        } else if (location.equals(CayenneProjects.RELATIONSHIPS_DELETE_RULES_PROJECT)) {
            dbHelper.deleteAll("DELETE_RULE_FLATA");
            dbHelper.deleteAll("DELETE_RULE_FLATB");
            dbHelper.deleteAll("DELETE_RULE_JOIN");
            dbHelper.deleteAll("DELETE_RULE_TEST1");
            dbHelper.deleteAll("DELETE_RULE_TEST2");
            dbHelper.deleteAll("DELETE_RULE_TEST3");
        } else if (location.equals(CayenneProjects.RELATIONSHIPS_FLATTENED_PROJECT)) {
            dbHelper.deleteAll("COMPLEX_JOIN");
            dbHelper.deleteAll("FLATTENED_CIRCULAR");
            dbHelper.deleteAll("FLATTENED_CIRCULAR_JOIN");
            dbHelper.deleteAll("FLATTENED_TEST_1");
            dbHelper.deleteAll("FLATTENED_TEST_2");
            dbHelper.deleteAll("FLATTENED_TEST_3");
            dbHelper.deleteAll("FLATTENED_TEST_4");
        } else if (location.equals(CayenneProjects.RELATIONSHIPS_SET_TO_MANY_PROJECT)) {
            dbHelper.deleteAll("SET_TO_MANY");
            dbHelper.deleteAll("SET_TO_MANY_TARGET");
        } else if (location.equals(CayenneProjects.RELATIONSHIPS_TO_MANY_FK_PROJECT)) {
            dbHelper.deleteAll("TO_MANY_FKDEP");
            dbHelper.deleteAll("TO_MANY_FKROOT");
            dbHelper.deleteAll("TO_MANY_ROOT2");
        } else if (location.equals(CayenneProjects.RELATIONSHIPS_TO_ONE_FK_PROJECT)) {
            dbHelper.deleteAll("TO_ONE_FK1");
            dbHelper.deleteAll("TO_ONE_FK2");
        } else if (location.equals(CayenneProjects.RETURN_TYPES_PROJECT)) {
            if (accessStackAdapter.supportsLobs()) {
                dbHelper.deleteAll("TYPES_MAPPING_LOBS_TEST1");
                dbHelper.deleteAll("TYPES_MAPPING_TEST2");
            }
            dbHelper.deleteAll("TYPES_MAPPING_TEST1");
        } else if (location.equals(CayenneProjects.SOFT_DELETE_PROJECT)) {
            dbHelper.deleteAll("SOFT_DELETE");
        } else if (location.equals(CayenneProjects.SUS_PROJECT)) {
            return;
        } else if (location.equals(CayenneProjects.TABLE_PRIMITIVES_PROJECT)) {
            dbHelper.deleteAll("TABLE_PRIMITIVES");
        } else if (location.equals(CayenneProjects.THINGS_PROJECT)) {
            dbHelper.deleteAll("BAG");
            dbHelper.deleteAll("BALL");
            dbHelper.deleteAll("BOX");
            dbHelper.deleteAll("BOX_INFO");
            dbHelper.deleteAll("BOX_THING");
            dbHelper.deleteAll("THING");
        } else if (location.equals(CayenneProjects.TOONE_PROJECT)) {
            dbHelper.deleteAll("TOONE_DEP");
            dbHelper.deleteAll("TOONE_MASTER");
        } else if (location.equals(CayenneProjects.UUID_PROJECT)) {
            dbHelper.deleteAll("UUID_PK_ENTITY");
            dbHelper.deleteAll("UUID_TEST");
        }
    }
}
