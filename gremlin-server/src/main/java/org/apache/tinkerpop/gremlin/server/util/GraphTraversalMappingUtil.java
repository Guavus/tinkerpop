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
package org.apache.tinkerpop.gremlin.server.util;

import org.apache.tinkerpop.gremlin.server.GraphManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for obtaining graph traversal name to graph name
 *
 * @author shekhar.bansal
 */
public class GraphTraversalMappingUtil {

  private static final Logger logger = LoggerFactory.getLogger(GraphTraversalMappingUtil.class);

  private static Map<String, String>  graphTraversalToNameMap = new ConcurrentHashMap<String, String>();

  public static void populateGraphTraversalToNameMapping(GraphManager graphManager){

    if(graphTraversalToNameMap.size() != 0){
      return;
    }

    Iterator<String> traversalSourceIterator = graphManager.getTraversalSourceNames().iterator();

    Map<StorageBackendKey, String> storageKeyToTraversalMap = new HashMap<StorageBackendKey, String>();
    while(traversalSourceIterator.hasNext()){
      String traversalSource = traversalSourceIterator.next();
      StorageBackendKey key = new StorageBackendKey(
              graphManager.getTraversalSource(traversalSource).getGraph().configuration());
      storageKeyToTraversalMap.put(key, traversalSource);
    }

    Iterator<String> graphNamesIterator =  graphManager.getGraphNames().iterator();
    while(graphNamesIterator.hasNext()) {
      String graphName = graphNamesIterator.next();
      StorageBackendKey key = new StorageBackendKey(
              graphManager.getGraph(graphName).configuration());
      graphTraversalToNameMap.put(storageKeyToTraversalMap.get(key), graphName);
    }

    logger.info("graphTraversalToNameMap:: {}" + graphTraversalToNameMap);
  }

  public static String getGraphName(String graphTraversalName){
    if(graphTraversalToNameMap.size() == 0){
      return null;
    } else if (graphTraversalToNameMap.size() == 1) {
      return graphTraversalToNameMap.entrySet().iterator().next().getValue();
    } else {
      return graphTraversalToNameMap.get(graphTraversalName);
    }
  }

}
