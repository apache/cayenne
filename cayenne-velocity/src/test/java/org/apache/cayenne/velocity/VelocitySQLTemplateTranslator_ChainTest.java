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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.access.translator.sqltemplate.TranslatedSQL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VelocitySQLTemplateTranslator_ChainTest {

	private VelocitySQLTemplateTranslator processor;

	@BeforeEach
	public void before() {
		processor = new VelocitySQLTemplateTranslator();
	}

	@Test
	public void processTemplateNoChunks() throws Exception {
		// whatever is inside the chain, it should render as empty if there
		// is no chunks...

		TranslatedSQL compiled = processor.translate("#chain(' AND ') #end",
				Collections.<String, Object> emptyMap());
		assertEquals("", compiled.sql());

		compiled = processor.translate("#chain(' AND ') garbage #end", Collections.<String, Object> emptyMap());
		assertEquals("", compiled.sql());

		compiled = processor.translate("#chain(' AND ' 'PREFIX') #end", Collections.<String, Object> emptyMap());

		assertEquals("", compiled.sql());
		compiled = processor.translate("#chain(' AND ' 'PREFIX') garbage #end",
				Collections.<String, Object> emptyMap());

		assertEquals("", compiled.sql());
	}

	@Test
	public void processTemplateFullChain() throws Exception {
		String template = "#chain(' OR ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end" + "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", "[A]");
		map.put("b", "[B]");
		map.put("c", "[C]");

		TranslatedSQL compiled = processor.translate(template, map);
		assertEquals("[A] OR [B] OR [C]", compiled.sql());
	}

	@Test
	public void processTemplateFullChainAndPrefix() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", "[A]");
		map.put("b", "[B]");
		map.put("c", "[C]");

		TranslatedSQL compiled = processor.translate(template, map);
		assertEquals("WHERE [A] OR [B] OR [C]", compiled.sql());
	}

	@Test
	public void processTemplatePartialChainMiddle() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", "[A]");
		map.put("c", "[C]");

		TranslatedSQL compiled = processor.translate(template, map);
		assertEquals("WHERE [A] OR [C]", compiled.sql());
	}

	@Test
	public void processTemplatePartialChainStart() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("b", "[B]");
		map.put("c", "[C]");

		TranslatedSQL compiled = processor.translate(template, map);
		assertEquals("WHERE [B] OR [C]", compiled.sql());
	}

	@Test
	public void processTemplatePartialChainEnd() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", "[A]");
		map.put("b", "[B]");

		TranslatedSQL compiled = processor.translate(template, map);
		assertEquals("WHERE [A] OR [B]", compiled.sql());
	}

	@Test
	public void processTemplateChainWithGarbage() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + " some other stuff" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", "[A]");
		map.put("c", "[C]");

		TranslatedSQL compiled = processor.translate(template, map);
		assertEquals("WHERE [A] some other stuff OR [C]", compiled.sql());
	}

	@Test
	public void processTemplateChainUnconditionalChunks() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk()C1#end" + "#chunk()C2#end" + "#chunk()C3#end" + "#end";

		TranslatedSQL compiled = processor.translate(template, Collections.<String, Object> emptyMap());
		assertEquals("WHERE C1 OR C2 OR C3", compiled.sql());
	}

	@Test
	public void processTemplateEmptyChain() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		TranslatedSQL compiled = processor.translate(template, Collections.<String, Object> emptyMap());
		assertEquals("", compiled.sql());
	}

	@Test
	public void processTemplateWithFalseOrZero1() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)[A]#end" + "#chunk($b)[B]#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", false);
		map.put("b", 0);

		TranslatedSQL compiled = processor.translate(template, map);
		assertEquals("WHERE [A] OR [B]", compiled.sql());
	}

	@Test
	public void processTemplateWithFalseOrZero2() throws Exception {
		String template = "#chain(' OR ' 'WHERE ')" + "#chunk($a)$a#end" + "#chunk($b)$b#end" + "#chunk($c)$c#end"
				+ "#end";

		Map<String, Object> map = new HashMap<>();
		map.put("a", false);
		map.put("b", 0);

		TranslatedSQL compiled = processor.translate(template, map);
		assertEquals("WHERE false OR 0", compiled.sql());
	}

}
