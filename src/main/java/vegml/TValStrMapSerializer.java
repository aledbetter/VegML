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


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.HashMap;
import java.util.List;

/**
 * Kryo Serializer for StrMap class
 */
class TValStrMapSerializer extends Serializer<MLValStrMap> {

    /**
     * 
     */
	@Override
    public void write(Kryo kryo, Output output, MLValStrMap map) {
       
		List<TIntObjectHashMap<String>> dml = map.getDMaps();
		HashMap<String, Integer> regm = map.getRegMap();
		
		output.writeInt(dml.size());
		output.writeInt(regm.keySet().size());
     
        // for each map
        for (int c = 0;c<dml.size();c++) {
        	TIntObjectHashMap<String> tm = dml.get(c);      	
	        output.writeInt(tm.size());
	        if (tm.size() < 1) continue;
	        
	        tm.forEachEntry((k, v) -> {
	            output.writeInt(k);
	            output.writeString(v);
	            return true;
	        });
        }
        
        //kryo.writeObjectOrNull(output, regm, HashMap.class);
    	for (String s:regm.keySet()) {
    		Integer k = regm.get(s);
            output.writeInt(k.intValue());
            output.writeString(s);
        }
    }

    @Override
    public MLValStrMap read(Kryo kryo, Input input, Class type) {

        int dcount = input.readInt();
        int regCnt = input.readInt();

        // Number of entries, always zero or positive.
        MLValStrMap map = new MLValStrMap();
        List<TIntObjectHashMap<String>> dm = map.getDMaps();
        
        for (int c = 0;c<dcount;c++) {
            int size = input.readInt();
            TIntObjectHashMap<String> rm = new TIntObjectHashMap<>();
            dm.add(rm);           
	        for (int i = 0; i < size; i++) {
	            int key = input.readInt();
	            String val = input.readString();
	            rm.put(key, val);
	        }
        }

		HashMap<String, Integer> regm = map.getRegMap();
        for (int i = 0; i < regCnt; i++) {
            int val = input.readInt();
            String key = input.readString();
            regm.put(key, val);
        }

        return map;
    }
}
