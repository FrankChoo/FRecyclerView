package com.sharry.librecyclerview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 支持添加 Header/Footer/空视图的 RecyclerView, 对 RecyclerView 的增强
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2017/10/11.
 */
class WrapperRecyclerView extends RecyclerView {


    /*
       View caches.
     */
    private View mEmptyView;
    private List<View> mHeaderViews = new ArrayList<>();
    private List<View> mFooterViews = new ArrayList<>();
    /*
      Fields
     */
    private Adapter mOriginAdapter;                      // 被装饰的 Adapter
    private DecoratedAdapter mDecoratedAdapter;          // 装饰的 Adapter
    private boolean mIsAdjustSpanSize;

    public WrapperRecyclerView(Context context) {
        super(context);
    }

    public WrapperRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WrapperRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public Adapter getAdapter() {
        return mOriginAdapter;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        // 解决多次setAdapter的问题
        if (mDecoratedAdapter != null) {
            mDecoratedAdapter.unregisterAdapterDataObserver();
            mDecoratedAdapter = null;
        }
        mDecoratedAdapter = new DecoratedAdapter(adapter);
        // 解决GridLayout添加头部和底部也要占据一行
        mDecoratedAdapter.adjustSpanSize(this, mIsAdjustSpanSize);
        // 添加空数据展示的 View
        if (mEmptyView != null) {
            mDecoratedAdapter.addEmptyDataView(mEmptyView);
        }
        // 添加页眉
        for (View headerView : mHeaderViews) {
            mDecoratedAdapter.addHeaderView(headerView);
        }
        // 添加页脚
        for (View footerView : mFooterViews) {
            mDecoratedAdapter.addFooterView(footerView);
        }
        // 保存原先的 Adapter
        mOriginAdapter = adapter;
        super.setAdapter(mDecoratedAdapter);
    }

    /**
     * 添加页眉
     */
    public void addHeaderView(View headerView) {
        mHeaderViews.add(headerView);
        if (mDecoratedAdapter != null) {
            mDecoratedAdapter.addHeaderView(headerView);
        }
    }

    /**
     * 移除页眉
     */
    public void removeHeaderView(View headerView) {
        mHeaderViews.remove(headerView);
        if (mDecoratedAdapter != null) {
            mDecoratedAdapter.removeHeaderView(headerView);
        }
    }

    /**
     * 添加页脚
     */
    public void addFooterView(View footerView) {
        mFooterViews.add(footerView);
        if (mDecoratedAdapter != null) {
            mDecoratedAdapter.addFooterView(footerView);
        }
    }

    /**
     * 添加页脚
     */
    void addFooterView(int position, View footerView) {
        mFooterViews.add(position, footerView);
        if (mDecoratedAdapter != null) {
            mDecoratedAdapter.addFooterView(footerView);
        }
    }

    /**
     * 移除页脚
     */
    public void removeFooterView(View footerView) {
        mFooterViews.remove(footerView);
        if (mDecoratedAdapter != null) {
            mDecoratedAdapter.removeFooterView(footerView);
        }
    }

    /**
     * 设置页眉页脚是否占用 GridLayout 的一行
     */
    public void setAdjustGrideSpanSize(boolean isAdjust) {
        mIsAdjustSpanSize = isAdjust;
        if (mDecoratedAdapter != null) {
            mDecoratedAdapter.adjustSpanSize(this, mIsAdjustSpanSize);
        }
    }

    /**
     * 添加空数据展示 View
     */
    public void addEmptyDataView(View emptyView) {
        mEmptyView = emptyView;
        if (mDecoratedAdapter != null) {
            mDecoratedAdapter.addEmptyDataView(emptyView);
        }
    }

    /**
     * 获取空数据视图
     */
    public View getEmptyDataView() {
        return mEmptyView;
    }

    /**
     * 获取头部数量
     */
    public int getHeaderCount() {
        return mHeaderViews.size();
    }

    /**
     * 获取页尾数量
     */
    public int getFooterCount() {
        return mFooterViews.size();
    }
}