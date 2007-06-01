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
package org.objectstyle.cayenne.query;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections.ComparatorUtils;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.util.ConversionUtil;
import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.util.XMLSerializable;

/** 
 * Defines a Comparator for Java Beans. Ordering can be used either
 * to define ORDER BY clause of a query in terms of object properties,
 * or as a Comparator for in-memory Java Beans sorting.
 * 
 * @author Andrei Adamchik
 * @author Craig Miskell
 */
public class Ordering implements Comparator, Serializable, XMLSerializable {

    /** 
     * Symbolic representation of ascending ordering criterion. 
     */
    public static final boolean ASC = true;

    /** 
     * Symbolic representation of descending ordering criterion. 
     */
    public static final boolean DESC = false;

    protected Expression sortSpec;
    protected boolean ascending;
    protected boolean caseInsensitive;

    /**
     * Orders a given list of objects, using a List of Orderings
     * applied according the default iteration order of the Orderings list. 
     * I.e. each Ordering with lower index is more significant than any other
     * Ordering with higer index. List being ordered is modified in place.
     */
    public static void orderList(List objects, List orderings) {
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

    public Ordering(
        Expression sortExpression,
        boolean ascending,
        boolean caseInsensitive) {
        setSortSpec(sortExpression);
        this.ascending = ascending;
        this.caseInsensitive = caseInsensitive;
    }

    /** 
     * Sets sortSpec to be OBJ_PATH expression. 
     * with path specified as <code>sortPathSpec</code>
     * parameter.
     * 
     * @deprecated Since 1.1 use {@link #setSortSpecString(String)}
     */
    public void setSortSpec(String sortSpecString) {
        this.sortSpec = Expression.fromString(sortSpecString);
    }

    /** 
     * Sets sortSpec to be an expression represented by string argument.
     * 
     * @since 1.1
     */
    public void setSortSpecString(String sortSpecString) {
        this.sortSpec =
            (sortSpecString != null) ? Expression.fromString(sortSpecString) : null;
    }

    /** 
     * Returns sortSpec string representation.
     * 
     * @since 1.1
     */
    public String getSortSpecString() {
        return (sortSpec != null) ? sortSpec.toString() : null;
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
        return sortSpec;
    }

    /**
     * Sets the expression defining a ordering Java Bean property.
     */
    public void setSortSpec(Expression sortSpec) {
        this.sortSpec = sortSpec;
    }

    /**
     * Orders the given list of objects according to the ordering that this 
     * object specifies. List is modified in-place.
     * 
     * @param objects a List of objects to be sorted
     */
    public void orderList(List objects) {
        Collections.sort(objects, this);
    }

    /**
     * Comparable interface implementation. Can compare two
     * Java Beans based on the stored expression.
     */
    public int compare(Object o1, Object o2) {
        Object value1 = sortSpec.evaluate(o1);
        Object value2 = sortSpec.evaluate(o2);

        // nulls first policy... maybe make this configurable as some DB do
        if (value1 == null) {
            return (value2 == null) ? 0 : -1;
        }
        else if (value2 == null) {
            return 1;
        }

        if (this.caseInsensitive) {
            // TODO: to upper case should probably be defined as a separate expression type
            value1 = ConversionUtil.toUpperCase(value1);
            value2 = ConversionUtil.toUpperCase(value2);
        }

        int compareResult =
            ConversionUtil.toComparable(value1).compareTo(
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
        if (sortSpec != null) {
            sortSpec.encodeAsXML(encoder);
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
