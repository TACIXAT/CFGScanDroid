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
import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;

public abstract class ControlFlowGraph {
	protected String identifier;
	protected String shortIdentifier;
	protected SparseDoubleMatrix2D adjacencyMatrix;
	protected int edgeCount;
	protected int vertexCount;
	protected String methodBytes = null;

	// public ControlFlowGraph(String identifier, SparseDoubleMatrix2D adjacencyMatrix) {
	// 	this.identifier = identifier;
	// 	this.shortIdentifier = identifier;
	// 	this.adjacencyMatrix = adjacencyMatrix;
	// 	this.vertexCount = adjacencyMatrix.columns();
	// 	this.edgeCount = adjacencyMatrix.cardinality();
	// }

	public abstract String getMethodBytesAsHexString();

	public String getIdentifier(boolean shortForm) {
		if(shortForm)
			return shortIdentifier;

		return identifier;
	}

	public SparseDoubleMatrix2D getAdjacencyMatrix() {
		return adjacencyMatrix;
	}

	public int getVertexCount() {
		return vertexCount;
	}

	public int getEdgeCount() {
		return edgeCount;
	}

	public void normalize() {
		int adjacencyMatrixSize = adjacencyMatrix.columns();
		
		if(adjacencyMatrixSize < 1)
			return;

		DoubleMatrix2D selector = new SparseDoubleMatrix2D(1, adjacencyMatrixSize);
		selector.set(0, 0, 1.0);
		// get vertices reachable from V0
		int cardinality = selector.cardinality();
		int lastCardinality = 0;

		DoubleDoubleFunction merge = new DoubleDoubleFunction() {
			public double apply(double a, double b) { 
				return (a > 0 || b > 0) ? 1 : 0; 
			}
		};

		while(cardinality != lastCardinality) {
			lastCardinality = cardinality;
			selector.assign(Algebra.DEFAULT.mult(selector, adjacencyMatrix), merge);
			cardinality = selector.cardinality();
		}

		if(cardinality == adjacencyMatrixSize) {
			return;
		}

		SparseDoubleMatrix2D reduced = new SparseDoubleMatrix2D(cardinality, cardinality);
		int iCurrentVertex = 0;
		for(int i=0; i<adjacencyMatrixSize; ++i) {
			if(selector.get(0, i) != 0.0) {
				int jCurrentVertex = 0;
				for(int j=0; j<adjacencyMatrixSize; ++j) {
					if(selector.get(0, j) != 0.0){
						reduced.set(iCurrentVertex, jCurrentVertex, adjacencyMatrix.get(i, j));
						++jCurrentVertex;
					}
				}
				++iCurrentVertex;
			}
		}

		adjacencyMatrix = reduced;
		vertexCount = adjacencyMatrix.columns();
		edgeCount = adjacencyMatrix.cardinality();
	}
}
