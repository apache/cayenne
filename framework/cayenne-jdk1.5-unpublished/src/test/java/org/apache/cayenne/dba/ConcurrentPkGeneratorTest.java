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

package org.apache.cayenne.dba;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.qualified.Qualified1;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime("cayenne-default.xml")
public class ConcurrentPkGeneratorTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;
    
    /*
     * Attempts to discover any problems regarding thread locking in the PkGenerator
     */
    public void testConcurrentInserts() throws Exception {
		final DataMap dataMap = runtime.getDataDomain().getDataMap("qualified");
		
		// clear out the table
		ObjectContext context = runtime.newContext();
		List<Qualified1> qualified1s = context.select(SelectQuery.query(Qualified1.class, null));
		context.deleteObjects(qualified1s);
		context.commitChanges();
		
		// perform concurrent inserts
		int numThreads = 2;
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		Runnable task = new Runnable() {
			public void run() {
				try {
					ObjectContext context = runtime.newContext();
					for (ObjEntity entity : dataMap.getObjEntities()) {
						context.newObject(entity.getJavaClass());
					}
					context.commitChanges();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		for (int j = 0; j < 100; j++) {
			for (int i = 0; i < numThreads; i++) {
				executor.submit(task);
			}
		}
		
		// check for completion or deadlock
		executor.shutdown();
		try {
			// normally this completes in less than 2 seconds. If it takes 30 then it failed.
			boolean didFinish = executor.awaitTermination(30, TimeUnit.SECONDS);
			if (!didFinish) {
				fail("Concurrent inserts either deadlocked or contended over the lock too long.");
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		// check for gaps in the generated sequence numbers
		qualified1s = context.select(SelectQuery.query(Qualified1.class, null));
		assertEquals(100 * numThreads, qualified1s.size());
		
		Collections.sort(qualified1s, new Comparator<Qualified1>() {
			public int compare(Qualified1 left, Qualified1 right) {
				Integer leftPk = Cayenne.intPKForObject(left);
				Integer rightPk = Cayenne.intPKForObject(right);
				return leftPk.compareTo(rightPk);
			}
		});
		
		// PKs will be used in order most of the time, but the implementation doesn't guarantee it.
//		int lastPk = Cayenne.intPKForObject(qualified1s.get(0)) - 1;
//		for (Qualified1 qualified1 : qualified1s) {
//			if (lastPk+1 != Cayenne.intPKForObject(qualified1)) {
//				fail("Found gap in sequence number: " + lastPk + " - " + Cayenne.intPKForObject(qualified1));
//			}
//			lastPk++;
//		}
    }
    
}
