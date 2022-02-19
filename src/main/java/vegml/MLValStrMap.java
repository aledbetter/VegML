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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import gnu.trove.map.hash.TIntObjectHashMap;

class MLValStrMap implements java.io.Serializable {
	private static final long serialVersionUID = -9106163269675195764L;
	
	private List<TIntObjectHashMap<String>> strMaps;
	private HashMap<String, Integer> mapReg;
	
	// change to single list of maps
	// add registry for dimension/dataplan
	// multiple dimensions/etc can mapp to the same set
	
	public MLValStrMap() {
		strMaps = new ArrayList<>();
		mapReg = new HashMap<>(4);
	}
	
	
	//
	// register Or get a string map for a dimension
	//
	// @param dimensionTag  dimension to register
	// @return mapId to use
	//
	public int regMap(String dimensionTag) {
		Integer ri = mapReg.get(dimensionTag);
		if (ri != null) return ri.intValue();
		TIntObjectHashMap<String> nm = new TIntObjectHashMap<String>(512);
		strMaps.add(nm);
		int id = strMaps.size()-1;
		mapReg.put(dimensionTag, id);
		return id;
	}
	
	//
	// register Or get a string map for a dimension
	//
	// @param dimensionTag  dimension to register
	// @param dataPlaneTag  dataPlane to register
	// @return mapId to use
	//
	public int regMap(String dimensionTag, String dataPlaneTag) {
		String tag = dimensionTag+"/"+dataPlaneTag;
		Integer ri = mapReg.get(tag);
		if (ri != null) return ri.intValue();
		TIntObjectHashMap<String> nm = new TIntObjectHashMap<String>(512);
		strMaps.add(nm);
		int id = strMaps.size()-1;
		mapReg.put(tag, id);
		return id;
	}
	
	// 
	// Add a string vector mapping to a map
	//
	// @param mapId  mapId from the registration
	// @param vector  vector to map
	// @param str String to map to the vector
	//
	public void add(int mapId, int vector, String str) {
		TIntObjectHashMap<String> dm = strMaps.get(mapId);
		if (dm == null) return;
		//	String s = tokenMap.get(vector);
		//	if (s != null && !s.equals(str)) System.out.println("Colision["+vector+"] " +s+ " != " + str);
		dm.put(vector, str);
	}
	
	//
	// get the string mappings for mapId
	//
	// @param mapId  map to look for string
	// @return string for vector
	//
	public TIntObjectHashMap<String> getMap(int mapId) {
		return strMaps.get(mapId);
	}
	
	
	//
	// get the string for this vector in the registred map
	//
	// @param mapId  map to look for string
	// @param vector  vector to find related string for
	// @return string for vector
	//
	public String get(int mapId, int vector) {
		TIntObjectHashMap<String> dm = strMaps.get(mapId);
		if (dm == null) return null;
		return dm.get(vector);
	}
	
	//
	// get the string for this vector in the registred map
	//
	// @param mapId  map to look for string
	// @param vector  vector to find related string for
	// @return string for vector
	//
	public String get(String dimensionTag, int vector) {
		if (strMaps == null || mapReg == null) return null;
		TIntObjectHashMap<String> dm = strMaps.get(mapReg.get(dimensionTag));
		if (dm == null) return null;
		return dm.get(vector);
	}
	// 
	// Add a string vector mapping to a map
	//
	// @param mapId  mapId from the registration
	// @param vector  vector to map
	// @param str String to map to the vector
	//
	public void add(String dimensionTag, int vector, String str) {
		TIntObjectHashMap<String> dm = strMaps.get(mapReg.get(dimensionTag));
		if (dm == null) return;
		//	String s = tokenMap.get(vector);
		//	if (s != null && !s.equals(str)) System.out.println("Colision["+vector+"] " +s+ " != " + str);
		dm.put(vector, str);
	}

	
	public int getCount() {
		int count = 0;
		for (int i=0;i<strMaps.size();i++) {
			count += strMaps.get(i).size();
		}
		return count;
	}
	public void print() {
		System.out.println("VectStrMap: TOTAL: " + getCount());
		for (String s:mapReg.keySet()) {
			System.out.println("  mapping["+s+"] " + mapReg.get(s));
		}
		for (int i=0;i<strMaps.size();i++) {
			System.out.println("  mapId["+i+"] " + strMaps.get(i).size());
		}

	}

	//
	// optimize after training complete
	//
	public void optimize() {
		for (int i=0;i<strMaps.size();i++) {
			strMaps.get(i).compact();
		}
	}
	
	//
	// Clear all strings and mappings
	//
	public void clear() {
		mapReg.clear();
		strMaps.clear();
	}

	//
	// Clear all strings and mappings for mapID
	//
	public void clear(int mapId) {
		TIntObjectHashMap<String> dm = strMaps.get(mapId);
		if (dm == null) return;
		dm.clear();
	}
	
	//
	// merge strings dimension and map 
	//
	public int merge(VegML vML, VegML xML, MLValStrMap xvsm) {
		if (!xML.hasDimensionStrings()) return getCount();
		
		for (int xdp=0;xdp<xML.getDataPlaneCount();xdp++) {
			VDataPlane xdpix = xML.getDataPlane(xdp);
			VDataPlane dpix = vML.getDataPlane(xdpix.getDimensionTag(), xdpix.getTag());
			if (xdpix.getStrDimensionMapID() >= 0) {
				// find the match
				TIntObjectHashMap<String> dm = strMaps.get(dpix.getStrDimensionMapID());
				TIntObjectHashMap<String> xdm = xvsm.strMaps.get(xdpix.getStrDimensionMapID());
				//System.out.println("STRMAP_DX["+xdix.getTag()+"]("+xdm.size()+") => ["+dix.getTag()+"]("+dm.size()+")");
				dm.putAll(xdm);
			}
			if (xdpix.getStrMapID() >= 0) {
				// find the match	
				TIntObjectHashMap<String> dm = strMaps.get(dpix.getStrMapID());
				TIntObjectHashMap<String> xdm = xvsm.strMaps.get(xdpix.getStrMapID());
				//System.out.println("STRMAP_dpX["+xdpix.getTag()+"]("+xdm.size()+") => ["+dpix.getTag()+"]("+dm.size()+")");
				dm.putAll(xdm);
			}
		}

		optimize();
		return getCount();
	}
	
	public int diff(VegML vML, VegML xML, MLValStrMap xvsm) {
		int cnt = 0;
		
		//System.out.println("Str DIFF: " + xML.hasDimensionStrings() + " / " + vML.hasDimensionStrings());
		if (!xML.hasDimensionStrings()) {
			if (!vML.hasDimensionStrings()) return 0;
			System.out.println(" *DIFF[strmap] X not have data");	
			return getCount();
		}
		if (!vML.hasDimensionStrings()) {
			System.out.println(" *DIFF[strmap] X has data");	
			return getCount();
		}
		
		for (int xdp=0;xdp<xML.getDataPlaneCount();xdp++) {
			VDataPlane xdpix = xML.getDataPlane(xdp);
			VDataPlane dpix = vML.getDataPlane(xdpix.getDimensionTag(), xdpix.getTag());
			if (xdpix.getStrMapID() >= 0) {
				if (dpix.getStrDimensionMapID() < 0) {
					System.out.println(" DIFF[strmap]dim["+xdpix.getTag()+"] X has data");	
					cnt++;
				} else {
					// find the match
					TIntObjectHashMap<String> dm = strMaps.get(dpix.getStrDimensionMapID());
					TIntObjectHashMap<String> xdm = xvsm.strMaps.get(xdpix.getStrDimensionMapID());
					if (dm.size() != xdm.size()) {
						System.out.println(" DIFF[strmap]dim["+xdpix.getTag()+"] size["+dm.size()+" != "+xdm.size()+"]");	
						cnt++;
					}
				}
			}
			if (xdpix.getStrMapID() >= 0) {
				if (dpix.getStrMapID() < 0) {
					System.out.println(" DIFF[strmap]dp["+xdpix.getTag()+"] X has data");	
					cnt++;						
				} else {
					// find the match	
					TIntObjectHashMap<String> dm = strMaps.get(dpix.getStrMapID());
					TIntObjectHashMap<String> xdm = xvsm.strMaps.get(xdpix.getStrMapID());
					if (dm.size() != xdm.size()) {
						System.out.println(" DIFF[strmap]dp["+xdpix.getTag()+"] size["+dm.size()+" != "+xdm.size()+"]");	
						cnt++;
					}
				}
			}
		}
		return cnt;
	}
	
	public List<TIntObjectHashMap<String>> getDMaps() {
		return strMaps;
	}
	public HashMap<String, Integer> getRegMap() {
		return mapReg;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// JSON serializer
	static JsonSerializer<MLValStrMap> getJSONSerializer() {
		return new JsonSerializer<MLValStrMap>() {  
		    @Override
		    public JsonElement serialize(MLValStrMap src, Type typeOfSrc, JsonSerializationContext context) {
		        JsonObject jsonV = new JsonObject();
		        
		        JsonElement jsonReg = context.serialize(src.mapReg, HashMap.class);
			    jsonV.add("reg", jsonReg);
			    			    
			    JsonArray jsonMaps = new JsonArray();
	            int cid = 0;
	            for (TIntObjectHashMap<String> map : src.strMaps) {	            	
            		JsonObject jsonMap = new JsonObject();
    			    JsonArray jsonVSs = new JsonArray();
	            	map.forEachEntry((k, v) -> {
	            		// add value string pair
	            		JsonArray jsonVS = new JsonArray();
	            		jsonVS.add(k);
	            		jsonVS.add(v);
	            		jsonVSs.add(jsonVS);
	    	            return true;
	    	        });
	            	jsonMap.addProperty("id", cid);
	            	jsonMap.add("map", jsonVSs);
	            	cid++;
	            	jsonMaps.add(jsonMap);
	            }
			    jsonV.add("maps", jsonMaps);
		        return jsonV;
		    }
		    // "scratchPad":{"inf_default_dtag":"text","inf_default_dptag":"pos"}
		    // "map":[{"220401487":"DT"},{"3780862":"RBS"}
		};
	}
}
