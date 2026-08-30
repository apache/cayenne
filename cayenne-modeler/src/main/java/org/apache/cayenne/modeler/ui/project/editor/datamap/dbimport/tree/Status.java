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

package org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree;

import java.awt.Color;

enum Status {

    INCLUDE             (new Color(60,179,113), new Color(60,179,113)),
    EXCLUDE_EXPLICIT    (Color.GRAY,            Color.WHITE),
    EXCLUDE_IMPLICIT    (Color.BLACK,           Color.BLACK);

    private final Color color;
    private final Color selectionColor;

    Status(Color color, Color selectionColor) {
        this.color = color;
        this.selectionColor = selectionColor;
    }

    /**
     * Returns the text color for this status. Selected rows keep their color, except for the grey of an
     * explicit exclude, that is invisible against the selection background.
     */
    public Color getColor(boolean selected) {
        return selected ? selectionColor : color;
    }
}
