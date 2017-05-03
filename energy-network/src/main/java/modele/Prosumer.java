package modele;

import javafx.util.Pair;
import strategy.negociationStrategy;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@XmlRootElement(name="prosumer")
public class Prosumer extends Node {
    private Double energyNeed ; // énergie nécessaire au prosumer
    private Double energyGenerated ; // énergie produite par le prosumer
    private boolean buyer ;// S'il achète ou vend
    private HashMap<Prosumer, Partner> partners ; // Tous ses partenaires
    private negociationStrategy strategy ;
    private double energy ; // Différence de l'énergie générée et nécessaire
    private double energyReceived ; // energy for power flow program
    private double energySend ;
    private double energyLost ;
    private boolean terminate ; // S'il a terminé


    public Prosumer(){
        this.setEnergyReceived(0);
        this.setEnergySend(0);
        this.setEnergyLost(0);
        this.partners = new HashMap<Prosumer, Partner>() ;
        this.setTerminate(false);
    }

    public boolean isSlackBus() {
        return false;
    }
    public double getBusMw() {
        return this.getEnergySend() - this.getEnergyReceived();
    }

    public void estimateState(){
        if(this.getEnergyNeed() == null)
            this.setEnergyNeed(15.0);
        if(this.getEnergyGenerated() == null ) {
            this.setEnergyGenerated(10.0 + Math.random() * 10);
        }
        this.setEnergy(getEnergyGenerated() - this.getEnergyNeed());
        if(this.getEnergyGenerated() < this.getEnergyNeed()){
            this.setBuyer(true);
        }else{
            this.setBuyer(false);
        }
    }

    /**
     * Recherche de partenaires
     * @param prosumers
     */
    public void lookForPartners(ArrayList<Prosumer> prosumers){
        this.getPartners().clear();
        for(Prosumer prosumer : prosumers){
            if(prosumer  != this) {// Il ne se regarde pas lui même
                if (!getPartners().containsKey(prosumer)) { // S'ils le sont pas déjà
                    if (strategy.arePotentialPartner(this, prosumer)) { // Si la stratégie les veut partenaires
                        Partner newPartner ;
                        if(this.buyer) {
                            newPartner = new Partner(this, prosumer);
                        }else{
                            newPartner = new Partner(prosumer, this);
                        }

                        getPartners().put(prosumer, newPartner);
                        prosumer.getPartners().put(this, newPartner);
                    }
                }
            }
        }
    }

    public boolean ChoosePartnersConcession(){
        if(this.isBuyer()){
            System.out.println("\nbuyer -- need "+this.energyLeft());
        }else {
            System.out.println("\nseller -- need "+this.energyLeft());
        }
        if(!this.strategy.checkEnd(this)) {
            System.out.println("Negociations restart");
            return true; // redémarrage des négociations
        }
        if(this.isTerminate()){
            System.out.println("nothing else to do");
        }else{
            System.out.println("Look for concessions");
            ArrayList<Pair<Prosumer, Double>> choose ;
            choose = this.isBuyer() ? strategy.chooseSellerPartnerConcession(this) : strategy.chooseBuyerPartnerConcession(this) ;// ON choisit les partenaires
            if(choose != null) {
                System.out.println(this.getId()+" make concessions à "+choose.size()+" prosumers");
                for (Pair<Prosumer, Double> ite : choose) {
                    strategy.makeConcession(this, ite.getKey(), ite.getValue()); // On fait les concessions
                }
            }

        }
        return false ;


    }

    public void sendFirstProposals(){
        strategy.initializeNegociation(this);
    }

    /**
     * still need if < 0, to send if > 0
     * @return
     */
    public double energyLeft(){
        //System.out.println("energyleft :"+this.getEnergy() +" "+ this.getEnergySend() +" "+ this.getEnergyReceived());
        return this.getEnergy() - this.getEnergySend() + this.getEnergyReceived() - this.getEnergyLost();
    }

    /**
     * Met fin aux négociations avec les participants
     * @return
     */
    public void stopNegociations(){
        System.out.println("stop négociations");
        for( Map.Entry<Prosumer, Partner> ite : this.getPartners().entrySet()){
            ite.getKey().getPartners().remove(this) ;
        }
        this.setTerminate(true);
    }

    @XmlTransient
    public negociationStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(negociationStrategy strategy) {
        this.strategy = strategy;
    }

    public HashMap<Prosumer, Partner> getPartners() {
        return partners;
    }

    public boolean isBuyer() {
        return buyer;
    }

    public void setBuyer(boolean buyer) {
        this.buyer = buyer;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public double getEnergyReceived() {
        return energyReceived;
    }

    public void setEnergyReceived(double energyReceived) {
        this.energyReceived = energyReceived;
    }

    public double getEnergySend() {
        return energySend;
    }

    public void setEnergySend(double energySend) {
        this.energySend = energySend;
    }

    public Double getEnergyNeed() {
        return energyNeed;
    }

    @XmlElement(name="energyNeed",nillable = true)
    public void setEnergyNeed(Double energyNeed) {
        this.energyNeed = energyNeed;
    }

    public Double getEnergyGenerated() {
        return energyGenerated;
    }

    @XmlElement(name="energyGenerated",nillable = true)
    public void setEnergyGenerated(Double energyGenerated) {
        this.energyGenerated = energyGenerated;
    }

    public void changeGenerateEnergy(){
        this.setEnergyGenerated(10+Math.random()*10);
        this.setEnergy(getEnergyGenerated() - this.getEnergyNeed());
    }

    public boolean isTerminate() {
        return terminate;
    }

    public void setTerminate(boolean terminate) {
        this.terminate = terminate;
    }

    public double getEnergyLost() {
        return energyLost;
    }

    public void setEnergyLost(double energyLost) {
        this.energyLost = energyLost;
    }
}
