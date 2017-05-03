package vue;

import modele.Node;
import modele.Prosumer;
import modele.Slack;
import org.jgrapht.ext.ComponentNameProvider;

public class energyNodeProvider implements ComponentNameProvider {
    public String getName(Object o) {
        Node n = (Node)o ;
        if(o instanceof Slack)
            return "Slack" ;
        if(o instanceof Prosumer) {
            Prosumer p = (Prosumer) o;
            return n.getId() + ")  " + (double) Math.round(100 * ( p.getEnergySend()+p.getEnergyLost()-p.getEnergyReceived())) / 100 + " | " + (double)Math.round(100*p.getEnergy())/100;
        }
        return ""+n.getId();
    }
}
