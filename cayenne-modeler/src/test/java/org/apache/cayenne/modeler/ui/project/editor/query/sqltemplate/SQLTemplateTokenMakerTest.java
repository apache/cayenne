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

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.Token;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLTemplateTokenMakerTest {

    @Test
    public void directivesAndVariables() {
        assertEquals(List.of(
                "SELECT:" + Token.RESERVED_WORD,
                "#result:" + Token.PREPROCESSOR,
                "(:" + Token.SEPARATOR,
                "'ID':" + Token.LITERAL_STRING_DOUBLE_QUOTE,
                "'int':" + Token.LITERAL_STRING_DOUBLE_QUOTE,
                "):" + Token.SEPARATOR,
                "from:" + Token.RESERVED_WORD,
                "t0.ARTIST:" + Token.IDENTIFIER,
                "where:" + Token.RESERVED_WORD,
                "a:" + Token.IDENTIFIER,
                "=:" + Token.OPERATOR,
                "#bind:" + Token.PREPROCESSOR,
                "(:" + Token.SEPARATOR,
                "$a:" + Token.VARIABLE,
                "):" + Token.SEPARATOR,
                "AND:" + Token.OPERATOR,
                "b:" + Token.IDENTIFIER,
                "=:" + Token.OPERATOR,
                "$b:" + Token.VARIABLE
        ), tokens("SELECT #result('ID' 'int') from t0.ARTIST where a = #bind($a) AND b = $b"));
    }

    @Test
    public void conditions() {
        assertEquals(List.of(
                "#if:" + Token.PREPROCESSOR,
                "(:" + Token.SEPARATOR,
                "$a:" + Token.VARIABLE,
                "):" + Token.SEPARATOR,
                "#else:" + Token.PREPROCESSOR,
                "#end:" + Token.PREPROCESSOR
        ), tokens("#if($a) #else #end"));
    }

    @Test
    public void variablesInStrings() {
        assertEquals(List.of(
                "like:" + Token.OPERATOR,
                "'%:" + Token.LITERAL_STRING_DOUBLE_QUOTE,
                "$name:" + Token.VARIABLE,
                "%':" + Token.LITERAL_STRING_DOUBLE_QUOTE
        ), tokens("like '%$name%'"));
        assertEquals(List.of(
                "':" + Token.LITERAL_STRING_DOUBLE_QUOTE,
                "$a:" + Token.VARIABLE,
                "$b:" + Token.VARIABLE,
                "':" + Token.LITERAL_STRING_DOUBLE_QUOTE
        ), tokens("'$a$b'"));
        assertEquals(List.of(
                "'open:" + Token.ERROR_STRING_DOUBLE
        ), tokens("'open"));
    }

    @Test
    public void comments() {
        assertEquals(List.of("a:" + Token.IDENTIFIER, "-- note:" + Token.COMMENT_EOL), tokens("a -- note"));
        assertEquals(List.of("a:" + Token.IDENTIFIER, "## note:" + Token.COMMENT_EOL), tokens("a ## note"));
        assertEquals(List.of("/* x */:" + Token.COMMENT_MULTILINE, "b:" + Token.IDENTIFIER), tokens("/* x */ b"));
    }

    @Test
    public void multiLineComment() {
        // a comment that doesn't end on its line passes its type to the next line
        Token first = tokenList("a /* open", Token.NULL);
        assertEquals(Token.COMMENT_MULTILINE, last(first).getType());

        assertEquals(List.of("still */:" + Token.COMMENT_MULTILINE, "b:" + Token.IDENTIFIER),
                lexemes(tokenList("still */ b", Token.COMMENT_MULTILINE)));
        assertEquals(Token.COMMENT_MULTILINE, last(tokenList("no end", Token.COMMENT_MULTILINE)).getType());
    }

    @Test
    public void quotedIdentifiers() {
        assertEquals(List.of("\"select\":" + Token.IDENTIFIER, "`from`:" + Token.IDENTIFIER),
                tokens("\"select\" `from`"));
    }

    @Test
    public void emptyLine() {
        assertEquals(List.of(), tokens(""));
    }

    // the non-whitespace tokens of a line as "lexeme:type"
    private static List<String> tokens(String line) {
        return lexemes(tokenList(line, Token.NULL));
    }

    private static Token tokenList(String line, int initialTokenType) {
        // the line is placed in the middle of a larger array, like in a document
        char[] array = ("##" + line + "##").toCharArray();
        Segment segment = new Segment(array, 2, line.length());
        return new SQLTemplateTokenMaker().getTokenList(segment, initialTokenType, 100);
    }

    private static List<String> lexemes(Token first) {
        List<String> tokens = new ArrayList<>();
        for (Token t = first; t != null && t.isPaintable(); t = t.getNextToken()) {
            if (t.getType() != Token.WHITESPACE) {
                tokens.add(t.getLexeme() + ":" + t.getType());
            }
        }
        return tokens;
    }

    private static Token last(Token first) {
        Token last = first;
        while (last.getNextToken() != null) {
            last = last.getNextToken();
        }
        return last;
    }
}
