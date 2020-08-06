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
package org.apache.cayenne.ashwood.graph;

import java.util.NoSuchElementException;

class EmptyIterator<E, V> implements ArcIterator<E, V> {

	@SuppressWarnings({ "rawtypes" })
	private static final ArcIterator EMPTY_ITERATOR = new EmptyIterator<>();
	
	@SuppressWarnings("unchecked")
	static <E, V> ArcIterator<E, V> instance() {
		return EMPTY_ITERATOR;
	}

	@Override
	public E getOrigin() {
		return null;
	}

	@Override
	public E getDestination() {
		return null;
	}

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public V next() {
		throw new NoSuchElementException("Iterator contains no elements");
	}

	@Override
	public void remove() {
		throw new IllegalStateException("Iterator contains no elements");
	}
}