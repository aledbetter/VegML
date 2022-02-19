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
import java.util.List;

import gnu.trove.map.hash.TIntIntHashMap;
import vegml.VegML.AccumType;
import vegml.ValProb;


/**
 * Accumulator for Integer values (4 bytes) with correct count
 * 
 * Stored in a HashMap for faster access when many values
 * memory and storage size are much larger than basic Integer Accumulator
 */
class AccumIntHmCrt extends Accum {
	private static final long serialVersionUID = -5705634515246294816L;
	// this map should be optimized to start size based on breadth of possible values
	protected static final int BASE_HASH = 4;
	protected TIntIntHashMap dataProb;
	protected TIntIntHashMap dataCrt;
	private List<ValProb> mostProb = null;
	protected int crtTotal;	// correct total
	
	public AccumIntHmCrt() {}

	//
	// get total correct response count from training
	//
	@Override
	public int getCrtTotal() {
		return crtTotal;
	}
	
	// how balanced is this
	@Override
	public double getBalance(int setsize) {
		if (this.getValueCount() != setsize) return 0;
		
		int maxc = 0, minc = 0;
		int [] pl = dataProb.keys();
		for (int i=0;i<pl.length;i++) {
			int k = pl[i];
			int cc = this.getCount(k);
			if (cc > maxc) {
				maxc = cc;		
			}
			if (cc < minc || minc == 0) {
				minc = cc;		
			}
		}
		// compare most to least
		return (double)minc/(double)maxc;
	}
	@Override
	AccumType getAccumulatorType() {
		return AccumType.HashCrt;
	}
	// get most probable
	@Override
	public boolean isMostProbableValue(long val) {
		if (total == 0) return false;
		makeMostProbable();
		return mostProb.get(0).value == val;
	}
	// get most probable
	@Override
	public boolean isMostProbableValues(long val) {
		if (total == 0) return false;
		makeMostProbable();
		for (int i=0;i<mostProb.size();i++) {
			if (mostProb.get(i).value == val) return true;
		}
		return false;
	}
	// get most probable
	@Override
	public ValProb getFirstMostProbable() {
		if (total == 0) return null;
		makeMostProbable();
		return mostProb.get(0).copy();
	}
	@Override
	public double getFirstMostProbablity() {
		if (total == 0) return 0;
		makeMostProbable();
		return mostProb.get(0).probability;
	}

	@Override
	public long getFirstMostProbablityValue() {
		if (total == 0) return 0;
		makeMostProbable();
		return mostProb.get(0).value;
	}
	@Override
	public int getMaxCount() {
		if (total == 0) return 0;
		makeMostProbable();
		return mostProb.get(0).count;
	}
	private static final int MAX_MOST_PROBABLE = 10;
	private List<ValProb> makeMostProbable() {		
		if (mostProb == null || (mostProb.size() < MAX_MOST_PROBABLE && mostProb.size() < getValueCount())) {
			List<ValProb> sList = this.getValPsSorted();					
			List<ValProb> vpList = new ArrayList<>();
			
			double p = sList.get(0).probability;
			for (int i=0;i<sList.size();i++) {
				ValProb vp = sList.get(i);
				vpList.add(vp);
				if (vp.probability == p) continue;
				if (vpList.size() >= MAX_MOST_PROBABLE) break;
			}
			mostProb = vpList;
		}
		return mostProb;
	}
	@Override
	public List<ValProb> getMostProbable() {
		if (total == 0) return null;
		// make list
		makeMostProbable();
		List<ValProb> vpList = new ArrayList<>();
		// make copy
		for (int i=0;i<mostProb.size();i++) {
			vpList.add(mostProb.get(i).copy());
		}
		return vpList;
	}
	
	// get the list of values and probabilities
	@Override
	public List<ValProb> getValPs(List<ValProb> vpList) {
		if (total == 0) return null;
		if (vpList == null) vpList = new ArrayList<>();
		int [] pl = dataProb.keys();
		for (int i=0;i<pl.length;i++) {
			ValProb vp = new ValProb();
			vp.value = pl[i];
			vp.probability = this.getProbability(pl[i]);
			vp.count = this.getCount(pl[i]);
			vpList.add(vp);
		}
		return vpList;
	}
	
	@Override
	public int getValueCount() {
		if (total == 0) return 0;
		return dataProb.size();
	}
	@Override
	public boolean hasValue(long val) {
		if (total == 0) return false;
		return dataProb.containsKey((int)val);
	}
	@Override
	int setCount(long val, int count, int crtCount) {	
		if (dataProb == null) dataProb = new TIntIntHashMap(BASE_HASH);
		int i = getCount(val);
		if (i > 0) dataProb.put((int)val, count);
		else dataProb.put((int)val, count);
		mostProb = null;
		if (crtCount > 0) {
			if (dataCrt == null) dataCrt = new TIntIntHashMap(BASE_HASH);
			i = getCrtCount(val);
			if (i > 0) dataCrt.put((int)val, crtCount);
			else dataCrt.put((int)val, crtCount);			
		}
		return count;
	}
	
	@Override
	int addCount(long val, int count) {		
		if (total != Integer.MAX_VALUE) total += count; // HACK to stop things here
		if (dataProb == null) dataProb = new TIntIntHashMap(BASE_HASH);
		mostProb = null;
		int i = getCount(val);
		if (i > 0) {
			if (i == Integer.MAX_VALUE) return i; // HACK to stop things here
			if (count == 1) dataProb.increment((int)val);
			else dataProb.put((int)val, i+count);
			return i+count;
		}
		dataProb.put((int)val, count);
		return count;
	}
	@Override
	int addCrtCount(long val, int count) {	
		if (crtTotal != Integer.MAX_VALUE) crtTotal += count; // HACK to stop things here
		if (dataCrt == null) dataCrt = new TIntIntHashMap(BASE_HASH);
		mostProb = null;
		int i = getCrtCount(val);
		if (i > 0) {
			if (i == Integer.MAX_VALUE) return i; // HACK to stop things here
			if (count == 1) dataCrt.increment((int)val);
			else dataCrt.put((int)val, i+count);
			return i+count;
		}
		dataCrt.put((int)val, count);
		return count;
	}
	@Override
	public int getCrtCount(long val) {
		if (crtTotal == 0) return 0;
		return dataCrt.get((int)val);
	}
	@Override
	public int getCount(long val) {
		if (total == 0) return 0;
		return dataProb.get((int)val);
	}
	@Override
	int reduceCount(long val, int count) {
		if (total == 0) return 0;
		mostProb = null;
		int cnt = getCount(val);	
		if (cnt <= count) return remove(val);
		dataProb.put((int)val, cnt-count);
		total -= count;
		return cnt;
	}

	// remove a value
	@Override
	int remove(long val) {
		if (total == 0) return 0;
		mostProb = null;
		int cnt = getCount(val);
		total -= cnt;
		dataProb.remove((int)val);	
		return cnt;
	}
	
	@Override
	void clear() {
		total = 0;
		crtTotal = 0;
		mostProb = null;
		if (dataProb != null) dataProb.clear();
		if (dataCrt != null) dataCrt.clear();
	}
	
	// merge this into intoAc
	@Override
	void mergeInto(Accum intoAc) {
		if (total == 0 || intoAc == null) return;
		mostProb = null;
		dataProb.forEachEntry((k, v) -> {
			intoAc.addCount(k, v);
            return true;
        });
		if (dataCrt != null) {
			dataCrt.forEachEntry((k, v) -> {
				intoAc.addCount(k, v);
	            return true;
	        });
		}
	}
	@Override
	public Accum copy() {
		Accum nc = new AccumIntHmCrt();
		nc.vsid = this.vsid;
		nc.vectorCode = this.vectorCode;
		mergeInto( nc);
		return nc;
	}
	
	@Override
	public void copyFrom(Accum from) {
		this.vsid = from.vsid;
		this.vectorCode = from.vectorCode;
		from.mergeInto(this);
	}
	
	// compare probabilities
	@Override
	public boolean compare(Accum ac) {
		if (ac == null) return false;
		if (total != ac.getTotal()) return false;
		if (ac.getValueCount() != this.getValueCount()) return false;
		int [] pl = dataProb.keys();
		for (int i=0;i<pl.length;i++) {
			double prob = this.getProbability(pl[i]);
			double acprob = ac.getProbability(pl[i]);
			if (acprob != prob) return false;
		}
		if (dataCrt != null) {
			pl = dataProb.keys();
			for (int i=0;i<pl.length;i++) {
			//	double prob = this.getProbability(pl[i]);
			//	double acprob = ac.getProbability(pl[i]);
			//	if (acprob != prob) return false;
			}
		}
		return true;
	}


	@Override
	public int diff(Accum ac) {
		int cnt = super.diff(ac);
		
		int [] pl = dataProb.keys();
		for (int i=0;i<pl.length;i++) {
			int c = ac.getCount(pl[i]);
			if (c != this.getCount(pl[i])) {
				cnt++;
				System.out.println("    DIFF[ac_hm]["+this.getVectorCode()+"] valCount["+pl[i]+"] = ["+c+"]["+this.getCount(pl[i])+"]");		
			}
		}
		if (dataCrt != null) {
			pl = dataCrt.keys();
			for (int i=0;i<pl.length;i++) {
				int c = ac.getCrtCount(pl[i]);
				if (c != this.getCrtCount(pl[i])) {
					cnt++;
					System.out.println("    DIFF[ac_hm]["+this.getVectorCode()+"] valCrtCount["+pl[i]+"] = ["+c+"]["+this.getCrtCount(pl[i])+"]");		
				}
			}
		}
		//protected int vsid;
		return cnt;
	}
	
	@Override
	void optimize() {
		// make this Accum optimal for the data
		if (dataProb != null) dataProb.compact();
		if (dataCrt != null) dataCrt.compact();
	}
}
