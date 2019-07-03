package com.sharry.librecyclerview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * 用于下拉刷新的 ViewCreator
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2017/10/11
 */
public interface RefreshViewCreator {

    RefreshViewCreator DEFAULT_REFRESH_VIEW_CREATOR = new RefreshViewCreator() {

        @Override
        public View getRefreshView(Context context, ViewGroup parent) {
            View view = new View(context);
            view.setLayoutParams(new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            return view;
        }

        @Override
        public void onPulling(View view, int currentDragHeight, int refreshViewHeight) {

        }

        @Override
        public void onRefreshing(View view) {

        }

        @Override
        public void onComplete(View view, CharSequence result) {

        }
    };

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
