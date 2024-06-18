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

package org.apache.cayenne.dba;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.qualified.Qualified1;
import org.apache.cayenne.unit.DerbyUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.QUALIFIED_PROJECT)
public class ConcurrentPkGeneratorIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

	@Inject
	private UnitDbAdapter unitDbAdapter;

	@Before
	public void prepareDerbyDb() {
		//use to fix random test failures on derby db
		if(unitDbAdapter instanceof DerbyUnitDbAdapter) {
			try(Connection connection = runtime.getDataDomain().getDataNode("qualified").getDataSource().getConnection()){
				CallableStatement cs =
						connection.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(?, ?)");
				cs.setString(1, "derby.language.sequence.preallocator");
				cs.setString(2, "1000");
				cs.execute();
				cs.close();
			} catch (SQLException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

    /*
     * Attempts to discover any problems regarding thread locking in the PkGenerator
     */
    @Test
    public void testConcurrentInserts() {
    	if(!unitDbAdapter.supportsPKGeneratorConcurrency()) {
    		return;
		}

		final DataMap dataMap = runtime.getDataDomain().getDataMap("qualified");
		
		// clear out the table
		ObjectContext context = runtime.newContext();
		List<Qualified1> qualified1s = context.select(ObjectSelect.query(Qualified1.class));
		context.deleteObjects(qualified1s);
		context.commitChanges();
		
		// perform concurrent inserts
		int numThreads = 2;
		int insertsPerThread = 100;

		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		Runnable task = () -> {
            try {
                ObjectContext context1 = runtime.newContext();
				EntityResolver entityResolver = context1.getEntityResolver();
				for (ObjEntity entity : dataMap.getObjEntities()) {
                    context1.newObject(entityResolver.getObjectFactory().getJavaClass(entity.getJavaClassName()));
                }
                context1.commitChanges();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
		
		for (int j = 0; j < insertsPerThread; j++) {
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
		qualified1s = context.select(ObjectSelect.query(Qualified1.class));
		assertEquals(insertsPerThread * numThreads, qualified1s.size());

		// PKs will be used in order most of the time, but the implementation doesn't guarantee it.
//		qualified1s.sort(Comparator.comparing(Cayenne::intPKForObject));
//
//		int lastPk = Cayenne.intPKForObject(qualified1s.get(0)) - 1;
//		for (Qualified1 qualified1 : qualified1s) {
//			if (lastPk+1 != Cayenne.intPKForObject(qualified1)) {
//				fail("Found gap in sequence number: " + lastPk + " - " + Cayenne.intPKForObject(qualified1));
//			}
//			lastPk++;
//		}
    }
    
}
