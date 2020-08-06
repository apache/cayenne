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

package org.apache.cayenne.velocity;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.access.jdbc.SQLStatement;
import org.junit.Before;
import org.junit.Test;

public class VelocitySQLTemplateProcessor_ChainTest {

	private VelocitySQLTemplateProcessor processor;

	@Before
	public void before() {
		processor = new VelocitySQLTemplateProcessor();
	}

	@Test
	public void testProcessTemplateNoChunks() throws Exception {
		// whatever is inside the chain, it should render as empty if there
		// is no chunks...

		SQLStatement compiled = processor.processTemplate("#chain(' AND ') #end",
				Collections.<String, Object> emptyMap());
		assertEquals("", compiled.getSql());

		compiled = processor.processTemplate("#chain(' AND ') garbage #end", Collections.<String, Object> emptyMap());
		assertEquals("", compiled.getSql());

		compiled = processor.processTemplate("#chain(' AND ' 'PREFIX') #end", Collections.<String, Object> emptyMap());

		assertEquals("", compiled.getSql());
		compiled = processor.processTemplate("#chain(' AND ' 'PREFIX') garbage #end",
				Collections.<String, Object> emptyMap());

		assertEquals("", compiled.getSql());
	}

	@Test
	public void testProcessTemplateFullChain() throws Exception {
		String template = "#chain(' OR ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end" + "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", "[A]");
		map.put("b", "[B]");
		map.put("c", "[C]");

		SQLStatement compiled = processor.processTemplate(template, map);
		assertEquals("[A] OR [B] OR [C]", compiled.getSql());
	}

	@Test
	public void testProcessTemplateFullChainAndPrefix() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", "[A]");
		map.put("b", "[B]");
		map.put("c", "[C]");

		SQLStatement compiled = processor.processTemplate(template, map);
		assertEquals("WHERE [A] OR [B] OR [C]", compiled.getSql());
	}

	@Test
	public void testProcessTemplatePartialChainMiddle() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", "[A]");
		map.put("c", "[C]");

		SQLStatement compiled = processor.processTemplate(template, map);
		assertEquals("WHERE [A] OR [C]", compiled.getSql());
	}

	@Test
	public void testProcessTemplatePartialChainStart() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("b", "[B]");
		map.put("c", "[C]");

		SQLStatement compiled = processor.processTemplate(template, map);
		assertEquals("WHERE [B] OR [C]", compiled.getSql());
	}

	@Test
	public void testProcessTemplatePartialChainEnd() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", "[A]");
		map.put("b", "[B]");

		SQLStatement compiled = processor.processTemplate(template, map);
		assertEquals("WHERE [A] OR [B]", compiled.getSql());
	}

	@Test
	public void testProcessTemplateChainWithGarbage() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + " some other stuff" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", "[A]");
		map.put("c", "[C]");

		SQLStatement compiled = processor.processTemplate(template, map);
		assertEquals("WHERE [A] some other stuff OR [C]", compiled.getSql());
	}

	@Test
	public void testProcessTemplateChainUnconditionalChunks() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk()C1#end" + "#chunk()C2#end" + "#chunk()C3#end" + "#end";

		SQLStatement compiled = processor.processTemplate(template, Collections.<String, Object> emptyMap());
		assertEquals("WHERE C1 OR C2 OR C3", compiled.getSql());
	}

	@Test
	public void testProcessTemplateEmptyChain() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		SQLStatement compiled = processor.processTemplate(template, Collections.<String, Object> emptyMap());
		assertEquals("", compiled.getSql());
	}

	@Test
	public void testProcessTemplateWithFalseOrZero1() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)[A]#end" + "#chunk($b)[B]#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", false);
		map.put("b", 0);

		SQLStatement compiled = processor.processTemplate(template, map);
		assertEquals("WHERE [A] OR [B]", compiled.getSql());
	}

	@Test
	public void testProcessTemplateWithFalseOrZero2() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", false);
		map.put("b", 0);

		SQLStatement compiled = processor.processTemplate(template, map);
		assertEquals("WHERE false OR 0", compiled.getSql());
	}

}
