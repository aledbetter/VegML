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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


import vegml.VResultSet.ResultValueStat;
import vegml.VegML.PredictionType;
import vegml.ValProb;
import vegml.Data.VDataSets;


class OptimizerMerge {
	/*
	 * NOTES:

	 *  5) merge optimize
	 *    - run both models, get full results
	 *    - run merge values in loop NOT in full process
	 *    - try non-linear merges.. 
	 *    		- weighting values	
	 *    		- not including values
	 */
	
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
	static final double TMIN_INC = 0.0005;	


	static void optimizeMergeValue(VDataPlane dp, String valueName, double initTestSet [], VDataSets dss) {
		optimizeValue(dp, "MW_"+valueName, initTestSet, dss);
	}
	
	// NOTE: this make wrong turns from time to time, fix if better optimize is needed
	private static double base_merge_set [] = {0, 100, 1, 0.01, 0.05, 0.1, 0.25, 0.3, 25, 0.5, 0.75, 2, 5, 10, 20, 30, 50, 80};	
	static double optimizeValue(VDataPlane dp, String valueName, double initTestSet [], VDataSets dss) {
		ArrayList<VResultSet> rList = new ArrayList<>();
		
		// try current value
		double sVal = dp.getCfgScratchDouble(valueName);	
		double initSet [] = base_merge_set;
		if (initTestSet != null) initSet = initTestSet;

		boolean fullData = false;
		
		////////////////////////////////////////////////////
		// set dataSet
		dss.genVSets();
		VDataSets tuneDs = dss.getTuneDataSets();
		VDataSets fullDs = tuneDs;
		if (fullData) fullDs = dss.getTrainAndTuneDataSets();
		// Threads and data subsets; thread count based on size	
		int setCount = fullDs.size()/30;
		if (setCount > 16) setCount = 16;
		List<VDataSets> threadFullDs = fullDs.getDataSetSplit(setCount);		
		System.out.println("optimizeValue["+dp.getDimensionTag()+"/"+dp.getTag()+"] MERGE[" + valueName + "]sz["+initSet.length+"] current[" + sVal+"]ds["+fullDs.size()+"]");

		
		double maxVal = TMAX, minVal = TMIN; // left /  right
		double curVal = sVal;
		VResultSet lastts = null;
		int ldir = 0;
		int sameSame = 0;
		VResultSet bts = null;
		int baseLine = 0;
		List<Object> set = new ArrayList<>();
		
		for (int xcnt=0;xcnt<MAX_ITR;xcnt++) {
			// make update
			dp.setCfgScratch(valueName, curVal);
			//dp.printCfgScratch();
			
			//////////////////////////////
			// check early exit for no-value merges
			if (xcnt == 6) {
				Collections.sort(rList, VegUtil.ResultSetSort);
				if (rList.get(rList.size()-1).passTotal == rList.get(0).passTotal) {
					break;
				}
			}
			String s = String.format("%10s", String.format("%.6f", curVal)) ;
			System.out.print("   Opt[" + valueName + "][" + s + "]  =>  ");

		
			
			//////////////////////////////
			// Test with Value
			VResultSet ts = MLThreadUtil.runTestPredictFull(dp, set, curVal, threadFullDs);
			ts.val = curVal;
			rList.add(ts);
			int dir = 0;
			if (sVal == curVal) baseLine = ts.passTotal;

			//////////////////////////////
			// test results
			// if results are non-linear this will be a problem.. retain all datapoints ?
			if (lastts == null || ts.passTotal > lastts.passTotal) {						
				dir = ldir; // better than last-> same direction
			} else if (ts.passTotal == lastts.passTotal) {
				dir = 0; // same as last
			} else { // worse than las				
				if (ldir == 1) dir = 0; // smaller
				else dir = 1; // larger
			}

			// if diff less than or == min done
			String tag = "     ";
			if (baseLine < ts.passTotal) tag = "+"+String.format("%-4d", (ts.passTotal-baseLine));
			else if (baseLine > ts.passTotal) tag = "-"+String.format("%-4d", (baseLine-ts.passTotal));
			System.out.println("unk["+String.format("%5d", ts.getPassTotal(PredictionType.PredictUnknown))+"] pass[" + String.format("%7d", ts.passTotal) + "][" + fmtPercent(ts.passPercent)+"]"+tag);
			
			
			//////////////////////////////
			// decide next: left/right/done
			double nextVal = -1;
			if (xcnt < initSet.length) {
				// check initial list
				nextVal = initSet[xcnt];
				if (rlistContains(rList, nextVal)) {
					xcnt++;
					if (xcnt < initSet.length) nextVal = initSet[xcnt];
					else nextVal = -1;	
				}
			} 
			
			if (nextVal == -1) {
				if (xcnt == initSet.length) {
					// update min/max for best from best 2 in set
					Collections.sort(rList, VegUtil.ResultSetSort);
					bts = rList.get(0);
					if (bts.val < rList.get(1).val) {
						//System.out.println("     BST1["+rList.get(1).val+"] -> ["+bts.val+"]");
						maxVal = rList.get(1).val;
						curVal = minVal = bts.val;
						dir = 1; // left/larger
					} else {
						//System.out.println("     BST2["+bts.val+"] -> ["+rList.get(1).val+"]");
						curVal = maxVal = bts.val;
						minVal = rList.get(1).val;
						dir = 0; // right/smaller
					}
					lastts = ts = bts;
				}  
				//if (lastts != null && lastts.val != ts.val && lastts.PredictTotal == ts.PredictTotal) {
				if (lastts != null && lastts.val != ts.val && lastts.passTotal == ts.passTotal) {
					//System.out.println("     DoneX["+curVal+"] last["+lastts.val+"]["+lastts.PredictTotal+"]");
					sameSame++;
					if (sameSame == 2) break; // done here..
				} else {
					sameSame = 0;
				}
				boolean done = false, realDone = false;
				while (!done) {
					if (dir == 0) { // go smaller (right)			
						nextVal = getNext(xcnt, curVal, maxVal, minVal, true);
						maxVal = curVal;
						if ((curVal-nextVal) <= TMIN_INC) {
							//System.out.println("     DoneM1["+curVal+"] ["+nextVal+"]");
							realDone = true;
							break;
						}
					} else if (dir == 1) { // go larger (left)
						nextVal = getNext(xcnt, curVal, maxVal, minVal, false);
						minVal = curVal;
						if ((nextVal-curVal) <= TMIN_INC) {
							//System.out.println("     DoneM2["+curVal+"] ["+nextVal+"]");
							realDone = true;
							break;
						}
					}
					done = true;
					// check if we have been there... if so try again 1/2 way
					for (VResultSet xts:rList) {
						if (xts.val == nextVal) {
							done = false;
							if (dir == 0) curVal = minVal = nextVal;
							else curVal = maxVal = nextVal;
							break;
						}
					}
				}
				if (realDone) break;
			}
			// new min/max/last
			curVal = nextVal;
			lastts = ts;
			ldir = dir;
		}
		MLThreadUtil.endThreads();
		
		// get final and update
		Collections.sort(rList, VegUtil.ResultSetSort);
		double bestValue = rList.get(0).val;
		
		// if all the same, it is not usefull
		if (rList.get(rList.size()-1).passTotal == rList.get(0).passTotal) bestValue = 0; 
		dp.setCfgScratch(valueName, bestValue);
		String tag = "     ";
		if (baseLine < rList.get(0).passTotal) tag = "+"+String.format("%-4d", (rList.get(0).passTotal-baseLine));
		else if (baseLine > rList.get(0).passTotal) tag = "-"+String.format("%-4d", (baseLine-rList.get(0).passTotal));
		System.out.println("  FINAL["+valueName+"]["+String.format("%.4f", bestValue)+"] pass["+String.format("%7d", rList.get(0).passTotal)+"]["+fmtPercent(rList.get(0).passPercent)+"%]"+tag);	
		System.out.println("");

		return bestValue;
	}
	private static boolean rlistContains(ArrayList<VResultSet> rList, double val) {
		for (int i=0;i<rList.size();i++) {
			if (rList.get(i).val == val) return true;
		}
		return false;
	}
	private static double getNext(int cnt, double last, double max, double min, boolean goMin) {
		if (goMin) return last - ((last - min) / (double)2);
		return  last + ((max - last) / (double)2);
	}
	static int optimizeValueAmp(VDataPlane dp, String valueName, VDataSets dss) {
		// check tune both vals AND flag
		// FIXME
		return optimizeValueInt(dp, valueName, null, dss);
	}
	
	static int base_tune_amp []  = {-2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 16, 17, 18, 19, 20, 25, 30, 35, 40, 50, 75, 100};	
	static int optimizeValueInt(VDataPlane dp, String valueName, int initTestSet [], VDataSets dss) {
		ArrayList<VResultSet> rList = new ArrayList<>();
		
		// try current value
		int sVal = dp.getCfgScratchInt(valueName);	
		int initSet [] = base_tune_amp;
		if (initTestSet != null) initSet = initTestSet;
		System.out.println("optimizeValue["+dp.getDimensionTag()+"/"+dp.getTag()+"] MERGE[" + valueName + "]sz["+initSet.length+"] current[" + sVal+"]");

		
		// setup data segments for theading
		// FIXME
		
		
	
		
		
		int maxVal = 0, minVal = 100; // left /  right
		int curVal = sVal;
		VResultSet lastts = null;
		int ldir = 0;
		int sameSame = 0;
		VResultSet bts = null;
		int baseLine = 0;
		
		dss.genVSets();
		VDataSets tuneDs = dss.getTuneDataSets();
		
		for (int xcnt=0;xcnt<MAX_ITR;xcnt++) {
			// make update
			dp.setCfgScratch(valueName, curVal);
			
			//////////////////////////////
			// check early exit for no-value merges
			if (xcnt == 6) {
				Collections.sort(rList, VegUtil.ResultSetSort);
				if (rList.get(rList.size()-1).passTotal == rList.get(0).passTotal) {
					break;
				}
			}
			String s = String.format("%10s", String.format("%4d", curVal));
			System.out.print("   Opt[" + valueName + "][" + s + "]  =>  ");

			//////////////////////////////
			// Test with Value
			// FIXME use thread API call
			dp.setCfgFramerArg(tuneDs.getValLLV());
			VResultSet ts = VegTest.testSets(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), tuneDs);
			ts.val = curVal;
			rList.add(ts);
			int dir = 0;
			if (sVal == curVal) baseLine = ts.passTotal;
			dp.setCfgFramerArg(null);

			//////////////////////////////
			// test results
			// if results are non-linear this will be a problem.. retain all datapoints ?
			if (lastts == null || ts.passTotal > lastts.passTotal) {						
				dir = ldir; // better than last-> same direction
			} else if (ts.passTotal == lastts.passTotal) {
				dir = 0; // same as last
			} else { // worse than las				
				if (ldir == 1) dir = 0; // smaller
				else dir = 1; // larger
			}

			// if diff less than or == min done
			String tag = "     ";
			if (baseLine < ts.passTotal) tag = "+"+String.format("%-4d", (ts.passTotal-baseLine));
			else if (baseLine > ts.passTotal) tag = "-"+String.format("%-4d", (baseLine-ts.passTotal));
			System.out.println("unk["+String.format("%5d", ts.getPassTotal(PredictionType.PredictUnknown))+"] pass[" + String.format("%7d", ts.passTotal) + "][" + fmtPercent(ts.passPercent)+"]"+tag);
			
			
			//////////////////////////////
			// decide next: left/right/done
			int nextVal = -2;
			if (xcnt < initSet.length) {
				// check initial list
				nextVal = initSet[xcnt];
				if (rlistContains(rList, nextVal)) {
					xcnt++;
					if (xcnt < initSet.length) nextVal = initSet[xcnt];
					else nextVal = -2;	
				}
			} 
			
			if (nextVal == -2) {
				if (xcnt == initSet.length) {
					// update min/max for best from best 2 in set
					Collections.sort(rList, VegUtil.ResultSetSort);
					bts = rList.get(0);
					if (bts.val < rList.get(1).val) {
						//System.out.println("     BST1["+rList.get(1).val+"] -> ["+bts.val+"]");
						maxVal = (int)rList.get(1).val;
						curVal = minVal = (int)bts.val;
						dir = 1; // left/larger
					} else {
						//System.out.println("     BST2["+bts.val+"] -> ["+rList.get(1).val+"]");
						curVal = maxVal = (int)bts.val;
						minVal = (int)rList.get(1).val;
						dir = 0; // right/smaller
					}
					lastts = ts = bts;
				}  
				//if (lastts != null && lastts.val != ts.val && lastts.PredictTotal == ts.PredictTotal) {
				if (lastts != null && lastts.val != ts.val && lastts.passTotal == ts.passTotal) {
					//System.out.println("     DoneX["+curVal+"] last["+lastts.val+"]["+lastts.PredictTotal+"]");
					sameSame++;
					if (sameSame == 2) break; // done here..
				} else {
					sameSame = 0;
				}
				boolean done = false, realDone = false;
				while (!done) {
					if (dir == 0) { // go smaller (right)			
						nextVal = (int)getNext(xcnt, curVal, maxVal, minVal, true);
						maxVal = curVal;
						if ((curVal-nextVal) <= TMIN_INC) {
							//System.out.println("     DoneM1["+curVal+"] ["+nextVal+"]");
							realDone = true;
							break;
						}
					} else if (dir == 1) { // go larger (left)
						nextVal = (int)getNext(xcnt, curVal, maxVal, minVal, false);
						minVal = curVal;
						if ((nextVal-curVal) <= TMIN_INC) {
							//System.out.println("     DoneM2["+curVal+"] ["+nextVal+"]");
							realDone = true;
							break;
						}
					}
					done = true;
					// check if we have been there... if so try again 1/2 way
					for (VResultSet xts:rList) {
						if (xts.val == nextVal) {
							done = false;
							if (dir == 0) curVal = minVal = (int)nextVal;
							else curVal = maxVal = (int)nextVal;
							break;
						}
					}
				}
				if (realDone) break;
			}
			// new min/max/last
			curVal =(int) nextVal;
			lastts = ts;
			ldir = dir;
		}
		
		// get final and update
		Collections.sort(rList, VegUtil.ResultSetSort);
		int bestValue = (int)rList.get(0).val;
		
		// if all the same, it is not usefull
		if (rList.get(rList.size()-1).passTotal == rList.get(0).passTotal) bestValue = 0; 
		dp.setCfgScratch(valueName, bestValue);
		String tag = "     ";
		if (baseLine < rList.get(0).passTotal) tag = "+"+String.format("%-4d", (rList.get(0).passTotal-baseLine));
		else if (baseLine > rList.get(0).passTotal) tag = "-"+String.format("%-4d", (baseLine-rList.get(0).passTotal));
		System.out.println("  FINAL["+valueName+"]["+String.format("%4d", bestValue)+"] pass["+String.format("%7d", rList.get(0).passTotal)+"]["+fmtPercent(rList.get(0).passPercent)+"%]"+tag);	
		System.out.println("");

		return bestValue;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// MergeMap
	//
	// resulting map of a merge between 2 models
	// maintains merge info for single, per PredictionType and per PredictionType/PredictionType
	//
	// methods on the class should be used to get mergeValue
	//
	// MODES:
	// -1 = always, 0 = never, 1 = single predictionType, 2 = double PredictionType, 3 = single value, 4 = even/1
	//
	public static class MergeMap {
		// per predictionType/predictionType
		double [][] megeMatrix = null;
		HashMap<Long, Double> valueMatrixWMatrix[][] = null;				
		// any merge for predictionType
		boolean [] megeMap = null;
		// single
		double megeValue;		
		HashMap<Long, Double> valueSingleWMatrix = null;
		// per predicitonType
		double [] megeList = null;
		HashMap<Long, Double> valueListWMatrix[] = null;
		int baseLineTotal; // base pass
		int baseTotal; // count
		int matrixTotal;
		int listTotal;
		int singleTotal;
		PredictionType opType;
		int mergeMode = 3; // current merge mode
		
		MergeMap() {
			mergeMode = 3;
			megeValue = -1;
		}
		
		// mode in gets
		// -1 = always, 0 = never, 1 = single predictionType, 2 = double PredictionType, 3 = single value, 4 = even/1
		public double getMergeValue(PredictionType pType1, PredictionType pType2, int mode) {
			if (mode == -1) return 100;
			if (mode == 0) return 0;
			if (mode == 4) return 1;
			if (mode == 1) return getMergeValue(pType1);
			if (mode == 2) return getMergeValue(pType1, pType2);
			return getMergeValue();
		}
		public HashMap<Long, Double> getMergeValueVM(PredictionType pType1, PredictionType pType2, int mode) {
			if (true) return null; /// HACK to disable for now
			// results are poor, need to factor in full picture in selecting final
			if (mode == -1) return null;
			if (mode == 0) return null;
			if (mode == 4) return null;
			if (mode == 2) return null;
		//	if (mode == 2) return getMergeValueVM(pType1, pType2);
			if (mode == 1) return getMergeValueVM(pType1);
			return getMergeValueVM();
		}
		public double getMergeValue(PredictionType pType1, PredictionType pType2) {
			return this.megeMatrix[pType1.ordinal()][pType2.ordinal()];
		}
		public HashMap<Long, Double>  getMergeValueVM(PredictionType pType1, PredictionType pType2) {
			if (valueMatrixWMatrix == null) return null;
			return this.valueMatrixWMatrix[pType1.ordinal()][pType2.ordinal()];
		}
		public double getMergeValue(PredictionType pType1) {
			return this.megeList[pType1.ordinal()];
		}
		public HashMap<Long, Double>  getMergeValueVM(PredictionType pType1) {
			if (valueListWMatrix == null) return null;
			return this.valueListWMatrix[pType1.ordinal()];
		}
		public double getMergeValue() {
			return megeValue;
		}
		public HashMap<Long, Double>  getMergeValueVM() {
			return valueSingleWMatrix;
		}
		public boolean haveMergeValue(PredictionType pType1) {
			return this.megeMap[pType1.ordinal()];
		}
		public int getMergeMode() {
			return this.mergeMode;
		}
		public int setMergeMode(int mmode) {
			return this.mergeMode = mmode;
		}
		public boolean haveMergeValue(PredictionType pType1, int mode) {
			if (mode == -1) return true;
			if (mode == 0) return false;
			if (mode == 4) return true;
			if (mode == 3 && megeValue != -1) return true;
			return this.megeMap[pType1.ordinal()];
		}
		void print() {
			System.out.println("----------------------------------");
			System.out.println("OPTIMIZED: " +((opType == null)?"ALL":opType));
			System.out.println("----------------------------------");
			System.out.println(" SINGLE PASS TOTAL["+fmtPercent(((double)singleTotal/(double)baseTotal)*100)+"]["+singleTotal+" of "+baseTotal+"] base["+baseLineTotal+"]");
			System.out.print("      SINGLE["+String.format("%10s", String.format("%.6f", megeValue))+"]");
			if (valueSingleWMatrix != null) System.out.print("  =>  Value Map["+valueSingleWMatrix.keySet().size()+"]");	
			System.out.println();
			System.out.println("----------------------------------");
			System.out.println(" LINE PASS TOTAL["+fmtPercent(((double)listTotal/(double)baseTotal)*100)+"]["+listTotal+" of "+baseTotal+"] base["+baseLineTotal+"]");
			for (int xx=0;xx<megeList.length;xx++) {
				if (megeList[xx] == 0 && (valueListWMatrix == null || valueListWMatrix[xx] == null)) continue;
				
				String lbl = String.format("%-14s", VegML.getPredictionTypeEnum(xx));
				System.out.print("      "+lbl+"["+String.format("%10s", String.format("%.6f", megeList[xx]))+"]");
				if (valueListWMatrix != null && valueListWMatrix[xx] != null) System.out.print("  =>  Value Map["+valueListWMatrix[xx].keySet().size()+"]");			
				System.out.println();
			}
			System.out.println("----------------------------------");
			System.out.println(" MATRIX PASS TOTAL["+fmtPercent(((double)matrixTotal/(double)baseTotal)*100)+"]["+matrixTotal+" of "+baseTotal+"] base["+baseLineTotal+"]");
			for (int xx=0;xx<megeMatrix.length;xx++) {
				for (int yy=0;yy<megeMatrix[0].length;yy++) {		
					if (megeMatrix[xx][yy] == 0 && (valueMatrixWMatrix == null || valueMatrixWMatrix[xx][yy] == null)) continue;
					
					String lbl = String.format("%-14s", VegML.getPredictionTypeEnum(xx))+"|"+String.format("%-14s", VegML.getPredictionTypeEnum(yy));				
					System.out.print("      "+lbl+"["+String.format("%10s", String.format("%.6f", megeMatrix[xx][yy]))+"]");
					if (valueMatrixWMatrix != null && valueMatrixWMatrix[xx][yy] != null) System.out.print("  =>  Value Map["+valueMatrixWMatrix[xx][yy].keySet().size()+"]");			
					System.out.println();				
				}
			}
		}
	}
	
	
	//
	// tuning value result
	//
	static class MergeResult {
		int resultValue;
		double mergeValue;
		MergeResult(int resultValue, double mergeValue) {
			this.resultValue = resultValue;
			this.mergeValue = mergeValue;
		}
	}
	
	//
	// Set of results for a number being tuned
	//
	static class MergeResultSet {
		static final int MIN_TO_STAY = 2;
		PredictionType pt1;
		PredictionType pt2;
		
		List<MergeResult> list;
		MergeResult best;	
		MergeResult left;
		MergeResult right;	
		MergeResult last;
		double lastMergeValue;
		boolean done;
		
		// per value mode
		HashMap<Long, List<MergeResult>> valueWeightList = null; // results
		HashMap<Long, Double> valueWeightMatrix = null; // best for on-going dependencies
		int valueCurPos;
		long valueCur;
		double valueMergeValueCur;
		int valueMergeValueCurPos;
		boolean valueDone;
		
		MergeResultSet() {
			this.list = new ArrayList<>();
			this.done = false;
			this.lastMergeValue = -1;
		}
		MergeResultSet(PredictionType pt1) {
			this();
			this.pt1 = pt1;
		}
		MergeResultSet(PredictionType pt1, PredictionType pt2) {
			this(pt1);
			this.pt2 = pt2;
		}
		void add(MergeResult res) {
			list.add(res);
		}
		void sort() {
			sortList(list);
		}
		void endValuePhase(VDataPlane dp, int basePassTotal) {
			valueWeightMatrix.clear();
			System.out.println("["+getLable()+"] ValMap: " + valueWeightList.keySet().size());
			for (Long val:valueWeightList.keySet()) {
				List<MergeResult> mrl = valueWeightList.get(val);
				MergeResult mBest = getBest(sortList(mrl), false);
				String tag = "     ";
				if (basePassTotal < mBest.resultValue) {
					
					int diff = (mBest.resultValue-basePassTotal);
					tag = "+"+String.format("%-4d", diff);
					if (diff > 1) {					
						valueWeightMatrix.put(val, mBest.mergeValue);	
						if (mBest.mergeValue == 0 || mBest.mergeValue == 100) {
							System.out.println("  VAL["+String.format("%-6s", dp.getString(val))+"]"+tag+" ["+String.format("%6d", mBest.resultValue)+"]["+String.format("%.4f", mBest.mergeValue)+"]");				
						} else {
							System.out.println("  val["+String.format("%-6s", dp.getString(val))+"]"+tag+" ["+String.format("%6d", mBest.resultValue)+"]["+String.format("%.4f", mBest.mergeValue)+"]");						
						}						
					}
				} else if (basePassTotal > mBest.resultValue) {
					tag = "-"+String.format("%-4d", (basePassTotal-mBest.resultValue));
				}
			}
		}
		List<MergeResult> sortList(List<MergeResult> dlist) {
			Collections.sort(dlist, new Comparator<MergeResult>() {
		        @Override
		        public int compare(MergeResult l, MergeResult r) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
		        	if (l.resultValue < r.resultValue) return 1;
		        	if (l.resultValue > r.resultValue) return -1;
		        	if (l.mergeValue < r.mergeValue) return 1;
		        	if (l.mergeValue > r.mergeValue) return -1;
		        	return 0;  
		        }
		    });
	        return dlist;
		}
		void setupValueWeight(List<Long> dataValList) {
			this.valueWeightList = new HashMap<>();
			this.valueWeightMatrix = new HashMap<>();
			this.valueDone = false;
			for (Long val:dataValList) this.valueWeightList.put(val, new ArrayList<>());	
		}
		private MergeResult getBest(List<MergeResult> dlist, boolean noExclude) {
			MergeResult mBest = dlist.get(0);
			
			if (noExclude && (dlist.get(0).mergeValue == 100 || dlist.get(0).mergeValue == 0)) {
				for (int cnt = 0;cnt<dlist.size();cnt++) {
					MergeResult mr = dlist.get(cnt);
					if (mr.mergeValue != 100 && mr.mergeValue != 0) {
						mBest = mr;
						break;
					}
				}
			} else if (dlist.get(0).resultValue == dlist.get(1).resultValue) {
				if (dlist.get(0).resultValue == 0) {
					// set this to zero
					for (int i=0;i<dlist.size();i++) {
						if (dlist.get(i).mergeValue == 0) {
							mBest = dlist.get(i);
							break;
						}
					}
				} else if (dlist.get(0).mergeValue == 0 || dlist.get(1).mergeValue == 0) {
					if ((dlist.get(0).resultValue-dlist.get(2).resultValue) <= MIN_TO_STAY) { // if all 0.. then no					
						// tie and one is 0 - use next to decide how much the 0 wins by
						if (dlist.get(0).mergeValue == 0) mBest = dlist.get(1);
					} else if ((dlist.get(0).resultValue-dlist.get(1).resultValue ) <= MIN_TO_STAY) { // if all 0.. then no					
						if (dlist.get(0).mergeValue == 0) mBest = dlist.get(1);
					}
					//System.out.println("     "+this.getLable()+" => DONE-0["+this.best.resultValue+"]["+this.best.mergeValue+"]");
				} else if (dlist.get(0).mergeValue == 100 || dlist.get(1).mergeValue == 100) {
					if ((dlist.get(0).resultValue-dlist.get(2).resultValue ) <= MIN_TO_STAY) { // if all 0.. then no					
						// tie and one is 100	- use next to decide how much the 100 wins by
						if (dlist.get(0).mergeValue == 100) mBest = dlist.get(1);
					} else if ((dlist.get(0).resultValue-dlist.get(1).resultValue) < MIN_TO_STAY) { // if all 0.. then no					
						if (dlist.get(0).mergeValue == 100) mBest = dlist.get(1);
					}
					//System.out.println("     "+this.getLable()+" => DONE-100["+r+"]["+dlist.get(1).resultValue+"]["+this.best.resultValue+"]["+this.best.mergeValue+"] ["+dlist.get(0).mergeValue+"]["+dlist.get(1).mergeValue+"]");
				} else {
					// series of same.. find middle for most stable
					int cnt = 0;
					MergeResult first = null;
					for (;cnt<dlist.size();cnt++) {
						if (dlist.get(cnt).resultValue != mBest.resultValue) break;
						// there may be gaps.. where this would not work
						if (first != null && isBetween(dlist.get(cnt), first)) break;
						first = dlist.get(cnt);
					}
					if (cnt > 3 && first.mergeValue != mBest.mergeValue) {
						double mvM = (first.mergeValue+mBest.mergeValue) / 2;
						MergeResult alt = null;
						for (int i = 0;i<dlist.size();i++) {
							if (dlist.get(i) == first) break;
							if (dlist.get(i).mergeValue < mvM) break;
							alt = dlist.get(i);
						}					
						//System.out.println("     "+this.getLable()+" => DONE+"+cnt+"["+first.mergeValue+"]->["+this.best.mergeValue+"] ["+this.best.resultValue+"] => ["+alt.mergeValue+"] ["+mvM+"]");
						mBest = alt;
					}
				}
			} else if (dlist.get(0).mergeValue == 0) {
				if ((dlist.get(0).resultValue-dlist.get(1).resultValue) <= MIN_TO_STAY) { // if all 0.. then no					
					if (dlist.get(0).mergeValue == 0) mBest = dlist.get(1);
				}
				//System.out.println("     "+this.getLable()+" => DONE-0["+this.best.resultValue+"]["+this.best.mergeValue+"]");
			} else if (dlist.get(0).mergeValue == 100) {
				if ((dlist.get(0).resultValue-dlist.get(1).resultValue) <= MIN_TO_STAY) { // if all 0.. then no					
					if (dlist.get(0).mergeValue == 100) mBest = dlist.get(1);
				}
			} 
			
			return mBest;
		}
		double setDone(boolean noExclude) {
			if (this.done) return this.best.mergeValue;
			this.done = true;
			sort();		
			this.best = getBest(list, noExclude);
			return this.best.mergeValue;
		}
		boolean isBetween(MergeResult small, MergeResult large) {
			for (int cnt = 0;cnt<list.size();cnt++) {
				if (list.get(cnt).mergeValue > small.mergeValue && list.get(cnt).mergeValue < large.mergeValue) return true;
			}
			return false;
		}
		
		// setup from values with positions filed
		void setForStart() {
			sort();
			this.best = this.right = this.list.get(0);
			this.left = this.list.get(1);
			if (left.mergeValue > this.right.mergeValue) {
				this.right = this.left;
				this.left = this.best;
			}
			this.last = null;
		}
		String getLable() {
			if (pt1 != null && pt2 != null) return String.format("%-14s", pt1)+"|"+String.format("%-14s", pt2);				
			else if (pt1 != null) return String.format("%-14s", pt1);				
			return "SINGLE";
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// merge types	
	private static final int MAX_TRY = 150;
	private static List<Double> modSet = null;
	public static boolean print = true;
	
	static MergeMap optimizeMergeModels(VDataPlane dp1, VDataPlane dp2,
			boolean modeLinear, boolean modeMatrix,
			PredictionType opType,
			boolean noExclude, 		// don't allow 0 / 100 
			boolean fullData, VDataSets dss) {
		return optimizeMergeModels(dp1, dp2, modeLinear, modeMatrix, opType, noExclude, false, fullData, dss);
	}
	@SuppressWarnings("unchecked")
	static MergeMap optimizeMergeModels(VDataPlane dp1, VDataPlane dp2,
			boolean modeLinear, boolean modeMatrix,
			PredictionType opType,
			boolean noExclude, 		// don't allow 0 / 100 
			boolean exactData, boolean fullData, VDataSets dss) {
		
		boolean show_level1 = false;
		boolean show_level2 = false;
		boolean show_values = false;
		
		boolean modeSingle = true;
		boolean modeSingleAndValue = false; // needs work to work-in-transfer
		boolean modeLinearAndValue = false; // needs work to work-in-transfer
		boolean modeMatrixAndValue = false; // needs work to work-in-transfer
		
		
		//////////////////////////
		// make a base test value list
		if (modSet == null) {
			modSet = new ArrayList<>();	
			for (double i=0;i<2;i+=0.05) modSet.add(i);
			for (double i=2;i<5;i+=0.25) modSet.add(i);
			for (double i=5;i<20;i+=1) modSet.add(i);
			for (double i=20;i<=100;i+=5) modSet.add(i);
		}
		double [] dataValMergeWeightSet = {0, 100, 0.05, 2, 20, 0.25, 0.5, 0.75, 0.95, 1.25, 1.5, 1.75, 2.5, 3, 5, 10, 50, 80};

		
		//////////////////////////
		// setup Data Set
		dss.genVSets();
		VDataSets fullDs = dss;
		if (!exactData) {
			fullDs = dss.getTuneDataSets();
			if (fullData) fullDs = dss.getTrainAndTuneDataSets();
		}

		if (print) System.out.println("MEGE_OPT["+opType+"] data["+ fullDs.size()+"]");

		//////////////////////////
		// Run Models to get result-model and baseline
		
		// make sure callout doeson't go
		dp1.setCfgMergeBreakBefore(dp2.getDimensionTag(), dp2.getTag(), true);
		dp2.setCfgCallout(null, null);
		
		dp1.setCfgFramerArg(fullDs.getValLLV());
		System.out.print("MERGE-EVAL["+dp1.getDimensionTag()+"/"+dp1.getTag()+"]");
		
		VResultSet rst1 = VegTest.testSets(dp1.getVegML(), dp1.getDimensionTag(), dp1.getTag(), fullDs, -1);	
		System.out.print("["+perc(rst1.getPassPercent(opType))+"]["+rst1.getPassTotal(opType)+" / "+rst1.getFailTotal(opType)+"]["+rst1.total+"]");		
		dp1.setCfgFramerArg(null);
		
		dp2.setCfgFramerArg(fullDs.getValLLV());
		System.out.print(" >> ["+dp2.getDimensionTag()+"/"+dp2.getTag()+"]");
		VResultSet rst2 = VegTest.testSets(dp2.getVegML(), dp2.getDimensionTag(), dp2.getTag(), fullDs, -1);			
		System.out.print("["+perc(rst2.getPassPercent(opType))+"]["+rst2.getPassTotal(opType)+" / "+rst2.getFailTotal(opType)+"]");		
		if (print) System.out.println("");
		else System.out.print("  => ");
		
		dp1.setCfgMergeBreakBefore(dp2.getDimensionTag(), dp2.getTag(), false);
		dp2.setCfgFramerArg(null);

		int passTypeTotalBase = rst1.getPassTotal(opType); // specific to op
		int typeTotal = rst1.getTotal(opType); // specific to op


		//////////////////////////
		// setup structures
		// make test value set ints
		List<List<Long []>> tuneValueSetsInt = fullDs.getValLLV();
			
		// list for best result
		MergeResultSet [][] megeMatrixRes = null;
		if (modeMatrix) {
			megeMatrixRes = new MergeResultSet[VegML.getPredictionTypeCount()][VegML.getPredictionTypeCount()]; 
			for (int i=0;i<megeMatrixRes.length;i++) {
				for (int x=0;x<megeMatrixRes[0].length;x++) megeMatrixRes[i][x] = new MergeResultSet(VegML.getPredictionTypeEnum(i), VegML.getPredictionTypeEnum(x)); 
			}
		}
		MergeResultSet [] megeListRes = null;
		if (modeLinear) {
			megeListRes = new MergeResultSet[VegML.getPredictionTypeCount()]; 
			for (int i=0;i<megeListRes.length;i++) megeListRes[i] = new MergeResultSet(VegML.getPredictionTypeEnum(i)); 
		}
		MergeResultSet megeRes = new MergeResultSet(); 

		// input value matrix
		double [][] megeMatrix = new double[VegML.getPredictionTypeCount()][VegML.getPredictionTypeCount()];
		// input per value value matrix
		HashMap<Long, Double> valueWeightMatrix[][] = null;
		if (modeSingleAndValue || modeLinearAndValue || modeMatrixAndValue) {
			valueWeightMatrix = new HashMap[VegML.getPredictionTypeCount()][VegML.getPredictionTypeCount()];
			for (int i=0;i<valueWeightMatrix.length;i++) {
				for (int x=0;x<valueWeightMatrix[0].length;x++) valueWeightMatrix[i][x] = new HashMap<>(); 
			}
		}


		// output results
		VResultSet [] megePassSet = new VResultSet[VegML.getPredictionTypeCount()];
		//ResultSet rsf1 = new ResultSet(dp1);
		VResultSet rsf1 = null;
		//ResultSet rsf2 = new ResultSet(dp1);
		VResultSet rsf2 = null;
		VResultSet rsfm = new VResultSet(dp1);
		
		// list of values
		List<Long> dataValList = new ArrayList<>();

		//if (!print) System.out.print("TESTING: ");
		
		//////////////////////////
		// Iterate until done
		// 1) the pre-set list
		// 2) complete single value
		// 3) complete linear values; per PredicitonType
		// 4) complete matrix values; per PredicitonType/PredicitonType
		double mergeMod = 0;
		int modeCnt = 0;
		boolean valueMatrixTest = false;
		int passBaseBest = 0;
		for (int x=0;;x++) {
			
			// limit cycles
			if (x > modSet.size()) {
				if (modeSingle) {
					if (modeCnt > MAX_TRY) {
						modeCnt = 0;
						modeSingle = false;
					}
				} else if (modeLinear) {
					if (modeCnt > MAX_TRY) {
						modeCnt = 0;
						modeLinear = false;
					}					
				} else {
					if (modeCnt > MAX_TRY) {
						modeCnt = 0;
						break; // done
					}	
				}
				modeCnt++;
			}
			
			if (x < modSet.size()) {
				mergeMod = modSet.get(x);
				fillArray(megeMatrix, mergeMod);
			} 
			
			boolean evalDetails = false;
			
			//////////////////////////
			// Run Merge test
			int passBase = 0;
			if (valueMatrixTest) {
				// individual value weights
				passBase = runTest(rst1, rst2, tuneValueSetsInt, megeMatrix, valueWeightMatrix, megePassSet, rsfm, rsf1, rsf2, !evalDetails);
			} else {
				// standard weight matrix
				passBase = runTest(rst1, rst2, tuneValueSetsInt, megeMatrix, null, megePassSet, rsfm, rsf1, rsf2, !evalDetails);
			}
			
			int passTypeBase = rsfm.getPassTotal(opType); // specific to op
			
			// percent
			double perc = ((double)passTypeBase / (double)typeTotal) * 100;
			
			//////////////////////////
			// Record result
			if (x < modSet.size()) {	
				if (passTypeBase > passBaseBest) passBaseBest = passTypeBase;
				{
					String tag = "     ";
					if (passTypeTotalBase < passTypeBase) tag = "+"+String.format("%-4d", (passTypeBase-passTypeTotalBase));
					else if (passTypeTotalBase > passTypeBase) tag = "-"+String.format("%-4d", (passTypeTotalBase-passTypeBase));
					if (x < modSet.size() && print) {
						System.out.print(".");			
						//System.out.print("["+String.format("%6s", String.format("%.2f", mergeMod))+"]"+tag);			
						if (((x+1) % 80) == 0 && x > 1) System.out.println("");					
					}
				}
				
				// SINGLE
				updateMatrix(megeRes, passBase, passTypeBase, mergeMod, true, noExclude);	
				for (int xx=0;xx<megePassSet.length;xx++) {
					// LINEAR
					if (modeLinear) {
						updateMatrix(megeListRes[xx], megePassSet[xx].passTotal, megePassSet[xx].getPassTotal(opType), mergeMod, true, noExclude);
					}
					
					// MATRIX
					if (modeMatrix) {
						for (int yy=0;yy<VegML.getPredictionTypeCount();yy++) {
							int resultValue = megePassSet[xx].getPassTotal(VegML.getPredictionTypeEnum(yy));
							updateMatrix(megeMatrixRes[xx][yy], resultValue, resultValue, mergeMod, true, noExclude);
						//	if (debugType != null && debugType.ordinal() == xx) {
						//		System.out.println("      "+String.format("%-14s", VegML.getPredictionTypeEnum(yy))+ " = " + String.format("%6d", resultValue )+ " ["+mergeMod+"]");
						//	}
						}
					}
				}
				
				if (x == (modSet.size()-1)) {
					// set for self selection
					if (megeMatrixRes != null) {
						for (int i=0;i<megeMatrixRes.length;i++) {
							for (int u=0;u<megeMatrixRes[0].length;u++) megeMatrixRes[i][u].setForStart();
						}
					}
					if (megeListRes != null) {
						for (int i=0;i<megeListRes.length;i++) megeListRes[i].setForStart();
					}
					megeRes.setForStart();
					mergeMod = -1;
					String tag = "     ";
					if (passTypeTotalBase < megeRes.best.resultValue) tag = "+"+String.format("%-4d", (megeRes.best.resultValue-passTypeTotalBase));
					else if (passTypeTotalBase > megeRes.best.resultValue) tag = "-"+String.format("%-4d", (passTypeTotalBase-megeRes.best.resultValue));
					if (print) {
						System.out.println("");
						perc = ((double)megeRes.best.resultValue / (double)typeTotal) * 100;
						System.out.println(" SINGLE-LIST BEST["+fmtPercent(perc)+"]["+passBaseBest+"]"+tag);
					}
				} else {
					continue;
				}
			} 
			
			//////////////////////////////
			// Record result Get NEXT
			//////////////////////////////
			
			//////////////////////////
			// Mode Single 
			if (modeSingle) {
				// single value
				mergeMod = updateMatrix(megeRes, passBase, passTypeBase, mergeMod, false, noExclude);	
				fillArray(megeMatrix, mergeMod);
				String tag = "     ";
				if (megeRes.done) {
					modeSingle = false;
					if (passTypeTotalBase < megeRes.best.resultValue) tag = "+"+String.format("%-4d", (megeRes.best.resultValue-passTypeTotalBase));
					else if (passTypeTotalBase > megeRes.best.resultValue) tag = "-"+String.format("%-4d", (passTypeTotalBase-megeRes.best.resultValue));
					if (print) {
						System.out.println("");
						System.out.println(" MERGE SINGLE-DONE BEST["+fmtPercent(perc)+"]["+megeRes.best.resultValue+"]"+tag +"  =>  ["+megeRes.best.mergeValue+"]");
						System.out.println("");
					} else {
						System.out.print(" MERGE-VAL["+perc(perc)+"]["+megeRes.best.resultValue+"]["+tag +"]>>["+megeRes.best.mergeValue+"]");
					}
					mergeMod = -1;
				} else {
					if (passTypeTotalBase < passTypeBase) tag = "+"+String.format("%-4d", (passTypeBase-passTypeTotalBase));
					else if (passTypeTotalBase > passTypeBase) tag = "-"+String.format("%-4d", (passTypeTotalBase-passTypeBase));
					//System.out.println(" SINGLE-WORK["+x+"] PASS TOTAL["+fmtPercent(perc)+"]["+passTypeBase+" of "+typeTotal+"]"+tag+" val["+String.format("%.6f", mergeMod)+"]");
					//if (x < modSet.size()) {
					//System.out.print("["+String.format("%6s", String.format("%.2f", mergeMod))+"]"+tag);			
			//		System.out.print(".");			
			//		if (((x+1) % 80) == 0 && x > 1) System.out.println("");					
					//}											
					continue;
				}
				if (!modeLinear && !modeMatrix && !modeSingleAndValue) break; // done
			}

			//////////////////////////
			// Mode Single and per value
			if (modeSingleAndValue) {
				// set initial values
				fillArray(megeMatrix, megeRes.best.mergeValue);
				checkValues("SINGLE", dp1, rst1, rst2, tuneValueSetsInt, typeTotal, passTypeTotalBase, opType, megeRes, null, null, dataValList, dataValMergeWeightSet, megePassSet, megeMatrix, valueWeightMatrix);
				modeSingleAndValue = false;
				if (!modeLinear && !modeMatrix) break; // done
			}
			
			//////////////////////////
			// Mode List and per value (after modeLinear)
			if (!modeLinear && modeLinearAndValue) {
				// setup best baseline
				for (int xx=0;xx<megePassSet.length;xx++) {
					Arrays.fill(megeMatrix[xx], megeListRes[xx].best.mergeValue);
				}				
				checkValues("LINE", dp1, rst1, rst2, tuneValueSetsInt, typeTotal, passTypeTotalBase, opType, null, megeListRes, null, dataValList, dataValMergeWeightSet, megePassSet, megeMatrix, valueWeightMatrix);				
				modeLinearAndValue = false;
				if (!modeMatrix) break; // done
			}
			
			if (!modeMatrix && modeMatrixAndValue) {
				// setup best baseline
				for (int xx=0;xx<megePassSet.length;xx++) {
					for (int yy=0;yy<VegML.getPredictionTypeCount();yy++) {
						megeMatrix[xx][yy] = megeMatrixRes[xx][yy].best.mergeValue;
					}
				}				
				checkValues("WORK", dp1, rst1, rst2, tuneValueSetsInt, typeTotal, passTypeTotalBase, opType, null, null, megeMatrixRes, dataValList, dataValMergeWeightSet, megePassSet, megeMatrix, valueWeightMatrix);				

				modeMatrixAndValue = false;
			}
			
			//////////////////////////
			// Linear and Matrix
			int bestCount = 0, doneCnt = 0, optCnt = 0;			
			for (int xx=0;xx<megePassSet.length;xx++) {
				if (modeLinear) {
					// get Next
					double nextValue = updateMatrix(megeListRes[xx], megePassSet[xx].passTotal, megePassSet[xx].getPassTotal(opType), -1, false, noExclude);
					Arrays.fill(megeMatrix[xx], nextValue);

					bestCount += megeListRes[xx].best.resultValue;
					if (megeListRes[xx].done) doneCnt++;
					optCnt++;
				} else if (modeMatrix) {					
					for (int yy=0;yy<VegML.getPredictionTypeCount();yy++) {
						int resultValue = megePassSet[xx].getPassTotal(VegML.getPredictionTypeEnum(yy));
				//		if (debugType != null && debugType.ordinal() == xx) {
				//			System.out.println("      "+String.format("%-14s", VegML.getPredictionTypeEnum(yy))+ " = " + String.format("%6d", resultValue)+ " ["+mergeMod+"]");
				//		}
						double nextValue = updateMatrix(megeMatrixRes[xx][yy], resultValue, resultValue, -1, false, noExclude);
						megeMatrix[xx][yy] = nextValue;
						bestCount += megeListRes[xx].best.resultValue;
						if (megeMatrixRes[xx][yy].done) doneCnt++;
						optCnt++;
					}
				} else if (modeMatrixAndValue) {
					//FIXME
				}
			}

			//////////////////////////
			// check mode done
			String tag = "     ";
			if (passTypeTotalBase < passTypeBase) tag = "+"+String.format("%-4d", (passTypeBase-passTypeTotalBase));
			else if (passTypeTotalBase > passTypeBase) tag = "-"+String.format("%-4d", (passTypeTotalBase-passTypeBase));
			if (doneCnt == optCnt) {
				if (modeLinear) {
					System.out.println(" LINE-DONE[" + doneCnt + " of " + optCnt+"] PASS["+fmtPercent(perc)+"]["+passTypeBase+"]"+tag);
					System.out.println("");
					modeLinear = false;
					if (!modeMatrix && !modeLinearAndValue) break; // done					
				} else {
					System.out.println(" DONE[" + doneCnt + " of " + optCnt+"] PASS["+fmtPercent(perc)+"]["+passTypeBase+"]"+tag);
					//System.out.println("");
					break;
				}
			} else {
				if (modeLinear) {
					System.out.println(" LINE-WORK[" + doneCnt + " of " + optCnt+"] PASS["+fmtPercent(perc)+"]["+passTypeBase+"]"+tag);
				} else {
					System.out.println(" WORK[" + doneCnt + " of " + optCnt+"] PASS["+fmtPercent(perc)+"]["+passTypeBase+"]"+tag);					
				}
			}
		}

		MLThreadUtil.endThreads();
		rst1.reset();
		rst2.reset();
		
		//////////////////////////
		// Build Merge map
		MergeMap mm = new MergeMap();
		mm.megeMatrix = new double[VegML.getPredictionTypeCount()][VegML.getPredictionTypeCount()];
		mm.megeMap = new boolean[VegML.getPredictionTypeCount()];
		mm.megeList = new double[VegML.getPredictionTypeCount()];
		
		Arrays.fill(mm.megeMap, false);
		Arrays.fill(mm.megeList, 0);
		
		// show finals
		int lpassTotal = 0, mpassTotal = 0, spassTotal = 0;
		spassTotal = megeRes.best.resultValue;
		for (int xx=0;xx<megePassSet.length;xx++) {
			if (megeListRes != null) {
				lpassTotal += megeListRes[xx].best.resultValue;
			}
			if (megeMatrixRes != null) {
				for (int yy=0;yy<VegML.getPredictionTypeCount();yy++) {
					mpassTotal += megeMatrixRes[xx][yy].best.resultValue;
				}
			}
		}
		mm.opType = opType;
		mm.baseLineTotal = rst1.getPassTotal(opType);
		mm.baseTotal = typeTotal;
		mm.matrixTotal = mpassTotal;
		mm.listTotal = lpassTotal;
		mm.singleTotal = spassTotal;
		
		for (int xx=0;xx<megePassSet.length;xx++) {
			if (megeMatrixRes != null) {
				for (int yy=0;yy<VegML.getPredictionTypeCount();yy++) {
					megeMatrixRes[xx][yy].setDone(noExclude);
					mm.megeMatrix[xx][yy] = megeMatrixRes[xx][yy].best.mergeValue;
					if (mm.megeMatrix[xx][yy] != 0) mm.megeMap[xx] = true;
					// save value Map
					if (megeMatrixRes[xx][yy].valueWeightMatrix != null && !megeMatrixRes[xx][yy].valueWeightMatrix.isEmpty()) {
						if (mm.valueMatrixWMatrix == null) mm.valueMatrixWMatrix = new HashMap[VegML.getPredictionTypeCount()][VegML.getPredictionTypeCount()];
						mm.valueMatrixWMatrix[xx][yy] = megeMatrixRes[xx][yy].valueWeightMatrix;					
					}
				}
			}
			// list
			if (megeListRes != null) {
				megeListRes[xx].setDone(noExclude);
				mm.megeList[xx] = megeListRes[xx].best.mergeValue;
				// save value Map
				if (megeListRes[xx].valueWeightMatrix != null && !megeListRes[xx].valueWeightMatrix.isEmpty()) {				
					if (mm.valueListWMatrix == null) mm.valueListWMatrix = new HashMap[VegML.getPredictionTypeCount()];
					mm.valueListWMatrix[xx] = megeListRes[xx].valueWeightMatrix;				
				}
			}
		}
		megeRes.setDone(noExclude);
		mm.megeValue = megeRes.best.mergeValue;
		if (megeRes.valueWeightMatrix != null && !megeRes.valueWeightMatrix.isEmpty()) {
			mm.valueSingleWMatrix = megeRes.valueWeightMatrix;
		}
		//mm.print();
		
		return mm;
	}
	
	//////////////////////////
	// run a test
	private static int runTest(VResultSet rst1, VResultSet rst2, List<List<Long []>> tuneValueSetsInt,
			double [][] megeMatrix, HashMap<Long, Double> valueWeightMatrix[][], 
			VResultSet [] megePassSet, // results per input2 predictionType
			VResultSet rsfm, // results for merge
			VResultSet rsf1, // results of input1 in merge
			VResultSet rsf2, // results of input2 in merge
			boolean noDetail // recored per value result info
			) {
		/*
		// Merege the data with the info
		List<List<List<ValProb>>> mergSetX = null;
		if (valueWeightMatrix != null) {
			// individual value weights
			mergSetX = VegUtil.mergeVPListSetsCopy(rst1.getResultsVpList(), rst2.getResultsVpList(), megeMatrix, valueWeightMatrix);						
		} else {
			// standard weight matrix
			mergSetX = VegUtil.mergeVPListSetsCopy(rst1.getResultsVpList(), rst2.getResultsVpList(), megeMatrix);		
		}*/
		
		// Compare with correct values 
		startResSet(megePassSet, rst1.getDataPlane());
		rsfm.reset();
		if (rsf1 != null) rsf1.reset();
		if (rsf2 != null) rsf2.reset();
		int passBase = compareIntValProbListsResults(tuneValueSetsInt,
													rst1.getResultsVpList(), rst2.getResultsVpList(), 
													rsfm, rsf1, rsf2, megePassSet, noDetail, megeMatrix, valueWeightMatrix);
		endResSet(megePassSet);
		
		return passBase;
	}
	
	// update for iteration of savce
	private static double updateMatrix(MergeResultSet set, int fullResultValue, int resultValue, double mergeValue, boolean recordOnly, boolean noExclude) {
		if (set.done) return set.best.mergeValue;
		
		MergeResult res = null;
		if (mergeValue < 0) {
			mergeValue = set.lastMergeValue;
		}
		// record new value
		if (resultValue >= 0 && mergeValue >= 0) {
			res = new MergeResult(resultValue, mergeValue);
			set.add(res);
		}
		if (recordOnly) return mergeValue;
		if (set.last == null) {
			res = set.best;
		} 
		
		
		// check if done
		if (set.left.resultValue == 0 && set.right.resultValue == 0) {
			return set.setDone(noExclude);
		}
		if ((set.left.mergeValue == 0 || set.right.mergeValue == 0) && set.left.resultValue == set.right.resultValue) {
			return set.setDone(noExclude);
		}
		if ((set.left.mergeValue == 100 || set.right.mergeValue == 100) && set.left.resultValue == set.right.resultValue) {
			return set.setDone(noExclude);
		}	
		if (set.left.resultValue == set.right.resultValue && set.left.resultValue == resultValue) {
			return set.setDone(noExclude);
		}
		
		boolean deb = false;					
		if (deb) {	
			System.out.println("      "+set.getLable()+ "" 
					+ "["+String.format("%10s", String.format("%.6f", set.left.mergeValue))+"]["+String.format("%6d", set.left.resultValue)+"] "
					+ "["+String.format("%10s", String.format("%.6f", set.right.mergeValue))+"]["+String.format("%6d", set.right.resultValue)+"] "
					+ "    res["+String.format("%10s", String.format("%.6f", res.mergeValue))+"]["+String.format("%6d", res.resultValue)+"] " + mergeValue);
		}
		
		// set for next run
		boolean goLeft = true;		
		// for all situations determine direction
		if (set.last == null) {
			// First after transition
			if (set.left.resultValue < set.right.resultValue) {
				if (deb) System.out.print("            ch[<]: V["+String.format("%6d", set.left.resultValue)+"]--["+String.format("%6d", set.right.resultValue)+"]");
				goLeft = false;
				res = set.left;
			} else {
				if (deb) System.out.print("            ch[-]: V["+String.format("%6d", set.left.resultValue)+"]--["+String.format("%6d", set.right.resultValue)+"]");
				res = set.right;
			}
		} else {
			// standard pass
			if (set.left.resultValue == res.resultValue) {
				if (deb) System.out.print("            ch[1=]: V["+String.format("%6d", set.left.resultValue)+"]-["+String.format("%6d", res.resultValue)+"]-["+String.format("%6d", set.right.resultValue)+"]");
				goLeft = false;
			} else if (set.right.resultValue == res.resultValue) {
				if (deb) System.out.print("            ch[2=]: V["+String.format("%6d", set.left.resultValue)+"]-["+String.format("%6d", res.resultValue)+"]-["+String.format("%6d", set.right.resultValue)+"]");			
			} else if (set.left.resultValue < res.resultValue && res.resultValue < set.right.resultValue ) {
				if (deb) System.out.print("            ch[<<]: V["+String.format("%6d", set.left.resultValue)+"]-["+String.format("%6d", res.resultValue)+"]-["+String.format("%6d", set.right.resultValue)+"]");
				goLeft = false;
			} else if (set.left.resultValue > res.resultValue && res.resultValue > set.right.resultValue) {
				if (deb) System.out.print("            ch[>>]: V["+String.format("%6d", set.left.resultValue)+"]-["+String.format("%6d", res.resultValue)+"]-["+String.format("%6d", set.right.resultValue)+"]");
			} else if (set.left.resultValue > res.resultValue && res.resultValue < set.right.resultValue) {
				if (deb) System.out.print("            ch[><]: V["+String.format("%6d", set.left.resultValue)+"]-["+String.format("%6d", res.resultValue)+"]-["+String.format("%6d", set.right.resultValue)+"]");
				if (set.left.resultValue < set.right.resultValue) goLeft = false;
			} else if (set.left.resultValue < res.resultValue && res.resultValue > set.right.resultValue) {
				if (deb) System.out.print("            ch[<>]: V["+String.format("%6d", set.left.resultValue)+"]-["+String.format("%6d", res.resultValue)+"]-["+String.format("%6d", set.right.resultValue)+"]");
				if (set.left.resultValue < set.right.resultValue) goLeft = false;
			} else {
				if (deb) System.out.print("            ch[??]: V["+String.format("%6d", set.left.resultValue)+"]-["+String.format("%6d", res.resultValue)+"]-["+String.format("%6d", set.right.resultValue)+"]");
			}	
		}		
 
		double newMergeValue = 0;
		if (goLeft) {
			newMergeValue = (set.left.mergeValue + res.mergeValue) / 2;
			set.right = res;
			
			if ((newMergeValue-set.left.mergeValue) <= TMIN_INC) {
				if (deb) System.out.println(" "+set.getLable()+ " ["+newMergeValue+"] DONE L");
				return set.setDone(noExclude);
			}
			if (deb) System.out.println(" LEFT => " + newMergeValue);
		} else {
			newMergeValue = (set.right.mergeValue + res.mergeValue) / 2;
			set.left = res;
			
			if ((set.right.mergeValue-newMergeValue) <= TMIN_INC) {
				if (deb) System.out.println(" "+set.getLable()+ " ["+newMergeValue+"] DONE R");
				return set.setDone(noExclude);
			}
			if (deb) System.out.println(" RIGHT => " + newMergeValue);
		}
		set.last = res;
		set.lastMergeValue = newMergeValue;
		return newMergeValue;
	}
	
	private static void checkValues(String modeName, VDataPlane dp1, VResultSet rst1, VResultSet rst2, List<List<Long[]>> tuneValueSetsInt,
			int total, int typeBaseLine,
			PredictionType opType, MergeResultSet mSet, MergeResultSet mSetList [], MergeResultSet mSetMatrix[][], 
			List<Long> dataValList, double [] dataValMergeWeightSet, VResultSet [] megePassSet,
			double [][] megeMatrix, HashMap<Long, Double> valueWeightMatrix[][]) {			
		
		//////////////////////////
		// SETUP - independent
		// Run Merge test baseline with optimized value
		VResultSet rsf1 = new VResultSet(dp1);
		VResultSet rsf2 = new VResultSet(dp1);
		VResultSet rsfm = new VResultSet(dp1);
		
		VResultSet [] megePassSetBaseLine = new VResultSet[VegML.getPredictionTypeCount()];

		runTest(rst1, rst2, tuneValueSetsInt, megeMatrix, null, megePassSetBaseLine, rsfm, rsf1, rsf2, false);
		int singleTypeBaseLine = rsfm.getPassTotal(opType); // specific to op
		
		double perc = ((double)singleTypeBaseLine / (double)total) * 100;
		String tag = "     ";
		if (typeBaseLine < singleTypeBaseLine) tag = "+"+String.format("%-4d", (singleTypeBaseLine-typeBaseLine));
		else if (typeBaseLine > singleTypeBaseLine) tag = "-"+String.format("%-4d", (typeBaseLine-singleTypeBaseLine));
		System.out.println(" "+modeName+"_V-WORK BASE PASS["+fmtPercent(perc)+"]["+singleTypeBaseLine+"]"+tag);
		// show each inputs details
		//rsf1.printValueStats();
		//rsf2.printValueStats();
		
		List<ResultValueStat> rsl = rsf2.getValueFscoreSortedReverse();
		dataValList.clear();
		for (ResultValueStat rs:rsl) dataValList.add(rs.value);
		
		// start by setting all values in valueWeightList with empty lists
		if (mSet != null) {
			mSet.setupValueWeight(dataValList);
			for (int i=0;i<valueWeightMatrix.length;i++) {
				for (int z=0;z<valueWeightMatrix[0].length;z++) valueWeightMatrix[i][z] = mSet.valueWeightMatrix; 
			}
		} else if (mSetList != null) {
			for (int xx=0;xx<mSetList.length;xx++) {
				mSetList[xx].setupValueWeight(dataValList);
				for (int h=0;h<valueWeightMatrix[xx].length;h++) valueWeightMatrix[xx][h] = mSetList[xx].valueWeightMatrix;
			}
		} else if (mSetMatrix != null) {
			for (int xx=0;xx<mSetMatrix.length;xx++) {				
				for (int yy=0;yy<mSetMatrix[0].length;yy++) {
					mSetMatrix[xx][yy].setupValueWeight(dataValList);
					valueWeightMatrix[xx][yy] = mSetMatrix[xx][yy].valueWeightMatrix;
				}
			}
		}
		
		// TEST ITERATION
		for (int vvp=0;vvp<dataValList.size();vvp++) {
			long curValue = dataValList.get(vvp);
			// set data value
			if (mSet != null) {
				mSet.valueCurPos = vvp;
				mSet.valueCur = curValue;
			} else if (mSetList != null) {
				for (int xx=0;xx<mSetList.length;xx++) {
					mSetList[xx].valueCurPos = vvp;
					mSetList[xx].valueCur = curValue;
				}
			} else if (mSetMatrix != null) {
				for (int xx=0;xx<mSetMatrix.length;xx++) {				
					for (int yy=0;yy<mSetMatrix[0].length;yy++) {
						mSetMatrix[xx][yy].valueCurPos = vvp;
						mSetMatrix[xx][yy].valueCur = curValue;
					}
				}
			}
			
			String vStr = dp1.getString(curValue);
			System.out.print(" "+modeName+"_V-WORK["+String.format("%-6s", vStr)+"] ");
			
			int noChangeCnt = 0, changeCnt = 0;
			for (int mvp=0;mvp<dataValMergeWeightSet.length;mvp++) {
				double curMergeVal = dataValMergeWeightSet[mvp];
				if (mSet != null) {
					// set merge value
					mSet.valueMergeValueCurPos = mvp;				
					mSet.valueMergeValueCur = curMergeVal;				
					mSet.valueWeightMatrix.clear();
					mSet.valueWeightMatrix.put(mSet.valueCur, mSet.valueMergeValueCur);
				} else if (mSetList != null) {
					for (int xx=0;xx<mSetList.length;xx++) {
						mSetList[xx].valueMergeValueCurPos = mvp;				
						mSetList[xx].valueMergeValueCur = curMergeVal;				
						mSetList[xx].valueWeightMatrix.clear();
						mSetList[xx].valueWeightMatrix.put(mSetList[xx].valueCur, mSetList[xx].valueMergeValueCur);
					}
				} else if (mSetMatrix != null) {
					for (int xx=0;xx<mSetMatrix.length;xx++) {				
						for (int yy=0;yy<mSetMatrix[0].length;yy++) {
							mSetMatrix[xx][yy].valueMergeValueCurPos = mvp;				
							mSetMatrix[xx][yy].valueMergeValueCur = curMergeVal;				
							mSetMatrix[xx][yy].valueWeightMatrix.clear();
							mSetMatrix[xx][yy].valueWeightMatrix.put(mSetMatrix[xx][yy].valueCur, mSetMatrix[xx][yy].valueMergeValueCur);
						}
					}
				}	
				
				// run test
				runTest(rst1, rst2, tuneValueSetsInt, megeMatrix, valueWeightMatrix, megePassSet, rsfm, rsf1, rsf2, true);
				int passTypeBase = rsfm.getPassTotal(opType);
																
				tag = "     ";
				if (singleTypeBaseLine < passTypeBase) {
					tag = "+"+String.format("%-4d", (passTypeBase-singleTypeBaseLine));
					System.out.print("["+String.format("%.3f", curMergeVal)+"]"+tag+" ");
				} else if (singleTypeBaseLine > passTypeBase) {
					tag = "-"+String.format("%-4d", (singleTypeBaseLine-passTypeBase));
				}
				
				// cut-out if nothing
				if (singleTypeBaseLine == passTypeBase) noChangeCnt++;
				else changeCnt++;
				if (changeCnt == 0 && noChangeCnt > 5) break; // nothing to change here
										
				// record result
				if (mSet != null) {
					MergeResult mr = new MergeResult(passTypeBase, mSet.valueMergeValueCur);
					mSet.valueWeightList.get(mSet.valueCur).add(mr);
				} else if (mSetList != null) {
					for (int xx=0;xx<mSetList.length;xx++) {
						int resultValue = megePassSet[xx].getPassTotal(opType);
						MergeResult mr = new MergeResult(resultValue, mSetList[xx].valueMergeValueCur);
						mSetList[xx].valueWeightList.get(mSetList[xx].valueCur).add(mr);
					}
				} else if (mSetMatrix != null) {
					for (int xx=0;xx<mSetMatrix.length;xx++) {				
						for (int yy=0;yy<mSetMatrix[0].length;yy++) {
							int resultValue = megePassSet[xx].getPassTotal(VegML.getPredictionTypeEnum(yy));
							MergeResult mr = new MergeResult(resultValue, mSetMatrix[xx][yy].valueMergeValueCur);
							mSetMatrix[xx][yy].valueWeightList.get(mSetMatrix[xx][yy].valueCur).add(mr);
						}
					}
				}	
			}
			System.out.println("");
		}
		
		// get best scores
		if (mSet != null) {
			mSet.endValuePhase(dp1, singleTypeBaseLine);
		} else if (mSetList != null) {
			for (int xx=0;xx<mSetList.length;xx++) {
				int baseLine = megePassSetBaseLine[xx].getPassTotal(opType);
				mSetList[xx].endValuePhase(dp1, baseLine);
			}
		} else if (mSetMatrix != null) {
			for (int xx=0;xx<mSetMatrix.length;xx++) {				
				for (int yy=0;yy<mSetMatrix[0].length;yy++) {
					int baseLine = megePassSetBaseLine[xx].getPassTotal(VegML.getPredictionTypeEnum(yy));
					mSetMatrix[xx][yy].endValuePhase(dp1, baseLine);
				}
			}
		}	
		
		// test together... 
		runTest(rst1, rst2, tuneValueSetsInt, megeMatrix, valueWeightMatrix, megePassSet, rsfm, rsf1, rsf2, true);
		int passTypeBase = rsfm.getPassTotal(opType);
		
		perc = ((double)passTypeBase / (double)total) * 100;
		tag = "     ";
		if (typeBaseLine < passTypeBase) tag = "+"+String.format("%-4d", (passTypeBase-typeBaseLine));
		else if (typeBaseLine > passTypeBase) tag = "-"+String.format("%-4d", (typeBaseLine-passTypeBase));
		System.out.println(" "+modeName+"_V-work INDEPENDENT DONE["+fmtPercent(perc)+"]["+passTypeBase+" of "+total+"]"+tag);

		
		// SETUP - dependent
		// check all that improved, order best of each
		// make dependent test set ordered with best overall set then follow with each tuning after
		// FIXME
		
		// SETUP - binary search dependent for final
		// FIXME
		
		// done		
		System.out.println(" "+modeName+"_V-DONE base["+singleTypeBaseLine+"]");
		System.out.println("");
		// clear matrix
		for (int i=0;i<valueWeightMatrix.length;i++) {
			for (int z=0;z<valueWeightMatrix[0].length;z++) valueWeightMatrix[i][z] = null; 
		}
	}

	
	// for each prediction type
	// do merge and get assessment
	public static int compareIntValProbListsResults(List<List<Long []>> valueSets1, 
													List<List<List<ValProb>>> inputv1, List<List<List<ValProb>>> inputv2, 
													VResultSet rsfm, VResultSet rsf1, VResultSet rsf2, VResultSet [] megePassMatrix,
													boolean noDetail, double probMatrix[][], HashMap<Long, Double> valueWeightMatrix[][]) {
		int passTotal = 0;	
		List<ValProb> freeVpList = new ArrayList<>();
		List<ValProb> retVpList = new ArrayList<>();
		Long cval [] = new Long[1];
		
		for (int i=0;i<valueSets1.size();i++) {
			List<Long[]> vl1 = valueSets1.get(i); // correct answers			
			List<List<ValProb>> inv1 = inputv1.get(i);
			List<List<ValProb>> inv2 = inputv2.get(i);
			
			// for each token, address the list
			for (int xi=0;xi<vl1.size();xi++) {
				// merge them
				if (freeVpList.size() < 300) freeVpList.addAll(retVpList);
				retVpList.clear();
				List<ValProb> vxList = mergeVPListOnlyCopyAvg(inv1.get(xi), inv2.get(xi), probMatrix, valueWeightMatrix, retVpList, freeVpList);
				
				ValProb vpx1 = null;
				PredictionType pt1 = PredictionType.Default;
				if (inv1.get(xi).size() > 0) {
					vpx1 = inv1.get(xi).get(0); // value in 
					pt1 = vpx1.type;
				} 				
				
				ValProb vpx2 = null;
				PredictionType pt2 = PredictionType.Default;
				if (inv2.get(xi).size() > 0) {
					vpx2 = inv2.get(xi).get(0); // value in 2
					pt2 = vpx2.type;
				} 
				long mvVal = 0;
				if (vxList.size() > 0) {
					mvVal = vxList.get(0).value;    // merged value
				}
				
				// for ptype of first
				cval[0] = vl1.get(xi)[0];	// FIXME list
				VResultSet cr = megePassMatrix[pt1.ordinal()];
				boolean pass = (cval[0] == mvVal);
				if (mvVal == 0) pass = false;
				if (pass) passTotal++;
				
				cr.addResponse(cval, mvVal, pass, pt2, null, noDetail); // mixed with respect to input2
				
				// merged
				rsfm.addResponse(cval, mvVal, pass, pt1, null, noDetail);
				
				// results keyed for full set
				if (rsf1 != null) {
					if (vpx1 != null) rsf1.addResponse(cval, vpx1.value, vpx1.value == cval[0], pt1, null);
					else rsf1.addResponse(cval, 0, false, pt1, null, noDetail);				
				}
				if (rsf2 != null) {
					if (vpx2 != null) rsf2.addResponse(cval, vpx1.value, vpx2.value == cval[0], pt2, null);	
					else rsf2.addResponse(cval, 0, false, pt2, null, noDetail);
				}
			}
		}
	
		return passTotal;
	}
	// with merge matrix
	static List<ValProb> mergeVPListOnlyCopyAvg(List<ValProb> vpListBase, List<ValProb> vpList, 
										double probMatrix[][], HashMap<Long, Double> valueWeightMatrix[][], 
										List<ValProb> retVpList, List<ValProb> freeVpList) { 
		List<ValProb> nl = retVpList;
			
		// get merge value
		double baseWeigth = 0;
		HashMap<Long, Double> valueWeight = null;
		if (probMatrix != null) {
			PredictionType pt1 = PredictionType.Default;
			if (vpListBase.size() > 0) pt1 = vpListBase.get(0).type;
			PredictionType pt2 = PredictionType.Default;
			if (vpList.size() > 0) pt2 = vpList.get(0).type;
			baseWeigth = probMatrix[pt1.ordinal()][pt2.ordinal()];
			if (valueWeightMatrix != null) {
				valueWeight = valueWeightMatrix[pt1.ordinal()][pt2.ordinal()];
			}
		}
		// add in the base
		for (int i=0;i<vpListBase.size();i++) {
			ValProb vp = vpListBase.get(i); 
			Double vw = null;
			if (valueWeight != null) vw = valueWeight.get(vp.value);
			if (vw != null && vw >= 100) continue; // don't add			
			else if (vw == null && baseWeigth >= 100) continue; // don't add
			
			if (freeVpList.size() > 0) vp = vp.copy(freeVpList.remove(freeVpList.size()-1));
			else vp = vp.copy();
			nl.add(vp);
		}
		// merge in the list
		for (int i=0;i<vpList.size();i++) { 
			ValProb vp = vpList.get(i);
			Double vw = null;
			if (valueWeight != null) vw = valueWeight.get(vp.value);
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
				ValProb vx = null;
				if (freeVpList.size() > 0) vx = vp.copy(freeVpList.remove(freeVpList.size()-1));
				else vx = vp.copy();
				vx.probability = vx.probability*baseWeigth;
				if (vw != null && vw < 100) vx.probability = vx.probability*vw;
				vx.counter = 1;
				nl.add(vx);
			}
		}
		// Average the values (based on the number here)
		for (int xi=0;xi<nl.size();xi++) {
			ValProb vpx = nl.get(xi);
			vpx.probability = vpx.probability / vpx.counter;
		}	
		Collections.sort(nl, VegUtil.VpSort);
		return nl;
	}	
	static void fillArray(double arry[][], double val) {
		for (int u=0;u<arry[0].length;u++) {
			Arrays.fill(arry[u], val);
		}
	}
	static void startResSet(VResultSet [] rset, VDataPlane dp) {
		for (int u=0;u<rset.length;u++) {
			if (rset[u] == null) rset[u] = new VResultSet(dp);
			else rset[u].reset();
			rset[u].start();
		}
	}
	static void endResSet(VResultSet [] rset) {
		for (int u=0;u<rset.length;u++) {
			rset[u].end();
		}
	}	
	static String fmtPercent(double val) {
		return String.format("%6s", String.format("%.2f", val)) + "%";
	}
	static String perc(double val) {
		return String.format("%.2f", val) + "%";
	}


	/*

	///////////////////////////////////////////////////////////////////////////////////////////
	// Thread Pool
	private static final int TOTAL_THREADS = 6;
	private static int threadcount = TOTAL_THREADS;
	static private List<Thread> theadList = null;
	static private boolean thruning = false;
	static class workItem {
		int cmd;
		Object item;
		List<Object> outList;
	}
	static private List<workItem> workItemList = null;

	// work item
	static class test extends workItem {
		public DataPlane dp;
		public VDataSets dss;
		public double mergeValue;
	}

	static ResultSet runTestPredictFull(DataPlane dp, List<Object> set, double mergeValue, List<VDataSets> dssl) {
		set.clear();
		for (int xx=0;xx<dssl.size();xx++) {
			test tp = new test();
			// setup
			tp.dp = dp;
			tp.mergeValue = mergeValue;
			tp.dss = dssl.get(xx);
			set.add(tp);
		}
		List<Object> rl = processSet(set, 2);
		// merge results
		ResultSet rst = null;
		for (Object r:rl) {
			ResultSet rt = (ResultSet)r;
			if (rst == null) rst = rt;
			else rst.add(rt);
		}
		return rst;
	}
	// 
	// do something
	// move complete items from inList to outList
	//
	static private void processWorkItem(workItem wi) {
		if (wi.item == null) return;
		Object out = null;
		
		if (wi.cmd == 2) {
			// process predict
			test tp = (test)wi;
			out = VegTest.testSets(tp.dp.getVegML(), tp.dp.getDimensionTag(), tp.dp.getTag(), tp.dss);
		} else {
			out = wi.item;
			// FIXME process this predictionSet
		}
			
		synchronized (wi.outList) {
			wi.outList.add(out);
		}	
	}

	
	// process this set with the pool
	static public List<Object> processSet(List<Object> set, int cmd) {	
		if (theadList == null) {
			loadThreads(threadcount);
		}
		
		//System.out.println("processList["+total+"] " + sess.getContext());
		int total = set.size();
		List<Object> listComplete = new ArrayList<>();

		// add each to work items
		for (int i=0;i<set.size();i++) {
			Object x = set.get(i);
			addWorkItem(cmd, (workItem)x, listComplete, i == (set.size()-1));
		}

		// wait for complete
		while (listComplete.size() < total) {
			// check if more time
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) { }
		}
		return listComplete;
	}
	
	// add some work
	static private void addWorkItem(int cmd, workItem wi, List<Object> outList, boolean signal) {
		wi.cmd = cmd;
		wi.outList = outList;
		wi.item = wi;
		synchronized (workItemList) {
			workItemList.add(wi);
			if (signal) workItemList.notifyAll();
			//if (workItemList.size() == 1) workItemList.notifyAll();
		}
	}
	static private void loadThreads(int count) {
		theadList = new ArrayList<>();
		workItemList = new ArrayList<>();
		thruning = true;
		for (int i=0;i<count;i++) addThread();
		//System.out.println("STARTED Threads["+count+"] ");
	}
	static private void endThreads() {
		if (!thruning) return;
		synchronized (workItemList) {
			if (workItemList == null) return;
			thruning = false;
			workItemList.notifyAll();
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) { }
		theadList = null;
		workItemList = new ArrayList<>();
		//System.out.println("STARTED Threads["+count+"] ");
	}
	
	
	// add async processing...
	static private Thread addThread() {
		// add thread / timer to process this
		Thread thread = new Thread("Thread:rss") {
			public void run() {
				// would need to have work list... with all params in the global context
				while (thruning) {
					workItem wi = getNextWorkItem();
					if (wi != null) {
						processWorkItem(wi);
					} else {
						synchronized (workItemList) {
						try {
							workItemList.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						}
					}
				}
			}
		};
		thread.start();
		synchronized (theadList) {
			theadList.add(thread);
		}		
		return thread;
	}	
	static private workItem getNextWorkItem() {
		synchronized (workItemList) {
			if (workItemList.size() > 0) return workItemList.remove(0);
			return null;
		}	
	}
	*/
	
}
