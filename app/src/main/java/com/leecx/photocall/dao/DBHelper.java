package com.leecx.photocall.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "photocall.db";
    private static final int DATABASE_VERSION = 3;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS people(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name VARCHAR," +
                        "phoneNum VARCHAR, " +
                        "photoPath TEXT," +
                        "rawContactId TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
