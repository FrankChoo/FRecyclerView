package com.sharry.librecyclerview;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

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

    protected float mDragIndex = 0.3f; // 手指拖拽的阻力指数
    // 当前刷新的状态
    private int mCurrentRefreshStatus;
    private int REFRESH_STATUS_NORMAL = 0x0011;// 默认状态
    private int REFRESH_STATUS_PULL_DOWN_REFRESH = 0x0022;// 下拉刷新状态
    private int REFRESH_STATUS_LOOSEN_REFRESHING = 0x0033;// 松开刷新状态
    private int REFRESH_STATUS_REFRESHING = 0x0044;// 正在刷新状态
    // 下拉刷新的辅助类
    private RefreshViewCreator mRefreshCreator;
    private int mRefreshViewHeight = 0; // 下拉刷新头部的高度
    private View mRefreshView; // 下拉刷新的头部View
    private int mFingerDownY;// 手指按下的Y位置
    private boolean mCurrentDrag = false; // 当前是否正在拖动
    // 处理刷新回调监听
    private OnRefreshListener mListener;

    public RefreshWrapperRecyclerView(Context context) {
        super(context);
    }

    public RefreshWrapperRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RefreshWrapperRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 添加头部的刷新View
     */
    public void addRefreshViewCreator(RefreshViewCreator refreshCreator) {
        if (refreshCreator == null) return;
        mRefreshCreator = refreshCreator;
        // 添加头部的刷新View
        View refreshView = mRefreshCreator.getRefreshView(getContext(), this);
        if (refreshView != null) {
            mRefreshView = refreshView;
            addHeaderView(mRefreshView);
        } else {
            throw new RuntimeException("下拉刷新的View不能为null");
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        // 按照常理RefreshView因为在顶部, 所以刚进入界面时一定会给予测量的
        // 但是这里与LoadView的写法保持一致,自行测量RefreshView的高度
        if (mRefreshView != null && mRefreshViewHeight == 0) {
            // 这里要求RefreshView的高度必须是精确值
            ViewGroup.LayoutParams params = mRefreshView.getLayoutParams();
            int refreshViewHeightSpec;
            if (params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                refreshViewHeightSpec = View.MeasureSpec.makeMeasureSpec((1 << 30) - 1, View.MeasureSpec.AT_MOST);
            } else if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                refreshViewHeightSpec = View.MeasureSpec.makeMeasureSpec(params.height, View.MeasureSpec.EXACTLY);
            } else {
                throw new RuntimeException("RefreshView的高度不能指定为Match_Parent");
            }
            mRefreshView.measure(widthSpec, refreshViewHeightSpec);
            // 获取头部刷新View的高度
            mRefreshViewHeight = mRefreshView.getMeasuredHeight();
            if (mRefreshViewHeight > 0) {
                // 隐藏头部刷新的View  marginTop  多留出1px防止无法判断是不是滚动到头部问题
                setRefreshViewMarginTop(-mRefreshViewHeight + 1);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 记录手指按下的位置 ,之所以写在dispatchTouchEvent那是因为如果我们处理了条目点击事件，
                // 那么就不会进入onTouchEvent里面，所以只能在这里获取
                mFingerDownY = (int) ev.getRawY();
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                // 如果是在最顶部才处理，否则不需要处理
                if (canScrollUp() || mCurrentRefreshStatus == REFRESH_STATUS_REFRESHING
                        || mRefreshView == null || mRefreshCreator == null) {
                    // 如果没有到达最顶端，也就是说还可以向上滚动就什么都不处理
                    return super.onTouchEvent(e);
                }
                // 解决下拉刷新自动滚动问题, 下拉刷新的时候将RecyclerView锁定在最后一行
                if (mCurrentDrag) {
                    scrollToPosition(0);
                }
                // 获取手指触摸拖拽的距离
                int distanceY = (int) ((e.getRawY() - mFingerDownY) * mDragIndex);
                // 如果是已经到达头部，并且不断的向下拉，那么不断的改变 refreshView 的 marginTop 的值
                if (distanceY > 0) {
                    int marginTop = distanceY - mRefreshViewHeight;
                    setRefreshViewMarginTop(marginTop);
                    updateRefreshStatus(distanceY);
                    mCurrentDrag = true;
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (mCurrentDrag) {
                    restoreRefreshView();
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

        if (mRefreshCreator != null) {
            mRefreshCreator.onPulling(mRefreshView, distanceY, mRefreshViewHeight);
        }
    }

    /**
     * 重置当前RefreshView的状态
     * 1. 手指松开后, RefreshView的回弹: 回弹到刷新位置, 回弹到最终的隐藏位置
     * 2. 刷新完成后, RefreshView回弹到最终的隐藏位置
     */
    private void restoreRefreshView() {
        if (mRefreshView == null) return;
        // 指定的最终位置(默认为隐藏的位置)
        int finalTopMargin = -mRefreshViewHeight + 1;
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
        mCurrentDrag = false;
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
     * 判断是不是滚动到了最顶部，这个是从SwipeRefreshLayout里面copy过来的源代码
     */
    private boolean canScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            return ViewCompat.canScrollVertically(this, -1) || this.getScrollY() > 0;
        } else {
            return ViewCompat.canScrollVertically(this, -1);
        }
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.mListener = listener;
    }

    public void setItemTouchHelperCallback() {

    }

    public interface OnRefreshListener {
        void onRefresh();
    }
}
