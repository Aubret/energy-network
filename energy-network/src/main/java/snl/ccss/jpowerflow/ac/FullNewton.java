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

import java.util.List;

import flanagan.complex.Complex;
import flanagan.math.Matrix;
import snl.ccss.jpowerflow.AbstractPFSolver;
import snl.ccss.jpowerflow.PFPowerSystem;
import snl.ccss.jpowerflow.util.Utilities;

/**
 * This class holds all the methods necessary for performing a
 * steady-state AC power flow analysis on a power system.
 * 
 * This class uses the Full Newton-Raphson iterative technique
 * for solving the unknowns in the power system.
 *  
 * @author Bryan T. Richardson - Sandia National Laboratories
 */
public class FullNewton
	   extends
	   AbstractPFSolver {
	
	protected List busList;
	protected List branchList;
	
	protected int busCount;
	protected int branchCount;
	
	protected int maxIterations;
	protected double adjustError;
	protected double convergeError;
	protected boolean enforceGenMvarLimits;
	
	protected int iterations;
	
	protected Complex[][] yBus;
	protected double[] power;
	protected double[] deltaPower;
	protected double[][] jac;
	protected boolean adjustFlag;
	protected boolean convergeFlag;
	
	/**
	 * Default constructor for FullNewton class.
	 * 
	 * Required parameters will need to be set using the get/set
	 * methods if this constructor is used.
	 */
	public FullNewton() { }
	
	/**
	 * Argument-accepting constructor for FullNewton class.
	 * 
	 * @param maxIterations Maximum number of iterations to perform
	 * @param adjustError Value of power mismatch that determines when generator reactive power limits should be examined
	 * @param convergeError Value of power mismatch that determines the system has converged
	 * @param enforceGenMvarLimits Set to true if reactive power limits of generators should be enforced
	 */
	public FullNewton(int maxIterations, double adjustError, double convergeError, boolean enforceGenMvarLimits) {
		this.maxIterations = maxIterations;
		this.adjustError = adjustError;
		this.convergeError = convergeError;
		this.enforceGenMvarLimits = enforceGenMvarLimits;
	}
	
	/**
	 * Main method for performing an AC power flow analysis.
	 * This method builds the yBus for the system and performs
	 * the Full Newton-Raphson iterative technique to solve for the
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

			/*
			 * Power values and the Jacobian matrix are calculated using
			 * current voltage values and system impedence values.
			 */
			calculatePowerJac();

			if (DEBUG) {
				System.out.println();
				System.out.println("Jacobian Matrix:");
				for (int i = 0; i < busCount * 2; i++) {
					for (int j = 0; j < busCount * 2; j++) {
						System.out.print(Utilities.roundDouble(jac[i][j], 2) + "  ");
					}
					System.out.println();
				}
				System.out.println();
			}
			/*
			 * Power mismatch is used to determine when to start making adjustments to the system
			 * and when the system has successfully converged.
			 */
			mismatchCheck();

			/*
			 * Once the convergence criteria has been met or the maximum number of iterations have been
			 * performed the values for power at each bus are updated into the objects.
			 */
			if (convergeFlag || iterations == maxIterations) {
				for (int i = 0; i < busCount; i++) {
					ACPFBus bus = (ACPFBus)busList.get(i);
					bus.setBusMw(power[i]);
					bus.setBusMvar(power[i + busCount]);
				}

				Utilities.jacobian = new Matrix(jac);
				Utilities.createJacobianMap(busList);

				break;
			}
			
			/*
			 * Once the adjustment criteria has been met the program begins checking to see if reactive power
			 * generation is within generator-specific limits.  If not, the reactive power output of the generator
			 * is set to it's maximum or minimum limit and Automatic Voltage Regulation (AVR) is turned off.
			 */
			
			/*
			 * Note: The ACPFBus methods isGenMvarWithinLimits() and adjustGenOutputMvar() have now been
			 * combined into one method, checkGenMvarOutput().  This method is called in mismatchCheck()
			 * and is told to adjust accordingly if the adjustment criteria have been met.
			 */
/*
			if (enforceGenMvarLimits) {
				if(adjustFlag) {
					if (DEBUG)
						System.out.println("Adjustment criteria for Generation VAR output has been met.");
					for (int i = 0; i < busCount; i++) {
						((ACPFBus)busList.get(i)).adjustGenOutputMvar(power[i + busCount]);
					}
				}
			}
*/
			
			/*
			 * Certain rows and columns of the Jacobian matrix are zeroed out to ensure
			 * that generation voltage and Mw values stay constant and to ensure that
			 * voltage values on the system swing bus stay at unity.
			 */
			zeroJac();
			
			/*
			 * Change in voltage values are calculated using Gaussian elimination and back substitution.
			 */
			Matrix matrixJac = new Matrix(jac);
			double[] deltaVoltage = matrixJac.solveLinearSet(deltaPower);
			
			/*
			 * Voltage values for each object are updated every iteration for use in the next iteration.
			 */
			for (int i = 0; i < busCount; i++) {
				ACPFBus bus = (ACPFBus)busList.get(i);
				bus.setAngle(bus.getAngle() + deltaVoltage[i]);
				bus.setVoltage(bus.getVoltage() + deltaVoltage[i + busCount]);
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
	
	/*
	 * This method uses data from each branch object to create yBus for the entire system.
	 */
	protected void buildYBus() {
		yBus = Complex.twoDarray(busCount, busCount);

		for (int i = 0; i < branchCount; i++) {
			ACPFBranch branch = (ACPFBranch)branchList.get(i);
			int fromBus = branch.getFromBus();
			int toBus = branch.getToBus();
			Complex[][] miniYBus = branch.getYBus();
			int from = -1;
			int to = -1;
			
			for (int j = 0; j < busCount; j++) {
				ACPFBus bus = (ACPFBus)busList.get(j);
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
				yBus[from][from].plusEquals(miniYBus[0][0]);
				yBus[from][to].plusEquals(miniYBus[0][1]);
				yBus[to][from].plusEquals(miniYBus[1][0]);
				yBus[to][to].plusEquals(miniYBus[1][1]);
			}
		}
	}
	
	/*
	 * Power and Jacobian values are calculated at the same time so as to minimize the number of for loops the simulation
	 * must run each iteration.  The Jacobian matrix is created here.
	 */
	protected void calculatePowerJac() {
		power = new double[busCount * 2];
		deltaPower = new double[busCount * 2];
		jac = new double[busCount * 2][busCount * 2];

		for (int i = 0; i < busCount; i++) {
			ACPFBus iBus = (ACPFBus)busList.get(i);
			for (int j = 0; j < busCount; j++) {
				ACPFBus jBus = (ACPFBus)busList.get(j);
				power[i] += Complex.abs(yBus[i][j]) * jBus.getVoltage() * Math.cos(iBus.getAngle() - jBus.getAngle() - Complex.arg(yBus[i][j]));
				power[i + busCount] += Complex.abs(yBus[i][j]) * jBus.getVoltage() * Math.sin(iBus.getAngle() - jBus.getAngle() - Complex.arg(yBus[i][j]));
				if (i != j) {
					jac[i][j] = iBus.getVoltage() * Complex.abs(yBus[i][j]) * jBus.getVoltage() * Math.sin(iBus.getAngle() - jBus.getAngle() - Complex.arg(yBus[i][j]));
					jac[i][j + busCount] = iBus.getVoltage() * Complex.abs(yBus[i][j]) * Math.cos(iBus.getAngle() - jBus.getAngle() - Complex.arg(yBus[i][j]));
					jac[i + busCount][j] = -iBus.getVoltage() * Complex.abs(yBus[i][j]) * jBus.getVoltage() * Math.cos(iBus.getAngle() - jBus.getAngle() - Complex.arg(yBus[i][j]));
					jac[i + busCount][j + busCount] = iBus.getVoltage() * Complex.abs(yBus[i][j]) * Math.sin(iBus.getAngle() - jBus.getAngle() - Complex.arg(yBus[i][j]));
				} else {
					for (int k = 0; k < busCount; k++) {
						ACPFBus kBus = (ACPFBus)busList.get(k);
						jac[i][i + busCount] += Complex.abs(yBus[i][k]) * kBus.getVoltage() * Math.cos(iBus.getAngle() - kBus.getAngle() - Complex.arg(yBus[i][k]));
						jac[i + busCount][i + busCount] += Complex.abs(yBus[i][k]) * kBus.getVoltage() * Math.sin(iBus.getAngle() - kBus.getAngle() - Complex.arg(yBus[i][k]));
						if (i != k) {
							jac[i][i] += Complex.abs(yBus[i][k]) * kBus.getVoltage() * Math.sin(iBus.getAngle() - kBus.getAngle() - Complex.arg(yBus[i][k]));
							jac[i + busCount][i] += Complex.abs(yBus[i][k]) * kBus.getVoltage() * Math.cos(iBus.getAngle() - kBus.getAngle() - Complex.arg(yBus[i][k]));
						}
					}

					jac[i][i] *= -iBus.getVoltage();
					jac[i][i + busCount] += iBus.getVoltage() * Complex.abs(yBus[i][i]) * Math.cos(Complex.arg(yBus[i][i]));
					jac[i + busCount][i] *= iBus.getVoltage();
					jac[i + busCount][i + busCount] += -iBus.getVoltage() * Complex.abs(yBus[i][i]) * Math.sin(Complex.arg(yBus[i][i]));
				}
			}
			power[i] *= iBus.getVoltage();
			power[i + busCount] *= iBus.getVoltage();
			
			double actualSusceptance = iBus.getVoltage() * iBus.getVoltage() * iBus.getSusceptance();
			deltaPower[i] = iBus.getBusMw() - power[i];
			deltaPower[i + busCount] = iBus.getBusMvar() + actualSusceptance - power[i + busCount];
		}
	}
	
	/*
	 * Method for looking at power mismatch to determine if convergence and adjustment criteria have been met.
	 */
	protected void mismatchCheck() {
		adjustFlag = true;
		convergeFlag = true;
		
		for (int i = 0; i < busCount; i++) {
			ACPFBus bus = (ACPFBus)busList.get(i);
			
			/*
			 * For a generation bus, only real power output is examined and compared because reactive power is
			 * variable.  Slack busList are not examined, because both real and reactive power are variable. For
			 * convergence criteria, generation reactive power limits are also examined.
			 */
			//TODO: Should slack generators be limited on Mvar output??
			if (bus.isGenerationBus()) {
				if (!bus.isSlackBus()) {
					if (Math.abs(deltaPower[i]) > adjustError)
						adjustFlag = false;

					if (enforceGenMvarLimits) {
//						if (!bus.isGenMvarWithinLimits(power[i + busCount])) {
						if (!bus.checkGenMvarOutput(power[i + busCount], adjustFlag)) {
							if (DEBUG) {
								System.out.println();
								System.out.println("Generation Bus " + (i+1) + " -- Out of Mvar Limits -- " + power[i + busCount]);
								System.out.println();
							}
							convergeFlag = false;
						} else if (Math.abs(deltaPower[i]) > convergeError) {
							if (DEBUG) {
								System.out.println();
								System.out.println("Generation Bus -- Delta Real Power Out of Limits");
								System.out.println();
							}
							convergeFlag = false;
						}
					} else {
						if (Math.abs(deltaPower[i]) > convergeError) {
							if (DEBUG) {
								System.out.println();
								System.out.println("Generation Bus -- Delta Real Power Out of Limits");
								System.out.println();
							}
							convergeFlag = false;
						}
					}
				}

			/*
			 * For a load bus, both real and reactive power are examined and compared because both values
			 * are fixed.
			 */
			} else {
				if (Math.abs(deltaPower[i]) > adjustError || Math.abs(deltaPower[i + busCount]) > adjustError)
					adjustFlag = false;

				if (Math.abs(deltaPower[i]) > convergeError) {
					if (DEBUG) {
						System.out.println();
						System.out.println("Load Bus -- Delta Real Power Out of Limits");
						System.out.println();
					}
					convergeFlag = false;
				} else if (Math.abs(deltaPower[i + busCount]) > convergeError) {
					if (DEBUG) {
						System.out.println();
						System.out.println("Load Bus -- Delta Reactive Power Out of Limits");
						System.out.println();
					}
					convergeFlag = false;
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
						jac[i + busCount][j] = 0;
						jac[i + busCount][j + busCount] = 0;
						jac[j][i + busCount] = 0;
						jac[j + busCount][i + busCount] = 0;
						jac[i + busCount][i + busCount] = 1e+10;
						jac[i][j] = 0;
						jac[i][j + busCount] = 0;
						jac[j][i] = 0;
						jac[j + busCount][i] = 0;
						jac[i][i] = 1e+10;
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
						jac[i + busCount][j] = 0;
						jac[i + busCount][j + busCount] = 0;
						jac[j][i + busCount] = 0;
						jac[j + busCount][i + busCount] = 0;
						jac[i + busCount][i + busCount] = 1e+10;
					}
				}
			}
		}
	}

	public void setAdjustError(double adjustError) {
		this.adjustError = adjustError;
	}

	public void setConvergeError(double convergeError) {
		this.convergeError = convergeError;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public int getIterations() {
		return iterations;
	}

	public void setEnforceGenMvarLimits(boolean enforceGenMvarLimits) {
		this.enforceGenMvarLimits = enforceGenMvarLimits;
	}
}