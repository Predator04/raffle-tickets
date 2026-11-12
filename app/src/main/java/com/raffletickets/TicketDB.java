package com.raffletickets;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class TicketDB extends SQLiteOpenHelper {

    public TicketDB(Context c) {
        super(c, "raffle_tickets.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tickets (number TEXT PRIMARY KEY, added_at INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS tickets");
        onCreate(db);
    }

    /** Returns 1 if added, 0 if the number already existed. */
    public int add(String number) {
        ContentValues cv = new ContentValues();
        cv.put("number", number);
        cv.put("added_at", System.currentTimeMillis());
        long r = getWritableDatabase().insertWithOnConflict(
                "tickets", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        return r == -1 ? 0 : 1;
    }

    public void delete(String number) {
        getWritableDatabase().delete("tickets", "number=?", new String[]{number});
    }

    public void deleteAll() {
        getWritableDatabase().delete("tickets", null, null);
    }

    public int count() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tickets", null);
        c.moveToFirst();
        int n = c.getInt(0);
        c.close();
        return n;
    }

    public List<String> getAll() {
        List<String> out = new ArrayList<>();
        Cursor c = getReadableDatabase().query("tickets",
                new String[]{"number"}, null, null, null, null, "number ASC");
        while (c.moveToNext()) out.add(c.getString(0));
        c.close();
        return out;
    }

    public List<String> findSuffix(String suffix) {
        List<String> out = new ArrayList<>();
        for (String t : getAll()) if (t.endsWith(suffix)) out.add(t);
        return out;
    }
}
