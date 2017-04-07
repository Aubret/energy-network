package strategy;

import javafx.util.Pair;
import modele.*;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import snl.ccss.jpowerflow.dc.DCSolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
// Doute sur le calcule du zeuten index !

public class zeuten implements negociationStrategy {
    private int tarifMax;
    private int tarifMin ;
    private PowerSystem powerSystem ;
    private DCSolver solver ;


    public zeuten(int tarifMin, int tarifMax){
        this.tarifMax=tarifMax;
        this.tarifMin=tarifMin ;
        this.solver = new DCSolver() ;
    }

    /**
     * Est-ce que deux prosumer peuvent être partenaires
     * @param p1
     * @param p2
     * @return
     */
    public boolean arePotentialPartner(Prosumer p1, Prosumer p2) {
        if( p1.isBuyer() == p2.isBuyer()){
            return false ;
        }
        return true;
    }

    /**
     * Premières propositions
     * @param prosumer
     */
    public void initializeNegociation(Prosumer prosumer){
        for(Map.Entry<Prosumer, Partner> partnerKey : prosumer.getPartners().entrySet()){
            double quantity = Math.min(Math.abs(prosumer.getEnergy()), Math.abs(partnerKey.getKey().getEnergy()));
            if(prosumer.isBuyer()){
                partnerKey.getValue().setBuyerProposition(new Offer(this.tarifMin, quantity ));
            }else{
                partnerKey.getValue().setSellerProposition(new Offer(this.tarifMax, quantity));
            }
        }
    }

    /**
     * Simulation  d'une offre pour le calcul des pertes  de congestion et de l'effet joule
     * @param prosumer
     * @param otherProsumer
     * @param offer
     * @return
     */
    private boolean simulate(Prosumer prosumer, Prosumer otherProsumer, Offer offer){
        double temp = prosumer.getEnergyReceived() ;
        double temp2 = otherProsumer.getEnergySend() ;
        double losses=0 ;
        double congestion_costs=0 ;
        prosumer.setEnergyReceived(offer.getQuantity());
        while( offer.getQuantity() + losses > otherProsumer.getEnergySend() +1 ) {
            otherProsumer.setEnergySend(offer.getQuantity()+losses); // On augmente l'offre d'énergie
            this.solver.solve(this.powerSystem);
            HashMap<Link, Double> hm = this.solver.getMwFlows();
            losses = 0;
            congestion_costs = 0;
            for (Map.Entry<Link, Double> entry : hm.entrySet()) {
                losses += entry.getKey().getJoule() * entry.getValue() / 100; // Pertes sur chaque lien
                congestion_costs += 5 * entry.getValue() / entry.getKey().getCapacity() ;
                System.out.println(entry.getValue()+" and lost : "+losses+" on the link between "+entry.getKey().getFirstNode().getId()+" and "+entry.getKey().getSecondNode().getId());
            }
        }
        offer.setLosses(losses);
        offer.setCongestion_cost(congestion_costs);
        offer.setTotalQuantity(otherProsumer.getEnergySend() +1);
        prosumer.setEnergyReceived(temp);
        otherProsumer.setEnergySend(temp2);
        if(otherProsumer.getEnergySend() > otherProsumer.getEnergy()){
            return false ; // le prosumer n'a pas assez d'énergie en prenant en compte les pertes
        }
        return true ;
    }

    /**
     * Calcule ed l'utilité d'une offre pour un prosumer
     * @param prosumer
     * @param otherProsumer
     * @return
     */
    private double calculUtility(Prosumer prosumer, Prosumer otherProsumer){
        Partner partner = prosumer.getPartners().get(otherProsumer) ;
        Offer offer ;
        if(prosumer.isBuyer()){
            offer = partner.getSellerProposition() ;
            if(simulate (prosumer, otherProsumer, offer) ){
                return Math.exp(-(partner.getBuyerProposition().getQuantity()
                        - partner.getSellerProposition().getQuantity()))
                        / (partner.getSellerProposition().getAmount() + offer.getCongestion_cost()); // couts de congestion à 0
            }
        }else {
            offer = partner.getBuyerProposition();
            if (simulate(otherProsumer, prosumer, offer)) {
                return Math.exp(-(partner.getSellerProposition().getQuantity()
                        - partner.getBuyerProposition().getQuantity()
                        + offer.getLosses())) * (partner.getBuyerProposition().getAmount() - offer.getCongestion_cost());
            }
        }
        return 0 ;

    }

    /**
     * Vérifie si un prosumer doit encore chercher ou vendre de l'énergie
     * @param p
     */
    public void checkEnd(Prosumer p) {
        double mineUtility ;
        double hisUtility ;
        if( (p.isBuyer() && p.energyLeft() >0 ) || (!p.isBuyer() && p.energyLeft() < 0 )  ){
            p.stopNegociations(); // Un interlocuteur a accepté son offre et il n'a donc plus rien
        }
        for( Map.Entry<Prosumer, Partner> ite: p.getPartners().entrySet()){
            Prosumer other = ite.getKey();
            mineUtility = this.calculUtility(p, other) ;
            hisUtility = this.calculUtility(other, p) ;
            System.out.println(mineUtility+" > "+hisUtility);
            if(mineUtility > hisUtility && hisUtility != 0){
                System.out.println("on est d'accord");
                if(p.isBuyer()) {
                    Offer agreedOffer = ite.getValue().getSellerProposition() ;
                    p.setEnergyReceived(p.getEnergyReceived() + agreedOffer.getQuantity());
                    other.setEnergySend(other.getEnergySend()+agreedOffer.getQuantity());
                    agreedOffer.setEvaluate(true);
                    if( p.energyLeft() >= 0 ) // Pas besoin de continuer d'acheter
                        p.stopNegociations();
                        return ;
                }else{
                    Offer agreedOffer = ite.getValue().getBuyerProposition() ;
                    p.setEnergySend(p.getEnergyReceived() + agreedOffer.getQuantity());
                    other.setEnergyReceived(other.getEnergyReceived()+ agreedOffer.getQuantity());
                    agreedOffer.setEvaluate(true);
                    if( p.energyLeft() <= 0 ) { // Pas besoin de continuer de vendre
                        p.stopNegociations() ;
                        return;
                    }
                }
            }

        }
    }

    /**
     * Choix de partenaires avec résolution de programme de contrainte
     * @return
     */
    public ArrayList<Pair<Prosumer, Integer>> chooseSellerPartnerConcession(Prosumer prosumer) {
        ArrayList<Pair<Prosumer, Integer>> result = new ArrayList();
        Model model = new Model("seller choice") ;
        ArrayList<Partner> concessionsPartners = this.getPossiblesPartners(prosumer);
        int size = concessionsPartners.size() ;
        if(size==0)
            return null ;
        IntVar[] qs = new IntVar[size];
        int[] coeffs = new int[size];
        IntVar sum = model.intVar("sum", 0,10000);
        for( int i =0 ; i<size ; i++ ){
            Offer offer = concessionsPartners.get(i).getSellerProposition() ;
            coeffs[i]= Double.valueOf(offer.getCongestion_cost() + offer.getAmount()).intValue() ;
            qs[i] = model.intVar("q"+i, 0, Math.abs(Double.valueOf(concessionsPartners.get(i).getSeller().getEnergy()).intValue()));
        }
        model.scalar(qs, coeffs, "=", sum).post() ; // On effectue la somme des multiplications, on met la somme dans sum
        model.sum(qs, "=",  Math.abs(Double.valueOf(prosumer.energyLeft()).intValue())).post();
        model.setObjective(model.MINIMIZE, sum); // On veut minimiser sum
        Solver solver = model.getSolver();
        if(solver.solve()){
            for(int i=0; i< size ; i++){
                if(qs[i].getValue() > 0) // On ajoute l'ensemble des partenaires au résultat avec la quantité
                    result.add(new Pair<Prosumer, Integer>(concessionsPartners.get(i).getSeller(), qs[i].getValue() ));
            }
            System.out.println("somme is : "+sum.getValue());
        }else{
            return null ;
        }
        return result ;
    }

    /**
     * Programme linéaire du vendeur
     * @param prosumer
     * @return
     */
    public ArrayList<Pair<Prosumer, Integer>> chooseBuyerPartnerConcession(Prosumer prosumer){
        ArrayList<Pair<Prosumer, Integer>> result = new ArrayList();
        Model model = new Model() ;
        ArrayList<Partner> concessionsPartners = this.getPossiblesPartners(prosumer);
        int size = concessionsPartners.size() ;
        if(size==0)
            return null ;
        IntVar[] qb = new IntVar[size];
        int[] coeffs = new int[size];
        int[] coeffsLosses = new int[size];
        IntVar sum = model.intVar("sum", 0, 10000);
        System.out.println(size);
        for( int i =0 ; i<size ; i++ ){
            Offer offer = concessionsPartners.get(i).getBuyerProposition() ;
            coeffsLosses[i] = 1+Double.valueOf(offer.getLosses()).intValue() ;
            coeffs[i]= Double.valueOf(offer.getCongestion_cost() + offer.getAmount()).intValue() ;
            qb[i] = model.intVar("q"+i, 0, Math.abs(Double.valueOf(concessionsPartners.get(i).getBuyer().getEnergy()).intValue()));
        }
        model.scalar(qb, coeffs, "=", sum).post() ; // On effectue la somme des multiplications, on met la somme dans sum
        model.scalar(qb, coeffsLosses, "<=", Double.valueOf(prosumer.energyLeft()).intValue()).post();
        model.setObjective(model.MAXIMIZE, sum); // On veut maximiser sum
        Solver solver = model.getSolver();
        if(solver.solve()){
            for(int i=0; i< size ; i++){
                if(qb[i].getValue() > 0) // On ajoute l'ensemble des partenaires au résultat avec la quantité
                    result.add(new Pair<Prosumer, Integer>(concessionsPartners.get(i).getBuyer(), qb[i].getValue() ));
            }
        }else{
            return null ;
        }
        return result ;
    }

    /**
     * Permet de faire une concession
     * @param prosumer
     * @param otherProsumer
     * @param quantity
     */
    public void makeConcession(Prosumer prosumer, Prosumer otherProsumer, int quantity){
        double mineUtility = this.calculUtility(prosumer, otherProsumer);
        double hisUtility = this.calculUtility(otherProsumer, prosumer);
        double zeutenIndex = (hisUtility - mineUtility)/hisUtility ;
        if(prosumer.isBuyer()) {
            Partner partner = prosumer.getPartners().get(otherProsumer) ;
            int newTarif = Double.valueOf(zeutenIndex + partner.getBuyerProposition().getAmount()).intValue();
            partner.setBuyerProposition((new Offer(newTarif, quantity)));
            System.out.println("new tarif is : "+newTarif);
        }else{
            Partner partner = otherProsumer.getPartners().get(otherProsumer) ;
            int newTarif = Double.valueOf(zeutenIndex + partner.getSellerProposition().getAmount()).intValue();
            partner.setSellerProposition((new Offer(newTarif, quantity)));
            System.out.println("new tarif is : "+newTarif);
        }
    }


    private ArrayList<Partner> getPossiblesPartners(Prosumer prosumer){
        ArrayList<Partner> concessionsPartners = new ArrayList<Partner>() ;
        for (Map.Entry<Prosumer, Partner> ite : prosumer.getPartners().entrySet()) {
            if(prosumer.equals(ite.getValue().hasToOffer()) ){ // Si c'est à lui de faire une offre
                concessionsPartners.add(ite.getValue());
            }
        }
        return concessionsPartners ;
    }

    public PowerSystem getPowerSystem() {
        return powerSystem;
    }

    public void setPowerSystem(PowerSystem powerSystem) {
        this.powerSystem = powerSystem;
    }
}
