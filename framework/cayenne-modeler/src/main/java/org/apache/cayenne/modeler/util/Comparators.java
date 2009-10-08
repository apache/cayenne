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

package org.apache.cayenne.modeler.util;

import java.util.Comparator;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.query.Query;

/**
 * A collection of useful Comparators used by the modeler.
 * 
 * @since 1.1
 */
public class Comparators {

    private static final Comparator dataDomainChildrenComparator = new DataDomainChildrenComparator();

    private static final Comparator dataMapChildrenComparator = new DataMapChildrenComparator();

    private static final Comparator entityChildrenComparator = new EntityChildrenComparator();

    private static final Comparator namedObjectComparator = new NamedObjectComparator();

    /**
     * Returns a comparator to order DataMap objects of mixed types. Objects of the same
     * type are ordered based on "name" property. Objects of different types are ordered
     * based on the following precedence: DataMap, DataNode.
     */
    public static Comparator getDataDomainChildrenComparator() {
        return dataDomainChildrenComparator;
    }

    /**
     * Returns a comparator to order DataMap objects of mixed types. Objects of the same
     * type are ordered based on "name" property. Objects of different types are ordered
     * based on the following precedence: DataMap, ObjEntity, DbEntity, Procedure and
     * Query.
     */
    public static Comparator getDataMapChildrenComparator() {
        return dataMapChildrenComparator;
    }

    /**
     * Returns a comparator to order Entity properties such as Attributes and
     * Relationships. Objects of the same type are ordered based on "name" property.
     * Objects of different types are ordered based on the following precedence:
     * Attribute, Relationship.
     */
    public static Comparator getEntityChildrenComparator() {
        return entityChildrenComparator;
    }

    /**
     * Returns a comparator to order java beans according to their "name" property.
     */
    public static Comparator getNamedObjectComparator() {
        return namedObjectComparator;
    }

    static class NamedObjectComparator implements Comparator {

        public int compare(Object o1, Object o2) {

            String name1 = ModelerUtil.getObjectName(o1);
            String name2 = ModelerUtil.getObjectName(o2);

            if (name1 == null) {
                return (name2 != null) ? -1 : 0;
            }
            else if (name2 == null) {
                return 1;
            }
            else {
                return name1.compareTo(name2);
            }
        }
    }

    final static class DataDomainChildrenComparator extends NamedObjectComparator {

        public int compare(Object o1, Object o2) {
            int delta = getClassWeight(o1) - getClassWeight(o2);
            if (delta != 0) {
                return delta;
            }
            else {
                return super.compare(o1, o2);
            }
        }

        private static int getClassWeight(Object o) {
            if (o instanceof DataMap) {
                return 1;
            }
            else if (o instanceof DataNode) {
                return 2;
            }
            else {
                // this should trap nulls among other things
                return Integer.MAX_VALUE;
            }
        }
    }

    final static class DataMapChildrenComparator extends NamedObjectComparator {

        public int compare(Object o1, Object o2) {
            int delta = getClassWeight(o1) - getClassWeight(o2);
            if (delta != 0) {
                return delta;
            }
            else {
                return super.compare(o1, o2);
            }
        }

        private static int getClassWeight(Object o) {
            if (o instanceof DataMap) {
                return 1;
            }
            else if (o instanceof ObjEntity) {
                return 2;
            }
            else if (o instanceof Embeddable) {
                return 3;
            }
            else if (o instanceof DbEntity) {
                return 4;
            }
            else if (o instanceof Procedure) {
                return 5;
            }
            else if (o instanceof Query) {
                return 6;
            }
            else {
                // this should trap nulls among other things
                return Integer.MAX_VALUE;
            }
        }
    }

    final static class EntityChildrenComparator extends NamedObjectComparator {

        public int compare(Object o1, Object o2) {
            int delta = getClassWeight(o1) - getClassWeight(o2);
            if (delta != 0) {
                return delta;
            }
            else {
                return super.compare(o1, o2);
            }
        }

        private static int getClassWeight(Object o) {
            if (o instanceof Entity) {
                return 1;
            }
            else if (o instanceof Attribute) {
                return 2;
            }
            else if (o instanceof Relationship) {
                return 3;
            }
            else {
                // this should trap nulls among other things
                return Integer.MAX_VALUE;
            }
        }
    }
}
