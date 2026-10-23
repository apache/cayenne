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

import java.util.List;
import java.util.Set;

import javax.swing.text.Segment;

import org.apache.cayenne.query.ObjectSelect;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

/**
 * Splits the lines of a Cayenne QL query (see {@link ObjectSelect#parse(String, Object...)}) into
 * tokens for highlighting. The clause keywords of the language are contextual, i.e. they are valid property names
 * outside the positions of the clauses. This is ignored here, and they are highlighted everywhere.
 */
public class QLTokenMaker extends AbstractTokenMaker {

    /**
     * The syntax style key of the language, to register this token maker in RSyntaxTextArea.
     */
    public static final String SYNTAX_STYLE = "text/cayenne-ql";

    static final List<String> KEYWORDS = List.of("select", "distinct", "from", "where", "having", "order", "by", "asc",
            "desc", "insensitive", "limit", "offset", "prefetch", "joint", "disjoint", "disjointById", "as", "self",
            "enclosing", "all", "any");

    static final List<String> OPERATORS = List.of("and", "or", "not", "like", "likeIgnoreCase", "in", "between",
            "exists");

    static final List<String> FUNCTIONS = List.of("avg", "min", "max", "sum", "count", "concat", "substring", "trim",
            "lower", "upper", "length", "locate", "abs", "sqrt", "mod", "currentDate", "currentTime",
            "currentTimestamp", "now", "year", "month", "week", "dayOfYear", "day", "dayOfMonth", "dayOfWeek", "hour",
            "minute", "second", "fn", "op", "agg");

    static final List<String> LITERALS = List.of("null", "NULL", "true", "TRUE", "false", "FALSE");

    private static final Set<String> PATH_PREFIXES = Set.of("db", "obj", "enum", "dbid");

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap map = new TokenMap();
        KEYWORDS.forEach(w -> map.put(w, Token.RESERVED_WORD));
        OPERATORS.forEach(w -> map.put(w, Token.OPERATOR));
        FUNCTIONS.forEach(w -> map.put(w, Token.FUNCTION));
        LITERALS.forEach(w -> map.put(w, Token.LITERAL_BOOLEAN));
        return map;
    }

    @Override
    public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
        // a word is a keyword only if it is a whole token, a path like "order.date" is not
        if (tokenType == Token.IDENTIFIER) {
            int type = wordsToHighlight.get(segment, start, end);
            if (type != -1) {
                tokenType = type;
            }
        }
        super.addToken(segment, start, end, tokenType, startOffset);
    }

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        resetTokenList();

        char[] array = text.array;
        int end = text.offset + text.count;

        // the offset of a char in the document is "shift" + its index in the array
        int shift = startOffset - text.offset;

        int i = text.offset;
        while (i < end) {
            char c = array[i];
            int start = i;

            if (Character.isWhitespace(c)) {
                while (i < end && Character.isWhitespace(array[i])) {
                    i++;
                }
                addToken(text, start, i - 1, Token.WHITESPACE, shift + start);
            } else if (c == '\'' || c == '"') {
                i++;
                while (i < end && array[i] != c) {
                    // an escaped char, the quote included
                    i += array[i] == '\\' ? 2 : 1;
                }
                boolean closed = i < end;
                i = Math.min(i + 1, end);
                addToken(text, start, i - 1, closed ? Token.LITERAL_STRING_DOUBLE_QUOTE : Token.ERROR_STRING_DOUBLE,
                        shift + start);
            } else if (c == '$') {
                i++;
                while (i < end && isPathChar(array[i])) {
                    i++;
                }
                addToken(text, start, i - 1, Token.VARIABLE, shift + start);
            } else if (Character.isDigit(c)) {
                boolean decimal = false;
                while (i < end && (Character.isLetterOrDigit(array[i]) || array[i] == '.')) {
                    decimal |= array[i] == '.';
                    i++;
                }
                addToken(text, start, i - 1, decimal ? Token.LITERAL_NUMBER_FLOAT : Token.LITERAL_NUMBER_DECIMAL_INT,
                        shift + start);
            } else if (isPathStart(c)) {
                while (i < end && isPathChar(array[i])) {
                    i++;
                }

                // "db:", "obj:", "enum:" or "dbid:" prefix of a path
                if (i < end && array[i] == ':' && PATH_PREFIXES.contains(new String(array, start, i - start))) {
                    i++;
                    addToken(text, start, i - 1, Token.DATA_TYPE, shift + start);
                } else {
                    addToken(text, start, i - 1, Token.IDENTIFIER, shift + start);
                }
            } else {
                i++;
                int type = c == '(' || c == ')' || c == ',' ? Token.SEPARATOR : Token.OPERATOR;
                addToken(text, start, start, type, shift + start);
            }
        }

        addNullToken();
        return firstToken;
    }

    private static boolean isPathStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    // the chars of a path, including the join "+" and alias "#" markers
    private static boolean isPathChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '+' || c == '#';
    }
}
