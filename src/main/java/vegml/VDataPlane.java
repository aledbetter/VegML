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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import vegml.VegML.AccumType;
import vegml.VegML.DPState;
import vegml.VegML.NSWeightBase;
import vegml.VegML.NumberSetType;
import vegml.VegML.PredictionType;
import vegml.VegML.ProbMethod;
import vegml.ValProb;
import vegml.VegML.VegPCalc;
import vegml.Data.VDataSet;
import vegml.Data.VDataSetDescriptor;
import vegml.Data.VectorToVid;
import vegml.Data.VDataSetDescriptor.DSDataType;
import vegml.Data.VDataSetDescriptor.DSInputSetType;
import vegml.Data.VDataSets;
import vegml.OptimizerMerge.MergeMap;
import vegml.VegTest.testModAcValue;
import vegml.VegTest.TestMod;
import vegml.VegTest.TestModSet;


public class VDataPlane implements java.io.Serializable {
	private static final long serialVersionUID = -8766041306013925677L;

	
	static final double DEFAULT_PROB_WEIGHT = 0.9; // 90% of min weight
	static final double WIN_MARGIN_PERCENT = 0.1; // for AverageRecall
	
	
	private transient VegML veg; // ISSUE: backlink breaks kryo on save
	
	private String tag;
	private String dtag;
		
	// data plane state
	private DPState state = DPState.Default;
	
	private Accum defAccum = null; 						// probability full set accumulator
	
	// probability & Per PredictionType
	private double prob = 0;
	private double [] probPT = new double[VegML.getPredictionTypeCountMax()+1];

	
	// stats
	private Accum recallAnswerAccum = null; 			// answer accumulator
	private int recallRecallCount = 0;
	private int recallRecallCrtCount = 0;
	private int recallPredictCount = 0;
	private int recallPredictCrtCount = 0;
	private int recallPredictRelateCount = 0;
	private int recallPredictRelateCrtCount = 0;
	private int recallRecallPredictCount = 0;
	private int recallRecallPredictCrtCount = 0;
	private int recallDefaultCount = 0;
	private int recallDefaultCrtCount = 0;

	private Accum predictAnswerAccum = null; 			// answer accumulator
	private int predictRecallCount = 0;
	private int predictRecallCrtCount = 0;
	private int predictPredictCount = 0;
	private int predictPredictCrtCount = 0;
	private int predictPredictRelateCount = 0;
	private int predictPredictRelateCrtCount = 0;
	private int predictRecallPredictCount = 0;
	private int predictRecallPredictCrtCount = 0;
	private int predictDefaultCount = 0;
	private int predictDefaultCrtCount = 0;
	private int predictUnknownCount = 0;
	private int predictUnknownCrtCount = 0;
	
	
	// space for retained instance variables / config
	private HashMap<String, Object> scratchPad = null;
	
	// Strings mapping
	private int strValueMapID;	// string map ID
	private int strDimensionMapID;	// string map ID

	// Data and tag definition
	private VDataSetDescriptor dataDef = null;	
	
	private AccumType accumulatorType = AccumType.Default;	

	private ProbMethod probMethod = ProbMethod.Default;
	private VegPCalc VegPCalculator;
	private VegCallOut VegCallOut = null;
	private Object VegCallOutArg = null;
	private VegFramer frameMaker = null;
	private Object frameMakerArg = null;
	private String framerName = null;
	private boolean frameReverse = false;
	
	private int window;			// in tokens for learning window (symmetric) 3/5/7/9...
	private int before, after;	// reference for framing
	private int region;			// numberset region size
	
	private boolean defaultNoFocus = false; // FIXME drop this... 
	private boolean identityOnly = true;
	private boolean noEmptyElements = true;
	private boolean noEmptyElementExcept = false;
	private int mappedExceptVectNum = -1;
	private boolean saveChildVids = false;
	
	// Number Sets
	private HashMap<Integer, MLNumberSetHash> nsHashHash = null; // make a hash so sparse and arbitrary sets are possible	
	private List<List<Integer>> numberSets = null;
	private List<List<List<Integer []>>> numberSetsTier = null;
	private int nsCount = 0;
	private double [] nsWeights = null;
	private double minNsWeight = 0;
	private int [] nsTypes = null;
	private NumberSetType nsBaseType;
	private NSWeightBase nsBase;
	
	private int nsFullNumber;
	private int nsIdentityNumber = 0;
	private int nsForceFullNumber = -1;
	private int nsForceIdentityNumber = -1;
	private boolean nsLocked = false;

	// numberSet vector mappings
	private boolean [][][] nsMapVectorPosition = null;
	private int [] nsMapVectorLength = null; 	// map of vector lengths: length -1 if turned off, 
	private int[][] nsMapToVector = null; 
	private int nsVectNumIdentity = -1;
	private int nsVectNumFull = -1;
	// numberSet child vector mappings
	private boolean [][][] nsChildMapVectorPosition = null;
	private int [] nsChildMapVectorLength = null; 	// map of vector lengths: length -1 if turned off, 

	// Solid Model data
	private double [][] probabilitySets = null;
	private int [][] valueSets = null;
	private long [][] valueSetsL = null;
	private int [][] groupSet = null;
	
	private HashMap<Long, Integer> trainFilter = null;

	private boolean modeBaseLineBoolean = false;
	private VectorToVid vtov = null;
	
	
	/**
	 * Vector to vector map, used for entangled DataPlanes
	 */
	public static class NSVectMap implements java.io.Serializable {
		private static final long serialVersionUID = -6423901407154105208L;
		long vectorCode;
		int count;
		int vsid;
		NSVectMap copy() {
			NSVectMap v = new NSVectMap();
			v.vectorCode = vectorCode;
			v.count = count;
			v.vsid = vsid;
			return v;
		}
	}
	
	//
	//
	//
	VDataPlane() {
		veg = null;
	}
	VDataPlane(VegML vML, String dtag, String tag, int window, NumberSetType setType) {
		this(vML, dtag, tag, window, window/2, setType);
	}
	VDataPlane(VegML vML, String dtag, String tag, int window, int before, NumberSetType setType) {
		setTag(tag);
		setDimensionTag(dtag);
		
		prob = 0;
		Arrays.fill(probPT, 0);

		this.state = DPState.Default;
		this.nsBase = NSWeightBase.Natural;
		this.modeBaseLineBoolean = false;
		this.vtov = new VectorToVid();
		this.region = -1;
		
		this.defaultNoFocus = false;
		this.identityOnly = true;
		this.saveChildVids = false;
		this.noEmptyElements = true;
		this.noEmptyElementExcept = false;
		this.mappedExceptVectNum = -1;
		this.nsForceFullNumber = -1;
		this.nsForceIdentityNumber = -1;
		 
		this.defAccum = new AccumIntHm();
		this.defAccum.setVectorCode(1);
		
		this.recallAnswerAccum = new AccumIntHmCrt();
		this.recallAnswerAccum.setVectorCode(1);
		this.predictAnswerAccum = new AccumIntHmCrt();
		this.predictAnswerAccum.setVectorCode(1);
		
		// base definition
		this.dataDef = new VDataSetDescriptor(DSInputSetType.Unique, DSDataType.String, "default", true);		
		this.veg = vML;
		
		this.window = window;		
		if (before < 0) this.before = window/2;
		else this.before = before;
		
		this.after = (this.window-this.before)-1;
		this.nsFullNumber = -1;
		
		this.nsBaseType = setType;
		this.numberSets = makeNumberSets(window);
		
		if (this.nsBaseType == NumberSetType.PowerSet) {
			this.nsFullNumber = numberSets.size()-1;		
		} else if (this.nsBaseType == NumberSetType.SequenceLeftId || this.nsBaseType == NumberSetType.SequenceRightId || this.nsBaseType == NumberSetType.SequenceEdgeId) {
			// last is focus
			this.before = this.window-1;
			this.after = (this.window-this.before)-1;
		}
		this.nsCount = numberSets.size();
		
		this.nsHashHash = new HashMap<>();	
		for (int i=0;i<numberSets.size();i++) {
			MLNumberSetHash nsh = new MLNumberSetHash(getBaseHashSize(), i, this.numberSets.get(i), null);
			this.nsHashHash.put(i, nsh);
		}
		this.strValueMapID = -1;
		this.strDimensionMapID = -1;
		strValueMapID = vML.vectStrMap.regMap(dtag, tag);
		strDimensionMapID = vML.vectStrMap.regMap(dtag);
		
		probMethod = ProbMethod.Default;
		VegPCalculator = vML.getPCalcProbabilityNS();
		
		updateCfgNS();	
		// set default framer
		this.setCfgFramer("token", vML.getCfgFramerDefault(), null, false);		
	}
	

	/**
	 * get the dataplane tag
	 * @return
	 */
	public String getTag() {
		return tag;
	}
	
	/**
	 * set the dataplane tag
	 * @param tag
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	/**
	 * get the dimension tag
	 * @return
	 */
	public String getDimensionTag() {
		return dtag;
	}
	
	/**
	 * set the dimension tag
	 * @param dtag
	 */
	public void setDimensionTag(String dtag) {
		this.dtag = dtag;
	}
	
	/**
	 * get the String map ID for this dataplan's value to valueIds
	 * @return
	 */
	public int getStrMapID() {
		return strValueMapID;
	}
	
	/**
	 * get the String map ID for this dataplan's dimension to valueIds
	 * @return
	 */
	public int getStrDimensionMapID() {
		return this.strDimensionMapID;
	}
	
	/**
	 * Set the String map ID for this dataplan's dimension to valueIds 
	 * Use this if it is shared with another dataplane
	 * @param mapId
	 */
	public void setStrDimensionMapID(int mapId) {
		this.strDimensionMapID = mapId;
	}
	
	/**
	 * set this dataplane to save dimension strings to the string map
	 * @param save
	 */
	public void setCfgSaveStrings(boolean save) {
		if (save) {
			if (strDimensionMapID == -1) strDimensionMapID = getVegML().vectStrMap.regMap(dtag);
		//	if (strValueMapID == -1) strValueMapID = getVegML().vectStrMap.regMap(dmx.getTag(), tag);
		} else {
		//	strValueMapID = -1;
			strDimensionMapID = -1;
		}
	}
	
	/**
	 * get VegML instance for this dataplane
	 * @return
	 */
	public VegML getVegML() {
		return veg;
	}
	
	/**
	 * set the VegML instance for this dataplane
	 * @param veg
	 */
	void setVegML(VegML veg) {
		this.veg = veg;
	}	
	
	/**
	 * get VectorToVid method object used to for valueId conversion
	 * @return
	 */
	public VectorToVid getCfgVToV() {
		if (vtov == null) return new VectorToVid();
		return vtov;
	}
	
	/**
	 * set VectorToVid method object used to for valueId conversion
	 * @param vtov
	 */
	void setCfgVToV(VectorToVid vtov) {
		this.vtov = vtov;
	}

	/**
	 * Set the region size, this is the mximum number of positions in a number set
	 * this is used to cap the complexity of the relationships in a dataplan and reduce the overall size
	 * 
	 * @param region number of regions, default -1
	 */
	public void setCfgRegion(final int region) {
		if (region >= this.window) this.region = -1;
		else this.region = region;
	}
	
	/**
	 * get the region size, this is the maximum number of positions in a number set
	 * 
	 * @return
	 */
	public int getCfgRegion() {
		return region;
	}	
	//
	// add to the training restrictions
	// 1 = identity only value
	// ??
	public void addTrainingFilter(long value, int fltType) {
		if (trainFilter == null) trainFilter = new HashMap<>();
		trainFilter.put(value, fltType);
	}
	public int getTrainingFilter(long value) {
		if (trainFilter == null) return -1;
		Integer i = trainFilter.get(value);
		if (i != null) return i;
		return -1;
	}
	public int getTrainingFilterSize() {
		if (trainFilter == null) return 0;
		return trainFilter.keySet().size();
	}
	
	/**
	 * Check if the state requested has been completed by this dataplane
	 * @param state state to check completion
	 * @return true if completed
	 */
	public boolean isStateComplete(DPState state) {
		if (state == this.state) return true;
		if (this.state == DPState.Ready) return true;
		
		if (this.state == DPState.Tuned) {
			if (state == DPState.Ready) return false;
			return true;
		}
		if (this.state == DPState.Trained) {
			if (state == DPState.Default) return true;
		}
		return false;
	}
	
	/**
	 * Check if this is the state of this dataplane
	 * @param state state to check for
	 * @return true if match
	 */
	public boolean isState(DPState state) {
		return this.state == state;
	}
	
	/**
	 * Set the state of this dataplane
	 * @param state
	 */
	public void setState(DPState state) {
		this.state = state;
	}
	
	/**
	 * Get teh current state of this dataplane
	 * @return
	 */
	public DPState getState() {
		return this.state;
	}
	
	
	//
	// Save child Vids to vsid instease of vectSet id
	//
	public void setCfgSaveChildVids(boolean save) {
		if (this.saveChildVids != save) {
			this.saveChildVids = save;
			if (nsMapVectorPosition != null) this.updateCfgNS();
		}
	}
	public boolean isCfgSaveChildVids() {
		return saveChildVids;
	}
	/**
	 * Get direct access to the dataplans number set list
	 * If this are modified all bets are off
	 * @return
	 */
	public List<List<Integer>> getNSs() {
		return numberSets;
	}
	
	/**
	 * Get direct access to the dataplans tiered number set list
	 * If this are modified all bets are off
	 * @return
	 */
	public List<List<List<Integer []>>> getNSsTier() {
		return numberSetsTier;
	}

	/**
	 * Drop all data and add all the numberSets again
	 */
	public void resetNSAndData() {
		this.clearCfgNS();
		
		this.numberSets = makeNumberSets(window);
		
		if (this.nsBaseType == NumberSetType.PowerSet) {
			this.nsFullNumber = numberSets.size()-1;		
		} else if (this.nsBaseType == NumberSetType.SequenceLeftId || this.nsBaseType == NumberSetType.SequenceRightId || this.nsBaseType == NumberSetType.SequenceEdgeId) {
			// last is focus
			this.before = this.window-1;
			this.after = (this.window-this.before)-1;
		}
		this.nsCount = numberSets.size();
		
		this.nsHashHash = new HashMap<>();	
		for (int i=0;i<numberSets.size();i++) {
			MLNumberSetHash nsh = new MLNumberSetHash(getBaseHashSize(), i, this.numberSets.get(i), null);
			this.nsHashHash.put(i, nsh);
		}

		this.updateCfgNS();
		//System.out.println(" NS added["+window+"] " + this.numberSets.size());
		//this.print(true);
		return;
	}
	

	/**
	 * destroy all number sets
	 * Don't use this
	 */
	public void clearCfgNS() {
		this.nsHashHash.clear();
		numberSets.clear();
		nsWeights = new double[0];
		nsCount = 0;
		nsFullNumber = -1;
		nsIdentityNumber = -1;
		nsVectNumIdentity = -1;
		nsVectNumFull = -1;
	}
	
	/**
	 *  clear all accums from all number sets
	 */
	public void clearNSData() {
		for (int i=0;i<nsCount;i++) {
			MLNumberSetHash nsh = this.getNSHash(i);
			if (nsh == null) continue;
			nsh.clear();
		}
	}
	
	/**
	 * Clear all number set data for specified numberset
	 * 
	 * @param setNumber number set to clear data from
	 */
	public void clearNSData(int setNumber) {
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return;
		nsh.clear();		
	}
	
	
	//
	// extend with arbitrary number set set
	// Identity->X, Fullset->X, X, allSets->X, allPositions(X), valueFucos(X)
	//
	// @param tag  used to identify what data to put in it and its vector
	// @return setNumber of added set
	int addCfgNSDefined(List<Integer> set, double weight) {
		if (set.size() > window || set.get(set.size()-1) >= window) {
			System.out.println("ERROR: numberset to large window["+window+"] ("+set.get(set.size()-1)+") "+MLNumberSetUtil.setToStringPosition(set, window, this.getCfgFrameFocus()));
			return -1;
		}
		
		//System.out.println("ADD["+set.size()+"]: " + NumberSets.setToStringPosition(set, window, this.getCfgFrameFocus()));
		// check for duplicate
		for (int i=0;i<this.numberSets.size();i++) {
			if (MLNumberSetUtil.compareSet(numberSets.get(i), set)) return -1;
		}
		
		
		// if no tag USE standard frame data
		// if tag[n] then set.get(n) gets data from tagged in input
		int setNumber = numberSets.size(); // always greater than base set
		if (this.getNSCount() > setNumber) setNumber = this.getNSCount();
		
		MLNumberSetHash nsh = new MLNumberSetHash(getBaseHashSize(), setNumber, set, null);
		this.nsHashHash.put(setNumber, nsh);
		this.nsWeights = Arrays.copyOf(nsWeights, nsCount+1);
		nsWeights[nsCount] = weight;
		nsCount++;
		// always?
		numberSets.add(set);
		return setNumber;
	}
	
	/**
	 * Find the number set index/id if it is present
	 * @param set number set positions list
	 * @return the number set index or -1
	 */
	public int findCfgNS(List<Integer> set) {
		if (set == null) return -1;
		for (int i=0;i<this.numberSets.size();i++) {
			if (MLNumberSetUtil.compareSet(numberSets.get(i), set)) return i;
		}		
		return -1;
	}

	/**
	 * Remove all number sets that are marked as turned off
	 * NOTE: this will renumber all numberSets
	 * @return
	 */
	public boolean removeCfgNSTurnedOff() {
		// make list to keep
		List<List<Integer>> setsKeep = new ArrayList<>();
		List<Double> setsKeepW = new ArrayList<>();
		List<MLNumberSetHash> setsKeepNsh = new ArrayList<>();
		int cnt = 0;
		for (int i=0;i<nsCount;i++) {
			if (isCfgNSTurnedOff(i)) {
				cnt++;
				continue;
			}
			List<Integer> ns = this.numberSets.get(i);
			setsKeep.add(ns);
			setsKeepW.add(getCfgNSWeight(i));
			setsKeepNsh.add(this.getNSHash(i));
		}
		if (cnt == 0) return false;
		// remove stuff
		this.nsCount = 0;
		this.nsHashHash.clear();
		this.nsWeights = null;
		this.numberSets.clear();
		this.nsFullNumber = -1;
		this.nsIdentityNumber = -1;
		this.nsVectNumIdentity = -1;
		this.nsVectNumFull = -1;
			
		// keep existing nsh
		this.nsWeights = new double[setsKeep.size()];

		for (int i=0;i<setsKeep.size();i++) {
			MLNumberSetHash nsh = setsKeepNsh.get(i);
			nsh.setSetNumber(i);
			this.nsHashHash.put(i, nsh);
			this.nsWeights[i] = setsKeepW.get(i);
			this.numberSets.add(setsKeep.get(i));
			this.nsCount++;
		}
		this.updateCfgNS();
		return true;
	}
	
	/**
	 * remove the context numberSets, those without identity in them
	 * NOTE: this will renumber all numberSets
	 */
	public void removeCfgNSContext() {
		List<Integer> delns = new ArrayList<>();
		for (int i=0;i<getNSCount();i++) {
			if (isCfgNSContext(i)) delns.add(i);
		}
		removeCfgNS(delns);
		
	}

	/**
	 * Remove specific numberSet by index
	 * NOTE: this will renumber all numberSets
	 * @param setNumber
	 * @return true if any removed
	 */
	public boolean removeCfgNS(int setNumber) {
		return removeCfgNS(null, setNumber);
	}
	
	/**
	 * Remove a list numberSet by indexs
	 * NOTE: this will renumber all numberSets
	 * @param nsList list of numberSet indexes
	 * @return true if any removed
	 */
	public boolean removeCfgNS(List<Integer> nsList) {
		return removeCfgNS(nsList, -1);
	}
	private boolean removeCfgNS(List<Integer> nsList, int setNumber) {
		// make list to keep
		List<List<Integer>> setsKeep = new ArrayList<>();
		List<Double> setsKeepW = new ArrayList<>();
		List<MLNumberSetHash> setsKeepNsh = new ArrayList<>();
		int cnt = 0;
		for (int i=0;i<nsCount;i++) {
			if (nsList != null && nsList.contains(i)) {
				cnt++;
				continue;
			} else if (i == setNumber) {
				cnt++;
				continue;
			}
			List<Integer> ns = this.numberSets.get(i);
			setsKeep.add(ns);
			setsKeepW.add(getCfgNSWeight(i));
			setsKeepNsh.add(this.getNSHash(i));
		}
		if (cnt == 0) return false;
		// remove stuff
		this.nsCount = 0;
		this.nsHashHash.clear();
		this.nsWeights = null;
		this.numberSets.clear();
		this.nsFullNumber = -1;
		this.nsIdentityNumber = -1;
		this.nsVectNumIdentity = -1;
		this.nsVectNumFull = -1;
			
		// keep existing nsh
		this.nsWeights = new double[setsKeep.size()];

		for (int i=0;i<setsKeep.size();i++) {
			MLNumberSetHash nsh = setsKeepNsh.get(i);
			nsh.setSetNumber(i);
			this.nsHashHash.put(i, nsh);
			this.nsWeights[i] = setsKeepW.get(i);
			this.numberSets.add(setsKeep.get(i));
			this.nsCount++;
		}
		this.updateCfgNS();
		return true;
	}

	
	public int addCfgNSFormatToAll(List<Integer> set, double weight) {	
		int cnt = 0;
		List<List<Integer>> setsNew = new ArrayList<>();
		List<Double> setsNewW = new ArrayList<>();
		// add self
		List<Integer> nsNew = new ArrayList<>(set);
		setsNew.add(nsNew);
		setsNewW.add(weight);
		
		for (int i=0;i<nsCount;i++) {
			List<Integer> ns = numberSets.get(i);
			nsNew = new ArrayList<>(ns);
			int add = 0;
			for (int ix=0;ix<set.size();ix++) {
				if (nsNew.contains(set.get(ix))) continue;
				nsNew.add(set.get(ix));
				add++;
			}
			if (add == 0) continue;
			Collections.sort(nsNew);
			if (MLNumberSetUtil.findSet(setsNew, nsNew) >= 0) continue;		
			setsNew.add(nsNew);
			setsNewW.add(this.nsWeights[i]+weight);
		}
		for (int i=0;i<setsNew.size();i++) {
			List<Integer> ns = setsNew.get(i);
			addCfgNSDefined(ns, setsNewW.get(i));
		}
		return cnt;
	}

	/**
	 * Get full map of all values and vectorIDs for a numberSet
	 * 
	 * @param setNumber
	 * @return hash map of vectorId/value
	 */
	public HashMap<Long, Integer> getNSValueSet(int setNumber) {
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return null;
		return nsh.getValueSet();
	}
	
	/**
	 * Check and update all numberSets for this dataplane
	 * Will re-assses full,identity numberSet indexes
	 * Will rebuild vector Maps
	 */
	public void updateCfgNS() {
		// from sets present
		nsFullNumber = nsIdentityNumber = -1;

		for (int i=0;i<nsCount;i++) {
			List<Integer> ns = numberSets.get(i);
			if (ns == null) continue;		
			
			if (ns.size() == 1 && ns.get(0) == this.getCfgFrameFocus()) {
				nsIdentityNumber = i;
			} 
			if (ns.size() == this.window) {
				nsFullNumber = i;
			} 	
		}

		if (!nsLocked) {
			updateNumberSetWeights();
		}
	    if (this.nsForceFullNumber >= 0) {
	    	nsFullNumber = nsForceFullNumber;
	    }
	    if (this.nsForceIdentityNumber >= 0) {
	    	nsIdentityNumber = nsForceIdentityNumber;
	    }
	    
	    if (this.getCfgInputDataTiers() > 1) {
	    	// make the dependent numberSet set for each NumberSet
	    	List<List<List<Integer []>>> nstList = new ArrayList<>();
			for (int i=0;i<nsCount;i++) {
				List<Integer> ns = numberSets.get(i);
				if (ns == null) continue;
				List<List<Integer []>> nsl = MLNumberSetUtil.generateDependentSubsets(ns, getCfgInputDataTiers()-1);
	//			getNSHash(i).setNumberSetTierd(nsl);
				nstList.add(nsl);
			}
			numberSetsTier = nstList;
	    } else {
	    	numberSetsTier = null;
	    }
	    makeVectorMaps();
	}

	/**
	 * Get the number set as a string representation
	 * - x X x -
	 * @param setNumber numberSet to get representation of
	 * @return
	 */
	public String getNSFormatString(int setNumber) {
		//if (setNumber < numberSets.size()) 
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return "unknown";
		int fo = getCfgFrameFocus();
		if (this.isCfgFameFocusNone()) fo = -1;
		return MLNumberSetUtil.setToStringPosition(nsh.getNS(), this.window, fo);
	}
	
	/**
	 * Get the count of numberSets with this number of positions in them
	 * @param size
	 * @return
	 */
	public int getNSCountForSize(int size) {
		int cnt = 0;
		for (int i=0;i<getNSCount();i++) {
			if (getNS(i).size() == size) cnt++;
		}
		return cnt;
	}
	
	/**
	 * Get the count of numberSets in this dataplane
	 * @return
	 */
	public int getNSCount() {
		return nsCount;
	}
	
	/**
	 * Get direct access to the numberSet positions for the full number set
	 * full numberSet contains all positions in the window/frame
	 * Do not modify
	 * @return
	 */
	public List<Integer> getCfgNSFull() {
		if (getCfgNSFullNumber() < 0) return null;
		return numberSets.get(getCfgNSFullNumber());
	}
	
	/**
	 * get the full numberSet index
	 * full numberSet contains all positions in the window/frame
	 * @return
	 */
	public int getCfgNSFullNumber() {
		return nsFullNumber;
	}
	
	/**
	 * set the full numberSet to the numberSet at index
	 * This forces the full numberSet to any numberSet, implications are unknown, good luck to you
	 * @param setNumber
	 */
	void setCfgNSFullNumber(int setNumber) {
		this.nsForceFullNumber = setNumber;
		updateCfgNS();
	}
	
	/**
	 * Get the vector map index of the full numberSet
	 * @return
	 */
	public int getCfgNSVectNumFullNumber() {
		return nsVectNumFull;
	}
	
	/**
	 * Get the identity numberSet index
	 * This is the numberSet at focus if any
	 * @return
	 */
	public int getCfgNSIdentityNumber() {
		return nsIdentityNumber;
	}	
	
	/**
	 * Set the Identity numberSet to the numberSet at index
	 * The implications of this are unknown, good luck to you.
	 * @param setNumber
	 */
	void setCfgNSIdentityNumber(int setNumber) {
		this.nsForceIdentityNumber = setNumber;
		updateCfgNS();
	}	
	
	/**
	 * Get the vector map index of the Identity numberSet
	 * @return
	 */
	public int getCfgNSVectNumIdentityNumber() {
		return nsVectNumIdentity;
	}

	
	/**
	 * true if  number set context (does not contain focus)
	 * @param setNumber numberSet to check 
	 * @return
	 */
	public boolean isCfgNSContext(int setNumber) {
		return nsTypes[setNumber] == 0;
	}
	
	/**
	 * Get the frame position of the Identity / focus
	 * @return frame postion
	 */
	public int getCfgIdentityPosition() {
		//if (getCfgNSIdentityNumber() >= 0) return getNS(getCfgNSIdentityNumber()).get(0);
		// else?
		return this.getCfgFrameFocus();
	}
	
	/**
	 * get direct access to the frame positions list of a numberSet
	 * @param setNumber the numberSet index
	 * @return numberSet list or null
	 */
	public List<Integer> getNS(int setNumber) {
		if (setNumber >= numberSets.size() || setNumber < 0) return null;
		return numberSets.get(setNumber);
	}
	
	/**
	 * get direct access to the frame positions list of a tiered numberSet
	 * @param setNumber the numberSet index
	 * @return numberSet list or null
	 */	
	public List<List<Integer []>> getNSTier(int setNumber) {
		if (numberSetsTier == null || setNumber >= numberSetsTier.size()) return null;
		return numberSetsTier.get(setNumber);
	}	
	
	/**
	 * Find the index of the numberSet with these positions
	 * @param set
	 * @return index or -1 if not found
	 */
	public int findSetNumber(List<Integer> set) {
		for (int i=0;i<numberSets.size();i++) {
			List<Integer> s = numberSets.get(i);
			if (MLNumberSetUtil.compareSet(s, set)) return i;
		}
        return -1;
	}

	
	//
	// Make the child numberSet -> IF IT IS IN THE DATAPLANE
	//
	public List<Integer> getNSChild(int setNumber) {
		// FIXME not correct for most sets: good for powerset		
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return null;
		List<Integer> set = new ArrayList<>(nsh.getNS());
		if (set.size() < 2) return null;
		// based on window and scratchpad info decide isAfter
		// FIXME for most NumberSetType.xxx need mid point and split remove
		boolean addAfter = getCfgScratchBool("posLastAddAfter");
		if (addAfter) {
			set.remove(set.size()-1);
		} else {
			set.remove(0);
		}
		return set;
	}
	public List<List<Integer []>> getNSTierChild(int setNumber) {
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return null;
		
		List<List<Integer []>> settl = numberSetsTier.get(setNumber);
		List<List<Integer []>> nsettl = new ArrayList<>();
		/*
		 * FIXME all them
			List<Integer> set = new ArrayList<>(nsh.getNS());
			if (set.size() < 2) return null;
			// based on window and scratchpad info decide isAfter
			boolean addAfter = getCfgScratchBool("posLastAddAfter");
			if (addAfter) {
				set.remove(set.size()-1);
			} else {
				set.remove(0);
			}
			*/
		return nsettl;
	}
	
	//
	// get the numberSet of the child -> IF IT IS IN THE DATAPLANE
	//
	public int getChildNs(int setNumber) {
		return this.findCfgNS(getNSChild(setNumber));
	}
	
	/**
	 * merge numberSet into another numberSet
	 * IF vector widths are not the same this will not work!
	 * @param fromNs
	 * @param toNs
	 * @return true if complete
	 */
	public boolean mergeNS(int fromNs, int toNs) {
		MLNumberSetHash fromNsh = this.getNSHash(fromNs);
		MLNumberSetHash toNsh = this.getNSHash(toNs);
		if (fromNsh == null || toNsh == null) return false;
		//System.out.println("Merge NS: " +fromNs + "|"+fromNsh.size()+" => " + toNs +"|"+toNsh.size());
		toNsh.merge(this, fromNsh, this);
		fromNsh.clear();
		return true;
	}
	
	/**
	 * Get all the values for a numberSet sorted by highest frequency
	 * 
	 * @param setNumber the numberSet to get values for
	 * @return list of ValProbs for the values
	 */
	public List<ValProb> getNSFrequencySorted(int setNumber) {
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return null;
		if (this.isSolid()) {
			int [] gidl = nsh.getListSolid();
			long [] vidl = nsh.getListSolidVid();
			List<ValProb> vpList = new ArrayList<>();
			for (int i=0;i<gidl.length;i++) {
				int g = gidl[i];
				if (g >= this.groupSet.length) break; // ?? bad thing
				//int probId = this.groupSet[gidl[i]][0];
				int valId = this.groupSet[g][1];
				int [] valList = this.valueSets[valId];
				ValProb vp = new ValProb();
				vp.count = valList.length*5; // hack
				vp.value = vidl[i];
				vp.counter = valList.length;
				vpList.add(vp);
			}
			Collections.sort(vpList, VegUtil.VpFreqSort);	
			return vpList;
		}
		return nsh.getListFrequencySorted();
	}
	
	/**
	 * sync accum totals with actual value totals - remove attenuation
	 * After: total for each accum will be the sum of the value totals
	 */
	public void syncAccumTotals() {
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = getNSHash(i);
			nsh.syncAccumTotals();
		}	
	}
	
	/**
	 * Get frequency sorted values for a numberSet
	 * @param setNumber numberSet to get accums for
	 * @return
	 */
	public List<Accum> getNSAccumSorted(int setNumber) {
		MLNumberSetHash nhs = this.getNSHash(setNumber);
		if (nhs == null) return null;
		return nhs.getListSorted();
	}
	
	/**
	 * get maximum count for this value in this numberset
	 * @param setNumber
	 * @param valueId
	 * @return
	 */
	public int getNSValueMaxCount(int setNumber, long valueId) {
		MLNumberSetHash nhs = this.getNSHash(setNumber);
		if (nhs == null) return 0;
		return nhs.getValueMaxCount(valueId);
	}

	/**
	 * get maximum count for this value in this numberset
	 * @param setNumber
	 * @param valueId
	 * @return
	 */
	public int getNSValueCount(int setNumber, long valueId) {
		MLNumberSetHash nhs = this.getNSHash(setNumber);
		if (nhs == null) return 0;
		return nhs.getValueCount(valueId);
	}
	
	/**
	 * get maximum count for any value in this numberset
	 * @param setNumber
	 * @return
	 */
	public int getNSSingleValueCountMax(int setNumber) {
		MLNumberSetHash nhs = this.getNSHash(setNumber);
		if (nhs == null) return 0;
		return nhs.getSingleValueCountMax();
	}
	
	/**
	 * Get the size / number of vectors for a numberSet
	 * @param setNumber
	 * @return
	 */
	public int getNSSize(int setNumber) {
		MLNumberSetHash nhs = this.getNSHash(setNumber);
		if (nhs == null) return 0;
		return nhs.size();
	}

	/**
	 * Get the number of vectors for a numberSet
	 * @param setNumber 
	 * @return
	 */
	public int getVectorCount(int setNumber) {
		MLNumberSetHash nsh = getNSHash(setNumber);
		if (nsh == null) return 0;
		return nsh.getVectorCount();	
	}
	
	/**
	 * Get the number of accumulators for a numberSet
	 * This will differ from the vector count if entangled
	 * @param setNumber
	 * @return
	 */
	public int getNSAccumulatorCount(int setNumber) {
		MLNumberSetHash nhs = this.getNSHash(setNumber);
		if (nhs == null) return 0;
		return nhs.size();
	}

	/**
	 * Get the vector count for this dataplane
	 * Turned Off numberSets are not counted
	 * @return
	 */
	public int getVectorCount() {
		int vCnt = 0;
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = getNSHash(i);
			if (nsh == null) continue;
			if (nsh.isTurnedOff()) continue;
			vCnt += nsh.getVectorCount();	
		}
		return vCnt;
	}
	
	/**
	 * Get the accumulator Count for this dataplane
	 * @return
	 */
	public int getAccumCount() {
		if (this.groupSet != null) return groupSet.length;
		int aCnt = 0;
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = getNSHash(i);
			if (nsh == null) continue;
			aCnt += nsh.size();	
		}
		return aCnt;
	}
	
	/**
	 * Get a string ID for this numberSet that is relevent for this VegML instance
	 * @return
	 */
	public String getIDString() {
		return this.dtag+"/"+this.tag;
	}
	
	//
	// probability of correct response
	//
	public double getProb() {
		return prob;
	}
	public void setProb(double prob) {
		this.prob = prob;
	}
	
	//
	// probability of correct response per PredictionType
	//
	public double getProb(PredictionType pType) {
		// FIXME merged types
		return probPT[pType.ordinal()];
	}
	public void setProb(PredictionType pType, double prob) {
		// FIXME merged types
		probPT[pType.ordinal()] = prob;
	}
	
	/**
	 * Get the window Size / frame Size for this dataplane
	 * @return
	 */
	public int getCfgWindowSize() {
		return window;
	}
	
	/**
	 * set the window size
	 * NOTE: this will renumber numberSets AND remove those out of frame
	 * add and remove left side more if odd number (with respect to focus)
	 * @param windowSize
	 */
	public void setCfgWindowSize(int windowSize) {
		if (window == windowSize) {
			if (this.window == 1) this.setCfgScratch("posLastAddAfter", true);
			return;
		}
		int woff = 0;
		int nbefore = this.before;
		int nafter = this.after;
		boolean isAfter = false;
		
		if (window < windowSize) {
			woff = windowSize-window;
			nbefore = before + woff/2;
			nafter = after + woff/2;
			if ((woff % 2) != 0) {
				// if currently not balanced, closer to balance
				if (before > after) {
					nafter++;   // else add to end		
					isAfter = true;
				}
				else nbefore++;  // else add to front
			}			
		} else {
			woff = window-windowSize;
			nbefore = before - woff/2;
			nafter = after - woff/2;
			if ((woff % 2) != 0) {
				// if currently not balanced, closer to balance
				if (before > after) nbefore--;   // else add to end			
				else {
					nafter++;  // else add to front	
					isAfter = true;
				}
			}
		}
		
		int off = nbefore-before; // get difference		
		//System.out.println(" WIN["+window+" > "+windowSize+"]off["+off+"] " + nbefore + " / " + nafter);				

		if (off != 0) {
			// adjust all numberSets of before changes
			List<Integer> rml = new ArrayList<>();
			for (int i=0;i<this.getNSCount();i++) {
				List<Integer> nss = numberSets.get(i);
				boolean del = false;
				for (int x=0;x<nss.size();x++) {
					int np = nss.get(x)+off;
					nss.set(x, np);
					if (np < 0 || np >= windowSize) {
						rml.add(i);
						del = true;
						break;
					}
				}
				if (del) continue;

				List<List<Integer []>> nsst = null;
				//System.out.println("   ns["+i+"]off["+off+"] " + NumberSets.setToStringPosition(nl, nwin, positionNumber));				
				//System.out.println("   ns["+i+"]off["+off+"]["+nss.size()+"] " + NumberSets.setToStringPosition(nss, windowSize, nbefore));				
				if (numberSetsTier != null) {
					nsst = numberSetsTier.get(i);
					for (int x=0;x<nsst.size();x++) {
						List<Integer []> nst = nsst.get(x);
						for (int xx=0;xx<nst.size();xx++) {
					//for (int x=0;x<nss.size();x++) nss.set(x, nss.get(x)+off);	
// FIXME
						}
					}
				}
				// update
				MLNumberSetHash nsh = getNSHash(i);
				nsh.setNumberSet(nss, nsst);
			}
		
			
			// remove those out of frame
			removeCfgNS(rml);
			this.before = nbefore;
		}
		this.after = nafter;
		this.window = windowSize;
		if (this.window == 1) isAfter = true;
		this.setCfgScratch("posLastAddAfter", isAfter);
		// update all numberSets
		this.updateCfgNS();
		// update the vectSets
		this.getVegML().vectSetMap.updateWindowSize(getVegML(), isAfter);
	}
	
	/**
	 * Increment the window size by a single token
	 * This will switch from adding start to adding end as it is called
	 * All numberSets and vectors will be updated
	 * NOTE: this will renumber numberSets AND remove those out of frame
	 * 
	 * @param isAfter add after
	 */
	public void incWindowSize(boolean isAfter) {
		int nbefore = this.before;
		int nafter = this.after;
		int windowSize = this.window+1;
		if (isAfter) {
			nafter++;
		} else {
			nbefore++;
		}
		
		int off = nbefore-before; // get difference		
		//System.out.println(" WIN["+window+" > "+windowSize+"]off["+off+"] " + nbefore + " / " + nafter);	
		boolean updateNS = false;
		if (this.getNSBaseType() == NumberSetType.PowerSet || this.getNSBaseType() == NumberSetType.Linear) {
			updateNS = true;
		}
		if (off != 0) {
			// adjust all numberSets of before changes
			List<Integer> rml = new ArrayList<>();
			for (int i=0;i<this.getNSCount();i++) {
				List<Integer> nss = numberSets.get(i);
				boolean del = false;
				for (int x=0;x<nss.size();x++) {
					int np = nss.get(x)+off;
					if (updateNS) nss.set(x, np);
					if (np < 0 || np >= windowSize) {
						rml.add(i);
						del = true;
						break;
					}
				}
				if (del) continue;

				List<List<Integer []>> nsst = null;
				//System.out.println("   ns["+i+"]off["+off+"] " + NumberSets.setToStringPosition(nl, nwin, positionNumber));				
				//System.out.println("   ns["+i+"]off["+off+"]["+nss.size()+"] " + NumberSets.setToStringPosition(nss, windowSize, nbefore));				
				if (numberSetsTier != null) {
					nsst = numberSetsTier.get(i);
					for (int x=0;x<nsst.size();x++) {
						List<Integer []> nst = nsst.get(x);
						for (int xx=0;xx<nst.size();xx++) {
					//for (int x=0;x<nss.size();x++) nss.set(x, nss.get(x)+off);	
							// FIXME
						}
					}
				}
				// update
				if (updateNS) getNSHash(i).setNumberSet(nss, nsst);
			}		
			
			// remove those out of frame
			removeCfgNS(rml);
			if (updateNS) this.before = nbefore;
		}
		if (updateNS) this.after = nafter;
		this.window = windowSize;
		// save update info
		this.setCfgScratch("posLastAddAfter", isAfter);
		this.setCfgScratch("posWindowAfter"+this.window, isAfter);
		
		// update all numberSets
		this.updateCfgNS();
		// update the vectSets
		this.getVegML().vectSetMap.updateWindowSize(getVegML(), isAfter);	
	}
	
	/**
	 * Get the count of positions before the frame focus
	 * @return
	 */
	public int getCfgBefore() {
		return before;
	}	
	
	/**
	 * Get the count of positions after the frame focus
	 * @return
	 */
	public int getCfgAfter() {
		return after;
	}
	
	/**
	 * get the frame focus for this dataPlane
	 * This is the position of the identity token and the position 
	 * for which tags are being trained / predicted
	 * @return 
	 */
	public int getCfgFrameFocus() {
		// should be -1 if none
		//if (defaultNoFocus) return -1;
		return before;
	}
	
	/**
	 * Set the frame focus for this dataPlane
	 * This is the position of the identity token and the position 
	 * for which tags are being trained / predicted
	 * @param valueFocus
	 */
	public void setCfgFrameFocus(int focus) {
		this.before = focus;
		this.after = (this.window-this.before)-1;
		//this.setCfgFrameFocusNone(true);	
		this.updateCfgNS();
	}

	/**
	 * Set this dataplane as having no focus
	 * This would be desired if the focus is not in the dataframe or 
	 * this is just a probability test independent of the frame values
	 * @param noFocus true for no focus
	 */
	public void setCfgFrameFocusNone(boolean noFocus) {
		this.defaultNoFocus = noFocus;
		this.updateCfgNS();
	}
	
	/**
	 * check if this dataplan has a focus or not
	 * @return
	 */
	public boolean isCfgFameFocusNone() {
		return defaultNoFocus;
	}	
	
	/**
	 * Set this dataplane to use Identity as a results filter
	 * if set then only values that are in the identity will be 
	 * returned for frames that contain a known identity
	 * 
	 * @param identityOnly true to turn on identity filter
	 */
	public void setCfgIdentityOnly(boolean identityOnly) {
		this.identityOnly = identityOnly;
	}
	
	/** 
	 * Check if this dataplane has identity filter set
	 * @return true if identity filter is set
	 */
	public boolean isCfgIdentityOnly() {
		return identityOnly;
	}
	
	/**
	 * Set the accumulator Type to use for this Dataplane
	 * DO NOT USE after accumulators have been added
	 * @param atype Type of accumulator
	 */
	public void setCfgAccumulatorType(AccumType atype) {
		this.accumulatorType = atype;
	}
	
	boolean [][][] getNSMapVectorPositions() {
		return nsMapVectorPosition;
	}	
	int [] getNSMapVectorLength() {
		return nsMapVectorLength;
	}	
	int getExceptVectNumber() {
		return mappedExceptVectNum;
	}
	boolean isNoEmptyElementsExcept() {
		return noEmptyElementExcept;
	}
	boolean [][][] getNSChildMapVectorPositions() {
		return this.nsChildMapVectorPosition;
	}	
	int [] getNSChildMapVectorLength() {
		return nsChildMapVectorLength;
	}	

	//
	// Get the base hash size
	//
	int getBaseHashSize() {
		return VegML.baseHash*window;
	}
	
	/**
	 * Get the base numberSet weight type
	 * 
	 * @return base numberSet type
	 */
	public NSWeightBase getCfgNSWeight() {
		return this.nsBase;
	}
	
	/**
	 * Get the base numberSet type
	 * This defines the base set of numberSets for the dataplane
	 * @return base numberSet type
	 */
	public NumberSetType getNSBaseType() {
		return this.nsBaseType;
	}
	
	/**
	 * Set the numberSet weight base type, the type will then be used to set weights on all
	 * NumberSets as they are added to the dataplane
	 * 
	 * will switch calculator to: None = probability-only, else default
	 * 
	 * @param baseType
	 */
	public void setCfgNSWeight(NSWeightBase baseType) {
		this.nsBase = baseType;
		if (baseType == NSWeightBase.None) {
			this.setCfgPCalc(getVegML().getPCalcProbabilityOnly());
		} else if (this.VegPCalculator == getVegML().getPCalcProbabilityOnly()) {
			this.setCfgPCalc(getVegML().getPCalcProbabilityNS());			
		}
		updateNumberSetWeights();
	}
		
	/**
	 * Get the numberSet weight for numberSet of index
	 * @param setNumber index of numberSet
	 * @return weight for numberSet
	 */
	public double getCfgNSWeight(int setNumber) {
		if (setNumber >= nsWeights.length) return 1;
		return nsWeights[setNumber];
	}
	
	// 
	/**
	 * Set the numberSet weight for a given numberSet
	 * NOTE: this will not be maintained if numberSets are altered, you must lock the numberSets
	 * to prevent them being updated based on the base numberSet type
	 * 
	 * @param setNumber index of numberSet
	 * @param weight weight to set
	 */
	public void setCfgNSWeight(int setNumber, double weight) {
		if (setNumber >= nsWeights.length) return;
		nsWeights[setNumber] = weight;
	}
	
	/**
	 * Set the full set of numberSet weights to the passed in set
	 * NOTE: this will not be maintained if numberSets are altered, you must lock the numberSets
	 * to prevent them being updated based on the base numberSet type
	 * 
	 * @param setWeights list of numberSet weights to set
	 */
	public void setCfgNSWeights(double [] setWeights) {
		this.nsWeights = Arrays.copyOf(setWeights, setWeights.length);
	}
	
	/**
	 * get a copy of the current numberSet weights
	 * @return
	 */
	public double [] getCfgNSWeightsCopy() {
		return Arrays.copyOf(nsWeights, nsWeights.length);
	}
	
	/**
	 * get a direct link to the current numberSet weights
	 * @return
	 */
	public double [] getCfgNSWeightRaw() {
		return nsWeights;
	}
	
	/**
	 * Set the numberSet weights as locked
	 * 
	 * @param locked true to lock, false to unlock
	 */
	public void setCfgNSWeightsLocked(boolean locked) {
		this.nsLocked = locked;
	}
	
	/**
	 * Check if the numberSet weights are locked
	 * @return
	 */
	public boolean isNSWeightsLocked() {
		return this.nsLocked;
	}
	
	// 
	// rebase all Accumulator totals with the trained value counts
	// - also removes the value
	//	
	public void setCfgBaseLineBooleanModeAndClear(long value, boolean remove) {
		this.modeBaseLineBoolean = true;
		
		// default accumulator
		int tot = this.getAccumDefault().getCount(value);
		if (remove) this.getAccumDefault().remove(value);
		this.getAccumDefault().adjustTotal(tot);
		
		for (int i=0;i<this.getNSCount();i++) {
			MLNumberSetHash nsh = this.getNSHash(i);
			nsh.updateAccumTotalsWithTrainedValue(value, remove);
		}
	}
	
	//
	// in this mode all values and probabilities represent 1 side of a boolean possiblity
	// thus it must go over %50 percent to not lose
	// - each value is independent of all other values
	//
	boolean isBaseLineBooleanMode() {
		return modeBaseLineBoolean;
	}
	

	//
	// scratchpadd to save additional info needed for some models
	//
	
	/**
	 * Get a value from the scratchpad
	 * @param key key for the value
	 * @return
	 */
	public Object getCfgScratch(String key) {
		if (scratchPad == null) return null;
		return scratchPad.get(key);
	}
	
	/**
	 * Get a string value from the scratchpad
	 * @param key key for the value
	 * @return
	 */
	public String getCfgScratchString(String key) {
		if (scratchPad == null) return null;
		return (String)scratchPad.get(key);
	}
	
	/**
	 * Get a double value from the scratchpad
	 * @param key key for the value
	 * @return
	 */
	public double getCfgScratchDouble(String key) {
		if (scratchPad == null) return 0;
		Object o = scratchPad.get(key);
		if (o == null) return 0;
		return ((Double)o).doubleValue();
	}
	
	/**
	 * Get a Integer value from the scratchpad
	 * @param key key for the value
	 * @return
	 */
	public int getCfgScratchInt(String key) {
		if (scratchPad == null) return 0;
		Object o = scratchPad.get(key);
		if (o == null) return 0;
		return ((Integer)o).intValue();
	}
	
	/**
	 * Get a boolean value from the scratchpad
	 * @param key key for the value
	 * @return
	 */
	public boolean getCfgScratchBool(String key) {
		if (scratchPad == null) return false;
		Object o = scratchPad.get(key);
		if (o == null) return false;
		return ((Boolean)o).booleanValue();
	}
	
	/**
	 * Set a value in the scratchpad
	 * @param key key for the value
	 * @param obj object to add
	 */
	public void setCfgScratch(String key, Object obj) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		scratchPad.put(key, obj);
	}
	
	/**
	 * Set a double value in the scratchpad
	 * @param key key for the value
	 * @param val value to add
	 */
	public void setCfgScratch(String key, double val) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		scratchPad.put(key, val);
	}
	
	/**
	 * Set a integer value in the scratchpad
	 * @param key key for the value
	 * @param val value to add
	 */
	public void setCfgScratch(String key, int val) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		scratchPad.put(key, val);
	}
	
	/**
	 * Set a boolean value in the scratchpad
	 * @param key key for the value
	 * @param val value to add
	 */
	public void setCfgScratch(String key, boolean val) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		scratchPad.put(key, val);
	}
	
	/**
	 * increment an integer value in the scratchpadd
	 * @param key key for the value
	 */
	public void setCfgScratchInc(String key) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		int v = getCfgScratchInt(key);
		setCfgScratch(key,v+1);
	}
	
	/**
	 * remove a value from the scratchpad
	 * @param key key for the value
	 */
	public void delCfgScratch(String key) {
		if (scratchPad == null) return;
		scratchPad.remove(key);
	}
	
	/**
	 * add ignore inputs for this dataplane
	 * @param valueSet set of valueIds for dimension/input to ignore on evaluation
	 */
	public void setIgnoreInputs(long [] valueSet) {
		setCfgScratch("IN_IGNORE", valueSet);
	}
	
	/**
	 * add ignore inputs for this dataplane
	 * @param stringSet set of Strings for dimension/input to ignore on evaluation
	 */
	public void setIgnoreInputs(String [] stringSet) {
		long [] valueSet = new long [stringSet.length];
		for (int i=0;i<stringSet.length;i++) valueSet[i] = (long)this.toVectorS(stringSet[i]);
		setIgnoreInputs(valueSet);
	}
	
	/**
	 * get ignore inputs if any
	 * @return
	 */
	public long [] getIgnoreInputs() {
		return (long [])getCfgScratch("IN_IGNORE");
	}
	
	/**
	 * add ignore inputs for this dataplane
	 * @param valueSet set of valueIds for dimension/input to ignore on evaluation
	 */
	public void setIgnoreInputsValues(long [] valueIdSet) {
		setCfgScratch("IN_IGNORE_VALS", valueIdSet);
	}
	
	/**
	 * add ignore inputs for this dataplane
	 * @param stringSet set of Strings for dimension/input to ignore on evaluation
	 */
	public void setIgnoreInputsValues(String [] stringIdSet) {
		long [] valueSet = new long [stringIdSet.length];
		for (int i=0;i<stringIdSet.length;i++) valueSet[i] = (long)this.toVectorS(stringIdSet[i]);
		setIgnoreInputsValues(valueSet);
	}
	
	/**
	 * get ignore input values if any
	 * @return
	 */
	public long [] getIgnoreInputsValues() {
		return (long [])getCfgScratch("IN_IGNORE_VALS");
	}
	
	/**
	 * get ignore input value
	 * @return
	 */
	public long getIgnoreInputsValue(int offset) {
		long [] l = getIgnoreInputsValues();
		if (l != null) return l[offset];
		return (long)VegML.emptyVect;
	}
	
	//
	// Merge weight
	//
	public void setCfgMergeWeight(String key, double mergeValue) {
		this.setCfgScratch("MW_"+key, mergeValue);
	}
	public void setCfgMergeWeight(VDataPlane dp, double mergeValue) {
		this.setCfgScratch("MW_"+dp.getDimensionTag(), mergeValue);
	}
	public double getCfgMergeWeight(String key) {
		if (scratchPad == null) return -1;
		Object o = scratchPad.get("MW_"+key);
		if (o == null) return -1;
		return ((Double)o).doubleValue();
	}
	public double getCfgMergeWeight(VDataPlane dp) {
		return this.getCfgMergeWeight(dp.getDimensionTag());
	}

	//
	// Merge mappings with mode
	//
	public MergeMap getCfgMergeMap(String dimensionTag2, String dataPlaneTag2) {
		return (MergeMap)this.getCfgScratch(dimensionTag2+"/"+dataPlaneTag2+"-mm");
	}
	public void setCfgMergeMap(String dimensionTag2, String dataPlaneTag2, MergeMap map) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		this.setCfgScratch(dimensionTag2+"/"+dataPlaneTag2+"-mm", map);
	}
	public void setCfgMergeMapMode(String dimensionTag2, String dataPlaneTag2, int mmode) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		MergeMap mm = getCfgMergeMap(dimensionTag2, dataPlaneTag2);
		mm.setMergeMode(mmode);
		setCfgMergeMap(dimensionTag2, dataPlaneTag2, mm);
	}
	public void removeMergeMap(String dimensionTag2, String dataPlaneTag2) {
		this.delCfgScratch(dimensionTag2+"/"+dataPlaneTag2+"-mm");
	}

	//
	// Amplify Tune Value mappings with mode
	//
	public static String getAmpTuneName(String dtag, String dptag) {		
		return "AMPX_"+dtag+"_"+dptag;
	}
	// -2 if not set
	public int getCfgAmpTuneValueX(String dtag, String dptag) {
		if (scratchPad == null) return -2;
		Object o = scratchPad.get(getAmpTuneName(dtag, dptag));
		if (o == null) return -2;
		return ((Integer)o).intValue();
	}
	public void setCfgAmpTuneValueX(String dtag, String dptag, int ampValue) {
		setCfgScratch(getAmpTuneName(dtag, dptag), ampValue);
	}
	
	//
	// set callout to break before or after merged dataplane
	//
	private static String getCfgMergeBreakName(String dtag, String dptag, boolean after) {		
		if (after) return "AFT_"+dtag+"_"+dptag;
		return "BFE_"+dtag+"_"+dptag;
	}
	public boolean getCfgMergeBreakBefore(String dtag, String dptag) {
		return getCfgScratchBool(getCfgMergeBreakName(dtag, dptag, false));
	}
	public void setCfgMergeBreakBefore(String dtag, String dptag, boolean dobreak) {
		setCfgScratch(getCfgMergeBreakName(dtag, dptag, false), dobreak);
	}
	public boolean getCfgMergeBreakAfter(String dtag, String dptag) {
		return getCfgScratchBool(getCfgMergeBreakName(dtag, dptag, true));
	}
	public void setCfgMergeBreakAfter(String dtag, String dptag, boolean dobreak) {
		setCfgScratch(getCfgMergeBreakName(dtag, dptag, true), dobreak);
	}	
	
	/**
	 * get the description for this dataplane
	 * @return
	 */
	public String getCfgDescription() {
		if (scratchPad == null) return null;
		return (String)scratchPad.get("dp-description");
	}
	
	/**
	 * Add a description for this dataplane
	 * @return
	 */
	public void setCfgDescription(String description) {
		setCfgScratch("dp-description", description);
	}
	
	/**
	 * print the contents of the scratchpad to stdout
	 */
	public void printCfgScratch() {		
		// get all merge key/values for dataPlane
		System.out.print("  ScratchPad["+this.getDimensionTag()+"/"+this.getTag()+"]");
		if (scratchPad == null) {
			System.out.println(" size[0]");
		} else {
			System.out.println(" size["+scratchPad.keySet().size()+"]");
		}
		// show
		for (String k:scratchPad.keySet()) {
			Object v = scratchPad.get(k);
			if (v instanceof Double) {
				System.out.println(" size["+String.format("%-30s", k)+"] = ["+((Double)v)+"]");				
			} else if (v instanceof Integer) {
				System.out.println(" size["+String.format("%-30s", k)+"] = ["+((Integer)v)+"]");				
			} else if (v instanceof Long) {
				System.out.println(" size["+String.format("%-30s", k)+"] = ["+((Long)v)+"]");				
			} else if (v instanceof Boolean) {
				System.out.println(" size["+String.format("%-30s", k)+"] = ["+((Boolean)v)+"]");		
			} else if (v instanceof String) {
				System.out.println(" size["+String.format("%-30s", k)+"] = ["+((String)v)+"]");				
			} else if (v != null) {
				System.out.println(" size["+String.format("%-30s", k)+"] = type["+v.getClass().getName()+"]");					
			} else {
				System.out.println(" size["+String.format("%-30s", k)+"] = [NULL]");									
			}
		}
	}
	
	/**
	 * Get the valueId for a string using this dataplans converter
	 * @param string string to get valueId for
	 * @return
	 */
	public int toVectorS(String string) {
		return this.getCfgVToV().toVectGen(string);
	}
	
	/**
	 * get a number set accumulator hash
	 * @param setNumber
	 * @return
	 */
	public MLNumberSetHash getNSHash(int setNumber) {
		return nsHashHash.get(setNumber);
	}
	
	/**
	 * get a number set accumulator hash
	 * @param setNumber
	 * @return
	 */
	public int getNSHashId(int setNumber) {
		MLNumberSetHash nsh = getNSHash(setNumber);
		if (nsh == null) return -1;
		return nsh.getNid();
	}
	
	/**
	 * get a number set accumulator hash
	 * @param nid
	 * @return
	 */
	public int getNSForHashId(int nid) {
		for (int ns = 0;ns < this.getNSCount();ns++) {
			if (getNSHashId(ns) == nid) return ns;
		}
		return -1;
	}
	
	/**
	 * Set the Probability calculation alg for determining the weighted probability for 
	 * a 1 value in 1 numberSet for 1 position
	 * 
	 * @param calculator the VegPCalc instance with calulate implemented
	 */
	public void setCfgPCalc(VegPCalc calculator) {
		this.VegPCalculator = calculator;
	}
	
	/**
	 * get the probability calculator for this dataplane
	 * @return
	 */
	public VegPCalc getCfgPCalc() {
		return this.VegPCalculator;
	}

	/**
	 * Set a result Callout handler
	 * @param callout VegCallOut instance for handler
	 * @param arg argument for handler
	 */
	public void setCfgCallout(VegCallOut callout, Object arg) {
		this.VegCallOut = callout;
		this.VegCallOutArg = arg;
	}
	
	/**
	 * get the current results callout handler if any is set
	 * @return handler instance or null
	 */
	public VegCallOut getCfgCallout() {
		return this.VegCallOut;
	}
	
	/**
	 * Get result callout argument if any
	 * @return object or null
	 */
	Object getCfgCalloutArg() {
		return this.VegCallOutArg;
	}	
	
	/**
	 * Set the default result callout for resolving AT this dataplane
	 * @param callout callout to set as default
	 * @param arg arugument to callout or null
	 */
	public void setCfgCalloutDefault(VegCallOut callout, Object arg) {
		this.setCfgScratch("resultCallout", callout);
		this.setCfgScratch("resultCalloutArg", arg);
		this.setCfgCalloutToDefault();
	}
	
	/**
	 * Set current callout with the default if any
	 */
	public void setCfgCalloutToDefault() {
		setCfgCallout(getCfgCalloutDefault(), getCfgCalloutDefaultArg());
	}
	
	/**
	 * Get the default result callout for resolving for this dataplane
	 * @return
	 */
	public VegCallOut getCfgCalloutDefault() {
		return (VegCallOut)this.getCfgScratch("resultCallout");
	}	
	
	/**
	 * get the default result callout argument
	 * @return
	 */
	public Object getCfgCalloutDefaultArg() {
		return this.getCfgScratch("resultCalloutArg");
	}	
	
	/**
	 * Set the Framing method if needed
	 * 
	 * @param name name for this framer
	 * @param framer the VegFramer instance 
	 * @param arg Arg to pass to framer
	 * @param reverse true if reverse framing
	 */
	public void setCfgFramer(String name, VegFramer framer, Object arg, boolean reverse) {
		this.frameMaker = framer;
		this.frameReverse = reverse;
		this.frameMakerArg = arg;
		this.framerName = name;
	}
	
	/**
	 * Set an arg to pass to the framer
	 * @param arg argument
	 */
	public void setCfgFramerArg(Object arg) {
		this.frameMakerArg = arg;
	}
	
	/**
	 * Get the framer instance to use for this dataplane
	 * @return framer instance
	 */
	public VegFramer getFramer() {
		return this.frameMaker;
	}
	
	/**
	 * Get the argument to pass to the framer
	 * @return argument
	 */
	public Object getFramerArg() {
		return this.frameMakerArg;
	}
	
	/**
	 * get the framer name
	 * @return name of framer
	 */
	public String getFramerName() {
		return this.framerName;
	}
	
	/**
	 * set the framer to reverse mode
	 * @param reverse true for reverse, false for normal
	 */
	public void setCfgFrameReverse(boolean reverse) {
		this.frameReverse = reverse;
	}
	
	/**
	 * Check if the framer is in reverse
	 * @return
	 */
	public boolean isFrameReverse() {
		return this.frameReverse;
	}
	

	/**
	 * Frame data for a position
	 * @param ctx context of use
	 * @param frame frame to place data in
	 * @param rs resultSet used for this flow
	 * @param dss DataSets from which data comes
	 * @param set DataSet number
	 * @param position position in dataset
	 * @param predict true if this is a prediction, else false
	 * @return true if process frame, false if not to process
	 */
	public boolean frameData(VContext ctx, VFrame frame, VResultSet rs, VDataSets dss, int set, int position, boolean predict) {
		return getFramer().makeFrameSetup(ctx, this, frame, this.getFramerArg(), predict, rs.valueOut, dss, set, position);
	}
	
	/**
	 * use the dataplans framer to make an input frame from a list of valueIds and a position
	 * this will not work for a framers that need history
	 * 
	 * @param dataIn list if input valueIds
	 * @param position position of focus in dataIn
	 * @return frame as valueId list
	 */
	public List<Long> frameDataV(List<Long> dataIn, int position) {
		VContext ctx = new VContext(this);
		VFrame frame = new VFrame(this);
		VDataSet ds = new VDataSet();
		ds.setDataLV(dataIn);
		VDataSets dss = new VDataSets(ds);
		
		if (getFramer().makeFrameSetup(ctx, this, frame, this.getFramerArg(), true, null, dss, 0, position)) {
			// get frame valueId list
			return frame.getFrameV();
		}
		return null;
	}

	/**
	 * use the dataplans framer to make an input frame from a list of strings and a position
	 * this will not work for a framers that need history
	 * 
	 * @param dataIn list if input strings
	 * @param position position of focus in dataIn
	 * @return frame as valueId list
	 */
	public List<Long> frameDataS(List<String> dataIn, int position) {
		VContext ctx = new VContext(this);
		VFrame frame = new VFrame(this);
		VDataSets dss = new VDataSets(new VDataSet(dataIn));
		
		if (getFramer().makeFrameSetup(ctx, this, frame, this.getFramerArg(), true, null, dss, 0, position)) {
			// get frame valueId list
			return frame.getFrameV();
		}
		return null;
	}
	
	/**
	 * Set true to not save vectors with empty elements
	 * @param noEmptyElements true to not allow vectors with empty tokens
	 */
	public void setCfgNoEmptyElements(boolean noEmptyElements) {
		this.noEmptyElements = noEmptyElements;
	}
	
	/**
	 * Set true to not save vectors with empty elements
	 * @param noEmptyElements true to not allow vectors with empty tokens
	 * @param exceptFull true to make exception for full numberSet
	 */
	public void setCfgNoEmptyElements(boolean noEmptyElements, boolean exceptFull) {
		this.noEmptyElements = noEmptyElements;
		this.noEmptyElementExcept = exceptFull;
		if (exceptFull) mappedExceptVectNum = this.getCfgNSFullNumber();
		else mappedExceptVectNum = -1;
	}
	
	/**
	 * Check if set to no empty elements
	 * @return
	 */
	public boolean isNoEmptyElements() {
		return this.noEmptyElements;
	}

	/**
	 * get the probability method used for this dataplane
	 * @return
	 */
	public ProbMethod getCfgProbMethod() {
		return probMethod;
	}

	/**
	 * set the probability method to use
	 * @param probMethod
	 */
	public void setCfgProbMethod(ProbMethod probMethod) {
		this.probMethod = probMethod;
	}
		
	/**
	 * Get default accumulator for the dataplane
	 * @return
	 */
	Accum getAccumDefault() {
		return defAccum;
	}
	
	/**
	 * Get default accumulator for numberSet
	 * @param setNumber
	 * @return
	 */
	Accum getAccumSetDefault(int setNumber) {
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return null;
		return nsh.getAccumSetDefault();
	}
	
	///////////////////////////////////////////////////
	// Recall
	Accum getRecallAnswerAccum(int setNumber) {
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return null;
		return nsh.getRecallAnswerAccum();
	}
	Accum getRecallAnswerAccum() {
		return this.recallAnswerAccum;
	}
	double getRecallAnswerCorrectPercentage() {
		double RansCrtPer = getRecallAnswerAccum().getCrtTotal();
		double RansPer = getRecallAnswerAccum().getTotal();
		if (RansCrtPer == 0 || RansPer == 0) return 0;
		return (RansCrtPer / RansPer) * (double)100;
	}
	double getRecallAnswerCorrectPercentage(int setNumber) {
		double RansCrtPer = getRecallAnswerAccum(setNumber).getCrtTotal();
		double RansPer = getRecallAnswerAccum(setNumber).getTotal();
		if (RansCrtPer == 0 || RansPer == 0) return 0;
		return (RansCrtPer / RansPer) * (double)100;
	}
	void clearRecallAnswerAccum() {
		this.recallAnswerAccum.clear();
		for (int i = 0;i<getNSCount();i++) {
			getRecallAnswerAccum(i).clear();
		}
		recallRecallCount = recallPredictCount = recallPredictRelateCount = recallRecallPredictCount = recallDefaultCount = 0;
		recallRecallCrtCount = recallPredictCrtCount = recallPredictRelateCrtCount = recallRecallPredictCrtCount = recallDefaultCrtCount = 0;
	}	
	int getRecallRecallCount() {
		return recallRecallCount;
	}
	int getRecallPredictCount() {
		return recallPredictCount;
	}
	int getRecallPredictRelateCount() {
		return recallPredictRelateCount;
	}
	int getRecallRecallPredictCount() {
		return recallRecallPredictCount;
	}
	int getRecallDefaultCount() {
		return recallDefaultCount;
	}
	int getRecallRecallCrtCount() {
		return recallRecallCrtCount;
	}
	int getRecallPredictCrtCount() {
		return recallPredictCrtCount;
	}
	int getRecallPredictRelateCrtCount() {
		return recallPredictRelateCrtCount;
	}
	int getRecallRecallPredictCrtCount() {
		return recallRecallPredictCrtCount;
	}
	int getRecallDefaultCrtCount() {
		return recallDefaultCrtCount;
	}
	void incRecallRecallCount(boolean correct) {
		if (correct) recallRecallCrtCount++;
		recallRecallCount++;
	}
	void incRecallPredictCount(boolean correct) {
		if (correct) recallPredictCrtCount++;
		recallPredictCount++;
	}
	void incRecallPredictRelateCount(boolean correct) {
		if (correct) recallPredictRelateCrtCount++;
		recallPredictRelateCount++;
	}
	void incRecallRecallPredictCount(boolean correct) {
		if (correct) recallRecallPredictCrtCount++;
		recallRecallPredictCount++;
	}
	void incRecallDefaultCount(boolean correct) {
		if (correct) recallDefaultCrtCount++;
		recallDefaultCount++;
	}
	int getRecallCrtCount() {
		return recallDefaultCrtCount+recallRecallPredictCrtCount+recallPredictCrtCount+recallRecallCrtCount+recallPredictRelateCrtCount;
	}
	int getRecallCount() {
		return recallDefaultCount+recallRecallPredictCount+recallPredictCount+recallRecallCount+recallPredictRelateCount;
	}
	
	double getRecallRecallCrtPercentage() {
		if (recallRecallCrtCount == 0 || recallRecallCount == 0) return 0;
		return ((double)recallRecallCrtCount / (double)recallRecallCount) * (double)100;
	}
	double getRecallPredictCrtPercentage() {
		if (recallPredictCrtCount == 0 || recallPredictCount == 0) return 0;
		return ((double)recallPredictCrtCount / (double)recallPredictCount) * (double)100;
	}
	double getRecallPredictRelateCrtPercentage() {
		if (recallPredictRelateCrtCount == 0 || recallPredictRelateCount == 0) return 0;
		return ((double)recallPredictRelateCrtCount / (double)recallPredictRelateCount) * (double)100;
	}
	double getRecallRecallPredictCrtPercentage() {
		if (recallRecallPredictCrtCount == 0 || recallRecallPredictCount == 0) return 0;
		return ((double)recallRecallPredictCrtCount / (double)recallRecallPredictCount) * (double)100;
	}
	double getRecallDefaultCrtPercentage() {
		if (recallDefaultCrtCount == 0 || recallDefaultCount == 0) return 0;
		return ((double)recallDefaultCrtCount / (double)recallDefaultCount) * (double)100;
	}
	
	
	///////////////////////////////////////////////////
	// Predict
	Accum getPredictAnswerAccum(int setNumber) {
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return null;
		return nsh.getPredictAnswerAccum();
	}
	Accum getPredictAnswerAccum() {
		return this.predictAnswerAccum;
	}
	double getPredictAnswerCorrectPercentage() {
		double RansCrtPer = getPredictAnswerAccum().getCrtTotal();
		double RansPer = getPredictAnswerAccum().getTotal();
		if (RansCrtPer == 0 || RansPer == 0) return 0;
		return (RansCrtPer / RansPer) * (double)100;
	}
	double getPredictAnswerCorrectPercentage(int setNumber) {
		double RansCrtPer = getPredictAnswerAccum(setNumber).getCrtTotal();
		double RansPer = getPredictAnswerAccum(setNumber).getTotal();
		if (RansCrtPer == 0 || RansPer == 0) return 0;
		return (RansCrtPer / RansPer) * (double)100;
	}
	void clearPredictAnswerAccum() {
		this.predictAnswerAccum.clear();
		for (int i = 0;i<getNSCount();i++) {
			getPredictAnswerAccum(i).clear();
		}
		predictRecallCount = predictPredictCount = predictRecallPredictCount = predictDefaultCount = 0;
		predictRecallCrtCount = predictPredictCrtCount = predictRecallPredictCrtCount = predictDefaultCrtCount = 0;
		predictUnknownCrtCount = predictUnknownCount = 0;
		predictDefaultCount = predictDefaultCrtCount = 0;
		predictPredictRelateCount = predictPredictRelateCrtCount = 0;
	}	
	int getPredictRecallCount() {
		return predictRecallCount;
	}
	int getPredictPredictCount() {
		return predictPredictCount;
	}
	int getPredictPredictRelateCount() {
		return predictPredictRelateCount;
	}
	int getPredictRecallPredictCount() {
		return predictRecallPredictCount;
	}
	int getPredictDefaultCount() {
		return predictDefaultCount;
	}
	int getPredictUnknownCount() {
		return predictUnknownCount;
	}
	int getPredictRecallCrtCount() {
		return predictRecallCrtCount;
	}
	int getPredictPredictCrtCount() {
		return predictPredictCrtCount;
	}
	int getPredictPredictRelateCrtCount() {
		return predictPredictRelateCrtCount;
	}
	int getPredictRecallPredictCrtCount() {
		return predictRecallPredictCrtCount;
	}
	int getPredictDefaultCrtCount() {
		return predictDefaultCrtCount;
	}
	int getPredictUnknownCrtCount() {
		return predictUnknownCrtCount;
	}
	int getPredictCrtCount() {
		return predictUnknownCrtCount+predictDefaultCrtCount+predictRecallPredictCrtCount+predictPredictCrtCount+predictRecallCrtCount+predictPredictRelateCrtCount;
	}
	int getPredictCount() {
		return predictUnknownCount+predictDefaultCount+predictRecallPredictCount+predictPredictCount+predictRecallCount+predictPredictRelateCount;
	}
	void incPredictRecallCount(boolean correct) {
		if (correct) predictRecallCrtCount++;
		predictRecallCount++;
	}
	void incPredictPredictCount(boolean correct) {
		if (correct) predictPredictCrtCount++;
		predictPredictCount++;
	}
	void incPredictPredictRelateCount(boolean correct) {
		if (correct) predictPredictRelateCrtCount++;
		predictPredictRelateCount++;
	}
	void incPredictRecallPredictCount(boolean correct) {
		if (correct) predictRecallPredictCrtCount++;
		predictRecallPredictCount++;
	}
	void incPredictDefaultCount(boolean correct) {
		if (correct) predictDefaultCrtCount++;
		predictDefaultCount++;
	}
	void incPredictUnknownCount(boolean correct) {
		if (correct) predictUnknownCrtCount++;
		predictUnknownCount++;
	}
	double getPredictRecallCrtPercentage() {
		if (predictRecallCrtCount == 0 || predictRecallCount == 0) return 0;
		return ((double)predictRecallCrtCount / (double)predictRecallCount) * (double)100;
	}
	double getPredictPredictCrtPercentage() {
		if (predictPredictCrtCount == 0 || predictPredictCount == 0) return 0;
		return ((double)predictPredictCrtCount / (double)predictPredictCount) * (double)100;
	}
	double getPredictPredictRelateCrtPercentage() {
		if (predictPredictRelateCrtCount == 0 || predictPredictRelateCount == 0) return 0;
		return ((double)predictPredictRelateCrtCount / (double)predictPredictRelateCount) * (double)100;
	}
	double getPredictRecallPredictCrtPercentage() {
		if (predictRecallPredictCrtCount == 0 || predictRecallPredictCount == 0) return 0;
		return ((double)predictRecallPredictCrtCount / (double)predictRecallPredictCount) * (double)100;
	}
	double getPredictDefaultCrtPercentage() {
		if (predictDefaultCrtCount == 0 || predictDefaultCount == 0) return 0;
		return ((double)predictDefaultCrtCount / (double)predictDefaultCount) * (double)100;
	}
	double getPredictUnknownCrtPercentage() {
		if (predictUnknownCrtCount == 0 || predictUnknownCount == 0) return 0;
		return ((double)predictUnknownCrtCount / (double)predictUnknownCount) * (double)100;
	}

	/**
	 * get the number of tiers in the input data
	 * default is 1, multiple allow sub-elements
	 * @return
	 */
	public int getCfgInputDataTiers() {
		return this.dataDef.getInputCount();
	}
	
	/**
	 * Set the value tag count for this dataplane
	 * boolean=2, x,y,z,o=4, default/unknown=-1
	 * @param dataWidth
	 */
	public void setCfgDataWidth(int dataWidth) {
		this.dataDef.setDataWidth(dataWidth);
	}	
	
	/**
	 * Get the datawidth of this dataplane
	 * @return
	 */
	public int getCfgDataWidth() {
		return this.dataDef.getDataWidth();
	}
	
	/**
	 * Set the input data type
	 * @param inputDataType data type the dataplane accepts
	 */
	public void setCfgInputDataType(DSDataType inputDataType) {
		this.dataDef.setDataType(inputDataType);
	}
	
	/**
	 * get the input data type
	 * @return
	 */
	public DSDataType getCfgInputDataType() {
		return this.dataDef.getDataType();
	}
	
	/**
	 * set the input valueId that is NOT a value: default is 0
	 * @param dataNonValue
	 */
	public void setCfgNonValue(long dataNonValue) {
		this.dataDef.setNonValue(dataNonValue);
	}
	
	/**
	 * get the input non-valueId
	 * @return
	 */
	public long getCfgNonValue() {
		return this.dataDef.getNonValue();
	}
	
	/**
	 * get the data defintion if one is set
	 * @return
	 */
	public VDataSetDescriptor getCfgDataDefinition() {
		return dataDef;
	}
	
	/**
	 * Set a data definition
	 * @param dataDef
	 */
	public void setCfgDataDefinition(VDataSetDescriptor dataDef) {
		this.dataDef = dataDef;
	}
	
	/**
	 * make a data definition and se tit
	 * @param name name of data definition
	 * @param SetType input set type
	 * @param DataType input data type
	 * @param independent true if independent
	 */
	public void setCfgDataDefinitionInput(String name, DSInputSetType SetType, DSDataType DataType, boolean independent) {
		this.dataDef.addInput(name, SetType, getCfgInputDataType(), independent);
	}


	//
	// 
	//
	/**
	 * Add a string into the string map registered for this dataplane
	 * @param valueId valueId of string
	 * @param string string 
	 */
	public void addString(int valueId, String string) {
		if (this.strValueMapID < 0) return;
		synchronized (this) {
			this.getVegML().vectStrMap.add(this.strValueMapID, valueId, string);
		}
	}
	
	/**
	 * Get a string for an integer valueId
	 * @param valueId
	 * @return
	 */
	public String getString(int valueId) {
		if (this.strValueMapID < 0) return null;
		return this.getVegML().vectStrMap.get(this.strValueMapID, valueId);	
	}
	
	/**
	 * Get a string for a long valueId
	 * @param valueId
	 * @return
	 */
	public String getString(long valueId) {
		return getString((int)valueId);
	}
	
	/**
	 * get the string representation for this vectSet
	 * @param vectSet
	 * @return
	 */
	public String getString(int [] vectSet) {
		if (this.strValueMapID < 0 || vectSet == null) return null;
		String s ="";
		for (int i=0;i<vectSet.length;i++) {
			s += "{ "+getString(vectSet[i]) + "} ";
		}
		return s.trim();
	}
	
	/**
	 *  Add a dimension string into the string map registered for this data plane
	 * @param vector
	 * @param string
	 */
	public void addDimensionString(int vector, String string) {
		if (this.strDimensionMapID < 0) return;
		this.getVegML().vectStrMap.add(this.strDimensionMapID, vector, string);
	}

	/**
	 * get the string for this integer dimension vector
	 * @param valueId
	 * @return
	 */
	public String getDimensionString(int valueId) {
		if (this.strDimensionMapID < 0) return null;
		return this.getVegML().vectStrMap.get(this.strDimensionMapID, valueId);	
	}
	
	/**
	 * get the string for this long dimension vector
	 * @param valueId
	 * @return
	 */
	public String getDimensionString(long valueId) {
		if (this.strDimensionMapID < 0) return null;
		return this.getVegML().vectStrMap.get(this.strDimensionMapID, (int)valueId);	
	}	
	
	/**
	 * get the dimension string representation for this vectSet
	 * @param vectSet
	 * @return
	 */
	public String getDimensionString(int [] vectSet) {
		if (this.strDimensionMapID < 0) return null;
		String s ="";
		for (int i=0;i<vectSet.length;i++) {
			s += getDimensionString(vectSet[i]) + " ";
		}
		return s.trim();
	}
	
	/**
	 * get the dimension string representation for this long vectSet
	 * @param vectSet
	 * @return
	 */
	public String getDimensionString(long [] vectSet) {
		if (this.strDimensionMapID < 0) return null;
		String s ="";
		for (int i=0;i<vectSet.length;i++) {
			s += getDimensionString(vectSet[i]) + " ";
		}
		return s.trim();
	}
	
	/**
	 * Check if a dimension string is in this dataplane
	 * Must have identity numberSet in dataplane
	 * 
	 * @param dimensionString dimension string for vector
	 * @return vector id or 0
	 */
	public long haveDimensionString(String dimensionString) {
		if (getCfgNSIdentityNumber() < 0 || dimensionString == null) return 0;
		// look for it in identity
		MLNumberSetHash nsh = getNSHash(getCfgNSIdentityNumber());
		long vid = this.getCfgVToV().toVectGen(dimensionString);
		if (isSolid()) {
			if (nsh.getSolid(getCfgDataWidth()) >= 0) return vid;
		} else {
			if (nsh.get(vid) != null) return vid;
		}
		return 0;
	}		
	
	Accum getDimensionStringIdentity(String dimensionString) {
		MLNumberSetHash nsh = getNSHash(getCfgNSIdentityNumber());
		// look for it in identity
		long vid = this.getCfgVToV().toVectGen(dimensionString);
		return nsh.get(vid);
	}	
	
	/**
	 * Get the ValProb list for a identity and dimensionString
	 * @param dimensionString dimension string for vector
	 * @return sorted ValProb list or null
	 */
	public List<ValProb> getDimensionStringProbList(String dimensionString) {
		if (getCfgNSIdentityNumber() < 0 || dimensionString == null) return null;
		long vid = this.getCfgVToV().toVectGen(dimensionString);
		return findCfgNSProbList(getCfgNSIdentityNumber(), vid);
	}
	
	/**
	 * Get the ValProb list for a identity and vector
	 * @param vid vector id
	 * @return sorted ValProb list or null
	 */
	public List<ValProb> getDimensionStringProbList(long vid) {
		return findCfgNSProbList(getCfgNSIdentityNumber(), vid);
	}
	
	/**
	 * Get the ValProb list for a numberSet and vector
	 * 
	 * @param setNumber numberSet to use
	 * @param vid vector id
	 * @return sorted ValProb list or null
	 */
	public List<ValProb> findCfgNSProbList(int setNumber, long vid) {
		// look for it in identity
		MLNumberSetHash nsh = getNSHash(setNumber);
		if (nsh == null) return null;
		if (isSolid()) {
			int gid = nsh.getSolid(vid);
			if (gid >= 0) {
				return getVPListForSolidID(gid);
			}
		} else {
			Accum ac = nsh.get(vid);
			if (ac != null) {
				return ac.getValPsSorted();
			}
		}
		return null;
	}	
	
	/**
	 * Get the ValProb for a numberSet, vector a value
	 * 
	 * @param setNumber numberSet to use
	 * @param vid vector id
	 * @param valueId valueId to find
	 * @return ValProb or null
	 */
	public ValProb findCfgNSProbList(int setNumber, long vid, long valueId) {
		// look for it in identity
		MLNumberSetHash nsh = getNSHash(setNumber);
		if (nsh == null) return null;
		if (isSolid()) {
			int gid = nsh.getSolid(vid);
			if (gid >= 0) return getVPForSolidID(gid, valueId);	
		} else {
			Accum ac = nsh.get(vid);
			if (ac != null) return ac.getValProb(valueId);
		}
		return null;
	}
	
	/**
	 * Get the vector set id for a numberSet, vector
	 * 
	 * @param setNumber numberSet to use
	 * @param vid vector id
	 * @return vector set id or -1
	 */
	public int findCfgNSVsid(int setNumber, long vid) {
		// look for it in identity
		MLNumberSetHash nsh = getNSHash(setNumber);
		if (nsh == null) return 0;
		if (isSolid()) {
			int gid = nsh.getSolid(vid);
			if (gid >= 0) {
				return 0;
			}
		} else {
			Accum ac = nsh.get(vid);
			if (ac != null) {
				return ac.getVectSetId();
			}
		}
		return 0;
	}	
	
	// get the list of ValProbs for a solid gid
	private ValProb getVPForSolidID(int gid, long value) {
		int probId = this.groupSet[gid][0];
		int valId = this.groupSet[gid][1];
		double [] probList = this.probabilitySets[probId];
		if (this.valueSets != null) {
			int [] valList = this.valueSets[valId];
			for (int i=0;i<probList.length;i++) {
				if (valList[i] == value) {
					ValProb vp = new ValProb();
					vp.counter = 1;
					vp.probability = probList[i];
					vp.value = value;
					vp.count = 1;
					return vp;
				}
			}			
		} else {
			long [] valList = this.valueSetsL[valId];
			for (int i=0;i<probList.length;i++) {
				if (valList[i] == value) {
					ValProb vp = new ValProb();
					vp.counter = 1;
					vp.probability = probList[i];
					vp.value = value;
					vp.count = 1;
					return vp;
				}
			}
		}
		return null;
	}
	
	// get the list of ValProbs for a solid gid
	List<ValProb> getVPListForSolidID(int gid) {
		int probId = this.groupSet[gid][0];
		int valId = this.groupSet[gid][1];
		double [] probList = this.probabilitySets[probId];
		List<ValProb> vpList = new ArrayList<>();
		if (this.valueSets != null) {
			int [] valList = this.valueSets[valId];
			for (int i=0;i<probList.length;i++) {
				// only use values in identity
				VegUtil.mergeIntoVPList(vpList, valList[i], probList[i], 1);
			}			
		} else {
			long [] valList = this.valueSetsL[valId];
			for (int i=0;i<probList.length;i++) {
				// only use values in identity
				VegUtil.mergeIntoVPList(vpList, valList[i], probList[i], 1);
			}
		}
		return vpList;
	}


	/**
	 * Get the Identity numberSets value frequency sorted list
	 * @return all values sorted by frequency
	 */
	public List<ValProb> getIdentityFrequencyProbList() {
		// look for it in identity
		MLNumberSetHash nsh = getNSHash(getCfgNSIdentityNumber());
		if (nsh == null) return null;
		
		if (this.isSolid()) {
			int [] gidl = nsh.getListSolid();
			long [] vidl = nsh.getListSolidVid();
			List<ValProb> vpList = new ArrayList<>();
			for (int i=0;i<gidl.length;i++) {
				int probId = this.groupSet[gidl[i]][0];
				int valId = this.groupSet[gidl[i]][1];
				int [] valList = this.valueSets[valId];
				ValProb vp = new ValProb();
				vp.count = valList.length*5; // hack
				vp.value = vidl[i];
				vp.counter = valList.length;
				vpList.add(vp);
			}
			Collections.sort(vpList, VegUtil.VpFreqSort);	
			return vpList;
		} else {
			return nsh.getListFrequencySorted();
		}
	}
	
	/**
	 * True if data has been added to all numberSets
	 * @return
	 */
	public boolean checkFullDataSet() {
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = this.getNSHash(i);
			if (nsh != null && nsh.size() == 0) return false;
		}
		return true;
	}
	
	/**
	 * Check if this hash is entangled
	 * @return true if entangled, else false
	 */
	public boolean isEntangled() {
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = this.getNSHash(i);
			if (nsh != null && nsh.isEntangled()) return true;
		}
		return false;
	}
	
	//
	// get accum for vector id
	//
	Accum getAccumulator(int ns, long vid) {
		MLNumberSetHash nsh = getNSHash(ns);
		if (nsh == null) return null;
		return nsh.get(vid);
	}
	
	/**
	 * Get the ValProb information for vid from the Identity numberSet
	 * @param vid vector id
	 * @return
	 */
	public ValProb getIdenityValProbIfSingle(long vid) {
		if (isSolid()) {
			int id = getNSHash(this.getCfgNSIdentityNumber()).getSolid(vid);
			if (id < 0) return null;
			long v = 0;
			if (this.isValLong()) {
				long [] ll = this.valueSetsL[this.groupSet[id][1]];
				if (ll.length != 1) return null;
				v = ll[0];
			} else {
				int [] ll = this.valueSets[this.groupSet[id][1]];	
				if (ll.length != 1) return null;
				v = ll[0];
			}
			ValProb vp = new ValProb(v);
			vp.probability = this.probabilitySets[groupSet[id][0]][0];
			vp.type = PredictionType.Predict;
			return vp;
		} else {
			Accum ac = getNSHash(this.getCfgNSIdentityNumber()).get(vid);
			if (ac == null || ac.getValueCount() != 1) return null;
			ValProb vp = ac.getValPs().get(0);  // could be optimized
			vp.type = PredictionType.Predict;
			return vp;
		}
	}
	
	/**
	 * get the sorted ValProb list for this vector
	 * @param vect an input vector
	 * @return sorted ValProb list
	 */
	public List<ValProb> getFullVpList(final long [] vect) {
		MLNumberSetHash nsh = getNSHash(this.getCfgNSFullNumber());
		if (nsh == null) return null;
		long vid = VectorToVid.toVectorV64(vect);
		Accum ac = nsh.get(vid);
		if (ac == null) return null;
		return ac.getValPsSorted();
	}	
	
	//
	// optimize dimensions
	//
	void optimize() {
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = this.getNSHash(i);
			if (nsh != null) nsh.optimize();
		}
		defAccum.optimize();
		recallAnswerAccum.optimize();
		predictAnswerAccum.optimize();
	}
	
	//
	// merge dimensions
	//
	void merge(VDataPlane xdp) {
		if (this.getVectorCount() == 0) {
			// adding 
			this.state = xdp.state;
			//private DataSetDefinition dataDef = null;	// TODO: use this not dataWidth
			this.frameReverse = xdp.frameReverse;
			this.frameMakerArg = xdp.frameMakerArg;
			this.accumulatorType = xdp.accumulatorType;
			this.probMethod = xdp.probMethod;
			this.frameMaker = xdp.frameMaker;
			this.framerName = xdp.framerName;
			this.VegCallOutArg = xdp.VegCallOutArg;
			this.VegCallOut = xdp.VegCallOut;
			this.VegPCalculator = xdp.VegPCalculator;
			this.window = xdp.window;
			this.before = xdp.before;
			this.after = xdp.after;
			this.saveChildVids = xdp.saveChildVids;
			this.nsFullNumber = xdp.nsFullNumber;
			this.nsIdentityNumber = xdp.nsIdentityNumber;
			this.nsForceFullNumber = xdp.nsForceFullNumber;
			this.nsVectNumIdentity = xdp.nsVectNumIdentity;
			this.nsVectNumFull = xdp.nsVectNumFull;	
			this.nsForceIdentityNumber = xdp.nsForceIdentityNumber;
			this.nsLocked = xdp.nsLocked;
			this.nsBase = xdp.nsBase;
			this.nsBaseType = xdp.nsBaseType;
			this.defaultNoFocus = xdp.defaultNoFocus;
			this.identityOnly = xdp.identityOnly;
			this.noEmptyElements = xdp.noEmptyElements;
			this.noEmptyElementExcept = xdp.noEmptyElementExcept;
			this.mappedExceptVectNum = xdp.mappedExceptVectNum;
			
			this.modeBaseLineBoolean = xdp.modeBaseLineBoolean;

			this.vtov = xdp.vtov;
			this.minNsWeight = xdp.minNsWeight;
			
			if (xdp.trainFilter != null) {
				this.trainFilter = xdp.trainFilter;
			}
			// FIXME merge it
			if (xdp.dataDef != null) {
				this.dataDef = xdp.dataDef;
			}
			
			this.nsCount = xdp.nsCount;
			this.nsHashHash.clear();
			this.numberSets.clear();
			
			this.nsWeights = Arrays.copyOf(xdp.nsWeights, xdp.nsWeights.length);
			this.nsTypes = Arrays.copyOf(xdp.nsTypes, xdp.nsTypes.length);
			this.nsMapVectorPosition = Arrays.copyOf(xdp.nsMapVectorPosition, xdp.nsMapVectorPosition.length);
			this.nsMapVectorLength = Arrays.copyOf(xdp.nsMapVectorLength, xdp.nsMapVectorLength.length);
			this.nsMapToVector = Arrays.copyOf(xdp.nsMapToVector, xdp.nsMapToVector.length);	
			
			if (xdp.nsChildMapVectorPosition != null) {
				this.nsChildMapVectorPosition = Arrays.copyOf(xdp.nsChildMapVectorPosition, xdp.nsChildMapVectorPosition.length);
				this.nsChildMapVectorLength = Arrays.copyOf(xdp.nsChildMapVectorLength, xdp.nsChildMapVectorLength.length);
			}
			
			if (this.numberSetsTier != null) this.numberSetsTier = new ArrayList<>(xdp.getNSsTier());
			else this.numberSetsTier = null;
			
			if (xdp.isSolid()) {
				this.probabilitySets = Arrays.copyOf(xdp.probabilitySets, xdp.probabilitySets.length);
				this.valueSets = Arrays.copyOf(xdp.valueSets, xdp.valueSets.length);
				this.valueSetsL = Arrays.copyOf(xdp.valueSetsL, xdp.valueSetsL.length);
				this.groupSet = Arrays.copyOf(xdp.groupSet, xdp.groupSet.length);
			}
			;
			
			// add the numberSets
			for (int i=0;i<xdp.getNSCount();i++) {
				MLNumberSetHash xnsh = xdp.getNSHash(i);
				if (xnsh == null) continue;
				this.numberSets.add(xnsh.getNS());		
				MLNumberSetHash nsh = new MLNumberSetHash(getBaseHashSize(), i, xnsh.getNS(), xnsh.getNSTier());
				this.nsHashHash.put(i, nsh);
				nsh.merge(this, xnsh, xdp);
			}
			// copy strings in
			
			if (xdp.strValueMapID != -1) {
				this.strValueMapID = this.getVegML().vectStrMap.regMap(dtag, tag);
				getVegML().vectStrMap.getMap(this.strValueMapID).putAll(xdp.getVegML().vectStrMap.getMap(xdp.strValueMapID));
			}
			//private int strValueMapID;	// string map ID
			//private int strDimensionMapID;	// string map ID
			// FIXME
		} else {
			// merging
			for (int i=0;i<xdp.getNSCount();i++) {
				MLNumberSetHash xnsh = xdp.getNSHash(i);
				if (xnsh == null || xnsh.size() < 1) continue;
				MLNumberSetHash hm = this.getNSHash(i);
				//hm.merge(this, dpix, xML, xnsh, xdpix, svidMap);
			}
		}

		defAccum.merge(xdp.defAccum);
		recallAnswerAccum.merge(xdp.recallAnswerAccum);
		predictAnswerAccum.merge(xdp.predictAnswerAccum);
		
		this.recallRecallCount += xdp.recallRecallCount;
		this.recallRecallCrtCount += xdp.recallRecallCrtCount;
		this.recallPredictCount += xdp.recallPredictCount;
		this.recallPredictCrtCount += xdp.recallPredictCrtCount;
		this.recallPredictRelateCount += xdp.recallPredictRelateCount;
		this.recallPredictRelateCrtCount += xdp.recallPredictRelateCrtCount;
		this.recallRecallPredictCount += xdp.recallRecallPredictCount;
		this.recallRecallPredictCrtCount += xdp.recallRecallPredictCrtCount;
		this.recallDefaultCount += xdp.recallDefaultCount;
		this.recallDefaultCrtCount += xdp.recallDefaultCrtCount;
		this.predictRecallCount += xdp.predictRecallCount;
		this.predictRecallCrtCount += xdp.predictRecallCrtCount;
		this.predictPredictCount += xdp.predictPredictCount;
		this.predictPredictCrtCount += xdp.predictPredictCrtCount;
		this.predictPredictRelateCount += xdp.predictPredictRelateCount;
		this.predictPredictRelateCrtCount += xdp.predictPredictRelateCrtCount;
		this.predictRecallPredictCount += xdp.predictRecallPredictCount;
		this.predictRecallPredictCrtCount += xdp.predictRecallPredictCrtCount;
		this.predictDefaultCount += xdp.predictDefaultCount;
		this.predictDefaultCrtCount += xdp.predictDefaultCrtCount;
		this.predictUnknownCount += xdp.predictUnknownCount;
		this.predictUnknownCrtCount += xdp.predictUnknownCrtCount;
		if (xdp.scratchPad != null) {
			this.scratchPad = new HashMap<>();
			this.scratchPad.putAll(xdp.scratchPad);
		}
		// map Xsvids to local
		makeVectorMaps();
	}
	
	//
	// diff dimensions
	// 
	int diff(VDataPlane xdpix) {
		int cnt = 0;
		// FIXME
		return cnt;
	}
	
	// sets them correctly
	void updateNumberSetWeights() {
		this.nsWeights = getCfgNSWeightsBase();
		VegUtil.softmax(nsWeights);
		minNsWeight = -1;
		// get min nsweight
		for (int i=0;i<nsWeights.length;i++) {
			if (minNsWeight == -1 || (nsWeights[i] > 0 && nsWeights[i] < minNsWeight)) minNsWeight = nsWeights[i];
		}
		
	}
	
	/**
	 * get the base non-softmaxed weights
	 * @return
	 */
	public double [] getCfgNSWeightsBase() {
		double [] ws = MLNumberSetUtil.makeNumberSetWeights(getNSs(), getCfgWindowSize(), getCfgFrameFocus(), nsBase);
		for (int i=0;i<this.getNSCount();i++) {
			if (isCfgNSTurnedOff(i)) ws[i] = 0;
		}	
		return ws;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// number set state
	boolean setCfgNSTurnedOff(List<Integer> setNumbers, boolean turnedOff) {
		int ns = getNSCount();
		for (Integer setNumber:setNumbers) {
			if (setNumber >= ns || setNumber < 0) return false;
		}
		boolean change = false;
		for (Integer setNumber:setNumbers) {
			MLNumberSetHash nsh = this.getNSHash(setNumber);
			if (nsh != null && nsh.isTurnedOff() != turnedOff) {
				change = true;
				nsh.setTurnedOff(turnedOff);
			}
		}
		if (change) {
			this.updateCfgNS();
		}
		return true;
	}
	boolean setCfgNSTurnedOff(int setNumber, boolean turnedOff) {
		int ns = getNSCount();
		if (setNumber >= ns || setNumber < 0) return false;
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh != null && nsh.isTurnedOff() != turnedOff) {
			nsh.setTurnedOff(turnedOff);
			this.updateCfgNS();
		}
		return true;
	}	
	void setCfgNSTurnedOff(boolean turnedOff) {
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = this.getNSHash(i);
			if (nsh != null) nsh.setTurnedOff(turnedOff);
		}
		this.updateCfgNS();
	}
	List<Integer> getNSTurnedOn() {
		List<Integer> nsl = new ArrayList<>();
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = this.getNSHash(i);
			if (nsh != null && !nsh.isTurnedOff()) nsl.add(i);
		}
		if (nsl.size() > 0) {
			if (nsl.size() == getNSCount()) return null; // all
			return nsl;
		}
		return nsl; 
	}	
	
	/**
	 * check if a numberSet is turned off
	 * @param setNumber number set to check
	 * @return true if turned off, else false
	 */
	public boolean isCfgNSTurnedOff(int setNumber) {
		if (setNumber >= getNSCount() || setNumber < 0) return false;
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return true;
 		return nsh.isTurnedOff();	
	}
	
	/**
	 * Get the count of numberSets that are turned on
	 * @return
	 */
	public int getNSTurnedOnCount() {
		int count = 0;
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = this.getNSHash(i);
			if (nsh != null && !nsh.isTurnedOff()) count++;
		}
		return count;
	}
	
	//
	// true if long values, else int
	//
	boolean isValLong() {
		return (this.accumulatorType == AccumType.Long || this.accumulatorType == AccumType.HashLong);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Make this a solid immutable model
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	int makeSolid() {
		if (probabilitySets != null) return 0;		
		// smash it / optimize and entangle 
		//this.getVegML().smash(this.getDimensionTag(), this.getTag());
		getVegML().entangle(this);
		//this.getVegML().print();
		//System.out.print("   makeSolid["+this.getDimensionTag()+"/"+this.getTag()+"] .");

		List<double []> pSets = new ArrayList<>();
		List<int []> gSets = new ArrayList<>();
		List<int []> vSets = null;
		List<long []> vSetsL = null;
		if (isValLong()) vSetsL = new ArrayList<>();
		else vSets = new ArrayList<>();

		// for each numberSet
		int cnt = 0;
		for (int i=0;i<nsCount;i++) {
			MLNumberSetHash nsh = this.getNSHash(i);
			if (nsh == null) continue;
			cnt += nsh.makeSolid(this, pSets, gSets, vSets, vSetsL);
		}	

		probabilitySets = new double [pSets.size()][];
		for (int i=0;i<pSets.size();i++) {
			probabilitySets[i] = pSets.get(i);
		}
		pSets = null;
		if (vSetsL != null) {
			valueSetsL = new long [vSets.size()][];
			for (int i=0;i<vSets.size();i++) {
				valueSetsL[i] = vSetsL.get(i);
			}	
			vSetsL = null;
		} else {
			valueSets = new int [vSets.size()][];
			for (int i=0;i<vSets.size();i++) {
				valueSets[i] = vSets.get(i);
			}
			vSets = null;
		}

		// make flat
		groupSet = new int [gSets.size()][2];
		for (int i=0;i<gSets.size();i++) {
			groupSet[i][0] = gSets.get(i)[0];
			groupSet[i][1] = gSets.get(i)[1];
		}
		
		this.optimize();
		//System.out.println("... COMPLETE groups["+groupSet.length+"]prob["+probabilitySets.length+"]["+valueSets.length+"]");
		return cnt;
	}
	private void makeVectorMaps() {	
		// make maps for dataplan one makeSolid();		
		int totalVectors = this.getNSCount();		
		if (totalVectors == 0) {
			nsTypes = null;
	    	nsMapVectorPosition = null;
			nsMapVectorLength = null;
	    	nsMapToVector = null;
	    	nsChildMapVectorPosition = null;	  	
			nsChildMapVectorLength = null;
			//System.out.println("ERROR NO-VECTS["+this.getIDString()+"]ns["+this.getNSCount()+"]");			
			return;
		}
		
		// make number set type map
		nsTypes = new int[this.getNSCount()];
		for (int i=0;i<getNSCount();i++) {
			// add direct for position (in window)
			nsTypes[i] = 0;	// context
			
			if (!isCfgFameFocusNone() && MLNumberSetUtil.setContainsOffset(getNSHash(i).getNS(), getCfgFrameFocus())) {
				nsTypes[i] = 1;	// non-context
			} 
		}
		
    	if (numberSetsTier != null) {
    		// count how many additional vectors there are from dependent sets
    		totalVectors = 0;
    		for (int i=0;i<numberSetsTier.size();i++) {
    			totalVectors += numberSetsTier.get(i).size(); // for dependent AND base
    		}
    	}
    	
		// boolean map of all vectors -1=don't do 
    	int tiers = getCfgInputDataTiers();
    	if (tiers > 1) tiers++; // need one extra to save primary
    	nsMapVectorPosition = new boolean[totalVectors][this.getCfgWindowSize()][tiers];	  	
		// map of vector lengths: length -1 if turned off, 
		nsMapVectorLength = new int[totalVectors]; 
		Arrays.fill(nsMapVectorLength, 0);
		// map of vector to numberSet / depNumber
    	nsMapToVector = new int[totalVectors][2]; 
    	makeNsVectorMap(numberSets, numberSetsTier, nsMapVectorPosition, nsMapVectorLength, nsMapToVector, true);

    	
    	if (isCfgSaveChildVids()) {
    		// if saving child Vids, gen child Vector info
	    	nsChildMapVectorPosition = new boolean[totalVectors][this.getCfgWindowSize()][tiers];	  	
			nsChildMapVectorLength = new int[totalVectors]; 
			Arrays.fill(nsChildMapVectorLength, 0);	
	    	
	    	// make child numberSets
	    	List<List<Integer>> nSetsChild = new ArrayList<>();
	    	List<List<List<Integer []>>> nSetsChildTier = null;  
	    	if (numberSetsTier != null) nSetsChildTier = new ArrayList<>();
			for (int i=0;i<numberSets.size();i++) {
				// add direct for position (in window)
				List<Integer> set = getNSChild(i);
				nSetsChild.add(set);
				//System.out.println(" ADD NS["+i+"][" + numberSets.size()+"] ["+this.getNSFormatString(i)+"] => " + set);

				if (nSetsChildTier != null) {
					List<List<Integer []>> settl = getNSTierChild(i);
					nSetsChildTier.add(settl);
				}
			}
	    	makeNsVectorMap(nSetsChild, nSetsChildTier, nsChildMapVectorPosition, nsChildMapVectorLength, null, false);
	    }
	}
	// generate the maps
	private void makeNsVectorMap(List<List<Integer>> nSets, List<List<List<Integer []>>> nSetsTier, 
								boolean [][][] mvPosition, int [] mvLength, int[][] mvNs, boolean setPositions) {
		
		int identVectNum = this.getCfgNSIdentityNumber();
		int fullVectNum = this.getCfgNSFullNumber();
		
	//	if (numberSetsTier != null) System.out.println("DP map ns["+getNSCount()+"]tier["+tiers+"] vect["+totalVectors+"] ");
		
		// make vector tables
		int vfc = getCfgFrameFocus();
		int cvPos = 0;
		for (int i=0;i<nSets.size();i++) {
			// add direct for position (in window)
			MLNumberSetHash nsh = getNSHash(i);
			List<Integer> set = nSets.get(i);
						
			List<List<Integer []>> depSet = null;
			if (nSetsTier != null) depSet = nSetsTier.get(i);		
			if (set == null) {		
				// empty vector; happens for childs
				for (int x=0;x<this.getCfgWindowSize();x++) {
					mvPosition[cvPos][x][0] = false;
					// deps ??
					// FIXME
				}
			} else {		
				for (int x=0;x<this.getCfgWindowSize();x++) {
					// contains this position?
					int idx = set.indexOf(x);
					boolean tag = false;
					if (idx >= 0) tag = true;
	
					mvPosition[cvPos][x][0] = tag;
					
					// each depSet is another Vector, map the values	
					if (depSet != null) {					
						if (i == this.getCfgNSFullNumber()) fullVectNum = cvPos;
						else if (i == this.getCfgNSIdentityNumber()) identVectNum = cvPos;
						
						for (int d=0;d<depSet.size();d++) {						
							// every depSet has this primary; check the dependency lists
							Arrays.fill(mvPosition[cvPos+d][x], false); // all off
							if (tag) {
								// this marks as in use [0]
								mvPosition[cvPos+d][x][0] = true;
								//if (i < 4) System.out.println("  DepMap["+this.getNSFormatString(i)+"]pos["+x+"]v["+(cvPos+d)+"] dep["+d+"] ");		
	
								// dset contains the values for positions
								Integer [] dset = depSet.get(d).get(idx); // vector d, position idx
								for (int dsi=0;dsi<dset.length;dsi++) {
									if (dset[dsi] == -1) continue;
									if (dsi == 0) {
										// the 0 sets the primary position at 1
										mvPosition[cvPos+d][x][1] = true;
									} else {
										// set the dependent positions
										mvPosition[cvPos+d][x][dset[dsi]+1] = true;
										//System.out.println("          v["+(cvPos+d)+"]len["+mvLength[cvPos+d]+"]");		
									}
								}
								mvLength[cvPos+d] += dset.length;					    	  
								/*
									for (int dsi=0;dsi<mvPosition[cvPos+d][x].length;dsi++) {
										System.out.println("        ["+this.getNSFormatString(i)+"]pos["+x+"]v["+(cvPos+d)+"] len["+mvLength[cvPos+d]+"] dep["+d+"]["+dsi+"] "+mvPosition[cvPos+d][x][dsi]);		
									}
									
									for (int dsi=0;dsi<dset.length;dsi++) {
										System.out.println("          D["+dset[dsi]+"]");		
									}
								*/
							} 
						}
					} else {
						if (tag) mvLength[cvPos]++;
					}
				}
			}
			
			// check if length for this vector (or set) is needed
			if (nsh.isTurnedOff()) mvLength[cvPos] = 0;	
			else if (isCfgFameFocusNone() &&  (vfc >= 0 && vfc <= this.getCfgWindowSize()) && nsTypes[i] == 1) {
				// if predicting at position: no sets that include that position / use valueFocus or default
				mvLength[cvPos] = 0;
			}

			// update NumberSet/DepPositino move to next vectorMap position
			if (depSet != null) {	
				if (mvNs != null) {
					for (int d=0;d<depSet.size();d++) {
						mvNs[cvPos+d][0] = i;
						mvNs[cvPos+d][1] = d;
					}
				}
				cvPos += depSet.size();
			} else {
				if (mvNs != null) {
					mvNs[cvPos][0] = i;
					mvNs[cvPos][1] = -1;	
				}
				cvPos++;
			}
		}

		// -1 for all no length
		for (int i=0;i<mvLength.length;i++) {
			if (mvLength[i] <= 0) mvLength[i] = -1;
		}
		
		if (setPositions) {
			nsVectNumIdentity = identVectNum;
			nsVectNumFull = fullVectNum;
			
			// set vectNumbers
			if (noEmptyElementExcept) mappedExceptVectNum = nsVectNumFull;
			else mappedExceptVectNum = -1;
		}
		/*
		// print map and check. on genearte something is off		
		System.out.println(" MAP:" + getNSCount() + " w["+this.getCfgWindowSize()+"]ns["+getNSCount()+"]v["+totalVectors+"]");
		for (int i=0;i<totalVectors;i++) {
			String s = "";
			for (int x=0;x<this.getCfgWindowSize();x++) {
				if (mvPosition[i][x][0]) s +=  " +";
				else s +=  " -";
			}
			System.out.print(" DA["+mvLength[i]+"]: " + s);
			if (mvPosition[i][0].length > 1) {
				s = "";
				for (int x=0;x<this.getCfgWindowSize();x++) {
					s += " [";
					for (int j=1;j<mvPosition[i][x].length;j++) {
						if (mvPosition[i][x][j]) s +=  "+";
						else s +=  "-";
					}
					s += "]";
				}	
				System.out.print(s);
			}
			System.out.println("");
		}
		*/
	}
	
	// get the map of vector lengths
	int [] getVectorLengthMap() {
		return nsMapVectorLength; 
	}
	int getMapVectorNumberSet(int vect) {
		return nsMapToVector[vect][0]; 
	}
	int getMapVectorDepNum(int vect) {
		return nsMapToVector[vect][1]; 
	}
	int getMappedVectorCount() {
		if (nsMapVectorLength == null) return 0;
		return nsMapVectorLength.length;
	}

	/**
	 * reset numberSet value counts 
	 */
	public void resetValueCounts() {
		for (int i=0;i<getNSCount();i++) {
			getNSHash(i).resetValueCounts();
		}
	}
	

	/**
	 * return true if this is a solid model
	 * @return true if solid, else false
	 */
	public boolean isSolid() {
		return (probabilitySets != null);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Tunning utils
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * remove this valueId from all in all numberSets except identity numberSet
	 * for numberSets not turned off
	 * 
	 * @param valueId valueId to remove
	 * @param retainProbability true if retain probability for remaining values
	 * @return count of removed
	 */
	public int removeAllNSValueExceptIdentity(long valueId, boolean retainProbability) {	
		int cnt = 0;
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = getNSHash(i);
			if (nsh == null || i == this.getCfgNSIdentityNumber()) continue;
			if (nsh.isTurnedOff()) continue;
			cnt += removeAllNSValue(i, valueId, retainProbability);
			
			int tot = nsh.getAccumSetDefault().getTotal();
			nsh.getAccumSetDefault().remove(valueId);
			if (retainProbability) nsh.getAccumSetDefault().adjustTotal(tot);
		}		
		return cnt;
	}

	/**
	 * remove valueId from the specified numberSet
	 * 
	 * @param setNumber numberSet to remove value from
	 * @param valueId valueId to remove
	 * @param retainProbability true if retain probability for remaining values
	 * @return count of removed
	 */
	public int removeAllNSValue(int setNumber, long valueId, boolean retainProbability) {	
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return 0;	
		if (this.isSolid()) return 0;
		return nsh.removeAllValue(valueId, retainProbability);
	}
	
	/**
	 * Remove valueId from the number set if it is under min or over max count
	 * @param setNumber numberSet to remove value from
	 * @param valueId valueId to remove
	 * @param minCount min count to retain or -1
	 * @param maxCount max count to retain or -1
	 * @param retainProbability true if retain probability for remaining values
	 * @return count of removed
	 */
	public int removeAllNSValue(int setNumber, long valueId, int minCount, int maxCount, boolean retainProbability) {	
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return 0;		
		if (this.isSolid()) return 0;
		
		return nsh.removeAllValue(valueId, minCount, maxCount, retainProbability, null);
	}
	
	/**
	 * Remove valueId from the number set if it is under min or over max count, with an exception list
	 * @param setNumber numberSet to remove value from
	 * @param valueId valueId to remove
	 * @param minCount min count to retain or -1
	 * @param maxCount max count to retain or -1
	 * @param exList list of valueIds to not remove
	 * @return count of removed
	 */
	public int removeAllNSValue(int setNumber, long valueId, int minCount, int maxCount, HashMap<Long, Integer> exList) {	
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return 0;		
		if (this.isSolid()) return 0;
		
		return nsh.removeAllValue(valueId, minCount, maxCount, true, exList);
	}

	/**
	 * Remove all valueIds that are in the remove list
	 * @param removeLists HashMap[NumberSet-index [vid, valueId]]
	 * @return count of removed
	 */
	public int removeAllNSValues(HashMap<Long, HashMap<Long, Integer>> [] removeLists) {	
		if (this.isSolid()) return 0;
		int cnt = 0;
		for (int ns=0;ns < this.getNSCount();ns++) {
			MLNumberSetHash nsh = this.getNSHash(ns);
			if (nsh == null) continue;
			cnt += nsh.removeAllValues(removeLists[ns], true);
		}
		return cnt;
	}
	
	/**
	 * Weight all valueId from the number set if it is under min or over max count, with an exception list
	 * @param setNumber numberSet to remove value from
	 * @param valueId valueId to remove
	 * @param minCount min count to retain or -1
	 * @param maxCount max count to retain or -1
	 * @param exList list of valueIds to not remove
	 * @param weight weight to add to each match
	 * @return count of modified
	 */
	public int weightAllNSValue(int setNumber, long valueId, int minCount, int maxCount, HashMap<Long, Integer> exList, double weight) {	
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return 0;
		if (this.isSolid()) return 0;
		return nsh.weightAllValue(valueId, minCount, maxCount, exList, weight);
	}
		
	/**
	 * Weight all values in the set 
	 * @param valueLists HashMap[NumberSet-index [vid, valueId]]
	 * @param weight weight to modify each with
	 * @return count of modified
	 */
	public int weightAllNSValues(HashMap<Long, HashMap<Long, Integer>> [] valueLists, double weight) {	
		if (this.isSolid()) return 0;
		int cnt = 0;
		for (int ns=0;ns < this.getNSCount();ns++) {
			MLNumberSetHash nsh = this.getNSHash(ns);
			if (nsh == null) continue;
			cnt += nsh.weightAllValues(valueLists[ns], weight);
		}
		return cnt;
	}
	
	/**
	 * Remove all vectors from the numberSet except those in the exceptionList
	 * @param ns number set to modify
	 * @param exceptionList list to retain
	 * @return
	 */
	public int removeAllNSVectorsExcept(int ns, List<Long> exceptionList) {	
		if (this.isSolid()) return 0;
		MLNumberSetHash nsh = this.getNSHash(ns);
		if (nsh == null) return 0;
		return nsh.removeAllVectorsExcept(exceptionList);
	}

	/**
	 * Remove all vectors from the numberSet that are in the list
	 * @param ns number set to modify
	 * @param vidList list of vectors to remove
	 * @return count removed
	 */
	public int removeAllNSVectors(int ns, List<Long> vidList) {	
		if (this.isSolid() || vidList.size() < 1) return 0;
		MLNumberSetHash nsh = this.getNSHash(ns);
		if (nsh == null) return 0;
		return nsh.removeAllVectors(vidList);
	}
	
	/**
	 * Remove accumulators from the numberSet if the accumulator total is > maxCount
	 * @param setNumber numberSet to modify
	 * @param maxCount max count to retain
	 * @param retainProbability if true retain probability
	 * @return count removed
	 */
	public int removeAllNSAccum(int setNumber, int maxCount, boolean retainProbability) {	
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return 0;
		
		if (this.isSolid()) {	
			int cnt = 0;
			return cnt;
		}
		return nsh.removeAllNSAccum(maxCount, retainProbability);
	}
	
	/**
	 * remove all empty accumulators
	 * @return
	 */
	public int removeAllEmptyAccum() {
		if (this.isSolid()) return 0;
		int cnt = 0;
		for (int ns=0;ns < this.getNSCount();ns++) {
			MLNumberSetHash nsh = this.getNSHash(ns);
			if (nsh == null) continue;
			cnt += nsh.removeAllEmptyAccum();
		}
		return cnt;
	}
		

	/**
	 * increase the count on a vector/value pair by 1
	 * @param setNumber numberSet of vector
	 * @param vid vector id
	 * @param valueId valueId to increment
	 */
	public void addVectorValueCount(int setNumber, long vid, long valueId) {	
		addVectorValueCount(setNumber, vid, valueId, 1);
	}
	
	/**
	 * increase the count on a vector/value pair
	 * @param setNumber numberSet of vector
	 * @param vid vector id
	 * @param valueId valueId to modify
	 * @param upCount count to increase by
	 */
	public void addVectorValueCount(int setNumber, long vid, long valueId, int upCount) {	
		addVectorValueCount(setNumber, vid, valueId, upCount, 0);
	}
	
	/**
	 * increase or decrease the count on a vector/value pair
	 * @param setNumber numberSet of vector
	 * @param vid vector id
	 * @param valueId valueId to modify
	 * @param upCount count to increase by
	 * @param downCount count to decrease by
	 */
	public void addVectorValueCount(int setNumber, long vid, long valueId, int upCount, int downCount) {	
		if (this.isSolid()) return;
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return;
		nsh.addVectorValueCount(vid, valueId, upCount, downCount);
	}
	
	/**
	 * increase or decrease the count on a set of vector/value pairs
	 * @param setNumber numberSet of vector
	 * @param vid vector id
	 * @param valueIds list of valueId to modify
	 * @param upCount count to increase by
	 * @param downCount count to decrease by
	 */
	public void addVectorValueCount(int setNumber, long vid, List<Long> valueIds, int upCount, int downCount) {	
		if (this.isSolid()) return;
		MLNumberSetHash nsh = this.getNSHash(setNumber);
		if (nsh == null) return;
		nsh.addVectorValueCount(vid, valueIds, upCount, downCount);
	}
	
	//
	// remove all Accum that are not identity/full or in map
	//
	public int removeAllNSByVotes(HashMap<Long, Integer>[] retainValues, boolean context) {
		if (this.isSolid()) return 0;
		int cnt = 0;
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = getNSHash(i);
			if (nsh == null || i == this.getCfgNSIdentityNumber()) continue;
			if (i == this.getCfgNSFullNumber()) continue;
			if (!context && this.isCfgNSContext(i)) continue;

			cnt += nsh.removeAllNSByVotes(retainValues[i], 0);	
		}		
		return cnt;
	}
	//
	// remove all Accum that are not identity/full or in map
	//
	public int removeAllNSByVotesExclude(HashMap<Long, Integer>[] removeList, boolean context) {
		if (this.isSolid()) return 0;
		int cnt = 0;
		for (int i=0;i<getNSCount();i++) {
			MLNumberSetHash nsh = getNSHash(i);
			if (nsh == null || i == this.getCfgNSIdentityNumber()) continue;
			if (i == this.getCfgNSFullNumber()) continue;
			if (!context && this.isCfgNSContext(i)) continue;

			cnt += nsh.removeAllNSByVotesExclude(removeList[i]);	
		}		
		return cnt;
	}	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Get base accumulators
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * generate vectors for this frame
	 * @param frame
	 */
	public void genVectors(VFrame frame) {
		VectorToVid.vectSetGen(this.nsMapVectorPosition, this.nsMapVectorLength, frame.getFrameFull(), frame.getVectSpace(), this.isNoEmptyElements(), this.getExceptVectNumber());
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Get Probability list base with mixing between dimensions and dataplanes
	//
	// Get Sorted list of VapProbs for this position
	//
	// modes 
	// - double value average
	// - Average 
	// 
	PredictionType getValPList(VContext ctx, VFrame frame, List<Long> valueOut, boolean nodefaults, Long [] valueIds, boolean segment) {
		if (this.getNSCount() == 0) return PredictionType.Fail;

		frame.vpList.clear();
		PredictionType pType = null;
		if (isSolid()) pType = this.getValPListDirectSolid_int(frame, nodefaults);			
		else pType = this.getValPListDirect_int(frame, nodefaults);	
		
		if (frame.vpList.size() > 0) frame.vpList.get(0).type = pType;
		long valueid = 0;
		if (valueIds != null) valueid = valueIds[0];

		if (VegCallOut != null) {
			VegCallOut.refineResults(ctx, this, VegCallOutArg, frame, valueOut, pType, frame.vpList, valueid);
			if (frame.vpList.size() > 0) pType = frame.vpList.get(0).type; // use the response type
		}

		// resolve ties: NOTE: this pushes probabilities over 1.0 if tie at 1.0
		VegUtil.finailzeVPList(this.getAccumDefault(), frame.vpList, !segment);
		if (frame.vpList.size() > 0) {
			if (frame.vpList.get(0).type == null) {
				System.out.println("WARN missing PredictionType refineResults["+this.getDimensionTag()+"/"+this.getTag()+"]["+frame.vpList.size()+"] "+pType);
				if (pType == null) pType = PredictionType.PredictUnknown;
				frame.vpList.get(0).type = pType;
			}
			if (valueOut != null) valueOut.add(frame.vpList.get(0).value);
		} else {
			if (valueOut != null) valueOut.add((long)VegML.emptyVect);	
		}
		return pType;
	}
 
	// DOES NOT call callout AND doe not add valueOut
	PredictionType getValPList(VContext ctx, VFrame frame, List<Long> valueOut, boolean nodefaults, long [] valSet, boolean limitSet, int noiseLimitFocus, int noiseLimitContext, double ampIdentity) {
		if (this.getNSCount() == 0) return PredictionType.Fail;
		frame.vpList.clear();
		PredictionType pType = null;
		if (isSolid()) pType = this.getValPListDirectSolidAmplify_int(frame, nodefaults, valSet, limitSet, noiseLimitFocus, noiseLimitContext, ampIdentity);					
		else pType = this.getValPListDirectAmplify_int(frame, nodefaults, valSet, limitSet, noiseLimitFocus, noiseLimitContext, ampIdentity);	
		
		if (frame.vpList.size() > 0) frame.vpList.get(0).type = pType;
		//if (VegCallOut != null) VegCallOut.refineResults(Context ctx, this, VegCallOutArg, frame, valueOut, pType, frame.vpList, valueId);
		
		// resolve ties
		VegUtil.finailzeVPList(this.getAccumDefault(), frame.vpList, true);
		return pType;
	}
	
	//
	// get for solid
	//
	private PredictionType getValPListDirectSolid_int(VFrame frame, boolean nodefaults) {
		if (this.defAccum.total == 0) return PredictionType.Fail;	
		
		/////////////////////////////////
		// Vector generate for all
		VectorToVid.vectSetGen(nsMapVectorPosition, nsMapVectorLength, frame.getFrameFull(), frame.getVectSpace(), isNoEmptyElements(), getExceptVectNumber());

		
		/////////////////////////////////
		// map group Ids		
		int nsCnt = mapAccumSpace(frame);
			
		PredictionType ret = PredictionType.Predict;
		long recallValue = 0;
		
		/////////////////////////////////
		// get fullest value
		int avsGid = -1;
		if (nsVectNumFull >= 0) avsGid = frame.getSetIds()[nsVectNumFull];
		if (avsGid >= 0) {
			ret = PredictionType.Recall;
			long [] valListL = null;
			int [] valList = null;
			if (isValLong()) valListL = this.valueSetsL[groupSet[avsGid][1]];
			else valList = this.valueSets[groupSet[avsGid][1]];
			
			if (valList != null && valList.length > 1) ret = PredictionType.RecallPredict;
			else if (valListL != null && valListL.length > 1) ret = PredictionType.RecallPredict;
			else if (getCfgProbMethod() == ProbMethod.AverageIfNotRecall) {
				//
				// get fullest AND no collisions then this
				//
				double [] probList = this.probabilitySets[this.groupSet[avsGid][0]];
				for (int i=0;i<probList.length;i++) {
					// only use values in identity
					long v = 0;
					if (isValLong()) v = valListL[i];
					else v = valList[i];
					VegUtil.mergeIntoVPList(frame.vpList, v, probList[i], 1);
				}
				//System.out.println(" RECAL[@"+frame.getDataSetPosition()+"] " + probList.length + " => " + vpList.size());
				Collections.sort(frame.vpList, VegUtil.VpSort);
				frame.vpList.get(0).type = ret;
				return ret;
			}
			recallValue = valList[0];
		}
		
		/////////////////////////////////
		// identity info if filtering by it
		int iacGid = -1;
		long [] iacValListL = null;
		int [] iacValList = null;
		if (nsVectNumIdentity >= 0) iacGid = frame.getSetIds()[nsVectNumIdentity];
		if (iacGid < 0 && !isCfgFameFocusNone()) ret = PredictionType.PredictUnknown;	
		if (!identityOnly) iacGid = -1;
		if (iacGid >= 0) {
			if (this.isValLong()) iacValListL = this.valueSetsL[this.groupSet[iacGid][1]];
			else iacValList = this.valueSets[this.groupSet[iacGid][1]];
		}
		
		/////////////////////////////////
		// get the probabilties and values
		for (int ns=0;ns<frame.getSetIds().length;ns++) {
			if (frame.getSetIds()[ns] < 0) continue;
			
			double [] probList = this.probabilitySets[groupSet[frame.getSetIds()[ns]][0]];
			boolean isCtx = isCfgNSContext(getMapVectorNumberSet(ns));
			
			boolean added = false;
			if (isValLong()) {
				long [] valList = this.valueSetsL[groupSet[frame.getSetIds()[ns]][1]];				
				for (int i=0;i<probList.length;i++) {
					long v = valList[i];
					if (iacValListL != null && isCtx && !containsVal(iacValListL, v)) continue; // a bit slow -> PERF issue
					VegUtil.mergeIntoVPList(frame.vpList, v, probList[i], 1);
					added = true;
				}				
			} else {
				int [] valList = this.valueSets[groupSet[frame.getSetIds()[ns]][1]];				
				for (int i=0;i<probList.length;i++) {
					long v = valList[i];
					if (iacValList != null && isCtx && !containsVal(iacValList, v)) continue; // a bit slow -> PERF issue
					VegUtil.mergeIntoVPList(frame.vpList, v, probList[i], 1);
					added = true;
				}
			}
			if (added && !isCtx && ret == PredictionType.Predict && ns != nsVectNumIdentity) ret = PredictionType.PredictRelate;
		}
		
		/////////////////////////////////
		// if nothing -> Fall back when nothing -> use general dimension set probability: get best
		if (frame.vpList.size() < 1) {
			if (nodefaults) return PredictionType.Fail;
			List<ValProb> vpl = getAccumDefault().getMostProbable();
			if (vpl == null) return PredictionType.Fail;	
			// add them in
			for (int i=0;i<vpl.size();i++) {
				ValProb vp = vpl.get(i).copy();
				//vp.probability =  vp.probability*DEFAULT_PROB_WEIGHT;	// FIXME lesser?
				vp.probability =  vp.probability*DEFAULT_PROB_WEIGHT*minNsWeight;
				vp.type = PredictionType.Default;
				vp.counter = 1;
				vp.count = 1;
				VegUtil.mergeIntoVPList(frame.vpList, vp);
			}
			frame.vpList.get(0).type = PredictionType.Default;
			return PredictionType.Default;
		}
	
		/////////////////////////////////
		// Average the values (based on the number here)
		for (int i=0;i<frame.vpList.size();i++) {
			ValProb vpx = frame.vpList.get(i);
			vpx.probability = vpx.probability / nsCnt;
			vpx.counter = 1;
		}
		
		Collections.sort(frame.vpList, VegUtil.VpSort);	 
	
		//if (iacValList != null) System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"]  iac["+iacGid+"]["+iacValList.length+"/"+this.getString(iacValList[0])+"]("+getCfgNSIdentityNumber()+")   ["+this.getString(recallValue)+"]["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");
		//else if (recallValue > 0) System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"] ix["+iacGid+"]   ["+this.getString(recallValue)+"]["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");
		//else System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"]   ix["+iacGid+"] ["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");
		
		/////////////////////////////////
		// forced winner
		if (getCfgProbMethod() == ProbMethod.AverageRecall && ret == PredictionType.Recall && recallValue != frame.vpList.get(0).value) {
			for (int i=0;i<frame.vpList.size();i++) {
				ValProb vp = frame.vpList.get(i);
				if (vp.value == recallValue) {
					vp.probability = frame.vpList.get(0).probability + (frame.vpList.get(0).probability * WIN_MARGIN_PERCENT);
					Collections.sort(frame.vpList, VegUtil.VpSort);	
					break;
				}
			}
		}	

		frame.vpList.get(0).type = ret;
		return ret;
	}
	
	private PredictionType getValPListDirectSolidAmplify_int(VFrame frame, boolean nodefaults, 
			 long [] valSet, boolean limitSet, int noiseLimitFocus, int noiseLimitContext, double ampIdentity) {
		if (this.defAccum.total == 0) return PredictionType.Fail;	
		
		/////////////////////////////////
		// Vector generate for all
		VectorToVid.vectSetGen(nsMapVectorPosition, nsMapVectorLength, frame.getFrameFull(), frame.getVectSpace(), isNoEmptyElements(), getExceptVectNumber());

		/////////////////////////////////
		// map group Ids		
		int nsCnt = mapAccumSpace(frame);
			
		PredictionType ret = PredictionType.Predict;
		long recallValue = 0;
		
		/////////////////////////////////
		// get fullest value
		int avsGid = -1;
		if (nsVectNumFull >= 0) avsGid = frame.getSetIds()[nsVectNumFull];
		if (avsGid >= 0) {
			ret = PredictionType.Recall;
			long [] valListL = null;
			int [] valList = null;
			if (isValLong()) valListL = this.valueSetsL[groupSet[avsGid][1]];
			else valList = this.valueSets[groupSet[avsGid][1]];
			
			if (valList != null && valList.length > 1) ret = PredictionType.RecallPredict;
			else if (valListL != null && valListL.length > 1) ret = PredictionType.RecallPredict;
			else if (getCfgProbMethod() == ProbMethod.AverageIfNotRecall) {
				//
				// get fullest AND no collisions then this
				//
				double [] probList = this.probabilitySets[this.groupSet[avsGid][0]];
				// get sum
				double mod = 0;
				if (isValLong()) mod = VegUtil.amplifyListProbGetTotal(probList, valListL, valSet, -1);
				else mod = VegUtil.amplifyListProbGetTotal(probList, valList, valSet, -1);
				for (int i=0;i<probList.length;i++) {
					// only use values in identity
					long v = 0;
					double p = probList[i];
					if (isValLong()) {
						v = valListL[i];
						p = VegUtil.amplifyListProbLimitSet(probList, valListL, i, valSet, mod);
					} else {
						v = valList[i];
						p = VegUtil.amplifyListProbLimitSet(probList, valList, i, valSet, mod);
					}
					if (p <= 0) continue;
					VegUtil.mergeIntoVPList(frame.vpList, v, p, 1);
				}
				//System.out.println(" RECAL[@"+frame.getDataSetPosition()+"] " + probList.length + " => " + vpList.size());
				Collections.sort(frame.vpList, VegUtil.VpSort);
				frame.vpList.get(0).type = ret;
				return ret;
			}
			recallValue = valList[0];
		}
		
		/////////////////////////////////
		// identity info if filtering by it
		int iacGid = -1;
		long [] iacValListL = null;
		int [] iacValList = null;
		if (nsVectNumIdentity >= 0) iacGid = frame.getSetIds()[nsVectNumIdentity];
		if (iacGid < 0 && !isCfgFameFocusNone()) ret = PredictionType.PredictUnknown;	
		if (!identityOnly) iacGid = -1;
		if (iacGid >= 0) {
			if (this.isValLong()) iacValListL = this.valueSetsL[this.groupSet[iacGid][1]];
			else iacValList = this.valueSets[this.groupSet[iacGid][1]];
		}		
				
		/////////////////////////////////
		// get the probabilties and values
		for (int ns=0;ns<frame.getSetIds().length;ns++) {
			if (frame.getSetIds()[ns] < 0) continue;
			int numberSet = getMapVectorNumberSet(ns);

			// per number set noise limits		
			//contains focus
			// per number set noise limits		
			int noiseLimit = noiseLimitContext;
			boolean isCtx = isCfgNSContext(numberSet);
			if (!isCtx) noiseLimit = noiseLimitFocus;

			double mod = 0;
			boolean added = false;
			
			double [] probList = this.probabilitySets[groupSet[frame.getSetIds()[ns]][0]];
			
			if (isValLong()) {
				long [] valList = this.valueSetsL[groupSet[frame.getSetIds()[ns]][1]];
				mod = VegUtil.amplifyListProbGetTotal(probList, valList, valSet, noiseLimit);
				double p = 0;
	
				for (int i=0;i<probList.length;i++) {
					long v = valList[i];
					if (!limitSet) p = VegUtil.amplifyListProbSet(probList, valList, i, valSet, mod);
					else p = VegUtil.amplifyListProbLimitSet(probList, valList, i, valSet, mod);
					if (p <= 0) continue;
					
					// best is numberSet change AND limit
					if (nsVectNumIdentity == ns && ampIdentity > 0) p *= ampIdentity;	// numberSet weight change?
					else if (iacValListL != null && isCtx && !containsVal(iacValListL, v)) continue; // a bit slow -> PERF issue			
					VegUtil.mergeIntoVPList(frame.vpList, v, p, 1);
					added = true;
				}				
			} else {
				int [] valList = this.valueSets[groupSet[frame.getSetIds()[ns]][1]];
				mod = VegUtil.amplifyListProbGetTotal(probList, valList, valSet, noiseLimit);
				double p = 0;

				for (int i=0;i<probList.length;i++) {
					long v = valList[i];
					if (!limitSet) p = VegUtil.amplifyListProbSet(probList, valList, i, valSet, mod);
					else p = VegUtil.amplifyListProbLimitSet(probList, valList, i, valSet, mod);					
					if (p <= 0) continue;
					
					// best is numberSet change AND limit
					if (nsVectNumIdentity == ns && ampIdentity > 0) p *= ampIdentity;	// numberSet weight change?
					else if (iacValList != null && isCtx && !containsVal(iacValList, v)) continue; // a bit slow -> PERF issue					
					VegUtil.mergeIntoVPList(frame.vpList, v, p, 1);
					added = true;
				}
			}
			if (added && !isCtx && ret == PredictionType.Predict && ns != nsVectNumIdentity) ret = PredictionType.PredictRelate;
		}
		
		/////////////////////////////////
		// if nothing -> Fall back when nothing -> use general dimension set probability: get best
		if (frame.vpList.size() < 1) {
			if (nodefaults) return PredictionType.Fail;
			List<ValProb> vpl = getAccumDefault().getMostProbable();
			if (vpl == null) return PredictionType.Fail;	
			// add them in
			for (int i=0;i<vpl.size();i++) {
				ValProb vp = vpl.get(i).copy();
				//vp.probability =  vp.probability*DEFAULT_PROB_WEIGHT;	// FIXME lesser?
				vp.probability =  vp.probability*DEFAULT_PROB_WEIGHT*minNsWeight;
				vp.type = PredictionType.Default;
				vp.counter = 1;
				vp.count = 1;
				VegUtil.mergeIntoVPList(frame.vpList, vp);
			}
			frame.vpList.get(0).type = PredictionType.Default;
			return PredictionType.Default;
		}
	
		/////////////////////////////////
		// Average the values (based on the number here)
		for (int i=0;i<frame.vpList.size();i++) {
			ValProb vpx = frame.vpList.get(i);
			vpx.probability = vpx.probability / nsCnt;
			vpx.counter = 1;
		}
		Collections.sort(frame.vpList, VegUtil.VpSort);	
	
		//if (iacValList != null) System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"]  iac["+iacGid+"]["+iacValList.length+"/"+this.getString(iacValList[0])+"]("+getCfgNSIdentityNumber()+")   ["+this.getString(recallValue)+"]["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");
		//else if (recallValue > 0) System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"] ix["+iacGid+"]   ["+this.getString(recallValue)+"]["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");
		//else System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"]   ix["+iacGid+"] ["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");
		
		/////////////////////////////////
		// forced winner
		if (getCfgProbMethod() == ProbMethod.AverageRecall && ret == PredictionType.Recall && recallValue != frame.vpList.get(0).value) {
			for (int i=0;i<frame.vpList.size();i++) {
				ValProb vp = frame.vpList.get(i);
				if (vp.value == recallValue) {
					vp.probability = frame.vpList.get(0).probability + (frame.vpList.get(0).probability * WIN_MARGIN_PERCENT);
					Collections.sort(frame.vpList, VegUtil.VpSort);	
					break;
				}
			}
		}	
		frame.vpList.get(0).type = ret;
		return ret;
	}
	
	// find value in array
	private static boolean containsVal(long [] iacValListL, long val) {
		for (int i=0;i<iacValListL.length;i++) {
			if (iacValListL[i] == val) return true;
		}
		return false;
	}
	private static boolean containsVal(int [] iacValList, long val) {
		for (int i=0;i<iacValList.length;i++) {
			if (iacValList[i] == val) return true;
		}
		return false;
	}
	
	// map the accum space for the vector set
	// return count of accumes mapped
	private int mapAccumSpace(VFrame frame) {
		int nsCnt = 0;
		for (int i=0;i<frame.getVectSpace().length;i++) {
			frame.getAccumSpace()[i] = null;
			frame.getSetIds()[i] = -1;
			if (frame.getVectSpace()[i] == -2 || frame.getVectSpace()[i] == -1 || frame.getVectSpace()[i] == 0) {
				frame.getVectSpace()[i] = -1; // set all to the same
				continue;	
			}

			int ns = getMapVectorNumberSet(i); // get numberSet for the vector
			if (isSolid()) frame.getSetIds()[i] = getNSHash(ns).getSolid(frame.getVectSpace()[i]);
			else frame.getAccumSpace()[i] = getNSHash(ns).get(frame.getVectSpace()[i]);
			nsCnt++;
			//System.out.println("  MAS["+i+"]["+nsCnt+"] => ["+frame.getVectSpace()[i]+"] " + frame.getAccumSpace()[i]);		
		}	
		return nsCnt;
	}
	
	
	//
	// get the simplest list: non-solid not-amplified
	//
	private PredictionType getValPListDirect_int(VFrame frame, boolean nodefaults) {
		if (this.defAccum.total == 0) return PredictionType.Fail;
				
		/////////////////////////////////
		// Vector generate for all
		VectorToVid.vectSetGen(nsMapVectorPosition, nsMapVectorLength, frame.getFrameFull(), frame.getVectSpace(), isNoEmptyElements(), getExceptVectNumber());

		/////////////////////////////////
		// map accumulators		
		int nsCnt = mapAccumSpace(frame);

		// get the default accumulator
		Accum dac = getAccumDefault();
		
		/////////////////////////////////
		// get fullest numberSet
		PredictionType ret = PredictionType.Predict;
		long recallValue = 0;
		Accum avs = null;
		if (nsVectNumFull >= 0) avs = frame.getAccumSpace()[nsVectNumFull];
		if (avs != null) {
			//System.out.println(" [@"+frame.getDataSetPosition()+"]["+ret+"] cnt["+frame.vpList.size()+"] ["+nsVectNumFullest+"] avs: " + avs);
			ret = PredictionType.Recall;
			if (avs.getValueCount() > 1) ret = PredictionType.RecallPredict;
			else if (getCfgProbMethod() == ProbMethod.AverageIfNotRecall) {
				//
				// get fullest AND no collisions then this
				//			
				frame.vpTempList.clear();
			//	if (this.isBaseLineBooleanMode()) avs.getValPsSortedBoolean(frame.vpTempList);
			//	else 
					avs.getValPs(frame.vpTempList);
				Accum sac = getAccumSetDefault(getCfgNSFullNumber());
				for (int i=0;i<frame.vpTempList.size();i++) {
					ValProb vp = frame.vpTempList.get(i);
					double wavgProb = getCfgPCalc().calculateProb(getCfgProbMethod(), this, dac, getCfgNSWeightRaw(), getCfgNSFullNumber(), true, sac, avs, vp);
					if (wavgProb <= 0) continue;

					vp.probability = wavgProb;
					vp.counter = 1;
					vp.type = ret;
					frame.vpList.add(vp);
				}
				Collections.sort(frame.vpList, VegUtil.VpSort);
				frame.vpList.get(0).type = ret;
				return ret;
			} 
		}
			
		/////////////////////////////////
		// identity info if filtering by it
		Accum iac = null;
		if (nsVectNumIdentity >= 0) iac = frame.getAccumSpace()[nsVectNumIdentity];
		if (iac == null && !isCfgFameFocusNone()) ret = PredictionType.PredictUnknown;
		if (!identityOnly) iac = null;

		/////////////////////////////////
		// get the probabilties and values
		int ccc = 0;
		for (int vi =0;vi<frame.getAccumSpace().length;vi++) {
			Accum vs = frame.getAccumSpace()[vi];
			if (vs == null) continue; // empty slot
			int numberSet = getMapVectorNumberSet(vi);

			// get full list and merge it into the complete list
			frame.vpTempList.clear();		
		//	if (this.isBaseLineBooleanMode()) vs.getValPsSortedBoolean(frame.vpTempList);
		//	else 
				vs.getValPs(frame.vpTempList);
			if (frame.vpTempList.size() < 1) continue;
			
			boolean isCtx = isCfgNSContext(numberSet);
			
			// get the set accumulators
			Accum sac = getAccumSetDefault(numberSet);
			boolean added = false;
			for (int i=0;i<frame.vpTempList.size();i++) {
				ValProb vp = frame.vpTempList.get(i);	
				// only use values in identity
				if (iac != null && isCtx && iac.getCount(vp.value) < 1) continue; // a bit slow
				
				// get the weighted version from the 'activation' fuction
				double wavgProb = getCfgPCalc().calculateProb(getCfgProbMethod(), this, dac, getCfgNSWeightRaw(), numberSet, (iac != null), sac, vs, vp);
				if (wavgProb <= 0) continue;
				vp.probability = wavgProb;
				vp.counter = 1;
				VegUtil.mergeIntoVPList(frame.vpList, vp);
				added = true;
			}
			if (added) {
				ccc++;
				if (vi == nsVectNumFull) recallValue = frame.vpTempList.get(0).value;	
				if (!isCtx && ret == PredictionType.Predict && vi != nsVectNumIdentity) ret = PredictionType.PredictRelate;
			}
		}

		/////////////////////////////////
		// if nothing -> Fall back when nothing -> use general dimension set probability: get best
		if (frame.vpList.size() < 1) {
			if (nodefaults) return PredictionType.Fail;
			List<ValProb> vpl = dac.getMostProbable();
			if (vpl == null) return PredictionType.Fail;				
			// add them in
			for (int i=0;i<vpl.size();i++) {
				ValProb vp = vpl.get(i).copy();
				//vp.probability =  vp.probability*DEFAULT_PROB_WEIGHT;	// FIXME lesser?
				vp.probability =  vp.probability*DEFAULT_PROB_WEIGHT*minNsWeight;
				vp.type = PredictionType.Default;
				vp.counter = 1;
				VegUtil.mergeIntoVPList(frame.vpList, vp);
			}
			frame.vpList.get(0).type = PredictionType.Default;
			return PredictionType.Default;
		}
		
		/////////////////////////////////
		// Average the values (based on the number here)
		for (int i=0;i<frame.vpList.size();i++) {
			ValProb vpx = frame.vpList.get(i);
			vpx.probability = vpx.probability / nsCnt;
			vpx.counter = 1;
		}		
		Collections.sort(frame.vpList, VegUtil.VpSort);
		
		/////////////////////////////////
		// forced winner
		if (getCfgProbMethod() == ProbMethod.AverageRecall && ret == PredictionType.Recall && recallValue != frame.vpList.get(0).value) {
			for (int i=0;i<frame.vpList.size();i++) {
				ValProb vp = frame.vpList.get(i);
				if (vp.value == recallValue) {
					vp.probability = frame.vpList.get(0).probability + (frame.vpList.get(0).probability * WIN_MARGIN_PERCENT);
					Collections.sort(frame.vpList, VegUtil.VpSort);	
					break;
				}
			}
		}
		frame.vpList.get(0).type = ret;
		return ret;		
	}
	
	// noiseLimitContext == -2 => no amp
	private PredictionType getValPListDirectAmplify_int(VFrame frame, boolean nodefaults,
			long [] valSet, boolean limitSet, int noiseLimitFocus, int noiseLimitContext, double ampIdentity) {
		if (this.defAccum.total == 0) return PredictionType.Fail;
		if (noiseLimitContext == -2) return PredictionType.Default;
			
		/////////////////////////////////
		// Vector generate for all
		VectorToVid.vectSetGen(nsMapVectorPosition, nsMapVectorLength, frame.getFrameFull(), frame.getVectSpace(), isNoEmptyElements(), getExceptVectNumber());

		/////////////////////////////////
		// map accumulators		
		int nsCnt = mapAccumSpace(frame);
		
		// get the default accumulator
		Accum dac = getAccumDefault();
		
		/////////////////////////////////
		// get fullest numberSet
		PredictionType ret = PredictionType.Predict;
		long recallValue = 0;
		Accum avs = null;
		if (nsVectNumFull >= 0) avs = frame.getAccumSpace()[nsVectNumFull];
		if (avs != null) {
			ret = PredictionType.Recall;
			if (avs.getValueCount() > 1) ret = PredictionType.RecallPredict;
			else if (getCfgProbMethod() == ProbMethod.AverageIfNotRecall) {
				//
				// get fullest AND no collisions then this
				//		
				frame.vpTempList.clear();			
				List<ValProb> vpMMList = avs.getValPsAmplifyLimitSet(frame.vpTempList, valSet, -1);
				if (vpMMList != null) {
					Accum sac = getAccumSetDefault(getCfgNSFullNumber());
					for (int i=0;i<vpMMList.size();i++) {
						ValProb vp = vpMMList.get(i);
						double wavgProb = getCfgPCalc().calculateProb(getCfgProbMethod(), this, dac, getCfgNSWeightRaw(), getCfgNSFullNumber(), true, sac, avs, vp);
						if (wavgProb <= 0) continue;
	
						vp.probability = wavgProb;
						vp.counter = 1;
						vp.type = ret;
						frame.vpList.add(vp);
					}
				}
				Collections.sort(frame.vpList, VegUtil.VpSort);
				frame.vpList.get(0).type = ret;
				return ret;
			} 
		}
			
		/////////////////////////////////
		// identity info if filtering by it
		Accum iac = null;
		if (nsVectNumIdentity >= 0) iac = frame.getAccumSpace()[nsVectNumIdentity];
		if (iac == null && !isCfgFameFocusNone()) ret = PredictionType.PredictUnknown;
		if (!identityOnly) iac = null;
		
		/////////////////////////////////
		// get the probabilties and values
		for (int vi =0;vi<frame.getAccumSpace().length;vi++) {
			Accum vs = frame.getAccumSpace()[vi];
			if (vs == null) continue; // empty slot
			int numberSet = getMapVectorNumberSet(vi);
			MLNumberSetHash nsh = getNSHash(numberSet);
			if (nsh == null) continue;
			
			// get the set accumulators
			Accum sac = getAccumSetDefault(numberSet);
			frame.vpTempList.clear();
			boolean isCtx = isCfgNSContext(numberSet);

			// per number set noise limits		
			int noiseLimit = noiseLimitContext;
			if (!isCtx) {
				noiseLimit = noiseLimitFocus;
				if (ret == PredictionType.Predict && vi != nsVectNumIdentity) ret = PredictionType.PredictRelate;
			}

			if (!limitSet) vs.getValPsAmplifySet(frame.vpTempList, valSet, noiseLimit);
			else vs.getValPsAmplifyLimitSet(frame.vpTempList, valSet, noiseLimit);		
			
			if (nsVectNumIdentity == vi && ampIdentity > 0) VegUtil.updateListProb(frame.vpTempList, ampIdentity, valSet);	// numberSet weight change?
			if (frame.vpTempList.size() > 0) {
				// get full list and merge it into the complete list
				boolean added = false;
				for (int i=0;i<frame.vpTempList.size();i++) {
					ValProb vp = frame.vpTempList.get(i);	
					// only use values in identity
					if (iac != null && isCtx && iac.getCount(vp.value) < 1) continue; // a bit slow
					
					// get the weighted version from the 'activation' fuction
					double wavgProb = getCfgPCalc().calculateProb(getCfgProbMethod(), this, dac, getCfgNSWeightRaw(), numberSet, (iac != null), sac, vs, vp);
					if (wavgProb <= 0) continue;
					vp.probability = wavgProb;
					vp.counter = 1;
					VegUtil.mergeIntoVPList(frame.vpList, vp);
					added = true;
				}
				if (added && vi == nsVectNumFull) recallValue = frame.vpTempList.get(0).value;
			}
		}

		/////////////////////////////////
		// if nothing -> Fall back when nothing -> use general dimension set probability: get best
		if (frame.vpList.size() < 1) {
			if (nodefaults) return PredictionType.Fail;
			List<ValProb> vpl = dac.getMostProbable();
			if (vpl == null) return PredictionType.Fail;	

			// add them in
			for (int i=0;i<vpl.size();i++) {
				ValProb vp = vpl.get(i).copy();
				//vp.probability =  vp.probability*DEFAULT_PROB_WEIGHT;	// FIXME lesser?
				vp.probability =  vp.probability*DEFAULT_PROB_WEIGHT*minNsWeight;
				vp.type = PredictionType.Default;
				vp.counter = 1;
				VegUtil.mergeIntoVPList(frame.vpList, vp);
			}
			frame.vpList.get(0).type = PredictionType.Default;
			return PredictionType.Default;
		}
		
		/////////////////////////////////
		// Average the values (based on the number here)
		for (int i=0;i<frame.vpList.size();i++) {
			ValProb vpx = frame.vpList.get(i);
			vpx.probability = vpx.probability / nsCnt;
			vpx.counter = 1;
		}		
		Collections.sort(frame.vpList, VegUtil.VpSort);
	
		//if (iac != null) System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"]  iac["+iac.getValPs().size()+"]("+getCfgNSIdentityNumber()+")   ["+this.getString(recallValue)+"]["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");
		//else if (recallValue > 0) System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"]   ["+this.getString(recallValue)+"]["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");
		//else System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"]   ["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");

		
		/////////////////////////////////
		// forced winner
		if (getCfgProbMethod() == ProbMethod.AverageRecall && ret == PredictionType.Recall && recallValue != frame.vpList.get(0).value) {
			for (int i=0;i<frame.vpList.size();i++) {
				ValProb vp = frame.vpList.get(i);
				if (vp.value == recallValue) {
					vp.probability = frame.vpList.get(0).probability + (frame.vpList.get(0).probability * WIN_MARGIN_PERCENT);
					Collections.sort(frame.vpList, VegUtil.VpSort);	
					break;
				}
			}
		}
		frame.vpList.get(0).type = ret;
		return ret;		
	}

	
	//
	// multi-run with modifications
	//
	PredictionType getValPListModify(VContext ctx, List<VFrame> frameList, TestModSet tests, List<List<Long>> valueOutList, boolean nodefaults, Long [] valueIds, boolean exactProbs) {
		
		for (int i=0;i<frameList.size();i++) frameList.get(i).vpList.clear();

		PredictionType pType = this.getValPListDirectModify_int(frameList, tests, null, null, nodefaults, exactProbs);	
		long valueId = 0;
		if (valueIds != null) valueId = valueIds[0];
		
		for (int i=0;i<frameList.size();i++) {
			VFrame frame = frameList.get(i);
			List<Long> valueOut = valueOutList.get(i);
			if (VegCallOut != null) {
				if (ctx.getModTests() != null) ctx.setModTest(i);
				VegCallOut.refineResults(ctx, this, VegCallOutArg, frame, valueOut, pType, frame.vpList, valueId);
				if (frame.vpList.size() > 0) pType = frame.vpList.get(0).type; // use the response type
				ctx.setModTest(-1);
			}
			
			// resolve ties
			if (frame.vpList.size() > 0) {
				VegUtil.finailzeVPList(this.getAccumDefault(), frame.vpList, true);
				if (frame.vpList.get(0).type == null) {			
					System.out.println("WARN missing PredictionType refineResultsMod["+this.getDimensionTag()+"/"+this.getTag()+"]["+frame.vpList.size()+"] "+pType);
					if (pType == null) pType = PredictionType.PredictUnknown;
					frame.vpList.get(0).type = pType;
				}
				if (valueOut != null) valueOut.add(frame.vpList.get(0).value);
			} else {
				if (valueOut != null) valueOut.add((long)VegML.emptyVect);
			}
		}
		return pType;
	}
	
	PredictionType getValPListModify(VContext ctx, VFrame frame, TestMod test, List<Long> valueOut, boolean nodefaults, Long [] valueIds, boolean exactProbs) {		
		frame.vpList.clear();
		
		PredictionType pType = this.getValPListDirectModify_int(null, null, frame, test, nodefaults, exactProbs);	
		long valueId = 0;
		if (valueIds != null) valueId = valueIds[0];
		
		if (VegCallOut != null) {
			VegCallOut.refineResults(ctx, this, VegCallOutArg, frame, valueOut, pType, frame.vpList, valueId);
			if (frame.vpList.size() > 0) pType = frame.vpList.get(0).type; // use the response type
		}
		// resolve ties
		VegUtil.finailzeVPList(this.getAccumDefault(), frame.vpList, true);
		if (frame.vpList.size() > 0 && frame.vpList.get(0).type == null) {
			System.out.println("WARN missing PredictionType refineResultsMod["+this.getDimensionTag()+"/"+this.getTag()+"]["+frame.vpList.size()+"] "+pType);
			if (pType == null) pType = PredictionType.PredictUnknown;
			frame.vpList.get(0).type = pType;
		}
		
		if (frame.vpList.size() > 0) valueOut.add(frame.vpList.get(0).value);
		else valueOut.add((long)VegML.emptyVect);
		return pType;
	}	
	
	//
	// modifiable multiple test version
	// get the simplest list: non-solid not-amplified
	//
	// NOTE: first test MUST include all numbersets to get full vectors
	//
	private PredictionType getValPListDirectModify_int(List<VFrame> frameList, TestModSet testSet, VFrame sframe, TestMod stest, boolean nodefaults, boolean exactProb) {
		if (this.defAccum.total == 0) return PredictionType.Fail;
		
		/////////////////////////////////
		// Vector generate for all
		VFrame bframe = sframe;
		if (bframe == null) bframe = frameList.get(0); // use this for vectors
		VectorToVid.vectSetGen(nsMapVectorPosition, nsMapVectorLength, bframe.getFrameFull(), bframe.getVectSpace(), isNoEmptyElements(), getExceptVectNumber());
		
		/////////////////////////////////
		// map accumulators		
		int bnsCnt = mapAccumSpace(bframe);
		
		// get the default accumulator
		Accum dac = getAccumDefault();
		PredictionType ret = PredictionType.Predict;
		int tstCnt = 1;
		List<TestMod> tests = null;
		if (stest != null) {
			tests = new ArrayList<>();
			tests.add(stest);
		} else if (testSet != null) {
			tests = testSet.get(this);
			tstCnt = testSet.size();
		}
		
		// for each test
		for (int tst=0;tst < tstCnt;tst++) {
			TestMod test = null;
			if (tests != null) test = tests.get(tst);
			VFrame frame = sframe;
			if (frame == null) frame = frameList.get(tst);
			int nsCnt = bnsCnt; // drop count accordinginly
			ret = PredictionType.Predict;
			double [] nsw = this.nsWeights;
			if (test != null) nsw = test.nsWeights;
			
			//FIXME need current fullest AND current identity numbers AFTER mods
			int fullVect = nsVectNumFull;
			int fullNs = getCfgNSFullNumber();
			//if (nsw[fullNs] == 0) {
		//		fullVect = this.nsVectNumIdentity;
		//		fullNs = this.getCfgNSIdentityNumber();
			//}
				
			/////////////////////////////////
			// identity info if filtering by it
			Accum iac = null;
			if (nsVectNumIdentity >= 0) {
				iac = bframe.getAccumSpace()[nsVectNumIdentity];
				if (test != null && test.nsWeights[getCfgNSIdentityNumber()] == 0) iac = null;
				if (test != null && iac != null && test.getModCount(getCfgNSIdentityNumber()) > 0) {
					frame.vpTempList.clear();
					int cnt = 0;
					List<ValProb> vpMMList = iac.getValPs(frame.vpTempList);
					for (int i=0;i<vpMMList.size();i++) {
						ValProb vp = vpMMList.get(i);
						vp = checkMod(test, getCfgNSIdentityNumber(), iac.vectorCode, vp);
						if (vp == null) continue;
						cnt++;
					}	
					if (cnt == 0) iac = null;
				}
				
				if (iac != null && isCfgIdentityOnly() && iac.getValueCount() == 1 && !test.isModNS(nsVectNumIdentity) && !exactProb) {
					// PERFORMANCE: fast path for single values..
					// only if NO modification to Identity	
					ValProb vp = iac.getFirstMostProbable();
					Accum sac = getAccumSetDefault(getCfgNSIdentityNumber());
					double wavgProb = getCfgPCalc().calculateProb(getCfgProbMethod(), this, dac, nsw, getCfgNSFullNumber(), true, sac, iac, vp);
					vp.probability = wavgProb;
					frame.vpList.add(vp);
					frame.vpList.get(0).type = PredictionType.Predict;
					continue;
				}
			} 
			
			Accum avs = null;
			if (fullNs >= 0) {
				avs = bframe.getAccumSpace()[fullVect];
				if (nsw[fullNs] == 0) avs = null;
			}
			
			/////////////////////////////////
			// get fullest numberSet
			long recallValue = 0;
			if (avs != null) {
				ret = PredictionType.Recall;
				frame.vpTempList.clear();
				List<ValProb> vpMMList = avs.getValPs(frame.vpTempList);
				if (avs.getValueCount() > 1) {
					ret = PredictionType.RecallPredict;
					if (test != null && test.getModCount(getCfgNSFullNumber()) > 0) {
						int cnt = 0;
						for (int i=0;i<vpMMList.size();i++) {
							ValProb vp = vpMMList.get(i);
							vp = checkMod(test, getCfgNSFullNumber(), avs.vectorCode, vp);
							if (vp == null) continue;
							cnt++;
						}
						if (cnt == 1) ret = PredictionType.Recall;
					} 
				}
				if (ret == PredictionType.Recall && getCfgProbMethod() == ProbMethod.AverageIfNotRecall) {
					//
					// get fullest AND no collisions then this
					//			
					Accum sac = getAccumSetDefault(getCfgNSFullNumber());
					int cnt = 0;
					for (int i=0;i<vpMMList.size();i++) {
						ValProb vp = vpMMList.get(i);
						// get value mod
						vp = checkMod(test, getCfgNSFullNumber(), avs.vectorCode, vp);
						if (vp == null) continue;
						double wavgProb = getCfgPCalc().calculateProb(getCfgProbMethod(), this, dac, nsw, getCfgNSFullNumber(), true, sac, avs, vp);
						if (wavgProb <= 0) continue;
						vp.probability = wavgProb;
						vp.counter = 1;
						vp.type = ret;
						frame.vpList.add(vp);
						cnt++;
					}
					if (cnt > 0) {
						Collections.sort(frame.vpList, VegUtil.VpSort);
						frame.vpList.get(0).type = ret;
						continue;
					}
					// not there
					ret = PredictionType.Predict;
				}
			}
				
			if (iac == null && !isCfgFameFocusNone()) ret = PredictionType.PredictUnknown;
			if (!isCfgIdentityOnly()) iac = null; // not used
			
			/////////////////////////////////
			// get the probabilties and values
			for (int vi =0;vi<bframe.getAccumSpace().length;vi++) {
				if (bframe.getVectSpace()[vi] == -1) continue;

				int numberSet = getMapVectorNumberSet(vi);
				if (nsw[numberSet] == 0) {
					nsCnt--; // account for this
					continue;
				}
						
				Accum vs = bframe.getAccumSpace()[vi];
				if (vs == null) continue; // empty slot

				// get the set accumulators
				Accum sac = getAccumSetDefault(numberSet);
				// get full list and merge it into the complete list
				frame.vpTempList.clear();
				vs.getValPs(frame.vpTempList);
				if (frame.vpTempList.size() < 1) continue;

				boolean isCtx = isCfgNSContext(numberSet);

				boolean added = false;
				for (int i=0;i<frame.vpTempList.size();i++) {
					ValProb vp = frame.vpTempList.get(i);	
					
					vp = checkMod(test, numberSet, vs.vectorCode, vp);
					if (vp == null) continue;

					// only use values in identity
					if (iac != null && isCtx) {
						vp = checkMod(test, this.getCfgNSIdentityNumber(), iac.vectorCode, vp);
						if (vp == null) continue;
						if (iac.getCount(vp.value) < 1) continue; // a bit slow
					}
					
					// get the weighted version from the 'activation' fuction
					double wavgProb = getCfgPCalc().calculateProb(getCfgProbMethod(), this, dac, nsw, vi, (iac != null), sac, vs, vp);
					if (wavgProb <= 0) continue;
					
					vp.probability = wavgProb;
					vp.counter = 1;
					VegUtil.mergeIntoVPList(frame.vpList, vp);
					added = true;
				}
				if (added) {
					if (vi == fullVect) recallValue = frame.vpTempList.get(0).value;
					if (!isCtx && ret == PredictionType.Predict && vi != nsVectNumIdentity) ret = PredictionType.PredictRelate;
				}
			}
	//		System.out.println("default["+tst+"] nsCnt["+nsCnt+"]["+acCnt+"]["+adCnt+"] ["+bCnt+"]");

			/////////////////////////////////
			// if nothing -> Fall back when nothing -> use general dimension set probability: get best
			if (frame.vpList.size() < 1) {
				if (nodefaults) return PredictionType.Fail;
				List<ValProb> vpl = dac.getMostProbable();
				if (vpl == null) return PredictionType.Fail;				
				// add them in
				for (int i=0;i<vpl.size();i++) {
					ValProb vp = vpl.get(i).copy();
					vp.probability =  vp.probability*DEFAULT_PROB_WEIGHT*minNsWeight;
					//vp.probability =  vp.probability*DEFAULT_PROB_WEIGHT;	// FIXME use identity weight?
					vp.type = PredictionType.Default;
					vp.counter = 1;
					VegUtil.mergeIntoVPList(frame.vpList, vp);
				}
				frame.vpList.get(0).type = PredictionType.Default;
				continue;
			}
			
			/////////////////////////////////
			// Average the values (based on the number here)
			for (int i=0;i<frame.vpList.size();i++) {
				ValProb vpx = frame.vpList.get(i);
				vpx.probability = vpx.probability / nsCnt; // voter elagable 
				vpx.counter = 1;
			}		
			Collections.sort(frame.vpList, VegUtil.VpSort);

			/////////////////////////////////
			// forced winner
			if (getCfgProbMethod() == ProbMethod.AverageRecall && ret == PredictionType.Recall && recallValue != frame.vpList.get(0).value) {
				for (int i=0;i<frame.vpList.size();i++) {
					ValProb vp = frame.vpList.get(i);
					if (vp.value == recallValue) {
						vp.probability = frame.vpList.get(0).probability + (frame.vpList.get(0).probability * WIN_MARGIN_PERCENT);
						Collections.sort(frame.vpList, VegUtil.VpSort);	
						break;
					}
				}
			}
			// save result
			frame.vpList.get(0).type = ret;
		}
		return ret;		
	}
	// check the test mod rule, if null.. exclide			
	ValProb checkMod(TestMod test, int ns, long vid, ValProb vp) {
		if (test == null) return vp;
		testModAcValue mod = test.getMod(ns, vp.value);
		if (mod == null) return vp;
		
		ValProb vx = vp;
		boolean match = false;
		if (test.getVectorId(ns, vid) != 0) { // check exclude/weight
			if (mod.minCount >= 0 && (mod.minCount == 0 || vp.count <= mod.minCount)) match = true;
			else if (mod.maxCount >= 0 && (mod.maxCount == 0 || vp.count >= mod.maxCount)) match = true;
		}
		if (match) {
			if (mod.weight <= 0) return null; // exclude
			vx.probability *= mod.weight;
			
		}
		return vx;
	}

	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// add the numberSets for this position and return the list of new sets
	// new sets are added and turned on after
	//
	int addIncWindowSizeNS(PredictionType optType, int positionNumber, boolean after, List<Integer> numberSets, List<List<Integer>> newNsSet) {		
		NumberSetType nsType = getNSBaseType();
		
		int rpos = 0; 
		if (after) rpos = getCfgWindowSize()-1;  // position in window
		
		// get the sets for the new size
		List<List<Integer>> pset = makeNumberSets(getCfgWindowSize());
		
		// Move  Existing numberSets if needed
		if (nsType == NumberSetType.PowerSet || nsType == NumberSetType.SequenceEdge) {			
			// no change
		} else if (nsType == NumberSetType.SequenceFan) {
			// add right or left of center
			// - - - x - -
			// - - x x - -  - - x - - 
			// - x x x x -  - x x x -
			// x x x x x x  x x x x x
			// FIXME

		} else if (nsType == NumberSetType.SequenceEdgeId) {
			// - - - - - x
			// x x x x x -
			// x x - x x -
			// x - - - x -
			// FIXME
		} else if (nsType == NumberSetType.SequenceLeft|| nsType == NumberSetType.SequenceLeftId) {
			// always add left
			// x x x x
			// x x x -
			// x x - -
			// x - - -
			// FIXME
			
		} else if (nsType == NumberSetType.SequenceRight|| nsType == NumberSetType.SequenceRightId) {
			// always add right
			// x x x x
			// - x x x
			// - - x x
			// - - - x
			// review
			// - - - - x
			// x x x x -
			// - x x x - 
			// - - x x -
			// - - - x -
			// FIXME	
		}
		// adjust focus
		if (this.nsBaseType == NumberSetType.SequenceLeftId || this.nsBaseType == NumberSetType.SequenceRightId || this.nsBaseType == NumberSetType.SequenceEdgeId) {
			// last is focus
			this.before = this.window-1;
			this.after = (this.window-this.before)-1;
		}
		
		int idp = this.getCfgIdentityPosition();

		// mix in the new
		for (int ns=0;ns < pset.size();ns++ ) {
			List<Integer> nl = pset.get(ns);
			
			// get list that have this position
			if (nl.contains(rpos)) {
				if (this.region > 0) {
					// must be <= the region size; controls complexity of dependent relationships
					if (nl.size() != this.window && nl.size() > region) {
						continue;
					}
				}
				
				if (optType == PredictionType.AnyUnknown || optType == PredictionType.PredictUnknown) {						
					if (nl.contains(idp)) continue; // no identity
				}

				int mns = ns;
				//System.out.println("pos["+positionNumber+"]rpos["+rpos+"]w["+windowSize+"]after["+after+"] nsSets["+pset.size()+"]");		
				// add this set to the dataPlane: use the NS returned from the add
				mns = addCfgNSDefined(nl, -1);
				//if (mns < 0) System.out.println("ERROR:     mns["+ns+"] " + NumberSets.setToStringPosition(nl, windowSize, positionNumber));
				//else System.out.println("   mns["+mns+"] " + NumberSets.setToStringPosition(nl, windowSize, positionNumber));		

				// add it if number set
				if (mns >= 0 && !numberSets.contains(mns)) {
					numberSets.add(mns);
					newNsSet.add(nl);
				}
			}
		}
		return numberSets.size();
	}

	/**
	 * Generate the numberSets for a window size based on the configured base numberSet type
	 * @param windowSize window size
	 * @return Array of numberSets
	 */
	public List<List<Integer>> makeNumberSets(int windowSize) {
		if (windowSize == 1) {
			return MLNumberSetUtil.getSubsetCopy(windowSize);
		} else if (this.nsBaseType == NumberSetType.PowerSet) {
			return MLNumberSetUtil.getSubsetCopy(windowSize);
		} else if (this.nsBaseType == NumberSetType.SequenceLeft) {
			return MLNumberSetUtil.getSequenceLeftSets(windowSize, getCfgFrameFocus());
		} else if (this.nsBaseType == NumberSetType.SequenceLeftId) {
			List<List<Integer>> ns = MLNumberSetUtil.getSequenceLeftSets(windowSize-1, getCfgFrameFocus());
			List<Integer> idns = new ArrayList<>();
			idns.add(windowSize-1);
			ns.add(idns);
			return ns;
		} else if (this.nsBaseType == NumberSetType.SequenceRight) {
			return MLNumberSetUtil.getSequenceRightSets(windowSize, getCfgFrameFocus());
		} else if (this.nsBaseType == NumberSetType.SequenceRightId) {
			List<List<Integer>> ns = MLNumberSetUtil.getSequenceRightSets(windowSize-1, getCfgFrameFocus());
			List<Integer> idns = new ArrayList<>();
			idns.add(windowSize-1);
			ns.add(idns);
			return ns;
		} else if (this.nsBaseType == NumberSetType.SequenceEdge) {
			return MLNumberSetUtil.getSequenceEdgeSets(windowSize, getCfgFrameFocus());
		} else if (this.nsBaseType == NumberSetType.SequenceEdgeId) {
			List<List<Integer>> ns = MLNumberSetUtil.getSequenceEdgeSets(windowSize-1, getCfgFrameFocus());
			List<Integer> idns = new ArrayList<>();
			idns.add(windowSize-1);
			ns.add(idns);
			return ns;
		} else if (this.nsBaseType == NumberSetType.SequenceFan) {
			return MLNumberSetUtil.getSequenceFanSets(windowSize, getCfgFrameFocus());
		} else if (this.nsBaseType == NumberSetType.Linear) {
			return MLNumberSetUtil.getLinearSets(windowSize, getCfgFrameFocus());
		}
		// None and done
		return new ArrayList<>();
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// JSON serializer
	static JsonSerializer<VDataPlane> getJSONSerializer() {
		return new JsonSerializer<VDataPlane>() {  
		    @Override
		    public JsonElement serialize(VDataPlane dp, Type typeOfSrc, JsonSerializationContext context) {
		        JsonObject jsonDP = new JsonObject();
		        jsonDP.addProperty("dtag", dp.getDimensionTag());
		        jsonDP.addProperty("tag", dp.getTag());
		        jsonDP.addProperty("desc", dp.getCfgDescription());	        
		        jsonDP.addProperty("state", ""+dp.state);
		        jsonDP.addProperty("prob", dp.prob);

		        jsonDP.addProperty("window", dp.window);
		        jsonDP.addProperty("before", dp.before);
		        jsonDP.addProperty("region", dp.region);		        
		        jsonDP.addProperty("strMapId", dp.strValueMapID);
		        jsonDP.addProperty("probMethod", ""+dp.probMethod);
		        jsonDP.addProperty("noFocus", dp.defaultNoFocus);
		        jsonDP.addProperty("identityOnly", dp.identityOnly);
		        jsonDP.addProperty("noEmpty", dp.noEmptyElements);
		        jsonDP.addProperty("noEmptyExcept", dp.noEmptyElementExcept);
		        jsonDP.addProperty("modeBaseLineBoolean", dp.modeBaseLineBoolean);
/*	
	// stats
	private Accum recallAnswerAccum = null; 			// answer accumulator
	private int recallRecallCount = 0;
	private int recallRecallCrtCount = 0;
	private int recallPredictCount = 0;
	private int recallPredictCrtCount = 0;
	private int recallPredictRelateCount = 0;
	private int recallPredictRelateCrtCount = 0;
	private int recallRecallPredictCount = 0;
	private int recallRecallPredictCrtCount = 0;
	private int recallDefaultCount = 0;
	private int recallDefaultCrtCount = 0;

	private Accum predictAnswerAccum = null; 			// answer accumulator
	private int predictRecallCount = 0;
	private int predictRecallCrtCount = 0;
	private int predictPredictCount = 0;
	private int predictPredictCrtCount = 0;
	private int predictPredictRelateCount = 0;
	private int predictPredictRelateCrtCount = 0;
	private int predictRecallPredictCount = 0;
	private int predictRecallPredictCrtCount = 0;
	private int predictDefaultCount = 0;
	private int predictDefaultCrtCount = 0;
	private int predictUnknownCount = 0;
	private int predictUnknownCrtCount = 0;

	// Data and tag definition
	private VDataSetDescriptor dataDef = null;		
	private AccumType accumulatorType = AccumType.Default;	
	private boolean frameReverse = false;
	private boolean saveChildVids = false;
	
	// Number Sets
	private List<List<Integer>> numberSets = null;
	private List<List<List<Integer []>>> numberSetsTier = null;
	
	private int nsForceFullNumber = -1;
	private int nsForceIdentityNumber = -1;
	private boolean nsLocked = false;
	private HashMap<Long, Integer> trainFilter = null;
	private boolean modeBaseLineBoolean = false;
	private VectorToVid vtov = null;
		        
 */
		    	// which calc
		        String VegPCalc = "other";
		        if (dp.VegPCalculator == dp.getVegML().getPCalcProbabilityNS()) VegPCalc = "ns";
		        else if (dp.VegPCalculator == dp.getVegML().getPCalcProbabilityOnly()) VegPCalc = "prob";
			    jsonDP.addProperty("VegPCalc", VegPCalc);

			    // framer: token / char / other
			    jsonDP.addProperty("framer", dp.getFramerName());
			    
			    // default Accumulator probabilities: array of probability / value
	            JsonArray jsonDac = new JsonArray();
	            List<ValProb> vpList = dp.defAccum.getValPsSorted();
	            if (vpList != null) {
		            for (int i = 0; i < vpList.size();i++) {
			            JsonArray jsonVP = new JsonArray();
			            jsonVP.add(vpList.get(i).probability);
			            jsonVP.add(vpList.get(i).value);
			            jsonDac.add(jsonVP);
		            }
	            }
		        jsonDP.add("dac", jsonDac);	
				
		        // MISC
		        JsonElement jsonScPad = context.serialize(dp.scratchPad, HashMap.class);
		        jsonDP.add("scratchPad", jsonScPad);
		        
		        
		        // NS
		        jsonDP.addProperty("nsCount", dp.nsCount);
		        jsonDP.addProperty("nsFull", dp.nsFullNumber);
		        jsonDP.addProperty("nsIdentity", dp.nsVectNumIdentity);
		        jsonDP.addProperty("nsWeightBase", ""+dp.nsBase);
		        jsonDP.addProperty("nsBaseType", ""+dp.nsBaseType);
		        jsonDP.addProperty("minNsWeight", dp.minNsWeight);
		        JsonElement jsonNSW = context.serialize(dp.nsWeights, double [].class);
		        jsonDP.add("nsWeights", jsonNSW);
		        JsonElement jsonNST = context.serialize(dp.nsTypes, int [].class);
		        jsonDP.add("nsTypes", jsonNST);
	
	            JsonArray jsonNHSs = new JsonArray();
	            for (int ns = 0; ns < dp.getNSCount();ns++) {
					JsonElement jsonNSH = context.serialize(dp.getNSHash(ns), MLNumberSetHash.class);    		
					jsonNHSs.add(jsonNSH);
	            }
	            jsonDP.add("nsh", jsonNHSs);
						
				// numberSet vector mappings
		        JsonElement jsonVPos = context.serialize(dp.nsMapVectorPosition, boolean [][][].class);
		        jsonDP.add("vectPosition", jsonVPos);
		        JsonElement jsonVLen = context.serialize(dp.nsMapVectorLength, int [].class);
		        jsonDP.add("vectLen", jsonVLen);   
		        JsonElement jsonVMap = context.serialize(dp.nsMapToVector, int [][].class);
		        jsonDP.add("vectMap", jsonVMap);
		        
		        // vector mapping
		        jsonDP.addProperty("vectIdentity", dp.nsVectNumIdentity);
		        jsonDP.addProperty("vectFull", dp.nsCount);
		        jsonDP.addProperty("vectExcept", dp.mappedExceptVectNum);
		    
				// Solid Model data
		        JsonElement jsonSMp = context.serialize(dp.probabilitySets, double [][].class);
		        jsonDP.add("probSets", jsonSMp);
		        JsonElement jsonSMv = context.serialize(dp.valueSets, int [][].class);
		        jsonDP.add("valSets", jsonSMv);
				//private AccumType accumulatorType = AccumType.Default;	 // NEED size only
		        //JsonElement jsonSMvl = context.serialize(dp.valueSetsL, long [][].class);
		        //jsonDP.add("valSetsl", jsonSMvl);
		        JsonElement jsonSMg = context.serialize(dp.groupSet, int [][].class);
		        jsonDP.add("groupSets", jsonSMg);
		        
		        return jsonDP;
		    }
		};
	}
	
	/**
	 * Directly 'train' a number set with a vector and valueId
	 * @param setNumber
	 * @param vid
	 * @param vvect
	 */
	public void trainNumberSet(int setNumber, long vid, long valueId) {
		// add direct for position (in window)
		MLNumberSetHash nsh = getNSHash(setNumber);

		// set probability
		Accum sac = nsh.getAccumSetDefault();
		sac.addCount(valueId);
		
		Accum vs = nsh.addCount(vid, valueId);	
		if (vs == null) {
			// make this variable to improve mem/performance for the set
			vs = getAccumulator();
			vs.setVectorCode(vid);
			vs.addCount(valueId);
			nsh.put(vs);
		}
	}
	
	/**
	 * get an accumulator by type
	 * @return new accumulator
	 */
	Accum getAccumulator() {
		if (accumulatorType == AccumType.Hash) return new AccumIntHm();
		if (accumulatorType == AccumType.HashCrt) return new AccumIntHmCrt();
		if (accumulatorType == AccumType.HashLong) return new AccumLongHm();
		if (accumulatorType == AccumType.Long) return new AccumLongHm();
		if (accumulatorType == AccumType.Boolean) return new AccumBool();		
		return new AccumInt();
	}

	/**
	 * Reset the Dataplan info
	 */
	void reset() {
		this.defAccum.clear();
		if (this.scratchPad != null) {
			String s = (String)scratchPad.get("dp-description");
			this.scratchPad.clear();
			this.scratchPad.put("dp-description", s);
		}
		this.clearCfgNS();
		this.clearPredictAnswerAccum();
		this.clearRecallAnswerAccum();
	}

	/**
	 * Print data plane data
	 * @param showAccum if true show all the accumulators
	 */
	public void printData(boolean showAccum) {
		printData(showAccum, -1);
	}
	
	/**
	 * Print data plane data
	 * @param showAccum if true show all the accumulators
	 * @param setNumber numberSet to show, -1 for all
	 */
	public void printData(boolean showAccum, int setNumber) {
		String wb = this.getCfgWindowSize()+":";
		if (isCfgFameFocusNone()) wb += "-";
		else wb += this.getCfgBefore();
		
		System.out.println("  DP["+this.getDimensionTag()+"/"+this.getTag()+"]["+wb+"]"
				+ " ns["+this.getNSCount()+" /"+this.getNSTurnedOnCount()+"]");
		
		for (int i = 0;i<getNSCount();i++) {
			MLNumberSetHash nsh = getNSHash(i);
			int cnt = nsh.getVectorCount();
			// value info
			int max = nsh.getValueCountMax();
			int min = nsh.getValueCountMin();
			int avg = nsh.getValueCountAvg();
			
			System.out.print("  NS["+String.format("%3d", i)+"]["+getNSFormatString(i)+"] v["+String.format("%8d", cnt)+"]w["+String.format("%.16f", getCfgNSWeight(i))+"]");				
			System.out.println(" val["+min+"/"+avg+"/"+max+"]");		
			if (nsh.isTurnedOff()) {
				System.out.println(" => turned off");				
				continue;
			}
			System.out.println("");		
			if (showAccum && (setNumber == -1 || setNumber == i)) nsh.printData();
		}
	}
	
	
	//
	// show Set probabilities/likeliness
	//
	private void showSets(boolean showEach, boolean showVals) {
		int ffset = getCfgNSFullNumber();
		int idset = getCfgNSIdentityNumber();
		
		for (int i = 0;i<getNSCount();i++) {
			MLNumberSetHash nsh = getNSHash(i);
			if (nsh == null || nsh.isTurnedOff()) continue;

			if (showEach) {
				// Answer info
				double PAnsCrtPer = getPredictAnswerCorrectPercentage(i);
				double RAnsCrtPer = getRecallAnswerCorrectPercentage(i);
				// value info
				int max = nsh.getValueCountMax();
				int min = nsh.getValueCountMin();
				int avg = nsh.getValueCountAvg();
								
				String note = "=";
				if (i == ffset && i == idset) note = "X";
				else if (i == ffset) note = "F";
				else if (i == idset) note = "I";
				
				int cnt = nsh.getVectorCount();
				String vs = "";
				if (showVals) {
					HashMap<Long, Integer> m = nsh.getValueSet();	
					vs = "vals["+m.keySet().size()+"] ["+VegUtil.getValueCountString(this, m)+"]";
				//} else {
					//vs = "totSum["+nsh.getTotalSum()+"]";
				}
				System.out.println("      SET["+String.format("%3d", i)+"]"
						+ " accum["+String.format("%8d", cnt)+"]w["+String.format("%.16f", getCfgNSWeight(i))+"]"
						+ "  ="+note+">  " +getNSFormatString(i)
						+ "  R["+fper(RAnsCrtPer)+"]"
						+ "  P["+fper(PAnsCrtPer)+"]"
						+ "  v["+String.format("%7d", nsh.getVectorCount())+"]val["+min+"/"+avg+"/"+max+"]"
						+ "  " + vs
						);

				// tag by window size ? this can be a NumberSets table - with windowSize this set is introduced in				
				//System.out.println("      R Bal[min: "+Gtil.fmtDouble20(ssd.minRightBalance)+" max: "+Gtil.fmtDouble20(ssd.maxRightBalance)+"] "+Gtil.fmtDouble20(ssd.avgRightBalance)+ " Freq[min: "+ssd.minRightFrequency+" max: "+ssd.maxRightFrequency+"] "+ssd.avgRightFrequency);
				//System.out.println("      W Bal[min: "+Gtil.fmtDouble20(ssd.minWrongBalance)+" max: "+Gtil.fmtDouble20(ssd.maxWrongBalance)+"] "+Gtil.fmtDouble20(ssd.avgWrongBalance)+ " Freq[min: "+ssd.minWrongFrequency+" max: "+ssd.maxWrongFrequency+"] "+ssd.avgWrongFrequency);
				//if (sac.getTotal() > 0) System.out.println("       SAC:  " + sac.getDistString());
				//if (slac.getTotal() > 0)System.out.println("       SLAC: " + slac.getDistString());
			}
		}
		

		String Rrp = getRecallRecallCount()
				+"/"+getRecallRecallPredictCount()
				+"/"+getRecallPredictRelateCount()
				+"/"+getRecallPredictCount();
		//String Rrpp = getRecallRecallCrtCount()+"/"+getRecallRecallPredictCrtCount()+"/"+getRecallPredictCrtCount()+"/"+getRecallDefaultCrtCount();
		String Rrppp = fper(getRecallRecallCrtPercentage())
				+"/"+fper(getRecallRecallPredictCrtPercentage())
				+"/"+fper(getRecallPredictRelateCrtPercentage())
				+"/"+fper(getRecallPredictCrtPercentage());
		String Prp = getPredictRecallCount()
				+"/"+getPredictRecallPredictCount()
				+"/"+getPredictPredictRelateCount()
				+"/"+getPredictPredictCount()
				+"/"+getPredictUnknownCount()
				+"/"+getPredictDefaultCount();
		//String Prpp = getPredictRecallCrtCount()+"/"+getPredictRecallPredictCrtCount()+"/"+getPredictPredictCrtCount()+"/"+getPredictDefaultCrtCount();
		String Prppp = fper(getPredictRecallCrtPercentage())
				+"/"+fper(getPredictRecallPredictCrtPercentage())
				+"/"+fper(getPredictPredictRelateCrtPercentage())
				+"/"+fper(getPredictPredictCrtPercentage())
				+"/"+fper(getPredictUnknownCrtPercentage())
				+"/"+fper(getPredictDefaultCrtPercentage());
			

		//System.out.println("      SETS["+actSets+"] Rc[" + rc+"]["+Rrp+"]["+Rrppp+"] Pc["+pc+"]["+Prp+"]["+Prppp+"]");
		System.out.println("      WIN["+getNSFormatString(getCfgNSFullNumber())+"]  R["+Rrp+"]["+Rrppp+"]  P["+Prp+"]["+Prppp+"]");
		if (getPredictCount() > 0) {
			System.out.println("        "
				+ "R[= "+(getRecallCount()-getRecallCrtCount())+"]  "
				+ "r["+fab(getPredictRecallCrtCount(), getPredictRecallCount())+"]"
				+ "rp["+fab(getPredictRecallPredictCrtCount(), getPredictRecallPredictCount())+"]"
				+ "pr["+fab(getPredictPredictRelateCrtCount(), getPredictPredictRelateCount())+"]"
				+ "p["+fab(getPredictPredictCrtCount(), getPredictPredictCount())+"]"
				+ "pu["+fab(getPredictUnknownCrtCount(), getPredictUnknownCount())+"]"
				+ "d["+fab(getPredictDefaultCrtCount(), getPredictDefaultCount())+"]"
				+ " tot["+fab(getPredictCrtCount(), getPredictCount())+" = "+(getPredictCount()-getPredictCrtCount())+"]");
		}
	}
	private static String fper(double p) {
		if (p == 0) return "-";
		return String.format("%.2f", p)+"%";
	}
	private static String fab(int a, int b) {
		if (a == 0 && b == 0) return "-";
		return a + " / " + b;
	}
	
	/**
	 * Print the dataplane information
	 */
	public void print() {
		print(false);
	}
	
	/**
	 * print the dataplane information 
	 * @param sets if true print full list of numberSets with details
	 */
	public void print(boolean sets) {		
		print(sets, false);
	}
	
	/**
	 * print the dataplane information 
	 * @param sets if true print full list of numberSets with details
	 * @param showVals if true show all data values
	 */
	public void print(boolean sets, boolean showVals) {		
		VDataPlane dpix = this;
		Accum dac = dpix.getAccumDefault();
		
		int AccCnt = dpix.getAccumCount();
		int vCnt = dpix.getVectorCount();
	
		// answer correct percent
		double PAnsCrtPer = dpix.getPredictAnswerCorrectPercentage();
		double RAnsCrtPer = dpix.getRecallAnswerCorrectPercentage();	
		String oth = "";
		if (dpix.getCfgCallout() != null) oth += "COUT ";
		if (!dpix.isNoEmptyElements()) oth += "EMT ";
		oth = oth.trim();
		if (dpix.getFramer() != null && dpix.getFramer() != getVegML().getCfgFramerDefault()) oth += "{"+dpix.getFramerName()+"} ";

		String dps = "DP";
		if (dpix.isSolid()) dps = "DP_SLD";
		String trn = "(-)";
		if (dpix.isState(DPState.Trained)) trn = "(t)";
		else if (dpix.isState(DPState.Tuned)) trn = "(T)";
		else if (dpix.isState(DPState.Ready)) trn = "";
		dps += trn;
		String ti = "";
		if (dpix.getCfgInputDataTiers() > 1) {
			ti = "int["+dpix.getCfgInputDataTiers()+"]";
		}
		String wb = this.getCfgWindowSize()+":";
		if (isCfgFameFocusNone()) wb += "-";
		else wb += this.getCfgBefore();
		System.out.println("  "+dps+"["+dpix.getDimensionTag()+"/"+dpix.getTag()+"]["+wb+"]"
				+ " ns["+dpix.getNSCount()+"/"+dpix.getNSTurnedOnCount()+"]nv["+dpix.getMappedVectorCount()+"]"+ti
				+ " in["+dac.getTotal()+"]vals["+dac.getValueCount()+"/"+dpix.getCfgDataWidth()+"]"
				+ " accum["+AccCnt+"]vect["+vCnt+"]w["+dpix.getCfgNSWeight()+"]"
			//	+ "avp["+String.format("%.6f", probAcc)+"]"
				+ " o["+oth+"]"
				//+ "vals["+AccValCnt+"]once["+AccOnce+"]"
				+ " R["+fper(RAnsCrtPer)+"]P["+fper(PAnsCrtPer)+"] ");
		//System.out.println("  DP["+dix.getTag()+"]/"+dpix.getTag()+"] in["+dac.getTotal()+"]vals["+dac.getValueCount()+"/"+dpix.getDataValueSetSize()+"] accum["+AccCnt+"]vals["+AccValCnt+"] sap["+sap+"]");
		
		showSets(sets, showVals);
	}
	
}
