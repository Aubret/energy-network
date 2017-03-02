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

package snl.ccss.jpowerflow.ac;

import flanagan.complex.Complex;
import flanagan.math.Matrix;
import snl.ccss.jpowerflow.PFPowerSystem;
import snl.ccss.jpowerflow.util.Utilities;

/**
 * This class extends FullNewton and performs a faster but less
 * accurate steady-state power flow analysis.
 * 
 * This class uses the Decoupled Newton-Raphson iterative
 * technique for solving the unknowns in the power system.
 *  
 * @author Bryan T. Richardson - Sandia National Laboratories
 */
public class DecoupledNewton
	   extends
	   FullNewton {
	
	protected double[] realPower;
	protected double[] reactPower;
	protected double[] deltaReal;
	protected double[] deltaReact;
	
	protected double[][] J1;
	protected double[][] J4;
	
	/**
	 * Default constructor for DecoupledNewton class.
	 * 
	 * Required parameters will need to be set using the get/set
	 * methods if this constructor is used.
	 */
	public DecoupledNewton() { super(); }
	
	/**
	 * Argument-accepting constructor for DecoupledNewton class.
	 * 
	 * @param maxIterations Maximum number of iterations to perform
	 * @param adjustError Value of power mismatch that determines when generator reactive power limits should be examined
	 * @param convergeError Value of power mismatch that determines the system has converged
	 * @param enforceGenMvarLimits Set to true if reactive power limits of generators should be enforced
	 */
	public DecoupledNewton(int maxIterations, double adjustError, double convergeError, boolean enforceGenMvarLimits) {
		super(maxIterations, adjustError, convergeError, enforceGenMvarLimits);
	}
	
	/**
	 * Main method for performing an AC power flow analysis.
	 * This method builds the yBus for the system and performs
	 * the Decoupled Newton-Raphson iterative technique to solve for the
	 * unknown values.
	 * 
	 * @param powerSystem PFPowerSystem that implements the required getBusList and getEnergizedBranchList methods
	 * @return true if simulation converged within maximum number of iterations
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
		
		iterations = 0;
		
		/*
		 * Y-Bus matrix is built
		 */
		buildYBus();

		if (DEBUG) {
			System.out.println();
			System.out.println("Y-Bus Matrix:");
			for (int i = 0; i < busCount; i++) {
				for (int j = 0; j < busCount; j++) {
					System.out.print(yBus[i][j] + "  ");
				}
				System.out.println();
			}
			System.out.println();
		}
		
		calculateJac();
		
		if (DEBUG) {
			System.out.println();
			System.out.println("Quadrant 1 of Jacobian Matrix:");
			for (int i = 0; i < busCount; i++) {
				for (int j = 0; j < busCount; j++) {
					System.out.print(Utilities.roundDouble(J1[i][j], 2) + "  ");
				}
				System.out.println();
			}
			System.out.println();

			System.out.println();
			System.out.println("Quadrant 4 of Jacobian Matrix:");
			for (int i = 0; i < busCount; i++) {
				for (int j = 0; j < busCount; j++) {
					System.out.print(Utilities.roundDouble(J4[i][j], 2) + "  ");
				}
				System.out.println();
			}
			System.out.println();
		}
		
		Utilities.jacobian = (new Matrix(J4)).copy();
		Utilities.createJacobianMap(busList);

		/*
		 * This begins the Newton-Raphson Iterative Technique
		 */
		for (int z = 0; z < maxIterations; z++) {
			iterations++;
			
			if (DEBUG) {
				System.out.println();
				System.out.println("Iteration #" + iterations);
				System.out.println();
			}

			calculatePower();
			
			//TODO: is there a better way to concat arrays?
			power = new double[busCount * 2];
			deltaPower = new double[busCount * 2];
			for (int i = 0; i < busCount; i++) {
				power[i] = realPower[i];
				deltaPower[i] = deltaReal[i];
				power[i + busCount] = reactPower[i];
				deltaPower[i + busCount] = deltaReact[i];
			}
			
			mismatchCheck();

			/*
			 * Once the convergence criteria has been met or the maximum number of iterations have been
			 * performed the values for power at each bus are updated into the objects.
			 */
			if (convergeFlag || iterations == maxIterations) {
				for (int i = 0; i < busCount; i++) {
					ACPFBus bus = (ACPFBus)busList.get(i);
					bus.setBusMw(realPower[i]);
					bus.setBusMvar(reactPower[i]);
				}
				break;
			}
			
			/*
			 * Once the adjustment criteria has been met the program begins checking to see if reactive power
			 * generation is within generator-specific limits.  If not, the reactive power output of the generator
			 * is set to it's maximum or minimum limit and Automatic Voltage Regulation (AVR) is turned off.
			 */
/*
			if (enforceGenMvarLimits) {
				if(adjustFlag) {
					if (DEBUG)
						System.out.println("Adjustment criteria for Generation VAR output has been met.");
					for (int i = 0; i < busCount; i++) {
						((ACPFBus)busList.get(i)).adjustGenOutputMvar(reactPower[i]);
					}
				}
			}
*/

			J4 = (Utilities.jacobian).getArrayCopy();
			zeroJac();

			Matrix matrixJ1 = new Matrix(J1);
			Matrix matrixJ4 = new Matrix(J4);
			
			double[] deltaAngle = matrixJ1.solveLinearSet(deltaReal);
			double[] deltaVoltage = matrixJ4.solveLinearSet(deltaReact);
			
			/*
			 * Voltage values for each object are updated every iteration for use in the next iteration.
			 */
			for (int i = 0; i < busCount; i++) {
				ACPFBus bus = (ACPFBus)busList.get(i);
				bus.setAngle(bus.getAngle() + deltaAngle[i]);
				bus.setVoltage(bus.getVoltage() + deltaVoltage[i]);
			}
		}
		
		if (iterations < maxIterations) {
			System.out.println("System converged after " + iterations + " iterations.");
			return true;
		} else {
			System.out.println("System did not converge before the maximum number of iterations were performed.");
			return false;
		}
	}
	
	protected void calculatePower() {
		realPower = new double[busCount];
		reactPower = new double[busCount];
		deltaReal = new double[busCount];
		deltaReact = new double[busCount];

		for (int i = 0; i < busCount; i++) {
			ACPFBus iBus = (ACPFBus)busList.get(i);
			for (int j = 0; j < busCount; j++) {
				ACPFBus jBus = (ACPFBus)busList.get(j);
				realPower[i] += iBus.getVoltage() * Complex.abs(yBus[i][j]) * jBus.getVoltage() * Math.cos(iBus.getAngle() - jBus.getAngle() - Complex.arg(yBus[i][j]));
				reactPower[i] += iBus.getVoltage() * Complex.abs(yBus[i][j]) * jBus.getVoltage() * Math.sin(iBus.getAngle() - jBus.getAngle() - Complex.arg(yBus[i][j]));
			}
			double actualSusceptance = iBus.getVoltage() * iBus.getVoltage() * iBus.getSusceptance();
			deltaReal[i] = iBus.getBusMw() - realPower[i];
			deltaReact[i] = iBus.getBusMvar() + actualSusceptance - reactPower[i];
		}
	}
	
	protected void calculateJac() {
		J1 = new double[busCount][busCount];
		J4 = new double[busCount][busCount];

		for (int i = 0; i < busCount; i++) {
			for (int j = 0; j < busCount; j++) {
				if (i != j) {
					if (!Complex.isEqual(yBus[i][j], new Complex(0,0))) {
						J1[i][j] = (1 / (Complex.inverse(yBus[i][j]).getImag()));
						J4[i][j] = -yBus[i][j].getImag();
					
						J1[i][i] += -J1[i][j];
						J4[i][i] += -J4[i][j];
					}
				}
			}
		}
	}
	
	/*
	 * This method zeroes out rows and colums of the Jacobian matrix that correspond to
	 * generation buses.
	 */
	protected void zeroJac() {
		for (int i = 0; i < busCount; i++) {
			ACPFBus bus = (ACPFBus)busList.get(i);
			if (bus.isGenerationBus()) {
				/*
				 * A slack bus will be zeroed out in both the 1st and 4th quadrant
				 * so as to make real and reactive power generation variable.
				 */
				if (bus.isSlackBus()) {
					for (int j = 0; j < busCount; j++) {
						J1[i][j] = 0;
						J1[j][i] = 0;
						J1[i][i] = 1e+10;
						J4[i][j] = 0;
						J4[j][i] = 0;
						J4[i][i] = 1e+10;
					}
				/*
				 * A generation bus with generators available for Automatic Voltage
				 * Regulation (AVR) will be zeroed out in the 4th quadrant because
				 * only reactive power is variable.  A generation bus with no generators
				 * available for AVR will not be zeroed out at all, making both real and
				 * reactive power constant.
				 */
				} else if (bus.isAVR()) {
					for (int j = 0; j < busCount; j++) {
						J4[i][j] = 0;
						J4[j][i] = 0;
						J4[i][i] = 1e+10;
					}
				}
			}
		}
	}
}