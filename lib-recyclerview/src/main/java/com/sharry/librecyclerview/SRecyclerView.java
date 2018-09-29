package com.sharry.librecyclerview;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

/**
 * 支持上拉加载更多的 RecyclerView, 用户通过 setLoadViewCreator() 方法自定义上拉加载效果
 * 继承了 RefreshRecyclerView: 下拉刷新, 添加 Header 和 Footer 的功能
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2018/8/23 9:30
 */
public class SRecyclerView extends RefreshWrapperRecyclerView {

    private static final int LOAD_STATUS_NORMAL = 14;
    private static final int LOAD_STATUS_PULL_UP_LOADING = 47;
    private static final int LOAD_STATUS_LOOSEN_LOADING = 480;
    private static final int LOAD_STATUS_LOADING = 799;

    private LoadViewCreator mLoadCreator = null;                                // 上拉加载更多的辅助类
    private OnLoadMoreListener mListener = null;                                // 上拉加载更多的触发时的回调
    private View mLoadView = null;                                              // 上拉加载更多的头部View
    private int mCurrentLoadStatus = LOAD_STATUS_NORMAL;                        // 当前的状态
    private int mLoadViewHeight = 0;                                            // 上拉加载更多头部的高度
    /*
      拖拽相关成员变量
     */
    private int mDistanceY = 0;                                                  // 当前拖拽的距离
    private int mPrevDragDistance = 0;                                           // 未换手之前的已经拖拽的距离
    private float mDragIndex = 0.3f;                                             // 手指拖拽阻尼系数
    private float mDragContrastY = 0f;                                           // 拖拽的基准位置
    private int mSwitchFingerCountInDragging = 0;                                // 记录手指切换次数
    private boolean mIsEdgeDragging = false;                                     // 是否正在进行边缘拖拽
    private boolean mIsSwitchOtherFinder = false;                                // 是否发生了手指切换

    public SRecyclerView(Context context) {
        super(context);
    }

    public SRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    /**
     * 设置触发上拉加载更多的回调
     */
    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.mListener = listener;
    }

    /**
     * 暴露给外界添加上拉加载View的Creator
     */
    public void setLoadViewCreator(LoadViewCreator loadCreator) {
        if (null == loadCreator) {
            throw new NullPointerException("Please ensure parameter loadCreator NonNull.");
        }
        mLoadCreator = loadCreator;
        // 添加头部的刷新View
        View loadView = mLoadCreator.getLoadView(getContext(), this);
        if (null == loadView) {
            throw new NullPointerException("Please ensure " + loadCreator + ".getLoadView() return NonNull.");
        } else {
            mLoadView = loadView;
            addFooterView(mLoadView);
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        // 为了防止 RecyclerView 的 Measure机制导致我们最底部的LoadView不给予测量
        // 自行测量 LoadView 的高度
        if (mLoadView != null && mLoadViewHeight == 0) {
            // 这里要求 LoadView 的高度必须是精确值
            ViewGroup.LayoutParams params = mLoadView.getLayoutParams();
            int loadViewHeightSpec;
            if (params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                loadViewHeightSpec = View.MeasureSpec.makeMeasureSpec((1 << 30) - 1, View.MeasureSpec.AT_MOST);
            } else if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                loadViewHeightSpec = View.MeasureSpec.makeMeasureSpec(params.height, View.MeasureSpec.EXACTLY);
            } else {
                throw new RuntimeException("LoadView 的高度不能指定为 Match_Parent");
            }
            mLoadView.measure(widthSpec, loadViewHeightSpec);
            // 测量完成之后获取测量高度
            mLoadViewHeight = mLoadView.getMeasuredHeight();
            if (mLoadViewHeight > 0) {
                setLoadViewMarginBottom(-mLoadViewHeight + 1);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // 记录手指按下的位置, 之所以写在 dispatchTouchEvent 那是因为如果我们处理了条目点击事件，
                // 那么就不会进入 onTouchEvent 里面，所以只能在这里获取
                mDragContrastY = ev.getRawY();
                mPrevDragDistance = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsSwitchOtherFinder) {
                    mPrevDragDistance = mDistanceY;
                    mDragContrastY = ev.getRawY();
                    mIsSwitchOtherFinder = false;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 当手指切换次数为奇数时, 此时这个触摸会获取 move 执行焦点
                if (mSwitchFingerCountInDragging % 2 == 1) {
                    mDragContrastY = ev.getRawY();
                    mPrevDragDistance = mDistanceY;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // 说明出现了中途切换手指的情况
                mSwitchFingerCountInDragging++;
                mIsSwitchOtherFinder = true;
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                // 如果是在最底部才处理，否则不需要处理
                if (canScrollDown() || LOAD_STATUS_LOADING == mCurrentLoadStatus ||
                        null == mLoadCreator || null == mLoadView) {
                    // 如果没有到达最顶端，也就是说还可以向上滚动就什么都不处理
                    return super.onTouchEvent(e);
                }
                // 解决上拉加载更多自动滚动问题, 上拉加载的时候将 RecyclerView 锁定在最后一行
                if (mIsEdgeDragging) {
                    scrollToPosition(getAdapter().getItemCount() - 1);
                }
                // 获取手指触摸拖拽的距离
                mDistanceY = mPrevDragDistance + (int) ((e.getRawY() - mDragContrastY) * mDragIndex);
                // 如果是已经到达底部，并且不断的向上拉，那么不断的改变loadView的marginBottom的值
                if (mDistanceY < 0) {
                    mIsEdgeDragging = true;
                    int marginBottom = -mDistanceY - mLoadViewHeight;
                    setLoadViewMarginBottom(marginBottom);
                    updateLoadStatus(-mDistanceY);
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (mIsEdgeDragging) {
                    restoreLoadView();
                    mDistanceY = 0;
                    mDragContrastY = 0;
                    mSwitchFingerCountInDragging = 0;
                    mIsEdgeDragging = false;
                }
                break;
            }
        }
        return super.onTouchEvent(e);
    }

    /**
     * 根据手指拖动的距离来更新加载的状态
     *
     * @param distanceY 上拉加载View被向上拉动的距离
     */
    private void updateLoadStatus(int distanceY) {
        if (distanceY <= 0) {
            mCurrentLoadStatus = LOAD_STATUS_NORMAL;
        } else if (distanceY < mLoadViewHeight) {
            mCurrentLoadStatus = LOAD_STATUS_PULL_UP_LOADING;
        } else {
            mCurrentLoadStatus = LOAD_STATUS_LOOSEN_LOADING;
        }
        if (mLoadCreator != null) {
            mLoadCreator.onPulling(mLoadView, distanceY, mLoadViewHeight);
        }
    }

    /**
     * 处理手指松开后, LoadView的回弹
     */
    private void restoreLoadView() {
        if (mLoadView == null) return;
        // 判断是否满足加载条件
        int currentBottomMargin = ((ViewGroup.MarginLayoutParams) mLoadView.getLayoutParams()).bottomMargin;
        int finalBottomMargin = -mLoadViewHeight + 1;
        if (mCurrentLoadStatus == LOAD_STATUS_LOOSEN_LOADING) {
            mCurrentLoadStatus = LOAD_STATUS_LOADING;
            finalBottomMargin = 0;
            if (mLoadCreator != null) {
                mLoadCreator.onLoading(mLoadView);
            }
            if (mListener != null) {
                mListener.onLoadMore();
            }
        }
        // 回弹到指定位置
        ValueAnimator animator = ObjectAnimator.ofFloat(currentBottomMargin, finalBottomMargin)
                .setDuration(Math.abs(currentBottomMargin - finalBottomMargin));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentBottomMargin = (float) animation.getAnimatedValue();
                setLoadViewMarginBottom((int) currentBottomMargin);
            }
        });
        animator.start();
    }

    /**
     * 设置加载View的marginBottom
     */
    private void setLoadViewMarginBottom(int marginBottom) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mLoadView.getLayoutParams();
        if (marginBottom < -mLoadViewHeight + 1) {
            marginBottom = -mLoadViewHeight + 1;
        }
        params.bottomMargin = marginBottom;
        mLoadView.setLayoutParams(params);
    }

    /**
     * 上拉加载完成
     *
     * @param result         加载结果
     * @param disappearDelay 刷新完成后的消失时间(mm)
     */
    public void onLoadComplete(CharSequence result, long disappearDelay) {
        // 只有在加载状态时才执行停止加载
        if (mCurrentLoadStatus == LOAD_STATUS_LOADING) {
            mCurrentLoadStatus = LOAD_STATUS_NORMAL;
            if (mLoadCreator != null) {
                mLoadCreator.onComplete(mLoadView, result);
            }
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    restoreLoadView();
                }
            }, disappearDelay);
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     * 判断是不是滚动到了最顶部，这个是从SwipeRefreshLayout里面copy过来的源代码
     */
    private boolean canScrollDown() {
        if (Build.VERSION.SDK_INT < 14) {
            return ViewCompat.canScrollVertically(this, 1) || this.getScrollY() < 0;
        } else {
            return ViewCompat.canScrollVertically(this, 1);
        }
    }

}
