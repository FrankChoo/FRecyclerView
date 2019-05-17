package com.sharry.librecyclerview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
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

    private static final String TAG = WrapperRecyclerView.class.getSimpleName();

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
    private WrapperAdapter mWrapperAdapter;          // 装饰的 Adapter
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
        // 解决多次 setAdapter 的问题
        if (mWrapperAdapter != null) {
            mWrapperAdapter.unregisterAdapterDataObserver();
            mWrapperAdapter = null;
        }
        mWrapperAdapter = new WrapperAdapter(adapter);
        // 解决 GridLayout 添加头部和底部也要占据一行
        mWrapperAdapter.adjustSpanSize(this, mIsAdjustSpanSize);
        // 添加页眉
        for (View headerView : mHeaderViews) {
            mWrapperAdapter.addHeaderView(headerView);
        }
        // 添加空数据展示的 View
        if (mEmptyView != null) {
            mWrapperAdapter.addEmptyDataView(mEmptyView);
        }
        // 添加页脚
        for (View footerView : mFooterViews) {
            mWrapperAdapter.addFooterView(footerView);
        }
        // 保存原先的 Adapter
        mOriginAdapter = adapter;
        super.setAdapter(mWrapperAdapter);
    }

    /**
     * 添加页眉
     */
    public void addHeaderView(@NonNull View headerView) {
        boolean isAdded = mHeaderViews.contains(headerView);
        if (!isAdded) {
            mHeaderViews.add(headerView);
            if (mWrapperAdapter != null) {
                mWrapperAdapter.addHeaderView(headerView);
            }
        } else {
            Log.i(TAG, "This header already added. View is " + headerView);
        }
    }

    /**
     * 移除页眉
     */
    public void removeHeaderView(@NonNull View headerView) {
        mHeaderViews.remove(headerView);
        if (mWrapperAdapter != null) {
            mWrapperAdapter.removeHeaderView(headerView);
        }
    }

    /**
     * 添加页脚
     */
    public void addFooterView(@NonNull View footerView) {
        boolean isAdded = mFooterViews.contains(footerView);
        if (!isAdded) {
            mFooterViews.add(footerView);
            if (mWrapperAdapter != null) {
                mWrapperAdapter.addFooterView(footerView);
            }
        } else {
            Log.i(TAG, "This footer already added. View is " + footerView);
        }
    }

    /**
     * 移除页脚
     */
    public void removeFooterView(@NonNull View footerView) {
        mFooterViews.remove(footerView);
        if (mWrapperAdapter != null) {
            mWrapperAdapter.removeFooterView(footerView);
        }
    }

    /**
     * 设置页眉页脚是否占用 GridLayout 的一行
     */
    public void setAdjustGrideSpanSize(boolean isAdjust) {
        mIsAdjustSpanSize = isAdjust;
        if (mWrapperAdapter != null) {
            mWrapperAdapter.adjustSpanSize(this, mIsAdjustSpanSize);
        }
    }

    /**
     * 添加空数据展示 View
     */
    public void addEmptyDataView(@NonNull View emptyView) {
        mEmptyView = emptyView;
        if (mWrapperAdapter != null) {
            mWrapperAdapter.addEmptyDataView(emptyView);
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