package com.leecx.photocall.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.leecx.photocall.domain.People;

import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private DBHelper helper;
    private SQLiteDatabase db;

    public DBManager(Context context) {
        helper = new DBHelper(context);
        db = helper.getWritableDatabase();
    }

    public void add(List<People> persons) {
        db.beginTransaction();
        try {
            for (People p : persons) {
                db.execSQL("INSERT INTO people VALUES(null,?,?,?,?,?)",
                        new Object[]{p.getName(), p.getPhoneNum(), p.getPhotoPath(), p.getRawContactId(), p.getOrderNo()});
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public void update(People p) {
        ContentValues cv = new ContentValues();
        cv.put("name", p.getName());
        cv.put("phoneNum", p.getPhoneNum());
        cv.put("photoPath", p.getPhotoPath());
        cv.put("orderNo", p.getOrderNo());
        db.update("people", cv, "id=?", new String[]{String.valueOf(p.getId())});
    }

    public void deleteById(String id) {
        db.delete("people", "id=?", new String[]{id});
    }

    public List<People> findAllContacts() {
        ArrayList<People> peopleArrayList = new ArrayList<People>();
        Cursor c = db.rawQuery("SELECT * FROM people order by orderNo asc", null);
        while (c.moveToNext()) {
            People people = new People();
            people.setId(c.getInt(c.getColumnIndex("id")));
            people.setName(c.getString(c.getColumnIndex("name")));
            people.setPhoneNum(c.getString(c.getColumnIndex("phoneNum")));
            people.setPhotoPath(c.getString(c.getColumnIndex("photoPath")));
            people.setRawContactId(c.getLong(c.getColumnIndex("rawContactId")));
            people.setOrderNo(c.getInt(c.getColumnIndex("orderNo")));
            peopleArrayList.add(people);
        }
        c.close();
        return peopleArrayList;
    }

    public Integer getNextOrderNo() {
        Integer order = 0;
        Cursor c = db.rawQuery("SELECT max(orderNo) FROM people ", null);
        while (c.moveToNext()) {

            order = c.getInt(0);
        }
        c.close();
        return order + 1;
    }

    //    public void dropTable(){
    //        db.execSQL("drop table person");
    //    }

    public void closeDB() {
        db.close();
    }


}
