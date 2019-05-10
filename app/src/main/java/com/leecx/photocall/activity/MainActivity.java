package com.leecx.photocall.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import com.leecx.photocall.R;
import com.leecx.photocall.dao.DBManager;
import com.leecx.photocall.domain.People;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    ViewPager pager = null;

    DBManager db = null;
    Map<Integer, People> map = new HashMap<>();
    List<View> pages = new ArrayList<>();

    private boolean firstLauch = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE); // 注意顺序
        setContentView(R.layout.activity_main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,      // 注意顺序
                R.layout.title);


        Button btnAdd = (Button) findViewById(R.id.btnAdd);
        Button btnUpdate = (Button) findViewById(R.id.btnUpdate);


        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.photo_view, null);
        view.setBackgroundColor((getResources().getColor(R.color.colorTitleBar)));
//        LinearLayout linearLayout = view.findViewById(R.id.titleLayout);
//        linearLayout.setBackgroundResource(R.color.colorTitleBar);

//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                LayoutInflater inflater = getLayoutInflater();
//                Window window = getWindow();
//                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//                window.setStatusBarColor(getResources().getColor(R.color.colorPrimary));
//                //底部导航栏0
//                //window.setNavigationBarColor(activity.getResources().getColor(colorResId));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        pager = (ViewPager) findViewById(R.id.viewPager);
        db = new DBManager(this);


        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ContactsAddFormActivity.class);
                intent.putExtra("action", "add");
                startActivity(intent);
            }
        });


        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        });


        Button btnDelete = (Button) findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                People people = map.get(id);
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
        });

        showAllContacts();
    }

    public void deleteSysContacts(long rawContactId){
        //根据姓名求id
        ContentResolver resolver = this.getBaseContext().getContentResolver();

        Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
        resolver.delete(uri, "_id=?", new String[]{String.valueOf(rawContactId)});

        uri = Uri.parse("content://com.android.contacts/data");
        resolver.delete(uri, "raw_contact_id=?", new String[]{String.valueOf(rawContactId)});
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

        for (People c : peopleList) {
            map.put(c.getId(), c);

            Uri uri = Uri.parse("file://" + c.getPhotoPath());
            if (uri == null) {
                continue;
            }



            View view = inflater.inflate(R.layout.photo_view, null);
            view.setTag(c.getId());
            ImageView imageView = view.findViewById(R.id.imgViewMain);

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                imageView.setImageBitmap(bitmap);
                imageView.setTag(c.getId());
//                imageView.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Integer id  = getViewTag(v);
//                        if (id == null) {
//                            return;
//                        }
//
//                        People contacts = map.get(id);
//                        if (contacts == null) {
//                            return;
//                        }
//
//                        String phoneNum = contacts.getPhoneNum();
//                        callPhone(phoneNum);
//                    }
//                });
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
