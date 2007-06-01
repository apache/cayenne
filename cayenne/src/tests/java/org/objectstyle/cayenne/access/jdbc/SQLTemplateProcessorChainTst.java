/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access.jdbc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * @author Andrei Adamchik
 */
public class SQLTemplateProcessorChainTst extends BasicTestCase {

    public void testProcessTemplateNoChunks() throws Exception {
        // whatever is inside the chain, it should render as empty if there
        // is no chunks...

        SQLStatement compiled =
            new SQLTemplateProcessor().processTemplate(
                "#chain(' AND ') #end",
                Collections.EMPTY_MAP);

        assertEquals("", compiled.getSql());

        compiled =
            new SQLTemplateProcessor().processTemplate(
                "#chain(' AND ') garbage #end",
                Collections.EMPTY_MAP);

        assertEquals("", compiled.getSql());

        compiled =
            new SQLTemplateProcessor().processTemplate(
                "#chain(' AND ' 'PREFIX') #end",
                Collections.EMPTY_MAP);

        assertEquals("", compiled.getSql());
        compiled =
            new SQLTemplateProcessor().processTemplate(
                "#chain(' AND ' 'PREFIX') garbage #end",
                Collections.EMPTY_MAP);

        assertEquals("", compiled.getSql());
    }

    public void testProcessTemplateFullChain() throws Exception {
        String template =
            "#chain(' OR ')"
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
        String template =
            "#chain(' OR ' 'WHERE ')"
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
        String template =
            "#chain(' OR ' 'WHERE ')"
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
        String template =
            "#chain(' OR ' 'WHERE ')"
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
        String template =
            "#chain(' OR ' 'WHERE ')"
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
        String template =
            "#chain(' OR ' 'WHERE ')"
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
        String template =
            "#chain(' OR ' 'WHERE ')"
                + "#chunk()C1#end"
                + "#chunk()C2#end"
                + "#chunk()C3#end"
                + "#end";

        SQLStatement compiled =
            new SQLTemplateProcessor().processTemplate(template, Collections.EMPTY_MAP);
        assertEquals("WHERE C1 OR C2 OR C3", compiled.getSql());
    }

    public void testProcessTemplateEmptyChain() throws Exception {
        String template =
            "#chain(' OR ' 'WHERE ')"
                + "#chunk($a)$a#end"
                + "#chunk($b)$b#end"
                + "#chunk($c)$c#end"
                + "#end";

        SQLStatement compiled =
            new SQLTemplateProcessor().processTemplate(template, Collections.EMPTY_MAP);
        assertEquals("", compiled.getSql());
    }

}
