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

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.StringUtil;
import org.json.JSONObject;

/**
 * Handles a client-side channel.
 */
@Sharable
public class ClientHandler extends SimpleChannelInboundHandler<String> {

    private final ClientImpl client;

    public ClientHandler(ClientImpl client) {
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        client.fireChannelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        // конвертируем строку с Json в JSONObject для дальнейшего его парсинга
        JSONObject msgJSON = new JSONObject(msg);

        String requestType = msgJSON.getString("requestType");
        if (requestType.equalsIgnoreCase("message")) {
            if (client.myKey != null) {
                String encodedTextAsHexNumbers = msgJSON.getString("text");
                byte[] encodedText = StringUtil.decodeHexDump(encodedTextAsHexNumbers);
                String text = Crypto.decodeText(encodedText, client.myKey);
                client.fireMessageReceived(text);
            } else {
                client.logPrintln("I have no key(((");
            }
        } else if (requestType.equalsIgnoreCase("keyExchange")) {
            String stage = msgJSON.getString("stage");
            byte[] publicBaseSequence = StringUtil.decodeHexDump(msgJSON.getString("publicBaseSequence"));
            byte[] publicPartOfKey = StringUtil.decodeHexDump(msgJSON.getString("publicPartOfKey"));
            boolean needToAnswer = false;
            if (stage.equalsIgnoreCase("giveAndAsk")) {
                needToAnswer = true;
            }
            client.fireKeyExchangeRequestReceived(ctx.channel(), publicBaseSequence, publicPartOfKey, needToAnswer);
        } else {
            client.logPrintln("Can't define request type. msg=\"" + msg + "\"");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
