package org.apache.cayenne.swing.components.textpane;

import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import org.apache.cayenne.swing.components.textpane.syntax.SyntaxConstant;

public class TextPaneViewFactory extends Object implements ViewFactory {

    SyntaxConstant syntaxConstant;
    public TextPaneViewFactory(SyntaxConstant syntaxConstant) {
        this.syntaxConstant = syntaxConstant;
    }

    public View create(Element elem) {
        return new TextPaneView(elem, syntaxConstant);
    }
}
