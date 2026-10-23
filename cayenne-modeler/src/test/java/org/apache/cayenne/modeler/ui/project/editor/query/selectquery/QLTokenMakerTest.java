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

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.Token;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QLTokenMakerTest {

    @Test
    public void clauses() {
        assertEquals(List.of(
                "select:" + Token.RESERVED_WORD,
                "distinct:" + Token.RESERVED_WORD,
                "name:" + Token.IDENTIFIER,
                "from:" + Token.RESERVED_WORD,
                "Artist:" + Token.IDENTIFIER,
                "where:" + Token.RESERVED_WORD,
                "name:" + Token.IDENTIFIER,
                "like:" + Token.OPERATOR,
                "$name:" + Token.VARIABLE,
                "and:" + Token.OPERATOR,
                "price:" + Token.IDENTIFIER,
                ">:" + Token.OPERATOR,
                "10.5:" + Token.LITERAL_NUMBER_FLOAT,
                "order:" + Token.RESERVED_WORD,
                "by:" + Token.RESERVED_WORD,
                "count:" + Token.FUNCTION,
                "(:" + Token.SEPARATOR,
                "paintings+:" + Token.IDENTIFIER,
                "):" + Token.SEPARATOR,
                "desc:" + Token.RESERVED_WORD
        ), tokens("select distinct name from Artist where name like $name and price > 10.5 "
                + "order by count(paintings+) desc"));
    }

    @Test
    public void pathPrefixes() {
        assertEquals(List.of(
                "db::" + Token.DATA_TYPE,
                "NAME:" + Token.IDENTIFIER,
                "=:" + Token.OPERATOR,
                "enum::" + Token.DATA_TYPE,
                "org.example.E.A:" + Token.IDENTIFIER
        ), tokens("db:NAME = enum:org.example.E.A"));
    }

    @Test
    public void keywordInPath() {
        // a keyword is only highlighted as a whole token
        assertEquals(List.of(
                "order.date:" + Token.IDENTIFIER,
                "=:" + Token.OPERATOR,
                "1:" + Token.LITERAL_NUMBER_DECIMAL_INT
        ), tokens("order.date = 1"));
    }

    @Test
    public void strings() {
        assertEquals(List.of(
                "'it\\'s':" + Token.LITERAL_STRING_DOUBLE_QUOTE,
                "\"a\":" + Token.LITERAL_STRING_DOUBLE_QUOTE,
                "'open:" + Token.ERROR_STRING_DOUBLE
        ), tokens("'it\\'s' \"a\" 'open"));
    }

    @Test
    public void literals() {
        assertEquals(List.of("null:" + Token.LITERAL_BOOLEAN, "true:" + Token.LITERAL_BOOLEAN), tokens("null true"));
    }

    @Test
    public void emptyLine() {
        assertEquals(List.of(), tokens(""));
    }

    // the non-whitespace tokens of a line as "lexeme:type"
    private static List<String> tokens(String line) {
        // the line is placed in the middle of a larger array, like in a document
        char[] array = ("##" + line + "##").toCharArray();
        Segment segment = new Segment(array, 2, line.length());

        List<String> tokens = new ArrayList<>();
        for (Token t = new QLTokenMaker().getTokenList(segment, Token.NULL, 100); t != null && t.isPaintable();
             t = t.getNextToken()) {
            if (t.getType() != Token.WHITESPACE) {
                tokens.add(t.getLexeme() + ":" + t.getType());
            }
        }
        return tokens;
    }
}
