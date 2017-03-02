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

/**
 * This interface defines all the methods needed by
 * the DC power flow solver for a bus object.
 * 
 * @author Bryan T. Richardson - Sandia National Laboratories
 */
public interface DCPFBus {

	/**
	 * This method should return the bus ID as it's represented in the system.
	 * @return bus ID number
	 */
	public int getNumber();

	/**
	 * This method should return true if the bus is the system slack bus, and false otherwise.
	 * @return true if system slack bus
	 */
	public boolean isSlackBus();

	/**
	 * This method should return the total amount of real power at the bus in per unit.
	 * This is represented by real generated power minus real load power.
	 * @return per unit total real power
	 */
	public double getBusMw();
}