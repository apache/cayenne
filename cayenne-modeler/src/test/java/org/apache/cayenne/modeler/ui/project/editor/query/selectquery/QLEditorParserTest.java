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
package org.apache.cayenne.modeler.ui.project.editor.query.selectquery;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;

import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QLEditorParserTest {

    @Test
    public void errorRangeOfUnexpectedToken() throws Exception {
        String ql = "from Artist\nwhere name = = 1";
        assertArrayEquals(new int[]{ql.lastIndexOf('='), 1}, QLEditorParser.errorRange(parseError(ql), document(ql)));
    }

    @Test
    public void errorRangeAtTheEnd() throws Exception {
        // the missing condition is reported past the end of the text, and shown on its last char
        String ql = "from Artist where";
        assertArrayEquals(new int[]{ql.length() - 1, 1}, QLEditorParser.errorRange(parseError(ql), document(ql)));
    }

    @Test
    public void errorRangeOfMultiCharToken() throws Exception {
        String ql = "from Artist where name = 'a' foo";
        assertArrayEquals(new int[]{ql.indexOf("foo"), 3}, QLEditorParser.errorRange(parseError(ql), document(ql)));
    }

    @Test
    public void errorRangeOfNonConditionOperand() throws Exception {
        String ql = "from Artist where artistName and a = 1";
        assertArrayEquals(new int[]{ql.indexOf("artistName"), 10},
                QLEditorParser.errorRange(parseError(ql), document(ql)));
    }

    @Test
    public void occurrences() throws Exception {
        String ql = "select nme from Artist\nwhere nme = 1 and db:NME = 2 and nme.x = 3";
        RSyntaxDocument doc = document(ql);

        assertEquals(List.of(ql.indexOf("nme") + ":3", ql.indexOf("nme", 10) + ":3"),
                ranges(QLEditorParser.occurrences(doc, "nme")));
        assertEquals(List.of(ql.indexOf("db:NME") + ":6"), ranges(QLEditorParser.occurrences(doc, "db:NME")));
    }

    @Test
    public void parse() throws Exception {
        DataMap map = new DataMap("map");
        ObjEntity artist = new ObjEntity("Artist");
        artist.addAttribute(new ObjAttribute("name", String.class.getName(), artist));
        map.addObjEntity(artist);

        SelectQueryDescriptor query = new SelectQueryDescriptor();
        query.setName("q");
        map.addQueryDescriptor(query);

        List<List<String>> reported = new ArrayList<>();
        QLEditorParser parser = new QLEditorParser(() -> query, reported::add);

        String valid = "from Artist where name = $name";
        query.setQl(valid);
        assertEquals(List.of(), parser.parse(document(valid), QLTokenMaker.SYNTAX_STYLE).getNotices());
        assertEquals(List.of(), reported.getLast());

        String badPath = "from Artist where nme = $name";
        query.setQl(badPath);
        ParseResult result = parser.parse(document(badPath), QLTokenMaker.SYNTAX_STYLE);
        assertEquals(List.of(badPath.indexOf("nme") + ":3"), notices(result));
        assertEquals(List.of("Can't resolve path 'nme' in Artist"), reported.getLast());

        String badSyntax = "from Artist where";
        query.setQl(badSyntax);
        assertEquals(List.of((badSyntax.length() - 1) + ":1"),
                notices(parser.parse(document(badSyntax), QLTokenMaker.SYNTAX_STYLE)));
        assertEquals(1, reported.getLast().size());
    }

    private static ExpressionException parseError(String ql) {
        SelectQueryDescriptor query = new SelectQueryDescriptor();
        query.setQl(ql);
        return assertThrows(ExpressionException.class, query::getParsedQuery);
    }

    private static RSyntaxDocument document(String text) throws BadLocationException {
        RSyntaxDocument doc = new RSyntaxDocument(null, QLTokenMaker.SYNTAX_STYLE);
        doc.setSyntaxStyle(new QLTokenMaker());
        doc.insertString(0, text, null);
        return doc;
    }

    private static List<String> ranges(List<int[]> ranges) {
        return ranges.stream().map(r -> r[0] + ":" + r[1]).toList();
    }

    private static List<String> notices(ParseResult result) {
        return result.getNotices().stream().map(ParserNotice.class::cast).map(n -> n.getOffset() + ":" + n.getLength())
                .toList();
    }
}
