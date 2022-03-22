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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import vegml.Data.VDataSets.DSFmt;

public class VDataSet implements java.io.Serializable, Comparable<VDataSet> {

	private static final long serialVersionUID = -3761522567387508024L;
	private DSFmt dataFmt;
	private DSFmt valueFmt;
	private Object data;	
	private Object value;
	private String name;
	private int length = 0;
	private int depth = 0;
	private List<RangeTag> rlist;
	
	private transient Object dataV;	
	private transient Object valueV;
	private transient HashMap<Integer, List<RangeTag>> remap;
	private transient HashMap<Integer, List<RangeTag>> rmap;
	private transient boolean comp = false;
	
	// range object
	public class RangeTag implements java.io.Serializable {
		private static final long serialVersionUID = -364639003072647246L;
		protected int d [];
		protected Object v;	// array[]	
		//protected transient Long [] vV;
		
		/**
		 * get the start position / token
		 * @return start position
		 */
		public int getStart() {
			return d[0];
		}
		
		/**
		 * get the end position / token
		 * @return end position
		 */
		public int getEnd() {
			return d[1];
		}	
		
		/**
		 * get the token count
		 * @return length
		 */
		public int getLength() {
			return (d[1]-d[0])+1;
		}	
		
		/**
		 * get depth of range
		 * @return depth
		 */
		public int getDepth() {
			return d[2];
		}
		
		/**
		 * set the depth for range
		 * @param depth of range
		 */
		public void setDepth(int depth) {
			d[2] = depth;
		}
		
		/**
		 * get values as String array
		 * @return array
		 */
		public String [] getValuesS() {
			return (String [])v;
		}
		
		/**
		 * get values as Long array
		 * @return array
		 */
		public Long [] getValuesL() {
			return (Long [])v;
		}
		
		/**
		 * get values as Integer array
		 * @return array
		 */
		public Integer [] getValuesI() {
			return (Integer [])v;
		}
		
		/**
		 * get values as Double array
		 * @return array
		 */
		public Double [] getValuesF() {
			return (Double [])v;
		}
		
		/**
		 * get values as Boolean array
		 * @return array
		 */
		public Boolean [] getValuesB() {
			return (Boolean [])v;
		}
		
		/**
		 * get values as ValueId array
		 * @return array
		 */
		public Long [] getValuesV() {
		//	if (vV != null) return vV;
			return (Long [])v;
		}
		/**
		 * get count of values in value array
		 * @return number of values in array
		 */
		public int getValueCount() {
			if (v == null) return 0;
//			if (vV != null) return vV.length;
			if (isFmtDataS()) return getValuesS().length;
			if (isFmtDataV()) return getValuesV().length;
			if (isFmtDataF()) return getValuesF().length;
			if (isFmtDataL()) return getValuesL().length;
			if (isFmtDataI()) return getValuesI().length;
			if (isFmtDataB()) return getValuesB().length;
			return 0;
		}
		
		/**
		 * get the link index for tag at offset; if any
		 * @param valueOffset offset in values
		 * @return index of linked range or -1
		 */
		public int getLinkIndex(int valueOffset) {
			if (getValuesS()[valueOffset] == null || !getValuesS()[valueOffset].startsWith("idx:")) return -1;
			int ex = getValuesS()[valueOffset].indexOf(':', 4);
			String e = getValuesS()[valueOffset].substring(4, ex);
		//	System.out.println(" I["+getValuesS()[valueOffset]+"]["+e+"]");
			return Integer.parseInt(e);	
		}
		
		/**
		 * get Link tag for the tag at offset; if any
		 * @param valueOffset offset in values
		 * @return link tag or null
		 */
		public String getLinkTag(int valueOffset) {
			if (getValuesS()[valueOffset] == null || !getValuesS()[valueOffset].startsWith("idx:")) return null;
			int ex = getValuesS()[valueOffset].indexOf(':', 4);
			if (ex < 0) return null;
			String t = getValuesS()[valueOffset].substring(ex+1, getValuesS()[valueOffset].length());
		//	System.out.println(" T["+getValuesS()[valueOffset]+"]["+t+"]");
			return t;
		}
		
		/**
		 * Add another tag to the end
		 * @param tag tag to add
		 * @return index of new tag
		 */
		public int addTag(String tag) {
			String [] nt = new String[getValuesS().length+1];
			for (int i=0;i<getValuesS().length;i++) nt[i] = getValuesS()[i];
			this.v = nt;
			getValuesS()[getValuesS().length-1] = tag;
			comp = false;
			return getValuesS().length-1;
		}

		/**
		 * Add another tag to the end
		 * @param tag tag to add
		 * @return index of new tag
		 */
		public int addTag(Long tag) {
			Long [] nt = new Long[getValuesL().length+1];
			for (int i=0;i<getValuesL().length;i++) nt[i] = getValuesL()[i];
			this.v = nt;
			getValuesL()[getValuesL().length-1] = tag;
			comp = false;
			return getValuesL().length-1;
		}
		
		/**
		 * Add another tag to the end
		 * @param tag tag to add
		 * @return index of new tag
		 */
		public int addTag(Double tag) {
			Double [] nt = new Double[getValuesF().length+1];
			for (int i=0;i<getValuesF().length;i++) nt[i] = getValuesF()[i];
			this.v = nt;
			getValuesF()[getValuesF().length-1] = tag;
			comp = false;
			return getValuesF().length-1;
		}
		
		/**
		 * Add another tag to the end
		 * @param tag tag to add
		 * @return index of new tag
		 */
		public int addTag(Boolean tag) {
			Boolean [] nt = new Boolean[getValuesB().length+1];
			for (int i=0;i<getValuesB().length;i++) nt[i] = getValuesB()[i];
			this.v = nt;
			getValuesB()[getValuesB().length-1] = tag;
			comp = false;
			return getValuesB().length-1;
		}
		
		/**
		 * Add another tag to the end
		 * @param tag tag to add
		 * @return index of new tag
		 */
		public int addTag(Integer tag) {
			Integer [] nt = new Integer[getValuesI().length+1];
			for (int i=0;i<getValuesI().length;i++) nt[i] = getValuesI()[i];
			this.v = nt;
			getValuesI()[getValuesI().length-1] = tag;
			comp = false;
			return getValuesI().length-1;
		}
		
		
		/**
		 * Get list of child linked RangeTags as a list
		 * 
		 * @return list of child linked RangeTags
		 */
		public List<RangeTag> getLinkedChildren() {
			List<RangeTag> ol = null;		
			for (int i=1;i<this.getValueCount();i++) {
				int cidx = this.getLinkIndex(i);
				if (cidx < 0) continue;
				if (ol == null) ol = new ArrayList<>();	
				ol.add(getRange(cidx));
			} 
			return ol;
		}
		
		/**
		 * Get list of child linked tags
		 * 
		 * @return list of child linked RangeTags
		 */
		public List<String> getLinkedChildrenTags() {
			List<String> ol = null;		
			for (int i=1;i<this.getValueCount();i++) {
				String t = getLinkTag(i);
				if (t == null) continue;
				if (ol == null) ol = new ArrayList<>();		
				ol.add(t);
			} 
			return ol;
		}
		
		/**
		 * Get as string of tokens if S type
		 * 
		 * @return list of child linked RangeTags
		 */
		public String getDataAsString() {
			String s = "";	
			if (this.getLength() == 1) return VDataSet.this.getDataS(getStart());
			for (int i=this.getStart();i<this.getEnd() && i<VDataSet.this.size();i++) {
				s += VDataSet.this.getDataS(i)+" ";
			} 
			return s.trim();
		}
		
		/**
		 * Get as string of tokens if S type
		 * 
		 * @return list of child linked RangeTags
		 */
		public List<String> getDataAsStringList() {
			List<String> sl = new ArrayList<>();
			for (int i=this.getStart();i<=this.getEnd() && i<VDataSet.this.size();i++) {
				sl.add(VDataSet.this.getDataS(i));
			} 
			return sl;
		}
	}
	
	public VDataSet() {
		dataFmt = DSFmt.N;
		valueFmt = DSFmt.N;
	}

	/**
	 * constructor with data string
	 * @param data string for single data value
	 */
	public VDataSet(String data) {
		this();
		this.setDataS(data);
	}
	
	/**
	 * constructor with data string list
	 * @param data string list data
	 */
	public VDataSet(List<String> data) {
		this();
		this.setDataLS(data);
	}
	
	/**
	 * constructor with data string array
	 * @param data string array data
	 */
	public VDataSet(String [] data) {
		this();
		this.setDataLS(data);
	}
	
	/**
	 * constructor with data string list and value list
	 * @param data string list data
	 * @param values string list values
	 */
	public VDataSet(List<String> data, List<String> values) {
		this();
		this.setDataLS(data);
		this.setValueLS(values);
	}
	
	/**
	 * constructor with data string list and value list
	 * @param name name or id of dataset
	 * @param data string list data
	 * @param values string list values
	 */
	public VDataSet(String name, List<String> data, List<String> values) {
		this();
		this.setDataLS(data);
		this.setValueLS(values);
		this.name = name;
	}
	
	/**
	 * constructor with data string array and value array
	 * @param data string array data
	 * @param values string array values
	 */
	public VDataSet(String [] data, String [] values) {
		this();
		this.setDataLS(data);
		this.setValueLS(values);
	}
	
	/**
	 * constructor with data string array and value array
	 * @param name name or id of dataset
	 * @param data string array data
	 * @param values string array values
	 */
	public VDataSet(String name, String [] data, String [] values) {
		this();
		this.setDataLS(data);
		this.setValueLS(values);
		this.name = name;
	}
	
	/**
	 * constructor with data string list and single value
	 * @param data string list data
	 * @param value string value
	 */
	public VDataSet(List<String> data, String value) {
		this();
		this.setDataLS(data);
		this.setValueS(value);
	}
	
	/**
	 * constructor with data string list and single value
	 * @param name name or id of dataset
	 * @param data string list data
	 * @param value string value
	 */
	public VDataSet(String name, List<String> data, String value) {
		this();
		this.setDataLS(data);
		this.setValueS(value);
		this.name = name;
	}
	
	/**
	 * constructor with data string array and single value
	 * @param data string array data
	 * @param value string value
	 */
	public VDataSet(String [] data, String value) {
		this();
		this.setDataLS(data);
		this.setValueS(value);
	}
	
	/**
	 * constructor with data string array and single value
	 * @param name name or id of dataset
	 * @param data string array data
	 * @param value string value
	 */
	public VDataSet(String name, String [] data, String value) {
		this();
		this.setDataLS(data);
		this.setValueS(value);
		this.name = name;
	}
	
	/**
	 * get the name of this dataset
	 * @return name or null
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * set the name of this dataset
	 * @param name name or id of dataset
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Free any resources used to optimize
	 */
	public void freeResources() {
		dataV = null;
		valueV = null;
		remap = null;
		rmap = null;
		comp = false;
	}
	
	
	//////////////////////////////////////
	//
	// get formats
	//
	
	/**
	 * get data format
	 * @return format type
	 */
	public DSFmt getFmtData() {
		return dataFmt;
	}
	
	/**
	 * get value format
	 * @return format type
	 */
	public DSFmt getFmtValue() {
		return valueFmt;
	}
	
	/**
	 * data format with multiple values per
	 * @return true if data is D form
	 */
	public boolean isFmtDataD() {
		if (dataFmt.ordinal() >= DSFmt.LLD.ordinal() && dataFmt.ordinal() <= DSFmt.LVD.ordinal()) return true;
		return false;
	}
	
	/**
	 * is data format valueId
	 * @return true if it is
	 */
	public boolean isFmtDataV() {
		if (dataFmt == DSFmt.LVD || dataFmt == DSFmt.LLV || dataFmt == DSFmt.LV || dataFmt == DSFmt.V) return true;
		return false;
	}

	/**
	 * is data format String
	 * @return true if it is
	 */
	public boolean isFmtDataS() {
		if (dataFmt == DSFmt.LSD || dataFmt == DSFmt.LLS || dataFmt == DSFmt.LS || dataFmt == DSFmt.S) return true;
		return false;
	}
	
	/**
	 * is data format Integer
	 * @return true if it is
	 */
	public boolean isFmtDataI() {
		if (dataFmt == DSFmt.LID || dataFmt == DSFmt.LLI || dataFmt == DSFmt.LI || dataFmt == DSFmt.I) return true;
		return false;
	}
	
	/**
	 * is data format Double
	 * @return true if it is
	 */
	public boolean isFmtDataF() {
		if (dataFmt == DSFmt.LFD || dataFmt == DSFmt.LLF || dataFmt == DSFmt.LF || dataFmt == DSFmt.F) return true;
		return false;
	}
	
	/**
	 * is data format Long
	 * @return true if it is
	 */
	public boolean isFmtDataL() {
		if (dataFmt == DSFmt.LLD || dataFmt == DSFmt.LLL || dataFmt == DSFmt.LL || dataFmt == DSFmt.L) return true;
		return false;
	}
	
	/**
	 * is data format Object
	 * @return true if it is
	 */
	public boolean isFmtDataO() {
		if (dataFmt == DSFmt.LOD || dataFmt == DSFmt.LLO || dataFmt == DSFmt.LO || dataFmt == DSFmt.O) return true;
		return false;
	}
	
	/**
	 * is data format Boolean
	 * @return true if it is
	 */
	public boolean isFmtDataB() {
		if (dataFmt == DSFmt.LBD || dataFmt == DSFmt.LLB || dataFmt == DSFmt.LB || dataFmt == DSFmt.B) return true;
		return false;
	}
	
	/**
	 * value format with multiple values per
	 * @return true if value is D form
	 */
	public boolean isFmtValueD() {
		if (valueFmt.ordinal() >= DSFmt.LD.ordinal() && dataFmt.ordinal() <= DSFmt.VD.ordinal()) return true;
		if (valueFmt.ordinal() >= DSFmt.LLD.ordinal() && dataFmt.ordinal() <= DSFmt.LVD.ordinal()) return true;
		return false;
	}
	
	/**
	 * is value format valueId
	 * @return true if it is
	 */
	public boolean isFmtValueV() {
		if (valueFmt == DSFmt.LVD || valueFmt == DSFmt.LLV || valueFmt == DSFmt.LV || valueFmt == DSFmt.V) return true;
		return false;
	}
	
	/**
	 * is value format String
	 * @return true if it is
	 */
	public boolean isFmtValueS() {
		if (valueFmt == DSFmt.LSD || valueFmt == DSFmt.LLS || valueFmt == DSFmt.LS || valueFmt == DSFmt.S) return true;
		return false;
	}
	
	/**
	 * is value format Integer
	 * @return true if it is
	 */
	public boolean isFmtValueI() {
		if (valueFmt == DSFmt.LID || valueFmt == DSFmt.LLI || valueFmt == DSFmt.LI || valueFmt == DSFmt.I) return true;
		return false;
	}
	
	/**
	 * is value format Double
	 * @return true if it is
	 */
	public boolean isFmtValueF() {
		if (valueFmt == DSFmt.LFD || valueFmt == DSFmt.LLF || valueFmt == DSFmt.LF || valueFmt == DSFmt.F) return true;
		return false;
	}
	
	/**
	 * is value format Long
	 * @return true if it is
	 */
	public boolean isFmtValueL() {
		if (valueFmt == DSFmt.LLD || valueFmt == DSFmt.LLL || valueFmt == DSFmt.LL || valueFmt == DSFmt.L) return true;
		return false;
	}
	
	/**
	 * is value format Object
	 * @return true if it is
	 */
	public boolean isFmtValueO() {
		if (valueFmt == DSFmt.LOD || valueFmt == DSFmt.LLO || valueFmt == DSFmt.LO || valueFmt == DSFmt.O) return true;
		return false;
	}
	
	/**
	 * is value format Boolean
	 * @return true if it is
	 */
	public boolean isFmtValueB() {
		if (valueFmt == DSFmt.LBD || valueFmt == DSFmt.LLB || valueFmt == DSFmt.LB || valueFmt == DSFmt.B) return true;
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
	 * Determine if dataset is empty
	 * 
	 * @return true if dataset is empty
	 */
	public boolean isEmpty() {
		return (size() == 0);
	}	
	
	/**
	 * get length of dataset
	 * 
	 * @return length of dataset
	 */
	public int length() {
		return this.length;
	}
	
	/**
	 * get length of dataset
	 * 
	 * @return length of dataset
	 */
	public int size() {
		return this.length;
	}
	
	/**
	 * get length of dataset
	 * 
	 * @return length of dataset
	 */
	public int depth() {
		return this.length;
	}
	

	/**
	 * Determine if this dataset has values
	 * 
	 * @return true if values present
	 */
	public boolean haveValues() {
		return (this.value != null);
	}

	public boolean haveDataV() {
		return (dataV == null);
	}
	
	///////////////////////////////////////////////////////////////////////////////
	// Range Tag mapping
	///////////////////////////////////////////////////////////////////////////////
	
	/**
	 * add a range copy of existing range
	 * @param rt RangeTag to copy
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(RangeTag rt) {
		return addRangeO(rt.getStart(), rt.getEnd(), rt.getDepth(), rt.v);
	}
	
	/**
	 * add a range copy of existing range
	 * @param rt RangeTag to copy
	 * @param offset to start and end for the copy
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(RangeTag rt, int offset) {
		return addRangeO(rt.getStart()+offset, rt.getEnd()+offset, rt.getDepth(), rt.v);
	}
	
	/**
	 * add a range with tag values that are String
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param depth depth of this range tag for sorting and access
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, int depth, String values[]) {
		return addRangeO(start, end, depth, values);
	}
	
	/**
	 * add a range with tag values that are String
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, String values[]) {
		return addRangeO(start, end, 0, values);
	}
	
	/**
	 * add a range with tag values that are Long
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param depth depth of this range tag for sorting and access
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, int depth, Long values[]) {
		return addRangeO(start, end, depth, values);		
	}
	
	/**
	 * add a range with tag values that are Long
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, Long values[]) {
		return addRangeO(start, end, 0, values);		
	}
	
	/**
	 * add a range with tag values that are Double
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param depth depth of this range tag for sorting and access
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, int depth, Double values[]) {
		return addRangeO(start, end, depth, values);		
	}
	
	/**
	 * add a range with tag values that are Double
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, Double values[]) {
		return addRangeO(start, end, 0, values);		
	}
	
	/**
	 * add a range with tag values that are Integer
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param depth depth of this range tag for sorting and access
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, int depth, Integer values[]) {
		return addRangeO(start, end, depth, values);
	}
	
	/**
	 * add a range with tag values that are Integer
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, Integer values[]) {
		return addRangeO(start, end, 0, values);
	}
	
	/**
	 * add a range with tag values that are Boolean
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param depth depth of this range tag for sorting and access
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, int depth, Boolean values[]) {
		return addRangeO(start, end, depth, values);
	}	
	
	/**
	 * add a range with tag values that are Boolean
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, Boolean values[]) {
		return addRangeO(start, end, 0, values);
	}
	
	/**
	 * add a range with tag values that are Object
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param depth depth of this range tag for sorting and access
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, int depth, Object values[]) {
		return addRangeO(start, end, depth, values);
	}	
	
	/**
	 * add a range with tag values that are Object
	 * @param start start token or position in dataset
	 * @param end end token or position in dataset
	 * @param values values to add
	 * @return the RangeTag that is added or null
	 */
	public RangeTag addRange(int start, int end, Object values[]) {
		return addRangeO(start, end, 0, values);
	}
	
	/**
	 * Make a tag to link to another RangeTag 
	 * 
	 * @param linkIndex index of RangeTag to link to
	 * @param tag tag to associate
	 * @return
	 */
	public static String genLinkTag(int linkIndex, String tag) {
		return "idx:"+linkIndex+":"+tag;			
	}
	
	private RangeTag addRangeO(int start, int end, int depth, Object values) {
		RangeTag r = new RangeTag();
		r.d = new int[3];
		r.d[0] = start;	
		r.d[1] = end;
		r.d[2] = depth;
		r.v = values;
		
		if (rlist == null) rlist = new ArrayList<>();
		rlist.add(r);
		comp = false;
		return r;
	}
	
	/**
	 * complete the dataset
	 * this will align and sort ranges and optimize some informatoin
	 */
	public void complete() {
		if (comp) return;
		if (rlist != null) {
			if (this.isFmtDataS()) {
				// need to re-link information if IDs used
				// save id / object
				HashMap<RangeTag, Integer> rtidx = new HashMap<>();
				for (int i=0;i<rlist.size();i++) rtidx.put(rlist.get(i), i);
				
				// link tag string: idx:92:<tag>
				Collections.sort(rlist, rtSortLF);
				
				HashMap<Integer, Integer> rtmap = new HashMap<>();
				for (int i=0;i<rlist.size();i++) {
					rtmap.put(rtidx.get(rlist.get(i)), i); // old to new
				}

				// update tags
				for (int i=0;i<rlist.size();i++) {
					RangeTag rt = rlist.get(i);
					for (int x=0;x<rt.getValuesS().length;x++) {
						int idx = rt.getLinkIndex(x);
						if (idx < 0) continue;
						int nidx = rtmap.get(idx);
						if (nidx == idx) continue;	
						rt.getValuesS()[x] = genLinkTag(nidx, rt.getLinkTag(x));
					}	
				}
			} else {
				Collections.sort(rlist, rtSortLF);
			}
			if (rmap != null) {
				for (List<RangeTag> rl:rmap.values()) Collections.sort(rl, rtSortLF);
				for (List<RangeTag> rl:remap.values()) Collections.sort(rl, rtSortLF);
			}
		}
	}

	// 
	// make the maps when needed
	//
	private void buildRangeHash() {
        // create maps if ranges
        if (this.rmap == null && rlist != null) {
	    	rmap = new HashMap<>();
	    	remap = new HashMap<>();
	    	for (RangeTag rt:rlist) {
	    		List<RangeTag> rel = rmap.get(rt.getStart());
	    		if (rel == null) {
	    			rel = new ArrayList<>();
	    			rmap.put(rt.getStart(), rel);
	    		}
	    		rel.add(rt);
	    		rel = remap.get(rt.getEnd());
	    		if (rel == null) {
	    			rel = new ArrayList<>();
	    			remap.put(rt.getEnd(), rel);
	    		}
	    		rel.add(rt);
	    	}
        }
	}
	
	/**
	 * Sort ranges longest first; smallest depth first
	 */
	public void sortRangesLongestFirst() {
		buildRangeHash();
		if (rmap == null) return;
		for (List<RangeTag> rl:rmap.values()) Collections.sort(rl, rtSortLF);
		for (List<RangeTag> rl:remap.values()) Collections.sort(rl, rtSortLF);
	}
	
	/**
	 * Sort ranges longest last; largest depth first
	 */
	public void sortRangesLongestLast() {
		buildRangeHash();
		if (rmap == null) return;
		for (List<RangeTag> rl:rmap.values()) Collections.sort(rl, rtSortLL);
		for (List<RangeTag> rl:remap.values()) Collections.sort(rl, rtSortLL);
	}
	private static final Comparator<RangeTag> rtSortLL = new Comparator<RangeTag>() {
        @Override
        public int compare(RangeTag lvp, RangeTag rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
        	if (lvp.getStart() < rvp.getStart()) return -1;
        	if (lvp.getStart() > rvp.getStart()) return 1; 
        	
        	if (lvp.getEnd() > rvp.getEnd()) return 1;
        	if (lvp.getEnd() < rvp.getEnd()) return -1; 
        	if (lvp.getDepth() < rvp.getDepth()) return 1;
        	if (lvp.getDepth() > rvp.getDepth()) return -1; 
        	return 0;   
        }
    };
	private static final Comparator<RangeTag> rtSortLF = new Comparator<RangeTag>() {
        @Override
        public int compare(RangeTag lvp, RangeTag rvp) { // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
        	if (lvp.getStart() < rvp.getStart()) return -1;
        	if (lvp.getStart() > rvp.getStart()) return 1; 

        	if (lvp.getEnd() < rvp.getEnd()) return 1;
        	if (lvp.getEnd() > rvp.getEnd()) return -1; 
        	if (lvp.getDepth() > rvp.getDepth()) return 1;
        	if (lvp.getDepth() < rvp.getDepth()) return -1; 
        	return 0;   
        }
    };
    
    /**
     * Get RangeTag at the index position
     * 
     * @param index of the rangetag
     * @return RangeTag or null
     */
	public RangeTag getRange(int index) {
		if (rlist == null || index < 0 || index >= rlist.size()) return null;
		return rlist.get(index);
	}
	
    /**
     * Get index of RangeTag 
     * 
     * @param RangeTag to get index of
     * @return index of rangeTag or -1 if not found
     */
	public int getRangeIndex(RangeTag rt) {
		if (rlist == null || rt == null) return -1;
		for (int i=0;i<rlist.size();i++) {
			if (rlist.get(i) == rt) return i;
		}
		return -1;
	}
	
    /**
     * Get the index of the last added RangeTag
     * 
     * @return index of last
     */
	public int getLastRangeIndex() {
		if (rlist == null) return -1;
		return rlist.size() -1;
	}
    
    /**
     * Get RangeTag list for start position
     * 
     * @param start position of start in ranges
     * @return list of ranges or null
     */
	public List<RangeTag> getRangeStart(int start) {
		buildRangeHash();
		if (rmap == null) return null;
		return rmap.get(start);
	}
	
    /**
     * Get RangeTag list for end position
     * 
     * @param end position of end in ranges
     * @return list of ranges or null
     */
	public List<RangeTag> getRangeEnd(int end) {
		buildRangeHash();
		if (remap == null) return null;
		return remap.get(end);
	}
	
	/**
	 * Determine if dataset contains ranges
	 * 
	 * @return true if dataset has ranges
	 */
	public boolean haveRanges() {
		return (rlist != null);
	}
	
	/**
	 * Get count of RangeTags in this dataset
	 * 
	 * @return count of ranges in this dataset
	 */
	public int getRangeCount() {
		if (rlist == null) return 0;
		return rlist.size();
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	// Data Modify
	///////////////////////////////////////////////////////////////////////////////


	/**
	 * toLowercase all data
	 */
	public void toLowercase() {
		if (dataFmt == DSFmt.LSD) {
			List<String []> ls = this.getDataLSD();
			for (int i=0;i<ls.size();i++) {
				String [] s = ls.get(i);
				if (s != null) {
					for (int x=0;x<s.length;x++) {
						if (s[x] != null) s[x] = s[x].toLowerCase();
					}
				}
			}
			dataV = null;
		} else if (dataFmt == DSFmt.LLS) {
			//??
		} else if (dataFmt == DSFmt.LS) {
			List<String> ls = this.getDataLS();
			for (int i=0;i<ls.size();i++) {
				String s = ls.get(i);
				if (s != null) ls.set(i, s.toLowerCase());
			}
			dataV = null;
		} else if (dataFmt == DSFmt.S) {
			data = ((String)data).toLowerCase();
			dataV = null;
		}
	}
	
	/**
	 * Remove a value in data from all
	 * This will also modify ranges
	 * 
	 * @param dataValue value to remove
	 */
	public void removeDataS(String dataValue) {
		boolean rmv = false, rmvd = false;
		if (this.isFmtValueList()) {
			rmv = true;
			if (this.isFmtValueD()) rmvd = true;
		}
		
		if (dataFmt == DSFmt.LSD) {
			List<String []> ls = this.getDataLSD();
			for (int i=0;i<ls.size();i++) {
				String [] s = ls.get(i);
				if (s != null) {
					for (int x=0;x<s.length;x++) {
						if (s[x] == null) continue;
						if (s[x].equals(dataValue)) {
							// REMOVE value and data
							ls.remove(i);
							if (rmvd) getValueLSD().remove(i);
							else if (rmv) getValueLS().remove(i);
							i--;
							
							if (this.getRangeCount() > 0) {
// FIXME								
							}
						}
					}
				}
			}
			this.length = ls.size();
			dataV = null;
		} else if (dataFmt == DSFmt.LLS) {
			//??
		} else if (dataFmt == DSFmt.LS) {
			//System.out.println("START["+dataValue+"] "+this.getDataLS().size()+"/ " + getValueLSD().size());
			List<String> ls = this.getDataLS();
			for (int i=0;i<ls.size();i++) {
				String s = ls.get(i);
				if (s == null) continue;
				if (s.equals(dataValue)) {
					// REMOVE value and data
					ls.remove(i);
					if (rmvd) getValueLSD().remove(i);
					else if (rmv) getValueLS().remove(i);
					i--;
					if (this.getRangeCount() > 0) {
//FIXME								
					}
				}
			}
			this.length = ls.size();
			dataV = null;
		} else if (dataFmt == DSFmt.S && data != null) {
			if (((String)data).equals(dataValue)) data = null;
			dataV = null;
		}	
	}
	
	/**
	 * Replace value with alternate
	 * @param dataValue value to replace
	 */
	public void replaceDataS(String dataValue, String newValue) {
		if (dataFmt == DSFmt.LSD) {
			List<String []> ls = this.getDataLSD();
			for (int i=0;i<ls.size();i++) {
				String [] s = ls.get(i);
				if (s != null) {
					for (int x=0;x<s.length;x++) {
						if (s[x] == null) continue;
						if (s[x].equals(dataValue)) s[x] = newValue;
					}
				}
			}
			dataV = null;
		} else if (dataFmt == DSFmt.LLS) {
			//??
		} else if (dataFmt == DSFmt.LS) {
			List<String> ls = this.getDataLS();
			for (int i=0;i<ls.size();i++) {
				String s = ls.get(i);
				if (s == null) continue;
				if (s.equals(dataValue)) ls.set(i, newValue);
			}
			dataV = null;
		} else if (dataFmt == DSFmt.S && data != null) {
			if (((String)data).equals(dataValue)) data = newValue;
			dataV = null;
		}
	}

	/**
	 * copy all data (and lists completely)
	 * 
	 * @return copy of this dataset
	 */
	public VDataSet copy() {
		VDataSet cp = new VDataSet();
		cp.dataFmt = this.dataFmt;
		cp.valueFmt = this.valueFmt;
		cp.length = this.length;
		cp.depth = this.depth;
		cp.dataV = null;
		cp.valueV = null;
		cp.data = this.data;
		cp.value = this.value;
			
		// single data value
		if (dataFmt.ordinal() <= DSFmt.V.ordinal()) {
			// SINGLE -> immutable
		} else if (dataFmt.ordinal() <= DSFmt.VD.ordinal()) {
			// DEP: could have changes to subs
		} else if (dataFmt.ordinal() <= DSFmt.LV.ordinal()) {
			// LIST: 
			if (isFmtDataS()) {
				data = new ArrayList<>(this.getDataLS());
			} else if (isFmtDataL()) {
				data = new ArrayList<>(this.getDataLL());
			} else if (isFmtDataV()) {
				data = new ArrayList<>(this.getDataLV());
			} else if (isFmtDataF()) {
				data = new ArrayList<>(this.getDataLF());
			} else if (isFmtDataB()) {
				data = new ArrayList<>(this.getDataLB());
			} else if (isFmtDataI()) {
				data = new ArrayList<>(this.getDataLI());
			} else if (isFmtDataO()) {
				data = new ArrayList<>(this.getDataLO());				
			}
		} else if (dataFmt.ordinal() <= DSFmt.LVD.ordinal()) {
			// LIST DEP: NOTE -> could have change to sub-values
			if (isFmtDataS()) {
				data = new ArrayList<>(this.getDataLSD());
			} else if (isFmtDataL()) {
				data = new ArrayList<>(this.getDataLLD());
			} else if (isFmtDataV()) {
				data = new ArrayList<>(this.getDataLVD());
			} else if (isFmtDataF()) {
				data = new ArrayList<>(this.getDataLFD());
			} else if (isFmtDataB()) {
				data = new ArrayList<>(this.getDataLBD());
			} else if (isFmtDataI()) {
				data = new ArrayList<>(this.getDataLID());
			} else if (isFmtDataO()) {
				data = new ArrayList<>(this.getDataLOD());				
			}
		}
		
		if (valueFmt.ordinal() <= DSFmt.V.ordinal()) {
			// SINGLE -> immutable
		} else if (valueFmt.ordinal() <= DSFmt.VD.ordinal()) {
			// DEP: could have changes to subs
		} else if (valueFmt.ordinal() <= DSFmt.LV.ordinal()) {
			// LIST: 
			if (isFmtValueS()) {
				value = new ArrayList<>(this.getValueLS());
			} else if (isFmtValueL()) {
				value = new ArrayList<>(this.getValueLL());
			} else if (isFmtValueV()) {
				value = new ArrayList<>(this.getValueLV());
			} else if (isFmtValueF()) {
				value = new ArrayList<>(this.getValueLF());
			} else if (isFmtValueB()) {
				value = new ArrayList<>(this.getValueLB());
			} else if (isFmtValueI()) {
				value = new ArrayList<>(this.getValueLI());
			} else if (isFmtValueO()) {
				value = new ArrayList<>(this.getValueLO());				
			}
		} else if (valueFmt.ordinal() <= DSFmt.LVD.ordinal()) {
			// LIST DEP: NOTE -> could have change to sub-values
			if (isFmtValueS()) {
				value = new ArrayList<>(this.getValueLSD());
			} else if (isFmtValueL()) {
				value = new ArrayList<>(this.getValueLLD());
			} else if (isFmtValueV()) {
				value = new ArrayList<>(this.getValueLVD());
			} else if (isFmtValueF()) {
				value = new ArrayList<>(this.getValueLFD());
			} else if (isFmtValueB()) {
				value = new ArrayList<>(this.getValueLBD());
			} else if (isFmtValueI()) {
				value = new ArrayList<>(this.getValueLID());
			} else if (isFmtValueO()) {
				value = new ArrayList<>(this.getValueLOD());				
			}
		}

		return cp;
	}
	
	/**
	 * standard compareTo 
	 * if not the same decision based on size
	 * @param ds dataset to compare to
	 * @return 0 if equal, -1 if less, 1 if more
	 */
	@Override
	public int compareTo(VDataSet ds) {
		boolean diff = false;
		if (this.dataFmt != ds.dataFmt) diff = true;
		if (this.valueFmt != ds.valueFmt) diff = true;
		if (this.haveRanges() != ds.haveRanges()) diff = true;
		int sz = (this.size() < ds.size()) ? -1 : ((this.size() == ds.size()) ? 0 : 1);
		if (diff && sz == 0) {
			// FIXME
			return 0;
		}
		return sz;
	}

	
	/**
	 * Get all Strings in data and add them to the set
	 * @param set set to add strings to
	 * @return set of all data strings
	 */
	public Set<String> getAllDataStrings(Set<String> set) {
		if (dataFmt == DSFmt.LLSD) {
			// not a thing
		} else if (dataFmt == DSFmt.LSD) {
			for (String [] sd:this.getDataLSD()) {
				for (String s:sd) set.add(s);
			}
		} else if (dataFmt == DSFmt.LLS) {
			// not a thing
		} else if (dataFmt == DSFmt.LS) {
			set.addAll(this.getDataLS());
		} else if (dataFmt == DSFmt.S) {
			set.add(this.getDataS());
		}
		return set;
	}

	/**
	 * Get all Strings in values and add them to the set
	 * @param set set to add strings to
	 * @return set of all value strings
	 */
	public Set<String> getAllValueStrings(Set<String> set) {
		if (valueFmt == DSFmt.LLSD) {
			// not a thing
		} else if (valueFmt == DSFmt.LSD) {
			for (String [] sd:this.getValueLSD()) {
				for (String s:sd) set.add(s);
			}
		} else if (valueFmt == DSFmt.LLS) {
			// not a thing
		} else if (valueFmt == DSFmt.LS) {
			set.addAll(this.getValueLS());
		} else if (valueFmt == DSFmt.S) {
			set.add(this.getValueS());
		}
		return set;
	}
	
	
	//////////////////////////////////////
	//
	// set the data
	//
	
	/**
	 * Set data to single String
	 * @param data single datum
	 */
	public void setDataS(String data) {
		this.dataFmt = DSFmt.S;
		this.data = data;
		this.length = 1;
		this.depth = 1;
	}
	
	/**
	 * Set data to single Integer
	 * @param data single datum
	 */
	public void setDataI(Integer data) {
		this.dataFmt = DSFmt.I;
		this.data = data;
		this.length = 1;
		this.depth = 1;
	}
	
	/**
	 * Set data to single Boolean
	 * @param data single datum
	 */
	public void setDataI(Boolean data) {
		this.dataFmt = DSFmt.B;
		this.data = data;
		this.length = 1;
		this.depth = 1;
	}
	
	/**
	 * Set data to single Long
	 * @param data single datum
	 */
	public void setDataL(Long data) {
		this.dataFmt = DSFmt.L;
		this.data = data;
		this.length = 1;
		this.depth = 1;
	}
	
	/**
	 * Set data to single Double
	 * @param data single datum
	 */
	public void setDataF(Double data) {
		this.dataFmt = DSFmt.F;
		this.data = data;
		this.length = 1;
		this.depth = 1;
	}
	
	/**
	 * Set data to single Object
	 * @param data single datum
	 */
	public void setDataO(Object data) {
		this.dataFmt = DSFmt.O;
		this.data = data;
		this.length = 1;
		this.depth = 1;
	}
	
	/**
	 * Set data to single Long
	 * @param data single datum
	 */
	public void setDataV(Long data) {
		this.dataFmt = DSFmt.V;
		this.data = data;
		this.length = 1;
		this.depth = 1;
	}
	
	/**
	 * Set data to List of Strings
	 * @param data List of data values
	 */
	public void setDataLS(List<String> data) {
		this.dataFmt = DSFmt.LS;
		this.data = data;
		this.length = data.size();
		this.depth = 1;
	}
	
	/**
	 * Set data to List of Strings
	 * @param data array of data values
	 */
	public void setDataLS(String [] data) {
		setDataLS(new ArrayList<>(arrayToList(data)));
		this.depth = 1;
	}
	
	/**
	 * Set data to List of Integers
	 * @param data List of data values
	 */
	public void setDataLI(List<Integer> data) {
		this.dataFmt = DSFmt.LI;
		this.data = data;
		this.length = data.size();
		this.depth = 1;
	}
	
	/**
	 * Set data to List of Integers
	 * @param data array of data values
	 */
	public void setDataLI(Integer [] data) {
		setDataLI(new ArrayList<>(arrayToList(data)));
	}
	
	/**
	 * Set data to List of Boolean
	 * @param data list of data values
	 */
	public void setDataLB(List<Boolean> data) {
		this.dataFmt = DSFmt.LB;
		this.data = data;
		this.length = data.size();
		this.depth = 1;
	}
	
	/**
	 * Set data to List of Boolean
	 * @param data array of data values
	 */
	public void setDataLB(Boolean [] data) {
		setDataLB(new ArrayList<>(arrayToList(data)));
	}
	
	/**
	 * Set data to List of Long
	 * @param data List of data values
	 */
	public void setDataLL(List<Long> data) {
		this.dataFmt = DSFmt.LL;
		this.data = data;
		this.length = data.size();
		this.depth = 1;
	}
	
	/**
	 * Set data to List of Long
	 * @param data array of data values
	 */
	public void setDataLL(Long [] data) {
		setDataLL(new ArrayList<>(arrayToList(data)));
	}
	
	/**
	 * Set data to List of Double
	 * @param data List of data values
	 */
	public void setDataLF(List<Double> data) {
		this.dataFmt = DSFmt.LF;
		this.data = data;
		this.length = data.size();
		this.depth = 1;
	}
	
	/**
	 * Set data to List of Double
	 * @param data array of data values
	 */
	public void setDataLF(Double [] data) {
		setDataLF(new ArrayList<>(arrayToList(data)));
	}
	
	/**
	 * Set data to List of Object
	 * @param data list of data values
	 */
	public void setDataLO(List<Object> data) {
		this.dataFmt = DSFmt.LO;
		this.data = data;
		this.length = data.size();
		this.depth = 1;
	}
	
	/**
	 * Set data to List of Object
	 * @param data array of data values
	 */
	public void setDataLO(Object [] data) {
		setDataLO(new ArrayList<>(arrayToList(data)));
	}
	
	/**
	 * Set data to List of valueId
	 * @param data list of data values
	 */
	public void setDataLV(List<Long> data) {
		this.dataFmt = DSFmt.LV;
		this.data = data;
		this.length = data.size();
		this.depth = 1;
	}
	
	/**
	 * Set data to List of valueId
	 * @param data array of data values
	 */
	public void setDataLV(Long [] data) {
		setDataLV(new ArrayList<>(arrayToList(data)));
	}
	
	public void setDataLOD(List<Object []> data) {
		this.dataFmt = DSFmt.LOD;
		this.data = data;
		this.length = data.size();
		this.depth = data.get(0).length; // this will not be correct for variable
	}
	public void setDataLOD(Object [][] data) {
		setDataLOD(new ArrayList<>(arrayToList(data)));
	}
	public void setDataLFD(List<Double []> data) {
		this.dataFmt = DSFmt.LFD;
		this.data = data;
		this.length = data.size();
		this.depth = data.get(0).length; // this will not be correct for variable
	}
	public void setDataLFD(Double [][] data) {
		setDataLFD(new ArrayList<>(arrayToList(data)));
	}
	public void setDataLSD(List<String []> data) {
		this.dataFmt = DSFmt.LSD;
		this.data = data;
		this.length = data.size();
		this.depth = data.get(0).length; // this will not be correct for variable
	}
	public void setDataLSD(String [][] data) {
		setDataLSD(new ArrayList<>(arrayToList(data)));
	}
	public void setDataLID(List<Integer []> data) {
		this.dataFmt = DSFmt.LID;
		this.data = data;
		this.length = data.size();
		this.depth = data.get(0).length; // this will not be correct for variable
	}
	public void setDataLID(Integer [][] data) {
		setDataLID(new ArrayList<>(arrayToList(data)));
	}
	public void setDataLBD(List<Boolean []> data) {
		this.dataFmt = DSFmt.LBD;
		this.data = data;
		this.length = data.size();
		this.depth = data.get(0).length; // this will not be correct for variable
	}
	public void setDataLBD(Boolean [][] data) {
		setDataLBD(new ArrayList<>(arrayToList(data)));
	}
	public void setDataLLD(List<Long []> data) {
		this.dataFmt = DSFmt.LLD;
		this.data = data;
		this.length = data.size();
		this.depth = data.get(0).length; // this will not be correct for variable
	}
	public void setDataLLD(Long [][] data) {
		setDataLLD(new ArrayList<>(arrayToList(data)));
	}
	public void setDataLVD(List<Long []> data) {
		this.dataFmt = DSFmt.LVD;
		this.data = data;
		this.length = data.size();
		this.depth = data.get(0).length; // this will not be correct for variable
	}
	public void setDataLVD(Long [][] data) {
		setDataLVD(new ArrayList<>(arrayToList(data)));
	}
	
	
	//////////////////////////////////////
	//
	// set the values
	//
	public void setValueS(String value) {
		this.valueFmt = DSFmt.S;
		this.value = value;
	}
	public void setValueI(Integer value) {
		this.valueFmt = DSFmt.I;
		this.value = value;
	}
	public void setValueB(Boolean value) {
		this.valueFmt = DSFmt.B;
		this.value = value;
	}
	public void setValueL(Long value) {
		this.valueFmt = DSFmt.L;
		this.value = value;
	}
	public void setValueF(Double value) {
		this.valueFmt = DSFmt.F;
		this.value = value;
	}
	public void setValueO(Object value) {
		this.valueFmt = DSFmt.O;
		this.value = value;
	}

	public void setValueLS(List<String> value) {
		this.valueFmt = DSFmt.LS;
		this.value = value;
	}
	public void setValueLS(String [] data) {
		setValueLS(new ArrayList<>(arrayToList(data)));
	}
	public void setValueLI(List<Integer> value) {
		this.valueFmt = DSFmt.LI;
		this.value = value;
	}
	public void setValueLI(Integer [] data) {
		setValueLI(new ArrayList<>(arrayToList(data)));
	}
	public void setValueLB(List<Boolean> value) {
		this.valueFmt = DSFmt.LB;
		this.value = value;
	}
	public void setValueLB(Boolean [] data) {
		setValueLB(new ArrayList<>(arrayToList(data)));
	}
	public void setValueLL(List<Long> value) {
		this.valueFmt = DSFmt.LL;
		this.value = value;
	}
	public void setValueLL(Long [] data) {
		setValueLL(new ArrayList<>(arrayToList(data)));
	}
	public void setValueLO(List<Object> value) {
		this.valueFmt = DSFmt.LO;
		this.value = value;
	}
	public void setValueLO(Long [] data) {
		setValueLO(new ArrayList<>(arrayToList(data)));
	}
	public void setValueLF(List<Double> value) {
		this.valueFmt = DSFmt.LF;
		this.value = value;
	}
	public void setValueLF(Double [] data) {
		setValueLF(new ArrayList<>(arrayToList(data)));
	}

	
	public void setValueLLD(List<Long []> value) {
		this.valueFmt = DSFmt.LLD;
		this.value = value;
	}
	public void setValueLOD(List<Object []> value) {
		this.valueFmt = DSFmt.LOD;
		this.value = value;
	}
	public void setValueLSD(List<String []> value) {
		this.valueFmt = DSFmt.LSD;
		this.value = value;
	}
	public void setValueLID(List<Integer []> value) {
		this.valueFmt = DSFmt.LID;
		this.value = value;
	}
	public void setValueLFD(List<Double []> value) {
		this.valueFmt = DSFmt.LFD;
		this.value = value;
	}
	public void setValueLBD(List<Boolean []> value) {
		this.valueFmt = DSFmt.LBD;
		this.value = value;
	}
	
	
	//////////////////////////////////////
	//
	// get data at position
	//
	public String getDataS(int offset) {
		if (this.dataFmt != DSFmt.LS) {
			if (this.dataFmt == DSFmt.S) return this.getDataS();
			return null;
		}
		if (offset >= this.size()) return null;
		return this.getDataLS().get(offset);
	}
	public Integer getDataI(int offset) {
		if (this.dataFmt != DSFmt.LI) {
			if (this.dataFmt == DSFmt.I) return this.getDataI();
			return null;
		}
		return this.getDataLI().get(offset);
	}
	public Boolean getDataB(int offset) {
		if (this.dataFmt != DSFmt.LB) {
			if (this.dataFmt == DSFmt.B) return this.getDataB();
			return null;
		}
		return this.getDataLB().get(offset);
	}
	public Long getDataL(int offset) {
		if (this.dataFmt != DSFmt.LL) {
			if (this.dataFmt == DSFmt.L) return this.getDataL();
			return null;
		}
		return this.getDataLL().get(offset);
	}
	public Double getDataF(int offset) {
		if (this.dataFmt != DSFmt.LF) {
			if (this.dataFmt == DSFmt.F) return this.getDataF();
			return null;
		}
		return this.getDataLF().get(offset);
	}
	public Object getDataO(int offset) {
		if (this.dataFmt != DSFmt.LO) {
			if (this.dataFmt == DSFmt.O) return this.getDataO();
			return null;
		}
		return this.getDataLO().get(offset);
	}
	public Long getDataV(int offset) {
		if (this.dataFmt != DSFmt.LV && this.dataV == null) return null;
		
		if (this.dataV != null && this.dataV instanceof Long) {
			return (Long) this.dataV;
		}	
		if (this.isFmtDataD()) {
			List<Long[]> l = this.getDataLVD();
			if (l.size() <= offset) return null;
			return l.get(offset)[0];			
		}
		List<Long> l = this.getDataLV();
		if (l.size() <= offset) return null;
		return l.get(offset);
	}
	public Object getDataRawO(int offset) {
		return this.getDataRawLO().get(offset);
	}
	
	public Object [] getDataOD(int offset) {
		if (this.dataFmt != DSFmt.LOD) {
			//if (this.dataFmt == DSFmt.OD) return this.getDataOD();
			return null;
		}
		return this.getDataLOD().get(offset);
	}
	public String [] getDataSD(int offset) {
		if (this.dataFmt != DSFmt.LSD) {
			//if (this.dataFmt == DSFmt.SD) return this.getDataSD();
			return null;
		}
		return this.getDataLSD().get(offset);
	}
	public Long [] getDataLD(int offset) {
		if (this.dataFmt != DSFmt.LLD) {
			//if (this.dataFmt == DSFmt.LD) return this.getDataLD();
			return null;
		}
		return this.getDataLLD().get(offset);
	}
	public Integer [] getDataID(int offset) {
		if (this.dataFmt != DSFmt.LID) {
			//if (this.dataFmt == DSFmt.ID) return this.getDataID();
			return null;
		}
		return this.getDataLID().get(offset);
	}
	public Boolean [] getDataBD(int offset) {
		if (this.dataFmt != DSFmt.LBD) {
			//if (this.dataFmt == DSFmt.BD) return this.getDataBD();
			return null;
		}
		return this.getDataLBD().get(offset);
	}
	public Double [] getDataFD(int offset) {
		if (this.dataFmt != DSFmt.LFD) {
			//if (this.dataFmt == DSFmt.FD) return this.getDataFD();
			return null;
		}
		return this.getDataLFD().get(offset);
	}
	public Long [] getDataVD(int offset) {
		if (this.dataFmt != DSFmt.LVD && this.valueV == null) return null;		
		List<Long []> l = this.getDataLVD();
		if (l.size() <= offset) return null;
		return l.get(offset);
	}	
	public Object [] getDataRawD(int offset) {
		return this.getDataRawD().get(offset);
	}
	
	//////////////////////////////////////
	//
	// get data
	//
	public String getDataS() {
		if (this.dataFmt != DSFmt.S) return null;
		return (String)this.data;
	}
	public Integer getDataI() {
		if (this.dataFmt != DSFmt.I) return null;
		return (Integer)this.data;
	}
	public Long getDataL() {
		if (this.dataFmt != DSFmt.L) return null;
		return (Long)this.data;
	}
	public Boolean getDataB() {
		if (this.dataFmt != DSFmt.B) return null;
		return (Boolean)this.data;
	}
	public Double getDataF() {
		if (this.dataFmt != DSFmt.F) return null;
		return (Double)this.data;
	}
	public Object getDataO() {
		return this.data;
	}
	public Long getDataV() {
		if (this.dataFmt != DSFmt.V) return (Long)this.dataV;
		return (Long)this.data;
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getDataLS() {
		if (this.dataFmt != DSFmt.LS) return null;
		return (List<String>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<Integer> getDataLI() {
		if (this.dataFmt != DSFmt.LI) return null;
		return (List<Integer>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<Boolean> getDataLB() {
		if (this.dataFmt != DSFmt.LB) return null;
		return (List<Boolean>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<Long> getDataLL() {
		if (this.dataFmt != DSFmt.LL) return null;
		return (List<Long>)this.data;
	}	
	@SuppressWarnings("unchecked")
	public List<Double> getDataLF() {
		if (this.dataFmt != DSFmt.LF) return null;
		return (List<Double>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<Object> getDataLO() {
		if (this.dataFmt != DSFmt.LO) return null;
		return (List<Object>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<Long> getDataLV() {
		if (this.dataFmt != DSFmt.LV) return (List<Long>)this.dataV;
		return (List<Long>)this.data;
	}	
	@SuppressWarnings("unchecked")
	public List<Object> getDataRawLO() {
		return (List<Object>)this.data;
	}
	
	@SuppressWarnings("unchecked")
	public List<Object []> getDataLOD() {
		if (this.dataFmt != DSFmt.LOD) return null;
		return (List<Object []>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<Double []> getDataLFD() {
		if (this.dataFmt != DSFmt.LFD) return null;
		return (List<Double []>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<Long []> getDataLLD() {
		if (this.dataFmt != DSFmt.LLD) return null;
		return (List<Long []>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<Integer []> getDataLID() {
		if (this.dataFmt != DSFmt.LID) return null;
		return (List<Integer []>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<Boolean []> getDataLBD() {
		if (this.dataFmt != DSFmt.LBD) return null;
		return (List<Boolean []>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<String []> getDataLSD() {
		if (this.dataFmt != DSFmt.LSD) return null;
		return (List<String []>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<Long []> getDataLVD() {
		if (this.dataFmt != DSFmt.LVD) return (List<Long []>)this.dataV;
		return (List<Long []>)this.data;
	}
	@SuppressWarnings("unchecked")
	public List<Object []> getDataRawD() {
		return (List<Object []>)this.data;
	}
	
	
	//////////////////////////////////////
	//
	// get value at position
	//
	
	/**
	 * Get value String at offset
	 * @param offset to get from
	 */
	public String getValueS(int offset) {
		if (this.valueFmt == DSFmt.S) return getValueS();
		if (this.valueFmt != DSFmt.LS) return null;
		return this.getValueLS().get(offset);
	}
	
	/**
	 * Get value Integer at offset
	 * @param offset to get from
	 */
	public Integer getValueI(int offset) {
		if (this.valueFmt == DSFmt.I) return getValueI();
		if (this.valueFmt != DSFmt.LI) return null;
		return this.getValueLI().get(offset);
	}
	
	/**
	 * Get value Boolean at offset
	 * @param offset to get from
	 */
	public Boolean getValueB(int offset) {
		if (this.valueFmt == DSFmt.B) return getValueB();
		if (this.valueFmt != DSFmt.LB) return null;
		return this.getValueLB().get(offset);
	}
	
	/**
	 * Get value Long at offset
	 * @param offset to get from
	 */
	public Long getValueL(int offset) {
		if (this.valueFmt == DSFmt.L) return getValueL();
		if (this.valueFmt != DSFmt.LL) return null;
		return this.getValueLL().get(offset);
	}
	
	/**
	 * Get value Double at offset
	 * @param offset to get from
	 */
	public Double getValueF(int offset) {
		if (this.valueFmt == DSFmt.F) return getValueF();
		if (this.valueFmt != DSFmt.LF) return null;
		return this.getValueLF().get(offset);
	}
	
	/**
	 * Get value Object at offset
	 * @param offset to get from
	 */
	public Object getValueO(int offset) {
		if (this.valueFmt == DSFmt.O) return getValueO();
		if (this.valueFmt != DSFmt.LO) return null;
		return this.getValueLO().get(offset);
	}
	
	/**
	 * Get value raw Object at offset for any data type
	 * @param offset to get from
	 */
	public Object getValueRawO(int offset) {
		if (valueFmt.ordinal() <= DSFmt.V.ordinal()) return getValueO();
		return this.getValueRawLO().get(offset);
	}
	
	
	public String [] getValueSD(int offset) {
		if (this.valueFmt == DSFmt.SD) return getValueSD();
		if (this.valueFmt != DSFmt.LSD) return null;
		return this.getValueLSD().get(offset);
	}
	public Integer [] getValueID(int offset) {
		if (this.valueFmt == DSFmt.ID) return getValueID();
		if (this.valueFmt != DSFmt.LID) return null;
		return this.getValueLID().get(offset);
	}
	public Long [] getValueLD(int offset) {
		if (this.valueFmt == DSFmt.LD) return getValueLD();
		if (this.valueFmt != DSFmt.LLD) return null;
		return this.getValueLLD().get(offset);
	}
	public Object [] getValueOD(int offset) {
		if (this.valueFmt == DSFmt.OD) return getValueOD();
		if (this.valueFmt != DSFmt.LOD) return null;
		return this.getValueLOD().get(offset);
	}
	public Double [] getValueFD(int offset) {
		if (this.valueFmt == DSFmt.FD) return getValueFD();
		if (this.valueFmt != DSFmt.LFD) return null;
		return this.getValueLFD().get(offset);
	}
	public Boolean [] getValueBD(int offset) {
		if (this.valueFmt == DSFmt.BD) return getValueBD();
		if (this.valueFmt != DSFmt.LBD) return null;
		return this.getValueLBD().get(offset);
	}

	
	
	//////////////////////////////////////
	//
	// get values
	//
	public String getValueS() {
		if (this.valueFmt != DSFmt.S) return null;
		return (String)this.value;
	}
	public Integer getValueI() {
		if (this.valueFmt != DSFmt.I) return null;
		return (Integer)this.value;
	}
	public Boolean getValueB() {
		if (this.valueFmt != DSFmt.B) return null;
		return (Boolean)this.value;
	}
	public Long getValueL() {
		if (this.valueFmt != DSFmt.L) return null;
		return (Long)this.value;
	}
	public Double getValueF() {
		if (this.valueFmt != DSFmt.F) return null;
		return (Double)this.value;
	}
	public Object getValueO() {
		return this.value;
	}
	@SuppressWarnings("unchecked")
	public List<String> getValueLS() {
		if (this.valueFmt != DSFmt.LS) return null;
		return (List<String>)this.value;
	}
	@SuppressWarnings("unchecked")
	public List<Integer> getValueLI() {
		if (this.valueFmt != DSFmt.LI) return null;
		return (List<Integer>)this.value;
	}
	@SuppressWarnings("unchecked")
	public List<Boolean> getValueLB() {
		if (this.valueFmt != DSFmt.LB) return null;
		return (List<Boolean>)this.value;
	}
	@SuppressWarnings("unchecked")
	public List<Double> getValueLF() {
		if (this.valueFmt != DSFmt.LF) return null;
		return (List<Double>)this.value;
	}
	@SuppressWarnings("unchecked")
	public List<Long> getValueLL() {
		if (this.valueFmt != DSFmt.LL) return null;
		return (List<Long>)this.value;
	}
	@SuppressWarnings("unchecked")
	public List<Long> getValueLV() {
		if (this.valueFmt != DSFmt.LV) return (List<Long>)this.valueV;
		return (List<Long>)this.value;
	}
	@SuppressWarnings("unchecked")
	public List<Object> getValueLO() {
		if (this.valueFmt != DSFmt.LO) return null;
		return (List<Object>)this.value;
	}

	@SuppressWarnings("unchecked")
	public List<Object> getValueRawLO() {
		return (List<Object>)this.value;
	}
	
	// multiple values
	@SuppressWarnings("unchecked")
	public List<Long []> getValueLLD() {
		if (this.valueFmt != DSFmt.LLD) return (List<Long []>)this.valueV;
		return (List<Long []>)this.value;
	}
	@SuppressWarnings("unchecked")
	public List<String []> getValueLSD() {
		if (this.valueFmt != DSFmt.LSD) return (List<String []>)this.valueV;
		return (List<String []>)this.value;
	}
	@SuppressWarnings("unchecked")
	public List<Integer []> getValueLID() {
		if (this.valueFmt != DSFmt.LSD) return (List<Integer []>)this.valueV;
		return (List<Integer []>)this.value;
	}
	@SuppressWarnings("unchecked")
	public List<Object []> getValueLOD() {
		if (this.valueFmt != DSFmt.LOD) return (List<Object []>)this.valueV;
		return (List<Object []>)this.value;
	}
	@SuppressWarnings("unchecked")
	public List<Double []> getValueLFD() {
		if (this.valueFmt != DSFmt.LFD) return (List<Double []>)this.valueV;
		return (List<Double []>)this.value;
	}
	@SuppressWarnings("unchecked")
	public List<Boolean []> getValueLBD() {
		if (this.valueFmt != DSFmt.LFD) return (List<Boolean []>)this.valueV;
		return (List<Boolean []>)this.value;
	}
	
	// multiple values
	public Long [] getValueLD() {
		if (this.valueFmt != DSFmt.LD) return (Long [])this.valueV;
		return (Long [])this.value;
	}
	public String [] getValueSD() {
		if (this.valueFmt != DSFmt.SD) return (String [])this.valueV;
		return (String [])this.value;
	}
	public Object [] getValueOD() {
		if (this.valueFmt != DSFmt.OD) return (Object [])this.valueV;
		return (Object [])this.value;
	}
	public Boolean [] getValueBD() {
		if (this.valueFmt != DSFmt.BD) return (Boolean [])this.valueV;
		return (Boolean [])this.value;
	}
	public Double [] getValueFD() {
		if (this.valueFmt != DSFmt.FD) return (Double [])this.valueV;
		return (Double [])this.value;
	}	
	public Integer [] getValueID() {
		if (this.valueFmt != DSFmt.ID) return (Integer [])this.valueV;
		return (Integer [])this.value;
	}	

	
	////////////////////////////////////////////////
	// ValueIds
	public void setValueVD(Long [] value) {
		this.valueFmt = DSFmt.VD;
		this.value = value;
	}
	public void setValueLVD(List<Long []> value) {
		this.valueFmt = DSFmt.LVD;
		this.value = value;
	}
	
	public Long [] getValueVD() {
		if (this.valueFmt != DSFmt.VD) return (Long [])this.valueV;
		return (Long [])this.value;
	}
	@SuppressWarnings("unchecked")
	public List<Long []> getValueLVD() {
		if (this.valueFmt != DSFmt.LVD) return (List<Long []>)this.valueV;
		return (List<Long []>)this.value;
	}	
	public Long [] getValueVD(int offset) {
		if (this.valueFmt.ordinal() <= DSFmt.VD.ordinal()) return getValueVD();
		if (this.valueFmt != DSFmt.LVD && this.valueV == null) return null;
		return this.getValueLVD().get(offset);
	}
	
	
	/**
	 * Get data as a single string with spaces between
	 * 
	 * @return list of child linked RangeTags
	 */
	public String getDataAsString() {
		String s = "";	
		for (int i=0;i<=this.size();i++) {
			s += VDataSet.this.getDataS(i)+" ";
		} 
		return s.trim();
	}
	 
	/**
	 * map all Data valueIds to strings, only String data types will be converted
	 * 
	 * @param vtov Value to ValueId converter to use
	 * @param map valueId to string hash
	 */
	public void mapDataValueIdS(VectorToVid vtov, HashMap<Long, String> map) {
		if (this.data == null || map == null) return;
		if (!this.isFmtDataS()) return;
		// TODO allow other mapping Types
		if (this.isFmtDataD()) {
			for (int i=0;i<this.size();i++) {
				String sd[] = this.getDataSD(i);
				for (String s:sd) {
					map.put((long)vtov.toVectS(s), s);
				}
			}
		} else {
			for (int i=0;i<this.size();i++) {
				String s = this.getDataS(i);
				map.put((long)vtov.toVectS(s), s);
			}
		}
	}
	
	/**
	 * map all Value valueIds to strings, only String data types will be converted
	 * 
	 * @param vtov Value to ValueId converter to use
	 * @param map valueId to string hash
	 */
	public void mapValueValueIdS(VectorToVid vtov, HashMap<Long, String> map) {
		if (this.value == null || map == null) return;
		if (!this.isFmtValueS()) return;
		
		// TODO allow other mapping Types
		if (this.isFmtValueD()) {
			for (int i=0;i<this.size();i++) {
				String sd[] = this.getValueSD(i);
				for (String s:sd) {
					map.put((long)vtov.toVectS(s), s);
				}
			}
		} else {
			for (int i=0;i<this.size();i++) {
				String s = this.getValueS(i);
				map.put((long)vtov.toVectS(s), s);
			}
		}
	}
	
	//////////////////////////////////////
	// convert to VVect
	
	/**
	 * Generate valueids for all data and values, retain internally
	 * 
	 * @param vtov Value to ValueId converter to use
	 */
	public void genVSet(VectorToVid vtov) {
		genDataVSet(vtov);
		genValueVSet(vtov);
	}
	private void genDataVSet(VectorToVid vtov) {
		if (this.dataV != null) return;
		if (dataFmt == DSFmt.LV || dataFmt == DSFmt.V || dataFmt == DSFmt.LVD) return;
		List<Long> vl = null;
		List<Long []> vvl = null;
		long valueId = 0;
		switch (dataFmt) {
			case LOD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getDataLOD());
				break;				
			case LSD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getDataLSD());
				break;
			case LFD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getDataLFD());
				break;
			case LLD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getDataLLD());
				break;
			case LID:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getDataLID());
				break;	
			case LBD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getDataLBD());
				break;	
			case LVD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getDataLVD());
				break;	
			case LS:
				vl = VectorToVid.makeVectListsGen(vtov, this.getDataLS());
				break;
			case LL:
				vl = VectorToVid.makeVectListsGen(vtov, this.getDataLL());
				break;
			case LI:
				vl = VectorToVid.makeVectListsGen(vtov, this.getDataLI());
				break;
			case LB:
				vl = VectorToVid.makeVectListsGen(vtov, this.getDataLB());
				break;
			case LF:
				vl = VectorToVid.makeVectListsGen(vtov, this.getDataLF());
				break;
			case LO:
				vl = VectorToVid.makeVectListsGen(vtov, this.getDataLO());
				break;
			case LV:
				vl = VectorToVid.makeVectListsGen(vtov, this.getDataLV());
				break;
			case S:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getDataS());
				else valueId = (long)VectorToVid.toVectorGen(this.getDataS());			
				break;
			case L:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getDataL());
				else valueId = (long)VectorToVid.toVectorGen(this.getDataL());	
				break;
			case V:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getDataV());
				else valueId = (long)VectorToVid.toVectorGen(this.getDataV());	
				break;
			case F:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getDataF());
				else valueId = (long)VectorToVid.toVectorGen(this.getDataF());	
				break;
			case I:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getDataI());
				else valueId = (long)VectorToVid.toVectorGen(this.getDataI());	
				break;
			case B:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getDataB());
				else valueId = (long)VectorToVid.toVectorGen(this.getDataB());	
				break;
			case O:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getDataO());
				else valueId = (long)VectorToVid.toVectorGen(this.getDataO());	
				break;
			default:
				break;
		}
		if (vl != null) {
			this.dataV = vl;
		} else if (vvl != null) {
			this.dataV = vvl;
		} else {
			this.dataV = Long.valueOf(valueId);
		}
	}
	private void genValueVSet(VectorToVid vtov) {
		if (this.valueV != null) return;
		if (valueFmt == DSFmt.LV || valueFmt == DSFmt.V) return;
		List<Long []> vvl = null;
		Long [] vd = null;
		long valueId = 0;
		switch (valueFmt) {
		// could use generic here?
			case LOD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getValueLOD());
				break;				
			case LSD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getValueLSD());
				break;
			case LFD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getValueLFD());
				break;
			case LLD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getValueLLD());
				break;
			case LID:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getValueLID());
				break;	
			case LBD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getValueLBD());
				break;	
			case LVD:
				vvl = VectorToVid.makeVectListsGenD(vtov, this.getValueLVD());
				break;
				
			case LS:
				vvl = VectorToVid.makeVectListsGenToD(vtov, this.getValueLS());
				break;
			case LL:
				vvl = VectorToVid.makeVectListsGenToD(vtov, this.getValueLL());
				break;
			case LI:
				vvl = VectorToVid.makeVectListsGenToD(vtov, this.getValueLI());
				break;
			case LB:
				vvl = VectorToVid.makeVectListsGenToD(vtov, this.getValueLB());
				break;
			case LF:
				vvl = VectorToVid.makeVectListsGenToD(vtov, this.getValueLF());
				break;
			case LO:
				vvl = VectorToVid.makeVectListsGenToD(vtov, this.getValueLO());
				break;
				
			case VD:
				vd = VectorToVid.makeVectListsGenD(vtov, this.getValueVD());
				break;
			case SD:
				vd = VectorToVid.makeVectListsGenD(vtov, this.getValueSD());
				break;
			case ID:
				vd = VectorToVid.makeVectListsGenD(vtov, this.getValueID());
				break;
			case LD:
				vd = VectorToVid.makeVectListsGenD(vtov, this.getValueLD());
				break;
			case OD:
				vd = VectorToVid.makeVectListsGenD(vtov, this.getValueOD());
				break;
			case FD:
				vd = VectorToVid.makeVectListsGenD(vtov, this.getValueFD());
				break;
			case BD:
				vd = VectorToVid.makeVectListsGenD(vtov, this.getValueBD());
				break;
				
			case S:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getValueS());
				else valueId = (long)VectorToVid.toVectorGen(this.getValueS());
				break;
			case L:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getValueL());
				else valueId = (long)VectorToVid.toVectorGen(this.getValueL());
				break;
			case F:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getValueF());
				else valueId = (long)VectorToVid.toVectorGen(this.getValueF());				
				break;
			case I:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getValueI());
				else valueId = (long)VectorToVid.toVectorGen(this.getValueI());
				break;
			case B:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getValueB());
				else valueId = (long)VectorToVid.toVectorGen(this.getValueB());
				break;
			case O:
				if (vtov != null) valueId = (long)vtov.toVectGen(this.getValueO());
				else valueId = (long)VectorToVid.toVectorGen(this.getValueO());
				break;
			default:
				break;
		}
		if (vd != null) {
			this.valueV = vd;
		} else if (vvl != null) {
			this.valueV = vvl;
		} else {
			Long [] vv = new Long[1];
			vv[0] = valueId;
			this.valueV = vv;
		}
		
		// add valueIds for ranges
		if (this.getRangeCount() > 0 && !this.isFmtDataV()) {
			for (int i = 0;i<this.rlist.size();i++) {
				//RangeTag rt = rlist.get(i);
				/*
				if (rt.vV != null || rt.v == null) continue;
				int len = rt.getValueCount();
				rt.vV = new Long[len];
				
				if (isFmtDataS()) {
					for (int i=0;i<len;i++) {
						if (vtov != null) rt.vV[i] = (long)vtov.toVectGen(rt.getValuesS()[i]);
						else rt.vV[i] = (long)VectorToVid.toVectorGen(rt.getValuesS()[i]);
					}						
				} else if (isFmtDataL()) {
					for (int i=0;i<len;i++) {
						if (vtov != null) rt.vV[i] = (long)vtov.toVectGen(rt.getValuesL()[i]);
						else rt.vV[i] = (long)VectorToVid.toVectorGen(rt.getValuesL()[i]);
					}
				} else if (isFmtDataF()) {
					for (int i=0;i<len;i++) {
						if (vtov != null) rt.vV[i] = (long)vtov.toVectGen(rt.getValuesF()[i]);
						else rt.vV[i] = (long)VectorToVid.toVectorGen(rt.getValuesF()[i]);
					}
				} else if (isFmtDataV()) {
					for (int i=0;i<len;i++) {
						if (vtov != null) rt.vV[i] = (long)vtov.toVectGen(rt.getValuesV()[i]);
						else rt.vV[i] = (long)VectorToVid.toVectorGen(rt.getValuesV()[i]);
					}
				} else if (isFmtDataB()) {
					for (int i=0;i<len;i++) {
						if (vtov != null) rt.vV[i] = (long)vtov.toVectGen(rt.getValuesB()[i]);
						else rt.vV[i] = (long)VectorToVid.toVectorGen(rt.getValuesB()[i]);
					}
				} else if (isFmtDataI()) {
					for (int i=0;i<len;i++) {
						if (vtov != null) rt.vV[i] = (long)vtov.toVectGen(rt.getValuesI()[i]);
						else rt.vV[i] = (long)VectorToVid.toVectorGen(rt.getValuesI()[i]);
					}
				}
				*/
			}
		}
	}

	
	//////////////////////////////////////
	// JSON serializer/deserialize 
	public static JsonSerializer<VDataSet> getJSONSerializer() {
		return new  JsonSerializer<VDataSet>() {  
		    @Override
		    public JsonElement serialize(VDataSet ds, Type typeOfSrc, JsonSerializationContext context) {
		        JsonObject jsonV = new JsonObject();		        
		        jsonV.addProperty("tfmt", ds.valueFmt.name());
		        jsonV.addProperty("dfmt", ds.dataFmt.name());
		        jsonV.addProperty("length", ds.length);
		        jsonV.addProperty("depth", ds.depth);
		        jsonV.addProperty("name", ds.name);
		        jsonV.add("tag", context.serialize(ds.getValueO(), ds.getValueO().getClass()));
	        	jsonV.add("data", context.serialize(ds.getDataO(), ds.getDataO().getClass()));
	        	if (ds.rlist != null) {
		            JsonArray jsonRTL = new JsonArray();
	        		for (int i=0;i<ds.getRangeCount();i++) {
	        			RangeTag rt = ds.getRange(i);
	    		        JsonObject jrt = new JsonObject();        
	    		        JsonElement jsonD = context.serialize(rt.d, int [].class);	    		   
	    		        JsonElement jsonT = context.serialize(rt.v, getClassForRT(ds.valueFmt));	    		   
	    		        jrt.add("d", jsonD);
	    		        jrt.add("v", jsonT);
	    		        jsonRTL.add(jrt);
	        		}
		            jsonV.add("ranges", jsonRTL);	
	        	}
		        return jsonV;
		    }
		};
	}
	// JSON serializer
	public static JsonDeserializer<VDataSet> getJSONDeserializer() {
		return new  JsonDeserializer<VDataSet>() {  
		    @Override
		    public VDataSet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
		        JsonObject jobj = json.getAsJsonObject();
		        VDataSet ds = new VDataSet();
		        ds.valueFmt = DSFmt.valueOf(DSFmt.class, jobj.get("tfmt").getAsString());
		        ds.dataFmt = DSFmt.valueOf(DSFmt.class, jobj.get("dfmt").getAsString());
		        ds.length = jobj.get("length").getAsInt();
		        ds.depth = jobj.get("depth").getAsInt();
		        if (jobj.has("name")) {
			        ds.name = jobj.get("name").getAsString();
		        }
		        if (jobj.has("data")) {
		        	ds.data = context.deserialize(jobj.get("data"), getClassFor(ds.dataFmt));
		        }
		        if (jobj.has("tag")) {
		        	ds.value = context.deserialize(jobj.get("tag"), getClassFor(ds.valueFmt));
		        }
		        if (jobj.has("ranges")) {	
		    		ds.rlist = new ArrayList<>();
			    	JsonArray jRTL = jobj.getAsJsonArray("ranges");
			        for (int i=0;i<jRTL.size();i++) {
			        	JsonElement jtb = jRTL.get(i);
			        	JsonObject jrt = jtb.getAsJsonObject();
			        	RangeTag rt = ds.new RangeTag();
			        	rt.d = context.deserialize(jrt.get("d"), int [].class);
			        	rt.v = context.deserialize(jrt.get("v"), getClassForRT(ds.valueFmt));
			    		ds.rlist.add(rt);
			        }
		        }
		        ds.comp = true;
		    	return ds;
		    }
		};
	}

	private static Type getClassFor(DSFmt e) {
		switch (e) {
		case L:return Long.class;
		case V:return Long.class;
		case S:return String.class;
		case F:return Double.class;
		case B:return Boolean.class;
		case I:return Integer.class;
		case O:return Object.class;
		case VD: return new TypeToken<Long[]>(){}.getType();
		case SD: return new TypeToken<String[]>(){}.getType();
		case ID: return new TypeToken<Integer[]>(){}.getType();
		case LD: return new TypeToken<Long[]>(){}.getType();
		case OD: return new TypeToken<Object[]>(){}.getType();
		case FD: return new TypeToken<Double[]>(){}.getType();
		case BD: return new TypeToken<Boolean[]>(){}.getType();
		case LL: return new TypeToken<List<Long>>(){}.getType();
		case LLD: return new TypeToken<List<Long[]>>(){}.getType();
		case LV: return new TypeToken<List<Long>>(){}.getType();
		case LVD: return new TypeToken<List<Long[]>>(){}.getType();
		case LO: return new TypeToken<List<Object>>(){}.getType();
		case LOD: return new TypeToken<List<Object[]>>(){}.getType();
		case LS: return new TypeToken<List<String>>(){}.getType();
		case LSD: return new TypeToken<List<String[]>>(){}.getType();
		case LI: return new TypeToken<List<Integer>>(){}.getType();
		case LID: return new TypeToken<List<Integer[]>>(){}.getType();
		case LB: return new TypeToken<List<Boolean>>(){}.getType();
		case LBD: return new TypeToken<List<Boolean[]>>(){}.getType();
		case LF: return new TypeToken<List<Double>>(){}.getType();
		case LFD: return new TypeToken<List<Double[]>>(){}.getType();
		default: return List.class;
		}
	}
	private static Type getClassForRT(DSFmt e) {
		switch (e) {
		case LLD:
		case LVD:
		case LD:
		case VD:
		case LL:
		case LV:		
		case L:
		case V:return Long [].class;	
		case LS:
		case LSD:
		case SD:		
		case S:return String [].class;
		case LF:
		case LFD:
		case FD:		
		case F:return Double [].class;
		case LB:
		case LBD:
		case BD:	
		case B:return Boolean [].class;
		case LI:
		case LID:
		case ID:	
		case I:return Integer [].class;
		default: return Object [].class;
		}
	}
	// list from array
	private static <T> List<T> arrayToList(final T[] array) {
		final List<T> l = new ArrayList<T>(array.length);
		for (final T s : array) l.add(s);
		return (l);
	}
	
	//
	// get string for an offset
	//
	public String getString(int offset) {
		if (offset < 0 || offset >= size()) return "";
		if (isFmtDataD()) {
			Object [] o = this.getDataRawD(offset);
			String s = "[";
			for (int x=0;x<o.length;x++) {
				if (o[x] == null) s += "NULL";
				else s += o[x].toString();
				if (x != (o.length-1)) s += " / ";
			}
			s = String.format("%-40s", s+"]") ;
			s += " == " + getValString(this.getValueRawO(offset));
			return s;
		} else {
			Object o = this.getDataRawO(offset);
			String s = "";
			if (o != null) s = String.format("%-20s", o.toString());		
			s += " == " + getValString(this.getValueRawO(offset));
			return s;
		}		
	}
	private String getValString(Object oi) {
		if (oi == null) return "null";
		if (this.isFmtValueD()) {
			Object [] o = (Object [])oi;
			String s = "[";
			for (int x=0;x<o.length;x++) {
				if (o[x] == null) s += "NULL";
				else s += o[x].toString();
				if (x != (o.length-1)) s += " / ";
			}	
			return s+"]";
		} else {
			return oi.toString();
		}
	}
	
	//
	// get string for an offset
	//
	public String getStringV(int offset) {
		if (offset < 0 || offset >= size()) return "";
		if (isFmtDataD()) {
			Long [] l = this.getDataVD(offset);
			String s = "";
			for (int x=0;x<l.length;x++) {
				s += l[x].toString();
				if (x != (l.length-1)) s += " / ";
			}
			s = String.format("%-40s", s);
			s += " == " + this.getValueVD(offset)[0];
			return s;
		} else {
			String s = String.format("%-20s", this.getDataV(offset)) ;
			s += " == " + this.getValueVD(offset)[0];
			return s;
		}		
	}
	
	//
	// print the dataSet
	//
	public void printData() {
		printData("");
	}
	public void printData(String prefix) {
		System.out.print(prefix+"VDataSet["+this.size()+"/"+this.depth()+"] data["+this.getFmtData()+"]val["+this.getFmtValue()+"]");				
		if (this.isFmtDataD()) System.out.print(" Dep[true]");
		if (this.haveRanges()) System.out.print(" range["+this.getRangeCount()+"]");
		if (this.dataV != null) System.out.print(" DV");
		if (this.valueV != null) System.out.print(" VV");
		System.out.println("");		
		
		for (int i=0;i<this.size();i++) {
	//		System.out.println(prefix+"  "+i+")  "+getString(i));
		}
	}
}
