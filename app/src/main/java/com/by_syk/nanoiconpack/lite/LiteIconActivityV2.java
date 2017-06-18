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
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 图标包简单图标列表界面
 *
 * v2.0
 * changelog
 * + 支持分类
 *
 * Created by By_syk on 2017-06-14.
 */

public class LiteIconActivityV2 extends Activity {
    private GridLayoutManager layoutManager;
    private IconAdapter adapter;

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

        adapter = new IconAdapter();

        layoutManager = new GridLayoutManager(this, calculateGridNum());
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isCate(position) ? layoutManager.getSpanCount() : 1;
            }
        });

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        ViewGroup.LayoutParams lpRv = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        recyclerView.setLayoutParams(lpRv);
        recyclerView.setBackgroundColor(0x80808080); // 保证图标视觉

        setContentView(recyclerView);
    }

    private int calculateGridNum() {
        final int MIN_GRID_SIZE = (int) (GRID_W_IN_DP * displayMetrics.density);
        int totalWidth = displayMetrics.widthPixels;
        return totalWidth / MIN_GRID_SIZE;
    }

    private List<Cate> getIcons() {
        List<Cate> dataList = new ArrayList<>();
        Cate defCate = new Cate(null);
        XmlResourceParser parser = getResources().getXml(R.xml.drawable);
        try {
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    switch (parser.getName()) {
                        case "category":
                            dataList.add(new Cate(parser.getAttributeValue(null, "title")));
                            break;
                        case "item":
                            String iconName = parser.getAttributeValue(null, "drawable");
                            if (dataList.isEmpty()) {
                                defCate.pushIcon(iconName);
                            } else {
                                dataList.get(dataList.size() - 1).pushIcon(iconName);
                            }
                            break;
                    }
                }
                event = parser.next();
            }
            if (!defCate.isEmpty()) {
                dataList.add(defCate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataList;
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

    private class LoadIconTask extends AsyncTask<String, Integer, List<Icon>> {
        @Override
        protected List<Icon> doInBackground(String... params) {
            List<Icon> iconList = new ArrayList<>();
            List<Cate> cateList = getIcons();
            for (Cate cate : cateList) {
                iconList.add(new Icon(cate.getName()));
                for (String iconName : cate.listIcons()) {
                    iconList.add(new Icon(iconName, cate.getName()));
                }
            }
            if (cateList.size() == 1 && cateList.get(0).getName() == null) { // 无任何分类
                iconList.remove(0);
            }
            return iconList;
        }

        @Override
        protected void onPostExecute(List<Icon> list) {
            super.onPostExecute(list);

            adapter.refresh(list);
        }
    }

    private class IconAdapter extends RecyclerView.Adapter {
        private List<Icon> dataList = new ArrayList<>();

        private static final int ITEM_TYPE_CATE = 0;
        private static final int ITEM_TYPE_ICON = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ITEM_TYPE_CATE) {
                return new CateHolder(initCateView());
            }
            return new IconHolder(initIconView());
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Icon icon = dataList.get(position);
            if (holder instanceof CateHolder) {
                CateHolder cateHolder = (CateHolder) holder;
                cateHolder.tvCate.setText(icon.getCate() != null ? icon.getCate() : "default");
            } else {
                IconHolder iconHolder = (IconHolder) holder;
                iconHolder.ivIcon.setImageResource(icon.getId());
            }
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return isCate(position) ? ITEM_TYPE_CATE : ITEM_TYPE_ICON;
        }

        private View initCateView() {
            TextView tvCate = new TextView(LiteIconActivityV2.this);
            tvCate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tvCate.setTypeface(Typeface.DEFAULT_BOLD);
            tvCate.setTextColor(Color.WHITE);
            tvCate.setSingleLine(true);
            int paddingH = (int) (displayMetrics.density * 16);
            tvCate.setPadding(paddingH, (int) (displayMetrics.density * 16),
                    paddingH, (int) (displayMetrics.density * 8));

            return tvCate;
        }

        private View initIconView() {
            FrameLayout contentView = new FrameLayout(LiteIconActivityV2.this);
            int gridW = (int) (displayMetrics.density * GRID_W_IN_DP);
            int gridH = (int) (displayMetrics.density * GRID_H_IN_DP);
            ViewGroup.LayoutParams lpCv = new ViewGroup.LayoutParams(gridW, gridH);
            contentView.setLayoutParams(lpCv);

            ImageView ivIcon = new ImageView(LiteIconActivityV2.this);
            ivIcon.setTag("iv");
            ivIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ivIcon.setClickable(true);
            ivIcon.setBackgroundResource(getClickBg());
            int iconSize = (int) (displayMetrics.density * ICON_SIZE_IN_DP);
            FrameLayout.LayoutParams lpIv = new FrameLayout.LayoutParams(iconSize, iconSize);
            lpIv.gravity = Gravity.CENTER;
            ivIcon.setLayoutParams(lpIv);
            contentView.addView(ivIcon);

            return contentView;
        }

        boolean isCate(int position) {
            return dataList.get(position).getName() == null;
        }

        void refresh(List<Icon> dataList) {
            this.dataList.clear();
            if (dataList != null) {
                this.dataList.addAll(dataList);
            }
            notifyDataSetChanged();
        }

        class CateHolder extends RecyclerView.ViewHolder {
            TextView tvCate;

            CateHolder(View itemView) {
                super(itemView);

                tvCate = (TextView) itemView;
            }
        }

        class IconHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;

            IconHolder(View itemView) {
                super(itemView);

                ivIcon = (ImageView) itemView.findViewWithTag("iv");
            }
        }
    }

    private class Cate {
        private String name; // 无名分类则置为 null
        private Set<String> iconSet;

        Cate(String name) {
            this.name = name;
            iconSet = new LinkedHashSet<>();
        }

        public String getName() {
            return name;
        }

        public void pushIcon(String iconName) {
            iconSet.add(iconName);
        }

        public boolean isEmpty() {
            return iconSet.isEmpty();
        }

        public List<String> listIcons() {
            List<String> list = new ArrayList<>(iconSet.size());
            list.addAll(iconSet);
            return list;
        }
    }

    private class Icon {
        // 0 为无效ID
        private int id;
        // 为 null 表示无效图标
        private String name;
        // 无所属分类则置为 null
        private String cate;

        Icon(String cate) {
            this.cate = cate;
        }

        Icon(String name, String cate) {
            this(cate);
            this.name = name;
            this.id = getResources().getIdentifier(name, "drawable", getPackageName());
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCate() {
            return cate;
        }
    }
}
