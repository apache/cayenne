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

package org.apache.cayenne.project.xml;

import java.util.function.Supplier;

import org.apache.cayenne.map.Commentable;
import org.xml.sax.Attributes;

/**
 * Reads the "comment" tag of an element that the parent handler processes inline, such as "dbAttribute", as the parent
 * handler doesn't get the text of the nested tags of such an element.
 */
final class CommentHandler extends NamespaceAwareNestedTagHandler {

    static final String COMMENT_TAG = "comment";

    private final Supplier<? extends Commentable> target;

    /**
     * @param target supplies the commented object, which the parent handler creates only after this handler
     */
    CommentHandler(NamespaceAwareNestedTagHandler parentHandler, Supplier<? extends Commentable> target) {
        super(parentHandler);
        this.target = target;
    }

    static void loadComment(LoaderContext loaderContext, Commentable target, String text) {
        // not trimmed, so that a comment is loaded back unchanged
        if (!loaderContext.isFullModel() || text.isBlank()) {
            return;
        }

        target.setComment(text);
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        return COMMENT_TAG.equals(localName);
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        if (!COMMENT_TAG.equals(localName)) {
            return false;
        }

        loadComment(loaderContext, target.get(), data);
        return true;
    }
}
