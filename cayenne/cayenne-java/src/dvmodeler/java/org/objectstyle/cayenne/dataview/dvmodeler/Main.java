/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.dataview.dvmodeler;

import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

/**
 * Main DVModeler class. Configures and starts the main application frame.
 * 
 * @author Nataliya Kholodna
 */
public class Main {

    // note that some themse (e.g. "Desert Blue") do not support Chinese and
    // Japanese chars
    public static final String DEFAULT_THEME_NAME = "Sky Bluer";
    public static final String DEFAULT_LAF_NAME = PlasticXPLookAndFeel.class.getName();

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(DEFAULT_LAF_NAME);
            
            PlasticTheme foundTheme = themeWithName(DEFAULT_THEME_NAME);
            if (foundTheme != null) {
                PlasticLookAndFeel.setMyCurrentTheme(foundTheme);
            }
        }
        catch (Throwable th) {
            th.printStackTrace();
        }
   
        JFrame instance = new DVModelerFrame();

        instance.setSize(800, 600);
        instance.validate();
        instance.setLocationRelativeTo(null);
        instance.setVisible(true);
    }
    
    static PlasticTheme themeWithName(String themeName) {
        List availableThemes = PlasticLookAndFeel.getInstalledThemes();
        for (Iterator i = availableThemes.iterator(); i.hasNext();) {
            PlasticTheme aTheme = (PlasticTheme) i.next();
            if (themeName.equals(aTheme.getName())) {
                return aTheme;
            }
        }
        
        return null;
    }
}