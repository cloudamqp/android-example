package com.cloudamqp.androidexample;

import com.rabbitmq.client.*;

import android.app.Activity;
import android.os.Bundle;

import android.util.Log;

public class ActivityHome extends Activity {
	private Connection connection;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);

	  Thread thread = new Thread(new Runnable(){
	    @Override
	    public void run() {
	      String uri = "CLOUDAMQP_URL";
	      try {
	        // Open a connection
	        ConnectionFactory factory = new ConnectionFactory();
	        factory.setUri(uri);
	        connection = factory.newConnection();  

	        Channel channel1 = connection.createChannel();
	        Channel channel2 = connection.createChannel();

	        String message =  "Hello CloudAMQP!";
	        
	        // Declare the queue 'hello' 
	        channel1.queueDeclare("hello", false, false, false, null);

	        QueueingConsumer consumer = new QueueingConsumer(channel2);
	        channel2.basicConsume("hello", true, consumer);

	        // Publish the message 'Hello CloudAMQP''
	        channel1.basicPublish("", "hello", null, message.getBytes());
	        Log.d("", " [x] Sent '" + message + "'");

	        while (true) {
	          QueueingConsumer.Delivery delivery = consumer.nextDelivery();
	          String message1 = new String(delivery.getBody());
	          Log.d("", " [x] Received '" + message1 + "'");
	        }
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
	    }
	  });
	  thread.start();
	}

    @Override
    protected void onResume() {
        super.onPause();
    }
 
    @Override
    protected void onPause() {
        super.onPause();
    }
 
}