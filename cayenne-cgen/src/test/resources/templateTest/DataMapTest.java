package test;

import test.auto._DataMapTest;

public class DataMapTest extends _DataMapTest {

    private static DataMapTest instance;

    private DataMapTest() {}

    public static DataMapTest getInstance() {
        if(instance == null) {
            instance = new DataMapTest();
        }

        return instance;
    }
}
