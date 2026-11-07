package rc800.decoder

import spinal.core._
import spinal.core.sim._
import spinal.lib._

import rc800.alu.AluOperation
import rc800.alu.Condition
import rc800.alu.OperandSource
import rc800.alu.PcCondition
import rc800.alu.PcTruePathSource

import rc800.control.MemoryStageAddressSource
import rc800.control.PipelineControl
import rc800.control.WriteBackValueSource

import rc800.control.component.RegisterControl

import rc800.registers.RegisterName

import rc800.Vectors

import Pipeline.PipelineControlPimp


/**
 * The combinational RC800 instruction decoder.  The decode table (`instructions`
 * in the companion below) is the implementation: this component applies each
 * row's effect on a priority match (see the dispatch), so an instruction is
 * defined in exactly one place.  Each row writes its whole effect inline; the
 * only methods kept here are the recipes more than one row shares - the stepped
 * access block and the stack operations.
 */
case class OpcodeDecoder() extends Component {
	val io = new Bundle {
		val opcode = in Bits(8 bits)
		val nmiActive = in Bool()
		val stackPointers = in Vec(UInt(8 bits), 4)
		val output = out (DecoderOutput())

		def controlSignals = output.stageControl
	}

	// The opcode-field-to-register mapping lives in Pipeline.RegisterField; these
	// are this instruction's operands and write destination chosen by the opcode's
	// register field.  The Operand/Destination objects turn them into operands and
	// write destinations for the table's rows.
	val registerPair = Pipeline.RegisterField.r16(io.opcode)
	val register = Pipeline.RegisterField.r8(io.opcode)

	object Operand {
		def opcode_r8 = Pipeline.Operand(register = register, selection = OperandSource.register)
		def opcode_r16 = Pipeline.Operand(register = registerPair, selection = OperandSource.register)
	}

	io.output.illegal := False
	io.output.overflow := False
	io.output.stageControl.nopEffect()

	// The decode table is the implementation.  Each row's effect is applied when
	// its opcode form matches and no higher-priority row has already matched -
	// the when/elsewhen chain expressed as data, so the table is the only place an
	// instruction is defined.  nopEffect() laid down the NOP baseline, so an
	// effect overrides only the fields its instruction uses.  Illegal opcodes take
	// priority over the whole table.
	when (!io.nmiActive && Opcodes.illegals.map(io.opcode === _).reduce(_ || _)) {
		io.output.illegal := True
		io.controlSignals.interrupt(U(Vectors.IllegalInstruction >> 3, 3 bits))
	} otherwise {
		OpcodeDecoder.instructions.foldLeft(False) { (matched, instr) =>
			val hits = io.opcode === instr.mask
			when(hits && !matched) { instr.effect(this) }
			matched || hits
		}
	}

	// --- shared recipes ------------------------------------------------------
	// The only methods the table needs are these four, each used by more than one
	// row.  Every other instruction writes its whole effect at its own row above.

	// The stepped byte block ($10-$1F): the plain FT-indirect access of $20-$2F
	// plus one step of FT afterwards, which is what a byte walk spells out today
	// as an access and an ADD FT,#1 in sequence.  Nothing is added to the
	// datapath: the address is FT as the memory stage reads it, the step is the
	// ALU subtracting the ones operand (FT - $FFFF wraps to FT + 1), and the
	// pointer write is an ordinary pair write in the writeback stage, so the FT
	// stack-top cell follows the pointer exactly as it does for ADD FT,#1.  The
	// step does not disturb the flags: only CMP writes them.  The byte operand is
	// named by the opcode's r8 field; F and T are illegal there because they are
	// the two halves of the stepped pointer.
	def ld_MEM_R8_inc: Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.ft
		io.controlSignals.memoryWriteData(register)
		io.controlSignals.writeMemory(MemoryStageAddressSource.register1)

		// Lane 1 carries the stored byte to the memory stage while the ALU steps FT.
		io.controlSignals.aluStageControl.selection(1) := Pipeline.Operand.ones.selection
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.sub
		io.controlSignals.dest.ft.write(WriteBackValueSource.alu)
	}

	def ld_R8_MEM_inc: Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.ft
		io.controlSignals.readMemory(MemoryStageAddressSource.register1)

		io.controlSignals.aluStageControl.selection(1) := Pipeline.Operand.ones.selection
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.sub
		io.controlSignals.dest.ft.write(WriteBackValueSource.alu)

		// Two registers move: the stepped FT on the main write port with the ALU
		// result, and the loaded byte on the auxiliary one (see the writeback
		// stage).  The byte cannot be F or T - it would land inside the stepped
		// pointer - which is why those two r8 cells of the block are illegal.
		val fileControl = io.controlSignals.writeStageControl.fileControl
		fileControl.writeAux := True
		fileControl.writeAuxRegister := register
		fileControl.auxFromMemory := True
	}

	// PUSH/POP/SWAP on the pair the opcode field selects: the operand setup the
	// three share, with the stack action (push/pop/swap) passed in.  POP and SWAP
	// call this directly; PUSH wraps it in a zero-SP fault.
	def stack(setter: RegisterControl => Unit): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r16
		io.controlSignals.operand2 := Pipeline.Operand.zero
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.or

		val control = io.controlSignals.writeStageControl.fileControl.registerControl(registerPair)
		setter(control)
	}

	// The all-pairs forms (PUSHA/POPA/SWAPA): the same stack action applied to
	// each of the four register stacks.
	def stackAll(setter: RegisterControl => Unit): Unit = {
		for (i <- 0 to 3) {
			val control = io.controlSignals.writeStageControl.fileControl.registerControl(i)
			setter(control)
		}
	}
}


/**
 * One row of the decode table: the instruction's name (the Opcodes constant it
 * is named after), the opcode form it matches, and the effect it produces.  The
 * effect is a function of the decoder so a row can reach the component's opcode
 * fields and shared helpers (`d.io.controlSignals`, `d.Operand.opcode_r16`,
 * `d.stack(...)`); keeping it a plain function rather than instance methods lets
 * the audit (OpcodeMap) read the table's names and masks without elaborating a
 * component.
 */
case class Instruction(
	name: String,                 // the Opcodes constant name, e.g. "ADD_T_R8"
	mask: MaskedLiteral,          // the opcode form, e.g. Opcodes.ADD_T_R8
	effect: OpcodeDecoder => Unit // what the instruction does
) {
	/** Assembly-ish label for the golden file, matching the historical form. */
	def mnemonic: String = name.replaceFirst("_", " ").replace('_', ',')
}


object OpcodeDecoder {

	/**
	 * The decode map as a single declarative table: one row per instruction form,
	 * in the priority order the decoder tests them.  This is the editing surface -
	 * a reviewer reads one row and sees the mnemonic, the opcode form it matches,
	 * and the effect it produces.
	 *
	 * Each row writes its whole effect out in full, so what an instruction does is
	 * visible where it is named.  Only the recipes more than one row shares - the
	 * stepped access block and the stack operations - are called from a method on
	 * the component above; a single-use helper would only hide the effect from the
	 * row that is its only caller.
	 *
	 * List order is load-bearing: overlapping forms (J_CC_I `1001----` over SYS_I
	 * and ADD_R16_I `100111--`) resolve by first match, exactly as the decoder
	 * dispatches them.  OpcodeMap derives its audit view from this table, and the
	 * golden-table and ROM-equivalence tests pin the hardware it produces.
	 */
	val instructions: Seq[Instruction] = Seq(

		// --- Opcodes with no fields -------------------------------------------
		Instruction("AND_T_I", Opcodes.AND_T_I, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.t
			p.operand2 := Pipeline.Operand.immediate_byte
			p.alu := AluOperation.and
			p.dest.t.write(WriteBackValueSource.alu)
		}),
		Instruction("DI",  Opcodes.DI,  _ => ()),   // intEnable handled in the Decoder wrapper
		Instruction("EI",  Opcodes.EI,  _ => ()),
		Instruction("EXT_T", Opcodes.EXT_T, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.t
			p.alu := AluOperation.extend1
			p.dest.f.write(WriteBackValueSource.alu)
		}),
		// A constant opcode, and it must be tested before LD_MEM_R8_INC: $10 is
		// also that stepped-store row's r8 = F cell (the pointer's high half).
		Instruction("EXG_T_F", Opcodes.EXG_T_F, d => d.io.controlSignals.dest.t.exchange(d.io.controlSignals.dest.f)),
		Instruction("LD_CR_T", Opcodes.LD_CR_T, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.c
			p.operand2 := Pipeline.Operand.t
			p.memoryStageControl.enable := True
			p.memoryStageControl.write := True
			p.memoryStageControl.config := True
			p.memoryStageControl.address := MemoryStageAddressSource.register1
		}),
		Instruction("LD_T_CR", Opcodes.LD_T_CR, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.c
			p.memoryStageControl.enable := True
			p.memoryStageControl.config := True
			p.memoryStageControl.write := False
			p.memoryStageControl.address := MemoryStageAddressSource.register1
			p.dest.t.write(WriteBackValueSource.memory)
		}),
		Instruction("LS_FT_I", Opcodes.LS_FT_I, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ft
			p.operand2 := Pipeline.Operand.immediate_byte
			p.alu := AluOperation.ls
			p.dest.ft.write(WriteBackValueSource.alu)
		}),
		Instruction("NEG_T", Opcodes.NEG_T, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.zero
			p.operand2 := Pipeline.Operand.t
			p.alu := AluOperation.sub
			p.dest.t.write(WriteBackValueSource.alu)
		}),
		Instruction("NEG_FT", Opcodes.NEG_FT, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.zero
			p.operand2 := d.Operand.opcode_r16
			p.alu := AluOperation.sub
			p.destination(d.registerPair).write(WriteBackValueSource.alu)
		}),
		Instruction("NOP", Opcodes.NOP, _ => ()),
		Instruction("NOT_F", Opcodes.NOT_F, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ones
			p.operand2 := Pipeline.Operand.f
			p.alu := AluOperation.xor
			p.dest.f.write(WriteBackValueSource.alu)
		}),
		Instruction("OR_T_I", Opcodes.OR_T_I, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.t
			p.operand2 := Pipeline.Operand.immediate_byte
			p.alu := AluOperation.or
			p.dest.t.write(WriteBackValueSource.alu)
		}),
		Instruction("POPA", Opcodes.POPA, d => d.stackAll(_.pop := True)),
		Instruction("PUSHA", Opcodes.PUSHA, d =>
			when(!d.io.nmiActive && (0 to 3).map(i => d.io.stackPointers(i) === 0).reduce(_ || _)) {
				d.io.output.overflow := True
				d.io.controlSignals.interrupt(U(Vectors.StackOverflow >> 3, 3 bits))
			} otherwise {
				d.stackAll(_.push := True)
			}),
		Instruction("RETI", Opcodes.RETI, d => { val p = d.io.controlSignals
			p.operand2 := Pipeline.Operand.hl
			p.aluStageControl.pcControl.truePath := PcTruePathSource.register2
			p.writeStageControl.fileControl.registerControl(RegisterName.hl).pop := True
		}),
		Instruction("RS_FT_I", Opcodes.RS_FT_I, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ft
			p.operand2 := Pipeline.Operand.immediate_byte
			p.alu := AluOperation.rs
			p.dest.ft.write(WriteBackValueSource.alu)
		}),
		Instruction("RSA_FT_I", Opcodes.RSA_FT_I, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ft
			p.operand2 := Pipeline.Operand.immediate_byte
			p.alu := AluOperation.rsa
			p.dest.ft.write(WriteBackValueSource.alu)
		}),
		Instruction("SWAPA", Opcodes.SWAPA, d => d.stackAll(_.swap := True)),
		Instruction("SYS_I", Opcodes.SYS_I, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.pc
			p.operand2 := Pipeline.Operand.ones
			p.alu := AluOperation.sub
			p.readMemory(MemoryStageAddressSource.pc, doIo = False, code = True)
			p.aluStageControl.pcControl.truePath := PcTruePathSource.vectorFromMemory
			p.pushValueHL()
		}),
		Instruction("XOR_T_I", Opcodes.XOR_T_I, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.t
			p.operand2 := Pipeline.Operand.immediate_byte
			p.alu := AluOperation.xor
			p.dest.t.write(WriteBackValueSource.alu)
		}),

		// --- Opcodes with a two-bit field (register pair) ---------------------
		Instruction("ADD_FT_R16", Opcodes.ADD_FT_R16, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ft
			p.operand2 := d.Operand.opcode_r16
			p.alu := AluOperation.add
			p.dest.ft.write(WriteBackValueSource.alu)
		}),
		Instruction("ADD_R16_I", Opcodes.ADD_R16_I, d => { val p = d.io.controlSignals
			p.operand1 := d.Operand.opcode_r16
			p.operand2 := Pipeline.Operand.signed_immediate_byte
			p.alu := AluOperation.add
			p.destination(d.registerPair).write(WriteBackValueSource.alu)
		}),
		Instruction("CMP_FT_R16", Opcodes.CMP_FT_R16, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ft
			p.operand2 := d.Operand.opcode_r16
			p.alu := AluOperation.compare
			p.dest.f.write(WriteBackValueSource.alu)
		}),
		Instruction("EXG_FT_R16", Opcodes.EXG_FT_R16, d => d.io.controlSignals.dest.ft.exchange(d.io.controlSignals.destination(d.registerPair))),
		Instruction("JAL_R16", Opcodes.JAL_R16, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.pc
			p.operand2 := d.Operand.opcode_r16
			p.alu := AluOperation.operand1
			p.aluStageControl.pcControl.truePath := PcTruePathSource.register2
			p.dest.hl.write(WriteBackValueSource.alu)
		}),
		Instruction("J_R16", Opcodes.J_R16, d => { val p = d.io.controlSignals
			p.operand2 := d.Operand.opcode_r16
			p.aluStageControl.pcControl.truePath := PcTruePathSource.register2
		}),
		Instruction("LD_FT_R16", Opcodes.LD_FT_R16, d => d.io.controlSignals.dest.ft.move(d.Operand.opcode_r16)),
		Instruction("LD_IO_T", Opcodes.LD_IO_T, d => { val p = d.io.controlSignals
			p.operand1 := d.Operand.opcode_r16
			p.operand2 := Pipeline.Operand.t
			p.writeMemory(MemoryStageAddressSource.register1, doIo = True)
		}),
		Instruction("LD_MEM_T", Opcodes.LD_MEM_T, d => { val p = d.io.controlSignals
			p.operand1 := d.Operand.opcode_r16
			p.operand2 := Pipeline.Operand.t
			p.writeMemory(MemoryStageAddressSource.register1)
		}),
		Instruction("LD_R16_FT", Opcodes.LD_R16_FT, d => d.io.controlSignals.destination(d.registerPair).move(Pipeline.Operand.ft)),
		Instruction("LD_T_IO", Opcodes.LD_T_IO, d => { val p = d.io.controlSignals
			p.operand1 := d.Operand.opcode_r16
			p.readMemory(MemoryStageAddressSource.register1, doIo = True)
			p.dest.t.write(WriteBackValueSource.memory)
		}),
		Instruction("LD_T_CODE", Opcodes.LD_T_CODE, d => { val p = d.io.controlSignals
			p.operand1 := d.Operand.opcode_r16
			p.readMemory(MemoryStageAddressSource.register1, doIo = False, code = True)
			p.dest.t.write(WriteBackValueSource.memory)
		}),
		Instruction("LD_T_MEM", Opcodes.LD_T_MEM, d => { val p = d.io.controlSignals
			p.operand1 := d.Operand.opcode_r16
			p.readMemory(MemoryStageAddressSource.register1)
			p.dest.t.write(WriteBackValueSource.memory)
		}),
		Instruction("PICK_I", Opcodes.PICK_I, d => { val p = d.io.controlSignals
			// The stack index is the immediate operand byte, fetched on the existing
			// immediate plumbing; T - and the whole hub - survive the access.
			p.loadImmediateByte()
			p.operand1 := d.Operand.opcode_r16
			p.alu := AluOperation.operand1
			p.writeStageControl.fileControl.registerControl(d.registerPair).pick := True
		}),
		Instruction("POP", Opcodes.POP, d => d.stack(_.pop := True)),
		Instruction("PUSH", Opcodes.PUSH, d =>
			when(!d.io.nmiActive && d.io.stackPointers(d.registerPair.asBits(1 downto 0).asUInt) === 0) {
				d.io.output.overflow := True
				d.io.controlSignals.interrupt(U(Vectors.StackOverflow >> 3, 3 bits))
			} otherwise {
				d.stack(_.push := True)
			}),
		Instruction("STOW_I", Opcodes.STOW_I, d => { val p = d.io.controlSignals
			p.loadImmediateByte()
			p.operand1 := d.Operand.opcode_r16
			p.alu := AluOperation.operand1
			p.writeStageControl.fileControl.registerControl(d.registerPair).stow := True
		}),
		Instruction("SUB_FT_R16", Opcodes.SUB_FT_R16, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ft
			p.operand2 := d.Operand.opcode_r16
			p.alu := AluOperation.sub
			p.dest.ft.write(WriteBackValueSource.alu)
		}),
		Instruction("SWAP", Opcodes.SWAP, d => d.stack(_.swap := True)),
		Instruction("TST_R16", Opcodes.TST_R16, d => { val p = d.io.controlSignals
			p.operand1 := d.Operand.opcode_r16
			p.operand2 := Pipeline.Operand.zero
			p.alu := AluOperation.compare
			p.dest.f.write(WriteBackValueSource.alu)
		}),

		// --- Opcodes with a three-bit field (8-bit register) ------------------
		Instruction("ADD_T_R8", Opcodes.ADD_T_R8, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.t
			p.operand2 := d.Operand.opcode_r8
			p.alu := AluOperation.add
			p.dest.t.write(WriteBackValueSource.alu)
		}),
		Instruction("ADD_R8_I", Opcodes.ADD_R8_I, d => { val p = d.io.controlSignals
			p.operand1 := d.Operand.opcode_r8
			p.operand2 := Pipeline.Operand.immediate_byte
			p.alu := AluOperation.add
			p.destination(d.register).write(WriteBackValueSource.alu)
		}),
		Instruction("AND_T_R8", Opcodes.AND_T_R8, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.t
			p.operand2 := d.Operand.opcode_r8
			p.alu := AluOperation.and
			p.dest.t.write(WriteBackValueSource.alu)
		}),
		Instruction("CMP_R8_I", Opcodes.CMP_R8_I, d => { val p = d.io.controlSignals
			p.operand1 := d.Operand.opcode_r8
			p.operand2 := Pipeline.Operand.immediate_byte
			p.alu := AluOperation.compare
			p.dest.f.write(WriteBackValueSource.alu)
		}),
		Instruction("CMP_T_R8", Opcodes.CMP_T_R8, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.t
			p.operand2 := d.Operand.opcode_r8
			p.alu := AluOperation.compare
			p.dest.f.write(WriteBackValueSource.alu)
		}),
		Instruction("DJ_R8_I", Opcodes.DJ_R8_I, d => { val p = d.io.controlSignals
			p.operand1 := d.Operand.opcode_r8
			p.operand2 := Pipeline.Operand.ones
			p.readMemory(MemoryStageAddressSource.pc, doIo = False, code = True)
			p.alu := AluOperation.add
			p.aluStageControl.pcControl.truePath  := PcTruePathSource.offsetFromMemory
			p.aluStageControl.pcControl.condition := PcCondition.whenResultNotZero
			p.destination(d.register).write(WriteBackValueSource.alu)
		}),
		Instruction("LD_MEM_R8", Opcodes.LD_MEM_R8, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ft
			p.operand2 := d.Operand.opcode_r8
			p.writeMemory(MemoryStageAddressSource.register1)
		}),
		Instruction("LD_MEM_R8_INC", Opcodes.LD_MEM_R8_INC, d => d.ld_MEM_R8_inc),
		Instruction("LD_R8_I", Opcodes.LD_R8_I, d => { val p = d.io.controlSignals
			p.loadImmediateByte()
			p.destination(d.register).write(WriteBackValueSource.memory)
		}),
		Instruction("LD_R8_MEM", Opcodes.LD_R8_MEM, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ft
			p.readMemory(MemoryStageAddressSource.register1)
			p.destination(d.register).write(WriteBackValueSource.memory)
		}),
		Instruction("LD_R8_MEM_INC", Opcodes.LD_R8_MEM_INC, d => d.ld_R8_MEM_inc),
		Instruction("LD_R8_T", Opcodes.LD_R8_T, d => d.io.controlSignals.destination(d.register).move(Pipeline.Operand.t)),
		Instruction("LD_T_R8", Opcodes.LD_T_R8, d => d.io.controlSignals.dest.t.move(d.Operand.opcode_r8)),
		Instruction("LS_FT_R8", Opcodes.LS_FT_R8, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ft
			p.operand2 := d.Operand.opcode_r8
			p.alu := AluOperation.ls
			p.dest.ft.write(WriteBackValueSource.alu)
		}),
		Instruction("OR_T_R8", Opcodes.OR_T_R8, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.t
			p.operand2 := d.Operand.opcode_r8
			p.alu := AluOperation.or
			p.dest.t.write(WriteBackValueSource.alu)
		}),
		Instruction("RS_FT_R8", Opcodes.RS_FT_R8, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ft
			p.operand2 := d.Operand.opcode_r8
			p.alu := AluOperation.rs
			p.dest.ft.write(WriteBackValueSource.alu)
		}),
		Instruction("RSA_FT_R8", Opcodes.RSA_FT_R8, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.ft
			p.operand2 := d.Operand.opcode_r8
			p.alu := AluOperation.rsa
			p.dest.ft.write(WriteBackValueSource.alu)
		}),
		Instruction("SUB_T_R8", Opcodes.SUB_T_R8, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.t
			p.operand2 := d.Operand.opcode_r8
			p.alu := AluOperation.sub
			p.dest.t.write(WriteBackValueSource.alu)
		}),
		Instruction("XOR_T_R8", Opcodes.XOR_T_R8, d => { val p = d.io.controlSignals
			p.operand1 := Pipeline.Operand.t
			p.operand2 := d.Operand.opcode_r8
			p.alu := AluOperation.xor
			p.dest.t.write(WriteBackValueSource.alu)
		}),

		// --- Opcodes with a four-bit field (condition) ------------------------
		Instruction("J_CC_I", Opcodes.J_CC_I, d => { val p = d.io.controlSignals
			val cc = Condition()
			cc.assignFromBits(d.io.opcode(3 downto 0))
			p.operand1 := Pipeline.Operand.f
			p.operand2 := Pipeline.Operand.zero
			p.readMemory(MemoryStageAddressSource.pc, doIo = False, code = True)
			p.alu := AluOperation.or
			p.aluStageControl.aluControl.condition := cc
			p.aluStageControl.pcControl.truePath  := PcTruePathSource.offsetFromMemory
			p.aluStageControl.pcControl.condition := PcCondition.whenConditionMet
		})
	)
}
