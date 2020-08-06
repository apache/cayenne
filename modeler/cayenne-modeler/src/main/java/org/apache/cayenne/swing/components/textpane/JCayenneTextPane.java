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
package org.apache.cayenne.swing.components.textpane;

import org.apache.cayenne.modeler.Main;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.swing.components.textpane.syntax.SQLSyntaxConstants;
import org.apache.cayenne.swing.components.textpane.syntax.SyntaxConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public class JCayenneTextPane extends JPanel {

    protected Highlighter.HighlightPainter painter;
    private JTextPaneScrollable pane;
    private JScrollPane scrollPane;

    private boolean imageError;
    private String tooltipTextError;
    private int startYPositionToolTip;
    private int endYPositionToolTip;

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public String getTooltipTextError() {
        return tooltipTextError;
    }

    public void setTooltipTextError(String tooltipTextError) {
        this.tooltipTextError = tooltipTextError;
    }

    private static Logger logObj = LoggerFactory.getLogger(Main.class);

    public void setText(String text) {
        pane.setText(text);
    }

    public String getText() {
        return pane.getText();
    }

    public JTextComponent getPane() {
        return pane;
    }

    public int getStartPositionInDocument() {
        return pane.viewToModel(scrollPane.getViewport().getViewPosition());
    }

    public int getEndPositionInDocument() {
        return pane.viewToModel(new Point(scrollPane.getViewport().getViewPosition().x + pane.getWidth(),
                scrollPane.getViewport().getViewPosition().y + pane.getHeight()));
    }

    public void repaintPane() {
        pane.repaint();
    }

    /**
     * Return an int containing the wrapped line index at the given position
     * 
     * @param pos int
     * @return int
     */
    public int getLineNumber(int pos) {
        int posLine;
        int y = 0;

        try {
            Rectangle caretCoords = pane.modelToView(pos);
            y = (int) caretCoords.getY();
        } catch (BadLocationException ex) {
            logObj.warn("Error: ", ex);
        }

        int lineHeight = pane.getFontMetrics(pane.getFont()).getHeight();
        posLine = (y / lineHeight) + 1;
        return posLine;
    }

    /**
     * Return an int position at the given line number and symbol position in this line
     * 
     * @param posInLine int
     * @param line int
     * @return int
     * @throws BadLocationException
     */
    public int getPosition(int line, int posInLine) throws BadLocationException {
        // translate lines to offsets
        int position = -1;
        int numrows = 1;
        char[] chararr = pane.getText().toCharArray();
        for (int i = 0; i < chararr.length; i++) {
            if (chararr[i] == '\n') {
                numrows++;
                if (numrows == line) {
                    position = i;
                    break;
                }
            }
        }
        return position + posInLine;
    }

    public JCayenneTextPane(SyntaxConstant syntaxConstant) {
        super();

        Dimension dimension = new Dimension(15, 15);
        setMinimumSize(dimension);
        setPreferredSize(dimension);
        setMinimumSize(dimension);
        setBorder(null);

        pane = new JTextPaneScrollable(new EditorKit(syntaxConstant)) {
            public void paint(Graphics g) {
                super.paint(g);
                JCayenneTextPane.this.repaint();
            }
        };

        pane.setFont(SQLSyntaxConstants.DEFAULT_FONT);
        pane.setBorder(new LineNumberedBorder(this));

        scrollPane = new JScrollPane(pane);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        painter = new UnderlineHighlighterForText.UnderlineHighlightPainter(Color.red);

        pane.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent evt) {
                try {
                    String text = pane.getText(evt.getOffset(), 1);
                    if (text.equals("/") || text.equals("*")) {
                        removeHighlightText();
                        pane.repaint();
                    } else if (text.equals(" ") || text.equals("\t") || text.equals("\n")) {
                        pane.repaint();
                    }
                } catch (Exception e) {
                    logObj.warn("Error: ", e);
                }
            }

            public void removeUpdate(DocumentEvent evt) {
            }

            public void changedUpdate(DocumentEvent evt) {
            }
        });

    }

    public void setHighlightText(int lastIndex, int endIndex) throws BadLocationException {
        Highlighter highlighter = pane.getHighlighter();
        removeHighlightText(highlighter);
        highlighter.addHighlight(lastIndex, endIndex, painter);
    }

    /**
     * set underlines text in JCayenneTextPane
     * 
     * @param line int - starting line for underlined text
     * @param lastIndex int - starting position in line for underlined text
     * @param size int
     * @param message String - text for toolTip, contains the text of the error
     */

    public void setHighlightText(int line, int lastIndex, int size, String message) {
        Highlighter highlighter = pane.getHighlighter();
        removeHighlightText(highlighter);
        if (getText().length() > 0) {
            try {
                int position = getPosition(line, lastIndex);
                int positionEnd = position + size;
                highlighter.addHighlight(position, positionEnd, painter);
                setToolTipPosition(line, message);
                repaintPane();
            } catch (BadLocationException e) {
                logObj.warn("Error: ", e);
            }
        } else {
            setToolTipPosition(0, "");
        }
    }

    public void removeHighlightText(Highlighter highlighter) {
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        for (Highlighter.Highlight h : highlights) {
            if (h.getPainter() instanceof UnderlineHighlighterForText.UnderlineHighlightPainter) {
                highlighter.removeHighlight(h);
            }
        }
    }

    public void setToolTipPosition(int line, String string) {
        if (line != 0) {
            int height = pane.getFontMetrics(pane.getFont()).getHeight();
            int start = (line - 1) * height;
            this.endYPositionToolTip = start;
            this.startYPositionToolTip = start + height;
            setTooltipTextError(string);
            imageError = !"".equals(string);
            setToolTipText("");
        } else {
            this.endYPositionToolTip = -1;
            this.startYPositionToolTip = -1;
            setTooltipTextError("");
            setToolTipText("");
            imageError = false;
        }
    }

    public String getToolTipText(MouseEvent e) {

        if (e.getPoint().y > endYPositionToolTip
                && e.getPoint().y < startYPositionToolTip
                && imageError) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            String htmlText = getTooltipTextError()
                    .replaceAll("\n", "<br>&nbsp;")
                    .replaceAll("\t", "&nbsp;")
                    .replaceAll("\r", "<br>&nbsp;");

            return "<HTML>"
                    + "<body bgcolor='#FFEBCD' text='black'>"
                    + htmlText
                    + "</body>";
        } else {
            setCursor(Cursor.getDefaultCursor());
            return null;
        }
    }

    public void removeHighlightText() {
        imageError = false;
        Highlighter highlighter = pane.getHighlighter();
        removeHighlightText(highlighter);
    }
    
    public int getCaretPosition() {
        return pane.getCaretPosition();        
    }

    public void paint(Graphics g) {

        super.paint(g);

        int start = getStartPositionInDocument();
        int end = getEndPositionInDocument(); // end pos in doc

        // translate offsets to lines
        Document doc = pane.getDocument();
        int startline = doc.getDefaultRootElement().getElementIndex(start) + 1;
        int endline = doc.getDefaultRootElement().getElementIndex(end) + 1;

        int fontHeight = g.getFontMetrics(pane.getFont()).getHeight();
        int fontDesc = g.getFontMetrics(pane.getFont()).getDescent();
        int starting_y = -1;

        try {
            if (pane.modelToView(start) == null) {
                starting_y = -1;
            } else {
                starting_y = pane.modelToView(start).y
                        - scrollPane.getViewport().getViewPosition().y
                        + fontHeight
                        - fontDesc;
            }
        } catch (Exception e1) {
            logObj.warn("Error: ", e1);
        }

        for (int line = startline, y = starting_y; line <= endline; y += fontHeight, line++) {
            Color color = g.getColor();

            if (line - 1 == doc.getDefaultRootElement().getElementIndex(pane.getCaretPosition())) {
                g.setColor(new Color(224, 224, 255));
                g.fillRect(0, y - fontHeight + 3, 30, fontHeight + 1);
            }

            if (imageError) {
                Image img = ModelerUtil.buildIcon("icon-error.png").getImage();
                g.drawImage(img, 0, endYPositionToolTip, this);
            }

            g.setColor(color);
        }
    }

    public Document getDocument() {
        return pane.getDocument();
    }

    public void setDocumentTextDirect(String text) {
        Document document = getDocument();
        try {
            if(!document.getText(0, document.getLength()).equals(text)) {
                document.remove(0, document.getLength());
                document.insertString(0, text, null);
            }
        } catch (BadLocationException ex) {
            logObj.warn("Error reading document", ex);
        }
    }

    public String getDocumentTextDirect() {
        Document document = getDocument();
        try {
            return document.getText(0, document.getLength());
        } catch (BadLocationException ex) {
            logObj.warn("Error reading document", ex);
            return null;
        }
    }

    class JTextPaneScrollable extends JTextPane {

        JTextPaneScrollable(EditorKit editorKit) {
            // Set editor kit
            this.setEditorKitForContentType(editorKit.getContentType(), editorKit);
            this.setContentType(editorKit.getContentType());
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            if (getParent() instanceof JViewport) {
                JViewport port = (JViewport) getParent();
                if (port.getWidth() > getUI().getPreferredSize(this).width) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
        }
    }

}
