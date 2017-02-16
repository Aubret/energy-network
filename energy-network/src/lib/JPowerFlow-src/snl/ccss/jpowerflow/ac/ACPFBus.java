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

/**
 * This interface defines all the methods needed by
 * the AC power flow simulators for a bus object.
 * 
 * @author Bryan T. Richardson - Sandia National Laboratories
 */
public interface ACPFBus {

	/**
	 * This method should return the bus ID as it's represented in the system.
	 * @return bus ID number
	 */
	public int getNumber();
	
	/**
	 * This method should return the bus voltage magnitude in per unit.
	 * @return per unit voltage magnitude
	 */
	public double getVoltage();
	
	/**
	 * This method should return the bus voltage angle in radians.
	 * @return voltage angle in radians
	 */
	public double getAngle();
	
	/**
	 * This method should return the total amount of real power at the bus in per unit.
	 * This is represented by real generated power minus real load power.
	 * @return per unit total real power
	 */
	public double getBusMw();
	
	/**
	 * This method should return the total amount of reactive power at the bus in per unit.
	 * This is represented by reactive generated power minus reactive load power.
	 * @return per unit total reactive power
	 */
	public double getBusMvar();
	
	/**
	 * This method should return the amount of susceptance present at the bus in per unit.
	 * @return per unit amount of susceptance
	 */
	public double getSusceptance();
	
	/**
	 * This method should return true if the bus has generation attached to it, and false otherwise.
	 * @return true if generation is present
	 */
	public boolean isGenerationBus();
	
	/**
	 * This method should return true if the bus is the system slack bus, and false otherwise.
	 * @return true if system slack bus
	 */
	public boolean isSlackBus();
	
	/**
	 * This method should return true if at least one generator connected to the bus is available
	 * for Automatic Voltage Regulation (AVR), and false otherwise.
	 * @return true if at least one generator available for AVR is present
	 */
	public boolean isAVR();
	
	/**
	 * This method should return true if all generators available for Automatic Voltage Regulation (AVR)
	 * are within the reactive power output limits of the generator.  This is used by the mismatch check
	 * method in FullNewton to determine if the power system is suitable for convergence.
	 * 
	 * The adjust flag should be set to true if the user would like to make generator reactive power
	 * output adjustments when this method returns false.
	 * @param busMvar Total amount of reactive power at the bus
	 * @param adjust Set to true of generators outside their limits should be adjusted
	 * @return true if all generators available for AVR are within reactive power output limits
	 */
	public boolean checkGenMvarOutput(double busMvar, boolean adjust);
	
	/**
	 * This method should update the voltage magnitude for the bus
	 * @param voltage Voltage magnitude in per unit
	 */
	public void setVoltage(double voltage);
	
	/**
	 * This method should update the voltage angle for the bus
	 * @param angle Voltage angle in radians
	 */
	public void setAngle(double angle);
	
	/**
	 * This method should update the total amount of real power at the bus.  New generation output values
	 * can be calculated from this by adding the total amount of load present at the bus.
	 * @param busMw Total amount of real power at the bus in per unit
	 */
	public void setBusMw(double busMw);
	
	/**
	 * This method should update the total amount of reactive power at the bus.  New generation output values
	 * can be calculated from this by adding the total amount of load present at the bus.
	 * @param busMvar Total amount of reactive power at the bus in per unit
	 */
	public void setBusMvar(double busMvar);
}