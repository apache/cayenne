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


package org.apache.cayenne.event;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * This class encapsulates the String that is used to identify the <em>subject</em> that
 * a listener is interested in. Using plain Strings causes several severe problems:
 * <ul>
 * <li>it's easy to misspell a subject, leading to undesired behaviour at runtime that is
 * hard to debug.</li>
 * <li>in systems with many different subjects there is no safeguard for defining the
 * same subject twice for different purposes. This is especially true in a distributed
 * setting.
 * </ul>
 * 
 * @author Dirk Olmes
 * @author Holger Hoffstaette
 */
public class EventSubject implements Serializable {

    // a Map that will allow the values to be GC'ed
    private static Map<String, EventSubject> _registeredSubjects = new ReferenceMap(
            ReferenceMap.HARD,
            ReferenceMap.WEAK);

    // Subject identifier in the form "com.foo.bar/SubjectName"
    private String _fullyQualifiedSubjectName;

    /**
     * Returns an event subject identified by the given owner and subject name.
     * 
     * @param subjectOwner the Class used for uniquely identifying this subject
     * @param subjectName a String used as name, e.g. "MyEventTopic"
     * @throws IllegalArgumentException if subjectOwner/subjectName are <code>null</code>
     *             or subjectName is empty.
     */
    public static EventSubject getSubject(Class<?> subjectOwner, String subjectName) {
        if (subjectOwner == null) {
            throw new IllegalArgumentException("Owner class must not be null.");
        }

        if ((subjectName == null) || (subjectName.length() == 0)) {
            throw new IllegalArgumentException("Subject name must not be null or empty.");
        }

        String fullSubjectName = subjectOwner.getName() + "/" + subjectName;
        EventSubject newSubject = _registeredSubjects.get(fullSubjectName);
        if (newSubject == null) {
            newSubject = new EventSubject(fullSubjectName);
            _registeredSubjects.put(newSubject.getSubjectName(), newSubject);
        }

        return newSubject;
    }

    /**
     * Private constructor to force use of #getSubject(Class, String)
     */
    @SuppressWarnings("unused")
    private EventSubject() {
    }

    /**
     * Protected constructor for new subjects.
     * 
     * @param fullSubjectName the name of the new subject to be created
     */
    protected EventSubject(String fullSubjectName) {
        _fullyQualifiedSubjectName = fullSubjectName;
    }

    public boolean equals(Object obj) {
        if (obj instanceof EventSubject) {
            return _fullyQualifiedSubjectName.equals(((EventSubject) obj)
                    .getSubjectName());
        }

        return false;
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 3).append(_fullyQualifiedSubjectName).toHashCode();
    }

    public String getSubjectName() {
        return _fullyQualifiedSubjectName;
    }

    /**
     * @return a String in the form <code>&lt;ClassName 0x123456&gt; SomeName</code>
     * @see Object#toString()
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(64);

        buf.append("<");
        buf.append(this.getClass().getName());
        buf.append(" 0x");
        buf.append(Integer.toHexString(System.identityHashCode(this)));
        buf.append("> ");
        buf.append(_fullyQualifiedSubjectName);

        return buf.toString();
    }
}
