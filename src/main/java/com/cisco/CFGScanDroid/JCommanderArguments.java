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
	@Parameter(names = {"-n", "-normalize"}, description = "Normalize the control flow graph to have a single entry point")
	private boolean normalize = false;

	@Parameter(names = {"-o", "-one-match"}, description = "Only match once on a method")
	private boolean oneMatch = false;

	@Parameter(names = {"-r", "-raw-signature"}, description = "Pass a signature in raw on the command line", splitter = NoSplitter.class)
	private List<String> rawSignatures = new ArrayList<String>();

	@Parameter(names = {"-g", "-subgraph"}, description = "Tries to match signature depth 0 to each function vertex")
	private boolean subgraphIsomorphism = false;

	@Parameter(names = {"-p", "-partial-match"}, description = "Find the signature graph within the function graph")
	private boolean partialMatch = false;

	@Parameter(names = {"-e", "-exact-match"}, description = "Only match complete signature CFG to function CFG", arity = 1)
	private boolean exactMatch = true;

	@Parameter(names = {"-t", "-print-statistics"}, description = "Print signature statistics after scan", arity = 1)
	private boolean printStatistics = true;

	@Parameter(names = {"-m", "-print-matched"}, description = "Print when a match is found", arity = 1)
	private boolean printMatched = true;

	@Parameter(names = {"-u", "-print-unmatched"}, description = "Print when no match is found", arity = 1)
	private boolean printUnmatched = true;

	@Parameter(names = {"-i", "-simple-match"}, description = "Match exact on vertex and edge count only (fp prone)")
	private boolean simpleMatch = false;

	@Parameter(names = {"-h", "-short-identifier"}, description = "Do not print full CFG identifier")
	private boolean shortIdentifier = false;

	@Parameter(names = {"-f", "-dex-files"}, description = "DEX file(s) to run", variableArity = true)
	private List<String> dexFiles = new ArrayList<String>();

	@Parameter(names = {"-d", "-dump-sigs"}, description = "Dump signature for each method of each DEX file")
	private boolean dumpSignatures = false;

	@Parameter(names = {"-s", "-sig-file"}, description = "A file containing signatures", variableArity = true)
	private List<String> signatureFiles = new ArrayList<String>();

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