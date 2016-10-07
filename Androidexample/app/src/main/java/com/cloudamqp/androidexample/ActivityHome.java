package com.cloudamqp.androidexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ActivityHome extends AppCompatActivity {

    private static final String EXCHANGE_NAME = "chat";

    @BindView(R.id.publish) Button button;
    @BindView(R.id.text) EditText et;
    @BindView(R.id.textView) TextView tv;
    Thread subscribeThread;
    Thread publishThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        BusProvider.getInstance().register(this);
        this.subscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        subscribeThread.interrupt();
        publishThread.interrupt();
    }

    @Subscribe
    public void messageAvailable(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.append(text + "\n");
            }
        });
    }

    private Channel getChannel() throws IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(Constants.CLOUDAMQP_URI);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        return channel;
    }

    @OnClick(R.id.publish)
    public void publishMessage(){
        final String message = this.et.getText().toString();
        this.et.setText("");
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Channel channel = getChannel();
                    channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
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
                    Channel channel = getChannel();
                    String queueName = channel.queueDeclare().getQueue();
                    channel.queueBind(queueName, EXCHANGE_NAME, "");
                    Consumer consumer = new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope,
                                                   AMQP.BasicProperties properties, byte[] body) throws IOException {
                            BusProvider.getInstance().post(new String(body, "UTF-8"));
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
