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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import vegml.ValProb;


/**
 * Accumulator for Integer values (4 bytes)
 *
 */
class AccumInt extends Accum {
	private static final long serialVersionUID = -6722705284720050211L;
	
	protected static final int VAL = 0;
	protected static final int CNT = 1;
	protected static final int PROBW = 2;
	protected static final int DSIZE = 3;	
	
	protected int [][] dataM = null;


	public AccumInt() {
		//locked = false;
	}
	
	@Override
	public double getProbability(long val) {
		if (total == 0 || dataM == null) return 0;
		return getProbabilityIdx(findValue(val));
	}
	@Override
	public ValProb getValProb(long val) {
		if (total == 0 || dataM == null) return null;
		int i = findValue(val);
		if (i < 0) return null;
	
		ValProb vp = new ValProb();
		vp.count = dataM[CNT][i];
		vp.probability = getProbabilityIdx(i);
		vp.counter = 1;
		vp.value = val;
		return vp;
	}
	
	private double getProbabilityIdx(int idx) {
		if (idx < 0) return 0;
		if (dataM[PROBW][idx] > 0) {
			return ((double) dataM[PROBW][idx]) / (double)Integer.MAX_VALUE;
		}
		return ((double) dataM[CNT][idx]) / (double)total;
	}

	
	//
	// modify the probability of a value by weighting it 
	//
	@Override
	public boolean weightProbability(long value, double weight) {
		// implement in 
		int idx = findValue(value);
		if (idx < 0) return false;
		double cp = getProbabilityIdx(idx);
		// current times weight 

		dataM[PROBW][idx] = (int)Math.round((cp*weight) * (double)Integer.MAX_VALUE);
		//System.out.println(" GOT["+dataM[CNT][idx]+"]["+dataM[PROBW][idx] +"] " + this.getProbabilityIdx(idx) + " VS " + (cp*weight));
		// upper limit == 1
		if (dataM[PROBW][idx] < 0 || dataM[PROBW][idx] > Integer.MAX_VALUE) {
			dataM[PROBW][idx] = Integer.MAX_VALUE;
		}
		return true;
	}
	// how balanced is this
	@Override
	public double getBalance(int dataWidth) {
		if (total == 0 || dataM == null) return 0;
		//	if (this.getValueCount() != dataWidth) return 0;
		
		int maxc = 0, minc = 0;
		
		for (int i=0;i<dataM[VAL].length;i++) {
			int cc = dataM[CNT][i];
			if (cc > maxc) maxc = cc;		
			if (cc < minc || minc == 0) minc = cc;		
		}
		// compare most to least
		return (double)minc/(double)maxc;
	}

	// get most probable
	@Override
	public boolean isMostProbableValue(long val) {
		if (total == 0 || dataM == null) return false;
		if (val == getFirstMostProbablityValue()) return true;
		return false;
	}
	// get most probable
	@Override
	public boolean isMostProbableValues(long val) {
		if (total == 0 || dataM == null) return false;
		double p = getFirstMostProbablity();
		if (getProbability(val) == p) return true;
		return false;
	}
	// get most probable
	@Override
	public ValProb getFirstMostProbable() {
		if (total == 0 || dataM == null) return null;
		int idx = getFirstMostProbablityIdx();
		ValProb valProb = new ValProb(); 
		valProb.value = dataM[VAL][idx];
		valProb.probability = getProbabilityIdx(idx);
		valProb.count = dataM[CNT][idx];
		return valProb;
	}
	@Override
	public double getFirstMostProbablity() {
		return getProbabilityIdx(getFirstMostProbablityIdx());
	}
	@Override
	public long getFirstMostProbablityValue() {
		if (total == 0 || dataM == null) return 0;
		return dataM[VAL][getFirstMostProbablityIdx()];
	}
	private int getFirstMostProbablityIdx() {
		if (total == 0 || dataM == null) return -1;
		double p = 0;		
		int idx = -1;
		int val = 0;
		for (int i=0;i<dataM[VAL].length;i++) {
			double cp = getProbabilityIdx(i);
			if (cp > p || (cp == p && dataM[VAL][i] < val)) {
				p = cp;	
				idx = i;
				val = dataM[VAL][i];
			} 		
		}
		return idx;
	}
	@Override
	public int getMaxCount() {
		if (total == 0 || dataM == null) return 0;
		double p = 0;	
		int val = 0;
		for (int i=0;i<dataM[VAL].length;i++) {
			double cp = getProbabilityIdx(i);
			if (cp > p) {
				p = cp;	
				val = dataM[CNT][i];
			}
		}
		return val;
	}
	
	// get the list of values and probabilities
	@Override
	public List<ValProb> getValPs(List<ValProb> vpList) {
		if (total == 0 || dataM == null) return null;
		if (vpList == null) vpList = new ArrayList<>();
		
		for (int i=0;i<dataM[VAL].length;i++) {
			ValProb vp = new ValProb(); 
			vp.probability = getProbabilityIdx(i);
			vp.value = dataM[VAL][i];
			vp.count = dataM[CNT][i];
			vpList.add(vp);		
		}
		return vpList;
	}

	@Override
	public int getValueCount() {
		if (total == 0 || dataM == null) return 0;
		return dataM[VAL].length;
	}
	
	@Override
	int setCount(long val, int count, int crtCount) {	
		synchronized (this) {
		int tot = this.total;
		if (dataM == null) {
			addCount(val, count);
		} else {
			int v = findValue(val);
			if (v >= 0) {
				dataM[CNT][v] = count;
			} else {
				addCount(val, count);
			}
		}
		// don't change total
		this.total = tot;
		return count;		
		}
	}
	
	@Override
	int addCount(long val, int count) {	
		synchronized (this) {
		if (total != Integer.MAX_VALUE) total += count; // HACK to stop things here
		
		if (dataM == null) {
			dataM = new int[DSIZE][1];
			dataM[VAL][0] = (int)val;
			dataM[CNT][0] = count;
			dataM[PROBW][0] = 0;
			return count;
		}
		int v = findValue(val);
		if (v >= 0) {
			if (dataM[CNT][v] == Integer.MAX_VALUE) return dataM[CNT][v];
			dataM[CNT][v] = dataM[CNT][v]+count;
			return dataM[CNT][v];			
		}
		// new value: extend array
		int [][] dm = new int[DSIZE][dataM[VAL].length+1];
		// copy in
		for (int i=0;i<dataM[VAL].length;i++) {
			dm[VAL][i] = dataM[VAL][i];
			dm[CNT][i] = dataM[CNT][i];
			dm[PROBW][i] = dataM[PROBW][i];
		}
		dm[VAL][dataM[VAL].length] = (int)val;
		dm[CNT][dataM[CNT].length] = count;
		dm[PROBW][dataM[PROBW].length] = 0;
		dataM = dm;
		return count;
		}
	}

	@Override
	public int getCount(long val) {
		synchronized (this) {
		if (total == 0 || dataM == null) return 0;
		int v = findValue(val);
		if (v < 0) return 0;
		return dataM[CNT][v];
		}
	}
	@Override
	public boolean hasValue(long val) {
		if (total == 0 || dataM == null) return false;
		int v = findValue(val);
		if (v < 0) return false;
		return true;
	}

	
	@Override
	int reduceCount(long val, int count) {
		synchronized (this) {
		if (total == 0 || dataM == null) return 0;
		int v = findValue(val);
		if (v < 0) return 0;
		int cnt = dataM[CNT][v];
		if (cnt <= count) return remove(val);
		dataM[CNT][v] -= count;
		total -= count;
		return cnt;
		}
	}
	// remove a value
	@Override
	int remove(long val) {
		synchronized (this) {
		if (total == 0 || dataM == null) return 0;
		int v = findValue(val);
		if (v < 0) return 0;
			
		int cnt = dataM[CNT][v];
		if (dataM[VAL].length == 1) {
			// empty
			dataM = null;
			total = 0;
		} else {
			// shrink
			int [][] dm = new int[DSIZE][dataM[VAL].length-1];
			// copy in
			int xi = 0;
			for (int i=0;i<dataM[VAL].length;i++) {
				if (i == v) continue;
				dm[VAL][xi] = dataM[VAL][i];
				dm[CNT][xi] = dataM[CNT][i];
				dm[PROBW][xi] = dataM[PROBW][i];
				xi++;
			}
			total -= cnt;
			dataM = dm;	
		}
		return cnt;
		}
	}
	
	@Override
	void clear() {
		synchronized (this) {
		if (total == 0) return;
		dataM = null;
		total = 0;
		}
	}
	
	@Override
	public void getValueSet(HashMap<Long, Integer> vm) { 
		synchronized (this) {
		if (total == 0 || dataM == null) return;
		for (int i=0;i<dataM[VAL].length;i++) {
			Integer cnt = vm.get((long)dataM[VAL][i]);
			if (cnt == null) vm.put((long)dataM[VAL][i], 1);
			else vm.put((long)dataM[VAL][i], cnt+1);
		}
		}
	}

	
	// merge this into intoAc
	@Override
	void mergeInto(Accum intoAc) {
		synchronized (this) {
		if (total == 0 || dataM == null || intoAc == null) return;
		for (int i=0;i<dataM[VAL].length;i++) {
			intoAc.addCount(dataM[VAL][i], dataM[CNT][i]);
			if (dataM[PROBW][i] > 0 && intoAc instanceof AccumInt) {
				AccumInt aip = (AccumInt)intoAc;
				aip.dataM[PROBW][aip.findValue(dataM[VAL][i])] = dataM[PROBW][i] ;
			}
		}
		}
	}
	@Override
	public Accum copy() {
		synchronized (this) {
		AccumInt nc = new AccumInt();
		nc.vsid = this.vsid;
		nc.vectorCode = this.vectorCode;
		nc.total = this.total;
	//	nc.locked = this.locked;
		nc.dataM = new int[DSIZE][dataM[VAL].length];
		for (int i=0;i<dataM[VAL].length;i++) {	
			nc.dataM[VAL][i] = this.dataM[VAL][i];
			nc.dataM[CNT][i] = this.dataM[CNT][i];
			nc.dataM[PROBW][i] = this.dataM[PROBW][i];
		}
		return nc;
		}
	}
	@Override
	public void copyFrom(Accum from) {
		synchronized (this) {
		this.vsid = from.vsid;
		this.vectorCode = from.vectorCode;
		from.mergeInto(this);
		}
	}
	
	// compare probabilities
	@Override
	public boolean compare(Accum ac) {
		if (ac == null) return false;
		if (total != ac.getTotal()) return false;
		if (ac.getValueCount() != this.getValueCount()) return false;
		// order is not known
		for (int i=0;i<dataM[VAL].length;i++) {
			double prob = this.getProbability(dataM[VAL][i]);
			double acprob = ac.getProbability(dataM[VAL][i]);
			if (acprob != prob) return false;
		}
		return true;
	}


	@Override
	public int diff(Accum ac) {
		int cnt = super.diff(ac);
		
		for (int i=0;i<dataM[VAL].length;i++) {
			int c = ac.getCount(dataM[VAL][i]);
			if (c != dataM[CNT][i]) {
				cnt++;
				System.out.println("    DIFF[ac_il]["+this.getVectorCode()+"] valCount["+dataM[VAL][i]+"]=["+c+"]["+dataM[CNT][i]+"]");		
			}
		}
		//protected int vsid;
		return cnt;
	}
	
	@Override
	void optimize() {
		//  TODO: sort the dataM by probability
		// question: it adds value only if it can tell it is optimized and access faster. how?
		//sortValues();
	}
	
	// lock this accum to this value
	@Override
	void lock(long value) {
		// make it this value
		dataM = new int[DSIZE][1];
		dataM[VAL][0] = (int)value;
		dataM[CNT][0] = total;
	//	locked = true;
	}

	@Override
	public int getValueTotal() {
		synchronized (this) {
		if (total == 0 || dataM == null) return -1;
		int tot = 0;
		for (int i=0;i<dataM[VAL].length;i++) {
			tot += dataM[CNT][i];
		}
		return tot;
		}
	}
	
	// find a value index
	private int findValue(long val) {
		synchronized (this) {
		if (total == 0 || dataM == null) return -1;
		for (int i=0;i<dataM[VAL].length;i++) {
			if (dataM[VAL][i] == val) return i;
		}
		return -1;
		}
	}

	@Override
	int addCrtCount(long val, int count) {
		return 0;
	}

	@Override
	public int getCrtCount(long val) {
		if (total == 0) return 0;
		return 0;
	}

}
