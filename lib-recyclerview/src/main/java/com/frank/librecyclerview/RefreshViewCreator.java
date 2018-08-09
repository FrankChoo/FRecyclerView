package com.frank.librecyclerview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by FrankChoo on 2017/10/11.
 * Email: frankchoochina@gmail.com
 * Description: 用于下拉刷新的 View
 */
public interface RefreshViewCreator {

    /**
     * 获取下拉刷新的View
     *
     * @param context 上下文
     * @param parent  RecyclerView
     */
    View getRefreshView(Context context, ViewGroup parent);

    /**
     * 正在下拉
     *
     * @param currentDragHeight 当前拖动的高度
     * @param refreshViewHeight 总的刷新高度
     */
    void onPulling(View view, int currentDragHeight, int refreshViewHeight);

    /**
     * 正在刷新中
     */
    void onRefreshing(View view);

    /**
     * 停止刷新
     *
     * @param result 刷新的结果
     */
    void onComplete(View view, CharSequence result);

}
