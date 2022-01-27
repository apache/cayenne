package org.apache.cayenne.dba.h2;

import org.apache.cayenne.access.types.CharType;

import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.Types;

/**
 * H2 specific char type handling - used to handle the correct setting of clobs
 *
 * @since 4.1.2
 */
public class H2CharType extends CharType {

    public H2CharType() {
        super(true, true);
    }

    @Override
    public void setJdbcObject(PreparedStatement st, String val, int pos, int type, int precision) throws Exception {

        if (type == Types.CLOB) {

            if (isUsingClobs()) {

                Clob clob = st.getConnection().createClob();
                clob.setString(1, val);
                st.setClob(pos, clob);

            } else {
                st.setString(pos, val);
            }
        } else {
            st.setObject(pos, val);
        }
    }
}
