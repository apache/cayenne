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
package org.apache.cayenne.modeler.util;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.apache.cayenne.validation.ValidationException;

/**
 * Text adapter with live validation, which is fired in
 * VALIDATION_DELAY time.
 */
public abstract class ValidatorTextAdapter extends TextAdapter {
    /**
     * Time between end of user input and validation firing
     */
    static final long VALIDATION_DELAY = 1500L;
    
    /**
     * Is the live-checking enabled for the text component
     */
    boolean liveCheckEnabled;
    
    public ValidatorTextAdapter(JTextField textField) {
        this(textField, true);
    }
    
    public ValidatorTextAdapter(JTextField textField, boolean liveCheckEnabled) {
        super(textField, true, false, true);
        setLiveCheckEnabled(liveCheckEnabled);
        install(textField);
    }
    
    public ValidatorTextAdapter(JTextArea textArea) {
        this(textArea, true);
    }
    
    public ValidatorTextAdapter(JTextArea textArea, boolean liveCheckEnabled) {
        super(textArea, true, false);
        setLiveCheckEnabled(liveCheckEnabled);
        install(textArea);
    }
    
    protected void install(JTextComponent textComponent) {
        TimerScheduler ts = new TimerScheduler();
        
        textComponent.getDocument().addDocumentListener(ts);
        textComponent.addFocusListener(ts);
    }
    
    /**
     * Live-checks if text is correct
     * @throws ValidationException if the text is incorrect
     */
    protected abstract void validate(String text) throws ValidationException;
 
    /**
     * @return Is the live-checking enabled for the text component
     */
    public boolean isLiveCheckEnabled() {
        return liveCheckEnabled;
    }
    
    /**
     * Enables/disables live-checking
     */
    public void setLiveCheckEnabled(boolean b) {
        liveCheckEnabled = b;
    }
    
    /**
     * Task to be fired after some delay 
     */
    class ValidationTimerTask extends TimerTask {
        @Override
        public void run() {
            validate();
        }
    }
    
    protected void validate() {
        try {
            validate(textComponent.getText());
            clear();
        }
        catch (ValidationException vex) {
            textComponent.setBackground(errorColor);
            textComponent.setToolTipText(wrapTooltip(vex.getUnlabeledMessage()));
        }
    }
    
    /**
     * Wraps the tooltip, making it multi-lined if needed.
     * Current implementation uses HTML markup to break the lines
     * @param tooltip single-line tooltip
     * @return multi-line tooltip.
     */
    protected String wrapTooltip(String tooltip) {
        tooltip = encodeHTMLAttribute(tooltip);
        tooltip = tooltip.replaceAll(System.getProperty("line.separator"), "<br>");
        
        return "<html>" + tooltip + "</html>";
    }
    
    /**
     * Encodes a string so that it can be used as an attribute value in an HMTL document.
     * Will do conversion of the greater/less signs and ampersands.
     * 
     * ***
     * This method is almost a copy of Util.encodeXmlAttribute(), but it does not replace
     * single quotes. So only '<', '>', '&', '"' chars will be replaced. 
     */
    public static String encodeHTMLAttribute(String str) {
        if (str == null) {
            return null;
        }

        int len = str.length();
        if (len == 0) {
            return str;
        }

        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == '<') {
                encoded.append("&lt;");
            }
            else if (c == '\"') {
                encoded.append("&quot;");
            }
            else if (c == '>') {
                encoded.append("&gt;");
            }
            else if (c == '&') {
                encoded.append("&amp;");
            }
            else {
                encoded.append(c);
            }
        }

        return encoded.toString();
    }
    
    /**
     * Listener to user input, which fires validation timer 
     */
    class TimerScheduler implements DocumentListener, FocusListener {
        /**
         * The timer, which fires validation after some delay
         */
        Timer validationTimer;
        
        Object sync; //to prevent concurrent collisions
        
        TimerScheduler() {
            sync = new Object();
        }
        
        public void insertUpdate(DocumentEvent e) {
            schedule();
        }

        public void changedUpdate(DocumentEvent e) {
            schedule();
        }

        public void removeUpdate(DocumentEvent e) {
            schedule();
        }

        void schedule() {
            if(isLiveCheckEnabled()) {
                synchronized (sync) {
                    if(validationTimer != null) {
                        validationTimer.cancel();
                    }
                    
                    clear();
                    
                    validationTimer = new Timer("cayenne-validation-timer");
                    validationTimer.schedule(new ValidationTimerTask(), VALIDATION_DELAY);
                }
            }
        }

        public void focusGained(FocusEvent e) {
        }

        public void focusLost(FocusEvent e) {
            synchronized (sync) {
                if(validationTimer != null) {
                    validationTimer.cancel();
                }
            }
        };
    }
}
