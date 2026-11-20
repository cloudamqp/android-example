package com.cloudamqp.androidexample;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

/**
 * Created by carlos on 10/11/16.
 */

public class AmqpProvider {

    private static final AmqpProvider instance;
    static {
        try {
            instance = new AmqpProvider();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static AmqpProvider getInstance() {
        return instance;
    }

    private Connection connection;
    private Channel readChannel;
    private Channel writeChannel;

    private AmqpProvider() throws IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(Constants.CLOUDAMQP_URI);
        this.connection = factory.newConnection();
    }

    public Channel getReadChannel() throws IOException {
        if(this.readChannel == null){
            this.readChannel = this.buildChannel();
        }
        return readChannel;
    }

    public Channel getWriteChannel() throws IOException {
        if(this.writeChannel == null){
            this.writeChannel = this.buildChannel();
        }
        return writeChannel;
    }

    private Channel buildChannel() throws IOException {
        Channel channel = this.connection.createChannel();
        channel.exchangeDeclare(Constants.EXCHANGE_NAME, Constants.EXCHANGE_TYPE);
        return channel;
    }
}
