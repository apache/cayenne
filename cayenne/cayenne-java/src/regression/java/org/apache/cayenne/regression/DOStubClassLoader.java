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

 
package org.apache.cayenne.regression;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ClassGen;

/**
 * DOStubClassLoader is a simple class loader to generate new types of DataObjects
 * (new classes) at runtime. This behavior is needed to trick Cayenne algorithms
 * based on usage of Classes of DataObject descendants for identifiers.
 *
 * @author Andriy Shapochka
 */

class DOStubClassLoader extends ClassLoader {
  private String superClassName = "org.apache.cayenne.CayenneDataObject";

  public Class findClass(String name) {
    byte[] b = loadClassData(name);
    return defineClass(name, b, 0, b.length);
  }

  private byte[] loadClassData(String name) {
    ClassGen cg = new ClassGen(name, superClassName,
                             "<generated>", Constants.ACC_PUBLIC | Constants.ACC_SUPER,
                             null);
    cg.addEmptyConstructor(Constants.ACC_PUBLIC);
    JavaClass hw = cg.getJavaClass();
    return hw.getBytes();
  }
}
