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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import vegml.VegML.NSWeightBase;



/**
 * Utility for methods that deal with numberSets
 * 
 * Not well documented, but not all that complex
 * 
 */
class MLNumberSetUtil {
	private static final int INIT_WINDOW = 7;

	//
	// set to retain for each size
	//	
	private static HashMap<Integer, List<List<Integer>>> setMap = new HashMap<>();
	static {
		for (int i=0;i<=INIT_WINDOW;i++) getSubset(i);
	}
	
	
	//
	// generate all subsets (ordered)
	// to optimize these sets should be static final NOT derived 
	// fullset and singles are not added
	// lset is the set of size-1 OR null
	private static List<List<Integer>> generateSubsets(int window) {
		if (window == 0) return null;
		List<List<Integer>> l = new ArrayList<>();	
		List<List<Integer>> lset = setMap.get(window-1);
		
		// list for each position and count: positions must be consistant
		// add last set to start
		if (lset != null) l.addAll(lset);
		int cnt = 0;
		List<Integer> lfull = null;
        // subsets one by one
        for (int i = 0; i < (1<<window); i++) {
    		List<Integer> ll = new ArrayList<>();	
            for (int j = 0; j < window; j++) {
                if ((i & (1 << j)) > 0) {
                    ll.add(j);
                } 
            }
            if (ll.size() < 1) continue;
            cnt++;
            if (lset != null && cnt <= lset.size()) continue;
            if (ll.size() == window) {
            	lfull = ll;
            } else {
            	l.add(ll);
            }
        }
        // add full last
        l.add(lfull);
		//System.out.println("SET["+window+"] => ["+l.size()+"]");
		return l;
	}

	//
	// generate the dependent sets for a numberSet and a dependent count
	//
	public static List<List<Integer []>> generateDependentSubsets(List<Integer> numberSet, int depCount) {
		List<List<Integer []>> nsl = new ArrayList<>();

		// A
		// A Ax
		// A Ax Ay Axy
		// make full set of dependents; all must include parent
		int maxSize = 0;
		List<List<Integer>> depSet = generateSubsets(depCount+1); 
		for (int i=0;i<depSet.size();i++) {
			if (!depSet.get(i).contains(0) || depSet.get(i).size() < 1 
				//	|| (depSet.get(i).size() == 1 && depSet.get(i).get(0) == 0) // just the primary
					) {
				//System.out.println("     drop: " + NumberSets.setToString(depSet.get(i)));
				depSet.remove(i);
				i--;
				continue;
			}
			if (depSet.get(i).size() > maxSize) maxSize = depSet.get(i).size();
			//System.out.println("   SUB: " + NumberSets.setToString(depSet.get(i)));
		}
		//System.out.println("GEN[" + NumberSets.setToString(numberSet) + "]   => ["+maxSize+"]: " + depSet.size());
	
		// for base and each dependent: always has an entry for each number		
		addNextValue(nsl, numberSet, depSet, new ArrayList<>(), 0, maxSize);
		return nsl;
	}
	
	private static void addNextValue(List<List<Integer []>> nsl, List<Integer> numberSet, List<List<Integer>> depSet, List<Integer []> ns, int ppos, int maxSize) {
		for (int depPos=0;depPos<depSet.size();depPos++) {
			Integer [] nsv = new Integer[maxSize];
			Arrays.fill(nsv, -1);
			
			nsv[0] = numberSet.get(ppos);
			int np = 1;
			for (int n=0;n<depSet.get(depPos).size();n++) {
				if (depSet.get(depPos).get(n) == 0) continue; // added real number there
				nsv[np] = depSet.get(depPos).get(n);
				np++;
			}
			
			List<Integer []> l = new ArrayList<>(ns);
			l.add(nsv);
			if (ppos == (numberSet.size()-1)) {
				// end of chain
				nsl.add(l);
				/*
				String s = "";
				for (Integer [] nl:l) {
					s += "{";
					for (Integer nsi:nl) s += nsi+" ";
					s += "} ";
				}
				System.out.println("  SET["+nsl.size()+"]: " + s);
				*/
			} else {
				// move on
				addNextValue(nsl, numberSet, depSet, l, ppos+1, maxSize);
			}
		}
	}
	

	// 
	static List<List<Integer>> getLinearSets(int window, int focus) {
		List<List<Integer>> sets = new ArrayList<>();
		if (window == 1) {
			List<Integer> s = new ArrayList<>();
			s.add(0);
			sets.add(s);
			return sets;
		}
		for (int i=0;i<window;i++) {
			List<Integer> s = new ArrayList<>();
			s.add(i);
			sets.add(s);
		}	
		// full
		List<Integer> s = new ArrayList<>();
		for (int i=0;i<window;i++) s.add(i);
		sets.add(s);
		return sets;
	}
	
	// 
	static List<List<Integer>> getSequenceLeftSets(int window, int focus) {
		List<List<Integer>> sets = new ArrayList<>();
		if (window == 1) {
			List<Integer> s = new ArrayList<>();
			s.add(0);
			sets.add(s);
			return sets;
		}
		//System.out.println("  getSequenceLeftSets: " +  window + " / " + focus);
		int end = focus;
		if (end < 0 || end > window) end = window;
		int max = 0;
		for (int i=1;i<=end;i++) {
			List<Integer> s = new ArrayList<>();
			for (int x=0;x<i;x++) s.add(x);
			sets.add(s);
			if (s.size() > max) max = s.size();
			//System.out.println("  xSeqL: " +  NumberSets.setToStringPosition(s, window, focus));
		}	
		// full
		if (max < window) {
			List<Integer> s = new ArrayList<>();
			for (int i=0;i<window;i++) s.add(i);
			sets.add(s);
		}
		return sets;
	}
	
	static List<List<Integer>> getSequenceRightSets(int window, int focus) {
		List<List<Integer>> sets = new ArrayList<>();
		if (window == 1) {
			List<Integer> s = new ArrayList<>();
			s.add(0);
			sets.add(s);
			return sets;
		}
		int start = focus;
		if (start < 0 || start >= window) start = 0;
		for (int i=start;i<window;i++) {
			List<Integer> s = new ArrayList<>();
			for (int x=i;x<window;x++) s.add(x);
			if (s.size() < window) sets.add(0, s);
		}
		// full
		List<Integer> s = new ArrayList<>();
		for (int i=0;i<window;i++) s.add(i);
		sets.add(s);
		return sets;
	}
	static List<List<Integer>> getSequenceFanSets(int window, int focus) {
		List<List<Integer>> sets = new ArrayList<>();
		if (window == 1) {
			List<Integer> s = new ArrayList<>();
			s.add(0);
			sets.add(s);
			return sets;
		}
		int mid = focus;

		for (int i=0;i<window && i<mid;i++) {
			// center to left
			List<Integer> s = new ArrayList<>();
			for (int x=i;x<=mid;x++) s.add(x);
			sets.add(s);
			//System.out.println(" SET0["+window+"]["+i+"] "+ NumberSets.setToString(s));
			
			for (int xi=mid;xi<window;xi++)  {
				s = new ArrayList<>();
				for (int x=i;x<=mid;x++) s.add(x);
				int c=0;
				for (int x=mid;x<=xi;x++) {
					if (!s.contains(x)) {
						s.add(x);
						c++;
					}
				}
				if (s.size() < window && c > 0) {
					sets.add(s);
				}
			}		
		}
		for (int i=(mid+1);i<=window;i++) {
			// center to right
			List<Integer> s = new ArrayList<>();
			for (int x=mid;x<i;x++) s.add(x);
			sets.add(s);
			//System.out.println("  xFanR: " +  NumberSets.setToStringPosition(s, window, focus));
		}

		List<Integer> s = new ArrayList<>();
		for (int i=0;i<window;i++) s.add(i);
		sets.add(s);
		return sets;
	}
	static List<List<Integer>> getSequenceEdgeSets(int window, int focus) {
		List<List<Integer>> sets = new ArrayList<>();
		if (window == 1) {
			List<Integer> s = new ArrayList<>();
			s.add(0);
			sets.add(s);
			return sets;
		}
		int mid = focus;
		for (int i=1;i<=window && i<=mid;i++) {
			// Left to center
			List<Integer> s = new ArrayList<>();
			for (int x=0;x<i;x++) s.add(x);
			sets.add(s);
			
			for (int xi=mid;xi<window;xi++)  {
				s = new ArrayList<>();
				for (int x=0;x<i;x++) s.add(x);
				for (int x=xi;x<window;x++) s.add(x);
				if (s.size() < window) sets.add(s);
			}
		}	
		for (int i=mid;i<window;i++) {
			// Right to center
			List<Integer> s = new ArrayList<>();
			for (int x=i;x<window;x++) s.add(x);
			sets.add(s);
		}	
		List<Integer> s = new ArrayList<>();
		for (int i=0;i<window;i++) s.add(i);
		sets.add(s);
		return sets;
	}



	public static String setToString(List<Integer> ll) {
		if (ll == null) return "NULL";
        String s = "";
		for (int o=0;o<ll.size();o++) s += ll.get(o)+" ";
		return s.trim();
	}
	public static String setToStringL(List<Long> ll) {
		if (ll == null) return "NULL";
        String s = "";
		for (int o=0;o<ll.size();o++) s += ll.get(o)+" ";
		return s.trim();
	}
	public static String setToString(Set<Integer> ll) {
		if (ll == null) return "NULL";
        String s = "";
		for (Integer i:ll) s += i+" ";
		return s.trim();
	}
	public static String setToString(int [] ll) {
		if (ll == null) return "NULL";
        String s = "";
		for (int o=0;o<ll.length;o++) s += ll[o]+" ";
		return s.trim();
	}
	public static String setToString(double [] ll) {
		if (ll == null) return "NULL";
        String s = "";
		for (int o=0;o<ll.length;o++) s += ll[o]+" ";
		return s.trim();
	}
	public static String setToStringCamma(double [] ll) {
		if (ll == null) return "NULL";
        String s = "";
		for (int o=0;o<ll.length;o++) {
			s += ll[o];
			if (o != ll.length-1) s += ", ";
		}
		return s.trim();
	}
	public static String setToStringPosition(List<Integer> ll, int window, int position) {
		if (ll == null) return "NULL";
        String s = "";
        for (int i=0;i<window;i++) {
        	boolean found = false;
			for (int o=0;o<ll.size();o++) {
				if (i == ll.get(o)) {
					found = true;
					break;
				}
			}
			if (found) {
				if (position == i) s+= "X ";
				else s+= "x ";				
			} else {
				if (position == i) s+= "+ ";
				else s+= "- ";
			}
        }
        // outside the window and definition
        for (int i=0;i<ll.size();i++) {
        	if (ll.get(i) < 0) s+= "D ";
        }
		return s.trim();
	}
	static void printSets(List<List<Integer>> sets, int window, int focus) {
		if (sets == null) return;
		System.out.println("SET["+window+"]["+focus+"] sz: " + sets.size());
		for (int o=0;o<sets.size();o++) {
			System.out.println(" " + MLNumberSetUtil.setToStringPosition(sets.get(o), window, focus));
		}
	}
	
	// get number sets for this window size
	static List<List<Integer>> getSubset(int window) {
		if (window <= 0) return null;
		List<List<Integer>> sets = setMap.get(window);
		if (sets != null) return sets;
		sets = generateSubsets(window);
		setMap.put(window, sets);
		return sets;
	}
	
	static List<List<Integer>> getSubsetCopy(int window) {
		List<List<Integer>> s = getSubset(window);
		List<List<Integer>> sc = new ArrayList<>();
		for (int i=0;i<s.size();i++) {
			List<Integer> sn = s.get(i);
			sc.add(new ArrayList<>(sn)); 
		}
		return sc;
	}	
	
	// true if this set is a sequence
	public static boolean isSetSequence(List<Integer> set) {
		if (set.size() == 1) return false;
		for (int k=1;k<set.size();k++) {
			if (set.get(k) != (set.get(k-1))+1) return false;
		}
		return true;
	}
	// true if this set is a bridge (has gap)
	public static boolean isSetBridge(List<Integer> set) {
		if (set.size() == 1) return false;
		for (int k=1;k<set.size();k++) {
			if (set.get(k) != (set.get(k-1))+1) return true;
		}
		return false;
	}
	
	// get the sets that are sequences
	// ALLOCATED LIST!!
	static List<List<Integer>> getSubsetSequences(int window) {
		List<List<Integer>> subs = setMap.get(window);
		List<List<Integer>> seqs = new ArrayList<>();
		for (int i=0;i<subs.size();i++) {
			List<Integer> set = subs.get(i);
			if (isSetSequence(set)) seqs.add(set);
		}
		return seqs;
	}
	static List<Integer> getSubsetNumbersSequences(int window) {
		List<List<Integer>> subs = setMap.get(window);
		List<Integer> seqs = new ArrayList<>();
		for (int i=0;i<subs.size();i++) {
			List<Integer> set = subs.get(i);
			if (isSetSequence(set)) seqs.add(i);
		}
		return seqs;
	}
	static List<List<Integer>> getSubsetSequencesEndWith(int window, int lastOffset) {
		List<List<Integer>> subs = setMap.get(window);
		List<List<Integer>> seqs = new ArrayList<>();
		for (int i=0;i<subs.size();i++) {
			List<Integer> set = subs.get(i);
			if (set.get(set.size()-1) != lastOffset) continue;
			if (isSetSequence(set)) seqs.add(set);
		}
		return seqs;
	}
	static List<Integer> getSubsetNumbersSequencesEndWith(int window, int lastOffset) {
		List<List<Integer>> subs = setMap.get(window);
		List<Integer> seqs = new ArrayList<>();
		for (int i=0;i<subs.size();i++) {
			List<Integer> set = subs.get(i);
			if (set.get(set.size()-1) != lastOffset) continue;
			if (isSetSequence(set))seqs.add(i);
		}
		return seqs;
	}
	static List<List<Integer>> getSubsetSequencesStartsWith(int window, int startOffset) {
		List<List<Integer>> subs = setMap.get(window);
		List<List<Integer>> seqs = new ArrayList<>();
		for (int i=0;i<subs.size();i++) {
			List<Integer> set = subs.get(i);
			if (set.get(0) != startOffset) continue;
			if (isSetSequence(set)) seqs.add(set);
		}
		return seqs;
	}
	static List<Integer> getSubsetNumbersSequencesStartsWith(int window, int startOffset) {
		List<List<Integer>> subs = setMap.get(window);
		List<Integer> seqs = new ArrayList<>();
		for (int i=0;i<subs.size();i++) {
			List<Integer> set = subs.get(i);
			if (set.get(0) != startOffset) continue;
			if (isSetSequence(set)) seqs.add(i);
		}
		return seqs;
	}
	static List<List<Integer>> getSubsetSequencesContains(int window, int offset) {
		List<List<Integer>> subs = setMap.get(window);
		List<List<Integer>> seqs = new ArrayList<>();
		for (int i=0;i<subs.size();i++) {
			List<Integer> set = subs.get(i);
			if (isSetSequence(set)) {
				if (set.get(0) > offset) continue;
				if (set.get(set.size()-1) < offset) continue;
				seqs.add(set);
			}
		}
		return seqs;
	}
	static List<Integer> getSubsetNumbersSequencesContains(int window, int offset) {
		List<List<Integer>> subs = setMap.get(window);
		List<Integer> seqs = new ArrayList<>();
		for (int i=0;i<subs.size();i++) {
			List<Integer> set = subs.get(i);
			if (isSetSequence(set)) {
				if (set.get(0) > offset) continue;
				if (set.get(set.size()-1) < offset) continue;
				seqs.add(i);
			}
		}
		return seqs;
	}
	
	// get a set based on its number
	static List<Integer> getSet(int window, int setNumber) {
		if (setNumber < 0) return null; // default sets
		if (setNumber >= getSubset(window).size()) return null;
		return getSubset(window).get(setNumber);
	}
	static List<Integer> getSetFull(int window) {
		// full is always last in the subset
		List<List<Integer>> s = getSubset(window);
		if (s == null) return null;
		return s.get(getSetFullNumber(window));
	}
	static int getSetFullNumber(int window) {
		// full is always last in the subset
		List<List<Integer>> s = getSubset(window);
		if (s == null) return 0;
		return s.size()-1;
	}
	
	// get all subsets numberSet numbers for setNumber
	// if subsetSize > 0 then only those of that size
	static List<Integer> getSubsetsOrder(List<List<Integer>> sets, int setNumber, int subsetSize) {
		List<Integer> set = sets.get(setNumber);
		List<Integer> list = new ArrayList<>();
		
		for (int i=0;i<sets.size();i++) {
			List<Integer> ss = sets.get(i);
			if (ss.size() >= set.size()) continue;
			if (subsetSize > 0 && ss.size() != subsetSize) continue;
			if (MLNumberSetUtil.isSubset(set, ss)) {
				list.add(i);
			}
		}
		
		return list;
	}	
	

	static int findSet(List<List<Integer>> lset, List<Integer> set, int s2offset) {
		if (lset == null) return -1;
        for (int si=0;si<lset.size();si++) {
        	if (compareSet(lset.get(si), set, s2offset)) return si;
        }
        return -1;
	}
	static int findSet(List<List<Integer>> lset, List<Integer> set) {
		if (lset == null) return -1;
        for (int si=0;si<lset.size();si++) {
        	if (compareSet(lset.get(si), set)) return si;
        }
        return -1;
	}
	
	// find the setnumber for this set
	static int findSetNumber(int window, List<Integer> set) {
		List<List<Integer>> su = setMap.get(window);
		for (int i=0;i<su.size();i++) {
			List<Integer> s = su.get(i);
			if (MLNumberSetUtil.compareSet(s, set)) return i;
		}
        return -1;
	}
	
	//
	// get count of members of findSet in set
	static int getCountInSet(List<Integer> findSet, List<Integer> set) {
		int count = 0;
		for (int i=0;i<findSet.size();i++) {
			if (set.contains(findSet.get(i))) count ++;
		}
        return count;
	}
	
	//
	// this returns a possible accumulator for each subset for the data at forcusOffset
	// makes a vector for each subset, then gets the accum
	//
	static boolean setContainsOffset(List<Integer> set, int off) {
		for (int x=0;x<set.size();x++) {
			if (set.get(x) == off) return true;
		}
		return false;
	}	
	// compare 2 sets with before offset in s2
	static boolean compareSet(List<Integer> s1, List<Integer> s2) {
		if (s1.size() != s2.size()) {
			return false;
		}
		for (int i=0;i<s1.size();i++) {
			if (s1.get(i).intValue() != (s2.get(i).intValue())) {
				return false;
			}
		}
		return true;
	}
	// compare 2 sets with before offset in s2
	private static boolean compareSet(List<Integer> s1, List<Integer> s2, int s2offset) {
		if (s1.size() != s2.size()) return false;
		for (int i=0;i<s1.size();i++) {
			if (s1.get(i).intValue() != (s2.get(i).intValue()+s2offset)) return false;
		}
		return true;
	}
	// of subset is a subset of set then true: A,B,C vs A,C/A/A,B/B/C/B,C
	static boolean isSubset(List<Integer> set, List<Integer> subset) {
		if (set.size() <= subset.size()) return false;
		for (int i=0;i<subset.size();i++) {
			int sv = subset.get(i).intValue();
			boolean found = false;
			for (int k=0;k<set.size();k++) {
				if (sv == set.get(k).intValue()) {
					found = true;
					break;
				}
			}
			if (!found) return false;
		}
		return true;
	}
	
	
	//
	// if the set contains a sequence of length that includes position
	//
	static boolean isSetContainsSequence(List<Integer> set, int size, int position) {
		if (set.size() < size) return false;
		int seqCnt = 1;
		boolean hasPosition = false;
		int last = -2;
		for (int i=0;i<set.size();i++) {
			int sv = set.get(i);
			if (sv == (last+1)) {
				seqCnt++;
			} else {
				hasPosition = false;
				seqCnt = 1;
				if (sv > position) return false;
			}
			if (sv == position) hasPosition = true;
			if (hasPosition && seqCnt >= size) return true;
			last = sv;
		}
		return false;
	}
	
	static boolean containsVectSet(int [] set, int [] vectSet) {
		if (set.length < vectSet.length) return false;
		for (int i=0;i<set.length;i++) {
			if (set[i] == vectSet[0]) {
				boolean match = true;
				for (int xi=1;xi<vectSet.length;xi++) {
					if ((i+xi) >= set.length || vectSet[xi] != set[i+xi]) {
						match = false;
						break;
					}
				}
				if (match) return true;
			}
		}
		return false;
	}
	static boolean contains(List<List<Integer>> vl, List<Integer> v2) {
		
		for (int i=0;i<vl.size();i++) {
			List<Integer> v1 = vl.get(i);
			if (MLNumberSetUtil.compareSet(v2, v1)) return true;
		}
		return false;
	}
	
	//
	// if the sub vector set is in the set vector set
	// this checks that position values are in both 
	// NOT FOR VECTSETS!!
	//
	static boolean isSubVectorSet(int [] set, int [] subset) {
		if (set.length <= subset.length) return false;
		for (int i=0;i<subset.length;i++) {
			int sv = subset[i];
			boolean found = false;
			for (int k=0;k<set.length;k++) {
				if (sv == set[k]) {
					found = true;
					break;
				}
			}
			if (!found) return false;
		}
		return true;
	}
	
	//
	// if set1 contains all NON-0 elements of set2 then Match
	// matchEnd starts from the end if set1 is smaller than set2, else start from the start
	static boolean isSetContains(int [] set1, int [] valSet, boolean matchEnd) {
		if (set1.length > valSet.length) return false;
		if (set1.length == valSet.length) {
			for (int i=0;i<valSet.length;i++) {
				if (valSet[i] == 0) continue;
				if (valSet[i] != set1[i]) return false;
			}
			return true;
		}
		// set1 < valSet  - if missing elements are null in valSet
		int off = valSet.length - set1.length;
		if (matchEnd) {
			// n, y, z, 0, 0
			for (int i=0;i<off;i++) {
				if (valSet[(valSet.length-1)-i] != 0) return false;
			}
			for (int i=0;i<set1.length;i++) {
				if (valSet[i] == 0) continue;
				if (valSet[i] != set1[i]) return false;
			}			
		} else {
			// 0, 0, n, y, z
			for (int i=0;i<off;i++) {
				if (valSet[i] != 0) return false;
			}
			for (int i=0;i<set1.length;i++) {
				if (valSet[i+off] == 0) continue;
				if (valSet[i+off] != set1[i]) return false;
			}	
		}
		return true;
	}
	
	//
	// Map a set of number set to a its subset: for a window size map to a smaller windowsize each
	// Map contains the position of larger set
	public static List<Integer> mapSetSizes(int size, int subsetSize) {
		List<Integer> map = new ArrayList<>();
		List<List<Integer>> set = MLNumberSetUtil.getSubset(size);
		List<List<Integer>> subset = MLNumberSetUtil.getSubset(subsetSize);
		
		// get the focus point to grow from
		int sFocus = size/2;
		int ssFocus = subsetSize/2;
		// Initial offset index to add to smaller set
		int off = sFocus-ssFocus;
		//7: - x - x - - x
		//6: - x - x - -
		//5:   x - x - -
		//4:   x - x -
		//3:     - x -
		//2:     -x		
		for (int i=0;i<subset.size();i++) {
			List<Integer> ss = subset.get(i);
			for (int xi=0;xi<set.size();xi++) {
				List<Integer> s = set.get(xi);
				if (s.size() != ss.size()) continue;
				boolean found = true;
				for (int xxi=0;xxi<s.size();xxi++) {
					if (s.get(xxi) != (ss.get(xxi)+off)) {
						found = false;
						break;
					}
				}
				if (found) {
					map.add(xi);
					break;
				}
			}
		}

		return map;
	}

	//
	// the value for a position
	// gets raw values
	// 
	public static double [] makeNumberSetWeights(List<List<Integer>> set, int window, int focus, NSWeightBase nsbase) {	
		if (set == null) return null; // 0 set
		double [] wset = new double[set.size()];

		for (int x=0;x<set.size();x++) {
			if (nsbase == NSWeightBase.NaturalId) {
				wset[x] = set.get(x).size(); // based on size		
				if (wset[x] == 1 && set.get(x).get(0) == focus) wset[x] = window; // identity
			} else if (nsbase == NSWeightBase.Natural) {
				wset[x] = set.get(x).size(); // based on size
				//wset[x] += ((double)(x*wset[x]) *  0.0001); // slight change
			} else if (nsbase == NSWeightBase.DistanceLinear) {
				List<Integer> nsi = set.get(x);
				double w = 0;
				for (int i=0;i<nsi.size();i++) {
					int ps = nsi.get(i);
					if (ps == focus) {
						w += window;
					} else {
						int d = (focus-ps); // before: degrade slower
						if (ps > focus) d = (ps-focus); // after: degrade faster?
						w += window-d;
					}
				}
				wset[x] = w;
			} else if (nsbase == NSWeightBase.Distance) {
				double div = 2; // 1/2 each step
				List<Integer> nsi = set.get(x);
				double w = 0;
				for (int i=0;i<nsi.size();i++) {
					int ps = nsi.get(i);
					int d = 0;
					double q = 1.0;
					if (ps != focus) {
						d = (focus-ps); // before: degrade slower
						if (ps > focus) d = (ps-focus); // after: degrade faster?
						for (int xx=0;xx<d;xx++) q /= div;
					}
					//System.out.println( "  "+x+")["+focus+"] "+d+" "+i+ " == " + q);
					w += q;
				}
				wset[x] = w;
			} else if (nsbase == NSWeightBase.Flat) {
				wset[x] = 1; // all the same
			} else if (nsbase == NSWeightBase.None) {
				wset[x] = 1; // all the same
			} else {
				wset[x] = 1; // all the same				
			}
		}
		return wset;
	}

}
