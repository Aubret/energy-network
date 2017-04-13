package modele;

import javafx.util.Pair;
import strategy.negociationStrategy;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@XmlRootElement(name="prosumer")
public class Prosumer extends Node {
    private double energyNeed ;
    private double energyGenerated ;
    private boolean buyer ;
    private HashMap<Prosumer, Partner> partners ;
    private negociationStrategy strategy ;
    private double energy ;
    private double energyReceived ; // energy for power flow program
    private double energySend ;
    private boolean terminate ; // S'il a terminé


    public Prosumer(){
        this.setEnergyNeed(15);
        this.setEnergyGenerated(10+Math.random()*10);
        this.setEnergyReceived(0);
        this.setEnergySend(0);
        this.setEnergy(getEnergyGenerated() - this.getEnergyNeed());
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

    public void ChoosePartnersConcession(){
        if(this.isBuyer()){
            System.out.println("\nbuyer -- need "+this.energyLeft());
        }else {
            System.out.println("\nseller -- need "+this.energyLeft());
        }
        this.strategy.checkEnd(this);
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
        return this.getEnergy() - this.getEnergySend() + this.getEnergyReceived();
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

    public double getEnergyNeed() {
        return energyNeed;
    }

    public void setEnergyNeed(double energyNeed) {
        this.energyNeed = energyNeed;
    }

    public double getEnergyGenerated() {
        return energyGenerated;
    }

    public void setEnergyGenerated(double energyGenerated) {
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
}
