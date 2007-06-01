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
package org.objectstyle.cayenne.modeler.dialog.objentity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.oro.text.perl.Perl5Util;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.util.Util;
import org.scopemvc.core.ModelChangeEvent;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;

/**
 * A model representing an Entity with a set of Relationships, with zero or one
 * selected Relationship.
 *  
 * @since 1.1
 * @author Andrei Adamchik
 */
public class EntityRelationshipsModel extends BasicModel {
    private static final Perl5Util regexUtil = new Perl5Util();

    public static final Selector RELATIONSHIP_DISPLAY_NAME_SELECTOR =
        Selector.fromString("relationshipDisplayName");

    protected Entity sourceEntity;
    protected String relationshipDisplayName;
    protected String defaultTargetName;
    protected Object[] relationshipNames;

    static String nameFromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }

        return regexUtil.match("/\\s\\[.+\\]$/", displayName)
            ? regexUtil.substitute("s/\\s\\[.+\\]$//g", displayName)
            : displayName;
    }

    static String displayName(Relationship relationship) {
        if (relationship == null) {
            return null;
        }
        return displayName(
            relationship.getName(),
            relationship.getSourceEntity(),
            relationship.getTargetEntity());
    }

    static String displayName(String name, Entity source, Entity target) {
        return name + " [" + source.getName() + " -> " + target.getName() + "]";
    }

    /**
     * Creates EntityRelationshipsModel with two unconnected Entities.
     */
    public EntityRelationshipsModel(Entity sourceEntity, Entity targetEntity) {
        this.sourceEntity = sourceEntity;
        this.defaultTargetName = targetEntity.getName();
        this.relationshipDisplayName = "";
    }

    /**
     * Creates EntityRelationshipsModel over the relationship connecting
     * two Entities.
     */
    public EntityRelationshipsModel(Relationship relationship) {
        this.sourceEntity = relationship.getSourceEntity();
        this.relationshipDisplayName = displayName(relationship);
    }

    public synchronized Object[] getRelationshipNames() {
        // build an ordered list of available relationship names
        // on demand
        if (relationshipNames == null) {
            Collection relationships = getSourceEntity().getRelationships();
            int size = relationships.size();
            Object[] names = new Object[size];

            Iterator it = relationships.iterator();
            for (int i = 0; i < size; i++) {
                DbRelationship next = (DbRelationship) it.next();
                names[i] = displayName(next);
            }
            Arrays.sort(names);
            this.relationshipNames = names;
        }

        return relationshipNames;
    }

    /**
     * Returns a root entity of this model.
     * @return
     */
    public Entity getSourceEntity() {
        return sourceEntity;
    }

    /**
     * Returns a String describing currently selected relationship.
     */
    public String getRelationshipDisplayName() {
        return relationshipDisplayName;
    }

    public void setRelationshipDisplayName(String relationshipDisplayName) {
        if (!Util
            .nullSafeEquals(relationshipDisplayName, this.relationshipDisplayName)) {
            this.relationshipDisplayName = relationshipDisplayName;
            relationshipNames = null;
            fireModelChange(
                ModelChangeEvent.VALUE_CHANGED,
                RELATIONSHIP_DISPLAY_NAME_SELECTOR);
        }
    }

    public void setRelationshipName(String relationshipName) {
        setRelationshipDisplayName(
            displayName(sourceEntity.getRelationship(relationshipName)));
    }

    public Relationship getSelectedRelationship() {
        return sourceEntity.getRelationship(nameFromDisplayName(relationshipDisplayName));
    }

    public String getSourceEntityName() {
        return sourceEntity.getName();
    }

    public String getTargetEntityName() {
        Relationship selected = getSelectedRelationship();
        return (selected != null) ? selected.getTargetEntityName() : defaultTargetName;
    }
}
