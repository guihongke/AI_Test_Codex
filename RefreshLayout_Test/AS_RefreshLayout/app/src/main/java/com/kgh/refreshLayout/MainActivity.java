package com.kgh.refreshLayout;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 首页只负责展示样式目录；每个条目进入独立 Activity，便于单独观察状态和动画。
        RecyclerView demoList = findViewById(R.id.demoList);
        demoList.setLayoutManager(new LinearLayoutManager(this));
        demoList.setAdapter(new DemoCatalogAdapter(
                Arrays.asList(
                        new DemoCatalogAdapter.Item(
                                R.string.demo_xml_title,
                                R.string.demo_xml_summary,
                                RefreshDemoActivity.DEMO_XML_PROGRESS
                        ),
                        new DemoCatalogAdapter.Item(
                                R.string.demo_classics_title,
                                R.string.demo_classics_summary,
                                RefreshDemoActivity.DEMO_CLASSICS
                        ),
                        new DemoCatalogAdapter.Item(
                                R.string.demo_material_title,
                                R.string.demo_material_summary,
                                RefreshDemoActivity.DEMO_MATERIAL
                        )
                ),
                this::openDemo
        ));
    }

    private void openDemo(int demoType) {
        // 通过类型参数复用一套数据逻辑，仅替换 Header/Footer 组件。
        Intent intent = new Intent(this, RefreshDemoActivity.class);
        intent.putExtra(RefreshDemoActivity.EXTRA_DEMO_TYPE, demoType);
        startActivity(intent);
    }
}
