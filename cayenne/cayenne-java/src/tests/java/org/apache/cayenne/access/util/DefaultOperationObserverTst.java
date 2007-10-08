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


package org.apache.cayenne.access.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DefaultOperationObserverTst extends CayenneTestCase {
	protected DefaultOperationObserver observer;

	public void setUp() throws Exception {
		observer = new DefaultOperationObserver();
	}

	public void testHasExceptions1() throws Exception {
		Logger observerLogger = Logger.getLogger(DefaultOperationObserver.class);
        Level oldLevel = observerLogger.getLevel();
        observerLogger.setLevel(Level.ERROR);

		try {
			assertFalse(observer.hasExceptions());
			observer.nextGlobalException(new Exception());
			assertTrue(observer.hasExceptions());
		} finally {
			observerLogger.setLevel(oldLevel);
		}
	}

	public void testHasExceptions2() throws Exception {
		Logger observerLogger = Logger.getLogger(DefaultOperationObserver.class);
        Level oldLevel = observerLogger.getLevel();
        observerLogger.setLevel(Level.ERROR);

		try {
			assertFalse(observer.hasExceptions());
			observer.nextQueryException(new SelectQuery(), new Exception());
			assertTrue(observer.hasExceptions());
		} finally {
			observerLogger.setLevel(oldLevel);
		}
	}
}
