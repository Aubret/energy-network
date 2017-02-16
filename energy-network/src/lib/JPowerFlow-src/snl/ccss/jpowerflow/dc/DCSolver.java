/*
 * Copyright (2006) Sandia Corporation.
 * Under the terms of Contract DE-AC04-94AL85000 with Sandia Corporation,
 * the U.S. Government retains certain rights in this software.
 * 
 * Contact: Bryan T. Richardson, Sandia National Laboratories, btricha@sandia.gov
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

package snl.ccss.jpowerflow.dc;

import java.util.HashMap;
import java.util.List;

import flanagan.math.Matrix;
import snl.ccss.jpowerflow.AbstractPFSolver;
import snl.ccss.jpowerflow.PFPowerSystem;

/**
 * This class holds all the methods necessary for
 * computing DC power flow on a power system.
 * 
 * @author Bryan T. Richardson - Sandia National Laboratories
 */
public class DCSolver 
	   extends
	   AbstractPFSolver {
	
	private List busList;
	private List branchList;
	
	private int busCount;
	private int branchCount;
	
	private double[][] bBus;
	private double[] angle;
	private HashMap mwFlows;
	private double slackOutput;
	

	/**
	 * Default constructor for DCSolver class.
	 */
	public DCSolver() { }
	
	/**
	 * Main method for performing a DC power flow analysis.
	 * This method builds the bBus matrix for the system and
	 * calculates real power flows on all branches in the
	 * system.
	 * 
	 * @param powerSystem PFPowerSystem that implements the required getBusList and getEnergizedBranchList methods
	 * @return true if solver was successful (in this case, always true)
	 */
	public boolean solve(PFPowerSystem powerSystem) {
		busList = powerSystem.getBusList();
		branchList = powerSystem.getEnergizedBranchList();
		
		busCount = busList.size();
		branchCount = branchList.size();
		
		if (DEBUG) {
			System.out.println();
			System.out.println("Number of buses = " + busCount);
			System.out.println("Number of branches = " + branchCount);
			System.out.println();
		}
		
		/*
		 * bBus matrix is built
		 */
		bBus = new double[busCount][busCount];
		buildBBus();
		
		if (DEBUG) {
			System.out.println();
			System.out.println("B-Bus Matrix:");
			for (int i = 0; i < busCount; i++) {
				for (int j = 0; j < busCount; j++) {
					System.out.print(bBus[i][j] + "  ");
				}
				System.out.println();
			}
			System.out.println();
		}
		
		/*
		 * Certain rows and columns in the bBus matrix are zeroed out to ensure
		 * the system swing bus maintains a voltage value of unity.
		 */
		zeroBBus();
		
		double[] power = new double[busCount];
		for (int i = 0; i < busCount; i++) {
			DCPFBus bus = (DCPFBus)busList.get(i);
			power[i] = bus.getBusMw();
		}
		
		/*
		 * Bus voltage angle values are calculated using Gaussian elimination and back substitution
		 */
		Matrix matrixBBus = new Matrix(bBus);
		angle = matrixBBus.solveLinearSet(power);
		
		/*
		 * Line flows for each branch in the system are calculated
		 */
		mwFlows = new HashMap();
		calculateMwFlows();
		
		return true;
	}
	
	/*
	 * This method uses data from each branch object to create bBus for the entire system.
	 */
	private void buildBBus() {
		for (int i = 0; i < branchCount; i++) {
			DCPFBranch branch = (DCPFBranch)branchList.get(i);
			int fromBus = branch.getFromBus();
			int toBus = branch.getToBus();
			int from = -1;
			int to = -1;
			
			for (int j = 0; j < busCount; j++) {
				DCPFBus bus = (DCPFBus)busList.get(j);
				int busNumber = bus.getNumber();
				if (fromBus == busNumber)
					from = j;
				else if (toBus == busNumber)
					to = j;
			}
			
			/*
			 * In some programs, the user/coder may wish to make it possible for buses and branches to be disabled.
			 * There should be a filter method of some sort when getting a list of bus and branch objects to send to 
			 * this simulation so that only enabled objects are present in the lists.  The "-1" check is here incase
			 * a bus on one or both ends of a branch is disabled, but the branch itself is still enabled.
			 */
			if (from != -1 && to != -1) {
				double bPrime = branch.getBPrime();
				bBus[from][to] = bPrime;
								
				bBus[to][from] = bPrime;
				
				bPrime = bBus[from][from];
				bBus[from][from] = (bPrime + -branch.getBPrime());
				
				bPrime = bBus[to][to];
				bBus[to][to] = (bPrime + -branch.getBPrime());
			}
		}
	}
	
	/*
	 * This method zeroes out rows and colums of the Jacobian matrix that correspond to
	 * the system swing bus.
	 */
	private void zeroBBus() {
		for (int i = 0; i < busCount; i++) {
			DCPFBus bus = (DCPFBus)busList.get(i);
			if (bus.isSlackBus()) {
				for (int j = 0; j < busCount; j++) {
					bBus[i][j] = 0;
					bBus[j][i] = 0;
					bBus[i][i] = 1e+10;
				}
			}
		}
	}

	/*
	 * This method calculates the flow of real power across each branch in the system.  It also calculates
	 * the total amount of real power leaving the system swing bus.
	 */
	private void calculateMwFlows() {
		double[] busOutput = new double[busCount];
		int slackBusIndex = -1;
		for (int i = 0; i < branchCount; i++) {
			DCPFBranch branch = (DCPFBranch)branchList.get(i);
			int fromBus = branch.getFromBus();
			int toBus = branch.getToBus();
			int from = -1;
			int to = -1;
			
			for (int j = 0; j < busCount; j++) {
				DCPFBus bus = (DCPFBus)busList.get(j);
				if (slackBusIndex == -1)
					if (bus.isSlackBus())
						slackBusIndex = j;
				int busNumber = bus.getNumber();
				if (fromBus == busNumber)
					from = j;
				else if (toBus == busNumber)
					to = j;
			}
			
			/*
			 * In some programs, the user/coder may wish to make it possible for busList and branchList to be disabled.
			 * There should be a filter method of some sort when getting a list of bus and branch objects to send to 
			 * this simulation so that only enabled objects are present in the lists.  The "-1" check is here incase
			 * a bus on one or both ends of a branch is disabled, but the branch itself is still enabled.
			 */
			if (from != -1 && to != -1) {
				double mw = -branch.getBPrime() * (angle[from] - angle[to]);
				mwFlows.put(branch, new Double(mw));
				busOutput[from] += mw;
				
				mw = -branch.getBPrime() * (angle[to] - angle[from]);
				busOutput[to] += mw;
			}
		}
		
		slackOutput = busOutput[slackBusIndex];
	}

	public HashMap getMwFlows() {
		return mwFlows;
	}

	public double getSlackOutput() {
		return slackOutput;
	}
}