package modele;


import snl.ccss.jpowerflow.PFPowerSystem;

import java.util.ArrayList;
import java.util.List;

public class PowerSystem implements PFPowerSystem {
    private List<Link> links ;
    private List<Node> bus ;

    public PowerSystem(){
        super() ;
        this.links = new ArrayList<Link>() ;
        this.bus = new ArrayList<Node>() ;
    }

    public void addLink(Link link){
        this.links.add(link);
    }

    public void addBus(Node bus){
        this.bus.add(bus);
    }

    public List getBusList() {
        return bus;
    }

    public List getEnergizedBranchList() {
        return links;
    }
}
