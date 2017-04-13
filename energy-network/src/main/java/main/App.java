package main;

import strategy.negociationStrategy;
import strategy.zeuten;

import java.io.File;


public class App 
{
    public static void main( String[] args )
    {
        File describe = new File("src/main/resources/arbre2.xml") ;
        negociationStrategy strategy = new zeuten(0, 200, 0.001) ;
        EnergyNegociation energyNegociation=new EnergyNegociation(describe, strategy);
        energyNegociation.initNegociation();

    }
}
