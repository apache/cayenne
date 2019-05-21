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
  * @since 4.1
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
	 * @param nextOrderings the sort ordering to add
	 * @return this
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
