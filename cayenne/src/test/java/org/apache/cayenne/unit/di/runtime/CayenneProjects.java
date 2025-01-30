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

package org.apache.cayenne.unit.di.runtime;

public class CayenneProjects {

    // known runtimes... unit tests may reuse these with @UseCayenneRuntime
    // annotation or can define their own on the fly
    // (TODO: how would that work with the global schema setup?)
    public static final String ARRAY_TYPE_PROJECT = "cayenne-array-type.xml";
    public static final String BINARY_PK_PROJECT = "cayenne-binary-pk.xml";
    public static final String CAY_2032 = "cayenne-cay-2032.xml";
    public static final String COMPOUND_PROJECT = "cayenne-compound.xml";
    public static final String DATE_TIME_PROJECT = "cayenne-date-time.xml";
    public static final String DELETE_RULES_PROJECT = "cayenne-delete-rules.xml";
    public static final String EMBEDDABLE_PROJECT = "cayenne-embeddable.xml";
    public static final String EMPTY_PROJECT = "cayenne-empty.xml";
    public static final String ENUM_PROJECT = "cayenne-enum.xml";
    public static final String EXTENDED_TYPE_PROJECT = "cayenne-extended-type.xml";
    public static final String GENERATED_PROJECT = "cayenne-generated.xml";
    public static final String GENERIC_PROJECT = "cayenne-generic.xml";
    public static final String INHERITANCE_PROJECT = "cayenne-inheritance.xml";
    public static final String INHERITANCE_SINGLE_TABLE1_PROJECT = "cayenne-inheritance-single-table1.xml";
    public static final String INHERITANCE_VERTICAL_PROJECT = "cayenne-inheritance-vertical.xml";
    public static final String JSON_PROJECT = "cayenne-json.xml";
    public static final String LEGACY_DATE_TIME_PROJECT = "cayenne-legacy-date-time.xml";
    public static final String LIFECYCLE_CALLBACKS_ORDER_PROJECT = "cayenne-lifecycle-callbacks-order.xml";
    public static final String LIFECYCLES_PROJECT = "cayenne-lifecycles.xml";
    public static final String LOB_PROJECT = "cayenne-lob.xml";
    public static final String LOCKING_PROJECT = "cayenne-locking.xml";
    public static final String MAP_TO_MANY_PROJECT = "cayenne-map-to-many.xml";
    public static final String MEANINGFUL_PK_PROJECT = "cayenne-meaningful-pk.xml";
    public static final String MISC_TYPES_PROJECT = "cayenne-misc-types.xml";
    public static final String MIXED_PERSISTENCE_STRATEGY_PROJECT = "cayenne-mixed-persistence-strategy.xml";
    public static final String MULTI_TIER_PROJECT = "cayenne-multi-tier.xml";
    public static final String MULTINODE_PROJECT = "cayenne-multinode.xml";
    public static final String NO_PK_PROJECT = "cayenne-no-pk.xml";
    public static final String NUMERIC_TYPES_PROJECT = "cayenne-numeric-types.xml";
    public static final String ONEWAY_PROJECT = "cayenne-oneway-rels.xml";
    public static final String PEOPLE_PROJECT = "cayenne-people.xml";
    public static final String PRIMITIVE_PROJECT = "cayenne-primitive.xml";
    public static final String QUALIFIED_PROJECT = "cayenne-qualified.xml";
    public static final String QUOTED_IDENTIFIERS_PROJECT = "cayenne-quoted-identifiers.xml";
    public static final String REFLEXIVE_PROJECT = "cayenne-reflexive.xml";
    public static final String RELATIONSHIPS_PROJECT = "cayenne-relationships.xml";
    public static final String RELATIONSHIPS_ACTIVITY_PROJECT = "cayenne-relationships-activity.xml";
    public static final String RELATIONSHIPS_MANY_TO_MANY_JOIN_PROJECT = "cayenne-relationships-many-to-many-join.xml";
    public static final String RELATIONSHIPS_CHILD_MASTER_PROJECT = "cayenne-relationships-child-master.xml";
    public static final String RELATIONSHIPS_CLOB_PROJECT = "cayenne-relationships-clob.xml";
    public static final String RELATIONSHIPS_COLLECTION_TO_MANY_PROJECT = "cayenne-relationships-collection-to-many.xml";
    public static final String RELATIONSHIPS_DELETE_RULES_PROJECT = "cayenne-relationships-delete-rules.xml";
    public static final String RELATIONSHIPS_FLATTENED_PROJECT = "cayenne-relationships-flattened.xml";
    public static final String RELATIONSHIPS_SET_TO_MANY_PROJECT = "cayenne-relationships-set-to-many.xml";
    public static final String RELATIONSHIPS_TO_MANY_FK_PROJECT = "cayenne-relationships-to-many-fk.xml";
    public static final String RELATIONSHIPS_TO_ONE_FK_PROJECT = "cayenne-relationships-to-one-fk.xml";
    public static final String RETURN_TYPES_PROJECT = "cayenne-return-types.xml";
    public static final String SOFT_DELETE_PROJECT = "cayenne-soft-delete.xml";
    public static final String SUS_PROJECT = "cayenne-sus.xml";
    public static final String TABLE_PRIMITIVES_PROJECT = "cayenne-table-primitives.xml";
    public static final String TESTMAP_PROJECT = "cayenne-testmap.xml";
    public static final String THINGS_PROJECT = "cayenne-things.xml";
    public static final String TOONE_PROJECT = "cayenne-toone.xml";
    public static final String UNSUPPORTED_DISTINCT_TYPES_PROJECT = "cayenne-unsupported-distinct-types.xml";
    public static final String UUID_PROJECT = "cayenne-uuid.xml";
    public static final String CUSTOM_NAME_PROJECT = "custom-name-file.xml";
    public static final String WEIGHTED_SORT_PROJECT = "cayenne-weighted-sort.xml";
    public static final String HYBRID_DATA_OBJECT_PROJECT = "cayenne-hybrid-data-object.xml";
    public static final String INHERITANCE_WITH_ENUM_PROJECT = "cayenne-inheritance-with-enum.xml";
    public static final String LAZY_ATTRIBUTES_PROJECT = "cayenne-lazy-attributes.xml";
    public static final String CAY_2666 = "cay2666/cayenne-cay-2666.xml";
    public static final String CAY_2641 = "cay2641/cayenne-cay-2641.xml";
    public static final String ANNOTATION = "annotation/cayenne-project.xml";
}
