package main;

import modele.*;
import snl.ccss.jpowerflow.dc.DCSolver;
import strategy.negociationStrategy;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnergyNegociation {

    private DCSolver solver ;
    private PowerSystem powerSystem ;
    private List<Link> links ; // all the links
    private List<Node> bus ; // all the nodes
    private ArrayList<Prosumer> prosumers ;
    private HashMap<Link, Double> hm ;
    private negociationStrategy strategy ;

    public EnergyNegociation(File init, negociationStrategy strategy){
        this.strategy = strategy ;
        this.prosumers =new ArrayList<Prosumer>();
        Node root = new Node();
        try {
            JAXBContext jc = JAXBContext.newInstance(Node.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            root = (Node) unmarshaller.unmarshal(init);
        }catch (JAXBException e) {
            e.printStackTrace();
            System.exit(0);
        }
        this.powerSystem = new PowerSystem() ;
        this.createLists(root);
        strategy.setPowerSystem(this.powerSystem);
        this.links=this.powerSystem.getEnergizedBranchList();
        this.bus=this.powerSystem.getBusList() ;
        for( Prosumer prosumer : this.prosumers){
            prosumer.setStrategy(strategy);
        }
        /*this.solver=new DCSolver();
        this.solver.solve(this.powerSystem) ;
        hm=this.solver.getMwFlows();
        for (Map.Entry<Link, Double> entry : hm.entrySet()) {
            System.out.println(entry.getKey().getCapacity());
            System.out.println(entry.getValue());
        }*/
        /*ArrayList<Link> res = this.prosumers.get(0).calculeWay(this.prosumers.get(5), new ArrayList()) ;
        for(Link l : res){
            System.out.println(l.getCapacity());
        }*/


    }

    private void createLists(Node node){
        if(node instanceof Prosumer)
            this.prosumers.add((Prosumer)node);
        this.powerSystem.addBus(node);
        node.adjustLinksSecondNode(); // On ajoute les nodes parents de chaque lien, le fichier xml est hiérarchisé mais on veut pas qu'il le soit
        for(Link l : node.getLinks()){
            if(! this.powerSystem.getEnergizedBranchList().contains(l)) {
                this.powerSystem.addLink(l);
                this.createLists(l.getFirstNode());
            }
        }
    }

    public void initNegociation(){
        /*boolean seller =false ;
        boolean buyer = false ;
        double energyquantity = -1 ;
        while( !seller || ! buyer || this.prosumers.get(0).isBuyer() || energyquantity < 0) {// On veut qu'il y ait au moins un vendeur et un acheteur
            // est-ce que le prosumer est acheteur ou vendeur
            seller = false ;
            buyer= false ;
            energyquantity = 0;
            for (Prosumer prosumer : this.prosumers) {
                prosumer.changeGenerateEnergy(); // Pour tester, à enlever
                prosumer.estimateState();
                seller=!prosumer.isBuyer() || seller;
                buyer=prosumer.isBuyer() || buyer ;
                energyquantity += prosumer.getEnergy() ;

            }
        }*/
        // ON détermine si le prosumer est un vendeur ou acheteur
        for (Prosumer prosumer : this.prosumers) {
            prosumer.estimateState();
            System.out.println(prosumer.getEnergy());
        }
        //Recherche des partenaires de négociation
        for ( Prosumer prosumer : this.prosumers){
            prosumer.lookForPartners(this.prosumers);
        }

        //Envoi des premières offrs
        for ( Prosumer prosumer : this.prosumers){
            prosumer.sendFirstProposals();
        }
        System.out.println("Fin d'envoi des premières offres\n");
        int i=0 ;
        ArrayList<Prosumer> prosumerFinished = new ArrayList<Prosumer>();
        while(this.stillCanNegociate() && i < 10) { // On répète les concessions jusqu'à la fin
            for(Prosumer prosumer : this.prosumers){ // ON récupéère tous les prosumers qui ont terminé de négocier
                if( prosumer.isTerminate() ){
                    prosumerFinished.add(prosumer);
                }
            }
            this.prosumers.removeAll(prosumerFinished); // On enlève des prosumers négociants tous ceux qui ont terminé

            for (Prosumer prosumer : this.prosumers) {
                prosumer.ChoosePartnersConcession();
            }
            i++ ;
            System.out.println("Fin d'une étape de concession--------------------------------------- \n");
        }
        System.out.println("fin du programme");
    }

    /**
     * Affichage des accords
     */
    public void printResult(){
        ArrayList<Accord> accords = strategy.getAccords() ;
        for( Accord accord : accords){
            Offer offer = accord.getOffer() ;
            Partner partner = accord.getPartner() ;
            System.out.println(partner.getBuyer().getId()+" achète "+offer.getQuantity()+" pour "+offer.getAmount()+" à "+partner.getSeller().getId());
        }
    }

    public void checkResult(){
        double energyToSold = 0 ;
        double energyToBuy = 0 ;
        for(Prosumer prosumer : this.prosumers){
            if (prosumer.isBuyer()){
                energyToBuy += prosumer.energyLeft() ;
            }else{
                energyToSold += prosumer.energyLeft() ;
            }
        }
        System.out.println("\n Energie restante à vendre : "+energyToSold);
        System.out.println("Energie restante à acheter : "+energyToBuy);
    }

    /**
     * On regarde si le programme est terminé, il continue s'il y a encore au moins un buyer et un seller
     * @return
     */
    private boolean stillCanNegociate(){
        boolean buyer = false ;
        boolean seller = false ;
        for(Prosumer prosumer : this.prosumers){
            if(!prosumer.isTerminate()){
                if(prosumer.isBuyer()){
                    buyer = true ;
                }else{
                    seller = true ;
                }
            }
        }
        return buyer && seller ;
    }
}
