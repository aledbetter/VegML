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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vegml.Data.VDataSet;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;
import vegml.Data.VectorToVid;


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// DATA SET training
//
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class VegTrain {
	

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// SINGLE Frame
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Data value training from exact frame set
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param ts resultSet to add to
	 * @param dataFrame data slotted for the frames
	 * @param valueid value to train to
	 */
	public static void trainDataFrameS(VegML vML, String dimensionTag, String dataPlaneTag, VResultSet ts, List<String> dataFrame, final Long [] valueid) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataFrame == null || dataPlane == null) return;		
		VFrame frame = new VFrame();
		frame.setValuesLS(dataPlane, null, dataFrame, 0, 0);
		trainFocus(dataPlane, frame, valueid);
		if (ts != null) ts.total++;
	}
	
	/**
	 * Data value training from exact frame set
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param ts resultSet to add to
	 * @param dataFrame data slotted for the frames
	 * @param value value to train to
	 */
	public static void trainDataFrameS(VegML vML, String dimensionTag, String dataPlaneTag, VResultSet ts, List<String> dataFrame, final String value) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataFrame == null || dataPlane == null) return;		
		VFrame frame = new VFrame();
		frame.setValuesLS(dataPlane, null, dataFrame, 0, 0);
		Long valueid [] = new Long[1];
		valueid[0] = (long)dataPlane.getCfgVToV().toVectGen(value);
		trainFocus(dataPlane, frame, valueid);
		if (ts != null) ts.total++;
	}
	
	
	/**
	 * Data value training from exact frame set
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param ts resultSet to add to
	 * @param dataFrameV data slotted for the frames with valueIds
	 * @param valueId value to train to
	 */
	public static void trainDataFrameL(final VegML vML, final String dimensionTag, final String dataPlaneTag, VResultSet ts, List<Long> dataFrameV, final Long [] valueId) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataFrameV == null || dataPlane == null) return;		
		VFrame frame = new VFrame();
		frame.setValuesLV(dataPlane, null, dataFrameV, 0, 0);
		trainFocus(dataPlane, frame, valueId);
		if (ts != null) ts.total++;
	}
	
	
	/**
	 * Data value training from exact frame set
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param ts resultSet to add to
	 * @param dataFrameV data slotted for the frames with array valueIds
	 * @param valueid value to train to
	 */
	public static void trainDataFrameLD(VegML vML, String dimensionTag, String dataPlaneTag, VResultSet ts, List<Long []> dataFrameV, Long [] valueid) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataFrameV == null || dataPlane == null) return;		
		VFrame frame = new VFrame();
		frame.setValuesLVD(dataPlane, null, dataFrameV, 0, 0);
		trainFocus(dataPlane, frame, valueid);
		if (ts != null) ts.total++;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Datasets
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Train dataplane with dataset using definition in dataset
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param dss dataset with definition
	 * @return result set
	 */
	public static VResultSet train(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss) {
		return train(vML, dimensionTag, dataPlaneTag, dss, false);
	}
	
	
	/**
	 * Train dataplane with dataset using definition in dataset, with threaded flag
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param dss dataset with definition
	 * @return result set
	 */
	public static VResultSet train(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, boolean threaded) {
		switch (dss.getDefinition().getDataStructure()) {
		case Segment:
			return trainSegments(vML, dimensionTag, dataPlaneTag, dss, threaded);
		case Stream:
			return trainStreams(vML, dimensionTag, dataPlaneTag, dss, threaded);
		case Block:
			return trainBlocks(vML, dimensionTag, dataPlaneTag, dss, threaded);
		default:
			return trainDataSets(vML, dimensionTag, dataPlaneTag, dss, threaded);
		}
	}

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Datasets
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Train dataplane with dataset as sequence
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param dss dataset 
	 * @return result set
	 */
	public static VResultSet trainDataSets(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss) {
		return trainDataSets(vML, dimensionTag, dataPlaneTag, dss, false);
	}
	
	/**
	 * Train dataplane with dataset as sequence, with threaded flag
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param dss dataset 
	 * @return result set
	 */
	public static VResultSet trainDataSets(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, boolean threaded) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dss == null || dataPlane == null) return null;
		
		int progressTick = 1, progressCnt = 0, progressLast = -1;
		if (vML.isCfgShowProgress()) {
			System.out.println("-trainDataSets["+dss.size()+"] w["+dataPlane.getCfgWindowSize()+"] dd["+dataPlane.getDimensionTag()+"/"+dataPlane.getTag()+"] val["+dataPlane.getCfgDataWidth()+"]");
			progressTick = VegML.getTickCount(dss.size());
		}
		VResultSet ts = new VResultSet(dataPlane);
		VContext ctx = new VContext(vML);
		ts.start();
		VFrame frame = new VFrame();
		
		// use vals for frame data				
		for (int set=0;set<dss.size();set++) {
			progressLast = VegML.showProgress(vML, progressCnt, progressTick, progressLast, "+");
			progressCnt++;
			
			// set value size if they didn't prior
			VDataSet ds = dss.get(set);
			List<Long> valueOut = new ArrayList<>(ds.size());
			
			for (int i=0;i<ds.size();i++) {
				Long [] valueId = ds.getValueVD(i); 			
				if (dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, frame, dataPlane.getFramerArg(), false, valueOut, dss, set, i)) {
					trainFocus(dataPlane, frame, valueId);
					ts.total++;
				} 
				valueOut.add(valueId[0]);
			}
		}
		if (!threaded) dataPlane.removeAllEmptyAccum();
		ts.end();
		if (vML.isCfgShowProgress()) System.out.println("");	
		return ts;
	}
	

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// SEGMENTS
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Train dataplane with dataset as segments
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param dss dataset 
	 * @return result set
	 */
	public static VResultSet trainSegments(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss) {
		return trainSegments(vML, dimensionTag, dataPlaneTag, dss, false);
	}
	
	/**
	 * Train dataplane with dataset as segments, with threaded flag
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param dss dataset 
	 * @return result set
	 */
	public static VResultSet trainSegments(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, boolean threaded) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dss == null || dataPlane == null) return null;
		
		int progressTick = 1, progressCnt = 0, progressLast = -1;
		if (vML.isCfgShowProgress()) {	
			int cnt = dataPlane.getNSTurnedOnCount();		
			String s = "("+cnt+")";
			System.out.println(" trainSegments["+dss.size()+"] w["+dataPlane.getCfgWindowSize()+"] dd["+dataPlane.getDimensionTag()+"/"+dataPlane.getTag()+"] NS["+s+"]");
			progressTick = VegML.getTickCount(dss.size());
		}
		VResultSet ts = new VResultSet(dataPlane);
		VContext ctx = new VContext(vML);
		ts.start();			
		VFrame frame = new VFrame();
		boolean isD = dss.isFmtValueD();
		
		// for each dataset
		int position = 0;
		for (int seg=0;seg<dss.size();seg++) {			
			progressLast = VegML.showProgress(vML,progressCnt, progressTick, progressLast, "+");
			progressCnt++;
			
			VDataSet segment = dss.get(seg);
			List<Long> valueOut = new ArrayList<>(segment.size());
			
			// just one value
			Long valueId [] = segment.getValueVD(seg);	

			// for each token
			for (int i=0;i<segment.size();i++,position++) {
				if (dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, frame, dataPlane.getFramerArg(), false, valueOut, dss, seg, i)) {
					trainFocus(dataPlane, frame, valueId);
					ts.total++;
				} 
				valueOut.add((long)valueId[0]);
			}
		}
		if (!threaded) dataPlane.removeAllEmptyAccum();
		ts.end();
		if (vML.isCfgShowProgress()) System.out.println("");	
		return ts;
	}	

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// blocks
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Train dataplane with dataset as blocks
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param dss dataset 
	 * @return result set
	 */
	public static VResultSet trainBlocks(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss) {
		return trainBlocks(vML, dimensionTag, dataPlaneTag, dss, false);
	}
	
	/**
	 * Train dataplane with dataset as blocks, with threaded flag
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param dss dataset 
	 * @return result set
	 */
	public static VResultSet trainBlocks(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, boolean threaded) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dss == null || dataPlane == null) return null;
		
		int progressTick = 1, progressCnt = 0, progressLast = -1;
		if (vML.isCfgShowProgress()) {
			System.out.println("-trainDataBlocks["+dss.size()+"] w["+dataPlane.getCfgWindowSize()+"] dd["+dataPlane.getDimensionTag()+"/"+dataPlane.getTag()+"] val["+dataPlane.getCfgDataWidth()+"]");
			progressTick = VegML.getTickCount(dss.size());
		}
		VResultSet ts = new VResultSet(dataPlane);
		VContext ctx = new VContext(vML);
		ts.start();
		VFrame frame = new VFrame();
		
		// make sure the window is the size of the blocks
		if (!dss.isFixedSetSize()) {
			// this might not be a good fit
			System.out.println("WARN: data is not fixed size["+dss.getMinSetSize()+" - "+dss.getMaxSetSize()+"]");
		} 
		if (dss.getMaxSetSize() != dataPlane.getCfgWindowSize()) {
			// this might not be a good fit
			System.out.println("WARN: blocksize["+dss.getMaxSetSize()+"] != windowsize["+dataPlane.getCfgWindowSize()+"]");
		}
		
		// for each dataSet			
		for (int block=0;block<dss.size();block++) {
			progressLast = VegML.showProgress(vML, progressCnt, progressTick, progressLast, "+");
			progressCnt++;
			
			VDataSet ds = dss.get(block);
			// set value size if they didn't prior
			List<Long> valueOut = new ArrayList<>(ds.size());
			
			// for each token
			for (int i=0;i<ds.size();i++) {
				Long valueId [] = ds.getValueVD(block);				

				if (dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, frame, dataPlane.getFramerArg(), false, valueOut, dss, block, i)) {
					trainFocus(dataPlane, frame, valueId);
					ts.total++;
				}
				valueOut.add(valueId[0]);
			}
		}
		if (!threaded) dataPlane.removeAllEmptyAccum();
		ts.end();
		if (vML.isCfgShowProgress()) System.out.println("");	
		return ts;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Stream
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Train dataplane with dataset as streams
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param dss dataset 
	 * @return result set
	 */
	public static VResultSet trainStreams(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss) {
		return trainStreams(vML, dimensionTag, dataPlaneTag, dss, false);
	}
	
	/**
	 * Train dataplane with dataset as streams, with threaded flag
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param dss dataset 
	 * @return result set
	 */
	public static VResultSet trainStreams(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets dss, boolean threaded) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dss == null || dataPlane == null) return null;
// FIXME find all dep sets and train together		
		
		int progressTick = 1, progressCnt = 0, progressLast = -1;
		if (vML.isCfgShowProgress()) {
			System.out.println("-trainStreams["+dss.size()+"] w["+dataPlane.getCfgWindowSize()+"] dd["+dataPlane.getDimensionTag()+"/"+dataPlane.getTag()+"] val["+dataPlane.getCfgDataWidth()+"]");
			progressTick = VegML.getTickCount(dss.size());
		}
		VResultSet ts = new VResultSet(dataPlane);
		VContext ctx = new VContext(vML);
		ts.start();
		VFrame frame = new VFrame();
		

		// for each dataSet			
		for (int block=0;block<dss.size();block++) {
			progressLast = VegML.showProgress(vML, progressCnt, progressTick, progressLast, "+");
			progressCnt++;
			
			VDataSet ds = dss.get(block);
			// set value size if they didn't prior
			List<Long> valueOut = new ArrayList<>(ds.size());
			
			// for each token
			for (int i=0;i<ds.size();i++) {
				Long valueId [] = ds.getValueVD(block);				
				if (dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, frame, dataPlane.getFramerArg(), false, valueOut, dss, block, i)) {
					trainFocus(dataPlane, frame, valueId);
					ts.total++;
				}
				valueOut.add(valueId[0]);
			}
		}
		
		if (!threaded) {
			// set baseline boolean mode
			long blvalue = dataPlane.getCfgVToV().toVectGen(dss.getDefinition().getTagValueBaselineTotal());
			dataPlane.setCfgBaseLineBooleanModeAndClear(blvalue, false);
			// remove the empty
			dataPlane.removeAllEmptyAccum();
		}
		
		ts.end();
		if (vML.isCfgShowProgress()) System.out.println("");	
		return ts;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Specialty Training
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Train data value for vector id
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param ts Result set to add to
	 * @param setNumber numberSet to train in
	 * @param vid vector id to train
	 * @param valueId value id to train to
	 * @return result set
	 */
	public static void trainNumberSet(VegML vML, String dimensionTag, String dataPlaneTag, VResultSet ts, int setNumber, long vid, long valueId) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;		
		vML.incCount();
		dataPlane.trainNumberSet(setNumber, vid, valueId);
		if (ts != null) ts.total++;
	}
	
	
	/**
	 * Train and lock a value for a dimension String input
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dimension tag/name
	 * @param dataPlaneTag dataplane tag/name
	 * @param dimensionString input String to train to value id
	 * @param valueId value id to train to
	 * @return result set
	 */
	public static int trainLocked(VegML vML, String dimensionTag, String dataPlaneTag, String dimensionString, long valueId) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dimensionString == null || dataPlane == null) return 0;
		
		// get the identity Acumm and lock it
		int vid = dataPlane.getCfgVToV().toVectGen(dimensionString);
		
		// update all other sets that assicoate this findString() has logic to get the accumes
		// get list of all vectSets with value at identity
		List<int []> vectSets = vML.vectSetMap.getSubsetsList(vid, dataPlane.getCfgFrameFocus());
		if (vectSets == null || vectSets.size() < 1) {
			// just make the default one
			MLNumberSetHash nsh = dataPlane.getNSHash(dataPlane.getCfgNSIdentityNumber());	
			Accum iac = new AccumInt();
			iac.setVectorCode(vid);
			iac.addCount(valueId);
			nsh.put(iac);
			iac.lock(valueId);
			//System.out.println("LOCKED BASE found["+vid+"]["+iac.getTotal()+"]: " + iac);
			nsh.getAccumSetDefault().addCount(valueId);	
			return 1;
		}
		//System.out.println(" findStringSequence vs["+vectSets.size()+"] vectSet: " +vid);
	
		Set<Long> vSet = new HashSet<>();
		List<Integer> fset = dataPlane.getCfgNSFull();
		int cnt = 0;
		
		// for each numberset that contains identityNumber
		for (int i=0;i<dataPlane.getNSCount();i++) {
			List<Integer> set = dataPlane.getNS(i);
			if (!MLNumberSetUtil.setContainsOffset(set, dataPlane.getCfgFrameFocus())) continue;
			//String setShow = NumberSets.setToStringPosition(this.getSet(i), this.getCfgWindowSize(), this.getValueFocus());			

			vSet.clear();
			// generate the unique set of vectors from the vector set
			for (int xi=0;xi<vectSets.size();xi++) {
				int vs[] = vectSets.get(xi);
				// check that these do have the correct content in the correct possitions
				int [] subVectSet = VegML.makeSubVector(vs, fset, set);				
				long v = VectorToVid.toVectorV64(subVectSet);			
				//if (subVectSet.length == 1)
				//System.out.println("         -->["+v+"] "  +NumberSets.setToString(vs));
				vSet.add(v);
			}
			//System.out.println("     SET   -->["+setShow+"] " +vSet.size());

			MLNumberSetHash nsh = dataPlane.getNSHash(i);	
			if (nsh == null) continue;
			Accum sac = nsh.getAccumSetDefault();
	
			// get full set of Accums for these vids
			for (long v:vSet) {
				Accum iac = nsh.get(v);
				if (iac == null) {
					iac = new AccumInt();
					iac.setVectorCode(vid);
					iac.addCount(valueId);
					nsh.put(iac);
					iac.lock(valueId);
					//System.out.println("LOCKED not found["+vid+"]["+iac.getTotal()+"]: " + dimensionString);
					sac.addCount(valueId);	
				} else {
					iac.lock(valueId);
					// sac stats are off now
			// FIXME
					//System.out.println("LOCKED["+cnt+"]["+i+"] ["+iac.getTotal()+"] => " + iac.getDistString());
				}	
				cnt++;
			}
		}
		return cnt;
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Train & test to produce learning curve
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Train and test to determine the learning curve for the model and dataset
	 * 
	 * @param vML VegML instance
	 * @param minWindow minimum window to try or -1 for any
	 * @param maxWindow maximum window to try or -1 for any
	 * @param dataSkip number of trained tokens between tests
	 * @param dss dataset to train and test with
	 * @param realTotal real total to use for test size
	 * @return List of dataset with list of pass counts
	 */
	public static List<List<Integer>> trainDataSetsLearningCurve(VegML vML, int minWindow, int maxWindow, int dataSkip, VDataSets dss, int realTotal) {
		VDataPlane dp = vML.getCfgDefaultDataPlane();
		if (dp == null) return null;
		System.out.println("Train Learning Curve["+dp.getDimensionTag()+"/"+dp.getTag()+"] TRAIN["+dss.getTrainCount()+"] TEST["+dss.getTuneCount()+"] windowMax["+maxWindow+"]");
		
		List<List<Integer>> passLists = new ArrayList<>();
		
		dss.genVSets();
		VDataSets trainDs = dss.getTrainDataSets();
		VDataSets testDs = dss.getTestDataSets();
		
		// for each window
		int ws = minWindow;
		for (;ws<=maxWindow;ws++) {
			System.out.print("  Window["+ws+"] ");
			String tmpfn = vML.getCfgDefaultDTag()+"-lc-"+ws+"-temp.veg";
			dp.clearCfgNS();
			dp.setCfgWindowSize(ws);
			dp.setCfgFrameFocus(ws/2);
			System.out.print("=");
			dp.resetNSAndData();
			System.out.print(">");
			vML.save(tmpfn);
			
			List<Integer> pl = trainDataSetsLearningCurve(vML, true, dataSkip, trainDs, testDs, realTotal);
			
			System.out.println(" samples["+pl.size()+"] ["+pl.get(0)+" - "+pl.get(pl.size()-1)+"]");
			passLists.add(pl);
			VFileUtil.delFile(tmpfn);
		}
		System.out.println("COMPLETE ");
	
		return passLists;
	}
	
	// return pass count for each trained token
	protected static List<Integer> trainDataSetsLearningCurve(VegML vML, boolean showMinProgress, int dataSkip, VDataSets trainDs, VDataSets testDs, int realTotal) {
		VDataPlane dataPlane = vML.getCfgDefaultDataPlane();
		if (trainDs == null || dataPlane == null) return null;
		
		int progressTick = 1, progressCnt = 0, progressLast = -1;
		if (showMinProgress) {
			progressTick = VegML.getTickCount(trainDs.size());
		}
		List<Integer> passList = new ArrayList<>();
		
		VResultSet ts = new VResultSet(dataPlane);
		VContext ctx = new VContext(vML);
		ts.start();
		int position = 0;
		
		// for each DataSet
		for (int set=0;set<trainDs.size();set++) {			
			if (showMinProgress) {
				progressLast = VegML.showProgressMark(vML, progressCnt, progressTick, progressLast, "+");
				progressCnt++;
			}
			
			// set value size if they didn't prior
			VFrame frame = new VFrame();
			List<Long> valueOut = new ArrayList<>(VResultSet.DEF_VALOUT);
			VDataSet ds = trainDs.get(set);
			// for each position in dataset
			for (int i=0;i<ds.size();i++) {
				// make data vector and retain	
				Long [] vvect = ds.getValueVD(i);
				
				if (!dataPlane.getFramer().makeFrameSetup(ctx, dataPlane, frame, dataPlane.getFramerArg(), false, valueOut, trainDs, set, i)) {
					valueOut.add((long)vvect[0]);
					continue;
				}

				trainFocus(dataPlane, frame, vvect);
				valueOut.add((long)vvect[0]);
				ts.total++;
				
				// test the test set
				boolean eval = true;
				if (dataSkip > 0) {
					if ((position%dataSkip) == 0) eval = true;
					else if (set == (trainDs.size()-1) && i == (trainDs.size()-1)) eval = true; // last
					else eval = false;
				}
				if (eval) {
					VResultSet resXCur = VegTest.testSets(vML, dataPlane.getDimensionTag(), dataPlane.getTag(), testDs);
					if (realTotal > 0) {
						// the fail count is real
						passList.add(realTotal-resXCur.failTotal);
					} else {
						passList.add(resXCur.passTotal);	
					}
					//if (showMinProgress) System.out.println(" w["+dataPlane.getCfgWindowSize()+"][@"+position+"] => " + String.format("%.4f", resXCur.getPassPercent(null)) + "%");
				}
				position++;
			}
		}
		dataPlane.removeAllEmptyAccum();
		ts.end();
		return passList;
	}

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//

	/**
	 * data item value training
	 * This could train 1 set or a few sets at a time, allowing for memory swaping or partial saving
	 * @param dp DataPlane in training
	 * @param frame input data frame
	 * @param valueIds values to train to if multiple
	 * @return true if value trained
	 */
	private static boolean trainFocus(final VDataPlane dp, final VFrame frame, final Long [] valueIds) {
		dp.getVegML().incCount();

		if (dp.getCfgNonValue() == valueIds[0] && valueIds.length == 1) return false; // nope
		
		// single pass vector gen and check
		VectorToVid.vectSetGen(dp.getNSMapVectorPositions(), dp.getNSMapVectorLength(), frame.getFrameFull(), frame.getVectSpace(), dp.isNoEmptyElements(), dp.getExceptVectNumber());				

		// check training filter for identity only
		// need to create / add to total on context numberSets; not add value
		boolean filtered = false;
		if (dp.getTrainingFilter(valueIds[0]) == 1) filtered = true;	
		
		if (dp.isCfgSaveChildVids() && !filtered) {
			// generate child Vids here
			VectorToVid.vectSetGen(dp.getNSChildMapVectorPositions(), dp.getNSChildMapVectorLength(), frame.getFrameFull(), frame.getVectSpaceCh(), dp.isNoEmptyElements(), dp.getExceptVectNumber());		
		}
			
		// get or add full set, get vsid to use
		long fvid = -1;
		if (dp.getCfgNSVectNumFullNumber() >= 0) fvid = frame.getVectSpace()[dp.getCfgNSVectNumFullNumber()];	
		MLNumberSetHash fnsh = dp.getNSHash(dp.getCfgNSFullNumber());
		
		// if predicting at position: no sets that include that position 
		// use valueFocus or default
		boolean nofull = false;
		if (fnsh == null || fnsh.isTurnedOff()) nofull = true;
		
		int [] fvectSet = frame.getFrame();	
		//frame.print();
		
		// for each value training to
		for (int vc = 0;vc<valueIds.length;vc++) {
			long valueId = valueIds[vc];
			
			long accumFValue = valueId;
			long accumFVid = fvid;
			
			// Reverse train; vector is value;  value is vector
			if (dp.isFrameReverse()) {
				accumFValue = fvid;
				accumFVid = valueId;
			} 
	
			// get full set
			int vsid = 0;
			if (nofull || fvid == -2 || fvid == -1 || fvid == 0 || filtered) {
				if (dp.getVegML().isCfgSaveVectSets() && !dp.isFrameReverse()) {
					Accum vs = fnsh.get(accumFVid);
					if (vs == null) vsid = dp.getVegML().vectSetMap.add(Arrays.copyOf(fvectSet, fvectSet.length));
					else vsid = vs.getVectSetId();
				}
			} else if (fnsh != null) {
				synchronized (fnsh) {
				Accum vs = fnsh.addCount(accumFVid, accumFValue);	
				if (vs == null) {
					// make this variable to improve mem/performance for the set
					vs = dp.getAccumulator();			
					if (dp.getVegML().isCfgSaveVectSets() && !dp.isFrameReverse()) {					
						vsid = dp.getVegML().vectSetMap.add(Arrays.copyOf(fvectSet, fvectSet.length));
						vs.setVectSetId(vsid);
					} else if (dp.isCfgSaveChildVids()) {
						vs.setVectChildVid(frame.getVectSpaceCh()[dp.getCfgNSVectNumFullNumber()]);	
					}
					vs.setVectorCode(accumFVid);
					vs.addCount(accumFValue);
					//if (isLocked) vs.lock(value);
					fnsh.put(vs);
				} else {
					vsid = vs.getVectSetId();
				}
				}
			}
						
			// defAccumulator accounting
			if (!filtered) dp.getAccumDefault().addCount(valueId);
			//else dp.getAccumDefault().adjustTotalInc(1);
			
			// account for set value
			if (fnsh != null) {
				if (!filtered) fnsh.getAccumSetDefault().addCount(accumFValue);	
				else fnsh.getAccumSetDefault().adjustTotalInc(1);
			}
			
			//
			// all subsets add a vector accum value
			for (int vectNum=0;vectNum<frame.getVectSpace().length;vectNum++) {
				if (dp.getCfgNSVectNumFullNumber() == vectNum) continue;
				long vid = frame.getVectSpace()[vectNum];		
				if (vid == -2 || vid == -1 || vid == 0) continue;
				
				int setNumber = dp.getMapVectorNumberSet(vectNum);
				// if filtered then only on context
				if (filtered && !dp.isCfgNSContext(setNumber)) {
					continue;
				}
											
				// add direct for position (in window)
				MLNumberSetHash nsh = dp.getNSHash(setNumber);
					
				// set probability
				if (!filtered) nsh.getAccumSetDefault().addCount(valueId);
				else nsh.getAccumSetDefault().adjustTotalInc(1);
			
				// get the Vid to set in the accumulator vectCode
				long accumVid = vid;
				long accumValue = valueId;		
				if (dp.isFrameReverse()) {
					// Reverse train; vector is value;  value is vector
					accumValue = vid;
					accumVid = valueId;
				} 
				
				synchronized (nsh) {
				if (filtered) {
					// just add to total
					Accum vs = nsh.addTotal(accumVid, accumValue);	
					if (vs == null) {
						// make this variable to improve mem/performance for the set
						vs = dp.getAccumulator();
						if (!dp.isCfgSaveChildVids()) {
							vs.setVectSetId(vsid);	
						}
						vs.setVectorCode(accumVid);
						vs.adjustTotal(1);	
						nsh.put(vs);
					} 
				} else {
					// add or update the value
					Accum vs = nsh.addCount(accumVid, accumValue);	
					if (vs == null) {
						// make this variable to improve mem/performance for the set
						vs = dp.getAccumulator();
						if (dp.isCfgSaveChildVids()) {
							vs.setVectChildVid(frame.getVectSpaceCh()[vectNum]);	
						} else {
							vs.setVectSetId(vsid);	
						}
						vs.setVectorCode(accumVid);
						vs.addCount(accumValue);
						nsh.put(vs);
					} 
				}
				}
			}
		}
		return true;
	}
}
