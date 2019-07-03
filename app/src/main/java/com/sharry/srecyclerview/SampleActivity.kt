package com.sharry.srecyclerview

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*

class SampleActivity : AppCompatActivity() {

    private val dataSet = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        initData()
    }

    private fun initView() {
        refreshScrollView.setTopEdgeElasticDraggable(true)
        refreshScrollView.setRefreshViewCreator(SampleRefreshViewCreator())
        refreshScrollView.setOnRefreshListener {
            refreshScrollView.postDelayed({
                refreshScrollView.notifyRefreshComplete("下拉刷新成功", 1000)
            }, 2000)
        }
        recyclerView.layoutManager = object : LinearLayoutManager(this) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        recyclerView.adapter = SampleAdapter(this, dataSet)

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

        // 设置是否开启底部边缘弹性拖拽
        recyclerView.setBottomEdgeElasticDraggable(true)
        // 添加上拉加载
        recyclerView.setLoadViewCreator(SampleLoadViewCreator())
        // 触发上拉加载的回调
        recyclerView.setOnLoadMoreListener {
            recyclerView.postDelayed({
                recyclerView.notifyLoadComplete("暂无更多数据", 1000)
            }, 1000)
        }
    }

    private fun initData() {
        for (index in 0 until 100) {
            dataSet.add(index.toString())
        }
        recyclerView.adapter?.notifyDataSetChanged()
    }

}
