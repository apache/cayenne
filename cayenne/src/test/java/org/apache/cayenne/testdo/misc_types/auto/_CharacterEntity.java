package org.apache.cayenne.testdo.misc_types.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.misc_types.CharacterEntity;

/**
 * Class _CharacterEntity was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _CharacterEntity extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<CharacterEntity> SELF = PropertyFactory.createSelf(CharacterEntity.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "CharacterEntity", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final BaseProperty<Character> CHARACTER_FIELD = PropertyFactory.createBase("characterField", Character.class);

    protected Character characterField;


    public void setCharacterField(Character characterField) {
        beforePropertyWrite("characterField", this.characterField, characterField);
        this.characterField = characterField;
    }

    public Character getCharacterField() {
        beforePropertyRead("characterField");
        return this.characterField;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "characterField":
                return this.characterField;
            default:
                return super.readPropertyDirectly(propName);
        }
    }

    @Override
    public void writePropertyDirectly(String propName, Object val) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch (propName) {
            case "characterField":
                this.characterField = (Character)val;
                break;
            default:
                super.writePropertyDirectly(propName, val);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeSerialized(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readSerialized(in);
    }

    @Override
    protected void writeState(ObjectOutputStream out) throws IOException {
        super.writeState(out);
        out.writeObject(this.characterField);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.characterField = (Character)in.readObject();
    }

}
