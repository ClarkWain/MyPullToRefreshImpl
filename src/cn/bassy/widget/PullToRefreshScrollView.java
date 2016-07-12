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
 *
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

	LinearLayout mHeaderLayout;
	View mHeaderView;
	int mHeaderViewHeight = 45; // DIP

	ScrollView mContentLayout;

	float mTouchDownY;
	int mContentStartScrollY;
	int mMainStartScrollY;
	boolean mIsTouchDown = false;

	float mPullRatio = 2.5f;

	Scroller mScroller;
	Animation mPullDownAnimation;
	Animation mProgressAnimation;

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

	private void init(Context context) {
		setClickable(true);

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
		params.topMargin = -params.height;

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
		mProgressAnimation.cancel();
		super.onDetachedFromWindow();
	}

	@Override
	public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
		if (child == mHeaderLayout || child == mContentLayout) {
			super.addView(child, index, params);
		} else {
			mContentLayout.addView(child, index, params);
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {

		int actionMask = ev.getAction() & MotionEvent.ACTION_MASK;

		switch (actionMask) {
		case MotionEvent.ACTION_DOWN: {
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

				Log.i(TAG, String.format("canPullDown=true"));
				mTouchDownY = ev.getY();
				mMainStartScrollY = getScrollY();
				mContentStartScrollY = mContentLayout.getScrollY();

			} else if (mPullAction == PullAction.DOWN) {

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
				changePullDownState(PullState.REFRESHING);
				animateTo(getScrollX(), getTop() - mHeaderViewHeight);
			} else if (mPullDownState == PullState.REFRESHING) {
				animateTo(getScrollX(), getTop() - mHeaderViewHeight);
			} else {
				animateTo(getScrollX(), getTop());
			}
			break;
		}
		default:
		}

		boolean ret = super.dispatchTouchEvent(ev);
		Log.d(TAG, "super.dispatchTouchEvent(ev)=" + ret);
		return ret;
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

	private void animateTo(int toX, int toY) {

		if (mScroller != null) {
			mScroller.abortAnimation();
			mScroller.forceFinished(true);
		}

		int fromX = getScrollX();
		int fromY = getScrollY();

		mScroller.startScroll(fromX, fromY, toX - fromX, toY - fromY, 300);
		invalidate();// 要求重绘（即调用draw，一该方法会调用computeScroll()
	}

	@Override
	public void computeScroll() {

		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			invalidate();// 要求重绘（即调用draw，一该方法会调用computeScroll()
		} else if (mScroller.isFinished()) {
			// 动画结束
			if (mPullDownState == PullState.COMPLETED && getScrollY() == getTop()) {
				changePullDownState(PullState.NONE);
			} else if (mPullDownState == PullState.PULL_DOWN_TO_REFRESH && getScaleY() == getTop()) {
				changePullDownState(PullState.NONE);
			}
		}
	}

	private void changePullDownState(PullState state) {
		if (mPullDownState == state) {
			return;
		}

		if ((mPullDownState == PullState.COMPLETED | mPullDownState == PullState.REFRESHING)
				&& (state == PullState.PULL_DOWN_TO_REFRESH | state == PullState.RELEASE_TO_REFRESH)) {
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
			}
			break;
		case COMPLETED:
			new Handler().postDelayed(new Runnable() {

				@Override
				public void run() {
					Log.e(TAG, "post delay start");
					animateTo(0, getTop());
				}
			}, 500);
			break;
		default:
		}
	}

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

	public void onRefreshCompleted() {
		changePullDownState(PullState.COMPLETED);
	}

	public OnRefreshListener getOnRefreshListener() {
		return mOnRefreshListener;
	}

	public void setOnRefreshListener(OnRefreshListener listener) {
		this.mOnRefreshListener = listener;
	}

	public interface OnRefreshListener {
		public void onRefreshing();
	}
}
