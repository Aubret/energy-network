package main;

import modele.Link;
import modele.Node;
import modele.PowerSystem;
import modele.Prosumer;
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

    public EnergyNegociation(File init, negociationStrategy strategy){
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
        boolean seller =false ;
        boolean buyer = false ;
        while( !seller || ! buyer || this.prosumers.get(0).isBuyer()) {// On veut qu'il y ait au moins un vendeur et un acheteur
            // est-ce que le prosumer est acheteur ou vendeur
            seller = false ;
            buyer= false ;
            for (Prosumer prosumer : this.prosumers) {
                prosumer.changeGenerateEnergy(); // Pour tester, à enlever
                prosumer.estimateState();
                seller=!prosumer.isBuyer() || seller;
                buyer=prosumer.isBuyer() || buyer ;

            }
        }
        //Recherche des partenaires de négociation
        for ( Prosumer prosumer : this.prosumers){
            prosumer.lookForPartners(this.prosumers);
        }

        //Envoi des premières offrs
        for ( Prosumer prosumer : this.prosumers){
            prosumer.sendFirstProposals();
        }
        System.out.println("Fin d'envoi des premières offres");
        while(this.stillCanNegociate()) { // On répète les concessions jusqu'à la fin
            for (Prosumer prosumer : this.prosumers) {
                prosumer.ChoosePartnersConcession();
            }
            System.out.println("Fin d'une étape de concession");
        }
        System.out.println("fin du programme");
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
                System.out.println(prosumer.getId());
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
