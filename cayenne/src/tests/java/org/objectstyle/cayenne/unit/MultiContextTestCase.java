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

package org.objectstyle.cayenne.unit;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.util.Util;

/**
 * Superclass of test cases requiring multiple DataContexts with 
 * the same parent DataDomain.
 * 
 * @author Andrei Adamchik
 */
public abstract class MultiContextTestCase extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    protected void fixSharedConfiguration() {
        // for context to deserialize properly, 
        // Configuration singleton must have the right default domain
        Configuration config = Configuration.getSharedConfiguration();
        if (getDomain() != config.getDomain()) {
            if (config.getDomain() != null) {
                config.removeDomain(config.getDomain().getName());
            }
            config.addDomain(getDomain());
        }
    }

    /**
     * Helper method to create a new DataContext with the ObjectStore
     * state being the mirror of the given context. This is done by
     * serializing/deserializing the DataContext.
     */
    protected DataContext mirrorDataContext(DataContext context) throws Exception {
        fixSharedConfiguration();

        DataContext mirror = (DataContext) Util.cloneViaSerialization(context);

        assertNotSame(context, mirror);
        assertNotSame(context.getObjectStore(), mirror.getObjectStore());

        if (context.isUsingSharedSnapshotCache()) {
            assertSame(
                context.getObjectStore().getDataRowCache(),
                mirror.getObjectStore().getDataRowCache());
        }
        else {
            assertNotSame(
                context.getObjectStore().getDataRowCache(),
                mirror.getObjectStore().getDataRowCache());
        }

        return mirror;
    }
}
