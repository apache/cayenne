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

package org.apache.cayenne.test.parallel;

/**
 * Helper class allowing unit tests to wait till a code in question executes in
 * a separate thread. There is still some element of uncertainty remains, since
 * this implementation simply tries to give other threads enough time to
 * execute, instead of watching for threads activity.
 * 
 * <p>
 * Note that result sampling is done every 300 ms., so if the test succeeds
 * earlier, test case wouldn't have to wait for the whole time period specified
 * by timeout.
 * </p>
 * 
 */
public abstract class ParallelTestContainer {

	protected abstract void assertResult() throws Exception;

	public void runTest(long timeoutMs) throws Exception {
		long checkEveryXMs;
		int maxMumberOfChecks;

		if (timeoutMs < 300) {
			maxMumberOfChecks = 1;
			checkEveryXMs = timeoutMs;
		} else {
			maxMumberOfChecks = Math.round(timeoutMs / 300.00f);
			checkEveryXMs = 300;
		}

		// TODO: for things asserting that a certain event DID NOT happen
		// we need a better implementation, that should probably sleep for
		// the whole timeout interval, since otherwise we may have a false
		// positive (i.e. assertion succeeded not because a certain thing did
		// not happen, but rather cause it happened after the assertion was
		// run).

		// for now lets wait for at least one time slice to decrease
		// the possibility of false positives
		Thread.sleep(checkEveryXMs);
		maxMumberOfChecks--;

		for (int i = 0; i < maxMumberOfChecks; i++) {
			try {
				assertResult();

				// success... return immediately
				return;
			} catch (Throwable th) {
				// wait some more
				Thread.sleep(checkEveryXMs);
			}
		}

		// if it throws, it throws...
		assertResult();
	}
}
