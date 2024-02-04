package org3.sport.timemarker.v1;

import java.util.Date;

/**
 * @author ikh
 * @since 2/9/14
 */
public class Marker {
    private final long timestamp;
    private final long precision; // o - precision was not accumulated
    private final Date date;
    private final String formattedPrecision;
    private Flag flag = Flag.NONE;

    public Marker(long timestamp, long precision) {

        this.timestamp = timestamp;
        this.precision = precision;

        date = new Date(timestamp);
        formattedPrecision = formatListItemPrecision(precision);
    }

    public Date getDate() {
        return date;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getPrecision() {
        return precision;
    }

    public String getFormattedPrecision() {
        return formattedPrecision;
    }

    public static String formatListItemPrecision(long precision) {
        String tpsString;
        if (precision > 0) {
            tpsString = String.format("± %d ms", precision);
        } else {
            tpsString = "NA";
        }
        return tpsString;
    }


    public void setFlag(Flag flag) {
        this.flag = flag;
    }

    public int getFlagDrawableId() {
        return flag.getDrawableId();
    }

    public Flag getFlag() {
        return flag;
    }

    public boolean hasFlag() {
        return flag != Flag.NONE;
    }

    public String getEmailFlag() {
        return String.format("!%s", flag.toStorageId());
    }
}
