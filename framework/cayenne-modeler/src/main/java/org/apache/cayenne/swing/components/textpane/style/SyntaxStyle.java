package org.apache.cayenne.swing.components.textpane.style;

import java.awt.Color;
import java.awt.Font;

public final class SyntaxStyle {

    private Color color;
    private Font font;

    public SyntaxStyle(Color color, Font fontStyle) {
        super();
        this.color = color;
        this.font = fontStyle;
    }

    public Font getFont() {
        return font;
    }

    public Color getColor() {
        return color;
    }
}
