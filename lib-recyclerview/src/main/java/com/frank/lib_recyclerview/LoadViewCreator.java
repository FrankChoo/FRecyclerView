package com.frank.lib_recyclerview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by FrankChoo on 2017/10/11.
 * Email: frankchoochina@gmail.com
 * Description: 用于上拉加载的 View
 */
public interface LoadViewCreator {

    /**
     * 获取上拉加载更多的View
     *
     * @param context 上下文
     * @param parent  RecyclerView
     */
    View getLoadView(Context context, ViewGroup parent);

    /**
     * 正在上拉
     *
     * @param currentDragHeight 当前拖动的高度
     * @param loadViewHeight    总的加载高度
     */
    void onPulling(View view, int currentDragHeight, int loadViewHeight);

    /**
     * 正在加载中
     */
    void onLoading(View view);

    /**
     * 停止加载
     *
     * @param result 加载的结果
     */
    void onComplete(View view, CharSequence result);

}
