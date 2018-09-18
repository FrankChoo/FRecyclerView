package com.sharry.srecyclerview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TestAdapter(this, arrayListOf("1", "2", "3",
                "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14"))

        // 添加下拉刷新
        recyclerView.addRefreshViewCreator(TestRefreshView())
        // 触发下拉刷新的回调
        recyclerView.setOnRefreshListener {
            recyclerView.postDelayed({
                recyclerView.onRefreshComplete("下拉刷新成功", 1000)
            }, 1000)
        }

        // 添加页眉
        val headerView = LayoutInflater.from(this).inflate(R.layout.recycler_item_test_header,
                recyclerView, false)
        headerView.setOnClickListener { recyclerView.removeFooterView(headerView) }
        recyclerView.addHeaderView(headerView)

        // 添加页脚
        val footerView = LayoutInflater.from(this).inflate(R.layout.recycler_item_test_footer,
                recyclerView, false)
        footerView.setOnClickListener { recyclerView.removeFooterView(footerView) }
        recyclerView.addFooterView(footerView)

        // 添加上拉加载
        recyclerView.addLoadViewCreator(TestLoadView())
        // 触发上拉加载的回调
        recyclerView.setOnLoadMoreListener {
            recyclerView.postDelayed({
                recyclerView.onLoadComplete("暂无更多数据", 1000)
            }, 1000)
        }
    }

}
