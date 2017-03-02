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
 * the DC power flow solver for a branch object.
 * 
 * @author Bryan T. Richardson - Sandia National Laboratories
 */
public interface DCPFBranch {

	/**
	 * This method should return the bus ID number of the bus connected to its "from" or "tap" side.
	 * @return ID number of bus on "from" or "tap" side
	 */
	public int getFromBus();

	/**
	 * This method should return the bus ID number of the bus connected to its "to" or "Z" side.
	 * @return ID number of bus on "to" or "Z" side
	 */
	public int getToBus();

	/**
	 * This method should return the b prime value for the branch.
	 * @return b prime for branch
	 */
	public double getBPrime();
}