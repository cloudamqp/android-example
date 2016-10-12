package com.cloudamqp.androidexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ActivityHome extends AppCompatActivity {

    @BindView(R.id.text) EditText et;
    @BindView(R.id.textView) TextView tv;

    private Thread subscribeThread;
    private Thread publishThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        this.subscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        subscribeThread.interrupt();
        publishThread.interrupt();
    }

    private void onMessageReceived(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.append(text + System.getProperty("line.separator"));
            }
        });
    }

    @OnClick(R.id.publish)
    public void publishMessage(){
        final String message = this.et.getText().toString();
        this.et.setText("");
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Channel channel = AmqpProvider.getInstance().getWriteChannel();
                    channel.basicPublish(Constants.EXCHANGE_NAME, "", null, message.getBytes());
                } catch (Exception e) {
                    e.getMessage();
                    e.printStackTrace();
                }
            }
        });
        publishThread.start();
    }

    private void subscribe(){
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Channel channel = AmqpProvider.getInstance().getReadChannel();
                    String queueName = channel.queueDeclare().getQueue();
                    channel.queueBind(queueName, Constants.EXCHANGE_NAME, "");
                    Consumer consumer = new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope,
                                                   AMQP.BasicProperties properties, byte[] body) throws IOException {
                            onMessageReceived(new String(body, "UTF-8"));
                        }
                    };
                    channel.basicConsume(queueName, true, consumer);
                } catch (Exception e) {
                    e.getMessage();
                    e.printStackTrace();
                }
            }
        });
        subscribeThread.start();
    }
}
