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
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class VDataSetDescriptor implements java.io.Serializable {
	private static final long serialVersionUID = 4620523433291810509L;
	//
	// Datatypes 
	//
	public enum DSDataType {
		Bit,
		Char,
		Bool,
		String,	
		Int,
		Long,
		Segment,
		Blob,
		Object,
	}
	//
	// type of input set for a tag
	//
	public enum DSSetType {
		Open,			// inputs set is unbound
		Closed,			// input set is bound (withing reason)
		Singleton,		// input has one-and-only-one value
		Range,			// input is a range of values (number)
	}
	//
	//
	//
	public enum DSInputSetType {
		Unique,			// string, value, etc
		Collection,		// always from a set
		Scale,			// Scale for values
		Time,			// Scale for times
	}
	//
	// Structure of this dataset
	// TODO: account for more complex sets that have too much human medeling: Q&A, broken down sentiment, etc
	//
	public enum DSStructure {
		Segment,	// data sets are each segments of data: text
		Block,		// each dataset is a single element with value, contianing all relations: XOR,OR,AND, math, logic
		Tokens,		// data sets are token sequences: pos
		Stream,		// a stream of data to evalute at each point, datasets may contain dependent sets for baseline
	}
	
	// define a tag
	public static class DSTag implements java.io.Serializable {
		private static final long serialVersionUID = -862050902167226431L;
		String tag;
		DSSetType setType;
		DSDataType type;
		int setSizeMin, setSizeMax;		// size of set approximate RANGE
		//boolean contextIdent;			// can/should be identified by context
		//List<String> formats;			// lower/upper/both/other/ order to check
		String description;				// in words
		public String getTag() {
			return tag;
		}
		//public long getTagV() {
		//	return VectorToVid.toVectorS(tag);
		//}
	}
	// define an input
	public static class DSInput implements java.io.Serializable {
		private static final long serialVersionUID = 5886930064715506227L;
		private String name;
		private DSInputSetType setType;
		private DSDataType dataType;		// data type: String / char / int / long / bool / bit	
		private DSDataType dataAtomType;	// atom type: String / char / int / long / bool / bit
		//private long inputRangeMax = 0;
		private boolean independent = false; // dependent or independent probability with peers
		DSInput() {
			this.dataType = DSDataType.String;
			this.dataAtomType = DSDataType.Char;
			this.setType = DSInputSetType.Unique;
			this.independent = false;
			this.name = "def";
		}
	}
	
	/*
	static class DSTagStats implements java.io.Serializable {
		private static final long serialVersionUID = -2587694914919976485L;
		int total;			// instances of tag
		int valueCount;		// values for tag
		int formatCount; 	// count per format?
	}*/
	
	// data set
	private DSStructure structure;
	private String name;
	private String description;
	
	// input
	private List<DSInput> inputs = null;
	private boolean palindrome;	// should be just in datasetdefinition?
	
	// process
	private String tagValueBaselineTotal;	// after train reset totals to this tag (and remove it)
	
	// data tags
	private int dataWidth = 0;
	private long dataNonValue = 0;
	private HashMap<String,DSTag> tags;


	public VDataSetDescriptor() {
		this.name = "default";	
		this.description = "default";	
		this.dataNonValue = 0;
		this.dataWidth = 0;
		this.structure = DSStructure.Tokens;
		this.tagValueBaselineTotal = "%veg%total%";
		this.inputs = new ArrayList<>();
		this.palindrome = false;
		addInput("def", DSInputSetType.Unique, DSDataType.String, false);
	}
	
	public VDataSetDescriptor(DSInputSetType inputSetType, DSDataType inputDataType, String name, boolean independent) {
		this();
		this.name = name;
		this.inputs.get(0).setType = inputSetType;
		this.inputs.get(0).dataType = inputDataType;
		this.inputs.get(0).independent = independent;
	}
	
	public VDataSetDescriptor(DSInputSetType inputSetType, DSDataType inputDataType, String name) {
		this();
		this.name = name;
		this.inputs.get(0).setType = inputSetType;
		this.inputs.get(0).dataType = inputDataType;
	}
	
	public String getName() {
		return name;
	}
	public String getDescription() {
		return this.description;
	}
	
	//
	// each set has the same value when inverted
	// this instruction allows each set will be trained in both directions
	// such as math, logic, OR/XOR/AND
	// example: D: 1 + 5 V: 6
	//
	public boolean isPalindromeSet() {
		return palindrome;
	}
	public void setPalindromeSet(boolean palindrome) {
		this.palindrome = palindrome;
	}
	public DSStructure getDataStructure() {
		return this.structure;
	}
	public void setDataStructure(DSStructure structure) {
		structure = this.structure;
	}

	
	public int getDataWidth() {
		return dataWidth;
	}
	public void setDataWidth(int dataWidth) {
		this.dataWidth = dataWidth;
	}
	public long getNonValue() {
		return dataNonValue;
	}
	public void setNonValue(long dataNonValue) {
		this.dataNonValue = dataNonValue;
	}

	public String getTagValueBaselineTotal() {
		return tagValueBaselineTotal;
	}
	public void setTagValueBaselineTotal(String tagValueBaselineTotal) {
		this.tagValueBaselineTotal = tagValueBaselineTotal;
	}

	
	//
	// get deviation between 2 input values
	//
	public int deviation(long value1, long value2, long learnedMetric) {
		int d = 0;
		if (value1 == value2) return 0; // same
/*
 * need full set and value distrubution to make an assessment metric for many
 * - then it can move forward
 */

		if (getInput(0).setType == DSInputSetType.Time) {
			if (value1 < value2) {
	//			learnedMetric
			} else {
				
			}
		} else if (getInput(0).setType == DSInputSetType.Scale) {
			if (value1 < value2) {
				
			} else {
				
			}
		} else if (getInput(0).setType == DSInputSetType.Collection) {
			
		} else if (getInput(0).setType == DSInputSetType.Unique) {
			// STRING ??
			// need base strings to check fromat
		}
		
	
		// FIXME
		return d;
	}
	/*
	 *for each accum
	 * get vsid
	 *  sort all bye in-value 
	 *   - create set of min/max around each
	 *   	- check neighbors for same values
	 *   	- if match merge
	 *   	- no match draw line
	 *   - assess min/max ranges
	 *   - determine min/max buffer
	 *   	- update boundries 
	 *   		- if single use min+buffer - no overlap with neighbors
	 *   		- if group buffer edges - no overlap with neighbors
	 *   - retain min/max/avg buffer-min/max for deviation decisions
	 *   - create mapping for incoming values to find correct range
	 *   - re-vid all accumes with new (accross all numbersets that have this position)
	 *   
	 *   
	 *   
	 */
	
	//
	// is this a scale / continueus set
	//
	public boolean isScale() {
		if (getInput(0).setType == DSInputSetType.Time || getInput(0).setType == DSInputSetType.Scale) return true;
		return false;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	// Inputs 
	///////////////////////////////////////////////////////////////////////////////
	public int getInputCount() {
		return this.inputs.size();
	}
	public List<DSInput> getInputs() {
		return this.inputs;
	}
	public DSInput getInput(int idx) {
		return this.inputs.get(idx);
	}
	
	// add an input
	public int addInput(String name, DSInputSetType SetType, DSDataType DataType, boolean independent) {
		DSInput it = new DSInput();
		it.name = name;
		it.setType = SetType;
		it.dataType = DataType;
		it.independent = independent;
		inputs.add(it);
		return inputs.size()-1;
	}

	
	//
	// true if independent probability from peers
	//
	public boolean isIndependent(int idx) {
		return getInput(idx).independent;
	}
	
	// Default 0
	public DSInputSetType getInputType() {
		return getInput(0).setType;
	}
	public DSDataType getDataType() {
		return getInput(0).dataType;
	}
	public void setDataType(DSDataType type) {
		getInput(0).dataType = type;
	}
	public DSDataType getDataAtomType() {
		return getInput(0).dataAtomType;
	}
	public void setDataAtomType(DSDataType type) {
		getInput(0).dataAtomType = type;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	// DataSet Tags
	///////////////////////////////////////////////////////////////////////////////

	//
	// Get tag count
	//
	public int getTagCount() {
		if (this.tags == null) return 0;
		return this.tags.keySet().size();
	}
	
	//
	// get data tag
	//
	public DSTag getDataTag(String name) {
		if (this.tags != null) return tags.get(name);
		return null;
	}
	
	//
	// Get all tags as a list
	//
	public List<DSTag> getTags() {
		if (this.tags == null) return null;
		return new ArrayList<DSTag>(tags.values());
	}
	
	//
	// Get all tags as a list
	//
	public List<String> getTagsStrings() {
		if (this.tags == null) return null;		
		return new ArrayList<String>(this.tags.keySet());
	}
	
	//
	// Get all tags / valueId map
	// 
	public HashMap<String,Long> getTagsValues(VectorToVid vtov) {
		if (this.tags == null) return null;	
		HashMap<String,Long> hm = new HashMap<>();
		for (String tag:tags.keySet()) hm.put(tag, (long)vtov.toVectGen(tag));
		return hm;
	}
	
	//
	// Add tag if it does not already exist
	//
	public DSTag addDataTag(String tagValue, DSSetType setType, DSDataType type, String desc) {
		if (tagValue == null || tagValue.isEmpty()) return null;
		if (this.tags == null) this.tags = new HashMap<>();
		DSTag tag = this.tags.get(tagValue);
		if (tag != null) return null;
		tag = new DSTag();
		this.tags.put(tagValue, tag);
		tag.tag = tagValue;
		tag.setType = setType;
		tag.type = type;
		tag.description = desc;
		return tag;
	}
	
	//
	// Add and increment the dataWidth
	//
	public DSTag addDataTagW(String tagValue, DSSetType setType, DSDataType type, String desc) {
		DSTag t = addDataTag(tagValue, setType, type, desc);
		if (t != null) this.dataWidth++;
		return t;
	}
	
	//
	// print summary
	//
	public void print() {
		System.out.println("DSDef["+this.name+"] inputs["+this.inputs.size()+"] width[" + this.getDataWidth()+"]");	
        for (DSInput di:inputs) {
    		System.out.println("   input["+di.name+"] type["+di.dataType+"]at["+di.dataAtomType+"]settype[" + di.setType+"] indie["+di.independent+"]");	
        }
	}
	
	///////////////////////////////////////////////////////////////////////////////
	// JSON in/out
	///////////////////////////////////////////////////////////////////////////////

	//
	// get JSON String
	//
	public String toJSON() {
		Gson gb = VDataSets.getJSONBuilder();
		return gb.toJson(this);
	}
	
	//
	// Write JSON to writer
	//
	public void toJSON(OutputStreamWriter writer) {
		Gson gb = VDataSets.getJSONBuilder();
		// write to steam
		gb.toJson(this, writer); 
	}
	
	//
	// export to a file
	//
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
	public static JsonSerializer<VDataSetDescriptor> getJSONSerializer() {
		return new  JsonSerializer<VDataSetDescriptor>() {  
		    @Override
		    public JsonElement serialize(VDataSetDescriptor dsd, Type typeOfSrc, JsonSerializationContext context) {
		        JsonObject jsonDef = new JsonObject();
		        jsonDef.addProperty("name", dsd.name);
		        jsonDef.addProperty("description", dsd.description);
		        jsonDef.addProperty("structure", dsd.structure.name());
		        jsonDef.addProperty("pdrome", dsd.palindrome);

		        // input
	        	JsonArray jInputs = new JsonArray();		   
		        for (DSInput di:dsd.inputs) {
			        JsonObject jInput = new JsonObject();
			        jInput.addProperty("type", di.dataType.name());
			        jInput.addProperty("atom_type", di.dataAtomType.name());
			        jInput.addProperty("set_type", di.setType.name());
			        jInput.addProperty("indie", di.independent);	
			        jInput.addProperty("name", di.name);	
			        jInputs.add(jInput);
		        }
		        jsonDef.add("inputs", jInputs);

		        // tags
		        JsonObject jTags = new JsonObject();
		        jTags.addProperty("width", dsd.dataWidth);
		        jTags.addProperty("non_value", dsd.dataNonValue);
		        jTags.addProperty("total_value", dsd.tagValueBaselineTotal);
		        
	        	JsonArray jsonVals = new JsonArray();
	        	if (dsd.tags != null) {
	        		// add info for each tag
		        	for (String ts:dsd.tags.keySet()) {
		        		DSTag tag = dsd.tags.get(ts);
				        JsonObject v = new JsonObject();
				        v.addProperty("tag", tag.tag);
				        v.addProperty("min_size", tag.setSizeMin);
				        v.addProperty("max_size", tag.setSizeMax);
				        v.addProperty("type", tag.type.name());
				        v.addProperty("set_type", tag.setType.name());
				        v.addProperty("description", tag.description);
			        	jsonVals.add(v);
		        	}
	        	}
	        	jTags.add("values", jsonVals);	        	
		        jsonDef.add("tags", jTags);
		        return jsonDef;
		    }
		};
	}	
	
	//
	// Load from JSON String
	//
	public static VDataSetDescriptor fromJSON(String json) {
		if (json == null) return null;
		return VDataSets.getJSONBuilder().fromJson(json, VDataSetDescriptor.class);
	}
	
	//
	// JSON deserializer
	//
	public static JsonDeserializer<VDataSetDescriptor> getJSONDeserializer() {
		return new  JsonDeserializer<VDataSetDescriptor>() {  
		    @Override
		    public VDataSetDescriptor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
		        JsonObject jobj = json.getAsJsonObject();
		        
		    	VDataSetDescriptor dsd = new VDataSetDescriptor();
		    	if (jobj.has("name")) dsd.name = jobj.get("name").getAsString();
	        	if (jobj.has("description")) dsd.description = jobj.get("description").getAsString();
		        dsd.structure = DSStructure.valueOf(DSStructure.class, jobj.get("structure").getAsString());
		        dsd.palindrome = jobj.get("pdrome").getAsBoolean();
		    	
		    	JsonArray jInputs = jobj.getAsJsonArray("inputs");
		    	dsd.inputs.clear();
		        for (int i=0;i<jInputs.size();i++) {
		        	JsonElement jtb = jInputs.get(i);
		        	JsonObject jt = jtb.getAsJsonObject();
		        	DSInput it = new DSInput();
			        it.dataType = DSDataType.valueOf(DSDataType.class, jt.get("type").getAsString());
			        it.dataAtomType = DSDataType.valueOf(DSDataType.class, jt.get("atom_type").getAsString());
			        it.setType = DSInputSetType.valueOf(DSInputSetType.class, jt.get("set_type").getAsString());
			    	it.independent = jt.get("indie").getAsBoolean();
			    	if (jt.has("name")) it.name = jt.get("name").getAsString();
			    	dsd.inputs.add(it);
		        }
		        	        
		    	JsonObject jTags = jobj.getAsJsonObject("tags");
		    	dsd.dataWidth = jTags.get("width").getAsInt();
		    	dsd.dataNonValue = jTags.get("non_value").getAsInt();
		    	dsd.tagValueBaselineTotal = jTags.get("total_value").getAsString();
		    			        
		    	JsonArray ja = jTags.getAsJsonArray("values");
		        for (int i=0;i<ja.size();i++) {
		        	JsonElement jtb = ja.get(i);
		        	JsonObject jt = jtb.getAsJsonObject();
		        	String desc = null;
		        	if (jt.has("description")) desc = jt.get("description").getAsString();
		        	DSDataType dt = DSDataType.valueOf(DSDataType.class, jt.get("type").getAsString());
		        	DSSetType st = DSSetType.valueOf(DSSetType.class, jt.get("set_type").getAsString());
		        	DSTag tag = dsd.addDataTag(jt.get("tag").getAsString(), st, dt, desc);
			    	tag.setSizeMax = jt.get("max_size").getAsInt();
			    	tag.setSizeMin = jt.get("min_size").getAsInt();
		        }
		    	return dsd;
		    }
		};
	}	
	
}
