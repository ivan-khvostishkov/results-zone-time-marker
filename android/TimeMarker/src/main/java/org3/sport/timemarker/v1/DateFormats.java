package org3.sport.timemarker.v1;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author ikh
 * @since 2/23/14
 */
final public class DateFormats {
    public static final SimpleDateFormat listDate =
            new SimpleDateFormat("dd.MM.yyyy", Locale.US);
    public static final SimpleDateFormat listTime =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    public static final SimpleDateFormat clockDow =
            new SimpleDateFormat("EEEE", Locale.US);
    public static final SimpleDateFormat clockMonthDate =
            new SimpleDateFormat("MMMM d", Locale.US);
    public static final SimpleDateFormat clockHhMm =
            new SimpleDateFormat("HH:mm", Locale.US);
    public static final SimpleDateFormat clockS =
            new SimpleDateFormat("ss", Locale.US);
    public static final SimpleDateFormat clockMs =
            new SimpleDateFormat("SSS", Locale.US);

    public static final SimpleDateFormat emailDateTime =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private DateFormats() {
    }
}
