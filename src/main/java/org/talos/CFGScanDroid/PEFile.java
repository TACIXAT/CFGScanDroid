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

import java.util.List;
import java.util.ArrayList;

public class PEFile {
	private String fileName;
	private String fileMD5;
	private String fileSHA256;
	private List<PortableExecutableCFG> functions;

	public PEFile(String fileName, String fileMD5, String fileSHA256, ArrayList<PortableExecutableCFG> functions) {
		this.fileName = fileName;
		this.fileMD5 = fileMD5;
		this.fileSHA256 = fileSHA256;
		this.functions = functions;
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

	public List<PortableExecutableCFG> getFunctions() {
		return functions;
	}
}