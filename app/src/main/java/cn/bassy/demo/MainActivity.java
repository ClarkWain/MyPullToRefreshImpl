package cn.bassy.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import cn.bassy.widget.PullToRefreshScrollView;
import cn.bassy.widget.PullToRefreshScrollView.OnRefreshListener;

public class MainActivity extends Activity {

	PullToRefreshScrollView mBoundView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mBoundView = (PullToRefreshScrollView) findViewById(R.id.boundview);
		mBoundView.setOnRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefreshing() {
				new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

					@Override
					public void run() {
						mBoundView.onRefreshCompleted();
					}
				}, 1500);
			}
		});
	}

	public void onButtonClick(View v){
        startActivity(new Intent(this, DetailActivity.class));
    }

}
