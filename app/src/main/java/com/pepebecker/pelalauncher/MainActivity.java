package com.pepebecker.pelalauncher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ActionBar actionBar;
    ViewPager viewPager;
    ViewPagerAdapter viewPagerAdapter;
    GridView drawerGridView;
    BottomSheetBehavior bottomSheetBehavior;

    public AppObject appDrag = null;
    List<AppObject> installedAppList = new ArrayList<>();

    int cellHeight;
    int drawerPeakHeight = 100;
    int numRow = 0;
    int numColumn = 0;

    final String PREFS_NAME = "PelaPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        getPermissions();
        getData();

        final LinearLayout topDrawerLayout = findViewById(R.id.topDrawerLayout);
        topDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                drawerPeakHeight = topDrawerLayout.getHeight();
                initializeHome();
                initializeDrawer();
            }
        });

        ImageButton settingsButton = findViewById(R.id.settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startActivity(new Intent(getApplicationContext(), SettingsActivity.class)); }
        });
    }

    private void initializeHome() {
        ArrayList<PagerObject> pagerAppList = new ArrayList<>();

        for (int page = 0; page < 3; page++) {
            ArrayList<AppObject> appList = new ArrayList<>();
            for (int i = 0; i < numColumn * numRow; i++) {
                Drawable icon = getResources().getDrawable(R.drawable.ic_launcher_foreground);
                appList.add(new AppObject("", "", icon, false));
            }
            pagerAppList.add(new PagerObject(appList));
        }

        cellHeight = (getDisplayContentHeight() - drawerPeakHeight) / numRow ;

        viewPager = findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(this, pagerAppList, cellHeight, numColumn);
        viewPager.setAdapter(viewPagerAdapter);
    }

    private void initializeDrawer() {
        View mBottomSheet = findViewById(R.id.bottomSheet);
        drawerGridView = findViewById(R.id.drawerGrid);
        bottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(drawerPeakHeight);

        installedAppList = getInstalledAppList();

        drawerGridView.setAdapter(new AppAdapter(this, installedAppList, cellHeight));

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (appDrag != null) return;

                if (newState == BottomSheetBehavior.STATE_COLLAPSED && drawerGridView.getChildAt(0).getY() != 0)
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                if (newState == BottomSheetBehavior.STATE_DRAGGING && drawerGridView.getChildAt(0).getY() != 0)
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

    }

    public void itemPress(AppObject app) {
        if (appDrag != null && !app.getName().equals("")) {
            Toast.makeText(this,"Cell Already Occupied", Toast.LENGTH_SHORT).show();
            return;
        }
        if (appDrag != null && !app.getIsAppInDrawer()) {

            app.setPackageName(appDrag.getPackageName());
            app.setName(appDrag.getName());
            app.setImage(appDrag.getImage());
            app.setIsAppInDrawer(false);

            if (!appDrag.getIsAppInDrawer()) {
                appDrag.setPackageName("");
                appDrag.setName("");
                appDrag.setImage(getResources().getDrawable(R.drawable.ic_launcher_foreground));
                appDrag.setIsAppInDrawer(false);
            }
            appDrag = null;
            viewPagerAdapter.notifyGridChanged();
        } else {
            Intent launchAppIntent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(app.getPackageName());
            if (launchAppIntent != null) {
                getApplicationContext().startActivity(launchAppIntent);
            }
        }
    }

    public void itemLongPress(AppObject app){
        collapseDrawer();
        appDrag = app;
    }

    private void collapseDrawer() {
        drawerGridView.setY(drawerPeakHeight);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private List<AppObject> getInstalledAppList() {
        List<AppObject> list = new ArrayList<>();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> untreatedAppList = getApplicationContext().getPackageManager().queryIntentActivities(intent, 0);

        for (ResolveInfo untreatedApp : untreatedAppList){
            String appName = untreatedApp.activityInfo.loadLabel(getPackageManager()).toString();
            String appPackageName = untreatedApp.activityInfo.packageName;
            Drawable appImage = untreatedApp.activityInfo.loadIcon(getPackageManager());

            AppObject app = new AppObject(appPackageName, appName, appImage, true);
            if (!list.contains(app))
                list.add(app);
        }
        return list;
    }

    private int getDisplayContentHeight() {
        final WindowManager windowManager = getWindowManager();
        final Point size = new Point();
        int screenHeight;
        int actionBarHeight = 0;
        int statusBarHeight = 0;

        if (actionBar != null) {
            actionBarHeight = actionBar.getHeight();
        }

        int resourceId = getResources().getIdentifier(Keys.StatusBarHeight, Keys.Dimen, Keys.Android);
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        int contentTop = (findViewById(android.R.id.content)).getTop();
        windowManager.getDefaultDisplay().getSize(size);
        screenHeight = size.y;
        return screenHeight - contentTop - actionBarHeight - statusBarHeight;
    }

    private void getData(){
        ImageView homeScreenImage = findViewById(R.id.homeScreenImage);
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String imageUri = sharedPreferences.getString(Keys.ImageUri, null);
        int numRow = sharedPreferences.getInt(Keys.NumRow, 7);
        int numColumn = sharedPreferences.getInt(Keys.NumColumn, 5);

        if (this.numRow != numRow || this.numColumn != numColumn) {
            this.numRow = numRow;
            this.numColumn = numColumn;
            initializeHome();
        }

        if (imageUri != null) {
            homeScreenImage.setImageURI(Uri.parse(imageUri));
        }

    }

    private void getPermissions() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getData();
    }

}
