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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.IParameterSplitter;
import java.util.List;
import java.util.ArrayList;

public class JCommanderArguments {
	@Parameter(names = {"-b", "-subgraph"}, description = "Tries to match signature depth 0 to each function vertex")
	private boolean subgraphIsomorphism = false;

	@Parameter(names = {"-d", "-dump-sigs"}, description = "Dump signature for each method of each DEX file")
	private boolean dumpSignatures = false;

	@Parameter(names = {"-e", "-exact-match"}, description = "Only match complete signature CFG to function CFG", arity = 1)
	private boolean exactMatch = true;

	@Parameter(names = {"-f", "-dex-files"}, description = "DEX file(s) to run", variableArity = true)
	private List<String> dexFiles = new ArrayList<String>();

	@Parameter(names = {"-g", "-graph"}, description = "Outputs a GraphML file of the matches")
	private boolean outputGraph = false;	

	@Parameter(names = {"-h", "-short-identifier"}, description = "Do not print full CFG identifier")
	private boolean shortIdentifier = false;

	@Parameter(names = {"-i", "-simple-match"}, description = "Match exact on vertex and edge count only (fp prone)")
	private boolean simpleMatch = false;

	@Parameter(names = {"-j", "-print-json"}, description = "Print JSON for matches")
	private boolean printJSON = false;

	@Parameter(names = {"-l", "-load-sigs-from-dex"}, description = "DEX file(s) whose methods to scan with", variableArity = true, hidden = true)
	private List<String> dexSigFiles = new ArrayList<String>();

	@Parameter(names = {"-m", "-print-matched"}, description = "Print when a match is found", arity = 1)
	private boolean printMatched = true;

	@Parameter(names = {"-n", "-normalize"}, description = "Normalize the control flow graph to have a single entry point")
	private boolean normalize = false;

	@Parameter(names = {"-o", "-one-match"}, description = "Only match once on a method")
	private boolean oneMatch = false;

	@Parameter(names = {"-p", "-partial-match"}, description = "Find the signature graph within the function graph")
	private boolean partialMatch = false;

	@Parameter(names = {"-r", "-raw-signature"}, description = "Pass a signature in raw on the command line", splitter = NoSplitter.class)
	private List<String> rawSignatures = new ArrayList<String>();

	@Parameter(names = {"-s", "-sig-file"}, description = "A file containing signatures", variableArity = true)
	private List<String> signatureFiles = new ArrayList<String>();

	@Parameter(names = {"-t", "-print-statistics"}, description = "Print signature statistics after scan", arity = 1)
	private boolean printStatistics = true;

	@Parameter(names = {"-u", "-print-unmatched"}, description = "Print when no match is found", arity = 1)
	private boolean printUnmatched = true;

	public String getUsage() {
		String usage = 	"USAGE:\n"
						+ "\tMust have one of (-d|-s|-l|-r) and you should probably specify some DEX files (-f) to use too\n"
						+ "\nESSENTIALS\n"
						+ "\t-f, -dex-files\n"
						+ "\t\tDEX file(s) to run\n"
						+ "\t-d, -dump-sigs\n"
						+ "\t\tDump signature for each method of each DEX file\n"
						+ "\t-s, -sig-file\n"
						+ "\t\tA file containing signatures\n"
						+ "\t-r, -raw-signature\n"
						+ "\t\tPass a signature in raw on the command line\n"
						+ "\t-l, -load-sigs-from-dex\n"
						+ "\t\tDEX file(s) whose methods to scan with\n"
						+ "\nSCAN MODES\n"
						+ "\t-e, -exact-match\n"
						+ "\t\tOnly match complete signature CFG to function CFG\n"
						+ "\t-p, -partial-match\n"
						+ "\t\tFind the signature graph within the function graph\n"
						+ "\t-o, -one-match\n"
						+ "\t\tOnly match once on a method\n"
						+ "\t-b, -subgraph\n"
						+ "\t\tTries to match signature depth 0 to each function vertex\n"
						+ "\t-i, -simple-match\n"
						+ "\t\tMatch exact on vertex and edge count only (fp prone)\n"
						+ "\t-n, -normalize\n"
						+ "\t\tNormalize the control flow graph to have a single entry point\n"
						+ "\nOUTPUT\n"	
						+ "\t-j, -print-json\n"
						+ "\t\tPrint JSON for matches\n"
						+ "\t-g, -graph\n"
						+ "\t\tOutputs a GraphML file of the matches\n"
						+ "\t-m, -print-matched\n"
						+ "\t\tPrint when a match is found\n"
						+ "\t-u, -print-unmatched\n"
						+ "\t\tPrint when no match is found\n"
						+ "\t-t, -print-statistics\n"
						+ "\t\tPrint signature statistics after scan\n"
						+ "\t-h, -short-identifier\n"
						+ "\t\tDo not print full CFG identifier\n";
						
		return usage;
	}

	public boolean outputGraph() {
		return outputGraph;
	}

	public List<String> getSignatureFiles() {
		return signatureFiles;
	}

	public List<String> getRawSignatures() {
		return rawSignatures;
	}

	public boolean oneMatch() {
		return oneMatch;
	}

	public boolean normalize() {
		return normalize;
	}

	public boolean dumpSignatures() {
		return dumpSignatures;
	}

	public boolean simpleMatch() {
		return simpleMatch;
	}

	public List<String> getDexFiles() {
		return dexFiles;
	}

	public boolean partialMatch() {
		return partialMatch;
	} 

	public boolean exactMatch() {
		return exactMatch;
	}

	public boolean shortIdentifier() {
		return shortIdentifier;
	}

	public boolean printStatistics() {
		return printStatistics;
	}

	public void setSimpleMatch(boolean value) {
		simpleMatch = value;
	}

	public void setPartialMatch(boolean value) {
		partialMatch = value;
	} 

	public void setExactMatch(boolean value) {
		exactMatch = value;
	}

	public void setNormalize(boolean value) {
		normalize = value;
	}

	public boolean printMatched() {
		return printMatched;
	}

	public boolean printJSON() {
		return printJSON;
	}

	public boolean subgraphIsomorphism() {
		return subgraphIsomorphism;
	}

	public boolean printUnmatched() {
		return printUnmatched;
	}

	public static class NoSplitter implements IParameterSplitter {
	    @Override
	    public List<String> split(String value) {
	        List<String> result = new ArrayList<String>();
	        result.add(value);
	        return result;
		}

	}
}