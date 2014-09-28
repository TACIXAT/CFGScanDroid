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

public class BasicBlock {
	List<BasicBlockInstruction> instructions;
	int startInstructionAddress;
	int endInstructionAddress;
	int outgoingEdges;
	int instructionCount;
	List<Integer> destinations;

	public BasicBlock(List<BasicBlockInstruction> bbList){
		instructions = new ArrayList<BasicBlockInstruction>(bbList);
		instructionCount = instructions.size();
		startInstructionAddress = instructions.get(0).address;
		BasicBlockInstruction tail = instructions.get(instructionCount-1);
		endInstructionAddress = tail.address;
		/*if(tail.destinations == null) {
			outgoingEdges = 1;
			this.destinations = new ArrayList<Integer>();
			this.destinations.add(tail.address + tail.instruction.getCodeUnits());
		} else {//*/
		if(tail.destinations != null) {
			outgoingEdges = tail.destinations.size();
			this.destinations = new ArrayList<Integer>(tail.destinations);
		} else {
			outgoingEdges = 0;
			this.destinations = null;
		}
		//}
	}
}
