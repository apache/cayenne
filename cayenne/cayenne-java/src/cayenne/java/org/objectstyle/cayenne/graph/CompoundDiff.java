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
package org.objectstyle.cayenne.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A GraphDiff that is a list of other GraphDiffs.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class CompoundDiff implements GraphDiff {

    protected List diffs;

    /**
     * Creates an empty CompoundDiff instance.
     */
    public CompoundDiff() {
    }

    /**
     * Creates CompoundDiff instance. Note that a List is not cloned in this constructor,
     * so subsequent calls to add and addAll would modify the original list.
     */
    public CompoundDiff(List diffs) {
        this.diffs = diffs;
    }

    /**
     * Returns true if this diff has no other diffs or if all of its diffs are noops.
     */
    public boolean isNoop() {
        if (diffs == null || diffs.isEmpty()) {
            return true;
        }

        Iterator it = diffs.iterator();
        while (it.hasNext()) {
            if (!((GraphDiff) it.next()).isNoop()) {
                return false;
            }
        }

        return true;
    }

    public List getDiffs() {
        return (diffs != null)
                ? Collections.unmodifiableList(diffs)
                : Collections.EMPTY_LIST;
    }

    public void add(GraphDiff diff) {
        nonNullDiffs().add(diff);
    }

    public void addAll(Collection diffs) {
        nonNullDiffs().addAll(diffs);
    }

    /**
     * Iterates over diffs list, calling "apply" on each individual diff.
     */
    public void apply(GraphChangeHandler tracker) {
        if (diffs == null) {
            return;
        }

        // implements a naive linear commit - simply replay stored operations
        Iterator it = diffs.iterator();
        while (it.hasNext()) {
            GraphDiff change = (GraphDiff) it.next();
            change.apply(tracker);
        }
    }

    /**
     * Iterates over diffs list in reverse order, calling "apply" on each individual diff.
     */
    public void undo(GraphChangeHandler tracker) {
        if (diffs == null) {
            return;
        }

        ListIterator it = diffs.listIterator(diffs.size());
        while (it.hasPrevious()) {
            GraphDiff change = (GraphDiff) it.previous();
            change.undo(tracker);
        }
    }

    List nonNullDiffs() {
        if (diffs == null) {
            synchronized (this) {
                if (diffs == null) {
                    diffs = new ArrayList();
                }
            }
        }

        return diffs;
    }
}
