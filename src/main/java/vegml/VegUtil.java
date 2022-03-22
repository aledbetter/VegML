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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import vegml.VegML.PredictionType;
import vegml.ValProb;

public class VegUtil {
	
	/**
	 * make String list
	 * @param sl
	 * @return
	 */
	public static String toStringList(List<String> sl) {
		String s = "";
		for (String st:sl) s += st + " ";
		return s.trim();
	}
	
	/**
	 * make String list
	 * @param sl
	 * @return
	 */
	public static String toStringList(String [] sl) {
		String s = "";
		for (String st:sl) s += st + " ";
		return s.trim();
	}
	
	/**
	 * make String list '{' '}'
	 * @param sl
	 * @return
	 */
	public static String toStringListSeg(List<String> sl) {
		String s = "";
		for (String st:sl) s += "{"+st + "} ";
		return s.trim();
	}
	
	/**
	 * make String list '{' '}'
	 * @param sl
	 * @return
	 */
	public static String toStringListSeg(String [] sl) {
		String s = "";
		for (String st:sl) s += "{"+st + "} ";
		return s.trim();
	}
	
	/**
	 * make String list '{' '}' and map values
	 * @param map
	 * @param sl
	 * @return
	 */
	public static String toStringListSeg(HashMap<Long, String> map, List<Long> sl) {
		String s = "";
		for (Long st:sl) {
			String xs = map.get(st);
			if (xs != null) s += "{"+xs + "} ";
			else s += "{<"+st + ">} ";
		}
		return s.trim();
	}
	
	/**
	 * make String list '{' '}' and map values
	 * @param map
	 * @param sl
	 * @return
	 */
	public static String toStringListSeg(HashMap<Long, String> map, Long [] sl) {
		String s = "";
		for (Long st:sl) {
			String xs = map.get(st);
			if (xs != null) s += "{"+xs + "} ";
			else s += "{<"+st + ">} ";
		}
		return s.trim();
	}
		
	/**
	 * make String list '{' '}' and map values
	 * @param dp dataplane to resolve the values to strings
	 * @param sl
	 * @return
	 */
	public static String toStringListSeg(VDataPlane dp, List<Long> sl) {
		String s = "";
		for (Long st:sl) {		
			String xs = dp.getString(st);
			if (xs != null) s += "{"+xs + "} ";
			else s += "{<"+st + ">} ";
		}
		return s.trim();
	}
	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	// ValProp Lists

	/**
	 * check if this is of the best values
	 * @param vpList
	 * @param valueId valueId to check
	 * @return
	 */
	public static boolean isBestValProb(List<ValProb> vpList, Long [] valueId) {
		if (vpList == null || vpList.size() < 1) return false;
		int idx = Arrays.binarySearch(valueId, vpList.get(0).value);
		if (idx >= 0) return true;
		return false;
	}

	/**
	 * check the second best position
	 * @param vpList
	 * @param valueId valueId to check
	 * @return
	 */
	public static boolean is2ndBestValProb(List<ValProb> vpList, long valueId) {
		if (vpList == null || vpList.size() < 2) return false;
		if (vpList.get(1).value == valueId) return true;
		return false;
	}	
	
	/**
	 * check if best value where there is a tie
	 * @param vpList
	 * @param valueId
	 * @return
	 */
	public static boolean isBestValProbX(List<ValProb> vpList, long valueId) {
		if (vpList == null || vpList.size() < 1) return false;
		double p = vpList.get(0).probability;
		for (int i=0;i<vpList.size();i++) {
			ValProb vp = vpList.get(i);
			if (vp.probability != p) return false;
			if (vp.value == valueId) return true;
		}
		return false;
	}
	
	/**
	 * Shallow copy fo vpList
	 * @param vpList
	 * @return
	 */
	public static List<ValProb> copy(List<ValProb> vpList) { 
		return new ArrayList<ValProb>(vpList);
	}

	/**
	 * Full copy of vpList
	 * @param vpList
	 * @return
	 */
	public static List<ValProb> copyFull(List<ValProb> vpList) { 
		if (vpList == null) return null;
		 List<ValProb> vl = new ArrayList<>(vpList.size());
		 for (ValProb vp:vpList) vl.add(vp.copy());
		return vl;
	}
	
	/**
	 * Full copy getting ValProb objects from freeList
	 * @param vpList list to copy
	 * @param freeList list of free ValProbs
	 * @return
	 */
	public static List<ValProb> copyFull(List<ValProb> vpList, List<ValProb> freeList) { 
		if (vpList == null) return null;
		return copyFull(new ArrayList<>(vpList.size()));
	}

	/**
	 * new list with copies of ValProbs from freeList when possible
	 * @param copyToList list to copy to
	 * @param vpList list copying
	 * @param freeList list of free ValProbs
	 * @return
	 */
	public static List<ValProb> copyFull(List<ValProb> copyToList, List<ValProb> vpList, List<ValProb> freeList) { 
		 if (vpList == null) return copyToList;
		 List<ValProb> vl = copyToList;
		 for (ValProb vp:vpList) {
			 ValProb nvp = null;
			 if (freeList.size() > 0) nvp = freeList.remove(freeList.size()-1);
			 else nvp = new ValProb();
			 vl.add(vp.copy(nvp));
		 }
		return vl;
	}
	
	/**
	 * copy pruned vpList shallow (new list same Vps)
	 * @param vpList list to copy
	 * @param maxSize max count to copy
	 * @return
	 */
	public static List<ValProb> copy(List<ValProb> vpList, int maxSize) { 
		if (maxSize < 1) return copy(vpList);
		List<ValProb> vpl = new ArrayList<ValProb>(maxSize);
		for (int i = 0;i<vpList.size() && i < maxSize;i++) vpl.add(vpList.get(i));
		return vpl;
	}
	
	/**
	 * Finalize results by sorting and optionally breaking ties
	 * @param dac default accumulator for the dataplane
	 * @param vpList result list
	 * @param makeWiner if true break ties
	 * @return
	 */
	public static List<ValProb> finailzeVPList(Accum dac, List<ValProb> vpList, boolean makeWiner) { 
		if (vpList.size() < 2) return vpList;

		Collections.sort(vpList, VpSort);

		PredictionType pType = vpList.get(0).type;
		double p = vpList.get(0).probability;
		if (p != vpList.get(1).probability) {
			vpList.get(0).type = pType;
			return vpList;
		}
		
		double lp = 0; // get last not one of these
		double cnt = 0;
		for (int i=0;i<vpList.size();i++) {
			ValProb vpx = vpList.get(i);
			if (vpx.probability != p) {
				lp = vpx.probability;
				break;	
			}
			vpx.position = dac.getCount(vpx.value); // used to order these
			cnt++; // count of tied probabilities
		}
		Collections.sort(vpList, VpSort); // sort proper

		if (makeWiner && false) {
			if (lp == 0) lp = p*(double)0.95;  // drop 5% if only tied values
			double split = ((p - lp) / cnt) * (double)0.50; // 90 percent of so we never reach next number
			
			for (int i=1;i<vpList.size() && i<cnt;i++) {
				ValProb vpx = vpList.get(i);
				vpx.probability = vpx.probability - (split*i);
		//		System.out.println(" CH["+i+"]["+vpx.probability+"] + ["+vpx.position+"] ["+(split * cnt)+"] o["+off+"] => " + (np));
			}
		}
		
		vpList.get(0).type = pType;
		return vpList;
	}

	/**
	 * prune vp list to maxSize
	 * @param vpList value probability list
	 * @param maxSize max size to allow
	 * @return
	 */
	public static List<ValProb> prune(List<ValProb> vpList, int maxSize) { 
		// Average the values (based on the number here)
		while (vpList.size() > maxSize) {
			vpList.remove(vpList.size()-1);
		}	
		return vpList;
	}

	/**
	 * remove entry for value
	 * @param vpList value probability list
	 * @param value value to remove
	 * @return
	 */
	public static List<ValProb> remove(List<ValProb> vpList, long value) { 
		int idx = ValProb.indexOf(vpList, value);
		if (idx >= 0) vpList.remove(idx);
		return vpList;
	}
	
	/**
	 * remove all entries not in the keeplist
	 * @param vpList value probability list
	 * @param keepList list of values to keep
	 * @return
	 */
	public static List<ValProb> remove(List<ValProb> vpList, List<ValProb> keepList) { 
		if (vpList == null) return vpList;
		if (keepList.size() == 0) {
			vpList.clear();
		} else {
			for (int x=0;x<vpList.size();) {
				int idx = ValProb.indexOf(keepList, vpList.get(x).value);
				if (idx < 0) vpList.remove(x);
				else x++;			
			}
		}
		return vpList;
	}

	/**
	 * Merge vpList2 into vplist1 with the mix factor
	 * mix = 0 only vpList1
	 * mix = 1 mix equal (no mod)
	 * mix = 100 only vpList2
	 * mix between mixes
	 * 
	 * @param vpList1 base list
	 * @param vpList2 list to merge on tope
	 * @param mix value to modifiy
	 * @return
	 */
	public static List<ValProb> mergeVPSet(List<ValProb> vpList1, List<ValProb> vpList2, double mix) { 

		if (mix >= 100) {
			vpList1.clear();
			if (vpList2 != null) {
				for (int i=0;i<vpList2.size();i++) {
					ValProb vp = vpList2.get(i); 
					vp.probability = vp.probability*mix;
					vpList1.add(vp);
				}
			}
			return vpList1;
		}
		if (mix <= 0 || vpList2 == null) return vpList1;
	
		for (int i=0;i<vpList2.size();i++) {
			ValProb vp = vpList2.get(i);
			int idx = ValProb.indexOf(vpList1, vp.value);
			if (idx >= 0) {
				// add to existing
				ValProb vx = vpList1.get(idx);
				vx.probability += vp.probability*mix;
				vx.count += vp.count;
				vx.counter++;
			} else {
				// add new
				vpList1.add(vp);
				vp.probability = vp.probability*mix;
				vp.counter = 1;
			}
		}
		// Average the values (based on the number here)
		for (int i=0;i<vpList1.size();i++) {
			ValProb vpx = vpList1.get(i);
			vpx.probability = vpx.probability / vpx.counter;
			vpx.counter = 1;
		}	
		return vpList1;
	}
	
	/**
	 * merge with mergeValues per value
	 * @param vpList1
	 * @param vpList2
	 * @param mix
	 * @param valueWeight
	 * @return
	 */
	public static List<ValProb> mergeVPSet(List<ValProb> vpList1, List<ValProb> vpList2, double mix, HashMap<Long, Double> valueWeight) { 
		if (valueWeight == null) {
			return mergeVPSet(vpList1, vpList2, mix);
		}
		// add in the base
		for (int i=0;i<vpList1.size();i++) {
			ValProb vp = vpList1.get(i); 
			Double vw = valueWeight.get(vp.value);
			boolean remove = false;
			if (vw != null && vw >= 100) remove = true;
			else if (vw == null && mix >= 100) remove = true;
			if (remove) {
				vpList1.remove(i);
				i--;
			}
		}
		if (vpList2 == null) return vpList1;
	
		// merge in the list
		for (int i=0;i<vpList2.size();i++) {
			ValProb vp = vpList2.get(i);
			Double vw = valueWeight.get(vp.value);
			if (vw != null && vw == 0) continue;  // nope
			else if (vw == null && mix <= 0) continue; // don't add
			
			int idx = ValProb.indexOf(vpList1, vp.value);
			if (idx >= 0) {
				// add to existing
				ValProb vx = vpList1.get(idx);
				vx.probability += vp.probability*mix;
				if (vw != null && vw < 100) vx.probability = vx.probability*vw;
				vx.count += vp.count;
				vx.counter++;
			} else {
				// add new
				vpList1.add(vp);
				vp.probability = vp.probability*mix;
				if (vw != null && vw < 100) vp.probability = vp.probability*vw;
				vp.counter = 1;
			}
		}

		// Average the values (based on the number here)
		for (int i=0;i<vpList1.size();i++) {
			ValProb vpx = vpList1.get(i);
			vpx.probability = vpx.probability / vpx.counter;
			vpx.counter = 1;
		}	
		return vpList1;
	}
	
	/**
	 * merge and Average vpList into vpBaseList
	 * @param vpListBase
	 * @param vpList
	 * @return
	 */
	public static List<ValProb> mergeVPList(List<ValProb> vpListBase, List<ValProb> vpList) { 
		return mergeVPList(vpListBase, vpList, 1);
	}	
	
	/**
	 * Merge vpList into vpListBase with mergeValue for probabilities
	 * Then average the results
	 * @param vpListBase
	 * @param vpList
	 * @param mergeValue
	 * @return
	 */
	public static List<ValProb> mergeVPList(List<ValProb> vpListBase, List<ValProb> vpList, double mergeValue) { 
		vpListBase = mergeVPListOnly(vpListBase, vpList, mergeValue);
		// Average the values (based on the number here)
		for (int i=0;i<vpListBase.size();i++) {
			ValProb vpx = vpListBase.get(i);
			vpx.probability = vpx.probability / vpx.counter;
			vpx.counter = 1;
		}	
		return vpListBase;
	}
	
	/**
	 * Merge vpList into vpListBase
	 * @param vpListBase
	 * @param vpList
	 * @return
	 */
	public static List<ValProb> mergeVPListOnly(List<ValProb> vpListBase, List<ValProb> vpList) { 
		return mergeVPListOnly(vpListBase, vpList, 1);
	}
	
	/**
	 * Merge vpList into vpListBase with mergeValue for probabilities
	 * @param vpListBase
	 * @param vpList
	 * @param mergeValue
	 * @return
	 */
	public static List<ValProb> mergeVPListOnly(List<ValProb> vpListBase, List<ValProb> vpList, double mergeValue) { 
		if (mergeValue <= 0) return vpListBase;
		for (int i=0;i<vpList.size();i++) {
			ValProb vp = vpList.get(i);
			int idx = ValProb.indexOf(vpListBase, vp.value);
			if (idx >= 0) {
				// add to existing
				ValProb vx = vpListBase.get(idx);
				vx.probability += vp.probability*mergeValue;
				vx.count += vp.count;
				vx.counter++;
			} else {
				// add new
				vpListBase.add(vp);
				vp.probability = vp.probability*mergeValue;
				vp.counter = 1;
			}
		}
		return vpListBase;
	}

	/**
	 * Merge vpList into vpListBase with mergeValue for probabilities
	 * return new list for merged values
	 * 
	 * @param vpListBase
	 * @param vpList
	 * @param mergeValue merge multiplyer, =>100 just vpList value <=0 just baseList value
	 * @return
	 */
	public static List<ValProb> mergeVPListOnlyCopy(List<ValProb> vpListBase, List<ValProb> vpList, double mergeValue) { 
		List<ValProb> nl = new ArrayList<>();
		if (mergeValue >= 100) {
			for (int i=0;i<vpList.size();i++) {
				ValProb vx = vpList.get(i).copy();
				vx.probability = vx.probability*mergeValue;
				nl.add(vx);
			}
			return nl;
		}
		for (int i=0;i<vpListBase.size();i++) nl.add(vpListBase.get(i).copy());
		if (mergeValue <= 0) return nl;

		for (int i=0;i<vpList.size();i++) {
			ValProb vp = vpList.get(i);
			int idx = ValProb.indexOf(nl, vp.value);
			if (idx >= 0) {
				// add to existing
				ValProb vx = nl.get(idx);
				vx.probability += vp.probability*mergeValue;
				vx.count += vp.count;
				vx.counter++;
			} else {
				// add new
				ValProb vx = vp.copy();
				vx.probability = vx.probability*mergeValue;
				vx.counter = 1;
				nl.add(vx);
			}
		}
		return nl;
	}
	
	/**
	 * merge with matrix per prediction type
	 * @param vpListBase
	 * @param vpList
	 * @param probMatrix
	 * @return
	 */
	public static List<ValProb> mergeVPListOnlyCopy(List<ValProb> vpListBase, List<ValProb> vpList, double probMatrix[][]) { 
		List<ValProb> nl = new ArrayList<>();
		// get merge value
		double mergeValue = 0;
		if (probMatrix != null) {
			PredictionType pt1 = PredictionType.Default;
			if (vpListBase.size() > 0) pt1 = vpListBase.get(0).type;
			PredictionType pt2 = PredictionType.Default;
			if (vpList.size() > 0) pt2 = vpList.get(0).type;
			mergeValue = probMatrix[pt1.ordinal()][pt2.ordinal()];
		}
		if (mergeValue >= 100) {
			for (int i=0;i<vpList.size();i++) {
				ValProb vx = vpList.get(i).copy();
				vx.probability = vx.probability*mergeValue;
				nl.add(vx);
			}
			return nl;
		}
		for (int i=0;i<vpListBase.size();i++) nl.add(vpListBase.get(i).copy());
		if (mergeValue <= 0) return nl;		 
		
		for (int i=0;i<vpList.size();i++) { 
			ValProb vp = vpList.get(i);
			int idx = ValProb.indexOf(nl, vp.value);
			if (idx >= 0) {
				// add to existing
				ValProb vx = nl.get(idx);
				vx.probability += vp.probability*mergeValue;
				vx.count += vp.count;
				vx.counter++;
			} else {
				// add new
				ValProb vx = vp.copy();
				vx.probability = vx.probability*mergeValue;
				vx.counter = 1;
				nl.add(vx);
			}
		}
		return nl;
	}	
	
	/**
	 * merge lists per value weights
	 * 100 - vpList only 
	 * 0 - vpListBase only
	 * n - weight
	 * @param vpListBase
	 * @param vpList
	 * @param baseWeigth
	 * @param valueWeight
	 * @return
	 */
	public static List<ValProb> mergeVPListOnlyCopy(List<ValProb> vpListBase, List<ValProb> vpList, double baseWeigth, HashMap<Long, Double> valueWeight) { 
		List<ValProb> nl = new ArrayList<>();
		// add in the base
		for (int i=0;i<vpListBase.size();i++) {
			ValProb vp = vpListBase.get(i); 
			Double vw = valueWeight.get(vp.value);
			if (vw != null && vw >= 100) continue; // don't add
			else if (vw == null && baseWeigth >= 100) continue; // don't add
			vp = vp.copy();
			nl.add(vp);
		}
		// merge in the list
		for (int i=0;i<vpList.size();i++) { 
			ValProb vp = vpList.get(i);
			Double vw = valueWeight.get(vp.value);
			if (vw != null && vw == 0) continue;  // don't add
			else if (vw == null && baseWeigth <= 0) continue; // don't add
			
			int idx = ValProb.indexOf(nl, vp.value);
			if (idx >= 0) {
				// add to existing
				ValProb vx = nl.get(idx);
				vx.probability += vp.probability*baseWeigth;
				if (vw != null && vw < 100) vx.probability = vx.probability*vw;
				vx.count += vp.count;
				vx.counter++;
			} else {
				// add new
				ValProb vx = vp.copy();
				vx.probability = vx.probability*baseWeigth;
				if (vw != null && vw < 100) vx.probability = vx.probability*vw;
				vx.counter = 1;
				nl.add(vx);
			}
		}
		return nl;
	}
	// with merge matrix
	public static List<ValProb> mergeVPListOnlyCopy(List<ValProb> vpListBase, List<ValProb> vpList, double probMatrix[][], HashMap<Long, Double> valueWeightMatrix[][]) { 
		List<ValProb> nl = new ArrayList<>();
		// get merge value
		double baseWeigth = 0;
		HashMap<Long, Double> valueWeight = null;
		if (probMatrix != null) {
			PredictionType pt1 = PredictionType.Default;
			if (vpListBase.size() > 0) pt1 = vpListBase.get(0).type;
			PredictionType pt2 = PredictionType.Default;
			if (vpList.size() > 0) pt2 = vpList.get(0).type;
			baseWeigth = probMatrix[pt1.ordinal()][pt2.ordinal()];
			valueWeight = valueWeightMatrix[pt1.ordinal()][pt2.ordinal()];
		}
		// add in the base
		for (int i=0;i<vpListBase.size();i++) {
			ValProb vp = vpListBase.get(i); 
			Double vw = valueWeight.get(vp.value);
			if (vw != null && vw >= 100) continue; // don't add
			else if (vw == null && baseWeigth >= 100) continue; // don't add
			vp = vp.copy();
			nl.add(vp);
		}
		// merge in the list
		for (int i=0;i<vpList.size();i++) { 
			ValProb vp = vpList.get(i);
			Double vw = valueWeight.get(vp.value);
			if (vw != null && vw == 0) continue;  // don't add
			else if (vw == null && baseWeigth <= 0) continue; // don't add
			
			int idx = ValProb.indexOf(nl, vp.value);
			if (idx >= 0) {
				// add to existing
				ValProb vx = nl.get(idx);
				vx.probability += vp.probability*baseWeigth;
				if (vw != null && vw < 100) vx.probability = vx.probability*vw;
				vx.count += vp.count;
				vx.counter++;
			} else {
				// add new
				ValProb vx = vp.copy();
				vx.probability = vx.probability*baseWeigth;
				if (vw != null && vw < 100) vx.probability = vx.probability*vw;
				vx.counter = 1;
				nl.add(vx);
			}
		}
		return nl;
	}	

	

	
	// list of lists
	public static List<List<ValProb>> mergeVPListsCopy(List<List<ValProb>> vpListsBase, List<List<ValProb>> vpLists) { 
		return mergeVPListsCopy(vpListsBase, vpLists, 1);
	}
	public static List<List<ValProb>> mergeVPListsCopy(List<List<ValProb>> vpListsBase, List<List<ValProb>> vpLists, double proboffset) { 
		List<List<ValProb>> outList = new ArrayList<>();
		for (int i=0;i<vpLists.size();i++) {
			List<ValProb> vpListBase = vpListsBase.get(i);
			PredictionType pType = vpListBase.get(0).type;
			List<ValProb> vpList = vpLists.get(i);
			
			List<ValProb> out = mergeVPListOnlyCopy(vpListBase, vpList, proboffset);
			// Average the values (based on the number here)
			for (int xi=0;xi<out.size();xi++) {
				ValProb vpx = out.get(xi);
				vpx.probability = vpx.probability / vpx.counter;
				vpx.counter = 1;
				vpx.type = pType;
			}	
			Collections.sort(out, VegUtil.VpSort);
			outList.add(out);
		}
		return outList;
	}
	// value map
	public static List<List<ValProb>> mergeVPListsCopy(List<List<ValProb>> vpListsBase, List<List<ValProb>> vpLists, double proboffset, HashMap<Long, Double> valueWeight) { 
		List<List<ValProb>> outList = new ArrayList<>();
		for (int i=0;i<vpLists.size();i++) {
			List<ValProb> vpListBase = vpListsBase.get(i);
			PredictionType pType = vpListBase.get(0).type;
			List<ValProb> vpList = vpLists.get(i);
			
			List<ValProb> out = mergeVPListOnlyCopy(vpListBase, vpList, proboffset, valueWeight);
			// Average the values (based on the number here)
			for (int xi=0;xi<out.size();xi++) {
				ValProb vpx = out.get(xi);
				vpx.probability = vpx.probability / vpx.counter;
				vpx.counter = 1;
				vpx.type = pType;
			}	
			Collections.sort(out, VegUtil.VpSort);
			outList.add(out);
		}
		return outList;
	}	
	
	
	public static List<List<ValProb>> mergeVPListsCopy(List<List<ValProb>> vpListsBase, List<List<ValProb>> vpLists, double probMatrix[][]) { 
		List<List<ValProb>> outList = new ArrayList<>();
		for (int i=0;i<vpLists.size();i++) {
			List<ValProb> vpListBase = vpListsBase.get(i);
			PredictionType pType = vpListBase.get(0).type;
			List<ValProb> vpList = vpLists.get(i);			
			
			List<ValProb> out = mergeVPListOnlyCopy(vpListBase, vpList, probMatrix);
			// Average the values (based on the number here)
			for (int xi=0;xi<out.size();xi++) {
				ValProb vpx = out.get(xi);
				vpx.probability = vpx.probability / vpx.counter;
				vpx.counter = 1;
				vpx.type = pType;
			}	
			Collections.sort(out, VegUtil.VpSort);
			outList.add(out);
		}
		return outList;
	}
	public static List<List<ValProb>> mergeVPListsCopy(List<List<ValProb>> vpListsBase, List<List<ValProb>> vpLists, double probMatrix[][], HashMap<Long, Double> valueWeightMatrix[][]) { 
		List<List<ValProb>> outList = new ArrayList<>();
		for (int i=0;i<vpLists.size();i++) {
			List<ValProb> vpListBase = vpListsBase.get(i);
			PredictionType pType = vpListBase.get(0).type;
			List<ValProb> vpList = vpLists.get(i);			
			
			List<ValProb> out = mergeVPListOnlyCopy(vpListBase, vpList, probMatrix, valueWeightMatrix);
			// Average the values (based on the number here)
			for (int xi=0;xi<out.size();xi++) {
				ValProb vpx = out.get(xi);
				vpx.probability = vpx.probability / vpx.counter;
				vpx.counter = 1;
				vpx.type = pType;
			}	
			Collections.sort(out, VegUtil.VpSort);
			outList.add(out);
		}
		return outList;
	}
	
	
	// list of lists
	public static List<List<List<ValProb>>> mergeVPListSetsCopy(List<List<List<ValProb>>> vpListsBase, List<List<List<ValProb>>> vpLists) { 
		return mergeVPListSetsCopy(vpListsBase, vpLists, 1);
	}
	public static List<List<List<ValProb>>> mergeVPListSetsCopy(List<List<List<ValProb>>> vpListsBase, List<List<List<ValProb>>> vpLists, double proboffset) { 
		List<List<List<ValProb>>> outList = new ArrayList<>();
		for (int i=0;i<vpLists.size();i++) {
			List<List<ValProb>> vpListBase = vpListsBase.get(i);
			List<List<ValProb>> vpList = vpLists.get(i);		
			List<List<ValProb>> out = mergeVPListsCopy(vpListBase, vpList, proboffset);
			outList.add(out);
		}
		return outList;
	}
	public static List<List<List<ValProb>>> mergeVPListSetsCopy(List<List<List<ValProb>>> vpListsBase, List<List<List<ValProb>>> vpLists, double probMatrix[][]) { 
		List<List<List<ValProb>>> outList = new ArrayList<>();
		for (int i=0;i<vpLists.size();i++) {
			List<List<ValProb>> vpListBase = vpListsBase.get(i);
			List<List<ValProb>> vpList = vpLists.get(i);
			List<List<ValProb>> out = mergeVPListsCopy(vpListBase, vpList, probMatrix);
			outList.add(out);
		}
		return outList;
	}
	public static List<List<List<ValProb>>> mergeVPListSetsCopy(List<List<List<ValProb>>> vpListsBase, List<List<List<ValProb>>> vpLists, double probMatrix[][], HashMap<Long, Double> valueWeightMatrix[][]) { 
		List<List<List<ValProb>>> outList = new ArrayList<>();
		for (int i=0;i<vpLists.size();i++) {
			List<List<ValProb>> vpListBase = vpListsBase.get(i);
			List<List<ValProb>> vpList = vpLists.get(i);
			List<List<ValProb>> out = mergeVPListsCopy(vpListBase, vpList, probMatrix, valueWeightMatrix);
			outList.add(out);
		}
		return outList;
	}
	
	// merge in only if in base
	public static List<ValProb> mergeVPListInBase(List<ValProb> vpListBase, List<ValProb> vpList) { 
		return mergeVPListInBase(vpListBase, vpList, 1);
	}
	public static List<ValProb> mergeVPListInBase(List<ValProb> vpListBase, List<ValProb> vpList, double proboffset) { 
		vpListBase = mergeVPListOnlyInBase(vpListBase, vpList, proboffset);
		// Average the values (based on the number here)
		for (int i=0;i<vpListBase.size();i++) {
			ValProb vpx = vpListBase.get(i);
			vpx.probability = vpx.probability / vpx.counter;
			vpx.counter = 1;
		}	
		return vpListBase;
	}
	public static List<ValProb> mergeVPListOnlyInBase(List<ValProb> vpListBase, List<ValProb> vpList) { 
		return mergeVPListOnlyInBase(vpListBase, vpList, 1);
	}
	public static List<ValProb> mergeVPListOnlyInBase(List<ValProb> vpListBase, List<ValProb> vpList, double proboffset) { 
		if (proboffset <= 0) return vpListBase;
		for (int i=0;i<vpList.size();i++) {
			ValProb vp = vpList.get(i);
			int idx = ValProb.indexOf(vpListBase, vp.value);
			if (idx >= 0) {
				// add to existing
				ValProb vx = vpListBase.get(idx);
				vx.probability += vp.probability*proboffset;
				vx.count += vp.count;
				vx.counter++;
			}
		}
		return vpListBase;
	}
	public static List<ValProb> mergeVPListOnlyInBaseRev(List<ValProb> vpListBase, List<ValProb> vpList, double proboffset) { 
		if (proboffset <= 0) return vpListBase;
		for (int i=0;i<vpList.size();i++) {
			ValProb vp = vpList.get(i);
			int idx = ValProb.indexOf(vpListBase, vp.value);
			if (idx >= 0) {
				// add to existing
				ValProb vx = vpListBase.get(idx);
				vx.probability += vp.probability*proboffset;
				vx.count += vp.count;
				vx.counter++;
			}
		}
		return vpListBase;
	}
	
	
	// merge single vp into list
	public static List<ValProb> mergeIntoVPList(List<ValProb> vpList, ValProb vp) { 
		int idx = ValProb.indexOf(vpList, vp.value);
		if (idx >= 0) {
			// add to existing
			ValProb vx = vpList.get(idx);
			vx.probability += vp.probability;
			vx.count += vp.count;
			vx.counter++;
		} else {
			vpList.add(vp);
			vp.counter = 1;
		}
		
		return vpList;
	}
	public static List<ValProb> mergeIntoVPList(List<ValProb> vpList, long value, double probability, int count) { 
		int idx = ValProb.indexOf(vpList, value);
		if (idx >= 0) {
			// add to existing
			ValProb vx = vpList.get(idx);
			vx.probability += probability;
			vx.count += count;
			vx.counter++;
		} else {
			ValProb vp = new ValProb();
			vp.value = value;
			vp.probability = probability;
			vp.count = count;
			vp.counter = 1;
			vpList.add(vp);
		}		
		return vpList;
	}
	public static List<ValProb> mergeIntoVPList(List<ValProb> vpList, long value, double probability, int count, List<ValProb> freeVpList) { 
		int idx = ValProb.indexOf(vpList, value);
		if (idx >= 0) {
			// add to existing
			ValProb vx = vpList.get(idx);
			vx.probability += probability;
			vx.count += count;
			vx.counter++;
		} else {
			ValProb vp = null;
			if (freeVpList.size() > 0) vp = freeVpList.remove(freeVpList.size()-1);
			else vp = new ValProb();
			vp.value = value;
			vp.probability = probability;
			vp.count = count;
			vp.counter = 1;
			vpList.add(vp);
		}		
		return vpList;
	}
	public static List<ValProb> mergeIntoVPList(List<ValProb> vpList, ValProb vp, List<ValProb> freeVpList) { 
		int idx = ValProb.indexOf(vpList, vp.value);
		if (idx >= 0) {
			// add to existing
			ValProb vx = vpList.get(idx);
			vx.probability += vp.probability;
			vx.count += vp.count;
			vx.counter++;
		} else {
			ValProb vx = null;
			if (freeVpList.size() > 0) vx = freeVpList.remove(freeVpList.size()-1);
			else vx = new ValProb();
			vx = vp.copy(vx);
			vx.counter = 1;
			vpList.add(vx);
		}		
		return vpList;
	}
	public static List<ValProb> mergeIntoVPList(List<ValProb> vpList, ValProb vp, List<ValProb> freeVpList, double mergeValue) { 
		int idx = ValProb.indexOf(vpList, vp.value);
		if (idx >= 0) {
			// add to existing
			ValProb vx = vpList.get(idx);
			vx.probability += vp.probability * mergeValue;
			vx.count += vp.count;
			vx.counter++;
		} else {
			ValProb vx = null;
			if (freeVpList.size() > 0) vx = freeVpList.remove(freeVpList.size()-1);
			else vx = new ValProb();
			vx = vp.copy(vx);
			vx.probability += vx.probability * mergeValue;
			vx.counter = 1;
			vpList.add(vx);
		}		
		return vpList;
	}
	static List<ValProb> removeNotInBase(List<ValProb> vpListBase, List<ValProb> vpList) { 
		for (int i=0;i<vpList.size();i++) {
			ValProb vp = vpList.get(i);
			int idx = ValProb.indexOf(vpListBase, vp.value);
			if (idx >= 0) {
				// add to existing
				ValProb vx = vpListBase.get(idx);
				vx.probability = vp.probability;
				vx.count = vp.count;
				vx.counter = 1;
			}
		}
		return vpListBase;
	}
	
	/**
	 * check if vpList contains value
	 * @param vpList list to check
	 * @param value value to find
	 * @return true if found, else false
	 */
	public static boolean contains(List<ValProb> vpList, long value) { 
		for (int i=0;i<vpList.size();i++) {
			ValProb vp = vpList.get(i);
			if (vp.value == value) return true;
		}
		return false;
	}
	
	/**
	 * check if valSet contains v
	 * @param valSet
	 * @param v
	 * @return true if in it
	 */
	private static boolean containsv(long [] valSet, long v) {
		for (int i=0; i < valSet.length; i++) {
			if (valSet[i] == v) return true;
		}
		return false;
	}
	
	/**
	 * check if value is in list
	 * @param list list of values to check
	 * @param value value to find
	 * @return position in list or -1
	 */
	public static int contains(long [] list, long value) {
		if (list == null) return -1;
		for (int i=0; i < list.length; i++) {
			if (list[i] == value) return i;
		}
		return -1;
	}
	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	// ValProb List modification
	
	// Complete: gain OR lost function
	public static List<ValProb> updateListProb(List<ValProb> vpListBase, double proboffset) { 
		if (proboffset <= 0) return vpListBase;
		// Average the values (based on the number here)
		for (int i=0;i<vpListBase.size();i++) {
			ValProb vpx = vpListBase.get(i);
			vpx.probability *= proboffset;
		}	
		return vpListBase;
	}
	// Partial: gain OR lost function
	public static List<ValProb> updateListProb(List<ValProb> vpListBase, double proboffset, long [] updateSet) { 
		if (proboffset <= 0) return vpListBase;
		// Average the values (based on the number here)
		for (int i=0;i<vpListBase.size();i++) {
			ValProb vp = vpListBase.get(i);
			if (containsv(updateSet, vp.value)) vp.probability *= proboffset;
		}	
		return vpListBase;
	}
	
	/**
	 * Partial gain functions: natural
	 * limited amplify on set
	 * 
	 * @param vpList
	 * @param ampSet
	 * @return
	 */
	public static List<ValProb> amplifyListProbLimitSet(List<ValProb> vpList, long [] ampSet) {
		return amplifyListProbLimitSet(vpList, ampSet, -1);
	}

	/**
	 * limted amplify with noiseLimit
	 * @param vpList
	 * @param ampSet
	 * @param noiseLimit
	 * @return
	 */
	public static List<ValProb> amplifyListProbLimitSet(List<ValProb> vpList, long [] ampSet, int noiseLimit) {
		if (vpList == null) return null;
		int tot = 0;
		double sum = 0, fsum = 0;
		Collections.sort(vpList, VegUtil.VpSort);

		// clear the no-noes
		int bpos = 0;
		for (int i = 0; i < vpList.size(); i++) {
			ValProb vp = vpList.get(i);
			fsum += vp.probability;
			if (!containsv(ampSet, vp.value)) {
				if (noiseLimit <= 0 || bpos < noiseLimit) tot += vp.count;  // noise filter hack
				else sum += vp.probability;
				vpList.remove(i);
				i--;
			} else {
				sum += vp.probability;
			}
			bpos++;
		}
		if (vpList.size() < 1) return vpList;
		if (tot == 0) return vpList;
		
		double mod = fsum / sum;
		
		// upate probabilities
		for (int i = 0; i < vpList.size(); i++) {
			ValProb vp = vpList.get(i);
			vp.probability = vp.probability * mod;
		}
		Collections.sort(vpList, VegUtil.VpSort);
		return vpList;
	}
	
	/**
	 * for solid, get probability
	 * @param probList
	 * @param valList
	 * @param pos
	 * @param ampSet
	 * @param mod
	 * @return
	 */
	public static double amplifyListProbLimitSet(double [] probList, long [] valList, int pos, long [] ampSet, double mod) {
		if (!containsv(ampSet, valList[pos])) return 0; // not this value
		if (mod == 0) return probList[pos];
		return probList[pos] * mod;
	}
	
	/**
	 * 
	 * @param probList
	 * @param valList
	 * @param pos
	 * @param ampSet
	 * @param mod
	 * @return
	 */
	public static double amplifyListProbLimitSet(double [] probList, int [] valList, int pos, long [] ampSet, double mod) {
		if (!containsv(ampSet, valList[pos])) return 0; // not this value
		if (mod == 0) return probList[pos];
		return probList[pos] * mod;
	}
	
	/**
	 * 
	 * @param probList
	 * @param valList
	 * @param ampSet
	 * @param noiseLimit
	 * @return
	 */
	public static double amplifyListProbGetTotal(double [] probList, long [] valList, long [] ampSet, int noiseLimit) {
		if (probList == null) return 0;	
		int tot = 0;
		double sum = 0, fsum = 0;		
		int cnt = 0;
		// clear the no-noes
		for (int i = 0; i < probList.length; i++) {
			fsum += probList[i];
			if (!containsv(ampSet, valList[i])) {
				if (noiseLimit <= 0 || cnt < noiseLimit) tot++;
				else sum += probList[i];
			} else {
				sum += probList[i];
			}
			cnt++;
		}
		if (tot == 0) return 0;
		return (fsum/sum);
	}
	
	/**
	 * 
	 * @param probList
	 * @param valList
	 * @param ampSet
	 * @param noiseLimit
	 * @return
	 */
	public static double amplifyListProbGetTotal(double [] probList, int [] valList, long [] ampSet, int noiseLimit) {
		if (probList == null) return 0;	
		int tot = 0;
		double sum = 0, fsum = 0;		
		int cnt = 0;
		// clear the no-noes
		for (int i = 0; i < probList.length; i++) {
			fsum += probList[i];
			if (!containsv(ampSet, valList[i])) {
				if (noiseLimit <= 0 || cnt < noiseLimit) tot++;
				else sum += probList[i];
			} else {
				sum += probList[i];
			}
			cnt++;
		}
		if (tot == 0) return 0;
		return (fsum/sum);
	}

	/**
	 * for solid, get probability
	 * @param probList
	 * @param valList
	 * @param pos
	 * @param ampSet
	 * @param mod
	 * @return
	 */
	public static double amplifyListProbSet(double [] probList, long [] valList, int pos, long [] ampSet, double mod) {
		if (mod == 0) return probList[pos];
		if (!containsv(ampSet, valList[pos])) return probList[pos]; // not this value
		return (probList[pos] * mod);
	}
	
	/**
	 * 
	 * @param probList
	 * @param valList
	 * @param pos
	 * @param ampSet
	 * @param mod
	 * @return
	 */
	public static double amplifyListProbSet(double [] probList, int [] valList, int pos, long [] ampSet, double mod) {
		if (mod == 0) return probList[pos];
		if (!containsv(ampSet, valList[pos])) return probList[pos]; // not this value
		return (probList[pos] * mod);
	}

	/**
	 * amplify set 
	 * @param vpList
	 * @param ampSet
	 * @return
	 */
	public static List<ValProb> amplifyListProbSet(List<ValProb> vpList, long [] ampSet) {
		return amplifyListProbSet(vpList, ampSet, -1);
	}

	/**
	 * amplify and noise limit
	 * @param vpList
	 * @param ampSet
	 * @param noiseLimit
	 * @return
	 */
	public static List<ValProb> amplifyListProbSet(List<ValProb> vpList, long [] ampSet, int noiseLimit) {
		if (vpList == null) return null;
		int tot = 0;
		double sum = 0, fsum = 0;
		int cnt = 0;
		Collections.sort(vpList, VegUtil.VpSort);

		// clear the no-noes
		for (int i = 0; i < vpList.size(); i++) {
			ValProb vp = vpList.get(i);
			fsum += vp.probability;
			if (!containsv(ampSet, vp.value)) {
				if (noiseLimit <= 0 || cnt < noiseLimit) tot++;  // noise filter hack
				else sum += vp.probability;
			} else {
				sum += vp.probability;
			}
			cnt++;
		}
		if (vpList.size() < 1) return vpList;
		if (tot == 0) return vpList;
		double mod = fsum / sum;
		
		for (int i = 0; i < vpList.size(); i++) {
			ValProb vp = vpList.get(i);
			if (containsv(ampSet, vp.value)) {
				vp.probability = vp.probability * mod;
			}
		}
		Collections.sort(vpList, VegUtil.VpSort);
		return vpList;
	}
	
	/**
	 * Partial loss functions, reduce
	 * @param vpList
	 * @param attnSet
	 * @return
	 */
	public static List<ValProb> attnuateListProbLimitSet(List<ValProb> vpList, long [] attnSet) {
		if (vpList == null) return null;
		int tot = 0;
		double sum = 0, fsum = 0;
				
		// clear the no-noes
		for (int i = 0; i < vpList.size(); i++) {
			ValProb vp = vpList.get(i);
			fsum += vp.probability;
			if (!containsv(attnSet, vp.value)) {
				tot += vp.count; 
			} else {
				sum += vp.probability;
			}
		}
		if (vpList.size() < 1) return vpList;
		if (tot == 0) return vpList;
		double mod = fsum / sum;
		
		for (int i = 0; i < vpList.size(); i++) {
			ValProb vp = vpList.get(i);
			if (containsv(attnSet, vp.value)) {
				vp.probability = vp.probability * mod;
			}
		}
		Collections.sort(vpList, VegUtil.VpSort);
		return vpList;
	}

	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	// compare operations for sort or binaray search
	
	/**
	 * ValProb comparator for sorting and binary search
	 */
	public static final Comparator<ValProb> VpSort = new Comparator<ValProb>() {
        @Override
        public int compare(ValProb lvp, ValProb rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
        	if (lvp.probability < rvp.probability) return 1;
        	if (lvp.probability > rvp.probability) return -1;
          	if (lvp.position < rvp.position) return 1;
        	if (lvp.position > rvp.position) return -1;
          	if (lvp.count < rvp.count) return 1;
        	if (lvp.count > rvp.count) return -1;
        	if (lvp.value < rvp.value) return 1;
        	if (lvp.value > rvp.value) return -1;
        	return 0;   
        }
    };
    
	/**
	 * ValProb Reverse sort comparator for sorting and binary search
	 */
	public static final Comparator<ValProb> VpSortR = new Comparator<ValProb>() {
        @Override
        public int compare(ValProb lvp, ValProb rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
        	if (lvp.probability > rvp.probability) return 1;
        	if (lvp.probability < rvp.probability) return -1;
          	if (lvp.position > rvp.position) return 1;
        	if (lvp.position < rvp.position) return -1;
          	if (lvp.count > rvp.count) return 1;
        	if (lvp.count < rvp.count) return -1;
        	if (lvp.value > rvp.value) return 1;
        	if (lvp.value < rvp.value) return -1;
        	return 0;   
        }
    };
    
    
	/**
	 * ValProb frequency comparator for sorting and binary search
	 */
	public static final Comparator<ValProb> VpFreqSort = new Comparator<ValProb>() {
        @Override
        public int compare(ValProb lvp, ValProb rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
          	if (lvp.count < rvp.count) return 1;
        	if (lvp.count > rvp.count) return -1;
        	if (lvp.counter < rvp.counter) return 1;
        	if (lvp.counter > rvp.counter) return -1;
        	if (lvp.probability < rvp.probability) return 1;
        	if (lvp.probability > rvp.probability) return -1;
           	if (lvp.value < rvp.value) return 1;
        	if (lvp.value > rvp.value) return -1;
        	return 0;   
        }
    };
    
	/**
	 * Accum comparator for sorting and binary search
	 */
	static final Comparator<Accum> AccumSort = new Comparator<Accum>() {
        @Override
        public int compare(Accum lvp, Accum rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
        	if (lvp.total < rvp.total) return 1;
        	if (lvp.total > rvp.total) return -1;
        	return 0;   
        }
    };
    
	static final Comparator<VResultSet> ResultSetSort = new Comparator<VResultSet>() {
        @Override
        public int compare(VResultSet lvp, VResultSet rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
        	if (lvp.passTotal < rvp.passTotal) return 1;
        	if (lvp.passTotal > rvp.passTotal) return -1;
        	if (lvp.pTpass[2] < rvp.pTpass[2] ) return 1;
        	if (lvp.pTpass[2] > rvp.pTpass[2] ) return -1;     
        	if (lvp.val < rvp.val) return 1;
        	if (lvp.val > rvp.val) return -1;  
        	return 0;   
        }
    };
	static final Comparator<VResultSet> ResultSetSortSmall = new Comparator<VResultSet>() {
        @Override
        public int compare(VResultSet lvp, VResultSet rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
        	if (lvp.passTotal < rvp.passTotal) return 1;
        	if (lvp.passTotal > rvp.passTotal) return -1;
        	if (lvp.val > rvp.val) return 1;
        	if (lvp.val < rvp.val) return -1;  
        	return 0;   
        }
    };

    // sort numbersets.. largest first
	static final Comparator<List<Integer>> nsSort = new Comparator<List<Integer>>() {
        @Override
        public int compare(List<Integer> lns, List<Integer> rns) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
        	if (lns.size() < rns.size()) return 1;
        	if (lns.size() > rns.size()) return -1;
        	return 0;   
        }
    };
    	
	/**
	 * compare 2 VpLists
	 * @param vpl1
	 * @param vpl2
	 * @param max
	 * @return
	 */
	public static double compareLists(List<ValProb> vpl1, List<ValProb> vpl2) { 
		double match = 0;
		int end = vpl1.size();
		for (int i=0;i<end;i++) {
			ValProb vp = vpl1.get(i);
			int idx = ValProb.indexOf(vpl2, vp.value);
			if (idx >= 0) match++;
		}
		if (match == 0) return 0;

		double v1miss = vpl1.size()-match;
		v1miss += vpl2.size()-match;
		if (v1miss == 0) return 1;
		v1miss /= 2; // avg
		return match / v1miss;
	}

    
	/////////////////////////////////////////////////////////////////////////////////////////////////
	// MATH
	/////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * log2
	 * @param v
	 * @return
	 */
    public static double log2(double v) {
        return Math.log(v) / Math.log(2);
    }
    
    /**
     * softmax of input
     * @param input
     * @param values
     * @return
     */
    public static double softmax(double input, double[] values) {
        double total = Arrays.stream(values).map(Math::exp).sum();
        return Math.exp(input) / total;
    }

    /**
     * softmax values and save to input list, exclude values <= 0
     * @param values
     */
    public static void softmax(double[] values) {
        double total = 0;
        for (int i=0;i<values.length;i++) {
        	if (values[i] <= 0) continue;
        	total += Math.exp(values[i]);
        }
        for (int i=0;i<values.length;i++) {
        	if (values[i] <= 0) continue;
        	values[i] = Math.exp(values[i]) / total;
        }
    }

    /**
     * sigmoid of x
     * @param x
     * @return
     */
    public static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }
    
    /**
     * relu of x
     * @param x
     * @return
     */
    public static double relu(double x) {
        return Math.max(0, x);
    }
    
    /**
     * softplus of x
     * @param x
     * @return
     */
    public static double softplus(double x) {
        return Math.log(1 + Math.exp(x));
    }
    
    /**
     * tanh of x
     * @param x
     * @return
     */
    public static double tanh(double x) {
        return Math.tanh(x);
    }
    
    /**
     * tanhDer of x
     * @param x
     * @return
     */
    public static double tanhDer(double x) {
        return 1. - Math.pow(tanh(x), 2);
    }
   
    /**
     * get value for gaussian curve
     * @param x
     * @param standardDeviation
     * @param variance
     * @param mean
     * @return
     */
    public static double gaussianCurve(double x, double standardDeviation, double variance, double mean) { 
        return Math.pow(Math.exp(-(((x - mean) * (x - mean)) / ((2 * variance)))), 1 / (standardDeviation * Math.sqrt(2 * Math.PI))); 
    } 
    
	/**
	 * get varience - for gaussian curve
	 * @param population
	 * @return
	 */
	public static double variance(double[] population) {
        long n = 0;
        double mean = 0;
        double s = 0.0;

        for (double x : population) {
                n++;
                double delta = x - mean;
                mean += delta / n;
                s += delta * (x - mean);
        }
        // if you want to calculate std deviation
        return (s / n);
	}
	
	/**
	 * get mean of set m
	 * @param m
	 * @return
	 */
	public static double mean(double[] m) {
	    double sum = 0;
	    for (int i = 0; i < m.length; i++) {
	        sum += m[i];
	    }
	    return sum / m.length;
	}

	/**
	 * get median of set m
	 * the array double[] m MUST BE SORTED
	 * @param m m MUST BE SORTED
	 * @return
	 */
	public static double median(double[] m) {
	    int middle = m.length/2;
	    if (m.length%2 == 1) {
	        return m[middle];
	    } else {
	        return (m[middle-1] + m[middle]) / 2.0;
	    }
	}
	
	/**
	 * get mode of set a
	 * @param a
	 * @return
	 */
	public static int mode(int a[]) {
	    int maxValue = 0, maxCount = 0;

	    for (int i = 0; i < a.length; ++i) {
	        int count = 0;
	        for (int j = 0; j < a.length; ++j) {
	            if (a[j] == a[i]) ++count;
	        }
	        if (count > maxCount) {
	            maxCount = count;
	            maxValue = a[i];
	        }
	    }

	    return maxValue;
	}
	
	/**
	 * get standard deviation of set
	 * @param numArray
	 * @return
	 */
	public static double standardDeviation(double numArray[]) {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        for(double num : numArray) {
            sum += num;
        }

        double mean = sum/length;

        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation/length);
	}
	
	/**
	 * get dot production of 2 vectors
	 * @param a1
	 * @param a2
	 * @return
	 */
	public static double dotProduct(double a1[], double a2[]) {
		if (a1.length != a2.length) return 0;
		double dp = 0;
		for (int i=0;i<a1.length;i++) dp += a1[i]*a2[i];
		return dp;
	}

	/**
	 * get cartesian product of 2 vectors
	 * @param a1
	 * @param a2
	 * @return
	 */
	public static double cartesianProduct(double a1[], double a2[]) {
		double dp1 = 0;
		double dp2 = 0;
		for (int i=0;i<a1.length;i++) {
			dp1 += a1[i];
			dp2 += a2[i];
		}
		return dp1*dp2;
	}
	
	/**
	 * get vector length
	 * @param a1
	 * @return
	 */
	public static double vectorLength(double a1[]) {
		return Math.sqrt(dotProduct(a1, a1));
	}
	
	/**
	 * get vector magnitude
	 * @param a1
	 * @return
	 */
	public static double vectorMagnitude(double a1[]) {
		double acc = 0.0; //create accumulator
		//Pythagoras' theorem
		for(double value : a1){
			acc += Math.pow(value, 2);
		}
		return Math.pow(acc, 0.5);
	}
	
	/**
	 * get vector angle
	 * @param a1
	 * @param a2
	 * @return
	 */
	public static double vectorAngle(double a1[], double a2[]) {
		return Math.acos(dotProduct(a1, a2) / (vectorMagnitude(a1)*vectorMagnitude(a2)));
	}

	/**
	 * find intercept of to sets
	 * @param yList
	 * @param xList
	 * @return
	 */
	public static double intercept(List<Double> yList, List<Double> xList) {
	    if (yList.size() != xList.size()) return 0;
	    if (yList.size() < 2) return 0;

	    double yAvg = average(yList);
	    double xAvg = average(xList);

	    double sumNumerator = 0d;
	    double sumDenominator = 0d;
	    for (int i = 0; i < yList.size(); i++) {
	        double y = yList.get(i);
	        double x = xList.get(i);
	        double yDiff = y - yAvg;
	        double xDiff = x - xAvg;
	        double numerator = xDiff * yDiff;
	        double denominator = xDiff * xDiff;
	        sumNumerator += numerator;
	        sumDenominator += denominator;
	    }

	    double slope = sumNumerator / sumDenominator;
	    double intercept = yAvg - (slope * xAvg);
	    return intercept;
	}

	/**
	 * get average of set
	 * @param doubles
	 * @return
	 */
	private static double average(Collection<Double> doubles) {
	    return doubles.stream().collect(Collectors.averagingDouble(d -> d));
	}
	
	/**
	 * calculate the slope of a set/vector
	 * @param yList
	 * @return
	 */
	public static double slope(List<Double> yList) {
	    if (yList.size() < 2) return 0;

	    double yAvg = average(yList);
	    double xAvg = 1;

	    double sumNumerator = 0d;
	    double sumDenominator = 0d;
	    for (int i = 0; i < yList.size(); i++) {
	        double y = yList.get(i);
	        double x = 1;
	        double yDiff = y - yAvg;
	        double xDiff = x - xAvg;
	        double numerator = xDiff * yDiff;
	        double denominator = xDiff * xDiff;
	        sumNumerator += numerator;
	        sumDenominator += denominator;
	    }

	    double slope = sumNumerator / sumDenominator;
	    return slope;
	}
	
	/**
	 * get average slope (start to points) for a vpList
	 * @param vpList
	 * @return
	 */
	public static double slopeProbabilties(List<ValProb> vpList) {
	    if (vpList.size() < 2) return 0;

	    // start to end
	    double avg = 0;
	    for (int i=1;i<vpList.size();i++) {
		    double d1 = vpList.get(i).probability - vpList.get(0).probability;
		    avg += d1/i;	    	
	    }
	    return avg/(vpList.size()-1);
	}

	/**
	 * make integer array
	 * @param list
	 * @return
	 */
	public static int[] toIntArray(List<Integer> list) {
		int[] array = list.stream().mapToInt(i->i).toArray();
		//		int[] array = list.stream().mapToInt(Integer::intValue).toArray();
		return array;
	}
	
	/**
	 * make double array
	 * @param list
	 * @return
	 */
	public static double[] toDoubleArray(List<Integer> list) {
		double[] array = list.stream().mapToDouble(Integer::intValue).toArray();
		return array;
	}
	
	/**
	 * check if the string is only letters
	 * @param str
	 * @return true if only letters, else false
	 */
	public static boolean onlyLetters(String str) {
		for (int i=0;i<str.length();i++) {
			if (str.charAt(i) == '-') continue;
			if (!Character.isLetter(str.charAt(i))) return false;
		}
		return true;
	}
	
	/**
	 * Get best size for the sublists (most balanced)
	 * @param count
	 * @param listSize
	 * @return
	 */
	public static int getListSplit(int count, int listSize) {
		if (count <= 0) return listSize;
		int sz = listSize/count;
		int m = listSize % count;
		if (m == 0) return sz;		
		// adjust for the leftover
		return sz+1;
	}
	
	/**
	 * get token count for list of list
	 * @param tokList
	 * @return
	 */
	public static int getTokenCount(List<List<String>> tokList) {
		int cnt = 0;
		for (List<String> sl:tokList) cnt += sl.size();
		return cnt;
	}
	
	/**
	 * get token count for list of list
	 * @param tokList
	 * @return
	 */
	public static int getTokenSetCount(List<List<List<String>>> tokLists) {
		int cnt = 0;
		for (List<List<String>> sl:tokLists) cnt += getTokenCount(sl);
		return cnt;
	}
		
	/**
	 * get count of tokens in this dataSet - make uniqe set of tokens
	 * @param tokList
	 * @param uniqeList
	 * @return
	 */
	public static int getTokenUnique(List<List<String>> tokList, HashSet<String> uniqeList) {
		int cnt = 0;
		for (List<String> sl:tokList) {
			cnt += sl.size();
			if (uniqeList != null) {
				for (String s:sl) uniqeList.add(s);
			}
		}
		return cnt;
	}
	
	/**
	 * get number of matching values
	 * NOTE: assumes list sizes are identical
	 * 
	 * @param valueSets1
	 * @param valueSets2
	 * @return number of matching values
	 */
	public static int compareIntLists(List<List<Long>> valueSets1, List<List<Long>> valueSets2) {
		int mCnt = 0;
		for (int i=0;i<valueSets1.size();i++) {
			List<Long> vl1 = valueSets1.get(i);
			List<Long> vl2 = valueSets2.get(i);
			for (int xi=0;xi<vl1.size();xi++) {
				if (vl1.get(xi) == vl2.get(xi)) mCnt++;
			}
		}
		return mCnt;
	}
	
	/**
	 * get number of matching values use first ValProb value
	 * NOTE: assumes list sizes are identical
	 * @param valueSets1
	 * @param valueSets2
	 * @return
	 */
	public static int compareIntValProbLists(List<List<Long>> valueSets1, List<List<List<ValProb>>>  valueSets2) {
		int mCnt = 0;
		for (int i=0;i<valueSets1.size();i++) {
			List<Long> vl1 = valueSets1.get(i);
			List<List<ValProb>> vl2 = valueSets2.get(i);
			for (int xi=0;xi<vl1.size();xi++) {
				ValProb vp = vl2.get(xi).get(0);
				if (vl1.get(xi) == vp.value) mCnt++;
			}
		}
		return mCnt;
	}
	
	/**
	 * Get number of matching values use first ValProb value
	 * NOTE: assumes list sizes are identical
	 * [predictionType.ordinal][0] = pass
	 * [predictionType.ordinal][1] = fail
	 * @param valueSets1
	 * @param valueSets2
	 * @return
	 */
	public static int [][] compareIntValProbListsByPType(List<List<Long>> valueSets1, List<List<List<ValProb>>> valueSets2) {
		int pass = 0, fail = 0;
		int [][] count = new int[VegML.getPredictionTypeCount()+1][2];
		
		for (int i=0;i<valueSets1.size();i++) {
			List<Long> vl1 = valueSets1.get(i);
			List<List<ValProb>> vl2 = valueSets2.get(i);
			for (int xi=0;xi<vl1.size();xi++) {
				ValProb vp = vl2.get(xi).get(0);
				if (vl1.get(xi) == vp.value) {
					pass++;
					count[vp.type.ordinal()][0]++;
				} else {
					count[vp.type.ordinal()][1]++;
					fail++;
				}
			}
		}
		count[VegML.getPredictionTypeCount()][0] = pass;
		count[VegML.getPredictionTypeCount()][1] = fail;
		return count;
	}
	
	/**
	 * compare by prediction type
	 * @param valueSets1
	 * @param valueSets2
	 * @param vs1
	 * @param vs2
	 * @param megePassMatrix
	 * @return
	 */
	public static int compareIntValProbListsByPType(List<List<Long>> valueSets1, List<List<List<ValProb>>> valueSets2,
													List<List<List<ValProb>>> vs1, List<List<List<ValProb>>> vs2, 
													double [][] megePassMatrix) {
		int pass = 0;		
		for (int i=0;i<valueSets1.size();i++) {
			List<Long> vl1 = valueSets1.get(i);
			List<List<ValProb>> vl2 = valueSets2.get(i);
			
			List<List<ValProb>> v1 = vs1.get(i);
			List<List<ValProb>> v2 = vs2.get(i);
			
			for (int xi=0;xi<vl1.size();xi++) {
				ValProb vp = vl2.get(xi).get(0);
				if (vl1.get(xi) == vp.value) {
					// pass
					pass++;
					ValProb vpx1 = v1.get(xi).get(0);
					ValProb vpx2 = v2.get(xi).get(0);
					megePassMatrix[vpx1.type.ordinal()][vpx2.type.ordinal()]++;
				} else {
					// fail
				}
			}
		}
	
		return pass;
	}

	//
	// get sorted value map string
	//
	static String getValueCountString(VDataPlane dp, HashMap<Long, Integer> vcm) {
		// get whats in it
		if (vcm == null) return "";
		class valCnt {
			long value;
			int count;
		}
		List<valCnt> vcl = new ArrayList<>();
		for (Long val:vcm.keySet()) {
			valCnt vc = new valCnt();
			vc.value = val;
			vc.count = vcm.get(val);
			vcl.add(vc);
		}
		Collections.sort(vcl, new Comparator<valCnt>() {
	        @Override
	        public int compare(valCnt lvp, valCnt rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
	        	if (lvp.count > rvp.count) return 1;
	        	if (lvp.count < rvp.count) return -1;
	        	if (lvp.value > rvp.value) return 1;
	        	if (lvp.value < rvp.value) return -1;
	        	return 0; 
	        }
	    });
		String vs = "";
		for (valCnt vc:vcl) vs += dp.getString(vc.value)+"/"+vc.count + " ";
		return vs;
	}
	
	//
	// Print result set
	//
	static void showDebugResponse(VDataPlane dp, VFrame frame, Accum [] aList, List<ValProb> exVpList, int value) {
		
		Accum fac = null;
		Accum iac = null;
		if (dp.getCfgNSIdentityNumber() >= 0) iac = aList[dp.getCfgNSIdentityNumber()];
		if (dp.getCfgNSFullNumber() >= 0) fac = aList[dp.getCfgNSFullNumber()];
		// JUST IAC		
		//if (iac == null) return;

		// show only recall-predict
		//if (fac == null || fac.getValueCount() == 1) {
			// NOTE: this could still use a better option if there is a subset with the same count/value
		//	return;
		//}
		boolean isIacMostProb = false, haveIac = false;
		if (iac != null) {
			haveIac = true;
			isIacMostProb = iac.isMostProbableValue(value);
		}
		boolean isFacMostProb = false;
		if (fac != null) isFacMostProb = fac.isMostProbableValue(value);
		Accum dac = dp.getAccumDefault();

		
		System.out.println("    VECT[@"+frame.getDataSetPosition()+"]["+frame.getStringDebug()+"] => "
				+ "["+frame.getString()+"]["+dp.getString(value)+"] Fac["+isFacMostProb+"]Iac["+haveIac+"/"+isIacMostProb+"]");	
		
		List<ValProb> sumL = new ArrayList<>();
		
		int maxNumberSetIac = -1;
		double maxNumberSetPower = 0;
		boolean haveWage = false;
	
		for (int setNumber=0;setNumber<aList.length;setNumber++) {
			Accum ac = aList[setNumber];
			if (ac == null) continue;
			MLNumberSetHash nsh = dp.getNSHash(setNumber);
			if (nsh == null) continue;
			if (nsh.isTurnedOff()) continue;
			// get the set accumulators
			Accum sac = dp.getAccumSetDefault(setNumber);

			String ns = dp.getNSFormatString(setNumber);
			//if (iac != null && nsh.getNS().contains(identitysetNumber-1)) {
			if (iac != null && nsh.getNS().contains(dp.getCfgFrameFocus())) {
				if (dp.getCfgNSWeight(setNumber) > maxNumberSetPower) {
					maxNumberSetPower = dp.getCfgNSWeight(setNumber);
					maxNumberSetIac = setNumber;
				}
			}
			boolean isMostProb = ac.isMostProbableValue(value);
			
			List<ValProb> vpList = aList[setNumber].getValPsSorted();
			String s = "SET";
			if (isMostProb) s = "CRT";
			System.out.print("      "+s+"["+String.format("%3d", setNumber)+"]["+ns+"] => ["+String.format("%3d", vpList.size())+"]w["+String.format("%.6f", dp.getCfgNSWeight(setNumber))+"] ");
			for (int v=0;v<vpList.size();v++) {
				ValProb vp = vpList.get(v);
				// NOTE: this may not be the exact list
				if (iac != null && dp.isCfgIdentityOnly() && iac.getCount(vp.value) < 1) continue;

				double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), setNumber, false, sac, ac, vp);
			//	if (wavgProb <= 0) continue;
				sumL = VegUtil.mergeIntoVPList(sumL, vp);
				// only show top 5
				if (v <= 5) {
					if (wavgProb != vp.probability) {
						haveWage = true;
						System.out.print("  <'"+dp.getString(vp.value)+"' "+vp.probability+"/"+wavgProb+" "+vp.count+">");
					}
					else System.out.print("  <'"+dp.getString(vp.value)+"' "+vp.probability+" "+vp.count+">");
				}
			}
			System.out.println("");
		}
		if (sumL.size() == 0) {
			System.out.println("    SUM[NOTHING FOUND]");		
		} else {
			Collections.sort(sumL, VegUtil.VpSort);		
			System.out.println("    SUM['"+dp.getString(sumL.get(0).value)+"' "+sumL.get(0).probability+"]["+ sumL.get(0).count+"] max["+maxNumberSetIac+"]("+dp.getCfgNSIdentityNumber()+")");		
			System.out.print("       ["+sumL.size()+"]");
			for (int v=0;v<sumL.size();v++) {
				ValProb vp = sumL.get(v);
				System.out.print("  <'"+dp.getString(vp.value)+"' "+vp.probability+">");
				if (v == 4) break; /// MAX
			}
			System.out.println("");
			if (haveWage) {
				System.out.println("    XSUM['"+dp.getString(exVpList.get(0).value)+"' "+exVpList.get(0).probability+"]["+ exVpList.get(0).count+"]");		
				System.out.print("        ["+sumL.size()+"]");
				for (int v=0;v<exVpList.size();v++) {
					ValProb vp = exVpList.get(v);
					System.out.print("  <'"+dp.getString(vp.value)+"' "+vp.probability+">");
					if (v == 4) break; /// MAX
				}
				System.out.println("");
			}
		}
	}
	
	/**
	 * determine the format of token this is
	 * CURRENT:
	 * 	word/proper/loud/letter/mixed/number/symbol/identifier/initials/abr/acronym/alpha_num/hyphen_word/
	 * @param text
	 * @return
	 */
	public static String getStringFormat(String text) {
		if (text == null) return null;
		
		int lower = 0, upper = 0, letters = 0, numbers = 0, mt = 0, mtu = 0, st = 0, std = 0, ls = 0, dot = 0, hyp = 0, slash = 0, colon = 0, coma = 0;
		boolean fup = false, enddot = false, shyp = false, sslash = false;
		char first = 0;
		//String hash = getHashString(text, false, true);	
		for (int i=0;i<text.length();i++) {
			char c = text.charAt(i);		
			if (i == 0) first = c;
			if (c == '.') {
				dot++;
				if (i == (text.length()-1)) enddot = true;
			} else if (c == '-' || c == '—' || c == '–') {
				if (i == 0) shyp = true; // starts with hyphen
				hyp++;
			} else if (c == ':') {
				colon++;
			} else if (c == ',') {
				coma++;
			} else if (c == '/') {
				if (i == 0) sslash = true; // starts with slash
				slash++;
			} else if (Character.isLetter(c)) {
				if (Character.isLowerCase(c)) lower++;
				else if (Character.isUpperCase(c)) {
					upper++;
					if (i == 0) fup = true;
					if (ls == 0) mtu++;
				}
				letters++;
				ls++;
			} else if (Character.isDigit(c)) {
				numbers++;
			}
			
			if (!Character.isLetter(c) || i == (text.length()-1)) {
				if (ls > 1) {
					mt++; // multi-letter
				} else if (ls > 0) {
					if (c == '.') std++; // single letter dot "H."
					st++; // single letter
				}
				ls = 0;
			}
		}
		if (letters == 0) {
			if (numbers > 0) {
				if (numbers == text.length()) return "number";
				if ((numbers+1) == text.length() && dot == 1) return "number"; // 23.2 / .23
				if ((numbers+coma) == text.length()) return "number"; // 2,200 / .23
				if ((numbers+2) == text.length() && shyp && dot == 1) return "number"; // -23.2 / -.23
				if (shyp && numbers == (text.length()-1)) return "number"; // negative number
				return "identifier";
			} else return "symbol";
		}
		if (letters > 0 && numbers > 0 && hyp > 0 && colon > 0) {
			return "identifier";
		}
		if (shyp) {
			return "hyphen_word";
		}
		if (sslash) {
			return "mixed";
		}
		if (text.length() == 2 && (upper == 1 || lower == 1) && dot == 1) {
			return "initials";
		}
		if ((first == '@' || first == '#') && letters > 0 && text.length() < 30) {
			return "identifier";
		}
		if (letters == 1 && text.length() == 1) { // just one letter
			return "letter";
		}
		if (numbers > 0 && letters > 0 && (letters + numbers) == text.length()) {
			return "alpha_num";
		}
		// Acronym  (NASA, USA)
		if (upper == text.length()) {
			if (upper > 6) return "loud";
			return "acronym";
		}	
		// initials (U.S.A.)
		// 1 token, all single letter, have dots, match initials alias
		if (mt == 0 && upper > 0 && lower == 0 && dot >= 2) {
			return "initials";
		}
		if (mt == 0 && st == letters && dot > 0) {
			return "initials"; // t.e.s.t. / t e s. 
		}			
		// abr (Mrs., Ca, Jan)
		// 1 token, start upper, possible dot, match abr alias 
		if (fup && mt == 1 && upper == 1 && lower >= 1 && (text.length() < 3 || enddot)) {
			if (enddot) return "abr";
		}
		if (text.length() == 2 && letters == 2 && upper == 2) {
			return "acronym"; 
		}
		if (fup && text.length() == letters && letters < 4 && upper == 1) {
			return "proper";	
		}
		
		if (fup && upper == 1 && text.length() == letters && letters < 3) {
			// St To Am Vt Dr Mr
			return "abr";
		}
		
		// hyphen word (this-and-that)
		if (hyp > 0) {
			if (hyp == 1 && fup && letters == upper && letters < 4 && numbers > 0 && (text.length() == letters + numbers+1)) {
				return "alpha_num";
			}
			return "hyphen_word";				
		}
		if (numbers > 0) { 
			return "alpha_num";
		}

		if (mt == 0 && st == letters && slash > 0) {
			return "acronym"; // n/a / t/e/s
		}		 
		if (letters > 0 && numbers == 0 && ((lower == 0 && letters == upper) 
				|| ( letters > 2 && lower == 1 && text.endsWith("s")))) {
			// acronym OR acronym plural
			return "acronym";				
		}
		
		// abr_multi (SoMa, SoFi, MassiveAttribute, NRPRadio
		// 1 token, multiple uppercase segments (usually sylobol based break out)
		if (upper > 1 && lower >= 1 && dot == 0) {
			if (upper == 2) return  "proper"; // "O'Hara"/D'Angelo.. etc"
			return "mixed";
		}
		
		// initials and Name (H.G.Wells)
		// 1 token, multiple dots
		if (mt == 1 && dot > 0 && std > 0) {
			return "initials";
		}
		if ((upper + dot) == text.length() && dot == 1 && enddot) {  // JR. MS. MR.
			return "abr";
		}
		// dr. mr.
		if ((letters + dot) == text.length() && dot == 1 && enddot && letters < 5) {
			return "abr";				
		}			
		
		if (mtu == 1 && upper == 1 && lower > 1 && (upper + lower) == text.length()) {
			return "proper";				
		}
		 
		if (letters > 0 && numbers == 0 && ((lower == 0 && letters == upper) || ( letters > 2 && lower == 1 && text.contains("s")))) {
			// acronym OR acronym plural
			return "acronym";				
		}
		
		if (enddot) {
			return "abr";				
		}	
		if (upper > 0 && upper == letters) { // 'N 'S N'T
			return "proper";
		}
		if (dot > 0) {
			return "identifier";	
		}
		if (slash > 0) {
			return "mixed";	
		}
		return "word";
	}
	
	/**
	 * Determine the format of this token
	 * number/identifier/symbol/letter/word/loud/mixed/proper/hyphen_word/slash_word
	 * @param text
	 * @return
	 */
	public static String getStringFormatMin(String text) {
		if (text == null) return null;
		
		int lower = 0, upper = 0, letters = 0, numbers = 0, ap = 0, dot = 0, hyp = 0, slash = 0, colon = 0, coma = 0;
		boolean fup = false;

		for (int i=0;i<text.length();i++) {
			char c = text.charAt(i);		
			if (c == '.') dot++;
			else if (c == '-' || c == '—' || c == '–') hyp++;
			else if (c == '\'') ap++;
			else if (c == ':') colon++;
			else if (c == ',') coma++;
			else if (c == '/') slash++;
			else if (Character.isDigit(c)) numbers++;
			else if (Character.isLetter(c)) {
				if (Character.isLowerCase(c)) lower++;
				else if (Character.isUpperCase(c)) {
					upper++;
					if (i == 0) fup = true;
				}
				letters++;
			} 
		}
		if (letters == 0 && numbers > 0) {
			if ((numbers+hyp+coma+dot+colon) == text.length()) return "number";
			return "identifier";
		}
		if (letters == 0) return "symbol";
		if (letters == text.length() && letters == 1) return "letter";
		if (lower == text.length()) return "word";
		if (upper == text.length()) return "loud";
		if (fup && text.length() == letters && upper == 1) return "proper";	
		if ((letters+ap+dot) == text.length()) return "mixed";
		if ((letters+slash) == text.length()) return "slash_word";
		if ((letters+hyp+ap) == text.length()) return "hyphen_word";
		return "identifier";
	}
	
	/**
	 * check if the string is all uppercase
	 * @param s
	 * @return true if uppercase, else false
	 */
	public static boolean isUpperCase(String s) {
	    for (int i=0; i<s.length(); i++) {
	        if (!Character.isUpperCase(s.charAt(i))) return false;
	    }
	    return true;
	}
	
	/**
	 * check if the string is all lowercase
	 * @param s
	 * @return true if lowercase, else false
	 */
	public static boolean isLowerCase(String s) {
	    for (int i=0; i<s.length(); i++) {
	        if (!Character.isLowerCase(s.charAt(i))) return false;
	    }
	    return true;
	}
	
	/**
	 * get a proper format version of this string
	 * @param str string to format
	 * @return
	 */
	public static String toProperFmt(String str) {
		return (str.charAt(0)+"").toUpperCase()+str.toLowerCase().substring(1);
	}
	
	

	
}
