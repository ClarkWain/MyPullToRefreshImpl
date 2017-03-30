package cn.bassy.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;

import cn.bassy.demo.R;

/**
 * 下拉刷新组件
 *
 * @author 韦天鹏
 * @version 1    2016-7-12
 */
public class PullToRefreshScrollView extends LinearLayout {

    private static final String TAG = "PullToRefreshScrollView";

    /**
     * 表示动作类型
     */
    enum PullAction {
        NONE, UP, DOWN;

        public String toString() {
            if (this == NONE)
                return "无";
            else if (this == UP)
                return "上拉";
            else if (this == DOWN)
                return "下拉";
            else
                return "";
        }
    }

    /**
     * 表示下拉状态
     */
    enum PullState {
        NONE, PULL_DOWN_TO_REFRESH, RELEASE_TO_REFRESH, REFRESHING, COMPLETED,
        COMPLETED_IMMEDIATELY
    }

    private CharSequence mTextPullToRefresh = "下拉刷新";
    private CharSequence mTextRefreshing = "正在刷新";
    private CharSequence mTextReleaseToRefresh = "松开立即刷新";
    private CharSequence mTextCompleted = "刷新完成";

    private Drawable mArrowDrawable;
    private Drawable mProgressDrawable;
    private Drawable mCompletedDrawable;

    private PullState mPullDownState = PullState.NONE;
    PullState mPullUpState = PullState.NONE;

    //下拉刷新的头部布局
    private LinearLayout mHeaderLayout;
    //这是头部布局里面的一个View
    private View mHeaderView;
    //显示头部箭头和进度图片
    private ImageView mHeaderIconView;
    //显示头部文本
    private TextView mHeaderTextView;
    //头部布局的高度，单位是DIP
    private int mHeaderViewHeight = 50;

    //这个是内容布局
    private ScrollView mContentLayout;

    //记录触摸按下位置
    private float mTouchDownY;
    //记录当前内容布局的滚动位置
    private int mContentStartScrollY;
    //记录当前整体布局的滚动位置
    private int mRootStartScrollY;

    //下拉偏移量与展示头部布局高度之间的比率
    private final float mPullRatio = 2.5f;

    //反弹效果
    private Scroller mScroller;
    //下拉图标动画
    private Animation mPullDownAnimation;
    //进度条动画
    private Animation mProgressAnimation;

    //下拉刷新接口
    private OnRefreshListener mOnRefreshListener;

    public PullToRefreshScrollView(Context context) {
        super(context);
        init(context, null);
    }

    public PullToRefreshScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PullToRefreshScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    //初始化
    private void init(Context context, AttributeSet attrs) {
        setClickable(true);//设置为可点击，否则无法滚动

        LayoutInflater mLayoutInflater = LayoutInflater.from(context);
        mHeaderLayout = createHeaderLayout(context);
        mHeaderView = mLayoutInflater.inflate(R.layout.header_view, this, false);
        mHeaderIconView = (ImageView) mHeaderView.findViewById(R.id.header_imageview);
        mHeaderTextView = (TextView) mHeaderView.findViewById(R.id.hreader_tv_msg);

        mHeaderViewHeight *= getResources().getDisplayMetrics().density;
        mHeaderView.getLayoutParams().width = LayoutParams.MATCH_PARENT;
        mHeaderView.getLayoutParams().height = mHeaderViewHeight;

        mHeaderLayout.addView(mHeaderView);
        this.setOrientation(VERTICAL);
        this.addView(mHeaderLayout);

        mContentLayout = createContentLayout(context);
        this.addView(mContentLayout);

        initAttributes(context, attrs);

        mScroller = new Scroller(context, new OvershootInterpolator());
        mPullDownAnimation = createPullDownAnimation();
        mProgressAnimation = createProgressAnimation();
    }

    @SuppressWarnings("deprecation")
    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PullToRefreshScrollView);

        Drawable drawable = ta.getDrawable(R.styleable.PullToRefreshScrollView_ptrHeaderBackground);
        if (drawable != null) {
            mHeaderLayout.setBackgroundDrawable(drawable);
        } else {
            mHeaderLayout.setBackgroundColor(0xFF333333);
        }

        drawable = ta.getDrawable(R.styleable.PullToRefreshScrollView_ptrHeaderArrowDrawable);
        if (drawable != null) {
            mArrowDrawable = drawable;
        } else {
            mArrowDrawable = getResources().getDrawable(R.drawable.refresh_arrow);
        }

        drawable = ta.getDrawable(R.styleable.PullToRefreshScrollView_ptrHeaderProgressDrawable);
        if (drawable != null) {
            mProgressDrawable = drawable;
        } else {
            mProgressDrawable = getResources().getDrawable(R.drawable.refresh_progress);
        }

        drawable = ta.getDrawable(R.styleable.PullToRefreshScrollView_ptrHeaderCompletedDrawable);
        if (drawable != null) {
            mCompletedDrawable = drawable;
        } else {
            mCompletedDrawable = getResources().getDrawable(R.drawable.refresh_completed);
        }

        CharSequence text =
                ta.getText(R.styleable.PullToRefreshScrollView_ptrHeaderPullToRefreshText);
        if (text != null) {
            mTextPullToRefresh = text;
        }

        text = ta.getText(R.styleable.PullToRefreshScrollView_ptrHeaderReleaseToRefreshText);
        if (text != null) {
            mTextReleaseToRefresh = text;
        }

        text = ta.getText(R.styleable.PullToRefreshScrollView_ptrHeaderRefreshingText);
        if (text != null) {
            mTextRefreshing = text;
        }

        text = ta.getText(R.styleable.PullToRefreshScrollView_ptrHeaderCompletedText);
        if (text != null) {
            mTextCompleted = text;
        }


        ta.recycle();

    }

    private LinearLayout createHeaderLayout(Context context) {
        int height = (int) (getResources().getDisplayMetrics().heightPixels / mPullRatio);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, height);
        params.topMargin = -params.height;//这里用了负数，使View强制向上偏移

        LinearLayout ll = new LinearLayout(context);
        ll.setLayoutParams(params);
        ll.setGravity(Gravity.BOTTOM);
        return ll;
    }

    private ScrollView createContentLayout(Context context) {
        ScrollView sv = new ScrollView(context);
        LayoutParams params =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        sv.setLayoutParams(params);
        return sv;
    }

    @SuppressWarnings("deprecation")
    public void setHeaderLayoutBackground(Drawable background) {
        mHeaderLayout.setBackgroundDrawable(background);
    }

    private Animation createPullDownAnimation() {
        RotateAnimation rotate = new RotateAnimation(0, 180, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(200);
        rotate.setFillAfter(true);
        return rotate;
    }

    private Animation createProgressAnimation() {
        RotateAnimation rotate = new RotateAnimation(0, 358, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setDuration(1000);
        rotate.setRepeatCount(Animation.INFINITE);
        return rotate;
    }

    @Override
    protected void onDetachedFromWindow() {
        //停止动画
        mProgressAnimation.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        if (child == mHeaderLayout || child == mContentLayout) {
            //对于头部布局，直接交给MainLayout(即本View）处理
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
                mRootStartScrollY = getScrollY();
                mContentStartScrollY = mContentLayout.getScrollY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                float deltaY = ev.getY() - mTouchDownY;

                PullAction pullAction = PullAction.NONE;
                if (deltaY > 0) {
                    pullAction = PullAction.DOWN;
                } else if (deltaY < 0) {
                    pullAction = PullAction.UP;
                } else {
                    pullAction = PullAction.NONE;
                }

                Log.d(TAG, String.format("Action= %s deltaY=%f", pullAction, deltaY));
                Log.i(TAG, String.format("mainLayout.startY=%d", mRootStartScrollY));
                Log.i(TAG, String.format("mainLayout.scrollY=%d", getScrollY()));
                Log.e(TAG, String.format("contentLayout.startY=%d", mContentStartScrollY));
                Log.e(TAG, String.format("contentLayout.scrollY=%d", mContentLayout.getScrollY()));

                if (pullAction == PullAction.DOWN && !canPullDown()) {

                    //重新记录位置
                    Log.i(TAG, "canPullDown=false");
                    mTouchDownY = ev.getY();
                    mRootStartScrollY = getScrollY();
                    mContentStartScrollY = mContentLayout.getScrollY();

                } else if (pullAction == PullAction.DOWN) {

                    //滚动MainLayout，使头部布局显示出来
                    Log.i(TAG, "canPullDown=true");
                    int newScrollY = (int) (mRootStartScrollY -
                            (deltaY - mContentStartScrollY) / mPullRatio);
                    mScroller.abortAnimation();
                    scrollTo(getScrollX(), newScrollY);
                    changePullDownState(computePullState());
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {

                if (mPullDownState == PullState.RELEASE_TO_REFRESH) {
                    //进入正在刷新状态
                    changePullDownState(PullState.REFRESHING);
                    smoothScrollTo(getScrollX(), getTop() - mHeaderViewHeight);
                } else if (mPullDownState == PullState.REFRESHING) {
                    smoothScrollTo(getScrollX(), getTop() - mHeaderViewHeight);
                } else {
                    smoothScrollTo(getScrollX(), getTop());
                }
                break;
            }
            default:
        }

        return super.dispatchTouchEvent(ev);
    }

    /**
     * 当前状态下，是否允许下拉刷新
     */
    private boolean canPullDown() {
        return !(mRootStartScrollY > 0 || mContentLayout.getScrollY() > 0) &&
                (mPullDownState == PullState.NONE ||
                        mPullDownState == PullState.PULL_DOWN_TO_REFRESH ||
                        mPullDownState == PullState.RELEASE_TO_REFRESH ||
                        mPullDownState == PullState.REFRESHING ||
                        mPullDownState == PullState.COMPLETED ||
                        mPullDownState == PullState.COMPLETED_IMMEDIATELY);

    }

    /**
     * 计算下拉状态
     */
    private PullState computePullState() {
        // Log.e(TAG, "getScrollY=" + getScrollY() + " | mHeaderViewHeight=" +
        // mHeaderViewHeight);
        return getScrollY() > -mHeaderViewHeight ? PullState.PULL_DOWN_TO_REFRESH :
                PullState.RELEASE_TO_REFRESH;
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
            if (mPullDownState == PullState.COMPLETED && getScrollY() == getTop()) {
                changePullDownState(PullState.NONE);
            } else if (mPullDownState == PullState.PULL_DOWN_TO_REFRESH &&
                    getScaleY() == getTop()) {
                changePullDownState(PullState.NONE);
            }
        }
    }

    /**
     * 更改下拉刷新的状态
     *
     * @param state 指定状态
     */
    private void changePullDownState(PullState state) {
        if (mPullDownState == state) {
            return;
        }

        if ((mPullDownState == PullState.COMPLETED || mPullDownState == PullState.REFRESHING) &&
                (state == PullState.PULL_DOWN_TO_REFRESH ||
                        state == PullState.RELEASE_TO_REFRESH)) {
            return;
        }

        updateHeaderView(mPullDownState, state);
        mPullDownState = state;

        switch (state) {
            case NONE:
                break;
            case PULL_DOWN_TO_REFRESH:
                break;
            case RELEASE_TO_REFRESH:
                break;
            case REFRESHING:
                if (mOnRefreshListener != null) {
                    mOnRefreshListener.onRefreshing();
                } else {
                    onRefreshCompletedImmediately();
                }
                break;
            case COMPLETED:
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        Log.e(TAG, "post delay start");
                        smoothScrollTo(0, getTop());
                    }
                }, 500);
                break;
            case COMPLETED_IMMEDIATELY:
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        Log.e(TAG, "post delay start");
                        smoothScrollTo(0, getTop());
                    }
                }, 0);
                break;
            default:
        }
    }

    /**
     * 更新头部布局的状态
     *
     * @param oldState 旧的状态
     * @param newState 新的状态
     */
    private void updateHeaderView(PullState oldState, PullState newState) {

        View layout = findViewById(R.id.header_img_layout);

        Log.e(TAG, "更新状态从" + oldState + "到" + newState);

        switch (newState) {
            case COMPLETED:
                mHeaderTextView.setText(mTextCompleted);
                mProgressAnimation.cancel();
                mHeaderIconView.setVisibility(View.GONE);
                mHeaderTextView.setCompoundDrawablePadding(
                        (int) (10 * getResources().getDisplayMetrics().density));
                mHeaderTextView
                        .setCompoundDrawablesWithIntrinsicBounds(mCompletedDrawable, null, null,
                                null);
                break;
            case NONE:
                mPullDownAnimation.setFillAfter(false);
                mPullDownAnimation.reset();
                mProgressAnimation.cancel();
                mHeaderTextView.setText("");
                mHeaderTextView.setCompoundDrawables(null, null, null, null);
                mHeaderIconView.setVisibility(View.GONE);
                break;
            case PULL_DOWN_TO_REFRESH:
                mPullDownAnimation.setFillAfter(false);
                mPullDownAnimation.reset();
                mHeaderTextView.setText(mTextPullToRefresh);
                mHeaderTextView.setCompoundDrawables(null, null, null, null);
                mHeaderIconView.setVisibility(View.VISIBLE);
                mHeaderIconView.setImageDrawable(mArrowDrawable);
                break;
            case RELEASE_TO_REFRESH:
                mHeaderIconView.setVisibility(View.VISIBLE);
                mPullDownAnimation.setFillAfter(true);
                mHeaderTextView.setText(mTextReleaseToRefresh);
                mHeaderTextView.setCompoundDrawables(null, null, null, null);
                mHeaderIconView.setVisibility(View.VISIBLE);
                mHeaderIconView.setImageDrawable(mArrowDrawable);
                layout.startAnimation(mPullDownAnimation);
                break;
            case REFRESHING:
                mHeaderTextView.setText(mTextRefreshing);
                mHeaderTextView.setCompoundDrawables(null, null, null, null);
                mHeaderIconView.setImageDrawable(mProgressDrawable);
                layout.startAnimation(mProgressAnimation);
                break;

            default:
        }
    }

    /**
     * 通知该下拉刷新组件，刷新工作已经完成了
     */
    public void onRefreshCompleted() {
        changePullDownState(PullState.COMPLETED);
    }

    private void onRefreshCompletedImmediately() {
        changePullDownState(PullState.COMPLETED_IMMEDIATELY);
    }

    /**
     * 获取下拉刷新回调监听器
     */
    public OnRefreshListener getOnRefreshListener() {
        return mOnRefreshListener;
    }

    /**
     * 设置下拉刷新回调监听器
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        this.mOnRefreshListener = listener;
    }

    /**
     * 下拉刷新回调监听器
     */
    public interface OnRefreshListener {
        /**
         * 当进入刷新状态的时候，该方法被调用
         */
        void onRefreshing();
    }
}
