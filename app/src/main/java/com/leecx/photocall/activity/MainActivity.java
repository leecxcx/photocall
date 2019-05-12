package com.leecx.photocall.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.leecx.photocall.R;
import com.leecx.photocall.dao.DBManager;
import com.leecx.photocall.domain.People;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private ViewPager pager = null;

    private DBManager db = null;
    private Map<Integer, People> map = new HashMap<>();
    private List<View> pages = new ArrayList<>();
    private boolean firstLauch = true;
    private Toolbar toolbar = null;
    private TabLayout mytab;

    private List<String> mTabTitles;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = (ViewPager) findViewById(R.id.viewPager);
        toolbar = (Toolbar) findViewById(R.id.mainTb);
        mytab = (TabLayout) findViewById(R.id.mytab);
        db = new DBManager(this);

        toolbar.inflateMenu(R.menu.main_toolbar_menu);

        showAllContacts();
        bingBtnEvent();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(getResources().getColor(R.color.colorTitleBar));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    private void toAddPeople() {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, ContactsAddFormActivity.class);
        intent.putExtra("action", "add");
        startActivity(intent);
    }

    private void toUpdatePeople() {
        if (pages.size() <= 0) {
            return;
        }
        int position = pager.getCurrentItem();
        View view = pages.get(position);

        Integer id = getViewTag(view);
        if (id == null) {
            return;
        }

        People people = map.get(id);

        Intent intent = new Intent();
        intent.setClass(MainActivity.this, ContactsAddFormActivity.class);
        intent.putExtra("action", "update");
        intent.putExtra("id", String.valueOf(people.getId()));
        intent.putExtra("name", people.getName());
        intent.putExtra("phoneNum", people.getPhoneNum());
        intent.putExtra("photoPath", people.getPhotoPath());
        intent.putExtra("rawContactId", people.getRawContactId());
        startActivity(intent);
    }

    private void toDeletePeople() {
        if (pages.size() <= 0) {
            return;
        }
        int position = pager.getCurrentItem();
        View view = pages.get(position);

        Integer id = getViewTag(view);
        if (id == null) {
            return;
        }

        final String idStr = String.valueOf(id);
        final People people = map.get(id);
        final long rawContactId = people.getRawContactId();

        AlertDialog confirmDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("是否删除")//标题
                .setIcon(R.mipmap.ic_launcher)//图标
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.deleteById(idStr);
                        deleteSysContacts(rawContactId);
                        restartActivity(MainActivity.this);
                        deletePhoto(people.getPhotoPath());
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                })
                .create();
        confirmDialog.show();
    }

    private void bingBtnEvent() {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_add:
                        toAddPeople();
                        break;
                    case R.id.action_update:
                        toUpdatePeople();
                        break;
                    case R.id.action_delete:
                        toDeletePeople();
                        break;
                }
                return true;
            }
        });
    }

    public void deleteSysContacts(long rawContactId) {
        //根据姓名求id
        ContentResolver resolver = this.getBaseContext().getContentResolver();

        Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
        resolver.delete(uri, "_id=?", new String[]{String.valueOf(rawContactId)});

        uri = Uri.parse("content://com.android.contacts/data");
        resolver.delete(uri, "raw_contact_id=?", new String[]{String.valueOf(rawContactId)});
    }

    private void deletePhoto(String filePath){
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                return;
            }
        }
    }

    public void onResume() {
        super.onResume();
        if (!firstLauch) {
            restartActivity(MainActivity.this);
        }
        firstLauch = false;
    }

    public static void restartActivity(Activity activity) {
        Intent intent = new Intent();
        intent.setClass(activity, activity.getClass());
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
        activity.finish();
    }


    private void showAllContacts() {
        List<People> peopleList = db.findAllContacts();
        if (peopleList == null || peopleList.size() <= 0) {
            return;
        }

        LayoutInflater inflater = getLayoutInflater();
        mTabTitles = new ArrayList<>();

        for (People c : peopleList) {
            map.put(c.getId(), c);
            mytab.addTab(mytab.newTab().setText(c.getName()));
            mTabTitles.add(c.getName());

            Uri uri = Uri.parse("file://" + c.getPhotoPath());
            if (uri == null) {
                continue;
            }


            View view = inflater.inflate(R.layout.photo_view, null);
            view.setTag(c.getId());
            ImageView imageView = view.findViewById(R.id.imgViewMain);
            TextView phoneText = view.findViewById(R.id.phoneText);
            phoneText.setText(c.getPhoneNum());

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                imageView.setImageBitmap(bitmap);
                imageView.setTag(c.getId());
                imageView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Integer id = getViewTag(v);
                        if (id == null) {
                            return false;
                        }

                        People people = map.get(id);
                        if (people == null) {
                            return false;
                        }

                        String phoneNum = people.getPhoneNum();
                        callPhone(phoneNum);
                        return true;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            pages.add(view);

        }

        ViewPager pager = (ViewPager) findViewById(R.id.viewPager);
        PagerAdapter adapter = new ViewAdapter(pages);
        pager.setAdapter(adapter);

        mytab.setupWithViewPager(pager);

        for (int i = 0; i < mTabTitles.size(); i++) {
            mytab.getTabAt(i).setText(mTabTitles.get(i));
        }
    }

    private Integer getViewTag(View v) {
        Object o = v.getTag();
        if (o == null) {
            return null;
        }

        Integer id = Integer.parseInt(v.getTag().toString());
        return id;
    }

    public void callPhone(String phoneNum) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse("tel:" + phoneNum);
        intent.setData(data);
        startActivity(intent);
    }


    class ViewAdapter extends PagerAdapter {
        private List<View> datas;


        public ViewAdapter(List<View> list) {
            datas = list;
        }

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return view == o;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = datas.get(position);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(datas.get(position));
        }
    }


}
