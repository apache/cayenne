/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.util;

import java.util.Comparator;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.query.Query;

/**
 * A collection of useful Comparators used by the modeler.
 * 
 * @since 1.1
 * @author Andrei Adamchik
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
            else if (o instanceof DbEntity) {
                return 3;
            }
            else if (o instanceof Procedure) {
                return 4;
            }
            else if (o instanceof Query) {
                return 5;
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