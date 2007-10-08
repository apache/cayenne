package cayenne.tutorial.client;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cayenne.tutorial.client.auto._Artist;

/**
 * A persistent class mapped as "Artist" Cayenne entity.
 */
public class Artist extends _Artist {
    static final String DEFAULT_DATE_FORMAT = "yyyyMMdd";

    /**
     * Sets date of birth using a string in format yyyyMMdd.
     */
    public void setDateOfBirthString(String yearMonthDay) {
        if (yearMonthDay == null) {
            setDateOfBirth(null);
        }
        else {

            Date date;
            try {
                date = new SimpleDateFormat(DEFAULT_DATE_FORMAT).parse(yearMonthDay);
            }
            catch (ParseException e) {
                throw new IllegalArgumentException("A date argument must be in format '"
                        + DEFAULT_DATE_FORMAT
                        + "': "
                        + yearMonthDay);
            }

            setDateOfBirth(date);
        }
    }
}