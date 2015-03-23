package de.jexp.jequel.tables;

import de.jexp.jequel.expression.Table;
import de.jexp.jequel.expression.IColumn;
import de.jexp.jequel.expression.types.INTEGER;
import de.jexp.jequel.expression.types.NUMERIC;

public interface TEST_TABLES {
    ARTICLE ARTICLE = new ARTICLE();

    final class ARTICLE extends Table<ARTICLE> {
        public NUMERIC OID = integer().primaryKey();
        public IColumn NAME = string();
        public IColumn ARTICLE_NO = integer();

        {
            initFields();
        }
    }

    /**
     * beim Kunden ist das der Artikel
     */
    ARTICLE_COLOR ARTICLE_COLOR = new ARTICLE_COLOR();

    final class ARTICLE_COLOR extends Table<ARTICLE_COLOR> {
        public INTEGER OID = integer().primaryKey();
        public IColumn ARTICLE_OID = foreignKey(ARTICLE.OID);

        {
            initFields();
        }
    }


    ARTICLE_EAN ARTICLE_EAN = new ARTICLE_EAN();

    final class ARTICLE_EAN extends Table<ARTICLE_EAN> {
        public IColumn OID = integer().primaryKey();
        public IColumn EAN = string();
        public IColumn ARTICLE_OID = foreignKey(ARTICLE.OID);

        {
            initFields();
        }

    }
}
