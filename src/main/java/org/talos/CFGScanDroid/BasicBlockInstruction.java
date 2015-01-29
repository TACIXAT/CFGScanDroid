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

import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction;
import java.util.List;

public class BasicBlockInstruction {
	Instruction instruction;
	boolean leader;
	boolean branch;
	int address;
	int offset = -1;
	byte rawBytes[];
	int length = 0;
	List<Integer> destinations;

	public BasicBlockInstruction(int address, Instruction insn) {
		this.instruction = insn;
		this.leader = address == 0 ? true : false;
		this.branch = isBranch();
		this.destinations = null;
		this.address = address;

		DexBackedInstruction dexBackedInstruction = (DexBackedInstruction)insn;
		this.offset = dexBackedInstruction.instructionStart;
		this.length = insn.getCodeUnits() * 2;
		this.rawBytes = new byte[this.length];
		// DexReader reader = readerAt(this.offset);
		for(int i=0; i<this.length; ++i) {
			this.rawBytes[i] = (byte)dexBackedInstruction.dexFile.readByte(this.offset + i);
			// System.out.print(this.rawBytes[i]);
			// System.out.print(' ');
		}
		// System.out.println();
	}

	private boolean isBranch() {
		switch(instruction.getOpcode()) {
	    	case PACKED_SWITCH_PAYLOAD:				// switch payloads
	    	case SPARSE_SWITCH_PAYLOAD:	
				return true;
			case RETURN_VOID:		// returns
			case RETURN:
			case RETURN_WIDE:
			case RETURN_OBJECT:
				return true;
			case GOTO:				// gotos
			case GOTO_16:
			case GOTO_32:
				return true;
			case PACKED_SWITCH:		// switches (to payload)
			case SPARSE_SWITCH:
				return true;
			case IF_EQ:				// ifs reg cmp reg
			case IF_NE:
			case IF_LT:
			case IF_GE:
			case IF_GT:
			case IF_LE:
				return true;
			case IF_EQZ: 			// ifs reg cmp zero
			case IF_NEZ:
			case IF_LTZ:
			case IF_GEZ:
			case IF_GTZ:
			case IF_LEZ:
				return true;
			default:				// not a branch
				return false;
		}
	}
}
