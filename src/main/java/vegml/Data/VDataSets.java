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


package vegml.Data;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class VDataSets implements java.io.Serializable, Comparable<VDataSets> {
	
	private static final long serialVersionUID = 2123092359900709218L;

	// formats for the dataset and datasets
	public enum DSFmt {
		N,		// none
		
		L,		// long
		S,		// string
		I,		// integer
		B,		// Boolean
		F,		// Float/double
		O,		// object
		V,		// vvect

		LD,		// long [] values
		SD,		// string [] values
		ID,		// integer [] values
		BD,		// Boolean [] values
		FD,		// Float/double [] values
		OD,		// object [] values
		VD,		// vvect [] values

		LL,		// list longs
		LS,		// list strings
		LI,		// list integers
		LB,		// list Boolean
		LF,		// list Float/double
		LO,		// list objects
		LV,		// list vvect
		
		LLD,	// list Long [] dep
		LSD,	// list String [] dep
		LID,	// list Integer [] dep
		LBD,	// list Boolean [] dep
		LFD,	// list Float/Double [] dep
		LOD,	// list objects [] dep
		LVD,	// list vvect [] dep
		
		LLL,	// list list longs
		LLS,	// list list strings
		LLI,	// list list integers
		LLB,	// list list Boolean
		LLF,	// list list Doubles
		LLO,	// list list objects
		LLV,	// list list vvect
		
		LLLD,	// list list Long dep
		LLSD,	// list list Strings dep
		LLID,	// list list Integer dep
		LLBD,	// list list Boolean dep
		LLFD,	// list list Doubles dep
		LLOD,	// list list object dep
		LLVD,	// list list vvect dep
		
		// multi-dimensional ? could use D ?
	}
	private static double tctl = 0.5; // tune control percent
	
	private VDataSetDescriptor dsd = null;
	private List<VDataSet> dsl = null;
	private int train;
	private int tune;
	private int test;
	private DSFmt dataFmt;
	private DSFmt valueFmt;
	private int minSetSize;
	private int maxSetSize;
	private boolean palindrome;	// should be just in datasetdefinition?
	
	// cached
	private transient VDataSets trainDs;
	private transient VDataSets tuneDs;
	private transient VDataSets testDs;
	private transient boolean vgen = false;
	private transient boolean comp = false;
	
	public VDataSets() {
		train = tune = test = 0;
		dataFmt = DSFmt.N;
		valueFmt = DSFmt.N;
		maxSetSize = minSetSize = -1;
		palindrome = false;
		vgen = false;
		clearCache();
	}
	
	/**
	 * create Datasets
	 * 
	 * @param dsd dataset descriptor
	 * @param dsl dataset list to start with
	 */		
	public VDataSets(VDataSetDescriptor dsd, List<VDataSet> dsl) {
		this();
		this.dsl = dsl;
		this.dsd = dsd;
	}
	
	/**
	 * create Datasets with a sinlge dataset and make complete
	 * 
	 * @param dsd dataset descriptor
	 * @param ds base dataset to start with
	 */			
	public VDataSets(VDataSet ds) {
		this();
		add(ds);
		this.complete();
	}
	
	private void clearCache() {
		trainDs = tuneDs = testDs = null;		
	}
	
	/**
	 * 
	 */
	public void freeResources() {
		clearCache();	
		if (dsl == null) return;
		vgen = false;
		for (VDataSet ds:dsl) ds.freeResources();
	}
	
	/**
	 * get the dataset data format
	 */	
	public DSFmt getFmtData() {
		return dataFmt;
	}
	public void setFmtData(DSFmt fmt) {
		dataFmt = fmt;
	}
	
	/**
	 * get the dataset value format
	 */	
	public DSFmt getFmtValue() {
		return valueFmt;
	}
	public void setFmtValue(DSFmt fmt) {
		valueFmt = fmt;
	}
	
	/**
	 * check data format
	 * 
	 * @return true if this is an xxxD data format
	 */	
	public boolean isFmtDataD() {
		if (dataFmt.ordinal() >= DSFmt.LLLD.ordinal()) return true;
		return false;
	}
	
	/**
	 * check data format
	 * 
	 * @return true if this is an valueId data format
	 */	
	public boolean isFmtDataV() {
		if (dataFmt == DSFmt.LLVD || dataFmt == DSFmt.LVD || dataFmt == DSFmt.LLV || dataFmt == DSFmt.LV || dataFmt == DSFmt.V) return true;
		return false;
	}
	
	/**
	 * check data format
	 * 
	 * @return true if this is an String data format
	 */	
	public boolean isFmtDataS() {
		if (dataFmt == DSFmt.LLSD || dataFmt == DSFmt.LSD || dataFmt == DSFmt.LLS || dataFmt == DSFmt.LS || dataFmt == DSFmt.S) return true;
		return false;
	}
	
	/**
	 * check value format is list
	 * 
	 * @return true if this is an valueId value format
	 */	
	public boolean isFmtValueList() {
		if (valueFmt.ordinal() >= DSFmt.LL.ordinal()) return true;
		return false;
	}
	
	/**
	 * check value format
	 * 
	 * @return true if this is an xxxD value format
	 */	
	public boolean isFmtValueD() {
		if (valueFmt.ordinal() >= DSFmt.LD.ordinal() && valueFmt.ordinal() <= DSFmt.VD.ordinal()) return true;
		if (valueFmt.ordinal() >= DSFmt.LLD.ordinal() && valueFmt.ordinal() <= DSFmt.LVD.ordinal()) return true;
		if (valueFmt.ordinal() >= DSFmt.LLLD.ordinal()) return true;
		return false;
	}
	
	/**
	 * check value format
	 * 
	 * @return true if this is an String data format
	 */	
	public boolean isFmtValueS() {
		if (valueFmt == DSFmt.LLSD || valueFmt == DSFmt.LSD || valueFmt == DSFmt.LLS || valueFmt == DSFmt.LS || valueFmt == DSFmt.S) return true;
		return false;
	}
	
	//
	// each set has the same value when inverted
	// this instruction allows each set will be trained in both directions
	// such as math, logic, OR/XOR/AND
	// example: D: 1 + 5 V: 6
	// Q: should this be part of the dataset definition?
	//
	public boolean isPalindromeSet() {
		return palindrome;
	}
	public void setPalindromeSet(boolean palindrome) {
		this.palindrome = palindrome;
	}

	/**
	 * Get the dataset definition
	 * 
	 * @return dataset definition
	 */
	public VDataSetDescriptor getDefinition() {
		return dsd;
	}
	
	/**
	 * Set the dataset definition
	 * 
	 * @param def dataset definition
	 */
	public void setDefinition(VDataSetDescriptor def) {
		dsd = def;
	}
	
	/**
	 * Get all datasets as a list
	 * NOTE: This is direct access to the VDataSets list
	 * 
	 * @return list of datasets
	 */
	public List<VDataSet> getAll() {
		return dsl;
	}
	
	/**
	 * Get dataset at position/index
	 * 
	 * @param idx index of dataset
	 * @return dataset
	 */
	public VDataSet get(int idx) {
		return dsl.get(idx);
	}
	
	/**
	 * Find a dataset by name
	 * 
	 * @param name name or id of dataset
	 * @return dataset or null
	 */
	public VDataSet find(String name) {
		if (dsl == null || name == null) return null;
		for (VDataSet ds:dsl) {
			if (ds.getName() != null && ds.getName().equals(name)) return ds;
		}
		return null;
	}
	
	/**
	 * Get size of datasets
	 * 
	 * @return number of dataset
	 */
	public int size() {
		if (dsl == null) return 0;
		return dsl.size();
	}
	
	/**
	 * Get count of tokens in the datasets
	 * 
	 * @return count of tokens in the datasets
	 */
	public int getTokenCount() {
		if (dsl == null) return 0;
		int cnt = 0;
		for (VDataSet ds:dsl) cnt += ds.size();
		return cnt;
	}	
	
	/**
	 * Get count of ranges in the datasets
	 * 
	 * @return count of ranges in the datasets
	 */
	public int getRangeCount() {
		if (dsl == null) return 0;
		int cnt = 0;
		for (VDataSet ds:dsl) cnt += ds.getRangeCount();
		return cnt;
	}

	/**
	 *  get count tokens in smallest set
	 *  
	 * @return smallest dataset size
	 */
	public int getMinSetSize() {
		return minSetSize;
	}
	
	/**
	 *  get count tokens in largest set
	 *  
	 * @return largest dataset size
	 */
	public int getMaxSetSize() {
		return maxSetSize;
	}

	/**
	 * Determine if all datasets are the same size
	 * @return true if all the same size
	 */
	public boolean isFixedSetSize() {
		if (size() < 2) return false;
		return (minSetSize == maxSetSize);
	}
	
	/**
	 * Set this DataSet as complete, should be done after construction
	 * This is automatically done on save, and already comp
	 */
	public void complete() {
		if (dsl == null || this.comp) return;
		for (VDataSet ds:dsl) {
			ds.complete();
			if (this.minSetSize == -1 || this.minSetSize > ds.size()) this.minSetSize = ds.size();
			if (this.maxSetSize == -1 || this.maxSetSize < ds.size()) this.maxSetSize = ds.size();
		}
		this.vgen = false;
		this.clearCache();
		
		VDataSet ds = this.get(0);
		this.dataFmt = DSFmt.valueOf(DSFmt.class, "L"+ds.getFmtData().name());
		if (ds.getFmtValue() == DSFmt.N) this.valueFmt = DSFmt.N; // no data
		else this.valueFmt = DSFmt.valueOf(DSFmt.class, "L"+ds.getFmtValue().name());
		this.comp = true;
	}
	
	/**
	 * copy all data (and lists completely)
	 * 
	 * @return copy of this datasets
	 */
	public VDataSets copy() {
		VDataSets cp = new VDataSets();
		cp.train = this.train;
		cp.tune = this.tune;
		cp.test = this.test;
		cp.dataFmt = this.dataFmt;
		cp.valueFmt = this.valueFmt;
		cp.minSetSize = this.minSetSize;
		cp.maxSetSize = this.maxSetSize;
		cp.palindrome = this.palindrome;
		cp.dsd = this.dsd;
		for (VDataSet ds:dsl) {
			cp.add(ds.copy());
		}	
		return cp;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	// Data Modify
	///////////////////////////////////////////////////////////////////////////////
	
	/**
	 * toLowercase all data
	 */
	public void toLowercase() {
		if (!this.isFmtDataS()) return;
		for (VDataSet ds:dsl) ds.toLowercase();
		this.vgen = false;
		clearCache();
	}
	
	/**
	 * Remove a value in data from all
	 * @param dataValue value to remove
	 */
	public void removeDataS(String dataValue) {
		for (VDataSet ds:dsl) ds.removeDataS(dataValue);
		this.vgen = false;
		clearCache();
	}
	
	/**
	 * Replace value with alternate
	 * @param dataValue value to replace
	 * @param newValue new data value
	 */
	public void replaceDataS(String dataValue, String newValue) {
		for (VDataSet ds:dsl) ds.replaceDataS(dataValue, newValue);
		this.vgen = false;
		clearCache();		
	}
	
	/**
	 * Append datasets in dss to this datasets
	 * 
	 * @param dss datasets to append
	 */
	public void append(VDataSets dss) {
		if (dss == null || dss.size() < 1) return;
		// TODO: check that types match
		for (VDataSet ds:dss.dsl) {
			this.add(ds);
		}
		this.complete();
	}
	
	/**
	 * Prepend datasets in dss to this datasets
	 * 
	 * @param dss datasets to prepend
	 */
	public void prepend(VDataSets dss) {
		if (dss == null || dss.size() < 1) return;
		// TODO: check that types match
		int p = 0;
		for (VDataSet ds:dss.dsl) {
			this.add(p, ds);
			p++;
		}
		this.complete();
	}
	
	/**
	 * standard compareTo 
	 * if not the same decision based on size
	 * TODO: compare full details
	 */
	@Override
	public int compareTo(VDataSets dss) {
		if (dss.size() == this.size()) {
			int cnt = 0, cnt2 = 0;
			boolean diff = false;
			if (this.dataFmt != dss.dataFmt) diff = true;
			if (this.valueFmt != dss.valueFmt) diff = true;
			
			for (int i=0;i<dss.size();i++) {
				VDataSet ds = this.get(i);
				VDataSet ds2 = dss.get(i);
				if (!diff && ds.compareTo(ds2) != 0) diff = true;
				cnt += ds.size();
				cnt2 += ds2.size();
			}
			if (!diff) return 0;
			if (cnt == cnt2) {
				// FIXME
				return 0;
			}
			return (cnt < cnt2) ? -1 : 1;
		}
		return (this.size() < dss.size()) ? -1 : ((this.size() == dss.size()) ? 0 : 1);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	// Train / Tune / Test data split
	///////////////////////////////////////////////////////////////////////////////

	/**
	 * set train, tune, test split by percent
	 * train starts at 0, tune follows, test is last
	 * 
	 * @param train percent for train datasets
	 * @param tune percent for tune datasets
	 * @param test percent for test datasets
	 */
	public void setSplitPercent(double train, double tune, double test) {
		// check over/under 100 percent sum
		//System.out.println("setSplitPercent("+train+", "+tune+", "+test+")");
		double sum = train + tune + test;
		if (sum == 1) {
			train *= 100;
			tune *= 100;
			test *= 100;
		} else if (sum != 100) {
			train += (100 - sum);
		}
		this.train = (int)((double)dsl.size() * (train/100));
		this.tune = (int)((double)dsl.size() * (tune/100));
		this.test = (int)((double)dsl.size() * (test/100));
		clearCache();
	}
	
	/**
	 * set train, tune, test split by exact count
	 * train starts at 0, tune follows, test is last
	 * 
	 * @param train count for train datasets
	 * @param tune count for tune datasets
	 * @param test count for test datasets
	 */
	public void setSplit(int train, int tune, int test) {
		this.train = train;
		this.tune = tune;
		this.test = test;
		clearCache();
	}
	
	/**
	 * Get the train dataset count/size
	 * 
	 * @return count of dataset in train
	 */
	public int getTrainCount() {
		if (train == 0 || train > dsl.size()) return dsl.size();
		return train;		
	}
	
	/**
	 * Get the tune dataset count/size
	 * 
	 * @return count of dataset in tune
	 */
	public int getTuneCount() {
		return tune;		
	}

	/**
	 * Get the test dataset count/size
	 * 
	 * @return count of dataset in test
	 */
	public int getTestCount() {
		return test;	
	}
	
	// tune test/control	
	public static void setTuneControlSize(double ctlSize) {
		tctl = ctlSize;
	}
	public static double getTuneControlSize() {
		return tctl;
	}
	public int getTuneTestCount() {
		return tune-getTuneControlCount();
	}
	public int getTuneControlCount() {
		return (int)((double)tune*tctl);		
	}

	
	/**
	 * Determine if datasets is empty
	 * @return true if empty
	 */
	public boolean isEmpty() {
		return (size() == 0);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	// convert values to vvect  / convert data to vvect	
	///////////////////////////////////////////////////////////////////////////////
	
	/**
	 * map all Data valueIds to strings, only String data types will be converted
	 * 
	 * @param vtov Value to ValueId converter to use, if null default is used
	 * @param map valueid to string hash
	 */
	public void mapDataValueIdS(VectorToVid vtov, HashMap<Long, String> map) {
		if (map == null || this.size() < 1) return;
		if (!this.isFmtDataS()) return;
		if (vtov == null) vtov = new VectorToVid();
		for (VDataSet ds:this.dsl) ds.mapDataValueIdS(vtov, map);
	}	
	
	/**
	 * map all Value valueIds to strings, only String data types will be converted
	 * 
	 * @param vtov Value to ValueId converter to use, if null default is used
	 * @param map valueid to string hash
	 */
	public void mapValueValueIdS(VectorToVid vtov, HashMap<Long, String> map) {
		if (map == null || this.size() < 1) return;
		if (!this.isFmtDataS()) return;
		if (vtov == null) vtov = new VectorToVid();
		for (VDataSet ds:this.dsl) ds.mapValueValueIdS(vtov, map);
	}		
	
	/**
	 * Generate valueIds for all data and value elements
	 * 
	 * @return true if generated
	 */
	public boolean genVSets() {
		return genVSets(null);
	}
	
	/**
	 * Generate valueIds for all data and value elements with specified converter
	 * 
	 * @param vtov vector to valueId converter object
	 * @return true if generated
	 */
	public boolean genVSets(VectorToVid vtov) {
		if (dsl == null) return false;
		if (this.vgen) return true;
		for (VDataSet ds:dsl) ds.genVSet(vtov);
		this.vgen = true;
		return true;
	}
	

	///////////////////////////////////////////////////////////////////////////////
	// get sub dataSets
	///////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Get the training datasets as a dataset
	 * 
	 * @return training datasets
	 */
	public VDataSets getTrainDataSets() {
		if (this.trainDs != null) return this.trainDs;
		VDataSets dss = new VDataSets(this.dsd, null);
		for (int i=0;i<getTrainCount();i++) dss.add(dsl.get(i));
		if (this.vgen) dss.vgen = true; // save data
		this.trainDs = dss;
		return this.trainDs;
	}
	
	/**
	 * Get the tuning datasets as a dataset
	 * 
	 * @return tuning datasets
	 */
	public VDataSets getTuneDataSets() {
		if (this.tuneDs != null) return this.tuneDs;
		VDataSets dss = new VDataSets(this.dsd, null);
		for (int i=getTrainCount();i<(getTrainCount()+getTuneCount());i++) dss.add(dsl.get(i));
		if (this.vgen) dss.vgen = true; // save data
		this.tuneDs = dss;
		return this.tuneDs;
	}
	// tune test: 2/3s of tune dataset
	public VDataSets getTuneTestDataSets() {
		//if (this.tuneDs != null) return this.tuneDs;
		VDataSets dss = new VDataSets(this.dsd, null);
		int s = getTrainCount();
		int e = (getTrainCount()+getTuneTestCount());
		
		//int s = (getTrainCount()+getTuneControlCount());
		//int e = (getTrainCount()+getTuneControlCount()+getTuneTestCount());
		
		for (int i=s;i<e;i++) dss.add(dsl.get(i));
		if (this.vgen) dss.vgen = true; // save data
		//this.tuneDs = dss;
		return dss;
	}
	// tune control: 1/3 of tune dataset
	public VDataSets getTuneControlDataSets() {
		//if (this.tuneDs != null) return this.tuneDs;
		VDataSets dss = new VDataSets(this.dsd, null);
		int s = (getTrainCount()+getTuneTestCount());
		int e = (getTrainCount()+getTuneTestCount()+getTuneControlCount());
		//int s = (getTrainCount());
		//int e = (getTrainCount()+getTuneControlCount());
		for (int i=s;i<e;i++) dss.add(dsl.get(i));
		if (this.vgen) dss.vgen = true; // save data
		//this.tuneDs = dss;
		return dss;
	}
	
	/**
	 * Get the test datasets as a dataset
	 * 
	 * @return test datasets
	 */
	public VDataSets getTestDataSets() {
		if (this.testDs != null) return this.testDs;
		VDataSets dss = new VDataSets(this.dsd, null);	
		for (int i=(getTrainCount()+getTuneCount());i<dsl.size();i++) dss.add(dsl.get(i));
		if (this.vgen) dss.vgen = true; // save data
		this.testDs = dss;
		return this.testDs;
	}	
	
	/**
	 * Get the train and tune datasets as a dataset
	 * 
	 * @return train and tune  datasets
	 */
	public VDataSets getTrainAndTuneDataSets() {
		VDataSets dss = new VDataSets(this.dsd, null);
		for (int i=0;i<(getTrainCount()+getTuneCount());i++) dss.add(dsl.get(i));
		if (this.vgen) dss.vgen = true; // save data
		return dss;
	}
	
	// train & tuneTest datasets
	public VDataSets getTrainAndTuneTestDataSets() {
		VDataSets dss = new VDataSets(this.dsd, null);
		for (int i=0;i<(getTrainCount()+getTuneTestCount());i++) dss.add(dsl.get(i));
		if (this.vgen) dss.vgen = true; // save data
		return dss;
	}
	
	/**
	 * Get set of datasets generated by spliting this into sets of count size
	 * Generally used for multi-threading usecases
	 * 
	 * @param count max size of generated datasets
	 * @return list of datasets
	 */
	public List<VDataSets> getDataSetSplit(int count) {
		if (this.size() < 1) return null;
		
		List<VDataSets> dsSets = new ArrayList<>();
		if (count == 1) {
			VDataSets dss = new VDataSets(this.dsd, new ArrayList<>(this.dsl));
			if (this.vgen) dss.vgen = true; // save data
			dsSets.add(dss);
		} else {
			int mz = getListSplit(count, size());

			VDataSets dss = null;
			for (int i=0;i<this.size();i++) {
				if (dss == null) {
					dss = new VDataSets(this.dsd, null);
					if (this.vgen) dss.vgen = true; // save data
					dsSets.add(dss);
				}
				dss.add(this.get(i));
				if (dss.size() == mz) dss = null;
			}
		}
		return dsSets;
	}	
	//
	// Get best size for the sublists (most balanced)
	//
	private static int getListSplit(int count, int listSize) {
		if (count <= 0) return listSize;
		int sz = listSize/count;
		int m = listSize % count;
		if (m == 0) return sz;		
		// adjust for the leftover
		return sz+1;
	}

	
	///////////////////////////////////////////////////////////////////////////////
	// create / add data
	///////////////////////////////////////////////////////////////////////////////

	/**
	 * Add dataSet object to end of dataset
	 * Add dataSet AFTER data has been added to auto set data and value fmt
	 * 
	 * @param ds dataset to add
	 */
	public void add(VDataSet ds) {
		add(-1, ds);
	}
	public void add(int pos, VDataSet ds) {
		if (dsl == null) {
			dsl = new ArrayList<>();
			// get format..
			this.dataFmt = DSFmt.valueOf(DSFmt.class, "L"+ds.getFmtData().name());
			if (ds.getFmtValue() == DSFmt.N) this.valueFmt = DSFmt.N; // no data
			else this.valueFmt = DSFmt.valueOf(DSFmt.class, "L"+ds.getFmtValue().name());			
		}
		this.vgen = false;
		this.comp = false;
		this.clearCache();
		if (pos >= 0) dsl.add(pos, ds);
		else dsl.add(ds);
		if (this.minSetSize == -1 || this.minSetSize > ds.size()) this.minSetSize = ds.size();
		if (this.maxSetSize == -1 || this.maxSetSize < ds.size()) this.maxSetSize = ds.size();
	}
	
	
	/**
	 * Get all data strings in the datasets and add them to a set
	 * @return all strings in data
	 */
	public Set<String> getAllDataStrings() {
		Set<String> set = new HashSet<>();
		for (VDataSet ds:this.dsl) ds.getAllDataStrings(set);
		return set;
	}

	/**
	 * Get all value strings in the datasets and add them to a set
	 * @return all strings in values
	 */
	public Set<String> getAllValueStrings() {
		Set<String> set = new HashSet<>();
		for (VDataSet ds:this.dsl) ds.getAllValueStrings(set);
		return set;
	}

		
	//
	// set the full dataset data
	//
	public void setDataLLS(List<List<String>> dataSet) {
		this.dataFmt = DSFmt.LLS;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<String> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLS(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLI(List<List<Integer>> dataSet) {
		this.dataFmt = DSFmt.LLI;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<Integer> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLI(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLB(List<List<Boolean>> dataSet) {
		this.dataFmt = DSFmt.LLB;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<Boolean> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLB(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLL(List<List<Long>> dataSet) {
		this.dataFmt = DSFmt.LLL;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<Long> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLL(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLF(List<List<Double>> dataSet) {
		this.dataFmt = DSFmt.LLF;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<Double> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLF(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLV(List<List<Long>> dataSet) {
		this.dataFmt = DSFmt.LLV;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<Long> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLV(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLO(List<List<Object>> dataSet) {
		this.dataFmt = DSFmt.LLV;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<Object> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLO(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLOD(List<List<Object []>> dataSet) {
		this.dataFmt = DSFmt.LLOD;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<Object []> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLOD(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLSD(List<List<String []>> dataSet) {
		this.dataFmt = DSFmt.LLSD;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<String []> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLSD(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLLD(List<List<Long []>> dataSet) {
		this.dataFmt = DSFmt.LLLD;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<Long []> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLLD(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLID(List<List<Integer []>> dataSet) {
		this.dataFmt = DSFmt.LLID;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<Integer []> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLID(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLBD(List<List<Boolean []>> dataSet) {
		this.dataFmt = DSFmt.LLBD;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<Boolean []> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLBD(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setDataLLVD(List<List<Long []>> dataSet) {
		this.dataFmt = DSFmt.LLVD;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<dataSet.size();i++) {
			List<Long []> l = dataSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setDataLVD(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	
	//
	// set the full dataset tags/values
	// Value per token
	//
	public void setValuesLLS(List<List<String>> valueSet) {
		this.valueFmt = DSFmt.LLS;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			List<String> l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueLS(l);
			dsl.add(ds);
		}	
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setValuesLLI(List<List<Integer>> valueSet) {
		this.valueFmt = DSFmt.LLI;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			List<Integer> l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueLI(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setValuesLLB(List<List<Boolean>> valueSet) {
		this.valueFmt = DSFmt.LLB;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			List<Boolean> l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueLB(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setValuesLLL(List<List<Long>> valueSet) {
		this.valueFmt = DSFmt.LLL;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			List<Long> l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueLL(l);
			dsl.add(ds);
		}	
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setValuesLLF(List<List<Double>> valueSet) {
		this.valueFmt = DSFmt.LLF;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			List<Double> l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueLF(l);
			dsl.add(ds);
		}	
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setValuesLLVD(List<List<Long []>> valueSet) {
		this.valueFmt = DSFmt.LLVD;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			List<Long []> l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueLVD(l);
			dsl.add(ds);
		}	
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setValuesLLO(List<List<Object>> valueSet) {
		this.valueFmt = DSFmt.LLO;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			List<Object> l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueLO(l);
			dsl.add(ds);
		}	
		this.vgen = this.comp = false;
		this.clearCache();
	}
	
	//
	// set the full dataset tags/values
	// Single VALUE per DataSet (segments)
	//
	public void setValuesLS(List<String> valueSet) {
		this.valueFmt = DSFmt.LS;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			String l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueS(l);
			dsl.add(ds);
		}	
		this.vgen = this.comp = false;
		this.clearCache();
	}	
	public void setValuesLI(List<Integer> valueSet) {
		this.valueFmt = DSFmt.LI;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			Integer l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueI(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setValuesLB(List<Boolean> valueSet) {
		this.valueFmt = DSFmt.LB;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			Boolean l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueB(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setValuesLF(List<Double> valueSet) {
		this.valueFmt = DSFmt.LF;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			Double l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueF(l);
			dsl.add(ds);
		}	
		this.vgen = this.comp = false;
		this.clearCache();
	}
	public void setValuesLL(List<Long> valueSet) {
		this.valueFmt = DSFmt.LL;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			Long l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueL(l);
			dsl.add(ds);
		}	
		this.vgen = this.comp = false;
		this.clearCache();
	}	
	public void setValuesLVD(List<Long []> valueSet) {
		this.valueFmt = DSFmt.LVD;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			Long [] l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueVD(l);
			dsl.add(ds);
		}	
		this.vgen = this.comp = false;
		this.clearCache();
	}	
	public void setValuesLO(List<Object> valueSet) {
		this.valueFmt = DSFmt.LO;
		boolean add = false;
		if (dsl == null) {
			dsl = new ArrayList<>();
			add = true;
		} 
		for (int i=0;i<valueSet.size();i++) {
			Object l = valueSet.get(i);
			if (add) dsl.add(new VDataSet());
			VDataSet ds = dsl.get(i);
			ds.setValueO(l);
			dsl.add(ds);
		}
		this.vgen = this.comp = false;
		this.clearCache();
	}
	
	///////////////////////////////////////////////////////////////////////////////
	// Get data in set
	///////////////////////////////////////////////////////////////////////////////

	// get data set data: these generate so caller needs to hold on
	public List<List<String>> getLLS() {
		if (this.dataFmt != DSFmt.LLS) return null;
		List<List<String>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLS());
		return l;
	}	
	public List<List<Integer>> getLLI() {
		if (this.dataFmt != DSFmt.LLI) return null;
		List<List<Integer>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLI());
		return l;
	}
	public List<List<Boolean>> getLLB() {
		if (this.dataFmt != DSFmt.LLB) return null;
		List<List<Boolean>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLB());
		return l;
	}
	public List<List<Double>> getLLF() {
		if (this.dataFmt != DSFmt.LLI) return null;
		List<List<Double>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLF());
		return l;
	}
	public List<List<Long>> getLLL() {
		if (this.dataFmt != DSFmt.LLL) return null;
		List<List<Long>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLL());
		return l;
	}
	public List<List<Object>> getLLO() {
		if (this.dataFmt != DSFmt.LLO) return null;
		List<List<Object>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLO());
		return l;
	}
	public List<List<Long>> getLLV() {
		if (this.dataFmt != DSFmt.LLV && !this.vgen) genVSets();
		List<List<Long>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLV());
		return l;
	}
	
	// LLOD (list list object tier)
	public List<List<Object []>> getLLOD() {
		if (this.dataFmt != DSFmt.LLOD) return null;
		List<List<Object []>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLOD());
		return l;
	}	
	public List<List<String []>> getLLSD() {
		if (this.dataFmt != DSFmt.LLSD) return null;
		List<List<String []>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLSD());
		return l;
	}
	public List<List<Double []>> getSizeLLFD() {
		if (this.dataFmt != DSFmt.LLFD) return null;
		List<List<Double []>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLFD());
		return l;
	}
	public List<List<Long []>> getLLLD() {
		if (this.dataFmt != DSFmt.LLLD) return null;
		List<List<Long []>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLLD());
		return l;
	}
	public List<List<Integer []>> getLLID() {
		if (this.dataFmt != DSFmt.LLID) return null;
		List<List<Integer []>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLID());
		return l;
	}
	public List<List<Boolean []>> getLLBD() {
		if (this.dataFmt != DSFmt.LLBD) return null;
		List<List<Boolean []>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLBD());
		return l;
	}
	public List<List<Long []>> getLLVD() {
		if (this.dataFmt != DSFmt.LLVD && !this.vgen) return null;
		List<List<Long []>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getDataLVD());
		return l;
	}
	
	// TAG / VALUE set
	
	// value aggregate across datasets
	public List<List<String>> getValLLS() {
		if (this.valueFmt != DSFmt.LLS) return null;
		List<List<String>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getValueLS());
		return l;
	}
	
	// value per token
	public List<List<Long []>> getValLLV() {
		if (this.valueFmt != DSFmt.LLV && !this.vgen) genVSets();
		List<List<Long []>> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getValueLVD());
		return l;
	}
	// single value per set
	public List<Long []> getValLV() {
		if (this.valueFmt != DSFmt.LV && !this.vgen) genVSets();
		List<Long []> l = new ArrayList<>();
		for (int i=0;i<size();i++) l.add(dsl.get(i).getValueVD());
		return l;
	}
	
	//
	// get Data at set/offset
	//
	public String getDataLLS(int set, int offset) {
		return this.get(set).getDataS(offset);
	}
	public Long getDataLLL(int set, int offset) {
		return this.get(set).getDataL(offset);
	}	
	public Object getDataLLO(int set, int offset) {
		return this.get(set).getDataO(offset);
	}	
	public Integer getDataLLI(int set, int offset) {
		return this.get(set).getDataI(offset);
	}	
	public Boolean getDataLLB(int set, int offset) {
		return this.get(set).getDataB(offset);
	}
	public Double getDataLLF(int set, int offset) {
		return this.get(set).getDataF(offset);
	}	
	public Long getDataLLV(int set, int offset) {
		genVSets();
		return this.get(set).getDataV(offset);
	}
	
	public Long [] getDataLLLD(int set, int offset) {
		return this.get(set).getDataLD(offset);
	}
	public Integer [] getDataLLID(int set, int offset) {
		return this.get(set).getDataID(offset);
	}
	public String [] getDataLLSD(int set, int offset) {
		return this.get(set).getDataSD(offset);
	}
	public Double [] getDataLLFD(int set, int offset) {
		return this.get(set).getDataFD(offset);
	}
	public Object [] getDataLLOD(int set, int offset) {
		return this.get(set).getDataOD(offset);
	}
	public Boolean [] getDataLLBD(int set, int offset) {
		return this.get(set).getDataBD(offset);
	}
	public Long [] getDataLLVD(int set, int offset) {
		genVSets();
		return this.get(set).getDataVD(offset);
	}	
	

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// JSON in / out
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * write out dataset as JSON
	 *
	 * @param writer output stream
	 */
	public void toJSON(OutputStreamWriter writer) {	
		// for Veg
		Gson gb = getJSONBuilder();
		// write to steam
		gb.toJson(this, writer);  	
	}
	
	/**
	 * Export dataset to JSON file
	 *
	 * @param filename file to save as
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
	
	//
	// JSON serializer
	//
	public static JsonSerializer<VDataSets> getJSONSerializer() {
		return new  JsonSerializer<VDataSets>() {  
			// FIXME
			/* "dataset":{
			 * 	"definition":{},
			 * 	"info":{
			 * 		"sets":1, // count of sets
			 * 		"content":"string" // format; string, value, vector
			 * 	},
			 *	"data":[{
			 * 			"data": {[x,y,z],[x,y,z]}
			 * 			"tag": [val],
			 * 			"stats": {
			 * 				// have this match definition format but with just stats for set input/tag
			 * 				"width": 30,	// width in set
			 * 				// per tag counts
			 * 			}
			 * 			"results" {
			 * 				// run results with tags to identify/compare
			 * 			}
			 * 		}
			 *  ]
			 * }
			 */			
		    @Override
		    public JsonElement serialize(VDataSets dss, Type typeOfSrc, JsonSerializationContext context) {
		        JsonObject jsonV = new JsonObject();
		        // first complete
		        if (!dss.comp) dss.complete();
		        
		        // add def	
			    jsonV.add("definition", context.serialize(dss.getDefinition(), VDataSetDescriptor.class));
			    // add info
		        JsonObject jsonInfo = new JsonObject();
		        jsonInfo.addProperty("sets", dss.size());
		        jsonInfo.addProperty("tfmt", dss.valueFmt.name());
		        jsonInfo.addProperty("dfmt", dss.dataFmt.name());
		        jsonInfo.addProperty("test", dss.test);
		        jsonInfo.addProperty("tune", dss.tune);
		        jsonInfo.addProperty("train", dss.train);
		        jsonInfo.addProperty("minsz", dss.minSetSize);
		        jsonInfo.addProperty("maxsz", dss.maxSetSize);
		        jsonInfo.addProperty("pdrome", dss.palindrome);
		        
		        jsonV.add("info", jsonInfo);
		        // add data
	        	JsonArray jsonVals = new JsonArray();
		        for (VDataSet ds:dss.dsl) {
		        	JsonElement dse = context.serialize(ds, VDataSet.class);
		        	// stats
		        	// results
		        	jsonVals.add(dse);
		        }
		        jsonV.add("data", jsonVals);		        
		        return jsonV;
		    }
		};
	}
	
	/**
	 * Load dataset from JSON String
	 *
	 * @param json JSON string
	 * @return loaded datasets
	 */	
	public static VDataSets fromJSON(String json) {
		if (json == null) return null;
		return getJSONBuilder().fromJson(json, VDataSets.class);
	}
	
	/**
	 * Load dataset from JSON file
	 *
	 * @param filename JSON filename
	 * @return loaded datasets
	 */	
	public static VDataSets importJSON(String filename) {
		if (filename == null) return null;
		String json = VFileUtil.loadTextFile(filename);
		if (json == null) return null;
		return fromJSON(json);
		
	}
	
	/**
	 * Load a JSON datasets file and append it to this datasets
	 * 
	 * @param filename name of datasets file to load and append
	 * @return count of datasets added
	 */
	public int merge(String filename) {
		VDataSets mds = importJSON(filename);
		if (mds == null || mds.size() < 1) return 0;
		this.append(mds);
		return mds.size();
	}
	
	//
	// JSON Deserializer
	//
	public static JsonDeserializer<VDataSets> getJSONDeserializer() {
		return new  JsonDeserializer<VDataSets>() {  
		    @Override
		    public VDataSets deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
		        JsonObject jobj = json.getAsJsonObject();
		        
		        VDataSets dss = new VDataSets();
		        // info
		    	JsonObject jInfo = jobj.getAsJsonObject("info");
		        dss.dataFmt = DSFmt.valueOf(DSFmt.class, jInfo.get("dfmt").getAsString());
		        dss.valueFmt = DSFmt.valueOf(DSFmt.class, jInfo.get("tfmt").getAsString());
		        dss.train = jInfo.get("train").getAsInt();
		        dss.test = jInfo.get("test").getAsInt();
		        dss.tune = jInfo.get("tune").getAsInt();
		        dss.minSetSize = jInfo.get("minsz").getAsInt();
		        dss.maxSetSize = jInfo.get("maxsz").getAsInt();
		        dss.palindrome = jInfo.get("pdrome").getAsBoolean();

		        // definition
		        dss.dsd = context.deserialize(jobj.get("definition"), VDataSetDescriptor.class);
		        
		        // list of data
		        JsonArray ja = jobj.getAsJsonArray("data");
		        for (int i=0;i<ja.size();i++) {
			        VDataSet ds = context.deserialize(ja.get(i), VDataSet.class);
			        dss.add(ds);
		        }	
		    	dss.comp = true;

		    	return dss;
		    }
		};
	}	
	
	
	/**
	 *  print data sets summary
	 */		
	public void print() {		
		System.out.print("VDataSets["+this.size()+"]tok["+getTokenCount()+"]range["+getRangeCount()+"]  data["+this.getFmtData()+"]val["+this.getFmtValue()+"]");		
		if (this.isFmtDataD()) System.out.print(" Dep[true]");
		if (this.isFixedSetSize()) System.out.print(" sz["+this.getMinSetSize()+"]");
		else System.out.print(" sz["+this.getMinSetSize() + " - " +this.getMaxSetSize()+"]");		
		if (this.getDefinition() != null) System.out.print(" vtags["+this.getDefinition().getTagCount()+"]");
		System.out.println(" => split["+this.getTrainCount()+"]["+this.getTuneCount()+"]["+this.getTestCount()+"]");	
	}
	
	/**
	 * get string for an offset
	 * 
	 * @param set dataset to access
	 * @param offset offset into the dataset
	 */		
	public String getString(int set, int offset) {
		return get(set).getString(offset);		
	}
	
	/**
	 * get string valueId for an offset
	 * 
	 * @param set dataset to access
	 * @param offset offset into the dataset
	 */		
	public String getStringV(int set, int offset) {
		return get(set).getStringV(offset);		
	}

	/**
	 * print the dataSet
	 */		
	public void printData() {
		print();	
		
		for (int i=0;i<this.size();i++) {
			VDataSet ds = get(i);
			ds.printData("  ["+i+"] ");
		}
	}
	
	//
	// Get Gson writer with replaced serializers
	static Gson getJSONBuilder() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		// datasets
		gsonBuilder.registerTypeAdapter(VDataSetDescriptor.class, VDataSetDescriptor.getJSONSerializer());		
		gsonBuilder.registerTypeAdapter(VDataSetDescriptor.class, VDataSetDescriptor.getJSONDeserializer());				
		gsonBuilder.registerTypeAdapter(VDataSets.class, VDataSets.getJSONSerializer());		
		gsonBuilder.registerTypeAdapter(VDataSets.class, VDataSets.getJSONDeserializer());
		gsonBuilder.registerTypeAdapter(VDataSet.class, VDataSet.getJSONSerializer());		
		gsonBuilder.registerTypeAdapter(VDataSet.class, VDataSet.getJSONDeserializer());		
		return gsonBuilder.create();  
	}

}
