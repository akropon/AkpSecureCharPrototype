/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.akropon.secureChatPrototype.clientApi;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.StringUtil;
import org.json.JSONObject;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * Simplistic telnet client.
 */
public final class ClientImpl {

    private String host = "localhost";  // "127.0.0.1" (u can delete this comment)
    private int port = 8080;

    private byte[] myBaseSequence;
    private byte[] mySecretPartOfKey;
    private byte[] myPublicPartOfKey;
    byte[] myKey = new byte[]{1, 2, 3, 4};

    private final EventHandler handler;
    private PrintStream LOG = null;

    private Channel channel;

    public ClientImpl(EventHandler handler) {
        this.handler = handler;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setLogger(PrintStream LOG) {
        this.LOG = LOG;
    }

    public void tryConnectToServer() {
        Thread thread = new Thread(this::tryConnectToServer1);
        thread.start();
    }

    private void tryConnectToServer1() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ClientInitializer(this));
            channel = b.connect(host, port).sync().channel();
            fireSuccessfullyConnectedToServer();
            channel.closeFuture().sync();
        } catch (Exception e) {
            fireFailedConnectionToServer(e);
            logPrintln(e.toString());
        } finally {
            Future<?> future = group.shutdownGracefully();
            waitForShutdowningGrasefully(future);
        }
    }

    private void waitForShutdowningGrasefully(Future<?> future) {
        try {
            future.sync();
            logPrintln("Client closed.");
            fireConnectionClosed();
        } catch (InterruptedException e) {
            logPrintln(e.toString());
            fireClosingFailed(e);
        }
    }

    private void fireClosingFailed(InterruptedException e) {
        handler.fireClosingFailed(e);
    }

    private void fireConnectionClosed() {
        handler.fireConnectionClosed();
    }

    private void fireFailedConnectionToServer(Exception e) {
        handler.fireFailedConnectionToServer(e);
    }

    private void fireSuccessfullyConnectedToServer() {
        handler.fireSuccessfullyConnectedToServer();
    }

    void fireMessageReceived(String msg) {
        handler.fireMessageReceived(msg);
    }

    public void sendMessage(String text) {
        if (text == null) {
            return;
        }
        String lineMsg = convertIntoOneLineMessage(text);
        channel.writeAndFlush(lineMsg + "\r\n");
    }

    public void close() {
        channel.close();
    }

    /*public void sendMessageasdffasfdfdfasdfsdfdsfsdfasdfsdfsd(String text) {
        // Read commands from the stdin.
        ChannelFuture lastWriteFuture = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            if (text == null) {
                break;
            }

            String lineMsg = convertIntoOneLineMessage(text);

            // Sends the received text to the server.
            lastWriteFuture = channel.writeAndFlush(lineMsg + "\r\n");

            // If user typed the 'bye' command, wait until the server closes
            // the connection.
            if ("exit".equals(text.toLowerCase())) {
                channel.close();
                channel.closeFuture().sync();
                break;
            }
        }

        // Wait until all messages are flushed before closing the channel.
        if (lastWriteFuture != null) {
            lastWriteFuture.sync();
        }
    }*/

    void logPrintln(String msg) {
        if (LOG != null)
            LOG.println(msg);
    }

    void fireChannelActive(ChannelHandlerContext ctx) {
        myBaseSequence = Crypto.genBaseSequence();
        mySecretPartOfKey = Crypto.genSecretPartOfKey();
        myPublicPartOfKey = Crypto.getPublicPartOfKey(myBaseSequence, mySecretPartOfKey);
        sendKeyExchangeData(ctx.channel(), true);
    }

    private void sendKeyExchangeData(Channel channel, boolean isRequest) {
        logPrintln("Doing key exchange procedure. IsAnswer=" + !isRequest);

        String stage = isRequest ? "giveAndAsk" : "answer";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("requestType", "keyExchange");
        jsonObject.put("stage", stage);
        jsonObject.put("publicBaseSequence", StringUtil.toHexString(myBaseSequence));
        jsonObject.put("publicPartOfKey", StringUtil.toHexString(myPublicPartOfKey));
        channel.writeAndFlush(jsonObject.toString() + "\n");
    }

    void fireKeyExchangeRequestReceived(Channel channel, byte[] baseSequence, byte[] publicPartOfKey, boolean needToAnswer) {

        if (needToAnswer) {
            myBaseSequence = baseSequence;
            mySecretPartOfKey = Crypto.genSecretPartOfKey();
            myPublicPartOfKey = Crypto.getPublicPartOfKey(myBaseSequence, mySecretPartOfKey);
            sendKeyExchangeData(channel, false);
        }

        myKey = Crypto.getSecretKey(mySecretPartOfKey, publicPartOfKey);
        logPrintln("secret: " + Arrays.toString(myKey));
    }

    private String convertIntoOneLineMessage(String text) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("requestType", "message");
        jsonObject.put("text", StringUtil.toHexString(Crypto.encodeText(text, myKey)));
        return jsonObject.toString();
    }


    public static ClientImpl client;

    public static void main(String[] args) throws Exception {
        client = new ClientImpl(new EventHandler() {
            @Override
            public void fireSuccessfullyConnectedToServer() {
                System.out.println("main(): connected successfully");
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        checkInStreamLoop(client);
                    }
                });
                thread.start();
            }

            @Override
            public void fireFailedConnectionToServer(Exception e) {
                System.out.println("main(): connect error");
            }

            @Override
            public void fireConnectionClosed() {
                System.out.println("main(): closed");
            }

            @Override
            public void fireClosingFailed(InterruptedException e) {
                System.out.println("main(): close error");
            }

            @Override
            public void fireMessageReceived(String msg) {
                System.out.println("man: "+msg);
            }
        });

        client.setHost("localhost");
        client.setPort(8080);
        client.setLogger(System.out);
        client.tryConnectToServer();
    }

    private static void checkInStreamLoop(ClientImpl client) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String text = readLineFrom(in);
            if (text == null) {
                break;
            }

            if ("exit".equals(text.toLowerCase())) {
                client.close();
                return;
            }

            client.sendMessage(text);
        }
    }

    private static String readLineFrom(BufferedReader in) {
        try {
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
