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

package org.apache.cayenne.modeler.undo;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class ChangePKGeneratorUndoableEdit extends CayenneUndoableEdit {

    private DbEntity dbEntity;

    private PkGeneratorState oldState;

    private PkGeneratorState newState;

    public ChangePKGeneratorUndoableEdit(DbEntity dbEntity) {
        this.dbEntity = dbEntity;
    }

    public void captureOldState() {
        oldState = captureState();
    }

    public void captureNewState() {
        newState = captureState();
    }

    private PkGeneratorState captureState() {
        return new PkGeneratorState(dbEntity.getPrimaryKeyGenerator(), findGeneratedAttribute());
    }

    private DbAttribute findGeneratedAttribute() {
        for (DbAttribute attribute : dbEntity.getPrimaryKeys()) {
            if(attribute.isGenerated()) {
                return attribute;
            }
        }
        return null;
    }

    @Override
    public void redo() throws CannotRedoException {
        newState.apply();
        fireEvents();
    }

    @Override
    public void undo() throws CannotUndoException {
        oldState.apply();
        fireEvents();
    }

    private void fireEvents() {
        controller.fireDbEntityEvent(new EntityEvent(this, dbEntity));
        controller.fireDbEntityDisplayEvent(new EntityDisplayEvent(this, dbEntity));
    }

    public boolean hasRealChange() {
        return !oldState.equals(newState);
    }

    private class PkGeneratorState {
        private DbKeyGenerator generator;
        private DbAttribute generatedAttribute;
        private PkGeneratorState(DbKeyGenerator generator, DbAttribute generatedAttribute) {
            this.generator = generator;
            this.generatedAttribute = generatedAttribute;
        }

        private void resetState() {
            DbAttribute oldAttribute = findGeneratedAttribute();
            if(oldAttribute != null) {
                oldAttribute.setGenerated(false);
            }
            dbEntity.setPrimaryKeyGenerator(null);
        }

        private void apply() {
            resetState();

            if(generator != null) {
                dbEntity.setPrimaryKeyGenerator(generator);
            }
            if(generatedAttribute != null) {
                generatedAttribute.setGenerated(true);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PkGeneratorState that = (PkGeneratorState) o;

            if (generator != null ? !generator.equals(that.generator) : that.generator != null) return false;
            return generatedAttribute != null ? generatedAttribute.equals(that.generatedAttribute) : that.generatedAttribute == null;

        }

        @Override
        public int hashCode() {
            int result = generator != null ? generator.hashCode() : 0;
            result = 31 * result + (generatedAttribute != null ? generatedAttribute.hashCode() : 0);
            return result;
        }
    }

}
