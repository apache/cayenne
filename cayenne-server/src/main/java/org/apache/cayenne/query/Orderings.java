package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
  * <p>Orderings is provided so that you can chain Ordering together and then use 
  * the result to pass into methods that require List&lt;Ordering&gt;</p>
  * <p>Example:</p>
  * <pre>Person.COMPANY_NAME.asc().then(Person.FIRST_NAME.desc)</pre>
  * 
  * @since 4.0
  */
public class Orderings extends ArrayList<Ordering> {
	 
	public Orderings() {
	 	super();
	 }
	 
	 public Orderings(int initialCapacity) {
	 	super(initialCapacity);
	 }

	 public Orderings(Collection<? extends Ordering> c) {
	 	super(c);
	 }

	 public Orderings(Ordering ordering) {
	 	super(Arrays.asList(ordering));
	 }

	 public Orderings(Ordering... orderings) {
	 	super(Arrays.asList(orderings));
	 }

	 /**
	 * Adds the given sort ordering to the end of this list and
	 * returns "this" so it can be chained again.
	 * 
	 * @param nextOrdering the sort ordering to add
	 * @return this (with nextOrdering appended)
	 */
	 public Orderings then(Ordering nextOrdering) {
	 	add(nextOrdering);
	 	
	 	return this;
	 }

	 /**
	 * Adds the given sort orderings to the end of this list and returns
	 * "this" so it can be chained again.
	 * 
	 * @param nextOrderings the sort ordering to add
	 * @return this (with nextOrderings appended)
	 */
	 public Orderings then(Orderings nextOrderings) {
	 	addAll(nextOrderings);
	 	
	 	return this;
	 }
	 
	 /**
	 * @see Orderings#then(Orderings)
	 * @param nextOrderings
	 * @return
	 */
	 public Orderings then(List<Ordering> nextOrderings) {
	 	addAll(nextOrderings);
	 	
	 	return this;
	 }
	 
	 /**
	 * Returns an list sorted with these Orderings.
	 * 
	 * @param <T> the type of the list
	 * @param list the list to sort
	 * @return a sorted copy of the list
	 */
	 public <T> List<T> orderedList(List<T> list) {
	 	return Ordering.orderedList(list, this);
	 }

	 /**
	 * Sorts the given array with these Orderings.
	 * 
	 * @param <T> the type of the list
	 * @param list the list to sort
	 */
	 public <T> void orderList(List<T> list) {
	 	Ordering.orderList(list, this);
	 }
}
