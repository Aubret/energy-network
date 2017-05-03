package modele;

import java.util.List;

public class Slack extends Node {

    public Slack(Node root, List<Link> links , List<Node> bus ){
        super() ;
        this.setId(0) ;
        Link l = new Link() ;
        l.setCongestion(0);
        l.setFirstNode(this);
        l.setSecondNode(root);
        l.setCapacity(1000);
        l.setJoule(2);
        this.addLink(l);
        links.add(l);
        bus.add(this);
    }
    public boolean isSlackBus() {
        return true;
    }

}
