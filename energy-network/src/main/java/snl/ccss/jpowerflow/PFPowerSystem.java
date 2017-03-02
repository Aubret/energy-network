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

import java.util.List;

/**
 * This interface defines all the methods needed by
 * the power flow simulators for a power system object.
 * 
 * @author Bryan T. Richardson - Sandia National Laboratories
 */
public interface PFPowerSystem {

	/**
	 * This method should return a list of all buses in the system.
	 * @return list of buses
	 */
	public List getBusList();

	/**
	 * This method should return a list of the energized branches
	 * in the system.  If a complete list of system brances is
	 * returned instead (i.e. if a branch offers the ability for
	 * a user to disable it and the list is not filtered for enabled
	 * branches only), then a power flow simulation will be ran on
	 * the entire system, regardless of the fact that some devices
	 * may actually be out of service (de-energized).
	 * @return list of energized branches
	 */
	public List getEnergizedBranchList();
}