package com.frank.lib_recyclerview;

import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by FrankChoo on 2017/10/11.
 * Email: frankchoochina@gmail.com
 * Description:
 * 1. 采用装饰设计模式
 * 2. 在构造函数中传入 Adapter 即可对用户自己创建的 Adapter 进行拓展
 */
class WrapRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = WrapRecyclerAdapter.class.getSimpleName();
    // 基本的头部类型开始位置, 充当 mHeaderViews 的key
    private int mKeyHeader = 1000;
    // 基本的底部类型开始位置, 充当 mFooterViews 的key
    private int mKeyFooter = 2000;

    // 相关的页眉/页脚/空视图
    private SparseArray<View> mHeaderViews;
    private SparseArray<View> mFooterViews;
    private View mEmptyDataView;

    // 原始的 Adapter
    private RecyclerView.Adapter mPrimitiveAdapter;

    // 创建代理观察者
    private RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            if (mPrimitiveAdapter == null) return;
            if (!(mPrimitiveAdapter instanceof WrapRecyclerAdapter)) {
                notifyDataSetChanged();
            }
            onItemDataChangedInternal();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (mPrimitiveAdapter == null) return;
            if (!(mPrimitiveAdapter instanceof WrapRecyclerAdapter)) {
                notifyItemRemoved(positionStart + mHeaderViews.size());
            }
            onItemDataChangedInternal();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (mPrimitiveAdapter == null) return;
            if (!(mPrimitiveAdapter instanceof WrapRecyclerAdapter)) {
                notifyItemMoved(fromPosition + mHeaderViews.size(), toPosition + mHeaderViews.size());
            }
            onItemDataChangedInternal();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            if (mPrimitiveAdapter == null) return;
            if (!(mPrimitiveAdapter instanceof WrapRecyclerAdapter)) {
                notifyItemChanged(positionStart + mHeaderViews.size());
            }
            onItemDataChangedInternal();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            if (mPrimitiveAdapter == null) return;
            if (!(mPrimitiveAdapter instanceof WrapRecyclerAdapter)) {
                notifyItemChanged(positionStart + mHeaderViews.size(), payload);
            }
            onItemDataChangedInternal();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (mPrimitiveAdapter == null) return;
            if (!(mPrimitiveAdapter instanceof WrapRecyclerAdapter)) {
                notifyItemInserted(positionStart + mHeaderViews.size());
            }
            onItemDataChangedInternal();
        }
    };


    public WrapRecyclerAdapter(RecyclerView.Adapter adapter) {
        mHeaderViews = new SparseArray<>();
        mFooterViews = new SparseArray<>();
        mPrimitiveAdapter = adapter;
        // 注册mAdapter状态变化的监听器, 统一由本类代理去实现
        mPrimitiveAdapter.registerAdapterDataObserver(mDataObserver);
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderPosition(position)) {
            // 直接返回position位置的所对应的key值, 充当viewType
            return mHeaderViews.keyAt(position);
        }
        if (isFooterPosition(position)) {
            // 直接返回position位置的所对应的key值, 充当viewType
            position = position - mHeaderViews.size() - mPrimitiveAdapter.getItemCount();
            return mFooterViews.keyAt(position);
        }
        // 返回列表Adapter的getItemViewType
        position = position - mHeaderViews.size();
        return mPrimitiveAdapter.getItemViewType(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 从getItemViewType中可知头部和底部返回的viewType就是Map的key
        if (mHeaderViews.indexOfKey(viewType) >= 0) {
            View headerView = mHeaderViews.get(viewType);
            return createHeaderFooterViewHolder(headerView);
        } else if (mFooterViews.indexOfKey(viewType) >= 0) {
            View footerView = mFooterViews.get(viewType);
            return createHeaderFooterViewHolder(footerView);
        } else {
            return mPrimitiveAdapter.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (isHeaderPosition(holder.getAdapterPosition())
                || isFooterPosition(holder.getAdapterPosition())) {
            return;
        }
        // 计算一下位置
        position = holder.getAdapterPosition() - mHeaderViews.size();
        mPrimitiveAdapter.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        // 条数三者相加 = 底部条数 + 头部条数 + Adapter的条数
        return mPrimitiveAdapter.getItemCount() + mHeaderViews.size() + mFooterViews.size();
    }

    /**
     * 添加头部
     */
    public void addHeaderView(View view) {
        int index = mHeaderViews.indexOfValue(view);
        // 判断该HeaderView是否已经被添加过
        if (index == -1) {
            mHeaderViews.put(mKeyHeader++, view);
        }
        notifyDataSetChanged();
    }

    /**
     * 添加底部
     */
    public void addFooterView(View view) {
        int index = mFooterViews.indexOfValue(view);
        // 判断该FooterView是否已经被添加过
        if (index == -1) {
            mFooterViews.put(mKeyFooter++, view);
        }
        notifyDataSetChanged();
    }

    /**
     * 移除头部
     */
    public void removeHeaderView(View view) {
        int index = mHeaderViews.indexOfValue(view);
        if (index == -1) return;
        mHeaderViews.removeAt(index);
        notifyDataSetChanged();
    }

    /**
     * 移除底部
     */
    public void removeFooterView(View view) {
        int index = mFooterViews.indexOfValue(view);
        if (index == -1) return;
        mFooterViews.removeAt(index);
        notifyDataSetChanged();
    }

    /**
     * 添加数据为空时, RecyclerView 需要显示的 View
     * 判断是否为空时不包含 Header 和 Footer
     */
    public void addEmptyDataView(View emptyDataView) {
        // 1. 移除之前的空数据View
        removeHeaderView(mEmptyDataView);
        // 2. 更新空数据View
        mEmptyDataView = emptyDataView;
        onItemDataChangedInternal();
    }

    /**
     * 是不是头部位置
     */
    private boolean isHeaderPosition(int position) {
        // 当前Item的position位置小于头部数量则说明是头部的Item
        return position < mHeaderViews.size();
    }

    /**
     * 是不是底部位置
     */
    private boolean isFooterPosition(int position) {
        return position >= (mHeaderViews.size() + mPrimitiveAdapter.getItemCount());
    }

    /**
     * 获取用于建页眉和页脚的 ViewHolder
     */
    private RecyclerView.ViewHolder createHeaderFooterViewHolder(View view) {
        return new RecyclerView.ViewHolder(view) {

        };
    }

    /**
     * 解决GridLayoutManager添加头部和底部不占用一行的问题
     */
    public void adjustSpanSize(RecyclerView recycler, boolean adjust) {
        if (adjust && recycler.getLayoutManager() instanceof GridLayoutManager) {
            final GridLayoutManager layoutManager = (GridLayoutManager) recycler.getLayoutManager();
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    boolean isHeaderOrFooter =
                            isHeaderPosition(position) || isFooterPosition(position);
                    return isHeaderOrFooter ? layoutManager.getSpanCount() : 1;
                }
            });
        }
    }

    /**
     * 内部通知数据变更了
     */
    private void onItemDataChangedInternal() {
        if (mEmptyDataView == null) return;
        // 获取原生 Adapter 的 Item 数量的 EmptyDataView 当做头部添加进去
        if (mPrimitiveAdapter.getItemCount() == 0) {
            addHeaderView(mEmptyDataView);
        } else {
            removeHeaderView(mEmptyDataView);
        }
    }

    /**
     * 解注册监听器
     */
    public void unregisterAdapterDataObserver() {
        try {
            unregisterAdapterDataObserver(mDataObserver);
        } catch (Exception e) {
            Log.e(TAG, "unregisterAdapterDataObserver failed.", e);
        }
    }

}
