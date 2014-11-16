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

import com.google.common.io.Files;
import com.google.common.hash.Hashing;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Match {
	private String fileName;
	private String fileMD5;
	private String fileSHA256;
	private CFGSig signature;
	private ControlFlowGraph cfg;

	public Match(File file, CFGSig signature, ControlFlowGraph cfg) {
		this.fileName = file.getPath();
		try {
			this.fileMD5 = Files.hash(file , Hashing.md5()).toString();
			this.fileSHA256 = Files.hash(file , Hashing.sha256()).toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.cfg = cfg;
		this.signature = signature;
	}

	public Match(Match previousMatch, CFGSig signature, ControlFlowGraph cfg) {
		this.fileName = previousMatch.getFileName();
		this.fileMD5 = previousMatch.getFileMD5();
		this.fileSHA256 = previousMatch.getFileSHA256();
		this.cfg = cfg;
		this.signature = signature;
	}

	public String getFileName() {
		return fileName;
	}

	public String getFileMD5() {
		return fileMD5;
	}

	public String getFileSHA256() {
		return fileSHA256;
	}

	public CFGSig getSignature() {
		return signature;
	}

	public ControlFlowGraph getControlFlowGraph() {
		return cfg;
	}

	public String toJSONString() {
		JSONObject output = new JSONObject();
		output.put("fileName", fileName);
		output.put("fileSHA256", fileSHA256);
		output.put("fileMD5", fileMD5);
		// output.put("signature", signature.getStringSignature());
		output.put("signature", signature.getName());
		// output.put("methodBytes", cfg.getMethodBytesAsHexString());
		return output.toJSONString();
	}
}
