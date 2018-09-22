package com.sharry.librecyclerview;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 封装好的 ViewHolder
 * 1. 设置常规 View 的一些属性
 * 2. 图片加载需要第三方自行实现
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2017/10/11
 */
public class SViewHolder extends RecyclerView.ViewHolder {

    private static final int INVALIDATE_VIEW_TYPE = -1;

    // 用来存放子 View 减少 findViewById 的次数
    private SparseArray<View> mViews = new SparseArray<>();
    // 用来存放设置了点击事件的 View 的 ID(防止多次 bindViewData, 创建多个点击事件实例)
    private List<Integer> mClickIds = new ArrayList<>();
    // 用来存放设置了长按事件的 View 的 ID(防止多次 bindViewData, 创建多个点击事件实例)
    private List<Integer> mLongClickIds = new ArrayList<>();

    // 获取当前的 View 类型
    private int mViewType = INVALIDATE_VIEW_TYPE;

    // 用于与 Adapter 之间进行交互
    private OnItemClickInteraction mClickInteraction;

    public SViewHolder(View itemView, int viewType, OnItemClickInteraction interaction) {
        super(itemView);
        bindItemViewListener(itemView);
        mViewType = viewType;
        mClickInteraction = interaction;
    }

    /**
     * 获取 ViewHolder 的类型
     */
    public int getViewType() {
        return mViewType;
    }

    /**
     * 通过 id 获取view
     */
    public <T extends View> T getView(int viewId) {
        // 先从缓存中找
        View view = mViews.get(viewId);
        if (view == null) {
            // 直接从ItemView中找
            view = itemView.findViewById(viewId);
            // 每调用一次getView方法则加入缓存Mao中
            mViews.put(viewId, view);
        }
        return (T) view;
    }

    /**
     * 设置 View 的 Visibility
     */
    public SViewHolder setViewVisibility(int viewId, int visibility) {
        getView(viewId).setVisibility(visibility);
        return this;
    }

    /**
     * 设置 TextView 文本
     */
    public SViewHolder setText(int viewId, CharSequence text) {
        TextView tv = getView(viewId);
        tv.setText(text);
        return this;
    }

    /**
     * 设置 ImageView 的资源文件
     */
    public SViewHolder setImageResource(int viewId, @DrawableRes int drawableRes) {
        ImageView imageView = getView(viewId);
        imageView.setImageResource(drawableRes);
        return this;
    }

    /**
     * 设置图片通过路径, 用户自行实现加载逻辑
     */
    public SViewHolder setImageUri(int viewId, String uri, HolderImageLoader imageLoader) {
        ImageView imageView = getView(viewId);
        if (imageLoader == null) {
            throw new NullPointerException("SViewHolder.setImageUri -> parameter imageLoader must not be null!");
        }
        imageLoader.displayImage(imageView.getContext(), uri, imageView);
        return this;
    }

    /**
     * ====================================== 配置点击事件 ==========================================
     * <p>
     * 添加子 View 点击事件
     */
    public SViewHolder addClickListener(int viewId) {
        if (!mClickIds.contains(viewId)) {
            mClickIds.add(viewId);
            getView(viewId).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickInteraction.onItemChildClick(v, getPositionWithoutHeader((ViewGroup)
                            itemView.getParent()));
                }
            });
        }
        return this;
    }

    /**
     * 添加子 View 长按事件
     */
    public SViewHolder addLongClickListener(int viewId) {
        if (!mLongClickIds.contains(viewId)) {
            mLongClickIds.add(viewId);
            getView(viewId).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return mClickInteraction.onItemChildLongClick(v, getPositionWithoutHeader((ViewGroup)
                            itemView.getParent()));
                }
            });
        }
        return this;
    }

    /**
     * 绑定 ItemView 的点击事件
     */
    private void bindItemViewListener(final View itemView) {
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickInteraction.onItemClick(v, getPositionWithoutHeader((ViewGroup)
                        itemView.getParent()));
            }
        });
        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return mClickInteraction.onItemLongClick(v, getPositionWithoutHeader((ViewGroup)
                        itemView.getParent()));
            }
        });
    }

    /**
     * 获取 holder 的 position
     * (Optimize: 有一定的侵入性)
     */
    private int getPositionWithoutHeader(ViewGroup parent) {
        int holderPosition = getAdapterPosition();
        if (parent != null && parent instanceof WrapRecyclerView) {
            holderPosition -= ((WrapRecyclerView) parent).getHeaderCount();
        }
        return holderPosition;
    }

    /**
     * 条目点击交互的接口
     */
    public interface OnItemClickInteraction {
        // 条目的点击
        void onItemClick(View v, int position);

        // 条目的长按
        boolean onItemLongClick(View v, int position);

        // 条目子元素的点击
        void onItemChildClick(View v, int position);

        // 条目子元素的长按
        boolean onItemChildLongClick(View v, int position);
    }

    /**
     * 用户加载图片
     */
    public interface HolderImageLoader {
        void displayImage(Context context, String uri, ImageView imageView);

    }
}