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
import android.content.Context;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 图标包简单图标列表界面
 *
 * v3.0
 * changelog
 * + 支持分类图标计数
 * + 支持点击图标提示名称
 *
 * Created by By_syk on 2017-06-14.
 */

public class LiteIconActivity extends Activity {
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
                List<String> subIconList = cate.listIcons();
                iconList.add(new Icon(cate.getName() + "..." + subIconList.size()));
                for (String iconName : subIconList) {
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
                String[] cateCount = {"default", ""};
                Log.d("test", icon.getCate());
                if (icon.getCate() != null) {
                    cateCount = icon.getCate().split("\\.\\.\\.");
                    cateCount[1] = "..." + cateCount[1];
                }
                cateHolder.tvCate.setText(cateCount[0]);
                cateHolder.tvCount.setText(cateCount[1]);
            } else {
                IconHolder iconHolder = (IconHolder) holder;
                iconHolder.ivIcon.setImageResource(icon.getId());

                final String ICON_NAME = icon.getName();
                iconHolder.ivIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ICON_NAME != null) {
                            GlobalToast.showToast(LiteIconActivity.this,
                                    ICON_NAME.replaceAll("_", " "));
                        }
                    }
                });
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
            LinearLayout contentView = new LinearLayout(LiteIconActivity.this);
            contentView.setOrientation(LinearLayout.HORIZONTAL);
            ViewGroup.LayoutParams lpCv = new ViewGroup.LayoutParams(ViewGroup
                    .LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            contentView.setLayoutParams(lpCv);

            TextView tvCate = new TextView(LiteIconActivity.this);
            tvCate.setTag("cate");
            tvCate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tvCate.setTypeface(Typeface.DEFAULT_BOLD);
            tvCate.setTextColor(Color.WHITE);
            tvCate.setSingleLine(true);
            int paddingH = (int) (displayMetrics.density * 16);
            int paddingVT = (int) (displayMetrics.density * 16);
            int paddingVB = (int) (displayMetrics.density * 8);
            tvCate.setPadding(paddingH, paddingVT, paddingH, paddingVB);
            LinearLayout.LayoutParams lpTvCate = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lpTvCate.weight = 1;
            tvCate.setLayoutParams(lpTvCate);
            contentView.addView(tvCate);

            TextView tvCount = new TextView(LiteIconActivity.this);
            tvCount.setTag("count");
            tvCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvCount.setPadding(0, 0, paddingH, paddingVB);
            LinearLayout.LayoutParams lpTvCount = new LinearLayout.LayoutParams(ViewGroup
                    .LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpTvCount.gravity = Gravity.BOTTOM;
            tvCount.setLayoutParams(lpTvCount);
            contentView.addView(tvCount);

            return contentView;
        }

        private View initIconView() {
            FrameLayout contentView = new FrameLayout(LiteIconActivity.this);
            int gridW = (int) (displayMetrics.density * GRID_W_IN_DP);
            int gridH = (int) (displayMetrics.density * GRID_H_IN_DP);
            ViewGroup.LayoutParams lpCv = new ViewGroup.LayoutParams(gridW, gridH);
            contentView.setLayoutParams(lpCv);

            ImageView ivIcon = new ImageView(LiteIconActivity.this);
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
            TextView tvCount;

            CateHolder(View itemView) {
                super(itemView);

                tvCate = (TextView) itemView.findViewWithTag("cate");
                tvCount = (TextView) itemView.findViewWithTag("count");
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

    private static class GlobalToast {
        private static Toast toast;

        public static void showToast(Context context, String msg) {
            if (context == null || msg == null) {
                return;
            }

            if (toast == null) { // Create Toast firstly.
                toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP | Gravity.RIGHT, 0, 0);
            } else {
                toast.setText(msg);
            }
            toast.show();
        }
    }
}
