package com.example.myshoppinghq;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase; // leer
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.util.Log;

public class ShoppingMemoDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "shoppinglist_db";
    public static final int DB_VERSION = 1;

    public static final String TABLE_SHOPPING_LIST = "shopping_list";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PRODUCT = "product";
    public static final String COLUMN_QUANTITY = "quantity";
    public static final String COLUMN_CHECKED = "checked";

    public static final String SQL_CREATE = "CREATE TABLE " + TABLE_SHOPPING_LIST +
            "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_PRODUCT + " TEXT NOT NULL, " +
            COLUMN_QUANTITY + " INTEGER NOT NULL, " +
            COLUMN_CHECKED + " BOOLEAN NOT NULL DEFAULT 0);";

    public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_SHOPPING_LIST;


    private static final String TAG = ShoppingMemoDbHelper.class.getSimpleName();

    public ShoppingMemoDbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        Log.d(TAG, "DbHelper hat die Datenbank angelegt: " + getDatabaseName());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(SQL_CREATE);
            Log.d(TAG, "Tabelle wurde mit " + SQL_CREATE + " erstellt");
        } catch (RuntimeException e) {
            Log.e(TAG, "Fehler beim Anlegen der Tabelle: ", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL(SQL_DROP);
            onCreate(db);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fehler beim Upgrade der Datenbank", e);
        }
    }
}
