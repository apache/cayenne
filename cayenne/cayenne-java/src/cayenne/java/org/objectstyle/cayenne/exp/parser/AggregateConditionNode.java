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
package org.objectstyle.cayenne.exp.parser;

import org.apache.commons.collections.Transformer;
import org.objectstyle.cayenne.exp.ExpressionException;

/**
 * Superclass of aggregated conditional nodes such as NOT, AND, OR. Performs
 * extra checks on parent and child expressions to validate conditions that
 * are not addressed in the Cayenne expressions grammar.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public abstract class AggregateConditionNode extends SimpleNode {
    AggregateConditionNode(int i) {
        super(i);
    }

    protected boolean pruneNodeForPrunedChild(Object prunedChild) {
        return false;
    }

    protected Object transformExpression(Transformer transformer) {
        Object transformed = super.transformExpression(transformer);

        if (!(transformed instanceof AggregateConditionNode)) {
            return transformed;
        }
        
        AggregateConditionNode condition = (AggregateConditionNode) transformed;

        // prune itself if the transformation resulted in 
        // no children or a single child
        switch (condition.getOperandCount()) {
            case 1 :
                return condition.getOperand(0);
            case 0 :
                return PRUNED_NODE;
            default :
                return condition;
        }
    }

    public void jjtSetParent(Node n) {
        // this is a check that we can't handle properly
        // in the grammar... do it here...

        // disallow non-aggregated condition parents...
        if (!(n instanceof AggregateConditionNode)) {
            String label =
                (n instanceof SimpleNode)
                    ? ((SimpleNode) n).expName()
                    : String.valueOf(n);
            throw new ExpressionException(expName() + ": invalid parent - " + label);
        }

        super.jjtSetParent(n);
    }

    public void jjtAddChild(Node n, int i) {
        // this is a check that we can't handle properly
        // in the grammar... do it here...

        // only allow conditional nodes...no scalars
        if (!(n instanceof ConditionNode) && !(n instanceof AggregateConditionNode)) {
            String label =
                (n instanceof SimpleNode)
                    ? ((SimpleNode) n).expName()
                    : String.valueOf(n);
            throw new ExpressionException(expName() + ": invalid child - " + label);
        }

        super.jjtAddChild(n, i);
    }
}
