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

package org.apache.cayenne.query;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.util.ConversionUtil;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;
import org.apache.commons.collections.ComparatorUtils;

/**
 * Defines object sorting criteria, used either for in-memory sorting of object lists or
 * as a specification for building <em>ORDER BY</em> clause of a SelectQuery query. Note
 * that in case of in-memory sorting, Ordering can be used with any JavaBeans, not just
 * DataObjects.
 * 
 * @author Andrus Adamchik
 * @author Craig Miskell
 */
public class Ordering implements Comparator<Object>, Serializable, XMLSerializable {

    /**
     * Symbolic representation of ascending ordering criterion.
     */
    public static final boolean ASC = true;

    /**
     * Symbolic representation of descending ordering criterion.
     */
    public static final boolean DESC = false;

    protected String sortSpecString;
    protected transient Expression sortSpec;
    protected boolean ascending;
    protected boolean caseInsensitive;
    protected boolean pathExceptionSuppressed = false;
    protected boolean nullSortedFirst = true;

    /**
     * Orders a given list of objects, using a List of Orderings applied according the
     * default iteration order of the Orderings list. I.e. each Ordering with lower index
     * is more significant than any other Ordering with higher index. List being ordered is
     * modified in place.
     */
    public static void orderList(List<?> objects, List<Ordering> orderings) {
        Collections.sort(objects, ComparatorUtils.chainedComparator(orderings));
    }

    public Ordering() {
    }

    public Ordering(String sortPathSpec, boolean ascending) {
        this(sortPathSpec, ascending, false);
    }

    public Ordering(String sortPathSpec, boolean ascending, boolean caseInsensitive) {
        setSortSpecString(sortPathSpec);
        this.ascending = ascending;
        this.caseInsensitive = caseInsensitive;
    }

    public Ordering(Expression sortExpression, boolean ascending) {
        this(sortExpression, ascending, false);
    }

    public Ordering(Expression sortExpression, boolean ascending, boolean caseInsensitive) {
        setSortSpec(sortExpression);
        this.ascending = ascending;
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * Sets sortSpec to be an expression represented by string argument.
     * 
     * @since 1.1
     */
    public void setSortSpecString(String sortSpecString) {
        if (!Util.nullSafeEquals(this.sortSpecString, sortSpecString)) {
            this.sortSpecString = sortSpecString;
            this.sortSpec = null;
        }
    }
     
    /**
     * Sets sort order for whether nulls are at the top or bottom of the resulting list. Default is true.
     * 
     * @param nullSortedFirst true sorts nulls to the top of the list, false sorts nulls to the bottom
     */
    public void setNullSortedFirst(boolean nullSortedFirst) {
        this.nullSortedFirst = nullSortedFirst;
    }
    
    /** 
     * Get sort order for nulls.
     * 
     *  @return true if nulls are sorted to the top of the list, false if sorted to the bottom
     */
    public boolean isNullSortedFirst() {
        return nullSortedFirst;
    }
    
    /**
     * Sets whether a path with a null in the middle is ignored. For example, a sort from <code>painting</code>
     * on <code>artist.name</code> would by default throw an exception if the artist was null.
     * If set to true, then this is treated just like a null value.
     * Default is false.
     * 
     * @param pathExceptionSuppressed true to suppress exceptions and sort as null
     */
    public void setPathExceptionSupressed(boolean pathExceptionSuppressed) {
        this.pathExceptionSuppressed = pathExceptionSuppressed;
    }
    
    /** 
     * Is a path with a null in the middle is ignored.
     * 
     * @return true is exception is suppressed and sorted as null
     */
    public boolean isPathExceptionSuppressed() {
        return pathExceptionSuppressed;
    }
    
    
    /**
     * Returns sortSpec string representation.
     * 
     * @since 1.1
     */
    public String getSortSpecString() {
        return sortSpecString;
    }

    /** Returns true if sorting is done in ascending order. */
    public boolean isAscending() {
        return ascending;
    }

    /** Sets <code>ascending</code> property of this Ordering. */
    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    /** Returns true if the sorting is case insensitive */
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    /** Sets <code>caseInsensitive</code> property of this Ordering. */
    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * Returns the expression defining a ordering Java Bean property.
     */
    public Expression getSortSpec() {
        if (sortSpecString == null) {
            return null;
        }

        // compile on demand
        if (sortSpec == null) {
            sortSpec = Expression.fromString(sortSpecString);
        }

        return sortSpec;
    }

    /**
     * Sets the expression defining a ordering Java Bean property.
     */
    public void setSortSpec(Expression sortSpec) {
        this.sortSpec = sortSpec;
        this.sortSpecString = (sortSpec != null) ? sortSpec.toString() : null;
    }

    /**
     * Orders the given list of objects according to the ordering that this object
     * specifies. List is modified in-place.
     * 
     * @param objects a List of objects to be sorted
     */
    public void orderList(List<?> objects) {
        Collections.sort(objects, this);
    }

    /**
     * Comparable interface implementation. Can compare two Java Beans based on the stored
     * expression.
     */
    public int compare(Object o1, Object o2) {
        Expression exp = getSortSpec();
		Object value1 = null;
		Object value2 = null;
		try {
        	value1 = exp.evaluate(o1);
		} catch (ExpressionException e) {
			if (pathExceptionSuppressed && e.getCause() instanceof org.apache.cayenne.reflect.UnresolvablePathException) {
				//do nothing, we expect this 
			} else {
				//rethrow
				throw e;
			}
		}
		
		try {
        	value2 = exp.evaluate(o2);
		} catch (ExpressionException e) {
			if (pathExceptionSuppressed && e.getCause() instanceof org.apache.cayenne.reflect.UnresolvablePathException) {
				//do nothing, we expect this 
			} else {
				//rethrow
				throw e;
			}
		}

		if (value1 == null && value2 == null) {
		    return 0;
		} else if (value1 == null) {
		    return nullSortedFirst? -1:1;
		} else if (value2 == null) {
            return nullSortedFirst? 1:-1;
        }

        if (this.caseInsensitive) {
            // TODO: to upper case should probably be defined as a separate expression
            // type
            value1 = ConversionUtil.toUpperCase(value1);
            value2 = ConversionUtil.toUpperCase(value2);
        }

        int compareResult = ConversionUtil.toComparable(value1).compareTo(
                ConversionUtil.toComparable(value2));
        return (ascending) ? compareResult : -compareResult;
    }

    /**
     * Encodes itself as a query ordering.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<ordering");

        if (!ascending) {
            encoder.print(" descending=\"true\"");
        }

        if (caseInsensitive) {
            encoder.print(" ignore-case=\"true\"");
        }

        encoder.print(">");
        if (getSortSpec() != null) {
            getSortSpec().encodeAsXML(encoder);
        }
        encoder.println("</ordering>");
    }

    public String toString() {
        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        XMLEncoder encoder = new XMLEncoder(pw);
        encodeAsXML(encoder);
        pw.close();
        buffer.flush();
        return buffer.toString();
    }
}
