package strategy;

import javafx.util.Pair;
import modele.*;

import java.lang.reflect.Array;
import java.util.ArrayList;

public interface negociationStrategy {

    public boolean arePotentialPartner(Prosumer p1, Prosumer p2) ;
    public void initializeNegociation(Prosumer p1);
    public void checkEnd(Prosumer p);
    public void setPowerSystem(PowerSystem powerSystem) ;
    public ArrayList<Pair<Prosumer, Double>> chooseSellerPartnerConcession(Prosumer prosumer);
    public ArrayList<Pair<Prosumer, Double>> chooseBuyerPartnerConcession(Prosumer prosumer);
    public void makeConcession(Prosumer prosumer, Prosumer otherProsumer, Double quantity);
    public ArrayList<Accord> getAccords() ;
}
