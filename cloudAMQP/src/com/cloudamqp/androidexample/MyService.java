package com.cloudamqp.androidexample;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class MyService extends Service {

	private final IBinder mBinder = new LocalBinder();
	public class LocalBinder extends Binder {
		MyService getService() {
			// Return this instance of LocalService so clients can call public methods
			return MyService.this;
		}
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
		String uri = "amqps://uquscgyg:Bep6Q9YNrfhbBbIjSF4_jyEQyrO8tC_A@yellow-turtle.rmq.cloudamqp.com/uquscgyg";
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
				while(!Thread.currentThread().isInterrupted())
				{
					try{
						Connection connection = connect();
						Channel channel = connection.createChannel();
						channel.basicQos(1);
						DeclareOk q = channel.queueDeclare();
						channel.queueBind(q.getQueue(), "amq.fanout", "chat");
						QueueingConsumer consumer = new QueueingConsumer(channel);
						channel.basicConsume(q.getQueue(), false, consumer);

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

							channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
						}
					}
					catch (InterruptedException e) 
					{
						Thread.currentThread().interrupt();
					}

					catch (Exception e1) {
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

	Thread subscribeThread;
	Thread publishThread;
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
				while(!Thread.currentThread().isInterrupted())
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

							}catch (Exception e)
							{
								Log.d("","[f] " + message);
								queue.putFirst(message);
								throw e;
							}
						}
					}
					catch (InterruptedException e) 
					{
						Thread.currentThread().interrupt();
					}
					catch (Exception e) {
					
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

@Override
public IBinder onBind(Intent intent) {
	setupConnectionFactory();		
	publishToAMQP();
	return mBinder;
}




}