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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeMap;
import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.apache.tinkerpop.gremlin.driver.message.ResponseMessage;
import org.apache.tinkerpop.gremlin.driver.message.ResponseStatusCode;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.server.authorization.AuthorizationException;
import org.apache.tinkerpop.gremlin.server.authorization.AuthorizationRequest;
import org.apache.tinkerpop.gremlin.server.authorization.Authorizer;
import org.apache.tinkerpop.gremlin.server.channel.NioChannelizer;
import org.apache.tinkerpop.gremlin.server.channel.WebSocketChannelizer;
import org.apache.tinkerpop.gremlin.server.util.GraphTraversalMappingUtil;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

import javax.script.Bindings;

import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;


import javax.script.CompiledScript;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal.Symbols.*;

/**
 * A SASL authorization handler that allows the {@link Authorizer} to be plugged into it. This handler is meant
 * to be used with protocols that process a {@link RequestMessage} such as the {@link WebSocketChannelizer}
 * or the {@link NioChannelizer}
 *
 * @author shekhar.bansal
 */
@ChannelHandler.Sharable
public class SaslAuthorizationHandler extends AbstractAuthorizationHandler {
    private static final Logger logger = LoggerFactory.getLogger(SaslAuthorizationHandler.class);

    protected final Settings.AuthorizationSettings authorizationSettings;

    private String[] writeStepsArray = {addV, addE, drop, property};
    private Set<String> writeStepsSet = new HashSet<>(Arrays.asList(writeStepsArray));

    private static final GremlinGroovyScriptEngine engine = new GremlinGroovyScriptEngine();
    private static Map<String, Bindings> graphBindingsMap = new HashMap<>();

    public SaslAuthorizationHandler(final Authorizer authorizer, final Settings.AuthorizationSettings authorizationSettings) {
        super(authorizer);
        this.authorizationSettings = authorizationSettings;
    }


    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {

        if (msg instanceof RequestMessage){
            final RequestMessage requestMessage = (RequestMessage) msg;

            final Attribute<RequestMessage> request = ((AttributeMap) ctx).attr(StateKey.REQUEST_MESSAGE);
            final Attribute<String> user = ((AttributeMap) ctx).attr(StateKey.AUTHENTICATED_USER);

            boolean hasWriteStep = false;
            Bytecode bytecode = null;
            String resource = null;

            if(requestMessage.getOp().equals(Tokens.OPS_EVAL)){
                //gremlin console request
                //Processor:session - if set while `remote connect`
                String query = (String) requestMessage.getArgs().get(Tokens.ARGS_GREMLIN);

                String traversalString = getGraphTraversalString(query);
                Bindings bindings = getGraphBinding(traversalString);

                CompiledScript compiledScript = engine.compile(query);
                Object traversalObject = compiledScript.eval(bindings);

                if(traversalObject instanceof Number || traversalObject instanceof String){
                    ctx.fireChannelRead(requestMessage);
                    return;
                } else if (traversalObject instanceof GraphTraversal.Admin){
                    bytecode = ((DefaultGraphTraversal) traversalObject).getBytecode();
                } else {
                    logger.warn("unrecognised traversal query:: {}, type:: {}", query, traversalObject.getClass());
                }

            } else if(requestMessage.getOp().equals(Tokens.OPS_BYTECODE)) {
                //java client request
                //Processor:traversal
                ObjectMapper mapper = GraphSONMapper.build().version(GraphSONVersion.V2_0).create().createMapper();

                final Object bytecodeObj = requestMessage.getArgs().get(Tokens.ARGS_GREMLIN);
                final Object aliasesObj = requestMessage.getArgs().get(Tokens.ARGS_ALIASES);

                if(aliasesObj instanceof Map){
                    Map<String, String> aliasesMap = ((Map) aliasesObj);
                    Iterator<Map.Entry<String, String>> entryIterator = aliasesMap.entrySet().iterator();
                    if(entryIterator.hasNext()){
                        String traversalName = entryIterator.next().getValue();
                        resource = GraphTraversalMappingUtil.getGraphName(traversalName);
                    }
                }

                bytecode = bytecodeObj instanceof Bytecode ? (Bytecode) bytecodeObj :
                        mapper.readValue(bytecodeObj.toString(), Bytecode.class);
            } else {
                logger.warn("method not supported " + requestMessage.getOp() + ", considering it as write request");
                hasWriteStep = true;
            }

            hasWriteStep = hasWriteStep || hasWriteStep(bytecode);

            try{
                authorize(user.get(), hasWriteStep, resource, ctx);
                ctx.fireChannelRead(requestMessage);
            } catch (AuthorizationException ae) {
                logger.info("returning 403");
                final ResponseMessage error = ResponseMessage.build(request.get())
                        .statusMessage(ae.getMessage())
                        .code(ResponseStatusCode.FORBIDDEN).create();
                ctx.writeAndFlush(error);
            }
        }
        else {
            logger.warn("{} only processes RequestMessage instances - received {} - channel closing",
                    this.getClass().getSimpleName(), msg.getClass());
            ctx.close();
        }
    }

    private boolean hasWriteStep(Bytecode bytecode) {
        if (bytecode !=null){
            for (Bytecode.Instruction instruction : bytecode.getStepInstructions()) {
                if(writeStepsSet.contains(instruction.getOperator())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Bindings getGraphBinding(String traversalString) {
        if(graphBindingsMap.containsKey(traversalString)){
            return graphBindingsMap.get(traversalString);
        } else {
            synchronized (SaslAuthorizationHandler.class){
                if(graphBindingsMap.containsKey(traversalString)){
                    return graphBindingsMap.get(traversalString);
                } else {
                    //final GremlinGroovyScriptEngine engine = new GremlinGroovyScriptEngine();
                    final Graph graph = EmptyGraph.instance();
                    final GraphTraversalSource g = graph.traversal();
                    final Bindings bindings = engine.createBindings();
                    bindings.put(traversalString, g);
                    graphBindingsMap.put(traversalString, bindings);
                    return bindings;
                }
            }
        }
    }

    private static final String DEFAULT_TRAVERSAL_STRING = "defaultgraph";
    private static String getGraphTraversalString(String query) {
        if(query.contains(".")){
            return query.substring(0, query.indexOf("."));
        } else {
            return DEFAULT_TRAVERSAL_STRING;
        }
    }

}
