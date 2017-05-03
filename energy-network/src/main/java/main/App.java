package main;

import strategy.negociationStrategy;
import strategy.zeuten;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


public class App 
{
    public static void main( String[] args ) throws InterruptedException {
        int numberProsumer = 10 ;
        File describe = new File("src/main/resources/arbre"+numberProsumer+".xml") ;
        File result = new File("src/main/results/result-arbre"+numberProsumer+".dot") ;
        negociationStrategy strategy = new zeuten(0.01, 100, 0.01) ;
        EnergyNegociation energyNegociation=new EnergyNegociation(describe, strategy);
        energyNegociation.initNegociation();
        energyNegociation.printResult();
        energyNegociation.checkResult(result);
    }
}
