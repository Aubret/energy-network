package vue;

import modele.Link;
import modele.Node;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.ExportException;
import org.jgrapht.ext.IntegerComponentNameProvider;
import org.jgrapht.ext.StringComponentNameProvider;
import org.jgrapht.graph.*;

import java.io.File;
import java.util.List;

public class Graph {
    private SimpleGraph graph;

    public Graph(List<Node> nodes, List<Link> links) {
        //ClassBasedEdgeFactory factory = new ClassBasedEdgeFactory<Node, Link>(Link.class);
        this.graph = new SimpleGraph(Link.class);
        for (Node node : nodes) {
            this.graph.addVertex(node);
        }
        for (Link link : links) {
            double kwh = link.getCongestion() ;
            //if( kwh > 0) {
                this.graph.addEdge(link.getFirstNode(), link.getSecondNode(), link);
            /*}else{
                this.graph.addEdge(link.getSecondNode(),link.getFirstNode(), link);
            }*/
        }


    }

    public void genereGraph(File result) {
        DOTExporter exporter = new DOTExporter<Node, Link>(
                new StringComponentNameProvider<Node>(),
                new energyNodeProvider(),
                new StringComponentNameProvider<Link>()
        );
        try {
            exporter.exportGraph(this.graph, result);
        } catch (ExportException e) {
            e.printStackTrace();
        }
    }
}
