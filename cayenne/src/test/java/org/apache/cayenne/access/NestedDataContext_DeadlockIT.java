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
package org.apache.cayenne.access;

import java.util.List;
import java.util.Random;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class NestedDataContext_DeadlockIT extends RuntimeCase {

	@Inject
	private DataContext parent;

	@Inject
	private CayenneRuntime runtime;

	@Inject
	protected DBHelper dbHelper;

	protected TableHelper tArtist;

	@Before
	public void setUp() throws Exception {
		tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
	}

	private void createArtists() throws Exception {
		for (int i = 0; i < 300; i++) {
			tArtist.insert(i + 1, "X" + i);
		}
	}

	@Test
	public void testDeadlock() throws Exception {

		createArtists();

		final Thread[] threads = new Thread[2];

		Random rnd = new Random(System.currentTimeMillis());
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new UpdateThread("UpdateThread-" + i, runtime.newContext(parent), rnd);
		}

		for (Thread thread : threads) {
			thread.start();
		}

		new ParallelTestContainer() {

			@Override
			protected void assertResult() throws Exception {
				for (Thread thread : threads) {
					// unfortunately here we'll have to leave some dead threads
					// behind... Of course if there's no deadlock, there won't
					// be a leak either
					assertFalse("Deadlocked thread", thread.isAlive());
				}
			}
		}.runTest(40000);

	}

	static class UpdateThread extends Thread {

		protected ObjectContext nestedContext;
		protected Random rnd;

		UpdateThread(String name, ObjectContext nestedContext, Random rnd) {
			super(name);
			setDaemon(true);
			this.nestedContext = nestedContext;
			this.rnd = rnd;
		}

		@Override
		public void run() {

			List<Artist> artists = nestedContext.select(ObjectSelect.query(Artist.class));

			for (int i = 0; i < 100; i++) {

				for (int j = 0; j < 5; j++) {
					int index = rnd.nextInt(artists.size());
					Artist a = artists.get(index);
					a.setArtistName("Y" + rnd.nextInt());
				}

				nestedContext.commitChanges();

				// ensure other threads get a chance to run too
				Thread.yield();
			}
		}
	}
}
