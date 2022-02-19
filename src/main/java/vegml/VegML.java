 /*************************************************************************
 * VegML version 1.0.0
 * __________________
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

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import gnu.trove.map.hash.TIntObjectHashMap;
import vegml.Data.VDataSet;
import vegml.Data.VDataSetDescriptor;
import vegml.Data.VDataSets;
import vegml.Data.VectorToVid;
import vegml.Data.VDataSetDescriptor.DSDataType;
import vegml.Data.VDataSetDescriptor.DSInputSetType;
import vegml.VDataPlane.NSVectMap;
import vegml.OptimizerMerge.MergeMap;


/**
 * Instance of Veg a Vector Addressed Accumulated Memory
 * This is the base implementation to support Relational Probability Memory
 * 
 * - Veg contains 0-n dataplanes as well as configuration
 * - veg instance can be treated as a model OR a container for multiple dataplane models
 *
 */
public class VegML implements java.io.Serializable {

	private static final long serialVersionUID = -250654542654836960L;
	
	private static final int PROGRESS_LINES = 10;
	private static final int PROGRESS_MARKS_LINE = 100;
		
	/**
	 * method getting base probabilities from numberSets
	 * Default, 			// == AverageIfNotPerfect
	 * Average, 			// average all numberSet probabilities
	 * AverageRecall,		// average, recall always wins
	 * AverageIfNotRecall,	// average  if not pefect match of fullest set
	 */
	public enum ProbMethod { 
		Default,
		Average,
		AverageRecall,
		AverageIfNotRecall,	
	};


	/**
	 * Base numberSet sets type; determines what numberSets to add to a dataplane
	 *  PowerSet, 			// power set: DEFAULT
	 *  SequenceLeft, 		// Sequence Left to Right
	 *  SequenceLeftId, 	// Sequence Left to Right AND identity
	 *  SequenceRight, 		// Sequence Right to Left
	 *  SequenceRightId, 	// Sequence Right to Left AND identity
	 *  SequenceEdge, 		// Sequence Edge to Focus
	 *  SequenceEdgeId, 	// Sequence Edge to Focus AND identity
	 *  SequenceFan, 		// Sequence Focus to Edge
	 *  Linear, 			// Singles linear
	 *  None, 				// Nothing
	 */
	public enum NumberSetType {
		PowerSet,
		SequenceLeft,
		SequenceLeftId,
		SequenceRight,
		SequenceRightId,
		SequenceEdge,
		SequenceEdgeId,
		SequenceFan, 
		Linear, 
		None,
	};
	
	/**
	 * NumberSet weight default type; determines how numberSets are weighted
	 *  Flat, 			// all the same
	 *  Distance,		// based on distance from focus; exponential backoff
	 *  Natural, 		// use set size
	 *  NaturalId, 		// use set size, identity gets window size
	 *  None, 			// use set size, identity gets window size
	 *  DistanceLinear,	// based on distance from focus; linear backoff
	 */
	public enum NSWeightBase {
		Flat,
		Distance,
		Natural,
		NaturalId,
		None,
		DistanceLinear,	
	};
	
	/**
	 * Type of accumulator to use for a dataplane
	 *  Default, 		// Default: Int
	 *  Boolean,		// Boolean - just 2 value options
	 *  Int,			// value is integer (4 bytes)
	 *  Long, 			// value is long (8 bytes)
	 *  Hash, 			// value is integer (4 bytes); access via hash: use for large value sets
	 *  HashLong, 		// value is long (8 bytes); access via hash: use for large value sets
	 */
	public enum AccumType {
		Default, 
		Boolean,
		Int,
		Long, 
		Hash, 
		HashLong,
		HashCrt
	};
	
	/**
	 * Dataplane state
	 *  Default, 	// intial state
	 *  Trained, 	// has been trained or marked trained
	 *  Tuned, 		// has been tuned or marked tuned
	 *  Ready, 		// is ready for use
	 */
	public enum DPState {
		Default, 
		Trained, 
		Tuned, 
		Ready, 
	};
	
	/**
	 * Prediction type for a result
	 * 
	 * Direct types:
	 *  Recall,			// recall, seen it before
	 *  RecallPredict,	// recall, multiple options
	 *  PredictRelate,	// predicted from elements with a relation
	 *  Predict,		// predicted from elements
	 *  PredictUnknown,	// predicted for unknown token
	 *  Default,		// No info: default for DataPlane		
	 *  Fail,			// Error: no trained data
	 *  
	 * Grouped types used for tuning and some APIs:
	 *  NotUnknown,		// not unknown
	 *  AnyRecall,		// RecallPredict or Recall
	 *  AnyPredict,		// PredictRelate or Predict
	 *  AnyUnknown,		// PredictRelate or Predict
	 *  All,			// All or baseLine
	 *  None,
	 */
	public enum PredictionType {
		Recall,
		RecallPredict,
		PredictRelate,
		Predict,
		PredictUnknown,
		Default,	
		Fail,
		NotUnknown,
		AnyRecall,
		AnyPredict,
		AnyUnknown,
		All,
		None,
	}	
	public static PredictionType getPredictionTypeEnum(int ordinal) {
		if (ordinal == 0) return PredictionType.Recall;
		if (ordinal == 1) return PredictionType.RecallPredict;
		if (ordinal == 2) return PredictionType.PredictRelate;
		if (ordinal == 3) return PredictionType.Predict;
		if (ordinal == 4) return PredictionType.PredictUnknown;
		if (ordinal == 5) return PredictionType.Default;
		if (ordinal == 6) return PredictionType.Fail;
		if (ordinal == 7) return PredictionType.NotUnknown;
		if (ordinal == 8) return PredictionType.AnyRecall;
		if (ordinal == 9) return PredictionType.AnyPredict;
		if (ordinal == 9) return PredictionType.AnyUnknown;
		return PredictionType.Fail;
	}
	public static int getPredictionTypeCount() {
		return PredictionType.Fail.ordinal();
	}
	public static int getPredictionTypeCountMax() {
		return PredictionType.None.ordinal();
	}
	
	public static int emptyVect = VectorToVid.toVectorGen(" ");
	static int baseHash = 2048; // base hash bucket size for dataplane sets: this * window gives base
	public static String copywrite = "VegML; Copyright (C) [2022] Aaron Ledbetter";
	
	
	/**
	 * Configuration and data for the Veg Instance
	 */
	private boolean saveVectSets = false;				// retain vector data for rules AND for some reductions
	private boolean showProgress = false;				// show progress to stdout for some calls
	private String tag = null;							// tag for Veg Instance
	private String description = null;					// description of the Veg Instance
	//private int defaultWindowSize = AUTO_START_SZ;		// default window size to use
	MLValStrMap vectStrMap;								// string/valueId mappings
	MLVectSetMap vectSetMap;							// vector vectorSetId mappings
	private HashMap<String, Object> scratchPad = null; 	// space for retained instance variables / config
	private long inCount = 0;							// count of trained frames in	
	private List<VDataPlane> dpList; 					// set of dataplanes
	private transient HashMap<String, VDataPlane> dpHash = null; // dataplanes hash for lookup	

	
	/**
	 * default constructor
	 */
	VegML() {
		this("default");
	}

	/**
	 * construct with default window and tag for VegML instance
	 * @param window default windowsize
	 * @param tag tag of name for instance
	 */
	public VegML(String tag) {
		this.vectStrMap = new MLValStrMap();
		this.vectSetMap = new MLVectSetMap();
		this.inCount = 0;
		this.dpHash = new HashMap<>();
		this.dpList = new ArrayList<>();
		this.setTag(tag);
		this.saveVectSets = false;
		this.showProgress = false;
	}
		
	/**
	 * print copywrite to stdout
	 */
	public static void showCopywrite() {
		System.out.println(copywrite);
	}
	
	/**
	 * get the tag for this model
	 * @return
	 */
	public String getTag() {
		return tag;
	}
	
	/**
	 * set the tag for this model
	 * @param tag
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 *  description of this model
	 * @return
	 */
	public String getCfgDescription() {
		return description;
	}
	
	/**
	 * set the description for this model
	 * @param description
	 */
	public void setCfgDescription(String description) {
		this.description = description;
	}
	
	/**
	 * get dataplane description
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public String getCfgDescription(String dimensionTag, String dataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		return dp.getCfgDescription();
	}
	
	/**
	 * set dataplane description
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param description description to set
	 */
	public void setCfgDescription(String dimensionTag, String dataPlaneTag, String description) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.setCfgDescription(description);
	}
	
	/**
	 * set the value focus for a dataplane
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param valueFocus
	 */
	public void setCfgFrameFocus(String dimensionTag, String dataPlaneTag, int valueFocus) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgFrameFocus(valueFocus);
	}
	
	/**
	 * Get a dataplanes focus
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @return
	 */
	public int getCfgFrameFocus(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return -1;
		return dataPlane.getCfgFrameFocus();
	}

	/**
	 * Set dataplan to have no focus
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param noFocus
	 */
	public void setCfgFrameFocusNone(String dimensionTag, String dataPlaneTag, boolean noFocus) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgFrameFocusNone(noFocus);
	}

	/**
	 * Set dataplane to use identity only filter
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param identityOnly
	 */
	public void setCfgIdentityOnly(String dimensionTag, String dataPlaneTag, boolean identityOnly) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgIdentityOnly(identityOnly);
	}
	
	/**
	 * Set the accumulator type for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param atype accumulator type
	 */
	public void setCfgAccumulatorType(String dimensionTag, String dataPlaneTag, AccumType atype) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgAccumulatorType(atype);
	}

	/**
	 * get current count of dataPlans
	 * @return count
	 */
	public int getDataPlaneCount() {
		return dpList.size();
	}

	/**
	 * get dataplane by index
	 * @param index
	 * @return
	 */
	VDataPlane getDataPlane(int index) {
		if (index > dpList.size()) return null;
		return dpList.get(index);
	}
	
	/**
	 * get data plan by tag
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return dataplan object
	 */
	public VDataPlane getDataPlane(String dimensionTag, String dataPlaneTag) {
		return dpHash.get(dimensionTag+"/"+dataPlaneTag);
	}
	
	// internal needed for deserialize
	void serdpHash(VDataPlane dp) {
		dpHash.put(dp.getDimensionTag()+"/"+dp.getTag(), dp);
	}

	/**
	 * get a list of all dataPlane tags 
	 * @return
	 */
	public List<String> getDataPlaneDPTagList() {
		if (getDataPlaneCount() == 0) return null;
		List<String> dl = new ArrayList<>();
		for (int d=0;d<getDataPlaneCount();d++) {	
			dl.add(dpList.get(d).getTag());
		}
		return dl;
	}
	
	/**
	 * get a list of all dataPlane dimension tags 
	 * @return
	 */
	public List<String> getDataPlaneDTagList() {
		if (getDataPlaneCount() == 0) return null;
		List<String> dl = new ArrayList<>();
		for (int d=0;d<getDataPlaneCount();d++) {	
			dl.add(dpList.get(d).getDimensionTag());
		}
		return dl;
	}
	
	/**
	 * get list of dataPlanes tags for a dimension tag
	 * @param dimensionTag
	 * @return
	 */
	public List<String> getDataPlaneTagList(String dimensionTag) {
		if (getDataPlaneCount() == 0) return null;
		List<String> dl = new ArrayList<>();
		for (int d=0;d<getDataPlaneCount();d++) {	
			if (dpList.get(d).getDimensionTag().equals(dimensionTag)) {
				dl.add(dpList.get(d).getTag());
			}
		}
		return dl;
	}
	
	/**
	 * get list of dataPlanes for a dimension tag
	 * @param dimensionTag
	 * @return list of dataplane instances, direct links
	 */
	public List<VDataPlane> getDataPlaneList(String dimensionTag) {
		if (getDataPlaneCount() == 0) return null;
		List<VDataPlane> dl = new ArrayList<>();
		for (int d=0;d<getDataPlaneCount();d++) {	
			if (dpList.get(d).getDimensionTag().equals(dimensionTag)) {
				dl.add(dpList.get(d));
			}
		}
		return dl;
	}
	
	/**
	 * Check if a dataplane exists
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return true if dataplane exists
	 */
	public boolean haveDataPlane(String dimensionTag, String dataPlaneTag) {
		return dpHash.get(dimensionTag+"/"+dataPlaneTag) != null;
	}
	
	//
	// get or add a dataPlan with this tag
	//
	private VDataPlane getOrAddDataPlane(String dimensionTag, String dataPlaneTag, int window, int before) {
		return getOrAddDataPlane(dimensionTag, dataPlaneTag, window, before, NumberSetType.PowerSet);
	}
	private VDataPlane getOrAddDataPlane(String dimensionTag, String dataPlaneTag, int window, int before, NumberSetType setType) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp != null) return dp;
		
		dp = new VDataPlane(this, dimensionTag, dataPlaneTag, window, before, setType);
		dpList.add(dp);
		dpHash.put(dimensionTag+"/"+dataPlaneTag, dp);
		if (dpList.size() == 1) {
			// set first as default
			setCfgDefaultDataPlane(dimensionTag, dataPlaneTag);
		}
		return dp;
	}
	
	/**
	 * remove the dataPlane from the model
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void removeDataPlane(String dimensionTag, String dataPlaneTag) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.reset();
		// remove strings...
		dpList.remove(dp);
		dpHash.remove(dimensionTag+"/"+dataPlaneTag);
		if (dimensionTag.equals(getCfgDefaultDTag()) && dataPlaneTag.equals(this.getCfgDefaultDPTag())) {
			// remove if default
			delCfgScratch("inf_default_dtag");
			delCfgScratch("inf_default_dptag");
		}
	}
	
	/**
	 * move data plane / change dimension tag
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param toDimensionTag move to dimension tag
	 * @param toDataPlaneTag move to tag
	 */
	public void moveDataPlane(String dimensionTag, String dataPlaneTag, String toDimensionTag, String toDataPlaneTag) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.setDimensionTag(toDimensionTag);
		dpHash.remove(dimensionTag+"/"+dataPlaneTag);
		dpHash.put(toDimensionTag+"/"+toDataPlaneTag, dp);	
		if (dimensionTag.equals(getCfgDefaultDTag()) && dataPlaneTag.equals(this.getCfgDefaultDPTag())) {
			// remove if default
			delCfgScratch("inf_default_dtag");
			delCfgScratch("inf_default_dptag");
		}
	}

	/**
	 * reset dataplane predict training stats
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void resetPredictTraining(String dimensionTag, String dataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.clearPredictAnswerAccum();
	}
	
	/**
	 * reset dataplane recall training stats
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void resetRecallTraining(String dimensionTag, String dataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.clearRecallAnswerAccum();
	}

	
	//////////////////////////////////////////////
	// Model Retention
	//////////////////////////////////////////////

	/**
	 * Save this instance to a file
	 * 
	 * @param filename filename to save to
	 */
	public void save(String filename) {
		System.out.println("VegML Saving[all] => ["+filename+"]");
		MLSerialize.saveVML(this, filename);
	}
	
	/**
	 * Save this instance to a file silently
	 * 
	 * @param filename filename to save to
	 */
	public void saveSilent(String filename) {
		MLSerialize.saveVML(this, filename);
	}

	/**
	 * save partial data from this instance
	 * @param filename	file name to save to
	 * @param dimensionData  save the dimension data to the file
	 * @param dimensionStrings save the strings to the file
	 * @param vectSets save the vectSets to the file
	 */
	public void savePartial(String filename, boolean dimensionData, boolean dimensionStrings, boolean vectSets) {
		MLValStrMap strm = this.vectStrMap;
		MLVectSetMap setm = this.vectSetMap;
		//List<Dimension> dl = this.dimensionList;
		
		String st = "";
		if (!dimensionData) {
			//this.dimensionList = new ArrayList<>();
		} else {
			st += "Dimensions ";
		}
		if (!dimensionStrings) this.vectStrMap = null;
		else st += "DimensionStrings ";
		if (!vectSets) this.vectSetMap = null;
		else st += "vectSets ";

		System.out.println("VegML Saving["+st+"] => ["+filename+"]");
		
		MLSerialize.saveVML(this, filename);
		this.vectStrMap = strm;
		this.vectSetMap = setm;
		//this.dimensionList = dl;		
	}
	
	/**
	 * load from saved model
	 * @param filename	file name to load from
	 * @return VegML from the content
	 */
	public static VegML load(String filename) {
		//System.out.println("VegML Loading <= ["+filename+"]");
		return MLSerialize.loadVML(filename);
	}

	/**
	 * import from a file into this VegML, these could be full or partials
	 * NOTE: if importing justStrings / vectSets vegML config maybe altered
	 * @param filename	file name to load data and import from
	 * @return true if loaded
	 */
	public boolean importModel(String filename) {
		System.out.println("VegML import <= ["+filename+"]");
		VegML vML = MLSerialize.loadVML(filename);
		if (vML == null) return false;
		// check if no dimensions
		if (vML.getDataPlaneCount() == 0) {
			// just strings, vectsets, or config
			vML.inCount = 0;
			// other updates?
		}
		return merge(vML);
	}
	
	/**
	 * True if data added to all sets
	 * @return
	 */
	boolean checkFullDataSet() {
		for (VDataPlane dp:dpList) {			
			if (!dp.checkFullDataSet()) return false;
		}
		return true;
	}
	
	/**
	 * merge model into this one
	 * @param xML instance to merge into this one
	 * @return
	 */
	public boolean merge(VegML xML) {		
		// window must be the same on import
		// 1) resize vML or this to the larger size
		boolean fullDataSet = checkFullDataSet();
		if (fullDataSet) {
			// stacking the data
			this.inCount += xML.inCount;
		}
		
		for (int xdp=0;xdp<xML.getDataPlaneCount();xdp++) {
			VDataPlane xdpix = xML.getDataPlane(xdp);
			VDataPlane dpix = this.getOrAddDataPlane(xdpix.getDimensionTag(), xdpix.getTag(), xdpix.getCfgWindowSize(), xdpix.getCfgBefore());
			dpix.merge(xdpix);
		}
		
		// add VectStrMap
		this.vectStrMap.merge(this, xML, xML.vectStrMap);
		
		// always optimize
		this.optimize();
		return true;
	}
	
	/**
	 * make a copy of this instance
	 * @return
	 */
	public VegML copy() {
		VegML vML = new VegML(this.tag);
		vML.merge(this);
		return vML;
	}
	
	/**
	 * get the trained input frame count
	 * @return
	 */
	long getInCount() {
		return inCount;
	}
	
	/**
	 * set framed input count
	 * @param inCount
	 */
	void setInCount(long inCount) {
		this.inCount = inCount;
	}
	
	/**
	 * increment the framed input count
	 */
	void incCount() {
		inCount++;
	}

	/**
	 * Dif this instance with another
	 * @param xML instance to dif with
	 * @return count of differences
	 */
	public int diff(VegML xML) {
		int cnt = 0;
				
		for (int xdp=0;xdp<xML.getDataPlaneCount();xdp++) {
			VDataPlane xdpix = xML.getDataPlane(xdp);
			VDataPlane dpix = this.getDataPlane(xdpix.getDimensionTag(), xdpix.getTag());
			if (dpix == null) {
				cnt++;
				System.out.println(" *DIFF[dataPlane]["+xdpix.getTag()+"] not have ");							
				continue;
			}
			// DataPlane
			int ccnt = dpix.getAccumDefault().diff(xdpix.getAccumDefault());
			if (ccnt > 0) {
				cnt += ccnt;
				System.out.println(" *DIFF[dac]["+dpix.getTag()+"] diffs["+ccnt+"] ");		
			}
			
			// number sets
			for (int i=0;i<xdpix.getNSCount();i++) {
				MLNumberSetHash xnsh = xdpix.getNSHash(i);
				if (xnsh == null || xnsh.size() < 1) continue;
				MLNumberSetHash hm = dpix.getNSHash(i);
				if (hm == null) {
					ccnt++;
					System.out.println(" *DIFF[nsh]["+i+"] diffs["+ccnt+" of NONE] ");	
										
				} else {
					ccnt = hm.diff(this, dpix, xML, xnsh, xdpix);
					if (ccnt > 0) {
						cnt += ccnt;
						System.out.println(" *DIFF[nsh]["+i+"] diffs["+ccnt+" of "+hm.size()+"] ");	
					}
				}
			}
		}
		
		// strings
		cnt += this.vectStrMap.diff(this, xML, xML.vectStrMap);
		
		// vectSets
		System.out.println("DIFF: differences: " + cnt);
		return cnt;
	}

	
	//////////////////////////////////////////////
	// DATA
	/**
	 * get count of frames trained with
	 * @return
	 */
	public long getDataTotal() {
		return this.inCount;
	}

	
	//////////////////////////////////////////////
	// config
	
	/**
	 * Set to save all input vectors for accumulators
	 * default is off, if enabled this will take additional memory and processing
	 * This must be enabled before training to support some logical analysis and tuning
	 * 
	 * @param save if true save
	 */
	public void setCfgSaveVectSets(boolean save) {
		saveVectSets = save;
	}
	
	/**
	 * check if save vectors for accumulators is enabled
	 * @return
	 */
	public boolean isCfgSaveVectSets() {
		return saveVectSets;
	}
	
	/**
	 * Set true to show progress in some training, tuning and testing
	 * @param showProgress
	 */
	public void setCfgShowProgress(boolean showProgress) {
		this.showProgress = showProgress;
	}	
	
	/**
	 * check if show progress is enabled
	 * @return
	 */
	public boolean isCfgShowProgress() {
		return showProgress;
	}
	
	/**
	 * set dataplane to save child vids to each accumulator instead of vectSetIds during training
	 * default is off
	 * 
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param save
	 */
	public void setCfgSaveChildVids(String dimensionTag, String dataPlaneTag, boolean save) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgSaveChildVids(save);
	}
	
	/**
	 * check if save child vids is enabled for a dataplane
	 * 
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @return
	 */
	public boolean isCfgSaveChildVids(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return false;
		return dataPlane.isCfgSaveChildVids();
	}



	/**
	* Accumulator Probability weight calculation
	*
	* @param  dataplane  data plane to get info from
	* @param  dac  default accumulator for Model
	* @param  setNumber 
	* @param  haveIdentity 
	* @param  setNumber 
	*
	* @return probability
	*/
	public static class VegPCalc {
		// 
		// calculate the probability to retain for a single numberSet and value
		//
		public double calculateProb(ProbMethod method, VDataPlane dataplane, Accum dac, double [] nsWeights, 
					int setNumber, boolean haveIdentity, Accum sac, Accum sval, ValProb vp) {
			return vp.probability * nsWeights[setNumber];
		}
	}
	// probability weighted
	static VegPCalc probNumberSetWeightCalc = new VegPCalc();
	// probability only
	static VegPCalc probOnlyCalc = new VegPCalc() {
		@Override
		public double calculateProb(ProbMethod method, VDataPlane dataplane, Accum dac, double [] nsWeights, 
				int setNumber, boolean haveIdentity, Accum sac, Accum sval, ValProb vp) {					
			return vp.probability;
		}
	};
	// frequency only
	static VegPCalc freqOnlyCalc = new VegPCalc() {
		@Override
		public double calculateProb(ProbMethod method, VDataPlane dataplane, Accum dac, double [] nsWeights,
				int setNumber, boolean haveIdentity, Accum sac, Accum sval, ValProb vp) {	
			return (double)vp.count/(double)dac.getTotal();
		}
	};


	/**
	 * get the instance of a probability only calculator, non-weighted
	 * @return
	 */
	public VegPCalc getPCalcProbabilityOnly() {
		return probOnlyCalc;
	}	
	
	/**
	 * get the instance of a frequency only calculator, non-weighted
	 * @return
	 */
	public VegPCalc getPCalcFrequencyOnly() {
		return freqOnlyCalc;
	}
	
	/**
	 * get the instance of a probability weighted by numberSet calculator
	 * @return
	 */
	public VegPCalc getPCalcProbabilityNS() {
		return probNumberSetWeightCalc;
	}
	
	/**
	 * Se the probability calculation for a dataplane
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param calculator
	 */
	public void setCfgPCalc(String dimensionTag, String dataPlaneTag, VegPCalc calculator) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgPCalc(calculator);
	}
	
	/**
	 * Get the Probability calculation for a dataplane
	 * 
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @return
	 */
	public VegPCalc getCfgPCalc(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;
		return dataPlane.getCfgPCalc();
	}	

	/**
	 * Set the VectorToVid method object for a dataplane
	 * 
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param vtov
	 */
	public void setCfgVToV(String dimensionTag, String dataPlaneTag, VectorToVid vtov) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgVToV(vtov);
	}
	
	/**
	 * Get VectorToVid method object for a dataplane 
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @return
	 */
	public VectorToVid getCfgVToV(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;
		return dataPlane.getCfgVToV();
	}
	
	/**
	 * Get the valueId for an object from the dataplans VectorToVid
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param obj
	 * @return
	 */
	public long getVaulueId(String dimensionTag, String dataPlaneTag, Object obj) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return 0;
		return dataPlane.getCfgVToV().toVectGen(obj);
	}

	/**
	 * Set the result callout, called for each result with full frame information
	 * This is used to determine a final result set, merges, mixes and external logic can be integrated
	 * default is no handler
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param callout VegCallOut handle
	 * @param arg argument to pass to handler
	 */
	public void setCfgCallout(String dimensionTag, String dataPlaneTag, VegCallOut callout, Object arg) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgCallout(callout, arg);
	}
	
	/**
	 * Get current result callout for a dataplane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return VegCallOut handle if any
	 */
	public VegCallOut getCfgCallout(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;
		return dataPlane.getCfgCallout();
	}	
	
	/**
	 * Set the default result callout for a dataplane, this will also set it as the active 
	 * result callout for the dataplane
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param callout VegCallOut handle
	 * @param arg argument to pass to handlerut
	 */
	public void setCfgCalloutDefault(String dimensionTag, String dataPlaneTag, VegCallOut callout, Object arg) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgCalloutDefault(callout, arg);
	}
	
	/**
	 * Set a dataplane to use its default result callout, if any
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void setCfgCalloutToDefault(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgCalloutToDefault();
	}
	
	/**
	 * Get the default result callout for a dataplane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return VegCallOut handle if any
	 */
	public VegCallOut getCfgCalloutDefault(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;
		return dataPlane.getCfgCalloutDefault();
	}		
		
	//
	// default is token framer
	//
	private static VegFramer VegFramerDefault = new VegFramer();
	
	/**
	 * Get the default framer for this veg instance
	 * @return
	 */
	public VegFramer getCfgFramerDefault() {
		return VegFramerDefault;
	}
	
	/**
	 * Set the token (default) framer for a dataplane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void setCfgFramerToken(String dimensionTag, String dataPlaneTag) {
		setCfgFramer(dimensionTag, dataPlaneTag, "token", VegFramerDefault, false);
	}

	/**
	 * Set framer with responses as inputs for a dataplane
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param srcDTag dimension tag of dataplane to generate input tags
	 * @param srcDPTag dataplane tag of dataplane to generate input tags
	 */
	public void setCfgFramerResponse(final String dimensionTag, final String dataPlaneTag, final String srcDTag, final String srcDPTag) {
		VegFramer.setCfgFramerResponse(this, dimensionTag, dataPlaneTag, srcDTag, srcDPTag);
	}

	/**
	 * Set framer with responses as inputs; except focus which is input for a dataplane
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param srcDTag dimension tag of dataplane to generate input tags
	 * @param srcDPTag dataplane tag of dataplane to generate input tags
	 */
	public void setCfgFramerResponseMix(final String dimensionTag, final String dataPlaneTag, final String srcDTag, final String srcDPTag) {
		VegFramer.setCfgFramerResponseMix(this, dimensionTag, dataPlaneTag, srcDTag, srcDPTag);		
	}

	/**
	 * Set frame for chars as inputs
	 * optionally include identity and token string format
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param minLen minimum length to map prefix / suffix or -1
	 * @param includeIdentity if true include identity
	 * @param includeFormat if true include token text format
	 * @param maxPrefix max size for the prefix
	 */
	public void setCfgFramerCharEdge(final String dimensionTag, final String dataPlaneTag, final int minLen, final boolean includeIdentity, final boolean includeFormat, final int maxPrefix) {
		VegFramer.setCfgFramerCharEdge(this, dimensionTag, dataPlaneTag, minLen, includeIdentity, includeFormat, maxPrefix);			
	}

	/**
	 * Set the framer for a dataplane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param framerName name for framer
	 * @param framer handle to framer instance
	 */
	public void setCfgFramer(String dimensionTag, String dataPlaneTag, String framerName, VegFramer framer) {
		setCfgFramer(dimensionTag, dataPlaneTag, framerName, framer, false);
	}
		
	/**
	 * Set the framer for a dataplane
	 * frame in reverse, values as input data as tags
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param framerName name for framer
	 * @param framer handle to framer instance
	 */
	public void setCfgFramerReverse(String dimensionTag, String dataPlaneTag, String framerName, VegFramer framer) {
		setCfgFramer(dimensionTag, dataPlaneTag, framerName, framer, true);
	}
	
	private void setCfgFramer(String dimensionTag, String dataPlaneTag, String framerName, VegFramer framer, boolean reverse) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgFramer(framerName, framer, null, reverse);
	}
	
	/**
	 * Set dataplane to frame in reverse or not; default is not
	 * frame in reverse, values as input data as tags
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param reverse true to frame in reverse
	 */
	public void setCfgFrameReverse(String dimensionTag, String dataPlaneTag, boolean reverse) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgFrameReverse(reverse);
	}	

	/**
	 * Set the framer argument for a dataplane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param arg argument to pass to framer
	 */
	public void setCfgFramerArg(String dimensionTag, String dataPlaneTag, Object arg) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgFramerArg(arg);
	}
	
	/**
	 * Set framer argument for all dataplanes
	 * @param arg argument to set
	 */
	public void setCfgFramerArg(Object arg) {
		for (int i=0;i<this.getDataPlaneCount();i++) {
			VDataPlane dataPlane = this.getDataPlane(i);
			if (dataPlane == null) continue;
			dataPlane.setCfgFramerArg(arg);
		}
	}

	/**
	 * use the dataplans framer to make an input frame from a list of valueIds and a position
	 * this will not work for a framers that need history
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dataIn list if input valueIds
	 * @param position position of focus in dataIn
	 * @return frame as valueId list
	 */
	public List<Long> frameDataV(String dimensionTag, String dataPlaneTag, List<Long> dataIn, int position) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;
		return dataPlane.frameDataV(dataIn, position);
	}

	/**
	 * use the dataplans framer to make an input frame from a list of strings and a position
	 * this will not work for a framers that need history
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dataIn list if input strings
	 * @param position position of focus in dataIn
	 * @return frame as valueId list
	 */
	public List<Long> frameDataS(String dimensionTag, String dataPlaneTag, List<String> dataIn, int position) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;
		return dataPlane.frameDataS(dataIn, position);
	}	
		
	
	/**
	 * Set true to not save vectors with empty elements
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param noEmptyElements true to not allow vectors with empty tokens
	 */
	public void setCfgNoEmptyElements(String dimensionTag, String dataPlaneTag, boolean noEmptyElements) {
		setCfgNoEmptyElements(dimensionTag, dataPlaneTag, noEmptyElements, true);
	}
	
	/**
	 * Set true to not save vectors with empty elements
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param noEmptyElements true to not allow vectors with empty tokens
	 * @param exceptFull true to make exception for full numberSet
	 */
	public void setCfgNoEmptyElements(String dimensionTag, String dataPlaneTag, boolean noEmptyElements, boolean exceptFull) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgNoEmptyElements(noEmptyElements, exceptFull);
	}

	/**
	 * get the probability method used for this dataplane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return probMethod object
	 */
	public ProbMethod getCfgProbMethod(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;
		return dataPlane.getCfgProbMethod();
	}

	/**
	 * set the probability method to use for a dataplane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param probMethod
	 */
	public void setCfgProbMethod(String dimensionTag, String dataPlaneTag, ProbMethod probMethod) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgProbMethod(probMethod);
	}
	
	/**
	 * Set the region size, this is the mximum number of positions in a number set
	 * this is used to cap the complexity of the relationships in a dataplan and reduce the overall size
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param region number of regions, default -1
	 */

	public void setCfgRegion(String dimensionTag, String dataPlaneTag, int region) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgRegion(region);
	}

	/**
	 * get the region size, this is the maximum number of positions in a number set
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return region size or -1
	 */
	public int getCfgRegion(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return -1;
		return dataPlane.getCfgRegion();
	}
	
	/**
	 * Lock or unlock the numberSet weights for a dataplane
	 * This prevents updates from recalculating the weights
	 * default is unlocked
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param locked true to lock, false to unlock
	 */
	public void setCfgNSWeightsLocked(String dimensionTag, String dataPlaneTag, boolean locked) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgNSWeightsLocked(locked);
	}
	
	/**
	 * Set the weight for a numberSet
	 * NOTE: this will reset if frameFocus is changed UNLESS the weights are locked
	 *  
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber id/index of numberSet
	 * @param weight weight to set 
	 */
	public void setCfgNSWeight(String dimensionTag, String dataPlaneTag, int setNumber, double weight) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgNSWeight(setNumber, weight);
	}
	
	/**
	 * Get the current weight for a number set in a dataplane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber id/index of numberSet
	 * @return weight of numberSet or 0
	 */
	public double getCfgNSWeight(String dimensionTag, String dataPlaneTag, int setNumber) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return -1;
		return dataPlane.getCfgNSWeight(setNumber);
	}
	
	/**
	 * Set the numberSet weights base type for a dataplane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param baseType numberSet weight type base
	 */
	public void setCfgNSWeight(String dimensionTag, String dataPlaneTag, NSWeightBase baseType) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgNSWeight(baseType);
	}
	
	/**
	 * Remove a number set by id/index
	 * NOTE: that this will renumber the numberSets
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber id/index of numberSet to remove
	 * @return true if removed
	 */
	public boolean removeCfgNS(String dimensionTag, String dataPlaneTag, int setNumber) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return false;		
		return dataPlane.removeCfgNS(setNumber);
	}	

	/**
	 * Add a number set with an array of frame positions and weight
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param set number set position array
	 * @param weight weight to set or -1 for default
	 * @return numberSet index
	 */
	public int addCfgNSDefined(String dimensionTag, String dataPlaneTag, List<Integer> set, double weight) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return -1;		
		return dataPlane.addCfgNSDefined(set, weight);
	}

	/**
	 * Add a numberSet with a format string and weight
	 * format is in dash and x format
	 * - x x - -
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param fmt format of number set positions
	 * @param weight weight to set or -1 for default
	 * @return numberSet index
	 */
	public int addCfgNSFormat(String dimensionTag, String dataPlaneTag, String fmt, double weight) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return -1;		
		String fmtX [] = fmt.toLowerCase().trim().split(" ");
		List<Integer> set = new ArrayList<>();
		//System.out.println("addCfgNSFormat["+fmt+"]");
		double w = 0;
		for (int i=0;i<fmtX.length;i++) {
			if (fmtX[i] != null && (fmtX[i].equals("x") || fmtX[i].equals("X"))) {
				set.add(i);
				w++;
			} 
		}
		if (weight < 0) weight = w;
		return dataPlane.addCfgNSDefined(set, weight);
	}
	public int addCfgNSFormatToAll(String dimensionTag, String dataPlaneTag, String fmt, double weight) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return -1;
		
		String fmtX [] = fmt.toLowerCase().trim().split(" ");
		//System.out.println("addCfgNSFormatToAll["+fmt+"] " + fmtX.length);
		
		List<Integer> set = new ArrayList<>();
		double w = 0;
		for (int i=0;i<fmtX.length;i++) {
			if (fmtX[i] != null && (fmtX[i].equals("x") || fmtX[i].equals("X"))) {
				set.add(i);
				w++;
			}
		}
		if (weight < 0) weight = w;
		return dataPlane.addCfgNSFormatToAll(set, weight);

	}
	
	/**
	 * Clear all numbersets, leaving none behind
	 * @param dimensionTag
	 * @param dataPlaneTag
	 */
	public void clearCfgNS(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.clearCfgNS();
	}
	
	/**
	 * update dataplans numberSets and vector map after number sets are complete
	 * @param dimensionTag
	 * @param dataPlaneTag
	 */
	public void updateCfgNS(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.updateCfgNS();
	}	

	/**
	 * clear all accumulators from all numbersets in a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void clearNSData(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.clearNSData();
	}

	/**
	 * clear all accumulators from a specified numberset in a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber numberSet to clear data from
	 */
	public void clearNSData(String dimensionTag, String dataPlaneTag, int setNumber) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.clearNSData(setNumber);
	}	
	
	/**
	 * Get the value count map for a numberSet, results are valueId/count
	 * 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber numberSet to get data from
	 * @return hashmap of valueId/counts
	 */
	public HashMap<Long, Integer> getNSValueSet(String dimensionTag, String dataPlaneTag, int setNumber) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;	
		return dataPlane.getNSValueSet(setNumber);
	}

	/**
	 * Merge numberSet fromNs into toNs
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param fromNs numberSet from
	 * @param toNs numberSet to
	 * @return true if merged
	 */
	public boolean mergeNS(String dimensionTag, String dataPlaneTag, int fromNs, int toNs) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return false;	
		return dataPlane.mergeNS(fromNs, toNs);
	}

	/**
	 * rebase all Accumulator totals with the trained value counts
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param value
	 */
	public void setCfgBaseLineBooleanModeAndClear(String dimensionTag, String dataPlaneTag, long value) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		// if remove the baseline loose the negatives for all positives
		dataPlane.setCfgBaseLineBooleanModeAndClear(value, false);
	}

	/**
	 * check if the state of the dataplane is trained
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public boolean isTrained(String dimensionTag, String dataPlaneTag) {	
		return isStateComplete(dimensionTag, dataPlaneTag, DPState.Trained);
	}
	
	/**
	 * check if the state of the dataplane is tuned
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public boolean isTuned(String dimensionTag, String dataPlaneTag) {
		return isStateComplete(dimensionTag, dataPlaneTag, DPState.Tuned);
	}	
	
	/**
	 * set the state of a dataplane as trained
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void setStateTrained(String dimensionTag, String dataPlaneTag) {
		setState(dimensionTag, dataPlaneTag, DPState.Trained);
	}
	
	/**
	 * set the state of a dataplane as tuned
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void setStateTuned(String dimensionTag, String dataPlaneTag) {
		setState(dimensionTag, dataPlaneTag, DPState.Tuned);
	}
	
	/**
	 * Set the state of a dataplane as ready
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void setStateReady(String dimensionTag, String dataPlaneTag) {
		setState(dimensionTag, dataPlaneTag, DPState.Ready);
	}
	
	/**
	 * Check if a state has been completed for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param state state to check if completed
	 * @return
	 */
	public boolean isStateComplete(String dimensionTag, String dataPlaneTag, DPState state) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return false;	
		return dataPlane.isStateComplete(state);
	}
	
	/**
	 * Check if the dataplane is in a state
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param state state to check
	 * @return
	 */
	public boolean isState(String dimensionTag, String dataPlaneTag, DPState state) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return false;	
		return dataPlane.isState(state);
	}
	
	/**
	 * Set the state for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param state to set
	 */
	public void setState(String dimensionTag, String dataPlaneTag, DPState state) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.setState(state);
	}
	
	/////////////////////////////////////////////
	// Dataplan scratchpad
	//

	/**
	 * get scratchpad value for a key
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param key key string for value
	 * @return value
	 */
	public Object getCfgScratch(String dimensionTag, String dataPlaneTag, String key) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;	
		return dataPlane.getCfgScratch(key);
	}
	
	/**
	 * get scratchpad string value for a key
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param key key string for value
	 * @return value
	 */
	public Object getCfgScratchString(String dimensionTag, String dataPlaneTag, String key) {
		return (String)getCfgScratch(dimensionTag, dataPlaneTag, key);
	}
	
	/**
	 * get scratchpad double value for a key
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param key key string for value
	 * @return value
	 */
	public double getCfgScratchDouble(String dimensionTag, String dataPlaneTag, String key) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return 0;	
		return dataPlane.getCfgScratchDouble(key);
	}
	
	/**
	 * get scratchpad integer value for a key
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param key key string for value
	 * @return value
	 */
	public int getCfgScratchInt(String dimensionTag, String dataPlaneTag, String key) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return 0;	
		return dataPlane.getCfgScratchInt(key);
	}
	
	/**
	 * get scratchpad boolean value for a key
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param key key string for value
	 * @return value
	 */
	public boolean getCfgScratchBool(String dimensionTag, String dataPlaneTag, String key) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return false;	
		return dataPlane.getCfgScratchBool(key);
	}
	
	/**
	 * set scratchpad value for a key
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param key key string for value
	 * @param obj object to set
	 */
	public void setCfgScratch(String dimensionTag, String dataPlaneTag, String key, Object obj) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.setCfgScratch(key, obj);
	}
	
	/**
	 * set scratchpad double value for a key
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param key key string for value
	 * @param val value to set
	 */
	public void setCfgScratch(String dimensionTag, String dataPlaneTag, String key, double val) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.setCfgScratch(key, val);
	}
	
	/**
	 * set scratchpad integer value for a key
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param key key string for value
	 * @param val value to set
	 */
	public void setCfgScratch(String dimensionTag, String dataPlaneTag, String key, int val) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.setCfgScratch(key, val);
	}
	
	/**
	 * set scratchpad string value for a key
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param key key string for value
	 * @param val value to set
	 */
	public void delCfgScratch(String dimensionTag, String dataPlaneTag, String key) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.delCfgScratch(key);
	}
	
	/**
	 * print scratchpad information for a dataplane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag tag
	 */
	public void printCfgScratch(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.printCfgScratch();
	}


	/////////////////////////////////////////////
	// VEG instance scratchpadd
	//
	
	/**
	 * get scratchpad value for a key
	 * @param key key string for value
	 * @return value
	 */
	public Object getCfgScratch(String key) {
		if (scratchPad == null) return null;
		return scratchPad.get(key);
	}
	
	/**
	 * get scratchpad string value for a key
	 * @param key key string for value
	 * @return value
	 */
	public String getCfgScratchString(String key) {
		return (String)getCfgScratch(key);
	}
	
	/**
	 * get scratchpad double value for a key
	 * @param key key string for value
	 * @return value
	 */
	public double getCfgScratchDouble(String key) {
		if (scratchPad == null) return 0;
		Object o = scratchPad.get(key);
		if (o == null) return 0;
		return ((Double)o).doubleValue();
	}
	
	/**
	 * get scratchpad integer value for a key
	 * @param key key string for value
	 * @return value
	 */
	public int getCfgScratchInt(String key) {
		if (scratchPad == null) return 0;
		Object o = scratchPad.get(key);
		if (o == null) return 0;
		return ((Integer)o).intValue();
	}
	
	/**
	 * get scratchpad boolean value for a key
	 * @param key key string for value
	 * @return value
	 */
	public boolean getCfgScratchBool(String key) {
		if (scratchPad == null) return false;
		Object o = scratchPad.get(key);
		if (o == null) return false;
		return ((Boolean)o).booleanValue();
	}
	
	/**
	 * set scratchpad value for a key
	 * @param key key string for value
	 * @param obj object to set
	 */
	public void setCfgScratch(String key, Object obj) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		scratchPad.put(key, obj);
	}
	
	/**
	 * set scratchpad double value for a key
	 * @param key key string for value
	 * @param val value to set
	 */
	public void setCfgScratch(String key, double val) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		scratchPad.put(key, val);
	}
	
	/**
	 * set scratchpad integer value for a key
	 * @param key key string for value
	 * @param val value to set
	 */
	public void setCfgScratch(String key, int val) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		scratchPad.put(key, val);
	}
	
	/**
	 * set scratchpad boolean value for a key
	 * @param key key string for value
	 * @param val value to set
	 */	
	public void setCfgScratch(String key, boolean val) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		scratchPad.put(key, val);
	}
	
	/**
	 * increment scratchpad integer value for a key
	 * @param key key string for value
	 */
	public void setCfgScratchInc(String key) {
		if (scratchPad == null) scratchPad = new HashMap<>();
		int v = getCfgScratchInt(key);
		setCfgScratch(key,v+1);
	}
	
	/**
	 * del scratchpad value for a key
	 * @param key key string for value
	 */
	public void delCfgScratch(String key) {
		if (scratchPad == null) return;
		scratchPad.remove(key);
	}
	
	/**
	 * print the instance scratchpad details
	 */
	public void printCfgScratch() {		
		// get all merge key/values for dataPlane
		System.out.print("  VegScratchPad["+this.getTag()+"]");
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
			} else {
				System.out.println(" size["+String.format("%-30s", k)+"] = type["+v.getClass().getName()+"]");					
			}
		}
	}	
	
	/**
	 * add ignore inputs for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param valueSet set of valueIds for dimension/input to ignore on evaluation
	 * @param valueIds set of Strings for valueId to ignore on evaluation
	 */
	public void setCfgIgnoreInputs(String dimensionTag, String dataPlaneTag, long [] valueSet, long [] valueIds) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.setIgnoreInputs(valueSet);
		if (valueIds != null) dataPlane.setIgnoreInputsValues(valueIds);
	}
	
	/**
	 * add ignore inputs for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param stringSet set of Strings for dimension/input to ignore on evaluation
	 * @param valueIds set of Strings for valueId to ignore on evaluation
	 */
	public void setCfgIgnoreInputs(String dimensionTag, String dataPlaneTag, String [] stringSet, String [] valueIds) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.setIgnoreInputs(stringSet);
		if (valueIds != null) dataPlane.setIgnoreInputsValues(valueIds);
	}
	
	/**
	 * get ignore inputs if any
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public long [] getCfgIgnoreInputs(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;	
		return dataPlane.getIgnoreInputs();
	}
	
	/**
	 * get ignore inputs values if any
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public long [] getCfgIgnoreInputsValues(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;	
		return dataPlane.getIgnoreInputsValues();
	}
	
	
	/////////////////////////////////////////////
	//
	// Manage Merge info: simple merge Value OR merge map with mode
	//
	
	/**
	 * Set a merge value for a key
	 * 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param key key for mergeValue
	 * @param val mergeValue
	 */
	public void setCfgMergeWeight(String dimensionTag, String dataPlaneTag, String key, double val) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.setCfgMergeWeight(key, val);
	}
	
	/**
	 * get a mergeWeigth for a dataplan and key
	 * 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param key key for mergeValue
	 * @return
	 */
	public double getCfgMergeWeight(String dimensionTag, String dataPlaneTag, String key) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return 0;	
		return dataPlane.getCfgMergeWeight(key);
	}		
	
	//
	// Merge mappings with mode
	//
	public MergeMap getCfgMergeMap(String dimensionTag, String dataPlaneTag, String dimensionTag2, String tag2) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;	
		return dataPlane.getCfgMergeMap(dimensionTag2, tag2);
	}
	public void setCfgMergeMap(String dimensionTag, String dataPlaneTag, String dimensionTag2, String dataPlaneTag2, MergeMap map) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.setCfgMergeMap(dimensionTag2, dataPlaneTag2, map);
	}
	public void setCfgMergeMapMode(String dimensionTag, String dataPlaneTag, String dimensionTag2, String dataPlaneTag2, int mmode) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.setCfgMergeMapMode(dimensionTag2, dataPlaneTag2, mmode);
	}
	
	//
	// Amplify Tune Value mappings with mode
	//
	public int getCfgAmpTuneValueX(String dimensionTag, String dataPlaneTag, String dtag, String dptag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return 0;	
		return dataPlane.getCfgAmpTuneValueX(dtag, dptag);
	}
	public void setCfgAmpTuneValueX(String dimensionTag, String dataPlaneTag, String dtag, String dptag, int ampValue) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;	
		dataPlane.setCfgAmpTuneValueX(dtag, dptag, ampValue);
	}
	public String getCfgAmpTuneValueXName(String dtag, String dptag) {
		return VDataPlane.getAmpTuneName(dtag, dptag);
	}
	
	//
	// Callout controls (used for tuning and optimization)
	//
	
	/**
	 * get dataplan callout control break before dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dtag dataplane dimension tag to break before
	 * @param dptag dataplane tag to break before
	 * @return true if break before
	 */
	public boolean getCfgMergeBreakBefore(String dimensionTag, String dataPlaneTag, String dtag, String dptag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return false;
		return dataPlane.getCfgMergeBreakBefore(dtag, dptag);
	}
	
	/**
	 * set dataplan callout control break before dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dtag dataplane dimension tag to break before
	 * @param dptag dataplane tag to break before
	 * @patam dobreak set true to break before
	 */
	public void setCfgMergeBreakBefore(String dimensionTag, String dataPlaneTag, String dtag, String dptag, boolean dobreak) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgMergeBreakBefore(dtag, dptag, dobreak);
	}
	
	/**
	 * get dataplan callout control break after dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dtag dataplane dimension tag to break after
	 * @param dptag dataplane tag to break after
	 * @return true if break after
	 */
	public boolean getCfgMergeBreakAfter(String dimensionTag, String dataPlaneTag, String dtag, String dptag) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return false;
		return dataPlane.getCfgMergeBreakAfter(dtag, dptag);
	}
	
	/**
	 * set dataplan callout control break before dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dtag dataplane dimension tag to break after
	 * @param dptag dataplane tag to break after
	 * @patam dobreak set true to break after
	 */
	public void setCfgMergeBreakAfter(String dimensionTag, String dataPlaneTag, String dtag, String dptag, boolean dobreak) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgMergeBreakAfter(dtag, dptag, dobreak);
	}
	
	
	/////////////////////////////////////////////
	// veg export information / meta
	
	/**
	 * Set the default dataplan for the Veg instance
	 * @param dimensionTag
	 * @param tag
	 */
	public void setCfgDefaultDataPlane(String dimensionTag, String tag) {
		this.setCfgScratch("inf_default_dtag", dimensionTag);
		this.setCfgScratch("inf_default_dptag", tag);
	}	
	
	/**
	 * get default dataplane dimension tag
	 * @return
	 */
	public String getCfgDefaultDTag() {
		return this.getCfgScratchString("inf_default_dtag");
	}
	
	/**
	 * get default dataplane dataplane tag
	 * @return
	 */
	public String getCfgDefaultDPTag() {
		return this.getCfgScratchString("inf_default_dptag");
	}
	
	/**
	 * Set default NumberSet weight type
	 * @param nsweight
	 */
	public void setCfgDefaultNSWeight(NSWeightBase nsweight) {
		this.setCfgScratch("inf_default_nsweight", nsweight);
	}
	
	/**
	 * get default NumberSet weight type
	 * @return
	 */	
	public NSWeightBase getCfgDefaultNSWeight() {
		return (NSWeightBase)this.getCfgScratch("inf_default_nsweight");
	}
	
	/**
	 * get the default dataplane
	 * @return
	 */
	public VDataPlane getCfgDefaultDataPlane() {
		return this.getDataPlane(getCfgDefaultDTag(), getCfgDefaultDPTag());
	}
	
	// predictionType type (veg may be limited or all
	public void setCfgPredictionType(PredictionType pType) {
		this.setCfgScratch("inf_ptype", pType);
	}
	public PredictionType getCfgPredictionType() {
		return (PredictionType)this.getCfgScratch("inf_ptype");
	}
	
	/**
	 * Set Veg instance model version
	 * @param versionNumber
	 */
	public void setCfgModelVersion(double versionNumber) {
		this.setCfgScratch("inf_version_number", versionNumber);
	}	
	
	/**
	 * Get Veg instance model version
	 * @return
	 */
	public double getCfgModelVersion() {
		return this.getCfgScratchDouble("inf_version_number");
	}
	
	/**
	 * Clear all dimension strings
	 */
	public void clearDimensionStrings() {
		this.vectStrMap.clear();
	}
	
	/**
	 * true if this has Dimension Strings
	 * @return
	 */
	public boolean hasDimensionStrings() {
		if (this.vectStrMap.getCount() > 0) return true;
		return false;
	}
	
	/**
	 * clear all vectSets
	 */
	public void clearVectSets() {
		this.vectSetMap.clear();
	}

	/**
	 * true if this has vectSets
	 * @return
	 */
	public boolean hasVectSets() {
		if (this.vectSetMap.getCount() > 0) return true;
		return false;
	}
	
	/**
	 * get vect set count
	 * @return
	 */
	public int getVectSetsCount() {
		return this.vectSetMap.getCount();
	}

	/**
	 * true if this has Dimension data
	 * @return
	 */
	public boolean hasDimensionData() {
		if (inCount > 0) return true;
		return false;
	}
	
	/**
	 * Sync accumualtor totals with acutal value totals - remove attenuation for DataPlane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void syncAccumTotals(String dimensionTag, String DataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return;
		dp.syncAccumTotals();
	}
	
	/**
	 * Get the count of numberSets for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public int getNSCount(String dimensionTag, String DataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return 0;
		return dp.getNSCount();
	}
	
	/**
	 * Get the mapped vector count for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public int getMappedVectorCount(String dimensionTag, String DataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return 0;
		return dp.getMappedVectorCount();
	}
	
	/**
	 * Get the numberSet positions for a numberSet id in a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber set number / index
	 * @return list of positions
	 */
	public List<Integer> getNS(String dimensionTag, String DataPlaneTag, int setNumber) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return null;
		return dp.getNS(setNumber);
	}	
	
	/**
	 * Set all numberSets to turned on or turned off in all dataplanes
	 * @param turnedOff true to turn off, false to turn on
	 */
	public void setCfgNSTurnedOff(boolean turnedOff) {
		for (int dp=0;dp<getDataPlaneCount();dp++) {
			getDataPlane(dp).setCfgNSTurnedOff(turnedOff);
		}
	}
	
	/**
	 * Set a numberSet to turned on or turned off based on the postions in the numberset
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumbers
	 * @param turnedOff true to turn off, false to turn on
	 * @return
	 */
	public boolean setCfgNSTurnedOff(String dimensionTag, String DataPlaneTag, List<Integer> setNumbers, boolean turnedOff) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return false;
		dp.setCfgNSTurnedOff(setNumbers, turnedOff);
		return true;
	}
	
	/**
	 * Set a numberSet to turned on or turned off
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber
	 * @param turnedOff true to turn off, false to turn on
	 * @return
	 */
	public boolean setCfgNSTurnedOff(String dimensionTag, String DataPlaneTag, int setNumber, boolean turnedOff) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return false;
		dp.setCfgNSTurnedOff(setNumber, turnedOff);
		return true;
	}	
	
	/**
	 * Set all numberSets to turned on or off
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param turnedOff true to turn all off, false to turn all on
	 */
	public void setCfgNSTurnedOff(String dimensionTag, String DataPlaneTag, boolean turnedOff) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return;
		dp.setCfgNSTurnedOff(turnedOff);
	}
	
	/**
	 * get a list of all numberSets that are turned on
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public List<Integer> getNSTurnedOn(String dimensionTag, String DataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return null;
		return dp.getNSTurnedOn();
	}
	
	/**
	 * check if a numberSet is turned on
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber
	 * @return
	 */
	public boolean isCfgNSTurnedOff(String dimensionTag, String DataPlaneTag, int setNumber) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return false;
		return dp.isCfgNSTurnedOff(setNumber);	
	}
	
	/**
	 * Get the count of numberSets that are turned on for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public int getNSTurnedOnCount(String dimensionTag, String DataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return 0;
		return dp.getNSTurnedOnCount();	
	}
	
	/**
	 * Remove all numberSets that are set to turned off
	 * NOTE: this will renumber all numbersets
	 *  
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void removeCfgNSTurnedOff(String dimensionTag, String DataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return;
		dp.removeCfgNSTurnedOff();
	}
	
	/**
	 * Get the full numberSet id for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public int getCfgNSFullNumber(String dimensionTag, String DataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return 0;
		return dp.getCfgNSFullNumber();			
	}
	
	/**
	 * Get the identity numberSet id for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public int getCfgNSIdentityNumber(String dimensionTag, String DataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return 0;
		return dp.getCfgNSIdentityNumber();			
	}
	
	/**
	 * Set the full set number/ recall number set
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param fullSetNumber set number for full
	 */
	public void setCfgNSFullNumber(String dimensionTag, String dataPlaneTag, int fullSetNumber) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgNSFullNumber(fullSetNumber);
	}
	
	/**
	 * set the identity set number for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param idSetNumber set number for identity
	 */
	public void setCfgNSIdentityNumber(String dimensionTag, String dataPlaneTag, int idSetNumber) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		dataPlane.setCfgNSIdentityNumber(idSetNumber);
	}
	
	/**
	 * get a formated numberSet string representation
	 * - x X x - 
	 * 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber numberSet index
	 * @return
	 */
	public String getNSFormatString(String dimensionTag, String dataPlaneTag, int setNumber) {
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;
		return dataPlane.getNSFormatString(setNumber);
	}
	
	//
	// get a list of number set numbers based on how many segments you want and what segment
	// IF TRAINING YOU MUST ALWAYS USE NUMBERSET 1 FIRST: unless you are not retaining vectSets
	// this may differ for any given altered dataplane
	public List<Integer> getNSSubset(int windowSize, int segment, int numberOfSegments) {
		int ns = MLNumberSetUtil.getSetFull(windowSize).size();
		// segment 1 always includes full set
		int cnt = ns / numberOfSegments;
		List<List<Integer>> subSets = MLNumberSetUtil.getSubset(windowSize);
		List<Integer> nsl = new ArrayList<>();
		if (segment == 1) nsl.add(subSets.size()-1); // full set first		
		int start = (segment-1)*cnt;
		int end = start+cnt;
		if (segment == numberOfSegments) end = (subSets.size()-1); // last was in the first
		
		for (int i=start;i<end;i++) nsl.add(i);
		//System.out.println("  SET["+segment+" of "+numberOfSegments+"]["+cnt+"/"+ns+"]w["+this.window+"]("+nsl.size()+") "+setToString(nsl));
		return nsl;
	}
	
	/**
	 * get the window size for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public int getCfgWindowSize(String dimensionTag, String DataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return -1;
		return dp.getCfgWindowSize();		
	}
	
	/**
	 * Set the window size for a dataplane
	 * 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param window window size to set
	 */
	public void setCfgWindowSize(String dimensionTag, String DataPlaneTag, int window) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return;
		dp.setCfgWindowSize(window);
		dp.updateCfgNS();
	}

	/**
	 * Get count of vectors in DataPlane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public int getVectorCount(String dimensionTag, String DataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return 0;
		return dp.getVectorCount();		
	}

	/**
	 * Get count of accumulaots(or equivilent) in DataPlane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public int getAccumCount(String dimensionTag, String DataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, DataPlaneTag);
		if (dp == null) return 0;
		return dp.getAccumCount();
	}

	/**
	 * Add a dataplane with the information
	 * window size default to 1
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dataValueSetSize number of values in tag space
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int dataValueSetSize) {
		addDataPlane(dimensionTag, dataPlaneTag, 1, 0, dataValueSetSize);
	}
	
	/**
	 * Add a dataplane with the information
	 * window size default to 1
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dataValueSetSize number of values in tag space
	 * @param nswBase numberSet base type
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int dataValueSetSize, NSWeightBase nswBase) {
		addDataPlane(dimensionTag, dataPlaneTag, 1, 0, dataValueSetSize, nswBase);
	}
	
	/**
	 * Add a dataplane with the information
	 * window size default to 1
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dataValueSetSize number of values in tag space
	 * @param setType number set type, for numbersets to add
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int dataValueSetSize, NumberSetType setType) {
		addDataPlane(dimensionTag, dataPlaneTag, 1, 0, dataValueSetSize, setType);
	}
	
	/**
	 * Add a dataplane with the information
	 * window size default to 1
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dataValueSetSize number of values in tag space
	 * @param setType number set type, for numbersets to add
	 * @param nswBase numberSet base type
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int dataValueSetSize, NumberSetType setType, NSWeightBase nswBase) {
		addDataPlane(dimensionTag, dataPlaneTag, 1, 0, dataValueSetSize, setType, nswBase);
	}
	
	/**
	 * Add a dataplane with the information
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param windowSize size of window
	 * @param dataValueSetSize number of values in tag space
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int windowSize, int dataValueSetSize) {
		addDataPlane(dimensionTag, dataPlaneTag, windowSize, -1, dataValueSetSize);
	}
	
	/**
	 * Add a dataplane with the information
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param windowSize size of window
	 * @param dataValueSetSize number of values in tag space
	 * @param nswBase numberSet base type
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int windowSize, int dataValueSetSize, NSWeightBase nswBase) {
		addDataPlane(dimensionTag, dataPlaneTag, windowSize, -1, dataValueSetSize, nswBase);
	}
	
	/**
	 * Add a dataplane with the information
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param windowSize size of window
	 * @param dataValueSetSize number of values in tag space
	 * @param setType number set type, for numbersets to add
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int windowSize, int dataValueSetSize, NumberSetType setType) {
		addDataPlane(dimensionTag, dataPlaneTag, windowSize, -1, dataValueSetSize, setType);
	}
	
	/**
	 * Add a dataplane with the information
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param windowSize size of window
	 * @param dataValueSetSize number of values in tag space
	 * @param setType number set type, for numbersets to add
	 * @param nswBase numberSet base type
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int windowSize, int dataValueSetSize, NumberSetType setType, NSWeightBase nswBase) {
		addDataPlane(dimensionTag, dataPlaneTag, windowSize, -1, dataValueSetSize, setType, nswBase);
	}
		
	/**
	 * Add a dataplane with the information
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param windowSize size of window
	 * @param focus offset of the frame focus
	 * @param dataValueSetSize number of values in tag space
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int windowSize, int focus, int dataValueSetSize) {
		VDataPlane dp = getOrAddDataPlane(dimensionTag, dataPlaneTag, windowSize, focus);
		dp.setCfgDataWidth(dataValueSetSize);
	}
	
	/**
	 * Add a dataplane with the information
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param windowSize size of window
	 * @param focus offset of the frame focus
	 * @param dataValueSetSize number of values in tag space
	 * @param nswBase numberSet base type
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int windowSize, int focus, int dataValueSetSize, NSWeightBase nswBase) {
		VDataPlane dp = getOrAddDataPlane(dimensionTag, dataPlaneTag, windowSize, focus);
		dp.setCfgDataWidth(dataValueSetSize);
		dp.setCfgNSWeight(nswBase);
	}
	
	/**
	 * Add a dataplane with the information
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param windowSize size of window
	 * @param focus offset of the frame focus
	 * @param dataValueSetSize number of values in tag space
	 * @param setType number set type, for numbersets to add
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int windowSize, int focus, int dataValueSetSize, NumberSetType setType) {
		VDataPlane dp = getOrAddDataPlane(dimensionTag, dataPlaneTag, windowSize, focus, setType);
		dp.setCfgDataWidth(dataValueSetSize);
	}
	
	/**
	 * Add a dataplane with the information
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param windowSize size of window
	 * @param focus offset of the frame focus
	 * @param dataValueSetSize number of values in tag space
	 * @param setType number set type, for numbersets to add
	 * @param nswBase numberSet base type
	 */
	public void addDataPlane(String dimensionTag, String dataPlaneTag, int windowSize, int focus, int dataValueSetSize, NumberSetType setType, NSWeightBase nswBase) {
		VDataPlane dp = getOrAddDataPlane(dimensionTag, dataPlaneTag, windowSize, focus, setType);
		dp.setCfgDataWidth(dataValueSetSize);
		dp.setCfgNSWeight(nswBase);
	}
	
	/**
	 * get the data width; the number of value tags associated with the dataSet
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public int getCfgDataWidth(String dimensionTag, String dataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return -1;
		return dp.getCfgDataWidth();
	}	
	
	/**
	 * get the input data type for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public DSDataType getCfgInputDataType(String dimensionTag, String dataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		return dp.getCfgInputDataType();
	}
	
	/**
	 * Set the input data type for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param inputDataType data type the dataplane accepts
	 */
	public void setCfgInputDataType(String dimensionTag, String dataPlaneTag, DSDataType inputDataType) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.setCfgInputDataType(inputDataType);
	}

	/**
	 * set the input valueId that is NOT a value for a dataplane: default is 0
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dataNonValue
	 */
	public void setCfgNonValue(String dimensionTag, String dataPlaneTag, long dataNonValue) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.setCfgNonValue(dataNonValue);
	}
	
	/**
	 * get the input non-valueId for a dataplane
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public long getCfgNonValue(String dimensionTag, String dataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return -1;
		return dp.getCfgNonValue();
	}	
	
	// 
	// get the number of tiers in the input data
	// default is 1, multiple allow sub-elements
	//
	public int getCfgInputDataTiers(String dimensionTag, String dataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return 0;
		return dp.getCfgInputDataTiers();
	}
	// manage the data definition
	public VDataSetDescriptor getCfgDataDefinition(String dimensionTag, String dataPlaneTag) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		return dp.getCfgDataDefinition();
	}
	public void setCfgDataDefinition(String dimensionTag, String dataPlaneTag, VDataSetDescriptor dataDef) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.setCfgDataDefinition(dataDef);
	}
	public void setCfgDataDefinitionInput(String dimensionTag, String dataPlaneTag, String name, DSInputSetType SetType, DSDataType DataType, boolean independent) {
		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.setCfgDataDefinitionInput(name, SetType, DataType, independent);
	}
	

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// get Probability sets
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



	// Decision
	// @param  dimension  dimensions to use for dicisions
	// @param  dataplane  data plane to get info from
	// @param  dataSet  list of input data ordered
	// @param  focusOffset  offset of data item
	// @param  vpList  empty list for results
	// @return PredictionType type of response 
	// 
	// get list of all values and probability
	PredictionType getValProbs(VContext ctx, String dimensionTag, String dataplaneTag, List<Long> valueOut, VDataSets dss, int position, int dataSetNumber, List<ValProb> vpList) {
		if (inCount == 0) return null;
		VDataPlane dataplane = getDataPlane(dimensionTag, dataplaneTag);
		if (dataplane == null) return null;
		
		boolean predict = true;
		
		VFrame frame = new VFrame();
		if (ctx == null) ctx = new VContext(this);			
		if (!dataplane.getFramer().makeFrameSetup(ctx, dataplane, frame, dataplane.getFramerArg(), predict, valueOut, dss, 0, position)) return null;
	
		PredictionType ret = dataplane.getValPList(ctx, frame, valueOut, false, null, false);
		vpList.clear();
		vpList.addAll(frame.vpList);
		Collections.sort(vpList, VegUtil.VpSort);
		return ret;
	}
	PredictionType getValProbs(VContext ctx, String dimensionTag, String dataplaneTag, List<Long> valueOut, VDataSet ds, int position, int dataSetNumber, List<ValProb> vpList) {
		return getValProbs(ctx, dimensionTag, dataplaneTag, valueOut, new VDataSets(ds), position, dataSetNumber, vpList);
	}


	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// UTIL
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// make the subsets vector to match
	//
	static int [] makeSubVector(int [] vectSet, List<Integer> set, List<Integer> subset) {
		if (vectSet == null || set == null || set == null) return null;
		if (set.size() < subset.size() || vectSet.length < set.size()) return null;
		
		int [] sVectSet = new int[subset.size()];
		for (int i=0;i<subset.size();i++) {
			int p = subset.get(i).intValue();
			for (int k=0;k<set.size();k++) {
				if (set.get(k).intValue() == p) sVectSet[i] = vectSet[k];
			}
		}
		return sVectSet;
	}

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// UTILS String mapping
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Set dataplane to use the same string map for dimension and dataplane values
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void setStringMappingMerged(String dimensionTag, String dataPlaneTag) {
		VDataPlane dataplane = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return;	
		dataplane.setStrDimensionMapID(dataplane.getStrMapID());
	}
	
	//
	// get the string mappings for mapId
	//
	// @param mapId  map to look for string
	// @return string for vector
	//
	public TIntObjectHashMap<String> getMap(String dimensionTag, String dataPlaneTag) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		return vectStrMap.getMap(dp.getStrMapID());
	}
	
	/**
	 * Add a string into the string map registered for this dimension
	 * @param dimensionTag dimension tag
	 * @param valueId valueId to set
	 * @param string string to add
	 */
	public void addStringMapping(String dimensionTag, int valueId, String string) {
		this.vectStrMap.add(dimensionTag, valueId, string);
	}	
	
	/**
	 * Add a string into the string map registered for this dimension
	 * @param dimensionTag dimension tag
	 * @param string string to add
	 */
	public void addStringMapping(String dimensionTag, String string) {
		this.vectStrMap.add(dimensionTag, VectorToVid.toVectorGen(string), string);
	}	

	/**
	 * Add a string into the string map registered for this dimension
	 * @param dimensionTag dimension tag
	 * @param strings collection of strings to add
	 */
	public void addStringMapping(String dimensionTag, Collection<String> strings) {
		for (String s:strings) this.vectStrMap.add(dimensionTag, VectorToVid.toVectorGen(s), s);
	}
	
	/**
	 * Add a string into the string map registered for this dataPlane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param valueId valueId to use
	 * @param string string to use
	 */
	public void addStringMapping(String dimensionTag, String dataPlaneTag, int valueId, String string) {
		VDataPlane dataplane = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return;		
		dataplane.addString(valueId, string);
	}

	/**
	 * Add a string into the string map registered for this dataPlane
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param string string to add
	 */
	public void addStringMapping(String dimensionTag, String dataPlaneTag, String string) {
		VDataPlane dataplane = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return;
		dataplane.addString(dataplane.getCfgVToV().toVectGen(string), string);
	}

	/**
	 * Add a collection of strings into the string map registered for this dataPlane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param strings collection of strings to add to string table
	 */
	public void addStringMapping(String dimensionTag, String dataPlaneTag, Collection<String> strings) {
		VDataPlane dataplane = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return;
		for (String s:strings) {
			dataplane.addString(dataplane.getCfgVToV().toVectGen(s), s);
		}
	}
	
	/**
	 * Add a dataset value strings into the string map registered for this dataPlane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dss dataset with values as string type
	 */
	public void addStringMappingValues(String dimensionTag, String dataPlaneTag, VDataSets dss) {
		VDataPlane dataplane = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return;
		if (!dss.isFmtValueS()) return;
		Set<String> ss = dss.getAllValueStrings();
		for (String s:ss) {
			dataplane.addString(dataplane.getCfgVToV().toVectGen(s), s);
		}
	}	
	
	/**
	 * Add a dataset data strings into the string map registered for this dataPlane
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param dss dataset with values as string type
	 */
	public void addStringMappingData(String dimensionTag, String dataPlaneTag, VDataSets dss) {
		VDataPlane dataplane = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return;
		if (!dss.isFmtValueS()) return;
		Set<String> ss = dss.getAllDataStrings();
		for (String s:ss) {
			dataplane.addString(dataplane.getCfgVToV().toVectGen(s), s);
		}
	}
	
	/**
	 * Get a String for the vector if mapped in dimension
	 * @param dimensionTag dimension tag
	 * @param valueId value to get string for
	 * @return String or null
	 */
	public String getStringMapping(String dimensionTag, int valueId) {
		String s = this.vectStrMap.get(dimensionTag, (int)valueId);
		if (s != null) return s;
		return "";
	}
	
	/**
	 * Get a String for the vector if mapped in dimension
	 * @param dimensionTag dimension tag
	 * @param valueId value to get string for
	 * @return String or null
	 */
	public String getStringMapping(String dimensionTag, long valueId) {
		return getStringMapping(dimensionTag, (int)valueId);
	}	
	
	/**
	 * Get a String for the vector if mapped in dimension
	 * @param dimensionTag dimension tag
	 * @param vectorSet list of valueIds/vectSet
	 * @return
	 */
	public String getStringMapping(String dimensionTag, int [] vectorSet) {
		if (vectorSet == null) return "";
		
		String w = "";
		for (int v:vectorSet) {
			String str = this.vectStrMap.get(dimensionTag, v);

			if (str == null) str = "<"+v+">";
			w += str + " ";
		}
		return  w.trim();
	}	
	
	/**
	 * Get string for dataplane int valueId
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param valueId valueId to lookup
	 * @return String or null
	 */
	public String getStringMapping(String dimensionTag, String dataPlaneTag, int valueId) {
		VDataPlane dataplane = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return null;		
		return dataplane.getString(valueId);
	}
	
	/**
	 * Get string for dataplane long valueId
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param valueId valueId to lookup
	 * @return String or null
	 */
	public String getStringMapping(String dimensionTag, String dataPlaneTag, long valueId) {
		VDataPlane dataplane = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return null;		
		return dataplane.getString(valueId);
	}
	
	/**
	 * Get string for dataplane long valueId
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param valueId valueId to lookup
	 * @return String or null
	 */
	public String getStringMapping(String dimensionTag, String dataPlaneTag, int [] vectorSet) {
		VDataPlane dataplane = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataplane == null) return null;		
		return dataplane.getString(vectorSet);
	}
	
	/**
	 * Get a vector set for a vector set id
	 * This only works if saveVectorSets is enabled
	 * 
	 * NOTE: this would be simpler if called from a dataPlane with a numberSet id to remove fset & set params
	 * 
	 * @param vsid vector set id
	 * @param fset the full NumberSet position array to extract
	 * @param set the NumberSet position array to extract
	 * @return vector with valueIds for dimension values
	 */
	public int [] getVectorSetForId(int vsid, List<Integer> fset, List<Integer> set) {
		return vectSetMap.get(fset, set, vsid);
	}
	
	/**
	 * Get a vector set for a vector set id, full vector
	 * This only works if saveVectorSets is enabled
	 * 
	 * @param vsid vector set id
	 * @return vector with valueIds for dimension values
	 */
	public int [] getVectorSetForId(int vsid) {
		return vectSetMap.getBase(vsid);
	}


	/**
	 * true if this dimension string has been trained/learned
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param dimensionString string to check for
	 * @return
	 */
	public boolean haveDimensionString(String dimensionTag, String dataPlaneTag, String dimensionString) {
		if (dimensionString == null) return false;
		if (dimensionTag == null || dataPlaneTag == null) return false;
		VDataPlane dataPlane = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return false;
		return (dataPlane.haveDimensionString(dimensionString) != 0);
	}

	
	//
	// rollup a segment into the largest trained elements
	// NOTE: this requires elements to be built up 1 token at a time, or it will miss (change later maybe)
	//
	public List<String> rollUpSegment(String dimensionTag, String dataPlaneTag, List<String> segment, int off) {
		if (segment == null || segment.size() < 2) return segment;
		if (dimensionTag == null || dataPlaneTag == null) return segment;
		VDataPlane dataPlane = this.getDataPlane(dimensionTag, dataPlaneTag);
		MLNumberSetHash nsh = dataPlane.getNSHash(dataPlane.getCfgNSIdentityNumber());
		List<String> ol = new ArrayList<>();
		// look for it in identity
		for (int i=0;i<segment.size();i++) {
			String bs = segment.get(i);
			boolean found = false;
			int last = -1;
			for (int xi=(i+1);xi<segment.size();xi++) {
				String cs = segment.get(xi);
				bs += " " + cs;
				// combine with next and test
				int vid = dataPlane.getCfgVToV().toVectGen(bs);
				if (nsh.get(vid) == null) {
					//if (off < 20) System.out.println("     nope["+off+"] ["+i+"/"+xi+"] X["+segment.size()+" => "+ol.size()+"] " +bs);
				} else if (xi > i) {
					if (ol.size() == 0 && xi == (segment.size()-1)) {
					} else {
						// have
						//if (off < 20) System.out.println("    got["+off+"] ["+i+"/"+xi+"] X["+segment.size()+" => "+ol.size()+"] " +bs);
						found = true;
						last = xi;
					}
				}
			}
			if (found) {
				String bsx = "";
				for (int xi=i;xi<=last;xi++) bsx += segment.get(xi) + " ";
				ol.add(bsx.trim());
				//if (off < 20) System.out.println("   ADD1["+off+"] X["+segment.size()+" => "+ol.size()+"] " +bsx);

				i = last; // skip

			} else {
				ol.add(segment.get(i));
				//if (off < 20) System.out.println("   ADD2["+off+"] X["+segment.size()+" => "+ol.size()+"] " +segment.get(i));
			}
		}
		return ol;
	}	
	
	//
	// find a dimension strings predicted value
	//
	public List<ValProb> findString(String dimensionTag, String dataPlaneTag, String dimensionString) {
		if (dimensionString == null) return null;
		String [] ds = new String[1];
		ds[0] = dimensionString;
		return findStringSequence(dimensionTag, dataPlaneTag, ds);
	}	
	
	//
	// in sequence - null for <any> string in position and show what it is predicted to be
	//
	public List<ValProb> findStringSequence(String dimensionTag, String dataPlaneTag, String [] dimensionStringSequence) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		
		System.out.println(" findStringSequence["+dimensionTag+"/"+dataPlaneTag+"] \'" + VegUtil.toStringList(dimensionStringSequence)+"\'");
		
		// make a vector id
		// make a vectSet
		int [] vectSet = new int[dimensionStringSequence.length];
		for (int i=0;i<dimensionStringSequence.length;i++) {
			vectSet[i] = dp.getCfgVToV().toVectGen(dimensionStringSequence[i]);
		}
		
		// get list of all vectSets with value at identity
		List<int []> vectSets = vectSetMap.getSubsetsListContains(vectSet, dp.getCfgFrameFocus());
		if (vectSets == null || vectSets.size() < 1) return null;
		//System.out.println(" findStringSequence vs["+vectSets.size()+"] vectSet: " + NumberSets.setToString(vectSet));
		
		Accum dac = dp.getAccumDefault();
		List<Accum> acList = new ArrayList<>();
		List<ValProb> vpList = new ArrayList<>();
		Set<Long> vSet = new HashSet<>();

		List<Integer> fset = dp.getCfgNSFull();
		
		// for each numberset that contains identityNumber
		for (int i=0;i<dp.getNSCount();i++) {
			MLNumberSetHash nsh = dp.getNSHash(i);			
			if (nsh == null || nsh.isTurnedOff() || nsh.size() < 1) continue;
			List<Integer> set = dp.getNS(i);
			if (set.size() < vectSet.length) continue;
		
			if (!MLNumberSetUtil.setContainsOffset(set, dp.getCfgFrameFocus())) continue;
			if (vectSet.length != 1) {
				// must contain the sequence AND possibly more
				if (!MLNumberSetUtil.isSetContainsSequence(set, vectSet.length, dp.getCfgFrameFocus())) continue;
			}
			
			vSet.clear();
			// generate the unique set of vectors from the vector set
			for (int xi=0;xi<vectSets.size();xi++) {
				int vs[] = vectSets.get(xi);
				// check that these do have the correct content in the correct possitions
				int [] subVectSet = makeSubVector(vs, fset, set);
				if (!MLNumberSetUtil.containsVectSet(subVectSet, vectSet)) continue;
				
				long v = VectorToVid.toVectorV64(subVectSet);			
				//if (subVectSet.length == 1) System.out.println("         -->["+v+"] "  +NumberSets.setToString(vs));
				vSet.add(v);
			}
			
			List<Accum> sacList = new ArrayList<>();
			List<ValProb> svpList = new ArrayList<>();
			String setShow = dp.getNSFormatString(i);
			Accum sac = nsh.getAccumSetDefault();
			
			// get full set of Accums for these vids
			for (long v:vSet) {
				// get 
				Accum ac = nsh.get(v);
				if (ac != null) {
					sacList.add(ac);

					//double balance = ac.getBalance(dp.getCfgDataWidth());
					// calculate and sum all accum from all sets					
					// this is the most likely to be correct
					List<ValProb> vpAList = ac.getValPs();
					for (int xi=0;xi<vpAList.size();xi++) {
						ValProb vp = vpAList.get(xi);
						vp.probability = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), i, false, sac, ac, vp);
					}						
					svpList = VegUtil.mergeVPList(svpList, ac.getValPs());
				}
			}

			Collections.sort(svpList, VegUtil.VpSort);
			String s = "";
			for (int xi=0;xi<svpList.size();xi++) {
				ValProb vp = svpList.get(xi);		
				s += "<\'"+dp.getString(vp.value)+"\' "+vp.count+"/"+vp.probability + "> ";				
			}
			System.out.println("   set["+String.format("%3d", i)+"]["+setShow+"]["+sacList.size()+"]  => " + s);
			acList.add(dac);
			vpList = VegUtil.mergeVPList(vpList, svpList);
		}
		
		if (vpList.size() > 0) {
			Collections.sort(vpList, VegUtil.VpSort);
			String s = "";
			for (int xi=0;xi<vpList.size();xi++) {
				ValProb vp = vpList.get(xi);		
				s += "<\'"+dp.getString(vp.value)+"\' "+vp.count+"/"+vp.probability + "> ";				
			}
			System.out.println("   Predicted["+vpList.size()+"] " + s);
		} else {
			System.out.println("   Predicted[0] NONE" );
		}
		return vpList;
	}

	//
	// find dimensionString and show what it predicts
	//
	public List<ValProb> findStringPredictor(String dimensionTag, String dataPlaneTag, String dimensionString) {
		if (dimensionString == null) return null;
		String [] ds = new String[1];
		ds[0] = dimensionString;
		return findStringSequencePredictor(dimensionTag, dataPlaneTag, ds);
	}
	
	//
	// in sequence - null for <any> string in position and show what it predicts
	//
	public List<ValProb> findStringSequencePredictor(String dimensionTag, String dataPlaneTag, String [] dimensionStringSequence) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		
		System.out.println(" findStringPredictor["+dimensionTag+"/"+dataPlaneTag+"] \'" + VegUtil.toStringList(dimensionStringSequence)+"\'");

		List<Accum> acList = new ArrayList<>();
		List<ValProb> vpList = new ArrayList<>();
		
		// make a vectSet
		int [] vectSet = new int[dimensionStringSequence.length];
		for (int i=0;i<dimensionStringSequence.length;i++) {
			vectSet[i] = dp.getCfgVToV().toVectGen(dimensionStringSequence[i]);
		}
		long v = VectorToVid.toVectorV64(vectSet);
		Accum dac = dp.getAccumDefault();

		// show in each set of 1
		for (int i=0;i<dp.getNSCount();i++) {
			MLNumberSetHash nsh = dp.getNSHash(i);			
			if (nsh == null || nsh.isTurnedOff() || nsh.size() < 1) continue;
	
			if (nsh.getNSSize() != vectSet.length) continue;
			if (vectSet.length != 1 && !MLNumberSetUtil.isSetSequence(dp.getNS(i))) continue; // not a sequenc
	
			String setShow = dp.getNSFormatString(i);
			Accum ac = nsh.get(v);
			if (ac != null) {
				acList.add(ac);

				//double balance = ac.getBalance(dp.getCfgDataWidth());
				// show this set
				System.out.println("   set["+i+"]["+setShow+"]["+ac.getTotal()+"]c["+ac.getCorrectness()+"] => " + ac.getDistString(dp));
				
				Accum sac = nsh.getAccumSetDefault();
	
				// this is the most likely to be correct
				List<ValProb> vpAList = ac.getValPs();
				for (int xi=0;xi<vpAList.size();xi++) {
					ValProb vp = vpAList.get(xi);
					vp.probability = dp.getCfgPCalc().calculateProb(dp.getCfgProbMethod(), dp, dac, dp.getCfgNSWeightRaw(), i, false, sac, ac, vp);
				}						
				vpList = VegUtil.mergeVPList(vpList, ac.getValPs());
			} else {
				System.out.println("   set["+String.format("%3d", i)+"]["+setShow+"] not found: " + v);
			}
		}
		if (vpList.size() > 0) {
			Collections.sort(vpList, VegUtil.VpSort);
			String s = "";
			for (int xi=0;xi<vpList.size();xi++) {
				ValProb vp = vpList.get(xi);		
				s += "<\'"+dp.getString(vp.value)+"\' "+vp.count+"/"+vp.probability + "> ";				
			}
			System.out.println("   Predictor["+vpList.size()+"] " + s);
		} else {
			System.out.println("   Predictor[0] NONE" );
		}
		return vpList;
	}
	
	//
	// in sequence - null for <any> string in position and show what it predicts
	//
	public List<ValProb> findStringIdentity(String dimensionTag, String dataPlaneTag, String [] vectorString) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		
		// make a vectSet
		int [] vectSet = new int[vectorString.length];
		for (int i=0;i<vectorString.length;i++) {
			vectSet[i] = dp.getCfgVToV().toVectGen(vectorString[i]);
		}
		return dp.getDimensionStringProbList(VectorToVid.toVectorV64(vectSet));
	}
	
	//
	// find dimensionString and show what it predicts
	//
	public List<ValProb> findStringIdentity(String dimensionTag, String dataPlaneTag, String dimensionString) {
		if (dimensionString == null) return null;
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		return dp.getDimensionStringProbList(dimensionString);
	}

	/**
	 * remove all of a value from a dataplanes values (except identity)
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param valueId valueId to remove
	 */
	public void removeAllValueId(String dimensionTag, String dataPlaneTag, long valueId) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.removeAllNSValueExceptIdentity(valueId, true);
	}

	/**
	 * remove all of a value from a dataplanes values (except identity)
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param obj object to get valueId for
	 */
	public void removeAllValueId(String dimensionTag, String dataPlaneTag, Object obj) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		long valueId = dp.getCfgVToV().toVectGen(obj);
		dp.removeAllNSValueExceptIdentity(valueId, true);
	}	
	
	/**
	 * remove all of a value from a dataplanes values (except identity)
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param obj object to get valueId for
	 */
	public void removeAllValueId(String dimensionTag, String dataPlaneTag, Object [] obj) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		long valueId = dp.getCfgVToV().toVectGen(obj);
		dp.removeAllNSValueExceptIdentity(valueId, true);
	}
	
	/**
	 * get vector set id (full) for vector in set, if available
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber NumberSet to check
	 * @param vid vector id
	 * @return vectorSetId or -1
	 */
	public int findCfgNSVsid(String dimensionTag, String dataPlaneTag, int setNumber, long vid) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return 0;
		return dp.findCfgNSVsid(setNumber, vid);
	}
	
	/**
	 * Find vector Set for vector, if available
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber numberSet to check
	 * @param vid vector id
	 * @return vector set or null
	 */
	public int [] findCfgNSVectSet(String dimensionTag, String dataPlaneTag, int setNumber, long vid) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		int vsid = dp.findCfgNSVsid(setNumber, vid);
		if (vsid <= 0) return null;
		return getVectorSetForId(vsid);
	}
		
	/**
	 * get Value Probability list for the vector in specified numberset
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber NumberSet id to use
	 * @param vid vector id
	 * @return List of ValProbs sorted
	 */
	public List<ValProb> findCfgNSProbList(String dimensionTag, String dataPlaneTag, int setNumber, long vid) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		return dp.findCfgNSProbList(setNumber, vid);
	}
	
	/**
	 * get Value Probability for valueId in numberSet and vector
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber NumberSet id to use
	 * @param vid vector id
	 * @param valueId valueId to find
	 * @return ValProb or null
	 */
	public ValProb findCfgNSProbList(String dimensionTag, String dataPlaneTag, int setNumber, long vid, long valueId) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		return dp.findCfgNSProbList(setNumber, vid, valueId);
	}
	
	/**
	 * Get the full list of ValProbs for valueIds from a dataplane's Identity numberSet frequency sorted
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return list of ValProbs
	 */
	public List<ValProb> getIdentityFrequencyProbList(String dimensionTag, String dataPlaneTag) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		// look for it in identity
		return dp.getIdentityFrequencyProbList();
	}
	
	/**
	 * Get the full list of ValProbs for valueIds from a dataplane's numberSet frequency sorted
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber NumberSet id
	 * @return list of ValProbs
	 */
	public List<ValProb> getNSFrequencyProbList(String dimensionTag, String dataPlaneTag, int setNumber) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		// look for it in identity
		return dp.getNSFrequencySorted(setNumber);
	}
	
	/**
	 * Get the size / number of vectors for a numberSet
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber numberSet id to check
	 * @return count of vectors
	 */
	public int getNSSize(String dimensionTag, String dataPlaneTag, int setNumber) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return 0;
		return dp.getNSSize(setNumber);
	}
	
	/**
	 * Get the full set of accumulators for a NumberSet frequency sorted
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param setNumber numberSet to get accumulators for
	 * @return list of accumulators
	 */
	public List<Accum> getNSAccumSorted(String dimensionTag, String dataPlaneTag, int setNumber) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return null;
		return dp.getNSAccumSorted(setNumber);
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// UTILS data manipulation
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * reduceCheck class to perform remove check on accumulators and values
	 * This is to be extended and overridden to create a customized reduction alg
	 */
	public static class reduceCheck {
		/**
		 * check Accumulator return true if it is to be removed
		 * 
		 * @param vML instance of Veg
		 * @param dataPlane dataplane being processed
		 * @param setNumber NumberSet being processed
		 * @param ac accumulator to check
		 * @return true to remove, false otherwise
		 */
		public boolean removeAccumulator(VegML vML, VDataPlane dataPlane, int setNumber, Accum ac) {
			return false;
		}
		
		/**
		 * return true if this reduceCheck implements removeAccumulatorValue and it is to be run 
		 * 
		 * @return
		 */
		public boolean checkValues() {
			return false;
		}	
		
		/**
		 * check valueId in accumulator return true to remove it
		 * 
		 * @param vML instance of Veg
		 * @param dataPlane dataplane being processed
		 * @param setNumber NumberSet being processed
		 * @param ac accumulator being processed
		 * @param vpList sorted ValProb list from accumulator
		 * @param position postion of value in ValProb list
		 * @param vp ValProb for value
		 * @return true to remove, false otherwise
		 */
		public boolean removeAccumulatorValue(VegML vML, VDataPlane dataPlane, int setNumber, Accum ac, List<ValProb> vpList, int position, ValProb vp) {
			return false;
		}
	}
	
	/**
	 * reduction processor
	 * Operates on a reduceCheck object that implements checks to reduce or not for accumulators and/or values
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param tester tester object
	 * @return count removed
	 */
	public long reduceScan(String dimensionTag, String dataPlaneTag, reduceCheck tester) {
		if (this.inCount < 1) return 0;

		VDataPlane dp = getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return -1;
		
		long rCnt = 0, aCnt = 0, vCnt = 0;
	
		if (isCfgShowProgress()) System.out.print("   START reduceScan["+dp.getDimensionTag()+"/"+dp.getTag()+"] sets[" + dp.getNSCount()+"] .");

		/////////////////////////////////////////////////
		// First base Probability Reduce
		for (int i=0;i<dp.getNSCount();i++) {
			// each of these is a different set where i == setNumber					
			MLNumberSetHash nsh = dp.getNSHash(i);
			//System.out.println("   SET["+i+"] HashSize[" + hm.size()+"] set: " + setToString(getSet(i)));	
			if (nsh == null || nsh.size() < 1 || nsh.isTurnedOff()) continue;
			//System.out.println("   SET["+i+"]  set: " + nsh.size());	

			aCnt += nsh.size();
			Set<Accum> delList = new HashSet<>();
			Iterator<Accum> it = nsh.iterator();
			while (it.hasNext()) {
				Accum ac = it.next();
				if (tester.checkValues()) {
					List<ValProb> vpList = ac.getValPsSorted();
					if (vpList != null && vpList.size() > 0) {
						for (int vp=0;vp<vpList.size();vp++) {
							boolean r = tester.removeAccumulatorValue(this, dp, i, ac, vpList, vp, vpList.get(vp));
							if (r) {
								ac.remove(vpList.get(vp).value);
								vCnt++;
							}
						}
					}
				}
				if (ac.getTotal() < 1) {
					delList.add(ac); // empty
				} else {
					boolean r = tester.removeAccumulator(this, dp, i, ac);
					if (r) delList.add(ac);
				}
			}
			
			// now delete for this set
			//System.out.println("remove: " +delList.size() + " tot: " + dac.getTotal());
			for (Accum ac:delList) {
				nsh.remove(ac.getVectorCode());
				rCnt++;
			}
			// make it good
			nsh.optimize();
		}
		int cCnt =0;
		for (int i=0;i<dp.getNSCount();i++) {
			// each of these is a different set where i == setNumber					
			MLNumberSetHash nsh = dp.getNSHash(i);
			//System.out.println("   SET["+i+"] HashSize[" + hm.size()+"] set: " + setToString(getSet(i)));	
			if (nsh == null || nsh.size() < 1 || nsh.isTurnedOff()) continue;
			cCnt += nsh.size();
		}
		if (cCnt > 0 && aCnt > 0) {
			double per = (double)100 - ((double)cCnt / (double)aCnt) * (double)100;	
			if (isCfgShowProgress()) System.out.println(".. COMPLETE reduced["+String.format("%.2f", per)+"%]["+aCnt+" -> "+cCnt+"]["+vCnt+"]");
		} else {
			if (dp.isSolid()) System.out.println(".. NO reduction SOLID Model");
			else if (isCfgShowProgress()) System.out.println(".. COMPLETE reduction["+rCnt+"]");
		}
		// reset counts
		dp.resetValueCounts();
		return rCnt;		
	}
	
	
	/**
	 * Entangle Accumulators for a dataplane
	 * This produces a smaller model with the same content and must be done before making solid 
	 * This must also be done before generating symbolic rules if they are to be rational
	 * 
	 *  1) fold together all that have exact probability set massive reduction for smaller sets (boolean/enum)
	 *  2) ALL non-conflicting Accumulators probabilities could be folded together as well
	 *  
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return 
	 */
	public long entangle(String dimensionTag, String dataPlaneTag) {
		if (this.inCount < 1) return 0;
		VDataPlane dpix = getDataPlane(dimensionTag, dataPlaneTag);
		if (dpix == null) return -1;
		return entangle(dpix);	
	}
	
	// 
	// entangle reduction DataPlane
	//
	long entangle(VDataPlane dataPlane) {
		if (this.inCount < 1) return 0;
		long rCnt = 0, cSize = 0, nSize = 0;
		if (isCfgShowProgress()) System.out.print("   START Entanglement["+dataPlane.getDimensionTag()+"/"+dataPlane.getTag()+"] sets["+dataPlane.getNSCount()+"] .");

		for (int setNumber=0;setNumber<dataPlane.getNSCount();setNumber++) {
			// each of these is a different set where i == setNumber
			MLNumberSetHash nsh = dataPlane.getNSHash(setNumber);
			if (nsh == null || nsh.size() < 1) continue;
			//System.out.print("  Ent["+nsh.getSetNumber()+" of "+dataPlane.getNSCount()+"] size["+nsh.size()+"] => ");
			cSize += nsh.size();
			rCnt += entangle(dataPlane, setNumber);
			nSize += nsh.size();
			//System.out.println("["+nsh.size()+"]");
		}
		double per0 = (double)100 - (((double)nSize / (double)cSize)*(double)100);
		if (isCfgShowProgress()) System.out.println(".. COMPLETE Reduction["+String.format("%.2f", per0)+"%] size["+cSize+" => "+nSize+"]");
		return rCnt;		
	}
	
	// 
	// entangle reduction Number Set
	//
	int entangle(VDataPlane dataPlane, int setNumber) {
		MLNumberSetHash nsh = dataPlane.getNSHash(setNumber);
		return entangle(dataPlane, nsh);
	}
	int entangle(VDataPlane dataPlane, MLNumberSetHash nsh) {
	
		// each of these is a different set where i == setNumber					
		if (nsh == null || nsh.size() < 1) return 0;
		if (nsh.isTurnedOff()) return 0;

		List<Accum> acList = nsh.getListSorted();
		Object [] acArray = acList.toArray();	// this is exponentially faster
		acList.clear();
		acList = null;

		//System.out.println("   SET["+i+"] HashSize[" + hm.size()+"/"+acList.size()+"] fProb: " + acList.get(0).getFirstMostProbablity());	
		// get start size and clear the map
		int stSize = nsh.size();
		nsh.clear();

		int mergeCmt = 0, matchCnt = 0;
		// get sorted list to work from for fast
		List<Accum> etgList = new ArrayList<>();	// get list to entangle
		
		for (int a=0;a<acArray.length;a++) {
			Accum ac = (Accum)acArray[a];
			if (ac == null) continue;
			double acProb = ac.getFirstMostProbablity();
			for (int a2=(a+1);a2<acArray.length;a2++) {
				Accum ac2 = (Accum)acArray[a2];
				if (ac2 == null) continue;
				if (ac2.getValueCount() != ac.getValueCount()) break;
				double ac2Prob = ac2.getFirstMostProbablity();
				if (ac2Prob != acProb) break;			
				// compare
				if (ac2.compare(ac)) {
					if (etgList.size() == 0) etgList.add(ac);	
					etgList.add(ac2);

					// remove from the list...
					acArray[a2] = null;
				}
			}
			if (etgList.size() > 1) {
				matchCnt++;
				mergeCmt += (etgList.size()-1);
				// entanglement..
				entangleAccums(nsh, etgList);
			} else {
				nsh.putDirect(ac, ac.vectorCode);				
			}
		}
	
		//int nSize = nsh.size();

		//double per0 = (double)100 - (((double)nSize / (double)stSize)*(double)100);
		// re-balance the hash
		nsh.optimize();
		//System.out.println("    SET["+dataPlane.getDimension().getIndex()+"/"+dataPlane.getIndex()+":"+hm.getSetNumber()+"]["+Gtil.toRatio2(per0)+"%] [match: "+matchCnt+" merge: "+mergeCmt+"] SIZE["+stSize+" => " + nSize+"]");	
		return mergeCmt;
	}
	
	//
	// entangle them and return the remaining one: it will have the vector to use and the VectSets needed
	// may also be used to map symbols together as desired: bob & bobby && robert
	//
	void entangleAccums(MLNumberSetHash hm, List<Accum> mlist) {
		Accum ac = mlist.remove(0);

		// map for the count
		hm.putDirect(ac, ac.vectorCode); // add direct
		hm.putVectorMapVector(ac.getVectorCode(), ac.getVectorCode(), ac.getVectSetId(), ac.getTotal());			
		
		for (int i=0;i<mlist.size();i++) {
			Accum pac = mlist.get(i);
			// add vector remapping
			hm.putVectorMapVector(pac.getVectorCode(), ac.getVectorCode(), pac.getVectSetId(), pac.getTotal());				
			// merge counts
			ac.merge(pac);
		}
		ac.setVectSetId(-1); // clear it
		mlist.clear();
	}

	/**
	 * De-Entangle accumulators for a dataplane
	 * Undo the entangling of an entangled dataplane
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public long deentanglement(String dimensionTag, String dataPlaneTag) {
		if (this.inCount < 1) return 0;
		VDataPlane dpix = getDataPlane(dimensionTag, dataPlaneTag);
		if (dpix == null) return -1;
		return deentanglement(dpix);	
	}

	// 
	// de-entangle DataPlane
	//
	long deentanglement(VDataPlane dataPlane) {
		if (this.inCount < 1) return 0;
		long rCnt = 0;
		//System.out.print("   START Entanglement["+dataPlane.getTag()+"/"+dataPlane.getTag()+"] sets["+dataPlane.getNSCount()+"]");

		for (int setNumber=0;setNumber<dataPlane.getNSCount();setNumber++) {
			// each of these is a different set where i == setNumber
			MLNumberSetHash nsh = dataPlane.getNSHash(setNumber);
			if (nsh == null || nsh.size() < 1) continue;
			rCnt += deentanglement(dataPlane, setNumber);
		}
		//double per0 = (double)100 - (((double)nSize / (double)cSize)*(double)100);
		//System.out.println("  COMPLETE Reduction["+Gtil.toRatio2(per0)+"%] size["+cSize+" => "+nSize+"]");
		return rCnt;		
	}
	//
	// de-entangle a number set (used for merge)
	//
	int deentanglement(VDataPlane dataPlane, int setNumber) {
		MLNumberSetHash nsh = dataPlane.getNSHash(setNumber);
		return deentanglement(dataPlane, nsh);
	}
	int deentanglement(VDataPlane dataPlane, MLNumberSetHash nsh) {
		// each of these is a different set where i == setNumber					
		if (nsh == null || nsh.size() < 1) return 0;
		if (nsh.isTurnedOff()) return 0;
		// is this set entangled?
		if (!nsh.isEntangled()) return 0;
		
		int setNumber = nsh.getSetNumber();
		
		List<Accum> acList = nsh.getListSorted();
		Object [] acArray = acList.toArray();	// this is exponentially faster
		acList.clear();
		acList = null;
		
		//System.out.println("   SET["+i+"] HashSize[" + hm.size()+"/"+acList.size()+"] fProb: " + acList.get(0).getFirstMostProbablity());	
		// get start size and clear the map
		int stSize = nsh.size();
		nsh.clear();

		int expCnt = 0, noVssidCnt = 0;
		// get sorted list to work from for fast
		for (int a=0;a<acArray.length;a++) {
			Accum ac = (Accum)acArray[a];
			if (ac == null) continue;
			
			if (ac.getVectSetId() != -1) {
				// not entangled
				nsh.putDirect(ac, ac.vectorCode);	
				noVssidCnt++;
			} else {
				expCnt += deentangleAccums(nsh, ac);
			}
		}
	
		int nSize = nsh.size();
		int bstSize = nsh.getBucketCount();

		double per0 = (((double)nSize / (double)stSize)*(double)100);
		// re-balance the hash
		nsh.optimize();
		int bnSize = nsh.getBucketCount();

		//System.out.println("    SETD["+dataPlane.getDimension().getIndex()+"/"+dataPlane.getIndex()+":"+setNumber+"]["+Gtil.toRatio2(per0)+"%] [expand: "+expCnt+"] noMap["+noVssidCnt+"] SIZE["+stSize+" => " + nSize+"]");	
		return expCnt;
	}
	int deentangleAccums(MLNumberSetHash nsh, Accum ac) {
		int expCnt = 0;
		
		List<NSVectMap> vml = nsh.getVectorMapVectorToSet(ac.getVectorCode());
		// for each vector that is mapped in hm
		List<ValProb> vpList = ac.getValPs();
		for (int i=0;i<vml.size();i++) {
			// for each vector create an Accum
			NSVectMap vm = vml.get(i);
			Accum mac = null;
			if (vm.vectorCode == ac.vectorCode) {
				mac = ac; // the base - just set values	
			} else {
				// set its values based on count and base
				mac = new AccumInt();
				expCnt++;
			}
			mac.setVectorCode(vm.vectorCode);
			mac.setVectSetId(vm.vsid);
			 
			// add info
			mac.total = vm.count;
			for (int k=0;k<vpList.size();k++) {
				ValProb vp = vpList.get(k);
				//count / total = probability -> count = total * probability;
				// gota round it or 5.999999 -> 5
				int cnt = (int)Math.round(((double)mac.total) * vp.probability);
				int crtCnt = (int)Math.round(((double)mac.getCrtTotal()) * vp.probability);
				mac.setCount(vp.value, cnt, crtCnt);				
			}
			nsh.putDirect(mac, mac.vectorCode);
			
			// remove from vector map
			nsh.removeVectorMapVector(vm.vectorCode);
		}
		return expCnt;
	}
	
	/**
	 * Optimize this Veg instance and its dataplanes
	 */
	public void optimize() {
		this.vectSetMap.optimize(this);
		this.vectStrMap.optimize();
		for (int d=0;d<this.getDataPlaneCount();d++) {		
			this.getDataPlane(d).optimize();
		}
	}
	
	/**
	 * Convert this dataplane into an optimized immutable 'solid' dataplane 
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void makeSolid(String dimensionTag, String dataPlaneTag) {
		if (this.inCount < 1) return;
		VDataPlane dpix = getDataPlane(dimensionTag, dataPlaneTag);
		if (dpix == null) return;	
		dpix.makeSolid();
	}
	
	/**
	 * Convert all dataplanes into an optimized immutable 'solid' dataplanes
	 */
	public void makeSolid() {
		if (this.inCount < 1) return;
		for (int d=0;d<this.getDataPlaneCount();d++) {		
			this.getDataPlane(d).makeSolid();
		}
	}	
	
	/**
	 * check if a dataplan is solid
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public boolean isSolid(String dimensionTag, String dataPlaneTag) {
		if (this.inCount < 1) return false;
		VDataPlane dpix = getDataPlane(dimensionTag, dataPlaneTag);
		if (dpix == null) return false;	
		return dpix.isSolid();
	}
	
	
	//
	// Smash the model when you have completed your training
	// you can still train more but it may skew (research pending)
	//
	public void smash(String dimensionTag, String dataPlaneTag) {
		smash(dimensionTag, dataPlaneTag, false);
	}
	public void smash(String dimensionTag, String dataPlaneTag, boolean forRules) {
		if (this.inCount < 1) return;
		VDataPlane dpix = getDataPlane(dimensionTag, dataPlaneTag);
		if (dpix == null) return;
		
		// subset reduction
		if (forRules) {
			VegTune.reduceRedundant(this, dimensionTag, dataPlaneTag, false);
			// remove all balanced Accumulators
			VegTune.reduceBalanced(this, dimensionTag, dataPlaneTag, true);
		}
		
		// merge all duplicates
		this.entangle(dimensionTag, dataPlaneTag);
		this.optimize();
	}
	
	//
	// Smash it all!
	//
	public void smash() {
		smash(false);
	}
	public void smash(boolean forRules) {
		if (this.inCount < 1) return;
		
		// add missing dimensions and dataPlanes
		for (int dp=0;dp<getDataPlaneCount();dp++) {
			VDataPlane dpix = getDataPlane(dp);
			
			// subset reduction
			if (forRules) {
				VegTune.reduceRedundant(this, dpix.getDimensionTag(), dpix.getTag(), false);
				// remove all balanced Accumulators
				VegTune.reduceBalanced(this, dpix.getDimensionTag(), dpix.getTag(), true);
			}
			
			// merge all duplicates
			this.entangle(dpix.getDimensionTag(), dpix.getTag());
		}
		this.optimize();
	}

	

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// get the next predicted input
	// NOTE: this is not currently public
	//
	// @param dataSet  the list of inputs starting with last input before desired one (as long as you like)
	//                 X, null, Z, <next>      <- do not include next ;-)
	//
	// decision is in the sort
	// CURRENT: longest highest frequency
	// ISSUE: once the window is large enough it is primarily exact recall (~5)
	// ISSUE: if the window is small then loops occur from short common sequences (3)
	// 
	// SO many ways to make this work much better if I were to spend more than a few min on it!
	// - must have a mix of both to make it write pages of dirvell
	//	
	// TODO: 
	//  1) set the EVAL window when called: 
	//      - can be smaller than actual window
	//      - can be larger -> requires multiple set scans
	//  2) allow some random() seed value to make it less predictable and more coherent - passed in so we can do complete recall when desired
	//  3) Provide goals to achieve
	//		- word or phrase to make it to (can gen decision tree for this)
	//		- words or phrase list that must be in the results (again, decision tree)
	//  4) make long text generator with tunning as an API here
	//
	

	
	/**
	 * ValProbAccumVect class extends ValProb to represent a value, its probability and vector source info
	 */
	static class ValProbAccumVect extends ValProb {
		Accum ac;
		int [] vectSetFull;
		int setNumber;
		int [] vect;
		
		public Accum getAccumulator() {
			return ac;
		}
	    /**
		 * ValProbAccumVect Reverse sort comparator for sorting and binary search
	     */
		static final Comparator<ValProb> VpavSort = new Comparator<ValProb>() {
	        @Override
	        public int compare(ValProb lp, ValProb rp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
	        	ValProbAccumVect lvp = (ValProbAccumVect)lp;
	        	ValProbAccumVect rvp = (ValProbAccumVect)rp;
	        	if (lvp.vect.length < rvp.vect.length) return 1;
	        	if (lvp.vect.length > rvp.vect.length) return -1;
	        	if (lvp.probability < rvp.probability) return 1;
	        	if (lvp.probability > rvp.probability) return -1;
	        	if (lvp.count < rvp.count) return 1;
	        	if (lvp.count > rvp.count) return -1;
	        	if (lvp.value < rvp.value) return 1;
	        	if (lvp.value > rvp.value) return -1;
	        	return 0;   
	        }
	    };
	}	
	List<ValProb> predictInputNext(String dimensionTag, String dataPlaneTag, List<String> dataSet, String dataValue) {
		if (dataSet == null) return null;
		if (!isCfgSaveVectSets()) return null;
		
		VDataPlane dataPlane = getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;	
				
		// create sequence frame of window size, last = null, no data = nul
		int [] vSeqFrame = new int [dataPlane.getCfgWindowSize()];
		int didx = (dataSet.size()-1);
		for (int i=(dataPlane.getCfgWindowSize()-2);i>=0;i--) {			
			if (didx < 0) break;			
			vSeqFrame[i] = dataPlane.getCfgVToV().toVectGen(dataSet.get(didx));
			didx--;
		}
		//System.out.println("pIN vSeqFrame["+window+"]: " + NumberSets.setToString(vSeqFrame));

		// get all vectSets based on dataSet
		List<int []> vsSets = vectSetMap.getSubsetsNext(vSeqFrame);
	
		// get all sets that are sequences containing NEXT and last (THIS IS A SLOW ALLOC - CACHE)
		List<Integer> seqSets = MLNumberSetUtil.getSubsetNumbersSequencesEndWith(dataPlane.getCfgWindowSize(), dataPlane.getCfgWindowSize()-1);
		//System.out.println("pIN seqSets["+(window-2)+"]: " + seqSets.size());	
		
		List<Integer> fset = dataPlane.getCfgNSFull();
				
		int dataVector = dataPlane.getCfgVToV().toVectGen(dataValue);
		// get total count for dataValue
		Accum dac = dataPlane.getAccumDefault();
		int totalCount = dac.getCount(dataVector);

		List<ValProb> vpList = new ArrayList<>();
		int chkCnt = 0;
		
		// get Vectors from vectSets for the sets, get accumes
		for (int i=0;i<vsSets.size();i++) {
			int [] vectSet = vsSets.get(i);
			//System.out.println(" VectSet["+dataPlane.getDimensionTag()+"]: " + dimension.getString(vectSet));

			for (int k=0;k<seqSets.size();k++) {
				int setNumber = seqSets.get(k);	
				
				// make vector (if possible)
				long vector = VectorToVid.toVectorV64(vectSet, fset, dataPlane.getNS(setNumber));
				if (vector == 0) continue;
				chkCnt++;
				
				//System.out.println(" ["+i+"]["+setNumber+"]["+vector+"] " +NumberSets.setToString(getSet(setNumber)));
				// get accumes
	    		MLNumberSetHash nsh = dataPlane.getNSHash(setNumber);
	    		if (nsh == null) continue;
	    		
				Accum ac = nsh.get(vector);
				if (ac != null) {
					// get probability for the value we are matching
					int count = ac.getCount(dataVector);
					if (count <= 0) continue;
					double prob = ac.getProbability(dataVector);
					if (prob == 0) continue;
					
					//  IF NSVectMap-ed must get real count 
					int mcount = nsh.getVectorMapCount(vector);
					if (mcount > 0) {
						// is mapped
						count = mcount;
					}

					int nextValue = vectSet[vectSet.length-1]; // last value
					String valStr = dataPlane.getDimensionString(nextValue);
					
					// get set info(FIXME this should be done once and added to list)
					Accum sac = dataPlane.getAccumSetDefault(setNumber);
					double setProb = sac.getProbability(dataVector);

					int setTotal = sac.getCount(dataVector);
					
					//
					// DECISION
					//  setProb - probability that this set determines data Value
					//  prob    - probability that this sequence will be data Value
					//  count   - the number of times this sequence has been this value **
					//  frequency - frequency of this set										
					// weight the value by the set probability ? don't think so
										
					// convert count to a frequency based on total for value
					double frequency = (double)count / (double)totalCount;
					double setfrequency = (double)count / (double)setTotal;
					int setDepth = dataPlane.getNS(setNumber).size();
					// TODO: should subsets be combind for the frequency
				// are subsets needed ?
				// should forward leading sets be included ? (where there is more future and less past?)
					// TODO
					// each set should have a weight?
					// longest always wins?
					
					// the frequency is then weighted by the setDepth
					double wfrequency = frequency * (setDepth);
				// frequency of the small chain is so high that it offsets much others
					// how to weight properly based on the window
					
					//System.out.println("   ["+i+"]["+setNumber+"] ["+wfrequency+"]["+frequency+"/"+setDepth+"] ["+prob+"]["+count+"] ["+valStr+"] bal[" + ac.getBalance(this.getCfgDataWidth())+"] set["+setProb+"]");
								
					// make a VP where the value is the next vector and probalbiity is itself
					ValProbAccumVect vp = new ValProbAccumVect();	
					vp.ac = ac;
					vp.vectSetFull = vectSet;
					vp.setNumber = setNumber;
					vp.vect = makeSubVector(vectSet, fset, dataPlane.getNS(setNumber));
					//if (vp.vect == null) System.out.println("   BAD["+i+"] vect[" + setToString(vectSet)+"] set["+setToString(fset)+"] NS[" + setToString(getSet(setNumber))+"]");				
					vp.count = count;
					vp.probability = frequency;
					vp.value = nextValue;
					vpList.add(vp);
				} else {
					
				}
			}
		}

		// decide
		Collections.sort(vpList, ValProbAccumVect.VpavSort);
		return vpList;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// JSON 
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Export this instance in JSON to a stream
	 * 
	 * JSON: 2 forms
	 * - Solid -> read only simplified
	 * - Full  -> much larger and has it all
	 * @param writer output stream
	 */
	public void toJSON(OutputStreamWriter writer) {	
		// for Veg
		Gson gb = MLSerializeJson.getJSONBuilder();
		// write to steam
		gb.toJson(this, writer);  	
	}	
	
	/**
	 * Export this instance in JSON to a file
	 * 
	 * JSON: 2 forms
	 * - Solid -> read only simplified
	 * - Full  -> much larger and has it all
	 * 
	 * @param filename filename to save to
	 */
	public void exportJSON(String filename) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(filename);
			toJSON(fw);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			System.err.println("ERROR exportJSON["+filename+"] failed: " + e.getMessage());
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
	
	// JSON serializer
	static JsonSerializer<VegML> getJSONSerializer() {
		return new  JsonSerializer<VegML>() {  
		    @Override
		    public JsonElement serialize(VegML vML, Type typeOfSrc, JsonSerializationContext context) {
		        JsonObject jsonV = new JsonObject();
		        jsonV.addProperty("tag", vML.getTag());
		        jsonV.addProperty("desc", vML.getCfgDescription());
		        jsonV.addProperty("inCount", vML.inCount);
		        jsonV.addProperty("empty", VegML.emptyVect);
		        jsonV.addProperty("sVectSets", vML.saveVectSets);
		        
		        // add objects	
		        JsonElement jsonScPad = context.serialize(vML.scratchPad, HashMap.class);
			    jsonV.add("scratchPad", jsonScPad);
			    
		        JsonElement jsonDPs = context.serialize(vML.dpList, MLSerializeJson.DPListType);
			    jsonV.add("dataPlans", jsonDPs);
			    
		        JsonElement jsonVStr = context.serialize(vML.vectStrMap, MLValStrMap.class);
			    jsonV.add("stringMap", jsonVStr);
		        
		        return jsonV;
		    }
		};
	}

	// used for progress ticks in train/tune/etc
	static int getTickCount(int size) {
		int progressTick = size/(PROGRESS_MARKS_LINE*PROGRESS_LINES);
		if (progressTick < 1) progressTick = 1;
		if ((size % progressTick) > 0) progressTick++;
		return progressTick;
	}
	static int getTickCount(int size, int maxLines) {
		int progressTick = size/(PROGRESS_MARKS_LINE*maxLines);
		if (progressTick < 1) progressTick = 1;
		if ((size % progressTick) > 0) progressTick++;
		return progressTick;
	}
	// used ot show progress in train/tune/etc
	static int showProgress(VegML vML, int progressCnt, int progressTick, int progressLast, String prog) {
		if (vML.isCfgShowProgress()) {
			return showProgressMark(vML, progressCnt, progressTick, progressLast, prog);
		}
		return progressLast;
	}
	// used ot show progress in train/tune/etc
	static int showProgressMark(VegML vML, int progressCnt, int progressTick, int progressLast, String prog) {
		int pos = (progressCnt/progressTick);
		if (pos != progressLast) {				
			if (pos == 0 || (pos % PROGRESS_MARKS_LINE) == 0) {
				if (pos != 0) System.out.println("");
				System.out.print("  "+String.format("%1$7s", progressCnt)+" ");
			}
			System.out.print(prog);
			return pos;
		}		
		return progressLast;
	}
	

	/**
	 * Debug tool to show response frame information
	 * 
	 * @param dataplane dataplane for prediction
	 * @param frame frame from prediction
	 * @param respSet response set
	 * @param vpListOut v
	 */
	 static void showResponse(VDataPlane dataplane, VFrame frame, Accum [] respSet, List<ValProb> vpListOut) {
		System.out.println("    FRAME[@"+frame.getDataSetPosition()+"]["+dataplane.getDimensionString(frame.getFrame())+"]");	
		for (int i=0;i<respSet.length;i++) {
			if (respSet[i] == null || respSet[i].total == 0) continue;
			String ns = dataplane.getNSFormatString(i);
			List<ValProb> vpList = respSet[i].getValPsSorted();
			System.out.print("      SET["+String.format("%3d", i)+"]["+ns+"] => ["+vpList.size()+"] ");
			for (int v=0;v<vpList.size();v++) {
				ValProb vp = vpList.get(v);
				System.out.print("  <'"+dataplane.getString(vp.value)+"' "+vp.probability+">");
				if (v == 5) break; /// MAX
			}
			System.out.println("");
		}
		System.out.println("    ANSWER['"+dataplane.getString(vpListOut.get(0).value)+"' "+vpListOut.get(0).probability+"]["+vpListOut.get(0).count+"]");		
		  System.out.print("          ["+vpListOut.size()+"]");
		for (int v=0;v<vpListOut.size();v++) {
			ValProb vp = vpListOut.get(v);
			System.out.print("  <'"+dataplane.getString(vp.value)+"' "+vp.probability+" "+vp.count+">");
			if (v == 5) break; /// MAX
		}
		System.out.println("");
	}
	 
	/**
	 * Print String valueId and vid mappings if retained
	 */
	public void printVectorStrings() {
        vectStrMap.print();
	}
	
	/**
	 * print all vector sets if retained
	 */
	public void printVectorSets() {
		vectSetMap.print();
	}
	
	/**
	 * print data from all dataplanes
	 * 
	 */
	public void printData() {
		System.out.println("-----------------------------------------------------------------------------------------------");
		System.out.println("vML["+tag+"]["+this.getDataPlaneCount()+"] s[v:"+saveVectSets+" "
				+ " IN["+inCount+"] strmap["+vectStrMap.getCount()+"] vectmap["+vectSetMap.getCount()+"]");

		for (int dp=0;dp<getDataPlaneCount();dp++) {			
			getDataPlane(dp).printData(false);
		}		
		System.out.println("-----------------------------------------------------------------------------------------------");
	}
	
	/**
	 * Print the data for a specific dataplane
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param showAccum true to show all data
	 */
	public void printData(String dimensionTag, String dataPlaneTag, boolean showAccum) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.printData(showAccum);
	}
	
	/**
	 * Print the data for a specific dataplane and nubmerSet
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param showAccum true to show all data
	 * @param setNumber NumberSet to show data for
	 */
	public void printData(String dimensionTag, String dataPlaneTag, boolean showAccum, int setNumber) {
		VDataPlane dp = this.getDataPlane(dimensionTag, dataPlaneTag);
		if (dp == null) return;
		dp.printData(showAccum, setNumber);
	}

	/**
	 * Show VegML instance information
	 */
	public void print() {
		print(false);
	}
	
	/**
	 * Show VegML instance information
	 * 
	 * @param sets if true shw dataplane numberSets
	 */
	public void print(boolean sets) {
		System.out.println("-----------------------------------------------------------------------------------------------");
		System.out.println("vML["+tag+"]["+this.getDataPlaneCount()+"] s[v:"+saveVectSets+" "
				+ " IN["+inCount+"] strmap["+vectStrMap.getCount()+"] vectmap["+vectSetMap.getCount()+"]");

		for (int dp=0;dp<getDataPlaneCount();dp++) {			
			getDataPlane(dp).print(sets);
		}

		System.out.println("-----------------------------------------------------------------------------------------------");
	}
	
	/**
	 * Show VegML instance information for a dataplane
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 */
	public void print(String dimensionTag, String dataPlaneTag) {
		print(dimensionTag, dataPlaneTag, false);
	}
	
	/**
	 * Show VegML instance information for a dataplane
	 * 
	 * @param dimensionTag dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param sets if true shw dataplane numberSets
	 */
	public void print(String dimensionTag, String dataPlaneTag, boolean sets) {
		System.out.println("-----------------------------------------------------------------------------------------------");
		System.out.println("vML["+tag+"]["+this.getDataPlaneCount()+"] s[v:"+saveVectSets+" "
				+ " IN["+inCount+"] strmap["+vectStrMap.getCount()+"] vectmap["+vectSetMap.getCount()+"]");
		this.getDataPlane(dimensionTag, dataPlaneTag).print(sets);
		System.out.println("-----------------------------------------------------------------------------------------------");
	}
	
}