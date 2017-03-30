package cn.bassy.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Scroller;

/**
 * 下拉刷新组件
 *
 * @author 韦天鹏
 * @version 1    2017-3-30
 */
public class BounceScrollView extends LinearLayout {

    private static final String TAG = "BounceScrollView";

    //内容视图
    private ScrollView mContentLayout;
    //反弹效果
    private Scroller mScroller;
    //记录触摸按下位置
    private float mTouchDownY;
    //下拉偏移量与展示头部布局高度之间的比率，控制手势偏移与View偏移的比例
    private final float mPullRatio = 2.5f;

    public BounceScrollView(Context context) {
        super(context);
        init(context, null);
    }

    public BounceScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BounceScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    //初始化
    private void init(Context context, AttributeSet attrs) {
        setClickable(true);//设置为可点击，否则无法滚动

        mScroller = new Scroller(context, new OvershootInterpolator());//带弹性效果
        mContentLayout = createContentLayout(context);
        addView(mContentLayout);
    }

    //创建滚动视图
    private ScrollView createContentLayout(Context context) {
        ScrollView sv = new ScrollView(context);
        LayoutParams params =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        sv.setLayoutParams(params);
        return sv;
    }

    @Override
    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        if (child == mContentLayout) {
            //内容视图交给父类处理
            super.addView(child, index, params);
        } else {
            //其它情况，交给ContentLayout处理
            mContentLayout.addView(child, index, params);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                //记录第一个手指的按下位置即可
                mTouchDownY = ev.getY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                float deltaY = ev.getY() - mTouchDownY;
                final int contentScrollY = mContentLayout.getScrollY();

                Log.i(TAG, "contentScrollY:" + contentScrollY);
                Log.i(TAG, "contentY:" + mContentLayout.getY());
                Log.i(TAG, "contentMaxScrollAmount:" + mContentLayout.getMaxScrollAmount());
                Log.i(TAG, "contentHeight:" + mContentLayout.getHeight());
                Log.i(TAG, "contentBottom:" + mContentLayout.getBottom());

                if (canPullDown() || canPullUp()) {
                    int newScrollY = (int) (-deltaY / mPullRatio);
                    mScroller.abortAnimation();
                    scrollTo(getScrollX(), newScrollY);
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                //回弹
                smoothScrollTo(getScrollX(), getTop());
                break;
            }
            default:
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * @return 返回当前是否可以下拉
     */
    private boolean canPullDown() {
        return mContentLayout.getScrollY() <= 0;
    }

    /**
     * @return 返回当前是否可以上拉
     */
    private boolean canPullUp() {
        View child = mContentLayout.getChildAt(0);
        //1、child为空
        //2、子View高度小于ScrollView的高度
        //3、ScrollView高度+滚动高度 >= 子View高度
        return (child == null) || (child.getHeight() <= mContentLayout.getHeight()) ||
                (mContentLayout.getHeight() + mContentLayout.getScrollY() >= child.getHeight());
    }

    /**
     * 平滑滚动到指定位置
     *
     * @param toX 目标水平位置
     * @param toY 目标垂直位置
     */
    private void smoothScrollTo(int toX, int toY) {

        mScroller.abortAnimation();
        mScroller.forceFinished(true);

        int fromX = getScrollX();
        int fromY = getScrollY();

        mScroller.startScroll(fromX, fromY, toX - fromX, toY - fromY, 300);
        invalidate();//要求重绘（即调用draw，该方法会调用computeScroll）
    }

    @Override
    public void computeScroll() {

        if (mScroller.computeScrollOffset()) {

            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();//要求重绘（即调用draw，该方法会调用computeScroll）

        } else if (mScroller.isFinished()) {
            // 动画结束
        }
    }
}
