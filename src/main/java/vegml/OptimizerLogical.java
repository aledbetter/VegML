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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vegml.VDRetainSet.DsRetainVal;
import vegml.VegML.NSWeightBase;
import vegml.VegML.PredictionType;
import vegml.ValProb;
import vegml.VegTest.TestMod;
import vegml.VegTest.TestModSet;
import vegml.Data.VDataSet;
import vegml.Data.VDataSets;


class OptimizerLogical {
	static boolean silent = false;
	static int maxStagesPerStep = 2;

	////////////////////////////////////////////////////////////////////////////////
	//
	// Check all dependencies into maps
	// 
	static int viaDependencyMapping(String fileName, String dimensionTag, String dataPlaneTag, 
			boolean update, NSWeightBase wType, PredictionType optType, boolean useReduction, boolean fullData,
			HashMap<Long, HashMap<Long, Integer>> [] crtWinDep, 		
			HashMap<Long, HashMap<Long, Integer>> [] crtDep,
			HashMap<Long, HashMap<Long, Integer>> [] crtWinOrDep,
			HashMap<Long, HashMap<Long, Integer>> [] loseDep,
			HashMap<Long, HashMap<Long, Integer>> [] loseOrDep,
			VDataSets dss, boolean ctlGroup) {		
		int maxSet = 20;
		int depLoseStage = 0;
		int depLoseOrStage = 1;
		int depCrtStage = 3;
		int depAltStage = 2;
		VegML vML = VegML.load(fileName);
		if (vML == null) return 0;
		System.out.println("dependencyMapping["+fileName+"]");

		VDataPlane dp = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) {
			System.out.println("ERROR dataPlane["+dimensionTag+"/"+dataPlaneTag+"] not found");
			return 0;
		}
		return viaDependencyMapping(fileName, dp, update, wType, optType, useReduction, fullData,
									maxSet, depLoseStage, depLoseOrStage, depCrtStage, depAltStage,
									crtWinDep, crtDep, crtWinOrDep, loseDep, loseOrDep, dss, ctlGroup);	

	}
	@SuppressWarnings("unchecked")
	static int viaDependencyMapping(String fileName, VDataPlane dp, 
			boolean update, NSWeightBase wType, PredictionType optType, boolean useReduction, boolean fullData,
			int maxSet, int depLoseStage, int depLoseOrStage, int depCrtStage, int depAltStage,
			HashMap<Long, HashMap<Long, Integer>> [] crtWinDep, 		
			HashMap<Long, HashMap<Long, Integer>> [] crtDep,
			HashMap<Long, HashMap<Long, Integer>> [] crtWinOrDep,
			HashMap<Long, HashMap<Long, Integer>> [] loseDep,
			HashMap<Long, HashMap<Long, Integer>> [] loseOrDep,
			VDataSets dss, boolean ctlGroup) {				


		//fullData = true;
		// vote table
		if (crtDep == null) crtDep = new HashMap[dp.getNSCount()]; 		// required for correct win
		if (crtWinDep == null) crtWinDep = new HashMap[dp.getNSCount()]; 		// required for correct win and won
		if (crtWinOrDep == null) crtWinOrDep = new HashMap[dp.getNSCount()]; // contributes OR to correct win
		if (loseDep == null) loseDep = new HashMap[dp.getNSCount()];		// required to lose
		if (loseOrDep == null) loseOrDep = new HashMap[dp.getNSCount()];	// contributes to lose

		int cnt = mapDependencies(dp, update, fullData, optType, wType, useReduction, 
									maxSet, depLoseStage, depLoseOrStage, depCrtStage, depAltStage,
									crtWinDep, crtDep, crtWinOrDep, loseDep, loseOrDep, dss, ctlGroup);
		
		// save it
		dp.setCfgScratch("optMethod", "depMapDrop"); 
		// retain opt state / info
		if (optType == null) {
			dp.setCfgScratch("optType", PredictionType.All.ordinal());
			dp.setCfgScratch("optTuneType", PredictionType.All);
		} else {
			dp.setCfgScratch("optType", optType.ordinal());
			dp.setCfgScratch("optTuneType", optType);
		}
		dp.setCfgScratch("optFullData", fullData);
		String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm.ss").format(new Date());
		dp.setCfgScratch("optTime", timeStamp); // count
		dp.setCfgScratch("optTrainDataSize", dss.getTrainCount()); // count		
		dp.setCfgScratch("optInitFile", fileName); // count
		dp.setCfgScratch("optMethod", "valueDepend"); 
		dp.setCfgScratch("optReduction", useReduction); 
	//	dp.setCfgScratch("optDownWeight", downWeight); // this is a set..
		
		if (fileName != null) dp.getVegML().save(fileName.substring(0, fileName.length()-4)+"-dep.veg");
		
		return cnt;
	}
	
	private static int mapDependencies(VDataPlane dp, boolean update, boolean fullData, PredictionType optType, NSWeightBase wType, 
			boolean useReduction,
			int maxSet, int depLoseStage, int depLoseOrStage, int depCrtStage, int depAltStage,
			HashMap<Long, HashMap<Long, Integer>> [] crtWinDep, 		// required for correct win and won
			HashMap<Long, HashMap<Long, Integer>> [] crtDep,
			HashMap<Long, HashMap<Long, Integer>> [] crtWinOrDep,
			HashMap<Long, HashMap<Long, Integer>> [] loseDep,
			HashMap<Long, HashMap<Long, Integer>> [] loseOrDep,
			VDataSets dss, boolean ctlGroup) {		
		System.gc();
		
		dss.genVSets();
		VDataSets tuneDs = dss.getTuneDataSets();
		if (ctlGroup) tuneDs = dss.getTuneTestDataSets();
		VDataSets testDs = dss.getTestDataSets();
		VDataSets fullDs = tuneDs;
		if (fullData) {
			if (!ctlGroup) fullDs = dss.getTrainAndTuneDataSets();
			else fullDs = dss.getTrainAndTuneTestDataSets();
		}
		
	
		//////////////////////////////////
		// initial baseline AND retained resolve set
		if (!silent) System.out.print("GENERATING BASELINE size["+fullDs.size()+"] =>");
		dp.setCfgFramerArg(fullDs.getValLLV());		
		VResultSet crst = VegTest.testSetsDsRetain(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), fullDs);		
		List<List<DsRetainVal>> rsdSet = crst.getRetainedResolutionSet();
		VResultSet brst = crst;
		if (!silent) System.out.println(" RESULT["+String.format("%.2f", brst.passPercent)+"%] pass["+brst.passTotal + " of " + brst.total+"] Time["+brst.getDurration()+"]");		
	
		int basePassTotal = brst.getPassTotal(optType);
		int baseFailTotal = brst.getFailTotal(optType);		
		int curPassTotal = basePassTotal;
		
		int baseKnownPassTotal = brst.getPassTotal(PredictionType.NotUnknown);
		int baseUnknownPassTotal = brst.getPassTotal(PredictionType.AnyUnknown);
		int curKnownPassTotal = baseKnownPassTotal;
		int curUnknownPassTotal = baseUnknownPassTotal;
		
		if (!silent) System.out.println("");	
		if (!silent) System.out.println("BASELINE["+optType+"] ["+String.format("%.2f", brst.getPassPercent(optType))+"%] pass["+basePassTotal+ " / "+baseFailTotal+" of " + brst.getTotal(optType)+"]");		
		
		// TEST test2
		int basePassTotal2 = 0;
		int baseKnownPassTotal2 = 0;
		int baseUnknownPassTotal2 = 0;
		
		if (false && !testDs.isEmpty()) {
			dp.setCfgFramerArg(testDs.getValLLV());
			if (!silent) System.out.print("BASELINE2 =>");
			VResultSet tst2 = VegTest.testSets(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), testDs);
			if (!silent) System.out.println(" RESULT["+String.format("%.2f", tst2.passPercent)+"%] pass["+tst2.passTotal + " of " + tst2.total+"] Time["+tst2.getDurration()+"]");		
			basePassTotal2 = tst2.passTotal;
			baseKnownPassTotal2 = tst2.getPassTotal(PredictionType.NotUnknown);
			baseUnknownPassTotal2 = tst2.getPassTotal(PredictionType.AnyUnknown);
			if (!silent) dp.print();
		}		
		dp.setCfgFramerArg(null);

		if (!silent) System.out.println("");	

		
		////////////////////////////////////////////////////
		// Threads and data subsets
		//  thread count based on size	
		int setCount = fullDs.size()/30;
		if (setCount > 16) setCount = 16;	
		/*
	phase 1:
		Dependency mapping 
		1. make a map from each resolution
			a. Check removing each value and see what makes it correct or wrong
				i. check removing other values 1 by one until correct
			b. Remove each one by one check for change
				i. TODO
			c. If Correct, Remove current correct values single 1-by-1 
				i. If goes wrong, req/req-or, if only one goes wrong req
				ii. If neither contribute
			d. If Wrong, Always remove the current 'winning' response largest probability
				i. If goes to correct, all prior are Evil/evil-or
				ii. if after change ?? Could be required could not-important 
					1) Check each
				iii. If was correct not-important
			e. Tag each Accum/Vector as (counter for each per value)
				i. Required
				ii. Required OR - this or another 
				iii. Contribute
				iv. Evil
				v. Evil OR - this or another
				vi. Not-important
			f. Tag each NumberSet the same
			g. Remove iteratively and check results
				i. The Ors first to resolve 1st tier dependencies 
					1) If only tagged as one type 
				ii. OR remove part, then re-scan
		 
		 *
	Phase 2:
		Cross dependency: after the first set have been cleared, find blocking dependencies that can be cleared
		1.pass 1 get current dependency set
		2 pass 2 identify dependencies that can be removed
			- second tier removal allows first tier removal
		
			- identify conditional pairs
				- will still win IF (crt & alt) dropped
				- will win if (xx && yy) dropped
				- no change if (xx && yy) dropped
		
		 *
		 */
		
		@SuppressWarnings("unchecked")
		HashMap<Long, HashMap<Long, Integer>>[] notCrtDep = new HashMap[dp.getNSCount()];
		@SuppressWarnings("unchecked")
		HashMap<Long, HashMap<Long, Integer>>[] notDep = new HashMap[dp.getNSCount()];
		@SuppressWarnings("unchecked")
		HashMap<Long, HashMap<Long, Integer>>[] altDep = new HashMap[dp.getNSCount()];

		for (int i=0;i<crtDep.length;i++) {
			crtDep[i] = new HashMap<>();
			crtWinOrDep[i] = new HashMap<>();
			loseDep[i] = new HashMap<>();
			loseOrDep[i] = new HashMap<>();
			crtWinDep[i] = new HashMap<>();			
			notCrtDep[i] = new HashMap<>();
			notDep[i] = new HashMap<>();
			altDep[i] = new HashMap<>();
		}		
		
		// adjustments
		double down = 0.90;
		double lordown = 0.95;
		double altdown = 0.99;
		double crtup = 1.01;  // UP correct that miss
	
		
//		test2Sets = null;  // not now
		
		int MAX_FAIL_COUNT = 4;
		
		int failOverCnt = 0;
		int stage = 0;
		int stateSteps = 0;
		
		boolean simple = false;
		if (wType != NSWeightBase.None && wType != NSWeightBase.Flat) simple = true;
		
		for (int set=0;set<maxSet;set++) {
			// clear
			for (int i=0;i<crtDep.length;i++) {
				crtDep[i].clear();
				crtWinOrDep[i].clear();
				loseDep[i].clear();
				loseOrDep[i].clear();
				crtWinDep[i].clear();				
				notCrtDep[i].clear();
				notDep[i].clear();
				altDep[i].clear();
			}


			if (!silent) System.out.println("");	
			if (!silent) System.out.println("STEP["+set+"] fail["+failOverCnt+"] stage["+stage+"] ");	
			

			// get dependencies
			if (!silent) System.out.print("  DEP     =>  ");
	// TODO take 2 sets AND a mergeValue for a merged dependency
			int tCnt = VDRetainSet.mapDependencies(dp, rsdSet, crtWinDep, crtDep, crtWinOrDep, loseDep, loseOrDep, altDep, notCrtDep, notDep);
				
			// resolve dependencies
			int depLoseOr = 0, depLose = 0, depNot = 0, depNotCrt = 0, depAlt = 0, depCrt = 0;
			int crtCnt = 0, crtOrCnt = 0, loseCnt = 0, loseOrCnt = 0, notCrtCnt = 0, notCnt = 0, altCnt = 0, crtWinCnt = 0;
			for (int i=0;i<crtDep.length;i++) {
				HashMap<Long, HashMap<Long, Integer>> crtWin = crtWinDep[i];
				crtWinCnt += crtWin.size();
				HashMap<Long, HashMap<Long, Integer>> crt = crtDep[i];
				crtCnt += crt.size();
				HashMap<Long, HashMap<Long, Integer>> crtOr = crtWinOrDep[i];
				crtOrCnt += crtOr.size();
				HashMap<Long, HashMap<Long, Integer>> lose = loseDep[i];
				loseCnt += lose.size();
				HashMap<Long, HashMap<Long, Integer>> loseOr = loseOrDep[i];
				loseOrCnt += loseOr.size();
				HashMap<Long, HashMap<Long, Integer>> alt = notDep[i];
				altCnt += alt.size();
				HashMap<Long, HashMap<Long, Integer>> notCrt = notCrtDep[i];
				notCrtCnt += notCrt.size();
				HashMap<Long, HashMap<Long, Integer>> not = notDep[i];
				notCnt += not.size();
	/*
	 * TODO:
	 * each has a count, these should be used to decide if the dependency should be kept
	 *  - if the cftDep == 1 and the lose == 5 > loose should win
	 *  			
	 */
								
				// lose must go first
				List<Long> rl = new ArrayList<>(lose.keySet());
				for (Long vid:rl) {
					HashMap<Long, Integer> lol = lose.get(vid);
					if (crtWin.get(vid) != null) {
						for (Long value:crtWin.get(vid).keySet()) lol.remove(value);
					}
					if (crt.get(vid) != null) {
						for (Long value:crt.get(vid).keySet()) lol.remove(value);
					}
					if (crtOr.get(vid) != null) {
						for (Long value:crtOr.get(vid).keySet()) lol.remove(value);
					}
					if (lol.keySet().size() == 0) lose.remove(vid);
				}
				depLose += lose.keySet().size();
				
				// check must go first
				rl = new ArrayList<>(loseOr.keySet());
				for (Long vid:rl) {
					HashMap<Long, Integer> lol = loseOr.get(vid);
					if (crtWin.get(vid) != null) {
						for (Long value:crtWin.get(vid).keySet()) lol.remove(value);
					}
					if (crt.get(vid) != null) {
						for (Long value:crt.get(vid).keySet()) lol.remove(value);
					}
					if (crtOr.get(vid) != null) {
						for (Long value:crtOr.get(vid).keySet()) lol.remove(value);
					}
					if (lol.keySet().size() == 0) loseOr.remove(vid);
				}
				depLoseOr += loseOr.keySet().size();
				
				// check must go first
				rl = new ArrayList<>(alt.keySet());
				for (Long vid:rl) {
					HashMap<Long, Integer> lol = alt.get(vid);
					if (crtWin.get(vid) != null) {
						for (Long value:crtWin.get(vid).keySet()) lol.remove(value);
					}
					if (crt.get(vid) != null) {
						for (Long value:crt.get(vid).keySet()) lol.remove(value);
					}
					if (crtOr.get(vid) != null) {
						for (Long value:crtOr.get(vid).keySet()) lol.remove(value);
					}
					if (loseOr.get(vid) != null) {
						for (Long value:loseOr.get(vid).keySet()) lol.remove(value);
					}
					if (lose.get(vid) != null) {
						for (Long value:lose.get(vid).keySet()) lol.remove(value);
					}
					if (lol.keySet().size() == 0) alt.remove(vid);
				}
				depAlt += alt.keySet().size();

				
				// lose must go first
				rl = new ArrayList<>(not.keySet());
				for (Long vid:rl) {
					HashMap<Long, Integer> lol = not.get(vid);
					if (crtWin.get(vid) != null) {
						for (Long value:crtWin.get(vid).keySet()) lol.remove(value);
					}
					if (crt.get(vid) != null) {
						for (Long value:crt.get(vid).keySet()) lol.remove(value);
					}
					if (crtOr.get(vid) != null) {
						for (Long value:crtOr.get(vid).keySet()) lol.remove(value);
					}
					if (loseOr.get(vid) != null) {
						for (Long value:loseOr.get(vid).keySet()) lol.remove(value);
					}
					if (lose.get(vid) != null) {
						for (Long value:lose.get(vid).keySet()) lol.remove(value);
					}
					if (notCrt.get(vid) != null) {
						for (Long value:notCrt.get(vid).keySet()) lol.remove(value);
					}
					if (lol.keySet().size() == 0) not.remove(vid);
				}
				depNot += not.keySet().size();
				
				// should win
				rl = new ArrayList<>(crt.keySet());
				for (Long vid:rl) {
					HashMap<Long, Integer> lol = crt.get(vid);
					if (alt.get(vid) != null) {
						for (Long value:alt.get(vid).keySet()) lol.remove(value);
					}
					if (lol.keySet().size() == 0) crt.remove(vid);
				}
				depCrt += crt.keySet().size();
			}

		/*
		 STEP[0] step[0] 
		  DEP   =>  COUNT[2118987] noMatch[1088]noInfo[25]notWinnable[269]
		    Total[1513953] crt[225896]crtOr[276350]lose[2664]loseOr[18538]other[31518] => depCrt[0]depLose[0] canWin[0]
		  CHECK   =>  pass[222485]     		
		 */
			if (!silent) System.out.println("   cnt    =>   crt["+crtCnt+"]crtOr["+crtOrCnt+"]lose["+loseCnt+"]loseOr["+loseOrCnt+"]alt["+altCnt+"]notCrt["+notCrtCnt+"]not["+notCnt+"]");
			if (!silent) System.out.println("   dep    =>   depLose["+depLose+"]depLoseOr["+depLoseOr+"]depAlt["+depAlt+"]depNotCrt["+depNotCrt+"]depNot["+depNot+"]depCrt["+depCrt+"]");
			
			if (!update) return curPassTotal;
			
			// DROP
			if (!silent) System.out.print("  REMOVE  =>  [");
			// kill the bad guys
			int dropCnt = 0, dropDPCnt = 0;
			if (depLose > 0 && depLoseStage >= 0 && stage >= depLoseStage) {
				// this is always good
				if (useReduction) {
					dropCnt += VDRetainSet.removeAllNumberSet(dp, rsdSet, loseDep);	
					dropDPCnt += dp.removeAllNSValues(loseDep);
				} else {
					dropCnt += VDRetainSet.weightAllNumberSet(dp, rsdSet, loseDep, down);
					dropDPCnt += dp.weightAllNSValues(loseDep, down);
				}
				if (!silent) System.out.print("L");
			}
			
			if (depLoseOr > 0 && depLoseOrStage >= 0 && stage >= depLoseOrStage) {
				// this is good if the probabilities are not balanced
				if (useReduction) {
					dropCnt += VDRetainSet.removeAllNumberSet(dp, rsdSet, loseOrDep);	
					dropDPCnt += dp.removeAllNSValues(loseOrDep);
				} else {
					dropCnt += VDRetainSet.weightAllNumberSet(dp, rsdSet, loseOrDep, lordown);
					dropDPCnt += dp.weightAllNSValues(loseOrDep, lordown);
				}
				if (!silent) System.out.print("O");
			}
			/*
			if (depNot > 0 && stage >= 4) {
				// this just reduces size
				if (useReduction) {
					dropCnt += DRetainSet.removeAllNumberSet(dp, rsdSet, notDep);	
					dropDPCnt += dp.removeAllNSValues(notDep);
				} else {
					dropCnt += DRetainSet.devalueAllNumberSet(dp, rsdSet, notDep, down);
					dropDPCnt += dp.devalueAllNumberSetValues(notDep, down);
				}
				if (!silent) System.out.print("N");
			}*/

			
			if (depCrt > 0 && depCrtStage >= 0 && stage >= depCrtStage) {
				dropCnt += VDRetainSet.weightAllNumberSet(dp, rsdSet, crtDep, crtup);
				dropDPCnt += dp.weightAllNSValues(crtDep, crtup);
				if (!silent) System.out.print("c");
			}
			if (depAlt > 0 && depAltStage >= 0 && stage >= depAltStage) {
				if (useReduction) {
					// this really doesn't work
				//	dropCnt += DRetainSet.removeAllNumberSet(dp, rsdSet, altDep);	
				//	dropDPCnt += dp.removeAllNSValues(altDep);
				} else {
					dropCnt += VDRetainSet.weightAllNumberSet(dp, rsdSet, altDep, altdown);
					dropDPCnt += dp.weightAllNSValues(altDep, altdown);
				}
				if (!silent) System.out.print("A");
			}		
		
			if (!silent) System.out.println("] change["+dropDPCnt+"]["+dropCnt+"]");
			
			/// CHECK
			TestModSet testList = new TestModSet();
			testList.add(dp, new TestMod(dp.getCfgNSWeightRaw()));	
			
			if (!silent) System.out.print("  CHECK   =>  ");
			List<VResultSet> rModList = VDRetainSet.testSetsModify(dp, testList, rsdSet);
			VResultSet res = rModList.get(0);
			int newCurPassTotal = res.getPassTotal(optType);
			int newCurKnownPassTotal = res.getPassTotal(PredictionType.NotUnknown);
			int newCurUnknownPassTotal = res.getPassTotal(PredictionType.AnyUnknown);
			
			if (!silent) System.out.print("chng["+res.changeCount+"]f["+res.getRDChangeToFail().size()+"]p["+res.getRDChangeToPass().size()+"] =>");
			// update set
			VDRetainSet.updateRetainSet(rsdSet, res);

			String tag = "     ";
			if (basePassTotal < newCurPassTotal) tag = "+"+(newCurPassTotal-basePassTotal);
			else if (basePassTotal > newCurPassTotal) tag = "-"+(basePassTotal-newCurPassTotal);		
			
			String ktag = "     ";
			if (baseKnownPassTotal < newCurKnownPassTotal) ktag = "+"+(newCurKnownPassTotal-baseKnownPassTotal);
			else if (baseKnownPassTotal > newCurKnownPassTotal) ktag = "-"+(newCurKnownPassTotal-newCurKnownPassTotal);		

			String utag = "     ";
			if (baseUnknownPassTotal < newCurUnknownPassTotal) utag = "+"+(newCurUnknownPassTotal-baseUnknownPassTotal);
			else if (baseUnknownPassTotal > newCurUnknownPassTotal) utag = "-"+(baseUnknownPassTotal-newCurUnknownPassTotal);		
			
			if (!silent) System.out.print("  passD["+newCurPassTotal+"]"+tag+"/"+ktag+"/"+utag);
			/*
			if (silent && tag.charAt(0) != ' ') {
				if (set == 0) System.out.print(" "+tag);
				else System.out.print("/"+tag);
			}*/

			// check stage changes
			boolean done = false;
			stateSteps++;
			String sup = "";
			if (res.changeCount == 0 || stateSteps > maxStagesPerStep) {
				stage++;
				stateSteps = 0;
			}
			
			// end with enough counts
			if ((res.getRDChangeToFail().size() > (res.getRDChangeToPass().size()+1) && newCurPassTotal < curPassTotal) 
					|| newCurPassTotal < curPassTotal
					|| (dropDPCnt == 0 && dropCnt == 0)
					|| (res.getRDChangeToFail().size() > res.getRDChangeToPass().size() && newCurPassTotal == curPassTotal)) {
				// FIXME need better exit
				failOverCnt++;
				sup += "   FAIL_CNT="+failOverCnt;
				// done
				if (failOverCnt >= MAX_FAIL_COUNT) done = true;
			} else {
				failOverCnt--;
				if (failOverCnt < 0) failOverCnt = 0;
			}
			curPassTotal = newCurPassTotal;
			curKnownPassTotal = newCurKnownPassTotal;
			curUnknownPassTotal = newCurUnknownPassTotal;
			
			// TEST test2
			if (false && !testDs.isEmpty() && !silent) {
				System.out.print(" =>");
				dp.setCfgFramerArg(testDs.getValLLV());
				VResultSet tst2 = VegTest.testSets(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), testDs, VegTest.SaveStats.SavePredict);
				dp.setCfgFramerArg(null);
				int newCurKnownPassTotal2 = tst2.getPassTotal(PredictionType.NotUnknown);
				int newCurUnknownPassTotal2 = tst2.getPassTotal(PredictionType.AnyUnknown);
				tag = "     ";
				if (basePassTotal2 < tst2.passTotal) tag = "+"+(tst2.passTotal-basePassTotal2);
				else if (basePassTotal2 > tst2.passTotal) tag = "-"+(basePassTotal2-tst2.passTotal);		
				
				ktag = "     ";
				if (baseKnownPassTotal2 < newCurKnownPassTotal2) ktag = "+"+(newCurKnownPassTotal2-baseKnownPassTotal2);
				else if (baseKnownPassTotal2 > newCurKnownPassTotal2) ktag = "-"+(newCurKnownPassTotal2-newCurKnownPassTotal2);		

				utag = "     ";
				if (baseUnknownPassTotal2 < newCurUnknownPassTotal2) utag = "+"+(newCurUnknownPassTotal2-baseUnknownPassTotal2);
				else if (baseUnknownPassTotal2 > newCurUnknownPassTotal2) utag = "-"+(baseUnknownPassTotal2-newCurUnknownPassTotal2);		

				System.out.println("  passT["+tst2.passTotal + "]"+tag+"/"+ktag+"/"+utag);		
				if (done) dp.print();
			} else {
				if (!silent) System.out.println("");					
			}
			if (done) break;
			if (!silent) if (!sup.isEmpty()) System.out.println(sup);
		}
		if (!silent) System.out.println("");	

		dp.setCfgFramerArg(null);		
		dp.setCfgScratch("optTuneDataSize", fullDs.size()); // count	
		
		//dp.print();
		if (!silent) System.out.println("STEP COMPLETE START["+basePassTotal +" => "+curPassTotal+"] ");	
		if (basePassTotal >= curPassTotal) return -1;
		return curPassTotal;
	}	

	
	
	
	////////////////////////////////////////////////////////////////////////////////
	//
	// optimize to exsting known good relationships
	// 
	static int optimizeVoteReduction(int step, String dimensionTag, String dataPlaneTag, String initName, 
			String pre, int window, NSWeightBase wType, PredictionType optType, boolean fullData, VDataSets dss) {		
		String fd = "";
		if (!fullData) fd ="p";
		String fn = pre+"-"+window+"-w"+wType.ordinal()+"-s"+fd+(step-1)+".veg";
		if (initName != null) fn = initName;
		VegML vML = VegML.load(fn);
		if (vML == null) return 0;
		System.out.println("OPTing step["+step+"] <== ["+fn+"]");

		VDataPlane dp = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return 0;
		fullData = true;
		//fullData = false;
		
		int topSetMin = -1;

		int mode = 0; // probability	
		//mode = 1; // frequency
		//mode = 2; // mixed
		boolean context = true; // true if remove context as well
		boolean singleList = false;
		if (wType == NSWeightBase.Flat) singleList = true;
		return nsValueVoteReduction(dp, initName, pre, fullData, optType, mode, context, singleList, topSetMin, wType, dss);
	}
	
	//
	// TODO: vote for and remove values instead of acumulators
	//
	private static int nsValueVoteReduction(VDataPlane dp, String initName, String pre, boolean fullData, PredictionType optType, int mode, 
			boolean context, boolean singleList, int topSetMin, NSWeightBase wType, VDataSets dss) {		
		System.gc();
		
		//////////////////////////////////
		// Setup data to tune with
		dss.genVSets();
		VDataSets tuneDs = dss.getTuneDataSets();
		VDataSets testDs = dss.getTestDataSets();
		VDataSets fullDs = tuneDs;
		if (fullData) fullDs = dss.getTrainAndTuneDataSets();
		
		dp.setCfgFramerArg(fullDs.getValLLV());
	
		//////////////////////////////////
		// initial baseline AND retained resolve set
		System.out.print("GENERATING BASELINE size["+fullDs.size()+"] =>");
		VResultSet crst = VegTest.testSetsDsRetain(dp.getVegML(), dp.getDimensionTag(), dp.getTag(), fullDs);		
		List<List<DsRetainVal>> rsdSet = crst.getRetainedResolutionSet();
		//System.out.print("..T[" + crst.getDurration()+"]");
		VResultSet brst = crst;
		//ResultSet brst = dp.getVegML().trainPredictSets(dp.getDimensionTag(), dp.getTag(), fulltuneSets, fulltuneValueSets, true);
		System.out.println(" RESULT["+String.format("%.2f", brst.passPercent)+"%] pass["+brst.passTotal + " of " + brst.total+"] Time["+brst.getDurration()+"]");		
	
		int basePassTotal = brst.getPassTotal(optType);
		int baseFailTotal = brst.getFailTotal(optType);
		
		int curBasePassTotal = basePassTotal;
		int curPassTotal = basePassTotal;
		int curFailTotal = baseFailTotal;
		int [] curPass = new int[VegML.getPredictionTypeCount() +1];
		
		System.out.println("");	
		System.out.println("BASELINE["+optType+"] ["+String.format("%.2f", brst.getPassPercent(optType))+"%] pass["+basePassTotal+ " / "+baseFailTotal+" of " + brst.getTotal(optType)+"]");		
		System.out.println("");	
		
		// vote table
		@SuppressWarnings("unchecked")
		HashMap<Long, Integer> [] nsVotes = new HashMap[dp.getNSCount()];
		@SuppressWarnings("unchecked")
		HashMap<Long, Integer> [] nsNoVotes = new HashMap[dp.getNSCount()];
		for (int i=0;i<nsVotes.length;i++) {
			nsVotes[i] = new HashMap<>();
			nsNoVotes[i] = new HashMap<>();
		}
		

		// mixed no context...
		//String pre = "votex";
		if (mode == 1) pre += "f";
		else if (mode == 0) pre += "p";
		else {
			pre += "m";
			mode = 2;
		}
		
		int cycles = 40; // FIXME based on windowsize
		if (singleList) cycles *= 2; // more cycles
		int min = 1;
		int maxMin = 5;
		int minMax = 3;
		
		System.out.println("CYCLEING["+cycles+"] mode["+mode+"] single["+singleList+"] context["+context+"] wt["+wType+"]");
	/*
	 * FIXME each cycle should start with fresh data .. or the results?	
	 *  - but not for all methods
	 *  
	 *  methods
	 *  1) keep only highest from each NS .. drop all others
	 *  2) drop only those found in matched that do not match
	 *  3) values vs accums(vectors) for all
	 *  
	 *  start with clean or start with already carved
	 */
		int smax = 8;
		int lastChange = 0;
		int startPass = 0;
		for (int set=0;set<(cycles-1);set++) {
			int max = cycles-set;
			for (int i=0;i<nsVotes.length;i++) {
				nsVotes[i].clear();
				nsNoVotes[i].clear();
			}

			//max = 1; // cut one each cycle
			System.out.println("STEP["+(set+2)+"] step["+set+"] max["+smax+"]min["+min+"]");

			
			// VOTE
			System.out.print("  VOTE    =>  ");
			int votes = VDRetainSet.testVoting(dp, rsdSet, nsVotes, nsNoVotes, mode, smax, min, singleList);
			System.out.print("votes["+votes+"]");
	
			// remove all nsVotes from 
			int nvotes = 0 ,invotes = 0;;
			for (int i=0;i<nsVotes.length;i++) {
				HashMap<Long, Integer> vo = nsVotes[i];
				HashMap<Long, Integer> no = nsNoVotes[i];
				invotes += no.keySet().size();
				for (Long id:vo.keySet()) no.remove(id);
				nvotes += no.keySet().size();
			}
			System.out.println("votes["+votes+"]no["+invotes+" -> "+nvotes+"]");

			
			// TEST REMOVE
			// FIXME if good.. finish
			
			// DROP
			System.out.print("  REMOVE  =>  ");
			// remove noVotes only
			int dropCnt = VDRetainSet.removeAllNSByVotesExclude(dp, rsdSet, nsNoVotes, context);
			int dpDropCnt = dp.removeAllNSByVotesExclude(nsNoVotes, context);
			// remove all not voted for
		//	int dropCnt = DataPlane.removeAllNSByVotes(dp, rsdSet, nsVotes, context);
		//	int dpDropCnt = dp.removeAllNSByVotes(nsVotes, context);
			System.out.println("change["+dpDropCnt+"]["+dropCnt+"]");
			
			
			/// CHECK
			TestModSet testList = new TestModSet();
			testList.clear();
			TestMod tst = new TestMod(dp.getCfgNSWeightRaw());
			//tst.addModMin(ns, brvs.value, minDropSet[h]); // value off (always retain prob here for now)
			//tst.setPredictionType(optType);
			testList.add(dp, tst);	
			
			System.out.print("  CHECK   =>  ");
			List<VResultSet> rModList = VDRetainSet.testSetsModify(dp, testList, rsdSet);
			VResultSet res = rModList.get(0);			
			String tag = "     ";
			if (basePassTotal < res.getPassTotal(optType)) tag = "+"+String.format("%-4d", (res.getPassTotal(optType)-basePassTotal));
			else if (basePassTotal > res.getPassTotal(optType)) tag = "-"+String.format("%-4d", (basePassTotal-res.getPassTotal(optType)));		
			System.out.println("pass["+res.getPassTotal(optType)+"]"+tag);
			if (set == 0) startPass = res.getPassTotal(optType);
						
			// retain opt state / info
			if (optType == null) {
				dp.setCfgScratch("optType", PredictionType.All.ordinal());
				dp.setCfgScratch("optTuneType", PredictionType.All);
			} else {
				dp.setCfgScratch("optType", optType.ordinal());
				dp.setCfgScratch("optTuneType", optType);
			}

			//dp.setCfgScratch("optNSWeightBase", wType.ordinal());
			dp.setCfgScratch("optFullData", fullData);
			String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm.ss").format(new Date());
			dp.setCfgScratch("optTime", timeStamp); // count
			dp.setCfgScratch("optTrainDataSize", dss.getTrainCount()); // count
			dp.setCfgScratch("optTuneDataSize", fullDs.size()); // count		
			dp.setCfgScratch("optInitFile", initName); // count
			dp.setCfgScratch("optMethod", "valueVote"); 
			dp.setCfgScratch("optVoteMax", smax); 
			dp.setCfgScratch("optVoteMin", min); 

			if (res.getPassTotal(optType) <= lastChange && dpDropCnt == 0) {
				if (minMax == smax && min == maxMin) {
					System.out.println("DONE");			
					String fd = "";
					if (!fullData) fd ="p";
					dp.getVegML().save(pre+"-"+dp.getCfgWindowSize()+"-w"+wType.ordinal()+"-s"+fd+(set+2)+".veg");
					break;
				}
				min++;
				if (min > maxMin) {
					min = maxMin;
					smax--;
					if (smax < minMax) {
						min++;
						smax = minMax;
					}
				}
			}

			if (res.getPassTotal(optType) == lastChange) {
				set--; // not file for this
			} else {
				String fd = "";
				if (!fullData) fd ="p";
				dp.getVegML().save(pre+"-"+dp.getCfgWindowSize()+"-w"+wType.ordinal()+"-s"+fd+(set+2)+".veg");
			}
			lastChange = res.getPassTotal(optType);
			if (min <= 0) break;
			if (max <= 0) break;
		}

		dp.setCfgFramerArg(null);	
		dp.print();

		System.out.println("STEP COMPLETE START["+basePassTotal +" => "+curPassTotal+"] ");			
		return curPassTotal;
	}	

	
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	// LOGICAL: reduce vectors and values that cannot be used due to the dataset definition
	//////////////////////////////////////////////////////////////////////////////////////////////
	// learn the dataSet info
	// clear what can not be, make filters for what is not allowed
	// this is a logical method -> move it?
	private static final int MAX_TOKEN = 20;
	private static final int MAX_TOKEN_MAP = 10;	// max individual tokens for tag
	static int reduceByDefinition(VDataPlane dp, PredictionType optType, HashMap<String, Integer> dropMap, boolean silent) {		

		int trs = dp.getTrainingFilterSize();
		if (trs > 0) return trs; 
		
		System.out.print(" DATA_LOGICAL["+dp.getDimensionTag()+"/"+dp.getTag()+"] w["+dp.getCfgWindowSize()+"]id["+dp.getCfgNSIdentityNumber()+"] => ");
			
		////////////////////////////////////////////////////
		// ##1
		// clear single vector values
		// if identity has only one value -> all but identity that yeild value can be removed: SBR/etc
		int idns = dp.getCfgNSIdentityNumber();
		HashMap<Long, Integer> valHash = new HashMap<>();
		HashMap<Long, List<Long>> valTokensHash = new HashMap<>();

		// get list of all inputTokens and all their value options
		List<ValProb> ivpList = dp.getIdentityFrequencyProbList();
		if (ivpList == null) {
			System.out.println(" => EMPTY");
			return 0;
		}
		
		int sglCnt = 0;
		for (int i=0;i<ivpList.size();i++) {
			ValProb vp = ivpList.get(i);
			if (vp.counter == 1) {
				// token that has only 1 value
				List<ValProb> vpList = dp.findCfgNSProbList(idns, vp.value);
				if (vpList == null || vpList.size() > 1) continue; // just in case
				
				//System.out.println(" just1["+vp.value+"]["+dp.getString(vp.value)+"]");

				// add value code
				Integer cnt = valHash.get(vpList.get(0).value);
				if (cnt == null) valHash.put(vpList.get(0).value, 1);
				else valHash.put(vpList.get(0).value, cnt+1);
				
				// hash all tokens that have a single value
				sglCnt++;
				// add token up to 10
				List<Long> vsl = valTokensHash.get(vpList.get(0).value);
				if (vsl == null) {
					vsl = new ArrayList<>();
					valTokensHash.put(vpList.get(0).value, vsl);
				}
				if (vsl.size() < MAX_TOKEN && !vsl.contains(vp.value)) vsl.add(vp.value);
			}
		}
		
		// remove values that were trained to an input token with more than one trained value
		for (int i=0;i<ivpList.size();i++) {
			ValProb vp = ivpList.get(i);
			if (vp.counter != 1) {
				List<ValProb> vpList = dp.findCfgNSProbList(idns, vp.value);
				if (vpList == null) continue; // just in case
				for (int x=0;x<vpList.size();x++) {				
					valHash.remove(vpList.get(x).value);
				}
			}
		}
		
		System.out.print("["+sglCnt+" of "+ivpList.size()+"]");
		if (!silent) System.out.print("  => ");
		else System.out.println("");
		
		// the set
		int changeTotal = 0;
		for (Long val:valHash.keySet()) {
			int cnt = valHash.get(val);

			// remove from all but identity
			if (cnt < MAX_TOKEN_MAP) {
				String sval = dp.getString(val);
				int chng = 0;
				if (trs <= 0) {
					chng = dp.removeAllNSValueExceptIdentity(val, true);
					// set filter on the dataPlane
					dp.addTrainingFilter(val, 1);
				}
				changeTotal += chng;
				if (!silent) System.out.print(" "+cnt+"["+sval+"]");
				if (dropMap != null) dropMap.put(sval, 0);
				// remove from dataPlane DAC - don't want as default
				dp.getAccumDefault().remove(val);
			}
		}
		System.out.println(" => change["+changeTotal+"]");

		return 1;
	}

	
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	// LOGICAL: reduce values masked by identity filter
	//////////////////////////////////////////////////////////////////////////////////////////////
	//
	// remove all vectors and vector values that are covered by the idenity filter
	//
	// FIXME this could generate a training filter to improve performance.. like the other
	static int reduceByIdentity(VDataPlane dp, VDataSets trainds, boolean keepFull, boolean silent) {		
		if (!dp.isCfgIdentityOnly() || dp.isSolid() || dp.getNSCount() < 2) return 0;
		
		if (!silent) System.out.print(" DATA_IDENT["+dp.getDimensionTag()+"/"+dp.getTag()+"] w["+dp.getCfgWindowSize()+"]id["+dp.getCfgNSIdentityNumber()+"] => ");
			
		////////////////////////////////////////////////////
		// get all identities, get list of those with only 1 value
	
		// get list of all inputTokens and all their value options
		List<ValProb> ivpList = dp.getIdentityFrequencyProbList();
		if (ivpList == null) {
			if (!silent) System.out.println(" => EMPTY");
			return 0;
		}
		// get just the single values
		Set<Long> idents = new HashSet<>();
		for (ValProb vp:ivpList) {
			if (vp.counter == 1) idents.add(vp.value);
		}
		if (!silent) System.out.print("single["+idents.size()+" of "+ivpList.size()+"]");

		ivpList = null;
		
		int idv = dp.getCfgNSVectNumIdentityNumber();
		int fullv = dp.getCfgNSVectNumFullNumber();
		

		// not to get all vectors that match these identities and remove
		int changeTotal = 0, tryTotal = 0;
		VFrame frame = new VFrame();
		frame.init(dp);			
		VContext ctx = new VContext(dp.getVegML());
		for (int set=0;set<trainds.size();set++) {
			VDataSet dataSet = trainds.get(set);
			ArrayList<Long> valueOut = new ArrayList<>(dataSet.size());
			// set value size if they didn't prior 
			for (int i=0;i<dataSet.size();i++) {
				Long dvid = dataSet.getDataV(i);
				if (dvid == null) continue;	// FIXME bug?
				
				if (!idents.contains(dvid)) continue;
				Long [] valueId = dataSet.getValueVD(i);
				
				if (!dp.getFramer().makeFrameSetup(ctx, dp, frame, null, false, valueOut, trainds, set, i)) {
					valueOut.add(valueId[0]);
					continue;
				}	
				// gen vectors
				dp.genVectors(frame);
				tryTotal++;
				// for each non-context and not-identity remove
				for (int vns=0;vns < frame.getVectSpace().length;vns++) {
					if (vns == idv) continue;
					if (keepFull && vns == fullv) continue;
					long vid = frame.getVectSpace()[vns];
					if (vid == -2 || vid == -1 || vid == 0) continue;	
					int ns = dp.getMapVectorNumberSet(vns);
					if (dp.isCfgNSTurnedOff(ns)) continue;				
					boolean isCtx = dp.isCfgNSContext(ns);
					if (isCtx) continue;
					
					// remove this one
					Accum ac = dp.getAccumulator(ns, vid);
					if (ac == null) continue;
					if (ac.remove(dvid) > 0) changeTotal++;
				}				
				valueOut.add(valueId[0]);
			}
		}
		// empty left 
		int rcnt = dp.removeAllEmptyAccum();

		if (!silent) System.out.println(" => change["+changeTotal+"]vect["+rcnt+"]");
		return 1;
	}


}
