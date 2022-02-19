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
import java.util.HashMap;
import java.util.List;

import vegml.Data.VFileUtil;
import vegml.Data.VDataSetDescriptor.DSDataType;
import vegml.Optimize.OptMethod;
import vegml.Data.VDataSets;
import vegml.VegML.NSWeightBase;
import vegml.VegML.PredictionType;
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

class Optimiser2 extends Optimiser {
	private int NS_MIN_POSITIVE = 0;
	private int processCycleCount = 1;
	
	Optimiser2() {
		super();
	}
	Optimiser2(VegML vML, String initName, PredictionType optType, boolean useReduction, OptMethod optm) {	
		super(vML.getCfgDefaultDTag(), vML.getCfgDefaultDPTag(), initName, null, null, false, optType, useReduction, optm);
	}
	Optimiser2(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag, boolean merge,
			PredictionType optType, boolean useReduction, OptMethod optm) {		
		super(vML.getCfgDefaultDTag(), vML.getCfgDefaultDPTag(), initName, tuneDimensionTag, tuneDataPlaneTag, merge, optType, useReduction, optm);
	}
	Optimiser2(String dimensionTag, String dataPlaneTag, String initName, PredictionType optType, boolean useReduction, OptMethod optm) {
		super(dimensionTag, dataPlaneTag, initName, null, null, false, optType, useReduction, optm);
	}
	Optimiser2(String dimensionTag, String dataPlaneTag, String initName, String tuneDimensionTag, String tuneDataPlaneTag, boolean merge,
			PredictionType optType, boolean useReduction, OptMethod optm) {
		super(dimensionTag, dataPlaneTag, initName, tuneDimensionTag, tuneDataPlaneTag, merge, optType, useReduction, optm);
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
		
		// get drop min config
		NS_MIN_POSITIVE = this.optm.getCfgInt("betterThanDrop");
		processCycleCount = this.optm.getCfgInt("processCycleCount");
		
		
		
		int step = 1;
		save(step);
		
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
		curPassCtlBaseBest = curPassCtlBest = 0;
		
		// save here. let the step = window size
		step++;
		save(step);
		int noAddCnt = 0;
		List<Integer> excludeNsSet = new ArrayList<>();
		
		// do it until it don't work no more
		while (true) {
			int snscnt = tunedp.getNSCount();
			//System.out.println("");
			if (this.isMerge()) System.out.print("STEP["+(step+1)+"]["+dp.getIDString()+"]["+tunedp.getIDString()+"]ns["+snscnt+"]  =>  ");
			else System.out.print("STEP["+(step+1)+"]["+tunedp.getIDString()+"]ns["+snscnt+"]  =>  ");
			
			boolean newStart = false;
			if (nsAddSets.size() == 0) newStart = true;

			// prepare next numberSets
			List<Integer> remNsList = getNextSetResults();
			if (remNsList == null) {
				System.out.println(" DONE ");
				break; // if no more space
			}
			
			// if moved to new super-cycle
			if (newStart) {
				cycleNSCount = snscnt;
				cyclePassTotal = curPassTotal;
				if (resCurCtl != null) {
					cyclePassTotalCtl = resCurCtl.getPassTotal(optType);
					if (cyclePassTotalCtl > curPassCtlBest && tunedp.getCfgWindowSize() > 2) {
						curPassCtlBest = cyclePassTotalCtl;
						curPassCtlBaseBest = curPassTotal;
					}
				}
			}
			
			// keep this complete
			curNSInDp.clear();
			for (int nsi=0;nsi<tunedp.getNSCount();nsi++) {
				curNSInDp.add(nsi); // part of the program
			}
			
			// make sure all are on
			tunedp.setCfgNSTurnedOff(false);

			// update mergeValue for merge before
			if (this.isMerge()) {
				getMergeValue(this.dp, remNsList, true);
			} 

			step++;
			
			System.out.print("  NS["+tunedp.getNSCount()+"/"+tunedp.getNSTurnedOnCount()+"]v["+tunedp.getVectorCount()+"] =>");
			
			//////////////////////////////////////
			// Start Cycle
			//////////////////////////////////////
			optm.processCycleStart(this, remNsList);
	
			//////////////////////////////////////
			// Check for drops
			//////////////////////////////////////
			checkDrop(excludeNsSet);
	
			
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
						
			// add count to each ns for processing
			for (int nsi=0;nsi<tunedp.getNSCount();nsi++) {
				if (tunedp.isCfgNSTurnedOff(nsi)) continue;
				int nid = tunedp.getNSHashId(nsi);
				Integer cc = processNSCounts.get(nid);
				if (cc == null) processNSCounts.put(nid, 1);
				else processNSCounts.put(nid, cc+1);
			}
			

			//vML.print(true);
			
			curPassTotal = resCur.getPassTotal(optType);
			
			// Check done at end of cycle
			if (nsAddSets.size() == 0) {
				//////////////////////////////////////
				// End Cycle
				//////////////////////////////////////
				// show for sub-cycle as well
				finalEval();
				
				// save the final
				save(step);
				
				// save solid and json
				saveJSON();
				
				int curPassMax = curPassTotal+optm.getCfgInt("betterThanMax");
				// did it do bad?
				if (cyclePassTotal >= curPassMax) {
						// no new numbersets added for window increase
						noAddCnt++;
						System.out.println("   NO-ADD["+noAddCnt+"] TUNE["+(curPassMax-curPassMax)+"] ns["+cycleNSCount+" / "+tunedp.getNSCount()+"] win["+cycleWindow+"] pass["+cyclePassTotal+" >= "+curPassMax+"]");	
						if (curPassTotal > bestWindowPass) {
							bestWindow = tunedp.getCfgWindowSize();
							bestWindowPass = curPassTotal;
						}
						if (noAddCnt > optm.getCfgInt("maxNoAddCnt")) break;	
				 } else if (ctlGroup && curPassCtlBest > resCurCtl.getPassTotal(optType)) {
					noAddCnt++;
					System.out.println("   NO-ADD["+noAddCnt+"] CTL["+(curPassCtlBest-resCurCtl.getPassTotal(optType))+"] max["+optm.getCfgInt("maxNoAddCnt")+"] ns["+cycleNSCount+" / "+tunedp.getNSCount()+"] win["+cycleWindow+"] "
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
		//tunedp.print(true);
		
		// if unknown, remove idenitity
		if (optType == PredictionType.AnyUnknown || optType == PredictionType.PredictUnknown) {
			VegML vjML = VegML.load(bestVeg);
			VDataPlane jdp = vjML.getDataPlane(this.dimensionTag, this.dataPlaneTag);
			jdp.removeCfgNS(jdp.getCfgNSIdentityNumber());
			vjML.saveSilent(getFileName());
		}
		return bestVeg;
	}


	
	//
	// check and order the numberSets
	//
	private boolean checkDrop(List<Integer> excludeNsSet) {

		tunedp.setCfgNSTurnedOff(false);

		////////////////////////////////////////
		// check for empty and remove them
		boolean removed = false;
		for (int x=0;x<tunedp.getNSCount();x++) {
			if (excludeNsSet.contains(x)) continue; // not new set
			int acCnt = tunedp.getNSAccumulatorCount(x);
			if (acCnt <= 0) {
				if (!removed) System.out.println("");
				System.out.println("       TURN OFF NS["+x+"]["+tunedp.getNSFormatString(x)+"]  v["+tunedp.getVectorCount(x)+"]");
				// remove numberSets
				removeNS(x, null);	
				tunedp.setCfgNSTurnedOff(false);
				x = 0; // start over
				removed = true;
			}
		}
		if (removed) {
			System.out.print("      RM-NS-DONE["+tunedp.getNSTurnedOnCount()+"] =>");
		}
		tunedp.setCfgNSTurnedOff(false);
		if (tunedp.getNSTurnedOnCount() < 1) {
			System.out.println(" NO-NS => DONE");	
			return true;
		}
			
		////////////////////////////////////////
		// get updated stats
		// test with each current NS removed to see if any could be dropped		
		TestModSet nstests = new TestModSet();
		getDropTests(nstests);		
		List<VResultSet> resCurDropList = MLThreadUtil.runTestPredictModify(dp, new ArrayList<>(), nstests, threadTuneDs, false, false);		
		// get baseLine	from set
		resCur = resCurDropList.remove(0);
		curPassTotal = resCur.getPassTotal(optType);
		nstests.remove(0);
		System.out.print(" pass["+curPassTotal+"]");
		
		if (nstests.size() > 1) {
			int min = -1;
			
			////////////////////////////////////////
			// check drop list for better result
			boolean dropTest = true;
			while (dropTest) {
				int drp_ns = -1, drpPassTotal = 0, drpCnt = 0;
				dropTest = false;
				for (int x=0;x<resCurDropList.size();x++) {	
					int ns = (int)nstests.get(tunedp, x).userTag;
					if (ns == -1 || ns == tunedp.getCfgNSFullNumber() || ns == tunedp.getCfgNSIdentityNumber()) continue;
					
					VResultSet rs = resCurDropList.get(x);
					// must account for more than 1 point
					int diff = resCur.getPassTotal(optType) - rs.getPassTotal(optType);
					if (min < 0 || diff < min) min = diff;
						
					if (rs.getPassTotal(optType) >= (resCur.getPassTotal(optType) - NS_MIN_POSITIVE)) {
						drpCnt++;
						if (drpPassTotal == 0 || drpPassTotal > rs.getPassTotal(optType)) {
							drpPassTotal = rs.getPassTotal(optType);
							drp_ns = ns;
						}
					} 
				}
				resCurDropList = null;
				System.gc();
				
				if (drp_ns >= 0) {
					System.out.println("");	
					System.out.print("  DROP_BETTER["+String.format("%3d", drp_ns)+"]["+tunedp.getNSFormatString(drp_ns)+"]ns["+String.format("%2d", tunedp.getNSCount())+"] ["+drpPassTotal+" >= "+resCur.getPassTotal(optType)+"] =>");		
					// remove the ns
					removeNS(drp_ns, null);						
					// update and re-run tests
					getDropTests(nstests);
					resCurDropList = MLThreadUtil.runTestPredictModify(dp, new ArrayList<>(), nstests, threadTuneDs, false, false);		
					// get baseLine	from set
					resCur = resCurDropList.remove(0);
					nstests.remove(0);	
					curPassTotal = resCur.getPassTotal(optType);
					System.out.print(" ns["+String.format("%2d", tunedp.getNSTurnedOnCount()) +"]v["+String.format("%7d", tunedp.getVectorCount())+"]  =>  pass["+curPassTotal+"]");						
					dropTest = true;
					min = -1;
				}
			}
			System.out.print(" min["+min+"]");	
		}
		if (this.ctlGroup) {
			// re-asses control
			nstests.clear();
			TestMod t = new TestMod(tunedp.getCfgNSWeightsBase());	
			nstests.add(tunedp, t.completeNSW());
			
			List<VResultSet> rsl = MLThreadUtil.runTestPredictModify(dp, new ArrayList<>(), nstests, threadTuneCtlDs, false, false);		
			resCurCtl = rsl.get(0);
		}
	
		System.out.println(" DONE");	
		return true;
	}
	
	//
	// make the drop tests
	// FIRST test will be complete current
	//
	public void getDropTests(TestModSet nstests) {
		nstests.clear();
		tunedp.setCfgNSTurnedOff(false);
		
		// add baseline for current complete: MUST BE FIRST for mod to work
		TestMod t = new TestMod(tunedp.getCfgNSWeightsBase());	
		t.userTag = -1;
		nstests.add(tunedp, t.completeNSW());
		
		// add full set without for each (aside from full / identity)
		for (int x=0;x<tunedp.getNSCount();x++) {					
			if (x == tunedp.getCfgNSFullNumber() || x == tunedp.getCfgNSIdentityNumber()) continue;
			t = new TestMod(tunedp.getCfgNSWeightsBase());	
			t.nsWeights[x] = 0; // drop this one
			t.userTag = x;
			nstests.add(tunedp, t.completeNSW());
		}	
	}


}
