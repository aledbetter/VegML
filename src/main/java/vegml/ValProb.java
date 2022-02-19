 /*************************************************************************
 * VegML version 1.0.0
 * __________________
 * 
 * Copyright (C) [2022] Aaron Ledbetter
 * All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains
 * the property of Aaron Ledbetter. The intellectual and technical 
 * concepts contained herein are proprietary to Aaron Ledbetter and 
 * may be covered by U.S. and Foreign Patents, patents in process, 
 * and are protected by trade secret or copyright law. 
 *
 * @author Aaron Ledbetter
 */
/**
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package vegml;

import java.util.List;

import vegml.VegML.PredictionType;

/**
 * ValProb class represents a value and its probability
 */
public class ValProb {
	public long value;
	public double probability;
	public int count;
	public int counter;	// counter for averaging
	public PredictionType type; // Prediction type
	public int position;
	
	ValProb() {}
	
	/**
	 * get new ValProb with defaults and val
	 * @param val
	 */
	public ValProb(long val) {
		this.count = 1;
		this.counter = 1;
		this.probability = 1.0;
		this.value = val;
		this.type = PredictionType.Predict;
	}
	
	/**
	 * clear this ValProb
	 */
	public void clear() {
		value = count = counter = position = 0;
		probability = 0;
	}
	
	/**
	 * copy this ValProb to the passed in object
	 * @param nvp ValProb to copy to
	 * @return 
	 */
	public ValProb copy(ValProb nvp) {
		nvp.count = count;
		nvp.probability = probability;
		nvp.counter = counter;
		nvp.type = type;
		nvp.value = value;
		nvp.position = position;
		return nvp;
	}
	
	/**
	 * make a copy of this ValProb
	 * @return
	 */
	public ValProb copy() {
		return copy(new ValProb());
	}
	
	/**
	 * Find this value in a ValProb list
	 * @param list list to search
	 * @param value value to search for
	 * @return index if found, else -1
	 */
	public static int indexOf(List<ValProb> list, long value) {
		if (list == null) return -1;
		for (int k=0;k<list.size();k++) {
			if (list.get(k).value == value) return k;
		}
		return -1;
	}
	
	/**
	 * Find this value in a ValProb list
	 * @param list list to search
	 * @param value value to search for
	 * @return ValProb if found, else null
	 */
	public static ValProb find(List<ValProb> list, long value) {
		if (list == null) return null;
		for (int k=0;k<list.size();k++) {
			if (list.get(k).value == value) return list.get(k);
		}
		return null;
	}
		
	/**
	 * value estimation when values are numbers results contain 
	 * the probability sum of the values
	 * Estimates should be calculated prior to tie breaking, or with tie breaking off
	 * 
	 * TODO: not implemented 1.0.1 feature
	 * 
	 * @param list list of valprob to estimate
	 * @param places number of places after decimal point
	 * @return estimated-value, probability
	 */
	public static double[] getValueEstimate(List<ValProb> list, int places) {
		double est[] = null;
		// TODO
		return est;
	}
	
	/**
	 * value estimation when values are numbers results contain 
	 * the probability sum of the values
	 * range - gets best range from the list
	 * Estimates should be calculated prior to tie breaking, or with tie breaking off
	 * 
	 * TODO: not implemented 1.0.1 feature
	 * 
	 * @param list list to search
	 * @return list[0] = start / end
	 */
	public static double[][] getValueRangeEstimate(List<ValProb> list) {
		double[][] est = null;
		// TODO
		return est;
	}
}
