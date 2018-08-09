package com.frank.librecyclerview;

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
 * Created by FrankChoo on 2017/10/11.
 * Email: frankchoochina@gmail.com
 * Description:
 * 支持上拉加载更多的 RecyclerView, 用户通过 addLoadViewCreator() 方法自定义上拉加载效果
 * 继承了 RefreshRecyclerView: 下拉刷新, 添加 Header 和 Footer 的功能
 */
public class LoadRefreshRecyclerView extends RefreshRecyclerView {

    public static int LOAD_STATUS_PULL_UP_LOADING = 0x00022;// 上拉加载更多状态
    public static int LOAD_STATUS_LOOSEN_LOADING = 0x00033;// 松开加载更多状态
    public int LOAD_STATUS_NORMAL = 0x00011;// 默认状态
    public int LOAD_STATUS_LOADING = 0x0044;// 正在加载更多状态
    // 当前的状态
    private int mCurrentLoadStatus;
    private LoadViewCreator mLoadCreator; // 上拉加载更多的辅助类
    private int mLoadViewHeight = 0;// 上拉加载更多头部的高度
    private View mLoadView;// 上拉加载更多的头部View
    private int mFingerDownY;// 手指按下的Y位置
    private boolean mCurrentDrag = false;// 当前是否正在拖动
    // 处理加载更多回调监听
    private OnLoadMoreListener mListener;

    public LoadRefreshRecyclerView(Context context) {
        super(context);
    }

    public LoadRefreshRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LoadRefreshRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 暴露给外界添加上拉加载View的Creator
     */
    public void addLoadViewCreator(LoadViewCreator loadCreator) {
        if (loadCreator == null) return;
        mLoadCreator = loadCreator;
        // 添加头部的刷新View
        View loadView = mLoadCreator.getLoadView(getContext(), this);
        if (loadView != null) {
            mLoadView = loadView;
            addFooterView(mLoadView);
        } else {
            throw new RuntimeException("上拉加载的 View 不能为 null");
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        // 为了防止RecyclerView的Measure机制导致我们最底部的LoadView不给予测量
        // 自行测量LoadView的高度
        if (mLoadView != null && mLoadViewHeight == 0) {
            // 这里要求LoadView的高度必须是精确值
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
        switch (ev.getAction()) {
            // 记录手指按下的位置 ,之所以写在dispatchTouchEvent那是因为如果我们处理了条目点击事件，
            // 那么就不会进入onTouchEvent里面，所以只能在这里获取
            case MotionEvent.ACTION_DOWN:
                mFingerDownY = (int) ev.getRawY();
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                // 如果是在最底部才处理，否则不需要处理
                if (canScrollDown() || mCurrentLoadStatus == LOAD_STATUS_LOADING
                        || mLoadCreator == null || mLoadView == null) {
                    // 如果没有到达最顶端，也就是说还可以向上滚动就什么都不处理
                    return super.onTouchEvent(e);
                }
                // 解决上拉加载更多自动滚动问题, 上拉加载的时候将RecyclerView锁定在最后一行
                if (mCurrentDrag) {
                    scrollToPosition(getAdapter().getItemCount() - 1);
                }
                // 获取手指触摸拖拽的距离
                int distanceY = (int) ((e.getRawY() - mFingerDownY) * mDragIndex);
                // 如果是已经到达底部，并且不断的向上拉，那么不断的改变loadView的marginBottom的值
                if (distanceY < 0) {
                    int marginBottom = -distanceY - mLoadViewHeight;
                    setLoadViewMarginBottom(marginBottom);
                    updateLoadStatus(-distanceY);
                    mCurrentDrag = true;
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (mCurrentDrag) {
                    restoreLoadView();
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
                mListener.onLoad();
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
        mCurrentDrag = false;
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
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     * 判断是不是滚动到了最顶部，这个是从SwipeRefreshLayout里面copy过来的源代码
     */
    public boolean canScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            return ViewCompat.canScrollVertically(this, 1) || this.getScrollY() < 0;
        } else {
            return ViewCompat.canScrollVertically(this, 1);
        }
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

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.mListener = listener;
    }

    public interface OnLoadMoreListener {
        void onLoad();
    }
}
