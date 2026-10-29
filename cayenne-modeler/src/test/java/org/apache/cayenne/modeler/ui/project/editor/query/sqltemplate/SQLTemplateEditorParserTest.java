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
package org.apache.cayenne.modeler.ui.project.editor.query.sqltemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.text.BadLocationException;

import org.apache.cayenne.modeler.ui.project.editor.query.sqltemplate.SQLTemplateEditorParser.Check;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLTemplateEditorParserTest {

    private final List<Check> checks = new ArrayList<>();
    private final SQLTemplateEditorParser parser = new SQLTemplateEditorParser(checks::add);

    @Test
    public void valid() throws Exception {
        String sql = """
                SELECT #result('ID' 'int'), NAME FROM ARTIST
                WHERE NAME = #bind($name) #if($id) AND ID = $id #end""";

        assertEquals(List.of(), notices(parse(sql)));
        assertEquals(new Check(List.of(), List.of(), Set.of("name", "id")), checks.getLast());
        assertEquals(List.of("name", "id"), List.copyOf(checks.getLast().parameters()));
    }

    @Test
    public void helperIsNotAParameter() throws Exception {
        parse("WHERE #bindEqual($helper.cayenneExp($a, 'db:ID'))");
        assertEquals(Set.of("a"), checks.getLast().parameters());
    }

    @Test
    public void empty() throws Exception {
        assertEquals(List.of(), notices(parse("  \n")));
        assertEquals(List.of("The SQL is empty"), checks.getLast().warnings());
    }

    @Test
    public void unexpectedToken() throws Exception {
        String sql = "SELECT * FROM ARTIST\n#end";
        assertEquals(List.of(sql.indexOf("#end") + ":4:ERROR"), notices(parse(sql)));
        assertEquals(List.of("Unexpected '#end'"), checks.getLast().errors());
    }

    @Test
    public void unexpectedTokenAfterTabs() throws Exception {
        // a tab is a single column in the editor
        String sql = "SELECT\t\t*\n\t\t#end";
        assertEquals(List.of(sql.indexOf("#end") + ":4:ERROR"), notices(parse(sql)));
    }

    @Test
    public void unclosedDirective() throws Exception {
        // the missing bracket is reported past the end of the text, and shown on its last char
        String sql = "WHERE ID = #bind($id";
        assertEquals(List.of((sql.length() - 1) + ":1:ERROR"), notices(parse(sql)));
        assertEquals(List.of("Unexpected end of the template, expected ')'"), checks.getLast().errors());
    }

    @Test
    public void unclosedCondition() throws Exception {
        parse("WHERE #if($a) A = $a");
        assertEquals(List.of("Unexpected end of the template, expected '#end'"), checks.getLast().errors());
    }

    @Test
    public void directiveWithoutArguments() throws Exception {
        String sql = "SELECT #result 'ID'";
        assertEquals(List.of(sql.indexOf(" '") + ":2:ERROR"), notices(parse(sql)));
        assertEquals(List.of("Expected '('"), checks.getLast().errors());
    }

    @Test
    public void unclosedString() throws Exception {
        String sql = "WHERE ID = #bind('abc";
        assertEquals(1, notices(parse(sql)).size());
        assertEquals(List.of("Unexpected end of the template"), checks.getLast().errors());
    }

    @Test
    public void unknownDirective() throws Exception {
        String sql = "WHERE #foo($a) AND #foo(1) AND #bind($b)";
        assertEquals(List.of(sql.indexOf("#foo") + ":4:WARNING", sql.lastIndexOf("#foo") + ":4:WARNING"),
                notices(parse(sql)));
        assertEquals(List.of(), checks.getLast().errors());
        assertEquals(List.of("Unknown directive #foo"), checks.getLast().warnings());
        assertEquals(Set.of("a", "b"), checks.getLast().parameters());
    }

    @Test
    public void velocityDirective() throws Exception {
        // the Cayenne template engine parses it, but doesn't know it
        parse("WHERE #chain('AND') #chunk($a) A = #bind($a) #end #end");
        assertEquals(List.of(), checks.getLast().errors());
        assertEquals(1, checks.getLast().warnings().size());
    }

    @Test
    public void velocityTemplate() throws Exception {
        // the Cayenne template engine can't parse it
        assertEquals(List.of(), notices(parse("SELECT #foreach($c in $columns) $c, #end 1 FROM T")));
        assertEquals(List.of(), checks.getLast().errors());
        assertEquals(List.of("The template uses the Velocity directive #foreach, so it isn't checked"),
                checks.getLast().warnings());
    }

    private ParseResult parse(String sql) throws BadLocationException {
        RSyntaxDocument doc = new RSyntaxDocument(null, SQLTemplateTokenMaker.SYNTAX_STYLE);
        doc.setSyntaxStyle(new SQLTemplateTokenMaker());
        doc.insertString(0, sql, null);
        return parser.parse(doc, SQLTemplateTokenMaker.SYNTAX_STYLE);
    }

    private static List<String> notices(ParseResult result) {
        return result.getNotices().stream()
                .map(ParserNotice.class::cast)
                .map(n -> n.getOffset() + ":" + n.getLength() + ":" + n.getLevel())
                .toList();
    }
}
