package com.sharry.librecyclerview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

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
       Constant associate with pull down refresh status.
    */
    private static final int REFRESH_STATUS_NORMAL = 795;
    private static final int REFRESH_STATUS_PULL_DOWN_REFRESH = 746;
    private static final int REFRESH_STATUS_LOOSEN_REFRESHING = 107;
    private static final int REFRESH_STATUS_REFRESHING = 272;

    /*
      Refresh Fields
     */
    private RefreshViewCreator mRefreshCreator;                                    // 下拉刷新视图的构造者
    private OnRefreshListener mRefreshListener;                                    // 处理刷新回调监听
    private View mRefreshView;                                                     // 下拉刷新的头部 View
    private int mCurrentRefreshStatus;                                             // 当前下拉刷新的状态
    private int mRefreshViewHeight = 0;                                            // 下拉刷新头部的高度

    /*
      拖拽相关成员变量
     */
    private int mDistanceY = 0;                                                   // 当前拖拽的距离
    private int mPrevDragDistance = 0;                                            // 未换手之前的已经拖拽的距离
    private float mDragIndex = 0.3f;                                              // 手指拖拽阻尼系数
    private float mDragContrastY = 0f;                                            // 拖拽的基准位置
    private float mLastMoveY = 0;                                                 // 记录上一次手指移动的距离
    private boolean mIsEdgeDragging = false;                                      // 是否正在进行边缘拖拽
    private boolean mIsPointChanged = false;                                      // 是否发生了手指切换

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

    /* ========================================== 下拉刷新相关 ==================================================*/

    /**
     * 设置下拉刷新视图的构建器
     */
    public void setRefreshViewCreator(RefreshViewCreator refreshCreator) {
        if (null == refreshCreator) {
            throw new NullPointerException("Please ensure parameter refreshCreator NonNull.");
        }
        mRefreshCreator = refreshCreator;
        // 添加头部的刷新 View
        View refreshView = mRefreshCreator.getRefreshView(getContext(), this);
        if (null == refreshView) {
            throw new NullPointerException("Please ensure " + refreshCreator + ".getRefreshView() return NonNull.");
        } else {
            mRefreshView = refreshView;
            addHeaderView(mRefreshView);
        }
    }

    /**
     * 设置下拉刷新已触发的回调
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        this.mRefreshListener = listener;
    }

    /**
     * 刷新完成
     *
     * @param result         刷新结果
     * @param disappearDelay 刷新完成后的消失时间(mm)
     */
    public void onRefreshComplete(CharSequence result, long disappearDelay) {
        if (mCurrentRefreshStatus == REFRESH_STATUS_REFRESHING) {
            if (null != mRefreshCreator) {
                mRefreshCreator.onComplete(mRefreshView, result);
            }
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    handleRefreshViewRestore();
                }
            }, disappearDelay);
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (null != mRefreshView && mRefreshViewHeight == 0) {
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
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mDragContrastY = ev.getRawY();
                mPrevDragDistance = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsPointChanged) {
                    // 记录切换焦点之前的距离
                    mPrevDragDistance = mDistanceY;
                    // 记录新的拖拽基准的位置
                    mDragContrastY = ev.getRawY();
                    mIsPointChanged = false;
                } else {
                    float curY = ev.getRawY();
                    // 未切换 Point 的情况下, 判断是否触发了双触点(此情况与焦点切换一样处理)
                    if (Math.abs(curY - mLastMoveY) >= ViewConfiguration.get(getContext())
                            .getScaledDoubleTapSlop()) {
                        mPrevDragDistance = mDistanceY;
                        mDragContrastY = curY;
                    }
                    mLastMoveY = ev.getRawY();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mIsPointChanged = true;
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                // 若不满足下拉刷新的条件则, 直接回调父类方法
                if (canScrollUp() || mCurrentRefreshStatus == REFRESH_STATUS_REFRESHING ||
                        null == mRefreshCreator || null == mRefreshView) {
                    return super.onTouchEvent(e);
                }
                // 下拉刷新的时候将 RecyclerView 锁定在第一个 item 的位置
                if (mIsEdgeDragging) {
                    scrollToPosition(0);
                }
                // 获取手指触摸拖拽的距离
                mDistanceY = mPrevDragDistance + (int) ((e.getRawY() - mDragContrastY) * mDragIndex);
                // 如果是已经到达头部，并且不断的向下拉，那么不断的改变 refreshView 的 marginTop 的值
                if (mDistanceY > 0) {
                    mIsEdgeDragging = true;
                    int marginTop = mDistanceY - mRefreshViewHeight;
                    setRefreshViewMarginTop(marginTop);
                    updateRefreshStatus(mDistanceY);
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                // 若没有手指在拖拽, 则进行释放
                if (mIsEdgeDragging) {
                    handleRefreshViewRestore();
                    mIsEdgeDragging = false;
                }
                break;
            }
            default:
                break;
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
            // 处于常态
            mCurrentRefreshStatus = REFRESH_STATUS_NORMAL;
        } else if (distanceY < mRefreshViewHeight) {
            // 继续拖拽才能触发下拉刷新
            mCurrentRefreshStatus = REFRESH_STATUS_PULL_DOWN_REFRESH;
        } else {
            // 松开即可刷新的状态
            mCurrentRefreshStatus = REFRESH_STATUS_LOOSEN_REFRESHING;
        }
        if (null != mRefreshCreator) {
            mRefreshCreator.onPulling(mRefreshView, distanceY, mRefreshViewHeight);
        }
    }

    /**
     * 处理下拉刷新 View 的回弹
     * 1. 手指松开后, RefreshView 的回弹: 回弹到刷新位置, 回弹到最终的隐藏位置
     * 2. 刷新完成后, RefreshView 回弹到最终的隐藏位置
     */
    private void handleRefreshViewRestore() {
        if (null == mRefreshView) {
            return;
        }
        if (REFRESH_STATUS_LOOSEN_REFRESHING == mCurrentRefreshStatus) {
            restoreToRefreshPos();
        } else {
            restoreToRefreshInitPos();
        }
    }

    /**
     * 回弹到刷新的位置
     */
    private void restoreToRefreshPos() {
        mCurrentRefreshStatus = REFRESH_STATUS_REFRESHING;
        if (null != mRefreshCreator) {
            mRefreshCreator.onRefreshing(mRefreshView);
        }
        if (null != mRefreshListener) {
            mRefreshListener.onRefresh();
        }
        // 回弹到刷新的位置
        int finalTopMargin = 0;
        int currentTopMargin = ((ViewGroup.MarginLayoutParams) mRefreshView.getLayoutParams()).topMargin;
        ValueAnimator animator = ObjectAnimator.ofInt(currentTopMargin, finalTopMargin)
                .setDuration(Math.abs(currentTopMargin - finalTopMargin));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setRefreshViewMarginTop((int) animation.getAnimatedValue());
            }
        });
        animator.start();
    }

    /**
     * 回弹到初始位置
     */
    private void restoreToRefreshInitPos() {
        // 回弹到初始位置
        int currentTopMargin = ((ViewGroup.MarginLayoutParams) mRefreshView.getLayoutParams()).topMargin;
        int finalTopMargin = 1 - mRefreshViewHeight;
        ValueAnimator animator = ObjectAnimator.ofInt(currentTopMargin, finalTopMargin)
                .setDuration(Math.abs(currentTopMargin - finalTopMargin));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setRefreshViewMarginTop((int) animation.getAnimatedValue());
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentRefreshStatus = REFRESH_STATUS_NORMAL;
            }
        });
        animator.start();
    }


    /**
     * 设置 RefreshView marginTop 的值
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
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    private boolean canScrollDown() {
        if (Build.VERSION.SDK_INT < 14) {
            return ViewCompat.canScrollVertically(this, 1) || this.getScrollY() < 0;
        } else {
            return ViewCompat.canScrollVertically(this, 1);
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up.
     */
    private boolean canScrollUp() {
        if (Build.VERSION.SDK_INT < 14) {
            return ViewCompat.canScrollVertically(this, -1) || this.getScrollY() > 0;
        } else {
            return ViewCompat.canScrollVertically(this, -1);
        }
    }

}
