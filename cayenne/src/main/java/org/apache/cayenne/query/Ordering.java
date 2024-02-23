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

package org.apache.cayenne.query;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.EmptyConfigurationNodeVisitor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.util.ConversionUtil;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Defines object sorting criteria, used either for in-memory sorting of object
 * lists or as a specification for building <em>ORDER BY</em> clause of a
 * SelectQuery query. Note that in case of in-memory sorting, Ordering can be
 * used with any JavaBeans, not just Persistent objects.
 */
public class Ordering implements Comparator<Object>, Serializable, XMLSerializable {

	private static final long serialVersionUID = -9167074787055881422L;

	protected String sortSpecString;
	protected transient Expression sortSpec;
	protected SortOrder sortOrder;
	protected boolean pathExceptionSuppressed = false;
	protected boolean nullSortedFirst = true;

	/**
	 * Orders a given list of objects, using a List of Orderings applied
	 * according the default iteration order of the Orderings list. I.e. each
	 * Ordering with lower index is more significant than any other Ordering
	 * with higher index. List being ordered is modified in place.
	 *
	 * @param objects elements to sort
	 * @param orderings list of Orderings to be applied
	 */
	@SuppressWarnings("unchecked")
	public static void orderList(List<?> objects, List<? extends Ordering> orderings) {
		if(objects == null || orderings == null || orderings.isEmpty()) {
			return;
		}
		Comparator<Object> comparator = orderings.get(0);
		for(int i=1; i<orderings.size(); i++) {
			comparator = comparator.thenComparing(orderings.get(i));
		}
		objects.sort(comparator);
	}

	/**
	 * Orders a given list of objects, using a List of Orderings applied
	 * according the default iteration order of the Orderings list. I.e. each
	 * Ordering with lower index is more significant than any other Ordering
	 * with higher index.
	 *
	 * @param objects elements to sort
	 * @param orderings list of Orderings to be applied
	 * @return new List with ordered elements
	 *
	 * @since 4.0
	 */
	public static <E> List<E> orderedList(final Collection<E> objects, List<? extends Ordering> orderings) {
		List<E> newList = new ArrayList<>(objects);
		
		orderList(newList, orderings);
		
		return newList;
	}
	
	public Ordering() {
	}

	/**
	 * Create an ordering instance with a provided path and ascending sorting
	 * strategy.
	 * 
	 * @since 4.0
	 */
	public Ordering(String sortPathSpec) {
		this(sortPathSpec, SortOrder.ASCENDING);
	}

	/**
	 * @since 3.0
	 */
	public Ordering(String sortPathSpec, SortOrder sortOrder) {
		setSortSpecString(sortPathSpec);
		setSortOrder(sortOrder);
	}

	/**
	 * @since 4.0
	 */
	public Ordering(Expression sortSpec) {
		this(sortSpec, SortOrder.ASCENDING);
	}

	/**
	 * @since 4.0
	 */
	public Ordering(Expression sortSpec, SortOrder sortOrder) {
		setSortSpec(sortSpec);
		setSortOrder(sortOrder);
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof Ordering)) {
			return false;
		}

		Ordering o = (Ordering) object;

		if (!Util.nullSafeEquals(sortSpecString, o.sortSpecString)) {
			return false;
		}

		if (sortOrder != o.sortOrder) {
			return false;
		}

		if (pathExceptionSuppressed != o.pathExceptionSuppressed) {
			return false;
		}

		if (nullSortedFirst != o.nullSortedFirst) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = sortSpecString != null ? sortSpecString.hashCode() : 0;
		result = 31 * result + (sortOrder != null ? sortOrder.hashCode() : 0);
		result = 31 * result + (pathExceptionSuppressed ? 1 : 0);
		result = 31 * result + (nullSortedFirst ? 1 : 0);
		return result;
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
	 * Sets sort order for whether nulls are at the top or bottom of the
	 * resulting list. Default is true.
	 * Affects only in-memory sorting.
	 * 
	 * @param nullSortedFirst
	 *            true sorts nulls to the top of the list, false sorts nulls to
	 *            the bottom
	 */
	public void setNullSortedFirst(boolean nullSortedFirst) {
		this.nullSortedFirst = nullSortedFirst;
	}

	/**
	 * Get sort order for nulls.
	 * 
	 * @return true if nulls are sorted to the top of the list, false if sorted
	 *         to the bottom
	 */
	public boolean isNullSortedFirst() {
		return nullSortedFirst;
	}

	/**
	 * Sets whether a path with a null in the middle is ignored. For example, a
	 * sort from <code>painting</code> on <code>artist.name</code> would by
	 * default throw an exception if the artist was null. If set to true, then
	 * this is treated just like a null value. Default is false.
	 * 
	 * @param pathExceptionSuppressed
	 *            true to suppress exceptions and sort as null
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

	/**
	 * Sets the sort order for this ordering.
	 * 
	 * @since 3.0
	 */
	public void setSortOrder(SortOrder order) {
		this.sortOrder = order;
	}

	/** Returns true if sorting is done in ascending order. */
	public boolean isAscending() {
		return sortOrder == null || sortOrder == SortOrder.ASCENDING || sortOrder == SortOrder.ASCENDING_INSENSITIVE;
	}

	/**
	 * Returns true if the sorting is done in descending order.
	 * 
	 * @since 3.0
	 */
	public boolean isDescending() {
		return !isAscending();
	}

	/**
	 * If the sort order is DESCENDING or DESCENDING_INSENSITIVE, sets the sort
	 * order to ASCENDING or ASCENDING_INSENSITIVE, respectively.
	 * 
	 * @since 3.0
	 */
	public void setAscending() {
		if (sortOrder == null || sortOrder == SortOrder.DESCENDING) {
			setSortOrder(SortOrder.ASCENDING);
		} else if (sortOrder == SortOrder.DESCENDING_INSENSITIVE) {
			setSortOrder(SortOrder.ASCENDING_INSENSITIVE);
		}
	}

	/**
	 * If the sort order is ASCENDING or ASCENDING_INSENSITIVE, sets the sort
	 * order to DESCENDING or DESCENDING_INSENSITIVE, respectively.
	 * 
	 * @since 3.0
	 */
	public void setDescending() {
		if (sortOrder == null || sortOrder == SortOrder.ASCENDING) {
			setSortOrder(SortOrder.DESCENDING);
		} else if (sortOrder == SortOrder.ASCENDING_INSENSITIVE) {
			setSortOrder(SortOrder.DESCENDING_INSENSITIVE);
		}
	}

	/** Returns true if the sorting is case insensitive */
	public boolean isCaseInsensitive() {
		return !isCaseSensitive();
	}

	/**
	 * Returns true if the sorting is case sensitive.
	 * 
	 * @since 3.0
	 */
	public boolean isCaseSensitive() {
		return sortOrder == null || sortOrder == SortOrder.ASCENDING || sortOrder == SortOrder.DESCENDING;
	}

	/**
	 * If the sort order is ASCENDING or DESCENDING, sets the sort order to
	 * ASCENDING_INSENSITIVE or DESCENDING_INSENSITIVE, respectively.
	 * 
	 * @since 3.0
	 */
	public void setCaseInsensitive() {
		if (sortOrder == null || sortOrder == SortOrder.ASCENDING) {
			setSortOrder(SortOrder.ASCENDING_INSENSITIVE);
		} else if (sortOrder == SortOrder.DESCENDING) {
			setSortOrder(SortOrder.DESCENDING_INSENSITIVE);
		}
	}

	/**
	 * If the sort order is ASCENDING_INSENSITIVE or DESCENDING_INSENSITIVE,
	 * sets the sort order to ASCENDING or DESCENDING, respectively.
	 * 
	 * @since 3.0
	 */
	public void setCaseSensitive() {
		if (sortOrder == null || sortOrder == SortOrder.ASCENDING_INSENSITIVE) {
			setSortOrder(SortOrder.ASCENDING);
		} else if (sortOrder == SortOrder.DESCENDING_INSENSITIVE) {
			setSortOrder(SortOrder.DESCENDING);
		}
	}

	/**
	 * Returns the expression defining a ordering Java Bean property.
	 */
	public Expression getSortSpec() {
		if (sortSpecString == null) {
			return null;
		}

		// compile on demand .. since orderings can only be paths, avoid the
		// overhead of
		// Expression.fromString, and parse them manually
		if (sortSpec == null) {

			if (sortSpecString.startsWith(ASTDbPath.DB_PREFIX)) {
				sortSpec = new ASTDbPath(sortSpecString.substring(ASTDbPath.DB_PREFIX.length()));
			} else if (sortSpecString.startsWith(ASTObjPath.OBJ_PREFIX)) {
				sortSpec = new ASTObjPath(sortSpecString.substring(ASTObjPath.OBJ_PREFIX.length()));
			} else {
				sortSpec = new ASTObjPath(sortSpecString);
			}
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
	 * Orders the given list of objects according to the ordering that this
	 * object specifies. List is modified in-place.
	 * 
	 * @param objects
	 *            a List of objects to be sorted
	 */
	public void orderList(List<?> objects) {
		Collections.sort(objects, this);
	}

	/**
	 * @since 4.0
	 */
	public <E> List<E> orderedList(Collection<E> objects) {
		List<E> newList = new ArrayList<>(objects);
		
		orderList(newList);
		
		return newList;
	}

	/**
	 * Comparable interface implementation. Can compare two Java Beans based on
	 * the stored expression.
	 */
	@Override
	public int compare(Object o1, Object o2) {
		Expression exp = getSortSpec();
		Object value1 = null;
		Object value2 = null;
		try {
			value1 = exp.evaluate(o1);
		} catch (ExpressionException e) {
			if (pathExceptionSuppressed && e.getCause() instanceof org.apache.cayenne.reflect.UnresolvablePathException) {
				// do nothing, we expect this
			} else {
				// re-throw
				throw e;
			}
		}

		try {
			value2 = exp.evaluate(o2);
		} catch (ExpressionException e) {
			if (pathExceptionSuppressed && e.getCause() instanceof org.apache.cayenne.reflect.UnresolvablePathException) {
				// do nothing, we expect this
			} else {
				// rethrow
				throw e;
			}
		}

		if (value1 == null && value2 == null) {
			return 0;
		} else if (value1 == null) {
			return nullSortedFirst ? -1 : 1;
		} else if (value2 == null) {
			return nullSortedFirst ? 1 : -1;
		}

		if (isCaseInsensitive()) {
			// TODO: to upper case should probably be defined as a separate
			// expression
			// type
			value1 = ConversionUtil.toUpperCase(value1);
			value2 = ConversionUtil.toUpperCase(value2);
		}

		int compareResult = ConversionUtil.toComparable(value1).compareTo(ConversionUtil.toComparable(value2));
		return (isAscending()) ? compareResult : -compareResult;
	}

	/**
	 * Encodes itself as a query ordering.
	 * 
	 * @since 1.1
	 */
	@Override
	public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
		encoder.start("ordering")
				.attribute("descending", isDescending())
				.attribute("ignore-case", isCaseInsensitive())
				.nested(getSortSpec(), delegate)
				.end();
	}

	@Override
	public String toString() {
		StringWriter buffer = new StringWriter();
		PrintWriter pw = new PrintWriter(buffer);
		XMLEncoder encoder = new XMLEncoder(pw);
		encodeAsXML(encoder, new EmptyConfigurationNodeVisitor());
		pw.close();
		buffer.flush();
		return buffer.toString();
	}

	/**
	 * Returns sort order for this ordering
	 * 
	 * @since 3.1
	 */
	public SortOrder getSortOrder() {
		return sortOrder;
	}
	
	
	 /**
	  * Returns Orderings with this Ordering followed by the provided
	  * next Ordering.
	  * 
	  * @param nextOrdering the next Ordering to chain to this
	  * @return a new Orderings with both Ordering
	  * @since 4.1
	  */
	 public Orderings then(Ordering nextOrdering) {
	 	return new Orderings(this, nextOrdering);
	 }

	 /**
	  * Returns Orderings with this Ordering followed by the provided
	  * list of next Orderings.
	  * 
	  * @param nextOrderings the next Orderings to chain to this
	  * @return an array of sort orderings
	  * @since 4.1
	  */
	 public Orderings then(Orderings nextOrderings) {
	 	Orderings newOrderings = new Orderings(this);
	 	
	 	return newOrderings.then(nextOrderings);
	 }
	 
	 /**
	  * @see Orderings#then(Orderings)
	  * @param nextOrderings the next Orderings to chain to this
	  * @return an array of sort orderings
	  * @since 4.1
	  */
	 public Orderings then(List<Ordering> nextOrderings) {
	 	Orderings newOrderings = new Orderings(this);
	 	
	 	return newOrderings.then(nextOrderings);
	 }
}
