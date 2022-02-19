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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vegml.VegML.PredictionType;
import vegml.VegML.ProbMethod;
import vegml.ValProb;
import vegml.VegTest.testModAcValue;
import vegml.VegTest.TestMod;
import vegml.VegTest.TestModSet;
import vegml.Data.VDataSets;



public class VDRetainSet {
	
	///////////////////////////////////////////////////////////////////////////////////////////
	// retained value
	//
	
	/**
	 * Retained processing result set
	 * NOTE another class to contain a set of these and maintain set info like mergeValue may be sensable
	 */
	public static class DsRetainVal {
		Long [] cvalue = null;				// correct value
		long dvalue = 0;				// initial response
		PredictionType dpType;			// initial response type
		List<ValProb> [] nsl = null;	// accume vplists
		long [] vid = null;				// accume ids
		int mode = 0;					// mode for use in pre/test as needed
		int baseCnt = 0;				// count of values in id if present
		List<ValProb> vpList;			// if merge base list, for other?
		// add mergeWieght to offset all as they are added to the result
		double mergeValue;
		boolean isbase = false;
	}
	

	///////////////////////////////////////////////////////////////////////////////////////////
	// resolve modifications with a retained response set
	/**
	 * 
	 * @param dp
	 * @param testSet
	 * @param dsrListSet
	 * @return
	 */
	static List<VResultSet> testSetsModify(VDataPlane dp, TestModSet testSet, List<List<DsRetainVal>> dsrListSet) {
		return testSetsModify(dp, testSet, dsrListSet, true);
	}
	
	/**
	 * 
	 * @param dp
	 * @param testSet
	 * @param dsrListSet
	 * @param noRsv
	 * @return
	 */
	static List<VResultSet> testSetsModify(VDataPlane dp, TestModSet testSet, List<List<DsRetainVal>> dsrListSet, boolean noRsv) {
		if (testSet == null || testSet.size() < 1 || dsrListSet == null) return null;
		
		List<VResultSet> tsList = new ArrayList<>();
		for (int i=0;i<testSet.size();i++) {
			VResultSet ts = new VResultSet(dp);
			ts.setRetaindDsr();
			ts.start();
			tsList.add(ts);
		}
		
		List<ValProb> vpList = new ArrayList<>();
		Accum dac = dp.getAccumDefault();
		ValProb defvp = dac.getFirstMostProbable();
		List<ValProb> freeVpList = new ArrayList<>();
		List<TestMod> tests = testSet.get(dp);
				
		// do the work
		for (int set=0;set<dsrListSet.size();set++) {
			List<DsRetainVal> dsrList = dsrListSet.get(set);
			
			for (int sset=0;sset<dsrList.size();sset++) {
				DsRetainVal drv = dsrList.get(sset);
				if (drv == null) continue;

				for (int x=0;x<tests.size();x++) {
					VResultSet ts = tsList.get(x);
					TestMod test = tests.get(x);
									
					// Identity
					List<ValProb> iac = null;
					if (dp.getCfgNSIdentityNumber() >= 0) {
						iac = drv.nsl[dp.getCfgNSIdentityNumber()];
						if (test.nsWeights[dp.getCfgNSIdentityNumber()] == 0) iac = null;
					}

					// optimization: if only intersted in:
					if (iac != null) {
						if (dp.isCfgIdentityOnly() && drv.baseCnt == 1 && drv.vpList == null) {
							// FIXME unless value mode to IAC is the value.. then default
							ts.addResponseDR(drv, drv.dvalue, drv.dpType, noRsv);
							continue;							
						}
						if (test.pType == PredictionType.PredictUnknown || test.pType == PredictionType.AnyUnknown) {
							// - unknown: skip if iac -> they dont matter
							ts.addResponseDR(drv, drv.dvalue, drv.dpType, noRsv);
							continue;
						}
					} else if (test.pType == PredictionType.NotUnknown) {
						// - unknown: skip if iac -> they dont matter
						ts.addResponseDR(drv, drv.dvalue, drv.dpType, noRsv);
						continue;						
					}
					
					// Full set
					List<ValProb> avs = null;
					if (dp.getCfgNSFullNumber() >= 0) {
						avs = drv.nsl[dp.getCfgNSFullNumber()];
						if (test.nsWeights[dp.getCfgNSFullNumber()] == 0) avs = null;
					}
					
					// optimization: if only intersted in:
					if (avs == null) {
						if (test.pType == PredictionType.Recall || test.pType == PredictionType.RecallPredict || test.pType == PredictionType.AnyRecall) {
							// - recall: skip if not recall -> they dont matter
							ts.addResponseDR(drv, drv.dvalue, drv.dpType, noRsv);
							continue;
						}						
					}
					
					// baseline vpList
					if (freeVpList.size() < 200) freeVpList.addAll(vpList);
					vpList.clear();

					PredictionType ret = PredictionType.Predict;
					long recallValue = 0;
					
					/////////////////////////////////
					// get fullest numberSet
					if (avs != null) {
						ret = PredictionType.Recall;
						if (avs.size() > 1) {
							ret = PredictionType.RecallPredict;
							if (test.getModCount(dp.getCfgNSFullNumber()) > 0) {
								int cnt = 0;
								for (int i=0;i<avs.size();i++) {
									ValProb vp = avs.get(i);
									if (vp.count <= 0) continue;
									if (checkMod(test, drv, dp.getCfgNSFullNumber(), vp, null) == null) continue;
									cnt++;
								}
								if (cnt == 1) ret = PredictionType.Recall;
							} 
						}
						if (ret == PredictionType.Recall && dp.getCfgProbMethod() == ProbMethod.AverageIfNotRecall) {
							//
							// get fullest AND no collisions then this
							//			
							int cnt = 0;
							for (int i=0;i<avs.size();i++) {
								ValProb vp = avs.get(i);
								if (vp.count <= 0) continue;
								// get value mod
								vp = checkMod(test, drv, dp.getCfgNSFullNumber(), vp, freeVpList);
								if (vp == null) continue;	
								
								// use altered weights
								double wavgProb = vp.probability * test.nsWeights[dp.getCfgNSFullNumber()];
								if (wavgProb <= 0) continue;
								vp.probability = wavgProb;
								vpList.add(vp);
								cnt++;
							}
							if (cnt > 0) {
								//Collections.sort(vpList, VegUtil.VpSort);
								setResult(dac, ts, freeVpList, drv, vpList, PredictionType.Recall, 1, noRsv);
								continue;
							}
							// not there
							ret = PredictionType.Predict;
						}
					}
										
					/////////////////////////////////
					// identity info if filtering by it
					if (iac != null && test.getModCount(dp.getCfgNSIdentityNumber()) > 0) {
						int cnt = 0;
						for (int i=0;i<iac.size();i++) {
							ValProb vp = iac.get(i);
							if (vp.count <= 0) continue;
							// get value mod
							vp = checkMod(test, drv, dp.getCfgNSIdentityNumber(), vp, null);
							if (vp == null) continue;	
							cnt++;
						}	
						if (cnt == 0) iac = null;
					}
					if (iac == null && !dp.isCfgFameFocusNone()) {				
						ret = PredictionType.PredictUnknown;
					}
										
					/////////////////////////////////
					// get the probabilties and values
					int acCnt = 0, adCnt = 0;
					for (int vi =0;vi<drv.nsl.length;vi++) {
						List<ValProb> vs = drv.nsl[vi];
						if (vs == null) continue; // empty slot
						if (test.nsWeights[vi] == 0) continue;
						acCnt++;
						boolean isCtx = dp.isCfgNSContext(vi);
						
						// get the set accumulators
						//Accum sac = dp.getAccumSetDefault(vi);
						boolean added = false;
						for (int i=0;i<vs.size();i++) {
							ValProb vp = vs.get(i);	
							if (vp.count <= 0) continue;
							
							//testModAcValue mod = test.getMod(vi, vp.value);
							vp = checkMod(test, drv, vi, vp, freeVpList);
							if (vp == null) continue;
							
							// only use values in identity
							if (iac != null && isCtx && dp.isCfgIdentityOnly()) {
								vp = checkMod(test, drv, dp.getCfgNSIdentityNumber(), vp, null);
								if (vp == null) continue;
							}
							
							// get the weighted version from the 'activation' fuction
							double wavgProb = vp.probability * test.nsWeights[vi];
							if (wavgProb <= 0) continue;						

							VegUtil.mergeIntoVPList(vpList, vp.value, wavgProb, vp.count, freeVpList);
							added = true;
						}
						if (added) {
							adCnt++;
							// get full list and merge it into the complete list
							if (vi == dp.getCfgNSFullNumber()) recallValue = vs.get(0).value;
							if (!isCtx && ret == PredictionType.Predict && vi != dp.getCfgNSIdentityNumber()) ret = PredictionType.PredictRelate;
						}
					}
					
					/////////////////////////////////
					// if nothing -> Fall back when nothing -> use general dimension set probability: get best
					if (vpList.size() < 1) {						
						VegUtil.mergeIntoVPList(vpList, defvp, freeVpList);
						setResult(dac, ts, freeVpList, drv, vpList, PredictionType.Default, 1, noRsv);						
						continue;
					}
					
					/////////////////////////////////
					// forced winner
					if (dp.getCfgProbMethod() == ProbMethod.AverageRecall && ret == PredictionType.Recall && recallValue != vpList.get(0).value) {
						Collections.sort(vpList, VegUtil.VpSort);
						for (int i=0;i<vpList.size();i++) {
							ValProb vp = vpList.get(i);
							if (vp.value == recallValue) {
								vp.probability = vpList.get(0).probability + (vpList.get(0).probability * VDataPlane.WIN_MARGIN_PERCENT);
								break;
							}
						}
					}
					
					// save result
					ts.uvalCount++;
					setResult(dac, ts, freeVpList, drv, vpList, ret, adCnt, noRsv);
				}
			}
		}
		for (int i=0;i<tests.size();i++) {
			tsList.get(i).end();
		}
		return tsList;
	}
	
	// check the test mod rule, if null.. exclide			
	// if freeVpList == null, will not modify but will return existing: for exclude only test
	static ValProb checkMod(TestMod test, DsRetainVal drv, int ns, ValProb vp, List<ValProb> freeVpList) {
		testModAcValue mod = test.getMod(ns, vp.value);
		if (mod == null) return vp;
		
		boolean match = false;
		if (test.getVectorId(ns, drv.vid[ns]) != 0) { // check exclude/weight
			if (mod.minCount >= 0 && (mod.minCount == 0 || vp.count <= mod.minCount)) match = true;
			else if (mod.maxCount >= 0 && (mod.maxCount == 0 || vp.count >= mod.maxCount)) match = true;
		}
		ValProb vx = vp;
		if (match) {
			if (mod.weight <= 0) return null; // exclude
			if (freeVpList != null) {
				// mod if desired
				if (freeVpList.size() > 0) vx = freeVpList.remove(freeVpList.size()-1);
				else vx = new ValProb();
				vp.copy(vx);
				vx.probability *= mod.weight;
			}
		} else if (freeVpList != null) {
			if (freeVpList.size() > 0) vx = freeVpList.remove(freeVpList.size()-1);
			else vx = new ValProb();
			vp.copy(vx);			
		}
		return vx;
	}
	
	// complete results
	static private void setResult(Accum dac, VResultSet rs, List<ValProb> freeVpList, DsRetainVal drv, List<ValProb> vpList, PredictionType pType, int adCnt, boolean noRsv) {
		// retain PredictionType of baseList 
		PredictionType fpType = pType;
		
		// average the VP list
		for (int i=0;i<vpList.size();i++) {
			ValProb vp = vpList.get(i);
			vp.probability = vp.probability / adCnt;
		}
		if (vpList.size() > 1) {
			Collections.sort(vpList, VegUtil.VpSort);
	
			// finalize for ties				
			double p = vpList.get(0).probability;
			if (p == vpList.get(1).probability) {
				// Average the values (based on the number here)	
				// this and finalize need to base before add.. then merge properly					
				double mw = 0;
				for (int i=0;i<vpList.size();i++) {
					ValProb vpx = vpList.get(i);
					double dacp = dac.getProbability(vpx.value);
					double mvv = vpx.probability / dacp;
					if (mvv > mw) mw = mvv;
				}
				for (int i=0;i<vpList.size();i++) {
					ValProb vpx = vpList.get(i);
					double dacp = dac.getProbability(vpx.value);
					vpx.probability = (vpx.probability+(dacp*mw)) / 2;
					if (vpx.probability != p) break;
				}	
				Collections.sort(vpList, VegUtil.VpSort);
			}
		}
		
		// get the value
		long fvalue = vpList.get(0).value;
		
		///////////////////////////
		// Merge With exiting list if set
		if (drv.vpList != null && drv.vpList.size() > 0) {			
			// get prediction type
			double mergeValue = 1;
			if (!drv.isbase) {
				// merge existing into this with the modifier, use this ptype		
				mergeValue = drv.mergeValue;
			} else {
				// update this list with modifier, use existing for ptype
				pType = drv.vpList.get(0).type;
				VegUtil.updateListProb(vpList, drv.mergeValue);
			}
			for (int i=0;i<drv.vpList.size();i++) {
				VegUtil.mergeIntoVPList(vpList, drv.vpList.get(i), freeVpList, mergeValue);
			}
			Collections.sort(vpList, VegUtil.VpSort);
			fvalue = vpList.get(0).value;			
		}
		// record it
		rs.addResponseDR(drv, fvalue, fpType, noRsv);
	}
	
	/**
	 * setup a retain set for tuning to a merge
	 * @param tuneDp dataplane in use
	 * @param dsrListSet data retain set
	 * @param mergeWithSet set to merge with from (rst.getResultsVpList())
	 * @param mergeValue merge modeifier value to weight the merge
	 * @param mergeSetIsbase true mergeWithSet is the base
	 * @return
	 */
	static VResultSet baseLineMergeModify(VDataPlane tuneDp, List<List<DsRetainVal>> dsrListSet, 
											List<List<List<ValProb>>> mergeWithSet, double mergeValue, boolean mergeSetIsbase) {
		// setup the values
		for (int set=0;set<dsrListSet.size();set++) {
			List<DsRetainVal> dsrList = dsrListSet.get(set);
			List<List<ValProb>> vpsl = mergeWithSet.get(set);			
			for (int sset=0;sset<dsrList.size();sset++) {
				DsRetainVal drv = dsrList.get(sset);
				if (drv == null) continue;
				drv.vpList = vpsl.get(sset);
				drv.mergeValue = mergeValue;
				drv.isbase = mergeSetIsbase;
				if (drv.isbase) {
					if (drv.vpList.size() > 0) drv.dpType = drv.vpList.get(0).type;
					else drv.dpType = PredictionType.Default;
				}
			}
		}
		// gen baseline 
		TestModSet testSet = new TestModSet();
		testSet.add(tuneDp, new TestMod(tuneDp.getCfgNSWeightRaw()));
		List<VResultSet> rList = VDRetainSet.testSetsModify(tuneDp, testSet, dsrListSet, false);

		return rList.get(0);
	}
	
	/**
	 * setup a retain set for tuning to a merge
	 * @param mergeSetDp dataplane to merge into
	 * @param tuneDp
	 * @param dsrListSet
	 * @param mergeValue merge modeifier value to weight the merge
	 * @param mergeSetIsbase true mergeSetDp is the base
	 * @param dss
	 * @return
	 */
	static VResultSet baseLineMergeModify(VDataPlane mergeSetDp, VDataPlane tuneDp, 
												List<List<DsRetainVal>> dsrListSet, double mergeValue, boolean mergeSetIsbase,
												VDataSets dss) {
		// get base merge
//		String s = String.format("%-16s", "["+mergeSetDp.getDimensionTag()+"/"+mergeSetDp.getTag()+"]");
//		System.out.print("   SET_MERGE_EVAL"+s+" data["+ dss.size()+"]mv["+mergeValue+"] =>");
		mergeSetDp.setCfgFramerArg(dss.getValLLV());
		VResultSet baseRst = VegTest.testSets(mergeSetDp.getVegML(), mergeSetDp.getDimensionTag(), mergeSetDp.getTag(), dss, -1);	
		mergeSetDp.setCfgFramerArg(null);	
//		System.out.println(" RESULT["+OptimizerMerge.fmtPercent(baseRst.passPercent)+"] pass["+baseRst.passTotal + " / "+baseRst.failTotal+" of " + baseRst.total+"] Time[" + baseRst.getDurration()+"]");		
		
		// setup
		return baseLineMergeModify(tuneDp, dsrListSet, baseRst.getResultsVpList(), mergeValue, mergeSetIsbase);
	}
	
	

	
	///////////////////////////////////////////////////////////////////////////////
	// 
	// Logical Reduction methods
	//
	private static void addVote(HashMap<Long, Integer>[] nsVotes, int ns, long vid) {
		Integer v = nsVotes[ns].get(vid);
		if (v == null) nsVotes[ns].put(vid, 1);
		else nsVotes[ns].put(vid, v+1);
	}
	//mode = 0; // probability
	//mode = 1; // frequency
	//mode = 2; // mixed
	@SuppressWarnings("unchecked")
	static int testVoting(VDataPlane dp, List<List<DsRetainVal>> dsrListSet, HashMap<Long, Integer>[] nsVotes, HashMap<Long, Integer>[] nsNoVotes, int mode, int max, int min, boolean singleList) {
		if (nsVotes == null || dsrListSet == null) return 0;

		Accum dac = dp.getAccumDefault();
		class vidVal {
			int ns;
			double val;
			long vid;
		}
		Comparator<vidVal> sv = new Comparator<vidVal>() {
	        @Override
	        public int compare(vidVal l, vidVal r) {     	
	        	if (l.val < r.val) return 1;
	        	if (l.val > r.val) return -1;
	        	return 0;  
	        }
	    };
		Comparator<vidVal> svr = new Comparator<vidVal>() {
	        @Override
	        public int compare(vidVal l, vidVal r) {     	
	        	if (l.val > r.val) return 1;
	        	if (l.val < r.val) return -1;
	        	return 0;  
	        }
	    };
		List<vidVal> voteValMax[] = new ArrayList[dp.getCfgWindowSize()+1];
		List<vidVal> voteNoValMax[] = new ArrayList[dp.getCfgWindowSize()+1];
		for (int i=0;i<=dp.getCfgWindowSize();i++) {
			voteValMax[i] = new ArrayList<>();
			voteNoValMax[i] = new ArrayList<>();
		}
	    int votes = 0, novotes = 0, noMatch = 0, noInfo = 0;
	    List<vidVal> freeList = new ArrayList<>();
	    
		// each numberSet has different count
		int nsCount[] = new int[dp.getCfgWindowSize()+1];
		int nsMax[] = new int[dp.getCfgWindowSize()+1];
		for (int i=1;i<=dp.getCfgWindowSize();i++) {
			nsCount[i] = dp.getNSCountForSize(i);
			nsMax[i] = (int)((double)nsCount[i] * (double)1/(double)max);
			if (nsMax[i] < 1) nsMax[i] = 1;
			nsMax[i] = max; // just use static
		}
		
		
		// do the work
		for (int set=0;set<dsrListSet.size();set++) {
			List<DsRetainVal> dsrList = dsrListSet.get(set);
			
			for (int sset=0;sset<dsrList.size();sset++) {
				DsRetainVal drv = dsrList.get(sset);							
				// frequency take the top max frequency per 
							
				/////////////////////////////////
				// get the probabilties and values
				int matchCnt = 0, infoCnt = 0;
				for (int vi =0;vi<drv.nsl.length;vi++) {
					List<ValProb> vs = drv.nsl[vi];
					if (vs == null) continue; // empty slot
					infoCnt++;
					boolean noDownVote = false;
					if (vi == dp.getCfgNSFullNumber() || vi == dp.getCfgNSIdentityNumber()) {
						noDownVote = true;
				//		continue; // not this one
					}
					
					int w = dp.getNS(vi).size();
					List<vidVal> vl = voteValMax[w];
					List<vidVal> vnl = voteNoValMax[w];
					
					boolean ctx = dp.isCfgNSContext(vi);					
					
					// get the set accumulators
					Accum sac = dp.getAccumSetDefault(vi);
					boolean voted = false, matched = false;
					for (int i=0;i<vs.size();i++) {
						ValProb vp = vs.get(i);	
						if (vp.count <= 0) continue;
						if (vp.value != drv.cvalue[0]) continue;	
						matched = true;
						matchCnt++;
						
						double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), vi, ctx, sac, null, vp);
						if (wavgProb <= 0) continue;
						
						// add if makes the cut
						double v = wavgProb;
						if (mode == 1) v = vp.count;	
						else if (mode == 2) v = wavgProb * vp.count;
						
						if (vl.size() < nsMax[w] || (vl.size() > 0 && vl.get(vl.size()-1).val < v)) {
							vidVal vv = null;
							if (freeList.size() > 0) vv = freeList.remove(0);
							else vv = new vidVal();
							vv.val = v;
							vv.ns = vi; // ns
							vv.vid = drv.vid[vi];								
							vl.add(vv);
							voted = true;
							Collections.sort(vl, sv);
							if (vl.size() > nsMax[w]) {
								vv = vl.remove(vl.size()-1);
								freeList.add(vv);
							}
						}
						break;
					}
					if (!noDownVote && !voted && !matched && vs.get(0).value != drv.cvalue[0]) {
					//if (!voted && vs.get(0).value != drv.cvalue) {
						vidVal vv = null;
						if (freeList.size() > 0) vv = freeList.remove(0);
						else vv = new vidVal();
						ValProb vp = vs.get(0);
						double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), vi, ctx, sac, null, vp);
						if (wavgProb <= 0) continue;
						//wavgProb = vp.probability; // highest
						double v = wavgProb;
						if (mode == 1) v = vp.count;	
						else if (mode == 2) v = wavgProb * vp.count;						
						vv.val = v; 
						vv.ns = vi; // ns
						vv.vid = drv.vid[vi];								
						vnl.add(vv);
					}
				}
				
				if (matchCnt == 0) {
					if (infoCnt == 0) noInfo++;
					noMatch++;
					continue;
				}
				
				
				// add the votes
				for (int i=0;i<=dp.getCfgWindowSize();i++) {
					List<vidVal> vl = voteValMax[i];
					for (int v=0;v<vl.size();v++) {
						votes++;
						vidVal vv = vl.get(v);
						addVote(nsVotes, vv.ns, vv.vid);
					}
					freeList.addAll(voteValMax[i]);
					voteValMax[i].clear();
				
					// add the no-votes
					vl = voteNoValMax[i];
					Collections.sort(vl, sv);
					for (int v=0;v<vl.size()&&v<min;v++) {
						vidVal vv = vl.get(v);
						novotes++;
						addVote(nsNoVotes, vv.ns, vv.vid);
					}
					freeList.addAll(voteNoValMax[i]);
					voteNoValMax[i].clear();
				}
				
			}
		}
		//System.out.println("VOTES["+votes+"] NOVOTES["+novotes+"]noMatch["+noMatch+"]noInfo["+noInfo+"]");

		return votes;
	}
	

	@SuppressWarnings("unchecked")
	static int testVoting2(VDataPlane dp, List<List<DsRetainVal>> dsrListSet, HashMap<Long, Integer>[] nsVotes, HashMap<Long, Integer>[] nsNoVotes, int mode, double maxPercent, int min, boolean singleList) {
		if (nsVotes == null || dsrListSet == null) return 0;

		class vidVal {
			int ns;
			double val;
			long vid;
		}
		Comparator<vidVal> sv = new Comparator<vidVal>() {
	        @Override
	        public int compare(vidVal l, vidVal r) {     	
	        	if (l.val < r.val) return 1;
	        	if (l.val > r.val) return -1;
	        	return 0;  
	        }
	    };
		Comparator<vidVal> svr = new Comparator<vidVal>() {
	        @Override
	        public int compare(vidVal l, vidVal r) {     	
	        	if (l.val > r.val) return 1;
	        	if (l.val < r.val) return -1;
	        	return 0;  
	        }
	    };
		List<vidVal> voteValMax[] = new ArrayList[dp.getCfgWindowSize()+1];
		List<vidVal> voteNoValMax[] = new ArrayList[dp.getCfgWindowSize()+1];
		for (int i=0;i<=dp.getCfgWindowSize();i++) {
			voteValMax[i] = new ArrayList<>();
			voteNoValMax[i] = new ArrayList<>();
		}
	    List<vidVal> freeList = new ArrayList<>();		
		List<ValProb> vpList = new ArrayList<>();
		List<ValProb> freeVpList = new ArrayList<>();
	    int votes = 0, novotes = 0, noMatch = 0, noInfo = 0;
		Accum dac = dp.getAccumDefault();
		
		// each numberSet has different count
		// each numberSet has different count
		int nsCount[] = new int[dp.getCfgWindowSize()+1];
		int nsMax[] = new int[dp.getCfgWindowSize()+1];
		for (int i=1;i<=dp.getCfgWindowSize();i++) {
			nsCount[i] = dp.getNSCountForSize(i);
			nsMax[i] = (int)((double)nsCount[i] * (double)1/(double)maxPercent);
			if (nsMax[i] < 1) nsMax[i] = 1;
			//	nsMax[i] = (int)max; // just use static
		}
		
		
		// do the work
		for (int set=0;set<dsrListSet.size();set++) {
			List<DsRetainVal> dsrList = dsrListSet.get(set);
			
			for (int sset=0;sset<dsrList.size();sset++) {
				DsRetainVal drv = dsrList.get(sset);							
				// frequency take the top max frequency per 
				boolean pass = false;
				if (true) {
					if (freeVpList.size() < 200) freeVpList.addAll(vpList);
					vpList.clear();
					PredictionType ret = PredictionType.Predict;
					long recallValue = 0;
					
					/////////////////////////////////
					// get fullest numberSet
					List<ValProb> avs = drv.nsl[dp.getCfgNSFullNumber()];
					if (avs != null) {
						ret = PredictionType.Recall;
						if (avs.size() > 1) ret = PredictionType.RecallPredict;
						if (ret == PredictionType.Recall && dp.getCfgProbMethod() == ProbMethod.AverageIfNotRecall) {
							Accum sac = dp.getAccumSetDefault(dp.getCfgNSFullNumber());
							int cnt = 0;
							for (int i=0;i<avs.size();i++) {
								ValProb vp = avs.get(i).copy();
								if (vp.count <= 0) continue;				
								// use altered weights
								double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), dp.getCfgNSFullNumber(), true, sac, null, vp);
								if (wavgProb <= 0) continue;
								vp.probability = wavgProb;
								vpList.add(vp);
								cnt++;
							}
							Collections.sort(vpList, VegUtil.VpSort);
							if (drv.cvalue[0] == vpList.get(0).value) pass = true;
						}
					}
					
					// Identity
					List<ValProb> iac = null;
					if (dp.getCfgNSIdentityNumber() >= 0) iac = drv.nsl[dp.getCfgNSIdentityNumber()];
					if (iac == null && !dp.isCfgFameFocusNone()) ret = PredictionType.PredictUnknown;
					// get the probabilties and values
					for (int vi =0;vi<drv.nsl.length;vi++) {
						List<ValProb> vs = drv.nsl[vi];
						if (vs == null) continue; // empty slot
								
						// get the set accumulators
						Accum sac = dp.getAccumSetDefault(vi);
						boolean added = false;
						for (int i=0;i<vs.size();i++) {
							ValProb vp = vs.get(i);	
							if (vp.count <= 0) continue;
							// get the weighted version from the 'activation' fuction
							double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), vi, (iac != null), sac, null, vp);
							if (wavgProb <= 0) continue;
							VegUtil.mergeIntoVPList(vpList, vp.value, wavgProb, vp.count, freeVpList);
							added = true;
						}
						if (added) {
							// get full list and merge it into the complete list
							if (vi == dp.getCfgNSFullNumber()) recallValue = vs.get(0).value;
							if (!dp.isCfgNSContext(vi)) {
								if (ret == PredictionType.Predict && vi != dp.getCfgNSIdentityNumber()) ret = PredictionType.PredictRelate;
							}
						}
					}
					
					/////////////////////////////////
					// if nothing -> Fall back when nothing -> use general dimension set probability: get best
					if (vpList.size() < 1) {						
						if (drv.cvalue[0] == dac.getFirstMostProbablityValue()) pass = true;					
						else pass = false;
					} else {				
						Collections.sort(vpList, VegUtil.VpSort);
		
						// forced winner
						if (dp.getCfgProbMethod() == ProbMethod.AverageRecall && ret == PredictionType.Recall && recallValue != vpList.get(0).value) {
							for (int i=0;i<vpList.size();i++) {
								ValProb vp = vpList.get(i);
								if (vp.value == recallValue) {
									vp.probability = vpList.get(0).probability + (vpList.get(0).probability * VDataPlane.WIN_MARGIN_PERCENT);
									Collections.sort(vpList, VegUtil.VpSort);
									break;
								}
							}
						}
						if (drv.cvalue[0] == vpList.get(0).value) pass = true;
						else pass = false;
					}					
				}		
				/////////////////////////////////
				// get the probabilties and values
				int matchCnt = 0, infoCnt = 0;
				for (int vi =0;vi<drv.nsl.length;vi++) {
					List<ValProb> vs = drv.nsl[vi];
					if (vs == null) continue; // empty slot
					infoCnt++;
					boolean noDownVote = false;
					if (vi == dp.getCfgNSFullNumber() || vi == dp.getCfgNSIdentityNumber()) {
						noDownVote = true;
				//		continue; // not this one
					}
					
					int w = dp.getNS(vi).size();
					List<vidVal> vl = voteValMax[w];
					List<vidVal> vnl = voteNoValMax[w];
					
					boolean ctx = dp.isCfgNSContext(vi);
					//if (ctx) nsMax++;
					
					// get the set accumulators
					Accum sac = dp.getAccumSetDefault(vi);
					boolean voted = false, matched = false;
					for (int i=0;i<vs.size();i++) {
						ValProb vp = vs.get(i);	
						if (vp.count <= 0) continue;
						if (vp.value != drv.cvalue[0]) continue;	
						matched = true;
						matchCnt++;
						
						double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), vi, ctx, sac, null, vp);
						if (wavgProb <= 0) continue;
						
						// add if makes the cut
						double v = wavgProb;
						if (mode == 1) v = vp.count;	
						else if (mode == 2) v = wavgProb * vp.count;
						
						if (vl.size() < nsMax[w] || (vl.size() > 0 && vl.get(vl.size()-1).val < v)) {
							vidVal vv = null;
							if (freeList.size() > 0) vv = freeList.remove(0);
							else vv = new vidVal();
							vv.val = v;
							vv.ns = vi; // ns
							vv.vid = drv.vid[vi];								
							vl.add(vv);
							voted = true;
							Collections.sort(vl, sv);
							if (vl.size() > nsMax[w]) {
								vv = vl.remove(vl.size()-1);
								freeList.add(vv);
							}
						}
						break;
					}
					if (!noDownVote && !voted && !matched && vs.get(0).value != drv.cvalue[0]) {
					//if (!voted && vs.get(0).value != drv.cvalue) {
					//if (!voted && vs.get(0).value != drv.cvalue) {
						vidVal vv = null;
						if (freeList.size() > 0) vv = freeList.remove(0);
						else vv = new vidVal();
						ValProb vp = vs.get(0);
						double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), vi, ctx, sac, null, vp);
						if (wavgProb <= 0) continue;
						
						double v = wavgProb;
						if (mode == 1) v = vp.count;	
						else if (mode == 2) v = wavgProb * vp.count;						
						vv.val = v; 
						vv.ns = vi; // ns
						vv.vid = drv.vid[vi];								
						vnl.add(vv);
					}
				}
				if (matchCnt == 0) {
					if (infoCnt == 0) noInfo++;
					noMatch++;
					continue;
				}
				// not if pass
			//	if (pass) continue;
	/*
	 * Establish natural boundery
	 * 1) find last positive added vp needed to match
	 * 2) find last non-positive removed vp needed to match
	 * - is match possible?
	 * - is match rational?
	 * 		-- if the distance is to far
	 * - check with AND without weighting
	 */
				
				
				// add the votes
				for (int i=0;i<=dp.getCfgWindowSize();i++) {
					List<vidVal> vl = voteValMax[i];
					for (int v=0;v<vl.size();v++) {
						votes++;
						vidVal vv = vl.get(v);
						addVote(nsVotes, vv.ns, vv.vid);
					}
					freeList.addAll(voteValMax[i]);
					voteValMax[i].clear();
				
					// add the no-votes
					vl = voteNoValMax[i];
					Collections.sort(vl, sv);
					for (int v=0;v<vl.size()&&v<min;v++) {
						vidVal vv = vl.get(v);
						novotes++;
						addVote(nsNoVotes, vv.ns, vv.vid);
					}
					freeList.addAll(voteNoValMax[i]);
					voteNoValMax[i].clear();
				}
				
			}
		}
		System.out.println("VOTES["+votes+"] NOVOTES["+novotes+"]noMatch["+noMatch+"]noInfo["+noInfo+"]");
		return votes;
	}		
 
	
	/**
	 * 
	 * @param dp
	 * @param dsrListSet
	 * @param crtDep
	 * @param crtWinDep
	 * @param crtWinOrDep
	 * @param loseDep
	 * @param loseOrDep
	 * @param altDep
	 * @param notCrtDep
	 * @param notDep
	 * @return
	 */
	static int mapDependencies(VDataPlane dp, List<List<DsRetainVal>> dsrListSet, 
			HashMap<Long, HashMap<Long, Integer>>[] crtDep, 		// required for correct win
			HashMap<Long, HashMap<Long, Integer>>[] crtWinDep, 		// required for correct win and won
			HashMap<Long, HashMap<Long, Integer>>[] crtWinOrDep, 	// contributes OR to correct win
			HashMap<Long, HashMap<Long, Integer>>[] loseDep,		// required to lose
			HashMap<Long, HashMap<Long, Integer>>[] loseOrDep,		// contributes to lose
			HashMap<Long, HashMap<Long, Integer>>[] altDep,			// second place 
			HashMap<Long, HashMap<Long, Integer>>[] notCrtDep,		// not correct important
			HashMap<Long, HashMap<Long, Integer>>[] notDep			// not important
			) {
		
		if (crtDep == null || dsrListSet == null) return 0;

		List<ValProb> vpList = new ArrayList<>();
		List<ValProb> freeVpList = new ArrayList<>();	
		
	    int noMatch = 0, noInfo = 0, notWinnable = 0;
		Accum dac = dp.getAccumDefault();
		long defVal = dac.getFirstMostProbablityValue();
		int cnt = 0;
		
		//////////////////////////
		// do the work
		for (int set=0;set<dsrListSet.size();set++) {
			List<DsRetainVal> dsrList = dsrListSet.get(set);
			
			for (int sset=0;sset<dsrList.size();sset++) {
				DsRetainVal drv = dsrList.get(sset);	
				if (drv == null) continue;
				// frequency take the top max frequency per 
				boolean pass = false;
				if (freeVpList.size() < 200) freeVpList.addAll(vpList);
				vpList.clear();
				PredictionType ret = PredictionType.Predict;
				long recallValue = 0;
				
				/////////////////////////////////
				// get fullest numberSet
				List<ValProb> avs = null;
				if (dp.getCfgNSFullNumber() >= 0) avs = drv.nsl[dp.getCfgNSFullNumber()];
				if (avs != null) {
					ret = PredictionType.Recall;
					if (avs.size() > 1) ret = PredictionType.RecallPredict;
					if (ret == PredictionType.Recall && dp.getCfgProbMethod() == ProbMethod.AverageIfNotRecall) {
						Accum sac = dp.getAccumSetDefault(dp.getCfgNSFullNumber());
						for (int i=0;i<avs.size();i++) {
							ValProb vp = avs.get(i).copy();
							if (vp.count <= 0) continue;				
							// use altered weights
							double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), dp.getCfgNSFullNumber(), true, sac, null, vp);
							if (wavgProb <= 0) continue;
							vp.probability = wavgProb;
							vpList.add(vp);
						}
						Collections.sort(vpList, VegUtil.VpSort);
						if (drv.cvalue[0] == vpList.get(0).value) pass = true;
					}
				}
				
				// Identity
				List<ValProb> iac = null;
				if (dp.getCfgNSIdentityNumber() >= 0) iac = drv.nsl[dp.getCfgNSIdentityNumber()];
				if (iac == null && !dp.isCfgFameFocusNone()) ret = PredictionType.PredictUnknown;
				
				// get the probabilties and values
				int nsCnt = drv.nsl.length;
				int matchCnt = 0, infoCnt = 0;
				for (int vi =0;vi<drv.nsl.length;vi++) {
					List<ValProb> vs = drv.nsl[vi];
					if (vs == null) continue; // empty slot
					infoCnt++;
	
					// get the set accumulators
					Accum sac = dp.getAccumSetDefault(vi);
					boolean added = false;
					for (int i=0;i<vs.size();i++) {
						ValProb vp = vs.get(i);	
						if (vp.count <= 0) continue;
						// get the weighted version from the 'activation' fuction
						double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), vi, (iac != null), sac, null, vp);
						if (wavgProb <= 0) continue;
						VegUtil.mergeIntoVPList(vpList, vp.value, wavgProb, vp.count, freeVpList);
						added = true;
						if (vp.value == drv.cvalue[0]) matchCnt++;
					}
					if (added) {
						// get full list and merge it into the complete list
						if (vi == dp.getCfgNSFullNumber()) recallValue = vs.get(0).value;
						if (!dp.isCfgNSContext(vi)) {
							if (ret == PredictionType.Predict && vi != dp.getCfgNSIdentityNumber()) ret = PredictionType.PredictRelate;
						}
					}
				}
				
				/////////////////////////////////
				// if nothing -> Fall back when nothing -> use general dimension set probability: get best
				if (vpList.size() < 1) {						
					if (drv.cvalue[0] == defVal) pass = true;					
					else pass = false;
				} else {				
					Collections.sort(vpList, VegUtil.VpSort);
	
					// forced winner
					if (dp.getCfgProbMethod() == ProbMethod.AverageRecall && ret == PredictionType.Recall && recallValue != vpList.get(0).value) {
						for (int i=0;i<vpList.size();i++) {
							ValProb vp = vpList.get(i);
							if (vp.value == recallValue) {
								vp.probability = vpList.get(0).probability + (vpList.get(0).probability * VDataPlane.WIN_MARGIN_PERCENT);
								Collections.sort(vpList, VegUtil.VpSort);
								break;
							}
						}
					}
					
					/////////////////////////////////
					// Average the values (based on the number here)
					for (int i=0;i<vpList.size();i++) {
						ValProb vpx = vpList.get(i);
						vpx.probability = vpx.probability / nsCnt;
						vpx.counter = 1;
					}	
					Collections.sort(vpList, VegUtil.VpSort);
					
					if (drv.cvalue[0] == vpList.get(0).value) pass = true;
					else pass = false;
				}					
							
				// get value
				int crtIdx = ValProb.indexOf(vpList, drv.cvalue[0]);
				
				// not-winnable
				if (matchCnt == 0) {
					if (infoCnt == 0) noInfo++;
					noMatch++;
					continue;
				}
				// not-reasonably-winnable bottom 20 percent
				if (!pass && vpList.size() > 5 && crtIdx > ((double)vpList.size() * 0.2)) {
					noMatch++;
					notWinnable++;
					continue;					
				}
				// what about identity missing correct value?
				// FIXME
				
				ValProb vpCorrect = vpList.get(crtIdx);
				ValProb vpCorrectAlt = null; // after correct
				if (vpList.size() > crtIdx+1) vpCorrectAlt = vpList.get(crtIdx+1);
				
				ValProb vpWinner = vpList.get(0);
				ValProb vpAlt = null;
				if (vpList.size() > 1) vpAlt = vpList.get(1);
				ValProb vpAlt2 = null;
				if (vpList.size() > 2) vpAlt2 = vpList.get(2);
				
				
				/////////////////////////////////
				// get the probabilties and values
				for (int vi =0;vi<drv.nsl.length;vi++) {
					List<ValProb> vs = drv.nsl[vi];
					if (vs == null) continue; // empty slot
					if (vi == dp.getCfgNSFullNumber() || vi == dp.getCfgNSIdentityNumber()) {
						// ignore this for now.
						continue; 
					}
					
					int w = dp.getNS(vi).size();
					//List<vidVal> vl = voteValMax[w];
					//List<vidVal> vnl = voteNoValMax[w];
					
					boolean ctx = dp.isCfgNSContext(vi);
					
					// get the set accumulators
					Accum sac = dp.getAccumSetDefault(vi);
					for (int i=0;i<vs.size();i++) {
						ValProb vp = vs.get(i);	
						if (vp.count <= 0) continue;
														
						double wavgProb = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), vi, ctx, sac, null, vp);
						if (wavgProb <= 0) continue;
	
						// is the winner?
						boolean winner = false, correct = false;
						if (vp.value == vpWinner.value) winner = true;
						if (vp.value == drv.cvalue[0]) correct = true;
						
						// contributing value
						double fProb = wavgProb/(double)nsCnt;
						ValProb vpThis = vpCorrect;
						if (!correct) vpThis = ValProb.find(vpList, vp.value);
						
						double rProb = vpThis.probability - fProb; // whats left if gone?
						
						
						/////////////////////////////////////////
						// record it's dependency status
						if (correct && winner) { // correct and winner
							if (vpAlt != null) {
								if (rProb <= vpAlt.probability) {
									// dependent to win
									addToMap(crtWinDep[vi], drv.vid[vi], vpThis.value);
									cnt++;
								} else if (wavgProb >= vpWinner.probability) {
						//		} else if (wavgProb >= vpAlt.probability) {  // not as good
									addToMap(crtWinOrDep[vi], drv.vid[vi], vpThis.value);
									cnt++;
								}
							} else if (wavgProb >= vpWinner.probability) {
								// better part of only?
								addToMap(crtWinDep[vi], drv.vid[vi], vpThis.value);
								cnt++;
							} 
						} else if (correct) { // correct and looser
							// does this bring it up or down
							if (vpCorrectAlt != null && rProb <= vpCorrectAlt.probability) {
								addToMap(crtDep[vi], drv.vid[vi], vpThis.value);
								cnt++;
							} else if (wavgProb >= vpThis.probability) {
								// dependency to be correct
								addToMap(crtDep[vi], drv.vid[vi], vpThis.value);
								cnt++;
							}
						} else if (winner) { // incorrect and winner
							if (rProb <= vpCorrect.probability) {
								// still not correct with this gone
								addToMap(loseDep[vi], drv.vid[vi], vpThis.value);
								cnt++;
							} else if (wavgProb >= vpCorrect.probability && wavgProb >= vpThis.probability) {
								// false win dependency
								addToMap(loseOrDep[vi], drv.vid[vi], vpThis.value);
								cnt++;
							}
						} else if (vpAlt == vpThis && pass) {
							// is alt and can go?
							if (vpAlt2 != null && rProb > vpAlt2.probability) {
								// check if above the next
								addToMap(altDep[vi], drv.vid[vi], vpThis.value);
								cnt++;
							} else if (wavgProb >= vpThis.probability) {
								// dependency to be correct
								addToMap(altDep[vi], drv.vid[vi], vpThis.value);
								cnt++;
							} 
						}
						
						// get list that do not contribute
						if (correct) { // correct and looser
							// correct winner but not important
							if (winner && vpAlt != null && wavgProb < vpThis.probability && rProb > vpAlt.probability) {
								addToMap(notCrtDep[vi], drv.vid[vi], vpThis.value);
								cnt++;							
							}
						} else {
							if (wavgProb > vpThis.probability) {
								// no love for this one
								addToMap(notDep[vi], drv.vid[vi], vpThis.value);
								cnt++;
							}
						}
					}
				}
			}
		}
		//System.out.println("COUNT["+cnt+"] noMatch["+noMatch+"]noInfo["+noInfo+"]notWinnable["+notWinnable+"]");
		return cnt;
	}	
	private static void addToMap(HashMap<Long, HashMap<Long, Integer>> setMap, long vid, long value) {
		HashMap<Long, Integer> vm = setMap.get(vid);
		if (vm == null) {
			vm = new HashMap<>();
			setMap.put(vid, vm);
		}
		Integer cnt = vm.get(value);
		if (cnt == null) vm.put(value, 1);
		else vm.put(value, cnt+1);
	}
	
	/**
	 * update the retain set with the changes from this result
	 * @param dsrListSet retain data set of results
	 * @param rs resultset to update
	 */
	public static void updateRetainSet(List<List<DsRetainVal>> dsrListSet, VResultSet rs) {		
		List<DsRetainVal> chList = rs.getRDChanges();
		if (chList.size() < 1) return;
		List<Long> chVList = rs.getRDChangesVals();

		// for each change update current value/ptype
		for (int i=0;i<chList.size();i++) {
			DsRetainVal ch = chList.get(i);
			ch.dvalue = chVList.get(i);	
		}
	}
	
	/**
	 * remove value from all in numberset
	 * @param dsrListSet
	 * @param setNumber
	 * @param value
	 * @param minCount
	 * @param maxCount
	 * @param exMap
	 * @return
	 */
	public static int removeAllNSValue(List<List<DsRetainVal>> dsrListSet, int setNumber, long value, 
											int minCount, int maxCount, HashMap<Long, Integer> exMap) {	
		int cnt = 0;
		for (int set=0;set<dsrListSet.size();set++) {
			List<DsRetainVal> dsrList = dsrListSet.get(set);
			for (int sset=0;sset<dsrList.size();sset++) {
				DsRetainVal drv = dsrList.get(sset);
				if (drv == null) continue;
				
				List<ValProb> vpList = drv.nsl[setNumber];
				if (vpList == null) continue;
				if (exMap != null) {
					Integer n = exMap.get(drv.vid[setNumber]);
					if (n != null && n == 0) continue; // exclusion
				}
						
				int idx = ValProb.indexOf(vpList, value);
				if (idx < 0) continue;
				
				ValProb vp = vpList.get(idx);
				boolean remove = false;
				if (minCount >= 0 && (vp.count <= minCount || minCount == 0)) remove = true;
				if (maxCount >= 0 && (vp.count >= maxCount || maxCount == 0)) remove = true;
				
				if (remove) {
					if (vpList.size() == 0 || vpList.size() == 1) drv.nsl[setNumber] = null;
					else {
						vp.count = -1;
						vp.value = 0;
						if (idx == (vpList.size()-1)) {
							vpList.remove(idx); // last one
						} else if (vpList.size() > 8) {
							int xcnt = 0;
							for (int i=0;i<vpList.size();i++) {
								if (vpList.get(i).count > 0) xcnt++;
							}
							if (xcnt == 0) drv.nsl[setNumber] = null;
							else if ((xcnt+8) <= vpList.size()) {
								// clear the removed
								drv.nsl[setNumber] = new ArrayList<>(xcnt);
								for (int i=0;i<vpList.size();i++) {
									if (vpList.get(i).count > 0) drv.nsl[setNumber].add(vpList.get(i));
								}
							}
						}
					}
					cnt++;
				}
			}
		}
		return cnt;
	}
	
	/**
	 * remove this value from all in numberset
	 * @param dp
	 * @param dsrListSet
	 * @param nsVotes
	 * @param context
	 * @return
	 */
	static int removeAllNSByVotes(VDataPlane dp, List<List<DsRetainVal>> dsrListSet, HashMap<Long, Integer>[] nsVotes, boolean context) {	
		int cnt = 0;
		for (int set=0;set<dsrListSet.size();set++) {
			List<DsRetainVal> dsrList = dsrListSet.get(set);
			for (int sset=0;sset<dsrList.size();sset++) {
				DsRetainVal drv = dsrList.get(sset);
				if (drv == null) continue;
				
				for (int ns=0;ns<nsVotes.length;ns++) {
					if (ns == dp.getCfgNSFullNumber()) continue;
					if (ns == dp.getCfgNSIdentityNumber()) continue;
					if (!context && dp.isCfgNSContext(ns)) continue;
					if (drv.vid[ns] == 0) continue;
					if (drv.nsl[ns] == null) continue;
					
					Integer votes = nsVotes[ns].get(drv.vid[ns]);				
					if (votes != null) continue; // has votes
									
					//System.out.println(" DRP["+ns+"] " + vpList.size());
					drv.nsl[ns] = null;
					cnt++;
				}
			}
		}
		return cnt;
	}
	
	static int removeAllNSByVotesExclude(VDataPlane dp, List<List<DsRetainVal>> dsrListSet, HashMap<Long, Integer>[] removeList, boolean context) {	
		int cnt = 0;
		for (int set=0;set<dsrListSet.size();set++) {
			List<DsRetainVal> dsrList = dsrListSet.get(set);
			for (int sset=0;sset<dsrList.size();sset++) {
				DsRetainVal drv = dsrList.get(sset);
				if (drv == null) continue;
				
				for (int ns=0;ns<removeList.length;ns++) {
					if (ns == dp.getCfgNSFullNumber()) continue;
					if (ns == dp.getCfgNSIdentityNumber()) continue;
					if (!context && dp.isCfgNSContext(ns)) continue;
					if (drv.vid[ns] == 0) continue;
					if (drv.nsl[ns] == null) continue;
					
					Integer votes = removeList[ns].get(drv.vid[ns]);				
					if (votes == null) continue; // has votes
									
					//System.out.println(" DRP["+ns+"] " + vpList.size());
					drv.nsl[ns] = null;
					cnt++;
				}
			}
		}
		return cnt;
	}
	
	/**
	 * remove value from numberSet in retain set
	 * @param drv retain data set
	 * @param ns numberSet id
	 * @param valueId value to remove
	 * @return count removed
	 */
	public static int removeValue(DsRetainVal drv, int ns, long valueId) {	
		List<ValProb> vpList = drv.nsl[ns];
		if (vpList == null) return 0;
		int idx = ValProb.indexOf(vpList, valueId);
		if (idx < 0) return 0;
		if (vpList.size() == 1) drv.nsl[ns] = null;
		else vpList.remove(idx);
		return 1;
	}

	/**
	 * remove all values in list
	 * @param dp dataplane to remove from
	 * @param dsrListSet
	 * @param removeList
	 * @return
	 */
	public static int removeAllNumberSet(VDataPlane dp, List<List<DsRetainVal>> dsrListSet, HashMap<Long, HashMap<Long, Integer>> [] removeList) {	
		int cnt = 0;
		for (int set=0;set<dsrListSet.size();set++) {
			List<DsRetainVal> dsrList = dsrListSet.get(set);
			for (int sset=0;sset<dsrList.size();sset++) {
				DsRetainVal drv = dsrList.get(sset);
				if (drv == null) continue;
				
				for (int ns=0;ns<removeList.length;ns++) {
					if (ns == dp.getCfgNSFullNumber()) continue;
					if (ns == dp.getCfgNSIdentityNumber()) continue;
					if (drv.vid[ns] == 0) continue;
					if (drv.nsl[ns] == null) continue;
					
					// get value list
					HashMap<Long, Integer> vl = removeList[ns].get(drv.vid[ns]);
					if (vl == null) continue;
					List<ValProb> vpList = drv.nsl[ns];
					for (Long value:vl.keySet()) {
						int idx = ValProb.indexOf(vpList, value);
						if (idx < 0) continue;
						cnt++;

						if (vpList.size() == 1) {
							drv.nsl[ns] = null;
							break;
						} else {
							vpList.remove(idx);
						}
					}
				}
			}
		}
		return cnt;
	}
	
	/**
	 * devalue all values in list
	 * @param dp
	 * @param dsrListSet
	 * @param removeList
	 * @param weight
	 * @return
	 */
	public static int weightAllNumberSet(VDataPlane dp, List<List<DsRetainVal>> dsrListSet, HashMap<Long, HashMap<Long, Integer>> [] removeList, double weight) {	
		int cnt = 0;
		
		Set<ValProb> hs = new HashSet<>();
		
		for (int set=0;set<dsrListSet.size();set++) {
			List<DsRetainVal> dsrList = dsrListSet.get(set);
			for (int sset=0;sset<dsrList.size();sset++) {
				DsRetainVal drv = dsrList.get(sset);
				if (drv == null) continue;
				
				for (int ns=0;ns<removeList.length;ns++) {
					if (ns == dp.getCfgNSFullNumber()) continue;
					if (ns == dp.getCfgNSIdentityNumber()) continue;
					if (drv.vid[ns] == 0) continue;
					if (drv.nsl[ns] == null) continue;
					
					// get value list
					HashMap<Long, Integer> vl = removeList[ns].get(drv.vid[ns]);
					if (vl == null) continue;
					
					List<ValProb> vpList = drv.nsl[ns];
					
					for (Long value:vl.keySet()) {
						int idx = ValProb.indexOf(vpList, value);
						if (idx < 0) continue;						
						ValProb vp = vpList.get(idx);
						
						if (hs.contains(vp)) continue; // no dup 
						hs.add(vp);
						
						// modify (with the same skew as the accum)
						vp.probability = Accum.weightProb(vp.probability, weight);
						cnt++;
					}
				}
			}
		}
		return cnt;
	}

	
	/**
	 * weight value from all in numberset
	 * @param dsrListSet
	 * @param setNumber
	 * @param value
	 * @param minCount
	 * @param maxCount
	 * @param exMap
	 * @param weight
	 * @return
	 */
	public static int weightAllNSValue(List<List<DsRetainVal>> dsrListSet, int setNumber, long value, 
												int minCount, int maxCount, HashMap<Long, Integer> exMap, double weight) {	
		int cnt = 0;
		Set<ValProb> hs = new HashSet<>();

		for (int set=0;set<dsrListSet.size();set++) {
			List<DsRetainVal> dsrList = dsrListSet.get(set);
			for (int sset=0;sset<dsrList.size();sset++) {
				DsRetainVal drv = dsrList.get(sset);
				if (drv == null) continue;

				List<ValProb> vpList = drv.nsl[setNumber];
				if (vpList == null) continue;
				if (exMap != null) {
					Integer n = exMap.get(drv.vid[setNumber]);
					if (n != null && n == 0) continue; // exclusion
				}
						
				int idx = ValProb.indexOf(vpList, value);
				if (idx < 0) continue;
				
				ValProb vp = vpList.get(idx);
				boolean update = false;
				if (minCount >= 0 && (vp.count <= minCount || minCount == 0)) update = true;
				if (maxCount >= 0 && (vp.count >= maxCount || maxCount == 0)) update = true;
				
				if (update) {
					if (hs.contains(vp)) continue; // no dup 
					hs.add(vp);
					
					// modify (with the same skew as the accum)
					vp.probability = Accum.weightProb(vp.probability, weight);
					cnt++;
				}
			}
		}
		return cnt;
	}
	

}
