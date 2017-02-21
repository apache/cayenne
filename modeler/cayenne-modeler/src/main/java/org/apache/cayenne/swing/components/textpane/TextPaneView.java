/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.swing.components.textpane;

import org.apache.cayenne.swing.components.textpane.style.SyntaxStyle;
import org.apache.cayenne.swing.components.textpane.style.TextPaneStyleMap;
import org.apache.cayenne.swing.components.textpane.style.TextPaneStyleTypes;
import org.apache.cayenne.swing.components.textpane.syntax.SQLSyntaxConstants;
import org.apache.cayenne.swing.components.textpane.syntax.SyntaxConstant;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.StyledDocument;
import javax.swing.text.Utilities;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextPaneView extends PlainView {

    private static HashMap<Pattern, SyntaxStyle> patternSyntaxStyle;
    private static Pattern patternComment;
    private static Pattern patternCommentStart;

    private static SyntaxStyle syntaxStyleComment;
    private static HashMap<Pattern, SyntaxStyle> patternValue;
    private static TextPaneStyleMap style = new TextPaneStyleMap();

    static {
        patternSyntaxStyle = new HashMap<>();
        patternValue = new HashMap<>();
    }

    public TextPaneView(Element elem, SyntaxConstant syntaxConstants) {
        super(elem);
        getDocument().putProperty(PlainDocument.tabSizeAttribute, 4);

        if (patternSyntaxStyle.isEmpty()) {
            addConstants(syntaxConstants.getKEYWORDS(), TextPaneStyleTypes.KEYWORDS);
            addConstants(syntaxConstants.getKEYWORDS2(), TextPaneStyleTypes.KEYWORDS2);
            addConstants(syntaxConstants.getOPERATORS(), TextPaneStyleTypes.KEYWORDS);
            addConstants(syntaxConstants.getTYPES(), TextPaneStyleTypes.TYPE);
        }

        if (patternValue.isEmpty()) {
            patternValue.put(
                    Pattern.compile(SQLSyntaxConstants.NUMBER_TEXT), style.syntaxStyleMap.get(TextPaneStyleTypes.NUMBER));
        }

        patternComment = Pattern.compile(SQLSyntaxConstants.COMMENT_TEXT, Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
        patternCommentStart = Pattern.compile(SQLSyntaxConstants.COMMENT_TEXT_START, Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
        syntaxStyleComment = style.syntaxStyleMap.get(TextPaneStyleTypes.COMMENT);
    }

    private void addConstants(String[] constants, TextPaneStyleTypes type) {
        for (String keyword : constants) {
            String patern = "(" + keyword + ")";
            patternSyntaxStyle.put(
                    Pattern.compile(patern, Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE),
                    style.syntaxStyleMap.get(type));
        }
    }

    @Override
    protected int drawUnselectedText(Graphics graphics, int x, int y, int p0, int p1) throws BadLocationException {

        boolean lineComment = false;
        Map<Integer, Integer> comment = new HashMap<>();
        Map<Integer, Integer> commentInLine = new HashMap<>();
        StyledDocument doc = (StyledDocument) getDocument();

        String text = doc.getText(p0, p1 - p0);
        Segment segment = getLineBuffer();

        Matcher m = patternComment.matcher(doc.getText(0, doc.getLength()));
        int maxEnd = 0;
        while (m.find()) {
            comment.put(m.start(), m.end());
            if (maxEnd < m.end()) {
                maxEnd = m.end();
            }
        }
        Matcher m3 = patternCommentStart.matcher(doc.getText(0, doc.getLength()));
        while (m3.find()) {
            if (maxEnd < m3.start()) {
                comment.put(m3.start(), doc.getLength());
                break;
            }
        }

        for (Map.Entry<Integer, Integer> entry : comment.entrySet()) {
            if (p0 >= entry.getKey() && p1 <= entry.getValue()) {
                lineComment = true;
                break;
            } else if (p0 <= entry.getKey() && p1 >= entry.getValue()) {
                commentInLine.put(entry.getKey() - p0, entry.getValue() - p0);
            } else if (p0 <= entry.getKey()
                    && p1 >= entry.getKey()
                    && p1 < entry.getValue()) {
                commentInLine.put(entry.getKey() - p0, p1 - p0);
            } else if (p0 <= entry.getValue()
                    && p1 >= entry.getValue()
                    && p0 > entry.getKey()) {
                commentInLine.put(0, entry.getValue() - p0);
            }
        }

        SortedMap<Integer, Integer> startMap = new TreeMap<>();
        SortedMap<Integer, SyntaxStyle> syntaxStyleMap = new TreeMap<>();

        if (lineComment) {
            startMap.put(0, text.length());
            syntaxStyleMap.put(0, syntaxStyleComment);
        } else {
            for (Map.Entry<Integer, Integer> entryCommentInLine : commentInLine
                    .entrySet()) {
                startMap.put(entryCommentInLine.getKey(), entryCommentInLine.getValue());
                syntaxStyleMap.put(entryCommentInLine.getKey(), syntaxStyleComment);
            }
            // Match all regexes on this snippet, store positions
            for (Map.Entry<Pattern, SyntaxStyle> entry : patternSyntaxStyle.entrySet()) {

                Matcher matcher = entry.getKey().matcher(text);
                while (matcher.find()) {
                     if ((text.length() == matcher.end()
                            || text.charAt(matcher.end()) == '\t'
                            || text.charAt(matcher.end()) == ' ' || text.charAt(matcher
                            .end()) == '\n')
                            && (matcher.start() == 0
                                    || text.charAt(matcher.start() - 1) == '\t'
                                    || text.charAt(matcher.start() - 1) == '\n'
                                    || text.charAt(matcher.start() - 1) == ' ')) {
                        boolean inComment = false;
                        for (Map.Entry<Integer, Integer> entryCommentInLine : commentInLine
                                .entrySet()) {
                            if (matcher.start(1) >= entryCommentInLine.getKey()
                                    && matcher.end() <= entryCommentInLine.getValue()) {
                                inComment = true;
                            }
                        }
                        if (!inComment) {

                            startMap.put(matcher.start(1), matcher.end());
                            syntaxStyleMap.put(matcher.start(1), entry.getValue());
                        }
                    }
                }
            }

            for (Map.Entry<Pattern, SyntaxStyle> entry : patternValue.entrySet()) {
                Matcher matcher = entry.getKey().matcher(text);
                while (matcher.find()) {
                    if ((text.length() == matcher.end()
                            || text.charAt(matcher.end()) == ' '
                            || text.charAt(matcher.end()) == ')'
                            || text.charAt(matcher.end()) == '\t'
                            || text.charAt(matcher.end()) == '\n')
                            && (matcher.start() == 0
                                    || text.charAt(matcher.start() - 1) == '\t'
                                    || text.charAt(matcher.start() - 1) == ' '
                                    || text.charAt(matcher.start() - 1) == '='
                                    || text.charAt(matcher.start() - 1) == '(')) {
                        boolean inComment = false;
                        for (Map.Entry<Integer, Integer> entryCommentInLine : commentInLine.entrySet()) {
                            if (matcher.start() >= entryCommentInLine.getKey()
                                    && matcher.end() <= entryCommentInLine.getValue()) {
                                inComment = true;
                            }
                        }
                        if (!inComment) {
                            startMap.put(matcher.start(), matcher.end());
                            syntaxStyleMap.put(matcher.start(), entry.getValue());
                        }
                    }
                }
            }
        }
        // TODO: check the map for overlapping parts

        int i = 0;
        // Colour the parts
        for (Map.Entry<Integer, Integer> entry : startMap.entrySet()) {
            int start = entry.getKey();
            int end = entry.getValue();

            if (i < start) {
                graphics.setColor(SQLSyntaxConstants.DEFAULT_COLOR);
                graphics.setFont(SQLSyntaxConstants.DEFAULT_FONT);
                doc.getText(p0 + i, start - i, segment);
                x = Utilities.drawTabbedText(segment, x, y, graphics, this, i);
            }

            graphics.setFont(syntaxStyleMap.get(start).getFont());
            graphics.setColor(syntaxStyleMap.get(start).getColor());
            i = end;
            doc.getText(p0 + start, i - start, segment);

            x = Utilities.drawTabbedText(segment, x, y, graphics, this, start);
        }

        // Paint possible remaining text black
        if (i < text.length()) {
            graphics.setColor(SQLSyntaxConstants.DEFAULT_COLOR);
            graphics.setFont(SQLSyntaxConstants.DEFAULT_FONT);
            doc.getText(p0 + i, text.length() - i, segment);
            x = Utilities.drawTabbedText(segment, x, y, graphics, this, i);
        }

        return x;
    }
}
