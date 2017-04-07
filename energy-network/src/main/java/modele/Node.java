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
        return true;
    }

    public double getBusMw() {
        return 0;
    }

    /**
     * Calcule du chemin entre deux noeuds
     * @param goal
     * @param alreadyChecked
     * @return
     */
    public ArrayList<Link> calculeWay(Node goal, ArrayList<Node> alreadyChecked){
        if(alreadyChecked.contains(this)){
            return null ;
        }
        if(goal.equal(this)){
            return new ArrayList<Link>() ;
        }
        ArrayList<Link> result=null ;
        alreadyChecked.add(this);
        int taille=this.links.size() ;
        int i = 0;
        while( i < taille && result == null){
            result = this.links.get(i).getFirstNode().calculeWay(goal, alreadyChecked);
            if(result == null )
                result = this.links.get(i).getSecondNode().calculeWay(goal, alreadyChecked);
            i++ ;
        }
        if ( result != null )
            result.add(this.links.get(i-1)) ;
        return result ;

    }

    public boolean equal(Node p){return p.getId() == this.getId() ;}

    public boolean equals(Node n){
        return this.getId() == n.getId() ;
    }

}
