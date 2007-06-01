/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.access.event;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.objectstyle.cayenne.event.CayenneEvent;

/**
 * Event sent on modification of the DataRowStore. 
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class SnapshotEvent extends CayenneEvent {

    protected long timestamp;
    protected Collection deletedIds;
    protected Map modifiedDiffs;
    protected Collection indirectlyModifiedIds;

    public SnapshotEvent(
        Object source,
        Object postedBy,
        Map modifiedDiffs,
        Collection deletedIds,
        Collection indirectlyModifiedIds) {
            
        super(source, postedBy, null);
    
        this.timestamp = System.currentTimeMillis();
        this.modifiedDiffs = modifiedDiffs;
        this.deletedIds = deletedIds;
        this.indirectlyModifiedIds = indirectlyModifiedIds;
    }

    public long getTimestamp() {
        return timestamp;
    }
 
    public Map getModifiedDiffs() {
        return (modifiedDiffs != null) ? modifiedDiffs : Collections.EMPTY_MAP;
    }

    public Collection getDeletedIds() {
        return (deletedIds != null) ? deletedIds : Collections.EMPTY_LIST;
    }
    
    public Collection getIndirectlyModifiedIds() {
        return (indirectlyModifiedIds != null) ? indirectlyModifiedIds : Collections.EMPTY_LIST;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[SnapshotEvent] source: ").append(getSource());

        Map modified = getModifiedDiffs();
        if (!modified.isEmpty()) {
            buffer.append(", modified ").append(modified.size()).append(" id(s)");
        }

        Collection deleted = getDeletedIds();
        if (!deleted.isEmpty()) {
            buffer.append(", deleted ").append(deleted.size()).append(" id(s)");
        }
        
        Collection related = getIndirectlyModifiedIds();
        if (!related.isEmpty()) {
            buffer.append(", indirectly modified ").append(related.size()).append(" id(s)");
        }

        return buffer.toString();
    }
}
