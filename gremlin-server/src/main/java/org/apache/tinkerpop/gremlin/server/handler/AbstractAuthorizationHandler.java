/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.server.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.tinkerpop.gremlin.server.auth.Authenticator;
import org.apache.tinkerpop.gremlin.server.authorization.AuthorizationException;
import org.apache.tinkerpop.gremlin.server.authorization.AuthorizationRequest;
import org.apache.tinkerpop.gremlin.server.authorization.Authorizer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;

public abstract class AbstractAuthorizationHandler extends ChannelInboundHandlerAdapter {
    protected final Authorizer authorizer;

    public AbstractAuthorizationHandler(final Authorizer authorizer) {
        this.authorizer = authorizer;
    }

    protected void authorize(String user, boolean hasWriteStep, String resource,
                           ChannelHandlerContext ctx) throws AuthorizationException {
        InetAddress remoteIP = getRemoteInetAddress(ctx);
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(user);
        authorizationRequest.setClientIPAddress(remoteIP.toString());
        authorizationRequest.setAccessTime((new Date()));
        authorizationRequest.setResource(resource);

        if(hasWriteStep){
            authorizationRequest.setAccessType(AuthorizationRequest.AccessType.WRITE);
        } else{
            authorizationRequest.setAccessType(AuthorizationRequest.AccessType.READ);
        }
        authorizationRequest.setAction(authorizationRequest.getAccessType().name());

        boolean isAccessAllowed = authorizer.isAccessAllowed(authorizationRequest);

        if(!isAccessAllowed) {
            throw new AuthorizationException("Action ["+authorizationRequest.getAccessType().name()+"] not allowed for user ["+authorizationRequest.getUser()+"]");
        }
    }

    private InetAddress getRemoteInetAddress(final ChannelHandlerContext ctx) {
        final Channel channel = ctx.channel();

        if (null == channel)
            return null;

        final SocketAddress genericSocketAddr = channel.remoteAddress();

        if (null == genericSocketAddr || !(genericSocketAddr instanceof InetSocketAddress))
            return null;

        return ((InetSocketAddress)genericSocketAddr).getAddress();
    }
}
