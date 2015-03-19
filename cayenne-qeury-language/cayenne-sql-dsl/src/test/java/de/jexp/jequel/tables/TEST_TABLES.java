package de.jexp.jequel.tables;

import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;
import de.jexp.jequel.table.types.NUMERIC;

public interface TEST_TABLES {
    ARTICLE ARTICLE = new ARTICLE();

    final class ARTICLE extends BaseTable<ARTICLE> {
        public NUMERIC OID = integer().primaryKey();
        public Field NAME = string();
        public Field<Integer> ARTICLE_NO = integer();

        {
            initFields();
        }
    }

    /**
     * beim Kunden ist das der Artikel
     */
    ARTICLE_COLOR ARTICLE_COLOR = new ARTICLE_COLOR();

    final class ARTICLE_COLOR extends BaseTable<ARTICLE_COLOR> {
        public Field<Integer> OID = integer().primaryKey();
        public Field ARTICLE_OID = foreignKey(ARTICLE.OID);

        {
            initFields();
        }
    }


    ARTICLE_EAN ARTICLE_EAN = new ARTICLE_EAN();

    final class ARTICLE_EAN extends BaseTable<ARTICLE_EAN> {
        public Field OID = integer().primaryKey();
        public Field EAN = string();
        public Field ARTICLE_OID = foreignKey(ARTICLE.OID);

        {
            initFields();
        }

    }
}
