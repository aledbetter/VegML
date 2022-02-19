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
import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TIntIntHashMap;


/**
 * Kryo Serializer for Trove primitive int-int hash maps.
 * Based on the corresponding Externalizable implementation from Trove itself.
 *
 * Using optimizePositive on keys and values, serialized NL R5 networks with linkages are 13% smaller.
 * Writing with this serializer is much faster than using the Kryo Externalizable serializer.
 */
class TIntIntHashMapSerializer extends Serializer<TIntIntHashMap> {

    private final boolean optimizePositive = true;


    /**
     * Based on writeExternal in the hierarchy of TIntIntHashMap.
     */
    @Override
    public void write(Kryo kryo, Output output, TIntIntHashMap map) {

        // Do not write load and compaction factors.
        // They're not public and we don't ever modify them in a principled way.

        // No-entry key and value. Often zero or -1 so don't optimize for positive values, do optimize for small values.
        output.writeVarInt(map.getNoEntryKey(), false);
        output.writeVarInt(map.getNoEntryValue(), false);

        // Number of entries, always zero or positive.
        output.writeVarInt(map.size(), true);

        // All entries, most are positive in our application?
        map.forEachEntry((k, v) -> {
            output.writeVarInt(k, optimizePositive);
            output.writeVarInt(v, optimizePositive);
            return true;
        });
    }

    @Override
    public TIntIntHashMap read(Kryo kryo, Input input, Class type) {

        // No-entry key and value. Often zero or -1 so don't optimize for positive values, do optimize for small values.
        int noEntryKey = input.readVarInt(false);
        int noEntryVal = input.readVarInt(false);

        // Number of entries, always zero or positive.
        int size = input.readVarInt(true);

        TIntIntHashMap map = new TIntIntHashMap(size, Constants.DEFAULT_LOAD_FACTOR, noEntryKey, noEntryVal);

        for (int i = 0; i < size; i++) {
            int key = input.readVarInt(optimizePositive);
            int val = input.readVarInt(optimizePositive);
            map.put(key, val);
        }
        return map;
    }
}
