package org.apache.cayenne.swing.components.textpane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;

import org.apache.cayenne.modeler.Main;
import org.apache.cayenne.swing.components.textpane.syntax.SQLSyntaxConstants;
import org.apache.cayenne.swing.components.textpane.syntax.SyntaxConstant;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JCayenneTextPane extends JPanel {

    protected Highlighter.HighlightPainter painter;
    private JTextPaneScrollable pane;
    public JScrollPane scrollPane;
    public boolean repaint;
    private static Log logObj = LogFactory.getLog(Main.class);

    public void setText(String text) {
        pane.setText(text);
    }

    public String getText() {
        return pane.getText();
    }

    public int getStartPositionInDocument() {
        return pane.viewToModel(scrollPane.getViewport().getViewPosition());
        // starting pos
        // in document
    }

    public int getEndPositionInDocument() {
        return pane.viewToModel(new Point(scrollPane.getViewport().getViewPosition().x
                + pane.getWidth(), scrollPane.getViewport().getViewPosition().y
                + pane.getHeight()));
    }

    /**
     * Return an int containing the wrapped line index at the given position
     * 
     * @param int pos
     * @return int
     */
    public int getLineNumber(int pos) {
        int posLine;
        int y = 0;

        try {
            Rectangle caretCoords = pane.modelToView(pos);
            y = (int) caretCoords.getY();
        }
        catch (BadLocationException ex) {
            logObj.warn("Error: ", ex);
        }

        int lineHeight = pane.getFontMetrics(pane.getFont()).getHeight();
        posLine = (y / lineHeight) + 1;
        return posLine;
    }

    /**
     * Return an int position at the given line number and symbol position in this line
     * 
     * @param int posInLine
     * @param int line
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

        setMinimumSize(new Dimension(30, 30));
        setPreferredSize(new Dimension(30, 30));
        setMinimumSize(new Dimension(30, 30));
        setBackground(new Color(245, 238, 238));
        setBorder(null);
        pane = new JTextPaneScrollable(new EditorKit(syntaxConstant)) {

            public void paint(Graphics g) {
                super.paint(g);
                JCayenneTextPane.this.repaint();
            }
        };

        pane.setFont(SQLSyntaxConstants.DEFAULT_FONT);

        scrollPane = new JScrollPane(pane);

        this.painter = new UnderlineHighlighterForText.UnderlineHighlightPainter(
                Color.red);

        pane.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent evt) {
                try {
                    removeHighlightText();
                    if (pane.getText(evt.getOffset(), 1).toString().equals("/")
                            || pane.getText(evt.getOffset(), 1).toString().equals("*")) {
                        pane.repaint();
                    }
                }
                catch (Exception e) {
                    logObj.warn("Error: ", e);
                }
            }

            public void removeUpdate(DocumentEvent evt) {
                removeHighlightText();
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

    public void setHighlightText(int line, int lastIndex, int size) {
        int k = 0;
        try {
            Matcher matcherTab = Pattern.compile("\t").matcher(
                    pane.getText(getPosition(line, 0), getPosition(line, lastIndex)));
            while (matcherTab.find()) {
                k += 7;
            }
        }
        catch (BadLocationException e1) {
            logObj.warn("Error: ", e1);
        }
        try {
            int position = getPosition(line, lastIndex-k);
            int positionEnd = position + size;
            Highlighter highlighter = pane.getHighlighter();
            removeHighlightText(highlighter);
            highlighter.addHighlight(position, positionEnd, painter);
        }
        catch (BadLocationException e) {
            logObj.warn("Error: ", e);
        }
    }

    public void removeHighlightText(Highlighter highlighter) {

        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        for (int i = 0; i < highlights.length; i++) {
            Highlighter.Highlight h = highlights[i];
            if (h.getPainter() instanceof UnderlineHighlighterForText.UnderlineHighlightPainter) {
                highlighter.removeHighlight(h);
            }
        }
    }

    public void removeHighlightText() {
        Highlighter highlighter = pane.getHighlighter();
        removeHighlightText(highlighter);
    }

    public void paint(Graphics g) {

        super.paint(g);

        int start = getStartPositionInDocument();
        int end = getEndPositionInDocument();
        // end pos in doc

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
            }
            else {
                starting_y = pane.modelToView(start).y
                        - scrollPane.getViewport().getViewPosition().y
                        + fontHeight
                        - fontDesc;
            }
        }
        catch (Exception e1) {
            logObj.warn("Error: ", e1);
        }

        for (int line = startline, y = starting_y; line <= endline; y += fontHeight, line++) {
            Color color = g.getColor();

            Font f2 = SQLSyntaxConstants.DEFAULT_FONT;
            FontMetrics fm2 = getFontMetrics(f2);
            g.setFont(f2);
            if (line - 1 == doc.getDefaultRootElement().getElementIndex(
                    pane.getCaretPosition())) {
                g.setColor(new Color(224, 224, 255));
                g.fillRect(0, y - fontHeight + 3, 30, fontHeight + 1);
            }

            g.setColor(Color.gray);
            g.drawString(Integer.toString(line), (30 - fm2.stringWidth(Integer
                    .toString(line))) / 2, y);
            g.setColor(color);
        }

    }

    public Document getDocument() {
        return pane.getDocument();
    }

    class JTextPaneScrollable extends JTextPane {

        private static final long serialVersionUID = 1L;

        public JTextPaneScrollable(EditorKit editorKit) {
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
