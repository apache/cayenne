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

import java.util.List;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

/**
 * Splits the lines of a SQLTemplate into tokens for highlighting: the SQL, plus the template directives
 * ("#bind(...)", "#if", "#end", etc.), the variables ("$name") and the template comments ("## ...").
 */
public class SQLTemplateTokenMaker extends AbstractTokenMaker {

    /**
     * The syntax style key of the language, to register this token maker in RSyntaxTextArea.
     */
    public static final String SYNTAX_STYLE = "text/cayenne-sqltemplate";

    static final List<String> KEYWORDS = List.of("select", "distinct", "from", "where", "group", "by", "having",
            "order", "asc", "desc", "limit", "offset", "fetch", "first", "next", "rows", "row", "only", "top", "as",
            "on", "join", "inner", "left", "right", "full", "outer", "cross", "natural", "using", "union",
            "intersect", "except", "minus", "insert", "into", "values", "update", "set", "delete", "merge",
            "returning", "create", "alter", "drop", "truncate", "table", "view", "index", "sequence", "primary",
            "key", "foreign", "references", "constraint", "unique", "check", "default", "case", "when", "then",
            "else", "end", "with", "recursive", "over", "partition", "window", "for", "begin", "commit", "rollback",
            "grant", "revoke", "call", "exec", "execute", "declare", "procedure", "function", "trigger", "if",
            "return");

    static final List<String> OPERATORS = List.of("and", "or", "not", "in", "is", "like", "ilike", "between",
            "exists", "all", "any", "some", "escape");

    static final List<String> FUNCTIONS = List.of("count", "sum", "avg", "min", "max", "coalesce", "nullif", "cast",
            "convert", "extract", "substring", "substr", "trim", "ltrim", "rtrim", "upper", "lower", "length",
            "char_length", "concat", "replace", "position", "locate", "abs", "round", "floor", "ceil", "ceiling",
            "mod", "power", "sqrt", "now", "current_date", "current_time", "current_timestamp", "nvl", "decode",
            "row_number", "rank", "dense_rank");

    static final List<String> TYPES = List.of("int", "integer", "smallint", "bigint", "tinyint", "decimal",
            "numeric", "number", "float", "real", "double", "precision", "char", "character", "varchar", "varchar2",
            "nchar", "nvarchar", "text", "clob", "blob", "binary", "varbinary", "boolean", "bit", "date", "time",
            "timestamp", "datetime", "interval");

    static final List<String> LITERALS = List.of("null", "true", "false");

    @Override
    public TokenMap getWordsToHighlight() {
        // SQL is case-insensitive
        TokenMap map = new TokenMap(true);
        KEYWORDS.forEach(w -> map.put(w, Token.RESERVED_WORD));
        OPERATORS.forEach(w -> map.put(w, Token.OPERATOR));
        FUNCTIONS.forEach(w -> map.put(w, Token.FUNCTION));
        TYPES.forEach(w -> map.put(w, Token.DATA_TYPE));
        LITERALS.forEach(w -> map.put(w, Token.LITERAL_BOOLEAN));
        return map;
    }

    @Override
    public String[] getLineCommentStartAndEnd(int languageIndex) {
        return new String[]{"-- ", null};
    }

    @Override
    public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
        // a word is a keyword only if it is a whole token, a column like "t0.name" is not
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

        // the rest of a comment that started on one of the previous lines
        if (initialTokenType == Token.COMMENT_MULTILINE) {
            int close = commentEnd(array, i, end);
            if (close < 0) {
                addToken(text, i, end - 1, Token.COMMENT_MULTILINE, shift + i);
                return firstToken;
            }
            addToken(text, i, close - 1, Token.COMMENT_MULTILINE, shift + i);
            i = close;
        }

        while (i < end) {
            char c = array[i];
            int start = i;

            if (Character.isWhitespace(c)) {
                while (i < end && Character.isWhitespace(array[i])) {
                    i++;
                }
                addToken(text, start, i - 1, Token.WHITESPACE, shift + start);
            } else if (startsWith(array, i, end, "--") || startsWith(array, i, end, "##")) {
                addToken(text, start, end - 1, Token.COMMENT_EOL, shift + start);
                i = end;
            } else if (startsWith(array, i, end, "/*")) {
                int close = commentEnd(array, i + 2, end);
                if (close < 0) {
                    // the comment continues on the next line, that starts with this token type
                    addToken(text, start, end - 1, Token.COMMENT_MULTILINE, shift + start);
                    return firstToken;
                }
                addToken(text, start, close - 1, Token.COMMENT_MULTILINE, shift + start);
                i = close;
            } else if (c == '\'') {
                i = string(text, i, end, shift);
            } else if (c == '"' || c == '`') {
                // a quoted identifier
                i++;
                while (i < end && array[i] != c) {
                    i++;
                }
                i = Math.min(i + 1, end);
                addToken(text, start, i - 1, Token.IDENTIFIER, shift + start);
            } else if (c == '#' && i + 1 < end && isWordStart(array[i + 1])) {
                i++;
                while (i < end && Character.isLetterOrDigit(array[i])) {
                    i++;
                }
                addToken(text, start, i - 1, Token.PREPROCESSOR, shift + start);
            } else if (c == '$' && i + 1 < end && isWordStart(array[i + 1])) {
                i++;
                while (i < end && isVariableChar(array[i])) {
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
            } else if (isWordStart(c)) {
                while (i < end && isWordChar(array[i])) {
                    i++;
                }
                addToken(text, start, i - 1, Token.IDENTIFIER, shift + start);
            } else {
                i++;
                int type = c == '(' || c == ')' || c == ',' || c == ';' ? Token.SEPARATOR : Token.OPERATOR;
                addToken(text, start, start, type, shift + start);
            }
        }

        addNullToken();
        return firstToken;
    }

    /**
     * Adds the tokens of a string literal that starts at the index, and returns the index past it. The variables in
     * the string are tokens of their own, as the template substitutes them.
     */
    private int string(Segment text, int start, int end, int shift) {
        char[] array = text.array;
        int partStart = start;
        int i = start + 1;
        while (i < end && array[i] != '\'') {
            if (array[i] == '$' && i + 1 < end && isWordStart(array[i + 1])) {
                if (i > partStart) {
                    addToken(text, partStart, i - 1, Token.LITERAL_STRING_DOUBLE_QUOTE, shift + partStart);
                }
                int variableStart = i++;
                while (i < end && isVariableChar(array[i])) {
                    i++;
                }
                addToken(text, variableStart, i - 1, Token.VARIABLE, shift + variableStart);
                partStart = i;
            } else {
                i++;
            }
        }

        boolean closed = i < end;
        i = Math.min(i + 1, end);
        if (i > partStart) {
            addToken(text, partStart, i - 1, closed ? Token.LITERAL_STRING_DOUBLE_QUOTE : Token.ERROR_STRING_DOUBLE,
                    shift + partStart);
        }
        return i;
    }

    // the index past the "*/" that closes a comment, or -1 if the comment doesn't end on this line
    private static int commentEnd(char[] array, int from, int end) {
        for (int i = from; i + 1 < end; i++) {
            if (array[i] == '*' && array[i + 1] == '/') {
                return i + 2;
            }
        }
        return -1;
    }

    private static boolean startsWith(char[] array, int i, int end, String prefix) {
        if (i + prefix.length() > end) {
            return false;
        }
        for (int j = 0; j < prefix.length(); j++) {
            if (array[i + j] != prefix.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWordStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    // a word includes the dots of a qualified name, like "t0.name"
    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }

    private static boolean isVariableChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
