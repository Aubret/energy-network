package main;

import modele.*;
import snl.ccss.jpowerflow.dc.DCSolver;
import strategy.negociationStrategy;
import vue.Graph;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnergyNegociation {

    private PowerSystem powerSystem ;
    private List<Link> links ; // all the links
    private List<Node> bus ; // all the nodes
    private ArrayList<Prosumer> prosumers ;
    private ArrayList<Prosumer> initProsumers ;
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
        Slack s = new Slack(root, this.links , this.bus) ; // On met le slack pour que powerflow fonctionne bien

        for( Prosumer prosumer : this.prosumers){
            prosumer.setStrategy(strategy);
        }
        initProsumers =  new ArrayList<Prosumer>(this.prosumers);

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
        strategy.setProsumers(this.prosumers); // On lui donne l'ensemble des prosumers.

        // ON détermine si le prosumer est un vendeur ou acheteur
        for (Prosumer prosumer : this.prosumers) {
            prosumer.estimateState();
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
        boolean noEnd=false ;
        while(this.stillCanNegociate() && i < 100) { // On répète les concessions jusqu'à la fin
            for(Prosumer prosumer : this.prosumers){ // ON récupéère tous les prosumers qui ont terminé de négocier
                if( prosumer.isTerminate() ){
                    prosumerFinished.add(prosumer);
                }
            }
            this.prosumers.removeAll(prosumerFinished); // On enlève des prosumers négociants tous ceux qui ont terminé
            if(noEnd) {
                //Recherche des partenaires de négociation
                for ( Prosumer prosumer : this.prosumers){
                    prosumer.lookForPartners(this.prosumers);
                }
                System.out.println("send the first proposals");
                //Envoi des premières offrs
                for (Prosumer prosumer : this.prosumers) {
                    prosumer.sendFirstProposals();
                }
                noEnd=false ;
            }
            System.out.println("concession the partners");
            for(Prosumer prosumer : this.prosumers) {
                noEnd = prosumer.ChoosePartnersConcession(); // Si les négociations ont redémarré, on veut renvoyer les propositions initiales
                if (noEnd) {
                    break;
                }
            }
            i++ ;
            System.out.println("Fin d'une étape de concession--------------------------------------- \n");
        }
        System.out.println("fin du programme : "+i);
        strategy.afterEnd();
    }

    /**
     * Affichage des accords
     */
    public void printResult(){
        ArrayList<Accord> accords = strategy.getAccords() ;
        double losses = 0;
        double congestions_costs = 0;
        for( Accord accord : accords){
            Offer offer = accord.getOffer() ;
            Partner partner = accord.getPartner() ;
            System.out.println(partner.getBuyer().getId()+" achète "+offer.getQuantity()+" pour "+offer.getAmount()+" à "+partner.getSeller().getId()+" avec "+partner.getSeller().getEnergyLost()+" pertes.");
            losses += offer.getLosses();
            congestions_costs+= offer.getCongestion_cost() ;

        }
        System.out.println("Les pertes sont de "+losses+" et la congestion coûte "+congestions_costs);
    }

    /**
     * Vérification des résultats
     */
    public void checkResult(File result){
        double energyToSold = 0 ;
        double energyToBuy = 0 ;
        double energysend = 0 ;
        double energyreceived = 0 ;
        double losses = 0;
        for(Prosumer prosumer : this.initProsumers){
            if (prosumer.isBuyer()){
                energyToBuy += prosumer.energyLeft() ;
                energyreceived +=prosumer.getEnergyReceived();
            }else{
                energyToSold += prosumer.energyLeft() ;
                energysend += prosumer.getEnergySend()+prosumer.getEnergyLost();
                losses+=prosumer.getEnergyLost() ;

            }
        }
        System.out.println("\n Energie restante à vendre : "+energyToSold);
        System.out.println("Energie restante à acheter : "+energyToBuy);
        System.out.println("Energie envoyée : "+energysend);
        System.out.println("Energie recue : "+energyreceived);
        System.out.println("Pourcentage de pertes : "+100*losses/energysend);
        System.out.println(this.links.size()+ " liens");
        Graph g = new Graph(this.bus, this.links) ;
        g.genereGraph(result);
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
