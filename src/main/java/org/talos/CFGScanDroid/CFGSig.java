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

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.list.IntArrayList;
import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.list.IntArrayList;

import java.util.List;
import java.util.ArrayList;

// name;node_count;adj_list

public class CFGSig {
	private String name;
	private String stringSignature;
	private SparseDoubleMatrix2D adjacencyMatrix;
	private int edgeCount, vertexCount;
	private long detectionCount = 0;

	public String getName() {
		return name;
	}

	public String getStringSignature() {
		return stringSignature;
	}

	public SparseDoubleMatrix2D getAdjacencyMatrix() {
		return adjacencyMatrix;
	}

	public int getEdgeCount() {
		return edgeCount;
	}

	public int getVertexCount() {
		return vertexCount;
	}

	public long getDetectionCount() {
		return detectionCount;
	}

	public void detected() {
		detectionCount++;
	}

	public void normalize() {
		// System.out.println(adjacencyMatrix);
		int adjacencyMatrixSize = adjacencyMatrix.columns();

		if(adjacencyMatrixSize < 1)
			return;

		DoubleMatrix2D selector = new SparseDoubleMatrix2D(1, adjacencyMatrixSize);
		selector.set(0, 0, 1.0);
		// System.out.println(selector);
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
			// selector = Algebra.DEFAULT.mult(selector, adjacencyMatrix);
			selector.assign(Algebra.DEFAULT.mult(selector, adjacencyMatrix), merge);
			// System.out.println(selector);
			cardinality = selector.cardinality();
		}

		// System.out.println(selector);

		if(cardinality == adjacencyMatrixSize) {
			return;
		}

		// IntArrayList nonZeros = new IntArrayList();
		// IntArrayList unusedInt = new IntArrayList();
		// DoubleArrayList unusedDouble = new DoubleArrayList();
		// selector.getNonZeros(unusedInt, nonZeros, unusedDouble);
		// SparseDoubleMatrix2D reduced = new SparseDoubleMatrix2D(adjacencyMatrix.viewSelection(nonZeros.elements(), nonZeros.elements()).toArray());

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
		// System.out.println(reduced);
		// System.out.println("=======");
		adjacencyMatrix = reduced;
		vertexCount = adjacencyMatrix.columns();
		edgeCount = adjacencyMatrix.cardinality();
	}

	public CFGSig(ControlFlowGraph cfg) {
		this.adjacencyMatrix = cfg.getAdjacencyMatrix();
		this.vertexCount = cfg.getVertexCount();
		this.edgeCount = cfg.getEdgeCount();
		this.name = cfg.getIdentifier(false);

		this.stringSignature = "";
		stringSignature += name + ";";
		stringSignature += adjacencyMatrix.rows();
		
		DoubleArrayList unused = new DoubleArrayList();
		IntArrayList indices = null;
		
		for(int i=0; i<adjacencyMatrix.rows(); ++i) {
			DoubleMatrix1D row = adjacencyMatrix.viewRow(i);
			if(row.cardinality() > 0) {
				stringSignature += ";" + i + ":";
				indices = new IntArrayList();
				row.getNonZeros(indices, unused);

				for(int j=0; j<indices.size(); ++j) {
					if(j > 0) 
						stringSignature += ",";

					stringSignature += indices.get(j);
				}
			}
		}
		// build sig from adjmat
	}

	public CFGSig(String sigString) {
		stringSignature = sigString;
		// System.out.println(sigString);
		String[] segments = sigString.split(";");
		if(segments.length < 3){
			System.err.println("Bad sig!");
			System.err.println(sigString);
			System.exit(1);
		}

		name = segments[0];
		int size = Integer.parseInt(segments[1]);
		adjacencyMatrix = new SparseDoubleMatrix2D(size, size);
		List<String> lookup = new ArrayList<String>();

		int i;
		for(i=2; i<segments.length; ++i) {
			if(segments[i].indexOf(":") == -1) {
				System.err.println("Bad signature segment near " + segments[i] + ".");
				System.exit(1);
			}

			String[] subsig = segments[i].split(":");

			if(subsig.length != 2) {
				System.err.println("Bad signature segment near " + segments[i] + ".");
				System.exit(1);
			}

			String basicBlock = subsig[0];
			//System.out.println(basicBlock);
			String[] destinations = subsig[1].split(",");
			int basicBlockIdx = lookup.indexOf(basicBlock);
			if(basicBlockIdx == -1) {
				lookup.add(basicBlock);
				if(lookup.size() > size){
					System.err.println("Basic block count is less than actual number of basic blocks.");
					System.exit(1);
				}
				basicBlockIdx = lookup.indexOf(basicBlock);
			}

			for(String dest: destinations) {
				int destIdx = lookup.indexOf(dest);
				if(destIdx == -1) {
					lookup.add(dest);
					if(lookup.size() > size) {
						System.err.println("Basic block count is less than actual number of basic blocks.");
						System.exit(1);
					}
					destIdx = lookup.indexOf(dest);
				}

				assert basicBlockIdx != -1 && destIdx != -1;

				adjacencyMatrix.set(basicBlockIdx, destIdx, 1.0);
			}
		}

		edgeCount = adjacencyMatrix.cardinality();
		vertexCount = size;
		//System.out.println(adjacencyMatrix);
	}
}
