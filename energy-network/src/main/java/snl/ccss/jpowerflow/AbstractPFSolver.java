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
 * This is an abstract implementation of the PFSolver
 * interface that can be extended by power flow
 * simulators.  This abstract class also implements
 * the Runnable interface so power flow simulations
 * can be ran concurrently in seperate threads.
 * 
 * @author Bryan T. Richardson - Sandia National Laboratories
 */
public abstract class AbstractPFSolver
	   implements
	   PFSolver,
	   Runnable {

	public static final boolean DEBUG = "true".equals(System.getProperty("snl.ccss.jpowerflow.debug"));

	protected PFPowerSystem powerSystem;
	
	/**
	 * This method is required by the Runnable interface
	 * calls on the solve method required by the PFsolver
	 * interface.  The PFPowerSystem powerSystem variable
	 * must be set via the setPowerSystem method before
	 * starting the thread.
	 */
	public void run() {
		solve(powerSystem);
	}
	
	public void setPowerSystem(PFPowerSystem powerSystem) { this.powerSystem = powerSystem; }
}