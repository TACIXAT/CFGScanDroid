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

import cern.colt.matrix.impl.SparseDoubleMatrix2D;

public class PortableExecutableCFG extends ControlFlowGraph {
	String offset;
	public PortableExecutableCFG(String functionName, String functionBytes, String functionAddress, SparseDoubleMatrix2D adjacencyMatrix) {
		this.identifier = functionName;
		this.shortIdentifier = functionAddress;
		this.methodBytes = functionBytes;
		this.offset = functionAddress;
		this.adjacencyMatrix = adjacencyMatrix;
		this.vertexCount = adjacencyMatrix.columns();
		this.edgeCount = adjacencyMatrix.cardinality();
	}	

	public String getMethodBytesAsHexString() {
		return methodBytes;
	}

	public String getOffset() {
		return offset;
	}
}
