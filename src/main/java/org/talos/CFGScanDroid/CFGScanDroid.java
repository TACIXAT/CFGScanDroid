/* 	CFGScanDroid - Control Flow Graph Scanning for Android
	Copyright (C) 2014  Douglas Gastonguay-Goddard

	This program is free software; you can redistribute it and/or
	modify it under the terms of the GNU General Public License
	as published by the Free Software Foundation; either version 2
	of the License, or (at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details */

package org.talos.CFGScanDroid;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.nio.file.Files;
import java.nio.file.Path;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.DexFileFactory;
import org.jf.util.ExceptionWithContext;

import org.json.JSONObject;

import com.google.common.collect.Ordering;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.Edge;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

// define: incest
	// when a vertex has a sibling at the same depth 
	// and that sibling is also it's parent

public class CFGScanDroid {
	static long scannedSampleCount = 0;
	static long detectedSampleCount = 0;
	static long scannedFunctionCount = 0;
	static JCommanderArguments parsedArguments;
	static boolean useShortIdentifier;
	static List<Match> matches = new ArrayList<Match>();

	public static Graph buildGraph() {
		TinkerGraph graph = new TinkerGraph("/tmp/cfggraph", TinkerGraph.FileType.GRAPHML); 
		graph.createIndex("signatureName", Vertex.class);
		graph.createIndex("sha256", Vertex.class);
		graph.createIndex("md5", Vertex.class);
		graph.createIndex("type", Vertex.class);

		Map sigLookup = new HashMap<String, Vertex>();
		Map fileLookup = new HashMap<String, Vertex>();
		
		for(Match match : matches) {
			// check map for sig
			CFGSig matchSig = match.getSignature();
			String sigString = matchSig.getStringSignature();
			Vertex sigVertex = (Vertex)sigLookup.get(sigString);

			if(sigVertex == null) {
				// create vertex
				sigVertex = graph.addVertex(null);
				sigVertex.setProperty("type", "signature");
				sigVertex.setProperty("signature", sigString);
				sigVertex.setProperty("signatureName", matchSig.getName());
				// add sig to map
				sigLookup.put(sigString, sigVertex);
			}

			// check map for file
			String fileSHA256 = match.getFileSHA256();
			Vertex fileVertex = (Vertex)fileLookup.get(fileSHA256);

			if(fileVertex == null) {
				// create vertex
				fileVertex = graph.addVertex(null);
				sigVertex.setProperty("type", "file");
				fileVertex.setProperty("sha256", fileSHA256);
				fileVertex.setProperty("md5", match.getFileMD5());
				fileVertex.setProperty("fileNameList", new ArrayList<String>());
				// add file to map
				fileLookup.put(fileSHA256, fileVertex);
			}

			// what idiot would scan the same file multiple times with different names?
			List<String> fileNames = fileVertex.getProperty("fileNameList");
			if(!fileNames.contains(match.getFileName())) {
				fileNames.add(match.getFileName());
			}

			// TODO: comment this out and see if it still works
			fileVertex.setProperty("fileNameList", fileNames);

			// create edge(sig, file)
			Edge matchEdge = graph.addEdge(null, sigVertex, fileVertex, "matches");

			ControlFlowGraph cfg = match.getControlFlowGraph();
			matchEdge.setProperty("method", cfg.getIdentifier(false));
			// matchEdge.setProperty("fileBytes", cfg.getMethodBytesAsHexString());
		}

		return graph;
	}

	public static void main(String[] args) throws IOException {
		parsedArguments = new JCommanderArguments();
		JCommander argParser = new JCommander(parsedArguments);

		// parse arguments
		try {
			argParser.parse(args);
		} catch(ParameterException exception) {
			System.err.println(exception);
			System.err.println("PARSE ERROR: Bad parameter");
			System.out.print(parsedArguments.getUsage());
			System.exit(1);
		}

		// make sure a useful set of arguments are set
		validateArguments(argParser);

		if(parsedArguments.getExeFile().size() > 0) {
			scanExeFile(parsedArguments.getExeFile());
			System.exit(0);
		}

		// get files from directories, one level deep
		List<File> fileList = getFileList();
		fileList = Ordering.natural().sortedCopy(fileList);

		// dump sigs
		if(parsedArguments.dumpSignatures()) {
			for(File file : fileList) 
				dumpSigs(file);
		// scan
		} else {
			// load signatures
			List<CFGSig> signatures = null;
			for(String sigFile : parsedArguments.getSignatureFiles()) {
				if(signatures == null)
					signatures = parseSignatures(sigFile);
				else
					signatures.addAll(parseSignatures(sigFile));
			}

			// load raw signatures
			for(String sig : parsedArguments.getRawSignatures()) {
				if(signatures == null)
					signatures = new ArrayList<CFGSig>();

				CFGSig cfgSig = new CFGSig(sig);
				signatures.add(cfgSig);
			}

			// normalize
			if(parsedArguments.normalize()) {
				for(CFGSig cfgSig : signatures) {
					// System.out.println("NORMALIZING SIGNATURE: " + cfgSig.getName());
					// System.out.println(cfgSig.getVertexCount());
					// System.out.println(cfgSig.getEdgeCount());
					cfgSig.normalize();
					// System.out.println(cfgSig.getVertexCount());
					// System.out.println(cfgSig.getEdgeCount());
				}
			}

			// for each file, scan
			for(File file : fileList)  {
				++scannedSampleCount;
				boolean detected = scanDexFile(file, signatures);
				if(detected)
					++detectedSampleCount;
			}

			// print stats
			if(parsedArguments.printStatistics()) {
				System.out.println();
				System.out.println("Samples Scanned:\t" + scannedSampleCount);
				System.out.println("Functions Scanned:\t" + scannedFunctionCount);
				System.out.println("Samples Detected:\t" + detectedSampleCount);
				for(CFGSig signature : signatures) {
					System.out.println(signature.getName() + ": " + signature.getDetectionCount());
				}
			}

			if(parsedArguments.outputGraph()) {
				Graph graph = buildGraph();
				graph.shutdown();
			}
		}

		return;
	}

	// build file list
	public static List<File> getFileList() {
		List<File> fileList = new ArrayList<File>();
		for(String fileName : parsedArguments.getDexFiles()) {
			File file = new File(fileName);
			// does file exist?
			if(!file.exists()) {
				System.out.println("404 - File not found! Discarding: " + fileName);
				continue;
			}

			// if directory, get files inside
			if(file.isDirectory()) {
				File[] innerFileList = file.listFiles();
				if(file == null || innerFileList == null || innerFileList.length == 0)
					continue;

				for(File entry : file.listFiles()) {
					// if file in directory is a file, add it
					if(entry.exists() && entry.isFile() && !fileList.contains(entry)) {
						fileList.add(entry);
					}
				}
				continue;
			}

			// if it's a file, add it
			if(file.isFile()  && !fileList.contains(file)) {
				fileList.add(file);
			}
		}

		return fileList;
	}

	public static void validateArguments(JCommander argParser) {
		// truncate long/class/path.fn to path.fn
		useShortIdentifier = parsedArguments.shortIdentifier();

		// must have signature or dump sigs flag
		if(parsedArguments.getRawSignatures().size() < 1 && 
		   parsedArguments.getSignatureFiles().size() < 1 && 
		   !parsedArguments.dumpSignatures() &&
		   parsedArguments.getExeFile().size() < 1) {
			System.err.println("PARSE ERROR: Must have one of (-s|-d|-r|-x)!");
			System.out.print(parsedArguments.getUsage());
			System.exit(1);
		}

		if(parsedArguments.simpleMatch() && parsedArguments.subgraphIsomorphism()) {
			System.err.println("ERROR: Please specify only one of (-g|-i)");
			System.out.print(parsedArguments.getUsage());
			System.exit(1);
		}

		// files are important
		if(parsedArguments.getDexFiles().size() == 0 && parsedArguments.getExeFile().size() < 1) {
			System.err.println("YOU SHOULD PROBABLY INCLUDE SOME FILES TO SCAN! (-f)");
		}

		// simple match implies exact match
		if(parsedArguments.simpleMatch()) {
			parsedArguments.setExactMatch(true);
			parsedArguments.setPartialMatch(false);
		}

		if(parsedArguments.subgraphIsomorphism()){
			parsedArguments.setPartialMatch(true);
			parsedArguments.setNormalize(true);
		}

		// partial match unsets exact match
		if(parsedArguments.partialMatch())
			parsedArguments.setExactMatch(false);
	}

	public static void dumpSigs(File dexFileFile) throws IOException {
		// list file
		System.out.println("#DUMPING: " + dexFileFile.getPath());
		if(!dexFileFile.exists()) {
			System.err.println("Dexfile not found!");
			return;
		}

		DexBackedDexFile dexFile = null;

		// load dex file
		try {
			dexFile = DexFileFactory.loadDexFile(dexFileFile, 15);
		} catch(org.jf.util.ExceptionWithContext e) {
			System.err.println(e);
			return;
		} catch(java.io.FileNotFoundException e) {
			System.err.println("Cannot scan a directory: " + dexFileFile.getPath());
			return;
		} catch(Exception e) {
			System.err.println("Error loading file: " + dexFileFile.getPath());
			return;
		} 

		// skip odex, has instructions I don't support currently
		if(dexFile.isOdexFile()) {
			System.err.println("Odex not supported!");
			return;
		}

		List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(dexFile.getClasses());
		// for each method, generate sig
		for(final ClassDef classDef: classDefs) {
			for(Method method: classDef.getMethods()) {
				ControlFlowGraph cfg = new AndroidCFG(method);

				if(parsedArguments.normalize()) {
					cfg.normalize();
				}

				CFGSig sig = new CFGSig(cfg);
				if(sig.getVertexCount() > 1)
					System.out.println(sig.getStringSignature());
			}
		}
	}

	public static void scanExeFile(List<String> exeFileList) {
		String exeFileName = exeFileList.get(0);
		ArrayList<CFGSig> peSignatures = new ArrayList<CFGSig>();
		ArrayList<PEFile> peFiles = new ArrayList<PEFile>();
		
		try{ 
			BufferedReader br = new BufferedReader(new FileReader(exeFileName));
			String jsonString = null;
			// read signatures
			while((jsonString = br.readLine()) != null) {
				JSONObject jsonFile = new JSONObject(jsonString);
				String fileName = jsonFile.getString("fileName");
				String fileMD5 = jsonFile.getString("fileMD5");
				String fileSHA256 = jsonFile.getString("fileSHA256");
				JSONObject jsonFunctions = jsonFile.getJSONObject("functions");
				ArrayList<PortableExecutableCFG> peCFGs = new ArrayList<PortableExecutableCFG>();

				Iterator<String> functionKeys = jsonFunctions.keys();
				while(functionKeys.hasNext()) {
					String key = functionKeys.next();
					JSONObject jsonFunction = jsonFunctions.getJSONObject(key);
					String functionName = jsonFunction.getString("functionName");
					String functionBytes = jsonFunction.getString("functionBytes");
					String functionAddress = jsonFunction.getString("functionAddress");
					String functionSig = jsonFunction.getString("functionSig");

					CFGSig cfgSig = new CFGSig(functionSig);
					PortableExecutableCFG peCFG = new PortableExecutableCFG(functionName, functionBytes, functionAddress, cfgSig.getAdjacencyMatrix());

					peSignatures.add(cfgSig);
					peCFGs.add(peCFG);
				}

				PEFile peFile = new PEFile(fileName, fileMD5, fileSHA256, peCFGs);
				peFiles.add(peFile); 
			}
			
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(peSignatures.size() == 0 || peFiles.size() == 0) {
			System.err.println("Nothing loaded from file " + exeFileName);
		}

		// for file in files
			// for function in functions
				// for signature in signatures
					// ScanningAlgorithm.scanMethod(signature.getAdjacencyMatrix(), cfg.getAdjacencyMatrix())

		for(PEFile peFile : peFiles) {
			for(PortableExecutableCFG peCFG : peFile.getFunctions()) {
				for(CFGSig signature : peSignatures) {
					if(ScanningAlgorithm.scanMethod(signature.getAdjacencyMatrix(), peCFG.getAdjacencyMatrix())) {
						Match match = new Match(peFile, signature, peCFG);
						matches.add(match);
					}
				}
			}
		}

		return;
	}

	// scan dexfile
	public static boolean scanDexFile(File dexFileFile, List<CFGSig> signatures) throws IOException {
		boolean detected = false;

		// this check is redundant now
		if(!dexFileFile.exists()) {
			System.err.println("Dexfile not found!");
			return detected;
		}

		DexBackedDexFile dexFile = null;

		// load dex file
		try {
			dexFile = DexFileFactory.loadDexFile(dexFileFile, 15);
		} catch(org.jf.util.ExceptionWithContext e) {
			System.err.println(e);
			return detected;
		} catch(java.io.FileNotFoundException e) {
			System.err.println("Cannot scan a directory: " + dexFileFile.getPath());
			return detected;
		} catch(Exception e) {
			System.err.println("Error loading file: " + dexFileFile.getPath());
			return detected;
		} 

		if(dexFile.isOdexFile()) {
			System.err.println("Odex not supported!");
			return detected;
		}

		List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(dexFile.getClasses());

		for(final ClassDef classDef: classDefs) {
			// for each method
			for(Method method: classDef.getMethods()) {
				// build CFG
				ControlFlowGraph cfg = new AndroidCFG(method); //, tryBlocks);
				
				// This is incredibly slow as it is called on all methods -
				// It would be good to put it after the conditionals for scanning
				// but that would skew the edge and vertex counts 
				if(parsedArguments.normalize()) {
					cfg.normalize();
				}

				// System.out.println(cfg.getIdentifier(false));

				++scannedFunctionCount;

				// for each signature, scan method
				for(CFGSig signature : signatures) {
					// exactMatch condition
					boolean exactCondition = signature.getEdgeCount() == cfg.getEdgeCount();
					exactCondition = exactCondition && (signature.getVertexCount() == cfg.getVertexCount());
					// partial match condition
					boolean partialCondition = signature.getEdgeCount() <= cfg.getEdgeCount();
					partialCondition = partialCondition && (signature.getVertexCount() <= cfg.getVertexCount());					
					// if a condition is met
					if((parsedArguments.exactMatch() && exactCondition) || 
					   (parsedArguments.partialMatch() && partialCondition)) {
						// if matched
					   	if((!parsedArguments.subgraphIsomorphism() && (parsedArguments.simpleMatch() || 
					   	   ScanningAlgorithm.scanMethod(signature.getAdjacencyMatrix(), cfg.getAdjacencyMatrix()))) || 
					   		(parsedArguments.subgraphIsomorphism() && 
					   		ScanningAlgorithm.scanMethodSubgraph(signature.getAdjacencyMatrix(), cfg.getAdjacencyMatrix()))) {
					   	   // scanMethod(signature.getAdjacencyMatrix(), cfg.getAdjacencyMatrix())) {
					   	   	boolean wasPreviouslyDetected = detected;
						   	detected = true;
						   	// alert unless suppressed
					   		Match match;
					   		if(!wasPreviouslyDetected) {
					   			match = new Match(dexFileFile, signature, cfg);
					   			if(parsedArguments.printMatched() && !parsedArguments.printJSON())
					   				System.out.println("FILE: " + dexFileFile.getPath());
					   		} else {
					   			// this skips rehashing the file (md5 / sha256)
					   			match = new Match(matches.get(matches.size()-1), signature, cfg);
					   		}

				   			matches.add(match);
					   		
					   		if(parsedArguments.printMatched() && parsedArguments.printJSON()) {
								System.out.println(match.toJSONString());
							} else if(parsedArguments.printMatched()) {
								System.out.println("\t" + signature.getName() + " MATCH FOUND: ");
								System.out.println("\t\t" + cfg.getIdentifier(useShortIdentifier));
							}

							signature.detected();
							// System.out.print("SIG - ");
							// System.out.println(signature.adjacencyMatrix);
							// System.out.print("FNC - ");
							// System.out.println(cfg.adjacencyMatrix);
							// System.out.println();
							if(parsedArguments.oneMatch())
								break;
						}
					}
				}
				//System.out.println();
			}
		}

		// print unmatched unless suppressed
		if(!detected && parsedArguments.printUnmatched() && !parsedArguments.printJSON()) {
			System.out.println("FILE: " + dexFileFile.getPath());
		}

		return detected;
	}

	public static ArrayList<CFGSig> parseSignatures(String signatureFileName) {
		ArrayList<CFGSig> signatures = new ArrayList<CFGSig>();
		
		try{ 
			BufferedReader br = new BufferedReader(new FileReader(signatureFileName));
			String sig = null;
			// read signatures
			while((sig = br.readLine()) != null) {
				int i = sig.indexOf(';');
				if(i == -1) {
					System.err.println("Broken sig!");
					System.exit(1);
				}

				if(sig != null && sig.startsWith("#"))
					continue;

				CFGSig cfgSig = new CFGSig(sig);
				signatures.add(cfgSig);
			}
			
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(signatures.size() == 0) {
			System.err.println("No signatures loaded from file " + signatureFileName);
		}

		return signatures;
	}

	
	
}
