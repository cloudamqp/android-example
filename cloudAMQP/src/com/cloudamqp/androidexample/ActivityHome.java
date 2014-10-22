package com.cloudamqp.androidexample;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.cloudamqp.R;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;

import android.app.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ActivityHome extends Activity {
	Thread subscribeThread;
	Thread publishThread;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("","on create");
		setContentView(R.layout.activity_main);

		Button button = (Button) findViewById(R.id.publish);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				EditText et = (EditText) findViewById(R.id.text);
				publishMessage(et.getText().toString());
				et.setText("");
			}
		});
		
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
		setupConnectionFactory();		
		publishToAMQP();
		subscribe(handler);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unsubscribe();
	}
	
	ConnectionFactory factory = new ConnectionFactory();
	private BlockingDeque<String> queue = new LinkedBlockingDeque<String>();

	public void publishMessage(String message) {
		//Adds a message to internal blocking queue
		try {
			Log.d("","[q] " + message);
			queue.putLast(message);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void setupConnectionFactory() {
		String uri = "CLOUDAMQP_URL";
		try {
			factory.setAutomaticRecoveryEnabled(false);	
			factory.setUri(uri);
		} catch (KeyManagementException | NoSuchAlgorithmException	| URISyntaxException e1) {
			e1.printStackTrace();
		}
	}

	private Connection connect() throws Exception
	{
		Connection connection = factory.newConnection();
		Log.d("", "Connection established");
		return connection; 
	}

	public void subscribe(final Handler handler)
	{
		subscribeThread = new Thread(new Runnable() { 
			@Override
			public void run() {
				while(true)
				{
					try{
						Connection connection = connect();
						Channel channel = connection.createChannel();
						channel.basicQos(1);
						DeclareOk q = channel.queueDeclare();
						channel.queueBind(q.getQueue(), "amq.fanout", "chat");
						QueueingConsumer consumer = new QueueingConsumer(channel);
						channel.basicConsume(q.getQueue(), true, consumer);

						// Process deliveries
						while (true) {
							QueueingConsumer.Delivery delivery = consumer.nextDelivery();

							String message = new String(delivery.getBody());
							Log.d("","[r] "+message);

							Message msg = handler.obtainMessage();
							Bundle bundle = new Bundle();

							bundle.putString("msg", message);
							msg.setData(bundle);
							handler.sendMessage(msg);
						}
					} catch (InterruptedException e) {
						break;
					} catch (Exception e1) {
						Log.d("","Connection broken"+ e1.getClass().getName());
						try {
							//sleep and then try again
							Thread.sleep(4000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
		subscribeThread.start();
	}

	public void unsubscribe()
	{
		publishThread.interrupt();
		subscribeThread.interrupt();
	}

	public void publishToAMQP()
	{
		publishThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true)
				{
					try {
						Connection connection = connect();
						Channel channel1 = connection.createChannel();
						channel1.confirmSelect();

						while (true) {
							String message = queue.takeFirst();
							try{
								channel1.basicPublish("amq.fanout", "chat", null, message.getBytes());
								Log.d("", "[s] " + message);
								channel1.waitForConfirmsOrDie();
							} catch (Exception e){
								Log.d("","[f] " + message);
								queue.putFirst(message);
								throw e;
							}
						}
					} catch (InterruptedException e) {
						break;
					} catch (Exception e) {
						Log.d("","Connection broken");
						try {
							//sleep and then try again
							Thread.sleep(5000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		});
		publishThread.start();
	}
}