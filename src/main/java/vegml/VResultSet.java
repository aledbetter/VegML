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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vegml.VDRetainSet.DsRetainVal;
import vegml.Data.VDataSetDescriptor;
import vegml.VegML.PredictionType;
import vegml.ValProb;


public class VResultSet {
	static final int DEF_VALOUT = 1024*5;

	//////////////////////////////////
	// stats
	public int total = 0;
	public int failTotal = 0;
	public int failSingleTotal = 0;
	public int failMultiTotal = 0;
	public int passTotal = 0; 
	public int passSingleTotal = 0; 	// pass when just single value
	public int passMultiTotal = 0; 	// pass when just single value
	public double failPercent = 0;
	public double passPercent = 0;
	
	public int changeCount = 0;
	public int changePass = 0;	// to pass
	public int changeFail = 0;	// to fail

	public int unkToken = 0;
	public int [] pTpass = new int[VegML.getPredictionTypeCount() +1];
	public int [] pTSpass = new int[VegML.getPredictionTypeCount() +1];
	public int [] pTfail = new int[VegML.getPredictionTypeCount() +1];
	
	// timing
	public long startTime = 0;
	public long endTime = 0;
	private VDataPlane dataPlane = null;
	 
	//////////////////////////////////
	// internal use
	// for optimizer
	double val = 0;
	double val2 = 0;
	int uvalCount = 0;  // user
	Object valData = null;
	
	// used for progress
	int progressLast = 0;
	int progressCnt = 0;	
	// results if test
	List<Long> valueOut = null;	// in context processing out... should this be on frame?	
	
	//////////////////////////////////
	// output	
	// vpList pruned()
	List<List<List<ValProb>>> responseVpOut = null;	// complex results
	List<List<Long>> responseOut = null;			// simplex results
	
	// retained resoltion set
	List<List<DsRetainVal>> dsrListSet = null;
	// it would be nice to always get this info.. (retain postion?)
	List<DsRetainVal> dsrToPass = null;		// change to pass
	List<DsRetainVal> dsrToFail = null;		// change to fail
	List<DsRetainVal> dsrToChange = null; 	// all changes
	List<Long> dsrToChangeVal = null;

	// In test when getting back counts for correct/contains responses
	private List<containInfo> containCounts = null;
	/**
	 * per token position info
	 */
	static class containInfo {
		int count;				// our count for winning value
		double probability;		// our prob for winning value
		double wprobability;	// winners probability
		boolean win;			// true if win
		long vid;				// our vid
		int vpSize;				// count of values
	}

	
	//////////////////////////////////
	// the ingress data set definition
	public VDataSetDescriptor dsd = null;

	/**
	 * response value stats
	 */
	static class ResultValueStat {
		public long value;
		public int fail;
		public double fscore = 0;
		public double passGuessPercent = 0;
		public double passPercent = 0;	
		public int pass;
		public int pfail;
		public int [] pTpass = new int[VegML.getPredictionTypeCount() +1];
		public int [] pTfail = new int[VegML.getPredictionTypeCount() +1];
		public int [] pTpfail = new int[VegML.getPredictionTypeCount() +1];
		ResultValueStat() {
			Arrays.fill(pTpass, 0);
			Arrays.fill(pTfail, 0);
			Arrays.fill(pTpfail, 0);
			pass = fail = pfail = 0;
			fscore = 0;
		}
	}		
	private HashMap<Long, ResultValueStat> valueStats = null;

	
	//////////////////////////////////
	// constructors
	public VResultSet() {
		this.valueOut = new ArrayList<>(DEF_VALOUT);
	}

	public VResultSet(VDataPlane dataplane) {
		this.dataPlane = dataplane;
		this.valueOut = new ArrayList<>();
	}	
	
	/**
	 * reset this result set to be used again
	 */
	void reset() {
		if (valueStats != null) valueStats.clear();
		dsrListSet = null;
		dsd = null;
		responseVpOut = null;
		responseOut = null;
		total = failTotal = passTotal = passSingleTotal = failSingleTotal = passMultiTotal = failMultiTotal = unkToken = 0;
		failPercent = passPercent = 0;	
		changeCount = changePass = changeFail = 0;
		Arrays.fill(pTpass, 0);
		Arrays.fill(pTSpass, 0);
		Arrays.fill(pTfail, 0);
		startTime = endTime = 0;
		//dataPlane = null;
		val = val2 = 0;
		progressLast = progressCnt = 0;	
		containCounts = null;
		valueOut.clear();
	}
	
	VDataPlane getDataPlane() {
		return this.dataPlane;
	}
	
	/**
	 * turn on before start to record all counts for correct or contains
	 */
	void turnOnRecordCorrect() {
		containCounts = new ArrayList<>();
	}

	/**
	 * get for each token in test set correct count
	 * @return
	 */
	List<containInfo> getContains() {
		return containCounts;
	}
	
	/**
	 * get the containsInfo for a position in the results
	 * @param position from flattend eval data
	 * @return
	 */
	containInfo getContains(int position) {
		return containCounts.get(position);
	}
	
	/**
	 * Add contains information for a position
	 * @param position
	 * @param correctValue
	 * @param vpList
	 * @param win
	 * @param vid
	 */
	void addResponseContains(int position, long correctValue, List<ValProb> vpList, boolean win, long vid) {
		// need space
		while (containCounts.size() < (position+1)) containCounts.add(new containInfo());
		
		containInfo c = containCounts.get(position);
		c.win = win;
		c.vid = vid;
		c.vpSize = 0;
		if (vpList != null && vpList.size() > 0) {
			c.vpSize = vpList.size();
			c.wprobability = vpList.get(0).probability;
			if (win) {
				// correct answer
				c.count = vpList.get(0).count;
				c.probability = vpList.get(0).probability;
			} else {
				ValProb crtVp = ValProb.find(vpList, correctValue);
				if (crtVp != null) {
					// not correct, contains correct
					c.count = crtVp.count;
					c.probability = crtVp.probability;
				} 
			}
		} 
	}
	
	// add respnose info
	void addResponseDR(DsRetainVal drv, long predictValue, PredictionType pType, boolean noRsv) {
		boolean pass = (Arrays.binarySearch(drv.cvalue, predictValue) >= 0);	
		addResponse(drv.cvalue, predictValue, pass, pType, null, noRsv);
		if (drv.dvalue != predictValue) {
			changeCount++;
			if (pass) {
				changePass++;
				if (this.dsrToPass != null) dsrToPass.add(drv);
			} else if (drv.dvalue == drv.cvalue[0]) {
				changeFail++;
				if (this.dsrToFail != null) dsrToFail.add(drv);
			} 
			if (this.dsrToChange != null) {
				dsrToChange.add(drv);			
				dsrToChangeVal.add(predictValue);			
			}
		}
	}

	void addResponse(Long [] correctValue, long predictValue, boolean match, PredictionType pType, List<ValProb> vpList) {
		addResponse(correctValue, predictValue, match, pType, vpList, false);
	}
	void addResponse(Long [] correctValue, long predictValue, boolean match, PredictionType pType, List<ValProb> vpList, boolean noRsv) {
		this.total++;
		if (match) {
			this.passTotal++;
			if (pType != null) this.pTpass[pType.ordinal()]++;
			if (vpList != null) {
				if (vpList.size() == 1) {			
					passSingleTotal++;
					if (pType != null) this.pTSpass[pType.ordinal()]++;
				} else {
					passMultiTotal++;
				}
			}
		} else {
			this.failTotal++;
			if (pType != null) this.pTfail[pType.ordinal()]++;
			
			if (vpList != null) {
				if (vpList.size() == 1) failSingleTotal++;	
				else failMultiTotal++;
			} 
		}
		// per value
		if (!noRsv) {
			if (valueStats == null) valueStats = new HashMap<>();
			long valu = (long)VegML.emptyVect;
			if (correctValue != null) valu = correctValue[0];
			// for each correctValue?
			ResultValueStat rvs = valueStats.get(valu);
			if (rvs == null) {
				rvs = new ResultValueStat();
				rvs.value = valu;
				valueStats.put(valu, rvs);
			}	
			if (match) {
				rvs.pass++;
				if (pType != null) rvs.pTpass[pType.ordinal()]++;
			} else {
				rvs.fail++;
				if (pType != null) rvs.pTfail[pType.ordinal()]++;
				ResultValueStat frvs = valueStats.get(predictValue);
				if (frvs == null) {
					frvs = new ResultValueStat();
					frvs.value = predictValue;
					valueStats.put(predictValue, frvs);
				}
				frvs.pfail++;
				frvs.pTpfail[pType.ordinal()]++;	
			}
		}
	}
	
	/**
	 * Add non-evaluated response
	 * Add valueout and contains if collection contains info
	 * nothing else
	 * @param position
	 * @param correctValue
	 */
	void addResponseNoEval(int position, long correctValue) {
		if (valueOut != null) this.valueOut.add((long)correctValue);
		if (containCounts != null) {
			addResponseContains(position, correctValue, null,false, 0);
		}
	}
	
	/**
	 * get the stats for all values sorted by FScores 
	 * @return
	 */
	public List<ResultValueStat> getValueFscoreSorted() {
		if (valueStats == null) return null;
		List<ResultValueStat> rl = new ArrayList<>(valueStats.values());
		Collections.sort(rl, new Comparator<ResultValueStat>() {
	        @Override
	        public int compare(ResultValueStat lvp, ResultValueStat rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
	        	if (lvp.fscore < rvp.fscore) return 1;
	        	if (lvp.fscore > rvp.fscore) return -1;
	        	if (lvp.pass < rvp.pass) return 1;
	        	if (lvp.pass > rvp.pass) return -1;
	        	if (lvp.fail > rvp.fail) return 1;
	        	if (lvp.fail < rvp.fail) return -1;
	        	if (lvp.pfail > rvp.pfail) return 1;
	        	if (lvp.pfail < rvp.pfail) return -1;
	        	return 0;  
	        }
	    });
		return rl;
	}
	
	/**
	 * get the stats for all values reverse sorted by FScores 
	 * @return
	 */
	public List<ResultValueStat> getValueFscoreSortedReverse() {
		List<ResultValueStat> rl = new ArrayList<>(valueStats.values());
		Collections.sort(rl, new Comparator<ResultValueStat>() {
	        @Override
	        public int compare(ResultValueStat lvp, ResultValueStat rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
	        	if (lvp.fscore > rvp.fscore) return 1;
	        	if (lvp.fscore < rvp.fscore) return -1;
	        	if (lvp.pass > rvp.pass) return 1;
	        	if (lvp.pass < rvp.pass) return -1;
	        	if (lvp.fail > rvp.fail) return 1;
	        	if (lvp.fail < rvp.fail) return -1;
	        	if (lvp.pfail > rvp.pfail) return 1;
	        	if (lvp.pfail < rvp.pfail) return -1;
	        	return 0;  
	        }
	    });
		return rl;
	}
	
	/**
	 * get the stats for all values reverse sorted by total pass
	 * @return
	 */
	public List<ResultValueStat> getValueStatsSortedTotalR() {
		List<ResultValueStat> rl = new ArrayList<>(valueStats.values());
		Collections.sort(rl, new Comparator<ResultValueStat>() {
	        @Override
	        public int compare(ResultValueStat lvp, ResultValueStat rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
	        	int ltot = lvp.pass + lvp.fail;
	        	int rtot = rvp.pass + rvp.fail;
	        	if (ltot > rtot) return 1;
	        	if (ltot < rtot) return -1;
	        	if (lvp.pass > rvp.pass) return 1;
	        	if (lvp.pass < rvp.pass) return -1;
	        	if (lvp.fail > rvp.fail) return 1;
	        	if (lvp.fail < rvp.fail) return -1;
	        	if (lvp.pfail > rvp.pfail) return 1;
	        	if (lvp.pfail < rvp.pfail) return -1;
	        	return 0;  
	        }
	    });
		return rl;
	}
	
	/**
	 * get the stats for all values 
	 * @return
	 */
	public HashMap<Long, ResultValueStat> getValueStats() {
		return valueStats;
	}
	
	/**
	 * print stats for all values
	 * dataplan must be set for this to work
	 */
	public void printValueStats() {
		class Sls {
			String s;
			double d;
		}
		// needs dataplan to work 
		if (valueStats != null && dataPlane != null) {
			System.out.println(" RESULT["+dataPlane.getDimensionTag()+"/"+dataPlane.getTag()+"] Values[" + valueStats.keySet().size()+"] of ["+dataPlane.getCfgDataWidth()+"]");
			List<Sls> sl = new ArrayList<>();
			for (Long v:valueStats.keySet()) {
				ResultValueStat rvs = valueStats.get(v);
				
			    int tot = rvs.pass + rvs.fail;
			    String s = dataPlane.getString(v);
			    if (s == null) s = "<"+v+">";
			    s = String.format("%-" + 13 + "s", s);  
			    
			    String p = String.format("%6s", String.format("%.2f", rvs.passPercent));  		    
			    String gp = String.format("%6s", String.format("%.2f", rvs.passGuessPercent));  
			    
			    Sls xs = new Sls();
			    xs.d = rvs.fscore;
				xs.s = "["+s+"]["+p+"%]["+gp+"%]["+String.format("%.3f", rvs.fscore)+"] "
						+ " pass["+String.format("%6d", rvs.pass)+"]"
						+ " fail["+String.format("%6d", rvs.fail)+"]"
						+ " pfail["+String.format("%6d", rvs.pfail)+"]"
						+ " total["+String.format("%6d", tot)+"]"
						+ "   pass["+rvs.pTpass[0]+"/"+rvs.pTpass[1]+"/"+rvs.pTpass[2]+"/"+rvs.pTpass[3]+"/"+rvs.pTpass[4]+"/"+rvs.pTpass[5]+"]"
						+ " pfail["+rvs.pTpfail[0]+"/"+rvs.pTpfail[1]+"/"+rvs.pTpfail[2]+"/"+rvs.pTpfail[3]+"/"+rvs.pTpfail[4]+"/"+rvs.pTpfail[5]+"]";

				sl.add(xs);
			}
			// sort
			Collections.sort(sl, new Comparator<Sls>() {
		        @Override
		        public int compare(Sls lvp, Sls rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
		        	if (lvp.d < rvp.d) return 1;
		        	if (lvp.d > rvp.d) return -1;
		        	return lvp.s.compareToIgnoreCase(rvp.s);   
		        }
		    });
			for (int i=0;i<sl.size();i++) {
				Sls xs = sl.get(i);
				System.out.println("    "+String.format("%3d", i)+") " +xs.s);
			}
		} else if (valueStats != null && dataPlane == null) {
			System.out.println(" NOTE: dataplane needed to show values");

		}
		// info per predict type
		for (int i=0;i<this.pTpass.length;i++) {
			int tot = this.pTpass[i] + this.pTfail[i];
			double per = (((double)this.pTpass[i] / (double)tot)*(double)100);
			if (tot == 0) per = 100;
		    String p = String.format("%6s", String.format("%.2f", per));  
		    PredictionType pt = VegML.getPredictionTypeEnum(i);
		    String px = String.format("%15s", ""+pt);  
			System.out.println("  "+px+"["+p+"%]  pass["+String.format("%8d", this.pTpass[i]) + "] fail["+String.format("%8d", this.pTfail[i])+ "] total[" + String.format("%8d", tot)+"]");	
		}
		System.out.println(" RESULT_SET["+String.format("%.2f", passPercent)+"%] fail["+failTotal + " / "+passTotal+ " of " + total+"]  =>  T["+getDurration()+"]");	
	}
	
	/**
	 * start the timing for this resultset
	 */
	public void start() {
		startTime = System.currentTimeMillis();
	}
	
	/**
	 * end the timing for this resultset
	 */
	public void end() {
		if (this.total > 0) {
			this.failPercent = (((double)this.failTotal / (double)this.total)*(double)100);
			// not good.. but is hack for ?
			this.passPercent = 100.00 - this.failPercent;
			//this.passPercent = (((double)this.passTotal / (double)this.total)*(double)100);
			
			if (valueStats != null) {
				for (Long v:valueStats.keySet()) {
					ResultValueStat rvs = valueStats.get(v);
					if ((rvs.pass + rvs.fail) > 0) {
					    rvs.passPercent = (((double)rvs.pass / (double)(rvs.fail+rvs.pass))*(double)100);		    
					}
					if ((rvs.pass + rvs.pfail) > 0) {
					    rvs.passGuessPercent = (((double)rvs.pass / (double)(rvs.pfail+rvs.pass))*(double)100);
					}
					if (rvs.pass == 0 && rvs.fail == 0 && rvs.pfail == 0) rvs.fscore = 0;
					else {	 
					    double true_positive = rvs.pass;
					    double false_positive = rvs.pfail;
					    double false_negative = rvs.fail;
						//if (true_positive == 0 && (false_positive != 0 || false_negative != 0)) true_positive = 0.5/(false_positive+false_negative); // want these to count 
					    rvs.fscore = true_positive / (true_positive + ((false_negative + false_positive)/2));
					}
				}
			}
		}
		endTime = System.currentTimeMillis();
		this.clearLinks(); // drop references
	}
	
	/**
	 * get the duration in milliseconds
	 * @return duration in milliseconds
	 */
	public long durration() {
		return endTime - startTime;
	}
	
	/**
	 * get a string representation of the duration
	 * @return string of duration
	 */
	public String getDurration() {
		long msec = durration();
		// total
		int sec = ((int)msec) / 1000;
		int min = sec / 60;	
		sec = sec % 60;
		msec = msec % 1000;
		return min + ":"+String.format("%02d", sec)+"."+String.format("%03d", msec);
	}

	/**
	 * merge in another result sets values
	 * @param ts result set to merge into this
	 */
	void add(VResultSet ts) {
		this.total += ts.total;
		this.failTotal += ts.failTotal;
		this.passTotal += ts.passTotal;
		this.passSingleTotal += ts.passSingleTotal;
		this.failSingleTotal += ts.failSingleTotal;
		this.passMultiTotal += ts.passMultiTotal;
		this.failMultiTotal += ts.failMultiTotal;
		this.unkToken += ts.unkToken;
		
		this.changeCount += ts.changeCount;
		this.changePass += ts.changePass;
		this.changeFail += ts.changeFail;
		
		for (int i=0;i<this.pTpass.length;i++) this.pTpass[i] += ts.pTpass[i];
		for (int i=0;i<this.pTSpass.length;i++) this.pTSpass[i] += ts.pTSpass[i];
		for (int i=0;i<this.pTfail.length;i++) this.pTfail[i] += ts.pTfail[i];
		
		if (ts.valueStats != null) {
			if (valueStats == null) valueStats = ts.valueStats;
			else {
				for (Long v:ts.valueStats.keySet()) {
					ResultValueStat rvs = ts.valueStats.get(v);
					ResultValueStat trvs = valueStats.get(v);
					if (trvs == null) valueStats.put(rvs.value, rvs);
					else {		
						trvs.pass += rvs.pass;
						trvs.pfail += rvs.pfail;
						trvs.fail += rvs.fail;
						for (int i=0;i<trvs.pTpass.length;i++) trvs.pTpass[i] += rvs.pTpass[i];
						for (int i=0;i<trvs.pTpfail.length;i++) trvs.pTpfail[i] += rvs.pTpfail[i];
						for (int i=0;i<trvs.pTfail.length;i++) trvs.pTfail[i] += rvs.pTfail[i];
					}
				}
			}
		}
		if (this.dsrListSet != null) {
			this.dsrListSet.addAll(ts.dsrListSet);
		}
		if (this.dsrToPass != null) {
			this.dsrToPass.addAll(ts.dsrToPass);
			this.dsrToFail.addAll(ts.dsrToFail);
			this.dsrToChange.addAll(ts.dsrToChange);
			this.dsrToChangeVal.addAll(ts.dsrToChangeVal);
		}
		// if enabled record every response with correct/contains count from answer
		if (containCounts != null) {
			containCounts.addAll(ts.containCounts);
		}
		this.uvalCount += ts.uvalCount;
		
		this.end();
	}
	
	/**
	 * set the dataplnae for this resultset
	 * @param dp
	 */
	void setDataPlane(VDataPlane dp) {
		this.dataPlane = dp;
	}

	//////////////////////////////////
	// Retained resolution set
	
	/**
	 * Get the retained dataset if it was generated
	 * @return
	 */
	public List<List<DsRetainVal>> getRetainedResolutionSet() {
		return dsrListSet;
	}
	
	/**
	 * Set the retained dataset on this result set
	 * @param dsrl
	 */
	public void setRetainedResolutionSet(List<List<DsRetainVal>> dsrl) {
		this.dsrListSet = dsrl;
	}
	
	public List<DsRetainVal> getRDChangeToPass() {
		return dsrToPass;
	}
	public List<DsRetainVal> getRDChangeToFail() {
		return dsrToFail;
	}
	public List<DsRetainVal> getRDChanges() {
		return dsrToChange;
	}
	public List<Long> getRDChangesVals() {
		return dsrToChangeVal;
	}
	
	/**
	 * set this result set to build a retained data set
	 */
	public void setRetaindDsr() {
		this.dsrToPass = new ArrayList<>();
		this.dsrToFail = new ArrayList<>();
		this.dsrToChange = new ArrayList<>();
		this.dsrToChangeVal = new ArrayList<>();
	}

	
	//////////////////////////////////
	// results

	public List<List<List<ValProb>>> getResultsVpList() {
		return responseVpOut;
	}
	public List<List<Long>> getResultsList() {
		return responseOut ;
	}
	// generate set of results with position in VpList results, null if none
	public List<List<String>> getResultsList(VDataPlane dp, int position) {
		if (responseVpOut == null || responseVpOut.size() < 1) return null;
		List<List<String>> rsl = new ArrayList<>();
		
		for (int i=0;i<responseVpOut.size();i++) { // for each set/file
			List<String> srsl = new ArrayList<>();
			rsl.add(srsl);
			List<List<ValProb>> rxl = responseVpOut.get(i);
			for (int x=0;x<rxl.size();x++) { // for each token
				List<ValProb> vpl = rxl.get(x); // for token
				
				if (vpl != null && vpl.size() >= (position+1)) {
					srsl.add(dp.getString(vpl.get(position).value));
				} else {
					srsl.add(null);
				}
			}
		}
		return rsl;
	}

	//////////////////////////////////
	// SINGLE RESPONSE
	public int getSingleTotal() {
		return this.passSingleTotal+this.failSingleTotal;
	}
	public double getPassSinglePercent() {
		if (getSingleTotal() <= 0) return 0;
		return (((double)passSingleTotal / (double)(getSingleTotal()))*(double)100);
	}
	public int getMultiTotal() {
		return this.passMultiTotal+this.failMultiTotal;
	}
	public double getPassMultiPercent() {
		if (getMultiTotal() <= 0) return 0;
		return (((double)passMultiTotal / (double)(getMultiTotal()))*(double)100);
	}
	
	
	//////////////////////////////////
	// per prediction type stats
	// get mixed Null for full
	public int getTotal(PredictionType pType) {
		return getFailTotal(pType)+getPassTotal(pType);
	}
	public double getPassPercent(PredictionType pType) {
		int p = getPassTotal(pType);
		int f = getFailTotal(pType);
		if ((p + f) <= 0) return 0;
		return (((double)p / (double)(f+p))*(double)100);		    
	}
	// get mixed Null for full
	public int getFailTotal(PredictionType pType) {
		if (pType == null) return this.failTotal;
		if (pType.ordinal() < pTfail.length) {		
			return pTfail[pType.ordinal()];					
		} else if (pType == PredictionType.NotUnknown) {
			return failTotal - (pTfail[PredictionType.PredictUnknown.ordinal()] + pTfail[PredictionType.Default.ordinal()]);				
		} else if (pType == PredictionType.AnyRecall) {
			return pTfail[PredictionType.Recall.ordinal()] + pTfail[PredictionType.RecallPredict.ordinal()];				
		} else if (pType == PredictionType.AnyPredict) {
			return pTfail[PredictionType.Predict.ordinal()] + pTfail[PredictionType.PredictRelate.ordinal()];				
		} else if (pType == PredictionType.AnyUnknown) {
			return (pTfail[PredictionType.PredictUnknown.ordinal()] + pTfail[PredictionType.Default.ordinal()]);				
		}
		
		return this.failTotal;
	}
	// get mixed Null for full
	public int getPassTotal(PredictionType pType) {
		if (pType == null) return this.passTotal;
	
		if (pType.ordinal() < pTpass.length) {		
			return pTpass[pType.ordinal()];					
		} else if (pType == PredictionType.NotUnknown) {
			return passTotal - (pTpass[PredictionType.PredictUnknown.ordinal()] + pTpass[PredictionType.Default.ordinal()]);				
		} else if (pType == PredictionType.AnyRecall) {
			return pTpass[PredictionType.Recall.ordinal()] + pTpass[PredictionType.RecallPredict.ordinal()];				
		} else if (pType == PredictionType.AnyPredict) {
			return pTpass[PredictionType.Predict.ordinal()] + pTpass[PredictionType.PredictRelate.ordinal()];				
		} else if (pType == PredictionType.AnyUnknown) {
			return (pTpass[PredictionType.PredictUnknown.ordinal()] + pTpass[PredictionType.Default.ordinal()]);				
		}
		return this.passTotal;
	}
	// get mixed Null for full
	public int getPassSingleTotal(PredictionType pType) {
		if (pType == null) return this.passSingleTotal;
	
		if (pType.ordinal() < pTSpass.length) {		
			return pTSpass[pType.ordinal()];					
		} else if (pType == PredictionType.NotUnknown) {
			return passSingleTotal - (pTSpass[PredictionType.PredictUnknown.ordinal()] + pTSpass[PredictionType.Default.ordinal()]);				
		} else if (pType == PredictionType.AnyRecall) {
			return pTSpass[PredictionType.Recall.ordinal()] + pTSpass[PredictionType.RecallPredict.ordinal()];				
		} else if (pType == PredictionType.AnyPredict) {
			return pTSpass[PredictionType.Predict.ordinal()] + pTSpass[PredictionType.PredictRelate.ordinal()];				
		} else if (pType == PredictionType.AnyUnknown) {
			return (pTSpass[PredictionType.PredictUnknown.ordinal()] + pTSpass[PredictionType.Default.ordinal()]);				
		}
		return this.passSingleTotal;
	}
	
	//////////////////////////////////
	// per prediction type stats
	public double getFScore(PredictionType pType, long value) {
		if (valueStats == null) return 0;

		double true_positive = this.getPassTotal(pType, value);
		double false_positive = this.getFailTotal(pType, value);
		double false_negative = this.getPFailTotal(pType, value);

		if (true_positive == 0 && false_positive == 0 && false_negative == 0) return 0;	
		//if (true_positive == 0 && (false_positive != 0 || false_negative != 0)) true_positive = 0.5/(false_positive+false_negative); // want these to count 
		return true_positive / (true_positive + ((false_negative + false_positive)/2));
	}
	public double getPassPercent(PredictionType pType, long value) {
		if (valueStats == null) return 0;
		double true_positive = this.getPassTotal(pType, value);
		double false_negative =  this.getFailTotal(pType, value);
		if ((true_positive + false_negative) <= 0) return 0;
		return (((double)true_positive / (double)(false_negative+true_positive))*(double)100);		    
	}
	public double getPassGuessPercent(PredictionType pType, long value) {
		if (valueStats == null) return 0;

		double true_positive = this.getPassTotal(pType, value);
		double false_positive = this.getPFailTotal(pType, value);
		if ((true_positive + false_positive) <= 0) return 0;
		return (((double)true_positive / (double)(false_positive+true_positive))*(double)100);		    
	}
	public int getTotal(PredictionType pType, long value) {
		if (valueStats == null) return 0;
		ResultValueStat rvs = valueStats.get(value);
		if (rvs == null) return 0;
		
		if (pType == null) return rvs.pass + rvs.fail;
		if (pType == PredictionType.NotUnknown) {
			return (rvs.pass + rvs.fail) - (rvs.pTpass[PredictionType.PredictUnknown.ordinal()] + rvs.pTpass[PredictionType.Default.ordinal()]
					+rvs.pTfail[PredictionType.PredictUnknown.ordinal()] + rvs.pTfail[PredictionType.Default.ordinal()]);				
		} else if (pType == PredictionType.AnyRecall) {
			return rvs.pTpass[PredictionType.Recall.ordinal()] + rvs.pTpass[PredictionType.RecallPredict.ordinal()]
					+rvs.pTfail[PredictionType.Recall.ordinal()] + rvs.pTfail[PredictionType.RecallPredict.ordinal()];
		} else if (pType == PredictionType.AnyPredict) {
			return rvs.pTpass[PredictionType.Predict.ordinal()] + rvs.pTpass[PredictionType.PredictRelate.ordinal()]
					+rvs.pTfail[PredictionType.Predict.ordinal()] + rvs.pTfail[PredictionType.PredictRelate.ordinal()];				
		} else if (pType == PredictionType.AnyUnknown) {
			return (rvs.pTpass[PredictionType.PredictUnknown.ordinal()] + rvs.pTpass[PredictionType.Default.ordinal()]
					+rvs.pTfail[PredictionType.PredictUnknown.ordinal()] + rvs.pTfail[PredictionType.Default.ordinal()]);				
		}
		return rvs.pTpass[pType.ordinal()] + rvs.pTfail[pType.ordinal()];
	}
	public int getPassTotal(PredictionType pType, long value) {
		if (valueStats == null) return 0;
		ResultValueStat rvs = valueStats.get(value);
		if (rvs == null) return 0;
		if (pType == null) return rvs.pass;
		if (pType == PredictionType.NotUnknown) {
			return rvs.pass - (rvs.pTpass[PredictionType.PredictUnknown.ordinal()] + rvs.pTpass[PredictionType.Default.ordinal()]);				
		} else if (pType == PredictionType.AnyRecall) {
			return rvs.pTpass[PredictionType.Recall.ordinal()] + rvs.pTpass[PredictionType.RecallPredict.ordinal()];				
		} else if (pType == PredictionType.AnyPredict) {
			return rvs.pTpass[PredictionType.Predict.ordinal()] + rvs.pTpass[PredictionType.PredictRelate.ordinal()];				
		} else if (pType == PredictionType.AnyUnknown) {
			return (rvs.pTpass[PredictionType.PredictUnknown.ordinal()] + rvs.pTpass[PredictionType.Default.ordinal()]);				
		}
		return rvs.pTpass[pType.ordinal()];
	}
	public int getFailTotal(PredictionType pType, long value) {
		if (valueStats == null) return 0;
		ResultValueStat rvs = valueStats.get(value);
		if (rvs == null) return 0;
		if (pType == null) return rvs.fail;
		if (pType == PredictionType.NotUnknown) {
			return rvs.fail - (rvs.pTfail[PredictionType.PredictUnknown.ordinal()] + rvs.pTfail[PredictionType.Default.ordinal()]);				
		} else if (pType == PredictionType.AnyRecall) {
			return rvs.pTfail[PredictionType.Recall.ordinal()] + rvs.pTfail[PredictionType.RecallPredict.ordinal()];				
		} else if (pType == PredictionType.AnyPredict) {
			return rvs.pTfail[PredictionType.Predict.ordinal()] + rvs.pTfail[PredictionType.PredictRelate.ordinal()];				
		} else if (pType == PredictionType.AnyUnknown) {
			return (rvs.pTfail[PredictionType.PredictUnknown.ordinal()] + rvs.pTfail[PredictionType.Default.ordinal()]);				
		}
		return rvs.pTfail[pType.ordinal()];
	}
	public int getPFailTotal(PredictionType pType, long value) {
		if (valueStats == null) return 0;
		ResultValueStat rvs = valueStats.get(value);
		if (rvs == null) return 0;
		if (pType == null) return rvs.pfail;
		if (pType == PredictionType.NotUnknown) {
			return rvs.pfail - (rvs.pTpfail[PredictionType.PredictUnknown.ordinal()] + rvs.pTpfail[PredictionType.Default.ordinal()]);				
		} else if (pType == PredictionType.AnyRecall) {
			return rvs.pTpfail[PredictionType.Recall.ordinal()] + rvs.pTpfail[PredictionType.RecallPredict.ordinal()];				
		} else if (pType == PredictionType.AnyPredict) {
			return rvs.pTpfail[PredictionType.Predict.ordinal()] + rvs.pTpfail[PredictionType.PredictRelate.ordinal()];				
		} else if (pType == PredictionType.AnyUnknown) {
			return (rvs.pTpfail[PredictionType.PredictUnknown.ordinal()] + rvs.pTpfail[PredictionType.Default.ordinal()]);				
		}
		return rvs.pTpfail[pType.ordinal()];
	}
	
	
	//
	// Sort list of result sets by ID 
	// .val -> has orignial position
	//
	static public List<VResultSet> sortList(List<VResultSet> rsl) {	
		for (int i=0;i<rsl.size();i++) {
			rsl.get(i).val = i;
		}
		// sort
		Collections.sort(rsl, new Comparator<VResultSet>() {
	        @Override
	        public int compare(VResultSet lvp, VResultSet rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	        	
	        	if (lvp.passTotal < rvp.passTotal) return 1;
	        	if (lvp.passTotal > rvp.passTotal) return -1;
	        	if (lvp.failTotal > rvp.failTotal) return 1;
	        	if (lvp.failTotal < rvp.failTotal) return -1;
	        	int lu = lvp.getPassTotal(PredictionType.PredictUnknown);
	        	int ru = rvp.getPassTotal(PredictionType.PredictUnknown);
	        	if (lu > ru) return 1;
	        	if (lu < ru) return -1;
	        	return 0;  
	        }
	    });
		return rsl;
	}
	
	static public List<VResultSet> sortListR(List<VResultSet> rsl) {	
		for (int i=0;i<rsl.size();i++) {
			rsl.get(i).val = i;
		}
		// sort
		Collections.sort(rsl, new Comparator<VResultSet>() {
	        @Override
	        public int compare(VResultSet lvp, VResultSet rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	        	
	        	if (lvp.passTotal > rvp.passTotal) return 1;
	        	if (lvp.passTotal < rvp.passTotal) return -1;
	        	if (lvp.failTotal < rvp.failTotal) return 1;
	        	if (lvp.failTotal > rvp.failTotal) return -1;
	        	return 0;  
	        }
	    });
		return rsl;
	}
	static public List<VResultSet> sortList(PredictionType pType, List<VResultSet> rsl) {			 
		if (pType == null || pType == PredictionType.All) return sortList(rsl);
		for (int i=0;i<rsl.size();i++) {
			rsl.get(i).val = i;
		}
		// sort
		Collections.sort(rsl, new Comparator<VResultSet>() {
	        @Override
	        public int compare(VResultSet lvp, VResultSet rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	        	
	        	int lpass = lvp.getPassTotal(pType);					
	        	int rpass = rvp.getPassTotal(pType);						        	
	        	if (lpass < rpass) return 1;
	        	if (lpass > rpass) return -1;
	        	int lfail = lvp.getFailTotal(pType);					
	        	int rfail = rvp.getFailTotal(pType);					
	        	if (lfail > rfail) return 1;
	        	if (lfail < rfail) return -1;
	        	int lu = lvp.getPassTotal(PredictionType.PredictUnknown);
	        	int ru = rvp.getPassTotal(PredictionType.PredictUnknown);
	        	if (lu > ru) return 1;
	        	if (lu < ru) return -1;
	        	return 0;  
	        }
	    });
		return rsl;
	}
	static public List<VResultSet> sortList(PredictionType pType, List<VResultSet> rsl, List<Integer> firstSet) {		
		if (firstSet == null) return sortList(pType, rsl);
		
		if (pType == null || pType == PredictionType.All) return sortList(rsl);
		for (int i=0;i<rsl.size();i++) {
			rsl.get(i).val = i;
		}
		// sort
		Collections.sort(rsl, new Comparator<VResultSet>() {
	        @Override
	        public int compare(VResultSet lvp, VResultSet rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	        	
	        	boolean lfs = firstSet.contains((int)lvp.val2);
	        	boolean rfs = firstSet.contains((int)rvp.val2);
	        	if (!lfs && rfs) return 1;
	        	if (lfs && !rfs) return -1;
	        	
	        	int lpass = lvp.getPassTotal(pType);					
	        	int rpass = rvp.getPassTotal(pType);						        	
	        	if (lpass < rpass) return 1;
	        	if (lpass > rpass) return -1;
	        	int lfail = lvp.getFailTotal(pType);					
	        	int rfail = rvp.getFailTotal(pType);					
	        	if (lfail > rfail) return 1;
	        	if (lfail < rfail) return -1;
	        	int lu = lvp.getPassTotal(PredictionType.PredictUnknown);
	        	int ru = rvp.getPassTotal(PredictionType.PredictUnknown);
	        	if (lu > ru) return 1;
	        	if (lu < ru) return -1;
	        	return 0;  
	        }
	    });
		return rsl;
	}
	
	


	//////////////////////////////////
	// Diff
	// FIXME diff result sets objects

	
	
	//////////////////////////////////
	//
    // Diff data results
    // return position of differences: dsList position / data position
	//
	public static int diffDataSetsResults(
			List<List<String>> dsList, List<List<String>> dsValList,
			List<List<String>> dsList2, List<List<String>> dsValList2,
			boolean print, HashMap<Integer, List<Integer>> diffMap) {
		return diffDataSetsResults(dsList, dsValList, dsList2, dsValList2, null, print, diffMap);
	}
	public static int diffDataSetsResults(
				List<List<String>> dsList, List<List<String>> dsValList,
				List<List<String>> dsList2, List<List<String>> dsValList2,
				List<List<String>> correctValList,
				boolean print, HashMap<Integer, List<Integer>> diffMap) {
		int cnt = 0;
		int posCnt = 0, negCnt = 0, bothCnt = 0, cnt1 = 0, cnt2 = 0;
		
		Set<String> uniq = new HashSet<>();
		Set<String> upos = new HashSet<>();
		Set<String> uneg = new HashSet<>();
		
		for (int i=0;i<dsList.size();i++) {
			List<String> list = dsList.get(i);
			List<String> vlist = dsValList.get(i);
			List<String> list2 = dsList2.get(i);
			List<String> vlist2 = dsValList2.get(i);			
			List<String> vcor = null;
			if (correctValList != null) vcor = correctValList.get(i);			

			
	        for (int x=0;x<list.size();x++) {
	        	String s = list.get(x);
	        	String sv = vlist.get(x);
	        	String s2 = list2.get(x);
	        	String sv2 = vlist2.get(x);
	        	String cv = null;
				if (vcor != null) cv = vcor.get(x);			

	        	if (!sv.equalsIgnoreCase(sv2)) {
	        		cnt++;
    				String val = "["+s+"]";
    				if (!s.equals(s2)) val += "["+s2+"]";
    				String mix = sv+"/"+sv2+"/"+val;
    				uniq.add(mix);
	        		if (print) {
        				String pos = String.format("%-13s", "["+i+"][@"+x+"]");
        			       				
	        			if (cv != null) {	        				
	        				if (sv.equalsIgnoreCase(cv)) {
		        				System.out.println(" DIFF"+pos+" -- cv["+cv+"]  ["+sv+"] != ["+sv2+"]  <=  "+val);	        					
		        				negCnt++;
		        				cnt1++;
		        				uneg.add(mix);
	        				} else if (sv2.equalsIgnoreCase(cv)) {
		        				System.out.println(" DIFF"+pos+" ++ cv["+cv+"]  ["+sv+"] != ["+sv2+"]  <=  "+val);	        					
		        				posCnt++;
		        				cnt2++;
		        				upos.add(mix);
	        				} else {
		        				System.out.println(" DIFF"+pos+" != cv["+cv+"]  ["+sv+"] != ["+sv2+"]  <=  "+val);	        					
		        				bothCnt++;
	        				}
	        			} else {
	        				System.out.println(" DIFF"+pos+" ["+sv+"] != ["+sv2+"]  <=  "+val);
	        			}
	        		}
	        		if (diffMap != null) {
		        		List<Integer> ml = diffMap.get(i);
		        		if (ml == null) {
		        			ml = new ArrayList<>();
		        			diffMap.put(i, ml);
		        		}
		        		ml.add(x);
		        	}
	        	} else if (cv != null && sv.equalsIgnoreCase(cv)) {
    				cnt1++;
    				cnt2++;      		
	        	}
	        } 
		}	
		if (print) {
			if (correctValList == null) {
				System.out.println("Total Different["+cnt+"] unique["+uniq.size()+"]");
			} else {
				if (cnt1 == cnt2) {
					System.out.println("Total Different["+cnt+"] SAME pass["+cnt1+"]");		
				} else if (cnt1 > cnt2) {
					int pdiff = cnt1 - cnt2;
					System.out.println("Total Different["+cnt+"] WORSE["+pdiff+"] d1["+cnt1+"] d2["+cnt2+"] => new pass["+posCnt+"]miss["+negCnt+"]missBoth["+bothCnt+"]  unique["+uniq.size()+"]pass["+upos.size()+"]miss["+uneg.size()+"]");			
				} else {
					int pdiff = cnt2 - cnt1;
					System.out.println("Total Different["+cnt+"] BETTER["+pdiff+"] d1["+cnt1+"] d2["+cnt2+"] => new pass["+posCnt+"]miss["+negCnt+"]missBoth["+bothCnt+"]  unique["+uniq.size()+"]pass["+upos.size()+"]miss["+uneg.size()+"]");		
				}
			}
		
		}
		return cnt;		
	}
	
	// drop references
	void clearLinks() {
		dataPlane = null;
		dsd = null;
	}
	// clean; retain contains info
	void clearForRetain() {
		clearLinks();
		valueStats = null;
		valData = null;
		valueOut = null;
		responseVpOut = null;
		responseOut = null;
		dsrListSet = null;
		dsrToPass = null;		// change to pass
		dsrToFail = null;		// change to fail
		dsrToChange = null; 	// all changes
		dsrToChangeVal = null;
		dsd = null;
	}
	// clean all data aside from high level stats
	void clearLinksAndMore() {
		clearForRetain();
		containCounts = null;
	}

}
