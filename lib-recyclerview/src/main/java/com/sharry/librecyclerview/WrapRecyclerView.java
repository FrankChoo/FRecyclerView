package com.sharry.librecyclerview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 1. 添加Header和Footer
 * 2. 添加空数据显示的View
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2017/10/11.
 */
class WrapRecyclerView extends RecyclerView {

    private Adapter mOriginAdapter;
    private WrapRecyclerAdapter mWrapAdapter;
    private View mEmptyView;
    private List<View> mHeaderViews = new ArrayList<>();
    private List<View> mFooterViews = new ArrayList<>();
    private boolean mIsAdjustSpanSize;

    public WrapRecyclerView(Context context) {
        super(context);
    }

    public WrapRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WrapRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public Adapter getAdapter() {
        return mOriginAdapter;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        // 解决多次setAdapter的问题
        if (mWrapAdapter != null) {
            mWrapAdapter.unregisterAdapterDataObserver();
            mWrapAdapter = null;
        }
        mWrapAdapter = new WrapRecyclerAdapter(adapter);
        // 解决GridLayout添加头部和底部也要占据一行
        mWrapAdapter.adjustSpanSize(this, mIsAdjustSpanSize);
        // 添加空数据展示的 View
        if (mEmptyView != null) {
            mWrapAdapter.addEmptyDataView(mEmptyView);
        }
        // 添加页眉
        for (View headerView : mHeaderViews) {
            mWrapAdapter.addHeaderView(headerView);
        }
        // 添加页脚
        for (View footerView : mFooterViews) {
            mWrapAdapter.addFooterView(footerView);
        }
        // 保存原先的 Adapter
        mOriginAdapter = adapter;
        super.setAdapter(mWrapAdapter);
    }

    /**
     * 添加页眉
     */
    public void addHeaderView(View headerView) {
        mHeaderViews.add(headerView);
        if (mWrapAdapter != null) {
            mWrapAdapter.addHeaderView(headerView);
        }
    }

    /**
     * 移除页眉
     */
    public void removeHeaderView(View headerView) {
        mHeaderViews.remove(headerView);
        if (mWrapAdapter != null) {
            mWrapAdapter.removeHeaderView(headerView);
        }
    }

    /**
     * 添加页脚
     */
    public void addFooterView(View footerView) {
        mFooterViews.add(footerView);
        if (mWrapAdapter != null) {
            mWrapAdapter.addFooterView(footerView);
        }
    }

    /**
     * 移除页脚
     */
    public void removeFooterView(View footerView) {
        mFooterViews.remove(footerView);
        if (mWrapAdapter != null) {
            mWrapAdapter.removeFooterView(footerView);
        }
    }

    /**
     * 设置页眉页脚是否占用 GridLayout 的一行
     */
    public void setAdjustGrideSpanSize(boolean isAdjust) {
        mIsAdjustSpanSize = isAdjust;
        if (mWrapAdapter != null) {
            mWrapAdapter.adjustSpanSize(this, mIsAdjustSpanSize);
        }
    }

    /**
     * 添加空数据展示 View
     */
    public void addEmptyDataView(View emptyView) {
        mEmptyView = emptyView;
        if (mWrapAdapter != null) {
            mWrapAdapter.addEmptyDataView(emptyView);
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