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

package org.apache.cayenne.util;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XMLEncoderTest {

    private static final String LS = System.lineSeparator();

    @Test
    public void textTag() {
        assertEquals("<a name=\"x\">" + LS
                        + "\t<comment>text</comment>" + LS
                        + "</a>" + LS,
                encode(e -> e.start("a").attribute("name", "x").textTag("comment", "text").end()));
    }

    @Test
    public void textTagEscaped() {
        assertEquals("<comment>a &lt; b &amp;&amp; c &gt; d, \"e\" 'f'</comment>" + LS,
                encode(e -> e.textTag("comment", "a < b && c > d, \"e\" 'f'")));
    }

    @Test
    public void textTagMultiLine() {
        assertEquals("<comment> line 1" + LS + "line 2 </comment>" + LS,
                encode(e -> e.textTag("comment", " line 1" + LS + "line 2 ")));
    }

    @Test
    public void textTagBlank() {
        assertEquals("<a/>" + LS, encode(e -> e.start("a").textTag("comment", null).end()));
        assertEquals("<a/>" + LS, encode(e -> e.start("a").textTag("comment", "").end()));
        assertEquals("<a/>" + LS, encode(e -> e.start("a").textTag("comment", " \t" + LS).end()));
    }

    @Test
    public void textTagFollowedByTag() {
        assertEquals("<a>" + LS
                        + "\t<comment>text</comment>" + LS
                        + "\t<b/>" + LS
                        + "</a>" + LS,
                encode(e -> e.start("a").textTag("comment", "text").start("b").end().end()));
    }

    private static String encode(Consumer<XMLEncoder> encoding) {
        StringWriter out = new StringWriter();
        encoding.accept(new XMLEncoder(new PrintWriter(out), "\t"));
        return out.toString();
    }
}
