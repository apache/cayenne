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
package org.apache.art;

/**
 * Class for testing callbacks on ObjEntity level
 *
 * @author Vasil Tarasevich
 * @version 1.0 Oct 22, 2007
 */

public class EntityListenerObjEntity {
    public void prePersistEntityListener(ArtistCallbackTest entity) {}
    public void postPersistEntityListener(ArtistCallbackTest entity) {}
    public void preUpdateEntityListener(ArtistCallbackTest entity) {}
    public void postUpdateEntityListener(ArtistCallbackTest entity) {}
    public void preRemoveEntityListener(ArtistCallbackTest entity) {}
    public void postRemoveEntityListener(ArtistCallbackTest entity) {}
    public void postLoadEntityListener(ArtistCallbackTest entity) {}
}

