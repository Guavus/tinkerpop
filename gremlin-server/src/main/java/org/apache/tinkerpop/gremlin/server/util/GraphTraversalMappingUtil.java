package org.apache.tinkerpop.gremlin.server.util;

import org.apache.tinkerpop.gremlin.server.GraphManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
