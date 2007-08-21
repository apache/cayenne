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
package org.apache.cayenne.reflect;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.Fault;
import org.apache.cayenne.access.ToManyListFault;
import org.apache.cayenne.access.ToManyMapFault;
import org.apache.cayenne.access.ToManySetFault;
import org.apache.cayenne.access.ToOneFault;
import org.apache.cayenne.exp.Expression;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
public class SingletonFaultFactory implements FaultFactory {

    protected Fault toOneFault = new ToOneFault();
    protected Fault listFault = new ToManyListFault();
    protected Fault setFault = new ToManySetFault();
    protected Map mapFaults = new HashMap();

    public Fault getCollectionFault() {
        return listFault;
    }

    public Fault getListFault() {
        return listFault;
    }

    public Fault getMapFault(Expression mapKeyExpression) {
        synchronized (mapFaults) {
            Fault fault = (Fault) mapFaults.get(mapKeyExpression.toString());

            if (fault == null) {
                fault = new ToManyMapFault(mapKeyExpression);
                mapFaults.put(mapKeyExpression, fault);
            }

            return fault;
        }
    }

    public Fault getSetFault() {
        return setFault;
    }

    public Fault getToOneFault() {
        return toOneFault;
    }
}
