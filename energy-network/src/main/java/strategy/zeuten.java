package strategy;

import javafx.util.Pair;
import modele.*;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import snl.ccss.jpowerflow.dc.DCSolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.chocosolver.solver.search.strategy.Search.activityBasedSearch;
import static org.chocosolver.solver.search.strategy.Search.setVarSearch;
// Doute sur le calcule du zeuten index !

public class zeuten implements negociationStrategy {
    private double tarifMax;
    private double tarifMin ;
    private PowerSystem powerSystem ;
    private DCSolver solver ;
    private ArrayList<Accord> accords ;
    private double approximation ;
    private List<Prosumer> prosumers ;
    private double losses ;
    private double congestion_costs;

    private Integer approximation2 ;

    public zeuten(double tarifMin, double tarifMax, double approximation){
        this.tarifMax=tarifMax;
        this.tarifMin=tarifMin ;
        this.solver = new DCSolver() ;
        this.accords = new ArrayList<Accord>();
        this.approximation = approximation ;
        this.approximation2 = (Double.valueOf(1/approximation)).intValue();
        this.losses=0 ;
        this.congestion_costs=0 ;
    }

    public void setProsumers(List<Prosumer> prosumers) {
        this.prosumers = prosumers ;
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
        ArrayList<Partner> partnersToStop = new ArrayList<Partner>();
        for(Map.Entry<Prosumer, Partner> partnerKey : prosumer.getPartners().entrySet()){
            double quantity = Math.min(Math.abs(prosumer.energyLeft()), Math.abs(partnerKey.getKey().energyLeft()));
            if(this.makeOffer(prosumer, partnerKey.getKey(), quantity, prosumer.isBuyer() ? this.tarifMin : this.tarifMax)){
                partnersToStop.add(partnerKey.getValue());
            }
        }
        for(Partner partner : partnersToStop){
            this.stopPartnership(partner);
        }
    }

    private void stopPartnership(Partner partner){
        Prosumer buyer = partner.getBuyer();
        Prosumer seller= partner.getSeller();
        buyer.getPartners().remove(seller);
        seller.getPartners().remove(buyer);
    }

    /**
     * Recommence négociation pour tous les partenariats
     */
    private void startNegociation(){
        for(Prosumer prosumer : this.prosumers){
            this.initializeNegociation(prosumer);
        }
        /*double quantity = Math.min(Math.abs(partner.getBuyer().energyLeft()), Math.abs(partner.getSeller().energyLeft()));
        this.makeOffer(partner.getBuyer(), partner.getSeller(), quantity, this.tarifMin);
        this.makeOffer(partner.getSeller(), partner.getBuyer(), quantity, this.tarifMax);

        //partner.setBuyerProposition(new Offer(this.tarifMin, quantity ));
        //partner.setSellerProposition(new Offer(this.tarifMax, quantity));
        partner.setToConsider(false);*/
    }


    /**
     * On doit assurer qu'il y a assez d'énergie à envoyer avec les pertes
     * @param proposer
     * @param receiver
     * @param quantity
     * @param tarif
     */
    private boolean makeOffer(Prosumer proposer, Prosumer receiver, double quantity, double tarif){
        Offer offer= new Offer(tarif, quantity);
        if(proposer.isBuyer()){
            while(!simulate(proposer, receiver, offer ) && quantity > 0){
                quantity -= Math.max(quantity/10, 0.1 ) ;
                offer.setQuantity(quantity);
            }
            if(quantity > 0 /* && this.checkCongestionCost(offer )*/){
                proposer.getPartners().get(receiver).setBuyerProposition(offer); // Mise à jour de l'offre
                return false ;
            }
        }else{
            while(!simulate(receiver, proposer, offer ) && quantity > 0){
                quantity -= Math.max(quantity/10, 0.1 )  ;
                offer.setQuantity(quantity);
            }
            if(quantity > 0 /*&& this.checkCongestionCost(offer )*/){
                proposer.getPartners().get(receiver).setSellerProposition(offer);// Mise à jour de l'offre
                return false;
            }
        }
        System.out.println("L'offre n'est pas bonne");
        return true ;
    }


    /**
     * Permet d'avoir un apercu de l'état des pertes et des couts de congestion actuellement sur le réseau
     */
    private void majLosses(){
        this.solver.solve(this.powerSystem);
        double losses =0 ;
        double congestion_costs = 0;
        for( Accord accord : this.accords){
            Offer offer = accord.getOffer() ;
            Partner partner = accord.getPartner() ;
            losses += offer.getLosses();
            congestion_costs+= offer.getCongestion_cost() ;

        }
        this.losses = losses ;
        this.congestion_costs = congestion_costs ;
    }

    /**
     * Simulation  d'une offre pour le calcul des pertes  de congestion et de l'effet joule
     * le premier prosumer doit être celui qui recoit, l'acheteur
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
        double newFlow=0 ;

        prosumer.setEnergyReceived(offer.getQuantity());
        while(temp2+ offer.getQuantity() + losses > otherProsumer.getEnergySend() +0.1 || congestion_costs == 0 ) {
            otherProsumer.setEnergySend(temp2+offer.getQuantity()+losses); // On augmente l'offre d'énergie
            this.solver.solve(this.powerSystem);
            HashMap<Link, Double> hm = this.solver.getMwFlows();
            losses = 0;
            congestion_costs = this.approximation;
            for (Map.Entry<Link, Double> entry : hm.entrySet()) {
                double flow = Math.abs(entry.getValue()) ;
                losses += entry.getKey().getJoule() * flow / 100; // Pertes sur chaque lien
                congestion_costs += 2 * flow/ entry.getKey().getCapacity() ;
                if(entry.getKey().getCapacity() ==0) // error in the xml file
                    System.out.println("ID : "+entry.getKey().getFirstNode().getId());
            }
            losses=losses-this.losses ;
            congestion_costs = congestion_costs-this.congestion_costs ;
        }
        offer.setLosses(losses);
        offer.setCongestion_cost(congestion_costs);
        offer.setEvaluate(true);
        double checkPossibilityEnergy = otherProsumer.getEnergySend()+otherProsumer.getEnergyLost();
        prosumer.setEnergyReceived(temp);
        otherProsumer.setEnergySend(temp2);
        //System.out.println("check possibility :"+checkPossibilityEnergy+" > "+otherProsumer.getEnergy());
        if(checkPossibilityEnergy > otherProsumer.getEnergy()){
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
    private double calculUtility(Prosumer prosumer, Prosumer otherProsumer, Offer offer){
        Partner partner = prosumer.getPartners().get(otherProsumer) ;
        //Offer offer ;
        if(prosumer.isBuyer()){
            //System.out.println(" el :"+Math.abs(prosumer.energyLeft())+"  q :"+offer.getQuantity()+" a: "+offer.getAmount()+ " c: "+offer.getCongestion_cost());
            double ut= Math.exp(-(Math.abs(prosumer.energyLeft())
                    - offer.getQuantity()))
                    / (offer.getAmount() + offer.getCongestion_cost());
            return ut ;
        }else {
            //System.out.println(" el :"+Math.abs(prosumer.energyLeft())+"  q :"+offer.getQuantity()+" a: "+offer.getAmount()+" c : "+offer.getCongestion_cost());
            double ut =  Math.exp(-(Math.abs(prosumer.energyLeft())
                    - offer.getQuantity()
                    + offer.getLosses())) * (offer.getAmount() - offer.getCongestion_cost());
            return ut ;
        }
    }

    /**
     * Vérifie si un prosumer doit encore chercher ou vendre de l'énergie
     * @param p
     */
    public int checkEnd(Prosumer p) {
        double mineUtility ;
        double hisUtility ;
        double mineUtility2 ;
        double hisUtility2;
        if(p.getPartners().isEmpty()){// Il a plus aucun partenaires, il arrete donc les négociations/
            //p.stopNegociations();
            return 0 ;
        }
        boolean stop = false ;
        boolean restart = false ;
        ArrayList<Prosumer> stopNegociationsProsumers = new ArrayList<Prosumer>();
        for( Map.Entry<Prosumer, Partner> ite: p.getPartners().entrySet()){
            Prosumer other = ite.getKey();
            //u b (x b ) ≤ u s (x b )
            Offer offer = p.isBuyer() ? ite.getValue().getSellerProposition() : ite.getValue().getBuyerProposition() ;
            //On remplit le taux de congestion et les pertes de l'offre en simulant sur le réseau
            //boolean success = p.isBuyer() ? simulate (p, ite.getKey(), offer) : simulate(ite.getKey(), p, offer); // est-ce que le vendeur peut gérer les pertes
            mineUtility = this.calculUtility(p, other, offer) ;
            hisUtility = this.calculUtility(other, p, offer) ;
            // u s (x s ) ≤ u b (x s ),
            Offer offer2 = !p.isBuyer() ? ite.getValue().getSellerProposition() : ite.getValue().getBuyerProposition() ;
            //On remplit le taux de congestion et les pertes de l'offre en simulant sur le réseau
            //boolean success2 = !p.isBuyer() ? simulate (p, ite.getKey(), offer2) : simulate(ite.getKey(), p, offer2); // est-ce que le vendeur peut gérer les pertes
            hisUtility2 = this.calculUtility(p, other, offer2) ;
            mineUtility2 = this.calculUtility(other, p, offer2) ;

            System.out.println("compare utility : "+hisUtility+" <= "+mineUtility+" || "+hisUtility2+" <= "+mineUtility2);
            if( hisUtility <= mineUtility || hisUtility2 <= mineUtility2 ){ // Cas où on accepte l'accord
                System.out.println("The deal is accepted");
                restart = true ;
                Offer agreedOffer ;
                if(p.isBuyer()) {
                    agreedOffer = ite.getValue().getSellerProposition() ;
                    this.accords.add(new Accord( agreedOffer, ite.getValue()) ); // On ajoute l'accord
                    p.setEnergyReceived(p.getEnergyReceived() + agreedOffer.getQuantity());
                    other.setEnergySend(other.getEnergySend()+agreedOffer.getQuantity());
                    other.setEnergyLost(other.getEnergyLost() +agreedOffer.getLosses());
                    if(other.energyLeft() <= approximation){ // L'autre n'a plus besoin de vendre
                        stopNegociationsProsumers.add(other);
                    }
                    if( p.energyLeft() >= -approximation ) { // Pas besoin de continuer d'acheter
                        stop =true ;
                    }
                }else{
                    agreedOffer = ite.getValue().getBuyerProposition() ;
                    this.accords.add(new Accord( agreedOffer, ite.getValue()) ); // On ajoute l'accord
                    p.setEnergySend(p.getEnergySend() + agreedOffer.getQuantity());
                    p.setEnergyLost(p.getEnergyLost() +agreedOffer.getLosses());
                    other.setEnergyReceived(other.getEnergyReceived()+ agreedOffer.getQuantity());
                    if( other.energyLeft() >= -approximation){
                        stopNegociationsProsumers.add(other);
                    }
                    if( p.energyLeft() <= approximation ) { // Pas besoin de continuer de vendre
                        stop = true ;
                    }
                }
                this.majLosses();
                break ;
            }
        }
        for(Prosumer prosumer : stopNegociationsProsumers){
            prosumer.stopNegociations();
        }
        if(stop)
            p.stopNegociations() ;
        if(restart)
            return 1 ;
        return 2 ;
    }

    /**
     * Choix de partenaires avec résolution de programme de contrainte
     * @return
     */
    public ArrayList<Pair<Prosumer, Double>> chooseSellerPartnerConcession(Prosumer prosumer) {
        ArrayList<Pair<Prosumer, Double>> result = new ArrayList();
        Model model = new Model("seller choice") ;
        ArrayList<Partner> concessionsPartners = this.getPossiblesPartners(prosumer);
        int size = concessionsPartners.size() ;
        if(size==0) {
            System.out.println("aucune concession à faire");
            return null;
        }
        IntVar[] qs = new IntVar[size];
        int[] coeffs = new int[size];
        IntVar sum = model.intVar("sum", 0,2000000000);
        long test = 0 ;
        for( int i =0 ; i<size ; i++ ){
            Offer offer = concessionsPartners.get(i).getSellerProposition() ;
            coeffs[i]= Double.valueOf(this.approximation2*(offer.getCongestion_cost() + offer.getAmount())).intValue() ;
            //System.out.println(1000+" * "+offer.getCongestion_cost()+" + "+offer.getAmount());
            test+=coeffs[i]*Math.abs(Double.valueOf(this.approximation2*(concessionsPartners.get(i).getSeller().energyLeft())).intValue());
            qs[i] = model.intVar("q"+i, 0, Math.abs(Double.valueOf(this.approximation2*(concessionsPartners.get(i).getSeller().energyLeft())).intValue()));
        }
        IntVar energy = model.intVar(Math.abs(Double.valueOf(this.approximation2*(prosumer.energyLeft())).intValue()));
        model.scalar(qs, coeffs, "=", sum).post() ; // On effectue la somme des multiplications, on met la somme dans sum
        model.sum(qs, "=", energy ).post();
        model.setObjective(model.MINIMIZE, sum); // On veut minimiser sum
        Solver solver = model.getSolver();
        /*solver.setSearch(Search.minDomLBSearch(qs));
        solver.showStatistics();*/
        //System.out.println("TEST : "+test+"    "+ (test < 2000000000));
        if(solver.solve()){
            //System.out.println(model);
            //  System.out.println("sum : "+sum);
            for(int i=0; i< size ; i++){
                if(qs[i].getValue() > 0){ // On ajoute l'ensemble des partenaires au résultat avec la quantité
                    result.add(new Pair<Prosumer, Double>(concessionsPartners.get(i).getSeller(), Double.valueOf(qs[i].getValue())/this.approximation2 ));
                }
            }
        }else{
            System.out.println(model);
            //solver.printFeatures();
            System.out.println("bug solving");
            return null ;
        }
        return result ;
    }

    /**
     * Programme linéaire du vendeur
     * @param prosumer
     * @return
     */
    public ArrayList<Pair<Prosumer, Double>> chooseBuyerPartnerConcession(Prosumer prosumer){
        ArrayList<Pair<Prosumer, Double>> result = new ArrayList();
        Model model = new Model("buyer choice") ;
        ArrayList<Partner> concessionsPartners = this.getPossiblesPartners(prosumer);
        int size = concessionsPartners.size() ;
        if(size==0) {
            System.out.println("Aucune concession à faire");
            return null;
        }
        IntVar[] qb = new IntVar[size];
        int[] coeffs = new int[size];
        int[] coeffsLosses = new int[size];
        IntVar sum = model.intVar("sum", 0, 2000000000);
        for( int i =0 ; i<size ; i++ ){
            Offer offer = concessionsPartners.get(i).getBuyerProposition() ;
            coeffsLosses[i] = Double.valueOf(this.approximation2+this.approximation2*offer.getLosses()).intValue() ;
            coeffs[i]= Double.valueOf(this.approximation2*(offer.getCongestion_cost() + offer.getAmount())).intValue() ;
            //System.out.println(" c : "+offer.getCongestion_cost()+" l : "+offer.getLosses()) ;
            qb[i] = model.intVar("q"+i, 0, Math.abs(Double.valueOf(this.approximation2*concessionsPartners.get(i).getBuyer().energyLeft()).intValue()));
        }
        model.scalar(qb, coeffs, "=", sum).post() ; // On effectue la somme des multiplications, on met la somme dans sum
        model.scalar(qb, coeffsLosses, "<=", Double.valueOf(this.approximation2*this.approximation2*prosumer.energyLeft()).intValue()).post();
        model.setObjective(model.MAXIMIZE, sum); // On veut maximiser sum
        Solver solver = model.getSolver();
        if(solver.solve()){
            //System.out.println("sum  : "+sum);
            for(int i=0; i< size ; i++){
                if(qb[i].getValue() > 0) { // On ajoute l'ensemble des partenaires au résultat avec la quantité
                    result.add(new Pair<Prosumer, Double>(concessionsPartners.get(i).getBuyer(), Double.valueOf(qb[i].getValue())/this.approximation2));
                    //System.out.println("ajout d'une concession");
                }
            }
        }else{
            System.out.println(model);
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
    public void makeConcession(Prosumer prosumer, Prosumer otherProsumer, Double quantity){
        Partner partner = prosumer.getPartners().get(otherProsumer) ;
        Double newTarif ;
        Double newQuantity ;
        if(prosumer.isBuyer()) {
            newTarif = (Double)partner.getBuyerData() + partner.getBuyerProposition().getAmount();
            newQuantity = Math.min(quantity, partner.getSellerProposition().getQuantity());
            if(this.makeOffer(prosumer, otherProsumer, quantity, newTarif))
                this.stopPartnership(prosumer.getPartners().get(otherProsumer));
            //partner.setBuyerProposition((new Offer(newTarif, newQuantity)));
            partner.getSellerProposition().setQuantity(newQuantity);
            System.out.println("Buyer's new tarif is : "+newTarif+ " with quantity "+newQuantity);
        }else{
            newTarif = - (Double)partner.getSellerData() + partner.getSellerProposition().getAmount();
            newQuantity = Math.min(quantity, partner.getBuyerProposition().getQuantity());
            if(this.makeOffer(otherProsumer, prosumer, quantity, newTarif))
                this.stopPartnership(prosumer.getPartners().get(otherProsumer));

            //partner.setSellerProposition((new Offer(newTarif, newQuantity)));
            partner.getBuyerProposition().setQuantity(newQuantity);
            System.out.println("Seller's new tarif is : "+newTarif+ " with quantity "+newQuantity);
        }
        if( newTarif < this.tarifMin || newTarif > this.tarifMax){
            System.out.println("LE TARIF EST MAUVAIS "+newTarif);
        }
    }


    private ArrayList<Partner> getPossiblesPartners(Prosumer prosumer){
        ArrayList<Partner> concessionsPartners = new ArrayList<Partner>() ;
        for (Map.Entry<Prosumer, Partner> ite : prosumer.getPartners().entrySet()) {
            if( ite.getKey().isBuyer() && ite.getKey().energyLeft() >= 0 )
                continue ;
            if(!ite.getKey().isBuyer() && ite.getKey().energyLeft() <= 0)
                continue ;

            double buyersUtility = calculUtility(ite.getValue().getBuyer(), ite.getValue().getSeller(),ite.getValue().getBuyerProposition());
            double buyersUtilityWithSellersOffer = calculUtility(ite.getValue().getBuyer(), ite.getValue().getSeller(),ite.getValue().getSellerProposition());
            Double zBuyer = this.calculZeuthenIndex(buyersUtility, buyersUtilityWithSellersOffer);
            ite.getValue().setBuyerData(zBuyer);

            double sellersUtility = calculUtility(ite.getValue().getSeller(), ite.getValue().getBuyer(),ite.getValue().getSellerProposition());
            double sellersUtilityWithBuyersOffer = calculUtility(ite.getValue().getSeller(), ite.getValue().getBuyer(),ite.getValue().getBuyerProposition());
            Double zSeller = this.calculZeuthenIndex(sellersUtility, sellersUtilityWithBuyersOffer);
            ite.getValue().setSellerData(zSeller);
            //System.out.println("q : "+ite.getValue().getSellerProposition().getQuantity()+"  "+ite.getValue().getBuyerProposition().getQuantity());
            System.out.println(buyersUtility+" "+buyersUtilityWithSellersOffer);
            System.out.println(" zbuyer : "+zBuyer+" zSeller : "+zSeller);

            if( ( zBuyer <= zSeller && prosumer.isBuyer() ) || ( zSeller <= zBuyer && !prosumer.isBuyer()))
                concessionsPartners.add(ite.getValue());
        }
        return concessionsPartners ;
    }

    private boolean checkCongestionCost(Offer offer){
        return offer.getCongestion_cost() < offer.getAmount() ;
    }

    private double calculZeuthenIndex(double utilityHisOffer, double utilityOtherOffer){
        return ( utilityHisOffer - utilityOtherOffer ) / utilityHisOffer ;
    }


    public PowerSystem getPowerSystem() {
        return powerSystem;
    }

    public void setPowerSystem(PowerSystem powerSystem) {
        this.powerSystem = powerSystem;
    }

    public ArrayList<Accord> getAccords(){
        return this.accords ;
    }

    public void afterEnd(){
        this.solver.solve(this.powerSystem);
        HashMap<Link, Double> hm = this.solver.getMwFlows();
        for (Map.Entry<Link, Double> entry : hm.entrySet()) {
            entry.getKey().setCongestion(entry.getValue());
        }
    }
}
