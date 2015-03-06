package de.jexp.jequel.generator.processor;

import de.jexp.jequel.generator.data.*;

/**
 * @author mh14 @ jexp.de
 * @since 22.10.2007 22:40:28 (c) 2007 jexp.de
 */
public class DumpSchemaMetaDataProcessor extends SchemaMetaDataProcessor {
    public DumpSchemaMetaDataProcessor(final SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }

    public void processMetaData() {
        schemaMetaData.iterateAllColumns(new TableMetaDataIteratorCallback() {
            public void startTable(final TableMetaData table) {
                System.out.print("table " + table + " ( ");
            }

            public void endTable(final TableMetaData table) {
                System.out.println(")");
            }

            public void forColumn(final TableMetaData table, final TableMetaDataColumn column) {
                System.out.print(column.getName() + ", ");
            }
        });
    }
}
