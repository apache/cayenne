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
package org.objectstyle.cayenne.modeler.util;

import java.util.StringTokenizer;

/**
 * Helper class to deal with version strings.
 * 
 * @author Andrus Adamchik
 */
public class Version implements Comparable {

    protected String versionString;
    protected int[] versionParts;

    public Version(String versionString) throws NumberFormatException {
        if (versionString == null) {
            throw new IllegalArgumentException("Null version.");
        }

        if (versionString.trim().length() == 0) {
            throw new IllegalArgumentException("Empty version.");
        }

        this.versionString = versionString;

        StringTokenizer toks = new StringTokenizer(versionString, ".");
        versionParts = new int[toks.countTokens()];

        for (int i = 0; i < versionParts.length; i++) {
            versionParts[i] = Integer.parseInt(toks.nextToken());
        }
    }

    public int compareTo(Object o) {

        if (o instanceof CharSequence) {
            o = new Version(o.toString());
        }
        else if (!(o instanceof Version)) {
            throw new IllegalArgumentException(
                    "Can only compare to Versions and Strings, got: " + o);
        }

        int[] otherVersion = ((Version) o).versionParts;

        int len = Math.min(otherVersion.length, versionParts.length);
        for (int i = 0; i < len; i++) {

            int delta = versionParts[i] - otherVersion[i];
            if (delta != 0) {
                return delta;
            }
        }

        if (versionParts.length < otherVersion.length) {
            return -1;
        }
        else if (versionParts.length > otherVersion.length) {
            return 1;
        }
        else {
            return 0;
        }
    }

    public String getVersionString() {
        return versionString;
    }
}
