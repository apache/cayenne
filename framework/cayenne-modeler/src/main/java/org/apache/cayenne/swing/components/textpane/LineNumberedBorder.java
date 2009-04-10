package org.apache.cayenne.swing.components.textpane;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.JTextPane;
import javax.swing.border.AbstractBorder;
import javax.swing.text.Document;

public class LineNumberedBorder extends AbstractBorder {
	private int etalon;
	private int lineNumberWidth;
	private JCayenneTextPane pane;

	public LineNumberedBorder(JCayenneTextPane pane) {
		setEtalon(10);
		this.pane = pane;
	}

	public Insets getBorderInsets(Component c) {
		return getBorderInsets(c, new Insets(0, 0, 0, 0));
	}

	public Insets getBorderInsets(Component c, Insets insets) {
		if (c instanceof JTextPane) {
			int width = lineNumberWidth((JTextPane) c);
			insets.left = width;
		}
		return insets;
	}

	/**
	 * Returns the width, in pixels
	 */
	private int lineNumberWidth(JTextPane textPane) {
		int lineCount = getEtalon();
		setLineNumberWidth(textPane.getFontMetrics(textPane.getFont())
				.stringWidth(lineCount + " "));
		return getLineNumberWidth();
	}

	public void paintBorder(Component c, Graphics g, int x, int y, int width,
			int height) {

		java.awt.Rectangle clip = g.getClipBounds();

		FontMetrics fm = g.getFontMetrics();
		int fontHeight = fm.getHeight();
		int ybaseline = y + fm.getAscent();

		int startingLineNumber = (clip.y / fontHeight) + 1;

		if (ybaseline < clip.y) {
			ybaseline = y + startingLineNumber * fontHeight
					- (fontHeight - fm.getAscent());
		}


		int yend = ybaseline + height;
		if (yend > (y + height)) {
			yend = y + height;
		}

		int lnxstart = x;
		int widhtBorder = getLineNumberWidth() - 2;

		g.setColor(new Color(255, 255, 224));
		g.fillRect(lnxstart, 0, lnxstart + widhtBorder, yend);
		g.setColor(new Color(214, 214, 214));
		g.drawRect(lnxstart - 1, -1, lnxstart + widhtBorder, yend + 1);

		int end = pane.getEndPositionInDocument();
		Document doc = pane.getDocument();
		int endline = doc.getDefaultRootElement().getElementIndex(end) + 1;
			
		while (startingLineNumber <= endline) {
			g.setColor(Color.gray);
			g.drawString(startingLineNumber + " ", lnxstart + 1, ybaseline);
			ybaseline += fontHeight;
			startingLineNumber++;
		}

		setEtalon(startingLineNumber-1);
	}

	public int getEtalon() {
		return etalon;
	}

	public void setEtalon(int etalon) {
		if(etalon<10){
			etalon = 10;
		}
		this.etalon = etalon;
	}

	public int getLineNumberWidth() {
		return lineNumberWidth;
	}

	public void setLineNumberWidth(int lineNumberWidth) {
		this.lineNumberWidth = lineNumberWidth;
	}
}
