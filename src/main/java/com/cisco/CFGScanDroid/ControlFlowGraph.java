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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.SwitchPayload;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import cern.colt.list.IntArrayList;
import cern.colt.list.DoubleArrayList;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;

public class ControlFlowGraph {
	private List<BasicBlock> basicBlocks;
	private int edgeCount;
	private int vertexCount;
	private SparseDoubleMatrix2D adjacencyMatrix;
	private String identifier;
	private String shortIdentifier;

	public String getIdentifier(boolean shortForm) {
		if(shortForm)
			return shortIdentifier;

		return identifier;
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

	public SparseDoubleMatrix2D getAdjacencyMatrix() {
		return adjacencyMatrix;
	}

	public int getVertexCount() {
		return vertexCount;
	}

	public int getEdgeCount() {
		return edgeCount;
	}

	public List<BasicBlock> getBasicBlocks() {
		return basicBlocks;
	}

	private static List<BasicBlockInstruction> getFlatMethod(Method method) {
		List<BasicBlockInstruction> flatMethod = new ArrayList<BasicBlockInstruction>();
		MethodImplementation impl = method.getImplementation();
		//List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = null;
		if(impl != null) {
			int address = 0;
			for(Instruction instruction: impl.getInstructions()) {
				BasicBlockInstruction bbinsn = new BasicBlockInstruction(address, instruction);
				//System.out.print("\t" + address + "\t" + instruction.getOpcode() + "\t" + bbinsn.branch);
				address += instruction.getCodeUnits();
				flatMethod.add(bbinsn);
			}
			//tryBlocks = impl.getTryBlocks();
		}

		return flatMethod;
	}

	public ControlFlowGraph(Method method) {
		this(getFlatMethod(method));
		String definingClass = method.getDefiningClass();
		this.identifier = definingClass + "." + method.getName() + "()" + method.getReturnType();
		// this.identifier = definingClass.substring(definingClass.lastIndexOf("/")+1) + method.getName() + "(" + method.getReturnType() + ")";
		this.identifier = this.identifier.replace(";", "");
		this.shortIdentifier = identifier.substring(definingClass.lastIndexOf("/")+1);
	}

	private ControlFlowGraph(List<BasicBlockInstruction> flatMethod) { 
		//, List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks) {
		List<Integer> leaders = new ArrayList<Integer>();
		Map<Integer, Integer> switchMap = new HashMap<Integer, Integer>();
		leaders.add(0);

		// bb parsing
		for(BasicBlockInstruction bbinsn : flatMethod) {
			if(bbinsn.branch) {
				parseDestinations(bbinsn, leaders, switchMap);
			} 
		}

		// TODO: can we merge this with the previous loop?
		BasicBlockInstruction last = null;
		for(BasicBlockInstruction bbinsn : flatMethod) {
			if(last != null && last.branch)
				if(!leaders.contains(bbinsn.address))
					leaders.add(bbinsn.address);

			if(leaders.contains(bbinsn.address)) {
				bbinsn.leader = true;
			}

			if(last != null && (!last.branch) && bbinsn.leader) {
				if(last.destinations == null)
					last.destinations = new ArrayList<Integer>();

				if(!last.destinations.contains(bbinsn.address))
					last.destinations.add(bbinsn.address);
			} 

			//System.out.print(bbinsn.address + "\t" + bbinsn.instruction.getOpcode() + "\t" );
			//if(bbinsn.destinations != null)
			//	for(Integer dest : bbinsn.destinations)
			//		System.out.print(dest + "\t");
			//System.out.println();
			last = bbinsn;
		}

		/*if(tryBlocks != null) {
			for(TryBlock<? extends ExceptionHandler> tryBlock : tryBlocks) {
				int start = tryBlock.getStartCodeAddress();
				int count = tryBlock.getCodeUnitCount();
				System.out.println("TB\t" + start + "\t" + count);
				for(ExceptionHandler exceptionHandler: tryBlock.getExceptionHandlers()) {
					System.out.println("\t" + exceptionHandler.getHandlerCodeAddress());
				}
			}
		}//*/

		// bb creation
		BasicBlock curr = null;
		List<BasicBlockInstruction> bbList = new ArrayList<BasicBlockInstruction>();
		basicBlocks = new ArrayList<BasicBlock>();
		edgeCount = 0;
		vertexCount = 0;
		adjacencyMatrix = null;
		for(BasicBlockInstruction bbinsn : flatMethod) {
			if(bbinsn.leader && !bbList.isEmpty()) {
				basicBlocks.add(new BasicBlock(bbList));
				vertexCount++;
				edgeCount += basicBlocks.get(basicBlocks.size()-1).outgoingEdges;
				bbList.clear();
			}
			bbList.add(bbinsn);
		}

		if(!bbList.isEmpty()) {
			basicBlocks.add(new BasicBlock(bbList));
			vertexCount++;
			edgeCount += basicBlocks.get(basicBlocks.size()-1).outgoingEdges;
			bbList.clear();
		}

		// if(basicBlocks.isEmpty())
		// 	basicBlocks = null;

		// build adjmat
		assert leaders.size() == vertexCount;
		// System.out.println("VERTEX COUNT: " + vertexCount);

		adjacencyMatrix = new SparseDoubleMatrix2D(vertexCount, vertexCount);
		for(BasicBlock bb : basicBlocks) {
			//System.out.println(leaders);
			//if(bb.destinations != null)
			//	System.out.print("\t" + bb.startInstructionAddress + " -> { ");

			int row = leaders.indexOf(bb.startInstructionAddress);
			if(bb.destinations == null) {
				//System.out.println();
				continue;
			}

			for(Integer dest : bb.destinations) {
				int col = leaders.indexOf(dest);
				// System.out.println("\t" + row + ", " + col);
				//System.out.print(dest + " ; ");
				if(row >= vertexCount || col >= vertexCount)
					continue;
				
				adjacencyMatrix.set(row, col, 1.0);
			}
			//System.out.println("\b\b}");
		}

		//System.out.println(vertexCount);
		//System.out.println(edgeCount);
		//System.out.println(adjacencyMatrix.cardinality());
		//System.out.println(adjacencyMatrix);
	}

	private boolean parseDestinations(BasicBlockInstruction bbinsn, List<Integer> leaders, Map<Integer, Integer> switchMap) {
		int offset = -1;
		switch(bbinsn.instruction.getOpcode()) {
	    	case PACKED_SWITCH_PAYLOAD:		// switch payloads
	    	case SPARSE_SWITCH_PAYLOAD:	
	    		Integer sourceAddress = switchMap.get(bbinsn.address);
	    		if(sourceAddress != null) {
					for(SwitchElement switchElement : ((SwitchPayload) bbinsn.instruction).getSwitchElements()){
						offset = switchElement.getOffset() + (int)sourceAddress;
						if(bbinsn.destinations == null)
							bbinsn.destinations = new ArrayList<Integer>();
						bbinsn.destinations.add(offset);
						if(!leaders.contains(offset))
							leaders.add(offset);
					}
				}
				break;
			case RETURN_VOID:				// returns
			case RETURN:
			case RETURN_WIDE:
			case RETURN_OBJECT:
				break;
			case GOTO:						// gotos
			case GOTO_16:
			case GOTO_32:
				if(bbinsn.destinations == null)
					bbinsn.destinations = new ArrayList<Integer>();
				offset = ((OffsetInstruction) bbinsn.instruction).getCodeOffset() + bbinsn.address;
				bbinsn.destinations.add(offset);
				if(!leaders.contains(offset))
					leaders.add(offset);

				break;
			case PACKED_SWITCH:				// switches (to payload)
			case SPARSE_SWITCH:
				offset = ((OffsetInstruction) bbinsn.instruction).getCodeOffset();
				switchMap.put(bbinsn.address+offset, bbinsn.address);
			case IF_EQ:						// ifs reg cmp reg
			case IF_NE:
			case IF_LT:
			case IF_GE:
			case IF_GT:
			case IF_LE:
			case IF_EQZ: 					// ifs reg cmp zero
			case IF_NEZ:
			case IF_LTZ:
			case IF_GEZ:
			case IF_GTZ:
			case IF_LEZ:
				if(bbinsn.destinations == null)
					bbinsn.destinations = new ArrayList<Integer>();

				offset = ((OffsetInstruction) bbinsn.instruction).getCodeOffset() + bbinsn.address;
				if(!leaders.contains(offset))
					leaders.add(offset);
				bbinsn.destinations.add(offset);
				
				offset = bbinsn.address + bbinsn.instruction.getCodeUnits();
				if(!leaders.contains(offset))
					leaders.add(offset);
				bbinsn.destinations.add(offset);
				
				break;
		}

		return true;
	}
}
