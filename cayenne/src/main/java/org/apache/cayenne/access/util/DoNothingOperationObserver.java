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
package org.apache.cayenne.access.util;

import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.query.Query;

/**
 * A very simple observer that does nothing with provided data, and rethrows any
 * reported exceptions. Can be used as a base superclass for custom observers.
 * 
 * @since 4.0
 */
public class DoNothingOperationObserver implements OperationObserver {

	@Override
	public boolean isIteratedResult() {
		return false;
	}

	@Override
	public void nextCount(Query query, int resultCount) {
		// TODO Auto-generated method stub

	}

	@Override
	public void nextBatchCount(Query query, int[] resultCount) {
		// TODO Auto-generated method stub

	}

	@Override
	public void nextRows(Query query, List<?> dataRows) {
		// TODO Auto-generated method stub

	}

	@Override
	public void nextRows(Query q, ResultIterator<?> it) {
		// TODO Auto-generated method stub

	}

	@Override
	public void nextGeneratedRows(Query query, ResultIterator<?> keys, List<ObjectId> idsToUpdate) {
		// do
	}

	@Override
	public void nextQueryException(Query query, Exception ex) {
		throw new CayenneRuntimeException(ex);
	}

	@Override
	public void nextGlobalException(Exception ex) {
		throw new CayenneRuntimeException(ex);
	}
}
