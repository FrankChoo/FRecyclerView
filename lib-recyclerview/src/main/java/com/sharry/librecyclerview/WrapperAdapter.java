package com.sharry.librecyclerview;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 装饰的 Adapter, 封装了 Header 和 Footer 的 Adapter, 对 RecyclerView.Adapter 的增强
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2018/8/23 9:32
 */
class WrapperAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /*
      Constants.
     */
    private static final String TAG = WrapperAdapter.class.getSimpleName();
    private static final int KEY_HEADER_START = 1000;
    private static final int KEY_FOOTER_START = 2000;
    /*
      View caches.
     */
    private SparseArray<View> mHeaderViews;
    private SparseArray<View> mFooterViews;
    private View mEmptyDataView;
    /*
      Keys associated with header and footer.
     */
    private int mCurKeyHeader = KEY_HEADER_START;
    private int mCurKeyFooter = KEY_FOOTER_START;
    /*
      原始的 Adapter
     */
    private RecyclerView.Adapter mOriginAdapter;
    /*
      数据变更的监听器
     */
    private RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            if (null == mOriginAdapter) {
                return;
            }
            if (!(mOriginAdapter instanceof WrapperAdapter)) {
                notifyDataSetChanged();
            }
            onItemDataChangedInternal();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (null == mOriginAdapter) {
                return;
            }
            if (!(mOriginAdapter instanceof WrapperAdapter)) {
                notifyItemRemoved(positionStart + mHeaderViews.size());
            }
            onItemDataChangedInternal();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (null == mOriginAdapter) {
                return;
            }
            if (!(mOriginAdapter instanceof WrapperAdapter)) {
                notifyItemMoved(fromPosition + mHeaderViews.size(), toPosition + mHeaderViews.size());
            }
            onItemDataChangedInternal();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            if (null == mOriginAdapter) {
                return;
            }
            if (!(mOriginAdapter instanceof WrapperAdapter)) {
                notifyItemChanged(positionStart + mHeaderViews.size());
            }
            onItemDataChangedInternal();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            if (null == mOriginAdapter) {
                return;
            }
            if (!(mOriginAdapter instanceof WrapperAdapter)) {
                notifyItemChanged(positionStart + mHeaderViews.size(), payload);
            }
            onItemDataChangedInternal();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (null == mOriginAdapter) {
                return;
            }
            if (!(mOriginAdapter instanceof WrapperAdapter)) {
                notifyItemInserted(positionStart + mHeaderViews.size());
            }
            onItemDataChangedInternal();
        }
    };

    WrapperAdapter(RecyclerView.Adapter adapter) {
        mHeaderViews = new SparseArray<>();
        mFooterViews = new SparseArray<>();
        mOriginAdapter = adapter;
        // 注册 mAdapter 状态变化的监听器, 统一我们自定义的 Observer 去实现
        mOriginAdapter.registerAdapterDataObserver(mDataObserver);
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderPosition(position)) {
            // 直接返回position位置的所对应的key值, 充当viewType
            return mHeaderViews.keyAt(position);
        }
        if (isFooterPosition(position)) {
            // 直接返回position位置的所对应的key值, 充当viewType
            position = position - mHeaderViews.size() - mOriginAdapter.getItemCount();
            return mFooterViews.keyAt(position);
        }
        // 返回列表Adapter的getItemViewType
        position = position - mHeaderViews.size();
        return mOriginAdapter.getItemViewType(position);
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
            return mOriginAdapter.onCreateViewHolder(parent, viewType);
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
        mOriginAdapter.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        // 条数三者相加 = 底部条数 + 头部条数 + Adapter的条数
        return mOriginAdapter.getItemCount() + mHeaderViews.size() + mFooterViews.size();
    }

    /**
     * 添加头部
     */
    void addHeaderView(View view) {
        int index = mHeaderViews.indexOfValue(view);
        // 判断该 HeaderView 是否已经被添加过
        if (index == -1) {
            mHeaderViews.put(mCurKeyHeader++, view);
        }
        notifyDataSetChanged();
    }

    /**
     * 添加底部
     */
    void addFooterView(View view) {
        int index = mFooterViews.indexOfValue(view);
        // 判断该FooterView是否已经被添加过
        if (index == -1) {
            mFooterViews.put(mCurKeyFooter++, view);
        }
        notifyDataSetChanged();
    }

    /**
     * 移除头部
     */
    void removeHeaderView(View view) {
        int index = mHeaderViews.indexOfValue(view);
        if (index != -1) {
            mHeaderViews.removeAt(index);
            notifyDataSetChanged();
        }
    }

    /**
     * 移除底部
     */
    void removeFooterView(View view) {
        int index = mFooterViews.indexOfValue(view);
        if (index != -1) {
            mFooterViews.removeAt(index);
            notifyDataSetChanged();
        }
    }

    /**
     * 添加空数据视图
     * <p>
     * conditions:
     * 1. 添加数据为空时, RecyclerView 需要显示的 View
     * 2. 判断是否为空时不包含 Header 和 Footer
     */
    void addEmptyDataView(View emptyDataView) {
        // 1. 移除之前的空数据 View
        removeHeaderView(mEmptyDataView);
        // 2. 更新空数据 View
        mEmptyDataView = emptyDataView;
        onItemDataChangedInternal();
    }

    /**
     * 解决GridLayoutManager添加头部和底部不占用一行的问题
     */
    void adjustSpanSize(RecyclerView recycler, boolean adjust) {
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
     * 解注册监听器
     */
    void unregisterAdapterDataObserver() {
        try {
            unregisterAdapterDataObserver(mDataObserver);
        } catch (Exception e) {
            Log.e(TAG, "unregisterAdapterDataObserver failed.", e);
        }
    }

    /**
     * 是不是头部位置
     */
    private boolean isHeaderPosition(int position) {
        // 当前 Item 的 position 位置小于头部数量则说明是 Header
        return position < mHeaderViews.size();
    }

    /**
     * 是不是底部位置
     */
    private boolean isFooterPosition(int position) {
        return position >= (mHeaderViews.size() + mOriginAdapter.getItemCount());
    }

    /**
     * 获取用于建页眉和页脚的 ViewHolder
     */
    private RecyclerView.ViewHolder createHeaderFooterViewHolder(View view) {
        return new RecyclerView.ViewHolder(view) {

        };
    }

    /**
     * 内部通知数据变更了
     */
    private void onItemDataChangedInternal() {
        if (null == mEmptyDataView) {
            return;
        }
        // 获取原生 Adapter 的 Item 数量的 EmptyDataView 当做头部添加进去
        if (0 == mOriginAdapter.getItemCount()) {
            addHeaderView(mEmptyDataView);
        } else {
            removeHeaderView(mEmptyDataView);
        }
    }
}
