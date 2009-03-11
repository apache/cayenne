package org.apache.cayenne.swing.components.textpane;

import javax.swing.text.StyledEditorKit;
import javax.swing.text.ViewFactory;
import org.apache.cayenne.swing.components.textpane.syntax.SyntaxConstant;

public class EditorKit extends StyledEditorKit {

    private ViewFactory xmlViewFactory;
    private String contentType;

    public EditorKit(SyntaxConstant syntaxConstant) {
        contentType = syntaxConstant.getContentType();
        xmlViewFactory = new TextPaneViewFactory(syntaxConstant);
    }

    @Override
    public ViewFactory getViewFactory() {
        return xmlViewFactory;
    }

    @Override
    public String getContentType() {
        return contentType;
    }
}
