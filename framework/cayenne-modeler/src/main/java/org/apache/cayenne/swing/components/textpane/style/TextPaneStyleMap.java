package org.apache.cayenne.swing.components.textpane.style;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;

import org.apache.cayenne.swing.components.textpane.syntax.SQLSyntaxConstants;



public class TextPaneStyleMap {
	
	public HashMap<TextPaneStyleTypes, SyntaxStyle> syntaxStyleMap = new HashMap<TextPaneStyleTypes, SyntaxStyle>();
	
	public TextPaneStyleMap() {
		Font plainFont = SQLSyntaxConstants.DEFAULT_FONT;
		Font boldFont = new Font(plainFont.getFamily(), Font.BOLD , plainFont.getSize());
		Font italicFont = new Font(plainFont.getFamily(), Font.ITALIC, plainFont.getSize());
		
		SyntaxStyle keywordsStyle = new SyntaxStyle(Color.blue, boldFont);
		SyntaxStyle numberStyle = new SyntaxStyle(Color.ORANGE, italicFont);
		SyntaxStyle typeStyle = new SyntaxStyle(Color.BLUE, boldFont);
		SyntaxStyle stringStyle = new SyntaxStyle(Color.blue, italicFont);
		SyntaxStyle commentStyle = new SyntaxStyle(Color.LIGHT_GRAY, italicFont);
		SyntaxStyle keywords2Style = new SyntaxStyle(Color.MAGENTA, plainFont);
		
		syntaxStyleMap.put(TextPaneStyleTypes.KEYWORDS, keywordsStyle);
		syntaxStyleMap.put(TextPaneStyleTypes.KEYWORDS2, keywords2Style);
		syntaxStyleMap.put(TextPaneStyleTypes.OPERATORS, keywordsStyle);
		syntaxStyleMap.put(TextPaneStyleTypes.NUMBER, numberStyle);
		syntaxStyleMap.put(TextPaneStyleTypes.TYPE, typeStyle);
		syntaxStyleMap.put(TextPaneStyleTypes.STRING, stringStyle);
		syntaxStyleMap.put(TextPaneStyleTypes.COMMENT, commentStyle);	
	}

}
