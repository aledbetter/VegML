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


package vegml.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vegml.VegML;

public class VectorToVid {	
	private static final int PRIME = 59; 
	private static final int PRIME2 = 37; 
	private static final int PRIME_START = 17; 
	private static final long PRIME_BASE = 1125899906842597L;
	static final int VIDplaceHolder = 1975;

	////////////////////////////////////////////////////////////////
	// Overridable default implementation
	////////////////////////////////////////////////////////////////
	//
	// make number(vector) from word
	//
	protected int toVectS(final String string) {
		if (string == null || string.length() == 0) return VegML.emptyVect;
		if (string.length() == 2) {
			// ugly hack to prevent colisions...(same alg hashCode32()) 
			int res = (PRIME*PRIME_START) + string.charAt(0);
			res = (PRIME*res) + '~';
			res = (PRIME*res) + ' ';
			return (PRIME*res) + string.charAt(1);
		}
		return hashCode32(string);
	}
	// from set of strings (faster than merging string)
	protected long toVectL(final long obj) {
		return (long)obj;
	}
	protected long toVectI(final int obj) {
		return (long)obj;
	}
	protected long toVectF(final Double obj) {
		return (long)obj.longValue();
	}	
	protected long toVectB(final Boolean obj) {
		return (((Boolean)obj) == true)?1:0;
	}
	protected long toVectC(final char obj) {
		return toVectS(""+obj);
	}
	public <T> int toVectGen(final T obj) {
		if (obj == null) return VegML.emptyVect;
		if (obj instanceof String) return toVectS((String)obj);
		if (obj instanceof Long) return (int)toVectL((Long)obj);
		if (obj instanceof Integer) return (int)toVectI((Integer)obj);
		if (obj instanceof Double) return (int)toVectF((Double)obj);
		if (obj instanceof Boolean) return (int)toVectB((Boolean)obj);
		if (obj instanceof Character) return (int)toVectC((Character)obj);
		return hashCode32(obj.toString());
	}

	
	
	
	////////////////////////////////////////////////////////////////
	// static default implementation
	////////////////////////////////////////////////////////////////
	// too many collisions
	//
	// make number(vector) from word
	//
	private static int toVectorS(final String string) {
		if (string == null || string.length() == 0) return VegML.emptyVect;
		if (string.length() == 2) {
			// ugly hack to prevent colisions...(same alg hashCode32()) 
			int res = (PRIME*PRIME_START) + string.charAt(0);
			res = (PRIME*res) + '~';
			res = (PRIME*res) + ' ';
			return (PRIME*res) + string.charAt(1);
		}
		return hashCode32(string);
	}
	// from set of strings (faster than merging string)
	private static long toVectorL(final long obj) {
		return (long)obj;
	}
	private static long toVectorI(final int obj) {
		return (long)obj;
	}
	private static long toVectorF(final Double obj) {
		return (long)obj.longValue();
	}	
	private static long toVectorB(final Boolean obj) {
		return (((Boolean)obj) == true)?1:0;
	}
	public static <T> int toVectorGen(final T obj) {
		if (obj == null) return VegML.emptyVect;
		if (obj instanceof String) return toVectorS((String)obj);
		if (obj instanceof Long) return (int)toVectorL((Long)obj);
		if (obj instanceof Integer) return (int)toVectorI((Integer)obj);
		if (obj instanceof Double) return (int)toVectorF((Double)obj);
		if (obj instanceof Boolean) return (int)toVectorB((Boolean)obj);
		return hashCode32(obj.toString());
	}
	
	
	public static int toVectorS(final List<String> stringSet) {
		if (stringSet == null || stringSet.size() < 1) return VegML.emptyVect;
		int result = PRIME_START;
		for (int i=0;i<stringSet.size();i++) {
			result = hashCode32(result, stringSet.get(i));
			if (i != stringSet.size()-1) result = hashCode32(result, ' ');
		}
		return result;
	}

	

	// map tiers
	public static void vectSetGen(final boolean nsMap[][][], final int nsMapLen [], final int frameVect [][], long [] vectSpace, final boolean noEmpty, final int vectNumAllowEmpty) {

		// base zero
		Arrays.fill(vectSpace, -1);
	
		// for space in window
		for (int i=0;i<nsMap[0].length;i++) {  // NS
			// for numberSet
			for (int x=0;x<nsMapLen.length; x++) {	// Window
				//System.out.println("  V["+i+"/"+x+"] len["+nsMapLen[x]+"] x["+nsMap[x][i][0]+"]");
				// for each get the info
				if (nsMapLen[x] < 1 || !nsMap[x][i][0]) continue; // have this position? (this works dependent or not)
				if (noEmpty && x != vectNumAllowEmpty && frameVect[0][i] == VegML.emptyVect) vectSpace[x] = -2;
				if (vectSpace[x] == -2) continue; // had empty spot
				// FOR dependent probability values
				// [vector][window][tier]
				if (nsMap[x][i].length > 1) {
					// may have dependent values 0 - n; map in those that exist (the above check for 0 ensures at least 1 element event if base)
					// position 0 -> used above to designate vector here
					// position 1 -> is the primary
					// position 2-n -> are the dependents
					for (int d=1;d<nsMap[x][i].length;d++) {
						int framedpos = d-1;
						//if (framedpos != 0) continue; // this should replicate without deps..

						if (nsMap[x][i][d] && frameVect[framedpos][i] == VegML.emptyVect && noEmpty && x != vectNumAllowEmpty) {
							// need this one and not here
							vectSpace[x] = -2;
							break;
						} 						
						// add all empty postions to prevent collisoins when depdent values are the same
						// increment the vector value
						if (!nsMap[x][i][d]) {
							vectSpace[x] = toVectorV64Inc(nsMapLen[x], VIDplaceHolder, vectSpace[x]);								
				//			System.out.println("     v["+i+"/"+x+"]pos["+framedpos+"|"+d+"] =F=> ["+nsMapLen[x]+"] [" + VIDplaceHolder+"] ["+vectSpace[x]+"]");	
						} else {
							vectSpace[x] = toVectorV64Inc(nsMapLen[x], frameVect[framedpos][i], vectSpace[x]);								
				//			System.out.println("     v["+i+"/"+x+"]pos["+framedpos+"|"+d+"] =T=> ["+nsMapLen[x]+"] [" + frameVect[framedpos][i]+"] ["+vectSpace[x]+"]");	
						}
						
					}
				} else {
					// increment the vector value
					vectSpace[x] = toVectorV64Inc(nsMapLen[x], frameVect[0][i], vectSpace[x]);	
				}
			}
		}
	}
	
	

	// incremental vector building.. 
	public static long toVectorV64Inc(final int length, final int vect, long curVect) {		
		if (length == 1) {
			return (long)vect;
		} else if (length == 2) {
			if (curVect == -1) {
				//System.out.println("      IS2-1["+curVect+"] ["+vect+"] ["+PRIME2+"] "+(((long)vect)*PRIME2) + " VS " + ((((long)vect)*PRIME2) & 0xffffffffL));
				return ((((long)vect)*PRIME2) & 0xffffffffL);
			}
			//System.out.println("      IS2-2["+curVect+"] ["+vect+"] "+((long)(vect) << 32) + " / " + (((long)(vect) << 32) | curVect));
			return (((long)vect) << 32) | curVect;			
		}
		if (curVect == -1) curVect = 0; 
		curVect = hashCode64(curVect, vect);
		return curVect;
	}

	public static long toVectorV64(final int vect) {
		return (long)vect;
	}
	public static long toVectorV64(final int vect1, final int vect2) {
		return (((long)vect2) << 32) | (((long)vect1*PRIME2) & 0xffffffffL);			
	}	
	// smaller one always first
	public static long toVectorV64sort(final int vect1, final int vect2) {
		if (vect1 <= vect2) return (((long)vect2) << 32) | ((vect1*PRIME2) & 0xffffffffL);	
		return (((long)vect1) << 32) | (((long)vect2*PRIME2) & 0xffffffffL);			
	}
	
	public static long toVectorV64(final int [] vect) {
		if (vect == null) return 0;
		if (vect.length == 1) {
			return (long)vect[0];
		} else if (vect.length == 2) {
			return (((long)vect[1]) << 32) | (((long)vect[0]*PRIME2) & 0xffffffffL);			
		}
		long vid = 0;
		for (int i=0;i<vect.length;i++) vid = hashCode64(vid, vect[i]);
		return vid;
	}
	public static long toVectorV64(final long [] vect) {
		if (vect == null) return 0;
		if (vect.length == 1) {
			return (long)vect[0];
		} else if (vect.length == 2) {
			return (((long)vect[1]) << 32) | (((long)vect[0]*PRIME2) & 0xffffffffL);			
		}
		long vid = 0;
		for (int i=0;i<vect.length;i++) vid = hashCode64(vid, (int)vect[i]);
		return vid;
	}

	// make vector from vect for set
	public static long toVectorV64(final int [] vectSet, final List<Integer> fset, final List<Integer> set) {
		if (vectSet == null || fset == null || set == null) return 0;
		if (vectSet.length == set.size()) {
			if (vectSet.length != fset.size()) return 0;
			return toVectorV64(vectSet);
		}
		if (set.size() == 1) {
			if (set.get(0) >= vectSet.length) return VegML.emptyVect;
			return (long)vectSet[set.get(0)];
			
		} else if (set.size() == 2) {	
			int i0 = 0, i1 = 0;
			if (set.get(0) >= vectSet.length) i0 = VegML.emptyVect;
			else i0 = vectSet[set.get(0)];
			if (set.get(1) >= vectSet.length) i1 = VegML.emptyVect;
			else i1 = vectSet[set.get(1)];	
			return (((long)i1) << 32) | ((i0*PRIME2) & 0xffffffffL);
		}	

		// do it from full set: offsets align from 0
		long vid = 0;
		for (int i=0;i<set.size();i++) {
			int p = set.get(i);	
			if (p >= vectSet.length) vid = hashCode64(vid, VegML.emptyVect);
			else vid = hashCode64(vid, vectSet[p]);
		}			
		return vid;
	}

	
	// 32 bit hashcode
	private static int hashCode32(final String string) {
		int result = PRIME_START;
		final int len = string.length();
		for (int i = 0; i < len; i++) {
			result = (PRIME*result) + string.charAt(i);
		}
		return result;
	}
	private static int hashCode32(int lastResult, final String string) {
		final int len = string.length();
		for (int i = 0; i < len; i++) {
			lastResult = (PRIME*lastResult) + string.charAt(i);
		}
		return lastResult;
	}
	private static int hashCode32(final char c) {
		return (PRIME*PRIME_START) + c;
	}
	private static int hashCode32(final int lastResult, final char c) {
		return (PRIME*lastResult) + c;
	}

	// 64 bit hashcode
	private static long hashCode64(final String string) {
		long result = PRIME_BASE; // prime
		final int len = string.length();
		for (int i = 0; i < len; i++) {
			result = (PRIME*result) + string.charAt(i);
		}
		return result;
	}
	private static long hashCode64(long cur, int num) {
		if (cur == 0) cur = PRIME_BASE; // prime
		if (num < 0) {
			cur = (PRIME*cur) + 45; // '-'
			num = (-num);
		} else {
		//	cur = (PRIME*cur) + 43; // '+'
		}
		//System.out.println("     hc["+cur+"] ["+num+"] ");
		while (num > 0) {
			//System.out.println("      hc2["+((PRIME*cur) + ((num % 10)+48))+"] ["+num+"] "+(PRIME*cur)+" + ("+((num % 10)+48)+")");
		    cur = (PRIME*cur) + ((num % 10)+48); // char values (no need)
		    num = num / 10;
			//System.out.println("      hc2["+cur+"] ["+num+"] ");
		}
		return cur;
	}

	
	//
	// make values into int list for compare
	//	
	public static <T> List<Long []> makeVectListsGenD(VectorToVid vtov, final List<T []> valueSet) {
		List<Long []> outList = new ArrayList<>();
		for (int xi=0;xi<valueSet.size();xi++) {
			T [] o = valueSet.get(xi);
			Long [] v = new Long[o.length];
			if (vtov != null) {
				for (int i=0;i<o.length;i++) v[i] = (long)vtov.toVectGen(o[i]);				
			} else {
				for (int i=0;i<o.length;i++) v[i] = (long)VectorToVid.toVectorGen(o[i]);				
			}
			outList.add(v);
		}
		return outList;
	}
		
	// list from array
	public static <T> List<Long> makeVectListsGen(VectorToVid vtov, final List<T> valueSet) {
		List<Long> outList = new ArrayList<Long>(valueSet.size());
		if (vtov != null) {
			for (int xi=0;xi<valueSet.size();xi++) {
				outList.add((long)vtov.toVectGen(valueSet.get(xi)));
			}
		} else {
			for (int xi=0;xi<valueSet.size();xi++) {
				outList.add((long)VectorToVid.toVectorGen(valueSet.get(xi)));
			}			
		}
		return outList;
	}
	// list from array
	public static <T> List<Long []> makeVectListsGenToD(VectorToVid vtov, final List<T> valueSet) {
		List<Long []> outList = new ArrayList<>(valueSet.size());
		if (vtov != null) {
			for (int xi=0;xi<valueSet.size();xi++) {
				Long [] v = new Long[1];
				v[0] = (long)vtov.toVectGen(valueSet.get(xi));
				outList.add(v);
			}
		} else {
			for (int xi=0;xi<valueSet.size();xi++) {
				Long [] v = new Long[1];
				v[0] = (long)VectorToVid.toVectorGen(valueSet.get(xi));
				outList.add(v);
			}			
		}
		return outList;
	}
	
	//
	// make values into int list for compare
	//	
	public static <T> Long [] makeVectListsGenD(VectorToVid vtov, final T [] valueSet) {
		Long [] v = new Long[valueSet.length];
		if (vtov != null) {
			for (int i=0;i<valueSet.length;i++) v[i] = (long)vtov.toVectGen(valueSet[i]);				
		} else {
			for (int i=0;i<valueSet.length;i++) v[i] = (long)VectorToVid.toVectorGen(valueSet[i]);				
		}		
		return v;
	}
	
}
