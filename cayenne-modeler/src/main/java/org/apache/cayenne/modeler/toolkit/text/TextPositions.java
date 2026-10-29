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
package org.apache.cayenne.modeler.toolkit.text;

import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * Conversions between the line / column positions reported by the parsers and the offsets in a text document.
 */
public final class TextPositions {

    private TextPositions() {
    }

    /**
     * Returns the offset of a position in the document. The line and the column are 1-based, a line past the end of
     * the document is taken as the last one.
     */
    public static int offsetOf(Document doc, int line, int column) {
        Element root = doc.getDefaultRootElement();
        int lineIndex = Math.max(0, Math.min(line - 1, root.getElementCount() - 1));
        return root.getElement(lineIndex).getStartOffset() + Math.max(0, column - 1);
    }

    /**
     * Returns the 0-based index of the line that contains the offset.
     */
    public static int lineOf(Document doc, int offset) {
        return doc.getDefaultRootElement().getElementIndex(offset);
    }

    /**
     * Returns the offset and the length of a range of text, moved into the document, so that a range at the end of
     * the text is shown on its last char. The length is at least 1, unless the document is empty.
     */
    public static int[] clamp(Document doc, int offset, int size) {
        int length = doc.getLength();
        if (length == 0) {
            return new int[]{0, 0};
        }

        int start = Math.max(0, Math.min(offset, length - 1));
        return new int[]{start, Math.max(1, Math.min(size, length - start))};
    }

    /**
     * Returns the first line of a parser message, without the details that follow it.
     */
    public static String firstLine(String message, String defaultMessage) {
        if (message == null) {
            return defaultMessage;
        }
        int eol = message.indexOf('\n');
        return (eol >= 0 ? message.substring(0, eol) : message).trim();
    }
}
