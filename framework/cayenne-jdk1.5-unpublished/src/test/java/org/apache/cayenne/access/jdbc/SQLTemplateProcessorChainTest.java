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

package org.apache.cayenne.access.jdbc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class SQLTemplateProcessorChainTest extends TestCase {

    public void testProcessTemplateNoChunks() throws Exception {
        // whatever is inside the chain, it should render as empty if there
        // is no chunks...

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                "#chain(' AND ') #end",
                Collections.EMPTY_MAP);

        assertEquals("", compiled.getSql());

        compiled = new SQLTemplateProcessor().processTemplate(
                "#chain(' AND ') garbage #end",
                Collections.EMPTY_MAP);

        assertEquals("", compiled.getSql());

        compiled = new SQLTemplateProcessor().processTemplate(
                "#chain(' AND ' 'PREFIX') #end",
                Collections.EMPTY_MAP);

        assertEquals("", compiled.getSql());
        compiled = new SQLTemplateProcessor().processTemplate(
                "#chain(' AND ' 'PREFIX') garbage #end",
                Collections.EMPTY_MAP);

        assertEquals("", compiled.getSql());
    }

    public void testProcessTemplateFullChain() throws Exception {
        String template = "#chain(' OR ')"
                + "#chunk($a)$a#end"
                + "#chunk($b)$b#end"
                + "#chunk($c)$c#end"
                + "#end";

        Map map = new HashMap();
        map.put("a", "[A]");
        map.put("b", "[B]");
        map.put("c", "[C]");

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(template, map);
        assertEquals("[A] OR [B] OR [C]", compiled.getSql());
    }

    public void testProcessTemplateFullChainAndPrefix() throws Exception {
        String template = "#chain(' OR ' 'WHERE ')"
                + "#chunk($a)$a#end"
                + "#chunk($b)$b#end"
                + "#chunk($c)$c#end"
                + "#end";

        Map map = new HashMap();
        map.put("a", "[A]");
        map.put("b", "[B]");
        map.put("c", "[C]");

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(template, map);
        assertEquals("WHERE [A] OR [B] OR [C]", compiled.getSql());
    }

    public void testProcessTemplatePartialChainMiddle() throws Exception {
        String template = "#chain(' OR ' 'WHERE ')"
                + "#chunk($a)$a#end"
                + "#chunk($b)$b#end"
                + "#chunk($c)$c#end"
                + "#end";

        Map map = new HashMap();
        map.put("a", "[A]");
        map.put("c", "[C]");

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(template, map);
        assertEquals("WHERE [A] OR [C]", compiled.getSql());
    }

    public void testProcessTemplatePartialChainStart() throws Exception {
        String template = "#chain(' OR ' 'WHERE ')"
                + "#chunk($a)$a#end"
                + "#chunk($b)$b#end"
                + "#chunk($c)$c#end"
                + "#end";

        Map map = new HashMap();
        map.put("b", "[B]");
        map.put("c", "[C]");

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(template, map);
        assertEquals("WHERE [B] OR [C]", compiled.getSql());
    }

    public void testProcessTemplatePartialChainEnd() throws Exception {
        String template = "#chain(' OR ' 'WHERE ')"
                + "#chunk($a)$a#end"
                + "#chunk($b)$b#end"
                + "#chunk($c)$c#end"
                + "#end";

        Map map = new HashMap();
        map.put("a", "[A]");
        map.put("b", "[B]");

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(template, map);
        assertEquals("WHERE [A] OR [B]", compiled.getSql());
    }

    public void testProcessTemplateChainWithGarbage() throws Exception {
        String template = "#chain(' OR ' 'WHERE ')"
                + "#chunk($a)$a#end"
                + " some other stuff"
                + "#chunk($c)$c#end"
                + "#end";

        Map map = new HashMap();
        map.put("a", "[A]");
        map.put("c", "[C]");

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(template, map);
        assertEquals("WHERE [A] some other stuff OR [C]", compiled.getSql());
    }

    public void testProcessTemplateChainUnconditionalChunks() throws Exception {
        String template = "#chain(' OR ' 'WHERE ')"
                + "#chunk()C1#end"
                + "#chunk()C2#end"
                + "#chunk()C3#end"
                + "#end";

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                template,
                Collections.EMPTY_MAP);
        assertEquals("WHERE C1 OR C2 OR C3", compiled.getSql());
    }

    public void testProcessTemplateEmptyChain() throws Exception {
        String template = "#chain(' OR ' 'WHERE ')"
                + "#chunk($a)$a#end"
                + "#chunk($b)$b#end"
                + "#chunk($c)$c#end"
                + "#end";

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                template,
                Collections.EMPTY_MAP);
        assertEquals("", compiled.getSql());
    }

    public void testProcessTemplateWithFalseOrZero1() throws Exception {
        String template = "#chain(' OR ' 'WHERE ')"
                + "#chunk($a)[A]#end"
                + "#chunk($b)[B]#end"
                + "#chunk($c)$c#end"
                + "#end";

        Map map = new HashMap();
        map.put("a", false);
        map.put("b", 0);

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(template, map);
        assertEquals("WHERE [A] OR [B]", compiled.getSql());
    }

    public void testProcessTemplateWithFalseOrZero2() throws Exception {
        String template = "#chain(' OR ' 'WHERE ')"
                + "#chunk($a)$a#end"
                + "#chunk($b)$b#end"
                + "#chunk($c)$c#end"
                + "#end";

        Map map = new HashMap();
        map.put("a", false);
        map.put("b", 0);

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(template, map);
        assertEquals("WHERE false OR 0", compiled.getSql());
    }

}
