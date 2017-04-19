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
package org.apache.cayenne.lifecycle.postcommit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.lifecycle.changemap.ChangeMap;
import org.junit.Test;

public class PostCommitModuleBuilderTest {

	@Test
	public void testListener_Object() {

		L listener = new L();
		Module m = PostCommitModuleBuilder.builder().listener(listener).build();

		Injector i = DIBootstrap.createInjector(m);
		List<PostCommitListener> listeners = i.getInstance(Key.getListOf(PostCommitListener.class));
		assertEquals(1, listeners.size());
		assertTrue(listeners.contains(listener));
	}

	@Test
	public void testListener_Class() {

		Module m = PostCommitModuleBuilder.builder().listener(L.class).build();

		Injector i = DIBootstrap.createInjector(m);
		List<PostCommitListener> listeners = i.getInstance(Key.getListOf(PostCommitListener.class));
		assertEquals(1, listeners.size());
		assertTrue(listeners.get(0) instanceof L);
	}

	public static class L implements PostCommitListener {

		@Override
		public void onPostCommit(ObjectContext originatingContext, ChangeMap changes) {
			// do nothing.
		}
	}

}
