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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Element;

import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.projecttools.validation.SelectQueryPaths;
import org.apache.cayenne.ql.ParseException;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;

/**
 * Checks the query String in the editor: marks a syntax error, or the entity names and paths that can't be resolved
 * against the mapping, and reports the found problems to a listener. Runs on the event dispatch thread, after a delay
 * since the last edit.
 */
class QLEditorParser extends AbstractParser {

    private static final Pattern POSITION = Pattern.compile("line (\\d+), column (\\d+)");

    private final Supplier<SelectQueryDescriptor> query;
    private final Consumer<List<String>> problemsListener;

    /**
     * @param query            the query edited, that holds the text of the editor
     * @param problemsListener receives the messages of the found problems, empty if there are none
     */
    QLEditorParser(Supplier<SelectQueryDescriptor> query, Consumer<List<String>> problemsListener) {
        this.query = query;
        this.problemsListener = problemsListener;
    }

    @Override
    public ParseResult parse(RSyntaxDocument doc, String style) {
        DefaultParseResult result = new DefaultParseResult(this);
        Element root = doc.getDefaultRootElement();
        result.setParsedLines(0, root.getElementCount() - 1);

        SelectQueryDescriptor descriptor = query.get();
        if (descriptor == null) {
            return result;
        }

        List<String> problems = new ArrayList<>();
        if (descriptor.getQl() == null || descriptor.getQl().isBlank()) {
            problems.add("The query is empty");
        } else if (!descriptor.isValid()) {
            try {
                descriptor.getParsedQuery();
            } catch (ExpressionException e) {
                String message = firstLine(e.getUnlabeledMessage());
                int[] range = errorRange(e, doc);
                result.addNotice(new DefaultParserNotice(this, message, lineOf(root, range[0]), range[0], range[1]));
                problems.add(message);
            }
        } else {
            for (SelectQueryPaths.Problem problem : SelectQueryPaths.check(descriptor)) {
                for (int[] range : occurrences(doc, problem.name())) {
                    result.addNotice(new DefaultParserNotice(this, problem.message(), lineOf(root, range[0]),
                            range[0], range[1]));
                }
                problems.add(problem.message());
            }
        }

        problemsListener.accept(problems);
        return result;
    }

    /**
     * Returns the offset and the length of the text that caused a parse error.
     */
    static int[] errorRange(ExpressionException error, RSyntaxDocument doc) {
        int length = doc.getLength();
        if (length == 0) {
            return new int[]{0, 0};
        }

        int offset;
        int size = 1;
        if (error.getCause() instanceof ParseException parseError
                && parseError.currentToken != null
                && parseError.currentToken.next != null) {
            // the token that the parser didn't expect
            int line = parseError.currentToken.next.beginLine;
            int beginColumn = parseError.currentToken.next.beginColumn;
            int endColumn = parseError.currentToken.next.endColumn;
            offset = offsetOf(doc, line, beginColumn);
            if (parseError.currentToken.next.endLine == line && endColumn >= beginColumn) {
                size = endColumn - beginColumn + 1;
            }
        } else {
            Matcher matcher = POSITION.matcher(String.valueOf(error.getMessage()));
            offset = matcher.find()
                    ? offsetOf(doc, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)))
                    : length - 1;
        }

        // an error at the end of the text is shown on its last char
        offset = Math.max(0, Math.min(offset, length - 1));
        size = Math.max(1, Math.min(size, length - offset));
        return new int[]{offset, size};
    }

    // the line and the column are 1-based
    private static int offsetOf(RSyntaxDocument doc, int line, int column) {
        Element root = doc.getDefaultRootElement();
        int lineIndex = Math.max(0, Math.min(line - 1, root.getElementCount() - 1));
        return root.getElement(lineIndex).getStartOffset() + Math.max(0, column - 1);
    }

    private static int lineOf(Element root, int offset) {
        return root.getElementIndex(offset);
    }

    /**
     * Returns the offsets and the lengths of the occurrences of a name in the text, either a whole identifier token, or
     * a prefixed one like "db:NAME".
     */
    static List<int[]> occurrences(RSyntaxDocument doc, String name) {
        List<int[]> ranges = new ArrayList<>();
        Element root = doc.getDefaultRootElement();
        for (int line = 0; line < root.getElementCount(); line++) {
            Token previous = null;
            for (Token t = doc.getTokenListForLine(line); t != null && t.isPaintable(); t = t.getNextToken()) {
                if (t.getType() == Token.IDENTIFIER && name.equals(t.getLexeme())) {
                    ranges.add(new int[]{t.getOffset(), t.length()});
                } else if (t.getType() == Token.IDENTIFIER && previous != null
                        && previous.getType() == Token.DATA_TYPE
                        && name.equals(previous.getLexeme() + t.getLexeme())) {
                    ranges.add(new int[]{previous.getOffset(), previous.length() + t.length()});
                }
                previous = t;
            }
        }
        return ranges;
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "Invalid query";
        }
        int eol = message.indexOf('\n');
        return (eol >= 0 ? message.substring(0, eol) : message).trim();
    }
}
