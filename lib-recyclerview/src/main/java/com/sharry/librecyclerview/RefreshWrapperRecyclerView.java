package com.sharry.librecyclerview;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

/**
 * 支持下拉刷新的View, 通过通过addRefreshViewCreator()去自定义下拉刷新效果
 * 支持侧滑删除与长按拖动
 * 继承了WrapRecyclerView: 添加Header和Footer的功能
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2017/10/11.
 */
class RefreshWrapperRecyclerView extends WrapRecyclerView {

    /*
      Constants
     */
    private static final int REFRESH_STATUS_NORMAL = 795;
    private static final int REFRESH_STATUS_PULL_DOWN_REFRESH = 746;
    private static final int REFRESH_STATUS_LOOSEN_REFRESHING = 107;
    private static final int REFRESH_STATUS_REFRESHING = 272;

    /*
      Fields
     */
    private RefreshViewCreator mRefreshCreator;   // 下拉刷新视图的构造者
    private View mRefreshView;                    // 下拉刷新的头部View
    private OnRefreshListener mListener;          // 处理刷新回调监听
    private int mCurrentRefreshStatus;            // 当前下拉刷新的状态
    protected float mDragIndex = 0.3f;            // 手指拖拽阻尼系数
    private int mRefreshViewHeight = 0;           // 下拉刷新头部的高度
    private int mFingerDownY;                     // 手指按下的Y位置
    private boolean mIsRefreshDragging = false;   // 是否正在进行下拉刷新的拖拽
    private int mCurrentTouchTarget = 0;          // 记录当前 RecyclerView 正在处理的触摸事件序列

    public RefreshWrapperRecyclerView(Context context) {
        super(context);
    }

    public RefreshWrapperRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RefreshWrapperRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public interface OnRefreshListener {
        void onRefresh();
    }

    /**
     * 添加头部的刷新View
     */
    public void addRefreshViewCreator(RefreshViewCreator refreshCreator) {
        if (refreshCreator == null) {
            return;
        }
        mRefreshCreator = refreshCreator;
        // 添加头部的刷新 View
        View refreshView = mRefreshCreator.getRefreshView(getContext(), this);
        if (refreshView != null) {
            mRefreshView = refreshView;
            addHeaderView(mRefreshView);
        } else {
            throw new RuntimeException("下拉刷新的 View 不能为 null.");
        }
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        // 按照常理 RefreshView 因为在顶部, 所以刚进入界面时一定会给予测量的
        // 但是这里与 LoadView 的写法保持一致, 自行测量 RefreshView 的高度
        if (mRefreshView != null && mRefreshViewHeight == 0) {
            // 这里要求 RefreshView 的高度必须是精确值
            ViewGroup.LayoutParams params = mRefreshView.getLayoutParams();
            int refreshViewHeightSpec;
            if (params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                refreshViewHeightSpec = View.MeasureSpec.makeMeasureSpec((1 << 30) - 1, View.MeasureSpec.AT_MOST);
            } else if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                refreshViewHeightSpec = View.MeasureSpec.makeMeasureSpec(params.height, View.MeasureSpec.EXACTLY);
            } else {
                throw new RuntimeException("RefreshView 的高度不能指定为 Match_Parent");
            }
            mRefreshView.measure(widthSpec, refreshViewHeightSpec);
            // 获取头部刷新 View 的高度
            mRefreshViewHeight = mRefreshView.getMeasuredHeight();
            if (mRefreshViewHeight > 0) {
                // 隐藏头部刷新的 View marginTop 多留出 1px 防止无法判断是不是滚动到头部问题
                setRefreshViewMarginTop(1 - mRefreshViewHeight);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 记录手指按下的位置, 之所以写在 dispatchTouchEvent 那是因为如果我们处理了条目点击事件，
                // 那么就不会进入 onTouchEvent 里面，所以只能在这里获取
                if (mCurrentTouchTarget++ == 0) {
                    mFingerDownY = (int) ev.getRawY();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                --mCurrentTouchTarget;
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                // 若不满足下拉刷新的条件则, 直接回调父类方法
                if (canScrollUp() || REFRESH_STATUS_REFRESHING == mCurrentRefreshStatus
                        || null == mRefreshCreator || null == mRefreshView) {
                    return super.onTouchEvent(e);
                }
                // 下拉刷新的时候将 RecyclerView 锁定在第一个 item 的位置
                if (mIsRefreshDragging) {
                    scrollToPosition(0);
                }
                // 获取手指触摸拖拽的距离
                int distanceY = (int) ((e.getRawY() - mFingerDownY) * mDragIndex);
                // 如果是已经到达头部，并且不断的向下拉，那么不断的改变 refreshView 的 marginTop 的值
                if (distanceY > 0) {
                    mIsRefreshDragging = true;
                    int marginTop = distanceY - mRefreshViewHeight;
                    setRefreshViewMarginTop(marginTop);
                    updateRefreshStatus(distanceY);
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                // 若没有手指在拖拽, 则进行释放
                if (mIsRefreshDragging) {
                    restoreRefreshView();
                    mIsRefreshDragging = false;
                }
                break;
            }
        }
        return super.onTouchEvent(e);
    }

    /**
     * 更新下拉刷新的状态
     *
     * @param distanceY 下拉刷新View被向下拖拽的距离
     */
    private void updateRefreshStatus(int distanceY) {
        if (distanceY <= 0) {
            mCurrentRefreshStatus = REFRESH_STATUS_NORMAL;
        } else if (distanceY < mRefreshViewHeight) {
            mCurrentRefreshStatus = REFRESH_STATUS_PULL_DOWN_REFRESH;
        } else {
            mCurrentRefreshStatus = REFRESH_STATUS_LOOSEN_REFRESHING;
        }
        if (null != mRefreshCreator) {
            mRefreshCreator.onPulling(mRefreshView, distanceY, mRefreshViewHeight);
        }
    }

    /**
     * 重置当前RefreshView的状态
     * 1. 手指松开后, RefreshView的回弹: 回弹到刷新位置, 回弹到最终的隐藏位置
     * 2. 刷新完成后, RefreshView回弹到最终的隐藏位置
     */
    private void restoreRefreshView() {
        if (mRefreshView == null) {
            return;
        }
        // 指定的最终位置(默认为隐藏的位置)
        int finalTopMargin = 1 - mRefreshViewHeight;
        // 当前的位置
        int currentTopMargin = ((ViewGroup.MarginLayoutParams) mRefreshView.getLayoutParams()).topMargin;
        // 判断是否满足刷新条件
        if (mCurrentRefreshStatus == REFRESH_STATUS_LOOSEN_REFRESHING) {
            finalTopMargin = 0;// 设置回弹到指定刷新的位置
            mCurrentRefreshStatus = REFRESH_STATUS_REFRESHING;
            if (mRefreshCreator != null) {
                mRefreshCreator.onRefreshing(mRefreshView);
            }
            if (mListener != null) {
                mListener.onRefresh();
            }
        }
        // 回弹到指定位置
        ValueAnimator animator = ObjectAnimator.ofFloat(currentTopMargin, finalTopMargin)
                .setDuration(Math.abs(currentTopMargin - finalTopMargin));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentTopMargin = (float) animation.getAnimatedValue();
                setRefreshViewMarginTop((int) currentTopMargin);
            }
        });
        animator.start();
    }

    /**
     * 设置刷新View的marginTop
     */
    private void setRefreshViewMarginTop(int marginTop) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mRefreshView.getLayoutParams();
        if (marginTop < -mRefreshViewHeight + 1) {
            marginTop = -mRefreshViewHeight + 1;
        }
        params.topMargin = marginTop;
        mRefreshView.setLayoutParams(params);
    }

    /**
     * 刷新完成
     *
     * @param result         刷新结果
     * @param disappearDelay 刷新完成后的消失时间(mm)
     */
    public void onRefreshComplete(CharSequence result, long disappearDelay) {
        if (mCurrentRefreshStatus == REFRESH_STATUS_REFRESHING) {
            mCurrentRefreshStatus = REFRESH_STATUS_NORMAL;
            if (mRefreshCreator != null) {
                mRefreshCreator.onComplete(mRefreshView, result);
            }
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    restoreRefreshView();
                }
            }, disappearDelay);
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     * 判断是不是滚动到了最顶部，这个是从 SwipeRefreshLayout 里面copy过来的源代码
     */
    private boolean canScrollUp() {
        return canScrollVertically(-1);/*
        if (android.os.Build.VERSION.SDK_INT < 14) {
            return ViewCompat.canScrollVertically(this, -1) || this.getScrollY() > 0;
        } else {
            return ViewCompat.canScrollVertically(this, -1);
        }*/
    }

}
