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

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.list.IntArrayList;
import cern.colt.list.DoubleArrayList;
import cern.colt.function.DoubleDoubleFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.HopcroftKarpBipartiteMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

public class ScanningAlgorithm {

	public static final int MAX_DEPTH = 512;

	// TODO: refine bfsCompare to return a status code rather than true false
	// this will allow us to leave this function early once there are insufficient nodes
	// to satisfy signature (or at least not consider that node's children)
	public static boolean scanMethodSubgraph(SparseDoubleMatrix2D signatureAdjacencyMatrix, SparseDoubleMatrix2D functionAdjacencyMatrix){
		// System.out.println("DEPTH: " + 0);
		// System.out.println(signatureAdjacencyMatrix);
		// System.out.println(functionAdjacencyMatrix);
		int signatureNodeCount = signatureAdjacencyMatrix.rows();
		int functionNodeCount = functionAdjacencyMatrix.rows();
		SparseDoubleMatrix2D candidateList = new SparseDoubleMatrix2D(signatureNodeCount, functionNodeCount);
		
		int[] signatureDepths = new int[signatureNodeCount];
		int[] functionDepths = new int[functionNodeCount];

		for(int functionEntry=0; functionEntry<functionNodeCount; ++functionEntry) {
			// arrays for tracking depths
			// function finds all nodes with no branches to them
			// first node (entry point) is always set at depth 0
			// System.out.println(functionEntry);
			candidateList.assign(0);

			Arrays.fill(signatureDepths, -1);
			Arrays.fill(functionDepths, -1);
		
			signatureDepths[0] = 0;
			functionDepths[functionEntry] = 0;

			// all nodes at depth 0 go into this vector
			SparseDoubleMatrix2D signatureVector = new SparseDoubleMatrix2D(1, signatureNodeCount);
			SparseDoubleMatrix2D functionVector = new SparseDoubleMatrix2D(1, functionNodeCount);

			// First node should be always be an entry point
			signatureVector.set(0, 0, 1.0);
			functionVector.set(0, functionEntry, 1.0);

			// get total children at depth 0+1
			// check function has sufficient nodes to satisfy sig at this depth
			// System.out.println("\t" + Algebra.DEFAULT.mult(signatureVector, signatureAdjacencyMatrix).cardinality());
			// System.out.println("\t" + Algebra.DEFAULT.mult(functionVector, functionAdjacencyMatrix).cardinality());
			if(Algebra.DEFAULT.mult(signatureVector, signatureAdjacencyMatrix).cardinality() > 
				Algebra.DEFAULT.mult(functionVector, functionAdjacencyMatrix).cardinality()) {
				// System.out.println("Insufficient function basic blocks at depth 1 to satisfy signature!");
				continue;
			}

			// build candidate list 
			candidateList.set(0, functionEntry, 1.0);

			// check bipartite match
			if(!bipartiteMatchingDepth(candidateList, signatureDepths, functionDepths, 0)) {
				// System.out.println("BIPARTITE MATCHING FAILED!");
				continue;
			}

			// check children recursively
			if(bfsCompare(candidateList, signatureAdjacencyMatrix, functionAdjacencyMatrix, signatureDepths, functionDepths, 1)) {
				return true;
			}
			// System.out.println();
		}

		return false;
	}

	public static boolean scanMethod(SparseDoubleMatrix2D signatureAdjacencyMatrix, SparseDoubleMatrix2D functionAdjacencyMatrix){
		// System.out.println("DEPTH: " + 0);
		// System.out.println(signatureAdjacencyMatrix);
		// System.out.println(functionAdjacencyMatrix);
		int signatureNodeCount = signatureAdjacencyMatrix.rows();
		int functionNodeCount = functionAdjacencyMatrix.rows();
		SparseDoubleMatrix2D candidateList = new SparseDoubleMatrix2D(signatureNodeCount, functionNodeCount);
		
		int[] signatureDepths = new int[signatureNodeCount];
		int[] functionDepths = new int[functionNodeCount];

		// arrays for tracking depths
		// function finds all nodes with no branches to them
		// first node (entry point) is always set at depth 0
		signatureDepths = initializeDepths(signatureAdjacencyMatrix, signatureDepths);
		functionDepths = initializeDepths(functionAdjacencyMatrix, functionDepths);

		// all nodes at depth 0 go into this vector
		SparseDoubleMatrix2D signatureVector = new SparseDoubleMatrix2D(1, signatureNodeCount);
		SparseDoubleMatrix2D functionVector = new SparseDoubleMatrix2D(1, functionNodeCount);

		// code for handling multiple entry points
		// creates a null node 0 effectively, see VF2 Algorithm
		// TODO: use depth arrays instead of searching again
		// find all sig nodes at depth 0
		for(int j=0; j<signatureNodeCount; ++j) {
			if(signatureAdjacencyMatrix.viewColumn(j).cardinality() == 0)
				signatureVector.set(0, j, 1.0);
		}

		// find function nodes at depth 0
		for(int j=0; j<functionNodeCount; ++j) {
			if(functionAdjacencyMatrix.viewColumn(j).cardinality() == 0) {
				functionVector.set(0, j, 1.0);
				// System.out.println("Entry node: " + j);
			}
		}

		// First node should be always be an entry point
		signatureVector.set(0, 0, 1.0);
		functionVector.set(0, 0, 1.0);
		
		// check current depth counts
		if(signatureVector.cardinality() > functionVector.cardinality()) {
			// System.out.println("Insufficient basic blocks at depth 0 to satisfy signature!");
			return false;
		}

		// get total children at depth 0+1
		// check function has sufficient nodes to satisfy sig at this depth
		if(Algebra.DEFAULT.mult(signatureVector, signatureAdjacencyMatrix).cardinality() > 
			Algebra.DEFAULT.mult(functionVector, functionAdjacencyMatrix).cardinality()) {
			// System.out.println("Insufficient function basic blocks at depth 1 to satisfy signature!");
			return false;
		}

		// build candidate list 
		IntArrayList signatureNonZeros = new IntArrayList();
		IntArrayList functionNonZeros = new IntArrayList();
		IntArrayList unusedInt = new IntArrayList();
		DoubleArrayList unusedDouble = new DoubleArrayList();

		SparseDoubleMatrix2D functionSelector = new SparseDoubleMatrix2D(1, functionNodeCount);
		SparseDoubleMatrix2D signatureSelector = new SparseDoubleMatrix2D(1, signatureNodeCount);

		signatureVector.getNonZeros(unusedInt, signatureNonZeros, unusedDouble);
		functionVector.getNonZeros(unusedInt, functionNonZeros, unusedDouble);

		for(int i=0; i<signatureNonZeros.size(); ++i) {
			// for node N, this sets the Nth bit of the vector to 1.0
			// this will select the children of each sig node, cardinality gives their count
			signatureSelector.assign(0.0);
			signatureSelector.set(0, signatureNonZeros.get(i), 1.0);
			int signatureChildCount = Algebra.DEFAULT.mult(signatureSelector, signatureAdjacencyMatrix).cardinality();

			for(int j=0; j<functionNonZeros.size(); ++j) {
				// get function node child count
				functionSelector.assign(0.0);
				functionSelector.set(0, functionNonZeros.get(j), 1.0);
				int functionChildCount = Algebra.DEFAULT.mult(functionSelector, functionAdjacencyMatrix).cardinality();

				// if function node has sufficient children
				// mark it as a candidate for sig node
				if(signatureChildCount <= functionChildCount){
					candidateList.set(signatureNonZeros.get(i), functionNonZeros.get(j), 1.0);
				}
			}
		}

		// check bipartite match
		if(!bipartiteMatchingDepth(candidateList, signatureDepths, functionDepths, 0)) {
			// System.out.println("BIPARTITE MATCHING FAILED!");
			return false;
		}

		// check children recursively
		if(bfsCompare(candidateList, signatureAdjacencyMatrix, functionAdjacencyMatrix, signatureDepths, functionDepths, 1)) {
			return true;
		}

		return false;
	}

	public static boolean bfsCompare(	SparseDoubleMatrix2D candidateList, 
										SparseDoubleMatrix2D signatureAdjacencyMatrix, 
										SparseDoubleMatrix2D functionAdjacencyMatrix, 
										int[] signatureDepths,
										int[] functionDepths,
										int depth) {
		// System.out.println("DEPTH: " + depth);
		// get vectors for nodes at this depth by retrieving parents (depth - 1)
		DoubleMatrix2D signatureVector = new SparseDoubleMatrix2D(1, signatureDepths.length);
		DoubleMatrix2D functionVector = new SparseDoubleMatrix2D(1, functionDepths.length);
		DoubleMatrix2D parentSigVector = new SparseDoubleMatrix2D(1, signatureDepths.length);
		DoubleMatrix2D parentFunVector = new SparseDoubleMatrix2D(1, functionDepths.length);

		// get parents
		for(int i=0; i<signatureDepths.length; ++i) {
			if(signatureDepths[i] == (depth-1))
				parentSigVector.set(0, i, 1.0);
		}

		for(int i=0; i<functionDepths.length; ++i) {
			if(functionDepths[i] == (depth-1))
				parentFunVector.set(0, i, 1.0);
		}

		// get current nodes
		signatureVector = Algebra.DEFAULT.mult(parentSigVector, signatureAdjacencyMatrix);
		functionVector = Algebra.DEFAULT.mult(parentFunVector, functionAdjacencyMatrix);

		// mark nodes' depths if they are not currently set
		for(int i=0; i<signatureDepths.length; ++i) {
			if(signatureVector.get(0,i) > 0 && signatureDepths[i] < 0)
				signatureDepths[i] = depth;
		}

		for(int i=0; i<functionDepths.length; ++i) {
			if(functionVector.get(0,i) > 0 && functionDepths[i] < 0)
				functionDepths[i] = depth;
		}

		// make signatureVector only have nodes at this depth
		for(int i=0; i<signatureDepths.length; ++i) {
			if(signatureDepths[i] != depth) {
				signatureVector.set(0, i, 0.0);
			}
		}

		// make signatureVector only have nodes at this depth
		for(int i=0; i<functionDepths.length; ++i) {
			if(functionDepths[i] != depth) {
				functionVector.set(0, i, 0.0);
			}
		}

		// check signode count <= funnode count
		if(	Algebra.DEFAULT.mult(signatureVector, signatureAdjacencyMatrix).cardinality() > 
			Algebra.DEFAULT.mult(functionVector, functionAdjacencyMatrix).cardinality()) {
			// System.out.println("Insufficient function basic blocks at depth " + depth + " to satisfy signature!");
			return false;
		}

		// TODO: edge check needed?
		
		// build candidate list 
		candidateList = buildCandidateList(	candidateList, 
											signatureAdjacencyMatrix, 
											functionAdjacencyMatrix, 
											signatureDepths, 
											functionDepths, 
											signatureVector, 
											functionVector, 
											depth);

		if(bipartiteMatchingVector(candidateList, signatureVector, functionVector, signatureDepths, functionDepths)) {
		//if(bipartiteMatchingDepth(candidateList, signatureDepths, functionDepths, depth)) {
			

			// check if we've reached end of graph
			if(Algebra.DEFAULT.mult(signatureVector, signatureAdjacencyMatrix).cardinality() == 0)
				return true;

		} else {
			// System.out.println("Bipartite matching failed!");
			return false;
		}

		if(depth > MAX_DEPTH)
			return false;

		return bfsCompare(candidateList, signatureAdjacencyMatrix, functionAdjacencyMatrix, signatureDepths, functionDepths, depth+1);
	}

	public static SparseDoubleMatrix2D buildCandidateList(	SparseDoubleMatrix2D candidateList, 
															SparseDoubleMatrix2D signatureAdjacencyMatrix, 
															SparseDoubleMatrix2D functionAdjacencyMatrix, 
															int[] signatureDepths,
															int[] functionDepths,
															DoubleMatrix2D signatureVector,
															DoubleMatrix2D functionVector,
															int depth) {
		IntArrayList signatureNonZeros = new IntArrayList();
		IntArrayList functionNonZeros = new IntArrayList();
		IntArrayList unusedInt = new IntArrayList();
		DoubleArrayList unusedDouble = new DoubleArrayList();

		// there's  a reason for using doublematrix instead of vector, I don't remember though
		// mult might now support vector * matrix
		SparseDoubleMatrix2D functionSelector = new SparseDoubleMatrix2D(1, functionDepths.length);
		SparseDoubleMatrix2D signatureSelector = new SparseDoubleMatrix2D(1, signatureDepths.length);

		signatureVector.getNonZeros(unusedInt, signatureNonZeros, unusedDouble);
		functionVector.getNonZeros(unusedInt, functionNonZeros, unusedDouble);

		DoubleMatrix2D signatureNodeIncestVector = signatureVector.copy();
		signatureNodeIncestVector.assign(0.0);

		DoubleMatrix2D functionNodeIncestVector = functionVector.copy();
		functionNodeIncestVector.assign(0.0);

		// don't break out into function, has incest vector as 'side effect'
		// bipartite matching on parent vectors
		for(int i=0; i<signatureNonZeros.size(); ++i) {
			// for node N, this sets the Nth bit of the vector to 1.0
			signatureSelector.assign(0.0);
			signatureSelector.set(0, signatureNonZeros.get(i), 1.0);
			int signatureChildCount = Algebra.DEFAULT.mult(signatureSelector, signatureAdjacencyMatrix).cardinality();
			DoubleMatrix2D signatureNodeParentVector = Algebra.DEFAULT.mult(signatureSelector, Algebra.DEFAULT.transpose(signatureAdjacencyMatrix));

			// use the correct count
			int signatureParentCount = signatureNodeParentVector.cardinality();
			// disregard incest condition for bipartite matching
			for(int iParent = 0; iParent < signatureNodeParentVector.columns(); ++iParent) {
				if(signatureNodeParentVector.get(0, iParent) > 0 && signatureDepths[iParent] == depth) {
					signatureNodeParentVector.set(0, iParent, 0.0);
					signatureNodeIncestVector.set(0, i, 1.0);
				}
			}

			for(int j=0; j<functionNonZeros.size(); ++j) {
				functionSelector.assign(0.0);
				functionSelector.set(0, functionNonZeros.get(j), 1.0);
				int functionChildCount = Algebra.DEFAULT.mult(functionSelector, functionAdjacencyMatrix).cardinality();
				DoubleMatrix2D functionNodeParentVector = Algebra.DEFAULT.mult(functionSelector, Algebra.DEFAULT.transpose(functionAdjacencyMatrix));
				// System.out.println(functionNodeParentVector);

				// use the correct count 
				int functionParentCount = functionNodeParentVector.cardinality();	
				// only bipartite match on non incest conditions
				for(int iParent = 0; iParent < functionNodeParentVector.columns(); ++iParent) {
					if(functionNodeParentVector.get(0, iParent) > 0 && functionDepths[iParent] == depth) {
						functionNodeParentVector.set(0, iParent, 0.0);
						functionNodeIncestVector.set(0, j, 1.0);
					}
				}
				
				if(	signatureChildCount <= functionChildCount && 
					signatureParentCount <= functionParentCount && 
					bipartiteMatchingVector(candidateList, signatureNodeParentVector, functionNodeParentVector, signatureDepths, functionDepths)) {
					// set candidatelist
					candidateList.set(signatureNonZeros.get(i), functionNonZeros.get(j), 1.0);
				}
			}
		}

		// reduce
		candidateList = reduceCandidateList(signatureNonZeros, functionNonZeros, candidateList);

		candidateList = checkPreviouslyVisitedChildren(	signatureNonZeros, 
														functionNonZeros, 
														signatureAdjacencyMatrix, 
														functionAdjacencyMatrix,
														signatureDepths,
														functionDepths,
														candidateList);

		// System.out.println(signatureNodeIncestVector);
		// System.out.println(functionNodeIncestVector);
		// System.out.println(candidateList);
		// reduce candidacy based on incest relation
		// for node in signature incest vector
		candidateList = checkIncestConditionParents(	signatureNodeIncestVector,
														functionNodeIncestVector,
														signatureAdjacencyMatrix, 
														functionAdjacencyMatrix,
														signatureDepths,
														functionDepths,
														depth,
														candidateList);

		// System.out.println(candidateList);
			// get sig parents
			// for node in function node incest vector
				// get fn parents
				// if fn node is candidate for sig node
					// bipartite matching on parents
					// reduce candidacy if no match


		return candidateList;
	}

	public static SparseDoubleMatrix2D checkIncestConditionParents(	DoubleMatrix2D signatureNodeIncestVector,
																	DoubleMatrix2D functionNodeIncestVector,
																	SparseDoubleMatrix2D signatureAdjacencyMatrix, 
																	SparseDoubleMatrix2D functionAdjacencyMatrix,
																	int[] signatureDepths,
																	int[] functionDepths,
																	int depth,
																	SparseDoubleMatrix2D candidateList) {

		SparseDoubleMatrix2D signatureSelector = new SparseDoubleMatrix2D(1, signatureDepths.length);
		SparseDoubleMatrix2D functionSelector = new SparseDoubleMatrix2D(1, functionDepths.length);

		for(int i=0; i<signatureNodeIncestVector.size(); ++i) {
			if(signatureNodeIncestVector.get(0, i) == 0.0)
				continue;

			signatureSelector.assign(0.0);
			signatureSelector.set(0, i, 1.0);

			DoubleMatrix2D signatureNodeParentVector = Algebra.DEFAULT.mult(signatureSelector, Algebra.DEFAULT.transpose(signatureAdjacencyMatrix));

			// only bipartite match on incest parents
			for(int iParent = 0; iParent < signatureNodeParentVector.columns(); ++iParent) {
				if(signatureNodeParentVector.get(0, iParent) > 0 && signatureDepths[iParent] != depth) {
					signatureNodeParentVector.set(0, iParent, 0.0);
				}
			}

			int signatureIncestParentCount = signatureNodeParentVector.cardinality();

			for(int j=0; j<functionNodeIncestVector.size(); ++j) {
				if(candidateList.get(i, j) == 0.0)
					continue;

				functionSelector.assign(0.0);
				functionSelector.set(0, j, 1.0);

				DoubleMatrix2D functionNodeParentVector = Algebra.DEFAULT.mult(functionSelector, Algebra.DEFAULT.transpose(functionAdjacencyMatrix));

				// only bipartite match on incest parents
				for(int iParent = 0; iParent < functionNodeParentVector.columns(); ++iParent) {
					if(functionNodeParentVector.get(0, iParent) > 0 && functionDepths[iParent] != depth) {
						functionNodeParentVector.set(0, iParent, 0.0);
					}
				}

				int functionIncestParentCount = functionNodeParentVector.cardinality();

				// bipartite matching, if !sufficient
				if(	candidateList.get(i, j) > 0 && (signatureIncestParentCount > functionIncestParentCount ||
					!bipartiteMatchingVector(candidateList, signatureNodeParentVector, functionNodeParentVector, signatureDepths, functionDepths))) {
					// remove candidacy
					candidateList.set(i, j, 0.0);
				}
			}
		}

		return candidateList;
	}

	public static SparseDoubleMatrix2D checkPreviouslyVisitedChildren(	IntArrayList signatureNonZeros, 
																		IntArrayList functionNonZeros,
																		SparseDoubleMatrix2D signatureAdjacencyMatrix, 
																		SparseDoubleMatrix2D functionAdjacencyMatrix,
																		int[] signatureDepths,
																		int[] functionDepths,
	 																	SparseDoubleMatrix2D candidateList) {
		
		SparseDoubleMatrix2D signatureSelector = new SparseDoubleMatrix2D(1, signatureDepths.length);
		SparseDoubleMatrix2D functionSelector = new SparseDoubleMatrix2D(1, functionDepths.length);

		// reduce candidacy based on previously visited children
		for(int i=0; i<signatureNonZeros.size(); ++i) {
			// for node N, this sets the Nth bit of the vector to 1.0
			signatureSelector.assign(0.0);
			signatureSelector.set(0, signatureNonZeros.get(i), 1.0);

			// get children at depth <= current
			DoubleMatrix2D signatureNodeChildVector = Algebra.DEFAULT.mult(signatureSelector, signatureAdjacencyMatrix);

			// remove signature node's unvisited children
			for(int iChild = 0; iChild < signatureNodeChildVector.columns(); ++iChild) {
				if(signatureNodeChildVector.get(0, iChild) > 0 && signatureDepths[iChild] == -1) { // for the oposite conditional && signatureDepths[iChild] <= depth) {
					signatureNodeChildVector.set(0, iChild, 0.0);
				}
			}

			int signatureVisitedChildCount = signatureNodeChildVector.cardinality();

			for(int j=0; j<functionNonZeros.size(); ++j) {
				functionSelector.assign(0.0);
				functionSelector.set(0, functionNonZeros.get(j), 1.0);
				// get children at depth <= current
				DoubleMatrix2D functionNodeChildVector = Algebra.DEFAULT.mult(functionSelector, functionAdjacencyMatrix);

				// remove function node's unvisited children
				for(int iChild = 0; iChild < functionNodeChildVector.columns(); ++iChild) {
					if(functionNodeChildVector.get(0, iChild) > 0 && functionDepths[iChild] == -1) { // for the oposite conditional && functionDepths[iChild] <= depth) {
						functionNodeChildVector.set(0, iChild, 0.0);
					}
				}

				int functionVisitedChildCount = functionNodeChildVector.cardinality();
				
				// bipartite matching, if !sufficient
				if(	candidateList.get(signatureNonZeros.get(i), functionNonZeros.get(j)) > 0 && 
					(signatureVisitedChildCount > functionVisitedChildCount ||
					!bipartiteMatchingVector(candidateList, signatureNodeChildVector, functionNodeChildVector, signatureDepths, functionDepths))) {
					// remove candidacy
					candidateList.set(signatureNonZeros.get(i), functionNonZeros.get(j), 0.0);
				}
			}
		}

		return candidateList;
	}

	public static SparseDoubleMatrix2D reduceCandidateList(IntArrayList signatureNonZeros, IntArrayList functionNonZeros, SparseDoubleMatrix2D candidateList) {
		// reduce
		for(int i=0; i<signatureNonZeros.size(); ++i) {
			// find nodes with only one possible candidate
			if(candidateList.viewRow(signatureNonZeros.get(i)).cardinality() == 1) {
				int nonZero = -1;
				for(int j=0; j<functionNonZeros.size(); ++j) {
					if(candidateList.get(signatureNonZeros.get(i), functionNonZeros.get(j)) != 0.0) {
						nonZero = functionNonZeros.get(j);
						break;
					}
				}	

				if(nonZero == -1){
					System.out.println("REDUCE LOOP IS BROKEN - J NOT FOUND");
					System.out.println(signatureNonZeros);
					System.out.println(functionNonZeros);
					System.out.println(candidateList);
					System.out.println();
					break;
				}

				// remove candidacy of other nodes since it /has/ to be that one node
				for(int x=0; x<signatureNonZeros.size(); ++x) {
					if(x != i) {
						candidateList.set(signatureNonZeros.get(x), nonZero, 0.0);
					}
				}
			}
		}

		return candidateList;
	}

	public static boolean bipartiteMatchingVector(DoubleMatrix2D candidateList, DoubleMatrix2D signatureNodeVector, DoubleMatrix2D functionNodeVector, int[] signatureDepths, int[] functionDepths) {
		UndirectedGraph<String, DefaultEdge> g = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);

		IntArrayList signatureNonZeros = new IntArrayList();
		IntArrayList functionNonZeros = new IntArrayList();
		IntArrayList unusedInt = new IntArrayList();
		DoubleArrayList unusedDouble = new DoubleArrayList();

		// get set column indices for signature vector and function vector
		signatureNodeVector.getNonZeros(unusedInt, signatureNonZeros, unusedDouble);
		functionNodeVector.getNonZeros(unusedInt, functionNonZeros, unusedDouble);
		
		List<String> signatureIdcs = new ArrayList<String>();
		List<String> functionIdcs = new ArrayList<String>();
		int signatureNodeCount = 0;
		// add signature nodes graph
		for(int i=0; i<signatureNonZeros.size(); ++i) {
			int signatureIdx = signatureNonZeros.get(i);
			if(signatureDepths[signatureIdx] != -1) {
				signatureIdcs.add("s"+signatureIdx);
				g.addVertex("s"+signatureIdx);
				signatureNodeCount++;
			}
		}

		// add function nodes graph
		for(int j=0; j<functionNonZeros.size(); ++j) {
			int functionIdx = functionNonZeros.get(j);
			if(functionDepths[functionIdx] != -1) {
				functionIdcs.add("f"+functionNonZeros.get(j));
				g.addVertex("f"+functionNonZeros.get(j));
			}
		}

		// add edges
		for(int i=0; i<signatureNonZeros.size(); ++i) {
			for(int j=0; j<functionNonZeros.size(); ++j) {
				if(candidateList.get(signatureNonZeros.get(i), functionNonZeros.get(j)) != 0) {
					g.addEdge("s"+signatureNonZeros.get(i), "f"+functionNonZeros.get(j));
				}
			}
		}

		// define sets
		Set<String> p1 = new HashSet<String>(signatureIdcs);
        Set<String> p2 = new HashSet<String>(functionIdcs);

        // bipartite matching!
		HopcroftKarpBipartiteMatching<String, DefaultEdge> alg = 
			new HopcroftKarpBipartiteMatching<String, DefaultEdge>(g, p1, p2);

		Set<DefaultEdge> match = alg.getMatching();
		// sat || unsat
		if(match.size() == signatureNodeCount) {
			return true;
		} else { 
			return false;
		}

	}

	public static boolean bipartiteMatchingFull(SparseDoubleMatrix2D candidateList, int[] signatureDepths, int[] functionDepths, int depth) {
		UndirectedGraph<String, DefaultEdge> g = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
		
		List<String> signatureIdcs = new ArrayList<String>();
		for(int i=0; i<signatureDepths.length; ++i) {
			signatureIdcs.add("s"+i);
			g.addVertex("s"+i);
		}

		List<String> functionIdcs = new ArrayList<String>();
		for(int j=0; j<functionDepths.length; ++j) {
			functionIdcs.add("f"+j);
			g.addVertex("f"+j);
		}

		for(int i=0; i<signatureDepths.length; ++i) {
			DoubleMatrix1D row = candidateList.viewRow(i);

			for(int j=0; j<row.size(); ++j) {
				if(row.get(j) != 0) {
					g.addEdge("s"+i, "f"+j);
				}
			}
		}

		Set<String> p1 = new HashSet<String>(signatureIdcs);
        Set<String> p2 = new HashSet<String>(functionIdcs);

		HopcroftKarpBipartiteMatching<String, DefaultEdge> alg = 
			new HopcroftKarpBipartiteMatching<String, DefaultEdge>(g, p1, p2);

		Set<DefaultEdge> match = alg.getMatching();

		//System.out.println(g.toString());
		//System.out.println(match);
		if(match.size() == signatureDepths.length)
			return true;
		else
			return false;

	}

	public static boolean bipartiteMatchingDepth(SparseDoubleMatrix2D candidateList, int[] signatureDepths, int[] functionDepths, int depth) {
		UndirectedGraph<String, DefaultEdge> g = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
		List<String> signatureIdcs = new ArrayList<String>();
		int signatureNodesAtDepth = 0;
		for(int i=0; i<signatureDepths.length; ++i) {
			if(signatureDepths[i] == depth) {
				signatureIdcs.add("s"+i);
				g.addVertex("s"+i);
				//System.out.println("bpm:\ts"+i);
				signatureNodesAtDepth++;
			}
		}

		List<String> functionIdcs = new ArrayList<String>();
		for(int j=0; j<functionDepths.length; ++j) {
			if(functionDepths[j] == depth) {
				functionIdcs.add("f"+j);
				g.addVertex("f"+j);
				//System.out.println("bpm:\tf"+j);
			}
		}

		for(int i=0; i<signatureDepths.length; ++i) {
			if(signatureDepths[i] == depth) {
				DoubleMatrix1D row = candidateList.viewRow(i);

				for(int j=0; j<row.size(); ++j) {
					if(row.get(j) == 1.0 && functionDepths[j] == depth) {
						g.addEdge("s"+i, "f"+j);
					}
				}
			}
		}

		Set<String> p1 = new HashSet<String>(signatureIdcs);
        Set<String> p2 = new HashSet<String>(functionIdcs);

		HopcroftKarpBipartiteMatching<String, DefaultEdge> alg = 
			new HopcroftKarpBipartiteMatching<String, DefaultEdge>(g, p1, p2);

		Set<DefaultEdge> match = alg.getMatching();

		// System.out.println(g.toString());
		// System.out.println(match);
		if(match.size() == signatureNodesAtDepth)
			return true;
		else
			return false;

	}

	public static int[] initializeDepths(SparseDoubleMatrix2D adjMat, int[] depths) {
		for(int j=0; j<depths.length; ++j){ 
			// this allows multiple entry points
			if(j == 0 || adjMat.viewColumn(j).cardinality() == 0){
				depths[j] = 0;
			} else {
				depths[j] = -1;
			}
		}
		return depths;
	}
}