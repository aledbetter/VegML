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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vegml.Data.VectorToVid;
import vegml.VDataPlane.NSVectMap;

/*
 * NOTE:
 *  1) VectSet should be with repect to a dimension
 *  2) compression of chains will make this rational
 *  3) optimize has a bug or 2
 */

class MLVectSetMap implements java.io.Serializable {
	private static final long serialVersionUID = 9137955073439245716L;
    static enum Direction {Forward, Backward};

	private List<int []> vsList;

	public MLVectSetMap() {
		vsList = new ArrayList<>();
	}
	

	public int add(int [] vectSet) {
		/*
		// check if same as last...
		if (vsList.size() > 0) {
			int [] vectSetLast = vsList.get(vsList.size()-1);
			if (compareToVS(vectSet, vectSetLast) == 0) {
				System.out.println(" SAME["+vsList.size()+"] [" + NumberSets.setToString(vectSet) + "] != ["+NumberSets.setToString(vectSetLast)+"]");
				return vsList.size()-1;
			}
		}*/

		vsList.add(vectSet);
		return vsList.size();
	}
	
	public int [] get(List<Integer> fset, List<Integer> set, int vsid) {		
		int [] vb = getBase(vsid);
		if (vb == null) return null;
		if (set == null) return vb;
		// if vb == size of setNumber send it
		if (set.size() == vb.length) return vb;
		// must create..
		return VegML.makeSubVector(vb, fset, set);
	}
	
	// get what ever is here
	public int [] getBase(int vsid) {
		if (vsid > vsList.size() || vsid <= 0) return null;
		return vsList.get(vsid-1);
	}

	public int getCount() {
		return vsList.size();
	}
	
	//
	// update for windowSize
	// NOTE: this will break if they cross over multiple dataplans that are not all updating
	// udpates by 1 only
	public void updateWindowSize(VegML vML, boolean addAfter) {
		if (this.getCount() < 1 || addAfter) return;		
		for (int [] set:vsList) {
			for (int i=0;i<set.length;i++) set[i]++;
		}
	}

	
	//
	// Optimize this data: should be done after training (when its smashed)
	//
	public void optimize(VegML vML) {
		if (this.getCount() < 1) return;
		
		System.out.print("   START Optimize VectSets["+vsList.size()+"] .");
		// get ordered list
		List<orderVS> ol = getOrderedVSList();
		
		// check for duplicates
		int dupCnt = 0;
		orderVS lasto = null;
		int nullCnt = 0;
    	for (int i=1;i<ol.size();i++) {
    		orderVS o = ol.get(i);
    		if (o.vectSect == null) nullCnt++;
    		if (lasto != null && compareToVS(o.vectSect, lasto.vectSect) == 0) {
    			o.vectSect = null; // mark it
    			dupCnt++;
    		} else {
    			lasto = o;
    		}
    	}
		
		// re-order base from ol: no duplicates
    	vsList.clear();
       	for (int i=0;i<ol.size();i++) {
    		orderVS o = ol.get(i);
			if (o.vectSect == null) continue; // nope it
    		vsList.add(o.vectSect);
       	}
		//System.out.println(" SetList updated to["+vsList.size()+"]["+ol.size()+"] dups["+dupCnt+"] null["+nullCnt+"]");

		// must move all vsids and vssids
		int mvDupCnt = 0, mvCnt = 0, offset = 0;
		lasto = null;
    	for (int i=0;i<ol.size();i++) {
    		orderVS o = ol.get(i);
    		int newVsid = i-offset; // account for the removed dupliate entries in vsList
    		if (o.vectSect == null) {
    			// duplicate
        		if (moveVsid(vML, o.order, newVsid)) mvDupCnt++;
        		offset++;
        		continue;
    		}
    		// move the Accum AND vssid
    		if (moveVsid(vML, o.order, newVsid)) mvCnt++;
    		lasto = o;
    	}
		System.out.println(".. COMPLETE moved["+mvCnt+"] dup["+mvDupCnt+"] VectSets["+vsList.size()+"]");
			
		// compress the vectors
		// generate chain list, add compression bit array, replace chains with chain list links
		// FIXME
		// TODO: if added must also add a decompress version of get()
	}
	
	
	//
	// Move a vsid to newVsid everywhere
	// NOTE: must move before altering vsid resolution in table (or change this code)
	//
	public boolean moveVsid(VegML vML, int vsid, int newVsid) {	
		if (vsid == newVsid) return false;

		// find all accum and update them

    	// GET ACCUMES and go from there
		for (int dp=0;dp<vML.getDataPlaneCount();dp++) {
			VDataPlane dpix = vML.getDataPlane(dp);
			List<Integer> fset = dpix.getCfgNSFull();
			
	    	for (int i=0;i<dpix.getNSCount();i++) {
	    		MLNumberSetHash hm = dpix.getNSHash(i);
	    		// make the vectorSet
	    		List<Integer> set = dpix.getNS(i);
	    		int [] vs = this.get(fset, set, vsid);
	    		long vector = VectorToVid.toVectorV64(vs);
	  
	    		NSVectMap vm = hm.getVectorMapVector(vector);
				if (vm != null) {
					if (vm.vsid == vsid) vm.vsid = newVsid;
					vector = vm.vectorCode;
				} 
				Accum ac = hm.get(vector);
				if (ac != null) {
					if (ac.getVectSetId() == vsid) {
					//	System.out.println("   M_MV_ac["+i+"]("+vs.length+") -> ["+VegML.setToString(vs)+"] (" +isSVSID(ac.getVectSetId())+")");
	    				ac.setVectSetId(newVsid);
		
					} else {
				//		System.out.println("   __MV_ac["+i+"]("+vs.length+") -> ["+VegML.setToString(vs)+"] ("+isSVSID(ac.getVectSetId())+")  "+vsid+" / "+newVsid+" != " +ac.getVectSetId());					
					}
					
				} else {
					//System.out.println("    ____ac["+i+"]n["+newVsid+"]("+vs.length+") -> ["+VegML.setToString(vs)+"] ");
				}					
	    	}
	    }
		return true;
	}
	
	
	class orderVS implements java.io.Serializable {
		private static final long serialVersionUID = 5607049276730494704L;
		int order;
		int vectSect[];
	}
	
	//
	// get a list of orderVS sorted by VS
	//
	List<orderVS> getOrderedVSList() {
		if (this.getCount() < 1) return null;
		
		List<orderVS> ol = new ArrayList<>();
    	for (int i=0;i<vsList.size();i++) {
    		orderVS o = new orderVS();
    		o.order = i;
    		o.vectSect = vsList.get(i);
    		ol.add(o);
    	}
    	Collections.sort(ol, vVectSetOrderSort);
    	return ol;
	}
	
	
	//
	// Get vectorSets that contain the vectorSequenceFrame (masking for 0's)
	// Sort is Forward: end-1 to 0
	// @param vectorSeqenceFrame frame size vectorSet to match in returned set
	//
	public List<int []> getSubsetsNext(int [] vectorSeqenceFrame) {
		if (this.getCount() < 1) return null;
		// get list sorted by second to last
		return getVSByPositionList(1, Direction.Forward, vectorSeqenceFrame);
	}
	//
	// Get vectorSets that contain the vectorSequenceFrame (masking for 0's)
	// Sort is Backward: position 1 to end
	// @param vectorSeqenceFrame frame size vectorSet to match in returned set
	//
	public List<int []> getSubsetsLast(int [] vectorSeqenceFrame) {
		if (this.getCount() < 1) return null;
		// get list sorted by second
		return getVSByPositionList(1, Direction.Backward, vectorSeqenceFrame);
	}
	

	//
	// get list ordered by a specific offset from start or end
	// @param Forward  from position to 0 sorted
	// @param Backward  from position to end sorted
	// @param valueFilter optional vectorSet for isSetContains() to limit the list that is allocated, sorted and returned
	List<int []> getVSByPositionList(int position, Direction direction) {
		return getVSByPositionList(position, direction, null);
	}
	List<int []> getVSByPositionList(int position, Direction direction, int [] valueFilter) {
		if (this.getCount() < 1) return null;
		
		List<int []> ol = new ArrayList<>();
    	for (int i=0;i<vsList.size();i++) {
    		int [] vs = vsList.get(i);
    		if (valueFilter != null) {
	    		if (MLNumberSetUtil.isSetContains(vs, valueFilter, true)) {
	    			ol.add(vs);
	    		}
    		} else {
    			ol.add(vs);
    		}   		
    	}

    	Collections.sort(ol, new Comparator<int []>() {
            @Override
            public int compare(int [] lvs, int [] rvs) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
            	return compareToVS(lvs, rvs, position, direction);   
            }
        });
		//for (int i=0;i<ol.size();i++) System.out.println(" ["+i+"]: " +VegML.setToString(ol.get(i)));	
    	return ol;
	}
	
	//
	// Get vectorSets that has the vid @ position
	// @param vectorSeqenceFrame frame size vectorSet to match in returned set
	//
	public List<int []> getSubsetsList(int vid, int position) {
		if (this.getCount() < 1) return null;
		// sort first by position row might be faster..
		List<int []> ol = new ArrayList<>();
    	for (int i=0;i<vsList.size();i++) {
    		int [] vs = vsList.get(i);
    		if (vs.length > position &&  vs[position] == vid) {
    			ol.add(vs);
    		}   		
    	}
    	return ol;
	}

	//
	// Get vectorSets that contain the vectorSequenceFrame @ position
	// @param vectorSeqenceFrame frame size vectorSet to match in returned set
	//
	public List<int []> getSubsetsListContains(int [] vectorSeqenceFrame, int position) {
		if (this.getCount() < 1) return null;
		// sort first by position row might be faster..
		List<int []> ol = new ArrayList<>();
    	for (int i=0;i<vsList.size();i++) {
    		int [] vs = vsList.get(i);
    		if (vs.length <= position) continue;
    		
        	for (int xi=0;xi<vectorSeqenceFrame.length;xi++) {
        		if (vs[position] == vectorSeqenceFrame[xi]) {
        			if (MLNumberSetUtil.containsVectSet(vs, vectorSeqenceFrame)) {
            			ol.add(vs);
            			break;
            		} 
        		}
        	}
    	}
    	return ol;
	}
	
	
	// sort the vectorSets
	static final Comparator<int []> vVectSetSort = new Comparator<int []>() {
        @Override
        public int compare(int [] lvs, int [] rvs) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
        	return compareToVS(lvs, rvs);   
        }
    };
	// sort the vectorSets
	static final Comparator<orderVS> vVectSetOrderSort = new Comparator<orderVS>() {
        @Override
        public int compare(orderVS lvs, orderVS rvs) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
        	return compareToVS(lvs.vectSect, rvs.vectSect);   
        }
    };
    
    public static int compareToVS(int [] lvs, int [] rvs) {	
    	if (lvs == null && rvs != null) return -1;
    	if (lvs != null && rvs == null) return 1;    	
    	if (lvs == null && rvs == null) return 0;    	
    	if (lvs.length < rvs.length) return 1;
    	if (lvs.length > rvs.length) return -1;
    	for (int i=0;i<lvs.length;i++) {
    		if (lvs[i] < rvs[i]) return 1;
    		if (lvs[i] > rvs[i]) return -1;
    	}
    	return 0;
    }
    
    public static int compareToVS(int [] lvs, int [] rvs, int position, Direction direction) {	
    	if (lvs.length < position) return -1;
    	if (rvs.length < position) return 1;
    	
    	if (direction == Direction.Backward) {
    		//front to back
	    	for (int i=position;i<lvs.length;i++) {
	        	if (rvs.length <= i) return -1;
	    		if (lvs[i] < rvs[i]) return 1;
	    		if (lvs[i] > rvs[i]) return -1;
	    	}
    	} else {
    		// back to front
	    	for (int i=position;i<lvs.length;i++) {
	    		int rl = (rvs.length-1)-i;
	        	if (rl < 0) return -1;
	    		if (lvs[(lvs.length-1)-i] < rvs[rl]) return 1;
	    		if (lvs[(lvs.length-1)-i] > rvs[rl]) return -1;
	    	}
    	}
    	if (lvs.length < rvs.length) return 1;
    	if (lvs.length > rvs.length) return -1;
    	return 0;
    }
    
    //
    // Clear the strings
    //
	public void clear() {
		vsList = new ArrayList<>();
	}
	
	//
	// print some details
	//
	public void print() {
		System.out.println("VectSetMap  VectSet["+vsList.size()+"] ");		

		int minSize = 100, maxSize = 0;
		int [] szCount = new int[50];
    	for (int i=0;i<vsList.size();i++) {
    		int [] vs = vsList.get(i);
    		if (vs.length < minSize) minSize = vs.length;
    		if (vs.length > maxSize) maxSize = vs.length;
    		szCount[vs.length]++;
    	}
		System.out.println("   VectSet length Min["+minSize+"] Max["+maxSize+"]");		
		for (int i=1;i<=maxSize;i++) System.out.println("     Length["+i+"] "+szCount[i]+"");			
	}
	
	//
	// diff 2 VectSetMaps
	//
	public int diff(VegML vML, VegML xvML, MLVectSetMap xvsm) {
		int cnt = 0;
		// check each VectSet
    	for (int i=0;i<vsList.size();i++) {
    		int [] vs = vsList.get(i);
    		boolean found = false;
        	for (int ii=0;ii<xvsm.vsList.size();ii++) {
        		int [] xvs = xvsm.vsList.get(ii);
        		if (compareToVS(vs, xvs) == 0) {
        			found = true;
        			break;
        		}
        	}	
        	if (!found) {
        		cnt++;
				System.out.println("    DIFF[setmap]["+i+"] VectSet not found: " + MLNumberSetUtil.setToString(vs));		
        	}
    	}
				
		return cnt;
	}
	

}
