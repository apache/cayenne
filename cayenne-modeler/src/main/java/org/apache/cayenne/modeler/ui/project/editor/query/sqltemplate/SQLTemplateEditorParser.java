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

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import org.apache.cayenne.access.translator.sqltemplate.parser.ASTDirective;
import org.apache.cayenne.access.translator.sqltemplate.parser.ASTVariable;
import org.apache.cayenne.access.translator.sqltemplate.parser.JavaCharStream;
import org.apache.cayenne.access.translator.sqltemplate.parser.Node;
import org.apache.cayenne.access.translator.sqltemplate.parser.ParseException;
import org.apache.cayenne.access.translator.sqltemplate.parser.SQLTemplateParser;
import org.apache.cayenne.access.translator.sqltemplate.parser.SQLTemplateParserConstants;
import org.apache.cayenne.access.translator.sqltemplate.parser.SQLTemplateParserTokenManager;
import org.apache.cayenne.access.translator.sqltemplate.parser.Token;
import org.apache.cayenne.access.translator.sqltemplate.parser.TokenMgrError;
import org.apache.cayenne.modeler.toolkit.text.TextPositions;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;

/**
 * Checks the SQL of a SQLTemplate in the editor with the template parser of Cayenne: marks a syntax error of the
 * template and the unknown directives, and reports the found problems and the template parameters to a listener. The
 * SQL itself is not checked. Runs on the event dispatch thread, after a delay since the last edit.
 */
public class SQLTemplateEditorParser extends AbstractParser {

    /**
     * The directives of the Cayenne template engine.
     */
    static final Set<String> DIRECTIVES = Set.of("bind", "bindEqual", "bindNotEqual", "bindObjectEqual",
            "bindObjectNotEqual", "result");

    /**
     * The directives that only the optional Velocity template engine (the "cayenne-velocity" module) has.
     */
    static final Set<String> VELOCITY_DIRECTIVES = Set.of("chain", "chunk", "foreach", "set", "macro", "elseif",
            "include", "parse", "evaluate", "define", "break", "stop");

    // a variable that the template engine provides itself
    private static final String HELPER_VARIABLE = "helper";

    // the tokens that close a construct, by their priority in a message about a construct not closed
    private static final List<String> CLOSING_TOKENS = List.of("')'", "']'", "'#end'");

    private static final Pattern POSITION = Pattern.compile("line (\\d+), column (\\d+)");
    private static final Pattern DIRECTIVE = Pattern.compile("#(\\w+)");

    private final Consumer<Check> checkListener;

    /**
     * @param checkListener receives the result of each check of the text
     */
    public SQLTemplateEditorParser(Consumer<Check> checkListener) {
        this.checkListener = checkListener;
    }

    /**
     * The result of a check of a template.
     *
     * @param errors     the problems that make the template fail at runtime
     * @param warnings   the problems that may be fine, like a directive unknown to the Cayenne template engine
     * @param parameters the names of the template variables, without the "$"
     */
    public record Check(List<String> errors, List<String> warnings, Set<String> parameters) {
    }

    @Override
    public ParseResult parse(RSyntaxDocument doc, String style) {
        DefaultParseResult result = new DefaultParseResult(this);
        Element root = doc.getDefaultRootElement();
        result.setParsedLines(0, root.getElementCount() - 1);

        String text;
        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            // can't happen, the range is the whole document
            throw new IllegalStateException(e);
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> parameters = new LinkedHashSet<>();

        if (text.isBlank()) {
            warnings.add("The SQL is empty");
        } else {
            try {
                Node template = parser(text).template();

                Set<String> directives = new LinkedHashSet<>();
                collect(template, parameters, directives);
                for (String directive : directives) {
                    if (!DIRECTIVES.contains(directive)) {
                        String message = VELOCITY_DIRECTIVES.contains(directive)
                                ? "#" + directive + " is a Velocity directive, it requires the cayenne-velocity module"
                                : "Unknown directive #" + directive;
                        for (int[] range : occurrences(text, "#" + directive)) {
                            result.addNotice(notice(doc, message, range, ParserNotice.Level.WARNING));
                        }
                        warnings.add(message);
                    }
                }
            } catch (ParseException | TokenMgrError e) {
                String velocityDirective = velocityDirective(text);
                if (velocityDirective != null) {
                    // the template is valid for the Velocity engine, that has its own syntax
                    warnings.add("The template uses the Velocity directive #" + velocityDirective
                            + ", so it isn't checked");
                } else {
                    String message = message(e);
                    result.addNotice(notice(doc, message, errorRange(e, doc), ParserNotice.Level.ERROR));
                    errors.add(message);
                }
            }
        }

        checkListener.accept(new Check(errors, warnings, parameters));
        return result;
    }

    private DefaultParserNotice notice(RSyntaxDocument doc, String message, int[] range, ParserNotice.Level level) {
        DefaultParserNotice notice = new DefaultParserNotice(this, message, TextPositions.lineOf(doc, range[0]),
                range[0], range[1]);
        notice.setLevel(level);
        return notice;
    }

    static SQLTemplateParser parser(String text) {
        return new SQLTemplateParser(new SQLTemplateParserTokenManager(new UntabbedCharStream(new StringReader(text))));
    }

    private static void collect(Node node, Set<String> parameters, Set<String> directives) {
        if (node instanceof ASTVariable variable && isName(variable.getIdentifier())
                && !HELPER_VARIABLE.equals(variable.getIdentifier())) {
            parameters.add(variable.getIdentifier());
        } else if (node instanceof ASTDirective directive) {
            directives.add(directive.getIdentifier());
        }

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            collect(node.jjtGetChild(i), parameters, directives);
        }
    }

    // a variable "name" may be any text that follows a "$" that is not a part of a name
    private static boolean isName(String identifier) {
        return identifier != null && !identifier.isEmpty() && Character.isJavaIdentifierStart(identifier.charAt(0));
    }

    private static String velocityDirective(String text) {
        Matcher matcher = DIRECTIVE.matcher(text);
        while (matcher.find()) {
            if (VELOCITY_DIRECTIVES.contains(matcher.group(1))) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * Returns the message of a template syntax error, without the position, that is marked in the editor.
     */
    static String message(Throwable error) {
        if (error instanceof ParseException parseError && unexpectedToken(parseError) != null) {
            Token token = unexpectedToken(parseError);

            Set<String> expected = new LinkedHashSet<>();
            if (parseError.expectedTokenSequences != null && parseError.tokenImage != null) {
                for (int[] sequence : parseError.expectedTokenSequences) {
                    if (sequence.length > 0) {
                        expected.add(tokenName(parseError.tokenImage[sequence[0]]));
                    }
                }
            }

            // a long list of the expected tokens tells nothing, except for a bracket or a block that is not closed
            String expectedText;
            if (!expected.isEmpty() && expected.size() <= 3) {
                expectedText = String.join(" or ", expected);
            } else {
                expectedText = CLOSING_TOKENS.stream().filter(expected::contains).findFirst().orElse(null);
            }

            if (token.kind == SQLTemplateParserConstants.EOF) {
                String found = "Unexpected end of the template";
                return expectedText != null ? found + ", expected " + expectedText : found;
            }

            // a text that starts with a space is shown as an unexpected space, so only the expected token is told
            if (token.image.isEmpty() || Character.isWhitespace(token.image.charAt(0))) {
                return expectedText != null ? "Expected " + expectedText : "Unexpected text";
            }

            String found = "Unexpected '" + token.image.strip() + "'";
            return expectedText != null ? found + ", expected " + expectedText : found;
        }

        if (error instanceof TokenMgrError) {
            return String.valueOf(error.getMessage()).contains("<EOF>")
                    ? "Unexpected end of the template"
                    : "Unexpected character";
        }

        return TextPositions.firstLine(error.getMessage(), "Invalid template");
    }

    private static Token unexpectedToken(ParseException error) {
        return error.currentToken != null ? error.currentToken.next : null;
    }

    // the image of a token in the grammar, either a quoted literal, or a <NAME> of a token kind
    private static String tokenName(String image) {
        if (image.length() > 1 && image.startsWith("\"") && image.endsWith("\"")) {
            return "'" + image.substring(1, image.length() - 1) + "'";
        }
        return "<IDENTIFIER>".equals(image) ? "a name" : image.toLowerCase().replaceAll("[<>_]", " ").trim();
    }

    /**
     * Returns the offset and the length of the text that caused a template syntax error.
     */
    static int[] errorRange(Throwable error, RSyntaxDocument doc) {
        int offset = doc.getLength() - 1;
        int size = 1;

        if (error instanceof ParseException parseError && unexpectedToken(parseError) != null) {
            Token token = unexpectedToken(parseError);
            if (token.kind != SQLTemplateParserConstants.EOF) {
                offset = TextPositions.offsetOf(doc, token.beginLine, token.beginColumn);
                if (token.endLine == token.beginLine && token.endColumn >= token.beginColumn) {
                    size = token.endColumn - token.beginColumn + 1;
                }
            }
        } else {
            Matcher matcher = POSITION.matcher(String.valueOf(error.getMessage()));
            if (matcher.find()) {
                offset = TextPositions.offsetOf(doc, Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)));
            }
        }

        // an error at the end of the text is shown on its last char
        return TextPositions.clamp(doc, offset, size);
    }

    /**
     * Returns the offsets and the lengths of the occurrences of a directive in the text, as a whole word.
     */
    static List<int[]> occurrences(String text, String directive) {
        List<int[]> ranges = new ArrayList<>();
        Matcher matcher = Pattern.compile(Pattern.quote(directive) + "(?!\\w)").matcher(text);
        while (matcher.find()) {
            ranges.add(new int[]{matcher.start(), directive.length()});
        }
        return ranges;
    }

    /**
     * A stream of the template chars that counts a tab as a single column, as the editor does, so that the positions
     * of the tokens can be converted to the document offsets.
     */
    private static final class UntabbedCharStream extends JavaCharStream {

        UntabbedCharStream(Reader reader) {
            super(reader);
            tabSize = 1;
        }
    }
}
