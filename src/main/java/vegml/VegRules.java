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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import vegml.VDataPlane.NSVectMap;
import vegml.ValProb;

import java.util.Set;

/**
 * Rule import/export and symbolic rule APIs for managing model information
 * 
 * TODO: extend to training and testing APIS
 *  // rules can have probability OR absolute
 *  if (X) then Y
 *  	X = x && u OR X = x || u
 *  	- position, value, input, position-input, position/value, position-input/value, input/value, ordered_or_unordered
 *  		[0][1][2]	- position
 *  		 a  b  c	- input
 *  		 9  8  9	- = value
 *  	- each may generate 0-n vector-values to add/remove or modify
 *  
 * 	addRule();
 * 		-- same as update: actions: weight+/-, set weight, set only value(drops others sets 1.0 probability)
 *  removeRule();
 *  
 *  
 *  // probability
 *  double testRule();
 *  
 *  // absolute
 *  boolean testRule();
 *  
 *  // value
 *  long testRule();
 *  
 *  
 */
public class VegRules {
	/**
	 * Type of rule
	 * DEFAULT - from model default
	 * SET - from NumberSet default
	 * GENERAL - from vector probability
	 */
	public enum RuleType {
	  DEFAULT,
	  SET,
	  GENERAL
	}	
	
	/**
	 * Rule information
	 */
	public static class VRuleData {
		RuleType ruleType;
		String rule [][];
		String outcome [];
		int outcomeOffset;		
		double probability;
		int setNumber;
		VRuleData() {
			ruleType = RuleType.GENERAL;
			setNumber = -1;
			outcomeOffset = -1;
			probability = 0;
		}
	}
	
	/**
	 * Format methods for import and export of rules
	 * 
	 * Default formatter produces Sedro SEQ format
	 * Override to import/export alternate format
	 */
	public static class VRuleFormatter {
		/**
		 * Rule generator, override with your own info to rule String
		 * @param vML VegML instance
		 * 
		 * @param ruleType
		 * @param rule sparse array of values in rule
		 * @param outcomeOffset
		 * @param outcome result valueId or value of rule
		 * @param probability rule match probability
		 * @param setNumber numberSet the rule is from
		 * @param setProbability
		 * @param setDefaultValue
		 * @param setDefaultProbability
		 * @param defaultValue
		 * @param defaultProbability
		 * @param obj
		 * @return
		 */
		public String makeRule(VegML vML, RuleType ruleType,
				String rule [], int outcomeOffset, String outcome, double probability, 
				int setNumber, double setProbability, String setDefaultValue, double setDefaultProbability,
				String defaultValue, double defaultProbability, Object obj) {	
			StringBuilder sb = new StringBuilder();
			
			if (ruleType == RuleType.DEFAULT) {
				sb.append("p[").append(probability).append("]val[").append(outcome).append("]t[default] X:p[0] ");
			
			} else if (ruleType == RuleType.SET) {
				sb.append("p[").append(probability).append("]val[").append(outcome).append("]t[set]ns[").append(setNumber).append("] ");
				for (int p=0;p<rule.length;p++) {
					String val = rule[p];
					if (p == outcomeOffset) {
						sb.append("X:");
						if (val == null) {
							sb.append("p[").append(p).append("] ");
							continue;
						}
					}	
					if (val == null) continue; // sparse
					sb.append("n[").append(p).append("] ");
				}
			} else {
				sb.append("p[").append(probability).append("]val[").append(outcome).append("]ns[").append(setNumber).append("] ");
				//srule = "p["+probability+"]val["+outcome+"]sp["+setProbability+"]svp["+setDefaultProbability+"] "; include set/set-value probability ?
				// add values
				for (int p=0;p<rule.length;p++) {
					String val = rule[p];
					if (p == outcomeOffset) {
						sb.append("X:");
						if (val == null) {
							sb.append("p[").append(p).append("] ");
							continue;
						}
					}	
					if (val == null) continue; // sparse
					sb.append("p[").append(p).append("]v[").append(val).append("] ");
				}
			}
			return sb.toString();
		}
		
		/**
		 * Load a symbolic rule to add to the model
		 * @param vML
		 * @param rule
		 * @param obj
		 * @return
		 */
		public VRuleData loadRule(VegML vML, String rule, Object obj) {	
			VRuleData rd = new VRuleData();
			rd.ruleType = RuleType.GENERAL;

			String x [] = rule.split("\\s+");
			// rule 0 - defines
			// p[] val[] t[] ns[]
			String x1 [] = x[0].split("]");
			for (int i=0;i<x1.length;i++) {
				int idx = x1[i].indexOf("[");
				String val = x1[i].substring(idx+1, x1[i].length());
				if (x1[i].startsWith("p[")) {
					rd.probability = Double.parseDouble(val);
				} else if (x1[i].startsWith("t[")) {
					if (val.equals("default")) rd.ruleType = RuleType.DEFAULT;
					else if (val.equals("set")) rd.ruleType = RuleType.SET;
				} else if (x1[i].startsWith("val[")) {
					rd.outcome = val.split("/");
				} else if (x1[i].startsWith("ns[")) {
					rd.setNumber = Integer.parseInt(val);
				}
			}
			rd.rule = new String[x.length-1][];

			for (int i=1;i<x.length;i++) {
				// p[] v[] X:
				String x2 [] = x[i].split("]");
				
				boolean outcome = false;
				String vs [] = null;
				int pos = 0;
				
				for (int xi=1;xi<x.length;xi++) {
					int idx = x2[xi].indexOf("[");
					String val = x2[xi].substring(idx+1, x2[xi].length());
					if (x2[xi].startsWith("X:")) {
						x2[xi] = x2[i].substring(2);				
						outcome = true;
					}
					if (x2[xi].startsWith("n[")) {	// position
						pos = Integer.parseInt(val);
					} else if (x2[xi].startsWith("p[")) {	// position
						pos = Integer.parseInt(val);
						if (outcome) rd.outcomeOffset = pos;
					}
					if (x2[xi].startsWith("v[")) { // values
						vs = val.split("/");
					} 
				}
				rd.rule[pos] = vs;
			}

			return rd;
		}
	}
	
	
	
	/**
	 * Load a rule into a VegML model
	 * 
	 * @param vML  VegML to generate from
	 * @param dimension  dimension tag to generate for
	 * @param dataPlane  dataPlane tag to generate for
	 * @param rGen  rule formatter
	 * @param rGenObj  caller pass through object
	 * @param rule  string format of rule
	 * @return 1 on success
	 */
	public static int loader(VegML vML, String dimensionTag, String dataPlaneTag, VRuleFormatter rGen, Object rGenObj, String rule) {
		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return -1;
		
		VRuleData dat = rGen.loadRule(vML, rule, rGenObj);
		if (dat == null) return -1;
		if (dat.setNumber >= 0) {
			// add to specific numberset 
			MLNumberSetHash nsh = dataPlane.getNSHash(dat.setNumber);
			// get set of vectors
			// FIXME
			
			// add or get set of accumulators
			// FIXME
			
			// ac.set(value, probabiltity) for each value
			// FIXME
						
		} else {
			// add where it looks appropriate
			
			// make numberSet mask for what it included
			
			// FIXME load this rule into the VegML
		}
		
		return 1;
	}
	
	/**
	 * Get list of rules from the vML
	 * 
	 * @param vML  VegML to generate from
	 * @param dimension  dimension tag to generate for
	 * @param dataPlane  dataPlane tag to generate for
	 * @param rGen  rule formatter
	 * @param rGenObj  caller pass through object
	 * @param retRules  true to return the rules in a string list
	 * @return list of string rules
	 */
	public static List<String> generateAll(VegML vML, String dimensionTag, String dataPlaneTag, 
			VRuleFormatter rGen, Object rGenObj, boolean retRules) {
		return generateAll(vML, dimensionTag, dataPlaneTag, 0, -1, null, rGen, rGenObj, retRules);
	}
	
	/**
	 * Get list of rules from the vML
	 * 
	 * @param vML VegML to generate from
	 * @param dimension dimension tag to generate for
	 * @param dataPlane dataPlane tag to generate for
	 * @param minProbability  minimum probability to allow
	 * @param maxDepth  of rules for each Accum -1=all, 0=varialbe, 1=first,2=first 2, 3=first 3... until dataWidth
	 * @param outcomeValue  only for rules with this outcome if present
	 * @param rGen  rule formatter
	 * @param rGenObj  caller pass through object
	 * @param retRules  true to return the rules in a string list
	 * @return list of string rules
	 */
	public static List<String> generateAll(VegML vML, String dimensionTag, String dataPlaneTag, 
			double minProbability, int maxDepth,
			Integer outcomeValue, VRuleFormatter rGen, Object rGenObj, boolean retRules) {
		if (vML.getDataTotal() == 0) return null;

		VDataPlane dataPlane = vML.getDataPlane(dimensionTag, dataPlaneTag);
		if (dataPlane == null) return null;
		if (!vML.hasDimensionStrings() || !vML.hasVectSets()) {
			System.out.println("ERROR: GenRule for["+dimensionTag+"]["+dataPlaneTag+"] missing dimensionStrings or VectSets: configure model");
			return null;
		}
		
		System.out.println("VegML GenRule for["+dimensionTag+"]["+dataPlaneTag+"]["+vML.getDataTotal()+"]");
		
		// Smash everything
		vML.smash(true);
		
		
		// determine min probabilityOutcome to retain
		// FIXME 
			// find the minimum-maximum, minimum-average of all Accumulators
			// one of these ?
			// the min will differ for each numberset ??

		
		
		//  need all dimensions and dataplans
		String rule [] = new String[dataPlane.getCfgWindowSize()];
		int cnt = 0;
		int baseRuleCnt = 0;
		int baseRuleReducedCnt = 0;
		int setAccList = 0;
		int leftCnt = 0;
						
		List<String> ruleList = new ArrayList<>();
		
		// default Accumulator
		Accum dac = dataPlane.getAccumDefault();
		ValProb defVP = dac.getFirstMostProbable();
				
		double defProbability = defVP.probability;
		String defValue = dataPlane.getString(defVP.value);
		if (defValue == null) defValue = "ERR:"+defVP.value;
				
		// call once with the default rule (dac)
		String dnr = rGen.makeRule(vML, RuleType.DEFAULT, rule, -1, defValue, defProbability, -1, (double)0, null, (double)0, 
									defValue, defProbability, rGenObj);
		if (dnr != null) {
			if (retRules) ruleList.add(dnr);
			cnt++;		
		}
		List<Integer> fset = dataPlane.getCfgNSFull();

		for (int i=0;i<dataPlane.getNSCount();i++) {
			setAccList++;
			MLNumberSetHash nsh = dataPlane.getNSHash(i);
			if (nsh == null || nsh.size() < 1) continue;
			if (vML.isCfgShowProgress()) System.out.println("  GenRule NumberSet["+i+"]["+nsh.size()+"] ["+dataPlane.getNSFormatString(i)+"]");
			
			// get the set
			List<Integer> set = nsh.getNS();
			// set Accumulator
			Accum sac = nsh.getAccumSetDefault();

			// FIXME use correct one			
			ValProb setVP = sac.getFirstMostProbable();
			if (setVP == null) {
				System.err.println("ERROR SET["+i+"]cnt["+nsh.size()+" / "+sac.getTotal()+"] missing getFirstMostProbable");
				continue;
			}
			
			String setValue = dataPlane.getString(setVP.value);
			if (setValue == null) setValue = "ERR:"+setVP.value;
			
			double setProbability = setVP.probability;
			
			int setMaxDepth = maxDepth;

			
			//int focus = vML.getSetFocus(set);
			//System.out.println("SET["+i+"]["+vML.getCfgWindowSize()+"]["+set.size()+"] focus["+vML.getFocusOffset()+"] cnt["+hm.size()+" / "+sac.getTotal()+"] " + VegML.setToString(set));
			
			// add the probability of the set matching??
			double sProb = (double)sac.getTotal() / (double)dac.getTotal();

			// add set rule IF it has a count			
			if (sac.getTotal() <= 0 || setProbability <= 0) {
				continue;
			}
			// number of elements in numberset
			int elementCount = nsh.getNSSize();
			
			// add set rule: positions in position spaces... no words to resolve (or '*') ..
			for (int ri=0;ri<rule.length;ri++) rule[ri] = null;
			for (Integer position:set) {
				if (position < rule.length && position >= 0) rule[position] = ""+position;
				// FIXME out of band
			}
			String snr = rGen.makeRule(vML, RuleType.SET, rule, dataPlane.getCfgFrameFocus(), setValue, setProbability, 
										i, sProb, setValue, setProbability, defValue, defProbability, rGenObj);
			if (snr != null) {
				if (retRules) ruleList.add(snr);
				cnt++;		
			}
		

			List<int []> vsSets = new ArrayList<>();
			List<ValProb> acSet = new ArrayList<>();

			// iterate through each SET
			Iterator<Accum> it = nsh.iterator();
			while (it.hasNext()) {
				Accum vs = it.next();
				// get the probabilities	
				List<ValProb> vpList = vs.getValPsSorted();
				
				// each based on the focus and set generate a simple match for probability
				// <1> <2> <X> value <3> == probability
				// thus each vss creates a list of match rules that give a probability for something
				baseRuleCnt += vpList.size();
				double mostProb = vpList.get(0).probability;
				if (mostProb < minProbability && minProbability > 0) {
					//	System.out.println("   Bst["+vs.getValueCount()+"]["+hp+"]");
					continue;
				} 				
				baseRuleReducedCnt += vpList.size();
					
				// get the vector set
				vsSets.clear();
				
				//if (VectSetMap.isSVSID(vs.getVectSetId())) {
				if (vs.getVectSetId() == -1) {
					// get the full list of vectSets
					List<NSVectMap> vml = nsh.getVectorMapVectorToSet(vs.getVectorCode());
					for (int mi=0;mi<vml.size();mi++) {
						int [] vectSet = vML.getVectorSetForId(vml.get(mi).vsid, fset, set);
						if (vectSet == null) continue;
						vsSets.add(vectSet);
					}
				} else {
					int [] vectSet = vML.getVectorSetForId(vs.getVectSetId(), fset, set);
					if (vectSet == null) continue;
					vsSets.add(vectSet);
				}
				// iterate the individual Accumulators values				
				int depth = 0;
				
				// get the next set with the same probability
				acSet.clear();
				ValProb lastVp = null;
				Set<String> ms = new HashSet<>();
				
				for (int k=0;k<vpList.size();k++) {
					ValProb vp = vpList.get(k);
					if (outcomeValue != null && vp.value != outcomeValue) {
						// not the desired outcome
						continue;
					}
					
					if (setMaxDepth >= 0 && depth >= setMaxDepth) {
						leftCnt += vpList.size()-k;
						break;
					}
					if (mostProb != vp.probability) depth++;
					
					// use method for getting probability
					double probability = dataPlane.getCfgPCalc().calculateProb(dataPlane.getCfgProbMethod(), dataPlane, dac, dataPlane.getCfgNSWeightRaw(), i, false, sac, vs, vp);
					// check minimum
					if (probability < minProbability && minProbability > 0) {
						// if we drop on minprobability then a set of low probability rules that provide high probability togther will not be retained
						continue;
					}
					
					///////////////////////////
					// check if end of list of like probabilities
					if (lastVp != null && (lastVp.probability != vp.probability || k == (vpList.size()-1))) {
						// new probabilty... end of list
						if (acSet.size() == 0) System.out.println(" AC["+vpList.size()+"]["+lastVp.probability+"] " + k);
						lastVp = vp;
						if (k != (vpList.size()-1)) k--;
					} else if (lastVp == null) {
						acSet.add(vp);
						if (vpList.size() != 1) {
							lastVp = vp;
							continue;
						}
					} else {
						acSet.add(vp);
						continue;
					}
					
					///////////////////////////
					// get value string(s)
					StringBuilder sbv = new StringBuilder();
					sbv.append(dataPlane.getString(acSet.get(0).value));			
					if (acSet.size() > 1) {
						for (int x=1;x<acSet.size();x++) {
							if (x != 0) sbv.append("/");
							sbv.append(dataPlane.getString(acSet.get(x).value));
						}						
					}
					String value = sbv.toString();
					acSet.clear();
				
					///////////////////////////
					// Make Final Rule
					int lposition = -1;
					
					// split vector sets into groups that can work in a rule as one
					// CAN only mix if 1 and only one difference
					Arrays.fill(rule, null);
					
					List<List<int []>> vsList = segmentVectSet(vsSets, elementCount);
					for (int x=0;x<vsList.size();x++) {
						List<int []> vsRuleSet = vsList.get(x);
									
						// for each position in the set
						for (int p=0;p<set.size();p++) {
							int position = set.get(p);
							if (position < 0) continue; // FIXME out of band
							
							if (vsRuleSet.size() == 1) {
								// one string mapping
								int [] vectSet = vsRuleSet.get(0);
								rule[position] = dataPlane.getDimensionString(vectSet[p]);
							} else if (elementCount == 1 && vsRuleSet.size() == dataPlane.getCfgDataWidth()) {
								// general mapping ALL in
								//int [] vectSet = vsSets.get(0);
								rule[position] = "*";
						//	} else if (vsSets.size() > (dataPlane.getCfgDataWidth()/2)) {
								// make execption rull instead for better performance
								// TODO
							} else {
								StringBuilder sb = new StringBuilder();
								ms.clear();
								
								// multiple string mappings: '/' between each
								for (int mi=0;mi<vsRuleSet.size();mi++) {
									int [] vectSet = vsRuleSet.get(mi);
									ms.add(dataPlane.getDimensionString(vectSet[p]));
								}
								int o = 0;
								for (String r:ms) {
									if (o != 0) sb.append("/");
									sb.append(r);
									o++;
								}
								rule[position] = sb.toString();
							}
							lposition = position;
						}
						
						// call the call back
						String nr = rGen.makeRule(vML, RuleType.GENERAL, rule, dataPlane.getCfgFrameFocus(), value, probability, 
								i, sProb, setValue, setProbability, 
								defValue, defProbability, rGenObj);
						if (retRules && nr != null) ruleList.add(nr);
						cnt++;
					}
				}
			}
		}
		System.out.println("   RULE_CNT["+cnt+"] vpCnt["+baseRuleCnt+"] vpNot["+baseRuleReducedCnt+"]");

		return ruleList;
	}
	
	// CAN only mix if 1 and only one difference
	private static List<List<int []>> segmentVectSet(List<int []> vsSets, int elementCount) {
		List<List<int []>> olist = new ArrayList<>();
		if (elementCount == 1) {
			olist.add(vsSets);
			return olist;
		}
		
		int sz = vsSets.get(0).length;	
		
		HashMap<Integer, List<int []>> tmpSet = new HashMap<>();
		List<List<int []>> nlist = new ArrayList<>();
		List<List<int []>> nnlist = new ArrayList<>();
		nlist.add(vsSets);
		
		for (int x=0;x<sz;x++) {
			// split for position x into sets in tmplist
			for (int y=0;y<nlist.size();y++) {
				
				List<int []> eSet = nlist.get(y);
				for (int i = 0;i<eSet.size();i++) {
					int [] vs = eSet.get(i);
					if (vs == null) continue;
					// check position X
					List<int []> vl = tmpSet.get(vs[x]);
					if (vl == null) {
						vl = new ArrayList<>();	// new
						tmpSet.put(vs[x], vl);
					} 
					vl.add(vs);
				}
				// move them here
				for (Entry<Integer, List<int []>> ent:tmpSet.entrySet()) {
					if (ent.getValue().size() == 1) olist.add(ent.getValue());	// no more merges
					else nnlist.add(ent.getValue());	// move for next pass
				}
				tmpSet.clear();
			}
			
			// move singles to final
			nlist = nnlist;
			nnlist = new ArrayList<>();	
		}
		
		olist.addAll(nlist);
		return olist;
	}
	
	
	
}
