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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vegml.Data.VDataSetDescriptor.DSDataType;
import vegml.OptimizerMerge.MergeMap;
import vegml.VegML.PredictionType;
import vegml.ValProb;

/**
 * for inline override methods of the callout and set it for a dataplane
 * This may cause issues if you save and reload.
 * 
 * Its recommended to extend the class and override the methods with an updated serialVersionUID
 * this should address strangeness in creating and processing
 *
 */
public class VegCallOut implements java.io.Serializable {
	private static final long serialVersionUID = -6163575846839386950L;

	public void refineResults(VContext ctx, VDataPlane dataplane, Object arg, VFrame frame, List<Long> valueOut, PredictionType predictType, List<ValProb> vpList, long answerValue) {		
	
	}
	public void init(VDataPlane dp) {
	}
	
	/**
	 * condition for conditional process
	 */
	static public class VegCOMCondition {
		public boolean condition(VContext ctx, VDataPlane dataplane, Object arg, VFrame frame, List<Long> valueOut, PredictionType predictType, List<ValProb> vpList) {	
			return true;
		}
		public void init(VDataPlane dp) {
		}
	}
	
	/**
	 * extended callout for configurable merging
	 *
	 */
	static public class VegCallOutMods extends VegCallOut {
		private static final long serialVersionUID = 5768965637773515225L;

		private static class VCOMap {
			PredictionType pType;
			String dTag;
			String dpTag;
			boolean amp;
			int ampOValue; 	// numberSets with focus - controls gain based on noise in set
			boolean lower;	// lowercase
			boolean replace;
			boolean ison;	// true if to be used
			double mergeValue;
			// TODO: extend with callback object for logic? for trylower et
			// TODO: option to set as last if values
			//Set<Long> checkSet = null;
			VegCOMCondition cond;
		}
		private HashMap<PredictionType, List<VCOMap>> vcListHm = null;
		
		VegCallOutMods() {
			vcListHm = new HashMap<>();
		}
		
		// 
		/**
		 * turn on/off mappings
		 * @param dTag dimension tag
		 * @param dpTag tag
		 * @param pType prediction type to bound to
		 * @param ison true to turn on, false off
		 */
		public void setIsOn(String dTag, String dpTag, PredictionType pType, boolean ison) {
			List<VCOMap> pl = vcListHm.get(pType);
			if (pl == null) return;
			for (VCOMap vm:pl) {
				if (dTag.equals(vm.dTag) && dpTag.equals(vm.dpTag)) {
					vm.ison = ison;
					break;
				}
			}
		}
		
		/**
		 * Add model as amplified
		 * 
		 * @param pType prediction type to bind to
		 * @param ampOValue amp value for default, -1 for natural
		 */
		public void setAmp(PredictionType pType, int ampOValue) {
			set(pType, null, null, true, 1.0, true, ampOValue, null);
		}
		
		/**
		 * Add model to re-run with inputs set to lowercase
		 * Add conditional if this is conditional
		 * @param pType prediction type to bind to
		 * @param cond condition or null for none
		 */
		public void setLower(PredictionType pType, VegCOMCondition cond) {
			VCOMap vm = set(pType, null, null, true, 1.0, false, -1, cond);
			vm.lower = true;
		}
		
		/**
		 * Add model to replace the results of the base or prior model
		 * 
		 * @param pType prediction type to bind to
		 * @param dTag dimension tag
		 * @param dpTag tag
		 */
		public void setReplace(PredictionType pType, String dTag, String dpTag) {
			set(pType, dTag, dpTag, true, 1.0, false, -1, null);
		}
		
		/**
		 * Add model to replace the results of the base or prior model with amplified results
		 * 
		 * @param pType prediction type to bind to
		 * @param dTag dimension tag
		 * @param dpTag tag
		 * @param ampOValue amp value for default, -1 for natural
		 */
		public void setReplaceAmp(PredictionType pType, String dTag, String dpTag, int ampOValue) {
			set(pType, dTag, dpTag, true, 1.0, true, ampOValue, null);
		}
		
		/**
		 * Add model to merge into existing results
		 * 
		 * @param pType prediction type to bind to
		 * @param dTag dimension tag
		 * @param dpTag tag
		 * @param defMergeValue set the default merge value
		 */
		public void setMerge(PredictionType pType, String dTag, String dpTag, double defMergeValue) {
			set(pType, dTag, dpTag, false, defMergeValue, false, -1, null);
		}
		
		/**
		 * Add model to merge into existing results, amplify the results prior to merge
		 * 
		 * @param pType prediction type to bind to
		 * @param dTag dimension tag
		 * @param dpTag tag
		 * @param defMergeValue set the default merge value
		 * @param ampOValue amp value for default, -1 for natural
		 */
		public void setMergeAmp(PredictionType pType, String dTag, String dpTag, double defMergeValue, int ampOValue) {
			set(pType, dTag, dpTag, false, defMergeValue, true, ampOValue, null);
		}
		
		/**
		 * Add a Model to the results processing stack, models can replace or merge, and can be amplified
		 * to amplify the base model just specify the same dTag/dpTag as the base
		 * 
		 * @param pType prediction type to bind to
		 * @param dTag dimension tag
		 * @param dpTag tag
		 * @param replace replace the existing results
		 * @param defMergeValue default merge value if merge
		 * @param amp true if amp
		 * @param ampOValue default amp range
		 * @param cond conditional if runs only on condition
		 * @return
		 */
		public VCOMap set(PredictionType pType, String dTag, String dpTag, boolean replace, double defMergeValue, boolean amp, int ampOValue, VegCOMCondition cond) {
			VCOMap vm = new VCOMap();
			vm.pType = pType;
			vm.dTag = dTag;
			vm.dpTag = dpTag;
			vm.replace = replace;
			vm.ampOValue = ampOValue;
			vm.amp = amp;
			vm.mergeValue = defMergeValue;
			vm.cond = cond;
			vm.lower = false;
			vm.ison = true;
			
			// map to many if mutli-type			
			if (pType == PredictionType.AnyUnknown) {
				addTo(PredictionType.PredictUnknown, vm);
				addTo(PredictionType.Default, vm);
				addTo(PredictionType.Fail, vm);
			} else if (pType == PredictionType.NotUnknown) {
				addTo(PredictionType.Predict, vm);
				addTo(PredictionType.Recall, vm);
				addTo(PredictionType.RecallPredict, vm);
				addTo(PredictionType.PredictRelate, vm);
			} else if (pType == PredictionType.AnyPredict) {
				addTo(PredictionType.Predict, vm);
				addTo(PredictionType.PredictRelate, vm);
			} else if (pType == PredictionType.AnyRecall) {
				addTo(PredictionType.Recall, vm);
				addTo(PredictionType.RecallPredict, vm);
			} else if (pType == PredictionType.All) {
				addTo(PredictionType.PredictUnknown, vm);
				addTo(PredictionType.Default, vm);
				addTo(PredictionType.Fail, vm);
				addTo(PredictionType.Predict, vm);
				addTo(PredictionType.Recall, vm);
				addTo(PredictionType.RecallPredict, vm);
				addTo(PredictionType.PredictRelate, vm);
			} else {
				addTo(pType, vm);
			}
			return vm;
		}
		private void addTo(PredictionType pType, VCOMap vm) {
			List<VCOMap> pl = vcListHm.get(pType);
			if (pl == null) {
				pl = new ArrayList<>();
				vcListHm.put(pType, pl);
			}
			pl.add(vm);			
		}
		
		String getAmpTuneName(VDataPlane dp) {
			return "AMP_"+dp.getDimensionTag()+"_"+dp.getTag();
		}
		String getBreakAfterName(VDataPlane dp) {
			return "AFT_"+dp.getDimensionTag()+"_"+dp.getTag();
		}
		String getBreakBeforeName(VDataPlane dp) {
			return "BFE_"+dp.getDimensionTag()+"_"+dp.getTag();
		}
		
		@Override
		public void refineResults(VContext ctx, VDataPlane dataplane, Object arg, VFrame frame, List<Long> valueOut, PredictionType predictType, List<ValProb> vpList, long answerValue) {		
			if (vcListHm.keySet().size() < 1) return;
			// get for prediction type
			List<VCOMap> pl = vcListHm.get(predictType);
			if (pl == null) return;
			
			VDataPlane ldp = null;
			
			// process them in order
			VDataPlane cdp = dataplane;
			VFrame cframe = frame;
			for (int x=0;x<pl.size();x++) {
				VCOMap vm = pl.get(x);
				if (!vm.ison) continue;
				
				// get dataplane
				VDataPlane dp = dataplane;
				if (vm.dpTag != null) dp = dataplane.getVegML().getDataPlane(vm.dTag, vm.dpTag);
				if (dp == null) {
					System.out.println("ERROR CA["+frame.getDataSetPosition()+"] dataplane["+vm.dTag+"/"+vm.dpTag+"] missing");
					continue;
				}
					
				// no recursion			
				if (!ctx.addCallout(dp)) {
					continue;
				}

				if (dataplane.getCfgMergeBreakBefore(dp.getDimensionTag(), dp.getTag())) {
					// break before
					ctx.removeCallout(dp);
					break;
				}
				if (ldp != null && dataplane.getCfgMergeBreakAfter(dp.getDimensionTag(), dp.getTag())) {
					// break after
					ctx.removeCallout(dp);
					break;
				}
				ldp = dp;
				
				if (vm.cond != null && !vm.cond.condition(ctx, dataplane, arg, frame, valueOut, predictType, vpList)) {
					ctx.removeCallout(dp);
					continue;		
				}
				
				int volen = valueOut.size();							
				VFrame unFrame = null;
		
				////////////////
				// use this dataplan at base
				if (vm.lower) {
					List<ValProb> ivpl = dataplane.getDimensionStringProbList(frame.getString().toLowerCase());
					if (ivpl != null) {		
						// need to be able to ensure correct size frame: add start / end
						List<ValProb> vl = VegTest.predictVPLS(ctx, dataplane.getDimensionTag(), dataplane.getTag(), valueOut, frame.getFrameStringsLower(), dataplane.getCfgFrameFocus(), frame.getDataSetNumber());								
						while (volen < valueOut.size()) valueOut.remove(valueOut.size()-1); // was added... remove		
						vpList.clear();
						if (vl != null && vl.size() > 0 && vl.get(0).type != PredictionType.Default) {
							vpList.addAll(vl);					
						} else {
							vpList.addAll(ivpl);
						}	
						Collections.sort(vpList, VegUtil.VpSort);				
						vpList.get(0).type = predictType;	
						ctx.removeCallout(dp);
						break;
					}
				}
									
				////////////////
				// This is Self amplify
				if (vm.dpTag == null && vm.amp) {
					if (vpList.size() > 1) {
						int ampVx = vm.ampOValue;
						int ampValx = dataplane.getCfgAmpTuneValueX(cdp.getDimensionTag(), cdp.getTag()); // self
						if (ampValx != -2) ampVx = ampValx;
						long [] valSet = new long[2];
						valSet[0] = vpList.get(0).value;
						valSet[1] = vpList.get(1).value;	
						// amp must be with the base dataplane after select..
						VegTest.getValAmp(ctx, cdp, cframe, valueOut, valSet, false, -1, ampVx);				
						if (vpList != cframe.getVPList()) {
							// if current frame is not from dataplane need to move list from cframe to frame's vplist
							vpList.clear();	
							vpList.addAll(cframe.getVPList());
						}
						Collections.sort(vpList, VegUtil.VpSort);
						vpList.get(0).type = predictType;
					}
					ctx.removeCallout(dp);
					continue;
				}
								
				////////////////
				// get predictions from another dataplane
				List<ValProb> pvl = null;	
				if (ctx.getModTest() != -1) {
					// modifier
					if (dp.getCfgInputDataType() == DSDataType.Char) {
						unFrame = VegTest.predictFrameVPFrameModify(ctx, vm.dTag, vm.dpTag, ctx.getModTests().get(dp, ctx.getModTest()), valueOut, frame.getString());	
					} else {
						unFrame = VegTest.predictVPFrameModify(ctx, vm.dTag, vm.dpTag, ctx.getModTests().get(dp, ctx.getModTest()), valueOut, frame);
					}
				} else {
					if (dp.getCfgInputDataType() == DSDataType.Char) {
						unFrame = VegTest.predictFrameVPFrame(ctx, vm.dTag, vm.dpTag, valueOut, frame.getString());
					} else {
						unFrame = VegTest.predictVPFrame(ctx, vm.dTag, vm.dpTag, valueOut, frame);
					}
				}
				if (unFrame != null) pvl = unFrame.getVPList();
				while (volen < valueOut.size()) valueOut.remove(valueOut.size()-1); // was added... remove		
				
				// got nothing..
				if (pvl == null || pvl.size() == 0 || pvl.get(0).type == PredictionType.Default) {
					if (vm.replace) { // replace
						cdp = dp;
						cframe = unFrame;
						vpList.clear();	
						if (pvl != null && vpList.size() > 0) {
							vpList.addAll(pvl);
							Collections.sort(vpList, VegUtil.VpSort);				
							vpList.get(0).type = predictType;
						}
					}
					ctx.removeCallout(dp);
					continue;
				}
								
				////////////////
				// amplify
				if (vm.amp && unFrame != null && pvl.size() > 1) {
					// if amplify AND more than 1 option AND results are from this dataplane
					int ampVx = vm.ampOValue;
					int ampValx = dataplane.getCfgAmpTuneValueX(dp.getDimensionTag(), dp.getTag()); // other
					if (ampValx != -2) ampVx = ampValx;
					long [] valSet = new long[2];
					valSet[0] = pvl.get(0).value;
					valSet[1] = pvl.get(1).value;
					VegTest.getValAmp(ctx, dp, unFrame, valueOut, valSet, false, -1, ampVx);	
					while (volen < valueOut.size()) valueOut.remove(valueOut.size()-1); // was added... remove		
					pvl = unFrame.getVPList();					
					Collections.sort(pvl, VegUtil.VpSort);					
				} 

				// integrate the results via replace or merge
				if (vm.replace) {
					// REPLACE
					vpList.clear();	
					vpList.addAll(pvl);
					cdp = dp;
					cframe = unFrame;
				} else {
					// MERGE / map and mode
					MergeMap mm = dataplane.getCfgMergeMap(vm.dTag, vm.dpTag);
					if (mm != null && mm.haveMergeValue(predictType, mm.getMergeMode())) {	
						// complex merge
						double mergePMixed = mm.getMergeValue(predictType, pvl.get(0).type, mm.getMergeMode());
						if (mm.getMergeMode() == 3) {
							VegUtil.mergeVPSet(vpList, pvl, mergePMixed);
						} else {
							VegUtil.mergeVPSet(vpList, pvl, mergePMixed, mm.getMergeValueVM(predictType, pvl.get(0).type, mm.getMergeMode()));	
						}
					} else {
						// value added
						double mValue = dataplane.getCfgMergeWeight(dp);
						if (mValue > 0) VegUtil.mergeVPSet(vpList, pvl, mValue);
						else if (mValue != 0) VegUtil.mergeVPSet(vpList, pvl, vm.mergeValue);
					}
				}
				if (vpList.size() > 0) {
					Collections.sort(vpList, VegUtil.VpSort);				
					vpList.get(0).type = predictType;
				}
				
				ctx.removeCallout(dp);
			}
		} 
	}
	
	/**
	 * Get a base for a modifiable callout
	 * 
	 * @return new callout
	 */
	public static VegCallOutMods makeCallOut() {
		VegCallOutMods vcom = new VegCallOutMods();
		return vcom;
	}	
	
	
	/**
	 * Get condition for lower case check 
	 * Where check if new line OR after tag ./'/:/``/(/BR 
	 * or is all upper-case
	 * @return
	 */
	public static VegCOMCondition getConditionCheckLowerCase() {	
		return new VegCOMCondition() {
			Set<Long> checkSet = new HashSet<>(6);			
			@Override
			public void init(VDataPlane dp) {
				checkSet.add((long)dp.getCfgVToV().toVectGen("."));
				checkSet.add((long)dp.getCfgVToV().toVectGen("''"));
				checkSet.add((long)dp.getCfgVToV().toVectGen(":"));
				checkSet.add((long)dp.getCfgVToV().toVectGen("``"));
				checkSet.add((long)dp.getCfgVToV().toVectGen("("));
				checkSet.add((long)dp.getCfgVToV().toVectGen("BR"));
			}
			@Override
			public boolean condition(VContext ctx, VDataPlane dataplane, Object arg, VFrame frame, List<Long> valueOut, PredictionType predictType, List<ValProb> vpList) {				
				String str = frame.getString();
				if (VegUtil.isLowerCase(str)) return false;				
				if (VegUtil.isUpperCase(str)) return true;								
				if (frame.getDataSetPosition() == 0 && frame.getDataSet() != null) return true;				
				if (valueOut.size() > 0 && checkSet.contains(valueOut.get(valueOut.size()-1))) return true;						
				return false;
			}
		};
	}
	
	/**
	 * Get callout that conditionally uses lowercase for unknowns and amplifies Predicts
	 * @param vML VegML instance
	 * @param dTag dimension tag
	 * @param dpTag tag
	 * @return
	 */
	public static VegCallOutMods getCallOutLowerCaseCondAmp(VegML vML, String dTag, String dpTag) {	
		VDataPlane dp = vML.getDataPlane(dTag, dpTag);

		// create condition to decide if lower case should be tried
		VegCOMCondition cond = getConditionCheckLowerCase();
		cond.init(dp);
			
		VegCallOutMods vcom = VegCallOut.makeCallOut();
		// conditional lowerCase
		vcom.setLower(PredictionType.AnyUnknown, cond);		
		vcom.setAmp(PredictionType.NotUnknown, -1);	
		vcom.setAmp(PredictionType.PredictUnknown, -1);	
		return vcom;
	}

	/**
	 * Get callout that amplifies Predicts
	 * @param vML VegML instance
	 * @param dTag dimension tag
	 * @param dpTag tag
	 * @return
	 */
	public static VegCallOutMods getCallOutAmp(VegML vML, String dTag, String dpTag) {	
		VegCallOutMods vcom = VegCallOut.makeCallOut();	
		vcom.setAmp(PredictionType.NotUnknown, -1);	
		vcom.setAmp(PredictionType.PredictUnknown, -1);	
		return vcom;
	}

	/**
	 * Create a callout that merges over on base
	 * @param vML VegML instance
	 * @param dTag base dimension tag
	 * @param dpTag base tag
	 * @param dTagOver merge over dimension tag
	 * @param dpTagOver merge over tag
	 * @param mergeValue
	 * @return callout 
	 */
	public static VegCallOutMods getMergeCallOut(VegML vML, String dTag, String dpTag, String dTagOver, String dpTagOver, double mergeValue) {		
		VegCallOutMods vcom = VegCallOut.makeCallOut();
		vcom.setMerge(PredictionType.All, dTagOver, dpTagOver, mergeValue);
		return vcom;
	}
	
	/**
	 * Create a callout that amplifies the base then merges over on top
	 * 
	 * @param vML VegML instance
	 * @param dTag base dimension tag
	 * @param dpTag base tag
	 * @param dTagOver merge over dimension tag
	 * @param dpTagOver merge over tag
	 * @param ampValOther default amp value for other
	 * @param mergeValue default merge value to use
	 * @return callout
	 */
	public static VegCallOutMods getMergeCallOutAmp(VegML vML, String dTag, String dpTag, String dTagOver, String dpTagOver, int ampValOther, double mergeValue) {
		VegCallOutMods vcom = VegCallOut.makeCallOut();		
		vcom.setAmp(PredictionType.All, ampValOther);	
		vcom.setMerge(PredictionType.All, dTagOver, dpTagOver, mergeValue);
		return vcom;
	}
}
