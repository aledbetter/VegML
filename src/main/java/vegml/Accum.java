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


import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import vegml.VegML.AccumType;
import vegml.ValProb;


/**
 * Accumulator for values and counts
 * 
 */
public abstract class Accum implements java.io.Serializable {
	private static final long serialVersionUID = 7818193352816842606L;
	
	private Accum next;
	protected long vectorCode;
	protected long vsid;
	protected int total;	// could remove total and sum

	public Accum() {}

	/**
	 * Get the ID for the saved vector set
	 * @return vsid id
	 */
	public int getVectSetId() {
		return (int)this.vsid;
	}
	
	/**
	 * Set the ID for the saved vector set
	 * @param vsid vector set id
	 */
	void setVectSetId(int vsid) {
		this.vsid = vsid;
	}
	
	/**
	 * set the child vector id
	 * @param vid child vector id
	 */
	void setVectChildVid(long vid) {
		this.vsid = vid;
	}
	
	/**
	 * get the child vector Id
	 * @return childs vector Id
	 */
	public long getVectChildVid() {
		return this.vsid;
	}
	
	Accum getNext() {
		return next;
	}
	void setNext(Accum next) {
		this.next = next;
	}
	
	/**
	 * get the Vector ID
	 * @return
	 */
	public long getVectorCode() {
		return vectorCode;
	}
	
	/**
	 * Set the vector ID
	 * @param vectorCode
	 */
	void setVectorCode(long vectorCode) {
		this.vectorCode = vectorCode;
	}
	
	/**
	 * get the vectors tier id
	 * @param tier
	 * @return
	 */
	public long getVectorCodeTier(int tier) {
		//	return vectorTierCode[tier];
		return 0;
	}
	
	/**
	 * Set the vector tier id
	 * @param tier tier number
	 * @param vectorCode vector id
	 */
	void setVectorCodeTier(int tier, long vectorCode) {
		//	this.vectorTierCode[tier] = vectorCode;
	}
	
	/**
	 * get the accumulator type
	 * @return
	 */
	AccumType getAccumulatorType() {
		return AccumType.Default;
	}
	
	/**
	 * is this accumulator to be ignored
	 * @return true if should ignore
	 */
	public boolean isIgnore() {
		return false;
	}
	
	/**
	 * Set this accumulator to be ignored or not
	 * @param ignore true to ignore, false for normal
	 */
	public void setIgnore(boolean ignore) {
	}
	
	
	/**
	 * Get the probability for a value
	 * @param val value to get probability for
	 * @return
	 */
	public double getProbability(long val) {
		if (total == 0) return 0;
		int v = getCount(val);
		if (v == 0) return 0;
		return ((double) v) / total;
	}
	
	/**
	 * Get the ValProb info for a value
	 * @param val value to get info for
	 * @return
	 */
	public ValProb getValProb(long val) {
		if (total == 0) return null;
		int v = getCount(val);
		if (v == 0) return null;
		ValProb vp = new ValProb();
		vp.count = v;
		vp.probability = ((double) v) / total;
		vp.counter = 1;
		vp.value = val;
		return vp;
	}

	/**
	 * Get the correctness of this accumulator
	 *  NOTE: after entanglement this may differ for each and the numberSetHash has the info to resolve
	 * @return 0 - 1 for correctness
	 */
	public double getCorrectness() {
		if (total == 0 || getCrtTotal() == 0) return 0;
		return (double)getCrtTotal()/(double)total;
	}

	/**
	 * get total correct response count from training
	 * @return
	 */
	public int getCrtTotal() {
		return 0;
	}
	
	/**
	 * get total value training instances for this accumulator
	 * @return
	 */
	public int getTotal() {
		return total;
	}
	
	/**
	 * get the total training instances of this value/accumulator
	 * @return
	 */
	public int getValueTotal() {
		int tot = 0;
		List<ValProb> vl = getValPs();
		if (vl.size() > 0) {
			for (int i=0;i<vl.size();i++) tot += vl.get(i).count;
		}
		return tot;
	}
	
	/**
	 * Adjust / set the total for this accumulator
	 * This value should be the same as the value totals OR higher if values are removed
	 * @param newTotal total to set
	 */
	public void adjustTotal(int newTotal) {
		synchronized (this) {
		total = newTotal;
		if (total < 1) total = 1;
		}
	}
	
	/**
	 * Adjust / set the total with an increment for this accumulator
	 * This value should be the same as the value totals OR higher if values are removed
	 * @param newTotal increment to update total with
	 */	
	public void adjustTotalInc(int incTotal) {
		synchronized (this) {
		total += incTotal;
		if (total < 1) total = 1;
		}
	}
	

	/**
	 * modify the probability of a value by weighting it 
	 * @param value valueId 
	 * @param weight weight to modify by
	 * @return
	 */
	public boolean weightProbability(long value, double weight) {
		// implement in 
		return false;
	}

	/**
	 * to keep the weighting consistent with the granularity of the Accumulator (for AccumInt)
	 * @param prob
	 * @param weight
	 * @return
	 */
	public static double weightProb(double prob, double weight) {
		// modify (with the same skew as the accum)
		int cc = (int)Math.round((prob*weight) * (double)Integer.MAX_VALUE);
		if (cc < 0 || cc > Integer.MAX_VALUE) cc = Integer.MAX_VALUE;
		return (((double)cc) / (double)Integer.MAX_VALUE);
	}
	
	/**
	 * get balance in distribution between values
	 * @param dataWidth
	 * @return
	 */
	public abstract double getBalance(int dataWidth);
	
	/**
	 * true if this is the most probable value
	 * @param val valueId
	 * @return
	 */
	abstract boolean isMostProbableValue(long val);
	
	/**
	 * true if this is the most probable value and tie
	 * @param val valueId
	 * @return
	 */
	public abstract boolean isMostProbableValues(long val);
	

	/**
	 * get largest count for a valueId in this Accum
	 * @return count
	 */
	public abstract int getMaxCount();

	// get the first of the most probable: NOTE: this is random but the probaility will be correct thus the count from the value is correct
	abstract ValProb getFirstMostProbable();
	abstract double getFirstMostProbablity();
	abstract long getFirstMostProbablityValue();
	
	List<ValProb> getMostProbable() {
		List<ValProb> vpList = getValPs();
		if (vpList == null || vpList.size() < 2) return vpList;
		// remove non-most
		double prob = vpList.get(0).probability;
		for (int i=0;i<vpList.size();) {
			if (vpList.get(i).probability < prob) vpList.remove(i);
			else i++;
		}
		return vpList;
	}
	 
	/**
	 * get the list of values and probabilities; not sorted
	 * @return
	 */
	public List<ValProb> getValPs() {
		return getValPs(null);
	}
	
	/**
	 * get the list of values and probabilities; not sorted; placed in vpList
	 * @param vpList list to place the ValProbs in
	 * @return
	 */
	public abstract List<ValProb> getValPs(List<ValProb> vpList);
	
	
	/**
	 * get the list of values and probabilities sorted
	 * @return
	 */
	public List<ValProb> getValPsSorted() {
		return getValPsSorted(null);
	}
	
	/**
	 * get the list of values and probabilities sorted, placed in vpList
	 * @param vpList list to place val probs in
	 * @return
	 */
	public List<ValProb> getValPsSorted(List<ValProb> vpList) {
		vpList = getValPs(vpList);
		if (vpList == null) return null;
		Collections.sort(vpList, VegUtil.VpSort);	
		return vpList;
	}
	 
	/**
	 * get full copy of sorted valprobs
	 * @return
	 */
	public List<ValProb> getValPsSortedCopy() {
		List<ValProb> vl = getValPsSorted(null);
		if (vl == null) return null;
		for (int i=0;i<vl.size();i++) {
			vl.set(i, vl.get(i).copy());
		}
		return vl;
	}
	
	// get boolean mode add negative properties for each
	public List<ValProb> getValPsSortedBoolean() {
		return getValPsSortedBoolean(null);
	}
	public List<ValProb> getValPsSortedBoolean(List<ValProb> vpList) {
		vpList = getValPs(vpList);
		//for each value add inverse negetive value
		int sz = vpList.size();
		for (int i=0;i<sz;i++) {
			if (vpList.get(i).count >= this.total) continue;
			ValProb ivp = vpList.get(i).copy();
			ivp.value = -(ivp.value);
			ivp.count = this.total - ivp.count;
			ivp.probability = ((double) ivp.count) / (double)this.total;
			vpList.add(ivp);
		}
		Collections.sort(vpList, VegUtil.VpSort);	
		return vpList;
	}
	
	// limited get and amplify
	public List<ValProb> getValPsAmplifyLimitSet(long [] ampSet) {
		return getValPsAmplifyLimitSet(null, ampSet, -1);
	}
	public List<ValProb> getValPsAmplifyLimitSet(List<ValProb> vpList, long [] ampSet, int noiseLimit) {
		if (vpList != null) vpList.clear();
		vpList = getValPsSorted(vpList);
		if (vpList == null) return null;
		int tot = 0;
		double sum = 0, fsum = 0;
		int totCount = 0, partCount = 0;
		int cnt = 0;
		
		for (int i = 0; i < vpList.size(); i++) {
			totCount += vpList.get(i).count;
		}

		// clear the no-noes
		for (int i = 0; i < vpList.size(); i++) {
			ValProb vp = vpList.get(i);
			fsum += vp.probability;
			totCount += vp.count;
			if (!containsv(ampSet, vp.value)) {
			//	if (noiseLimit <= 0 || vpList.size() < noiseLimit) tot++;  // noise filter hack
				if (noiseLimit <= 0 || cnt < noiseLimit) tot++;  // noise filter hack
				else {
					sum += vp.probability;
					partCount += vp.count;
				}
				vpList.remove(i);
				i--;
			} else {
				sum += vp.probability;
				partCount += vp.count;
			}
			cnt++;
		}
		if (vpList.size() < 1) return vpList;
		if (tot == 0) return vpList;
		
		double mod = fsum / sum;
		//double mod = (double)totCount / (double)partCount;

		// upate probabilities
		for (int i = 0; i < vpList.size(); i++) {
			ValProb vp = vpList.get(i);
			//if (totCount != this.getTotal()) vp.probability = (double)vp.count / (double) totCount;  // probability on all may be reduced do to attenuation		
			vp.probability = vp.probability * mod;
		}
		Collections.sort(vpList, VegUtil.VpSort);
		return vpList;
	}

	public List<ValProb> getValPsAmplifySet( List<ValProb> vpList, long [] ampSet, int noiseLimit) {
		if (vpList != null) vpList.clear();

		vpList = getValPsSorted(vpList);
		if (vpList == null) return null;
		int tot = 0;
		int totCount = 0, partCount = 0;
		double sum = 0, fsum = 0;
		int cnt = 0;

				
		// clear the no-noes
		for (int i = 0; i < vpList.size(); i++) {
			ValProb vp = vpList.get(i);
			fsum += vp.probability;
			totCount += vp.count;
			if (!containsv(ampSet, vp.value)) {
				if (noiseLimit <= 0 || cnt < noiseLimit) tot++;  // noise filter hack
				else {
					sum += vp.probability;
					partCount += vp.count;
				}
			} else {
				sum += vp.probability;
				partCount += vp.count;
			}
			cnt++;
		}
		if (vpList.size() < 1) return vpList;
		if (tot == 0) return vpList;
		double mod = fsum / sum;
		//	double mod = (double)totCount / (double)partCount; // probability on all may be reduced do to attenuation
		
		for (int i = 0; i < vpList.size(); i++) {
			ValProb vp = vpList.get(i);
			if (containsv(ampSet, vp.value)) {
				vp.probability = vp.probability * mod;
			}
		}
		Collections.sort(vpList, VegUtil.VpSort);		
		return vpList;
	}
	private boolean containsv(long [] valSet, long v) {
		for (int i=0; i < valSet.length; i++) {
			if (valSet[i] == v) return true;
		}
		return false;
	}
		
	public List<ValProb> getCrtValPsSorted(List<ValProb> vpList) {
		vpList = getCrtValPs(vpList);
		if (vpList == null) return null;
		Collections.sort(vpList, VegUtil.VpSort);	
		return vpList;
	}
	public List<ValProb> getCrtValPs(List<ValProb> vpList) {
		return null;
	}
	
	/**
	 * Get count of unique values in this Accum
	 * @return
	 */
	public abstract int getValueCount();


	/**
	 * Add 1 count for value
	 * @param val valueId
	 * @return
	 */
	int addCount(long val) {
		return addCount(val, 1);
	}
	
	/**
	 * Add count for value
	 * @param val valueId
	 * @param count count to increase by
	 * @return
	 */
	abstract int addCount(long val, int count);
	
	/**
	 * Add 1 correct count for value
	 * @param val valueId
	 * @return
	 */
	int addCrtCount(long val) {
		return addCrtCount(val, 1);
	}
	
	/**
	 * Add correct count for value
	 * @param val valueId
	 * @param count count to increase by
	 * @return
	 */
	abstract int addCrtCount(long val, int count);
	
	/**
	 * Set the count and correct count for a value
	 * values must be >= 0
	 * @param val valueId
	 * @param count
	 * @param crtCount
	 * @return
	 */
	abstract int setCount(long val, int count, int crtCount);
	
	/**
	 * reduce the count for a value, must be >= 0
	 * @param val
	 * @param count
	 * @return
	 */
	abstract int reduceCount(long val, int count);
	
	/**
	 * get count for valueId
	 * @param val valueId
	 * @return
	 */
	public abstract int getCount(long val);
	
	/**
	 * Get correct count for valueId
	 * @param val valueId
	 * @return
	 */
	public abstract int getCrtCount(long val);
	
	/**
	 * check if Accum has a val
	 * @param val valueId
	 * @return true if exists, else false
	 */
	public abstract boolean hasValue(long val);
	
	/**
	 * Get a full copy of this Accum
	 * @return
	 */
	public abstract Accum copy();
	
	/**
	 * make this Accum a copy of from
	 * @param from Accum to copy content from
	 */
	public abstract void copyFrom(Accum from);


	/**
	 * merge Accum into this Accum: for each value add count
	 * @param ac Accum to merge into this one
	 */
	public void merge(Accum ac) {
		ac.mergeInto(this);
	}
	
	abstract void mergeInto(Accum intoAc);

	/**
	 * Remove a value from this Accum
	 * NOTE: this will modify the total, thus all probabilities
	 * to prevent this, retain the total and adjust after the removal
	 * @param val valueId to remove
	 * @return new total
	 */
	abstract int remove(long val);
	
	/**
	 * Merge or move value into new value
	 * 
	 * @param value valueId to move to
	 * @param oldValue valueId move from
	 * @return count of value
	 */
	public int mergeValue(long value, long oldValue) {
		int c = this.getCount(oldValue);
		if (c > 0) {
			this.addCount(value, c);
			this.remove(oldValue);
		}
		return this.getCount(value);
	}
	
	/**
	 * Compare Accums, true if same values and probability of each
	 * @param ac Accum to compare to
	 * @return
	 */
	public abstract boolean compare(Accum ac);

	abstract void clear();
	
	// lock this accum
	void lock(long value) {}
	boolean isLocked() {
		return false;
	}

	/**
	 * 
	 * @param vm map to place values in
	 */
	public void getValueSet(HashMap<Long, Integer> vm) { }


	public String getDistString() {
		if (total == 0) return "NONE";
		List<ValProb> vpList = getValPsSorted();
		String s = "";
		
		for (int i=0;i<vpList.size();i++) {
			ValProb vp = vpList.get(i);		
			s += "<"+vp.value+" "+vp.count+"/"+vp.probability + "> ";
		}
		return s;
	}
	public String getDistString(VDataPlane dp) {
		if (total == 0) return "NONE";
		List<ValProb> vpList = getValPsSorted();
		String s = "";
		
		for (int i=0;i<vpList.size();i++) {
			ValProb vp = vpList.get(i);		
			s += "<\'"+dp.getString(vp.value)+"\' "+vp.count+"/"+vp.probability + "> ";
		}
		return s;
	}
	
	public int diff(Accum ac) {
		int cnt = 0;
		if (ac.total != this.total) {
			cnt++;
			System.out.println("    DIFF[ac]["+this.getVectorCode()+"] total["+ac.total+"]["+this.total+"]");
		}
		if (ac.getVectorCode() != this.getVectorCode()) {
			cnt++;
			System.out.println("    DIFF[ac]["+this.getVectorCode()+"] vectCode["+ac.getVectorCode()+"]["+this.getVectorCode()+"]");
		}
		if (ac.getValueCount() != this.getValueCount()) {
			cnt++;
			System.out.println("    DIFF[ac]["+this.getVectorCode()+"] valcount["+ac.getValueCount()+"]["+this.getValueCount()+"]");
		}
		//protected int vsid;
		return cnt;
	}
	
	void optimize() {
		// make this Accum optimal for the data
	}
}
