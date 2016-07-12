package cn.bassy.widget;

import android.content.Context;
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
 * @version 1	2016-7-12
 */
public class PullToRefreshScrollView extends LinearLayout {

	private static final String TAG = "BoundView";

	/** 表示动作类型*/
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
		};
	}

	/** 表示下拉状态*/
	enum PullState {
		NONE, PULL_DOWN_TO_REFRESH, RELEASE_TO_REFRESH, REFRESHING, COMPLETED;
		public String toString() {
			if (this == NONE)
				return "无";
			else if (this == PULL_DOWN_TO_REFRESH)
				return "下拉刷新";
			else if (this == RELEASE_TO_REFRESH)
				return "松开立即刷新";
			else if (this == REFRESHING)
				return "正在刷新";
			else if (this == COMPLETED)
				return "刷新完成";
			else
				return "";
		};
	}

	PullAction mPullAction = PullAction.NONE;
	PullState mPullDownState = PullState.NONE;
	PullState mPullUpState = PullState.NONE;

	LayoutInflater mLayoutInflater;

	//下拉刷新的头部布局
	LinearLayout mHeaderLayout;
	//这是头部布局里面的一个View
	View mHeaderView;
	//头部布局的高度，单位是DIP
	int mHeaderViewHeight = 45;

	//这个是内容布局
	ScrollView mContentLayout;

	//记录触摸按下位置 
	float mTouchDownY;
	//记录当前内容布局的滚动位置
	int mContentStartScrollY;
	//记录当前整体布局的滚动位置
	int mMainStartScrollY;

	//下拉偏移量与展示头部布局高度之间的比率
	float mPullRatio = 2.5f;

	//反弹效果
	Scroller mScroller;
	//下拉图标动画
	Animation mPullDownAnimation;
	//进度条动画
	Animation mProgressAnimation;

	//下拉刷新接口
	OnRefreshListener mOnRefreshListener;

	public PullToRefreshScrollView(Context context) {
		super(context);
		init(context);
	}

	public PullToRefreshScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public PullToRefreshScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	//初始化
	private void init(Context context) {
		setClickable(true);//设置为可点击，否则无法滚动

		mLayoutInflater = LayoutInflater.from(context);
		mHeaderLayout = createHeaderLayout(context);
		mHeaderView = mLayoutInflater.inflate(R.layout.header_view, this, false);

		mHeaderViewHeight *= getResources().getDisplayMetrics().density;
		mHeaderView.getLayoutParams().width = LayoutParams.MATCH_PARENT;
		mHeaderView.getLayoutParams().height = mHeaderViewHeight;

		mHeaderLayout.addView(mHeaderView);
		this.addView(mHeaderLayout);

		mContentLayout = createContentLayout(context);
		this.addView(mContentLayout);

		mScroller = new Scroller(context);
		mPullDownAnimation = createPullDownAnimation();
		mProgressAnimation = createProgressAnimation();
	}

	private LinearLayout createHeaderLayout(Context context) {
		int height = (int) (getResources().getDisplayMetrics().heightPixels / mPullRatio);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, height);
		params.topMargin = -params.height;//这里用了负数，使View强制向上偏移

		LinearLayout ll = new LinearLayout(context);
		ll.setLayoutParams(params);
		ll.setGravity(Gravity.BOTTOM);
		ll.setBackgroundColor(0xFF333333);
		return ll;
	}

	private ScrollView createContentLayout(Context context) {
		ScrollView sv = new ScrollView(context);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
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
			mMainStartScrollY = getScrollY();
			mContentStartScrollY = mContentLayout.getScrollY();
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			float deltaY = ev.getY() - mTouchDownY;

			if (deltaY > 0) {
				mPullAction = PullAction.DOWN;
			} else if (deltaY < 0) {
				mPullAction = PullAction.UP;
			} else {
				mPullAction = PullAction.NONE;
			}

			Log.d(TAG, String.format("Action= %s deltaY=%f", mPullAction, deltaY));
			Log.i(TAG, String.format("mainLayout.startY=%d", mMainStartScrollY));
			Log.i(TAG, String.format("mainLayout.scrolly=%d", getScrollY()));
			Log.e(TAG, String.format("contentLayout.startY=%d", mContentStartScrollY));
			Log.e(TAG, String.format("contentLayout.scrolly=%d", mContentLayout.getScrollY()));

			if (mPullAction == PullAction.DOWN && !canPullDown()) {

				//重新记录位置
				Log.i(TAG, String.format("canPullDown=true"));
				mTouchDownY = ev.getY();
				mMainStartScrollY = getScrollY();
				mContentStartScrollY = mContentLayout.getScrollY();

			} else if (mPullAction == PullAction.DOWN) {

				//滚动MainLayout，使头部布局显示出来
				Log.i(TAG, String.format("canPullDown=false"));
				int newScrollY = (int) (mMainStartScrollY - (deltaY - mContentStartScrollY) / mPullRatio);
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

	/** 当前状态下，是否允许下拉刷新 */
	private boolean canPullDown() {
		if (mMainStartScrollY > 0 || mContentLayout.getScrollY() > 0)
			return false;

		return mPullDownState == PullState.NONE | mPullDownState == PullState.PULL_DOWN_TO_REFRESH
				| mPullDownState == PullState.RELEASE_TO_REFRESH | mPullDownState == PullState.REFRESHING
				|| mPullDownState == PullState.COMPLETED;
	}

	/** 计算下拉状态 */
	private PullState computePullState() {
		// Log.e(TAG, "getScrollY=" + getScrollY() + " | mHeaderViewHeight=" +
		// mHeaderViewHeight);
		return getScrollY() > -mHeaderViewHeight ? PullState.PULL_DOWN_TO_REFRESH : PullState.RELEASE_TO_REFRESH;
	}

	/**
	 * 平滑滚动到指定位置
	 * @param toX	目标水平位置
	 * @param toY	目标垂直位置
	 */
	private void smoothScrollTo(int toX, int toY) {

		if (mScroller != null) {
			mScroller.abortAnimation();
			mScroller.forceFinished(true);
		}

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
			} else if (mPullDownState == PullState.PULL_DOWN_TO_REFRESH && getScaleY() == getTop()) {
				changePullDownState(PullState.NONE);
			}
		}
	}

	/**
	 * 更改下拉刷新的状态
	 * @param state	指定状态
	 * @return 返回是否更改成功
	 */
	private boolean changePullDownState(PullState state) {
		if (mPullDownState == state) {
			return false;
		}

		if ((mPullDownState == PullState.COMPLETED | mPullDownState == PullState.REFRESHING)
				&& (state == PullState.PULL_DOWN_TO_REFRESH | state == PullState.RELEASE_TO_REFRESH)) {
			return false;
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
		default:
		}
		return true;
	}

	/** 
	 * 更新头部布局的状态
	 * 
	 * @param oldstate	旧的状态
	 * @param newstate	新的状态
	 */
	private void updateHeaderView(PullState oldstate, PullState newstate) {

		TextView tv = (TextView) findViewById(R.id.hreader_tv_msg);
		ImageView img = (ImageView) findViewById(R.id.header_imageview);
		View layout = findViewById(R.id.header_img_layout);

		Log.e(TAG, "更新状态从" + oldstate + "到" + newstate);

		switch (newstate) {
		case COMPLETED:
			tv.setText("刷新成功　　");
			mProgressAnimation.cancel();
			img.setVisibility(View.GONE);
			Drawable left = getResources().getDrawable(R.drawable.refresh_success);
			tv.setCompoundDrawablePadding((int) (10 * getResources().getDisplayMetrics().density));
			tv.setCompoundDrawablesWithIntrinsicBounds(left, null, null, null);
			break;
		case NONE:
			mPullDownAnimation.setFillAfter(false);
			mPullDownAnimation.reset();
			mProgressAnimation.cancel();
			tv.setText("");
			tv.setCompoundDrawables(null, null, null, null);
			img.setVisibility(View.GONE);
			break;
		case PULL_DOWN_TO_REFRESH:
			mPullDownAnimation.setFillAfter(false);
			mPullDownAnimation.reset();
			tv.setText("下拉刷新");
			tv.setCompoundDrawables(null, null, null, null);
			img.setVisibility(View.VISIBLE);
			img.setImageResource(R.drawable.refresh_arrow);
			break;
		case RELEASE_TO_REFRESH:
			img.setVisibility(View.VISIBLE);
			mPullDownAnimation.setFillAfter(true);
			tv.setText("松开立即刷新");
			tv.setCompoundDrawables(null, null, null, null);
			img.setVisibility(View.VISIBLE);
			img.setImageResource(R.drawable.refresh_arrow);
			layout.startAnimation(mPullDownAnimation);
			break;
		case REFRESHING:
			tv.setText("正在刷新");
			tv.setCompoundDrawables(null, null, null, null);
			img.setImageResource(R.drawable.refresh_progress);
			layout.startAnimation(mProgressAnimation);
			break;

		default:
		}
	}

	/** 通知该下拉刷新组件，刷新工作已经完成了*/
	public void onRefreshCompleted() {
		changePullDownState(PullState.COMPLETED);
	}

	/** 获取下拉刷新回调监听器*/
	public OnRefreshListener getOnRefreshListener() {
		return mOnRefreshListener;
	}

	/** 设置下拉刷新回调监听器*/
	public void setOnRefreshListener(OnRefreshListener listener) {
		this.mOnRefreshListener = listener;
	}

	/** 下拉刷新回调监听器 */
	public interface OnRefreshListener {
		/** 当进入刷新状态的时候，该方法被调用*/
		public void onRefreshing();
	}
}
