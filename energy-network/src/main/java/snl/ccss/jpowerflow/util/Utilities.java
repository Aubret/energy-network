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

package snl.ccss.jpowerflow.util;

import java.util.HashMap;
import java.util.List;

import flanagan.math.Matrix;

/**
 * This class is a place holder for common utilities needed
 * by either the solvers created in this package or by other
 * classes that might use this package.  All methods should
 * be static.
 * 
 * @author Bryan T. Richardson - Sandia National Laboratories
 */
public class Utilities {

	/**
	 * Latest update of the Jacobian matrix, updated by FullNewton
	 * or DecoupledNewton class.  If updated by DecoupledNewton,
	 * it will only be quadrant 4 of the Jacobian matrix.
	 */
	public static Matrix jacobian;
	
	/**
	 * Latest update of the Jacobian map, updated by calling
	 * createJacobianMap.  Jacobian values in the map are always
	 * from the diagonal of the 4th quadrant in the Jacobian matrix.
	 */
	public static HashMap jacobianMap;
	
	public static double[] getInverseSensitivities(double[] busArray) {
		return (jacobian.transpose()).solveLinearSet(busArray);
	}
	
	public static double getMvarVoltageSensitivity(int position) {
		return jacobian.getElement(position, position);
	}
	
	/**
	 * This method creates a new Jacobain map with an ACPFBus as
	 * the key and its corresponding Jacobian diagonal value as
	 * the value.  This map only contains diagonal values from
	 * quadrant 4 of the Jacobian matrix.  This provides reactive
	 * power sensitivity values for change in voltages.
	 * @param busList List of bus objects in the power system
	 */
	public static void createJacobianMap(List busList) {
		jacobianMap = new HashMap();
		int busCount = busList.size();
		for (int i = 0; i < busCount; i++) {
			if (jacobian.getNrow() == busCount)
				jacobianMap.put(busList.get(i), new Double(jacobian.getElement(i,i)));
			else
				jacobianMap.put(busList.get(i), new Double(jacobian.getElement(i + busCount,i + busCount)));
		}
	}
	
	/**
	 * This method prints the Jacobian matrix in a readable
	 * matrix-style format.
	 */
	public static void printJacobian() {
		for (int i = 0; i < jacobian.getNrow(); i++) {
			for (int j = 0; j < jacobian.getNrow(); j++) {
				System.out.print(roundDouble(jacobian.getElement(i,j), 3) + "  ");
			}
			System.out.println();
		}
	}
	
	/**
	 * This method rounds the double value given to the nth
	 * decimal place given.
	 * @param d Double value to be rounded
	 * @param places Number of decimal places to round to
	 * @return double value given rounded to the nth decimal place given
	 */
	public static double roundDouble(double d, int places) {
        return Math.round(d * Math.pow(10, (double)places)) / Math.pow(10, (double)places);
    }
}