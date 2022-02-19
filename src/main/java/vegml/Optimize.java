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

import vegml.VegTest.TestModSet;
import vegml.Data.VDataSetDescriptor.DSDataType;


class Optimize {
	
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Optimize method to perform in Optimizer
	// 
	// Overload methods to prep / start cycle / end cycle / process
	////////////////////////////////////////////////////////////////////////////////////////////////
	static public class OptMethod {
		private String name;
		private HashMap<String, Object> config;
		
		OptMethod(Optimiser mz) {
			config = new HashMap<>();
			reset(mz);
		}
		void setName(String name) {
			this.name = name;			
		}

		String getCfgString(String name) {
			return (String)config.get(name);
		}
		int getCfgInt(String name) {
			Integer i = (Integer)config.get(name);
			if (i == null) return -1;
			return i;
		}
		boolean getCfgBool(String name) {
			Boolean b = (Boolean)config.get(name);
			if (b == null) return false;
			return b;
		}
		double getCfgDouble(String name) {
			Double i = (Double)config.get(name);
			if (i == null) return 0;
			return i;
		}
		int [] getCfgIntArray(String name) {
			int [] i = (int [])config.get(name);
			if (i == null) return null;
			return i;
		}
		Object getCfg(String name) {
			return config.get(name);
		}
		// Set
		void setCfg(String name, String value) {
			config.put(name, value);
		}
		void setCfg(String name, int value) {
			config.put(name, value);
		}
		void setCfg(String name, boolean value) {
			config.put(name, value);
		}
		void setCfg(String name, double value) {
			config.put(name, value);
		}
		void setCfg(String name, Object value) {
			config.put(name, value);
		}

		// add config on-top of base config
		void setConfig(HashMap<String, Object> cfg) {
			for (String k:cfg.keySet()) {
				Object v = cfg.get(k);
				if (v == null) config.remove(k);
				else config.put(k, v);
			}
		}
		
		
		String getName() {
			return name;
		}
		
		//
		// prep-processing on each numberSet
		//
		void processNSPrep(Optimiser mz, List<Integer> curNS, List<Integer> newNS, List<VResultSet> rList) {	
		}
		
		//
		// complete-processing on each numberSet
		//
		List<VResultSet> processNS(Optimiser mz, int ns, TestModSet nstests) {
			return null;
		}

		//
		// Cycle Start processing
		//
		void processCycleStart(Optimiser mz, List<Integer> newNsSet) {	
		}
		
		//
		// Cycle End processing
		//
		void processCycleEnd(Optimiser mz) {	
		}
		
		//
		// set/reset this
		//
		void reset(Optimiser mz) {
			config.clear();
			// generic
			setCfg("useReduction", true);
			setCfg("downWeight", 0.8);
			setCfg("maxNoAddCnt", 3);	
			
			setCfg("exportJSON", false);	
			setCfg("exportSolid", false);	

			// higher bar than current?
			setCfg("useBetterThan", true);
			setCfg("betterThanMax", 9);
			setCfg("betterThanPossiblePrep", 7);
			setCfg("betterThanPossibleRaw", 7);
			setCfg("fullAlways", true);			
			setCfg("betterThanDrop", 6);
			setCfg("processCycleCount", 2);
			
			// these changes for char
			if (mz.getTuneDP() != null && mz.getTuneDP().getCfgInputDataType() == DSDataType.Char) {
				setCfg("useBetterThan", false);
				setCfg("betterThanMax", 1);
				setCfg("betterThanPossiblePrep", 1);
				setCfg("betterThanPossibleRaw", 1);
				setCfg("maxNoAddCnt", 4);
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Optimization With Carve and Depdendency
	// configurable for what and where
	//
	////////////////////////////////////////////////////////////////////////////////////////////////
	static public class OptMethodCarveDep extends OptMethod{	
		OptMethodCarveDep(Optimiser mz) {
			super(mz);
		}
		
		//
		// set/reset this
		//
		@Override
		void reset(Optimiser mz) {
			super.reset(mz);
			
			// generic
			setCfg("downWeight", 0.8);
			setCfg("maxNoAddCnt", 2);
			setCfg("fullAlways", true);
			
			// higher bar than current?
			setCfg("useBetterThan", true);
			setCfg("betterThanMax", 9);
			setCfg("betterThanPossiblePrep", 7);
			setCfg("betterThanPossibleRaw", 7);
			
			// Specific
			setCfg("CARVE_MAX_NS", 4); 

			setCfg("applyCarve", true);
			setCfg("applyPrepCarve", true);
			setCfg("applyDependency", true);
			setCfg("applyPrepVectMap", false);
			
			setCfg("carveAloneWithIdentity", false);
			setCfg("carveAloneWithFull", false);
			setCfg("carveWeight", 0.8);
			setCfg("noZero", false);
			setCfg("noPositive", false);
			
			// along carve
			setCfg("carveReduction", true);
			setCfg("carveNoBackingUp", false);
			//int [] minDropSet = {0, 1};
			int [] minDropSet = {0, 1, 2};
			setCfg("minDropSet", minDropSet);
			
			// standard carve
			setCfg("carveReductionCombo", true);
			setCfg("carveNoBackingUpCombo", false);
			int [] minDropSetCombo = {0, 1};
			setCfg("minDropSetCombo", minDropSetCombo);

			// dependency
			setCfg("useReductionDep", true);
			setCfg("fullDataDep", false);
			
			setCfg("noDepIfCarveNeg", 6);	// window >= 6
			setCfg("noDepIfCarveSame", 8);	// window >= 8
			
			
			// these changes for char
			if (mz.getTuneDP() != null && mz.getTuneDP().getCfgInputDataType() == DSDataType.Char) {
				setCfg("noDepIfCarveNeg", 100);
				setCfg("noDepIfCarveSame", 100);
				setCfg("useBetterThan", false);
				setCfg("betterThanMax", 1);
				setCfg("betterThanPossiblePrep", 1);
				setCfg("betterThanPossibleRaw", 1);
				setCfg("maxNoAddCnt", 4);
			}
		}
		
		//
		// prep-processing on each numberSet
		//
		@Override
		void processNSPrep(Optimiser mz, List<Integer> curNS, List<Integer> newNS, List<VResultSet> rList) {	
			if (getCfgBool("applyPrepVectMap")) {				
				// map and reduce all vectors
				mz.getTestMap().mapRawNSVectors(mz.getTuneDP(), rList);
				System.gc();
			}
			if (getCfgBool("applyPrepCarve")) {				
				// carve alone before assessment		
				// turn all off..
				System.out.print("  PREP[carve]["+newNS.size()+"] =");
				mz.getTuneDP().setCfgNSTurnedOff(true);
				
				// carve alone; with identity, full, or both
				if (getCfgBool("carveAloneWithIdentity")) mz.getTuneDP().setCfgNSTurnedOff(mz.getTuneDP().getCfgNSIdentityNumber(), false);	
				if (getCfgBool("carveAloneWithFull")) mz.getTuneDP().setCfgNSTurnedOff(mz.getTuneDP().getCfgNSFullNumber(), false);	
				
				for (int x=0;x<newNS.size();x++) {
					int ns = newNS.get(x);
					if (ns == mz.getTuneDP().getCfgNSFullNumber()) continue;
					// turn this one ont
					mz.getTuneDP().setCfgNSTurnedOff(ns, false);	

					// carve it alone
					if (false && mz.isMerge()) {
						// if merge
						// FIXME what do here? alone or with merge?
						OptimizerStatistical.carveStepVectorValues(mz.getDP(), 0, mz.isFullData(), mz.getPType(), 1000, getCfgIntArray("minDropSet"), null, 
								getCfgBool("noZero"), getCfgBool("noPositive"), 
								getCfgBool("carveReduction"), getCfgBool("carveNoBackingUp"), getCfgDouble("carveWeight"),
								false, new HashMap<>(), mz.getTuneDP(), false, null, mz.dss, mz.ctlGroup, true);
						
					} else {
						OptimizerStatistical.carveStepVectorValues(mz.getTuneDP(), 0, mz.isFullData(), mz.getPType(), 1000, getCfgIntArray("minDropSet"), null, 
								getCfgBool("noZero"), getCfgBool("noPositive"), 
								getCfgBool("carveReduction"), getCfgBool("carveNoBackingUp"), getCfgDouble("carveWeight"),
								false, new HashMap<>(), null, false, null, mz.dss, mz.ctlGroup, true);
					}
					// turn back off
					mz.getTuneDP().setCfgNSTurnedOff(ns, true);								
					System.out.print("=");
				}
				mz.getTuneDP().setCfgNSTurnedOff(true);
				// turn back on
				mz.getTuneDP().setCfgNSTurnedOff(curNS, false);
				System.gc();
				System.out.print("> DONE["+newNS.size()+"]");
			}

		}

			
		//
		// complete-processing on each numberSet
		//
		@Override
		List<VResultSet> processNS(Optimiser mz, int ns, TestModSet nstests) {

			List<VResultSet> rsl = null;			
			if (getCfgBool("applyCarve") && mz.getTuneDP().getNSCount() > 1) {	
				List<Integer> nsSetFirst = null;
				// setup carve drop map
				HashMap<String, Integer> dropMap = new HashMap<>();
				// setup carve drop map
				dropMap.clear();
				for (Integer nid:mz.getProcessNSCounts().keySet()) {
					Integer cnt = mz.getProcessNSCounts().get(nid);
					if (cnt == null) continue;
					int nsi = mz.getTuneDP().getNSForHashId(nid);	
					if (nsi < 0) continue;
					if (cnt >= getCfgInt("CARVE_MAX_NS")) {
						// add ignore NS to map
						dropMap.put(""+nsi, 0);
					}
				}

				// carve one pass							
				if (mz.isMerge()) {
					//mz.getTuneDP().print(true);
					// merge
					OptimizerStatistical.carveStepVectorValues(mz.getTuneDP(), 0, mz.isFullData(), mz.getPType(), 1000, getCfgIntArray("minDropSetCombo"), null, 
							getCfgBool("noZero"), getCfgBool("noPositive"), 
							getCfgBool("carveReductionCombo"), getCfgBool("carveNoBackingUpCombo"),
							getCfgDouble("carveWeight"), false, dropMap, mz.getDP(), true, nsSetFirst, mz.dss, mz.ctlGroup, true);	
					//mz.getTuneDP().print(true);

				} else {
					OptimizerStatistical.carveStepVectorValues(mz.getTuneDP(), 0, mz.isFullData(), mz.getPType(), 1000, getCfgIntArray("minDropSetCombo"), null, 
							getCfgBool("noZero"), getCfgBool("noPositive"), 
							getCfgBool("carveReductionCombo"), getCfgBool("carveNoBackingUpCombo"),
							getCfgDouble("carveWeight"), false, dropMap, null, false, nsSetFirst, mz.dss, mz.ctlGroup, true);		
					
				}
			}
			System.out.print(">");
							
			// dependency method
			if (getCfgBool("applyDependency")) {				
				int curPassTotalAfterCarve = mz.getCurTotal();
				boolean carvePass = true;
				if (getCfgBool("applyCarve")) {
					// assess before depend, then decide if keep depend	
					rsl = MLThreadUtil.runTestPredictModify(mz.getDP(), new ArrayList<>(), nstests, mz.threadTuneDs, true, true);
					curPassTotalAfterCarve = rsl.get(0).getPassTotal(mz.getPType());	
					
					if (getCfgBool("fullAlways") && ns == mz.getTuneDP().getCfgNSFullNumber()) {
					} else if (curPassTotalAfterCarve <= mz.getCurTotal() && mz.getTuneDP().getVectorCount(ns) < mz.getMinVectorCount()) {
						carvePass = false; // carve didn't pass
						System.out.print("  NOv["+String.format("%4d", (curPassTotalAfterCarve-mz.getCurTotal()))+"]");				

					} else if (curPassTotalAfterCarve == mz.getCurTotal() && (mz.getTuneDP().getCfgWindowSize() >= getCfgInt("noDepIfCarveSame"))) {
					} else if (curPassTotalAfterCarve <= mz.getCurTotal() && (mz.getTuneDP().getCfgWindowSize() >= getCfgInt("noDepIfCarveNeg"))) {
						carvePass = false; // carve didn't pass
						System.out.print("   NO["+String.format("%4d", (curPassTotalAfterCarve-mz.getCurTotal()))+"]");				
					}
				}
				if (carvePass) {
					// do dependency
					System.out.print("  DEP");						
					OptimizerLogical.silent = true;
					OptimizerLogical.maxStagesPerStep = 0;
					int maxSet = 4;
					int depLoseStage = 0;
					int depLoseOrStage = 1;
					int depAltStage = 2;	
					int depCrtStage = -1; // .. don't use
					if (getCfgBool("applyDependencyMin")) {	
						maxSet = 3;
						depAltStage = -1;
					}
					mz.saveTemp();
					int ncurPassTotal = 0;
					if (mz.isMerge()) {
						// if merge
// FIXME
						ncurPassTotal = OptimizerLogical.viaDependencyMapping(null, mz.getTuneDP(), true, mz.getTuneDP().getCfgNSWeight(), mz.getPType(), 
								getCfgBool("useReductionDep"), getCfgBool("fullDataDep"),
								maxSet, depLoseStage, depLoseOrStage, depCrtStage, depAltStage, null, null, null, null, null, mz.dss, mz.ctlGroup);		
					} else {
						ncurPassTotal = OptimizerLogical.viaDependencyMapping(null, mz.getTuneDP(), true, mz.getTuneDP().getCfgNSWeight(), mz.getPType(), 
											getCfgBool("useReductionDep"), getCfgBool("fullDataDep"),
											maxSet, depLoseStage, depLoseOrStage, depCrtStage, depAltStage, null, null, null, null, null, mz.dss, mz.ctlGroup);		
					}
					// if not better revert
					//int off = ncurPassTotal-curPassTotal;
					int off = ncurPassTotal-curPassTotalAfterCarve;
					if (ncurPassTotal < 0 || off <= 0) {
						// NOPE role-back
						if (curPassTotalAfterCarve > mz.getCurTotal()) {
							mz.loadTemp(); // only if not going to roll back after return
						}
						System.out.print("[NOPE]");
					} else {
						System.out.print("["+String.format("%4d", off)+"]");
						rsl = null;
					}
					mz.delTemp();
				} 
			} else {
				System.out.print("  ");
			}
			return rsl;
		}

		//
		// Cycle Start processing
		//
		@Override
		void processCycleStart(Optimiser mz, List<Integer> newNsSet) {	
			// this is a set of hacks that are in test.. making NS inclusion more restrictive as the window gets wider)


		}
		
		//
		// Cycle complete processing
		//
		@Override
		void processCycleEnd(Optimiser mz) {
			// dependency at the end of cycle
			if (getCfgBool("applyDependency")) {				
				OptimizerLogical.silent = true;
				OptimizerLogical.maxStagesPerStep = 0;
				int maxSet = 4;
				int depLoseStage = 0;
				int depLoseOrStage = 1;
				int depAltStage = 2;	
				int depCrtStage = -1; // .. don't use
				if (getCfgBool("applyDependencyMin")) {	
					maxSet = 3;
					depAltStage = -1;
				}
				mz.saveTemp();
				int ncurPassTotal = 0;
				
				if (mz.isMerge()) {
					// if merge
//FIXME			
					ncurPassTotal = OptimizerLogical.viaDependencyMapping(null, mz.getTuneDP(), true, mz.getTuneDP().getCfgNSWeight(), mz.getPType(), 
										getCfgBool("useReductionDep"), getCfgBool("fullDataDep"),
										maxSet, depLoseStage, depLoseOrStage, depCrtStage, depAltStage, null, null, null, null, null, mz.dss, mz.ctlGroup);			

				} else {
					ncurPassTotal = OptimizerLogical.viaDependencyMapping(null, mz.getTuneDP(), true, mz.getTuneDP().getCfgNSWeight(), mz.getPType(), 
										getCfgBool("useReductionDep"), getCfgBool("fullDataDep"),
										maxSet, depLoseStage, depLoseOrStage, depCrtStage, depAltStage, null, null, null, null, null, mz.dss, mz.ctlGroup);			
				}
				// if not better revert
				int off = ncurPassTotal-mz.getCurTotal();
				if (ncurPassTotal < 0 || off <= 0) {
					// NOPE role-back
					mz.loadTemp();
				} else {
					System.out.println(" SET COMPLETE DEP["+String.format("%4d", off)+"] ");
					// remap these
					mz.getTestMap().mapCurrentAndSet(mz, true);
				}
				mz.delTemp();
			}	
		}
	}
	
	
	static public class OptMethodCarveDep2 extends OptMethodCarveDep {	
		OptMethodCarveDep2(Optimiser mz) {
			super(mz);
		}
		//
		// prep-processing on each numberSet
		//
		@Override
		void processNSPrep(Optimiser mz, List<Integer> curNS, List<Integer> newNS, List<VResultSet> rList) {	
			if (getCfgBool("applyPrepCarve")) {				
				// carve alone before assessment		
				// turn all off..
				System.out.print("  PREP[carve]["+newNS.size()+"] =");
				mz.getTuneDP().setCfgNSTurnedOff(true);
				
				// carve alone; with identity, full, or both
				if (getCfgBool("carveAloneWithIdentity")) mz.getTuneDP().setCfgNSTurnedOff(mz.getTuneDP().getCfgNSIdentityNumber(), false);	
				if (getCfgBool("carveAloneWithFull")) mz.getTuneDP().setCfgNSTurnedOff(mz.getTuneDP().getCfgNSFullNumber(), false);	
				
				for (int x=0;x<newNS.size();x++) {
					int ns = newNS.get(x);
					if (ns == mz.getTuneDP().getCfgNSFullNumber()) continue;
					// turn this one ont
					mz.getTuneDP().setCfgNSTurnedOff(ns, false);	
	
					// carve it alone
					if (false && mz.isMerge()) {
						// if merge
						// FIXME what do here? alone or with merge?
						OptimizerStatistical.carveStepVectorValues(mz.getDP(), 0, mz.isFullData(), mz.getPType(), 1000, getCfgIntArray("minDropSet"), null, 
								getCfgBool("noZero"), getCfgBool("noPositive"), 
								getCfgBool("carveReduction"), getCfgBool("carveNoBackingUp"), getCfgDouble("carveWeight"),
								false, new HashMap<>(), mz.getTuneDP(), false, null, mz.dss, mz.ctlGroup, true);
						
					} else {
						OptimizerStatistical.carveStepVectorValues(mz.getTuneDP(), 0, mz.isFullData(), mz.getPType(), 1000, getCfgIntArray("minDropSet"), null, 
								getCfgBool("noZero"), getCfgBool("noPositive"), 
								getCfgBool("carveReduction"), getCfgBool("carveNoBackingUp"), getCfgDouble("carveWeight"),
								false, new HashMap<>(), null, false, null, mz.dss, mz.ctlGroup, true);
					}
					// turn back off
					mz.getTuneDP().setCfgNSTurnedOff(ns, true);								
					System.out.print("=");
				}
				mz.getTuneDP().setCfgNSTurnedOff(false);
				System.gc();
				System.out.println("> DONE["+newNS.size()+"]");
			}
	
		}
		
		//
		// Cycle Start processing
		//
		@Override
		void processCycleStart(Optimiser mz, List<Integer> newNsSet) {	
			// this is a set of hacks that are in test.. making NS inclusion more restrictive as the window gets wider)
			
			if (getCfgBool("applyCarve") && mz.getTuneDP().getNSCount() > 1) {	
				List<Integer> nsSetFirst = newNsSet;
				int carveCnt = this.getCfgInt("processCycleCount");
				HashMap<String, Integer> dropMap = new HashMap<>();
				for (int x=0;x<carveCnt;x++) {
					// setup carve drop map
					dropMap.clear();
					for (Integer nid:mz.getProcessNSCounts().keySet()) {
						Integer cnt = mz.getProcessNSCounts().get(nid);
						if (cnt == null) continue;
						int nsi = mz.getTuneDP().getNSForHashId(nid);	
						if (nsi < 0) continue;
						if (cnt >= getCfgInt("CARVE_MAX_NS")) {
							// add ignore NS to map
							dropMap.put(""+nsi, 0);
						}
					}
					//System.out.println("CNT["+dropMap.keySet().size()+"] ["+mz.getProcessNSCounts().keySet().size()+"]");
					// carve one pass							
					if (mz.isMerge()) {
						//mz.getTuneDP().print(true);
						// merge
						OptimizerStatistical.carveStepVectorValues(mz.getTuneDP(), 0, mz.isFullData(), mz.getPType(), 1000, getCfgIntArray("minDropSetCombo"), null, 
								getCfgBool("noZero"), getCfgBool("noPositive"), 
								getCfgBool("carveReductionCombo"), getCfgBool("carveNoBackingUpCombo"),
								getCfgDouble("carveWeight"), false, dropMap, mz.getDP(), true, nsSetFirst, mz.dss, mz.ctlGroup, true);	
						//mz.getTuneDP().print(true);
	
					} else {
						OptimizerStatistical.carveStepVectorValues(mz.getTuneDP(), 0, mz.isFullData(), mz.getPType(), 1000, getCfgIntArray("minDropSetCombo"), null, 
								getCfgBool("noZero"), getCfgBool("noPositive"), 
								getCfgBool("carveReductionCombo"), getCfgBool("carveNoBackingUpCombo"),
								getCfgDouble("carveWeight"), false, dropMap, null, false, nsSetFirst, mz.dss, mz.ctlGroup, true);		
						
					}
				}
			}

							
			// dependency method
			if (getCfgBool("applyDependency")) {				
				int curPassTotalAfterCarve = mz.getCurTotal();
				// do dependency
				System.out.print("  DEP");						
				OptimizerLogical.silent = true;
				OptimizerLogical.maxStagesPerStep = 0;
				int maxSet = 4;
				int depLoseStage = 0;
				int depLoseOrStage = 1;
				int depAltStage = 2;	
				int depCrtStage = -1; // .. don't use
				// lesser
				if (getCfgBool("applyDependencyMin")) {	
					maxSet = 3;
					depAltStage = -1;
				}
				
				mz.saveTemp();
				int ncurPassTotal = 0;
				if (mz.isMerge()) {
					// if merge
// FIXME
					ncurPassTotal = OptimizerLogical.viaDependencyMapping(null, mz.getTuneDP(), true, mz.getTuneDP().getCfgNSWeight(), mz.getPType(), 
							getCfgBool("useReductionDep"), getCfgBool("fullDataDep"),
							maxSet, depLoseStage, depLoseOrStage, depCrtStage, depAltStage, null, null, null, null, null, mz.dss, mz.ctlGroup);		
				} else {
					ncurPassTotal = OptimizerLogical.viaDependencyMapping(null, mz.getTuneDP(), true, mz.getTuneDP().getCfgNSWeight(), mz.getPType(), 
										getCfgBool("useReductionDep"), getCfgBool("fullDataDep"),
										maxSet, depLoseStage, depLoseOrStage, depCrtStage, depAltStage, null, null, null, null, null, mz.dss, mz.ctlGroup);		
				}
				// if not better revert
				//int off = ncurPassTotal-curPassTotal;
				int off = ncurPassTotal-curPassTotalAfterCarve;
				if (ncurPassTotal < 0 || off <= 0) {
					// NOPE role-back
					if (curPassTotalAfterCarve > mz.getCurTotal()) {
						mz.loadTemp(); // only if not going to roll back after return
					}
					System.out.print("[NOPE]");
				} else {
					System.out.print("["+String.format("%4d", off)+"]");
				}
				mz.delTemp();
			} 
		}
	}
	
}
