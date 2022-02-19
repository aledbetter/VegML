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


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

class MLSerialize {

	/**
	 * Serialize VegML instance to a file
	 * This uses kryo to save the information in a non-standard form; that is fast and small
	 * Alternately the JSON export can be used
	 * 
	 * @param vML VegML instance to save
	 * @param filename filename to save to
	 * @return true if saved, else false
	 */
	static public boolean saveVML(VegML vML, String filename) {
	    Output output = null;
	    FileOutputStream fout = null;
	    Kryo kryo = getKryo();

		try {
			fout = new FileOutputStream(filename, false);
			output = new Output(fout);
			// VegML
			kryo.writeClassAndObject(output, vML);

		} catch (Exception e) {
		    System.out.println("NOT SAVED to[" + filename+ "] " + e.getMessage());
		    e.printStackTrace();
		    return false;
		} finally {
			try {
				output.flush();
				output.close();
				if (fout != null) {
					fout.flush();
					fout.close();
					fout = null;
				}
				output = null;
			} catch (Exception e) {}
		}
		return true;
	}
	
	/**
	 * Load a VegML instance from a file that has a kryo format of an instance
	 * @param filename file to load from
	 * @return VegML instance is loaded, else null
	 */
	static public VegML loadVML(String filename) {
		InputStream si = null;
	    VegML vML = null;
	    int cnt = 0;
	    String msg = null;
		try {
			si = new FileInputStream(filename);		
			Input input = new Input(si);		    	
		    Kryo kryo = getKryo();
		    
		    Object obj = null;
		    do {
		    	obj = null;
		    	try {
				//	System.out.println("Load... " );
		    		obj = kryo.readClassAndObject(input);		    		
		    	} catch (KryoException eg) {
		    		msg = eg.getMessage();
					//System.out.println("  ERROR: loadVML2["+filename+"] exception on read: " + eg.getMessage());
		    		//eg.printStackTrace();
		    	} catch (Throwable e) {
		    		msg = e.getMessage();
					//System.out.println("  ERROR: loadVML["+filename+"] exception on read: " + cnt);
					//e.printStackTrace();
		    	}
		        if (obj != null) {
					//System.out.println("loadVML["+filename+"] obj: " + obj.getClass().getName());
		        	cnt++;	        	
		        	if (obj instanceof VegML) vML = (VegML)obj; 
		        } 
		    } while (obj != null);

		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + filename);
		} catch (Exception e) {
			System.out.println("ERROR Exception " + e.getMessage());
		} finally {
			//System.out.println("END getAllNodesFile["+filename+"] ");
			try {
			    if (si != null) {
			    	si.close();
			    	si = null;
			    }
			} catch (Throwable t) {}
		}	
		if (vML == null) {
			System.out.println("Load Failed file["+filename+"]["+cnt+"] "+msg);
		} else {
			// HACK 
			// must re-link things as references are off in kryo due to its bugs...
			for (int c=0;c<vML.getDataPlaneCount();c++) {
				VDataPlane dp = vML.getDataPlane(c);
				vML.serdpHash(dp);
				//System.out.println("loaded DP["+dp.getDimensionTag()+"/"+dp.getTag()+"]");
				dp.setVegML(vML);
			}
		}
	    return vML;
	}
	
	/**
	 * Get an instance of kryo to serialize/desierialize
	 * 
	 * @return kryo instance
	 */
	private static Kryo getKryo() {
	    Kryo kryo = new Kryo();
	    // must turn off for large.. but then all references to an object are new copies
	    // ISSUE: make sure there are 1 and only 1 links to objects saved
	    kryo.setReferences(false);
	    
	   // Log.set(LEVEL_TRACE);
	   // Log.set(LEVEL_WARN);
	    
	    kryo.register(MLValStrMap.class, new TValStrMapSerializer());	
	    
	    // for trove classes
	    kryo.register(TIntIntHashMap.class, new TIntIntHashMapSerializer());
	    kryo.register(TLongIntHashMap.class, new TLongIntHashMapSerializer());   
	    kryo.register(TIntObjectHashMap.class, new TIntObjectHashMapSerializer());
	    kryo.register(TLongObjectHashMap.class, new TLongObjectHashMapNSVectMapSerializer());
	    
	    return kryo;
	}
}
