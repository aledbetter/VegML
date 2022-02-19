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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Stream;

import vegml.Data.VDataSet.RangeTag;
import vegml.Data.VDataSetDescriptor.DSDataType;
import vegml.Data.VDataSetDescriptor.DSSetType;

/**
 * This code is simple, but not clean
 * TODO: cleanup the file format loads to consistent interface
 *
 */
public class VFileUtil {

	//
	// all the languages to access for ConLL
	//
	public static final String conLLSet [][] = {
			 {"UD_Dutch", "nl"},
			 {"UD_Dutch-LassySmall", "nl"},
			 {"UD_German", "de"},
			 {"UD_Latin", "la"},
			 {"UD_Latin-ITTB", "la"},
			 {"UD_Latin-PROIEL", "la"},
			 {"UD_Latvian", "lv"},
			 {"UD_Gothic","got"},
			 {"UD_Ancient_Greek-PROIEL", "grc"},
			 {"UD_Ancient_Greek", "grc"},
			 {"UD_Greek", "el"},
			// {"UD_Slovenian", "sl"},
			// {"UD_Slovenian-SST", "sl"},
			 {"UD_Slovak", "sk"},
			 {"UD_Arabic", "ar"},
			 {"UD_Hebrew", "he"},
			 {"UD_Basque", "eu"},
			 {"UD_English", "en"},
			 {"UD_English-LinES", "en"},
			 {"UD_English-ParTUT", "en"},
			 {"UD_Hindi", "hi"},
			 {"UD_Norwegian-Bokmaal", "no"},
			 {"UD_Norwegian-Nynorsk", "no"},
			 {"UD_Spanish", "es"},
			 {"UD_Spanish-AnCora", "es"},
			 {"UD_Old_Church_Slavonic", "cu"},
			 {"UD_Swedish", "sv"},
			 {"UD_Swedish-LinES", "sv"},
			 {"UD_Bulgarian", "bg"},
			 {"UD_Estonian", "et"},
			 {"UD_Hungarian", "hu"},
			 {"UD_Catalan", "ca"},
			 {"UD_Indonesian", "id"},
			 {"UD_Chinese", "zh"},
			 {"UD_Finnish", "fi"},
			 {"UD_Finnish-FTB", "fi"},
			 {"UD_Irish", "ga"},
			 {"UD_Persian", "fa"},
			 {"UD_Polish", "pl"},
			 {"UD_Turkish", "tr"},
			 {"UD_Croatian", "hr"}, 
			 {"UD_French", "fr"}, 
			 //{"UD_French-ParTUT", "fr"}, 
			 {"UD_French-Sequoia", "fr"},
			 {"UD_Italian", "it"}, 
			// {"UD_Italian-ParTUT", "it"},  // no gold
			 {"UD_Danish", "da"},
			 {"UD_Portuguese", "pt"},
			 {"UD_Portuguese-BR", "pt"},
			 {"UD_Ukrainian", "uk"},
			 {"UD_Japanese", "ja"},
			 {"UD_Urdu", "ur"},
			 {"UD_Romanian", "ro"},
			 {"UD_Uyghur", "ug"},
			 {"UD_Czech", "cs"},
			 {"UD_Czech-CLTT", "cs"},
			 {"UD_Czech-CAC", "cs"},
			 {"UD_Galician", "gl"},
			 {"UD_Galician-TreeGal", "gl"},
			 {"UD_Kazakh", "kk"},
	 		 {"UD_Korean", "ko"},
			 {"UD_Russian", "ru"},
			 {"UD_Russian-SynTagRus", "ru"},
			 {"UD_Vietnamese", "vi"}
		};

	/**
	 * delete a file or directory (if empty)
	 * @param fn
	 */
	public static void delFile(String fn) {
		  try{
		    File myObj = new File(fn); 
		    myObj.delete();
		  } catch (Throwable e) {
		  }
	}
	static boolean deleteDirectoryR(File directoryToBeDeleted) {
	    File[] allContents = directoryToBeDeleted.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	        	deleteDirectoryR(file);
	        }
	    }
	    return directoryToBeDeleted.delete();
	}
	
	/**
	 * Recersive file and directory delete
	 * @param fn
	 */
	public static void delDirRecursive(String fn) {
		  try{
			 deleteDirectoryR(new File(fn));
		  } catch (Throwable e) {
		  }
	}
	
	/**
	 * make a directory if it does not exist
	 * @param path
	 */
    public static void makeDir(String path){
        File directory = new File(path);
        if (!directory.exists()){
            directory.mkdir();
        }
    }
    
    /**
     * copy file from path to topath
     * @param path
     * @param toPath
     */
    public static void copyFile(String path, String toPath)  {   		 
    	Path from = Paths.get(path);
    	Path to = Paths.get(toPath);
    	try {
			Files.copy(from, to);
		} catch (IOException e) {
			
		} 
    }
    
	/**
	 * Check if file exists
	 * @param path
	 * @return true if exists
	 */
    public static boolean fileExists(String path) {
        try {
    	   File myObj = new File(path); 
           return myObj.exists();
 		} catch (Throwable e) {          
        }
        return false;
    }
    
    /**
     * Check if directory exists
     * @param path
     * @return true if exists and is directory
     */
    public static boolean dirExists(String path) {
        try {
    	   File myObj = new File(path); 
           return (myObj.exists() && !myObj.isFile());
 		} catch (Throwable e) {          
        }
        return false;
    }   
	//
	public static List<String> tokenizeKeepOnlyWords(List<String> tokens) {
		Iterator<String> sl = tokens.iterator();
		while (sl.hasNext()) {
			String s = sl.next();
			if (s.equals(".") || s.equals("!") || s.equals("?")) continue;
			else if (!s.matches("[a-zA-Z]+")) sl.remove();
		}
		return tokens;
	}
	// flip it
	public static List<String> tokenizeReverse(List<String> tokens) {
		List<String> al = new ArrayList<>();
		
		for (int i = (tokens.size()-1);i>=0;i--) al.add(tokens.get(i));
		
		return tokens;
	}
	// flip it
	public static List<List<String>> tokenizeReverseSet(List<List<String>> tokensSets) {
		List<List<String>> sl = new ArrayList<>();
		for (List<String> tokens:tokensSets) {
			sl.add(tokenizeReverse(tokens));
		}
		return sl;
	}
	
	// bad tokenizer... should use sedro parser
	public static List<String> tokenizeString(String data, boolean cleanAbit, boolean startEnd) {
		return tokenizeString(data, cleanAbit, startEnd, false);
	}
	public static List<String> tokenizeString(String data, boolean cleanAbit, boolean startEnd, boolean lowerFirst) {
		if (data == null || data.length() < 1) return null;
		String twsp[] = data.split("\\s+"); // bad.. want to keep the newlines/tabs/others ??
		if (twsp.length < 1) return null;
		
		List<String> dataSet = new ArrayList<>(twsp.length);
		String last = null;
		for (int i=0;i<twsp.length;i++) {
			if (twsp[i] == null || twsp[i].isEmpty()) continue;
			if (i == 0) twsp[i] = twsp[i].toLowerCase();
			else if (lowerFirst) {
				if (last != null && (last.startsWith("./") || last.startsWith("!/") || last.startsWith("?/"))) twsp[i] = twsp[i].toLowerCase();
				else if (i == 0) twsp[i] = twsp[i].toLowerCase();
			}
			String s = twsp[i].trim();
			if (s.endsWith("/nil")) { // 156 nill tagged words - drop
				//System.out.println(" THIS["+s+"] " + ccnt++);
				continue;
			}
			dataSet.add(s);
			last = s;			
		}
		//List<String> dataSet = new ArrayList<>(Arrays.asList(twsp));
		if (cleanAbit) {
			for (int i=0;i<dataSet.size();i++) {
				String xs = dataSet.get(i);
				if (xs.length() < 2) continue;
				char lc = xs.charAt(xs.length()-1);
				char lc1 = xs.charAt(xs.length()-2);
				switch (lc) {
				case '!':
				case '?':
				case '"':
				case '[':
				case ']':
				//case '(':
				case '.':
					if (Character.isLetterOrDigit(lc1)) {
						dataSet.set(i, xs.substring(0, xs.length()-1));
						dataSet.add(i+1, ""+lc);
					}
					break;
					
				case ',':
					if (Character.isLetterOrDigit(lc1)) {
						dataSet.set(i, xs.substring(0, xs.length()-1));
						dataSet.add(i+1, ",");
					}
					break;
				}
			}
		}
		if (startEnd) {
			dataSet.add(0, "<SOF>");
			dataSet.add("<EOF>");
		}
		return dataSet;
	}
	public static List<String> normalizePosString(List<String> data, boolean justUpper, boolean simplify, boolean universal) {
		for (int i=0;i<data.size();i++) {
			String s = data.get(i).toUpperCase();
			data.set(i, s);
			if (justUpper) continue;
			
			if (s.startsWith("FW-")) {
				s = s.replace("FW-", "");
				//s = "FW";
				data.set(i, s);			
			}
			
			if (s.endsWith("-HL")) {
				s = s.replace("-HL", "");
				data.set(i, s);					
			}

			int idx = s.indexOf("-");
			if (idx >= 0 && !s.equals("--")) {
				s = s.substring(0, idx);
				data.set(i, s);
			}
			
			if (s.length() > 1 && s.endsWith("*")) {
				s = s.replace("*", "");
				data.set(i, s);					
			}
			
			idx = s.indexOf("+");
			if (idx >= 0) {
				s = s.substring(0, idx);
				data.set(i, s);
			}

			if (universal || simplify) {
				s = getMappedPOS(s);
				data.set(i, s);	
			}
			if (universal) {
				s = getMappedUniversalPOS(s);
				data.set(i, s);	
			}
		}
		return data;
	}
	
	// split the tokens into 2
	public static List<List<String>> tokenizeString(List<String> data, String spliter) {
		if (data == null) return null;
		List<String> vals = new ArrayList<>(data.size());
		for (int i=0;i<data.size();i++) {
			String s = data.get(i);
			if (s == null) {
				vals.add(null);
				continue;
			}
			int idx = s.lastIndexOf(spliter);
			if (idx < 0) {
				vals.add(null);
				continue;
			}
			
			String tok = s.substring(0, idx);
			String tag = s.substring(idx+1);
			data.set(i, tok);
			vals.add(tag);
			
		}
		List<List<String>> out = new ArrayList<>();
		out.add(data);
		out.add(vals);
		return out;
	}
	
	
	//
	// segment dataSets to sentences
	// This will not work for universal tags!! as end of sentence has no designation
	//
	public static VDataSets dataSetsToSentence(VDataSets dss) {
		VDataSets sds = new VDataSets(dss.getDefinition(), null);
		
		Set<String> vset = new HashSet<>();
		List<RangeTag> arl = new ArrayList<>();
		int offset = 0;
		
		for (int x=0;x<dss.size();x++) {
			VDataSet ds = dss.get(x);

			List<String> sent = new ArrayList<>();
			List<String []> sentVal = new ArrayList<>();
			arl.clear();
			
			boolean dfmt = ds.isFmtValueD();
			offset = 0;
			
			for (int i=0;i<ds.size();i++) {
				String [] tag = null;
				// assume strings..
				if (dfmt) {
					tag = ds.getValueSD(i);
				} else {
					tag = new String[1];
					tag[0] = ds.getValueS(i);
				}
				String text = ds.getDataS(i);
				
	//			if (text.equals(".") && !tag[0].equals("PE")) System.out.println("SENT["+text+"]["+tag[0]+"] => ");
	//			if (text.equals(".")) System.out.println("SENT["+text+"]["+tag[0]+"] => ");
	//			if (tag[0].equals("PE")) System.out.println("SENT["+text+"]["+tag[0]+"] => ");

				
				sent.add(text);
				sentVal.add(tag);
				vset.add(tag[0]);
// FIXME issues getting sentence rangeTags needs work		
				List<RangeTag> rl = ds.getRangeStart(i);
				if (rl != null) arl.addAll(rl);

				boolean end = false;
				String etype = null;
				rl = ds.getRangeEnd(i);
				if (rl != null) {
					for (int z=0;z<rl.size();z++) {
						RangeTag rt = rl.get(z);
						
						if (rt.getValuesS()[0].equals("S")) {
							// if first tag is S
							end = true;
							etype = rt.getValuesS()[0];
							if (rt.getLength() == 1)  { // generally a new line
								//System.out.println("RT-S[@"+i+"]["+rt.getLength()+"]["+etype+"] => ["+text+"][" +rt.getDataAsString()+"] " + ds.size());
							}
							break;	
						}
						/*							 
						} else if (rt.getValuesS()[0].equals("P") || rt.getValuesS()[0].equals("TK") || rt.getValuesS()[0].equals("FUNC")) {
							// BUG sedro TK depth=0
							// FUNC / TK
						} else if (rt.getDepth() == 0) {
							// S/CODE/INTJ/NP.. save the end
							end = true;
							etype = rt.getValuesS()[0];
							System.out.println("RT-D["+rt.getLength()+"]["+etype+"] => " +rt.getDataAsString());
							break;
						}*/
					}					
				}
				if (!end) {
					if (tag[0].equals(".") || tag[0].equals("PE")) {
						end = true;  // END // END (ext sedro tag)
						etype = tag[0];
					}
				}
				
				if (end) {
					VDataSet nvd = new VDataSet();
					nvd.setDataLS(sent);
					nvd.setValueLSD(sentVal);
					sds.add(nvd);
					
					//System.out.println("SENT["+nvd.size()+"]["+etype+"] => " +nvd.getDataAsString());

					sent = new ArrayList<>();
					sentVal = new ArrayList<>();
					for (RangeTag rt:arl) nvd.addRange(rt, -offset);
					offset = i+1;
					arl.clear();
				} 
			}
			if (sent.size() > 0) {
				VDataSet nvd = new VDataSet();
				nvd.setDataLS(sent);
				nvd.setValueLSD(sentVal);
				sds.add(nvd);
				for (RangeTag rt:arl) nvd.addRange(rt, -offset);
				arl.clear();
			}
		}
		
		if (sds.getDefinition() == null) {
			// add the tags
			VDataSetDescriptor dsd = new VDataSetDescriptor();
			for (String v:vset) dsd.addDataTag(v, DSSetType.Open, DSDataType.String, null); // add tags
			sds.setDefinition(dsd);
		}
		// set same split ratio
		double trp = ((double)dss.getTrainCount()/(double)dss.size())*100;
		double tup = ((double)dss.getTuneCount()/(double)dss.size())*100;
		double tsp = ((double)dss.getTestCount()/(double)dss.size())*100;
		sds.setSplitPercent(trp, tup, tsp);
		
		return sds;
	}
	public static void dataSetsToSentence(List<List<String>> posList, List<List<String>> posValList) {
		
		List<List<String>> nList = new ArrayList<>();
		List<List<String>> nValList = new ArrayList<>();
		
		// same.. broken out by sentence
		for (int i=0;i<posList.size();i++) {
			 List<String> segTok = posList.get(i);
			 List<String> segVal = posValList.get(i);
			 
			 List<String> sent = new ArrayList<>();
			 List<String> sentVal = new ArrayList<>();
			 int e = -1;
			 for (int x=0;x<segTok.size();x++) {
				 if (segVal.get(x).equals(".")) {
					 e = x;
					 sent.add(segTok.get(x));
					 sentVal.add(segVal.get(x));
				 } else {
					 if (e >= 0) {				 
						 nList.add(sent);
						 nValList.add(sentVal);
						 sentVal = new ArrayList<>();
						 sent = new ArrayList<>();
					 } 
					 sent.add(segTok.get(x));
					 sentVal.add(segVal.get(x));					 
				 }
			 }
			 if (sent.size() > 0) {
				 nList.add(sent);
				 nValList.add(sentVal);
			 }
		}
		// switch data
		posList.clear();
		posList.addAll(nList);
		posValList.clear();
		posValList.addAll(nValList);
	}
	

	// load text file directory
	public static List<String> loadTextFiles(String directory) {
		return loadTextFiles(directory, -1, -1);
	}
	public static List<String> loadTextFiles(String directory, int cur, int max) {
		List<String> fnl = VFileUtil.fileList(directory);
		if (fnl == null || fnl.size() < 1) return null;
		List<String> dl = new ArrayList<>();
		for (String fn:fnl) {
			if (max >= 0 && (cur + dl.size()) >= max) return dl;
			if (fn.startsWith(".")) continue;
			if (fn.equalsIgnoreCase("README")) continue;
			if (fn.equalsIgnoreCase("CONTENTS")) continue;
			String data = loadTextFile(directory+"/"+fn);
			if (data != null) dl.add(data);
		}
		return dl;
	}
	
	// load text files  and return filename
	public static List<List<String>> loadTextFilesAndName(String directory) {
		List<String> fnl = VFileUtil.fileList(directory);
		if (fnl == null || fnl.size() < 1) return null;
		List<String> dl = new ArrayList<>();
		List<String> nl = new ArrayList<>();
		for (String fn:fnl) {
			if (fn.startsWith(".")) continue;
			if (fn.equalsIgnoreCase("README")) continue;
			if (fn.equalsIgnoreCase("CONTENTS")) continue;
			//System.out.println("FILE: " + fn);
			String data = loadTextFile(directory+"/"+fn);
			if (data != null) {
				dl.add(data);
				nl.add(fn);
			} 
		}
		List<List<String>> ddl = new ArrayList<>();
		ddl.add(dl);
		ddl.add(nl);
		return ddl;
	}
	
	
	// load text file
	public static String loadTextFile(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            try {
        	stream.forEach(s -> contentBuilder.append(s).append("\n"));
            } catch (Throwable t) {
            	// error loading data
        		System.out.println("ERROR: file["+filePath+"]");
            }
        } catch (IOException e) {
            System.err.println("ERROR >> file not found["+filePath+"]: " + e.getMessage());
        }
        return contentBuilder.toString();
    }
	
	// load treebank file
	/*
		 ----tagged----
	[ Pierre/NNP Vinken/NNP ]
	,/, 
	[ 61/CD years/NNS ]
	old/JJ ,/, will/MD join/VB 
	[ the/DT board/NN ]
	as/IN 
	[ a/DT nonexecutive/JJ director/NN Nov./NNP 29/CD ]
	./. 
	======================================

	 */
	public static List<List<String>> loadTreebankFiles(String directory, boolean includeBreaks) {
		List<String> fnl = VFileUtil.fileList(directory);
		if (fnl == null || fnl.size() < 1) return null;

		List<List<String>>  dl = new ArrayList<>();
		for (String fn:fnl) {
			if (fn.startsWith(".")) continue;
			if (fn.equalsIgnoreCase("README")) continue;
			List<String> tokens = loadTreebankFile(directory+"/"+fn, includeBreaks);
			if (tokens != null) dl.add(tokens);
		}
		return dl;
	}
	public static List<String> loadTreebankFile(String filePath, boolean includeBreaks) {	
		String line = "";
		List<String> tok = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
            	if (line.length() < 2) {
            		//if (includeBreaks && tok.size() > 0) {
            			//String last = tok.get(tok.size()-1);
            			//if (!last.startsWith("<==")) tok.add("<===NNN>/NLBR");
            		//}
          			continue;
            	}
            	if (line.startsWith("===")) {
					if (includeBreaks) {
	            		//if (tok.size() > 0) {
	            		//	String last = tok.get(tok.size()-1);
	            		//	if (last.startsWith("<===NNN>")) tok.set(tok.size()-1, "<===NNN>/NLBR");
	            		//	else tok.add("<====>/BR");
	            		//} else {
	            			tok.add("<====>/BR");            		
					}
					continue;
            	}
				String sset [] = line.split("\\s+");
				for (int i=0;i<sset.length;i++) {
					if (sset[i].contains("/")) {
						// what about new lines?
						tok.add(sset[i].trim());
					} else if (includeBreaks) {
						if (sset[i].equals("[")) {
							tok.add("<===SSS>/SBR");
						} else if (sset[i].equals("]")) {
							tok.add("<===CCC>/CBR");
						}
					}
				}
            }
        } catch (IOException e) {
    		System.out.println("ERROR fixFile["+filePath+"] " + e.getMessage());
        }
        // make sure ... there isn't perfect consistancy in the files...
		if (includeBreaks && !tok.get(0).equals("<====>/BR")) {
			tok.add(0, "<====>/BR");
		}
		if (includeBreaks && !tok.get(tok.size()-1).equals("<====>/BR")) {
			//if (tok.get(tok.size()-1).startsWith("<===NNN>")) tok.set(tok.size()-1, "<===NNN>/NLBR");
			//else tok.add("<====>/BR");
			tok.add("<====>/BR");
		}
        return tok;
    }
	// load mrg files
	/*
	( (S 
	    (NP-SBJ 
	      (NP (NNP Pierre) (NNP Vinken) )
	      (, ,) 
	      (ADJP 
	        (NP (CD 61) (NNS years) )
	        (JJ old) )
	      (, ,) )
	    (VP (MD will) 
	      (VP (VB join) 
	        (NP (DT the) (NN board) )
	        (PP-CLR (IN as) 
	          (NP (DT a) (JJ nonexecutive) (NN director) ))
	        (NP-TMP (NNP Nov.) (CD 29) )))
	    (. .) ))
	 */
	public static List<Object []> loadTreebankMrgFiles(String directory) {
		List<String> dnl = VFileUtil.fileDirList(directory);
		if (dnl == null || dnl.size() < 1) return null;
		List<Object []>  dl = new ArrayList<>();
		
		for (String dn:dnl) {
			String dir = directory+"/"+dn;
			List<String> fnl = VFileUtil.fileList(dir);
			if (fnl == null || fnl.size() < 1) continue;
	
			for (String fn:fnl) {
				//System.out.println("OOL["+dir+"/"+fn+"] " );
				if (fn.startsWith(".")) continue;
				if (fn.startsWith("readme")) continue;
				if (!fn.endsWith(".mrg")) continue;
				Object [] rs = loadTreebankMrgFile(dir+"/"+fn);
	
				if (rs != null) dl.add(rs);
			}
		}
		return dl;
	}
	
/*

( @0y0012sx-a-11/CD )
( END_OF_TEXT_UNIT )
( (S 
    (NP-SBJ * /XXX )
    (VP List/VB 
      (NP 
        (NP the/DT flights/NNS )
        (PP-DIR from/IN 
          (NP Baltimore/NNP ))
        (PP-DIR to/TO 
          (NP Seattle/NNP ))
        (SBAR 
          (WHNP-1 that/WDT )
          (S 
            (NP-SBJ *T*-1/XXX )
            (VP stop/VBP 
              (PP-LOC in/IN 
                (NP Minneapolis/NNP )))))))))
( END_OF_TEXT_UNIT )
( @0y0022sx-d-5/CD )
( END_OF_TEXT_UNIT )
( (SQ Does/VBZ 
    (NP-SBJ this/DT flight/NN )
 */
	// token/POS list
	// obj/TAG list <start/end>
	public static class ThingTag {
		public int start, end, depth;
		public String tag;
		public String val;
	}
	public static Object [] loadTreebankMrgFile(String filePath) {
		String data = loadTextFile(filePath);
		if (data == null) return null;
		
		List<String> tokl = new ArrayList<>();
		List<String> tagl = new ArrayList<>();
		List<List<String>> lls = new ArrayList<>();
		lls.add(tokl);
		lls.add(tagl);
		List<ThingTag> tls = new ArrayList<>();
		
		// scan for ( OR )
		List<ThingTag> tstack = new ArrayList<>();
		ThingTag top = null;
		StringBuilder sb = new StringBuilder();
		
		for (int i=0;i<data.length();i++) {
			char c = data.charAt(i);
			if (c == '(') {
				top = new ThingTag();
				top.start = tokl.size();
				top.end = -1;
				top.val = top.tag = null;
				top.depth = tstack.size();
				tstack.add(top);
				
			} else if (c == ')') {
				// end string (if any)
				String s = null;
				if (sb.length() > 0) {				
					s = sb.toString();
					sb = new StringBuilder(); 
				}
				
				if (top.tag != null && top.start == tokl.size() && (top.tag.equals("-NONE-") || top.tag.equals("-DFL-"))) {
					// special with value: "-NONE-" add to value parent..
				//	ThingTag t = tstack.get(tstack.size()-2);
				//	t.val = s;
					top.end = tokl.size();	
					top.val = s;
					tls.add(top);
				} else if (top.tag != null && top.start == tokl.size() && (top.tag.equals("-SP-") || top.tag.equals("-NL-"))) {
					// special with value
					top.end = tokl.size();	
					top.val = s;
					tls.add(top);
				} else if (top.start == tokl.size() && s != null) {
					// is this just a token ?
					tokl.add(s);
				// FIXME OR tags need addressed
					tagl.add(top.tag);
				} else {
					// add thing				
					top.end = tokl.size()-1;	
					if (s != null) top.val = s; // value before close (classifier)
					if (top.tag == null) top.tag = "BS";
					tls.add(top);
				}
				
				// remove from stack
				tstack.remove(tstack.size()-1);
				top = null;
				if (tstack.size() > 0) top = tstack.get(tstack.size()-1);
				
			} else if (Character.isWhitespace(c)) {
				if (sb.length() > 0) {
					// this is a TAG
					String s = sb.toString();
					sb = new StringBuilder();
					if (top != null) {
						top.tag = s;
					} else {
					//	System.out.println("ERR["+i+"] ["+s+"]["+filePath+"] " + tstack.size());
					}
				}
			} else {
				sb.append(c);
			}
		}
			
		Object [] rls = new Object[2];
		rls[0] = lls;
		rls[1] = tls;
		return rls;
	}
	
	/*
	1	From	from	ADP	IN	_	3	case	_	_
	2	the	the	DET	DT	Definite=Def|PronType=Art	3	det	_	_
	3	AP	AP	PROPN	NNP	Number=Sing	4	obl	_	_
	4	comes	come	VERB	VBZ	Mood=Ind|Number=Sing|Person=3|Tense=Pres|VerbForm=Fin	0	root	_	_
	5	this	this	DET	DT	Number=Sing|PronType=Dem	6	det	_	_
	6	story	story	NOUN	NN	Number=Sing	4	nsubj	_	_
	7	:	:	PUNCT	:	_	4	punct	_	_
	 */
	public static List<List<List<String>>> loadUDPipeFiles(String directory, String extension) {
		List<String> fnl = VFileUtil.fileList(directory);
		if (fnl == null || fnl.size() < 1) return null;
		List<List<List<String>>> datal = new ArrayList<>();
		for (String fn:fnl) {
			if (!fn.endsWith(extension)) continue;
			List<List<String>> data = loadUDPipeFile(directory+"/"+fn);
			if (data != null) datal.add(data);
		}
		return datal;
	}
	// token/POS-U/POS-penntree/inf/lemma
	public static List<List<String>> loadUDPipeFile(String filePath) {	
		String line = "";
		List<String> tok = new ArrayList<>();
		List<String> posu = new ArrayList<>();
		List<String> posx = new ArrayList<>();
		List<String> inf = new ArrayList<>();
		List<String> lemma = new ArrayList<>();
		//System.out.println(" File => " + filePath);

		int cnt = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
            	if (line.length() < 2) continue;
            	if (line.startsWith("# sent_id")) {
            		continue;
            	}
            	if (line.startsWith("# text")) {
            		continue;
            	}
            	if (line.startsWith("#")) continue;
            	//ID, FORM, LEMMA, UPOS, XPOS, FEATS, HEAD, DEPREL, DEPS, MISC = range(10)
				String sset [] = line.split("\t");
				//num word lemma G_POS P_POS <specific>
				//System.out.println(" X["+sset[1]+"/"+sset[2]+"] pos["+sset[3]+"/"+sset[4]+"] inf["+sset[5]+"] => " + line);
				tok.add(sset[1].trim());
				if (sset[2].trim().equals("_")) lemma.add(""); // empty
				else lemma.add(sset[2].trim());
				//if (sset[3].equals("_")) System.out.println(" X["+sset[1]+"/"+sset[2]+"] pos["+sset[3]+"/"+sset[4]+"] inf["+sset[5]+"] => " + line);
				if (sset[3].trim().equals("_")) posu.add(""); // empty
				else posu.add(sset[3].trim());
				if (sset[4].trim().equals("_")) posx.add(""); // empty
				else posx.add(sset[4].trim());
				if (sset[5].trim().equals("_")) inf.add(""); // empty
				else inf.add(sset[5].trim());
				cnt++;
            }

        } catch (IOException e) {
    		System.out.println("ERROR fixFile["+filePath+"] " + e.getMessage());
        }
        if (cnt <= 0) return null;
        List<List<String>> dl = new ArrayList<>();
        dl.add(tok);
        dl.add(posu);
        dl.add(posx);
        dl.add(inf);
        dl.add(lemma);
        return dl;
    }
	

    // get directory file list
    public static List<String> fileDirList(String dirName) {
    	List<String> fl = new ArrayList<>();
    	
        File fileName = new File(dirName);
        File[] fileList = fileName.listFiles();
        if (fileList == null) return null;
        for (File file: fileList) fl.add(file.getName());
        Collections.sort(fl);
        return fl;
    }
    public static List<String> fileList(String dirName) {    	
        File fileName = new File(dirName);
        File[] fileList = fileName.listFiles();
        if (fileList == null) return null;
        
    	List<String> fl = new ArrayList<>();
    	for (File file: fileList) {
        	if (file.isFile()) fl.add(file.getName());
        }
      	Collections.sort(fl);
        return fl;
    }
    public static List<String> directoryList(String dirName) {
    	List<String> fl = new ArrayList<>();
    	
        File fileName = new File(dirName);
        File[] fileList = fileName.listFiles();
        for (File file: fileList) {
        	if (file.isDirectory()) fl.add(file.getName());
        }
        Collections.sort(fl);
        return fl;
    }
	public static String getFileExtension(String fileName) {
		if (!fileName.contains(".")) return "txt";
		return fileName.substring(fileName.lastIndexOf('.') + 1);
	}
	
	
	// as lines
	// or as tokens
    public static void writeListToFile(List<String> list, String path, boolean asTokens) {
    	if (list.size() < 1) return;
    
        FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(path);
		} catch (IOException e2) {
			System.err.println("ERROR >> opening file[" + path +"] " + e2.getMessage());	
			return;
		}
		
		try {
			PrintWriter printWriter = new PrintWriter(fileWriter); 
	        for (String s:list) {
	        	if (asTokens) {
		        	printWriter.print(s);
		        	printWriter.print(" ");	        		
	        	} else {
		        	printWriter.println(s);	        		
	        	}
	        }       
	        printWriter.close();
		} catch (Throwable t) {
			System.err.println("ERROR >> save file[" + path +"] " + t.getMessage());
		}
    }  
    
	// as csv
	// or as tsv
    public static void writeListToTable(List<String []> list, String path, boolean asTsv) {
    	if (list.size() < 1) return;
    
        FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(path);
		} catch (IOException e2) {
			System.err.println("ERROR >> opening file[" + path +"] " + e2.getMessage());	
			return;
		}
		
		try {
			PrintWriter printWriter = new PrintWriter(fileWriter); 
	        for (String [] sl:list) {
	        	for (int i=0;i<sl.length;i++) {
		        	printWriter.print(sl[i]);
		        	if (i != (sl.length-1)) {
		        		if (asTsv)printWriter.print("\t");	        		
		        		else printWriter.print(",");
		        	} else {
		        		printWriter.print("\r\n");
		        	}
	        	}
	        }       
	        printWriter.close();
		} catch (Throwable t) {
			System.err.println("ERROR >> save file[" + path +"] " + t.getMessage());
		}
    }  
    
    //
    // Write data to a file
    //
    public static void writeTextToFile(String path, String content) {
	    BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(path));
		    writer.write(content);	
		    writer.close();
		} catch (IOException e) {
			//e.printStackTrace();
		}		   
    }
    
    //
    // load text from files
    //
	public static int loadTextFilesToTokens(String dirname, List<List<String>> dsList, int max) {
		dsList.clear();
		int cnt = 0;
		
		List<String> fileSet = new ArrayList<>();
		List<String> fileSetb =  VFileUtil.loadTextFiles(dirname, fileSet.size(), max);		
		if (fileSetb != null) fileSet.addAll(fileSetb);
		// recursive 
		List<String> dlist = VFileUtil.directoryList(dirname);
		for (String dir:dlist) {
			if (fileSet.size() > max) break;
			List<String> fs =  VFileUtil.loadTextFiles(dirname+"/"+dir, fileSet.size(), max);		
			fileSet.addAll(fs);
		}
		System.out.println("Files["+fileSet.size()+"] loaded... tokenizing");
		Iterator<String> it = fileSet.iterator();
		while (it.hasNext()) {
			String s = it.next();
			it.remove();
			List<String> tks = VFileUtil.tokenizeStringText(s);
			if (tks == null || tks.size() < 10) continue;
			dsList.add(tks);	
			cnt += tks.size();
		} 
		return cnt;
	}
	public static List<String> tokenizeStringText(String data) {
		if (data == null || data.length() < 1) return null;
		String twsp[] = data.split("\\s+"); // bad.. want to keep the newlines/tabs/others ??
		if (twsp.length < 1) return null;
		
		List<String> dataSet = new ArrayList<>(twsp.length);
		String last = null;
		for (int i=0;i<twsp.length;i++) {
			if (twsp[i] == null || twsp[i].isEmpty()) continue;
			String s = twsp[i].trim();
			if (last != null && s.equals("@") && last.equals("@")) {
				continue;
			}
			if (s.length() > 1) {
				if (s.length() > 2) {
					s = s.replace("_", " ").trim();
					if (s.contains(" ")) s = s.replace("", "-"); // this_is_it -> this-is-it
					s = s.replace("&amp;", "&");
					s = s.replace("&amp", "&");
					s = s.replace("&nbsp;", "");
					s = s.replace("&nbsp", "");
					s = s.replace("&lt;", "<");
					s = s.replace("&lt", "<");
					s = s.replace("&gt;", ">");
					s = s.replace("&gt", ">");
					if (s.length() < 1) continue;
					if (s.equals("<p>") || s.equals("</p>") || s.equals("</h>") || s.equals("<h>") || s.equals("</b>") || s.equals("<b>")|| s.equals("<br>")) { 
						continue;
					}
				}
				if (s.startsWith("@") || s.startsWith("#") || s.startsWith("@@") || s.startsWith("@!") || s.startsWith("**")
						|| s.startsWith("---") || s.startsWith("==")|| s.startsWith("</")) { 
					//System.out.println(" THIS["+s+"] " + ccnt++);
					continue;
				}
		
				// issue for taggers
				// " -> '' OR ``
				
				//ends with ./,/?/!/"/%/)/}/]/:/;
				//starts with "/'/$/(/{/[
				char c = s.charAt(0);
				switch (c) {
				case '\'':
					if (s.equals("'s") || s.equals("'d") || s.equals("'ll") || s.equals("'ve") || s.equals("'re") || s.equals("'m") || s.equals("'ol")) break;
				case '$':
				case '*':
				case '/':
				case '{':
				case '(':
				case '<':
				case '[':
					dataSet.add(""+c);
					s = s.substring(1, s.length());
					break;
				case '"':
					dataSet.add("``");
					s = s.substring(1, s.length());
					break;
				}
				if (s.length() < 1) continue;
				if (s.length() > 1) {
					c = s.charAt(s.length()-1);
					switch (c) {
					case '.':
					case '?':
					case ':':
					case ';':
					case '\'':
					case '!':
					case ',':
					case '/':
					case '%':
					case ')':
					case '}':
					case '>':
					case ']':
						s = s.substring(0, s.length()-1);
						dataSet.add(s);
						s = ""+c;
						break;
					case '"':
						s = s.substring(0, s.length()-1);
						dataSet.add(s);
						s = "''";
						break;
					}
				}
			}
			
			/* - file headers
			 *  - some ##3283052 <h>  files
				- @ @ @ @ @ @ @ @ @ @ 
				- <p> **29;2259;TOOLONG
				- @@12747079 @1347079/ <h> 	
				- @343   @!James:		
				*/
			if (s.length() < 1) continue;
			dataSet.add(s);
			last = s;
		}

		return dataSet;
	}
    
	
	public static VDataSets loadDataSetsDS(DataSetType dtype, String filename, double percentTune, double percentTest) {
		List<List<String>> dsList = new ArrayList<>();
		List<List<String>> dsValList = new ArrayList<>();
		List<List<String[]>> dsValList2 = new ArrayList<>();
		List<List<String>> dsTuneList = new ArrayList<>();
		List<List<String>> dsTuneValList = new ArrayList<>();	
		List<List<String[]>> dsTuneValList2 = new ArrayList<>();	
		List<List<String>> dsTestList = new ArrayList<>();
		List<List<String>> dsTestValList = new ArrayList<>();
		List<List<String[]>> dsTestValList2 = new ArrayList<>();
		List<List<String>> ds2TestList = new ArrayList<>();
		List<List<String>> ds2TestValList = new ArrayList<>();
		List<List<String[]>> ds2TestValList2 = new ArrayList<>();
		Set<String> vset = new HashSet<>();
		
		boolean single = false;
		List<List<ThingTag>> dsTl = new ArrayList<>();
		List<List<ThingTag>> dsTuneTl = new ArrayList<>();
		List<List<ThingTag>> dsTestTl = new ArrayList<>();
		VFileUtil.loadDataSets(dtype, filename, dsList, dsValList, dsValList2, 
				dsTuneList, dsTuneValList, dsTuneValList2, 
				dsTestList, dsTestValList, dsTestValList2, 
				ds2TestList, ds2TestValList, ds2TestValList2, vset, 0, 0, dsTl, dsTuneTl, dsTestTl);			

		// add the tags
		VDataSetDescriptor dsd = new VDataSetDescriptor();
		for (String v:vset) dsd.addDataTag(v, DSSetType.Open, DSDataType.String, null); // add tags
		
		VDataSets ds = new VDataSets(dsd, null);
		// add data
		for (int i=0;i<dsList.size();i++) {
			VDataSet d = new VDataSet();
			d.setDataLS(dsList.get(i));
			if (single)	d.setValueLS(dsValList.get(i));		
			else d.setValueLSD(dsValList2.get(i)); 
			ds.add(d);
			if (i>=dsTl.size()) continue;
			List<ThingTag> tl = dsTl.get(i);
			for (ThingTag tt:tl) {
				String [] a = null;
				if (tt.val != null) {
					a = new String[2];
					a[1] = tt.val;
				} else {
					a = new String[1];
				}
				a[0] = tt.tag;
				d.addRange(tt.start, tt.end, tt.depth, a);
			}
			d.sortRangesLongestFirst();
		}
		int train = ds.size();
		for (int i=0;i<dsTuneList.size();i++) {
			VDataSet d = new VDataSet();
			d.setDataLS(dsTuneList.get(i));
			if (single)	d.setValueLS(dsTuneValList.get(i));		
			else d.setValueLSD(dsTuneValList2.get(i)); 
			ds.add(d);
			if (i>=dsTuneTl.size()) continue;
			List<ThingTag> tl = dsTuneTl.get(i);
			for (ThingTag tt:tl) {
				String [] a = null;
				if (tt.val != null) {
					a = new String[2];
					a[1] = tt.val;
				} else {
					a = new String[1];
				}
				a[0] = tt.tag;
				d.addRange(tt.start, tt.end, tt.depth, a);
			}
			d.sortRangesLongestFirst();
		}
		int tune = ds.size();
		for (int i=0;i<dsTestList.size();i++) {
			VDataSet d = new VDataSet();
			d.setDataLS(dsTestList.get(i));
			if (single)	d.setValueLS(dsTestValList.get(i));		
			else d.setValueLSD(dsTestValList2.get(i)); 
			ds.add(d);
			
			if (i>=dsTestTl.size()) continue;
			List<ThingTag> tl = dsTestTl.get(i);
			for (ThingTag tt:tl) {
				String [] a = null;
				if (tt.val != null) {
					a = new String[2];
					a[1] = tt.val;
				} else {
					a = new String[1];
				}
				a[0] = tt.tag;
				d.addRange(tt.start, tt.end, tt.depth, a);
			}
			d.sortRangesLongestFirst();
		}
		int test = ds.size();
		for (int i=0;i<ds2TestList.size();i++) {
			VDataSet d = new VDataSet();
			d.setDataLS(ds2TestList.get(i));
			if (single)	d.setValueLS(ds2TestValList.get(i));		
			else d.setValueLSD(ds2TestValList2.get(i)); 
			ds.add(d);
		}

		
		int test2 = ds.size();
		// set correct train/tune/test
		if (test2 > train) {
			if (tune == train) ds.setSplit(train, test-train, test2-test);
			else ds.setSplit(train, tune-train, test2-tune);
		} else {
			ds.setSplitPercent(((double)100)-(percentTune+percentTest), percentTune, percentTest);
		}
		return ds;
	}
	public static VDataSets loadDataSetsDSConLL(String type, String baseDir, String languageTag, String setDir, String xposOrupos) {
		// load dataSet
		List<List<String>> dsList = new ArrayList<>();
		List<List<String>> dsValList = new ArrayList<>();
		List<List<String>> dsTuneList = new ArrayList<>();
		List<List<String>> dsTuneValList = new ArrayList<>();	
		List<List<String>> dsTestList = new ArrayList<>();
		List<List<String>> dsTestValList = new ArrayList<>();
		Set<String> vset = new HashSet<>();

		VFileUtil.loadDataSetsConLL(type, baseDir, languageTag, setDir, xposOrupos, dsList, dsValList, dsTuneList, dsTuneValList, dsTestList, dsTestValList, vset);
		
		// add the tags
		VDataSetDescriptor dsd = new VDataSetDescriptor();
		for (String v:vset) dsd.addDataTag(v, DSSetType.Open, DSDataType.String, null); // add tags
		
		VDataSets ds = new VDataSets(dsd, null);
		// add data
		for (int i=0;i<dsList.size();i++) {
			ds.add(new VDataSet(dsList.get(i), dsValList.get(i)));
		}
		int train = ds.size();
		for (int i=0;i<dsTuneList.size();i++) {
			ds.add(new VDataSet(dsTuneList.get(i), dsTuneValList.get(i)));
		}
		int tune = ds.size();
		for (int i=0;i<dsTestList.size();i++) {
			ds.add(new VDataSet(dsTestList.get(i), dsTestValList.get(i)));
		}
		int test = ds.size();
		// set correct train/tune/test
		ds.setSplit(train, tune-train, test-tune);

		return ds;
	}
	static final String conll17_test_directory = "ud-test-v2.0-conll2017/input/conll17-ud-test-2017-05-09";	
	static final String conll17_test_dev_directory = "ud-test-v2.0-conll2017/input/conll17-ud-development-2017-03-19";	
	
	static final String conll17_gold_directory = "ud-test-v2.0-conll2017/gold/conll17-ud-test-2017-05-09";	
	static final String conll17_gold_dev_directory = "ud-test-v2.0-conll2017/gold/conll17-ud-development-2017-03-19";
	
	static final String conll17_train_directory = "ud-2.0-conll17-baselinemodel-split";
	
	// language / SetDirectory[UD_Langauge-XXX or ALL
	private static boolean loadDataSetsConLL(String type, String baseDir, String languageTag, String setDir, 
			String xposOrupos,
			List<List<String>> dsList, List<List<String>> dsValList, 
			List<List<String>> dsTuneList, List<List<String>> dsTuneValList,
			List<List<String>> dsTestList, List<List<String>> dsTestValList,
			Set<String> vset) {
		vset.clear();
		dsList.clear();
		dsValList.clear();
		dsTestList.clear();
		dsTestValList.clear();
		dsTuneList.clear();
		dsTuneValList.clear();
		
		List<List<List<String>>> dev = new ArrayList<>();
		List<List<List<String>>> train = new ArrayList<>();
		List<List<List<String>>> tune = new ArrayList<>();
		
		String bdir = baseDir+"/"+conll17_train_directory;
		//System.out.println("DIR: " + bdir);
		// training / dev / tune data
    	List<String> trainDirs = fileDirList(baseDir+"/"+conll17_train_directory);
    	for (String tdir:trainDirs) {
    		// check set match
			if (setDir != null && !tdir.equalsIgnoreCase(setDir)) continue;
			if (tdir.startsWith(".")) continue;
    		String sdir = bdir+"/"+tdir;
    		//UD_Enlish / UD_Entlish-LinES
    		List<String> filz = fileList(sdir);
   			if (filz == null) continue;
   			
    		// check language extnsion
    		for (String fn:filz) {
	    		// filter lang tag
    			if (languageTag != null && !fn.startsWith(languageTag+"-") && !fn.startsWith(languageTag+"_")) continue;
    			if (!fn.endsWith(".conllu")) continue;
	    		// load files	   
    			List<List<String>> fdl = loadUDPipeFile(sdir+"/"+fn);
    			if (fn.endsWith("dev.conllu")) {
    				dev.add(fdl);
    			} else if (fn.endsWith("train.conllu")) {
    				train.add(fdl);
    			} else if (fn.endsWith("tune.conllu")) {
    				tune.add(fdl);    				
    			}
    		}
    	}
    	
    	// gold and input test data??	    	
		List<List<List<String>>> test = null;
		String fbase = null;
		if (type.equals("gold")) {
			fbase = baseDir+"/"+conll17_gold_directory+"/";
    	} else if (type.equals("test")) {
			fbase = baseDir+"/"+conll17_test_directory;
    	}

		if (fbase != null) {
			if (setDir != null) {
    			// UD_English-LinES -> en_lines
				setDir = setDir.toLowerCase().substring(2, setDir.length()); // english-lines
				int idx = setDir.indexOf("-");
				if (idx>=0) setDir = languageTag+"_"+setDir.substring(idx+1, setDir.length()); // en_lines
				else setDir = languageTag;
			}

			// load these
			test = new ArrayList<>();
    		List<String> filz = fileList(fbase);
			System.out.println(" ENDing test["+fbase+"] ["+setDir+"] " + filz.size());

    		for (String fn:filz) {
    			if (!fn.endsWith(".conllu")) continue;
    			if (fn.endsWith("_pud.conllu")) continue;
    			if (fn.endsWith("_pud-udpipe.conllu")) continue;
    			// Filter
    			//System.out.println(" Loaded["+fn+"] languageTag["+languageTag+"] setDir["+setDir+"]");

    			if (languageTag != null && !fn.startsWith(languageTag+".") && !fn.startsWith(languageTag+"-")&& !fn.startsWith(languageTag+"_")) continue;
    			if (setDir != null && !fn.startsWith(setDir+".") && !fn.startsWith(setDir+"-")) continue;
   			
    			List<List<String>> fdl = loadUDPipeFile(fbase+"/"+fn);
    			test.add(fdl);
    		}
    	} else {
    		// dev
    		test = dev;
    	}
		System.out.println(" Loaded["+type+"]["+xposOrupos+"] test["+test.size()+"] train["+train.size()+"] tune["+tune.size()+"]");

		// map out the data
 		for (int i=0;i<test.size();i++) {
 			List<List<String>> tv = test.get(i);
	 		//token/pos1/pos2/inf/lema
			dsTestList.add(tv.get(0));
			if (xposOrupos != null && xposOrupos.equalsIgnoreCase("XPOS")) dsTestValList.add(tv.get(2));
			else dsTestValList.add(tv.get(1));
		//	dsTestValList.add(tv.get(3));
		//	dsTestValList.add(tv.get(4));
		}
 		for (int i=0;i<train.size();i++) {
 			List<List<String>> tv = train.get(i);
 			dsList.add(tv.get(0));
			if (xposOrupos != null && xposOrupos.equalsIgnoreCase("XPOS")) {
				dsValList.add(tv.get(2));
				for (String s:tv.get(2)) vset.add(s);
			} else {
				dsValList.add(tv.get(1));
	 			for (String s:tv.get(1)) vset.add(s);
			}
 		 //	dsValList.add(tv.get(3));
 		//	dsValList.add(tv.get(4));
		}
 		for (int i=0;i<tune.size();i++) {
 			List<List<String>> tv = tune.get(i);
 			dsTuneList.add(tv.get(0));
			if (xposOrupos != null && xposOrupos.equalsIgnoreCase("XPOS")) dsTuneValList.add(tv.get(2));
			else dsTuneValList.add(tv.get(1));
 		//	dsTuneValList.add(tv.get(3));
 		//	dsTuneValList.add(tv.get(4));
		}	
		return true;
	}

    // data fyle types
	public static enum DataSetType {BrownOldTags, BrownCleanTags, BrownPennTreebankTags, BrownUniversalTags, WSJTreebank, WSJTreebank3,
					GenericTokens, FileNameTagsTokens, GenericAndFileTagTokens,
					UDPipeConLL, TreeBanks};
	private static boolean loadDataSets(DataSetType dtype, String filename, 
			List<List<String>> dsList, List<List<String>> dsValList, List<List<String[]>> dsValList2, 
			List<List<String>> dsTuneList, List<List<String>> dsTuneValList, List<List<String[]>> dsTuneValList2,
			List<List<String>> dsTestList, List<List<String>> dsTestValList, List<List<String[]>> dsTestValList2,
			List<List<String>> ds2TestList, List<List<String>> ds2TestValList, List<List<String[]>> ds2TestValList2,
			Set<String> vset, double percentTune, double percentTest, 
			List<List<ThingTag>> dsTl, List<List<ThingTag>> dsTuneTl, List<List<ThingTag>> dsTestTl) {
		vset.clear();
		
		dsList.clear();
		dsValList.clear();
		dsValList2.clear();
		
		dsTestList.clear();
		dsTestValList.clear();
		dsTestValList2.clear();
		
		dsTuneList.clear();
		dsTuneValList.clear();
		dsTuneValList2.clear();
		
		ds2TestList.clear();
		ds2TestValList.clear();
		ds2TestValList2.clear();
		ds2TestValList2.clear();
		
		dsTl.clear();
		dsTuneTl.clear();
		dsTestTl.clear();
		
		boolean includeBreaks = true; // include breaks in Treebanks (or others if they exist)
		boolean singleTag = false;
		double percentTrain = (100 - (percentTest+percentTune));
		
		if (dtype == DataSetType.WSJTreebank3) {
			// Penntreebank_3 .. complete
			//	Training data: sections 0-18
			//	Development test data: sections 19-21
			//	Testing data: sections 22-24
	 
			int posTrainEnd = 18;
			int posTuneEnd = 21;
			int posTestEnd = 24;
			int pipCnt = 0;
			for (int i = 0;i<=posTestEnd;i++) {
				String dn = filename+"/"+String.format("%02d", i);
				List<List<String>> set = VFileUtil.loadTreebankFiles(dn, includeBreaks);
				for (int x=0;x<set.size();x++) {
					List<String> s = set.get(x);	
					List<List<String>> spl = VFileUtil.tokenizeString(s, "/");
					List<String []> valx = new ArrayList<String[]>(); // for secondary tags
					
					for (int z=0;z<spl.get(1).size();z++) {
						String sv = spl.get(1).get(z);
						valx.add(null);	// secondary tag default null
						
						if (sv.contains("|")) {	
							// has multiple tags
							pipCnt++;
							int idx = sv.indexOf("|");
							String sv1 = sv.substring(0, idx);
							spl.get(1).set(z, sv1);	// set primary tag
							if (singleTag) {
								String [] s1 = new String[1];
								s1[0] = sv1;
								valx.set(z, s1);	// set secondary tag								
							} else {
								// second option
								sv = sv.substring(idx+1, sv.length());
								String [] s2 = new String[2];
								s2[0] = sv1;
								s2[1] = sv;
								valx.set(z, s2);	// set secondary tag								
							}
						} else {
							String [] s1 = new String[1];
							s1[0] = sv;
							valx.set(z, s1);	// set secondary tag
						}
						vset.add(sv);							
					}
					if (i <= posTrainEnd) {
						dsValList.add(spl.get(1));			
						dsValList2.add(valx);			
						dsList.add(spl.get(0));	
					} else if (i <= posTuneEnd) {
						dsTestValList.add(spl.get(1));			
						dsTestValList2.add(valx);			
						dsTestList.add(spl.get(0));	
					} else if (i <= posTestEnd) {
						ds2TestValList.add(spl.get(1));
						ds2TestValList2.add(valx);
						ds2TestList.add(spl.get(0));									
					}
				}
			}
			if (percentTune > 0) {
				int posTrainSize = dsList.size() - ((int)((double)dsList.size() * (double)((double)percentTune/(double)100)));
				for (int i=0;i<dsList.size();i++) {
					if (i>posTrainSize) {
						dsTuneValList.add(dsValList.get(i));
						dsTuneList.add(dsList.get(i));
						dsValList.remove(i);
						dsList.remove(i);
						i--;
					}
				}
			}
			
		} else if (dtype == DataSetType.TreeBanks) {
			//	System.out.println("MultiTag["+pipCnt+"]"); // MultiTag[147]
			//	for(String s:vset) System.out.println("TAG["+s+"]");
			List<Object []> ool = loadTreebankMrgFiles(filename);
			
			int posTrainSize = (int)((double)ool.size() * (double)((double)percentTrain/(double)100));
			int posTuneSize = (int)((double)ool.size() * (double)((double)percentTune/(double)100));
			int posTestSize = (int)((double)ool.size() * (double)((double)percentTest/(double)100));
			if (posTestSize == 0 && posTuneSize == 0) percentTrain = ool.size();
			int tot = posTrainSize + posTuneSize + posTestSize;
			if (tot < ool.size()) posTrainSize += (ool.size()-tot);
		
			for (int i=0;i<ool.size();i++) {
				Object [] o = ool.get(i);
				@SuppressWarnings("unchecked")
				List<ThingTag> ttl = (List<ThingTag>)o[1];
				@SuppressWarnings("unchecked")
				List<List<String>> stl = (List<List<String>>)o[0];
				List<String> tokl = stl.get(0);
				List<String> tagl = stl.get(1);
				List<String []> valx = new ArrayList<String[]>(); // for secondary tags
				for (int x=0;x<tagl.size();x++) {
					String [] s = new String[1];
					s[0] = tagl.get(x);
					valx.add(s);
					vset.add(s[0]);
				}
				
				// Add to correct section				
				if (dsList.size() < posTrainSize) {
					dsValList.add(tagl);			
					dsValList2.add(valx);			
					dsList.add(tokl);	
					dsTl.add(ttl);
				} else if (dsTuneList.size() < posTuneSize) {
					dsTuneValList.add(tagl);			
					dsTuneValList2.add(valx);			
					dsTuneList.add(tokl);	
					dsTuneTl.add(ttl);
				} else {
					dsTestValList.add(tagl);			
					dsTestValList2.add(valx);			
					dsTestList.add(tokl);
					dsTestTl.add(ttl);
				}					
			}			

		} else if (dtype != DataSetType.WSJTreebank) {
			List<List<String>>  posAllAndNames =  VFileUtil.loadTextFilesAndName(filename);
			List<String> posAll =  posAllAndNames.get(0);
			List<String> namsAll =  posAllAndNames.get(1);
			int posTrainSize = (int)((double)posAll.size() * (double)((double)percentTrain/(double)100));
			int posTuneSize = (int)((double)posAll.size() * (double)((double)percentTune/(double)100));
			int posTestSize = (int)((double)posAll.size() * (double)((double)percentTest/(double)100));
			if (posTestSize == 0 && posTuneSize == 0) percentTrain = posAll.size();
			int tot = posTrainSize + posTuneSize + posTestSize;
			if (tot < posAll.size()) posTrainSize += (posAll.size()-tot);
			
			// tokenize
			int pos = -1;
			List<String> posAll2 = new ArrayList<>();
			List<String> namesAll2 = new ArrayList<>();
			for (int i=0;i<posAll.size();i++) {
				if (i > posAll.size()/2) {
					if (pos < 0) pos = 0;
					else pos += 2;
					posAll2.add(pos, posAll.get(i));						
					namesAll2.add(pos, namsAll.get(i));						
				} else {
					posAll2.add(posAll.get(i));
					namesAll2.add(namsAll.get(i));
				}
			}
			
			for (int i=0;i<posAll2.size();i++) {
				String s = posAll2.get(i);	
				List<String> tks = VFileUtil.tokenizeString(s, false, false, false);
			
				List<List<String>> spl = VFileUtil.tokenizeString(tks, "/");			
				List<String> np = null;
				if (dtype == DataSetType.BrownPennTreebankTags) np = VFileUtil.normalizePosString(spl.get(1), false, true, false);
				else if (dtype == DataSetType.BrownUniversalTags) np = VFileUtil.normalizePosString(spl.get(1), false, true, true);
				else if (dtype == DataSetType.BrownCleanTags) np = VFileUtil.normalizePosString(spl.get(1), false, false, false);
				else if (dtype == DataSetType.GenericTokens) np = spl.get(1);
				else if (dtype == DataSetType.FileNameTagsTokens || dtype == DataSetType.GenericAndFileTagTokens) {
					if (spl.get(1).get(0) == null) { // name
						String nameTag = namesAll2.get(i).toUpperCase();
						int idx = nameTag.indexOf("--");
						if (idx > 0) {
							nameTag = nameTag.substring(0, idx);
							for (int x=0;x<spl.get(1).size();x++) spl.get(1).set(x, nameTag);
						}
					} else if (spl.get(1).get(0).indexOf("/") > 0) { // has count
						for (int x=0;x<spl.get(1).size();x++) {
							String sx [] =  spl.get(1).get(x).split("/");
							int cnt = Integer.parseInt(sx[1]);
							spl.get(1).set(x, sx[0]);
		// FIXME duplicate
						}
					} 
					np = spl.get(1);
				}
				else np = VFileUtil.normalizePosString(spl.get(1), true, false, false);
				
				for (String sv:np) vset.add(sv);
				if (dsList.size() < posTrainSize) {
					dsValList.add(np);			
					dsList.add(spl.get(0));	
				} else if (dsTuneList.size() < posTuneSize) {
					dsTuneValList.add(np);			
					dsTuneList.add(spl.get(0));	
				} else {
					dsTestList.add(spl.get(0));
					dsTestValList.add(np);
				}				
			}
		} else {
			// Penntreebank .. sample from NTLK is all that has been available [199 files]
			List<List<String>> posAll =  VFileUtil.loadTreebankFiles(filename, includeBreaks);	
			int posTrainSize = (int)((double)posAll.size() * (double)((double)percentTrain/(double)100));
			int posTuneSize = (int)((double)posAll.size() * (double)((double)percentTune/(double)100));
			int posTestSize = (int)((double)posAll.size() * (double)((double)percentTest/(double)100));
			if (posTestSize == 0 && posTuneSize == 0) percentTrain = posAll.size();
			int tot = posTrainSize + posTuneSize + posTestSize;
			if (tot < posAll.size()) posTrainSize += (posAll.size()-tot);
			
			// tokenize
			for (int i=0;i<posAll.size();i++) {
				List<String> s = posAll.get(i);	
				List<List<String>> spl = VFileUtil.tokenizeString(s, "/");
				for (String sv:spl.get(1)) vset.add(sv);
				
				if (dsList.size() < posTrainSize) {
					dsValList.add(spl.get(1));			
					dsList.add(spl.get(0));	
				} else if (dsTuneList.size() < posTuneSize) {
					dsTuneValList.add(spl.get(1));			
					dsTuneList.add(spl.get(0));	
				} else {
					dsTestValList.add(spl.get(1));
					dsTestList.add(spl.get(0));
				}
			}
		}
		//for(String s:vset) System.out.println("TAG["+s+"]");
		return true;
	}
    public static String getMappedPOS(String pos) {
    	for (int i=0;i<posMap.length;i++) {
    		if (posMap[i][0].equals(pos)) return posMap[i][1];
    	}
    	return pos;
    }
    static final String [][] posMap = {
    		{"CD", "CD"},//2.	CD	Cardinal number
    		{"OD", "CD"},//2.	CD	Cardinal number
    		
    		{"DT", "DT"},//3.	DT	Determiner
    		{"AT", "DT"},//3.	DT	Determiner
    		{"DTI", "DT"},//3.	DT	Determiner
    		{"DTS", "DT"},//3.	DT	Determiner
    		
    		{"ABL", "PDT"},//16.	PDT	Predeterminer
    		{"ABN", "PDT"},//16.	PDT	Predeterminer
    		{"ABX", "PDT"},//16.	PDT	Predeterminer
    		{"DTX", "PDT"},//16.	PDT	Predeterminer
    		{"AP", "PDT"},//16.	PDT	Predeterminer

    		{"EX", "EX"},//4.	EX	Existential there
    		
    		{"FW", "FW"},//5.	FW	Foreign word
    		
    		{"IN", "IN"},//6.	IN	Preposition or subordinating conjunction
    		{"CS", "IN"},//6.	IN	Preposition or subordinating conjunction
    		{"CC", "CC"},//1.	CC	Coordinating conjunction
    		
    		{"JJ", "JJ"},//7.	JJ	Adjective
    		{"JJR", "JJR"},//8.	JJR	Adjective, comparative
    		{"JJS", "JJS"},//9.	JJS	Adjective, superlative
    		{"JJT", "JJS"},//9.	JJS	Adjective, superlative
    		
    		{"LS", "LS"},//10.	LS	List item marker
    		   		
    		{"NN", "NN"},//12.	NN	Noun, singular or mass
    		{"NR", "NN"},//12.	NN	Noun, singular or mass
    		{"NR$", "NN$"},
    		{"NN$", "NN$"},
    		{"NNS", "NNS"},//13.	NNS	Noun, plural
    		{"NRS", "NNS"},//13.	NNS	Noun, plural
    		{"NNS$", "NNS$"},
    		{"NP", "NNP"},//14.	NNP	Proper noun, singular
    		{"NP$", "NNP$"},
    		{"NPS", "NNPS"},//15.	NNPS	Proper noun, plural
    		{"NPS$", "NNPS$"},
    		
    		{"POS", "POS"},//17.	POS	Possessive ending
    		{"PN", "PRP"},//18.	PRP	Personal pronoun
    		{"PPL", "PRP"},//18.	PRP	Personal pronoun
    		{"PPO", "PRP"},//18.	PRP	Personal pronoun
    		{"PPLS", "PRP"},//18.	PRP	Personal pronoun
    		{"PPS", "PRP"},//18.	PRP	Personal pronoun
    		{"PPSS", "PRP"},//18.	PRP	Personal pronoun
    		{"PN$", "PRP$"},//19.	PRP$	Possessive pronoun
    		{"PP$", "PRP$"},//19.	PRP$	Possessive pronoun
    		{"PP$$", "PRP$"},//19.	PRP$	Possessive pronoun
    		{"PN$", "PRP$"},//19.	PRP$	Possessive pronoun
    		
    		{"RB", "RB"},//20.	RB	Adverb   		
    		{"*", "RB"},//20.	RB	Adverb   		
    		{"QL", "RB"},//20.	RB	Adverb   		
    		{"QLP", "RB"},//20.	RB	Adverb   		
    		{"RN", "RB"},//20.	RB	Adverb   		
    		{"RBR", "RBR"},//21.	RBR	Adverb, comparative
    		{"RBT", "RBS"},//22.	RBS	Adverb, superlative
    		{"RP", "RP"},//23.	RP	Particle
    		
    		{"SYM", "SYM"},//24.	SYM	Symbol
    		
    		{"TO", "TO"},//25.	TO	to
    		
    		{"UH", "UH"},//26.	UH	Interjection
    		
    		{"MD", "MD"},//11.	MD	Modal
		
    		{"VB", "VB"},//27.	VB	Verb, base form
    		{"BE", "VB"},//27.	VB	Verb, base form
    		{"HV", "VB"},//27.	VB	Verb, base form
    		{"DO", "VB"},//27.	VB	Verb, base form
    		{"BEM", "VB"},//27.	VB	Verb, base form
    		
    		{"VBD", "VBD"},//28.	VBD	Verb, past tense
    		{"HVD", "VBD"},//28.	VBD	Verb, past tense
    		{"BED", "VBD"},//28.	VBD	Verb, past tense
    		{"DOD", "VBD"},//28.	VBD	Verb, past tense
    		
    		{"VBG", "VBG"},//29.	VBG	Verb, gerund or present participle
    		{"HVG", "VBG"},//29.	VBG	Verb, gerund or present participle
    		{"BEG", "VBG"},//29.	VBG	Verb, gerund or present participle
    		
    		{"VBN", "VBN"},//30.	VBN	Verb, past participle
    		{"HVN", "VBN"},//30.	VBN	Verb, past participle
    		{"BEN", "VBN"},//30.	VBN	Verb, past participle
    		
    		{"VBP", "VBP"},	//??		//31.	VBP	Verb, non-3rd person singular present
    		{"BER", "VBP"},	//??		//31.	VBP	Verb, non-3rd person singular present
    		
    		{"VBZ", "VBZ"},//32.	VBZ	Verb, 3rd person singular present
    		{"DOZ", "VBZ"},//32.	VBZ	Verb, 3rd person singular present
    		{"HVZ", "VBZ"},//32.	VBZ	Verb, 3rd person singular present
    		{"BEZ", "VBZ"},//32.	VBZ	Verb, 3rd person singular present
    		{"BEDZ", "VBZ"},//32.	VBZ	Verb, 3rd person singular present
    		
    		{"WDT", "WDT"},//33.	WDT	Wh-determiner
    		{"WPO", "WP"},//34.	WP	Wh-pronoun
    		{"WPS", "WP"},//34.	WP	Wh-pronoun
    		{"WP$", "WP$"},//35.	WP$	Possessive wh-pronoun
    		{"WRB", "WRB"},//36.	WRB	Wh-adverb
    		{"WQL", "WRB"},//36.	WRB	Wh-adverb
    		
    		{"NC", "NC"}, // X) NC	cited word (hyphenated after regular tag)	
    		{".", "."},
    		{",", ","},
    		{"(", "("},
    		{")", ")"},
    		{"'", "'"},
    		{"--", "--"},
    		{":", ":"},
    		
    		{"AP$", "PDT$"},//other's
    		{"JJ$", "NNP$"}, // Great's in a NAME
    		//TAG[DT$] // another's
    		//TAG[CD$] // 1950's

    };
    
    //
    // universal dependencies
    // https://universaldependencies.org/u/pos/
    // https://universaldependencies.org/tagset-conversion/en-penn-uposf.html
    public static String getMappedUniversalPOS(String pos) {
    	for (int i=0;i<posUniversalMap.length;i++) {
    		if (posUniversalMap[i][0].equals(pos)) return posUniversalMap[i][1];
    	}
    	return pos;
    }
    static final String [][] posUniversalMap = {
    		{"JJ", "ADJ"},//ADJ: adjective
    		{"JJR", "ADJ"},//ADJ: adjective
    		{"JJS", "ADJ"},//ADJ: adjective
    		{"JJ$", "ADJ"},//ADJ: adjective
    		{"AFX", "ADJ"},//ADJ: adjective
    		    		
    		{"RB", "ADV"},//ADV: adverb
    		{"RBR", "ADV"},//ADV: adverb
    		{"RBS", "ADV"},//ADV: adverb
    		{"WRB", "ADV"},//ADV: adverb
  
    		{"RP", "ADP"},//ADP: adposition
    		{"IN", "ADP"},//ADP: adposition

    		{"POS", "PART"},//PART: particle
    		{"TO", "PART"},//PART: particle
    		  		
    		{"CC", "CCONJ"},//CCONJ: coordinating conjunction
    		{"CO", "SCONJ"},//SCONJ: subordinating conjunction
    		
    		{"DT", "DET"},//DET: determiner
    		{"DT$", "DET"},//DET: determiner
    		{"PDT", "DET"},//DET: determiner
    		//{"PRP$", "DET"},//DET: determiner
    		{"WDT", "DET"},//DET: determiner
    		{"WP$", "DET"},//DET: determiner
   		
    		{"PRP", "PRON"},//PRON: pronoun
    		{"PRP$", "PRON"},//PRON: pronoun
    		{"WP", "PRON"},//PRON: pronoun
    		{"EX", "PRON"},//PRON: pronoun
    		
    		//{"??", "AUX"},//AUX: auxiliary
  		
    		{"VB", "VERB"},//VERB: verb
    		{"VBD", "VERB"},//VERB: verb
    		{"VBG", "VERB"},//VERB: verb
    		{"VBN", "VERB"},//VERB: verb
    		{"VBP", "VERB"},//VERB: verb
    		{"VBZ", "VERB"},//VERB: verb
    		{"MD", "VERB"},//VERB: verb
   		
    		{"NN", "NOUN"},//NOUN: noun
    		{"NN$", "NOUN"},//NOUN: noun
    		{"NNS", "NOUN"},//NOUN: noun
    		{"NNS$", "NOUN"},//NOUN: noun

    		{"NNP", "PROPN"},//PROPN: proper noun
    		{"NNP$", "PROPN"},//PROPN: proper noun
    		{"NNPS", "PROPN"},//PROPN: proper noun
    		{"NNPS$", "PROPN"},//PROPN: proper noun
   		
    		{".", "PUNCT"},//PUNCT: punctuation
    		{",", "PUNCT"},//PUNCT: punctuation
    		{")", "PUNCT"},//PUNCT: punctuation
    		{"(", "PUNCT"},//PUNCT: punctuation
    		{"'", "PUNCT"},//PUNCT: punctuation
    		{"--", "PUNCT"},//PUNCT: punctuation
    		{":", "PUNCT"},//PUNCT: punctuation
    		{"''", "PUNCT"},//PUNCT: punctuation
    		{"``", "PUNCT"},//PUNCT: punctuation
    		{"-LRB-", "PUNCT"},//PUNCT: punctuation
    		{"-RRB-", "PUNCT"},//PUNCT: punctuation
    		{"HYPH", "PUNCT"},//PUNCT: punctuation	
    		
    		{"UH", "INTJ"},//INTJ: interjection
    		{"CD", "NUM"},//NUM: numeral
    		
    		{"NC", "X"},//X: other
    		{"FW", "X"},//X: other
    		{"LS", "X"},//X: other
    		{"NIL", "X"},//X: other
    		
    		{"SYM", "SYM"},//SYM: symbol
    		{"#", "SYM"},//SYM: symbol
    		{"$", "SYM"},//SYM: symbol
    };

    
    //
    // Save data results to a file
    //
	public static boolean saveDataSetsResults(String filePath, List<List<String>> dsList, List<List<String>> dsValList) {
		if (dsList == null || dsList.size() < 1) return false;

        FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(filePath);
		} catch (IOException e2) {
			System.out.println("ERROR: opening file: " + filePath);	
			System.out.println("   RR: " + e2.getMessage());	
			return false;
		}
		
		try {
			PrintWriter printWriter = new PrintWriter(fileWriter); 
			for (int i=0;i<dsList.size();i++) {
				List<String> list = dsList.get(i);
				List<String> vlist = dsValList.get(i);
		        for (int x=0;x<list.size();x++) {
		        	String s = list.get(x);
		        	String sv = vlist.get(x);
		        	printWriter.println(s+" / " + sv);
		        } 
	        	printWriter.println("\n#----------: "+i+"\n");
			}
	        printWriter.close();
		} catch (Throwable t) {
			System.out.println("ERROR: save file: " + filePath);
		}

		return true;
	}
	
    //
    // Load data results to a file
    //
	public static boolean loadDataSetsResults(String filePath, List<List<String>> dsList, List<List<String>> dsValList) {
		dsList.clear();
		dsValList.clear();
		
		String line = "";
		List<String> tok = new ArrayList<>();
		List<String> vtok = new ArrayList<>();
		
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
            	if (line.length() < 2) continue;
            	if (line.startsWith("#----")) {
            		dsList.add(tok);
            		dsValList.add(vtok);
            		tok = new ArrayList<>();
            		vtok = new ArrayList<>();
            		continue;
            	}
				String sset [] = line.split("/");
				tok.add(sset[0].trim());
				vtok.add(sset[1].trim());
            }

        } catch (IOException e) {
    		System.out.println("ERROR: load file["+filePath+"] " + e.getMessage());
    		return false;
        }
        if (dsList.size() < 1) return false;
		return true;
	}
	
	/*
	//
	// load csv/tsv files
	// - comma seperated OR tab seperated
	// - ignore first line OR not (header)
	//https://www.univocity.com/pages/univocity_parsers_tutorial.html#introduction-to-univocity-parsers
	public static List<String []> loadTsvFile(String filePath) {

		TsvParserSettings settings = new TsvParserSettings();
		TsvParser parser = new TsvParser(settings);

		// parses all rows in one go.
		List<String[]> allRows = null;
		try {
			allRows = parser.parseAll(new FileReader(filePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
        return allRows;
    }
	public static List<String []> loadCsvFile(String filePath) {

		CsvParserSettings settings = new CsvParserSettings();
		CsvParser parser = new CsvParser(settings);

		// parses all rows in one go.
		List<String[]> allRows = null;
		try {
			allRows = parser.parseAll(new FileReader(filePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
        return allRows;
    }
	
	//
	// Save as a csv file
	//
	public static void saveCsvFile(String filePath, List<String []> data) {
    	if (data.size() < 1) return;
        FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(filePath);
		} catch (IOException e2) {
			System.out.println("ERROR: opening file[" + filePath+"] "+ e2.getMessage());	
			return;
		}
		
		try {
			PrintWriter printWriter = new PrintWriter(fileWriter); 
	        for (String [] l:data) {
	        	for (int i=0;i<l.length;i++) {
	        		String n = l[i];
		        	if (i == (l.length-1)) printWriter.print(n);
		        	else printWriter.print(n+",");
		        }
	        	printWriter.println("");
	        }       
	        printWriter.close();
		} catch (Throwable t) {
			System.out.println("ERROR: save file[" + filePath+"] " + t.getMessage());	
		}
    }
    */
	// values should be baseic type: String, Integer, Double, etc
	public static void saveCsvFileValues(String filePath, List<List<Integer>> data) {
    	if (data.size() < 1) return;
        FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(filePath);
		} catch (IOException e2) {
			System.out.println("ERROR: opening file[" + filePath+"] "+ e2.getMessage());	
			return;
		}
		
		try {
			PrintWriter printWriter = new PrintWriter(fileWriter); 
	        for (List<Integer> l:data) {
	        	for (int i=0;i<l.size();i++) {
	        		Integer n = l.get(i);
		        	if (i == (l.size()-1)) printWriter.print(n);
		        	else printWriter.print(n+",");
		        }
	        	printWriter.println("");
	        }       
	        printWriter.close();
		} catch (Throwable t) {
			System.out.println("ERROR: save file[" + filePath+"] " + t.getMessage());	
		}
    }
	// values should be baseic type: String, Integer, Double, etc
	public static void saveCsvFileValuesString(String filePath, List<List<String>> data) {
    	if (data.size() < 1) return;
        FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(filePath);
		} catch (IOException e2) {
			System.out.println("ERROR: opening file[" + filePath+"] "+ e2.getMessage());	
			return;
		}
		
		try {
			PrintWriter printWriter = new PrintWriter(fileWriter); 
	        for (List<String> l:data) {
	        	for (int i=0;i<l.size();i++) {
	        		String n = l.get(i);
		        	if (i == (l.size()-1)) printWriter.print(n);
		        	else printWriter.print(n+",");
		        }
	        	printWriter.println("");
	        }       
	        printWriter.close();
		} catch (Throwable t) {
			System.out.println("ERROR: save file[" + filePath+"] " + t.getMessage());	
		}
    } 

 
}
