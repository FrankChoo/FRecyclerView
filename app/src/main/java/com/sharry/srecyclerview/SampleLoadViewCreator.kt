package com.sharry.srecyclerview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sharry.librecyclerview.LoadViewCreator

/**
 * 测试用的上拉加载的 Creator 的示例代码
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2018/10/7 12:49
 */
class SampleLoadViewCreator : LoadViewCreator {

    private lateinit var mTvItemLoad: TextView

    override fun getLoadView(context: Context?, parent: ViewGroup?): View {
        val loadView = LayoutInflater.from(context).inflate(R.layout.recycler_item_test_load, parent, false)
        mTvItemLoad = loadView.findViewById(R.id.tv_item_load)
        return loadView
    }

    override fun onPulling(view: View?, currentDragHeight: Int, loadViewHeight: Int) {
        mTvItemLoad.text = if (currentDragHeight < loadViewHeight) "上拉加载更多" else "松开加载更多"
    }

    override fun onLoading(view: View?) {
        mTvItemLoad.text = "正在拼命加载中..."
    }

    override fun onComplete(view: View?, result: CharSequence?) {
        result?.let { mTvItemLoad.text = it }
    }

}