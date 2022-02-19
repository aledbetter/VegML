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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import vegml.VDRetainSet.DsRetainVal;
import vegml.VegML.NSWeightBase;
import vegml.VResultSet.ResultValueStat;
import vegml.VegML.DPState;
import vegml.VegML.PredictionType;
import vegml.VegML.ProbMethod;
import vegml.ValProb;
import vegml.VegTest.testModAcValue;
import vegml.VegTest.TestMod;
import vegml.VegTest.TestModSet;
import vegml.Data.VDataSets;

/**
 * Statistical tuning methods
 * This should really be broken out to a class per method; but that would pollute the directory
 *
 */
class OptimizerStatistical {

	/*
	 * NOTES:
	 * 1) if identity has only one value -> all but identity that yeild value can be removed: SBR/etc
	 * 	- scan Identity for values: count how many vectors match a value; if 1-and-only-1 then it is so
	 * 
	 * 2) create 2 modles -> many models
	 * 	 - one optimized for unknowns that has only context
	 *   - one optimized only for knowns 
	 *   -> this should produce better results
	 *   -> a model to produce best results for each predict type may be sensable
	 *   
	 * 3) api to run multiple tests
	 * 	- pass in list of numberSet wieghts -> 0 means off
	 *  - for each a list of minCounts for values -> 0 means not included
	 *  - This will work better if the last N are addressed from each THEN it is repeated with last N+x
	 *  	- this would factor in the changes from the first set of reductions
	 *  	or complete it, then do it again?
	 *  
	 * 4) for weight/NS select
	 *  - run entire set -> remove best 
	 *   - iterate
	 *   - can include weight changes 
	 *   
	 *  5) merge optimize
	 *    - run both models, get full results
	 *    - run merge values in loop NOT in full process
	 *    - try non-linear merges.. 
	 *    		- weighting values	
	 *    		- not including values
	 */

	public static int MAX_EXCLUDE = 50;				// max count of fails to exclude
	public static boolean noEndThreads = false;
	public static boolean allowIdentity = false;
	public static boolean allowFullest = false;
	static int MAX_STEPS = 14;
	static int MIN_PROGRESS = 10;
	public static double mergeValue = 1;	
	public static boolean autoMergeValue = true;	
	
	
	OptimizerStatistical() {
		MAX_EXCLUDE = 50;				// max count of fails to exclude
		noEndThreads = false;
		allowIdentity = false;
		allowFullest = false;		
		
		MAX_STEPS = 14;
		MIN_PROGRESS = 10;	
		
		mergeValue = 1; // default
		autoMergeValue = true;
	}
	

	public static int carveMergeSteps(String dimensionTag, String dataPlaneTag, String initName, 
			String pre, int window, NSWeightBase wType, 
			PredictionType optType, double dropPercent, int minSet, int phases, 
			int minProgress, boolean identityFilter, 
			boolean noBackingUp, boolean useReduction, double downWeight,
			String mergeDimensionTag, String mergeDataPlaneTag, boolean mergeSetIsbase,
			int setFullData, VDataSets dss) {		
		if (minProgress < 1) minProgress = MIN_PROGRESS;
		if (phases < 0) phases = 3; // default
				
		int step = 1;
		int lp = 0;

		for (;step < MAX_STEPS;step++) {
			int r = carveMergeStepN(step, dimensionTag, dataPlaneTag, initName, pre, window, wType, optType, 
									dropPercent, minSet, phases, identityFilter, noBackingUp, useReduction, downWeight, 
									mergeDimensionTag, mergeDataPlaneTag, mergeSetIsbase, null,
									setFullData, dss);
			if (r == 0) {
				System.out.println("CHANGE COMPLETE 0["+lp+"] >> ["+r+"] increase["+(r-lp)+"] step["+step+"]");
				return step;
			}
			initName = null;
			if (lp == 0 || step == 1) {
				System.out.println("CHANGE INIT["+r+"] step["+step+"]");
				lp = r;
			} else if (step >= (phases+2) && r < (lp+minProgress)) {
				System.out.println("CHANGE COMPLETE["+lp+"] >> ["+r+"] increase["+(r-lp)+"] step["+step+"]");
				return step; // not enough progress
			} else {
				System.out.println("CHANGE FROM["+lp+"] >> ["+r+"] increase["+(r-lp)+"] step["+step+"]");
				lp = r;				
			}
			// NOTE: the last one may not be the best -> check the diff between it and prior (std and AMP)
		}
		
		return step;
	}

	
	public static int carveMergeStepN(int step, String dimensionTag, String dataPlaneTag, String initName, 
			String pre, int window, NSWeightBase wType, 
			PredictionType optType, double dropPercent, int minSet, int phases, boolean identityFilter,
			boolean noBackingUp, boolean useReduction, double downWeight,
			String mergeDimensionTag, String mergeDataPlaneTag, boolean mergeSetIsbase,
			List<Integer> nsSetFirst,
			int setFullData, VDataSets dss) {		

		if (!useReduction && downWeight <= 0) downWeight = 0.8;
		boolean cleanup = true;
		boolean ctlGroup = false;
		
		boolean fullData = false;
	//	if (optType == PredictionType.PredictUnknown || optType == PredictionType.AnyUnknown) fullData = true;		
		if (setFullData == 100) fullData = true;
		
		String fd = "";
		if (step != 0 && !fullData) fd ="p";
		String fn = pre+"-"+window+"-w"+wType.ordinal()+"-s"+fd+(step-1)+".veg";
		if (initName != null) fn = initName;
		
		VegML vML = VegML.load(fn);
		if (vML == null) return 0;
		System.out.println("OPTIMIZE step["+step+"]["+dimensionTag+"/"+dataPlaneTag+"] <== ["+fn+"]");

		VDataPlane dp = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) {
			System.out.println("ERROR["+dimensionTag+"/"+dataPlaneTag+"] not found");
			return 0;
		}
		if (step == 1) {
			// save entry
			dp.setCfgFramerArg(null);
			dp.getVegML().save(pre+"-"+dp.getCfgWindowSize()+"-w"+wType.ordinal()+"-s0.veg");			
		}
		// get merge if there
		VDataPlane mergeSetDp = null;
		if (mergeDimensionTag != null) {
			mergeSetDp = vML.getDataPlane(mergeDimensionTag, mergeDataPlaneTag);
			if (mergeSetDp == null) {
				System.out.println("ERROR Merge["+mergeDimensionTag+"/"+mergeDataPlaneTag+"] not found");
				return 0;
			}
		}

		// change idenityt filter
		boolean idFilterState = dp.isCfgIdentityOnly();
		dp.setCfgIdentityOnly(identityFilter);
		
		boolean noPositive = false;
		boolean noZero = false;
	
		// these should be linked to the models data		
		int [] maxDropSet1 = {5, 10, 15, 20, 25, 30, 35}; // top percent of orriginal
		int [] maxDropSet = maxDropSet1;
		maxDropSet = null;

		// min drop
		int [] minDropSetNaive = {1};
		int [] minDropSet0 = {0, 1};
		int [] minDropSet1 = {0, 1, 2, 3, 4, 5, 10};
		int [] minDropSet2 = {0, 1, 2, 3, 4, 5, 10, 15, 20, 25, 50, 100};
		int [] minDropSet3 = {0, 1, 2, 3, 4, 5, 10, 15, 20, 25, 50, 100, 250, 500, 750, 
								1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 
								15000, 20000, 30000, 50000}; // should be generated for numberset based on % and max
		int [] minDropSet = minDropSet0;
			
		if (minSet == 0) minDropSet = minDropSet0;
		else if (minSet == 1) minDropSet = minDropSet1;
		else if (minSet == 2) minDropSet = minDropSet2;
		else if (minSet == 3) minDropSet = minDropSet3;
				
		// per step..
		if (step == 5) {
			if (phases >= 4) noPositive = true;
		} else if (step == 4) {
			if (phases >= 3) noPositive = true;
		} else if (step == 3) {
			if (phases >= 2) noPositive = true;
			if (phases >= 3) noZero = true;
		} else if (step == 2) {
			if (phases >= 2) noPositive = true;
			if (phases >= 1) noZero = true;
		}
		
		// first pass reductions and such
		if (step == 2) {
			if (optType == PredictionType.Recall || optType == PredictionType.RecallPredict || optType == PredictionType.AnyRecall) {
				dropPercent = 0;			
			} else if (optType == PredictionType.PredictUnknown || optType == PredictionType.AnyUnknown) {
			//	dropPercent = dropPercent / 2;
				dp.setCfgIdentityOnly(false);
				dp.setCfgProbMethod(ProbMethod.Average);
				dp.clearNSData(dp.getCfgNSFullNumber());
				// remove all other number sets with identity
				for (int i=0;i<dp.getNSCount();i++) {
					if (i == dp.getCfgNSIdentityNumber()) continue;
					if (i == dp.getCfgNSFullNumber()) continue;
					if (dp.isCfgNSContext(i)) continue;
					dp.setCfgNSTurnedOff(i, true);
					dp.clearNSData(i);
					dp.setCfgNSWeight(i, 0);
				}
				// remove turned off numberSets
				dp.removeCfgNSTurnedOff();
				
				// identity
				//dp.clearNSData(dp.getCfgNSIdentityNumber()); // can't optimize if this
				// removing in-frequent words from identity that are of non-closed sets can expand the test range, providing better options
	// FIXME need to do this ONLY base on values: N/J/R/V			
			//	int scnt = dp.getNSAccumulatorCount(dp.getCfgNSIdentityNumber());
			//	dp.removeAllNSAccum(dp.getCfgNSIdentityNumber(), 1, false);
			//	int ecnt = dp.getNSAccumulatorCount(dp.getCfgNSIdentityNumber());
			//	System.out.println("UNKNOWN IdentityNS["+dp.getCfgNSIdentityNumber()+"] reduction["+scnt+" => "+ecnt+"] ");

				// including extended data in testing also will improve if some have been removed
				//dp.print(true);
				dp.setCfgNSWeight(wType);
			}
		}
		
		@SuppressWarnings("unchecked")
		HashMap<String, Integer> dropMap = (HashMap<String, Integer>)dp.getCfgScratch("dropMap");
		if (dropMap == null) dropMap = new HashMap<>();
		if (wType == null) wType = NSWeightBase.None;
			
		int passTotal = 0;
		if (step == 1) {
			int cstep = dp.getCfgScratchInt("optStep");
			if (cstep > 1) {
				dp.setCfgFramerArg(null);
				dp.getVegML().save(pre+"-"+dp.getCfgWindowSize()+"-w"+wType.ordinal()+"-s"+fd+step+".veg");
				return 100;
			}
			// Show per value Per numberSet pass/fail/pfail accountabilty 
			// this information could be used to reduce noise, weight values, or generate rules
			// - TODO: make a filter with these, so they can not be guessed if NOT in identity
			// - TODO: this process can work for closed-ended sets as well; best for smaller ones
			if (wType == NSWeightBase.None) {
				dp.setCfgNSWeight(NSWeightBase.Flat);
				dp.setCfgPCalc(VegML.probOnlyCalc);			
			} else {
				dp.setCfgNSWeight(wType);
				dp.setCfgPCalc(VegML.probNumberSetWeightCalc);
			}
			// numberSet weights locked
			dp.setCfgNSWeightsLocked(true);
			
			// map the dataSet
			passTotal = OptimizerLogical.reduceByDefinition(dp, optType, dropMap, false);	
			passTotal += OptimizerLogical.reduceByIdentity(dp, dss.getTrainDataSets(), false, false);	

			dp.setCfgScratch("optMethod", "valueComp"); 
		} else {
			// retain opt state / info
			dp.setCfgScratch("optPhases", phases);
			dp.setCfgScratch("optSet", minSet);
			dp.setCfgScratch("optDropPercent", dropPercent);
			dp.setCfgScratch("optMethod", "valueCarve"); 

			if (step == 2) {
				// Naive pass
				/*
				passTotal = carveStep(dp, step, fullData, 
						optType, 1000, minDropSetNaive, maxDropSet, false, false, dropMap, blockTest, 
						mergeSetDp, mergeSetIsbase,
						dataSets, valueSets, tuneSets, tuneValueSets);	
				dp.setCfgScratch("optEndVal", passTotal); // count
				*/
				// drop NS based on requested percentage
				nsDropSet(dp, fullData, dropPercent, optType, dss);
				// save here
				//dp.getVegML().save(pre+"-"+dp.getCfgWindowSize()+"-w"+wType.ordinal()+"-s"+fd+step+"-naive.veg");			
			}
			// Carving
			passTotal = carveStepVectorValues(dp, step, fullData, 
										optType, 1000, minDropSet, maxDropSet, noZero, noPositive, 
										useReduction, noBackingUp, downWeight, cleanup, dropMap, 
										mergeSetDp, mergeSetIsbase, nsSetFirst, dss, ctlGroup, false);	
			dp.setCfgScratch("optEndVal", passTotal); // count
		}
		
		if (optType == null) optType = PredictionType.All;
		dp.setCfgScratch("optType", optType.ordinal());
		dp.setCfgScratch("optTuneType", optType);
		dp.setCfgScratch("optNSWeightBase", wType.ordinal());
		dp.setCfgScratch("optFullData", fullData);
		dp.setCfgScratch("optTrainDataSize", dss.getTrainCount()); // count
		dp.setCfgScratch("optTuneDataSize", dss.getTuneCount()); // count		dp.setCfgScratch("optStep", step);
		if (fullData) dp.setCfgScratch("optTuneDataSize", dss.getTrainCount()+dss.getTuneCount()); // count
		String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm.ss").format(new Date());
		dp.setCfgScratch("optTime", timeStamp); // count
		dp.setCfgScratch("dropMap", dropMap);
		dp.setCfgScratch("optStep", step);
		dp.setCfgScratch("optReduction", useReduction);
		dp.setCfgScratch("optNoBackingUp", noBackingUp);
		dp.setCfgScratch("optDownWeight", downWeight);

		
		// step descriptor
		String desc = "fullData="+fullData+",phases="+phases+",dropSet="+minSet+",depSoft="+noBackingUp+",reduction="+useReduction+",dropPercent="+dropPercent+",tunePType="+optType;
		if (!useReduction) desc += ",downWeight="+downWeight;
			
		dp.setCfgScratch("opt_"+step, desc);
		
		dp.setState(DPState.Tuned);
		dp.setCfgCallout(null, null);
		dp.setCfgFramerArg(null);
		dp.setCfgIdentityOnly(idFilterState);

		dp.getVegML().save(pre+"-"+dp.getCfgWindowSize()+"-w"+wType.ordinal()+"-s"+fd+step+".veg");
		//dp.print(true, true);
		
		return passTotal;
	}

	
	// clear by the value and numberset
	// drop map records what was removed already
	private static class resData {
		int fMax = 0, fMin = 100000, pfCnt = 0;
		int minDrop = -1, maxDrop = -1;
		HashMap<Long, Integer> exHash = null;
	}
	
	/**
	 * Iterative Carving by value Naiveity
	 * 
		0. Baseline for model accuracy
		1. Get accuracy for each numberSet stand alone
		2. Sort the numberSets highest accuracy first
		3. get models values sorted by fscore, best first
		4. iterate values
			- test list of vector-value reductions based on naivaty, times value has been seen
			- apply the reduction that produces highest accuracy or the largest reduction that does not alter accuracy
		5. at end of iteration test new baseline and test control set
			- no progress for N iterations on baseline complete
			- if no or negative progress on control for N iterations complete	
			
	 * @param dp
	 * @param step
	 * @param fullData
	 * @param optType
	 * @param maxNSPer
	 * @param minDropSet
	 * @param maxDropSet
	 * @param noZero
	 * @param noPositive
	 * @param useReduction
	 * @param noBackingUp
	 * @param downWeight
	 * @param cleanup
	 * @param dropMap
	 * @param mergeSetDp
	 * @param mergeSetIsbase
	 * @param nsSetFirst
	 * @param dss
	 * @param ctlGroup
	 * @param silent
	 * @return
	 */
	static int carveStepVectorValues(VDataPlane dp, int step, boolean fullData,
				PredictionType optType, int maxNSPer, int [] minDropSet, int [] maxDropSet, boolean noZero, boolean noPositive,
				boolean useReduction, boolean noBackingUp, double downWeight, boolean cleanup,
				HashMap<String, Integer> dropMap, 		
				VDataPlane mergeSetDp, boolean mergeSetIsbase,
				List<Integer> nsSetFirst,
				VDataSets dss, boolean ctlGroup, boolean silent) {		
		if (dp.getNSTurnedOnCount() < 1) return 10;
		System.gc();

		// class for each NS/value
		class Sls {
			String s;
			double d;
			double dfscore;
			int ns;
			long val;
			int pass;
			int fail;
			int pfail;
			int maxCnt;
			int maxVCnt;
			String tag;
		}
		
		dss.genVSets();
		VDataSets tuneDs = dss.getTuneDataSets();
		if (ctlGroup) tuneDs = dss.getTuneTestDataSets();
		
		VDataSets fullDs = tuneDs;
		if (fullData) {
			if (!ctlGroup) fullDs = dss.getTrainAndTuneDataSets();
			else fullDs = dss.getTrainAndTuneTestDataSets();
		}
				
		// Threads and data subsets ; thread count based on size	
		int setCount = fullDs.size()/30;
		if (setCount > 16) setCount = 16;
		
		//silent = false;		
		List<VDataSets> threadFullDs = fullDs.getDataSetSplit(setCount);

		if (mergeSetDp != null && autoMergeValue) {
			// new merge value
			mergeValue = VegTune.optimizeMergeModels(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), 
														mergeSetDp.getDimensionTag(), mergeSetDp.getTag(), 
														false, false, optType, true, false, dss);
									//					false, false, optType, false, fullDs);
			// baseline
			System.gc();
			System.out.println("MERGE-VALUE => ["+mergeValue+"]");
		}
	
	
		//////////////////////////////////
		// initial baseline AND retained resolve set
		dp.setCfgFramerArg(fullDs.getValLLV());
		String s = String.format("%-16s", "["+dp.getDimensionTag()+"/"+dp.getTag()+"]");
		if (!silent) System.out.print("GENERATE_BASELINE"+s+" data["+ fullDs.size()+"] =>");		
		//ResultSet brst = VegTest.testSetsDsRetain(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), fulltuneSets, fulltuneValueSets);		
		VResultSet brst = MLThreadUtil.runtestSetsDsRetain(dp, threadFullDs);
		List<List<DsRetainVal>> rsdSet = brst.getRetainedResolutionSet();
		if (!silent) System.out.println(" RESULT["+String.format("%.2f", brst.passPercent)+"%] pass["+brst.passTotal + " / "+brst.failTotal+" of " + brst.total+"] Time["+brst.getDurration()+"]");		
		
		
		//////////////////////////////////
		// Merge tune setup
		if (mergeSetDp != null) {
			// baseline
			brst = VDRetainSet.baseLineMergeModify(mergeSetDp, dp, rsdSet, mergeValue, mergeSetIsbase, fullDs);	
			System.gc();
		}

		int basePassTotal = brst.getPassTotal(optType);
		int baseFailTotal = brst.getFailTotal(optType);
		
		int curBasePassTotal = basePassTotal;
		int curPassTotal = basePassTotal;
		int curFailTotal = baseFailTotal;
		int [] curPass = new int[VegML.getPredictionTypeCount() +1];
		
		if (!silent) {
			System.out.println("");	
			System.out.println("BASELINE["+optType+"] ["+String.format("%.2f", brst.getPassPercent(optType))+"%] pass["+basePassTotal+ " / "+baseFailTotal+" of " + brst.getTotal(optType)+"]");		
			System.out.println("");	
		}

		////////////////////////////////////////////////////
		// Threads and Retain subsets
		List<List<List<DsRetainVal>>> rsdSets = new ArrayList<>();
		if (setCount != 1) {
			int p=0;
			for (VDataSets d:threadFullDs) {
				List<List<DsRetainVal>> rSets = new ArrayList<>();
				rsdSets.add(rSets);
				for (int i=0;i<d.size();i++,p++) rSets.add(rsdSet.get(p));
			}
		} else {
			rsdSets.add(rsdSet);
		}	

		
		////////////////////////////////////////////////////
		// ## 2
		//
		// Baseline the numbersets by removing each and getting diff
		// Get accuracy for each numberSet stand alone
		// 
		HashMap<Integer, List<ValProb>> vcHash = new HashMap<>();		
		List<HashMap<Long, Integer>> vml = new ArrayList<>();
		List<String> vmlStr = new ArrayList<>();		
		List<Integer> nsMap = new ArrayList<>();
		TestModSet nstests = new TestModSet();
		
		// next
		List<VResultSet> rList = null;

		
		if (dp.getNSTurnedOnCount() == 1) {
			for (int x=0;x<dp.getNSCount();x++) {
				// no identity or off
				if (dp.isCfgNSTurnedOff(x) || x == dp.getCfgNSIdentityNumber() 
						|| (!OptimizerStatistical.allowFullest && x == dp.getCfgNSFullNumber())) {
					vml.add(null); // to keep place
					vmlStr.add(null); // to keep place
					continue;
				}
				// all but one NS test
				TestMod t = new TestMod(dp.getCfgNSWeightsBase());	
				t.nsWeights[x] = 0; // set one off..
				nstests.add(dp, t.completeNSW());	
				
				// get values in number set 
				HashMap<Long, Integer> m = dp.getNSValueSet(x);
				vml.add(m);
				vmlStr.add(VegUtil.getValueCountString(dp, m));
				nsMap.add(x);
				
				// use info from initial baseline
				rList = new ArrayList<>();
				rList.add(brst);
				brst.val2 = x;
			}			
		} else {
			// add complete first (must be first and here)
			TestMod t = new TestMod(dp.getCfgNSWeightsBase());	
			nstests.add(dp, t.completeNSW());
		
			for (int x=0;x<dp.getNSCount();x++) {	
				// no identity or off
				if (dp.isCfgNSTurnedOff(x) || x == dp.getCfgNSIdentityNumber() 
						|| (!OptimizerStatistical.allowFullest && x == dp.getCfgNSFullNumber())) {
					vml.add(null); // to keep place
					vmlStr.add(null); // to keep place
					continue;
				}
				// all but one NS test
				t = new TestMod(dp.getCfgNSWeightsBase());	
				t.nsWeights[x] = 0; // set one off..
				nstests.add(dp, t.completeNSW());				
				
				// get values in number set 
				HashMap<Long, Integer> m = dp.getNSValueSet(x);
				vml.add(m);
				vmlStr.add(VegUtil.getValueCountString(dp, m));
				nsMap.add(x);
			//	System.out.println("  NS_X["+String.format("%3d", x)+"]["+dp.getNSFormatString(x)+"] ["+vml.size()+"] " + dp.isCfgNSTurnedOff(x));	
			}
		}
		
		// get all the data		
		if (!silent) System.out.print("PREPARING NumberSets["+nstests.size()+"]["+dp.getNSTurnedOnCount()+" / "+dp.getNSCount()+"] =>");
		
		List<Object> set = new ArrayList<>();
		if (rList == null) {
			// this should use the rsdSet; currently it is very very slow for no good reason
			rList = MLThreadUtil.runTestPredictModify(dp, set, nstests, rsdSets, threadFullDs, false);
			rList.remove(0); // remove complete (not used)
			for (int x=0;x<rList.size();x++) rList.get(x).val2 = nsMap.get(x);
			if (!silent) System.out.println(" Time["+rList.get(0).getDurration()+"]");	
		}	
		
		//
		// Sort the numberSets highest accuracy first
		//
		VResultSet.sortList(optType, rList, nsSetFirst);
		
		//
		// show some info
		for (int x=0;x<rList.size();x++) {
			VResultSet rst = rList.get(x);	
			rst.responseOut = null;
			if (silent) continue;
			int position = (int)rst.val; // after sort
			if (position >= nsMap.size()) continue;
			
			int ns = (int)rst.val2; // numberSet number
			
	//		System.out.println("  NS_SETx["+x+"/"+position+"/"+ns+"]["+String.format("%3d", ns)+"]["+dp.getNSFormatString(ns)+"] ["+rList.size()+"]  " + dp.isCfgNSTurnedOff(ns));				
			int vectCnt = 0;		
			// account for all values
			List<ValProb> vectList = dp.getNSFrequencySorted(ns);
			if (vectList != null && vectList.size() > 0) {
				vectCnt = vectList.size();
				List<ValProb> vcl = new ArrayList<>();
				vcHash.put(x, vcl);
				for (int i=0;i<vectList.size();i++) {
					ValProb vvp = vectList.get(i);
					// get valprob for vector
					List<ValProb> vpList = dp.findCfgNSProbList(ns, vvp.value); // get this vector info
					if (vpList != null && vpList.size() > 0) {
						for (ValProb vp:vpList) vp.count = 1; // single count them
						VegUtil.mergeVPList(vcl, vpList);
					}
				}
			}
			
			// values in this one
			String vs = "vals[0]";
			HashMap<Long, Integer> vm = vml.get(ns);
			if (vm != null) vs = "vals["+vm.keySet().size()+"] ["+vmlStr.get(ns)+"]";
			
			if (!silent) System.out.print("  NS_SET["+String.format("%3d", ns)+"]["+dp.getNSFormatString(ns)+"]  ");	
			String ar = "==>";
			if (ns == dp.getCfgNSIdentityNumber()) ar = "=I>";
			else if (ns == dp.getCfgNSFullNumber()) ar = "=F>";
			else if (ns == dp.getCfgNSFullNumber()) ar = "=X>";
			System.out.println(ar+"  tot["+rst.passTotal+" / "+rst.failTotal+"]["+String.format("%.2f", rst.passPercent) +"%] vect[" +String.format("%7d", vectCnt)+"]  " + vs);				
		}		
		
		////////////////////////////////////////////////////
		// ## 3
		//
		// Reduce the lowest of them
		//	
		// Get models values sorted by fscore, highest first
		//
		List<ResultValueStat> rvsl = brst.getValueFscoreSorted();
		for (int vi=0;vi < rvsl.size();vi++) {
			ResultValueStat brvs = rvsl.get(vi);
			
			String sval = dp.getString(brvs.value);

			// check if gone
			Integer doneDropSet = dropMap.get(sval);
			boolean doneNS = false;
			if (doneDropSet != null && doneDropSet == 0) doneNS = true;
			int vtotal = brst.getTotal(optType, brvs.value);
		    String bp = String.format("%6s", String.format("%.2f", brst.getPassPercent(optType, brvs.value)));  	    
		    String bgp = String.format("%6s", String.format("%.2f", brst.getPassGuessPercent(optType, brvs.value)));  
		    String vfs = String.format("%9s", String.format("%.6f", brst.getFScore(optType, brvs.value)));  
		    String vvs = String.format("%-20s", "["+String.format("%-6s", sval)+"]["+vi+" of " + rvsl.size()+"]");  	    		    
			String bsx = vvs;
			if (vtotal == 0) bsx += " => NONE";
			else bsx += " => fs["+vfs+"]T["+String.format("%6d", vtotal)+"]p["+String.format("%6d", brst.getPassTotal(optType, brvs.value))+"]f["+String.format("%5d", (int)brst.getFailTotal(optType, brvs.value))+"]fp["+String.format("%5d", (int)brst.getPFailTotal(optType, brvs.value))+"]  ["+bp+"%]["+bgp+"%]";	
			
			
			// addd info from NS value mapping
			int cnt = 0;
			String ins = "";	
			for (int x=0;x<dp.getNSCount();x++) {
				if (dp.isCfgNSTurnedOff(x) || x == dp.getCfgNSIdentityNumber() 
						|| (!OptimizerStatistical.allowFullest && x == dp.getCfgNSFullNumber())) {
					continue;
				}				
				Integer mi = vml.get(x).get(brvs.value);
				if (mi == null) {
					dropMap.put(x+sval, 0); // record it
				} else {
					ins += x+ " ";
					cnt++;
				}
			}

			//int bvcount = 0;
			List<Sls> sl = new ArrayList<>();
			
			//
			// go through the sorted list of numberSets and validate/complete reductions
			//
			for (int i=0;i<rList.size();i++) {
				VResultSet fr = rList.get(i);
				int ns = (int)fr.val2;
				
				if (dp.isCfgNSTurnedOff(ns)) continue;
				
				fr.getValueStats().get(brvs.value);
				// per value
				ResultValueStat rvs = fr.getValueStats().get(brvs.value);
				if (rvs == null) continue; // bad
				
				// save pass/fail/
			    String p = String.format("%6s", String.format("%.2f", rvs.passPercent));  
			    String gp = String.format("%6s", String.format("%.2f", rvs.passGuessPercent));  

			    // compare with baseline for the NS metrics
			    double dfscore = rvs.fscore - brvs.fscore;			
			    int dtrue_positive = rvs.pass - brvs.pass;
			    int dfalse_negative = rvs.fail - brvs.fail;
			    int dfalse_positive = rvs.pfail - brvs.pfail;
			    // backwards does better... 
			  //  double dfscore = brvs.fscore - rvs.fscore;			
			   // int dtrue_positive = brvs.pass - rvs.pass;
			   // int dfalse_negative = brvs.fail - rvs.fail;
			   // int dfalse_positive = brvs.pfail - rvs.pfail;
			    /*
			    int vcount = 0;
			    double vprob = 0;
				List<ValProb> vpList = vcHash.get(i);
				if (vpList != null) {
					int idx = ValProb.indexOf(vpList, brvs.value);
					if (idx >= 0) {
						ValProb vp = vpList.get(idx);
						vcount = vp.count;
						vprob = vp.probability;
					}
				}
			    bvcount += vcount;
			    */
			    // order and assess based on PredictionType
			    // TODO
			    
			    int maxVCnt = 0;
			    int maxCnt = 0;
			    if (!doneNS) {
			    	maxVCnt = dp.getNSValueMaxCount(ns, brvs.value);
			    	maxCnt = dp.getNSSingleValueCountMax(ns);
			    }
			    
			    String fs = String.format("%9s", String.format("%.6f", dfscore));  
			    if (dfscore == 0 && dfalse_positive == 0 && dfalse_negative == 0 && dtrue_positive == 0) fs = "---------";	    
				String sx = "  ["+String.format("%3d", ns)+"]["+dp.getNSFormatString(ns)+"]";
				sx += "x["+String.format("%6d", maxCnt)+"]["+String.format("%6d", maxVCnt)+"]  ";				    
				sx += "fs["+fs+"] p["+String.format("%5d", dtrue_positive)+"]fn["+String.format("%5d", dfalse_negative)+"]fp["+String.format("%5d", dfalse_positive)+"]  ["+p+"]["+gp+"]";	
			   // sx += " ["+String.format("%5d", vcount)+"]["+vprob+"]";
				if (!doneNS) {
					Sls xs = new Sls();			
				    xs.d = rvs.fscore; // or diff?
					xs.s = sx;
					xs.ns = ns;
					xs.tag = sval;
					xs.dfscore = dfscore;
					xs.pfail = (int)dfalse_positive;
					xs.fail = (int)dfalse_negative;
					xs.pass = (int)dtrue_positive;
					xs.val = brvs.value;
				//	xs.maxCnt = maxCnt;
					xs.maxVCnt = maxVCnt;
					sl.add(xs);
				}
			}
				
		   // bsx += " valCnt["+bvcount+"]";		    
		    bsx += "  =>  NS_CNT["+cnt+"]";    
		   // bsx += " ["+ ins+"]";		    
			
			// check if gone
			if (doneNS) {
			//	System.out.print("  DONE ");	
			    if (!silent) System.out.println(bsx + " DONE");	
			    continue; // already gone
			}
		    if (!silent) System.out.println(bsx);

			TestModSet testList = new TestModSet();
			List<TestMod> freeTestList = new ArrayList<>();
			TestModSet nexttestList = new TestModSet();
			resData reData = new resData();
			resData renData = new resData();
			
			///////////////////////////
			// through the Number sets for this Value
			for (int pc=0;pc<sl.size() && pc<=maxNSPer;pc++) {
				Sls lsl = sl.get(pc);
				int ns = lsl.ns;
				
				if (dp.isCfgNSTurnedOff(ns)) continue;
				if (!allowFullest && ns == dp.getCfgNSFullNumber()) continue;
				if (!allowIdentity && ns == dp.getCfgNSIdentityNumber()) continue;

				// f-score over...
				if (lsl.dfscore == 0 && noZero) continue;
				if (lsl.dfscore > 0 && noPositive) continue;

				// check what is done Full NS or NS/value
				Integer doneDropNS = dropMap.get(""+ns);
				if (doneDropNS != null && doneDropNS == 0) continue;
				
				Integer doneDropI = dropMap.get(ns+sval);
				if (doneDropI != null && (doneDropI == 0 || doneDropI == minDropSet[minDropSet.length-1])) {
					//System.out.println("  =>  DONE["+doneDropI+"]");
					//System.out.println("  ns["+String.format("%3d", lsl.ns)+"]  =>  max["+dp.getNSValueMaxCount(ns, brvs.value)+"]["+doneDropI+"] ");		
					continue;
				}
								
				// get max counts for this NM
				int nsVCnt = lsl.maxVCnt; // slow for every one
				// check vs prior

				///////////////////////////////////////
				// Setup tests if any
				// then check reduce with max
			    String fs = String.format("%9s", String.format("%.6f", lsl.dfscore));  		
			    if (!silent) System.out.print("  ns["+String.format("%3d", lsl.ns)+"]["+dp.getNSFormatString(lsl.ns)+"]x["+String.format("%6d", nsVCnt)+"]fs["+fs+"]");	
				testList.clear();
				
				int ac = 0;
				// MIN
				if (minDropSet != null) {
					for (int h=0;h<minDropSet.length;h++) {
						if (minDropSet[h] != 0) {
							if (doneDropI != null && doneDropI >= minDropSet[h]) continue; // already done
							if (minDropSet[h] > nsVCnt) continue; // don't bother higher than whats there
						}
						TestMod tst = null;
						if (freeTestList.size() > 0) {
							tst = freeTestList.remove(freeTestList.size()-1);
							tst.reset(dp.getCfgNSWeightsBase());
						} else {
							tst = new TestMod(dp.getCfgNSWeightsBase());
						}
						testModAcValue mod = tst.addModMin(ns, brvs.value, minDropSet[h]); // value off (always retain prob here for now)
						if (!useReduction) mod.weight = downWeight;
						tst.setPredictionType(optType);
						testList.add(dp, tst.completeNSW());	
						ac++;
					}	
				}
				// MAX
				if (maxDropSet != null) {
					nsVCnt = dp.getNSValueMaxCount(ns, brvs.value);
					for (int h=0;h<maxDropSet.length;h++) {
						// percentage of max for this value
						//lsl.maxCnt
						int max = (int) ((double)lsl.maxVCnt * ((double)maxDropSet[h]/(double)100));
						//System.out.println(" XX["+max+" of "+nsVCnt+"] " + maxDropSet[h]);			
						if (max <= 10 || max > nsVCnt) continue;
						
						//if (doneDropI != null && minDropSet[h] != 0 && doneDropI >= minDropSet[h]) continue; // already done
						TestMod tst = null;
						if (freeTestList.size() > 0) {
							tst = freeTestList.remove(freeTestList.size()-1);
							tst.reset(dp.getCfgNSWeightsBase());
						} else {
							tst = new TestMod(dp.getCfgNSWeightsBase());
						}
						
						//TestMod tst = new TestMod(dp.getCfgNSWeightsCopy());
						testModAcValue mod = tst.addModMax(ns, brvs.value, max); // value off (always retain prob here for now)	
						if (!useReduction) mod.weight = downWeight;
						tst.setPredictionType(optType);
						testList.add(dp, tst.completeNSW());	
						ac++;
					}
				}
				if (ac == 0) {
					if (useReduction) dropMap.put(lsl.ns+sval, 0); // done
					if (!silent) System.out.println("  =>  max["+dp.getNSValueMaxCount(ns, brvs.value)+"]["+doneDropI+"] ");		
					continue;
				}
					
				////////////////////////
				// TEST the options for change
				List<VResultSet> rModList = null;
				//List<ResultSet> rdModList = null;
				if (threadFullDs.size() == 1) {
					rModList = VDRetainSet.testSetsModify(dp, testList, rsdSet);
				} else {
					rModList = MLThreadUtil.runTestPredictModify(dp, set, testList, rsdSets, threadFullDs, true);					
				}
				if (rModList == null) continue;
			
				////////////////////////
				// Find best Match: make new test list more selective
				resData rData = reData;
				VResultSet ptsk = getBestResult(dp, brvs, rModList, curPassTotal, optType, noBackingUp, ns, testList, nexttestList, null, null, reData);	
				
				////////////////////////
				// Check new tests
				if (nexttestList.size() > 0) {
					if (!silent) System.out.print(" "+String.format("%2d", nexttestList.size())+"> ");
					// test these with the second filter
					List<VResultSet> rnModList = null;
					if (threadFullDs.size() == 1) {
						rnModList = VDRetainSet.testSetsModify(dp, nexttestList, rsdSet);
					} else {
						rnModList = MLThreadUtil.runTestPredictModify(dp, set, nexttestList, rsdSets, threadFullDs, true);
					}
					
					////////////////////////
					// Find best Match
					VResultSet nptsk = getBestResult(dp, brvs, rnModList, curPassTotal, optType, noBackingUp, ns, nexttestList, null, ptsk, rData, renData);	
					if (nptsk != ptsk && nptsk != null) {
						ptsk = nptsk;
						rData = renData;
					}
				} else {
					if (!silent) System.out.print("     ");		
				}
				
				freeTestList.addAll(testList.get(dp));					
				testList.clear();
				nexttestList.clear();
				
				if (ptsk == null) {
					int mxCnt = dp.getNSValueCount(ns, brvs.value);
					if (!silent) {
						if (reData.pfCnt == 0 && renData.pfCnt == 0) System.out.println(" => NONE Cnt["+String.format("%6d", mxCnt)+"]");
						else if (reData.pfCnt != 0 && renData.pfCnt != 0) {
							System.out.println(" => NONE Cnt["+String.format("%6d", mxCnt)+"]"
									+ " m["+reData.pfCnt+"]f["+String.format("%3d", reData.fMin)+" - "+reData.fMax+"]"
									+ " => m["+renData.pfCnt+"]f["+String.format("%3d", renData.fMin)+" - "+renData.fMax+"]");		
						} else if (reData.pfCnt != 0) {
							System.out.println(" => NONE Cnt["+String.format("%6d", mxCnt)+"]"
									+ " m["+reData.pfCnt+"]f["+String.format("%3d", reData.fMin)+" - "+reData.fMax+"]");			
						} else {
							System.out.println(" => NONE Cnt["+String.format("%6d", mxCnt)+"]"
									+ " => m["+renData.pfCnt+"]f["+String.format("%3d", renData.fMin)+" - "+renData.fMax+"]");		
						}
					}
					continue;
				}
				
				int passt = ptsk.getPassTotal(optType);
				int failt = ptsk.getFailTotal(optType);					
				if (!silent) System.out.print(" => chg["+String.format("%4d", ptsk.changeCount)+"]p["+String.format("%3d", ptsk.getRDChangeToPass().size())+"]f["+String.format("%3d", ptsk.getRDChangeToFail().size())+"]"
						+ "  =>  ["+String.format("%.2f", ptsk.getPassPercent(optType))+"%] pass["+passt + " / "+failt+"] ");		
				

				String v = ""+rData.minDrop;
				if (rData.minDrop == -1) v = ">"+rData.maxDrop;
				else if (rData.exHash != null)  v = "x"+rData.minDrop;
				String drp = "";
				if (doneDropI != null) drp = ""+doneDropI;
				
				////////////////////////
				// make the change
//				if (curPassTotal < passt) {
				if (curPassTotal <= passt) {
					// Make the reduction full or with minCount	
					if (rData.minDrop == 0) rData.maxDrop = 0;

					// remove
					int chng = 0;
					if (useReduction) {
						// reduction
						chng = VDRetainSet.removeAllNSValue(rsdSet, ns, brvs.value, rData.minDrop, rData.maxDrop, rData.exHash);
						chng = dp.removeAllNSValue(ns, lsl.val, rData.minDrop, rData.maxDrop, rData.exHash);
					} else {
						// weighted
						chng = VDRetainSet.weightAllNSValue(rsdSet, ns, brvs.value, rData.minDrop, rData.maxDrop, rData.exHash, downWeight);
						chng = dp.weightAllNSValue(ns, lsl.val, rData.minDrop, rData.maxDrop, rData.exHash, downWeight);
					}

					// update model with changes 
					VDRetainSet.updateRetainSet(rsdSet, ptsk);

					String str = "";
					if (passt == curPassTotal) str = " =>  chng("+v+")["+chng+"]";
					else str = " =>  CHNG("+v+")["+chng+"]";		
					if (!silent) System.out.print(String.format("%-25s", str));

					if (rData.exHash == null) {
						// record it IF no missed elements
						if (useReduction) dropMap.put(ns+sval, rData.minDrop); 
					}
					
					// update current
					curPassTotal = passt;
					curBasePassTotal = ptsk.passTotal;
					curFailTotal = failt;
					for (int vx=0;vx<curPass.length;vx++) curPass[vx] = ptsk.pTpass[vx];
					
					if (ptsk.changeCount > 0 && ptsk.getRDChangeToPass().size() > 0) {
						// if progress, whats the cost?
						if (ptsk.getRDChangeToFail().size() == 0) drp += "++";
						else drp += "--";
					}						
				} else {
					if (!silent) System.out.print(String.format("%-25s", "       no("+v+") "));
				}
				if (!silent) System.out.println(" T["+ptsk.getDurration()+"] " + drp);										
			}
		}
		
		if (useReduction && cleanup) {
			// turn off any numberSets without accumulators
			int dcnt = 0;
			for (int x=0;x<dp.getNSCount();x++) {
				if (dp.isCfgNSTurnedOff(x) || x == dp.getCfgNSIdentityNumber() 
						|| (!OptimizerStatistical.allowFullest && x == dp.getCfgNSFullNumber())) {
					continue;
				}
				
				int acCnt = dp.getNSAccumulatorCount(x);
				if (acCnt <= 0) {
					if (!silent) System.out.println("TURN OFF NS["+x+"]  ac["+acCnt+"]");
					dp.setCfgNSTurnedOff(x, true);
					dp.clearNSData(x);
					dp.setCfgNSWeight(x, 0);
					dcnt++;
				}
			}
			// remove turned off numberSets		
			dp.removeCfgNSTurnedOff();	
		} 
		
		if (!noEndThreads) MLThreadUtil.endThreads();
		dp.setCfgFramerArg(null);	
		
		if (!silent) System.out.println("STEP COMPLETE START["+basePassTotal +" => "+curPassTotal+" / "+curFailTotal+"] ");			
		return curPassTotal;
	}
	private static VResultSet getBestResult(VDataPlane dp, ResultValueStat brvs, List<VResultSet> rModList, int curPassTotal, PredictionType optType, boolean noBackingUp,
								int ns, TestModSet testList, TestModSet nextTestList, VResultSet lBest, resData oldData, resData reData) {
		if (rModList == null) return null;
		reData.fMax = 0;
		reData.fMin = 100000;
		reData.pfCnt = 0;
		reData.minDrop = reData.maxDrop = -1;
		reData.exHash = null;
		if (lBest != null) {
			reData.minDrop = oldData.minDrop;
			reData.maxDrop = oldData.maxDrop;		
			reData.exHash = oldData.exHash;;
		}
		
		VResultSet ptsk = lBest;
		for (int x=0;x<rModList.size();x++) {
			TestMod tst = testList.get(dp, x);
			if (tst.getModCount(ns) == 0) continue; // not for this numberSet
			// find best in set: do largest removal if equal
			boolean use = false;
			VResultSet tsk = rModList.get(x);
			
			int fCnt = tsk.getRDChangeToFail().size();
			int ttot = tsk.getPassTotal(optType);
			if (ttot >= curPassTotal) {
				reData.pfCnt++;
				if (fCnt > reData.fMax) reData.fMax = fCnt;
				if (fCnt < reData.fMin) reData.fMin = fCnt;
			}
			// NO reverse		
			if (noBackingUp && fCnt > 0) {
				// try again excluding those vectors if some pass
				// should limit based on a percent of the current count of vectors.
				if (nextTestList != null && (fCnt+ttot) > curPassTotal) { // must be better
					boolean add = true;
					for (int h=0;h<fCnt;h++) {
						long vid = tsk.getRDChangeToFail().get(h).vid[ns];
						if (tst.addModVectorList(ns, vid, 0) > MAX_EXCLUDE) {
							// over max number of vectors
							add = false;
							break;
						}
					}
					if (add) nextTestList.add(dp, tst);	
				} 
				continue;
			}

			if (ptsk == null) use = true;
			else if (ttot > ptsk.getPassTotal(optType)) use = true;
			else if (nextTestList != null && ttot == ptsk.getPassTotal(optType)) { // if has exMap then must be positive
				int min = tst.getMod(ns, brvs.value).minCount;
				if (reData.minDrop == 0) use = false;
				else if (min == 0) use = true;
				else if (min > reData.minDrop) use = true;
				else if (min == reData.minDrop && reData.exHash != null && tst.nsVectorMods[ns].size() < reData.exHash.size()) {
					use = true;
				} else {
					int max = tst.getMod(ns, brvs.value).maxCount;
					if (reData.maxDrop == 0) use = false;
					if (max == 0) use = true;
					else if (max > reData.maxDrop) use = true;
				}
			}
			if (use) {
				ptsk = tsk;
				TestMod mtsk = tst;
				if (mtsk.nsVectorMods != null) reData.exHash = mtsk.nsVectorMods[ns];	
				else reData.exHash = null;
				
				if (mtsk.getMod(ns, brvs.value).maxCount >= 0) reData.maxDrop = mtsk.getMod(ns, brvs.value).maxCount;	
				else reData.minDrop = mtsk.getMod(ns, brvs.value).minCount;
			}					
		}
		return ptsk;
	}
	
	
	
	///
	// Drop the least usefull percentage of numberSets
	//
	private static int nsDropSet(VDataPlane dp, boolean fullData, double nsDropPercent, PredictionType optType, VDataSets dss) {	
		if (nsDropPercent <= 0) return 0;

		VDataSets fullDs = dss.getTuneDataSets();
		dp.setCfgFramerArg(fullDs.getValLLV());
		
		////////////////////////////////////////////////////
		// if drop_ns the lowest percentage
		// use the ALONE stats
		int dropCnt = (int)((double)(dp.getNSCount()+1) * ((double)nsDropPercent/(double)100));
		System.out.println("DROP lowest NumberSets["+dropCnt+"] <- ["+nsDropPercent+"%]");				

		// if remove the lowest percentage
		// use the ALONE stats
		int dcnt = 0;

		TestModSet nstestsAlone = new TestModSet();
		for (int x=0;x<dp.getNSCount();x++) {
			if (dp.isCfgNSTurnedOff(x)) continue;
			// just one NS test
			TestMod t = new TestMod(dp.getNSCount());	
			t.nsWeights[x] = dp.getCfgNSWeight(x);
			nstestsAlone.add(dp, t);	
		}
		// Only one tests
		List<VResultSet> rListAlone = VegTest.testSetsModify(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), nstestsAlone, fullDs, false);
		VResultSet.sortList(rListAlone);
		//ResultSet.sortList(optType, rListAlone);
		for (int x=0;x<rListAlone.size();x++) {
			VResultSet rst = rListAlone.get(x);	
			int ns = (int)rst.val; // after sort
			System.out.print("  NS_ALONE["+String.format("%3d", ns)+"]["+dp.getNSFormatString(ns)+"]  ");	
			String ar = "==>";
			if (ns == dp.getCfgNSIdentityNumber()) ar = "=I>";
			else if (ns == dp.getCfgNSFullNumber()) ar = "=F>";
			else if (ns == dp.getCfgNSFullNumber()) ar = "=X>";
			System.out.println(ar+"  tot["+rst.getPassTotal(optType)+"]["+String.format("%.2f", rst.getPassPercent(optType)) +"%]  ");	
		}
		
		for (int i=0;dcnt<dropCnt;i++) {
			VResultSet rst = rListAlone.get((rListAlone.size()-1)-i);
			int ns = (int)rst.val;
			if (ns == dp.getCfgNSIdentityNumber()) continue;
			if (ns == dp.getCfgNSFullNumber()) continue;
			// is this in lowest of the others ?
			dp.setCfgNSTurnedOff(ns, true);
			dp.clearNSData(ns);
			dp.setCfgNSWeight(ns, 0);
			dcnt++;
			System.out.println("  "+dcnt+") DROP_NS["+String.format("%3d", ns)+"]["+dp.getNSFormatString(ns)+"]  ==>  tot["+rst.passTotal+"]["+String.format("%.2f", rst.passPercent) +"%]  ");	
		}
		
		// remove turned off numberSets
		dp.removeCfgNSTurnedOff();	
		dp.print(true);
		
		//dp.print(true);
		dp.setCfgFramerArg(false);
		System.gc();	
		return dropCnt;
	}
	
	

	
	////////////////////////////////////////////////////////////////////////////////////
	//
	// Optimize A double value, generally for merge
	// Value is in the scratchpad, initial value is set
	// binary search is used
	//
	static final int MAX_VALUE = 100;	
	static final int MIN_VALUE = 0;	
	static final int MAX_ITR = 50;		
	static final double MIN_VAL = 0.000001;
	static final double TMAX = 0.999999;	
	static final double TMIN = 0.000001;	

	/*
	 * Start with the powerset model, all numberSet weights == 1
	 * 
	 * check which number sets are least accurate
	 * - from least accurate to most accurate 
	 * 	- reduce the weight bye 1/2
	 * 		- run and check results
	 * 			- if good keep move to the next, else change back
	 * - when complete do the same pass again
	 * 	- when values are less than a MIN set the numberSet to off for the test
	 * - when complete
	 * 	- remove all turned off numberSets
	 *  - save the weights as a starting point for optimizeing weight
	 * 
	 */
	private static final double MIN_W = 0.00001;
	private static final double MAX_CYCLE = 10;

	
	static void optimizeNumberSetsDrop(VDataPlane dp, VDataSets dss) {		
		NSWeightBase NSWeightBases = NSWeightBase.Distance;
		// FIXME option to work with the ns and weights start with
		boolean resetWieghts = true;
		dss.genVSets();
		
		System.out.println("NUMBER_SET optimize["+dp.getDimensionTag()+"/"+dp.getTag()+"] w["+dp.getCfgWindowSize()+"]nscount["+dp.getNSCount()+"] base["+NSWeightBases+"]");	
		
		double [] wset = dp.getCfgNSWeightsCopy();
		if (resetWieghts) {		
			wset = MLNumberSetUtil.makeNumberSetWeights(dp.getNSs(), dp.getCfgWindowSize(), dp.getCfgFrameFocus(), NSWeightBases);
			dp.setCfgNSWeights(wset);
		}
		dp.setCfgNSWeightsLocked(true);
		double [] bwset = Arrays.copyOf(wset, wset.length); // base

		// baseline AFTER set?
		VResultSet bestts = VegTest.testSets(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), dss.getTuneDataSets());
		VResultSet lastts = bestts;
		int passStart = bestts.passTotal;
		double passStartPercent = bestts.passPercent;

		dp.print(true);
		System.out.println(" BASE NS["+dp.getNSCount()+"] pass["+passStart+"]["+String.format("%.2f", passStartPercent) +"%]");	

		// peer/inverse sets together?
		// try dependent AND independent?
			// set all back to base before each is tested VS retaining prior changes

		// last change
		int lc_ns = -1;
		int lc_cycle = 0;
		
		// iterate
		for (int i=0;i<MAX_CYCLE;i++) {	
			boolean done = false;

			boolean noUp = true;
			boolean noOff = false;
			boolean minDown = false;
			
			boolean offOnly = true;
			
			System.out.println(" CYCLE["+i+"] pass["+passStart+"]["+String.format("%.2f", passStartPercent) +"%]  ==>  pass["+bestts.passTotal+"]["+String.format("%.2f", bestts.passPercent) +"%]");	

			// context only
			System.out.println(" ctx sets");
		
			if (i != 0) noUp = false;
	//		if (i == 0) noOff = true;
			//noUp = true;
			//noUp = true;
			if (offOnly) noOff = false;
		//noOff = true;			
			for (int xi=0;xi<dp.getCfgWindowSize();xi++) {
				for (int x=0;x<dp.getNSCount();x++) {
					
					// if not change since last run.. DONE		
					if (i > 1 && lc_cycle < i && lc_ns > 0) {  // last cycle
						int ws = dp.getNS(lc_ns).size();
						if (dp.isCfgNSContext(lc_ns) && ((ws < x) || ws == x && lc_ns <= x)) done = true; // before this width OR before this ns
					}
					if (!dp.isCfgNSContext(x)) continue;		
					if (dp.getNS(x).size() != xi) continue;
					if (x == dp.getCfgNSIdentityNumber() || x == dp.getCfgNSFullNumber()) continue;
					VResultSet ol = bestts;
					bestts = testChangeNS(dp, bestts, wset, bwset, noOff, offOnly, noUp, minDown, x, dss.getTuneDataSets());
					if (bestts != ol) {
						lc_ns = x;
						lc_cycle = i;
					}
				}
			}
			if (done) break;
			
			if (i == 0) noOff = minDown = true;
			else noOff = false;
			//noUp = true;
		//noOff = true;
			noUp = false;
			if (offOnly) {
				noOff = false;
				if (i == 0 && lc_ns > 0) continue;
			}
			System.out.println(" id sets: " +noOff);
			for (int xi=0;xi<dp.getCfgWindowSize();xi++) {
				for (int x=0;x<dp.getNSCount();x++) {
					// check last change...
					// if not change since last run.. DONE	
					if (i > 1 && lc_cycle < i && lc_ns > 0) {  // last cycle
						int ws = dp.getNS(lc_ns).size();
						if (dp.isCfgNSContext(lc_ns)) done = true; 
						else if ((ws < x) || ws == x && lc_ns <= x) done = true; // before this width OR before this ns
					}
					if (dp.isCfgNSContext(x)) continue;
					if (dp.getNS(x).size() != xi) continue;
					if (x == dp.getCfgNSIdentityNumber() || x == dp.getCfgNSFullNumber()) continue;
					VResultSet ol = bestts;
					bestts = testChangeNS(dp, bestts, wset, bwset, noOff, offOnly, noUp, minDown, x, dss.getTuneDataSets());
					if (bestts != ol) {
						lc_ns = x;
						lc_cycle = i;
					}
				}				
			}
			if (done) break;
						
			// always do id and fullest last
			System.out.println(" final");			
			//noUp = true;
			VResultSet ol = bestts;
			bestts = testChangeNS(dp, bestts, wset, bwset, true, offOnly, noUp, true, dp.getCfgNSFullNumber(), dss.getTuneDataSets());
			bestts = testChangeNS(dp, bestts, wset, bwset, true, offOnly, noUp, true, dp.getCfgNSIdentityNumber(), dss.getTuneDataSets());	
			if (bestts != ol) {
				lc_ns = dp.getNSCount();
				lc_cycle = i;
			}		
			
			if (bestts == lastts) break;
			lastts = bestts;
			//dp.print(true);
		}
		
		// done.. 
		dp.print(true);
		
		// Show the code to add this..
		System.out.println("vML.setCfgNSTurnedOff(\""+dp.getDimensionTag()+"\", \""+dp.getTag()+"\", false);");
		for (int x=0;x<dp.getNSCount();x++) {
			if (dp.isCfgNSTurnedOff(x)) {
				System.out.println("vML.setCfgNSTurnedOff(\""+dp.getDimensionTag()+"\", \""+dp.getTag()+"\", "+x+", true);");
			} else {
				System.out.println("vML.setCfgNSWeight(\""+dp.getDimensionTag()+"\", \""+dp.getTag()+"\", "+x+", "+dp.getCfgNSWeight(x)+");");				
			}		
		}	
		System.out.println("vML.setCfgNSWeightsLocked(\""+dp.getDimensionTag()+"\", \""+dp.getTag()+"\", true);");
		System.out.println("vML.removeCfgNSTurnedOff(\""+dp.getDimensionTag()+"\", \""+dp.getTag()+"\");");

		VegUtil.softmax(wset);
		dp.setCfgNSWeights(wset);	
		// remove what isn't needed
		// not work on solid model..		
		//dp.removeCfgNSTurnedOff();
		
		//ResultSet ts = dp.getVegML().testSets(dp.getDimensionTag(), dp.getTag(), tuneSets, tuneValueSets);
		System.out.println(" FINAL NS["+dp.getNSCount()+"] pass["+passStart+"]["+String.format("%.2f", passStartPercent) 
							+"%]  ==>  pass["+bestts.passTotal+"]["+String.format("%.2f", bestts.passPercent) +"%]");	
	}
	
	private static final int MIN_DIFF_OFF = 5;			// improvement must be this or better
	private static final boolean CHANGE_ONLY = true;	// only update if improved
	private static boolean setCfgNSWeight(VDataPlane dp, double [] wset, boolean noOff, double w, int ns) {
		if (w <= MIN_W && noOff) return false;
		if (w <= MIN_W) {
			dp.setCfgNSTurnedOff(ns, true);
			wset[ns] = 0;
		}
		wset[ns] = w;
		double [] wset2 = Arrays.copyOf(wset, wset.length);
		VegUtil.softmax(wset2);
		dp.setCfgNSWeights(wset2);				
		return true;
	}
	
	// 90%, 50%, 0.1%, 0% >> increse by %10
	private static VResultSet testChangeNS(VDataPlane dp, VResultSet bestts, double [] wset, double [] bwset, 
								boolean noOff, boolean offOnly, boolean noUP, boolean minDown, int x, VDataSets tuneDs) {
		if (dp.isCfgNSTurnedOff(x)) return bestts;

		if (offOnly && noOff) return bestts;
		boolean independent = false;
		// if independent
		if (independent) wset = Arrays.copyOf(bwset, bwset.length);
		
		double cw = wset[x];
		double w = cw; 
		double bw = cw;
		VResultSet ts = bestts;
		
		System.out.print("  NS_SET["+String.format("%3d", x)+"]["+dp.getNSFormatString(x)+"]["+String.format("%.8f", cw)+"].");	
	
		if (!noUP && !offOnly) {
			double w1 = bw*1.10; 
			setCfgNSWeight(dp, wset, noOff, w1, x);		
			VResultSet tsx = VegTest.testSets(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), tuneDs);
			if (tsx.passTotal >= ts.passTotal) {
				ts = tsx;
				w = w1;
			}
		}
		System.out.print(".");	
				
		if (!dp.isCfgNSTurnedOff(x) && !offOnly) {
			double w1 = bw*0.90; 
			if (setCfgNSWeight(dp, wset, noOff, w1, x)) {
				VResultSet tsx = VegTest.testSets(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), tuneDs);
				if (tsx.passTotal >= ts.passTotal) {
					ts = tsx;
					w = w1;
				} else {
					dp.setCfgNSTurnedOff(x, false);
				}
			}
		}
		System.out.print(".");	
		
		if (!dp.isCfgNSTurnedOff(x) && !offOnly) {
			double w1 = bw/2; 
			if (setCfgNSWeight(dp, wset, noOff, w1, x)) {
				VResultSet tsx = VegTest.testSets(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), tuneDs);
				if (tsx.passTotal >= ts.passTotal) {
					ts = tsx;
					w = w1;
				} else {
					dp.setCfgNSTurnedOff(x, false);
				}
			}
		}
		System.out.print(".");	
		
		if (!minDown && !dp.isCfgNSTurnedOff(x) && !offOnly) {
			double w1 = bw/10; 
			if (setCfgNSWeight(dp, wset, noOff, w1, x)) {
				VResultSet tsx = VegTest.testSets(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), tuneDs);
				if (tsx.passTotal >= ts.passTotal) {
					ts = tsx;
					w = w1;
				} else {
					dp.setCfgNSTurnedOff(x, false);
				}
			}
		}
		System.out.print(".");	
		
		if (!noOff && !dp.isCfgNSTurnedOff(x)) {
			setCfgNSWeight(dp, wset, noOff, 0, x);
			VResultSet tsx = VegTest.testSets(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), tuneDs);
			System.out.print("["+tsx.passTotal+"]");	
			if (tsx.passTotal > (ts.passTotal+MIN_DIFF_OFF)) {
				ts = tsx;
				w = 0;
			} else {
				dp.setCfgNSTurnedOff(x, false);
			}
		}

		String ar = "==>";
		if (x == dp.getCfgNSIdentityNumber()) ar = "=I>";
		else if (x == dp.getCfgNSFullNumber()) ar = "=F>";
		System.out.print("["+String.format("%.8f", w)+"]  "+ar+"  tot["+ts.passTotal+"]["+String.format("%.2f", ts.passPercent) +"%]");	
		
		wset[x] = cw;		
		
		boolean update = false;
		if (bestts.passTotal < ts.passTotal) update = true;
		else if (bestts.passTotal == ts.passTotal && w != cw) {
			if (!CHANGE_ONLY || minDown) update = true;
		}
		
		if (update) {
			wset[x] = w;
			if (independent) ts.passTotal = bestts.passTotal; // bad info presented
			if (w <= MIN_W) {
				wset[x] = 0;
				bestts = ts;
				dp.setCfgNSTurnedOff(x, true);
				System.out.println(" -> OFF  <<  ["+bestts.passTotal+"]");
			} else {
				// if no change in results we will lower
				dp.setCfgNSTurnedOff(x, false);
				if (bestts.passTotal == ts.passTotal) {
					if (w > cw) System.out.println(" -> up   <<  ["+bestts.passTotal+"]");
					else System.out.println(" -> down <<  ["+bestts.passTotal+"]");
				} else {
					bestts = ts;
					if (w > cw) System.out.println(" -> UP   <<  ["+bestts.passTotal+"]");
					else System.out.println(" -> DOWN <<  ["+bestts.passTotal+"]");
				}
			}
			
		} else {  // not better... set it back	
			dp.setCfgNSTurnedOff(x, false);
			//dp.setCfgNSWeight(x, cw);
			if (bestts.passTotal != ts.passTotal) System.out.println(" -> NOPE <<  ["+bestts.passTotal+"]");
			else System.out.println(" -> nope <<  ["+bestts.passTotal+"]");
		}
		
		return bestts;
	}
	

	// get the positions for the value
	public static List<Integer> getPositionsOf(List<Integer> set, int value) {
		List<Integer> ret = new ArrayList<>();
		for (int k=0;k<set.size();k++) {
			if (set.get(k) == value) ret.add(k);
		}
		return ret;
	}
	
}
