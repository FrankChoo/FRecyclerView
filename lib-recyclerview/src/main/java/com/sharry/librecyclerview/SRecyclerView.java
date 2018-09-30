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
 * 支持上拉加载更多的 RecyclerView, 对 RefreshWrapperRecyclerView 的增强
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2018/8/23 9:30
 */
public class SRecyclerView extends RefreshWrapperRecyclerView {

    /*
      Constant associated with pull up load view status.
     */
    private static final int LOAD_STATUS_NORMAL = 14;
    private static final int LOAD_STATUS_PULL_UP_LOADING = 47;
    private static final int LOAD_STATUS_LOOSEN_LOADING = 480;
    private static final int LOAD_STATUS_LOADING = 799;

    /*
      Fields associated with pull up load view.
     */
    private LoadViewCreator mLoadCreator = null;                                // 上拉加载更多的辅助类
    private OnLoadMoreListener mLoadListener = null;                            // 上拉加载更多的触发时的回调
    private View mLoadView = null;                                              // 上拉加载更多的头部View
    private int mCurrentLoadStatus = LOAD_STATUS_NORMAL;                        // 当前的状态
    private int mLoadViewHeight = 0;                                            // 上拉加载更多头部的高度
    /*
      拖拽相关成员变量
     */
    private int mDistanceY = 0;                                                   // 当前拖拽的距离
    private int mPrevDragDistance = 0;                                            // 未换手之前的已经拖拽的距离
    private float mDragContrastY = 0f;                                            // 拖拽的基准位置
    private float mLastMoveY = 0;                                                 // 记录上一次手指移动的距离
    private boolean mIsEdgeDragging = false;                                      // 是否正在进行边缘拖拽
    private boolean mIsPointChanged = false;                                      // 是否发生了手指切换

    public SRecyclerView(Context context) {
        super(context);
    }

    public SRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /* ========================================== 上拉加载更多相关 ==================================================*/

    /**
     * 上拉加载更多触发的监听器
     */
    public interface OnLoadMoreListener {
        void onLoadMore();
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

    /**
     * 设置触发上拉加载更多的回调
     */
    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.mLoadListener = listener;
    }

    /**
     * 通知上拉加载完成
     *
     * @param result         加载结果
     * @param disappearDelay 刷新完成后的消失时间(mm)
     */
    public void notifyLoadComplete(CharSequence result, long disappearDelay) {
        // 只有在加载状态时才执行停止加载
        if (mCurrentLoadStatus == LOAD_STATUS_LOADING) {
            if (mLoadCreator != null) {
                mLoadCreator.onComplete(mLoadView, result);
            }
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    handleLoadViewRestore();
                }
            }, disappearDelay);
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        // 为了防止 RecyclerView 的 Measure机制导致我们最底部的 LoadView 不给予测量
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
                // 如果是在最底部才处理，否则不需要处理
                if (canScrollDown() || LOAD_STATUS_LOADING == mCurrentLoadStatus ||
                        null == mLoadCreator || null == mLoadView) {
                    // 如果没有到达最顶端，也就是说还可以向上滚动就什么都不处理
                    return super.onTouchEvent(e);
                }
                // 解决上拉加载更多自动滚动问题, 上拉加载的时候将 RecyclerView 锁定在最后一行
                if (mIsEdgeDragging) {
                    // scrollToPosition(getAdapter().getItemCount() - 1);
                }
                // 获取手指触摸拖拽的距离
                mDistanceY = mPrevDragDistance + (int) ((e.getRawY() - mDragContrastY) * mDragCoefficient);
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
                    releaseArgs();
                }
                break;
            }
            default:
                break;
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
     * 处理上拉加载 View 的回弹
     */
    private void handleLoadViewRestore() {
        if (null == mLoadView) {
            return;
        }
        if (LOAD_STATUS_LOOSEN_LOADING == mCurrentLoadStatus) {
            restoreToLoadPos();
        } else {
            restoreToLoadInitPos();
        }
    }

    /**
     * 回弹到加载位置
     */
    private void restoreToLoadPos() {
        mCurrentLoadStatus = LOAD_STATUS_LOADING;
        if (mLoadCreator != null) {
            mLoadCreator.onLoading(mLoadView);
        }
        if (mLoadListener != null) {
            mLoadListener.onLoadMore();
        }
        // 回弹到指定位置
        int finalBottomMargin = 0;
        int currentBottomMargin = ((ViewGroup.MarginLayoutParams) mLoadView.getLayoutParams()).bottomMargin;
        ValueAnimator animator = ObjectAnimator.ofInt(currentBottomMargin, finalBottomMargin)
                .setDuration(Math.abs(currentBottomMargin - finalBottomMargin));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setLoadViewMarginBottom((int) animation.getAnimatedValue());
            }
        });
        animator.start();
    }

    /**
     * 回弹到初始位置
     */
    private void restoreToLoadInitPos() {
        // 回弹到指定位置
        int finalBottomMargin = 1 - mLoadViewHeight;
        int currentBottomMargin = ((ViewGroup.MarginLayoutParams) mLoadView.getLayoutParams()).bottomMargin;
        ValueAnimator animator = ObjectAnimator.ofInt(currentBottomMargin, finalBottomMargin)
                .setDuration(Math.abs(currentBottomMargin - finalBottomMargin));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setLoadViewMarginBottom((int) animation.getAnimatedValue());
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentLoadStatus = LOAD_STATUS_NORMAL;
            }
        });
        animator.start();
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
            if (mLoadListener != null) {
                mLoadListener.onLoadMore();
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
     * 设置 LoadView marginBottom 的值
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

    /**
     * 重置与刷新相关的参数
     */
    private void releaseArgs() {
        mDistanceY = 0;
        mPrevDragDistance = 0;
        mDragContrastY = 0f;
        mLastMoveY = 0;
        mIsEdgeDragging = false;
        mIsPointChanged = false;
    }
}
