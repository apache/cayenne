package de.jexp.jequel.generator.processor;

import de.jexp.jequel.generator.data.*;

public class DumpSchemaMetaDataProcessor extends SchemaMetaDataProcessor {
    public DumpSchemaMetaDataProcessor(SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }

    public void processMetaData() {
        getSchemaMetaData().iterateAllColumns(new TableMetaDataIteratorCallback() {
            @Override
            public void startTable(TableMetaData table) {
                System.out.print("table " + table + " ( ");
            }

            @Override
            public void endTable(TableMetaData table) {
                System.out.println(")");
            }

            @Override
            public void forColumn(TableMetaData table, TableMetaDataColumn column) {
                System.out.print(column.getName() + ", ");
            }
        });
    }
}
