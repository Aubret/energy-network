package strategy;

import javafx.util.Pair;
import modele.Partner;
import modele.PowerSystem;
import modele.Prosumer;

import java.util.ArrayList;

public interface negociationStrategy {

    public boolean arePotentialPartner(Prosumer p1, Prosumer p2) ;
    public void initializeNegociation(Prosumer p1);
    public void checkEnd(Prosumer p);
    public void setPowerSystem(PowerSystem powerSystem) ;
    public ArrayList<Pair<Prosumer, Integer>> chooseSellerPartnerConcession(Prosumer prosumer);
    public ArrayList<Pair<Prosumer, Integer>> chooseBuyerPartnerConcession(Prosumer prosumer);
    public void makeConcession(Prosumer prosumer, Prosumer otherProsumer, int quantity);
}
