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

package org.apache.cayenne.rop.http;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.rop.ROPSerializationService;
import org.apache.cayenne.rop.ServerHessianSerializationServiceProvider;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import static org.junit.Assert.*;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class HessianROPSerializationServiceIT extends ClientCase {
	
	@Inject
	private CayenneContext context;

	@Test
	public void testByteArraySerialization() throws Exception {
		ClientMtTable1 table1 = context.newObject(ClientMtTable1.class);
		table1.setGlobalAttribute1("Test table1");

		ClientMtTable2 table2 = context.newObject(ClientMtTable2.class);
		table2.setGlobalAttribute("Test table2");
		table2.setTable1(table1);

		ROPSerializationService clientService = createClientSerializationService();
		ROPSerializationService serverService = createServerSerializationService();
		
		// test client to server serialization
		byte[] data = clientService.serialize(table2);
		ClientMtTable2 serverTable2 = serverService.deserialize(data, ClientMtTable2.class);
		
		assertEquals("Test table2", serverTable2.getGlobalAttribute());
		assertEquals("Test table1", serverTable2.getTable1().getGlobalAttribute1());
		
		// test server to client serialization
		data = serverService.serialize(table2);
		ClientMtTable2 clientTable2 = clientService.deserialize(data, ClientMtTable2.class);
		
		assertEquals("Test table2", clientTable2.getGlobalAttribute());
		assertEquals("Test table1", clientTable2.getTable1().getGlobalAttribute1());
	}

	@Test
	public void testStreamSerialization() throws Exception {
		ClientMtTable1 table1 = context.newObject(ClientMtTable1.class);
		table1.setGlobalAttribute1("Test table1");

		ClientMtTable2 table2 = context.newObject(ClientMtTable2.class);
		table2.setGlobalAttribute("Test table2");
		table2.setTable1(table1);

		ROPSerializationService clientService = createClientSerializationService();
		ROPSerializationService serverService = createServerSerializationService();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		// test client to server serialization
		clientService.serialize(table2, out);
		out.flush();
		ClientMtTable2 serverTable2 = serverService.deserialize(
				new ByteArrayInputStream(out.toByteArray()), ClientMtTable2.class);

		assertEquals("Test table2", serverTable2.getGlobalAttribute());
		assertEquals("Test table1", serverTable2.getTable1().getGlobalAttribute1());

		// test server to client serialization
		out = new ByteArrayOutputStream();
		serverService.serialize(table2, out);
		out.flush();
		ClientMtTable2 clientTable2 = clientService.deserialize(
				new ByteArrayInputStream(out.toByteArray()), ClientMtTable2.class);

		assertEquals("Test table2", clientTable2.getGlobalAttribute());
		assertEquals("Test table1", clientTable2.getTable1().getGlobalAttribute1());
	}

	private ROPSerializationService createClientSerializationService() {
		return new ClientHessianSerializationServiceProvider().get();
	}

	private ROPSerializationService createServerSerializationService() {
		return new ServerHessianSerializationServiceProvider(() -> context.getChannel(), Collections.emptyList()).get();
	}
	
}
