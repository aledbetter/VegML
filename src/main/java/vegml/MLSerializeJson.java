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
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import vegml.Data.VDataSet;
import vegml.Data.VDataSetDescriptor;
import vegml.Data.VDataSets;


class MLSerializeJson {
	static Type DPListType = new TypeToken<List<VDataPlane>>() {}.getType();  
	static Type NSHListType = new TypeToken<List<MLNumberSetHash>>() {}.getType();  


	/**
	 * Get instance of Gson for serialization/deserialization
	 * 
	 * @return instance of gson
	 */
	public static Gson getJSONBuilder() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		// register special serializers
		gsonBuilder.registerTypeAdapter(VegML.class, VegML.getJSONSerializer());		
		gsonBuilder.registerTypeAdapter(VDataPlane.class, VDataPlane.getJSONSerializer());	
		gsonBuilder.registerTypeAdapter(DPListType, DPLSerializer);	
		gsonBuilder.registerTypeAdapter(MLNumberSetHash.class, MLNumberSetHash.getJSONSerializer());	
		gsonBuilder.registerTypeAdapter(NSHListType, HNSLSerializer);	
		gsonBuilder.registerTypeAdapter(MLValStrMap.class, MLValStrMap.getJSONSerializer());	
		
		// datasets
		gsonBuilder.registerTypeAdapter(VDataSetDescriptor.class, VDataSetDescriptor.getJSONSerializer());		
		gsonBuilder.registerTypeAdapter(VDataSetDescriptor.class, VDataSetDescriptor.getJSONDeserializer());				
		gsonBuilder.registerTypeAdapter(VDataSets.class, VDataSets.getJSONSerializer());		
		gsonBuilder.registerTypeAdapter(VDataSets.class, VDataSets.getJSONDeserializer());
		gsonBuilder.registerTypeAdapter(VDataSet.class, VDataSet.getJSONSerializer());		
		gsonBuilder.registerTypeAdapter(VDataSet.class, VDataSet.getJSONDeserializer());		

		return gsonBuilder.create();  
	}
	
	/**
	 * data plane list serializer
	 */
	static JsonSerializer<List<VDataPlane>> DPLSerializer = new JsonSerializer<List<VDataPlane>>() {
        @Override
        public JsonElement serialize(List<VDataPlane> src, Type typeOfSrc, JsonSerializationContext context) {
        	JsonArray jsonDP = new JsonArray();
            for (VDataPlane dp : src) {
    	        JsonElement jsonDPs = context.serialize(dp, VDataPlane.class);
    	        jsonDP.add(jsonDPs);
            }
            return jsonDP;
        }
	};
	
	/**
	 * NumberSetHash list serializer
	 */
	static JsonSerializer<List<MLNumberSetHash>> HNSLSerializer = new JsonSerializer<List<MLNumberSetHash>>() {
        @Override
        public JsonElement serialize(List<MLNumberSetHash> src, Type typeOfSrc, JsonSerializationContext context) {
        	JsonArray jsonDP = new JsonArray();
            for (MLNumberSetHash dp : src) {
    	        JsonElement jsonDPs = context.serialize(dp, VDataPlane.class);
    	        jsonDP.add(jsonDPs);
            }
            return jsonDP;
        }
	};	

	
}
