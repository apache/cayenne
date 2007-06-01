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
package org.objectstyle.cayenne.pref;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;

/**
 * A Cayenne-based PreferenceService.
 * 
 * @author Andrei Adamchik
 */
public abstract class CayennePreferenceService implements PreferenceService {

    public static final int MIN_SAVE_INTERVAL = 500;
    public static final int DEFAULT_SAVE_INTERVAL = 20000;
    public static final String SAVE_INTERVAL_KEY = "saveInterval";

    protected int saveInterval = DEFAULT_SAVE_INTERVAL;
    protected Timer saveTimer;
    protected DataContext dataContext;
    protected String defaultDomain;

    public CayennePreferenceService(String defaultDomain) {
        this.defaultDomain = defaultDomain;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public void setDataContext(DataContext preferencesContext) {
        this.dataContext = preferencesContext;
    }

    public int getSaveInterval() {
        return saveInterval;
    }

    public void setSaveInterval(int ms) {
        if (this.saveInterval != ms) {
            this.saveInterval = ms;

            // save to preferences
            getPreferenceDomain()
                    .getDetail(SAVE_INTERVAL_KEY, true)
                    .setIntProperty(SAVE_INTERVAL_KEY, ms);
        }
    }

    /**
     * Returns a top-level domain.
     */
    public Domain getDomain(String name, boolean create) {
        List results = getDataContext().performQuery(
                "TopLevelDomain",
                Collections.singletonMap("name", name),
                false);

        if (results.size() > 1) {
            throw new CayenneRuntimeException("Found "
                    + results.size()
                    + " Domain objects for name '"
                    + name
                    + "', only one expected.");
        }

        if (results.size() == 1) {
            return (Domain) results.get(0);
        }

        if (!create) {
            return null;
        }

        Domain domain = (Domain) getDataContext()
                .createAndRegisterNewObject(Domain.class);
        domain.setLevel(new Integer(0));
        domain.setName(name);
        savePreferences();

        return domain;
    }

    /**
     * Configures service to run stopService on JVM shutdown.
     */
    public void stopOnShutdown() {
        Thread shutdown = new Thread("CayennePrefrencesService Shutdown") {

            public void run() {
                stopService();
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdown);
    }

    public void savePreferences() {
        DataContext context = this.dataContext;

        if (context != null) {
            context.commitChanges();
        }
    }

    protected Domain getPreferenceDomain() {
        Domain defaultDomain = getDomain(this.defaultDomain, true);
        return defaultDomain.getSubdomain(getClass());
    }

    /**
     * Initializes service own prefrences.
     */
    protected void initPreferences() {
        Domain preferenceDomain = getPreferenceDomain();
        PreferenceDetail saveInterval = preferenceDomain.getDetail(
                SAVE_INTERVAL_KEY,
                false);
        if (saveInterval != null) {
            setSaveInterval(saveInterval.getIntProperty(
                    SAVE_INTERVAL_KEY,
                    DEFAULT_SAVE_INTERVAL));
        }
    }

    /**
     * Helper method that provides an easy way for subclasses to create preferences schema
     * on the fly.
     */
    protected void initSchema() {
        DataDomain domain = dataContext.getParentDataDomain();

        Iterator maps = domain.getDataMaps().iterator();
        while (maps.hasNext()) {
            DataMap map = (DataMap) maps.next();
            DataNode node = domain.lookupDataNode(map);
            DbAdapter adapter = node.getAdapter();
            DbGenerator generator = new DbGenerator(adapter, map);

            try {
                generator.runGenerator(node.getDataSource());
            }
            catch (Throwable th) {
                throw new PreferenceException("Error creating preferences DB", th);
            }
        }
    }

    /**
     * Starts preferences save timer.
     */
    protected void startTimer() {
        TimerTask saveTask = new TimerTask() {

            public void run() {
                savePreferences();
            }
        };

        int interval = (saveInterval > MIN_SAVE_INTERVAL)
                ? saveInterval
                : MIN_SAVE_INTERVAL;
        saveTimer = new Timer(true);
        saveTimer.schedule(saveTask, interval, interval);
    }
}