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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import vegml.Data.VFileUtil;
import vegml.Data.VDataSetDescriptor.DSDataType;
import vegml.Optimiser.OptTesterMap.tPosition;
import vegml.Optimize.OptMethod;
import vegml.Optimize.OptMethodCarveDep;
import vegml.Optimize.OptMethodCarveDep2;
import vegml.Data.VDataSets;
import vegml.VResultSet.containInfo;
import vegml.VegML.NSWeightBase;
import vegml.VegML.NumberSetType;
import vegml.VegML.PredictionType;
import vegml.VegTest.SaveStats;
import vegml.VegTest.TestMod;
import vegml.VegTest.TestModSet;

/*
	Load next set of ns
	- eval with base
	- eval alone (no weight influence)
	
	If improves or has better answer for specific question. Add it.
	
	Carving:
	Add it, carve, next
	- option: don’t cut list of special answers from it in carve. Or use per accumulator fscores to decide
	
	Logical:
	Add in only better answers as determined by accumulator fscores between new and existing. Drop existing if better
	- this method has the potential to have the smallest and best set of information possible.
	
	Next:
	Can this be done with multiple models at the same time.? Add position by position and check across models?
 */

class Optimiser {
	protected VegML vML;

	protected String dimensionTag; // test
	protected String dataPlaneTag; // test
	protected VDataPlane dp = null; // test
	
	protected String tuneDimensionTag; // tune
	protected String tuneDataPlaneTag; // tune	
	protected VDataPlane tunedp = null; // tune
	// merge optimize with
	protected boolean mixOver = true;
	protected boolean isMerge = false;
	protected boolean isDiffTune = false;
	
	protected String initName;
	protected String initBaseName;
	protected int initNameStep;
	protected PredictionType optType;
	protected int maxBefore = -1;
	protected int maxAfter = -1;
	protected int maxWindow = -1;
	
	int betterTryLimit = 64;
	protected int minVectorCount = 5;

	// test/train data
	protected boolean fullData;
	boolean ctlGroup = true;	// use tuneTest/control
	
	VDataSets dss = null;
	VDataSets trainDs = null;
	VDataSets tuneDs = null;
	VDataSets tuneCtlDs = null;
	VDataSets testDs = null;
	VDataSets fullDs = null;
	List<VDataSets> threadTuneDs = null;	
	List<VDataSets> threadTuneCtlDs = null;	
	List<VDataSets> threadTrainDs = null;

	// new NS info state current incoming set
	protected List<VResultSet> resSingleList = null; // results for current sets in test Single
	protected List<VResultSet> resComboList = null; // results for current sets in test Combined with base
	protected int resNSFullest = -1; // fullest numberSet from generated in test
	// sets in current incoming
	static int MAX_NS_PER_PASS = 32;
	static int MIN_NS_PER_PASS = 16;
	protected int maxNSPerPass = MAX_NS_PER_PASS;

	static int MAX_NS_PER_PASS_PREP = 32;
	static int MIN_NS_PER_PASS_PREP = 24;
	
	protected List<List<List<Integer>>> nsAddSets = null;
	protected HashMap<Integer, Integer> processNSCounts = null;
		
	// current in data plane
	protected VResultSet resCur = null; 	// current base set results
	protected VResultSet resCurCtl = null; // current control set results
	protected List<VResultSet> curSingleList = null; // results singles
	protected List<Integer> curNSInDp = null;
	protected List<List<Integer>> nsUniqeList = null;
	
	// tune
	protected int curPassTotal = 0;
	protected int curPassTotalBest = 0;
	protected int curPassTotalLast = 0;
	protected int curPassCtlBaseBest = 0; // tune set at ctl's best
	// cntl
	protected int curPassCtlBest = 0;
	protected int curPassCtlLast = 0;
	// tult
	protected int curPassTultBest = 0;
	protected int curPassTultLast = 0;
	// test
	protected int curPassTestBest = 0;
	protected int curPassTestLast = 0;
	// tslt
	protected int curPassTsltBest = 0;
	protected int curPassTsltLast = 0;
	
	protected int nsTryTotal = 0;
	protected List<List<Integer>> nsTrySet = null;
	
	// tester mapping for full logical eval
	protected OptTesterMap testMap = null;

	// method of optimization
	protected OptMethod optm = null;

	Optimiser() {
		
	}
	
	//
	// create for a model and goal with method
	//
	Optimiser(VegML vML, String initName, PredictionType optType, boolean useReduction, OptMethod optm) {	
		this(vML.getCfgDefaultDTag(), vML.getCfgDefaultDPTag(), initName, null, null, false, optType, useReduction, optm);
	}
	Optimiser(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag, boolean merge,
			PredictionType optType, boolean useReduction, OptMethod optm) {		
		this(vML.getCfgDefaultDTag(), vML.getCfgDefaultDPTag(), initName, tuneDimensionTag, tuneDataPlaneTag, merge, optType, useReduction, optm);
	}
	Optimiser(String dimensionTag, String dataPlaneTag, String initName, PredictionType optType, boolean useReduction, OptMethod optm) {
		this(dimensionTag, dataPlaneTag, initName, null, null, false, optType, useReduction, optm);
	}
	Optimiser(String dimensionTag, String dataPlaneTag, String initName, String tuneDimensionTag, String tuneDataPlaneTag, boolean merge,
			PredictionType optType, boolean useReduction, OptMethod optm) {
		// test
		this.dimensionTag = dimensionTag;
		this.dataPlaneTag = dataPlaneTag;
		// tune
		this.tuneDimensionTag = tuneDimensionTag;
		this.tuneDataPlaneTag = tuneDataPlaneTag;

		this.isMerge = merge;
		this.isDiffTune = false;
		if (tuneDimensionTag == null) {
			this.tuneDimensionTag = dimensionTag;
			this.tuneDataPlaneTag = dataPlaneTag;			
		} else if (!this.tuneDimensionTag.equals(dimensionTag)) {
			isDiffTune = true;
		}
		
		// break apart name
		this.initName = initName;
		this.initBaseName = initName.substring(0, initName.length()-4);
		this.initNameStep = 0;
		if (Character.isDigit(initBaseName.charAt(initBaseName.length()-1))) {
			int idx = initBaseName.lastIndexOf("-");
			if (idx > 0) {
				this.initNameStep = Integer.parseInt(initBaseName.substring(idx+1, initBaseName.length()));
				this.initBaseName = initName.substring(0, idx);
			}
		}
		
		this.optType = optType;
		this.curNSInDp = new ArrayList<>();
		this.nsUniqeList = new ArrayList<>();
		this.curSingleList = new ArrayList<>();
		this.nsAddSets = new ArrayList<>();
		this.nsTrySet = new ArrayList<>();
		this.processNSCounts = new HashMap<>();
		
		// set carving method..
		this.optm = optm;
		if (this.optm == null) {
			if (this instanceof Optimiser2) {
				this.optm = new OptMethodCarveDep2(this);
				this.optm.setName("carve-dep2"); 	
			} else {
				this.optm = new OptMethodCarveDep(this);
				this.optm.setName("carve-dep"); 
			}
		}
		this.optm.setCfg("useReduction", useReduction);
		
		// reduce for unknowns
		if (optType != null && optType != PredictionType.All) {
			this.optm.setCfg("betterThanMax", 1);
		}
	}
	
	//
	// Accessors for optimizer Methods
	//
	boolean isFullData() {
		return fullData;
	}
	PredictionType getPType() {
		return optType;
	}
	VDataPlane getDP() {
		return dp;
	}
	VDataPlane getTuneDP() {
		return tunedp;
	}
	VResultSet getCurRs() {
		return resCur;
	}
	VResultSet getCurCtlRs() {
		return resCurCtl;
	}
	int getCurTotal() {
		return curPassTotal;
	}
	HashMap<Integer, Integer> getProcessNSCounts() {
		return processNSCounts;
	}
	OptTesterMap getTestMap() {
		return testMap;
	}
	List<VResultSet> getCurRSListSingles() {
		return curSingleList;
	}
	List<List<Integer>> getTryNumberSets() {
		return nsTrySet;
	}
	int getMinVectorCount() {
		return minVectorCount;
	}
	boolean isMixOver() {
		return mixOver;
	}
	boolean isMerge() {
		return isMerge;
	}
	boolean isDiffTune() {
		return isDiffTune;
	}
	
	//
	// initialize with the data set
	//
	boolean init(int maxBefore, int maxAfter, int maxWindow, NSWeightBase nsWBaseType, boolean fullData, VDataSets dss, HashMap<String, Object> cfg) {	
		vML = VegML.load(initName);
		if (vML == null) {
			System.out.println("ERROR["+initName+"] file not found");
			return false;
		}
		dp = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) {
			System.out.println("ERROR["+dimensionTag+"/"+dataPlaneTag+"] dataplane not found");
			return false;
		}
		if (isDiffTune()) {
			// check if base exists
			tunedp = this.vML.getDataPlane(tuneDimensionTag, tuneDataPlaneTag);
			if (tunedp == null) {
				System.out.println("ERROR: test["+tuneDimensionTag+"/"+tuneDataPlaneTag+"] does isn't present");
				return false;
			}
			if (dp.getVectorCount() == 0) {
				System.out.println("ERROR: test["+dp.getDimensionTag()+"/"+dp.getTag()+"] is empty");
				return false;
			}
			System.out.println("Miser Init test["+dimensionTag+"/"+dataPlaneTag+"]tune["+tuneDimensionTag+"/"+tuneDataPlaneTag+"]ns["+tunedp.getNSCount()+"]["+optType+"] <== ["+initName+"]");
		} else {
			tunedp = dp;
			System.out.println("Miser Init["+dimensionTag+"/"+dataPlaneTag+"]ns["+tunedp.getNSCount()+"]["+optType+"] <== ["+initName+"]");
		}

		// override
		if (this.optType == PredictionType.AnyUnknown || this.optType == PredictionType.PredictUnknown) {
			cfg.put("fullAlways", false);
			//ctlGroup = false;
		}
		
		
		////////////////////////////////////////////////////
		// setup data sets standard and theaded
		dss.genVSets();
		this.dss = dss;		
		this.testDs = dss.getTestDataSets();
		this.trainDs = dss.getTrainDataSets();
		if (!ctlGroup) this.tuneDs = dss.getTuneDataSets();		
		else {
			this.tuneDs = dss.getTuneTestDataSets();
			this.tuneCtlDs = dss.getTuneControlDataSets();
		}
		this.fullData = fullData;
		this.fullDs = tuneDs;
		if (this.fullData) {
//			if (!ctlGroup) this.fullDs = dss.getTrainAndTuneDataSets();
//			else this.fullDs = dss.getTrainAndTuneTestDataSets();
		}
		
		// Threads and data subsets; thread count based on size	
		int setCount = fullDs.size()/30;
		if (setCount > 16) setCount = 16;
		
		this.threadTuneDs = fullDs.getDataSetSplit(setCount);		
		this.threadTrainDs = trainDs.getDataSetSplit(setCount);
		if (ctlGroup) {
			this.threadTuneCtlDs = tuneCtlDs.getDataSetSplit(setCount/3);					
		}
		
		// set some info 
		PredictionType pt = optType;
		if (pt == null) pt = PredictionType.All;
		tunedp.setCfgScratch("optType", pt.ordinal());
		tunedp.setCfgScratch("optTuneType", pt);
		tunedp.setCfgScratch("optMethod", "miser"); 
		tunedp.setCfgScratch("optFullData", fullData);
		tunedp.setCfgScratch("optTrainDataSize", dss.getTrainCount()); // count
		tunedp.setCfgScratch("optTuneDataSize", fullDs.size()); // count
		String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm.ss").format(new Date());
		tunedp.setCfgScratch("optTime", timeStamp); // count
		tunedp.setCfgScratch("optReduction", optm.getCfgBool("useReduction"));
		tunedp.setCfgScratch("optDownWeight", optm.getCfgDouble("downWeight"));
			
		int tokens = fullDs.getTokenCount();
		this.testMap = new OptTesterMap(tokens);
		
		// update optm with dataSet / dp / etc
		optm.reset(this);
		
		OptimizerStatistical.noEndThreads = true;	// don't stop threads after each stat
		OptimizerStatistical.allowFullest = false;	// allow carving of fullest -> poor results
		OptimizerStatistical.allowIdentity = false;	// allow carving of identity -> poor results
		OptimizerMerge.print = false;	// silence is nice
		OptimizerStatistical.mergeValue = 1;	
		OptimizerStatistical.autoMergeValue = false;	
		
		
		if (cfg != null && cfg.keySet().size() > 0) {
			this.optm.setConfig(cfg);
		}
		// init merge if it is
		if (!initMerge()) return false;
		
		if (optm.getCfg("betterTryLimit") != null) {
			betterTryLimit = optm.getCfgInt("betterTryLimit");
		}
		if (optm.getCfg("MAX_NS_PER_PASS") != null) {
			MAX_NS_PER_PASS = optm.getCfgInt("MAX_NS_PER_PASS");
		}	
		if (optm.getCfg("MIN_NS_PER_PASS") != null) {
			MIN_NS_PER_PASS = optm.getCfgInt("MIN_NS_PER_PASS");
		}
		
		// set default result callout
		dp.setCfgCalloutToDefault();


		// if limits
		this.maxBefore = maxBefore;
		this.maxAfter = maxAfter;
		if (maxBefore >= 0 && maxAfter >= 0 && maxWindow <= 0) maxWindow = maxAfter+maxBefore;
		this.maxWindow = maxWindow;
				
		//////////////////////////////////
		// remove all but identity
		tunedp.setCfgWindowSize(1); 
		if (nsWBaseType != null) tunedp.setCfgNSWeight(nsWBaseType);
		tunedp.setCfgScratch("posLastAdd", 0);
		tunedp.setCfgSaveChildVids(true);
		nsTryTotal = 1;
		nsTrySet.add(tunedp.getNS(0));
		System.out.println("");
				
		if (tunedp.getAccumCount() == 0) {
			// train if not already
			System.out.print("TRAIN_IDENTITY["+tunedp.getNSTurnedOnCount()+"] =>");
			VResultSet rc = MLThreadUtil.runTrainDataSets(tunedp, this.threadTrainDs);
			System.out.println(" TRAINED["+rc.total+"] Time["+rc.getDurration()+"]");		
		}
		
		//////////////////////////////////
		// logical first
		VegTune.logicalDataDefinitionReduction(vML, tuneDimensionTag, tuneDataPlaneTag, optType);

		//////////////////////////////////
		// Baseline starting point -> just the identity
		finalEval();
		System.out.println("");
		
		//////////////////////////////////
		// unknown extend ?
		if (optType == PredictionType.PredictUnknown || optType == PredictionType.AnyUnknown) {
			if (optm.getCfgBool("extendUnknowns")) {
				// identity
				//dp.clearNSData(dp.getCfgNSIdentityNumber()); // can't optimize if this
				// removing in-frequent words from identity that are of non-closed sets can expand the test range, providing better options
				int scnt = tunedp.getNSAccumulatorCount(dp.getCfgNSIdentityNumber());
				tunedp.removeAllNSAccum(tunedp.getCfgNSIdentityNumber(), 1, false);
				int ecnt = tunedp.getNSAccumulatorCount(dp.getCfgNSIdentityNumber());
				System.out.println("UNKNOWN IdentityNS["+tunedp.getCfgNSIdentityNumber()+"] reduction["+scnt+" => "+ecnt+"] ");
				finalEval();
				System.out.println("");
			}
		}
		
		return true;
	}
	
	//
	// init merge of optimizing over OR under 
	//
	protected boolean initMerge() {
		String ty = this.optm.getCfgString("mixWithType");
		if (ty != null && !ty.equalsIgnoreCase("over")) mixOver = false; // why?
		else mixOver = true;		
		
		return true;
	}
	
	//
	// called with data sets
	// returns file name of best
	//
	public String miser(int maxBefore, int maxAfter, int maxWindow, NSWeightBase nsWBaseType, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {	
		
		if (!init(maxBefore, maxAfter, maxWindow, nsWBaseType, fullData, dss, cfg)) return this.initName;

		
		// do other stuff
		List<VResultSet> rs = new ArrayList<>();
		rs.add(resCur);
		this.testMap.mapRaw(this, rs, rs, resCur);
		
		if (optm.getCfgBool("applyPrepVectMap")) {
			this.testMap.mapRawNSVectors(tunedp, rs);
		} 
		resCur.clearForRetain();
		this.testMap.mapPreped(this, rs, rs, resCur);
		this.testMap.mapCurrentAndSet(this, false);

		// check if identity only should be used
		if (resCur.getPassPercent(null) > 50 && this.optType != PredictionType.AnyUnknown && this.optType != PredictionType.PredictUnknown) {
			tunedp.setCfgIdentityOnly(true);
			System.out.println("IdentityOnly[TRUE]");		
		} else {
			tunedp.setCfgIdentityOnly(false);			
			System.out.println("IdentityOnly[FALSE]");		
		}

		// correct probability method
		if (tunedp.getCfgInputDataType() != DSDataType.Char) {
			// FIXME should be average
			// methods not work for DEP and carve?
			//dp.setCfgProbMethod(ProbMethod.AverageIfNotRecall);
			//dp.setCfgProbMethod(ProbMethod.AverageRecall);
			
		}
				
		//////////////////////////////////
		// determine nsBaseWieght
		if (nsWBaseType == null && tunedp.getCfgNSWeight() != NSWeightBase.None) {
			NSWeightBase dnsw = vML.getCfgDefaultNSWeight();
			if (dnsw == null) dnsw = decideBestWeightBase();
			else System.out.println("NSWeightBase => use["+dnsw+"]");	
			tunedp.setCfgNSWeight(dnsw);
			// set for default in vML
			vML.setCfgDefaultNSWeight(dnsw);
		} else {
			System.out.println("NSWeightBase => use["+tunedp.getCfgNSWeight()+"]");	
		}
		
		int bestWindow = tunedp.getCfgWindowSize();
		int bestWindowPass = curPassTotal;
		int cycleWindow = bestWindow;
		int cycleNSCount = tunedp.getNSCount();
		int cyclePassTotal = curPassTotal;
		int cyclePassTotalCtl = 0;
		if (resCurCtl != null) resCurCtl.getPassTotal(optType);
	
		
		// save here. let the step = window size
		int step = 1;	
		save(step);
		
		TestModSet nstests = new TestModSet();
		int noAddCnt = 0;
		
		// do it until it don't work no more
		while (true) {
			int snscnt = tunedp.getNSCount();
			//System.out.println("");
			if (this.isMerge()) System.out.print("STEP["+(step+1)+"]["+dp.getIDString()+"]["+tunedp.getIDString()+"]ns["+snscnt+"]  =>  ");
			else System.out.print("STEP["+(step+1)+"]["+tunedp.getIDString()+"]ns["+snscnt+"]  =>  ");
						
			// prepare next numberSets
			List<Integer> remNsList = getNextSetResults();
			if (remNsList == null) {
				System.out.println(" DONE ");
				break; // if no more space
			}
			
			boolean firstPass = false;	
			// if moved to new super-cycle
			if (cycleWindow != tunedp.getCfgWindowSize()) {
				cycleNSCount = snscnt;
				cyclePassTotal = curPassTotal;
				firstPass = false;
				if (resCurCtl != null) {
					cyclePassTotalCtl = resCurCtl.getPassTotal(optType);
				}
			}
			
			// update mergeValue for merge before
			if (this.isMerge()) {
				getMergeValue(this.dp, remNsList, true);
			} 

			step++;
			boolean SetChanged = true;
			
			//////////////////////////////////////
			// Start Cycle
			//////////////////////////////////////
			optm.processCycleStart(this, remNsList);
			
			// optimize set being added; logical & statistical			
			for (int i = 0;remNsList.size() > 0;i++) {
				// get ordered list from nsCurList
				assessNumberSets(remNsList, SetChanged, firstPass);
				firstPass = false;
				
				// assess by test and sort resComboList/resSingleList
				if (SetChanged) assessResponseMapAndSort();
				
				// get new base
				curPassTotal = resCur.getPassTotal(optType);
				
				// if additional
				int betterThanScore = curPassTotal;				
				int bmx = curPassTotal+optm.getCfgInt("betterThanMax");
				if (optm.getCfgBool("useBetterThan")) {
					// TODO: find a logical limiter
					betterThanScore += (tunedp.getCfgWindowSize()-1);
					if (betterThanScore > bmx) betterThanScore = bmx;
				} else {
					// flat add
					betterThanScore = bmx;
				}
				
				// limit baseline possible updside required to eval
				int betterThanPossiblePrep = optm.getCfgInt("betterThanPossiblePrep");

				// sort using combind list
				VResultSet rsc = resComboList.get(0);			
				int ns = (int)rsc.val2;
				int sidx = findRsInList(resSingleList, ns);
				VResultSet rss = resSingleList.get(sidx);
				resComboList.remove(0);
				resSingleList.remove(sidx);

				// get position Record map
				testInfo tinf = this.testMap.getInfoMap(tunedp.getNS(ns));
				
				// remove from lists
				remNsList.remove(remNsList.indexOf(ns));
				List<Integer> numberSet = tunedp.getNS(ns);
				List<List<Integer[]>> numberSetTier = tunedp.getNSTier(ns);
				
				int cpassTotal = rsc.getPassTotal(optType);
				//int passSingleTotal = rss.getPassSingleTotal(optType);
				
				// Show NS info line
				System.out.print("  "+String.format("%3d", i)+") ["+String.format("%3d", ns)+"]["+tunedp.getNSFormatString(ns)+"]");								
				System.out.print(
						"  psb["+String.format("%4d", tinf.prpsbm)+"]win["+String.format("%4d", tinf.pcrtOnly)+"]"	// correct only / combo correct only											
						+ "S["+perc(rss.getPassSinglePercent())+"]  pass["+String.format("%6d", cpassTotal)+"] =");
			
				boolean keepNS = true, reload = true;
				int drop_ns = -1, cpass_drop = 0;	
				int vc = tunedp.getVectorCount(ns);
// FIXME this should be based on results from last position pass
// betterThanPossiblePrep
				
				if (vc <= getMinVectorCount()) {
					// min vector count
					System.out.println("> NOT_ENOUGH_VECTORS  =>      max["+vc+" <= "+getMinVectorCount()+"]");
					keepNS = reload = false;
					// turn off this numberset
					tunedp.setCfgNSTurnedOff(ns, true);
				} else if ((tinf.prpsbm + tinf.pcrtOnly) < betterThanPossiblePrep && !(optm.getCfgBool("fullAlways") && ns == tunedp.getCfgNSFullNumber())) {
			//	if ((tinf.prpsb + tinf.pcrtOnly) < betterThanPossiblePrep && !(optm.getCfgBool("fullAlways") && ns == tunedp.getCfgNSFullestNumber())) {
					// not enough possibilities to improve
					System.out.println("> NOT_ENOUGH_POSSIBLE  =>      max["+(tinf.prpsb + tinf.pcrtOnly)+"/"+(tinf.prpsbm + tinf.pcrtOnly)+"]");
				
					keepNS = reload = false;
					// turn off this numberset
					tunedp.setCfgNSTurnedOff(ns, true);
				} else {
				
					// combine with current and see the results
					// multiple algs could be used here including dependency/vote/carve
					//
	
					// create eval test for combined AND without AND with but without each other numberSet (replace check)
					tunedp.setCfgNSTurnedOff(ns, false);
					nstests.clear();
					// add complete (must be first)
					TestMod t = new TestMod(tunedp.getCfgNSWeightsBase());	
					nstests.add(tunedp, t.completeNSW());	
					
					t = new TestMod(tunedp.getCfgNSWeightsBase());	
					t.nsWeights[ns] = 0;
					t.userTag = ns;
					nstests.add(tunedp, t.completeNSW());				
					// test with each current NS removed to see if any could be dropped	 / replaced	
					for (int x=0;x<curNSInDp.size();x++) {
						int tns = curNSInDp.get(x);
						if (tns == ns || tns == tunedp.getCfgNSFullNumber() || tns == tunedp.getCfgNSIdentityNumber()) continue;
						t = new TestMod(tunedp.getCfgNSWeightsBase());	
						t.nsWeights[tns] = 0; // drop this one
						t.userTag = tns;
						nstests.add(tunedp, t.completeNSW());	
					}
					// just this one
					t = new TestMod(tunedp.getNSCount());	
					t.nsWeights[ns] = tunedp.getCfgNSWeight(ns);
					t.userTag = ns;
					nstests.add(tunedp, t.completeNSW());	
	 				
					// save to recover if not a match
					save(step);
					
					//////////////////////////////////////////////////
					// Apply the methods
					List<VResultSet> rsl = optm.processNS(this, ns, nstests);
					
					// vectors
					int vectCnt = tunedp.getVectorCount(ns);
					System.out.print("v["+String.format("%6d", vectCnt)+"]  =>  ");
					
					// test combined AND without
					if (rsl == null) {
						rsl = MLThreadUtil.runTestPredictModify(dp, new ArrayList<>(), nstests, threadTuneDs, true, true);
					}

					cpassTotal = rsl.get(0).getPassTotal(optType); // together
					int cwpassTotal = rsl.get(1).getPassTotal(optType); // without
					VResultSet rsx = rsl.remove(rsl.size()-1);
					rsx.val2 = ns;
					
					// check the full set of replace opts
					if (ns != tunedp.getCfgNSFullNumber()) {
						List<TestMod> tests = nstests.get(tunedp);
						for (int x=2;x<rsl.size();x++) {
							int tns = (int)tests.get(x).userTag;
							if (tns == ns || tns == tunedp.getCfgNSFullNumber() || tns == tunedp.getCfgNSIdentityNumber()) continue;						
							int cpass = rsl.get(x).getPassTotal(optType);
							if (cpass <= cpass_drop) continue;
							if (cpass > cpassTotal && cpass > curPassTotal) {
							//if (cpass >= cpassTotal && cpass > curPassTotal) {
								drop_ns = tns;
								cpass_drop = cpass;
							}
						}
					}
						
					// decide if changes for prior content are needed (even if carving)
					// the best answer is desired for each problem
					
					// turn this NS back off
					tunedp.setCfgNSTurnedOff(ns, true);
					
					// final map (only if keeping??
					// FIXME each single update probabilities					
					int diff = this.testMap.mapFinal(this, rsx, rsl.get(0));
					if (diff > 0) System.out.print("D["+String.format("%5d", diff)+"]["+perc(rsx.getPassSinglePercent())+"]");

					///////////////////////////////////////////////////////////////////////
					// DECIDE
					// do we keep it?
					boolean vectUpdate = false;
					if (optm.getCfgBool("fullAlways") && ns == tunedp.getCfgNSFullNumber()) {
						// add the fullest for each new set?
						if (cpassTotal < curPassTotal) System.out.print("    full");
						else System.out.print("    FULL");
					} else if ((cpassTotal > betterThanScore && cpassTotal > cwpassTotal)) {
						// yes
						System.out.print("  BETTER");	
					} else if (cpass_drop > betterThanScore) {
						// replace on
						System.out.print("  Better");
					//} else if (cwpassTotal > curPassTotal && cpassTotal > cwpassTotal && (nsTryTotal < betterTryLimit || tinf.pcrtOnly > 0)) {
					} else if (cwpassTotal > curPassTotal && cpassTotal > cwpassTotal && nsTryTotal < betterTryLimit) {
						// yes but	
						System.out.print("  better");
					} else if (cpassTotal > curPassTotal && tinf.pcrtOnly > 0 && nsTryTotal < betterTryLimit) {
						// check if still any uniqueness
						int remain = nsRetainOnlyUnique(rss, ns, false);
						if (remain == 0) {
							System.out.print("    nope");
							keepNS = false;
						} else {
							System.out.print("  unique");
							nsUniqeList.clear();
							nsUniqeList.add(tunedp.getNS(ns));
						}
					} else if (false && tinf.pcrtOnly > 0) {
						// retain only the unique; check that this unique is still unique
						int remain = nsRetainOnlyUnique(rss, ns, true);
						if (remain > 0) {
							// run the nums..
							rsl = MLThreadUtil.runTestPredictModify(tunedp, new ArrayList<>(), nstests, threadTuneDs, true, false);		
							cpassTotal = rsl.get(0).getPassTotal(optType);
							cwpassTotal = rsl.get(1).getPassTotal(optType);
			        		vectCnt = tunedp.getVectorCount(ns);
							vectUpdate = true;		
						// FIXME seems often this cpassTotal does not match reality: MINO[223821 > 223815]+6     v[3] 
							if (cpassTotal > curPassTotal && vectCnt > 0) System.out.print("    MINO"); // keep it
							else remain = 0;
						}
						if (remain <= 0) {
							// not enought to help
							System.out.print("    NOPE");	
							keepNS = false;
						}
					} else {
						// does it have a unique answer or improve results?
						if (cwpassTotal > curPassTotal) System.out.print("    nope");
						else if (cpassTotal > curPassTotal) System.out.print("    nope");
						else System.out.print("    NOPE");					
						keepNS = false;
					}
					// have child?
		        	//boolean lhavec = hasNumberSetChild(dp, tunedp.getNS(ns), tunedp.getCfgScratchBool("posLastAddAfter"));
		        	////if(lhavec) System.out.print("c");
		        	//else System.out.print("_");
		        	
		        	if (cpassTotal == curPassTotal) System.out.print("["+cpassTotal+" = "+curPassTotal+"]");
		        	else if (cpassTotal < curPassTotal) System.out.print("["+cpassTotal+" < "+curPassTotal+"]");					
		        	else System.out.print("["+cpassTotal+" > "+curPassTotal+"]");	
		        	
					String tag = "     ";
					if (curPassTotal < cpassTotal) tag = "+"+String.format("%-4d", (cpassTotal-curPassTotal));
					else if (curPassTotal > cpassTotal) tag = "-"+String.format("%-4d", (curPassTotal-cpassTotal));
		        	System.out.print(tag);
		        	
					// vectors
		        	if (vectUpdate) {
		        		System.out.println("  v["+vectCnt+"]");
		        	} else {
		        		System.out.println("");
		        	}
				}

				SetChanged = false;
				if (keepNS) {
					SetChanged = true;
					
					// turn on NumberSet
					tunedp.setCfgNSTurnedOff(ns, false);
					curNSInDp.add(ns);
					curSingleList.add(rss); // add to current set

					// add count to each ns for processing
					for (int nsi=0;nsi<tunedp.getNSCount();nsi++) {
						if (!tunedp.isCfgNSTurnedOff(nsi)) {
							int nid = tunedp.getNSHashId(nsi);
							Integer cc = processNSCounts.get(nid);
							if (cc == null) processNSCounts.put(nid, 1);
							else processNSCounts.put(nid, cc+1);
						}
					}
					
					if (drop_ns >= 0) {
						// drop if this is a replace
						if (cpass_drop == cpassTotal) System.out.println("  DROP_replace["+drop_ns+"]["+tunedp.getNSFormatString(drop_ns)+"]["+cpassTotal+" == "+cpass_drop+"]");
						else System.out.println("  DROP_REPLACE["+drop_ns+"]["+tunedp.getNSFormatString(drop_ns)+"]["+cpass_drop+" > "+cpassTotal+"]");
						cpassTotal = cpass_drop;
						// remove the ns
						removeNS(drop_ns, remNsList);
					}
				} else {
					// rollback
					if (reload) load();
					// remove this ns
					removeNS(ns, remNsList);	
				}
				// remap these if needed
				this.testMap.mapCurrentAndSet(this, SetChanged);
				curPassTotal = cpassTotal;
				nsTryTotal++;
				nsTrySet.add(numberSet);

			}

			//////////////////////////////////////
			// End Sub-Cycle
			//////////////////////////////////////
			if (nsAddSets.size() == 0) {
				// update mergeValue for merge after
				if (this.isMerge()) {
					// FIXME			
					//getMergeValue(this.dp, null);
				} 
				if (optm.getCfgBool("mergeValueOnly")) {
					VDataPlane bdp = vML.getDataPlane(optm.getCfgString("mergeSetInfoDefaultD"), optm.getCfgString("mergeSetInfoDefaultDP"));	
					if (bdp != null) {
						getMergeValue(bdp, null, false);
					} else {
						getMergeValue(this.dp, null, false);
					}
				}
				
				// do processing
				optm.processCycleEnd(this);
			}

			// clean up numberset
			// turn off any numberSets without accumulators		
			System.out.println("  CHECK_VECTS["+tunedp.getNSCount()+"]v["+tunedp.getVectorCount()+"]");
			for (int x=0;x<tunedp.getNSCount();x++) {
				if (tunedp.isCfgNSTurnedOff(x)) continue;
				if (x == tunedp.getCfgNSIdentityNumber()) continue;
				if (x == tunedp.getCfgNSFullNumber()) continue;
				int acCnt = tunedp.getNSAccumulatorCount(x);
				if (acCnt <= 0) {
					System.out.println("    TURN OFF NS["+x+"]["+tunedp.getNSFormatString(x)+"]  v["+tunedp.getVectorCount(x)+"]");
					tunedp.setCfgNSTurnedOff(x, true);
					tunedp.clearNSData(x);
					tunedp.setCfgNSWeight(x, 0);
				}
			}
			// remove turned off numberSets
			tunedp.removeCfgNSTurnedOff();
			
			
			// save the final
			save(step);
			curPassTotal = resCur.getPassTotal(optType);

			// Check done at end of cycle
			if (nsAddSets.size() == 0) {
				//////////////////////////////////////
				// End Cycle
				//////////////////////////////////////
				finalEval();
				
				// save solid and json
				saveJSON();
				
				int curPassMax = curPassTotal+optm.getCfgInt("betterThanMax");
				// did it do bad?
				 if (cycleNSCount == tunedp.getNSCount() || cyclePassTotal >= curPassMax) {
						// no new numbersets added for window increase
						noAddCnt++;
						System.out.println("   NO-ADD["+noAddCnt+"] max["+optm.getCfgInt("maxNoAddCnt")+"] ns["+cycleNSCount+" / "+tunedp.getNSCount()+"] win["+cycleWindow+"] pass["+cyclePassTotal+" >= "+curPassMax+"]");	
						if (curPassTotal > bestWindowPass) {
							bestWindow = tunedp.getCfgWindowSize();
							bestWindowPass = curPassTotal;
						}
						if (noAddCnt > optm.getCfgInt("maxNoAddCnt")) break;	
				 } else if (ctlGroup && curPassCtlBest > resCurCtl.getPassTotal(optType)) {
					noAddCnt++;
					System.out.println("   NO-ADD["+noAddCnt+"]DOWN["+(curPassCtlBest-resCurCtl.getPassTotal(optType))+"] max["+optm.getCfgInt("maxNoAddCnt")+"] ns["+cycleNSCount+" / "+tunedp.getNSCount()+"] win["+cycleWindow+"] "
									+ "pass["+cyclePassTotal+" / "+curPassMax+"]ctl["+curPassCtlBest +" / "+ resCurCtl.getPassTotal(optType)+"]");	
					if (curPassTotal > bestWindowPass) {
						if ((curPassTotal+resCurCtl.getPassTotal(optType)) > (curPassCtlBaseBest+curPassCtlBest)) {
							// if total now is better than total then
							bestWindow = tunedp.getCfgWindowSize();
							bestWindowPass = curPassTotal;
						}
					}
					if (noAddCnt > optm.getCfgInt("maxNoAddCnt")) break;			
				} else if (ctlGroup && curPassCtlBest == resCurCtl.getPassTotal(optType)) {
					// stand
					if (curPassTotal > bestWindowPass) {
						bestWindow = tunedp.getCfgWindowSize();
						bestWindowPass = curPassTotal;
					}
				} else {
					noAddCnt = 0;
					bestWindow = tunedp.getCfgWindowSize();
					bestWindowPass = curPassTotal;
				}
				if (bestWindowPass == curPassTotal && bestWindow == tunedp.getCfgWindowSize()) System.out.print("   STAT_BEST");
				else if (noAddCnt > 0) System.out.print("   STAT_MISS");
				else System.out.print("   STAT");
				System.out.println("["+noAddCnt+"]ns["+tunedp.getNSCount()+"]v["+tunedp.getVectorCount()+"] win["+tunedp.getCfgWindowSize()+"] "
						+ " best["+bestWindowPass+"]win["+bestWindow+"]"
						+ " pass["+curPassTotal+"]ctl["+resCurCtl.getPassTotal(optType)+ " >= " +curPassCtlBest+"]");
				System.out.println("");

				cycleWindow = tunedp.getCfgWindowSize();
			}

		}
		
		
		//////////////////////////////////////
		// Its Over
		//////////////////////////////////////
		System.out.println("");
		
		// stop threads
		MLThreadUtil.endThreads();
		
		// save current
		step++;
		save(step);
		
		// remove any saved models after the best
		if (tunedp.getCfgWindowSize() > bestWindow) {
			for (int i=tunedp.getCfgWindowSize();i>bestWindow;i--) VFileUtil.delFile(getFileName(i));
		}
		
		String bestVeg = getFileName(bestWindow);
		System.out.println("   Final["+bestVeg+"]");	
		System.out.println("");	
		return bestVeg;
	}
	
	// 
	// do end of cycle update
	//
	protected void finalEval() {	
		if (tunedp.getNSCount() < 1) return;
		
		// tune group results
		System.out.print(" TUNE =>");
		tunedp.setCfgFramerArg(this.tuneDs.getValLLV()); // set on tune DP (it is the same or if merge it is the target)
		resCur = VegTest.testSets(vML, dimensionTag, dataPlaneTag, this.tuneDs, SaveStats.SavePredict, -1, true);
		tunedp.setCfgFramerArg(null);
		resCur.clearForRetain();
		int cp = resCur.getPassTotal(optType);
		String st = resStat(cp, curPassTotalLast, curPassTotalBest);

		System.out.print(" ["+perc(resCur.getPassPercent(optType))+"] pass["+String.format("%6d", cp) + " / "+String.format("%6d", resCur.getFailTotal(optType))+" of " + String.format("%8d", resCur.getTotal(optType))+"]");		
		if (optType != null) System.out.print(" full["+String.format("%6d", cp) + " / "+String.format("%6d", resCur.failTotal)+" of " + String.format("%8d", resCur.total)+"]");
		System.out.println("  -> "+st+"["+curPassTotalBest+"]");	
		curPassTotal = curPassTotalLast = cp;
		if (curPassTotalLast > curPassTotalBest) curPassTotalBest = curPassTotalLast;
				
		VDataPlane bdp = vML.getDataPlane(optm.getCfgString("mergeSetInfoDefaultD"), optm.getCfgString("mergeSetInfoDefaultDP"));	
			
		// control group results
		if (this.ctlGroup) {
			System.out.print(" CNTL =>");			
			tunedp.setCfgFramerArg(this.tuneCtlDs.getValLLV());	
			resCurCtl = VegTest.testSets(vML, dimensionTag, dataPlaneTag, this.tuneCtlDs, SaveStats.NoSave);
			tunedp.setCfgFramerArg(null);
			cp = resCurCtl.getPassTotal(optType);
			st = resStat(cp, curPassCtlLast, curPassCtlBest);
			
			System.out.print(" ["+perc(resCurCtl.getPassPercent(optType))+"] pass["+String.format("%6d", cp) + " / "+String.format("%6d", resCurCtl.getFailTotal(optType))+" of " + String.format("%8d", resCurCtl.getTotal(optType))+"]");
			if (optType != null) System.out.print(" full["+String.format("%6d", cp) + " / "+String.format("%6d", resCurCtl.failTotal)+" of " + String.format("%8d", resCurCtl.total)+"]");
			System.out.println("  -> "+st+"["+curPassCtlBest+"]");	
			curPassCtlLast = cp;			
			if (curPassCtlLast > curPassCtlBest) {
				curPassCtlBest = curPassCtlLast;
				curPassCtlBaseBest = curPassTotal;
			}
			// test from what will be saved dataplane
			if (bdp != null) {
				System.out.print(" CULT =>");			
				VResultSet resXCur = VegTest.testSets(vML, optm.getCfgString("mergeSetInfoDefaultD"), optm.getCfgString("mergeSetInfoDefaultDP"), this.tuneCtlDs, SaveStats.NoSave);
				cp = resXCur.getPassTotal(optType);
				st = resStat(cp, curPassTultLast, curPassTultBest);

				System.out.print(" ["+perc(resXCur.getPassPercent(optType))+"] pass["+String.format("%6d", cp) + " / "+String.format("%6d", resXCur.getFailTotal(optType))+" of " + String.format("%8d", resXCur.getTotal(optType))+"]");
				if (optType != null) System.out.print(" full["+String.format("%6d", cp) + " / "+String.format("%6d", resXCur.failTotal)+" of " + String.format("%8d", resXCur.total)+"]");
				System.out.println("  -> "+st+"["+curPassTultBest+"]");	
				curPassTultLast = cp;
				if (curPassTultLast > curPassTultBest) curPassTultBest = curPassTultLast;
			}
		}
		
		// test set results
		if (this.testDs != null && this.testDs.size() > 0) {
			System.out.print(" TEST =>");			
		//	tunedp.setCfgFramerArg(this.testDs.getValLLV());	
			VResultSet resXCur = VegTest.testSets(vML, dimensionTag, dataPlaneTag, this.testDs, SaveStats.NoSave);
		//	tunedp.setCfgFramerArg(null);
			cp = resXCur.getPassTotal(optType);
			st = resStat(cp, curPassTestLast, curPassTestBest);

			System.out.print(" ["+perc(resXCur.getPassPercent(optType))+"] pass["+String.format("%6d", cp) + " / "+String.format("%6d", resXCur.getFailTotal(optType))+" of " + String.format("%8d", resXCur.getTotal(optType))+"]");
			if (optType != null) System.out.print(" full["+String.format("%6d", cp) + " / "+String.format("%6d", resXCur.failTotal)+" of " + String.format("%8d", resXCur.total)+"]");
			System.out.println("  -> "+st+"["+curPassTestBest+"]");	
			curPassTestLast = cp;			
			if (curPassTestLast > curPassTestBest) curPassTestBest = curPassTestLast;
			
			// test from what will be saved dataplane
			if (bdp != null) {
				System.out.print(" TSLT =>");			
				resXCur = VegTest.testSets(vML, optm.getCfgString("mergeSetInfoDefaultD"), optm.getCfgString("mergeSetInfoDefaultDP"), this.testDs, SaveStats.NoSave);
				cp = resXCur.getPassTotal(optType);
						
				System.out.print(" ["+perc(resXCur.getPassPercent(optType))+"] pass["+String.format("%6d", cp) + " / "+String.format("%6d", resXCur.getFailTotal(optType))+" of " + String.format("%8d", resXCur.getTotal(optType))+"]");
				if (optType != null) System.out.print(" full["+String.format("%6d", cp) + " / "+String.format("%6d", resXCur.failTotal)+" of " + String.format("%8d", resXCur.total)+"]");
				st = resStat(cp, curPassTsltLast, curPassTsltBest);
				System.out.println("  -> "+st+"["+curPassTsltBest+"]");	
				curPassTsltLast = cp;			
				if (curPassTsltLast > curPassTsltBest) curPassTsltBest = curPassTsltLast;
			}
		}
		//System.out.println("");
	}
	private static String resStat(int cp, int last, int best) {
		String st = "SAME";
		if (cp > best) st = "BEST";
		else if (cp > last) st = "MORE";
		else if (cp < last) st = "LESS";
		return st;
	}

	//Create map of each token and solutions per numberSet
	// map base current combo, then each numberSet
	protected void assessResponseMapAndSort() {
		// map them
		this.testMap.mapPreped(this, resSingleList, resComboList, resCur);
		
		// make the sets
		this.testMap.mapAllPrepped(this);	
			
		System.out.println("      tst-eval["+resComboList.size()+"]"
				+ "simp["+testMap.pEasyCnt+"/"+testMap.cEasyCnt+"]"
				+ "easy["+testMap.pSimpleCnt+"/"+testMap.cSimpleCnt+"]"
				+ "hard["+testMap.pHardCnt+"/"+testMap.cHardCnt+"]"
				+ "noCrt["+testMap.pNoAnswerCnt+"/"+testMap.cNoAnswerCnt+"]");
		
		// sort the sets		// this needs to factor in the test info
		int sortType = 0; // 1 smallest / else largest first
		sortList(sortType, resNSFullest, resSingleList);
		sortList(sortType, resNSFullest, resComboList);
	}

	//
	// get results for each numberSet; added position before and after
	// results are of the numberSet alone AND of numberSet with current base set
	// lists are sorted best to worst
	//
	protected List<Integer> getNextSetResults() {
		List<Integer> nsCurList = null;
		
		int positionLastAdd = tunedp.getCfgScratchInt("posLastAdd");
		boolean positionLastAddAfter = tunedp.getCfgScratchBool("posLastAddAfter");
		int nsFull = -1;
		
		// check nsAddSets for next set	
		if (nsAddSets.size() > 0) {
			List<List<Integer>> nsSet  = nsAddSets.remove(0);
			System.out.print("EXT_NS_SET["+nsAddSets.size()+"/"+nsSet.size()+"] w["+tunedp.getCfgWindowSize()+"]pos["+positionLastAdd+"]");		
			nsCurList = new ArrayList<>();
			for (List<Integer> nss:nsSet) {
				int nsm = tunedp.addCfgNSDefined(nss, -1);
				if (nsm >= 0) nsCurList.add(nsm);
			}
			System.out.print(" => ns["+nsCurList.size()+"] =>");			

		} else if (this.maxWindow > 0 && tunedp.getCfgWindowSize() == this.maxWindow) {
			// reached pre-set max on window size
			return null;
		} else {
			// apply limits
			if (positionLastAddAfter && this.maxBefore >= 0 && tunedp.getCfgBefore() >= this.maxBefore) {
				// only after adds
				positionLastAddAfter = false;
			} else if (!positionLastAddAfter && this.maxAfter >= 0 && tunedp.getCfgAfter() >= this.maxAfter) {
				// only before adds
				positionLastAddAfter = true;
			}
			
			nsCurList = new ArrayList<>();

			// add new numberSets; initially add 2 at a time, then when massive add left, then right
			List<List<Integer>> nsSet = new ArrayList<>();
			// add just one
			if (positionLastAddAfter) {
				// before
				positionLastAdd++;
				tunedp.setCfgScratch("posLastAdd", positionLastAdd);
				System.out.print("EXT_NS_BEFORE["+tunedp.getCfgWindowSize()+" -> "+(tunedp.getCfgWindowSize()+1)+"]");			

				tunedp.incWindowSize(false);
				tunedp.getFramer().getIncWindowSizeNS(tunedp, optType, positionLastAdd, false, nsCurList, nsSet);
				// update testSet numberSet map
				testMap.remapNumberSetsWindowExtend();
			} else {
				// after
				System.out.print("EXT_NS_AFTER["+tunedp.getCfgWindowSize()+" -> "+(tunedp.getCfgWindowSize()+1)+"]");			
				tunedp.incWindowSize(true);
				tunedp.getFramer().getIncWindowSizeNS(tunedp, optType, positionLastAdd, true, nsCurList, nsSet);
			}
			// remove if not for type: just in case
			removeForPType(nsCurList, nsSet);
			
			// update unique list / clear it
			nsUniqeList.clear();
			
			System.out.print(" => ns["+nsCurList.size()+"/"+nsSet.size()+"] =>");	
			
			// get max per
			maxNSPerPass = nsCurList.size() / 2;
			if (maxNSPerPass > MAX_NS_PER_PASS) maxNSPerPass = MAX_NS_PER_PASS;
			else if (maxNSPerPass < MIN_NS_PER_PASS) maxNSPerPass = MIN_NS_PER_PASS;
			
			int maxNSPerPassPrep = nsCurList.size() / 2;
			if (maxNSPerPassPrep > MAX_NS_PER_PASS_PREP) maxNSPerPassPrep = MAX_NS_PER_PASS_PREP;
			else if (maxNSPerPassPrep < MIN_NS_PER_PASS_PREP) maxNSPerPassPrep = MIN_NS_PER_PASS_PREP;
				
			if (nsCurList.size() > maxNSPerPass) { // over eval size
				System.out.println(" NS-MULTI-SET["+nsCurList.size()+"]");	
				//List<ResultSet> addResultSets = new ArrayList<>();
				// remove all new sets
				tunedp.removeCfgNS(nsCurList);
				HashMap<Integer, List<Integer>> nsMap = new HashMap<>();
				
				List<Integer> nsCurPartial = new ArrayList<>();
				// break this down into sets
				int ns = 0;
				int nsSetCnt = 0;
				int nsSetTotal = nsCurList.size()/maxNSPerPassPrep; // prep size
				int setSize = VegUtil.getListSplit(nsSetTotal, nsCurList.size());
				if (nsSetTotal < 1) nsSetTotal = 1;
				
				// this is to reduce memory usage here... it is critical
				class nsInfTmp {
					int ns;
					int passTotal;
					int failTotal;
					int unkPassTotal;
				}
				List<nsInfTmp> addRSInfo = new ArrayList<>();
				// make full numberSet
				List<Integer> fns = new ArrayList<>();
				for (int i=0;i<tunedp.getCfgWindowSize();i++) fns.add(i);
				
				while (ns<nsCurList.size()) {
					// add this set into it
					int nsIn = 0;
					nsCurPartial.clear();
					int bns = ns;

					for (;ns<nsCurList.size() && nsIn < setSize;ns++) {
						nsIn++;
						int mns = tunedp.addCfgNSDefined(nsSet.get(ns), -1);
						if (mns == -1) {
							System.out.println("ERROR["+ns+"] " + MLNumberSetUtil.setToString(nsSet.get(ns)));
							tunedp.print(true);
						}
						nsCurPartial.add(mns); // add new nsID
						// retain the original number here;
						nsMap.put(ns, nsSet.get(ns));
						if (nsSet.get(ns).size() == tunedp.getCfgWindowSize()) nsFull = ns;
					}
					
					int tmpNsFull = -1;
					// always add the full numberSet
					if (optm.getCfgBool("fullAlways")) {
						tunedp.addCfgNSDefined(fns, -1);
						tunedp.updateNumberSetWeights();
						tunedp.updateCfgNS();
						tmpNsFull = tunedp.getCfgNSFullNumber();
					} else {
						tunedp.updateNumberSetWeights();
						tunedp.updateCfgNS();						
					}
				
					nsSetCnt++;
					System.out.print(" SET-NS["+nsSetCnt+" of "+nsSetTotal+"]cnt["+nsCurPartial.size()+"]["+tmpNsFull+"] =>");	
					
					// train & cleanup
					int sz = nsCurPartial.size();
					trainAndPrepSubset(nsCurPartial, false, tmpNsFull);
					System.out.print(" =>  set-eval["+sz+" >> "+nsCurPartial.size()+"]cur["+tunedp.getNSTurnedOnCount()+"]ns["+curNSInDp.size()+"]v["+tunedp.getVectorCount()+"] =>");	

					// get numbers
					if (nsCurPartial.size() > 0) {
						// save with original number from nsSet
						for (int nsx=0;nsx<this.resComboList.size();nsx++, bns++) {
							VResultSet rs = this.resComboList.get(nsx);
							nsInfTmp xns = new nsInfTmp();
							xns.failTotal = rs.failTotal;
							xns.passTotal = rs.passTotal;
							xns.unkPassTotal = rs.getPassTotal(PredictionType.PredictUnknown);
							xns.ns = bns;
							addRSInfo.add(xns);
							rs.clearLinksAndMore();			
						}					
					}
					if (nsCurPartial.size() > 0 || tmpNsFull >= 0) {
						// remove new ns
						if (tmpNsFull >= 0) nsCurPartial.add(tmpNsFull);
						tunedp.removeCfgNS(nsCurPartial);
						tunedp.updateNumberSetWeights();
						tunedp.updateCfgNS();
					}
					System.gc();
					System.out.println(" DONE");	
				}
				// sort numberSets
				Collections.sort(addRSInfo, new Comparator<nsInfTmp>() {
			        @Override
			        public int compare(nsInfTmp lvp, nsInfTmp rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	        	
			        	if (lvp.passTotal < rvp.passTotal) return 1;
			        	if (lvp.passTotal > rvp.passTotal) return -1;
			        	if (lvp.failTotal > rvp.failTotal) return 1;
			        	if (lvp.failTotal < rvp.failTotal) return -1;
			        	if (lvp.unkPassTotal > rvp.unkPassTotal) return 1;
			        	if (lvp.unkPassTotal < rvp.unkPassTotal) return -1;
			        	return 0;  
			        }
			    });				
				if (optm.getCfgBool("fullAlways") && nsFull >= 0) {
					// move full to first in combiniation
					for (int x=0;x<addRSInfo.size();x++) {
						if (addRSInfo.get(x).ns == nsFull) {
							addRSInfo.add(0, addRSInfo.remove(x));
							break;
						}
					}
				}
												
				// break into sets
				int nsIn = 0;
				List<List<Integer>> nsiSet = new ArrayList<>();
				for (int i = 0;i<addRSInfo.size();i++) {
					nsIn++;					
					nsiSet.add(nsMap.get(addRSInfo.get(i).ns)); // add the set of numbers
					if (nsIn == maxNSPerPass || i == (addRSInfo.size()-1)) {
						nsAddSets.add(nsiSet);
						nsiSet = new ArrayList<>();
						nsIn = 0;
					}
				}

				// add first set in
				nsSet  = nsAddSets.remove(0);
				System.out.print("EXT_NS_SET["+nsAddSets.size()+"/"+nsSet.size()+"]w["+tunedp.getCfgWindowSize()+"]pos["+positionLastAdd+"]");		
				nsCurList = new ArrayList<>();
				for (List<Integer> nss:nsSet) {
					int nsm = tunedp.addCfgNSDefined(nss, -1);
					if (nsm >= 0) nsCurList.add(nsm);
				}
				System.out.print("=> ns["+nsCurList.size()+"]");					
			}
		}
		// have new set; train and prep it
		if (optm.getCfgBool("fullAlways")) nsFull = tunedp.getCfgNSFullNumber();		
		else nsFull = -1;			
		
		// update current NS list: curNSInDp	
		curNSInDp.clear();
		for (int i=0;i<tunedp.getNSCount();i++) {
			if (nsCurList.contains(i)) continue;
			if (tunedp.isCfgNSTurnedOff(i)) continue;
			curNSInDp.add(i);
		}	
		
		// train and clean the subsets
		trainAndPrepSubset(nsCurList, true, nsFull);
		return nsCurList;
	}
	
	//
	// remove if not usable due to something
	//
	protected void removeForPType(List<Integer> nsCurList, List<List<Integer>> nsSets) {
		if (optType == PredictionType.AnyUnknown || optType == PredictionType.PredictUnknown) {
			int idp = tunedp.getCfgIdentityPosition();
			
			List<Integer> del = new ArrayList<>();
			for (int x=0;x<nsSets.size();x++) {
				List<Integer> nss = nsSets.get(x);
				if (!nss.contains(idp)) continue; // not new set
				// remove numberSets
				int ns = tunedp.findCfgNS(nss);
				del.add(ns);
				int idx = nsCurList.indexOf(ns);
				nsCurList.remove(idx);
				nsSets.remove(x);
				x--;
			}
			tunedp.removeCfgNS(del);	
			// reset new list
			nsCurList.clear();
			for (int x=0;x<nsSets.size();x++) {
				int ns = tunedp.findCfgNS(nsSets.get(x));
				nsCurList.add(ns);
			}
			//System.out.println("left["+del.size()+"]["+nsCurList.size()+"]["+tunedp.getCfgNSIdentityNumber()+"]");
		}
	}
	
	
	//
	// get the mergeValue for current
	// NOTE: this mergevalue may not be good, the other method may work better
	//
	protected double getMergeValue(VDataPlane bdp, List<Integer> remNsList, boolean noExclude) {
		// turn all in set on, get merge value, turn them back off
		// make sure current are on, new are off
		if (remNsList != null) tunedp.setCfgNSTurnedOff(remNsList, false);
		tunedp.setCfgNSTurnedOff(curNSInDp, false);
		
		// old method
		//VegTune.optimizeMergeValue(vML, bdp, tunedp, null, dss);
		//double mergeValue = bdp.getCfgMergeWeight(tunedp);
		//noExclude[100 or 0 not allowed]
		// use the full tune dataset always
		double mergeValue = VegTune.optimizeMergeModels(bdp.getVegML(), bdp.getDimensionTag(), bdp.getTag(), 
														tunedp.getDimensionTag(), tunedp.getTag(), 
														false, false, optType, noExclude, true, false, dss.getTuneDataSets());					
		System.out.println("");
		//System.out.println("GOT ["+mergeValue+"] vs ["+mergeValue2+"]");
		bdp.removeMergeMap(tunedp.getDimensionTag(), tunedp.getTag());
		//mergeValue = mergeValue2;
		// turn new off
		if (remNsList != null) tunedp.setCfgNSTurnedOff(remNsList, true);
		
		// set base in dataplane/carve
		bdp.setCfgMergeWeight(tunedp, mergeValue);
		OptimizerStatistical.mergeValue = mergeValue;					
		//System.out.println("  MERGE-VAL["+mergeValue+"]");
		return mergeValue;
	}
	
	//
	// check and order the numberSets
	//
	protected boolean assessNumberSets(List<Integer> nsSets, boolean SetChanged, boolean firstPass) {
		// run test
		if (!SetChanged) {
			//System.out.println(" nochange pass["+resCur.passTotal+"]");	
			return true;
		}	
		System.out.print("      set-eval["+nsSets.size()+"]cur["+tunedp.getNSTurnedOnCount()+"]ns["+curNSInDp.size()+"]v["+tunedp.getVectorCount()+"] =>");	

		// make sure current are on, new are off
		tunedp.setCfgNSTurnedOff(nsSets, true);
		tunedp.setCfgNSTurnedOff(curNSInDp, false);
		
		if (!firstPass) {
			////////////////////////////////////////
			// check for empty and remove them
			boolean removed = false;
			for (int x=0;x<tunedp.getNSCount();x++) {
				if (nsSets.contains(x)) continue; // not new set
				int acCnt = tunedp.getNSAccumulatorCount(x);
				if (acCnt <= 0) {
					if (!removed) System.out.println("");
					System.out.println("       TURN OFF NS["+x+"]["+tunedp.getNSFormatString(x)+"]  v["+tunedp.getVectorCount(x)+"]");
					// remove numberSets
					removeNS(x, nsSets);	
					x = 0; // start over
					removed = true;
				}
			}
			if (removed) {
				System.out.print("      RM-NS-DONE["+tunedp.getNSTurnedOnCount()+"] =>");
			}
			
			////////////////////////////////////////
			// get updated stats
			// test with each current NS removed to see if any could be dropped		
			TestModSet nstests = new TestModSet();
			getDropTests(nstests);		
			List<VResultSet> resCurDropList = MLThreadUtil.runTestPredictModify(dp, new ArrayList<>(), nstests, threadTuneDs, false, true);		
			// get baseLine	from set
			resCur = resCurDropList.remove(0);
			curPassTotal = resCur.getPassTotal(optType);
			nstests.remove(0);
			System.out.print(" pass["+curPassTotal+"]");
			
			if (nstests.size() > 1) {
	//			tunedp.print(true);
//System.out.println("");
				////////////////////////////////////////
				// check drop list for better result
				boolean dropTest = true;
				while (dropTest) {
					int drp_ns = -1, drpPassTotal = 0, drpCnt = 0;
					dropTest = false;
					for (int x=0;x<resCurDropList.size();x++) {	
						int ns = (int)nstests.get(tunedp, x).userTag;
						if (ns == -1 || ns == tunedp.getCfgNSFullNumber() || ns == tunedp.getCfgNSIdentityNumber()) continue;
						
						// prevent unique from removal (for as long as it is in the list)
						if (MLNumberSetUtil.findSet(nsUniqeList, tunedp.getNS(ns)) >= 0) continue;	
						VResultSet rs = resCurDropList.get(x);
						if (rs.getPassTotal(optType) >= resCur.getPassTotal(optType)) {
							drpCnt++;
							if (drpPassTotal == 0 || drpPassTotal > rs.getPassTotal(optType)) {
								drpPassTotal = rs.getPassTotal(optType);
								drp_ns = ns;
							}
						} 
					}
		
					if (drp_ns >= 0) {
						System.out.println("");	
						System.out.print("  DROP_BETTER["+String.format("%3d", drp_ns)+"]["+tunedp.getNSFormatString(drp_ns)+"] ["+drpPassTotal+" >= "+resCur.getPassTotal(optType)+"]cnt("+drpCnt+") =>");		
		
						// remove the ns
						removeNS(drp_ns, nsSets);		
						// update and re-run tests
						getDropTests(nstests);
						resCurDropList = MLThreadUtil.runTestPredictModify(dp, new ArrayList<>(), nstests, threadTuneDs, false, true);		
						// get baseLine	from set
						resCur = resCurDropList.remove(0);
						nstests.remove(0);	
						curPassTotal = resCur.getPassTotal(optType);
						System.out.println(" update-pass["+curPassTotal+"]");
					
						System.out.print("      set-eval["+nsSets.size()+"]cur["+tunedp.getNSTurnedOnCount()+"]ns["+curNSInDp.size()+"]v["+tunedp.getVectorCount()+"] =>");						
						dropTest = true;
					}
				}
			}
			if (this.ctlGroup) {
				// re-asses control
				nstests.clear();
				TestMod t = new TestMod(tunedp.getCfgNSWeightsBase());	
				nstests.add(tunedp, t.completeNSW());
				
				List<VResultSet> rsl = MLThreadUtil.runTestPredictModify(dp, new ArrayList<>(), nstests, threadTuneCtlDs, false, false);		
				resCurCtl = rsl.get(0);
			}
		}
		
		getSingleCombindNSRs(nsSets, true, -1);

		// make sure turned on
		tunedp.setCfgNSTurnedOff(curNSInDp, false);
				
		System.out.println(" DONE");	
		return true;
	}
	
	//
	// make the drop tests
	// FIRST test will be complete current
	//
	public void getDropTests(TestModSet nstests) {
		nstests.clear();
		// add baseline for current complete: MUST BE FIRST for mod to work
		TestMod t = new TestMod(tunedp.getCfgNSWeightsBase());	
		t.userTag = -1;
		nstests.add(tunedp, t.completeNSW());
		
		// add full set without for each (aside from full / identity)
		for (int x=0;x<curNSInDp.size();x++) {					
			int tns = curNSInDp.get(x);
			if (tns == tunedp.getCfgNSFullNumber() || tns == tunedp.getCfgNSIdentityNumber()) continue;
			t = new TestMod(tunedp.getCfgNSWeightsBase());	
			t.nsWeights[tns] = 0; // drop this one
			t.userTag = tns;
			nstests.add(tunedp, t.completeNSW());	
		}	
	}

	
	//
	// drop ns currently in; updaets lists
	//
	// if remove numberSet all get re-numbered must re-do-all	
	protected void removeNS(List<Integer> delList, List<Integer> nsSets) {
		removeNS(-1, delList, nsSets);
	}
	protected void removeNS(int ns, List<Integer> nsSets) {
		removeNS(ns, null, nsSets);
	}
	protected void removeNS(int delNs, List<Integer> nsDelList, List<Integer> nsSets) {
		if (delNs < 0 && nsDelList.size() < 1) return;
		
		// the NS are re-numbered: save current to NS descriptor
		HashMap<Integer, List<Integer>> nsMap = new HashMap<>();
		if (nsSets != null) for (Integer nsi:nsSets) nsMap.put(nsi, tunedp.getNS(nsi));	
		HashMap<Integer, List<Integer>> curNsMap = new HashMap<>();
		for (int nsi=0;nsi<tunedp.getNSCount();nsi++) {
			if (tunedp.isCfgNSTurnedOff(nsi)) continue;
			curNsMap.put(nsi, tunedp.getNS(nsi));
		}
	
		// reduce retained information
		if (nsDelList != null) {
			if (nsSets != null) {
				for (int dpns:nsSets) {
					VResultSet rs = testMap.getResultSetNS(testMap.getNSforDpNs(dpns));
					if (rs != null) rs.clearLinksAndMore();
				}
			}
		} else {
			VResultSet rs = testMap.getResultSetNS(testMap.getNSforDpNs(delNs));
			if (rs != null) rs.clearLinksAndMore();			
		}
		
		// must remove from all
		//System.out.println(" removeNS["+nsSets.size()+"] ["+tunedp.getNSCount()+" / "+tunedp.getNSTurnedOnCount()+"] ["+tunedp.getNSFormatString(delNs)+"] ");

		// only the "in" numberSets on
		tunedp.setCfgNSTurnedOff(true);
		tunedp.setCfgNSTurnedOff(curNSInDp, false);
			
		// remove the ns
		if (nsDelList != null) tunedp.removeCfgNS(nsDelList);
		else tunedp.removeCfgNS(delNs);	
			
		tunedp.updateCfgNS();
		
		// must correct curNSInDp and newlist (passed in)
		curNSInDp.clear();
		if (nsSets != null) nsSets.clear();		
		for (int nsi=0;nsi<tunedp.getNSCount();nsi++) {
			if (tunedp.isCfgNSTurnedOff(nsi)) {
				if (nsSets != null) nsSets.add(nsi); // still to do
			} else {
				curNSInDp.add(nsi); // part of the program
			}
		}
		
		// remap existing NS
		// update NS on cur singles
		List<VResultSet> rssl = new ArrayList<>(curSingleList);
		for (int i=0;i<rssl.size();i++) {
			VResultSet rsx = rssl.get(i);
			List<Integer> nl = curNsMap.get((int)rsx.val2);
			int nnsi = tunedp.findCfgNS(nl);
			if (nnsi >= 0) rsx.val2 = tunedp.findCfgNS(nl);
			else curSingleList.remove(rsx);
		}
		
		// must re-map numbersSets numbers
		this.testMap.reMapNSMapping(this);
		
		// re-map numbersets in resultlists
		if (resSingleList != null && nsSets != null) {
			for (VResultSet rsx:resSingleList) {
				int xns = tunedp.findCfgNS(nsMap.get((int)rsx.val2));
				if (xns == -1) System.out.println("ERROR["+resSingleList.size()+"]["+tunedp.getNSCount()+"/"+tunedp.getNSTurnedOnCount()+"]rm-ns["+xns+"]fns["+rsx.val2+"]  remNsList["+nsSets.size() + " /"+nsMap.keySet().size()+"] ["+MLNumberSetUtil.setToString(nsSets)+"]");
				rsx.val2 = xns;
			}
			for (VResultSet rsx:resComboList) {
				rsx.val2 = tunedp.findCfgNS(nsMap.get((int)rsx.val2));
			}
		}
		tunedp.setCfgNSTurnedOff(true);
		tunedp.setCfgNSTurnedOff(curNSInDp, false);
	}
	
	//
	// get complete current results
	//
	protected int getCurrentRes() {
		// add baseline for current complete
		TestModSet nstests = new TestModSet();
		TestMod t = new TestMod(tunedp.getCfgNSWeightsBase());	
		nstests.add(tunedp, t.completeNSW());			
		List<VResultSet> rl = MLThreadUtil.runTestPredictModify(dp, new ArrayList<>(), nstests, threadTuneDs, false, true);		
		// get baseLine	from set
		return rl.get(0).getPassTotal(optType);
	}
	
	//
	// test subset of numberSEts
	// returns set of results for each in newNS plus the current set
	//
	protected List<VResultSet> getSingleCombindNSRs(List<Integer> newNS, boolean testBothAndSave, int nsFull) {

		// turn on all new numberSets
		tunedp.setCfgNSTurnedOff(newNS, false);
		if (nsFull > 0) tunedp.setCfgNSTurnedOff(nsFull, false);
		//System.out.print(".");	
	
		// get test SINGLE
		double [] ws = tunedp.getCfgNSWeightsBase();
		// current complete must be first
		TestModSet nstests = new TestModSet();
		TestMod t = new TestMod(tunedp.getCfgNSWeightsBase());
		nstests.add(tunedp, t.completeNSW());
		
		if (testBothAndSave) {
			for (int x=0;x<newNS.size();x++) {
				int ns = newNS.get(x);
				t = new TestMod(tunedp.getNSCount());	
				t.nsWeights[ns] = ws[ns];
				nstests.add(tunedp, t.completeNSW());	
			}
		}
		// test Combined
		for (int x=0;x<newNS.size();x++) {
			int ns = newNS.get(x);
			t = new TestMod(tunedp.getNSCount());	
			for (int xx=0;xx<curNSInDp.size();xx++) t.nsWeights[curNSInDp.get(xx)] = ws[curNSInDp.get(xx)]; // add in all the current on
			t.nsWeights[ns] = ws[ns]; // add this one
			if (nsFull >= 0) t.nsWeights[nsFull] = ws[nsFull]; // add full NS if good
			nstests.add(tunedp, t.completeNSW());	
		}

		boolean getinfo = testBothAndSave;
		
		//System.out.print(" =>");
		// run base line for each		
		List<VResultSet> resList = MLThreadUtil.runTestPredictModify(dp, new ArrayList<>(), nstests, threadTuneDs, false, getinfo);
		//System.out.print(".");	
		VResultSet rcur = resList.remove(0); // drop current complete??
		
		if (testBothAndSave) {
			resSingleList = resComboList = null;
			resComboList = resList;
			// get the singles into seperate list
			resSingleList = new ArrayList<>();
			for (int x=0;x<newNS.size();x++) resSingleList.add(resComboList.remove(0));
			
			// save NumberSet to each in val2
			resNSFullest = -1;
			for (int x=0;x<newNS.size();x++) {
				int ns = newNS.get(x);
				resSingleList.get(x).val2 = ns;
				resSingleList.get(x).clearForRetain();
				resComboList.get(x).val2 = ns;	
				resComboList.get(x).clearForRetain(); // this is just used for sort	
				if (tunedp.getNS(ns).size() == tunedp.getCfgWindowSize()) resNSFullest = ns;
			}	
		} else {
			for (int x=0;x<newNS.size();x++) {
				resList.get(x).val2 = newNS.get(x);
			}			
		}
		
		// old on, new off
		tunedp.setCfgNSTurnedOff(newNS, true);
		if (nsFull > 0) tunedp.setCfgNSTurnedOff(nsFull, true);
		tunedp.setCfgNSTurnedOff(curNSInDp, false);
		
		//System.out.println("");
		return resList;
	}
	
	
	//
	// train and clean and carveSingle list of added numberSets
	//
	protected void trainAndPrepSubset(List<Integer> newNS, boolean prep, int nsFull) {
		// update all numberSets
		tunedp.updateNumberSetWeights();
		tunedp.updateCfgNS();
		tunedp.setCfgNSTurnedOff(newNS, false);
		if (nsFull > 0) tunedp.setCfgNSTurnedOff(nsFull, false);

		//
		// train new numberSets
		//
		// old numberSets off (they have their training)
		tunedp.setCfgNSTurnedOff(curNSInDp, true);
		tunedp.setCfgFramerArg(this.trainDs.getValLLV());	
		MLThreadUtil.runTrainDataSets(tunedp, this.threadTrainDs);
	//	OptimizerThreads.runTrainDataSets(dp, this.threadTrainSets, this.threadTrainValueSets);
	//	VegTrain.trainDataSetsS(vML, this.dimensionTag, this.dataPlaneTag, this.trainDataSets, this.trainDataValueSets);	
		tunedp.setCfgFramerArg(null);
		System.out.print(" TRAINED");
		
		// logical cleanup
		int trs = tunedp.getTrainingFilterSize();
		if (trs <= 0) System.out.println("");

		VegTune.logicalDataDefinitionReduction(vML, tuneDimensionTag, tuneDataPlaneTag, optType);	
		VegTune.logicalDataIdentityReduction(vML, tuneDimensionTag, tuneDataPlaneTag, false, trainDs, true);
		
		// old back on
		tunedp.setCfgNSTurnedOff(curNSInDp, false); 

		// need containsCounts(vids) for mapRaw
		getSingleCombindNSRs(newNS, true, nsFull);
		
		// reg this prep?
		this.testMap.mapRaw(this, resSingleList, resComboList, resCur);
		
		// check new set for under-preformers
		int betterThanPossibleRaw = optm.getCfgInt("betterThanPossibleRaw");
		if (betterThanPossibleRaw > 0) {
			List<Integer> delList = new ArrayList<>();
			for (int i=0;i<newNS.size();i++) {
				int dpns = newNS.get(i);
				if (optm.getCfgBool("fullAlways") && dpns == tunedp.getCfgNSFullNumber()) continue;				
				// get the raw map
				testInfo tinf = testMap.getInfoMap(tunedp.getNS(dpns));				
				// check if it should go
				//if ((tinf.rrpsb + tinf.rcrtOnly) < betterThanPossibleRaw) {
				if ((tinf.rrpsbm + tinf.rcrtOnly) < betterThanPossibleRaw) {
					// not enough possibilities to improve
					System.out.println(" RAW_POSSIBLE_NO ns["+dpns+"]["+tunedp.getCfgNSFullNumber()+"]["+tunedp.getNSFormatString(dpns)+"]"
										+ " psb["+String.format("%3d", tinf.rpsb)+"]rpsb["+String.format("%3d", tinf.rrpsb)+"]psbm["+tinf.rrpsbm+"]crt["+tinf.rcrtOnly+"]"
										+ " total["+(tinf.rrpsb + tinf.rcrtOnly)+" / "+(tinf.rrpsbm + tinf.rcrtOnly)+" < "+betterThanPossibleRaw+"]");
					delList.add(dpns);
					
					// remove from resultSet
					for (VResultSet r:resSingleList) {
						if (r.val2 == dpns) {
							resSingleList.remove(r);
							break;
						}
					}
				}
			}
			removeNS(delList, newNS);
		}
		//System.out.println(" REMAIN new["+newNS.size()+"]["+rList.size()+"] del["+delList.size()+"]");
		if (newNS.size() == 0) return;
		
		// run any data prep or mods
		if (prep) {
			System.out.println("");	
			optm.processNSPrep(this, curNSInDp, newNS, resSingleList);
			//System.out.println("");	
		} 
	}

	protected String getFileName() {
		return initBaseName + "-"+(initNameStep+tunedp.getCfgWindowSize())+".veg";
	}
	protected String getFileName(int windowSize) {
		return initBaseName + "-"+(initNameStep+windowSize)+".veg";
	}
	protected String getFileNameSolid() {
		return initBaseName + "-"+(initNameStep+tunedp.getCfgWindowSize())+"-solid.veg";
	}
	protected String getFileNameJSON() {
		return initBaseName + "-"+(initNameStep+tunedp.getCfgWindowSize())+".json";
	}
	protected String getFileNameTemp() {
		return initName.substring(0, initName.length()-4) + "-"+tunedp.getCfgWindowSize()+"-temp.veg";
	}

	// save to new
	void save(int step) {		
		// use window size
		VFileUtil.delFile(getFileName());
		String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm.ss").format(new Date());
		tunedp.setCfgScratch("optTime", timeStamp); // count
		tunedp.setCfgScratch("optStep", step); // count
		tunedp.setCfgScratch("optNSTry", nsTryTotal); // ns tried	
		
		// save with alternate default?
		if (this.optm.getCfg("mergeSetInfoDefaultD") != null) {
			VDataPlane ddp = vML.getCfgDefaultDataPlane();
			vML.setCfgDefaultDataPlane(optm.getCfgString("mergeSetInfoDefaultD"), optm.getCfgString("mergeSetInfoDefaultDP"));	
			vML.saveSilent(getFileName());
			vML.setCfgDefaultDataPlane(ddp.getDimensionTag(), ddp.getTag());
		} else {
			vML.saveSilent(getFileName());
		}
		/*
		// if unknown, remove idenitity
		if (optType == PredictionType.AnyUnknown || optType == PredictionType.PredictUnknown) {
			VegML vjML = this.loadCopy();
			DataPlane jdp = vjML.getDataPlane(this.dimensionTag, this.dataPlaneTag);
			jdp.removeCfgNS(jdp.getCfgNSIdentityNumber());
			vjML.saveSilent(getFileName()+"i");
		}
		*/
		System.out.println("SAVE["+getFileName()+"]");

		System.gc();
	}
	void saveTemp() {		
		String fn = getFileNameTemp();
		VFileUtil.delFile(fn);
		vML.saveSilent(fn);
		System.gc();
	}
	void saveJSON() {	
		if (!optm.getCfgBool("exportJSON") && !optm.getCfgBool("exportSolid")) return;
		VegML vjML = this.loadCopy();
		vjML.makeSolid();
		if (optm.getCfgBool("exportJSON")) {
			System.out.println("JSON["+getFileNameJSON()+"]");
			vjML.exportJSON(getFileNameJSON());			
		}		
		if (optm.getCfgBool("exportSolid")) {
			System.out.println("Solid["+getFileNameSolid()+"]");
			vjML.save(getFileNameSolid());
		}
		vjML = null;
		System.gc();
	}

	
	void delTemp() {		
		VFileUtil.delFile(getFileNameTemp());
	}
	
	// save to new
	void load() {		
		VegML vvML = loadCopy();
		if (vvML != null) {
			vML = vvML;
			dp = vML.getDataPlane(this.dimensionTag, this.dataPlaneTag);
			tunedp = vML.getDataPlane(this.tuneDimensionTag, this.tuneDataPlaneTag);
			System.gc();
		}
	}
	VegML loadCopy() {		
		VegML vvML =  VegML.load(getFileName());
		if (vvML == null) System.err.println("ERROR: failed to load["+getFileName()+"]");
		return vvML;
	}
	void loadTemp() {		
		String fn = getFileNameTemp();
		VegML vvML = VegML.load(fn);
		if (vvML != null) {
			vML = vvML;
			dp = vML.getDataPlane(this.dimensionTag, this.dataPlaneTag);
			tunedp = vML.getDataPlane(this.tuneDimensionTag, this.tuneDataPlaneTag);
			System.gc();
		} else {
			System.err.println("ERROR: failed to temp load["+fn+"]");
		}
	}	

	// get index of ns in list
	int findRsInList(List<VResultSet> list, int ns) {
		for (int i=0;i<list.size();i++) {
			if (list.get(i).val2 == ns) return i;
		}
		return -1;
	}

	
	//
	// check if dataplan contains child numberSet (ns without pos position number)
	// check ancestor distance?
	static boolean hasNumberSetChild(VDataPlane dp, List<Integer> ns, boolean addAfter) {
		if (ns.size() < 2) return false;
		List<Integer> nsx = new ArrayList<>(ns);
		
		// iterate until find last
		if (!addAfter) nsx.remove(0); // first
		else nsx.remove(nsx.size()-1); // last
		if (dp.findCfgNS(nsx) >= 0) return true;
		
		
		return false;
	}
	
	//
	// sort with numberSet size a primary factor: smallest to largest
	// val2 - has NS when called / returned
	// val - retains original position
	List<VResultSet> sortList(final int sortType, final int nsFull, List<VResultSet> rsl) {	
		// save current positions
		for (int i=0;i<rsl.size();i++) {
			rsl.get(i).val = i;
		}
		//boolean addAfter = tunedp.getCfgScratchBool("posLastAddAfter");

		// sort
		Collections.sort(rsl, new Comparator<VResultSet>() {
	        @Override
	        public int compare(VResultSet lvp, VResultSet rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	        	
	        	if (lvp.passTotal < rvp.passTotal) return 1;
	        	if (lvp.passTotal > rvp.passTotal) return -1;
	        	if (lvp.failTotal > rvp.failTotal) return 1;
	        	if (lvp.failTotal < rvp.failTotal) return -1;
	        	int lu = lvp.getPassTotal(PredictionType.PredictUnknown);
	        	int ru = rvp.getPassTotal(PredictionType.PredictUnknown);
	        	if (lu > ru) return 1;
	        	if (lu < ru) return -1;
	        	return 0;  
	        }
	    });
		
		if (optm.getCfgBool("fullAlways") && nsFull >= 0) {
			// move full to first in combiniation
			for (int x=0;x<rsl.size();x++) {
				if (rsl.get(x).val2 == nsFull) {
					rsl.add(0, rsl.remove(x));
					break;
				}
			}		
		}
		return rsl;
	}

	
	//
	// retain only the unique vector info for this
	//
	// IF vectSets ON, then retain like sets?
	protected int nsRetainOnlyUnique(VResultSet rss, int ns, boolean remove) {	
		List<Long> vidRetainList = new ArrayList<>();
		// use the current set
		
		for (int i = 0;i<rss.getContains().size();i++) {
			tPosition tp = this.testMap.testPositions.get(i);
			// single correct AND this correct			
			if (tp.getPWinCount() == 1 && rss.getContains(i).win) { 
				vidRetainList.add(rss.getContains(i).vid);
			}
		}
		
		if (remove) {
			// remove all vid except the exception list
			tunedp.removeAllNSVectorsExcept(ns, vidRetainList);
			// get how many retained
			return tunedp.getVectorCount(ns);
		}
			// get how many still present
		int total = 0;
		for (int i = 0;i<vidRetainList.size();i++) {
			if (tunedp.getAccumulator(ns, vidRetainList.get(i)) != null) total++;
		}
		
		return total;
	}
	
	
	//
	// test subset of numberSets
	// first is complete current
	//
	protected List<VResultSet> testDPIndividuals() {
		double [] ws = tunedp.getCfgNSWeightsBase();
		TestModSet nstests = new TestModSet();
		
		// test full current set (must be first)
		TestMod t = new TestMod(ws);	
		nstests.add(tunedp, t.completeNSW());	
		
		// test SINGLE
		for (int ns=0;ns<tunedp.getNSCount();ns++) {
			t = new TestMod(tunedp.getNSCount());	
			t.nsWeights[ns] = ws[ns];
			nstests.add(tunedp, t.completeNSW());	
		}	
			
		// run base line for each
		List<VResultSet> resList = MLThreadUtil.runTestPredictModify(dp, new ArrayList<>(), nstests, threadTuneDs, false, true);

		// save NumberSet to each in val2
		for (int ns=0;ns<resList.size();ns++) resList.get(ns).val2 = ns;
		resList.get(resList.size()-1).val2 = -1; // full set
		
		return resList;
	}
	
	// 
	// Decide the best weight base for this model
	//
	protected static int NWSBASE_WINDOW = 5;	// window size used to decide base
	protected NSWeightBase decideBestWeightBase() {
		// if set to none, retain it
		if (tunedp.getCfgNSWeight() == NSWeightBase.None) return tunedp.getCfgNSWeight();
		
		System.out.print("Determine NSWeightBase  ");	
		// load copy
		VegML vMLbt = loadCopy();
		System.out.print("=");	

		// train window 6 (so distance can be evaled)
		VDataPlane dpbt = vMLbt.getDataPlane(this.tuneDimensionTag, this.tuneDataPlaneTag);
		dpbt.setCfgNSWeight(NSWeightBase.Flat);
		dpbt.setCfgWindowSize(NWSBASE_WINDOW);
		dpbt.resetNSAndData();
		dpbt.setCfgFramerArg(this.trainDs.getValLLV());
		System.out.print(">");	
		// this is slow... could switch to partial dataset
		MLThreadUtil.runTrainDataSets(dpbt, this.threadTrainDs);
		dpbt.setCfgFramerArg(null);
		System.out.print(" TRAINED ");	
		
		// test with each base (via nstests)
		TestModSet nstests = new TestModSet();
		for (NSWeightBase wb:NSWeightBase.values()) {
			if (wb == NSWeightBase.None) continue;
			dpbt.setCfgNSWeight(wb);
			TestMod t = new TestMod(dpbt.getCfgNSWeightsBase());	
			nstests.add(tunedp, t.completeNSW());
			t.userTag = wb;
		}
		System.out.print("=>");	

		// run base line for each
		List<VResultSet> resList = MLThreadUtil.runTestPredictModify(dpbt, new ArrayList<>(), nstests, threadTuneDs, false, false);
		// clear
		dpbt.reset();
		
		// check values; best wins
		VResultSet br = null;
		NSWeightBase wbase = null;
		for (int i=0;i<resList.size();i++) {
			if (br == null || resList.get(i).passTotal > br.passTotal) {
				br = resList.get(i);
				wbase = (NSWeightBase)nstests.get(tunedp, i).userTag;
			}
		}
		
		System.out.println(" best["+wbase+"]");	
		return wbase;
	}

	////////////////////////////
	// map the recorded test info for a NumberSet
	static class testInfo {
		int ns = -1;
		int state = 0;
		int rcrt = 0, rcrtOnly = 0, rcont = 0;
		int pcrt = 0, pcrtOnly = 0, pcont = 0;
		int tcrt = 0, tcrtOnly = 0, tcont = 0;
		// state in comparison for this NS
		// 0 is == to current group
		// 1 is >> than current group
		int comp = -1;	
		
		// possible; if cont and no win in group
		int rpsb = 0, ppsb = 0;
		int rrpsb = 0, prpsb = 0;
		int rrpsbm = 0, prpsbm = 0;
		
		// single value stats
		int preSCount = 0;
		int preSWinCount = 0;
		int grpSWinCount = 0;
		
		testInfo(int ns) {
			this.ns = ns;
			this.state = 1;
		}
		void reset() {
			tcrt = tcrtOnly = tcont = 0;
			pcrt = pcrtOnly = pcont = 0;
			rcrt = rcrtOnly = rcont = 0;
			grpSWinCount = preSWinCount = preSCount = 0;
			rpsb = ppsb = 0;
			rrpsb = prpsb = 0;
			rrpsbm = prpsbm = 0;
			comp = -1;
			ns = -1;
			state = 0;
		}
	}
	//
	// Test taker map for all NS evaluated
	//
	static class OptTesterMap {
		class tResp {
			int ns;
			int count;
			double probability;
			tResp(int ns, int count, double probability) {
				set(ns, count, probability);
			}
			void set(int ns, int count, double probability) {
				this.ns = ns;
				this.count = count;
				this.probability = probability;
			}
		}
		class tPosition {
		 	// list of contains/wins sorted by count (OR probability ?)
			int rating = 0;
			// raw mapping
			protected List<tResp> rawCont = null;
			protected List<tResp> rawWin = null;
			int rawContCnt = 0, rawWinCnt = 0;
			// prepped mapping
			protected List<tResp> prepCont = null;
			protected List<tResp> prepWin = null;
			int prepContCnt = 0, prepWinCnt = 0;
			// current mapping
			protected List<tResp> curCont = null;
			protected List<tResp> curWin = null;
			tPosition() {
				rawCont = new ArrayList<>();
				prepCont = new ArrayList<>();
				curCont = new ArrayList<>();
			}
			int getRContCount() {
				return rawContCnt;
			}
			int getRWinCount() {
				return rawWinCnt;
			}
			int getPContCount() {
				return prepContCnt;
			}
			int getPWinCount() {
				return prepWinCnt;
			}
			int getCWinCount() {
				if (curWin == null) return 0;
				return curWin.size();
			}
			int getCContCount() {
				if (curCont == null) return 0;
				return curCont.size();
			}
		}
		
		// numberSet info
		// linear list of ns number to set
		protected List<List<Integer>> nsMapping = null;
		protected HashMap<Integer, VResultSet> nsMapRs = null; 
		// map cur ns-value to nsMapping ns-value
		protected HashMap<Integer, Integer> nsCurMap = null; 
		// for parent nsid get childnsid
		protected HashMap<Integer, Integer> nsChildMap = null; 

		// tester info
		protected List<tPosition> testPositions = null;
		protected HashMap<Integer, testInfo> nsState = null;
		protected int total = 0;	
		int pNoAnswerCnt = 0, pNoneCnt = 0, pEasyCnt = 0, pSimpleCnt = 0, pHardCnt = 0;
		int cNoAnswerCnt = 0, cNoneCnt = 0, cEasyCnt = 0, cSimpleCnt = 0, cHardCnt = 0;
		protected List<tResp> trFreeList = null;
		protected int MAX_TR_FREE = 2000000;
		
		OptTesterMap(int total) {
			this.nsMapping = new ArrayList<>();
			this.testPositions = new ArrayList<>();
			this.trFreeList = new ArrayList<>();
			this.nsMapRs = new HashMap<>();
			this.nsCurMap = new HashMap<>();
			this.nsChildMap = new HashMap<>();
			
			this.total = total;
			this.MAX_TR_FREE = total * MAX_NS_PER_PASS;
			this.nsState = new HashMap<>();
			for (int i=0;i<total;i++) {
				this.testPositions.add(new tPosition());
			}
		}
		// get info
		testInfo getInfoMap(List<Integer> numberSet) {
			int ns = this.findCfgNS(numberSet);
			return nsState.get(ns);
		}
		// get result set retained
		VResultSet getResultSetNS(int ns) {
			return nsMapRs.get(ns);
		}
		int getNSforDpNs(int dpns) {
			if (nsCurMap == null) return -1;
			Integer ns = nsCurMap.get(dpns);
			if (ns == null) return -1;
			return ns;
		}
		
		// add numberSet, get back the ns number used here
		int addCfgNSFormat(List<Integer> numberSet, boolean addAfter) {
			nsMapping.add(new ArrayList<>(numberSet));
			int ns = nsMapping.size()-1;
			// map to child
			List<Integer> ch = makeChildNS(numberSet, addAfter);
			int chns = this.findCfgNS(ch);
			if (chns >= 0) nsChildMap.put(ns, chns);	
			return ns;
		}
		// make child with first or last
		protected List<Integer> makeChildNS(List<Integer> numberSet, boolean addAfter) {
			if (numberSet.size() < 2) return null;
			List<Integer> ns = new ArrayList<>(numberSet);
			if (addAfter) ns.remove(ns.size()-1);
			else ns.remove(0);
			return ns;
		}
		// get child
		// child is exact with out extended position
		// x - x x x  ->  - x x x <added before>
		// - x x x  ->  - x x     <added after>	
		int getChildNS(int ns) {
			Integer n = nsChildMap.get(ns);
			if (n == null) return -1;
			return n;
		}
		// numbers in mapped sets need to move over on prepend windowsize
		// ISSUE: this does not correct NS for center splits like char / etc.. 
		void remapNumberSetsWindowExtend() {
			for (int i=0;i<nsMapping.size();i++) {
				for (int p=0;p<nsMapping.get(i).size();p++) {
					int v = nsMapping.get(i).get(p);
					nsMapping.get(i).set(p, v+1);
				}
			}			
		}

		// find a numberSets ns-value
		int findCfgNS(List<Integer> numberSet) {
			if (numberSet == null) return -1;
			for (int i=0;i<nsMapping.size();i++) {
				if (MLNumberSetUtil.compareSet(numberSet, nsMapping.get(i))) return i;
			}
			return -1;
		}
		protected void recycle(List<tResp> l) {
			if (l == null) return;
			if (trFreeList.size() < MAX_TR_FREE) trFreeList.addAll(l);
			l.clear();
		}
		protected void recycle(List<tResp> l, int maxSize) {
			if (l == null) return;
			if (maxSize <= 0) {
				recycle(l);
				return;
			}
			while (l.size() > maxSize) {
				if (trFreeList.size() < MAX_TR_FREE) trFreeList.add(l.remove(l.size()-1));
				else l.remove(l.size()-1);
			}
		}

		protected tResp getTr(int ns, int count, double probability) {
			tResp tr = null;
			if (trFreeList.size() > 0) {
				tr = trFreeList.remove(trFreeList.size()-1);
				tr.set(ns, count, probability);
				return tr;
			}
			return tr = new tResp(ns, count, probability);
		}
		
		// map set of raw
		void mapRaw(Optimiser opt, List<VResultSet> rsl, List<VResultSet> crsl, VResultSet curGrp) {
			// add as before
			boolean addAfter = opt.tunedp.getCfgScratchBool("posLastAddAfter");

			HashMap<Long, Integer> vidNotpMap = new HashMap<>();
			HashMap<Long, Integer> vidpMap = new HashMap<>();
			
			// use dp to get correct numberSet values
			for (int i=0;i<rsl.size();i++) {
				VResultSet rs = rsl.get(i);
				VResultSet crs = crsl.get(i);
				
				int dpns = (int)rs.val2;
				if (this.total != rs.getContains().size()) continue;
				
				// get NS
				List<Integer> numberSet = opt.tunedp.getNS(dpns);
				int ns = findCfgNS(numberSet);
				if (ns >= 0) continue;
				
				ns = addCfgNSFormat(numberSet, addAfter);							
				testInfo nsInfo = new testInfo(ns);
				nsInfo.state = 1;
				nsState.put(ns, nsInfo);
				if (opt.tunedp.getCfgWindowSize() == 1) continue; // identity hasn't this info with it, and no value to add it
				
				vidNotpMap.clear();
				vidpMap.clear();
				
				// for each value in dataSet
				for (int p=0;p<this.total;p++) {
					containInfo cpCont = rs.getContains(p);
					if (cpCont == null) continue;
					if (cpCont.vid == 0 || cpCont.vid == -1 || cpCont.vid == -1 || opt.tunedp.getAccumulator(dpns, cpCont.vid) == null) continue;
					
					// have correct
					tPosition tp = testPositions.get(p);		
					tResp tr = getTr(ns, cpCont.count, cpCont.probability);
					tp.rawCont.add(tr);
					Collections.sort(tp.rawCont, trSortProb);
					tp.rawContCnt++;
					nsInfo.rcont++;
					if (cpCont.win) {
						// win this one?
						if (tp.rawWin == null) tp.rawWin = new ArrayList<>();
						tp.rawWin.add(tr);
						Collections.sort(tp.rawWin, trSortProb);
						tp.rawWinCnt++;
						nsInfo.rcrt++;
					}
					// compare with group
					containInfo grpCont = curGrp.getContains(p);
					if (!grpCont.win) {
						if (cpCont.probability > grpCont.wprobability) {
							nsInfo.rpsb++; // higher prob than winning prob
						}	
						if (!cpCont.win) {
							// get combined probability for win value
							double pr = ((grpCont.probability * opt.curNSInDp.size()) + cpCont.probability) / (opt.curNSInDp.size()+1);
							if (pr > grpCont.wprobability) {
								// factor in other values for this set that have higher probabilty
								nsInfo.rrpsb++; // higher prob than winning prob
								Integer pcnt = vidpMap.get(cpCont.vid);
								if (pcnt == null) {
									vidpMap.put(cpCont.vid, 1);
									vidNotpMap.put(cpCont.vid, 0);
								} else {
									vidpMap.put(cpCont.vid, pcnt+1);
								}
							}
						}
					}
				}
				// FIXME check group win for these?	

				// check the win/possibles for false positive counts
				for (int p=0;p<this.total;p++) {
					containInfo cpCont = rs.getContains(p);
					if (cpCont == null) continue;
					if (cpCont.win) continue;
					if (cpCont.vid == 0 || cpCont.vid == -1 || cpCont.vid == -1 || opt.tunedp.getAccumulator(dpns, cpCont.vid) == null) continue;
					containInfo grpCont = curGrp.getContains(p);
					if (!grpCont.win) continue;
					
					containInfo combCont = crs.getContains(p); // get combind

					// group winner / this NS not win
					Integer pcnt = vidNotpMap.get(cpCont.vid);
					if (pcnt != null) {
						// without this it would win
						// check impact on winnning value		
						if (!combCont.win) {
							// not win together
							vidNotpMap.put(cpCont.vid, pcnt+1);	// lose together
						}
					}
				}
				
				
				// check the results
				nsInfo.rrpsbm = 0;
				for (Long tvid:vidpMap.keySet()) {
					int cF = vidNotpMap.get(tvid);
					int cP = vidpMap.get(tvid);
					if (cF < cP) nsInfo.rrpsbm += cP - cF;
				}
				
				//System.out.println("MAP RAW NS["+ns+"/"+nsInfo.ns+"] ["+NumberSets.setToStringPosition(numberSet, tunedp.getCfgWindowSize(), tunedp.getCfgFrameFocus())+"] "
				//		+ "r["+nsInfo.rcont+" / "+nsInfo.rcrt+" / "+ nsInfo.rcrtOnly+"]");
			}
			// map raw info
			//nsMapVectors(dp, rs, ns);
		}

		// map set of preped
		void mapPreped(Optimiser opt, List<VResultSet> rsl, List<VResultSet> crsl, VResultSet curGrp) {
			
			//ResultSet rc = VegTest.testSets(opt.vML, opt.dimensionTag, opt.dataPlaneTag, opt.fulltuneSets, opt.fulltuneValueSets, -1);
			HashMap<Long, Integer> vidNotpMap = new HashMap<>();
			HashMap<Long, Integer> vidpMap = new HashMap<>();
			
			// use dp to get correct numberSet values
			for (int i=0;i<rsl.size();i++) {
				VResultSet rs = rsl.get(i);
				VResultSet crs = crsl.get(i);
				
				int dpns = (int)rs.val2;
				// get NS
				if (dpns < 0) continue;
				List<Integer> numberSet = opt.tunedp.getNS(dpns);
				int ns = findCfgNS(numberSet);
				testInfo nsInfo = nsState.get(ns);
				if (nsInfo.state > 1) continue;
				
				// retain this NS
				nsMapRs.put(ns, rs);
				
				// compare
				nsInfo.comp = -1;
				if (rs.getPassTotal(opt.optType) > curGrp.getPassTotal(opt.optType)) nsInfo.comp = 1;
				else if (rs.getPassTotal(opt.optType) == curGrp.getPassTotal(opt.optType)) nsInfo.comp = 0;
				
				if (opt.tunedp.getCfgWindowSize() == 1) {
					nsInfo.state = 2;
					continue; // identity hasn't this info with it, and no value to add it
				}
				
				vidNotpMap.clear();
				vidpMap.clear();
				
				// map it
				for (int p=0;p<this.total;p++) {
					containInfo cpCont = rs.getContains(p);
/*
 * FIXME
 * this count is less than the actual increase on some
 * - need only values from NS vids (no defaults)
 * - need group-win-probabilty AND group correct-probabilty			
 */
					if (cpCont == null) continue;			
					if (opt.tunedp.getAccumulator(dpns, cpCont.vid) == null) continue;

					// have correct
					tPosition tp = testPositions.get(p);		
					tResp tr = getTr(ns, cpCont.count, cpCont.probability);
					tp.prepCont.add(tr);
					Collections.sort(tp.prepCont, trSortProb);
					tp.prepContCnt++;
					nsInfo.pcont++;
					
					if (cpCont.win) {
						// check if win
						if (tp.prepWin == null) tp.prepWin = new ArrayList<>();
						tp.prepWin.add(tr);
						Collections.sort(tp.prepWin, trSortProb);
						tp.prepWinCnt++;
						nsInfo.pcrt++;
					}
					
					// compare singles with combined
					containInfo combCont = crs.getContains(p); // get combind
					// account for singles		
					if (cpCont.vpSize == 1) {
						nsInfo.preSCount++;
						if (cpCont.win) {
							nsInfo.preSWinCount++;
						}
						if (combCont.win) {
							nsInfo.grpSWinCount++;
						}
					}
					
					// compare with group
					containInfo grpCont = curGrp.getContains(p);
					if (!grpCont.win) {	
						if (cpCont.probability > grpCont.wprobability) {
							nsInfo.ppsb++; // higher prob than winning prob
						}
						if (!cpCont.win) {
							// get combined probability
							double pr = ((grpCont.probability * opt.curNSInDp.size()) + cpCont.probability) / (opt.curNSInDp.size()+1);							
							// need opCount.prob for the false positive
							if (pr > grpCont.wprobability) {
								nsInfo.prpsb++; // higher prob than winning prob
								Integer pcnt = vidpMap.get(cpCont.vid);
								if (pcnt == null) {
									vidpMap.put(cpCont.vid, 1);
									vidNotpMap.put(cpCont.vid, 0);
								} else {
									vidpMap.put(cpCont.vid, pcnt+1);
								}
							}			
						}
					
					} else if (!cpCont.win) {
						// FIXME 
						// how many does this force to loose?		
						// 1) lower the winning probability
						// 2) have other higher probability
						// 3) increase another to highest probability (info here can not account)
							// need probabiltiy for other
					}
				}

				// FIXME check group win for these?	

				// check the win/possibles for false positive counts
				for (int p=0;p<this.total;p++) {
					containInfo cpCont = rs.getContains(p);
					if (cpCont == null) continue;
					if (cpCont.win) continue;
					if (cpCont.vid == 0 || cpCont.vid == -1 || cpCont.vid == -1 || opt.tunedp.getAccumulator(dpns, cpCont.vid) == null) continue;
					
					containInfo grpCont = curGrp.getContains(p);
					if (!grpCont.win) continue;
					
					containInfo combCont = crs.getContains(p); // get combind
					
					// group winner / this NS not win
					Integer pcnt = vidNotpMap.get(cpCont.vid);
					if (pcnt != null) {
						// without this it would win
						// check impact on winnning value					
						// check if it pushes other value to the winner
						if (!combCont.win) {
							vidNotpMap.put(cpCont.vid, pcnt+1);	// lose together
						}			
					}
				}
									
				// check the results
				nsInfo.prpsbm = 0;
				for (Long tvid:vidpMap.keySet()) {
					int cF = vidNotpMap.get(tvid);
					int cP = vidpMap.get(tvid);
					if (cF < cP) nsInfo.prpsbm += cP - cF;
				}
				
				nsInfo.state = 2;
				
				//System.out.println("MAP PREP NS["+ns+"/"+nsInfo.ns+"] ["+NumberSets.setToStringPosition(numberSet, tunedp.getCfgWindowSize(), tunedp.getCfgFrameFocus())+"] "
				//		+ "p["+nsInfo.pcont+" / "+nsInfo.pcrt+" / "+ nsInfo.pcrtOnly+"]");
				// map the vector sets				
				//nsMapVectors(dp, rs, ns);
			}
		}
		// map set of preped
		int mapFinal(Optimiser opt, VResultSet rs, VResultSet crs) {
			int preSWinCount = 0, grpSWinCount = 0, preSCount = 0;
			// map it
			int cnt = 0;
			for (int p=0;p<this.total;p++) {
				containInfo cpCont = rs.getContains(p);
				if (cpCont == null) continue;			

				// account for singles		
				if (cpCont.vpSize == 1) {
					preSCount++;
					if (cpCont.win) preSWinCount++;
					
					// compare singles with combined
					containInfo combCont = crs.getContains(p); // get combind
					if (combCont.win) grpSWinCount++;
				} 
				cnt++;
			}
			//System.out.print("i["+rs.passSingleTotal+"/"+rs.failSingleTotal+"] tot["+preSCount+"]");

			return preSWinCount - grpSWinCount;
		}

		void reMapNSMapping(Optimiser opt) {
			// reset current NS mapping
			nsCurMap.clear();
			for (int i=0;i<opt.tunedp.getNSCount();i++) {
				int ns = findCfgNS(opt.tunedp.getNS(i));
				nsCurMap.put(i, ns);
			}			
		}
		
		//
		// map the current dataPlane AND cleanup raw & prep
		// can check the vectorCount in each NS to determine if it has changed (if using reduce)		
		void mapCurrentAndSet(Optimiser opt, boolean change) {
			// set/reset
			if (change) {
				// reset current NS mapping
				reMapNSMapping(opt);

				// clear current info lists
				for (int p=0;p<this.total;p++) {
					recycle(testPositions.get(p).curCont);
					recycle(testPositions.get(p).curWin);
				}
				// clear NS stats for current
				for (testInfo ti:nsState.values()) {
					ti.tcont = ti.tcrt = ti.tcrtOnly = 0;
				}
				
				//
				// get fresh results for all current ns in the dp
				List<VResultSet> rsl = opt.testDPIndividuals();
				for (int i=0;i<rsl.size();i++) {
					VResultSet rs = rsl.get(i);
					// get NS	
					int ns = -1;// default fullSet
					if (rs.val2 >= 0) ns = getNSforDpNs((int)rs.val2);
					if (ns == -1) {
						// FIXME group	
						// keep in different list
						continue;
					}
					testInfo nsInfo = nsState.get(ns);
					if (nsInfo.state > 3) continue;
					
				//	System.out.println("MAP CUR NS["+(int)rs.val2+"/"+ns+"] ["+opt.tunedp.getNSFormatString((int)rs.val2)+"]");
	
					for (int p=0;p<this.total;p++) {
						containInfo cpCont = rs.getContains(p);
						if (cpCont == null) continue;
						tPosition tp = testPositions.get(p);		
						tResp tr = getTr(ns, cpCont.count, cpCont.probability);
						tp.curCont.add(tr);
						Collections.sort(tp.curCont, trSortProb);
						nsInfo.tcont++;
						if (rs.getContains(p).win) {
							if (tp.curWin == null) tp.curWin = new ArrayList<>();
							tp.curWin.add(tr);
							Collections.sort(tp.curWin, trSortProb);
							nsInfo.tcrt++;
						}					
					}
					nsInfo.state = 3;
				}
				
				// update NS stats for current
				for (int p=0;p<this.total;p++) {
					tPosition tp = testPositions.get(p);
					for (int i=0;i<tp.curCont.size();i++) {
						tResp tr = tp.curCont.get(i);
						nsState.get(tr.ns).tcont++;			
					}
					for (int i=0;i<tp.getCWinCount();i++) {
						tResp tr = tp.curWin.get(i);
						testInfo tinf = nsState.get(tr.ns);
						tinf.tcrt++;
						if (tp.getPWinCount() == 1) tinf.tcrtOnly++;
					}
				}
			}
			
			//
			// cleanup raw / prep; even if no change
			//
			// somthing to base max size on
			int maxWinCount = opt.tunedp.getNSCount() + 10;
			int maxCountCount = opt.tunedp.getNSCount();
			
			for (int p=0;p<this.total;p++) {
				tPosition tp = testPositions.get(p);					
				if (tp.getRWinCount() > maxWinCount) {;
					recycle(tp.rawWin, maxWinCount);
					recycle(tp.rawCont);
				} else {
					recycle(tp.rawCont, maxCountCount);
				}
				if (tp.getPWinCount() > maxWinCount) {
					recycle(tp.prepWin, maxWinCount);
					recycle(tp.prepCont);
				} else {
					recycle(tp.prepCont, maxCountCount);
				}
			}
		}
		

		//
		// make raw/preped map for all ? save per NS
		// build per-ns mRInfo
		void mapAllPrepped(Optimiser opt) {	
			/*
			 * 1. Each in group seperate
			 * 2. Each new seperate / before AND after carve alone
			 * 3. group
			 * 4. Each new with group 
			 * 	
			 * 	-- group member required for group to get response correct
			 *  
			 * 
			 * - rank each problem
			 * - rank each numberSet
			 * - fscore EACH vector that wins to decide which is best
			 * 	- this needs to then account for 'ancestry' or relationship to determine what to do
			 * 
			 *  - what if we look at how common all other responses are to the problem ?
			 *  	- this could give another indication of what can be removed (those that present common incorrect responses vs those that present uncommon ones)
			 * 
			 * x- process to remove all but usefull vectors from a numberSet
			 *    - vectors that produce unique correct
			 *    - vectors that are correct and better than other numberSets
			 *    - vectors that are correct
			 *    - FLAGS to decide which sets to retain
			 *  
		 	*/
				
			// optimize set being added; logical & statistical	
			int nsTotCnt = opt.curSingleList.size() + opt.resComboList.size()+1; 
			int many = (int)((double)(nsTotCnt) * (double)0.50);
			if (many < 3) many = 3;
			int topSet = (int)((double)(nsTotCnt) * (double)0.80);
			int some = 4;
			if (some > many) some = many;
			
			// for each token in tuning set (flattend)
			pNoAnswerCnt = pNoneCnt = pEasyCnt = pSimpleCnt = pHardCnt = 0;
			cNoAnswerCnt = cNoneCnt = cEasyCnt = cSimpleCnt = cHardCnt = 0;
			
			// reset only... 
			for (testInfo tinf:nsState.values()) {
				tinf.pcrtOnly = tinf.rcrtOnly = 0;
			}
			
			for (int p = 0;p<this.total;p++) {
				tPosition tpos = testPositions.get(p);
				
				// rate this one
				tpos.rating = 1;
				
				// Any reference correct?
				if (tpos.getCContCount() == 0) {
					cNoneCnt++;
					if (tpos.getRContCount() == 0 && tpos.getPContCount() == 0) {
						pNoneCnt++;
						tpos.rating = 10;
					}
				}
				
				if (tpos.getCWinCount() == 0) {
					tpos.rating = 9;
					cNoAnswerCnt++;
					
				} 
				if (tpos.getPWinCount() == 0) {
					tpos.rating = 9;
					pNoAnswerCnt++;
				} else if (tpos.getRWinCount() == 0) {
					tpos.rating = 8;
				}
				// hard
				if (tpos.getPWinCount() == 1) {
					pHardCnt++;
				}
				if (tpos.getCWinCount() == 1) {
					cHardCnt++;
				}
				// simple
				if (tpos.getPWinCount() > some) {
					pSimpleCnt++;
				}
				if (tpos.getCWinCount() > some) {
					cSimpleCnt++;
				}
				// easy
				if (tpos.getPWinCount() > many) {
					pEasyCnt++;
				}
				if (tpos.getCWinCount() > many) {
					cEasyCnt++;
				}
/*
				System.out.println(" "+p+") " + tpos.rating
						+ " r["+tpos.rawWin.size()+"/"+tpos.rawCont.size()+"]"
						+ " p["+tpos.prepWin.size()+"/"+tpos.prepCont.size()+"]"
						+ " c["+tpos.curWin.size()+"/"+tpos.curCont.size()+"]"
						+ "    ns["+opt.tunedp.getNSCount()+"/"+opt.nsTotalTry+"]["+this.total+"]"
						);
*/	
					
				////////////////////////////////
				// Question with each test taker
				// for each matched value, add to its set
				if (tpos.getRWinCount() == 1) {
					nsState.get(tpos.rawWin.get(0).ns).rcrtOnly++;	
				}
				if (tpos.getPWinCount() == 1) {
					nsState.get(tpos.prepWin.get(0).ns).pcrtOnly++;				
				}
			}
			/*
			// clear NS stats for current
			for (testInfo ti:nsState.values()) {
				System.out.println(" ns["+ti.ns+"] {"+ti.state+"} "
						+ "r["+ti.rcont+" / "+ti.rcrt+" / "+ ti.rcrtOnly+"]"
						+ "p["+ti.pcont+" / "+ti.pcrt+" / "+ ti.pcrtOnly+"]"
						+ "c["+ti.tcont+" / "+ti.tcrt+" / "+ ti.tcrtOnly+"]"
						);
			}
			*/
		}
		
		// map set of raw
		void mapRawNSVectors(VDataPlane dp, List<VResultSet> rsl) {
			// use dp to get correct numberSet values
			for (int i=0;i<rsl.size();i++) {
				VResultSet rs = rsl.get(i);
				int dpns = (int)rs.val2;
				// get NS
				int ns = findCfgNS(dp.getNS(dpns));
				// map raw info
				nsMapVectors(dp, rs, ns);
			}
		}	
		
		//
		// remove sets of vectors that don't add value
		// - use child ns to make judements?
		//
		// get the current correct map for this in rss
		// - fscore and do carve?
		// - just carve cut-individual only
		// - match correct with actual values and remove un-related?
		//
		boolean nsMapVectors(VDataPlane dp, VResultSet rss, int ns) {
			int chns = getChildNS(ns);
			if (chns < 0) return false;
			
			// get chns correct
			VResultSet rsch = nsMapRs.get(chns);
			// get child ns
			List<Integer> chNsSet = nsMapping.get(chns);
			List<Integer> fNsSet = nsMapping.get(ns);
			int dpns = dp.findCfgNS(fNsSet);
			
			// get correct numberSet
			System.out.print("MAP_NS["+String.format("%3d", ns)+"/"+String.format("%3d", dpns)+"]["+dp.getNSFormatString(dpns)+"] ");
			if (dp.getNSBaseType() != NumberSetType.PowerSet) System.out.print("< [" + MLNumberSetUtil.setToStringPosition(chNsSet, dp.getCfgWindowSize(), dp.getCfgFrameFocus())+"]");		

			// make ns peer sets
			HashMap<Long, Set<Long>> nsPeerSets = dp.getNSHash(dpns).mapVectorPeers();

			// for each in set needed; win/loose/present	
			System.out.print("sets["+String.format("%6d", nsPeerSets.keySet().size())+"] => ");	
			if (dp.getVectorCount(dpns) == nsPeerSets.keySet().size()) {
				System.out.println("NO-MAPPINGS");	
				return true;
			}
				
			// do all this in preped: can bring back info that is missing by additional training
			//FIXME account for values as well
			HashMap<Set<Long>, Integer> winMap = new HashMap<>();
			HashMap<Set<Long>, Integer> pfailMap = new HashMap<>();
			HashMap<Set<Long>, Integer> chWinMap = new HashMap<>();
			HashMap<Set<Long>, Integer> chFailMap = new HashMap<>();
			
			int noAccum = 0;
			int noAccumB = 0;
			
			// for each each result in the test
			for (int i=0;i<rss.getContains().size();i++) {
				long vid = rss.getContains(i).vid;	
				
				// get peers
				Set<Long> sps = nsPeerSets.get(vid);
				boolean win = rss.getContains(i).win;				
				if (win) {
					// this is a win	
					Integer c = winMap.get(sps);
					if (c == null) winMap.put(sps, 1);
					else winMap.put(sps, c+1);			
				} else {
					// this is a false positive	
					Integer c = pfailMap.get(sps);
					if (c == null) pfailMap.put(sps, 1);
					else pfailMap.put(sps, c+1);							
				}
				// for each peerSet track 
				// win/fail/contains
				// count for each vid?
				
				Accum ac = dp.getAccumulator(dpns, vid);
				if (ac == null) {
					// no accumulators for these: many missing from logical; others are for vectors not trained into the model
					noAccum++;
					if (vid == -2 || vid == -1 || vid == 0) noAccumB++;
				} else {
					// check child win
					long chvid = ac.getVectChildVid(); // child of vector
					if (chvid != 0) {
						// check child win/fail counts
						boolean chwin = rsch.getContains(i).win;
						long xchvid = rsch.getContains(i).vid; // child NS vid
						if (xchvid == chvid) {
							// FIXME
							if (chwin) {
								Integer c = chWinMap.get(sps);
								if (c == null) chWinMap.put(sps, 1);
								else chWinMap.put(sps, c+1);	

							}
						} else {
							Integer c = chFailMap.get(sps);
							if (c == null) chFailMap.put(sps, 1);
							else chFailMap.put(sps, c+1);	

						}
					}
				}

			}
			
			List<Long> vidList = new ArrayList<>();

			int mix = 0, fwcnt = 0, fonly = 0, wwcnt = 0, wfcnt = 0;

			// fail and no wins
			for (Set<Long> set:pfailMap.keySet()) {
				if (set == null) continue;
				if (winMap.get(set) == null) {
					// fail only
					fonly++;
					//vidList.addAll(set);
					if (chFailMap.get(set) == null && chWinMap.get(set) != null) {
						// child wins only!
						vidList.addAll(set);
						fwcnt++;
					}
				} else {
					// fails and wins
					if (set.size() == 1) {
						// single vector .. 
				//		vidList.addAll(set);
					} 
					if (chFailMap.get(set) == null && chWinMap.get(set) != null) {
						// child wins only!
						vidList.addAll(set);
						fwcnt++;
					}
					mix++;
				}
			}
			// wins check child wins too
			for (Set<Long> set:winMap.keySet()) {
				if (chFailMap.get(set) == null && chWinMap.get(set) != null) {
					// child wins only!
					vidList.addAll(set);
					wwcnt++;
				} else if (chFailMap.get(set) != null && chWinMap.get(set) == null) {
					// child fails only
					wfcnt++;
				}	
			}
						
			// win wins .. which is better?
			// FIXME
						
			System.out.println("no["+String.format("%6d", noAccum)+"] "
					+ " w["+String.format("%5d", winMap.keySet().size())+"]fail["+String.format("%5d", pfailMap.keySet().size())+"]mix["+String.format("%5d", mix)+"]fonly["+String.format("%5d", fonly)+"] "
					+ "  chww["+String.format("%6d", wwcnt)+"]chfw["+String.format("%4d", fwcnt)+"]chwf["+String.format("%3d", wfcnt)+"]");
			
			if (dpns == dp.getCfgNSIdentityNumber() || dpns == dp.getCfgNSFullNumber()) return true;
			
			// clear never used
			// clear never win
			// if all wins have wining child, then clear and let child win; unless child loses more often (fscore win)
			int vd = 0;
			//vd = tunedp.removeAllNSVectors(dpns, vidList);
			System.out.println("  fonly["+String.format("%3d", fonly)+"] total["+vidList.size()+"] => ["+vd+"]");
			
			return true;
		}
		
		


		// sort count first
		static Comparator<tResp> trSortCount = new Comparator<tResp>() {
	        @Override
	        public int compare(tResp lvp, tResp rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	        	
	        	if (lvp.count < rvp.count) return 1;
	        	if (lvp.count > rvp.count) return -1;
	        	if (lvp.probability < rvp.probability) return 1;
	        	if (lvp.probability > rvp.probability) return -1;
	        	return 0;  
	        }
	    };
		// sort prob first
		static Comparator<tResp> trSortProb = new Comparator<tResp>() {
	        @Override
	        public int compare(tResp lvp, tResp rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	        	
	        	if (lvp.count < rvp.count) return 1;
	        	if (lvp.count > rvp.count) return -1;
	        	if (lvp.probability < rvp.probability) return 1;
	        	if (lvp.probability > rvp.probability) return -1;
	        	return 0;  
	        }
	    };
	}
	
	
	/*
	// test framer increment window
	for (int p=0;p<20;p++) {
		
		List<Integer> nsCurList = new ArrayList<>();
		int positionLastAdd = tunedp.getCfgScratchInt("posLastAdd");
		boolean positionLastAddAfter = tunedp.getCfgScratchBool("posLastAddAfter");
		List<List<Integer>> nsSet = new ArrayList<>();

		// add just one
		if (positionLastAddAfter) {
			// before
			positionLastAdd++;
			tunedp.setCfgScratch("posLastAdd", positionLastAdd);
			System.out.print("EXT_NS_BEFORE["+tunedp.getCfgWindowSize()+" -> "+(tunedp.getCfgWindowSize()+1)+"]pos["+positionLastAdd+"]");			
			tunedp.incWindowSize(false);
			//tunedp.addIncWindowSizeNS(positionLastAdd, false, nsCurList, nsSet); // before	
			tunedp.getFramer().getIncWindowSizeNS(dp, positionLastAdd, false, nsCurList, nsSet);
			// update testSet numberSet map
			testMap.remapNumberSetsWindowExtend();
		} else {
			// after
			System.out.print("EXT_NS_AFTER["+tunedp.getCfgWindowSize()+" -> "+(tunedp.getCfgWindowSize()+1)+"]pos["+positionLastAdd+"]");			
			tunedp.incWindowSize(true);
			//tunedp.addIncWindowSizeNS(positionLastAdd, true, nsCurList, nsSet); // after
			tunedp.getFramer().getIncWindowSizeNS(dp, positionLastAdd, true, nsCurList, nsSet);
		}	
		if (p == 10) return "end";
	}*/
	
	
	String perc(double p) {
	    return String.format("%5s", String.format("%.2f", p)+"%");
	}
}
