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

package org.apache.cayenne;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

public class TranslationCase {

    protected Object tstObject;
    protected String sqlExp;
    protected String rootEntity;

    public TranslationCase(String rootEntity, Object tstObject, String sqlExp) {
        this.tstObject = tstObject;
        this.rootEntity = rootEntity;
        this.sqlExp = trim("\\b\\w+\\.", sqlExp);
    }

    protected String trim(String pattern, String str) {
        return trim(pattern, str, "");
    }

    protected String trim(String pattern, String str, String subst) {
        Matcher matcher = Pattern.compile(pattern).matcher(str);
        return (matcher.find()) ? matcher.replaceFirst(subst) : str;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(this.getClass().getName()).append(tstObject);
        return buf.toString();
    }

    public void assertTranslatedWell(String translated) {
        if (sqlExp == null) {
            Assert.assertNull(translated);
            return;
        }

        Assert.assertNotNull(translated);

        // strip column aliases
        String aliasSubstituted = trim("\\b\\w+\\.", translated);
        Assert.assertEquals(
                "Unexpected translation: " + translated + "....",
                sqlExp,
                aliasSubstituted);
    }

    public String getRootEntity() {
        return rootEntity;
    }

    public String getSqlExp() {
        return sqlExp;
    }
}
