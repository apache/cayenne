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

package org.apache.cayenne.exp.parser;

import java.util.regex.Pattern;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.Util;

/**
 * Superclass of pattern matching nodes. Assumes that subclass is a binary expression with
 * the second operand being a pattern.
 * 
 * @since 1.1
 */
public abstract class PatternMatchNode extends ConditionNode {

    protected transient Pattern pattern;
    protected transient boolean patternCompiled;
    protected boolean ignoringCase;
    protected char escapeChar;

    PatternMatchNode(int i, boolean ignoringCase) {
        super(i);
        this.ignoringCase = ignoringCase;
    }
    
    PatternMatchNode(int i, boolean ignoringCase, char escapeChar) {
        super(i);
        this.ignoringCase = ignoringCase;
        setEscapeChar(escapeChar);
    }
    
    /**
     * <p>This method will return an escape character for the like
     * clause.  The escape character will eventually end up in the
     * query as <code>...(t0.foo LIKE ? {escape '|'}) where the
     * pipe symbol is the escape character.</p>
     *
     * <p>Note that having no escape character is represented as 
     * the character 0.</p>
     */
    
    public char getEscapeChar() { return escapeChar; }
    
    /**
     * <p>This method allows the setting of the escape character.
     * The escape character can be used in a LIKE clause.  The
     * character 0 signifies no escape character.  The escape
     * characyer '?' is disallowed.</p>
     */
    
    public void setEscapeChar(char value) {
        
        if('?'==value)
            throw new CayenneRuntimeException("the use of the '?' as an escape character in LIKE clauses is disallowed.");
        
        escapeChar = value;
    }

    protected boolean matchPattern(String string) {
        return (string != null) ? getPattern().matcher(string).find() : false;
    }

    protected Pattern getPattern() {
        // compile pattern on demand
        if (!patternCompiled) {

            synchronized (this) {

                if (!patternCompiled) {
                    pattern = null;

                    if (jjtGetNumChildren() < 2) {
                        patternCompiled = true;
                        return null;
                    }

                    // precompile pattern
                    ASTScalar patternNode = (ASTScalar) jjtGetChild(1);
                    if (patternNode == null) {
                        patternCompiled = true;
                        return null;
                    }

                    String srcPattern = (String) patternNode.getValue();
                    if (srcPattern == null) {
                        patternCompiled = true;
                        return null;
                    }

                    pattern = Util.sqlPatternToPattern(srcPattern, ignoringCase);
                    patternCompiled = true;
                }
            }
        }

        return pattern;
    }

    @Override
    public void jjtAddChild(Node n, int i) {
        // reset pattern if the node is modified
        if (i == 1) {
            patternCompiled = false;
        }

        super.jjtAddChild(n, i);
    }
}
