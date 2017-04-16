package main;

import strategy.negociationStrategy;
import strategy.zeuten;

import java.io.File;


public class App 
{
    public static void main( String[] args )
    {
        File describe = new File("src/main/resources/arbre6.xml") ;
        negociationStrategy strategy = new zeuten(0, 100, 0.001) ;
        EnergyNegociation energyNegociation=new EnergyNegociation(describe, strategy);
        energyNegociation.initNegociation();
        energyNegociation.printResult();
        energyNegociation.checkResult();

    }
}
