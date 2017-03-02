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

package snl.ccss.jpowerflow;

/**
 * Interface that should be implemented by any type
 * of power system power flow solver created for use
 * in this library.
 * 
 * @author Bryan T. Richardson - Sandia National Laboratories
 */
public interface PFSolver {

	/**
	 * This method should perform the calculations
	 * required by the power flow solver.
	 * 
	 * @param powerSystem PFPowerSystem that implements the required getBusList and getEnergizedBranchList methods
	 * @return true if the simulation succeeded
	 */
	public boolean solve(PFPowerSystem powerSystem);
}