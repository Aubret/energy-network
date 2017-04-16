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
    private double tarifMax;
    private double tarifMin ;
    private PowerSystem powerSystem ;
    private DCSolver solver ;
    private ArrayList<Accord> accords ;
    private double approximation ;


    public zeuten(double tarifMin, double tarifMax, double approximation){
        this.tarifMax=tarifMax;
        this.tarifMin=tarifMin ;
        this.solver = new DCSolver() ;
        this.accords = new ArrayList<Accord>();
        this.approximation = approximation ;
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
     * Recommence négociation pour un partenariat
     * @param partner
     */
    private void startNegociation(Partner partner){
        double quantity = Math.min(Math.abs(partner.getBuyer().energyLeft()), Math.abs(partner.getSeller().energyLeft()));
        partner.setBuyerProposition(new Offer(this.tarifMin, quantity ));
        partner.setSellerProposition(new Offer(this.tarifMax, quantity));
        partner.setToConsider(false);
    }

    /**
     * Simulation  d'une offre pour le calcul des pertes  de congestion et de l'effet joule
     * le premier prosumer doit être celui qui recoit
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
        while( offer.getQuantity() + losses > otherProsumer.getEnergySend() +0.5 || congestion_costs == 0 ) {
            otherProsumer.setEnergySend(offer.getQuantity()+losses); // On augmente l'offre d'énergie
            this.solver.solve(this.powerSystem);
            HashMap<Link, Double> hm = this.solver.getMwFlows();
            losses = 0;
            congestion_costs = 0.001;
            for (Map.Entry<Link, Double> entry : hm.entrySet()) {
                losses += entry.getKey().getJoule() * entry.getValue() / 100; // Pertes sur chaque lien
                congestion_costs += 5 * entry.getValue() / entry.getKey().getCapacity() ;
                //System.out.println(entry.getValue()+" and lost : "+losses+" on the link between "+entry.getKey().getFirstNode().getId()+" and "+entry.getKey().getSecondNode().getId());
            }
        }
        offer.setLosses(losses);
        offer.setCongestion_cost(Math.abs(congestion_costs));
        offer.setTotalQuantity(otherProsumer.getEnergySend() +1);
        offer.setEvaluate(true);
        prosumer.setEnergyReceived(temp);
        otherProsumer.setEnergySend(temp2);
        //System.out.println("congestion in simulation : "+offer.getCongestion_cost());
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
    private double calculUtility(Prosumer prosumer, Prosumer otherProsumer, Offer offer){
        Partner partner = prosumer.getPartners().get(otherProsumer) ;
        //Offer offer ;
        if(prosumer.isBuyer()){
            //System.out.println(" el :"+Math.abs(prosumer.energyLeft())+"  q :"+offer.getQuantity()+" a: "+offer.getAmount());
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
    public void checkEnd(Prosumer p) {
        double mineUtility ;
        double hisUtility ;
        if(p.getPartners().isEmpty()){// Il a plus aucun partenaires, il arrete donc les négociations/
            p.stopNegociations();
        }
        boolean stop = false ;
        ArrayList<Prosumer> stopNegociationsProsumers = new ArrayList<Prosumer>();
        for( Map.Entry<Prosumer, Partner> ite: p.getPartners().entrySet()){
            Prosumer other = ite.getKey();
            Offer offer = p.isBuyer() ? ite.getValue().getSellerProposition() : ite.getValue().getBuyerProposition() ;
            //On remplit le taux de congestion et les pertes de l'offre en simulant sur le réseau
            boolean success = p.isBuyer() ? simulate (p, ite.getKey(), offer) : simulate(ite.getKey(), p, offer); // est-ce que le vendeur peut gérer les pertes
            mineUtility = this.calculUtility(p, other, offer) ;
            hisUtility = this.calculUtility(other, p, offer) ;
            System.out.println("compare utility : "+hisUtility+" <= "+mineUtility);
            if( hisUtility <= mineUtility ){ // Cas où on accepte l'accord
                System.out.println("The deal is accepted");
                Offer agreedOffer ;
                if(p.isBuyer()) {
                    agreedOffer = ite.getValue().getSellerProposition() ;
                    this.accords.add(new Accord( agreedOffer, ite.getValue()) ); // On ajoute l'accord
                    p.setEnergyReceived(p.getEnergyReceived() + agreedOffer.getQuantity());
                    other.setEnergySend(other.getEnergySend()+agreedOffer.getQuantity() +agreedOffer.getLosses());
                    this.startNegociation(ite.getValue());
                    if(other.energyLeft() <= approximation){ // L'autre n'a plus besoin de vendre
                        //other.stopNegociations();
                        stopNegociationsProsumers.add(other);
                    }
                    if( p.energyLeft() >= -approximation ) { // Pas besoin de continuer d'acheter
                        stop =true ;
                        break ;
                    }
                }else{
                    agreedOffer = ite.getValue().getBuyerProposition() ;
                    this.accords.add(new Accord( agreedOffer, ite.getValue()) ); // On ajoute l'accord
                    p.setEnergySend(p.getEnergyReceived() + agreedOffer.getQuantity());
                    other.setEnergyReceived(other.getEnergyReceived()+ agreedOffer.getQuantity());
                    this.startNegociation(ite.getValue());
                    if( other.energyLeft() >= -approximation){
                        stopNegociationsProsumers.add(other);
                        //other.stopNegociations();
                    }
                    if( p.energyLeft() <= approximation ) { // Pas besoin de continuer de vendre
                        stop = true ;
                        break ;
                    }
                }

            }
        }
        for(Prosumer prosumer : stopNegociationsProsumers){
            prosumer.stopNegociations();
        }
        if(stop)
            p.stopNegociations() ;
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
        for( int i =0 ; i<size ; i++ ){
            Offer offer = concessionsPartners.get(i).getSellerProposition() ;
            coeffs[i]= Double.valueOf(1000*(offer.getCongestion_cost() + offer.getAmount())).intValue() ;
            //System.out.println(1000+" * "+offer.getCongestion_cost()+" + "+offer.getAmount());
            qs[i] = model.intVar("q"+i, 0, Math.abs(Double.valueOf(1000*(concessionsPartners.get(i).getSeller().energyLeft())).intValue()));
        }
        model.scalar(qs, coeffs, "=", sum).post() ; // On effectue la somme des multiplications, on met la somme dans sum
        model.sum(qs, "=",  Math.abs(Double.valueOf(1000*(prosumer.energyLeft())).intValue())).post();
        model.setObjective(model.MINIMIZE, sum); // On veut minimiser sum
        Solver solver = model.getSolver();
        /*System.out.println(" coeff : "+coeffs[0]);
        System.out.println("qs1 : "+qs[0].getValue());
        System.out.println(" qsi max : "+Math.abs(Double.valueOf(1000*(concessionsPartners.get(0).getSeller().energyLeft()))));
        System.out.println(qs[0].getValue()+" = "+Math.abs(Double.valueOf(1000*(prosumer.energyLeft())).intValue()));
        System.out.println(qs[0].getValue()+" * "+coeffs[0]+" = "+sum);
        System.out.println("qs : "+Double.valueOf(qs[0].getValue())/1000);*/
        if(solver.solve()){
            //  System.out.println("sum : "+sum);
            for(int i=0; i< size ; i++){
                /*System.out.println(" coeff : "+coeffs[i]);
                System.out.println("qs1 : "+qs[i].getValue());
                System.out.println(" qsi max : "+Math.abs(Double.valueOf(1000*(concessionsPartners.get(i).getSeller().energyLeft()))));
                System.out.println(qs[i].getValue()+" = "+Math.abs(Double.valueOf(1000*(prosumer.energyLeft())).intValue()));
                System.out.println(qs[i].getValue()+" * "+coeffs[i]+" = "+sum);
                System.out.println("qs : "+Double.valueOf(qs[i].getValue())/1000);*/
                if(qs[i].getValue() > 0){ // On ajoute l'ensemble des partenaires au résultat avec la quantité
                    result.add(new Pair<Prosumer, Double>(concessionsPartners.get(i).getSeller(), Double.valueOf(qs[i].getValue())/1000 ));
                }
            }
        }else{
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
        Model model = new Model() ;
        ArrayList<Partner> concessionsPartners = this.getPossiblesPartners(prosumer);
        int size = concessionsPartners.size() ;
        if(size==0) {
            System.out.println("Aucune concession à faire");
            return null;
        }
        IntVar[] qb = new IntVar[size];
        int[] coeffs = new int[size];
        int[] coeffsLosses = new int[size];
        IntVar sum = model.intVar("sum", 0, 100000000);
        for( int i =0 ; i<size ; i++ ){
            Offer offer = concessionsPartners.get(i).getBuyerProposition() ;
            coeffsLosses[i] = Double.valueOf(1000+1000*offer.getLosses()).intValue() ;
            coeffs[i]= Double.valueOf(1000*(offer.getCongestion_cost() + offer.getAmount())).intValue() ;
            //System.out.println(" c : "+offer.getCongestion_cost()+" l : "+offer.getLosses()) ;
            qb[i] = model.intVar("q"+i, 0, Math.abs(Double.valueOf(1000*concessionsPartners.get(i).getBuyer().energyLeft()).intValue()));
        }
        model.scalar(qb, coeffs, "=", sum).post() ; // On effectue la somme des multiplications, on met la somme dans sum
        model.scalar(qb, coeffsLosses, "<=", Double.valueOf(1000000*prosumer.energyLeft()).intValue()).post();
        model.setObjective(model.MAXIMIZE, sum); // On veut maximiser sum
        Solver solver = model.getSolver();
        if(solver.solve()){
            //System.out.println("sum  : "+sum);
            for(int i=0; i< size ; i++){
                /*System.out.println("losses : "+coeffsLosses[i]);
                System.out.println(" coeff : "+coeffs[i]);
                System.out.println("qb1 : "+qb[i].getValue());
                System.out.println(qb[i].getValue()+"*"+coeffs[i]+" = "+sum);
                System.out.println(qb[i].getValue()+"*"+coeffsLosses[i]+"( "+qb[i].getValue()*coeffsLosses[i]+" ) <= "+Double.valueOf(1000000*prosumer.energyLeft()).intValue());
                System.out.println("qb : "+Double.valueOf(qb[i].getValue())/1000);*/
                if(qb[i].getValue() > 0) { // On ajoute l'ensemble des partenaires au résultat avec la quantité
                    result.add(new Pair<Prosumer, Double>(concessionsPartners.get(i).getBuyer(), Double.valueOf(qb[i].getValue())/1000));
                    //System.out.println("ajout d'une concession");
                }
            }
        }else{
            System.out.println("bug solving");
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
        if(prosumer.isBuyer()) {
            newTarif = (Double)partner.getBuyerData() + partner.getBuyerProposition().getAmount();
            partner.setBuyerProposition((new Offer(newTarif, quantity)));
            System.out.println("Buyer's new tarif is : "+newTarif+ " with quantity "+quantity);
        }else{
            /*double mineUtility = this.calculUtility(prosumer, otherProsumer, partner.getSellerProposition());// Sa proposition
            double hisUtility = this.calculUtility(prosumer, otherProsumer, partner.getBuyerProposition()); // Avec celle de l'autre
            double zeutenIndex = this.calculZeuthenIndex(mineUtility, hisUtility) ;*/
            newTarif = - (Double)partner.getSellerData() + partner.getSellerProposition().getAmount();
            partner.setSellerProposition((new Offer(newTarif, quantity)));
            System.out.println("Seller's new tarif is : "+newTarif+ " with quantity "+quantity);
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
            if(!ite.getValue().isToConsider()){
                ite.getValue().setToConsider(true);
                continue ;
            }
            if(!ite.getValue().getSellerProposition().isEvaluate())
                simulate(ite.getValue().getBuyer(),ite.getValue().getSeller(),ite.getValue().getSellerProposition());
            if(!ite.getValue().getBuyerProposition().isEvaluate())
                simulate(ite.getValue().getBuyer(),ite.getValue().getSeller(),ite.getValue().getBuyerProposition());



            double buyersUtility = calculUtility(ite.getValue().getBuyer(), ite.getValue().getSeller(),ite.getValue().getBuyerProposition());
            double buyersUtilityWithSellersOffer = calculUtility(ite.getValue().getBuyer(), ite.getValue().getSeller(),ite.getValue().getSellerProposition());
            Double zBuyer = this.calculZeuthenIndex(buyersUtility, buyersUtilityWithSellersOffer);
            ite.getValue().setBuyerData(zBuyer);

            simulate(ite.getValue().getBuyer(),ite.getValue().getSeller(),ite.getValue().getSellerProposition());
            double sellersUtility = calculUtility(ite.getValue().getSeller(), ite.getValue().getBuyer(),ite.getValue().getSellerProposition());
            double sellersUtilityWithBuyersOffer = calculUtility(ite.getValue().getSeller(), ite.getValue().getBuyer(),ite.getValue().getBuyerProposition());
            Double zSeller = this.calculZeuthenIndex(sellersUtility, sellersUtilityWithBuyersOffer);
            ite.getValue().setSellerData(zSeller);
            //System.out.println(buyersUtility+" "+buyersUtilityWithSellersOffer);
            System.out.println(" zbuyer : "+zBuyer+" zSeller : "+zSeller);
            if( ( zBuyer <= zSeller && prosumer.isBuyer() ) || ( zSeller <= zBuyer && !prosumer.isBuyer()))
                concessionsPartners.add(ite.getValue());

            /*if(prosumer.equals(ite.getValue().hasToOffer()) ){ // Si c'est à lui de faire une offre
                concessionsPartners.add(ite.getValue());
            }*/
        }
        return concessionsPartners ;
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
}
