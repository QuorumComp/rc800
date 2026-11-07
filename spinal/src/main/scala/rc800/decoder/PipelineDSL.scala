package rc800.decoder

import spinal.core._

import rc800.alu.AluOperation
import rc800.alu.Condition
import rc800.alu.OperandSource
import rc800.alu.PcCondition
import rc800.alu.PcTruePathSource
import rc800.alu.ShiftOperation

import rc800.control.MemoryStageAddressSource
import rc800.control.PipelineControl
import rc800.control.WriteBackValueSource

import rc800.registers.RegisterName


object Pipeline {

	/**
	 * The opcode-field-to-register mapping, in one place and documented.  RC800
	 * puts the register index directly in the opcode: bits 2..0 select one of the
	 * eight 8-bit registers and bits 1..0 select one of the four 16-bit pairs.  The
	 * prefixes make the raw field land in the right region of the shared
	 * RegisterName enum - "0" ## r8 in its 8-bit bank (f..l), "10" ## r16 in its
	 * pair bank (ft..hl).
	 */
	object RegisterField {
		def r8(opcode: Bits): RegisterName.C = (B"0" ## opcode(2 downto 0)).as(RegisterName())
		def r16(opcode: Bits): RegisterName.C = (B"10" ## opcode(1 downto 0)).as(RegisterName())
	}

	implicit class PipelineControlPimp(pipeline: PipelineControl) {
		def loadImmediateByte(): Unit = {
			readMemory(MemoryStageAddressSource.pc, doIo = False, code = True)
			pipeline.aluStageControl.pcControl.truePath := PcTruePathSource.offsetFromDecoder
			pipeline.aluStageControl.pcControl.decodedOffset := U(1)
		}

		def writeMemory(addressSource: MemoryStageAddressSource.E, doIo: Bool = False): Unit = {
			pipeline.memoryStageControl.enable := True
			pipeline.memoryStageControl.write := True
			pipeline.memoryStageControl.io := doIo
			pipeline.memoryStageControl.address := addressSource
		}

		def readMemory(addressSource: MemoryStageAddressSource.E, doIo: Bool = False, code: Bool = False): Unit =  {
			pipeline.memoryStageControl.enable := True
			pipeline.memoryStageControl.write := False
			pipeline.memoryStageControl.io := doIo
			pipeline.memoryStageControl.address := addressSource
			pipeline.memoryStageControl.code := code
		}

		// The memory stage takes its write data straight from a register lane, so a
		// store can name that lane without deciding what the ALU makes of it - which
		// a stepped store needs, because its ALU second operand is the step.
		def memoryWriteData(register: RegisterName.C): Unit = {
			pipeline.readStageControl.registers(1) := register
		}

		def pushValueHL(): Unit = {
			pipeline.writeStageControl.source := WriteBackValueSource.alu
			pipeline.writeStageControl.fileControl.write := True
			pipeline.writeStageControl.fileControl.writeRegister := RegisterName.hl
			val hl = pipeline.writeStageControl.fileControl.registerControl(RegisterName.hl)
			hl.push := True
		}

		def interrupt(vector: UInt): Unit = {
			operand1 := Operand.pc
			operand2 := Operand.ones
			pipeline.aluStageControl.aluControl.operation := AluOperation.add

			pipeline.memoryStageControl.enable := False

			pipeline.aluStageControl.pcControl.vector := vector
			pipeline.aluStageControl.pcControl.truePath := PcTruePathSource.vectorFromDecoder

			pushValueHL()
		}

		case class OperandWriter(index: Int) {
			def := (operand: Operand): Unit = {
				pipeline.readStageControl.registers(index) := operand.register
				pipeline.aluStageControl.selection(index) := operand.selection
				if (operand.selection == OperandSource.memory || operand.selection == OperandSource.signed_memory) {
					loadImmediateByte()
				}
			}
			def := (operand: RegisterName.C): Unit = {
				this := Operand(operand, OperandSource.register)
			}
		}

		val operand1 = OperandWriter(0)
		val operand2 = OperandWriter(1)

		def destination(register: RegisterName.C) =
			Destination(register)

		// A destination register, written by one of three distinct actions.  They
		// were once three overloads of `:=`, which meant `dest.t := x` hid which of
		// them a line performed until you checked x's type; they are now named.
		case class Destination(val register: RegisterName.C) {
			private val control = pipeline.writeStageControl.fileControl

			// Take the value from a writeback source (the ALU result or memory).
			def write(source: WriteBackValueSource.E): Unit = {
				pipeline.writeStageControl.source := source

				control.write := True
				control.writeRegister := register
			}

			// Move an operand into the destination.  RC800 has no direct
			// register-to-register transfer, so a move is the ALU passing its first
			// operand straight through.
			def move(src: Operand): Unit = {
				operand1 := src
				pipeline.aluStageControl.aluControl.operation := AluOperation.operand1
				this.write(WriteBackValueSource.alu)
			}

			// Move another register into the destination (see move).
			def moveReg(src: RegisterName.C): Unit =
				this.move(Operand(src, OperandSource.register))

			// Exchange this register with another: this takes the other's value via
			// the move above, and the other takes this register's value on the
			// auxiliary write port.
			def exchange(opcodeRegister: PipelineControlPimp#Destination): Unit = {
				this.moveReg(opcodeRegister.register)
				operand2 := register

				val opcodeControl = pipeline.writeStageControl.fileControl
				opcodeControl.writeAux := True
				opcodeControl.writeAuxRegister := opcodeRegister.register
			}
		}

		object Destination {
			def f = destination(RegisterName.f)
			def t = destination(RegisterName.t)
			def b = destination(RegisterName.b)
			def c = destination(RegisterName.c)
			def d = destination(RegisterName.d)
			def e = destination(RegisterName.e)
			def h = destination(RegisterName.h)
			def l = destination(RegisterName.l)

			def ft = destination(RegisterName.ft)
			def bc = destination(RegisterName.bc)
			def de = destination(RegisterName.de)
			def hl = destination(RegisterName.hl)
		}

		// Shorthands so a decode-table row reads as the recipe it is instead of a
		// chain of stage-control field paths: `p.alu := add` and `p.dest.t.write(...)`
		// rather than spelling the whole control-word location each time.  For a
		// register chosen at runtime (an opcode field), use `destination(register)`.
		def alu = pipeline.aluStageControl.aluControl.operation
		def dest = Destination

		// The control word for NOP, and the baseline every instruction starts from:
		// the decoder lays this down, then an instruction's effect overrides only the
		// fields it uses.  So an instruction that forgets a field silently inherits
		// NOP's - which is memory off, register-file write off, ALU = and, PC = +1.
		// NopIsBaselineTest pins this meaning.
		def nopEffect(): Unit = {
			pipeline.readStageControl.registers.foreach(_ := RegisterName.ft)

			pipeline.memoryStageControl.enable  := False
			pipeline.memoryStageControl.write   := False
			pipeline.memoryStageControl.io      := False
			pipeline.memoryStageControl.code    := False
			pipeline.memoryStageControl.config  := False
			pipeline.memoryStageControl.address := MemoryStageAddressSource.register1

			pipeline.aluStageControl.selection.foreach(_ := OperandSource.register)
			pipeline.aluStageControl.aluControl.operation := AluOperation.and
			pipeline.aluStageControl.aluControl.condition := Condition.t

			pipeline.writeStageControl.source := WriteBackValueSource.alu
			val fileControl = pipeline.writeStageControl.fileControl
			fileControl.write := False
			fileControl.writeRegister := RegisterName.ft
			fileControl.writeAux := False
			fileControl.writeAuxRegister := RegisterName.ft
			fileControl.auxFromMemory := False
			for (i <- 0 to 3) {
				val registerControl = fileControl.registerControl(i)
				registerControl.push  := False
				registerControl.pop   := False
				registerControl.swap  := False
				registerControl.pick  := False
				registerControl.stow  := False
			}

			pipeline.aluStageControl.pcControl.truePath      := PcTruePathSource.offsetFromDecoder
			pipeline.aluStageControl.pcControl.decodedOffset := U(0)
			pipeline.aluStageControl.pcControl.vector        := U(0)
			pipeline.aluStageControl.pcControl.condition     := PcCondition.always
		}

	}


	case class Operand(val register: RegisterName.C, val selection: OperandSource.E) {
		def === (rhs: Operand): Bool = {
			(register === rhs.register) && (selection === rhs.selection)
		}
	}

	object Operand {
		def f = Operand(register = RegisterName.f, selection = OperandSource.register)
		def t = Operand(register = RegisterName.t, selection = OperandSource.register)
		def b = Operand(register = RegisterName.b, selection = OperandSource.register)
		def c = Operand(register = RegisterName.c, selection = OperandSource.register)
		def d = Operand(register = RegisterName.d, selection = OperandSource.register)
		def e = Operand(register = RegisterName.e, selection = OperandSource.register)
		def h = Operand(register = RegisterName.h, selection = OperandSource.register)
		def l = Operand(register = RegisterName.l, selection = OperandSource.register)

		def ft = Operand(register = RegisterName.ft, selection = OperandSource.register)
		def bc = Operand(register = RegisterName.bc, selection = OperandSource.register)
		def de = Operand(register = RegisterName.de, selection = OperandSource.register)
		def hl = Operand(register = RegisterName.hl, selection = OperandSource.register)

		def immediate_byte = Operand(register = RegisterName.ft, selection = OperandSource.memory)
		def signed_immediate_byte = Operand(register = RegisterName.ft, selection = OperandSource.signed_memory)
		def ones = Operand(register = RegisterName.ft, selection = OperandSource.ones)
		def zero = Operand(register = RegisterName.ft, selection = OperandSource.zero)
		def pc = Operand(register = RegisterName.ft, selection = OperandSource.pc)
	}
}