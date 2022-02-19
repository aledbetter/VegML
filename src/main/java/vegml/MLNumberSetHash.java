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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import vegml.VDataPlane.NSVectMap;
import vegml.ValProb;


/**
 * internal class to manage a single numberSet for a Dataplane
 * This is where the accumulators live, solid models don't need this object
 * 
 */
class MLNumberSetHash implements java.io.Serializable, Iterable<Accum> {
	private static final long serialVersionUID = 8376313452171752092L;

	private static final int BASE_SIZE = 2048;
	private static final int MIN_D_SIZE = 512;
	private static final int MIN_SIZE = 64;
	
	private static final double BASE_GROW_FACTOR = 2;	
	private static final int MAX_CHAIN = 25;	
	
	private static int NSH_ID_BASE = 12;
	
	private Accum [] aclist = null;
	private int size = 0;
	private boolean turnedOff;
	private int setNumber;
	private List<Integer> numberSet = null;
	private List<List<Integer []>> numberSetTier = null;
	private double prob = 0;		// probabilty of correct
	private double probSingle = 0; // probability of a single value vector correct
	
	private int nid = -1; // id
	
	private TLongObjectHashMap<NSVectMap> vectVectMap;
	
	// set accumulators 
	private Accum recallAnswerAccum;		// answer correct
	private Accum predictAnswerAccum;		// answer correct
	private Accum probAccum;				// value counts

	// solid model..
	private TLongIntHashMap idSetMap;
	private boolean solid = false;
	
	// stats for transient
	private transient int valMaxCount = -1;
	private transient int valMinCount = -1;
	private transient int valAvgCount = -1;

	//
	// Constructor
	//
	MLNumberSetHash() {
		aclist = new Accum [BASE_SIZE];
		prob = probSingle = 0;
		this.nid = NSH_ID_BASE++;
	}
	MLNumberSetHash(int setNumber, List<Integer> numberSet, List<List<Integer []>> numberSetTier) {
		this(BASE_SIZE, setNumber, numberSet, numberSetTier);
	}
	//
	// Constructor; set base bucket size
	//
	MLNumberSetHash(int baseSize, int setNumber, List<Integer> numberSet, List<List<Integer []>> numberSetTier) {
		aclist = new Accum[baseSize];
		this.vectVectMap = new TLongObjectHashMap<>();
		this.turnedOff = false;
		this.setNumber = setNumber;
		this.numberSet = numberSet;
		this.numberSetTier = numberSetTier;
		this.prob = 0;
				
		this.probAccum = new AccumIntHm();						
		this.probAccum.setVectorCode(setNumber);
		
		this.recallAnswerAccum = new AccumIntHmCrt();
		this.recallAnswerAccum.setVectorCode(setNumber);	
				
		this.predictAnswerAccum = new AccumIntHmCrt();
		this.predictAnswerAccum.setVectorCode(setNumber);	
		this.solid = false;
		this.nid = NSH_ID_BASE++;
	}
	
	
	//
	// check if this hash for a dimension/setNumber is on or off; if off no training on it
	//
	boolean isTurnedOff() {
		return turnedOff;
	}
	
	//
	// set this hash for a dimension/setNumber to on or off; if off no training on it
	//
	void setTurnedOff(boolean turnedOff) {
		this.turnedOff = turnedOff;
	}
	
	//
	// ID for this nsh
	//
	int getNid() {
		return nid;
	}
	
	//
	// true if solid model
	//
	boolean isSolid() {
		return solid;
	}
	
	//
	// Get the setnumber
	//
	int getSetNumber() {
		return setNumber;
	}
	
	//
	// probability of correct response
	//
	double getProb() {
		return prob;
	}
	void setProb(double prob) {
		this.prob = prob;
	}
	
	//
	// probability of correct response when single value
	//
	double getProbSingle() {
		return probSingle;
	}
	void setProbSingle(double prob) {
		this.probSingle = prob;
	}

	//
	// Get the setnumber
	//
	void setSetNumber(int setNumber) {
		this.setNumber = setNumber;
		this.probAccum.setVectorCode(setNumber);
		this.recallAnswerAccum.setVectorCode(setNumber);	
		this.predictAnswerAccum.setVectorCode(setNumber);	
	}
	List<Integer> getNS() {
		return numberSet;
	}
	void setNumberSet(List<Integer> numberSet, List<List<Integer []>> numberSetTier) {
		this.numberSet = numberSet;
		this.numberSetTier = numberSetTier;
	}
	int getNSSize() {
		return numberSet.size();
	}
	List<List<Integer []>> getNSTier() {
		return numberSetTier;
	}
	
	//
	// put this Accum for vector code, check remapping
	//
	Accum put(Accum accum) {
		if (isSolid()) return null;
		NSVectMap vidM = getVectorMapVector(accum.getVectorCode());
		if (vidM != null) return putDirect(accum, vidM.vectorCode);
		return putDirect(accum, accum.getVectorCode());
	}
	
	//
	// put this Accum for vector code, no-remapping
	//
	Accum putDirect(Accum accum, long vmid) {
		if (isSolid()) return null;
		int hc = getHash(vmid, aclist.length);
		Accum lac = null;
		Accum ac = aclist[hc];
		int cnt = 0;
		while (ac != null) {
			if (ac.getVectorCode() == vmid) {
				if (ac == accum) return ac; // same object
				accum.setNext(ac.getNext());
				ac.setNext(null);
				if (lac != null) lac.setNext(accum);
				else aclist[hc] = accum;
				return accum;
			}
			cnt++;
			lac = ac;
			ac = ac.getNext();
		}
		accum.setNext(null);
		if (lac != null) lac.setNext(accum);
		else aclist[hc] = accum;
		this.size++;
		checkAndGrow(cnt);
		return accum;
	}
	
	//
	// update a value count
	//
	void addVectorValueCount(long vectorCode, long value) {
		addVectorValueCount(vectorCode, value, 1);
	}
	void addVectorValueCount(long vectorCode, long value, int upCount) {
		addVectorValueCount(vectorCode, value, upCount, 0);
	}
	void addVectorValueCount(long vectorCode, long value, int upCount, int downCount) {
		if (isSolid()) return;
		Accum ac = this.get(vectorCode);
		if (ac == null) return;

		if (upCount > 0) ac.addCount(value, upCount);
		else if (downCount > 0) ac.reduceCount(value, downCount);	
	}
	// down count anything not in list
	void addVectorValueCount(long vectorCode, List<Long> valueIds, int upCount, int downCount) {
		if (isSolid()) return;
		Accum ac = this.get(vectorCode);
		if (ac == null) return;
		List<ValProb> vpList = ac.getValPs();
		int tot = ac.getTotal();
		for (ValProb vp:vpList) {
			if (valueIds.contains(vp.value)) {
				if (upCount > 0) ac.addCount(vp.value, upCount);
			} else {
				if (downCount > 0) {
					int c = ac.getCount(downCount);
					if (c <= downCount) {
						c = (downCount -c)+1; // to 1
						ac.reduceCount(vp.value, downCount);
					}
					else ac.reduceCount(vp.value, downCount);	
				}
			}
		}
		ac.adjustTotal(tot);
	}	
	 
	
	//
	// update the count on matching accume; check remapping, upate remap-count
	// if found return accume, else null
	//
	Accum addCount(long vectorCode, long value) {
		if (isSolid()) return null;
		Accum ac = null;
		NSVectMap vidM = getVectorMapVector(vectorCode);
		if (vidM != null) {
			ac = getDirect(vidM.vectorCode);
			if (ac != null) vidM.count++;
		}
		ac = getDirect(vectorCode);
		if (ac == null) return null;
		ac.addCount(value);
		return ac;
	}
	// add the to total only not the value
	Accum addTotal(long vectorCode, long value) {
		if (isSolid()) return null;
		Accum ac = getDirect(vectorCode);
		if (ac == null) return null;
		ac.adjustTotal(ac.total+1);
		return ac;
	}
	
	//
	// get sum of totals
	//
	int getTotalSum() {
		if (isSolid()) return 0;
		int tot = 0;
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			tot += ac.getTotal();
		}
		return tot;
	}
		
	//
	// get this Accum for vector code, check remapping
	//
	Accum get(long vectorCode) {
		if (isSolid()) return null; // solid
		NSVectMap vidM = getVectorMapVector(vectorCode);
		if (vidM != null) return getDirect(vidM.vectorCode);
		return getDirect(vectorCode);
	}
	
	//
	// get this Accum for vector code; no remapping
	//
	Accum getDirect(long vmid) {
		if (isSolid()) return null; // solid
		int hc = getHash(vmid, aclist.length);
		Accum ac = aclist[hc];
		int cnt = 0;
		while (ac != null) {
			if (ac.getVectorCode() == vmid) return ac;
			ac = ac.getNext();
			cnt++;
			//if (cnt > MAX_CHAIN_WARN) System.out.println("AccumHash["+size+"] cnt["+cnt+"] hc["+hc+"] ["+vmid+"] CURR: "+aclist.length);
		}
		return null;
	}
	
	boolean remove(long vectorCode) {
		NSVectMap vidM = getVectorMapVector(vectorCode);
		if (vidM != null) return removeDirect(vidM.vectorCode);
		return removeDirect(vectorCode);
	}
	boolean removeDirect(long vmid) {
		int hc = getHash(vmid, aclist.length);
		Accum lac = null;
		Accum ac = aclist[hc];
		
		while (ac != null) {
			if (ac.getVectorCode() == vmid) {
				if (lac != null) lac.setNext(ac.getNext());
				else aclist[hc] = ac.getNext();
				ac.setNext(null);
				size--;
				return true;
			}
			lac = ac;
			ac = ac.getNext();
		}
		return false;
	}
	
	//
	// remove this value from all accums
	// retain existing probability
	//
	int removeAllValue(long value, boolean retainProbability) {
		if (isSolid()) return 0;

		int cnt = 0;
		List<Accum> del = new ArrayList<>();
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			int tot = ac.getTotal();
			int c = ac.remove(value);
			if (ac.getValueCount() == 0) del.add(ac);
			else if (c > 0) {
				cnt += c;
				if (retainProbability) ac.adjustTotal(tot);
			}
		}
		for (Accum ac:del) {
			this.removeDirect(ac.vectorCode);	
		}
		return cnt;
	}
	
	//
	// remove all accum with no values
	//
	int removeAllEmptyAccum() {
		if (isSolid()) return 0;
				
		List<Accum> del = new ArrayList<>();
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			if (ac.getValueCount() <= 0) del.add(ac);
		}
		for (Accum ac:del) this.removeDirect(ac.vectorCode);
		return del.size();
	}
	
	//
	// remove all that are in the list
	//
	int removeAllValues(HashMap<Long, HashMap<Long, Integer>> removeLists, boolean retainProbability) {	
		if (this.isSolid()) return 0;
		int cnt = 0;
	
		List<Accum> del = new ArrayList<>();
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			HashMap<Long, Integer> rm = removeLists.get(ac.vectorCode);
			if (rm == null) continue;

			int tot = ac.getTotal();
			for (Long value:rm.keySet()) {
				int c = ac.remove(value);
				if (c > 0) {
					cnt += c;
					if (ac.getValueCount() == 0) {
						del.add(ac);
						break;
					}
				}
			}
			if (retainProbability) ac.adjustTotal(tot);
		}
		for (Accum ac:del) this.removeDirect(ac.vectorCode);
		
		return cnt;
	}
	
	//
	// remove all except those in the list
	//
	int removeAllVectorsExcept(List<Long> exceptionList) {	
		if (this.isSolid()) return 0;
		int cnt = 0;
	
		List<Accum> del = new ArrayList<>();
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			if (exceptionList.contains(ac.getVectorCode())) continue;
			del.add(ac);
		}
		for (Accum ac:del) this.removeDirect(ac.vectorCode);
		
		return cnt;
	}
	
	//
	// remove all except those in the list
	//
	int removeAllVectors(List<Long> vidList) {	
		if (this.isSolid()) return 0;
		int cnt = 0;
		for (Long vid:vidList) {
			if (this.removeDirect(vid)) cnt++;
		}
		return cnt;
	}
	
	//
	// devalue all that are in the list
	//
	int weightAllValues(HashMap<Long, HashMap<Long, Integer>> removeLists, double weight) {	
		if (this.isSolid()) return 0;
		int cnt = 0;
	
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			HashMap<Long, Integer> rm = removeLists.get(ac.vectorCode);
			if (rm == null) continue;
			for (Long value:rm.keySet()) {
				if (ac.weightProbability(value, weight)) cnt++;
			}
		}
		
		return cnt;
	}	
	//
	// remove this value from all IF maxCount is >= maxCount
	// retain existing probability
	//
	int removeAllValue(long value, int minCount, int maxCount, boolean retainProbability, HashMap<Long, Integer> exMap) {
		if (isSolid()) return 0;
		
		int cnt = 0;
		List<Accum> del = new ArrayList<>();
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			int cv = ac.getCount(value);
			if (cv < 1) continue;
			
			boolean remove = false;
			if (minCount >= 0 && (cv <= minCount || minCount == 0)) remove = true;
			if (maxCount >= 0 && (cv >= maxCount || maxCount == 0)) remove = true;
			if (!remove) continue;	
			
			if (exMap != null) {
				Integer n = exMap.get(ac.vectorCode);
				if (n != null && n == 0) continue; // exclusion
			}
			
			int tot = ac.getTotal();
			int c = ac.remove(value);
			if (c > 0) {
				cnt += c;
				if (ac.getValueCount() == 0) del.add(ac);
				else if (retainProbability) ac.adjustTotal(tot);
			}
		}
		for (Accum ac:del) this.removeDirect(ac.vectorCode);
		return cnt;
	}
	
	//
	// weight  value from all IF maxCount is >= maxCount
	// retain existing probability
	//
	int weightAllValue(long value, int minCount, int maxCount, HashMap<Long, Integer> exMap, double weight) {
		if (isSolid()) {
			return 0;
		}
		int cnt = 0;
		List<Accum> del = new ArrayList<>();
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			int cv = ac.getCount(value);
			
			boolean remove = false;
			if (minCount >= 0 && (cv <= minCount || minCount == 0)) remove = true;
			if (maxCount >= 0 && (cv >= maxCount || maxCount == 0)) remove = true;
			if (!remove) continue;			
			if (exMap != null) {
				Integer n = exMap.get(ac.vectorCode);
				if (n != null && n == 0) continue; // exclusion
			}	
			if (ac.weightProbability(value, weight)) cnt++;
		}
		for (Accum ac:del) this.removeDirect(ac.vectorCode);
		return cnt;
	}
	
	
	/**
	 * remove accumulators if maxCount is >= maxCount
	 * 
	 * @param maxTotal
	 * @param retainProbability if true retain existing probability
	 * @return
	 */
	int removeAllNSAccum(int maxTotal, boolean retainProbability) {
		if (isSolid()) {
			return 0;
		}
		List<Accum> del = new ArrayList<>();
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			if (ac.getTotal() > maxTotal) continue;
			del.add(ac);
		}
		int cnt = 0;
		for (Accum ac:del) {
			this.removeDirect(ac.vectorCode);
			cnt++;
		}
		return cnt;
	}
	
	//
	// remove from all IF maxCount is >= maxCount
	// retain existing probability
	//
	int removeAllNSByVotes(HashMap<Long, Integer> nsVotes, int minVotes) {
		if (isSolid()) return 0;
		
		List<Accum> del = new ArrayList<>();
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			Integer votes = nsVotes.get(ac.vectorCode);				
			if (votes != null && votes >= minVotes) continue; // has votes
			del.add(ac);
		}
		int cnt = 0;
		for (Accum ac:del) { 
			this.removeDirect(ac.vectorCode);
			cnt++;
		}
		return cnt;
	}
	
	//
	// remove from all IF maxCount is >= maxCount
	// retain existing probability
	//
	int removeAllNSByVotesExclude(HashMap<Long, Integer> removeList) {
		if (isSolid()) return 0;
		
		List<Accum> del = new ArrayList<>();
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			Integer votes = removeList.get(ac.vectorCode);				
			if (votes == null) continue; // has votes
			del.add(ac);
		}
		int cnt = 0;
		for (Accum ac:del) { 
			this.removeDirect(ac.vectorCode);
			cnt++;
		}
		return cnt;
	}
	
	// 
	// rebase all Accumulator totals with the trained value counts
	// - also removes the value
	//
	void updateAccumTotalsWithTrainedValue(long value, boolean remove) {
		if (isSolid()) return;
		
		// set accumulator
		int tot = this.getAccumSetDefault().getCount(value);
		if (remove) this.getAccumSetDefault().remove(value);
		this.getAccumSetDefault().adjustTotal(tot);
		
		List<Accum> del = new ArrayList<>();
		Iterator<Accum> it = this.iterator();
		int nf = 0;
		while (it.hasNext()) {
			Accum ac = it.next();
			int count = ac.getCount(value);
			if (remove) {
				ac.remove(value);
				if (ac.getValueCount() == 0) del.add(ac);
			}
			if (count > 0) {
				ac.adjustTotal(count);
			} else {
				nf++;
			}
		}
		for (Accum ac:del) { 
			this.removeDirect(ac.vectorCode);
		}
		//if (nf > 0) System.out.println("ns-baseline["+this.getSetNumber()+"] NOT FOUND: " + nf);
	}
	
	//
	// Update Accumulator Totals to match actuals - remove attenuation
	//
	int syncAccumTotals() {
		if (isSolid()) {
			return 0;
		}
		int cnt = 0;
		List<Accum> del = new ArrayList<>();
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			int totr = ac.getValueTotal();
			ac.adjustTotal(totr);
		}
		for (Accum ac:del) this.removeDirect(ac.vectorCode);
		return cnt;
	}
	
	//
	// get list of values/counts here
	// NOTE: this should be consistant with SAC but after mods it is not
	//
	// @return map of value to count
	HashMap<Long, Integer> getValueSet() {
		HashMap<Long, Integer> map = new HashMap<>();
		if (isSolid()) return map;
		
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			ac.getValueSet(map);
		}
		return map;
	}
	
	//
	// get the  count of a value in an accums
	//
	int getValueCount(long value) {
		if (isSolid()) {
			return 0;
		}
		int Cnt = 0;
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			int mx = ac.getCount(value);
			if (mx > 0) Cnt++;
		}
		return Cnt;
	}
	//
	// get the maximum count of a value in an accume
	//
	int getValueMaxCount(long value) {
		if (isSolid()) {
			return 0;
		}
		int maxCount = 0;
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			int mx = ac.getCount(value);
			if (mx > maxCount) maxCount = mx;
		}
		return maxCount;
	}
	int getSingleValueCountMax() {
		if (isSolid()) {
			return 0;
		}
		int maxCount = 0;
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			int mx = ac.getMaxCount();
			if (mx > maxCount) maxCount = mx;
		}
		return maxCount;
	}
	
	/**
	 * get the min/max/avg count of values per vector
	 * @return
	 */
	int getValueCountMax() {
		getValueCounts();
		return valMaxCount;
	}
	int getValueCountMin() {
		getValueCounts();
		return valMinCount;
	}
	int getValueCountAvg() {
		getValueCounts();
		return valAvgCount;
	}
	
	void resetValueCounts() {
		valAvgCount = valMaxCount = valMinCount = -1;
	}

	private void getValueCounts() {
		if (isSolid()) {
			return;
		}
		if (valMinCount >= 0) {
			return;
		}
		int maxCount = 0, minCount = -1;
		long totCount = 0;
		int cnt = 0;
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			int mx = ac.getValueCount();
			if (mx > maxCount) maxCount = mx;
			if (mx < minCount || minCount < 0) minCount = mx;
			totCount += mx;
			cnt++;
		}
		if (cnt == 0) {
			valAvgCount = valMaxCount = valMinCount;
		} else {
			valAvgCount = (int)(totCount/cnt);	
			valMaxCount = maxCount;
			valMinCount = minCount;
		}
		return;
	}

	
	
	//
	// get number of accums
	//
	int size() {
		return size;
	}
	
	//
	// get number of mappings
	//
	int sizeMapping() {
		return vectVectMap.size();
	}
	
	
	//
	// get count of hash buckets
	//
	int getBucketCount() {
		return aclist.length;
	}
	
	//
	// iterator for the HashTable
	//
    @Override
    public Iterator<Accum> iterator() {
        Iterator<Accum> it = new Iterator<Accum>() {
            protected int currentBucket = 0;
            protected Accum lastAccum = null;
            
            @Override
            public boolean hasNext() {
            	int i = currentBucket;
            	if (lastAccum != null) {
            		if (lastAccum.getNext() != null) return true; // in same bucket
            		i++;
            	}
            	// next bucket
            	for (;i<aclist.length;i++) {
            		if (aclist[i] != null) return true;
            	}
            	return false;
            }

            @Override
            public Accum next() {
        		// next in bucket
            	if (lastAccum != null) {
        			lastAccum = lastAccum.getNext();
        			if (lastAccum != null) return lastAccum; // in same bucket next  		
            		// end bucket so next
            		currentBucket++;
            	} 

            	// next bucket
            	for (;currentBucket<aclist.length;currentBucket++) {
            		if (aclist[currentBucket] != null) {
            			lastAccum = aclist[currentBucket];
                    	//System.out.println("getNext3["+currentBucket+"] " + lastAccum);
            			return lastAccum;
            		}
            	}
                return null;
            }

            @Override
            public void remove() {
               return;
            }
        };
        return it;
    }
	
	//
	// Check hash table for need to grow, resize it for better fit
    //
	private void checkAndGrow(int bucketSize) {	
		int avgChain = size/aclist.length;
		if (avgChain < MAX_CHAIN && bucketSize < MAX_CHAIN) return;
		int nsize = (int)(aclist.length*BASE_GROW_FACTOR);
		if (nsize < BASE_SIZE) nsize = BASE_SIZE;
		if (nsize == this.getBucketCount()) {
			nsize = nsize*2;
		}
		resizeHash(nsize);
	}
	
	//
	// resize the hash for best fit
	//
	private void reBalanceHash() {	
		int nsize = (size/(int)MAX_CHAIN)*2;
		if (nsize < MIN_D_SIZE) {
			nsize = (size/(int)MAX_CHAIN)*4;
			if (nsize < MIN_SIZE) nsize = MIN_SIZE; // can go small
			else nsize = MIN_D_SIZE;
		}
		resizeHash(nsize);
	}
	
	//
	// resize the hash and move what needs to move
	//
	private void resizeHash(int nsize) {	
		//System.out.println("AccumHash["+size+"] GROW: "+aclist.length+" -> " + nsize);
		// grow it
		Accum [] oldlist = aclist;
		aclist = new Accum [nsize];
		size = 0;
		
		for (int i=0;i<oldlist.length;i++) {
			// have to re-add all to get the correct buckets
			Accum ac = oldlist[i];
			while (ac != null) {				
				Accum mvc = ac;
				ac = ac.getNext();
				mvc.setNext(null);
				//putDirect(mvc, mvc.getVectorCode());
				int hc = getHash( mvc.getVectorCode(), aclist.length);
	// TODO: optimize, order in bucket based on getTotal()
				mvc.setNext(aclist[hc]);
				aclist[hc] = mvc;
				this.size++;
			}
		}
	}

	//
	// get the hash bucket for a vectorCode
	//
	private static int getHash(long vectorCode, int buckets) {
		//int val = (int)((vectorCode >> 32) ^ vectorCode); // fold it
		if (buckets < Short.MAX_VALUE) {
			int val = (int)((vectorCode >>> 32) ^ vectorCode ^ (vectorCode >>> 16) ^ (vectorCode >>> 48)); // fold it
			return Math.floorMod(val, buckets);
		}
		// larger
		int val = (int)((vectorCode >>> 32) ^ vectorCode); // fold it
		return Math.floorMod(val, buckets);
	}
	
	//
	// Check if this hash is entangled
	//
	boolean isEntangled() {
		if (sizeMapping() > 0) return true;
		return false;
	}
	
	//
	// merge all the Accum into this hash
	//
	int merge(VDataPlane dataPlane, MLNumberSetHash xhm, VDataPlane xdataPlane) {
		if (xhm == null || xhm.size() < 1) return size;
		if (xhm == this) return size;
		if (this.size() == 0) {
			this.turnedOff = xhm.turnedOff;
			if (xhm.vectVectMap != null) {
				this.vectVectMap = new TLongObjectHashMap<>();
				Object o[] = xhm.vectVectMap.values();
				for (int i=0;i<o.length;i++) {
					NSVectMap vm = (NSVectMap)o[i];
					this.vectVectMap.put(vm.vectorCode, vm);
				}
			}
			if (xhm.idSetMap != null) {
				this.idSetMap = new TLongIntHashMap();
				this.idSetMap.putAll(xhm.idSetMap);
			}		
		}

		if (xhm.isSolid()) {
			// FIXME
			this.solid = xhm.solid;
		} else {
			boolean entangled = dataPlane.isEntangled();
			boolean xentangled = xhm.isEntangled();
			VegML vML = dataPlane.getVegML();
			VegML xML = xdataPlane.getVegML();
			
			// deentangle both
			vML.deentanglement(dataPlane, this);
			xML.deentanglement(xdataPlane, xhm);
	
			int mCnt = 0, aCnt = 0, vsaCnt = 0, vsaFCnt = 0, ssize = this.size, vsStart = vML.vectSetMap.getCount();
			Iterator<Accum> it = xhm.iterator();
			while (it.hasNext()) {
				Accum xac = it.next();
				// check vsid mapping the VSID
		//		int nvsid = svidMap.get(xac.getVectSetId());
			
				Accum tac = this.get(xac.getVectorCode());			
				if (tac == null) {
					long nvsid = xac.getVectorCode();
					// add
					if (nvsid == 0 && xac.getVectSetId() >= 0) {
						// no current mapping, add one map
						nvsid = vML.vectSetMap.add(xML.vectSetMap.getBase(xac.getVectSetId()));
				//		svidMap.put(xac.getVectSetId(), nvsid); // add to map
						//System.out.println("PUT["+xac.getVectSetId()+"] => " +nvsid);
						vsaCnt++;
					} else {
						vsaFCnt++;
					}
					Accum nac = xac.copy();	
					//nac.setVectSetId(nvsid);	
					this.putDirect(nac, xac.getVectorCode());
					aCnt++;
				} else {
					// merge
				//	if (nvsid == 0) svidMap.put(xac.getVectSetId(), tac.getVectSetId()); // add to map
					tac.merge(xac);
					mCnt++;
				}
			}
		
			//System.out.println("  MERG_AH["+getSetNumber()+"] merge: " + mCnt + " add: " + aCnt + " size[" + ssize + " => "+size+" X: " + xhm.size+ "] "
			//	+ "vs["+vsaCnt+"/"+vsaFCnt+" : "+vsStart+" => "+vML.vectSetMap.getCount() +"]ent["+entangled+"/"+xentangled+"]");
		
			// re-engangle
			if (entangled) vML.entangle(dataPlane, this);
			if (xentangled) xML.entangle(xdataPlane, xhm);
		}
		
		this.probAccum.merge(xhm.probAccum);
		this.recallAnswerAccum.merge(xhm.recallAnswerAccum);
		this.predictAnswerAccum.merge(xhm.predictAnswerAccum);

		// other info
		this.nid = xhm.nid;
		this.prob = xhm.prob;
		this.probSingle = xhm.probSingle;

		optimize();

		return size;
	}

	//
	// diff 2 AccumeHash
	//
	int diff(VegML vML, VDataPlane dataPlane, VegML xML, MLNumberSetHash xhm, VDataPlane xdataPlane) {
		int cnt = 0;
		if (this.size != xhm.size) {
			System.out.println("    DIFF[ah]["+setNumber+"] is["+this.size+" vs "+xhm.size+"]");
			cnt++;
		}
		// set accumulator
		int ccnt = this.probAccum.diff(xhm.probAccum);
		if (ccnt > 0) {
			cnt += ccnt;
			System.out.println(" *DIFF[ah]["+setNumber+"][sac] diffs["+ccnt+"] ");		
		}	

		ccnt = this.recallAnswerAccum.diff(xhm.recallAnswerAccum);
		if (ccnt > 0) {
			cnt += ccnt;
			System.out.println(" *DIFF[ah]["+setNumber+"][aac] diffs["+ccnt+"] ");		
		}
		// FIXME
		// predict
		
		// FIXME
		
		// diff stats
		// TODO??
		int vCnt = 0;
		Iterator<Accum> it = xhm.iterator();
		while (it.hasNext()) {
			Accum xac = it.next();
			Accum tac = this.get(xac.getVectorCode());
			if (tac == null) {
				vCnt++;
			} else {
				cnt += tac.diff(xac);
			}
		}
		if (vCnt > 0) {
			System.out.println("    DIFF[ah]["+setNumber+"] missing vectors["+vCnt+"]");
			cnt += vCnt;
		}
		return cnt;
	}
	
	
	//
	// clear all entries from map
	//
	void clear() {
		this.size = 0;
		for (int i=0;i<aclist.length;i++) {
			Accum ac = aclist[i];
			while (ac != null) {
				Accum lac = ac;
				ac = ac.getNext();
				lac.setNext(null);
			}
			aclist[i] = null;
		}		
		vectVectMap.clear();		
	}
	
	//
	// clear all mappings from map
	//
	void clearMappings() {
		vectVectMap.clear();
	}
	
	// count the hard way
	private int countAccum() {
		int cnt = 0;
		for (int i=0;i<aclist.length;i++) {
			Accum ac = aclist[i];
			while (ac != null) {
				ac = ac.getNext();
				cnt++;
			}
		}
		return cnt;
	}
	
	//
	// find find an accume in a list
	//
	private Accum find(List<Accum> aList, long vcode) {
		for (int i=0;i<aList.size();i++) {
			if (aList.get(i).getVectorCode() == vcode) return aList.get(i);		
		}
		return null;
	}
		
	//
	// Get all accums as a sorted list; this allocates the list
	//
	List<Accum> getListSorted() {
		List<Accum> aList = new ArrayList<>(this.size);
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			aList.add(ac);
		}
		
		Collections.sort(aList, AccumSort);	
		if (aList.size() != this.size) {
			System.out.println("ERROR: getList["+this.size+" != "+aList.size()+"] " + countAccum());
		}
		return aList;
	}
	
	// 
	// frequency list, for singles only
	//
	List<ValProb> getListFrequencySorted() {
		List<ValProb> vpList = new ArrayList<>(this.size);
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			ValProb vp = new ValProb();
			vp.count = ac.total;
			vp.value = ac.getVectorCode();
			vp.counter = ac.getValueCount();
			vpList.add(vp);
		}
		// sort based on count/counter
		Collections.sort(vpList, VegUtil.VpFreqSort);	
		return vpList;
	}
	// get list if sold
	int [] getListSolid() {
		return idSetMap.values();
	}
	long [] getListSolidVid() {
		return idSetMap.keys();
	}
	


	//
	// sort by probability / valueCount
	//
	static final Comparator<Accum> AccumSort = new Comparator<Accum>() {
        @Override
        public int compare(Accum lvp, Accum rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	    	  
        	double lp = lvp.getFirstMostProbablity();
        	double rp = rvp.getFirstMostProbablity();
        	if (lp < rp) return 1;
        	if (lp > rp) return -1;
        	if (lvp.getValueCount() < rvp.getValueCount()) return 1;
        	if (lvp.getValueCount() > rvp.getValueCount()) return -1; 
        	return 0; 
        }
    };

    
    //
    // Add mapped vector and current count 
    //
    NSVectMap putVectorMapVector(long vectorCode, long vid, int svid, int count) {
    	NSVectMap vidM = new NSVectMap();
		vidM.vectorCode = vid;
		vidM.count = count;
		vidM.vsid = svid;
		NSVectMap v = vectVectMap.put(vectorCode, vidM);
		if (v != null) return v;
		return vidM;
	}
	
	//
	// Get mapped vector 
	//
    NSVectMap getVectorMapVector(long vectorCode) {
		return vectVectMap.get(vectorCode);
	}
	
	//
	// Get mapped vector 
	//
	void removeVectorMapVector(long vectorCode) {
		vectVectMap.remove(vectorCode);
	}
	
	//
	// get the count for a mapped vector
	//
	int getVectorMapCount(long vectorCode) {
		NSVectMap vm = vectVectMap.get(vectorCode);
		if (vm == null) return 0;
		return vm.count;
	}
	
	
	//
	// get the size of mapped vectors
	//
	int getVectorMapSize() {
		return vectVectMap.size();
	}

	//
	// Get all mapps for a mapped to vectorCode
	//
	List<NSVectMap> getVectorMapVectorToSet(long vectorCode) {
		if (vectVectMap.size() < 1) return null;
		
		List<NSVectMap> vidMl = new ArrayList<>();
		
		vectVectMap.forEachEntry((k, v) -> {
			NSVectMap vidM = (NSVectMap)v;
			if (vidM.vectorCode == vectorCode) {
				NSVectMap vm = new NSVectMap();
				vm.vectorCode = k;	// mapped value
				vm.count = vidM.count; // count
				vm.vsid = vidM.vsid;
				vidMl.add(vm);
			}
            return true;
        });		
		return vidMl;
	}
	
	//
	// change vsid by brute force
	//
	void updateVsidBrutus(int vsid, int newVsid) {
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum at = it.next();
			if (at.getVectSetId() == vsid) {
				//System.out.println("   MATB["+vsid+"]-> ["+newVsid+"] ");
				at.setVectSetId(newVsid);
			}
		}
	}
	
	
	//
	// get set accumulator
	//
	Accum getAccumSetDefault() {
		return probAccum;
	}
	
	//
	// get set answer correct accumulator
	//
	Accum getRecallAnswerAccum() {
		return recallAnswerAccum;
	}

	//
	// get set answer correct accumulator
	//
	Accum getPredictAnswerAccum() {
		return predictAnswerAccum;
	}	

	
	//
	// get minimum-maximum and minimum-average of accumes in this set
	//
	double getMinMaxAvg(VegML vML) {
		double minAvg = 10, minMax = 10;

// FIXME
		int vCnt = 0;
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			// ask the accum
			ValProb vp = ac.getFirstMostProbable();
	//		double prob = vML.getCfgPCalc().calculateProb(vML.getCfgProbMethodDefault(), dataPlane, dac, dataPlane.getCfgNSWeightRaw(), i, sac, slac, correctness, vs, vp);

			double mAvg = 0;
			double mMax = 0;
					
		}
	
		return minMax;
	}

	//
	// get the count of vectors herein
	//
	int getVectorCount() {
		int vCnt = 0;
		if (idSetMap != null) {
			vCnt = idSetMap.size();
		} else {
			// not exact
			vCnt += vectVectMap.size(); // mapped
			vCnt += size(); // all others
			// FIXME gets to many if entangled
		}
		return vCnt;
	}
	
	
	//
	// optimize after training complete
	//
	void optimize() {
		vectVectMap.compact();
		if (this.idSetMap != null) {
			idSetMap.compact();
		} else {
			this.reBalanceHash();			
		}
		recallAnswerAccum.optimize();
		predictAnswerAccum.optimize();
		probAccum.optimize();
	}
	
	///////////////////////////////////////////////////////////
	// get the Solidified Accumulator group
	//
	int getSolid(long vid) {
		int id = idSetMap.get(vid);
		if (id <= 0) return -2;
		return id-1;
	}
	
	///////////////////////////////////////////////////////////
	//
	// make solid immutable Model
	//
	int makeSolid(VDataPlane dp, List<double []> probabilitySets, List<int []> groupList, List<int []> valueSets, List<long []> valueSetsL) {
		if (idSetMap != null) return 0;

		// iterate the vectVectMap
		idSetMap = new TLongIntHashMap();
		Accum dac = dp.getAccumDefault();
		
		//System.out.print("   makeSolid NS["+this.getSetNumber()+"]size[" + this.size+"]["+vectVectMap.size()+"] .");
		TLongIntHashMap idTempMap = new TLongIntHashMap();
		
		// hash sets by size to speed lookups (add sorting to improve
		HashMap<Integer, List<Integer>> szVal = new HashMap<>();
		HashMap<Integer, List<Integer>> szProb = new HashMap<>();
		HashMap<Integer, List<Integer>> szGid = new HashMap<>();
		
		if (vectVectMap != null && vectVectMap.size() > 0) {
			// for each VID in this numberSet -> make/get the group, link the probabilities and values
			long [] keys = vectVectMap.keys();
			for (long k:keys) {
				NSVectMap vidM = vectVectMap.get(k);
				// check for existing group	
				Integer cgid = idTempMap.get(vidM.vectorCode);
				if (cgid != idTempMap.getNoEntryKey()) {
					// have group
					idSetMap.put(k, cgid);	
				} else {
					// get Accum			
					Accum ac = this.getDirect(vidM.vectorCode);
					List<ValProb> vpList = ac.getValPsSorted();
					// weight
					for (int i=0;i<vpList.size();i++) {
						ValProb vp = vpList.get(i);
						double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), this.getSetNumber(), true, this.getAccumSetDefault(), ac, vp);
						if (wavgProb <= 0) {
							vpList.remove(i);
							i--;
							continue;
						}
						vp.probability = wavgProb;
						vp.count = 1;
					}
					
					// get probabilitySet / Value
					int ppos = getOrAddProbabilitySet(szProb, probabilitySets, vpList);
					int vpos = getOrAddValueSet(szVal, valueSets, valueSetsL, vpList);
					//vm.count = vidM.count; // count
					//vm.vsid = vidM.vsid;
					// add group and info
					int gid = getOrAddGroupList(szGid, vpList.size(), groupList, ppos, vpos);
					gid++;
					idSetMap.put(k, gid);	
					idTempMap.put(vidM.vectorCode, gid);			
				}
	        }
		}
		
		// now for individuals
		int iCnt = 0;
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			int x = idSetMap.get(ac.vectorCode);
			if (x > 0) continue; // got it
			
			List<ValProb> vpList = ac.getValPsSorted();
			// weight
			for (int i=0;i<vpList.size();i++) {
				ValProb vp = vpList.get(i);
				double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), this.getSetNumber(), true, this.getAccumSetDefault(), ac, vp);
				if (wavgProb <= 0) {
					vpList.remove(i);
					i--;
					continue;
				}
				vp.probability = wavgProb;
				vp.count = 1;
			}
			// get probabilitySet / Value
			int ppos = getOrAddProbabilitySet(szProb, probabilitySets, vpList);
			int vpos = getOrAddValueSet(szVal, valueSets, valueSetsL, vpList);
			//vm.count = vidM.count; // count
			//vm.vsid = vidM.vsid;
			// add group and info
			int gid = getOrAddGroupList(szGid, vpList.size(), groupList, ppos, vpos);
			gid++;
			idSetMap.put(ac.vectorCode, gid);	
			iCnt++;
		}

		this.clear();
		this.aclist = new Accum[2];
		this.vectVectMap = new TLongObjectHashMap<>();
		//System.out.println("... DONE NS["+this.getSetNumber()+"] groups["+groupList.size()+"]prob["+probabilitySets.size()+"]["+valueSets.size()+"] idset["+idSetMap.size()+"]");
		
		this.solid = true;
		return idSetMap.size();
	}
	
	static int getOrAddProbabilitySet(HashMap<Integer, List<Integer>> szProb, List<double []> probabilitySets, List<ValProb> vpList) {
		List<Integer>  szl = szProb.get(vpList.size());
		if (szl == null) {
			szl = new ArrayList<>();
			szProb.put(vpList.size(), szl);
		} else {	
			//szl = probabilitySets;
			for (int i=0;i<szl.size();i++) {
				int pos = szl.get(i);
				// check match
				double [] v = probabilitySets.get(pos);
				if (v.length != vpList.size()) continue;
				boolean match = true;
				for (int x=0;x<vpList.size();x++) {
					if (vpList.get(x).probability != v[x]) {
						match = false;
						break;
					}
				}
				if (match) return pos;
			}
		}
		// add new
		double [] p = new double [vpList.size()];
		for (int x=0;x<vpList.size();x++) {
			p[x] = vpList.get(x).probability;
		}
		probabilitySets.add(p);
		szl.add(probabilitySets.size()-1); 
		return probabilitySets.size()-1;
	}
	static int getOrAddValueSet(HashMap<Integer, List<Integer>> szVal, List<int []> valueSets, List<long []> valueSetsL, List<ValProb> vpList) {	
		List<Integer> szl = szVal.get(vpList.size());
		if (szl == null) {
			szl = new ArrayList<>();
			szVal.put(vpList.size(), szl);
		} else {
			for (int i=0;i<szl.size();i++) {
				int pos = szl.get(i);
				boolean match = true;
				if (valueSets != null) {
					int [] v = valueSets.get(pos);
					if (v.length != vpList.size()) continue;
					for (int x=0;x<vpList.size();x++) {
						if (vpList.get(x).value != v[x]) {
							match = false;
							break;
						}
					}
				} else {
					long [] vl = valueSetsL.get(pos);
					if (vl.length != vpList.size()) continue;
					for (int x=0;x<vpList.size();x++) {
						if (vpList.get(x).value != vl[x]) {
							match = false;
							break;
						}
					}
				}				
				// check match
				if (match) return pos;
			}
		}
		// add new
		if (valueSets != null) {
			int [] p = new int [vpList.size()];
			for (int x=0;x<vpList.size();x++) {
				p[x] = (int)vpList.get(x).value;
			}
			valueSets.add(p);
			szl.add(valueSets.size()-1);
			return valueSets.size()-1;			
		} else {
			long [] p = new long [vpList.size()];
			for (int x=0;x<vpList.size();x++) {
				p[x] = vpList.get(x).value;
			}
			valueSetsL.add(p);
			szl.add(valueSetsL.size()-1);
			return valueSetsL.size()-1;
		}
	}
	static int getOrAddGroupList(HashMap<Integer, List<Integer>> szGid, int sz, List<int []> groupList, int probId, int valId) {
		List<Integer> szl = szGid.get(sz);
		if (szl == null) {
			szl = new ArrayList<>();
			szGid.put(sz, szl);
		} else {	
			for (int i=0;i<szl.size();i++) {
				int pos = szl.get(i);
				// check match
				int [] v = groupList.get(pos);
				//int [] v = szl.get(i);
				if (v[0] != probId || v[1] != valId) continue;
				//return i;
				return pos;
			}
		}
		// add new
		int idS[] = new int[2];	
		idS[0] = probId;
		idS[1] = valId;
		groupList.add(idS);
		szl.add(groupList.size()-1);
		return groupList.size()-1;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// map vector 'peer's together -> those with the same child
	HashMap<Long, Set<Long>> mapVectorPeers() {
		HashMap<Long, Set<Long>> nsPeerVids = new HashMap<>();
		HashMap<Long, Set<Long>> chPeerVids = new HashMap<>();
		
		int ma = 0;		
		Iterator<Accum> it = this.iterator();
		while (it.hasNext()) {
			Accum ac = it.next();
			Set<Long> peerVids = chPeerVids.get(ac.getVectChildVid());
			if (peerVids == null) {
				peerVids = new HashSet<>();
				chPeerVids.put(ac.getVectChildVid(), peerVids);	
			} else {
				ma++;
			}
			// add to child set (peer direct)
			peerVids.add(ac.getVectorCode());
			// map to self
			nsPeerVids.put(ac.getVectorCode(), peerVids);
		}
		chPeerVids.clear();
		//System.out.print(" mvp["+size()+"]match["+ma+"] ");
		return nsPeerVids;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// JSON serializer
	static JsonSerializer<MLNumberSetHash> getJSONSerializer() {
		return new JsonSerializer<MLNumberSetHash>() {  
		    @Override
		    public JsonElement serialize(MLNumberSetHash src, Type typeOfSrc, JsonSerializationContext context) {
		        JsonObject jsonV = new JsonObject();
		        if (src.solid) {
			        // just the map of vid/groupid
		        	JsonArray jsonNVList = new JsonArray();
	            	src.idSetMap.forEachEntry((k, v) -> {
			        	JsonArray jsonNV = new JsonArray();
			        	//k=1;v=0; // 20 -> 5.4 MB
	            		jsonNV.add(k);
	            		jsonNV.add(v);
	            		jsonNVList.add(jsonNV);
	    	            return true;
	    	        });
		            
	            	jsonV.addProperty("ns", src.setNumber);
			        JsonElement jsonNS = context.serialize(src.numberSet, List.class);
				    jsonV.add("numSet", jsonNS);
			        JsonElement jsonNST = context.serialize(src.numberSetTier, List.class);
				    jsonV.add("numSetTier", jsonNST);			    
				    jsonV.add("vids", jsonNVList);
				    
				    jsonV.addProperty("prob", src.prob);
				    jsonV.addProperty("probs", src.probSingle);
				    jsonV.addProperty("nid", src.nid);
		        }
		        return jsonV;
		    }
		};
	}
	
	

	// 
	// print the data
	//	
	private static final boolean printFlat = false;
	void printData() {
		Iterator<Accum> it = this.iterator();
		List<ValProb> vpList = new ArrayList<>();
		while (it.hasNext()) {
			Accum at = it.next();
			System.out.println("   AC["+String.format("%12d", at.vectorCode)+"] vc["+String.format("%3d", at.getValueCount())+"] tot["+String.format("%6d", at.getTotal())+"] x["+at.vsid+"] ");
			vpList.clear();
			vpList = at.getValPsSorted(vpList);
			if (printFlat) {
				String s = "";
				for (int i=0;i<vpList.size();i++) {
					ValProb vp = vpList.get(i);
					s += " v[" + vp.value+"]c["+vp.count+"]p["+vp.probability+"]";
				}
				System.out.println("     => " +s);
			} else {
				for (int i=0;i<vpList.size();i++) {
					ValProb vp = vpList.get(i);
					System.out.println("     " +" v[" + String.format("%12d", vp.value)+"] c["+String.format("%5d", vp.count)+"] p["+vp.probability+"]");
				}
			}
		}		
	}
}
