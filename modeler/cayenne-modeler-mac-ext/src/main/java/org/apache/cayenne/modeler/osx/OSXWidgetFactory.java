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
package org.apache.cayenne.modeler.osx;

import java.awt.event.KeyEvent;

import org.apache.cayenne.modeler.util.DefaultWidgetFactory;
import org.syntax.jedit.DefaultInputHandler;
import org.syntax.jedit.JEditTextArea;

public class OSXWidgetFactory extends DefaultWidgetFactory {

    @Override
    public JEditTextArea createJEditTextArea() {
        JEditTextArea area = super.createJEditTextArea();
        area.setInputHandler(new MacInputHandler());
        return area;
    }

    /**
     * Class for enabling Mac OS X keys
     */
    private static class MacInputHandler extends DefaultInputHandler {

        MacInputHandler() {
            addDefaultKeyBindings();
        }

        public void addDefaultKeyBindings() {
            addKeyBinding("BACK_SPACE", BACKSPACE);
            addKeyBinding("M+BACK_SPACE", BACKSPACE_WORD);
            addKeyBinding("DELETE", DELETE);
            addKeyBinding("M+DELETE", DELETE_WORD);

            addKeyBinding("ENTER", INSERT_BREAK);
            addKeyBinding("TAB", INSERT_TAB);

            addKeyBinding("INSERT", OVERWRITE);
            addKeyBinding("M+\\", TOGGLE_RECT);

            addKeyBinding("HOME", HOME);
            addKeyBinding("END", END);
            addKeyBinding("M+A", SELECT_ALL);
            addKeyBinding("S+HOME", SELECT_HOME);
            addKeyBinding("S+END", SELECT_END);
            addKeyBinding("M+HOME", DOCUMENT_HOME);
            addKeyBinding("M+END", DOCUMENT_END);
            addKeyBinding("MS+HOME", SELECT_DOC_HOME);
            addKeyBinding("MS+END", SELECT_DOC_END);

            addKeyBinding("PAGE_UP", PREV_PAGE);
            addKeyBinding("PAGE_DOWN", NEXT_PAGE);
            addKeyBinding("S+PAGE_UP", SELECT_PREV_PAGE);
            addKeyBinding("S+PAGE_DOWN", SELECT_NEXT_PAGE);

            addKeyBinding("LEFT", PREV_CHAR);
            addKeyBinding("S+LEFT", SELECT_PREV_CHAR);
            addKeyBinding("A+LEFT", PREV_WORD); // option + left
            addKeyBinding("AS+LEFT", SELECT_PREV_WORD); // option + shift + left
            addKeyBinding("RIGHT", NEXT_CHAR);
            addKeyBinding("S+RIGHT", SELECT_NEXT_CHAR);
            addKeyBinding("A+RIGHT", NEXT_WORD); // option + right
            addKeyBinding("AS+RIGHT", SELECT_NEXT_WORD); // option + shift + right
            addKeyBinding("UP", PREV_LINE);
            addKeyBinding("S+UP", SELECT_PREV_LINE);
            addKeyBinding("DOWN", NEXT_LINE);
            addKeyBinding("S+DOWN", SELECT_NEXT_LINE);

            addKeyBinding("M+ENTER", REPEAT);

            // Clipboard
            addKeyBinding("M+C", CLIP_COPY); // command + c
            addKeyBinding("M+V", CLIP_PASTE); // command + v
            addKeyBinding("M+X", CLIP_CUT); // command + x
        }
        
        @Override
        public void keyTyped(KeyEvent evt) {
            
            // keys pressed with command key shouldn't generate text
            int modifiers = evt.getModifiers();
            if ((modifiers & KeyEvent.META_MASK) == 0) {
                super.keyTyped(evt);
            }
        }
    }
}
