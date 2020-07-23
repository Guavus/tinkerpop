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
import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.server.authorization.AuthorizationException;
import org.apache.tinkerpop.gremlin.server.authorization.AuthorizationRequest;
import org.apache.tinkerpop.gremlin.server.authorization.Authorizer;
import org.apache.tinkerpop.gremlin.server.util.GraphTraversalMappingUtil;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal.Symbols.*;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal.Symbols.property;

public abstract class AbstractAuthorizationHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(AbstractAuthorizationHandler.class);
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
        authorizationRequest.setResource(GraphTraversalMappingUtil.getGraphName(resource));

        if(hasWriteStep){
            authorizationRequest.setAccessType(AuthorizationRequest.AccessType.WRITE);
        } else{
            authorizationRequest.setAccessType(AuthorizationRequest.AccessType.READ);
        }
        authorizationRequest.setAction(authorizationRequest.getAccessType().name());

        boolean isAccessAllowed = authorizer.isAccessAllowed(authorizationRequest);

        if(!isAccessAllowed) {
            throw new AuthorizationException("Action ["+authorizationRequest.getAccessType().name()
                    +"] not allowed for user ["+authorizationRequest.getUser()
                    +"] on graph [" +authorizationRequest.getResource()
                    +"]");
        }
    }

    private static final GremlinGroovyScriptEngine engine = new GremlinGroovyScriptEngine();
    private static Map<String, Bindings> graphBindingsMap = new HashMap<>();
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
    protected static String getGraphTraversalString(String query) {
        if(query.contains(".")){
            return query.substring(0, query.indexOf("."));
        } else {
            return DEFAULT_TRAVERSAL_STRING;
        }
    }

    private InetAddress getRemoteInetAddress(final ChannelHandlerContext ctx) {
        final Channel channel = ctx.channel();
        if (null == channel) return null;

        final SocketAddress genericSocketAddr = channel.remoteAddress();
        if (null == genericSocketAddr || !(genericSocketAddr instanceof InetSocketAddress)) return null;

        return ((InetSocketAddress)genericSocketAddr).getAddress();
    }

    private static final String[] writeStepsArray = {addV, addE, drop, property};
    private static final Set<String> writeStepsSet = new HashSet<>(Arrays.asList(writeStepsArray));
    protected boolean hasWriteStep(Bytecode bytecode) {
        if (bytecode !=null){
            for (Bytecode.Instruction instruction : bytecode.getStepInstructions()) {
                if(writeStepsSet.contains(instruction.getOperator())) {
                    return true;
                }
            }
        }
        return false;
    }
    protected Object getTraversalObjectFromQuery(String query, String traversalString, boolean supressMalformedRequestException)
            throws ScriptException {
        try {
            Bindings bindings = getGraphBinding(traversalString);
            CompiledScript compiledScript = engine.compile(removeTrailingNext(query));
            return compiledScript.eval(bindings);
        } catch (ScriptException ex) {
            logger.error("Error parsing query:: {}", ex);

            if(ex.getCause() instanceof groovy.lang.MissingMethodException) {
                return "MissingMethod";
            }
            if(supressMalformedRequestException) {
                return null;
            }
            throw ex;
        }
    }
    private String removeTrailingNext(String query) {
        if(query.trim().endsWith(";")){
            query = query.trim().substring(0, query.length()-1);
        }
        if(query.trim().endsWith("next()")) {
            return query.trim().substring(0, query.length()-7);
        }
        return query;
    }
}
