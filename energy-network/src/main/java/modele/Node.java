package modele;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import snl.ccss.jpowerflow.dc.DCPFBus;

@XmlRootElement(name="node")
public class Node implements DCPFBus {

    private int id ;

    private List<Link> links ;

    public Node(){
        this.links= new ArrayList<Link>();
    }

    public int getId() {
        return id;
    }

    @XmlAttribute(name="id")
    public void setId(int id) {
        this.id = id;
    }

    public List<Link> getLinks() {
        return links;
    }

    @XmlElement(name="link")
    public void setLinks(List<Link> links) {
        this.links = links;

    }

    public void adjustLinksSecondNode(){
        for( Link l : this.links) {
            if(l.getSecondNode() == null ) {
                if(l.getFirstNode() != this ) {
                    l.setSecondNode(this);
                }
            }
        }
    }


    public void addLink(Link link){
        this.links.add(link);
    }

    public int getNumber() {
        return this.id;
    }

    public boolean isSlackBus() {
        return false;
    }

    public double getBusMw() {
        return 0;
    }

    public boolean equal(Node p){return p.getId() == this.getId() ;}

    public boolean equals(Node n){
        return this.getId() == n.getId() ;
    }

    public String toString(){
        return Integer.toString(this.getId());
    }

}
