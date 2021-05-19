package rc800.decoder

import spinal.core._
import spinal.lib._

import rc800.alu.AluOperation
import rc800.alu.Condition
import rc800.alu.OperandSelection
import rc800.alu.ShiftOperation

import rc800.registers.OperandPart

import rc800.Vectors
import rc800.registers.RegisterName
import rc800.registers.RegisterControl
import rc800.registers.RegisterFileControl
import rc800.registers.WriteMask

case class Decoder() extends Component {
	val io = new Bundle {
		val opcode    = in Bits(8 bits)
		val nmiReq    = in Bool
		val intReq    = in Bool
		val intEnable = in Bool
		val nmiActive = in Bool
		val intActive = in Bool
		val sysActive = in Bool

		val output = out (DecoderOutput())
	}

	private val registerPair2 = io.opcode(1 downto 0).as(RegisterName())
	private val registerPair3 = io.opcode(2 downto 1).as(RegisterName())
	private val registerPair3Low = io.opcode(0)
	private val registerPair3Part = registerPair3Low ? OperandPart.low | OperandPart.high

	private val registerPair3WritePart = WriteMask()
	registerPair3WritePart := registerPair3Low ? WriteMask.low | WriteMask.high

	private case class Operand(val register: RegisterName.C, val part: OperandPart.C, val selection: OperandSelection.E) {
		def === (rhs: Operand): Bool = {
			(register === rhs.register) && (part === rhs.part) && (selection === rhs.selection)
		}
	}

	private object Operand {
		val f = Operand(register = RegisterName.ft, part = OperandPart.high, selection = OperandSelection.register)
		val t = Operand(register = RegisterName.ft, part = OperandPart.low, selection = OperandSelection.register)
		val b = Operand(register = RegisterName.bc, part = OperandPart.high, selection = OperandSelection.register)
		val c = Operand(register = RegisterName.bc, part = OperandPart.low, selection = OperandSelection.register)
		val d = Operand(register = RegisterName.de, part = OperandPart.high, selection = OperandSelection.register)
		val e = Operand(register = RegisterName.de, part = OperandPart.low, selection = OperandSelection.register)
		val h = Operand(register = RegisterName.hl, part = OperandPart.high, selection = OperandSelection.register)
		val l = Operand(register = RegisterName.hl, part = OperandPart.low, selection = OperandSelection.register)

		val ft = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSelection.register)
		val bc = Operand(register = RegisterName.bc, part = OperandPart.full, selection = OperandSelection.register)
		val de = Operand(register = RegisterName.de, part = OperandPart.full, selection = OperandSelection.register)
		val hl = Operand(register = RegisterName.hl, part = OperandPart.full, selection = OperandSelection.register)

		val opcode_r8 = Operand(register = registerPair3, part = registerPair3Part, selection = OperandSelection.register)
		val opcode_r16 = Operand(register = registerPair2, part = OperandPart.full, selection = OperandSelection.register)
		val immediate_byte = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSelection.memory)
		val signed_immediate_byte = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSelection.signed_memory)
		val ones = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSelection.ones)
		val zero = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSelection.zero)
		val pc = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSelection.pc)
	}

	private case class OperandWriter(index: Int) {
		def := (operand: Operand): Unit = {
			io.output.readStageControl.registers(index) := operand.register
			io.output.readStageControl.part(index) := operand.part
			io.output.aluStageControl.selection(index) := operand.selection
			if (operand.selection == OperandSelection.memory || operand.selection == OperandSelection.signed_memory) {
				loadImmediateByte()
			}
		}
	}

	private val operand1 = OperandWriter(0)
	private val operand2 = OperandWriter(1)

	private class Destination(val operand: Operand, val mask: WriteMask.C) {
		private val control = io.output.writeStageControl.fileControl(operand.register)
		def := (source: WriteSource.E): Unit = {
			io.output.writeStageControl.source := source

			control.rot8 := mask === WriteMask.low
			control.sourceExg := False
			control.registerControl.write := True
			control.registerControl.mask := mask
		}

		def := (source: Operand): Unit = {
			operand1 := source
			io.output.aluStageControl.operation := AluOperation.operand1
			this := WriteSource.alu
		}

		def exchange(opcodeRegister: Destination): Unit = {
			when (opcodeRegister.operand === Operand.f) {
				operand1 := Operand.ft
				io.output.aluStageControl.operation := AluOperation.operand1

				io.output.writeStageControl.source := WriteSource.alu

				control.rot8 := True
				control.sourceExg := False
				control.registerControl.write := True
				control.registerControl.mask := WriteMask.full
			} otherwise {
				this := opcodeRegister.operand
				operand2 := operand

				val opcodeControl = io.output.writeStageControl.fileControl(opcodeRegister.operand.register)
				opcodeControl.rot8 := opcodeRegister.mask === WriteMask.low
				opcodeControl.sourceExg := True
				opcodeControl.registerControl.write := True
				opcodeControl.registerControl.mask := opcodeRegister.mask
			}
		}
	}

	private object Destination {
		val f = new Destination(operand = Operand.f, mask = WriteMask.high)
		val t = new Destination(operand = Operand.t, mask = WriteMask.low)
		val b = new Destination(operand = Operand.b, mask = WriteMask.high)
		val c = new Destination(operand = Operand.c, mask = WriteMask.low)
		val d = new Destination(operand = Operand.d, mask = WriteMask.high)
		val e = new Destination(operand = Operand.e, mask = WriteMask.low)
		val h = new Destination(operand = Operand.h, mask = WriteMask.high)
		val l = new Destination(operand = Operand.l, mask = WriteMask.low)

		val ft = new Destination(operand = Operand.ft, mask = WriteMask.full)
		val bc = new Destination(operand = Operand.bc, mask = WriteMask.full)
		val de = new Destination(operand = Operand.de, mask = WriteMask.full)
		val hl = new Destination(operand = Operand.hl, mask = WriteMask.full)

		val opcode_r8 = new Destination(operand = Operand.opcode_r8, mask = registerPair3WritePart)
		val opcode_r16 = new Destination(operand = Operand.opcode_r16, mask = WriteMask.full)
	}

	private val anyActive = io.nmiActive || io.intActive || io.sysActive

	private val reqExtInt = io.intReq && io.intEnable && !io.intActive && !io.nmiActive

	def setDefaults(): Unit = {
		io.output.intEnable := io.intEnable
		io.output.nmiActive := io.nmiActive
		io.output.intActive := io.intActive
		io.output.sysActive := io.sysActive
		
		io.output.readStageControl.registers.foreach(_ := RegisterName.ft)
		io.output.readStageControl.part.foreach(_ := OperandPart.full)

		io.output.memoryStageControl.enable  := False
		io.output.memoryStageControl.write   := False
		io.output.memoryStageControl.io      := False
		io.output.memoryStageControl.code    := False
		io.output.memoryStageControl.config  := False
		io.output.memoryStageControl.data    := ValueSource.register1
		io.output.memoryStageControl.address := ValueSource.register2

		io.output.aluStageControl.selection.foreach(_ := OperandSelection.register)
		io.output.aluStageControl.operation := AluOperation.and
		io.output.aluStageControl.condition := Condition.t
		io.output.aluStageControl.shiftOperation := ShiftOperation.ls

		io.output.writeStageControl.source    := WriteSource.alu
		for (i <- 0 to 3) {
			val fileControl = io.output.writeStageControl.fileControl(i)
			fileControl.rot8 := False
			fileControl.sourceExg := False
			fileControl.registerControl.write := False
			fileControl.registerControl.push  := False
			fileControl.registerControl.pop   := False
			fileControl.registerControl.swap  := False
			fileControl.registerControl.mask  := WriteMask.none
		}

		io.output.pcControl.truePath      := PcTruePath.offsetFromDecoder
		io.output.pcControl.decodedOffset := U(0)
		io.output.pcControl.vector        := U(0)
		io.output.pcControl.condition     := PcCondition.always
	}

	setDefaults()

	when (io.nmiReq) {
		io.output.nmiActive := True
		interrupt(Vectors.NonMaskableInterrupt)
	}.elsewhen (reqExtInt) {
		io.output.intActive := True
		interrupt(Vectors.ExternalInterrupt)
	}.otherwise {
		switch (io.opcode) {
			for (op <- Opcodes.illegals) {
				is (op) { 
					io.output.nmiActive := True
					interrupt(Vectors.IllegalInstruction)
				}
			}

			// Opcodes with no fields
			is (Opcodes.AND_T_I)  { operation_T_I(AluOperation.and) }
			is (Opcodes.DI)       { di() }
			is (Opcodes.EI)       { ei() }
			is (Opcodes.EXT_T)    { ext_T() }
			is (Opcodes.LD_CR_T)  { ld_CR_T() }
			is (Opcodes.LD_T_CR)  { ld_T_CR() }
			is (Opcodes.LS_FT_I)  { shift_FT(ShiftOperation.ls, Operand.immediate_byte) }
			is (Opcodes.NEG_T)    { operation_T(Operand.zero, AluOperation.sub) }
			is (Opcodes.NEG_FT)   { modifyRegisterPair(Operand.zero, AluOperation.sub) }
			is (Opcodes.NOP)      { }
			is (Opcodes.NOT_F)    { not_F() }
			is (Opcodes.OR_T_I)   { operation_T_I(AluOperation.or) }
			is (Opcodes.POPA)     { stackAll(_.pop := True) }
			is (Opcodes.PUSHA)    { stackAll(_.push := True) }
			is (Opcodes.RETI)     { reti() }
			is (Opcodes.RS_FT_I)  { shift_FT(ShiftOperation.rs, Operand.immediate_byte) }
			is (Opcodes.RSA_FT_I) { shift_FT(ShiftOperation.rsa, Operand.immediate_byte) }
			is (Opcodes.SWAPA)    { stackAll(_.swap := True) }
			is (Opcodes.SYS_I)    {
				when (anyActive) {
					io.output.nmiActive := True
					interrupt(Vectors.IllegalInterrupt)
				}.otherwise {
					io.output.sysActive := True
					sys()
				}
			}
			is (Opcodes.XOR_T_I)  { operation_T_I(AluOperation.xor) }


			// Opcodes with two-bit field
			is (Opcodes.ADD_FT_R16) { operation_FT_R16(AluOperation.add) }
			is (Opcodes.ADD_R16_I)  { add_R16_I() }
			is (Opcodes.CMP_FT_R16) { cmp_FT_R16() }
			is (Opcodes.EXG_FT_R16) { exg_FT_R16() }
			is (Opcodes.JAL_R16)    { jal_R16() }
			is (Opcodes.J_R16)      { j_R16() }
			is (Opcodes.LD_FT_R16)  { ld_FT_R16() }
			is (Opcodes.LD_IO_T)    { ld_IO_T() }
			is (Opcodes.LD_MEM_T)   { ld_MEM_T() }
			is (Opcodes.LD_R16_FT)  { ld_R16_FT() }
			is (Opcodes.LD_T_IO)    { ld_T_IO() }
			is (Opcodes.LD_T_CODE)  { ld_T_CODE() }
			is (Opcodes.LD_T_MEM)   { ld_T_MEM() }
			is (Opcodes.POP)        { stack(_.pop := True) }
			is (Opcodes.PUSH)       { stack(_.push := True) }
			is (Opcodes.SUB_FT_R16) { operation_FT_R16(AluOperation.sub) }
			is (Opcodes.SWAP)       { stack(_.swap := True) }
			is (Opcodes.TST_R16)    { tst_R16() }


			// Opcodes with three-bit field
			is (Opcodes.ADD_T_R8)  { operation_T_R8(AluOperation.add) }
			is (Opcodes.ADD_R8_I)  { add_R8_I() }
			is (Opcodes.AND_T_R8)  { operation_T_R8(AluOperation.and) }
			is (Opcodes.CMP_R8_I)  { cmp_R8_I() }
			is (Opcodes.CMP_T_R8)  { cmp_T_R8() }
			is (Opcodes.DJ_R8_I)   { dj_R8_I() }
			is (Opcodes.EXG_T_R8)  { exg_T_R8() }
			is (Opcodes.LD_MEM_R8) { ld_MEM_R8() }
			is (Opcodes.LD_R8_I)   { ld_R8_I() }
			is (Opcodes.LD_R8_MEM) { ld_R8_MEM() }
			is (Opcodes.LD_R8_T)   { ld_R8_T() }
			is (Opcodes.LD_T_R8)   { ld_T_R8() }
			is (Opcodes.LS_FT_R8)  { shift_FT(ShiftOperation.ls, Operand.opcode_r8) }
			is (Opcodes.OR_T_R8)   { operation_T_R8(AluOperation.or) }
			is (Opcodes.RS_FT_R8)  { shift_FT(ShiftOperation.rs, Operand.opcode_r8) }
			is (Opcodes.RSA_FT_R8) { shift_FT(ShiftOperation.rsa, Operand.opcode_r8) }
			is (Opcodes.SUB_T_R8)  { operation_T_R8(AluOperation.sub) }
			is (Opcodes.XOR_T_R8)  { operation_T_R8(AluOperation.xor) }


			// Opcodes with four-bit field
			is (Opcodes.J_CC_I)    {
				val cc = Condition()
				cc.assignFromBits(io.opcode(3 downto 0))
				jumpLong(cc)
			}

			default {
				io.output.nmiActive := True
				interrupt(Vectors.IllegalInstruction)
			}
		}
	}

	def di(): Unit =
		io.output.intEnable := False

	def ei(): Unit =
		io.output.intEnable := True

	def loadImmediateByte(): Unit = {
		readMemory(ValueSource.pc, doIo = False, code = True)
		io.output.pcControl.truePath := PcTruePath.offsetFromDecoder
		io.output.pcControl.decodedOffset := U(1)
	}

	def exg_T_R8(): Unit = {
		Destination.t.exchange(Destination.opcode_r8)
	}

	def exg_FT_R16(): Unit = {
		Destination.ft.exchange(Destination.opcode_r16)
	}

	def operation_T_I(operation: AluOperation.E): Unit = {
		operand1 := Operand.t
		operand2 := Operand.immediate_byte
		io.output.aluStageControl.operation := operation
		Destination.t := WriteSource.alu
	}

	def add_R8_I(): Unit = {
		operand1 := Operand.opcode_r8
		operand2 := Operand.immediate_byte
		io.output.aluStageControl.operation := AluOperation.add
		Destination.opcode_r8 := WriteSource.alu
	}

	def add_R16_I(): Unit = {
		operand1 := Operand.opcode_r16
		operand2 := Operand.signed_immediate_byte
		io.output.aluStageControl.operation := AluOperation.add
		Destination.opcode_r16 := WriteSource.alu
	}

	def shift_FT(operation: ShiftOperation.E, operand: Operand): Unit = {
		operand1 := Operand.ft
		operand2 := operand

		io.output.aluStageControl.operation := AluOperation.shift
		io.output.aluStageControl.shiftOperation := operation

		Destination.ft := WriteSource.alu
	}

	def ext_T(): Unit = {
		operand1 := Operand.t

		io.output.aluStageControl.operation := AluOperation.extend1

		Destination.f := WriteSource.alu
	}

	def operation_T(op1: Operand, operation: AluOperation.C): Unit = {
		operand1 := op1
		operand2 := Operand.t
		
		io.output.aluStageControl.operation := operation

		Destination.t := WriteSource.alu
	}

	def operation_T_R8(operation: AluOperation.C): Unit = {
		operand1 := Operand.t
		operand2 := Operand.opcode_r8
		
		io.output.aluStageControl.operation := operation

		Destination.t := WriteSource.alu
	}
	
	def not_F(): Unit = {
		operand1 := Operand.ones
		operand2 := Operand.f
		
		io.output.aluStageControl.operation := AluOperation.xor

		Destination.f := WriteSource.alu
	}
	
	def modifyRegister(op1: Operand, operation: AluOperation.C): Unit = {
		operand1 := op1
		operand2 := Operand.opcode_r8

		io.output.aluStageControl.operation := operation

		Destination.opcode_r8 := WriteSource.alu
	}

	def operation_FT_R16(operation: AluOperation.C): Unit = {
		operand1 := Operand.ft
		operand2 := Operand.opcode_r16

		io.output.aluStageControl.operation := operation

		Destination.ft := WriteSource.alu
	}

	def cmp_FT_R16(): Unit = {
		operand1 := Operand.ft
		operand2 := Operand.opcode_r16

		io.output.aluStageControl.operation := AluOperation.compare

		Destination.f := WriteSource.alu
	}

	def cmp_T_R8(): Unit = {
		operand1 := Operand.t
		operand2 := Operand.opcode_r8

		io.output.aluStageControl.operation := AluOperation.compare

		Destination.f := WriteSource.alu
	}

	def cmp_R8_I(): Unit = {
		operand1 := Operand.opcode_r8
		operand2 := Operand.immediate_byte

		io.output.aluStageControl.operation := AluOperation.compare

		Destination.f := WriteSource.alu
	}

	def tst_R16(): Unit = {
		operand1 := Operand.opcode_r16
		operand2 := Operand.zero
		io.output.aluStageControl.operation := AluOperation.compare

		Destination.f := WriteSource.alu
	}

	def modifyRegisterPair(op1: Operand, operation: AluOperation.C): Unit = {
		operand1 := op1
		operand2 := Operand.opcode_r16

		io.output.aluStageControl.operation := operation

		Destination.opcode_r16 := WriteSource.alu
	}

	def writeMemory(addressSource: ValueSource.E, dataSource: ValueSource.E, doIo: Bool = False): Unit = {
		io.output.memoryStageControl.enable := True
		io.output.memoryStageControl.write := True
		io.output.memoryStageControl.io := doIo
		io.output.memoryStageControl.address := addressSource
		io.output.memoryStageControl.data := dataSource
	}

	def readMemory(addressSource: ValueSource.E, doIo: Bool = False, code: Bool = False): Unit =  {
		io.output.memoryStageControl.enable := True
		io.output.memoryStageControl.write := False
		io.output.memoryStageControl.io := doIo
		io.output.memoryStageControl.address := addressSource
		io.output.memoryStageControl.code := code
	}

	def jumpLong(condition: Condition.C = Condition.t): Unit = {
		operand1 := Operand.f
		readMemory(ValueSource.pc, doIo = False, code = True)
		io.output.aluStageControl.operation := AluOperation.operand1
		io.output.aluStageControl.condition := condition
		io.output.pcControl.truePath  := PcTruePath.offsetFromMemory
		io.output.pcControl.condition := PcCondition.whenConditionMet
	}

	def dj_R8_I(): Unit = {
		operand1 := Operand.opcode_r8
		operand2 := Operand.ones
		readMemory(ValueSource.pc, doIo = False, code = True)
		io.output.aluStageControl.operation := AluOperation.add
		io.output.pcControl.truePath  := PcTruePath.offsetFromMemory
		io.output.pcControl.condition := PcCondition.whenResultNotZero
		Destination.opcode_r8 := WriteSource.alu
	}

	def stack(setter: RegisterControl => Unit): Unit = {
		operand1 := Operand.opcode_r16
		io.output.aluStageControl.operation := AluOperation.operand1

		io.output.writeStageControl.source := WriteSource.alu

		val fileControl = io.output.writeStageControl.fileControl(registerPair2)
		setter(fileControl.registerControl)
	}

	def stackAll(setter: RegisterControl => Unit): Unit = {
		for (i <- 0 to 3) {
			val fileControl = io.output.writeStageControl.fileControl(i)
			setter(fileControl.registerControl)
		}
	}

	def ld_MEM_T(): Unit = {
		operand1 := Operand.opcode_r16
		operand2 := Operand.t
		writeMemory(ValueSource.register1, ValueSource.register2)
	}

	def ld_MEM_R8(): Unit = {
		operand1 := Operand.ft
		operand2 := Operand.opcode_r8
		writeMemory(ValueSource.register1, ValueSource.register2)
	}

	def ld_T_MEM(): Unit = {
		operand1 := Operand.opcode_r16
		readMemory(ValueSource.register1)
		Destination.t := WriteSource.memory
	}

	def ld_R8_MEM(): Unit = {
		operand1 := Operand.ft
		readMemory(ValueSource.register1)
		Destination.opcode_r8 := WriteSource.memory
	}

	def ld_T_CODE(): Unit = {
		operand1 := Operand.opcode_r16
		readMemory(ValueSource.register1, doIo = False, code = True)
		Destination.t := WriteSource.memory
	}

	def jal_R16(): Unit = {
		operand1 := Operand.pc
		operand2 := Operand.opcode_r16
		io.output.aluStageControl.operation := AluOperation.operand1
		io.output.pcControl.truePath := PcTruePath.register2
		Destination.hl := WriteSource.alu
	}

	def j_R16(): Unit = {
		operand1 := Operand.opcode_r16
		io.output.pcControl.truePath := PcTruePath.register1
	}

	def ld_R8_T(): Unit = {
		Destination.opcode_r8 := Operand.t
	}

	def ld_T_R8(): Unit = {
		Destination.t := Operand.opcode_r8
	}

	def ld_FT_R16(): Unit = {
		Destination.ft := Operand.opcode_r16
	}

	def ld_R16_FT(): Unit = {
		Destination.opcode_r16 := Operand.ft
	}

	def ld_R8_I(): Unit = {
		loadImmediateByte()
		Destination.opcode_r8 := WriteSource.memory
	}

	def ld_IO_T(): Unit = {
		operand1 := Operand.opcode_r16
		operand2 := Operand.t
		writeMemory(ValueSource.register1, ValueSource.register2, doIo = True)
	}

	def ld_T_IO(): Unit = {
		operand1 := Operand.opcode_r16
		readMemory(ValueSource.register1, doIo = True)
		Destination.t := WriteSource.memory
	}

	def ld_CR_T(): Unit = {
		operand1 := Operand.t
		operand2 := Operand.c
		io.output.memoryStageControl.enable := True
		io.output.memoryStageControl.write := True
		io.output.memoryStageControl.config := True
		io.output.memoryStageControl.address := ValueSource.register2
		io.output.memoryStageControl.data := ValueSource.register1
		io.output.aluStageControl.operation := AluOperation.operand1
	}

	def ld_T_CR(): Unit = {
		operand1 := Operand.c
		io.output.memoryStageControl.enable := True
		io.output.memoryStageControl.config := True
		io.output.memoryStageControl.write := False
		io.output.memoryStageControl.address := ValueSource.register1
		Destination.t := WriteSource.memory
	}

	def pushValueHL(): Unit = {
		io.output.writeStageControl.source := WriteSource.alu
		val hl = io.output.writeStageControl.fileControl(RegisterName.hl)
		hl.registerControl.push := True
		hl.registerControl.write := True
		hl.registerControl.mask := WriteMask.full
	}

	def sys(): Unit = {
		operand1 := Operand.pc
		operand2 := Operand.ones
		io.output.aluStageControl.operation := AluOperation.sub

		readMemory(ValueSource.pc, doIo = False, code = True)
		io.output.pcControl.truePath := PcTruePath.vectorFromMemory

		pushValueHL()
	}

	def interrupt(vector: Int): Unit = {
		operand1 := Operand.pc
		operand2 := Operand.ones
		io.output.aluStageControl.operation := AluOperation.add

		io.output.pcControl.vector := vector >> 3
		io.output.pcControl.truePath := PcTruePath.vectorFromDecoder

		pushValueHL()
	}

	def reti(): Unit = {
		when (anyActive) {
			when (io.nmiActive) {
				io.output.nmiActive := False
			}.elsewhen (io.intActive) {
				io.output.intActive := False
			}.otherwise /* handlingSys */ {
				io.output.sysActive := False
			}
			operand1 := Operand.hl
			io.output.pcControl.truePath := PcTruePath.register1

			io.output.writeStageControl.fileControl(RegisterName.hl).registerControl.pop := True
		}.otherwise {
			io.output.nmiActive := True
			interrupt(Vectors.IllegalInterrupt)
		}
	}
}

