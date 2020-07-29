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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.util.Attribute;
import io.netty.util.AttributeMap;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.server.authorization.AuthorizationException;
import org.apache.tinkerpop.gremlin.server.authorization.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.apache.tinkerpop.shaded.jackson.databind.JsonNode;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;

/**
 * Implements authorization for use with the {@link HttpGremlinEndpointHandler} and HTTP based API calls.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class HttpAuthorizationHandler extends AbstractAuthorizationHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpAuthorizationHandler.class);
    private static final Logger auditLogger = LoggerFactory.getLogger(GremlinServer.AUDIT_LOGGER_NAME);
    private final Settings.AuthorizationSettings authorizationSettings;

    private final Base64.Decoder decoder = Base64.getUrlDecoder();

    private static final ObjectMapper mapper = new ObjectMapper();

    public HttpAuthorizationHandler(final Authorizer authorizer,
                                    final Settings.AuthorizationSettings authorizationSettings) {
        super(authorizer);
        this.authorizationSettings = authorizationSettings;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof FullHttpMessage) {
            final FullHttpMessage request = (FullHttpMessage) msg;
            ByteBuf jsonBuf = request.content();
            String jsonStr = jsonBuf.toString(CharsetUtil.UTF_8);

            String query = null;
            Bytecode bytecode = null;
            try {
                final JsonNode body = mapper.readTree(request.content().toString(CharsetUtil.UTF_8));
                final JsonNode scriptNode = body.get(Tokens.ARGS_GREMLIN);
                if (null == scriptNode) throw new IllegalArgumentException("no gremlin script supplied");

                query = scriptNode.asText();
                logger.debug("gremlinQuery:: {}", query);
                if (null == query) throw new IllegalArgumentException("null gremlin script");

            } catch (IOException ioe) {
                throw new IllegalArgumentException("body could not be parsed", ioe);
            }

            String traversalResource = getGraphTraversalString(query);
            Object traversalObject = getTraversalObjectFromQuery(query, traversalResource);

            if(traversalObject==null || traversalObject instanceof Number || traversalObject instanceof String){
                ctx.fireChannelRead(request);
                return;
            } else if (traversalObject instanceof GraphTraversal.Admin){
                bytecode = ((DefaultGraphTraversal) traversalObject).getBytecode();
            } else {
                logger.warn("unrecognised traversal query:: {}, type:: {}", query, traversalObject.getClass());
            }

            boolean hasWriteStep = hasWriteStep(bytecode);

            final Attribute<String> user = ((AttributeMap) ctx).attr(StateKey.AUTHENTICATED_USER);
            try {
                authorize(user.get(), hasWriteStep, traversalResource, ctx);
                ctx.fireChannelRead(request);
            } catch (AuthorizationException ae) {
                sendError(ctx, msg);
            }
        }
    }

    private void sendError(final ChannelHandlerContext ctx, final Object msg) {
        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN)).addListener(ChannelFutureListener.CLOSE);
        ReferenceCountUtil.release(msg);
    }
}
