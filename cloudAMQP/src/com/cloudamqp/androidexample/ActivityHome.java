package com.cloudamqp.androidexample;
import java.text.SimpleDateFormat;
import java.util.Date;


import com.cloudamqp.R;
import com.cloudamqp.androidexample.MyService.LocalBinder;


import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class ActivityHome extends Activity {

	MyService mService;
	boolean mBound = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		
		Button button = (Button) findViewById(R.id.publish);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(mBound)
				{
					EditText et = (EditText) findViewById(R.id.text);
					mService.publishMessage(et.getText().toString());
					et.setText("");
				}
			}
		});
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d("","onstart");
		Intent intent = new Intent(this, MyService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);		
		
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d("","onStop");
		mService.unsubscribe();  
		// Unbind from the service
		if (mBound) {
			unbindService(mConnection); 
			mBound = false;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		
		super.onDestroy();
		Log.d("","onDestroy");
		
		
	}
	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className,
				IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;

			Handler handler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					String message=msg.getData().getString("msg");
					TextView tv = (TextView) findViewById(R.id.textView);
					Date dNow = new Date( );
					SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss"); 
					tv.append(ft.format(dNow) + ' ' + message + '\n');
				}
			};
			mService.subscribe(handler);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			 mService = null;
	         mBound = false;
		}
	};

}

