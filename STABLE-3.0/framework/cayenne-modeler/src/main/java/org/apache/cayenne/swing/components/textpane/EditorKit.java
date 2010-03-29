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
