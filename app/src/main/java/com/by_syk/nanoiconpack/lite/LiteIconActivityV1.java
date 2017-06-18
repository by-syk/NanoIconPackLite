/*
 * Copyright 2017 By_syk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.by_syk.nanoiconpack.lite;

import android.app.Activity;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 图标包简单图标列表界面
 * v1.2
 *
 * Created by By_syk on 2017-06-13.
 */

public class LiteIconActivityV1 extends Activity {
    private RecyclerView recyclerView;

    private DisplayMetrics displayMetrics;

    private final int GRID_W_IN_DP = 72;
    private final int GRID_H_IN_DP = 64;
    private final int ICON_SIZE_IN_DP = 48;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();

        (new LoadIconTask()).execute();
    }

    private void init() {
        setTheme(android.R.style.Theme_DeviceDefault_Wallpaper);

        displayMetrics = getResources().getDisplayMetrics();

        recyclerView = new RecyclerView(this);
        ViewGroup.LayoutParams lpRv = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        recyclerView.setLayoutParams(lpRv);
        recyclerView.setLayoutManager(new GridLayoutManager(this, calculateGridNum()));

        setContentView(recyclerView);
    }

    private int calculateGridNum() {
        final int MIN_GRID_SIZE = (int) (GRID_W_IN_DP * displayMetrics.density);
        int totalWidth = displayMetrics.widthPixels;
        return totalWidth / MIN_GRID_SIZE;
    }

    private Set<String> getIcons() {
//        Set<String> iconSet = new TreeSet<>(); // 字母顺序
        Set<String> iconSet = new LinkedHashSet<>(); // 录入顺序
        XmlResourceParser parser = getResources().getXml(R.xml.drawable);
        try {
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    if (!"item".equals(parser.getName())) {
                        event = parser.next();
                        continue;
                    }
                    iconSet.add(parser.getAttributeValue(null, "drawable"));
                }
                event = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return iconSet;
    }

    private int getClickBg() {
        TypedArray typedArray = obtainStyledAttributes(new int[] {Build.VERSION.SDK_INT >= 21
                ? android.R.attr.selectableItemBackgroundBorderless
                : android.R.attr.selectableItemBackground
        });
        int resId = typedArray.getResourceId(0, 0);
        typedArray.recycle();
        return resId;
    }

    private class LoadIconTask extends AsyncTask<String, Integer, List<String>> {
        @Override
        protected List<String> doInBackground(String... params) {
            List<String> iconList = new ArrayList<>();
            iconList.addAll(getIcons());
            return iconList;
        }

        @Override
        protected void onPostExecute(List<String> list) {
            super.onPostExecute(list);

            IconAdapter adapter = new IconAdapter(list);
            recyclerView.setAdapter(adapter);
        }
    }

    private class IconAdapter extends RecyclerView.Adapter {
        private List<String> dataList;

        IconAdapter(List<String> dataList) {
            this.dataList = dataList;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            FrameLayout contentView = new FrameLayout(LiteIconActivityV1.this);
            int gridW = (int) (displayMetrics.density * GRID_W_IN_DP);
            int gridH = (int) (displayMetrics.density * GRID_H_IN_DP);
            ViewGroup.LayoutParams lpCv = new ViewGroup.LayoutParams(gridW, gridH);
            contentView.setLayoutParams(lpCv);

            ImageView ivIcon = new ImageView(LiteIconActivityV1.this);
            ivIcon.setTag("iv");
            ivIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ivIcon.setClickable(true);
            ivIcon.setBackgroundResource(getClickBg());
            int iconSize = (int) (displayMetrics.density * ICON_SIZE_IN_DP);
            FrameLayout.LayoutParams lpIv = new FrameLayout.LayoutParams(iconSize, iconSize);
            lpIv.gravity = Gravity.CENTER;
            ivIcon.setLayoutParams(lpIv);
            contentView.addView(ivIcon);

            return new IconHolder(contentView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            IconHolder iconHolder = (IconHolder) holder;
            int id = getResources().getIdentifier(dataList.get(position),
                    "drawable", getPackageName());

            iconHolder.ivIcon.setImageResource(id);
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return super.getItemViewType(position);
        }

        class IconHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;

            IconHolder(View itemView) {
                super(itemView);

                ivIcon = (ImageView) itemView.findViewWithTag("iv");
            }
        }
    }
}
