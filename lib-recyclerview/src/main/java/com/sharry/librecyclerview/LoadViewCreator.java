package com.sharry.librecyclerview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * 用于上拉加载的 ViewCreator
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2017/10/11
 */
public interface LoadViewCreator {

    LoadViewCreator DEFAULT_LOAD_VIEW_CREATOR = new LoadViewCreator() {

        @Override
        public View getLoadView(Context context, ViewGroup parent) {
            View view = new View(context);
            view.setLayoutParams(new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            return view;
        }

        @Override
        public void onPulling(View view, int currentDragHeight, int refreshViewHeight) {

        }

        @Override
        public void onLoading(View view) {

        }

        @Override
        public void onComplete(View view, CharSequence result) {

        }
    };

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
