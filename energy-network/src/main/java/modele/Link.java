package modele;

import snl.ccss.jpowerflow.dc.DCPFBranch;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="link")
public class Link implements DCPFBranch{

    private int congestion ;
    private int capacity ;
    private int joule ;
    @XmlTransient
    private double susceptance ;

    private Node firstNode ;

    @XmlTransient
    private Node secondNode;

    public Link(){
        this.susceptance = 0.5 ;
    }


    public int getFromBus() {
        return firstNode.getId() ;
    }

    public int getToBus() {
        return secondNode.getId();
    }

    public double getBPrime() {
        return this.susceptance;
    }

    public int getCongestion() {
        return congestion;
    }

    @XmlElement(name="congestion")
    public void setCongestion(int congestion) {
        this.congestion = congestion;
    }

    public int getCapacity() {
        return capacity;
    }

    @XmlElement(name="capacity")
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getJoule() {
        return joule;
    }

    @XmlElement(name="joule")
    public void setJoule(int joule) {
        this.joule = joule;
    }

    public Node getFirstNode() {
        return firstNode;
    }


    @XmlElements({
            @XmlElement(name="node", type=Node.class),
            @XmlElement(name="prosumer", type=Prosumer.class)
    })
    public void setFirstNode(Node firstNode) {
        this.firstNode = firstNode;
        firstNode.addLink(this);
    }

    public Node getSecondNode() {
        return secondNode;
    }

    public void setSecondNode(Node secondNode) {
        this.secondNode = secondNode;
    }

}
