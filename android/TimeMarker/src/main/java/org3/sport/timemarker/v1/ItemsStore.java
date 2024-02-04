package org3.sport.timemarker.v1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * @author ikh
 * @since 2/9/14
 */
public class ItemsStore extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 4;

    private static final String ITEMS_TABLE_NAME = "item";

    private static final String MARK_TIMESTAMP = "mark_timestamp";
    private static final String PRECISION = "precision";
    private static final String FLAG = "flag";

    private static final String CREATE_TABLE = "CREATE TABLE " + ITEMS_TABLE_NAME + " (" +
            MARK_TIMESTAMP + " BIGINT, " +
            FLAG + " TEXT, " +
            PRECISION + " BIGINT);";

    private static final String CLEAR_TABLE = "DELETE FROM " + ITEMS_TABLE_NAME;

    ItemsStore(Context context) {
        super(context, "timeMarker", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    public List<Marker> queryAll() {
        SQLiteDatabase database = getReadableDatabase();
        assert  database != null;

        String[] tableColumns = new String[] {
                MARK_TIMESTAMP, PRECISION, FLAG
        };
        Cursor c = database.query(ITEMS_TABLE_NAME, tableColumns,
                null /*whereClause*/, null/*whereArgs*/,
                null, null, MARK_TIMESTAMP);

        int mark_timestamp_idx = c.getColumnIndex(MARK_TIMESTAMP);
        int precision_idx = c.getColumnIndex(PRECISION);
        int flag_idx = c.getColumnIndex(FLAG);

        List<Marker> result = new LinkedList<Marker>();
        if (c.moveToFirst()) {
            do {
                long timestamp = c.getLong(mark_timestamp_idx);
                Marker marker = new Marker(timestamp, c.getLong(precision_idx));
                String flagName = c.getString(flag_idx);
                assert flagName != null;
                marker.setFlag(Flag.valueOf(flagName.toUpperCase()));
                result.add(marker);
            } while (c.moveToNext());

        }
        c.close();
        try {
            return result;
        } finally {
            database.close();
        }
    }

    public long insert(Marker marker) {
        SQLiteDatabase database = getWritableDatabase();
        assert database != null;

        ContentValues contentValues = new ContentValues();
        contentValues.put(MARK_TIMESTAMP, marker.getTimestamp());
        contentValues.put(PRECISION, marker.getPrecision());
        contentValues.put(FLAG, marker.getFlag().toStorageId());
        try {
            return database.insert(ITEMS_TABLE_NAME, null, contentValues);
        } finally {
            database.close();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(MainActivity.TIME_MARKER_TAG,
                "Upgrade data from version " + oldVersion + " to version " + newVersion);

        if (oldVersion != DATABASE_VERSION) {
            db.execSQL("ALTER TABLE " + ITEMS_TABLE_NAME +
                    " ADD COLUMN " + FLAG + " TEXT NOT NULL" +
                    " DEFAULT '" + Flag.NONE.toStorageId() + "'");
        }
    }

    public void clear() {
        SQLiteDatabase database = getWritableDatabase();
        assert database != null;
        database.execSQL(CLEAR_TABLE);
        database.close();
    }

    public void updateFlag(Marker marker, Flag flag) {
        Log.i(MainActivity.TIME_MARKER_TAG, "Update " + marker + " flag: " + flag);

        ContentValues args = new ContentValues();
        args.put(FLAG, flag.toStorageId());

        SQLiteDatabase database = getWritableDatabase();
        assert  database != null;
        try {
            database.update(ITEMS_TABLE_NAME, args,
                    MARK_TIMESTAMP + "=" + marker.getTimestamp(), null);
        } finally {
            database.close();
        }
    }
}
