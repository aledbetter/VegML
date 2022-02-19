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


import java.util.HashMap;
import java.util.List;

import vegml.VegML.NSWeightBase;
import vegml.VegML.PredictionType;
import vegml.ValProb;
import vegml.VegML.reduceCheck;
import vegml.Data.VDataSets;
import vegml.Data.VectorToVid;
import vegml.OptimizerMerge.MergeMap;

/**
 * Model tuning methods and tools
 * 
 * Logical
 * -
 * -
 * 
 * Statistical
 * - Carving
 * -
 * 
 * Mixed
 * - Adaptive Training
 *
 */
public class VegTune {
	private static final boolean error_checking = false; // check for some errors... off to make faster

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Adaptive Training
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Adaptive Training
	 * Assess relationships and build tuned model with best relationships only
	 * 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param initName
	 * @param optType
	 * @param useReduction
	 * @param cfg
	 * @param fullData
	 * @param dss
	 * @return
	 */
	public static String adaptiveTraining(String dimensionTag, String dataPlaneTag, String initName,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(dimensionTag, dataPlaneTag, initName, optType, useReduction, null);
		return op.miser(-1, -1, -1, null, cfg, fullData, dss);
	}
	
	/**
	 * Adaptive Training
	 * Assess relationships and build tuned model with best relationships only
	 * 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param initName
	 * @param maxWindow
	 * @param optType
	 * @param useReduction
	 * @param cfg
	 * @param fullData
	 * @param dss
	 * @return
	 */
	public static String adaptiveTraining(String dimensionTag, String dataPlaneTag, String initName, int maxWindow,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(dimensionTag, dataPlaneTag, initName, optType, useReduction, null);
		return op.miser(-1, -1, maxWindow, null, cfg, fullData, dss);
	}
	
	/**
	 * Adaptive Training
	 * Assess relationships and build tuned model with best relationships only
	 * 
	 * directly for a vML
	 * 
	 * @param vML
	 * @param initName
	 * @param optType
	 * @param useReduction
	 * @param cfg
	 * @param fullData
	 * @param dss
	 * @return
	 */
	public static String adaptiveTraining(VegML vML, String initName,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, optType, useReduction, null);
		return op.miser(-1, -1, -1, null, cfg, fullData, dss);
	}

	/**
	 * Adaptive Training
	 * Assess relationships and build tuned model with best relationships only
	 * 
	 * force NSWeightBase type
	 * @param vML
	 * @param initName
	 * @param nsWBaseType
	 * @param optType
	 * @param useReduction
	 * @param cfg
	 * @param fullData
	 * @param dss
	 * @return
	 */
	public static String adaptiveTraining(VegML vML, String initName, NSWeightBase nsWBaseType,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, optType, useReduction, null);
		return op.miser(-1, -1, -1, nsWBaseType, cfg, fullData, dss);
	}
	
	/**
	 * Adaptive Training
	 * Assess relationships and build tuned model with best relationships only
	 * 
	 * @param vML
	 * @param initName
	 * @param maxWindow
	 * @param optType
	 * @param useReduction
	 * @param cfg
	 * @param fullData
	 * @param dss
	 * @return
	 */
	public static String adaptiveTraining(VegML vML, String initName, int maxWindow,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, optType, useReduction, null);
		return op.miser(-1, -1, maxWindow, null, cfg, fullData, dss);
	}
	
	/**
	 * Adaptive Training
	 * Assess relationships and build tuned model with best relationships only
	 * 
	 * @param vML
	 * @param initName
	 * @param maxWindow
	 * @param nsWBaseType
	 * @param optType
	 * @param useReduction
	 * @param cfg
	 * @param fullData
	 * @param dss
	 * @return
	 */
	public static String adaptiveTraining(VegML vML, String initName, int maxWindow, NSWeightBase nsWBaseType,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, optType, useReduction, null);
		return op.miser(-1, -1, maxWindow, nsWBaseType, cfg, fullData, dss);
	}
	
	/**
	 * Adaptive Training
	 * Assess relationships and build tuned model with best relationships only
	 * 
	 * @param vML
	 * @param initName
	 * @param maxBefore
	 * @param maxAfter
	 * @param optType
	 * @param useReduction
	 * @param cfg
	 * @param fullData
	 * @param dss
	 * @return
	 */
	public static String adaptiveTraining(VegML vML, String initName, int maxBefore, int maxAfter,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, optType, useReduction, null);
		return op.miser(maxBefore, maxAfter, -1, null, cfg, fullData, dss);
	}
	
	//
	// merge tune: default is entry point, tuneDimensionTag/tuneDataPlaneTag is dataplane getting tuned
	//
	// directly for a vML
	public static String adaptiveTrainingMerge(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, tuneDimensionTag, tuneDataPlaneTag, true, optType, useReduction, null);
		return op.miser(-1, -1, -1, null, cfg, fullData, dss);
	}
	// force NSWeightBase type
	public static String adaptiveTrainingMerge(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag, NSWeightBase nsWBaseType,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, tuneDimensionTag, tuneDataPlaneTag, true, optType, useReduction, null);
		return op.miser(-1, -1, -1, nsWBaseType, cfg, fullData, dss);
	}
	public static String adaptiveTrainingMerge(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag, int maxWindow,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, tuneDimensionTag, tuneDataPlaneTag, true, optType, useReduction, null);
		return op.miser(-1, -1, maxWindow, null, cfg, fullData, dss);
	}
	public static String adaptiveTrainingMerge(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag, int maxWindow, NSWeightBase nsWBaseType,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, tuneDimensionTag, tuneDataPlaneTag, true, optType, useReduction, null);
		return op.miser(-1, -1, maxWindow, nsWBaseType, cfg, fullData, dss);
	}
	public static String adaptiveTrainingMerge(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag, int maxBefore, int maxAfter,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, tuneDimensionTag, tuneDataPlaneTag, true, optType, useReduction, null);
		return op.miser(maxBefore, maxAfter, -1, null, cfg, fullData, dss);
	}
	
	//
	// test with default tune: default is entry point, tuneDimensionTag/tuneDataPlaneTag is dataplane getting tuned
	//
	// directly for a vML
	public static String adaptiveTraining(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, tuneDimensionTag, tuneDataPlaneTag, false, optType, useReduction, null);
		return op.miser(-1, -1, -1, null, cfg, fullData, dss);
	}
	// force NSWeightBase type
	public static String adaptiveTraining(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag, NSWeightBase nsWBaseType,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, tuneDimensionTag, tuneDataPlaneTag, false, optType, useReduction, null);
		return op.miser(-1, -1, -1, nsWBaseType, cfg, fullData, dss);
	}
	public static String adaptiveTraining(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag, int maxWindow,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, tuneDimensionTag, tuneDataPlaneTag, false, optType, useReduction, null);
		return op.miser(-1, -1, maxWindow, null, cfg, fullData, dss);
	}
	public static String adaptiveTraining(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag, int maxWindow, NSWeightBase nsWBaseType,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, tuneDimensionTag, tuneDataPlaneTag, false, optType, useReduction, null);
		return op.miser(-1, -1, maxWindow, nsWBaseType, cfg, fullData, dss);
	}
	public static String adaptiveTraining(VegML vML, String initName, String tuneDimensionTag, String tuneDataPlaneTag, int maxBefore, int maxAfter,
			PredictionType optType, boolean useReduction, HashMap<String, Object> cfg, boolean fullData, VDataSets dss) {
		Optimiser2 op = new Optimiser2(vML, initName, tuneDimensionTag, tuneDataPlaneTag, false, optType, useReduction, null);
		return op.miser(maxBefore, maxAfter, -1, null, cfg, fullData, dss);
	}
		
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Carving
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Optimize with Carving method and steps
	 * 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param initName
	 * @param pre
	 * @param window
	 * @param wType
	 * @param optType
	 * @param dropPercent
	 * @param minSet
	 * @param phases
	 * @param minProgress
	 * @param identityFilter
	 * @param noBackingUp
	 * @param useReduction
	 * @param downWeight
	 * @param setFullData
	 * @param dss
	 * @return
	 */
	public static int carveSteps(String dimensionTag, String dataPlaneTag, String initName, 
			String pre, int window, NSWeightBase wType, 
			PredictionType optType, double dropPercent, int minSet, int phases, 
			int minProgress, boolean identityFilter, 
			boolean noBackingUp, boolean useReduction, double downWeight,
			int setFullData, VDataSets dss) {	
		return carveMergeSteps(dimensionTag, dataPlaneTag, initName, 
				pre, window, wType, optType, dropPercent, minSet, phases,  minProgress, identityFilter, 
				noBackingUp, useReduction, downWeight, null, null, false, setFullData, dss);
	}
	
	/**
	 * 
	 * @param step
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param pre
	 * @param window
	 * @param wType
	 * @param optType
	 * @param minSet
	 * @param phases
	 * @param identityFilter
	 * @param noBackingUp
	 * @param useReduction
	 * @param downWeight
	 * @param nsSetFirst
	 * @param setFullData
	 * @param dss
	 * @return
	 */
	public static int carveStepN(int step, String dimensionTag, String dataPlaneTag, 
			String pre, int window, NSWeightBase wType, 
			PredictionType optType, int minSet, int phases, boolean identityFilter,		
			boolean noBackingUp, boolean useReduction, double downWeight,
			List<Integer> nsSetFirst,
			int setFullData, VDataSets dss) {	
		return OptimizerStatistical.carveMergeStepN(step, dimensionTag, dataPlaneTag, null, pre, window, wType, optType, 0, minSet, phases, 
							identityFilter, noBackingUp, useReduction, downWeight, null, null, false, nsSetFirst, setFullData, dss);
	}
	
	/**
	 * 
	 * @param step
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param initName
	 * @param pre
	 * @param window
	 * @param wType
	 * @param optType
	 * @param dropPercent
	 * @param minSet
	 * @param phases
	 * @param identityFilter
	 * @param noBackingUp
	 * @param useReduction
	 * @param downWeight
	 * @param nsSetFirst
	 * @param setFullData
	 * @param dss
	 * @return
	 */
	public static int carveStepN(int step, String dimensionTag, String dataPlaneTag, String initName, 
			String pre, int window, NSWeightBase wType, 
			PredictionType optType, double dropPercent, int minSet, int phases, boolean identityFilter,
			boolean noBackingUp, boolean useReduction, double downWeight,
			List<Integer> nsSetFirst,
			int setFullData, VDataSets dss) {	
		return OptimizerStatistical.carveMergeStepN(step, dimensionTag, dataPlaneTag, initName, pre, window, wType, optType, dropPercent, minSet, phases, 
				identityFilter, noBackingUp, useReduction, downWeight, null, null, false, nsSetFirst, setFullData, dss);	
	}
	
	/**
	 * Optimize dataplane merged on base dataplane
	 * 
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param initName
	 * @param pre
	 * @param window
	 * @param wType
	 * @param optType
	 * @param dropPercent
	 * @param minSet
	 * @param phases
	 * @param minProgress
	 * @param identityFilter
	 * @param noBackingUp
	 * @param useReduction
	 * @param downWeight
	 * @param mergeDimensionTag
	 * @param mergeDataPlaneTag
	 * @param mergeSetIsbase
	 * @param setFullData
	 * @param dss
	 * @return
	 */
	public static int carveMergeSteps(String dimensionTag, String dataPlaneTag, String initName, 
			String pre, int window, NSWeightBase wType, 
			PredictionType optType, double dropPercent, int minSet, int phases, 
			int minProgress, boolean identityFilter, 
			boolean noBackingUp, boolean useReduction, double downWeight,
			String mergeDimensionTag, String mergeDataPlaneTag, boolean mergeSetIsbase,
			int setFullData, VDataSets dss) {	
		return OptimizerStatistical.carveMergeSteps(dimensionTag, dataPlaneTag, initName, pre, window, 
				wType, optType, dropPercent, minSet, phases, minProgress, identityFilter, noBackingUp, useReduction, 
				downWeight, mergeDimensionTag, mergeDataPlaneTag, mergeSetIsbase, setFullData, dss);
	}

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Data Definition and discovery
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Use Dataset definition as derived from the dataset to remove useless vectors to match static data
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param optType prediction type to optimise for
	 */
	public static void logicalDataDefinitionReduction(VegML vML, String dimensionTag, String dataPlaneTag, PredictionType optType) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		OptimizerLogical.reduceByDefinition(dataPlane, optType, null, false);
	}
	
	/**
	 * Use Dataset definition as derived from the dataset to remove useless vectors to match static data
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param optType prediction type to optimise for
	 * @param silent don't print
	 */
	public static void logicalDataDefinitionReduction(VegML vML, String dimensionTag, String dataPlaneTag, PredictionType optType, boolean silent) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		OptimizerLogical.reduceByDefinition(dataPlane, optType, null, silent);
	}	
	
	/**
	 * Remove vector-values that are not usable when the idenity only flag is set
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param keepFull if true don't apply to full numberset
	 * @param trainDs the training data set used
	 */
	public static void logicalDataIdentityReduction(VegML vML, String dimensionTag, String dataPlaneTag, boolean keepFull, VDataSets trainDs) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		OptimizerLogical.reduceByIdentity(dataPlane, trainDs, keepFull, false);
	}
	
	/**
	 * Remove vector-values that are not usable when the idenity only flag is set
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param keepFull if true don't apply to full numberset
	 * @param trainDs the training data set used
	 * @param silent don't print
	 */
	public static void logicalDataIdentityReduction(VegML vML, String dimensionTag, String dataPlaneTag, boolean keepFull, VDataSets trainDs, boolean silent) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return;
		OptimizerLogical.reduceByIdentity(dataPlane, trainDs, keepFull, silent);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Dependencies
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * use dependency mapping for positive and negative corrolations
	 * @param fileName
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param wType
	 * @param optType
	 * @param useReduction
	 * @param fullData
	 * @param dss
	 * @param ctlGroup
	 * @return
	 */
	public static int logicalViaDependencyMapping(String fileName, String dimensionTag, String dataPlaneTag, 
			NSWeightBase wType, PredictionType optType, boolean useReduction, boolean fullData, VDataSets dss, boolean ctlGroup) {		
		return logicalViaDependencyMapping(fileName, dimensionTag, dataPlaneTag, true, wType, optType, useReduction, 
											fullData, null, null, null, null, null, dss, ctlGroup);
	}
	
	/**
	 * use dependency mapping for positive and negative corrolations
	 * @param fileName
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param update
	 * @param wType
	 * @param optType
	 * @param useReduction
	 * @param fullData
	 * @param crtWinDep
	 * @param crtDep
	 * @param crtWinOrDep
	 * @param loseDep
	 * @param loseOrDep
	 * @param dss
	 * @param ctlGroup
	 * @return
	 */
	public static int logicalViaDependencyMapping(String fileName, String dimensionTag, String dataPlaneTag, 
			boolean update, NSWeightBase wType, PredictionType optType, boolean useReduction, boolean fullData,
			HashMap<Long, HashMap<Long, Integer>> [] crtWinDep, 		
			HashMap<Long, HashMap<Long, Integer>> [] crtDep,
			HashMap<Long, HashMap<Long, Integer>> [] crtWinOrDep,
			HashMap<Long, HashMap<Long, Integer>> [] loseDep,
			HashMap<Long, HashMap<Long, Integer>> [] loseOrDep,
			VDataSets dss, boolean ctlGroup) {		
		return OptimizerLogical.viaDependencyMapping(fileName, dimensionTag, dataPlaneTag, update, wType, optType, useReduction, 
														fullData, crtWinDep, crtDep, crtWinOrDep, loseDep, loseOrDep, dss, ctlGroup);
	}

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// voted reduction
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static int logicalVoteReduction(int step, String dimensionTag, String dataPlaneTag, String initName, 
			String pre, int window, NSWeightBase wType, PredictionType optType, boolean fullData, VDataSets dss) {		
		return OptimizerLogical.optimizeVoteReduction(step, dimensionTag, dataPlaneTag, initName, pre, window, wType, optType, fullData, dss);
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Optimzie number set and weights
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void numberSetsWeightAndDrop(VegML vML, String dimensionTag, String dataPlaneTag, VDataSets ds) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (ds == null || dataPlane == null) return;
		OptimizerStatistical.optimizeNumberSetsDrop(dataPlane, ds);
	}

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Generate opimal merge map between 2 dataplanes
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// @return single merge value
	public static double optimizeMergeModels(VegML vML, String dimensionTag, String dataPlaneTag, String dimensionTag2, String dataPlaneTag2, 
			PredictionType opType, boolean fullData, VDataSets ds) {
		VDataPlane dp1 = vML.getDataPlane(dimensionTag, dataPlaneTag);
		VDataPlane dp2 = vML.getDataPlane(dimensionTag2, dataPlaneTag2);
		if (ds == null || dp1 == null || dp2 == null) return -1;
		MergeMap mMap = OptimizerMerge.optimizeMergeModels(dp1, dp2, true, true, opType, false, fullData, ds);
		dp1.setCfgMergeMap(dimensionTag2, dataPlaneTag2, mMap);
		return mMap.getMergeValue();
	}
	public static double optimizeMergeModels(VegML vML, String dimensionTag, String dataPlaneTag, String dimensionTag2, String dataPlaneTag2, 
			boolean modeLinear, boolean modeMatrix,	PredictionType opType, boolean noExclude, boolean fullData, VDataSets ds) {
		VDataPlane dp1 = vML.getDataPlane(dimensionTag, dataPlaneTag);
		VDataPlane dp2 = vML.getDataPlane(dimensionTag2, dataPlaneTag2);
		if (ds == null || dp1 == null || dp2 == null) return -1;
		MergeMap mMap = OptimizerMerge.optimizeMergeModels(dp1, dp2, modeLinear, modeMatrix, opType, noExclude, fullData, ds);
		dp1.setCfgMergeMap(dimensionTag2, dataPlaneTag2, mMap);
		return mMap.getMergeValue();
	}
	public static double optimizeMergeModels(VegML vML, String dimensionTag, String dataPlaneTag, String dimensionTag2, String dataPlaneTag2, 
			boolean modeLinear, boolean modeMatrix,	PredictionType opType, boolean noExclude, boolean exactData, boolean fullData, VDataSets ds) {
		VDataPlane dp1 = vML.getDataPlane(dimensionTag, dataPlaneTag);
		VDataPlane dp2 = vML.getDataPlane(dimensionTag2, dataPlaneTag2);
		if (ds == null || dp1 == null || dp2 == null || dp1.getNSCount() < 1 || dp2.getNSCount() < 1) return -1;
		MergeMap mMap = OptimizerMerge.optimizeMergeModels(dp1, dp2, modeLinear, modeMatrix, opType, noExclude, exactData, fullData, ds);
		dp1.setCfgMergeMap(dimensionTag2, dataPlaneTag2, mMap);
		return mMap.getMergeValue();
	}
	
	public static MergeMap getOptimizeMergeMap(VegML vML, String dimensionTag, String dataPlaneTag, String dimensionTag2, String dataPlaneTag2) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;
		return dataPlane.getCfgMergeMap(dimensionTag2, dataPlaneTag2);
	}
	 
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Optimzie Merge Values
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void optimizeMergeValue(VegML vML, String dimensionTag, String dataPlaneTag, String valueName, double initTestSet [], VDataSets ds) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (ds == null || dataPlane == null) return;
		OptimizerMerge.optimizeMergeValue(dataPlane, valueName, initTestSet, ds);
	}
	public static void optimizeMergeValue(VegML vML, VDataPlane baseDp, VDataPlane dp, double initTestSet [], VDataSets ds) {
		if (ds == null || baseDp == null || dp == null) return;
		OptimizerMerge.optimizeMergeValue(baseDp, dp.getDimensionTag(), initTestSet, ds);
	}

	public static void optimizeValueInt(VegML vML, String dimensionTag, String dataPlaneTag, String valueName, int initTestSet [], VDataSets ds) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (ds == null || dataPlane == null) return;
		OptimizerMerge.optimizeValueInt(dataPlane, valueName, initTestSet, ds);
	}
	
	public static void optimizeValueAmp(VegML vML, String dimensionTag, String dataPlaneTag, String valueName, VDataSets ds) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (ds == null || dataPlane == null) return;
		OptimizerMerge.optimizeValueAmp(dataPlane, valueName, ds);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Optimzie a value in the scratchpad
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * 
	 * @param vML
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param valueName
	 * @param initTestSet
	 * @param ds
	 * @return
	 */
	public static double optimizeValue(VegML vML, String dimensionTag, String dataPlaneTag, String valueName, double initTestSet [], VDataSets ds) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (ds == null || dataPlane == null) return -1;
		return OptimizerMerge.optimizeValue(dataPlane, valueName, initTestSet, ds);
	}
 
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Reductions
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static final boolean DEBUG_REDUCE = false;
	/**
	// Reduce remove redundant sets betten supper set and subset that have same result
	//
	// NOTE: this does not produce the results I expected, after reduction there is a drop of ~1.5%
	//  	- it may be a bug, it may be best to always keep the full set, some investigation is needed
	// NOTE: must have vector sets for this isCfgSaveVectSets()
	//
	// ISSUE:
	//	this removes redundant Subset that produce the same result - and are needed from clean symbolic rules
	//  HOWEVER the remaining subsets may not have the same set probability(likeliness) thus the results will skew
	//  how to account for this? thoughts
	//  	- offset the probability of the remaining node to account of likeliness skew (but decision alg dependent)
	// 		- fold redundant sets into the remaining set, thus it would retain a list that could be used to get the likeliness
	//		  at time of decision:
	//		  - save list of sets OR boolean that it was done as the subsets/supersets are already know
	 * @param vML
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param retainNormalcy
	 * @return
	 */
	public static long reduceRedundant(VegML vML, String dimensionTag, String dataPlaneTag, boolean retainNormalcy) {
		if (vML.getInCount() < 1) return 0;
		if (!vML.isCfgSaveVectSets()) {
			System.out.println("   START reduceRedundant["+dimensionTag+"/"+dataPlaneTag+"] FAILED must have saveVectSets on");
			return 0; // must have vector sets
		}
		boolean noIdentity = false;
		boolean noFullest = true;
		if (retainNormalcy) {
			noIdentity = noFullest = true;
		}

		VDataPlane dpix = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dpix == null) return -1;
		
		long rCnt = 0;

		int cCnt = 0, aCnt = 0;
		for (int i=dpix.getCfgWindowSize();i>1;i--) {
			MLNumberSetHash nsh = dpix.getNSHash(i);			
			if (nsh == null) continue;
			aCnt += nsh.size();
		}
		
		boolean ent = dpix.isEntangled();
		if (ent) {
			// deentangle
			vML.deentanglement(dpix);
			if (vML.isCfgShowProgress()) System.out.print("   START reduceRedundant["+dpix.getDimensionTag()+"/"+dpix.getTag()+"] Entangled sets[" + dpix.getNSCount()+"] .");
		} else {
			if (vML.isCfgShowProgress()) System.out.print("   START reduceRedundant["+dpix.getDimensionTag()+"/"+dpix.getTag()+"] sets[" + dpix.getNSCount()+"] .");
		}
		
		/////////////////////////////////////////////////
		// REDUCE Supersets -> retain smallest set that proves rule				
		/*
		 * Each set has ordered list of direct child sets: such that all sets fit in to a tree
		 * The order of processing the sets then is to process the Full Window sets
		 *  - if match child, then remove self.. Next
		 *  -  in a given vector List there can be only 1 child match for each set
		 *  
		 *  - we can get the actual direct value in each set
		 *  	- take the superset and get the subset vector from the values 
		 */
		//this.print(true, true);
		// process this sets in steps down
		for (int i=dpix.getCfgWindowSize();i>1;i--) {
			//System.out.println(" WW["+i+"]");
			for (int ns=0;ns<dpix.getNSCount();ns++) {			
				MLNumberSetHash nsh = dpix.getNSHash(ns);			
				if (nsh == null || nsh.size() < 1 || nsh.isTurnedOff()) continue;
				List<Integer> numberSet = nsh.getNS();
				if (numberSet.size() != i) continue;
				if (noFullest && ns == dpix.getCfgNSFullNumber()) continue;
				if (noIdentity && ns == dpix.getCfgNSIdentityNumber()) continue;

				rCnt += reduceSubsetsWork(dpix, ns);
			}
		}
		
		if (ent) {
			// reentangle
			vML.entangle(dpix);
		}
		for (int i=dpix.getCfgWindowSize();i>1;i--) {
			MLNumberSetHash nsh = dpix.getNSHash(i);			
			if (nsh == null) continue;
			cCnt += nsh.size();
		}
		if (cCnt > 0 && aCnt > 0) {
			double per = (double)100 - ((double)cCnt / (double)aCnt) * (double)100;	
			if (vML.isCfgShowProgress()) System.out.println(".. COMPLETE reduced["+String.format("%.2f", per)+"%]["+aCnt+" -> "+cCnt+"]");
		} else {
			if (vML.isCfgShowProgress()) System.out.println(".. COMPLETE reduction["+rCnt+"]");
		}
		return rCnt;
	}
	// reduce children
	private static final int MIN_TOTAL_REDUCE = 4;
	private static long reduceSubsetsWork(VDataPlane dataPlane, int numberSet) {
		// this needs to be threaded to improve perfomance... and maybe a bit of thought too				
		// each of these is a different set where i == setNumber		

		MLNumberSetHash nsh = dataPlane.getNSHash(numberSet);
		if (nsh == null || nsh.size() < 1 || nsh.isTurnedOff()) return 0;
		if (nsh.getNSSize() < 2) return 0;
				
		// get the superset (this slots set) for this iteration
		List<Integer> superset = dataPlane.getNS(numberSet);
		if (superset == null) return 0;
	//	System.out.println("   RedundentSet["+dataPlane.getNSFormatString(numberSet)+"]");

		long rCnt = 0;
				
		// get list
		List<Accum> acList = nsh.getListSorted();
		Object [] acArray = acList.toArray();	// this is exponentially faster
		acList.clear();
		acList = null;
		HashMap<Long, Integer> mergeCnt = new HashMap<>();
		
		//System.out.println("   RedundentSet["+dataPlane.getNSFormatString(numberSet)+"] ac: " + acArray.length);

		/////////////////////////////
		// for each accumulator in this numberSet
		for (int i=0;i<acArray.length;i++) {
			Accum ac = (Accum)acArray[i];
			if (ac == null) continue;
			//System.out.println("     accum["+i+"]");
			if (ac.getTotal() < MIN_TOTAL_REDUCE) continue; // MIN HACK
			
			// each of these is a different set where i == setNumber
			// we can map what sets are subsets of the others to determine where exactly to get the subsets from... makeing it fast						
			List<ValProb> vpl = ac.getValPs();
			mergeCnt.clear();
			
			// through the other number sets
			// get list from DataPlane numbersets, with correct numbers, sorted and just size-1
			List<Integer> subOrder = MLNumberSetUtil.getSubsetsOrder(dataPlane.getNSs(), numberSet, superset.size()-1);
			if (subOrder == null || subOrder.size() == 0) continue;
			//System.out.println("   RedundentSet["+dataPlane.getNSFormatString(numberSet)+"] ac: " + acArray.length + " => " + subOrder.size());
			
			/////////////////////////////
			// for each subset of this number set that is 1 element smaller
			for (Integer sorder:subOrder) {
				int ks = sorder.intValue();				
				MLNumberSetHash snsh = dataPlane.getNSHash(ks);
				if (snsh == null || snsh.size() < 1) continue;
				List<Integer> subset = dataPlane.getNS(ks);

				//System.out.println("      subSet["+dataPlane.getNSFormatString(numberSet)+"] => ["+dataPlane.getNSFormatString(ks)+"]");
			
				// get the vector for this set
				int [] vectSet = dataPlane.getVegML().getVectorSetForId(ac.getVectSetId(), superset, superset);
				int [] sVectSet = VegML.makeSubVector(vectSet, superset, subset);
				long subVector = VectorToVid.toVectorV64(sVectSet);
				Accum lvs = snsh.get(subVector);		
				if (lvs == null) continue;	// NOT HERE
								
				if (error_checking) {
					int [] lvectSet = dataPlane.getVegML().getVectorSetForId(lvs.getVectSetId(), superset, superset);

					// check if exact subset: value check
					if (!MLNumberSetUtil.isSubVectorSet(vectSet, lvectSet)) {
						System.out.println("  ERROR   vector Collision["+ subset.size()+"] SETS["+MLNumberSetUtil.setToString(lvectSet)+"]["+MLNumberSetUtil.setToString(sVectSet)+"]  VECTS["+ subVector+"]["+VectorToVid.toVectorV64(lvectSet)+"] ");
						continue;
					}
					if (DEBUG_REDUCE) System.out.println("      subVectSet["+ subset.size()+"]["+superset.size()+"] ("+MLNumberSetUtil.setToString(vectSet)+" == "+MLNumberSetUtil.setToString(lvectSet)+")");
				}


				// check the probabilities
				for (int u=0;u<vpl.size();u++) {
					ValProb vp = vpl.get(u);
					// Check all values
					if (lvs.getProbability(vp.value) == vp.probability) {
						//System.out.println("       numS x["+ks+"] vpl["+vpl.size()+"] ["+vp.value+"] " + ac.getDistString());
						// check count? may be from multiple sources... thus reducing the probability (count) would be correct
					//	if (vs.getCount(vp.intValue()) == vs.getCount(vp.intValue())) {						
							// remove if match
							ac.remove(vp.value);	
							rCnt++;
							// increase vp's probability by  x cnt
							// record info, increase AFTER finished with AC
							Integer ct = mergeCnt.get(vp.value);
							if (ct == null) mergeCnt.put(vp.value, 1);
							else mergeCnt.put(vp.value, ct+1);		
							// FIXME factor in the numberSet weight
							
							//if (DEBUG_REDUCE) System.out.println("  RM_val_su["+keys[k]+"]["+vp.intValue()+"]["+vp.probability+"]");
					//	} else {
							// never gets here
					//		System.out.println("  RM_val_su["+vs.getCount(vp.intValue())+"]["+vs.getCount(vp.intValue())+"]");
					//	}
					}
				}
								
				// remove Accum if empty
				if (ac.getValueCount() < 1) {
					//System.out.println(" RM_acc_su["+keys[k]+"]");
					acArray[i] = null;
					nsh.removeDirect(ac.getVectorCode());
				}

				// JUST ONE SUBSET for a GIVE SUPERSET within a specific SET
				// parent removed ..so job done
				break;
			}	
			
			// update probabilities to account for merges
			// this adds very little...
			if (mergeCnt.keySet().size() > 0) {
				for (Long val:mergeCnt.keySet()) {
					Integer ct = mergeCnt.get(val);
					int ccnt = ac.getCount(val);
					ac.addCount(val, ccnt*ct);
				}
			}

		}

		//System.out.println("     reduceSET_E["+numberSet+" ] size["+hm.size()+"] " + rCnt);
		return rCnt;
	}

	/**
	 * Reduce remove all non-unique nodes: any that have more than one value
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public static long reduceNonUnique(VegML vML, String dimensionTag, String dataPlaneTag) {
		if (vML.getInCount() < 1) return 0;
		return vML.reduceScan(dimensionTag, dataPlaneTag, new reduceCheck() {
	        @Override
			public boolean removeAccumulator(VegML vML, VDataPlane dataPlane, int setNumber, Accum ac) {
	        	if (ac.getValueCount() > 1) return true;
	        	return false;   
	        }
	    });
	}
	
	/**
	 * Reduce balanced vectors
	 * Remove vectors where all values are balanced ie have the same count / probability
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param allValues if true only remove if all values in dataset are included
	 * @return
	 */
	public static long reduceBalanced(VegML vML, String dimensionTag, String dataPlaneTag, boolean allValues) {
		if (vML.getInCount() < 1) return 0;
		final int dw = vML.getCfgDataWidth(dimensionTag, dataPlaneTag);
		return vML.reduceScan(dimensionTag, dataPlaneTag, new reduceCheck() {
	        @Override
			public boolean removeAccumulator(VegML vML, VDataPlane dataPlane, int setNumber, Accum ac) {
	        	if (allValues && dw != ac.getValueCount()) return false;
	        	if (ac.getBalance(dataPlane.getCfgDataWidth()) == 1) return true;
	        	return false;   
	        }
	    });
	}

	/**
	 * Naive Reduction
	 * reduce vector-values that have been seen <= minUseCount
	 * 
	 * @param vML VegML instance
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param minUseCount min value count to reduce
	 * @param ingnoreFullSet if true don't reduce full numberset
	 * @param ingnoreIdentity if true don't reduce identity numberset
	 * @return
	 */
	public static long reduceNaive(VegML vML, String dimensionTag, String dataPlaneTag, int minUseCount, boolean ingnoreFullSet, boolean ingnoreIdentity) {
		if (vML.getInCount() < 1) return 0;
		//if (minPredictionPercentage < 1) {
			//NOTE: this percentage goes down with larger window and changes per data set
			//      thus it must be set by user OR a function of the data		
		//}
		//System.out.print(" LowByUse["+minUseCount+"]["+maxSetWidth+" of "+window+"] mpp[" + minPredictionPercentage+"] fu["+ingnoreMostFullSet+"]");
		return vML.reduceScan(dimensionTag, dataPlaneTag, new reduceCheck() {
	        @Override
			public boolean removeAccumulator(VegML vML, VDataPlane dataPlane, int setNumber, Accum ac) {
	        	// not the fullest set
	        	if (ingnoreFullSet && setNumber == dataPlane.getCfgNSFullNumber()) return false;
	        	if (ingnoreIdentity && setNumber == dataPlane.getCfgNSIdentityNumber()) return false;

	        	// limit by range
	        	if (ac.getCrtTotal() < minUseCount && ac.getTotal() < minUseCount) {
	        		return true;
	        	}
	        	return false;   
	        }
	    });
	}
	
	/**
	 * Naive Reduction
	 * Remove any vector-value that have been seen less that 2 times
	 * 
	 * @param vML VegML instance to use
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @return
	 */
	public static long reduceNaive(VegML vML, String dimensionTag, String dataPlaneTag) {
		if (vML.getInCount() < 1) return 0;

		//System.out.print(" reduceNaive["+minUseCount+"]["+maxSetWidth+" of "+window+"] mpp[" + minPredictionPercentage+"] fu["+ingnoreMostFullSet+"]");
		return vML.reduceScan(dimensionTag, dataPlaneTag, new reduceCheck() {
	        @Override
			public boolean removeAccumulator(VegML vML, VDataPlane dataPlane, int setNumber, Accum ac) {
	        	// is context
	        	if (!dataPlane.isCfgNSContext(setNumber)) return false;
	        	// limit by range
	        	if (ac.getCrtTotal() < 2 && ac.getTotal() < 2) return true;            	
	        	return false;   
	        }
	        
	        @Override
			public boolean checkValues() {
				return true;
			}
	        /*
	        @Override
			public boolean removeAccumulatorValue(VegML vML, DataPlane dataPlane, int setNumber, Accum ac, List<ValProb> vpList, int position, ValProb vp) {
	        	if (setNumber == dataPlane.getCfgNSIdentityNumber()) return false;
	        	if (setNumber == dataPlane.getCfgNSFullestNumber()) return false;
	        	if (vp.count == 0) return true;
	        	// some diferential
	        	// TODO: this should be a percentage/ratio
	        	if ((vpList.get(0).count/vp.count) < MIN_TOTAL_DIFF_VALUE) return false;
	        	// remove it this is less than 33rd percentile
			//	if (position > (dataPlane.getCfgDataWidth()/3)) return true;	
				if (position > (dataPlane.getCfgDataWidth()/2)) return true;	
	        	return false;
			}*/
	    });
	}

	/**
	 * Wide Reduction
	 * Remove any vector-value that are insignificant in wide vectors
	 * 
	 * @param vML VegML instance to use
	 * @param dimensionTag dataplane dimension tag
	 * @param dataPlaneTag dataplane tag
	 * @param maxWidth maximum number of values to allow per vector
	 * @param reduceValueRetainProbability if true reduce values after most probable and retain probability
	 * @return
	 */
	public static long reduceWideVectors(VegML vML, String dimensionTag, String dataPlaneTag, int maxWidth, boolean reduceValueRetainProbability) {
		if (vML.getInCount() < 1) return 0;

		//System.out.print(" reduceNaive["+minUseCount+"]["+maxSetWidth+" of "+window+"] mpp[" + minPredictionPercentage+"] fu["+ingnoreMostFullSet+"]");
		return vML.reduceScan(dimensionTag, dataPlaneTag, new reduceCheck() {
	        @Override
			public boolean removeAccumulator(VegML vML, VDataPlane dataPlane, int setNumber, Accum ac) {
	        	// is context
	        	if (ac.getValueTotal() > maxWidth) {
	        		if (reduceValueRetainProbability) {
	        			int tot = ac.getTotal();
	        			List<ValProb> vpList = ac.getValPsSorted();
	        			for (int i=maxWidth;i<vpList.size();i++) {
	        				ac.remove(vpList.get(i).value);	        				
	        			}
	    				ac.adjustTotal(tot);
	    				return false;
	        		}
	        		return true; 
	        	}           	
	        	return false;   
	        }
	        
	        @Override
			public boolean checkValues() {
				return true;
			}
	    });
	}

}
