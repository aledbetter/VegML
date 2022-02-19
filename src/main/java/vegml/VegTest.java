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
import java.util.HashMap;
import java.util.List;

import vegml.VDRetainSet.DsRetainVal;
import vegml.Data.VDataSet;
import vegml.Data.VDataSets;
import vegml.VegML.PredictionType;
import vegml.ValProb;


public class VegTest {
	
	/**
	 * Save mode for dataplane / dataset stats
	 * 	SaveRecall, 		 - save as recall stats
	 *	SaveRecallNoReset, 	 - add to existing recall stats
	 *	SavePredict,		 - save as prediction stats
	 *	SavePredictNoReset,	 - add to existing prediction stats
	 *	NoSave				 - don't save info
	 */
	public enum SaveStats {
		SaveRecall, 
		SaveRecallNoReset, 
		SavePredict,
		SavePredictNoReset,
		NoSave
	};
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Test Predict Segments
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Test datasets as a set of segments
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets  testing dataset
	 * @return result set
	 */
	public static VResultSet testSegments(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dataSets) {	
		return testSegments(vML, dimensionTag, dataPlaneTag, dataSets, SaveStats.NoSave);
	}
	
	/**
	 * Test datasets as a set of segments, and retain stats
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets  testing dataset
	 * @param saveStats what save stats setting to use
	 * @return result set
	 */
	public static VResultSet testSegments(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dataSets, SaveStats saveStats) {			
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataSets == null || dataPlane == null || dataPlane.getNSCount() < 1) return null;
		
		int progressTick = 1, progressCnt = 0, progressLast = -1;
		if (vML.isCfgShowProgress()) {
			progressTick = VegML.getTickCount(dataSets.size());
		}
		
		if (saveStats == SaveStats.SavePredict) {
			dataPlane.clearPredictAnswerAccum();
		} else if (saveStats == SaveStats.SaveRecall) {
			dataPlane.clearRecallAnswerAccum();
		}
		
		VResultSet ts = new VResultSet(dataPlane);
		ts.start();
		VContext ctx = new VContext(vML);

		for (int seg=0;seg<dataSets.size();seg++) {
			VDataSet ds = dataSets.get(seg);
			
			Long [] vvect = ds.getValueVD();
			
			progressLast = VegML.showProgress(vML,progressCnt, progressTick, progressLast, "=");
			progressCnt++;
			List<ValProb> vpList = new ArrayList<>();

			PredictionType ret = testAnswerFocusSegment(ctx, dataPlane, false, ts.valueOut, dataSets, seg, vvect, vpList, saveStats);
			if (ret == PredictionType.None) {
				ts.addResponse(vvect, 0, false, PredictionType.Predict, null);
			} else {
				ts.addResponse(vvect, 0, true, ret, null);
			}
	//		System.out.println("SEG["+seg+"]("+vpList.size()+") " + ret + " ["+vpList.get(0).value+"]!=["+vvect[0]+"] " + vpList.get(0).probability);
		}

		ts.end();
				
		if (vML.isCfgShowProgress()) System.out.println("!");
		return ts;		
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Predict with values to compare
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Test datasets as sequence set
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @return result set
	 */
	public static VResultSet testSets(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss) {
		return testSets(vML, dimensionTag, dataPlaneTag, dss, SaveStats.NoSave, 0, false);
	}
	
	/**
	 * Test datasets as sequence set
	 * Save stats
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @param saveStats stat save mode
	 * @return result set
	 */
	public static VResultSet testSets(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, SaveStats saveStats) {
		return testSets(vML, dimensionTag, dataPlaneTag, dss, saveStats, 0, false);
	}
	
	/**
	 * Test datasets as sequence set
	 * Save vplist per token
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @param maxVpListSize max vplist to save per position or -1 for all
	 * @return result set
	 */
	public static VResultSet testSets(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, int maxVpListSize) {
		return testSets(vML, dimensionTag, dataPlaneTag, dss, SaveStats.NoSave, maxVpListSize);
	}
	
	/**
	 * Test datasets as sequence set
	 * Save vplist per token and save stats
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @param saveStats stat save mode
	 * @param maxVpListSize max vplist to save per position or -1 for all
	 * @return result set
	 */
	public static VResultSet testSets(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, SaveStats saveStats, int maxVpListSize) {
		return testSets(vML, dimensionTag, dataPlaneTag, dss, saveStats, maxVpListSize, false);
	}
	
	/**
	 * Test datasets as sequence set
	 * Save vplist per token and save stats, optionally retain correct value per token
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @param saveStats stat save mode
	 * @param maxVpListSize max vplist to save per position or -1 for all
	 * @param recordCorrect true to record correct value per position
	 * @return result set
	 */
	public static VResultSet testSets(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, 
					SaveStats saveStats, int maxVpListSize, boolean recordCorrect) {

		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dss == null || dataPlane == null || dataPlane.getNSCount() < 1) return null;

		int progressTick = 1, progressCnt = 0, progressLast = -1;
		if (vML.isCfgShowProgress()) {
			progressTick = VegML.getTickCount(dss.size());
		}
		
		if (saveStats == SaveStats.SavePredict) {
			dataPlane.clearPredictAnswerAccum();
		} else if (saveStats == SaveStats.SaveRecall) {
			dataPlane.clearRecallAnswerAccum();
		}
		
		VContext ctx = new VContext(vML);
		VResultSet ts = new VResultSet(dataPlane);
		if (recordCorrect) ts.turnOnRecordCorrect(); // if recording correct
		ts.start();
		ts.responseVpOut = new ArrayList<>();	

		long [] inputNo = dataPlane.getIgnoreInputs();
		VFrame frame = new VFrame();
		frame.init(dataPlane);
		
		int position = 0;		
		for (int set=0;set<dss.size();set++) {
			VDataSet dataSet = dss.get(set);
		
			List<List<ValProb>> valueVpOut = null;
			if (maxVpListSize != 0) valueVpOut = new ArrayList<>(dataSet.size());
			
			progressLast = VegML.showProgress(vML,progressCnt, progressTick, progressLast, "=");
			progressCnt++;
					
			// set value size if they didn't prior 
			for (int i=0;i<dataSet.size();i++, position++) {
				Long [] valueId = dataSet.getValueVD(i);
				long dvid = dataSet.getDataV(i);
				
				int pidx = VegUtil.contains(inputNo, dvid);
				if (pidx >= 0) {
					// need to map in the value to add for each	
					long valu = dataPlane.getIgnoreInputsValue(pidx);
					ts.addResponseNoEval(position, valu);		
					if (maxVpListSize != 0) {
						frame.vpList.clear();
						frame.vpList.add(new ValProb(valu));
						valueVpOut.add(VegUtil.copy(frame.vpList, maxVpListSize));
					}
					continue;
				}		
				
				if (dataPlane.isCfgIdentityOnly()) {
					// PERFORMANCE: fast path for single values..
					ValProb vp = dataPlane.getIdenityValProbIfSingle(dvid);
					if (vp != null) {
						frame.vpList.clear();
						frame.vpList.add(vp);
						PredictionType ret = testAnswerFocusResult(ctx, dataPlane, frame, ts.valueOut, frame.vpList, vp.type, valueId, false, saveStats);
						ts.addResponse(valueId, frame.getPredictionValue(), (ret != PredictionType.None), frame.getPredictionType(), frame.vpList);
						ts.valueOut.add(frame.vpList.get(0).value);
						if (recordCorrect) {	
							ts.addResponseContains(position, valueId[0], frame.vpList, (ret != PredictionType.None), dvid);
						} 
						// add results copy
						if (maxVpListSize != 0) {
							valueVpOut.add(VegUtil.copy(frame.vpList, maxVpListSize));
							frame.vpList.clear();
						}
						continue;
					}
				}
				
				if (!dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, frame, dataPlane.getFramerArg(), false, ts.valueOut, dss, set, i)) {
					ts.addResponseNoEval(position, (long)VegML.emptyVect);		
					if (maxVpListSize != 0) valueVpOut.add(new ArrayList<ValProb>()); // empty list
					continue;
				}	
				PredictionType ret = testAnswerFocus(ctx, dataPlane, frame, ts.valueOut, valueId, saveStats);
				ts.addResponse(valueId, frame.getPredictionValue(), (ret != PredictionType.None), frame.getPredictionType(), frame.vpList);
				if (recordCorrect && frame.getVectSpace() != null) {	
					long vid = frame.getVectSpace()[0];	
					ts.addResponseContains(position, valueId[0], frame.vpList, (ret != PredictionType.None), vid);
				} 
				
				// add results copy
				if (maxVpListSize != 0) {
					valueVpOut.add(VegUtil.copy(frame.vpList, maxVpListSize));
					frame.vpList.clear();
				}
			}
			// add complex result
			if (maxVpListSize != 0) {
				ts.responseVpOut.add(valueVpOut);
			}
		}
		ts.end();
		if (vML.isCfgShowProgress()) System.out.println("");
		return ts;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Predict blocks
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Test datasets as blocks 
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @return result set
	 */	
	public static VResultSet testBlocks(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss) {
		return testBlocks(vML, dimensionTag, dataPlaneTag, dss, SaveStats.NoSave, 0);
	}
	
	/**
	 * Test datasets as blocks 
	 * Save stats
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @param saveStats stat save mode
	 * @return result set
	 */
	public static VResultSet testBlocks(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, SaveStats saveStats) {
		return testBlocks(vML, dimensionTag, dataPlaneTag, dss, saveStats, 0);
	}
	
	/**
	 * Test datasets as blocks 
	 * Save vplist per token
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @param maxVpListSize max vplist to save per position or -1 for all
	 * @return result set
	 */
	public static VResultSet testBlocks(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, int maxVpListSize) {
		return testBlocks(vML, dimensionTag, dataPlaneTag, dss, SaveStats.NoSave, maxVpListSize);
	}
	
	/**
	 * Test datasets as blocks 
	 * Save vplist per token and save stats
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @param saveStats stat save mode
	 * @param maxVpListSize max vplist to save per position or -1 for all
	 * @return result set
	 */
	public static VResultSet testBlocks(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, SaveStats saveStats, int maxVpListSize) {
		return testBlocks(vML, dimensionTag, dataPlaneTag, dss, saveStats, maxVpListSize, false);
	}
	
	/**
	 * Test datasets as blocks 
	 * Save vplist per token and save stats, optionally retain correct value per token
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @param saveStats stat save mode
	 * @param maxVpListSize max vplist to save per position or -1 for all
	 * @param recordCorrect true to record correct value per position
	 * @return result set
	 */
	public static VResultSet testBlocks(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, SaveStats saveStats, int maxVpListSize, boolean recordCorrect) {

		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dss == null || dataPlane == null || dataPlane.getNSCount() < 1) return null;

		int progressTick = 1, progressCnt = 0, progressLast = -1;
		if (vML.isCfgShowProgress()) {
			progressTick = VegML.getTickCount(dss.size());
		}
		
		if (saveStats == SaveStats.SavePredict) {
			dataPlane.clearPredictAnswerAccum();
		} else if (saveStats == SaveStats.SaveRecall) {
			dataPlane.clearRecallAnswerAccum();
		}
		
		VContext ctx = new VContext(vML);
		VResultSet ts = new VResultSet(dataPlane);
		if (recordCorrect) ts.turnOnRecordCorrect(); // if recording correct
		ts.start();
		ts.responseVpOut = new ArrayList<>();	
		
		int position = 0;		
		for (int set=0;set<dss.size();set++) {
			VDataSet dataSet = dss.get(set);
			List<List<ValProb>> valueVpOut = null;
			if (maxVpListSize != 0) valueVpOut = new ArrayList<>(dataSet.size());
			
			progressLast = VegML.showProgress(vML,progressCnt, progressTick, progressLast, "=");
			progressCnt++;
			VFrame frame = new VFrame();
	// FIXME frame per dataSet				
			
			
			// set value size if they didn't prior 
			for (int i=0;i<dataSet.size();i++, position++) {
				Long [] valueId = dataSet.getValueVD(i); 			
				if (!dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, frame, dataPlane.getFramerArg(), false, ts.valueOut, dss, set, i)) {
					ts.addResponseNoEval(position, (long)VegML.emptyVect);		
					if (maxVpListSize != 0) valueVpOut.add(new ArrayList<ValProb>()); // empty list
					continue;
				}	
				PredictionType ret = testAnswerFocus(ctx, dataPlane, frame, ts.valueOut, valueId, saveStats);
				ts.addResponse(valueId, frame.getPredictionValue(), (ret != PredictionType.None), frame.getPredictionType(), frame.vpList);
				if (recordCorrect) {	
					long vid = frame.getVectSpace()[0];	
					ts.addResponseContains(position, valueId[0], frame.vpList, (ret != PredictionType.None), vid);
				} 
				// add results copy
				if (maxVpListSize != 0) {
					valueVpOut.add(VegUtil.copy(frame.vpList, maxVpListSize));
					frame.vpList.clear();
				}
			}
			// add complex result
			if (maxVpListSize != 0) {
				ts.responseVpOut.add(valueVpOut);
			}
		}
		ts.end();
		if (vML.isCfgShowProgress()) System.out.println("");
		return ts;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Test Predict Stream
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Test datasets as stream of tokens 
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @return result set
	 */
	public static VResultSet testStreams(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dataSets) {	
		return testStreams(vML, dimensionTag, dataPlaneTag, dataSets, SaveStats.NoSave);
	}
	
	/**
	 * Test datasets as stream of tokens 
	 * Save stats
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dataSets testing dataset
	 * @param saveStats stat save mode
	 * @return result set
	 */
	public static VResultSet testStreams(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dataSets, SaveStats saveStats) {			
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataSets == null || dataPlane == null || dataPlane.getNSCount() < 1) return null;
		
		int progressTick = 1, progressCnt = 0, progressLast = -1;
		if (vML.isCfgShowProgress()) {
			progressTick = VegML.getTickCount(dataSets.size());
		}
		
		if (saveStats == SaveStats.SavePredict) {
			dataPlane.clearPredictAnswerAccum();
		} else if (saveStats == SaveStats.SaveRecall) {
			dataPlane.clearRecallAnswerAccum();
		}
		
		VResultSet ts = new VResultSet(dataPlane);
		ts.start();
		VContext ctx = new VContext(vML);
	/*
	 *for boolean set dps OR use streams?
	 *NEED ask if is/is-not XXX
	 *	- this should produce much better results then open ended most likely to do thing
	 *  - requires the Accum to add negatives
	 */
		long bvvect = 0;
		if (dataSets.getDefinition() != null) {
			bvvect = dataPlane.getCfgVToV().toVectGen(dataSets.getDefinition().getTagValueBaselineTotal());
		}
				
		boolean li = false;
		if (dataSets.isFmtValueList()) li = true;
		
	//BUG: final ties don't get broken -> random finish		
	/*
	 * ISSUE:
	 * 	need to get position that decision exceeds threshold?		
	 */
		for (int seg=0;seg<dataSets.size();seg++) {
			VDataSet ds = dataSets.get(seg);
			progressLast = VegML.showProgress(vML,progressCnt, progressTick, progressLast, "=");
			progressCnt++;
		
			if (li) {
				// do the evaluation
				List<ValProb> vpList = VegTest.predictSegmentSet_int(ctx, dataPlane, false, dataSets, seg);				
				
				// no check each value  (in the 0 position)
				Long [] valueId = ds.getValueVD(0);
				Long [] vvect = new Long[1];

				for (Long v:valueId) {
					if (v == bvvect) continue; // not the baseline
					vvect[0] = v;
					ValProb vp = ValProb.find(vpList, v);
					
			// FIXME this is wrong? need the negative probability, together they may be greater or lesser than 1		
					// if boolean type AND value's probabilitiy > 50% then pass
					if (vp != null && vp.probability > 0.50) {
						vpList.remove(vp);
						vpList.add(0, vp);
					}
					PredictionType ret = testAnswerFocusResult(ctx, dataPlane, null, ts.valueOut, vpList, PredictionType.Predict, vvect, true, saveStats);	
// FIXME must account for baseline being negative set					
					if (ret == PredictionType.None) {						
						ts.addResponse(vvect, 0, false, PredictionType.Predict, null);
		//				if (vp != null) System.out.println(" FAIL("+vpList.size()+")["+v+"] -> ["+vp.probability+"]["+vp.count+"] ");
		//				else System.out.println(" FAIL("+vpList.size()+")["+v+"] -> [NONE]");
					} else {
						ts.addResponse(vvect, 0, true, ret, null);
		//				System.out.println(" PASS("+vpList.size()+")["+v+"] -> ["+vp.probability+"]["+vp.count+"] ");
					}				
				}
			} else {
				List<ValProb> vpList = new ArrayList<>();
				Long [] vvect = ds.getValueVD();
				PredictionType ret = testAnswerFocusSegment(ctx, dataPlane, false, ts.valueOut, dataSets, seg, vvect, vpList, saveStats);
				if (ret == PredictionType.None) {
					ts.addResponse(vvect, 0, false, PredictionType.Predict, null);
				} else {
					ts.addResponse(vvect, 0, true, ret, null);
				}
			}			
		}

		ts.end();
				
		if (vML.isCfgShowProgress()) System.out.println("!");
		return ts;		
	}



	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Test Predict multiple with modifications for numberSets and accumulators
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	static class testModAcValue {
		long value;
		double weight;	// add this weight to this value
		int minCount;	// min count to include, 0 = not included
		int maxCount;	// max count to include, 0 = not included
	}
	/**
	 * Test Modification
	 */
	public static class TestMod {
		PredictionType pType = null;
		int singleNS = -1;
		int vcnt = 0;
		int val; // temp
		double [] nsWeights; 		// 0 for off
		int [] nsOff; 		// list of off, in order
		HashMap<Long, testModAcValue> [] nsValueMods;	// per ns, list of mods
		HashMap<Long, Integer> [] nsVectorMods;	// per ns, list of mods
		Object userTag = null;	// set by caller, not modified
		
		// clears the ns values
		@SuppressWarnings("unchecked")
		public TestMod(int ns) {
			this.nsValueMods = new HashMap[ns];
			this.nsWeights = new double[ns];
			Arrays.fill(this.nsWeights, 0.0);
			this.singleNS = -1;
			this.vcnt = 0;
		}
		
		// makes copy of weights
		@SuppressWarnings("unchecked")
		public TestMod(double [] nsW) {
			this.nsValueMods = new HashMap[nsW.length];
			this.nsWeights = Arrays.copyOf(nsW, nsW.length);
			this.singleNS = -1;
			this.vcnt = 0;
		}
		
		testModAcValue getMod(int ns, long value) {
			if (nsValueMods[ns] == null) return null;
			return nsValueMods[ns].get(value);
		}
		void setPredictionType(PredictionType pType) {
			this.pType = pType;
		}
		int getModCount(int ns) {
			if (nsValueMods[ns] == null) return 0;
			return nsValueMods[ns].keySet().size();
		}
		testModAcValue addModMin(int ns, long value, int minCount) {
			if (nsValueMods[ns] == null) nsValueMods[ns] = new HashMap<>();
			testModAcValue mod = nsValueMods[ns].get(value);
			if (mod == null) mod = new testModAcValue();
			mod.value = value;
			mod.minCount = minCount;
			mod.maxCount = -1;
			vcnt++;
			nsValueMods[ns].put(value, mod);
			return mod;
		}
		testModAcValue addModMax(int ns, long value, int maxCount) {
			if (nsValueMods[ns] == null) nsValueMods[ns] = new HashMap<>();
			testModAcValue mod = nsValueMods[ns].get(value);
			if (mod == null) mod = new testModAcValue();
			mod.value = value;
			mod.maxCount = maxCount;
			mod.minCount = -1;
			vcnt++;
			nsValueMods[ns].put(value, mod);
			return mod;
		}
		testModAcValue addMod(int ns, long value, double weight) {
			if (nsValueMods[ns] == null) nsValueMods[ns] = new HashMap<>();
			testModAcValue mod = nsValueMods[ns].get(value);
			if (mod == null) mod = new testModAcValue();
			mod.value = value;
			mod.weight = weight;
			mod.minCount = mod.minCount = -1;
			vcnt++;
			nsValueMods[ns].put(value, mod);
			return mod;
		}
		// get vector
		int getVectorId(int ns, long vid) {
			if (nsVectorMods == null || nsVectorMods[ns] == null) return -1;
			Integer n = nsVectorMods[ns].get(vid);
			if (n == null) return -1;
			return n;
		}
		boolean isVectorExclude(int ns, long vid) {
			if (this.nsVectorMods == null || nsVectorMods[ns] == null) return false;
			Integer n = nsVectorMods[ns].get(vid);
			if (n == null || n != 0) return false;
			return true;
		}
		int getVectorCount(int ns) {
			if (nsVectorMods == null || nsVectorMods[ns] == null) return 0;
			return nsVectorMods[ns].keySet().size();
		}
		// list of vectors to exclude: don't remove 
		@SuppressWarnings("unchecked")
		int addModVectorList(int ns, List<Long> excludeList, List<Long> includeList) {
			if (nsVectorMods == null) nsVectorMods = new HashMap[nsWeights.length];
			if (nsVectorMods[ns] == null) nsVectorMods[ns] = new HashMap<>();
			if (excludeList != null) {
				for (Long vid:excludeList) {
					nsVectorMods[ns].put(vid, 0); // exclude
				}
			} else if (includeList != null) {
				for (Long vid:includeList) {
					nsVectorMods[ns].put(vid, 1); // include
				}
			}
			return nsVectorMods[ns].keySet().size();
		}
		@SuppressWarnings("unchecked")
		int addModVectorList(int ns, long excludeVid, long includeVid) {
			if (nsVectorMods == null) nsVectorMods = new HashMap[nsWeights.length];
			if (nsVectorMods[ns] == null) nsVectorMods[ns] = new HashMap<>();
			if (excludeVid != 0) nsVectorMods[ns].put(excludeVid, 0); // exclude
			else if (includeVid != 0) nsVectorMods[ns].put(includeVid, 1); // include
			return nsVectorMods[ns].keySet().size();
		}
		void reset(double [] nsW) {
			if (nsValueMods != null) Arrays.fill(nsValueMods, null);
			if (nsVectorMods != null) Arrays.fill(nsVectorMods, null);
			if (this.nsWeights != null && this.nsWeights.length == nsW.length) {
				for (int i=0;i<this.nsWeights.length;i++) this.nsWeights[i] = nsW[i];
			} else {
				this.nsWeights = Arrays.copyOf(nsW, nsW.length);
			}
			this.singleNS = -1;
			this.vcnt = 0;
			
		}
		// check if ns has mods
		boolean isModNS(int ns) {
			if (this.nsWeights[ns] == 0) return true;
			if (nsVectorMods == null) return false;
			if (nsVectorMods[ns] == null || nsVectorMods[ns].size() == 0) return false;
			return true;
		}
		// if single NS changed then
		int getSingleNS() {
			return singleNS;
		}
		// get set of numberSets turned off by this change
		int [] getOffNS() {
			return nsOff;
		}
		public TestMod completeNSW() {
			int c = 0, nsCnt = 0, nsLast = -1;
			for (int i=0;i<nsWeights.length;i++) if (nsWeights[i] == 0) c++;
			nsOff = new int[c];
			c = 0;
			for (int i=0;i<nsWeights.length;i++)  {
				if (nsWeights[i] == 0) {
					nsOff[c] = i;
					c++;
				} else {
					nsCnt++;
					nsLast = i;
				}
			}
			VegUtil.softmax(nsWeights);
			if (nsCnt == 1) singleNS = nsLast;
			return this;
		}
	}
	
	/**
	 * Test Modification Set
	 * Contains list of independent modifications to test
	 */
	public static class TestModSet {
		private HashMap<String, List<TestMod>> hm = null;
		private int count = 0;
		
		public int size() {
			return count;
		}
		public void clear() {
			if (hm != null) hm.clear();
			count = 0;
		}
		public void add(VDataPlane dp, TestMod test) {
			if (hm == null) hm = new HashMap<>();
			List<TestMod> tl = hm.get(dp.getIDString());
			if (tl == null) {
				tl = new ArrayList<>(); 
				hm.put(dp.getIDString(), tl);
			}
			tl.add(test);
			count++;
		}
		public void remove(int pos) {
			if (hm == null) return;
			for (List<TestMod> tl:hm.values()) tl.remove(pos);
			count--;
		}
		// get test for the dataplane
		public TestMod get(VDataPlane dp, int pos) {
			if (hm == null) return null;
			List<TestMod> tl = hm.get(dp.getIDString());
			if (tl == null) return null;
			return tl.get(pos);
		}
		public List<TestMod> get(VDataPlane dp) {
			if (hm == null) return null;
			return hm.get(dp.getIDString());
		}
		// check if ns has mods
		boolean isModNS(int ns) {
			if (hm == null || ns < 0) return false;
			for (List<TestMod> tl:hm.values()) {
				for (TestMod t:tl) {
					if (t.isModNS(ns)) return true;
				}
			}
			return false;
		}
	}
	

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Modification Testing
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Test a set of modifications to the model with the dataset, getting results for each
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param tests modification test set
	 * @param dss testing dataset
	 * @param minInfo include minimal info in result set
	 * @return list of result sets for each test
	 */
	public static List<VResultSet> testSetsModify(VegML vML, String dimensionTag, String dataPlaneTag, TestModSet tests, VDataSets dss, boolean minInfo) {
		return testSetsModify(vML, dimensionTag, dataPlaneTag, tests, dss, minInfo, false);
	}
	
	/**
	 * Test a set of modifications to the model with the dataset, getting results for each
	 * Optionally retain correct value per position
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param tests modification test set
	 * @param dss testing dataset
	 * @param minInfo include minimal info in result set
	 * @param recordCorrect true to get correct value for each token
	 * @return list of result sets for each test
	 */
	public static List<VResultSet> testSetsModify(VegML vML, String dimensionTag, String dataPlaneTag, TestModSet tests, VDataSets dss, boolean minInfo, boolean recordCorrect) {
		return testSetsModify(vML, dimensionTag, dataPlaneTag, tests, dss, minInfo, recordCorrect, false);
	}
	
	/**
	 * Test a set of modifications to the model with the dataset, getting results for each
	 * Optionally retain correct value per position
	 * Optionally generate DsRetain dataset for retain testing
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param tests modification test set
	 * @param dss testing dataset
	 * @param minInfo include minimal info in result set
	 * @param recordCorrect true to get correct value for each token
	 * @param dsRetain true to generate full DsRetain data
	 * @return list of result sets for each test
	 */
	@SuppressWarnings("unchecked")
	public static List<VResultSet> testSetsModify(VegML vML, String dimensionTag, String dataPlaneTag, TestModSet tests, VDataSets dss, boolean minInfo, boolean recordCorrect, boolean dsRetain) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dss == null || dataPlane == null || dataPlane.getNSCount() < 1) return null;
		if (tests == null || tests.size() < 1) return null;
		
		int progressTick = 1, progressCnt = 0, progressLast = -1;
		if (vML.isCfgShowProgress()) {
			progressTick = VegML.getTickCount(dss.size());
		}

		VContext ctx = new VContext(vML);
		ctx.setModTests(tests);
		
		List<VFrame> frameList = new ArrayList<>();
		List<VResultSet> tsList = new ArrayList<>();		
		List<TestMod> nstest = tests.get(dataPlane);	
		
		for (int i=0;i<tests.size();i++) {
			VFrame fr = new VFrame();
			fr.init(dataPlane);
			frameList.add(fr);
			VResultSet ts = new VResultSet(dataPlane);
			if (recordCorrect) ts.turnOnRecordCorrect(); // if recording correct
			ts.start();
			tsList.add(ts);
		} 
		
		// make retained copy?, this does not account for mods a this time. (don't modify this test!)
		List<List<DsRetainVal>> dsrListSet = null;
		int tstRetain = -1;
		for (int x=0;x<tests.size();x++) {
			if (dsRetain) {
				dsrListSet = new ArrayList<>();
				tstRetain = x;
				break;
			}
		}
		// is identity modified?
		boolean isIdMod = tests.isModNS(dataPlane.getCfgNSIdentityNumber());
		
		// FIXME what if it is set to 0 for some: most tests include NS alone OR are getting the dsrListSet			
		HashMap<Accum, List<ValProb>> vpLHash = new HashMap<>();
		List<List<Long>> valueOutList = new ArrayList<>();		
		for (int i=0;i<tests.size();i++) {
			valueOutList.add(new ArrayList<>(VResultSet.DEF_VALOUT));
		}
		
		long [] inputNo = dataPlane.getIgnoreInputs();
		
		//////////////////
		// for each dataset
		int position = 0;
		for (int set=0;set<dss.size();set++) {
			VDataSet ds = dss.get(set);
			List<DsRetainVal> dsrList = null;
			if (dsrListSet != null) {
				dsrList = new ArrayList<>();
				dsrListSet.add(dsrList);
			}
			for (int x=0;x<tests.size();x++) {
				valueOutList.get(x).clear();
			}			
			if (!minInfo) {
				progressLast = VegML.showProgress(vML,progressCnt, progressTick, progressLast, "=");
				progressCnt++;
			}
					
			//////////////////
			// for each token in dataset
			for (int i=0;i<ds.size();i++, position++) {
				Long [] vvect = ds.getValueVD(i);
				long dvid = ds.getDataV(i);
				
				int pidx = VegUtil.contains(inputNo, position);

				if (pidx >= 0) {
					long valu = dataPlane.getIgnoreInputsValue(pidx);
					// framer says not this token
					for (int x=0;x<tests.size();x++) {
						tsList.get(x).addResponseNoEval(position, valu);
						valueOutList.get(x).add(valu);
					}
					if (dsrListSet != null) dsrList.add(null); // space filler	
					continue;
				}
				
				if (dataPlane.isCfgIdentityOnly() && !isIdMod && dsrListSet == null) {
					// PERFORMANCE: fast path for single values..
					// only if NO modification to Identity
					ValProb vp = dataPlane.getIdenityValProbIfSingle(dvid);
					if (vp != null) {
						// check response
						VFrame frame = frameList.get(0);
						frame.vpList.clear();
						frame.vpList.add(vp);
						boolean pass = VegUtil.isBestValProb(frame.vpList, vvect);
						
						for (int x=0;x<tests.size();x++) {
							VResultSet trs = tsList.get(x);
							// add response info
							trs.addResponse(vvect, vp.value, pass, vp.type, frame.vpList, minInfo);						

							// record vid (only first frame has Vids)
							if (recordCorrect) {
								// FIXME??
								trs.addResponseContains(position, vvect[0], frame.vpList, pass, dvid);
							} 
							valueOutList.get(x).add(vp.value);
						}																	
						continue;
					}
				}
				
				//////////////////
				// for each test make a frame
				boolean fini = false;
				for (int x=0;x<tests.size();x++) {
					if (x == 0) {
						// make the frame for the first one
						if (!dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, frameList.get(x), dataPlane.getFramerArg(), false, valueOutList.get(x), dss, set, i)) {
							fini = true;
						}
					} else if (fini) {	
						// not doing this one				
					} else {
						// copy the first frame (slow)
						frameList.get(x).copyFrom(frameList.get(0));
					}
				}
						
				if (fini) {
					// framer says not this token
					for (int x=0;x<tests.size();x++) {
						tsList.get(x).addResponseNoEval(position, vvect[0]);
						valueOutList.get(x).add((long)vvect[0]);
					}
					if (dsrListSet != null) dsrList.add(null); // space filler	
					continue;
				}
												
				// get the results: saved to frames in frame list
				dataPlane.getValPListModify(ctx, frameList, tests, valueOutList, false, vvect, false);
				
				// for each test record the results: online first frame has vectors
				VFrame vframe = frameList.get(0);
				
				for (int x=0;x<tests.size();x++) {
					VFrame frame = frameList.get(x);
					VResultSet trs = tsList.get(x);
					PredictionType fpt = frame.getPredictionType();
					// check response
					boolean pass = VegUtil.isBestValProb(frame.vpList, vvect);
					
					// add response info
					trs.addResponse(vvect, frame.getPredictionValue(), pass, fpt, frame.vpList, minInfo);						

					// record vid (only first frame has Vids)
					if (recordCorrect) {
						long vid = vframe.getVectSpace()[0];					
						if (nstest != null && nstest.get(x) != null && nstest.get(x).getSingleNS() >= 0) vid = vframe.getVectSpace()[nstest.get(x).getSingleNS()];					
						trs.addResponseContains(position, vvect[0], frame.vpList, pass, vid);
					} 
				}

				// add retain information
				if (dsrListSet != null) {
					DsRetainVal dsr = new DsRetainVal();
					// 0 frame has he complete accume set
					VFrame frame = frameList.get(0);
					dsr.cvalue = vvect;
					dsr.dvalue = frame.getPredictionValue();
					dsr.dpType = frame.getPredictionType();
					dsr.nsl = new List[frame.getAccumSpace().length];
					dsr.vid = new long[frame.getAccumSpace().length];
					for (int n=0;n<dsr.nsl.length;n++) {
						dsr.nsl[n] = null; 
						dsr.vid[n] = 0;
						if (frame.getAccumSpace()[n] != null) {
							dsr.vid[n] = frame.getAccumSpace()[n].vectorCode;
							dsr.nsl[n] = vpLHash.get(frame.getAccumSpace()[n]);
							if (dsr.nsl[n] == null) {
								dsr.nsl[n] = frame.getAccumSpace()[n].getValPsSorted();
								vpLHash.put(frame.getAccumSpace()[n], dsr.nsl[n]);
							}
							if (dsr.nsl[n] != null && dsr.nsl[n].size() < 1) dsr.nsl[n] = null;
						}
					}
					
					// account for identity filter		
					if (dataPlane.getCfgNSIdentityNumber() >= 0) {
						List<ValProb> ivl = dsr.nsl[dataPlane.getCfgNSIdentityNumber()];
						if (ivl != null) {
							if (dataPlane.isCfgIdentityOnly()) {
								if (ivl.size() < 1) {
									dsr.nsl[dataPlane.getCfgNSIdentityNumber()] = null;
								} else {
									for (int n=0;n<dsr.nsl.length;n++) {
										if (dsr.nsl[n] == null) continue;
										if (n == dataPlane.getCfgNSIdentityNumber()) continue;
										dsr.nsl[n] = new ArrayList<>(dsr.nsl[n]); // copy as the other is linked in many places
										VegUtil.remove(dsr.nsl[n], ivl);
										if (dsr.nsl[n].size() < 1) dsr.nsl[n] = null;
									}
								}
							}
							dsr.baseCnt = ivl.size();
						} else {
							dsr.baseCnt = -1;
						}
					} else {
						dsr.baseCnt = -1;						
					}
					dsrList.add(dsr);
				}
			}
		}
		
		vpLHash.clear();
		
		// end it
		for (int i=0;i<tests.size();i++) {
			tsList.get(i).end();
			if (!minInfo) tsList.get(i).valueOut = valueOutList.get(i);
		}
		if (dsrListSet != null) {
			tsList.get(tstRetain).setRetainedResolutionSet(dsrListSet);
		}		
		
		if (vML.isCfgShowProgress()) System.out.println("");
		return tsList;
	}

	
	/**
	 * Get Predict response information for a single input string, with a modification
	 * 
	 * @param ctx context for evaluation
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param test modifcation test set
	 * @param valueOut history of result valuesIds
	 * @param dataValue data input value 
	 * @return Frame with results
	 */
	public static VFrame predictFrameVPFrameModify(VContext ctx, String dimensionTag, String dataPlaneTag, TestMod test, List<Long> valueOut, String dataValue) {	
		if (ctx == null) return null;
		if (ctx.getVegML().getInCount() == 0) return null;
		VDataPlane dataPlane = ctx.getVegML().getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;

		// get list of all values and probability
		VFrame frame = new VFrame();
		// make dataSets
		VDataSets dss = new VDataSets(new VDataSet(dataValue));
		if (!dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, frame, dataPlane.getFramerArg(), true, valueOut, dss, 0, 0)) return null;
		
	//	dataPlane.getValPList(ctx, frame, valueOut, false, null, false);				
	//	etValPList(VContext ctx, VFrame frame, List<Long> valueOut, boolean nodefaults, Long [] valueIds, boolean segment) {
		dataPlane.getValPListModify(ctx, frame, test, valueOut, false, null, false);
		if (frame.vpList.size() < 1) return null;
		return frame;
	}
	
	
	/**
	 * Get Predict response information for an input frame, with a modification
	 * 
	 * @param ctx context for evaluation
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param test modification test set
	 * @param valueOut history of result valuesIds
	 * @param frame data input in frame
	 * @return Frame with results
	 */
	public static VFrame predictVPFrameModify(VContext ctx, String dimensionTag, String dataplaneTag, TestMod test, List<Long> valueOut, VFrame frame) {
		if (ctx == null) return null;
		if (ctx.getVegML().getInCount() == 0) return null;
		VDataPlane dataPlane = ctx.getVegML().getDataPlane(dimensionTag, dataplaneTag);
		if (dataPlane == null) return null;
		
		VFrame nframe = new VFrame();
		if (!dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, nframe, dataPlane.getFramerArg(), true, valueOut, frame.getDataSet(), 
												frame.getDataSetNumber(), frame.getDataSetPosition())) return null;	
		dataPlane.getValPListModify(ctx, nframe, test, valueOut, false, null, false);

		return nframe;
	}	
	
	/**
	 * Test and generate full DsRetain data for retain set testing
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dss test datasets
	 * @return result set
	 */
	public static VResultSet testSetsDsRetain(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dss == null || dataPlane == null) return null;
		TestModSet dsrTest = new TestModSet();
		dsrTest.add(dataPlane, new TestMod(dataPlane.getCfgNSWeightRaw()));
		List<VResultSet> dsrL = VegTest.testSetsModify(vML, dataPlane.getDimensionTag(), dataPlane.getTag(), dsrTest, dss, false, false, true);
		return dsrL.get(0);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Predict -> tests without compare
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Predict results for a dataset as a sequence
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dss test dataset
	 * @return result set
	 */
	public static VResultSet predictSets(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss) {
		return predictSets(vML, dimensionTag, dataPlaneTag, dss, 0);
	}
	
	/**
	 * Predict results for a dataset as a sequence
	 * Optionally retain vplist per token
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag  dimension tag/name
	 * @param dataPlaneTag  dataplane tag/name
	 * @param dss test dataset
	 * @param maxVpListSize max vplist to save per position or -1 for all
	 * @return result set
	 */
	public static VResultSet predictSets(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, int maxVpListSize) {		
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dss == null || dataPlane == null || dataPlane.getNSCount() < 1) return null;

		int progressTick = 1, progressCnt = 0, progressLast = -1;
		if (vML.isCfgShowProgress()) {
			progressTick = VegML.getTickCount(dss.size());
		}
		
		VResultSet ts = new VResultSet(dataPlane);
		ts.start();
		ts.responseOut = new ArrayList<>();
		ts.responseVpOut = new ArrayList<>();		
		VContext ctx = new VContext(vML);

		for (int set=0;set<dss.size();set++) {
			VDataSet ds = dss.get(set);
			List<Long> valueOut = new ArrayList<>(ds.size());
			List<List<ValProb>> valueVpOut = null;
			if (maxVpListSize != 0) valueVpOut = new ArrayList<>(ds.size());
			
			progressLast = VegML.showProgress(vML,progressCnt, progressTick, progressLast, "=");
			progressCnt++;
			VFrame frame = new VFrame();
			frame.init(dataPlane);
			
			// set value size if they didn't prior
			for (int i=0;i<ds.size();i++) {			
				if (dataPlane.isCfgIdentityOnly()) {
					// PERFORMANCE: fast path for single values..
					long dvid = ds.getDataV(i);
					ValProb vp = dataPlane.getIdenityValProbIfSingle(dvid);
					if (vp != null) {
						frame.vpList.clear();
						frame.vpList.add(vp);
						testAnswerFocus(ctx, dataPlane, frame, valueOut, null, SaveStats.NoSave);
						// add results copy
						if (maxVpListSize != 0) {
							valueVpOut.add(VegUtil.copy(frame.vpList, maxVpListSize));
							frame.vpList.clear();
						}
						continue;
					}
				}
				if (!dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, frame, dataPlane.getFramerArg(), true, valueOut, dss, set, i)) {
					valueOut.add((long)VegML.emptyVect);
					if (maxVpListSize != 0) valueVpOut.add(new ArrayList<ValProb>()); // empty list
					continue;
				}	
				testAnswerFocus(ctx, dataPlane, frame, valueOut, null, SaveStats.NoSave);
				// add results copy
				if (maxVpListSize != 0) {
					valueVpOut.add(VegUtil.copy(frame.vpList, maxVpListSize));
					frame.vpList.clear();
				}
				ts.total++;
			}
			// add simple result 
			
			// add complex result
			if (maxVpListSize != 0) {
				ts.responseVpOut.add(valueVpOut);
				valueOut.clear();
			}
			else ts.responseOut.add(valueOut);
		}
		ts.end();
		if (vML.isCfgShowProgress()) System.out.println("");
		return ts;
	}

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Predict Value
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * get most best prediction as a string, must be string type and have strings retained in model
	 * @param ctx
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param valueOut
	 * @param dataSet
	 * @param position
	 * @param dataSetNumber
	 * @return
	 */
	public static String predict(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, VDataSet dataSet, int position, int dataSetNumber) {	
		if (ctx == null) return null;
		List<ValProb> vpList = new ArrayList<>();
		ctx.getVegML().getValProbs(ctx, dimensionTag, dataPlaneTag, valueOut, dataSet, position, dataSetNumber, vpList);
		if (vpList.size() < 1) return null;
		VDataPlane dp = ctx.getVegML().getDataPlane(dimensionTag, dataPlaneTag);
		return dp.getString(vpList.get(0).value);
	}
	
	/**
	 * predict for a string as input for framing
	 * @param ctx
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param valueOut
	 * @param dataSet
	 * @param position
	 * @param dataSetNumber
	 * @return
	 */
	public static List<ValProb> predictVPLS(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, List<String> dataSet, int position, int dataSetNumber) {	
		if (ctx == null) return null;
		List<ValProb> vpList = new ArrayList<>();		
		ctx.getVegML().getValProbs(ctx, dimensionTag, dataPlaneTag, valueOut, new VDataSet(dataSet), position, dataSetNumber, vpList);
		if (vpList.size() < 1) return null;
		return vpList;
	}
	
	/**
	 * 
	 * @param ctx
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param valueOut
	 * @param dataSet
	 * @param position
	 * @param dataSetNumber
	 * @return
	 */
	public static List<ValProb> predictVP(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, VDataSet dataSet, int position, int dataSetNumber) {	
		if (ctx == null) return null;
		List<ValProb> vpList = new ArrayList<>();
		ctx.getVegML().getValProbs(ctx, dimensionTag, dataPlaneTag, valueOut, dataSet, position, dataSetNumber, vpList);
		if (vpList.size() < 1) return null;
		return vpList;
	}
	
	/**
	 * 
	 * @param ctx
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param valueOut
	 * @param frame
	 * @return
	 */
	public static List<ValProb> predictVP(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, VFrame frame) {	
		if (ctx == null) return null;
		List<ValProb> vpList = new ArrayList<>();
		ctx.getVegML().getValProbs(ctx, dimensionTag, dataPlaneTag, valueOut, frame.getDataSet(frame.getDataSetNumber()), frame.getDataSetPosition(), frame.getDataSetNumber(), vpList);
		if (vpList.size() < 1) return null;
		return vpList;
	}
	
	/**
	 * predict for a frame AND get a new frame back
	 * @param ctx context for this resolution
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param valueOut list of previous results or null
	 * @param frame
	 * @return
	 */
	public static VFrame predictVPFrame(VContext ctx, String dimensionTag, String dataplaneTag, List<Long> valueOut, VFrame frame) {
		if (ctx == null) return null;
		if (ctx.getVegML().getInCount() == 0) return null;
		VDataPlane dataPlane = ctx.getVegML().getDataPlane(dimensionTag, dataplaneTag);
		if (dataPlane == null) return null;
		
		VFrame nframe = new VFrame();
		nframe.init(dataPlane);
		if (!dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, nframe, dataPlane.getFramerArg(), true, valueOut, frame.getDataSet(), 
												frame.getDataSetNumber(), frame.getDataSetPosition())) return null;	
		dataPlane.getValPList(ctx, nframe, valueOut, false, null, false);
		return nframe;
	}
		
	/**
	 * predict for a frame
	 * 
	 * @param ctx context for this resolution
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param valueOut list of previous results or null
	 * @param dataValue
	 * @return
	 */
	public static List<ValProb> predictFrameVP(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, String dataValue) {	
		VFrame fr = predictFrameVPFrame(ctx, dimensionTag, dataPlaneTag, valueOut, dataValue);				
		if (fr == null || fr.vpList.size() < 1) return null;
		return VegUtil.copy(fr.vpList);
	}
	
	/**
	 * predict for a frame
	 * 
	 * @param ctx context for this resolution
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param valueOut list of previous results or null
	 * @param dataValue
	 * @return
	 */
	public static VFrame predictFrameVPFrame(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, String dataValue) {	
		if (ctx == null) return null;
		if (ctx.getVegML().getInCount() == 0) return null;
		VDataPlane dataPlane = ctx.getVegML().getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;

		// get list of all values and probability
		VFrame frame = new VFrame();
		frame.init(dataPlane);
		// make dataSets
		VDataSets dss = new VDataSets(new VDataSet(dataValue));
		if (!dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, frame, dataPlane.getFramerArg(), true, valueOut, dss, 0, 0)) return null;
		dataPlane.getValPList(ctx, frame, valueOut, false, null, false);				
		if (frame.vpList.size() < 1) return null;
		return frame;
	}
	
	/**
	 * Get Prediction results list for defined dataframe with a specific dataset
	 * 
	 * @param ctx context for this resolution
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param valueOut list of previous results or null
	 * @param dataSet
	 * @param position
	 * @param dataFrame input data aligned for a frame in this dataplane
	 * @return
	 */
	public List<ValProb> predictFrameVP(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, VDataSet dataSet, int position, List<String> dataFrame) {	
		if (ctx == null) return null;
		if (ctx.getVegML().getInCount() == 0) return null;
		VDataPlane dataplane = ctx.getVegML().getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return null;

		// get list of all values and probability
		VFrame frame = new VFrame();
		frame.init(dataplane);
		frame.setValues(dataplane, null, dataSet, 0, position);

		dataplane.getValPList(ctx, frame, valueOut, false, null, false);		
		if (frame.vpList.size() < 1) return null;
		return VegUtil.copy(frame.vpList);
	}
	
	/**
	 * Get prediction results for a dataplane given a defined dataFrame
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param dataFrame input data aligned for a frame in this dataplane
	 * @return list of results
	 */
	public static List<ValProb> predictFrameVP(VegML vML, String dimensionTag, String dataPlaneTag, List<String> dataFrame) {	
		VFrame frame = predictFrameVPFrame(new VContext(vML), dimensionTag, dataPlaneTag, null, dataFrame);		
		if (frame == null || frame.vpList.size() < 1) return null;
		return VegUtil.copy(frame.vpList);
	}

	/**
	 * Get prediction results for a dataplane given a defined dataFrame
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param dataFrame input data aligned for a frame in this dataplane
	 * @return frame with results in vplist and additional information
	 */
	public static VFrame predictFrameVPFrame(VegML vML, String dimensionTag, String dataPlaneTag, List<String> dataFrame) {	
		return predictFrameVPFrame(new VContext(vML), dimensionTag, dataPlaneTag, null, dataFrame);		
	}
	
	/**
	 * Get prediction results for a dataplane given a defined dataFrame
	 * 
	 * @param ctx context for this resolution
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param dataFrame input data aligned for a frame in this dataplane
	 * @return list of results
	 */
	public static List<ValProb> predictFrameVP(VContext ctx, String dimensionTag, String dataPlaneTag, List<String> dataFrame) {	
		VFrame frame = predictFrameVPFrame(ctx, dimensionTag, dataPlaneTag, null, dataFrame);		
		if (frame == null || frame.vpList.size() < 1) return null;
		return VegUtil.copy(frame.vpList);
	}

	/**
	 * Get prediction results for a dataplane given a defined dataFrame
	 * 
	 * @param ctx context for this resolution
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param dataFrame input data aligned for a frame in this dataplane
	 * @return frame with results in vplist and additional information
	 */
	public static VFrame predictFrameVPFrame(VContext ctx, String dimensionTag, String dataPlaneTag, List<String> dataFrame) {	
		return predictFrameVPFrame(ctx, dimensionTag, dataPlaneTag, null, dataFrame);		
	}
	
	/**
	 * Get prediction results for a dataplane given a defined dataFrame and list of prior result values (if any)
	 * 
	 * @param ctx context for this resolution
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param valueOut list of previous results or null
	 * @param dataFrame input data aligned for a frame in this dataplane
	 * @return list of results
	 */
	public static List<ValProb> predictFrameVP(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, List<String> dataFrame) {	
		VFrame frame = predictFrameVPFrame(ctx, dimensionTag, dataPlaneTag, valueOut, dataFrame);		
		if (frame == null || frame.vpList.size() < 1) return null;
		return VegUtil.copy(frame.vpList);
	}
	
	/**
	 * Get prediction results for a dataplane given a defined dataFrame and list of prior result values (if any)
	 * 
	 * @param ctx context for this resolution 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param valueOut list of previous results or null
	 * @param dataFrame input data aligned for a frame in this dataplane
	 * @return frame with results in vplist and additional information
	 */
	public static VFrame predictFrameVPFrame(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, List<String> dataFrame) {	
		if (ctx == null) return null;
		if (ctx.getVegML().getInCount() == 0) return null;
		VDataPlane dataplane = ctx.getVegML().getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return null;

		if (valueOut == null) valueOut = new ArrayList<>();
		
		// get list of all values and probability
		VFrame frame = new VFrame();
		frame.init(dataplane);
		frame.setValuesLS(dataplane, null, dataFrame, 0, 0);
		dataplane.getValPList(ctx, frame, valueOut, false, null, false);		
		if (frame.vpList.size() < 1) return null;
		return frame;
	}	
	
	
	/**
	 * Get prediction results for a dataplane given a defined dataFrame
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param dataFrame input data aligned for a frame in valueIds
	 * @return list of results
	 */
	public static List<ValProb> predictFrameVPV(VegML vML, String dimensionTag, String dataPlaneTag, List<Long> dataFrameV) {	
		VFrame frame = predictFrameVPVFrame(new VContext(vML), dimensionTag, dataPlaneTag, null, dataFrameV);		
		if (frame == null || frame.vpList.size() < 1) return null;
		return VegUtil.copy(frame.vpList);
	}

	/**
	 * Get prediction results for a dataplane given a defined dataFrame
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param dataFrame input data aligned for a frame in valueIds
	 * @return frame with results in vplist and additional information
	 */
	public static VFrame predictFrameVPVFrame(VegML vML, String dimensionTag, String dataPlaneTag, List<Long> dataFrameV) {	
		return predictFrameVPVFrame(new VContext(vML), dimensionTag, dataPlaneTag, null, dataFrameV);		
	}
	
	/**
	 * Get prediction results for a dataplane given a defined dataFrame
	 * 
	 * @param ctx context for this resolution
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param dataFrame input data aligned for a frame in valueIds
	 * @return list of results
	 */
	public static List<ValProb> predictFrameVPV(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> dataFrameV) {	
		VFrame frame = predictFrameVPVFrame(ctx, dimensionTag, dataPlaneTag, null, dataFrameV);		
		if (frame == null || frame.vpList.size() < 1) return null;
		return VegUtil.copy(frame.vpList);
	}

	/**
	 * Get prediction results for a dataplane given a defined dataFrame
	 * 
	 * @param ctx context for this resolution
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param dataFrame input data aligned for a frame in valueIds
	 * @return frame with results in vplist and additional information
	 */
	public static VFrame predictFrameVPVFrame(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> dataFrameV) {	
		return predictFrameVPVFrame(ctx, dimensionTag, dataPlaneTag, null, dataFrameV);		
	}
	
	/**
	 * Get prediction results for a dataplane given a defined dataFrame and list of prior result values (if any)
	 * 
	 * @param ctx context for this resolution
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param valueOut list of previous results or null
	 * @param dataFrameV input data aligned for a frame in valueIds
	 * @return list of results
	 */	
	public static List<ValProb> predictFrameVPV(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, List<Long> dataFrameV) {	
		if (ctx == null) return null;
		if (ctx.getVegML().getInCount() == 0) return null;
		VDataPlane dataplane = ctx.getVegML().getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return null;

		// get list of all values and probability
		VFrame frame = predictFrameVPVFrame(ctx, dimensionTag, dataPlaneTag, valueOut, dataFrameV);
		if (frame.vpList.size() < 1) return null;
		return VegUtil.copy(frame.vpList);
	}
	
	/**
	 * Get prediction results for a dataplane given a defined dataFrame and list of prior result values (if any)
	 * 
	 * @param ctx context for this resolution 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param valueOut list of previous results or null
	 * @param dataFrame input data aligned for a frame in valueIds
	 * @return frame with results in vplist and additional information
	 */
	public static VFrame predictFrameVPVFrame(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, List<Long> dataFrameV) {	
		if (ctx == null) return null;
		if (ctx.getVegML().getInCount() == 0) return null;
		VDataPlane dataplane = ctx.getVegML().getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return null;

		// get list of all values and probability
		VFrame frame = new VFrame();
		frame.init(dataplane);
		frame.setValuesLV(dataplane, null, dataFrameV, 0, 0);
		dataplane.getValPList(ctx, frame, valueOut, false, null, false);		
		return frame;
	}
	
	/**
	 * predict for a frame
	 * @param ctx context for this resolution 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param valueOut list of previous results or null
	 * @param dataSet test dataset
	 * @param position position in dataSet for focus
	 * @return
	 */
	public static String predictFrameSet(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, List<String> dataSet, int position) {	
		if (ctx == null) return null;
		List<ValProb> vpList = predictFrameSetVP(ctx, dimensionTag, dataPlaneTag, valueOut, dataSet, position);
		if (vpList == null) return null;
		VDataPlane dataplane = ctx.getVegML().getDataPlane(dimensionTag, dataPlaneTag);
		return dataplane.getString(vpList.get(0).value);
	}
	
	/**
	 * predict result list for a frame 
	 * 
	 * @param ctx context for this resolution 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param valueOut list of previous results or null
	 * @param dataSet test dataset
	 * @param position position in dataSet for focus
	 * @return
	 */
	public static List<ValProb> predictFrameSetVP(VContext ctx, String dimensionTag, String dataPlaneTag, List<Long> valueOut, List<String> dataSet, int position) {	
		return predictFrameVP(ctx, dimensionTag, dataPlaneTag, valueOut, dataSet.get(position));
	}

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// SEGMENT predict value
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////	

	/**
	 * get best prediction only for segment, if strings and saved in model
	 * 
	 * @param ctx context for this resolution 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param dss test datasets
	 * @return best prediction or null 
	 */
	public static String predictSegment(VContext ctx, String dimensionTag, String dataPlaneTag, VDataSets dss) {
		if (ctx == null || dss == null) return null;
		VDataPlane dataplane = ctx.getVegML().getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null ) return null;	
		List<ValProb> vpList = predictSegmentSet_int(ctx, dataplane, true, dss, 0);
		if (vpList == null) return null;
		return dataplane.getString(vpList.get(0).value);
	}

	/**
	 * get value probability list of the segment, first is best
	 * 
	 * @param ctx context for this resolution 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane data tag
	 * @param dss test datasets
	 * @return list of predictions
	 */
	public static List<ValProb> predictSegmentVP(VContext ctx, String dimensionTag, String dataPlaneTag, VDataSets dss) {
		if (ctx == null || dss == null) return null;
		VDataPlane dataplane = ctx.getVegML().getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null ) return null;		
		return predictSegmentSet_int(ctx, dataplane, true, dss, 0);
	}

	// internal
	private static List<ValProb> predictSegmentSet_int(VContext ctx, VDataPlane dataplane, boolean predict, VDataSets dss, int set) {
		if (ctx == null) return null;
		List<ValProb> responseVpList = null;		
		
		// just iterate through each for now
		int vCnt = 0, missCnt = 0;
		VFrame frame = new VFrame();
		frame.init(dataplane);
		
		VDataSet dataSet = dss.get(set);
		List<Long> valueOut = new ArrayList<>(dataSet.size());
		
		/*
		////////////
		// first check if this is a known segment
		if (testKnown) {
			long vid = dataplane.haveDimensionString(segment);
			if (vid != 0) {
				// just get the identity
				NumberSetHash nsh = dataplane.getNSHash(dataplane.getCfgNSIdentityNumber());	
				Accum iac = nsh.get(vid);
				if (iac != null) return iac.getValPsSorted();
			}
		}
		*/
		

		////////////
		// for each token in the segment
		for (int i=0;i<dataSet.size();i++) {
			boolean frm = true;
			frm = dataplane.getFramer().makeFrameSetup(ctx, dataplane, frame, dataplane.getFramerArg(), predict, valueOut, dss, set, i);
			if (!frm) {
				//ts.addResponseNoEval(valueId);		
				continue;				
			}
			// get the results for this token in the segment
			// AMP option? or callouts
			PredictionType ret = dataplane.getValPList(ctx, frame, valueOut, true, null, true);
			//		System.out.println("SEG["+seg+"]("+vpList.size()+") " + ret + " ["+vpList.get(0).value+"]!=["+vvect[0]+"] " + vpList.get(0).probability);
		
			// neg for each positive?			
			if (frame.vpList.size() < 1) {
				// get no vectors for a recall when empty values / empty primary values
				missCnt++;
				//System.out.println("SEG["+set+"]["+i+"] (0)("+frame.getVectorCount()+"/"+frame.getAccumCount()+") => NONE" );
				//frame.printFrameDebug();
				continue;
			} 
			if (dataplane.isBaseLineBooleanMode()) {
	//			// probs over 1 most always?
	//			System.out.println("SEG["+set+"]["+i+"] ("+frame.vpList.size()+")("+frame.getVectorCount()+"/"+frame.getAccumCount()+") => " + frame.vpList.get(0).value + " / "+frame.vpList.get(0).count+" <= "+frame.vpList.get(0).probability);
			}
			
			// retain all and make list
			if (responseVpList == null) responseVpList = new ArrayList<>();
			
			vCnt++;	
			// classification-average - average the results from all elements, option to exclude locked or default values
			responseVpList = VegUtil.mergeVPListOnly(responseVpList, frame.vpList);
		}
		
		////////////
		// if Default only
		if (responseVpList == null || responseVpList.size() < 1) {
			// This happens only if there is NO matching data at all
			Accum vaDef = dataplane.getAccumDefault();
			//System.out.println(" NONE[@] "); 

			//System.out.println(" predictSegment_NO["+dataPlane.getDimensionTag()+"/"+dataplane.getTag()+"] ["+segment.size()+"] " + vCnt + " dac: " + vaDef.getDistString());
			return vaDef.getValPsSorted();
		}
		
		// responseVpList an average for each value accross all responses
		for (int i=0;i<responseVpList.size();i++) {
			ValProb svp = responseVpList.get(i);
			svp.probability = svp.probability / svp.counter;
		}
		
		// sort for best
		Collections.sort(responseVpList, VegUtil.VpSort);	
		
		return responseVpList;
	}
	

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// get amplified 
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * get amplified results for a valSet
	 * vplist in frame is updated
	 * 
	 * @param ctx context for this resolution 
	 * @param dp dataplane to get amplified results from, must be same as initial results
	 * @param frame frame of initial results
	 * @param valueOut list of previous results or null
	 * @param valSet set of result valueIds to amplify
	 * @param limitSet  
	 * @param noiseLimitFocus noise limit on non-context numberSets, -1 for natural
	 * @param noiseLimitContext noise limit on context numberSets, -1 for natural
	 * @return prediction type
	 */
	public static PredictionType getValAmp(VContext ctx, VDataPlane dp, VFrame frame, List<Long> valueOut, long [] valSet, boolean limitSet, int noiseLimitFocus, int noiseLimitContext) {
		return VegTest.getValAmp(ctx, dp, frame, valueOut, valSet, limitSet, noiseLimitFocus, noiseLimitContext, 1.0);
	}

	/**
	 * get amplified results for a valSet
	 * vplist in frame is updated
	 * 
	 * @param ctx context for this resolution 
	 * @param dp dataplane to get amplified results from, must be same as initial results
	 * @param frame frame of initial results
	 * @param valueOut list of previous results or null
	 * @param valSet set of result valueIds to amplify
	 * @param limitSet
	 * @param noiseLimitFocus noise limit on non-context numberSets, -1 for natural
	 * @param noiseLimitContext noise limit on context numberSets, -1 for natural
	 * @param ampIdentity amount to amplify Identity numberSet, -1 for natural
	 * @return
	 */
	public static PredictionType getValAmp(VContext ctx, VDataPlane dp, VFrame frame, List<Long> valueOut, long [] valSet, boolean limitSet, int noiseLimitFocus, int noiseLimitContext, double ampIdentity) {
		PredictionType pt = dp.getValPList(ctx, frame, valueOut, false, valSet, limitSet, noiseLimitFocus, noiseLimitContext, ampIdentity);
		Collections.sort(frame.getVPList(), VegUtil.VpSort);	
		return pt;
	}
	
	
	/**
	 * Get prediction from a framed frame for the dataplane
	 * @param ctx context of evaluation
	 * @param dp dataplane to use
	 * @param frame framed frame of data
	 * @param rs resultSet in use
	 * @return PredictionType
	 */
	public static PredictionType getPredictionFramed(VContext ctx, VDataPlane dp, VFrame frame, VResultSet rs) {		
		PredictionType ret =  dp.getValPList(ctx, frame, rs.valueOut, false, null, false);
		return testAnswerFocusResult(ctx, dp, frame, rs.valueOut, frame.vpList, ret, null, false, SaveStats.NoSave);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Test correct answers at position, train predict OR recall
	//	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	static PredictionType testAnswerFocus(VContext ctx, VDataPlane dp, VFrame frame, List<Long> valueOut, Long [] valueId, SaveStats saveType) {		
		PredictionType ret = dp.getValPList(ctx, frame, valueOut, false, valueId, false);	
		return testAnswerFocusResult(ctx, dp, frame, valueOut, frame.vpList, ret, valueId, false, saveType);
	}

	static PredictionType testAnswerFocusSegment(VContext ctx, VDataPlane dp, boolean predict, List<Long> valueOut, VDataSets dss, int set, Long [] valueId, List<ValProb> vpListOut, SaveStats saveType) {
		List<ValProb> vpList = VegTest.predictSegmentSet_int(ctx, dp, predict, dss, set);
		vpListOut.addAll(vpList);
		return testAnswerFocusResult(ctx, dp, null, valueOut, vpList, PredictionType.Predict, valueId, true, saveType);	
	}

	private static PredictionType testAnswerFocusResult(VContext ctx, VDataPlane dp, VFrame frame, List<Long> valueOut, List<ValProb> vpList, PredictionType ret, Long [] valueId, boolean segment, SaveStats saveType) {
		if (saveType == SaveStats.SavePredict || saveType == SaveStats.SavePredictNoReset) {
			dp.getPredictAnswerAccum().addCount(valueId[0]);	
			if (VegUtil.isBestValProb(vpList, valueId)) { 
				dp.getPredictAnswerAccum().addCrtCount(valueId[0]);	
				trainMissAccums(dp, ret, true, true);
				return ret;
			} else {
				trainMissAccums(dp, ret, false, true);
			}
		} else if (saveType == SaveStats.SaveRecall || saveType == SaveStats.SaveRecallNoReset) {
			dp.getRecallAnswerAccum().addCount(valueId[0]);	
			if (VegUtil.isBestValProb(vpList, valueId)) { 
				dp.getRecallAnswerAccum().addCrtCount(valueId[0]);	
				trainMissAccums(dp, ret, true, false);				
				return ret;
			} else {
				trainMissAccums(dp, ret, false, false);
			}
		} else if (valueId == null || valueId[0] == 0) {
			return ret;
		} else if (VegUtil.isBestValProb(vpList, valueId)) { 
			trainMissAccums(dp, ret, true, false);				
			return ret;
		}
		return PredictionType.None;
	}
	static private void trainMissAccums(VDataPlane dp, PredictionType ptype, boolean correct, boolean trainPredict) {
		// most correct
		if (ptype == PredictionType.Recall) {
			// RECALL
			if (trainPredict) dp.incPredictRecallCount(correct);
			else dp.incRecallRecallCount(correct);
		} else if (ptype == PredictionType.Predict) {	
			// PREDICT
			if (trainPredict) dp.incPredictPredictCount(correct);
			else dp.incRecallPredictCount(correct);
		} else if (ptype == PredictionType.PredictRelate) {	
			// PREDICT
			if (trainPredict) dp.incPredictPredictRelateCount(correct);
			else dp.incRecallPredictRelateCount(correct);
		} else if (ptype == PredictionType.PredictUnknown) {	
			if (trainPredict) dp.incPredictUnknownCount(correct);
			else dp.incRecallPredictCount(correct);
		} else if (ptype == PredictionType.RecallPredict) {	
			//recallPREDICT
			if (trainPredict) dp.incPredictRecallPredictCount(correct);
			else dp.incRecallRecallPredictCount(correct);
		} else {	
			if (trainPredict) dp.incPredictDefaultCount(correct);
			else dp.incRecallDefaultCount(correct);
		}	
	}

}
