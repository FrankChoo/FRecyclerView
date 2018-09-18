package com.sharry.srecyclerview

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.sharry.librecyclerview.RefreshViewCreator

/**
 * Created by Frank on 2018/8/3.
 * Email: SharryChooCHN@Gmail.com
 * Version: 1.0
 * Description:
 */
class TestRefreshView : RefreshViewCreator {

    private lateinit var mTvRefresh: TextView
    private lateinit var mIvRefresh: ImageView
    private var mAnimator: ObjectAnimator? = null

    override fun getRefreshView(context: Context?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item_test_refresh, parent, false)
        mTvRefresh = view.findViewById(R.id.tv_item_refresh)
        mIvRefresh = view.findViewById(R.id.iv_indicator)
        return view
    }

    override fun onPulling(view: View?, currentDragHeight: Int, refreshViewHeight: Int) {
        mTvRefresh.text = if (currentDragHeight < refreshViewHeight) "上拉加载更多" else "松开加载更多"
        mIvRefresh.rotation = currentDragHeight.toFloat()
    }

    override fun onRefreshing(view: View?) {
        startRefreshAnimator()
        mTvRefresh.text = "正在刷新中..."
    }

    override fun onComplete(view: View?, result: CharSequence?) {
        mAnimator?.cancel()
        result?.let { mTvRefresh.text = it }
    }

    private fun startRefreshAnimator() {
        if (mAnimator == null) {
            mAnimator = ObjectAnimator.ofFloat(mIvRefresh, "rotation", 0f, 360f)
            mAnimator!!.duration = 1000
            mAnimator!!.repeatCount = -1
        }
        mAnimator!!.start()
    }

}