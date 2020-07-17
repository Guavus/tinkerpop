package org.apache.tinkerpop.gremlin.server.util;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.server.GraphManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GraphTraversalMappingUtil {

  private static Map<String, String>  graphTraversalToNameMap = new ConcurrentHashMap<String, String>();

  public static void populateGraphTraversalToNameMapping(GraphManager graphManager){

    if(graphTraversalToNameMap.size() != 0){
      return;
    }

    Iterator<String> traversalSourceIterator = graphManager.getTraversalSourceNames().iterator();
    Map<String, String> graphNameTraversalMap = new HashMap<String, String>();
    while(traversalSourceIterator.hasNext()){
      String traversalSource = traversalSourceIterator.next();
      String currentGraphString = ( (GraphTraversalSource) graphManager.getAsBindings().get(traversalSource)).getGraph().toString();
      graphNameTraversalMap.put(currentGraphString, traversalSource);
    }

    Iterator<String> graphNamesIterator =  graphManager.getGraphNames().iterator();
    while(graphNamesIterator.hasNext()){
      String graphName = graphNamesIterator.next();
      String currentGraphString = graphManager.getGraph(graphName).toString();
      String traversalSource = graphNameTraversalMap.get(currentGraphString);
      graphTraversalToNameMap.put(traversalSource, graphName);
    }
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
