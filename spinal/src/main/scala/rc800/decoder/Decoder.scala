package rc800.decoder

import spinal.core._
import spinal.lib._

import rc800.alu.AluOperation
import rc800.alu.Condition
import rc800.alu.OperandSelection
import rc800.alu.ShiftOperation

import rc800.registers.OperandPart

import rc800.Vectors
import rc800.registers.Register
import rc800.registers.WritePart
import rc800.registers.StackOperation

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

	private val registerPair2 = io.opcode(1 downto 0).as(Register())
	private val registerPair3 = io.opcode(2 downto 1).as(Register())
	private val registerPair3Low = io.opcode(0)
	private val registerPair3Part = registerPair3Low ? OperandPart.low | OperandPart.high

	private val registerPair3WritePart = WritePart()
	registerPair3WritePart := registerPair3Low ? WritePart.low | WritePart.high

	private case class Destination(val register: Register.C, val part: WritePart.C, val operation: StackOperation.E) {
		def := (source: WriteSource.E): Unit = {
			io.output.writeStageControl.enable   := True
			io.output.writeStageControl.register := register
			io.output.writeStageControl.part     := part
			io.output.writeStageControl.stack    := operation
			io.output.writeStageControl.source   := source
		}
	}

	private object Destination {
		val f = Destination(register = Register.ft, part = WritePart.high, StackOperation.write)
		val t = Destination(register = Register.ft, part = WritePart.low, StackOperation.write)
		val b = Destination(register = Register.bc, part = WritePart.high, StackOperation.write)
		val c = Destination(register = Register.bc, part = WritePart.low, StackOperation.write)
		val d = Destination(register = Register.de, part = WritePart.high, StackOperation.write)
		val e = Destination(register = Register.de, part = WritePart.low, StackOperation.write)
		val h = Destination(register = Register.hl, part = WritePart.high, StackOperation.write)
		val l = Destination(register = Register.hl, part = WritePart.low, StackOperation.write)

		val ft = Destination(register = Register.ft, part = WritePart.full, StackOperation.write)
		val bc = Destination(register = Register.bc, part = WritePart.full, StackOperation.write)
		val de = Destination(register = Register.de, part = WritePart.full, StackOperation.write)
		val hl = Destination(register = Register.hl, part = WritePart.full, StackOperation.write)

		def opcode_r8 = Destination(register = registerPair3, part = registerPair3WritePart, StackOperation.write)
		def opcode_r8_exg = Destination(register = registerPair3, part = registerPair3WritePart, StackOperation.exchangeT)
		def opcode_r16 = Destination(register = registerPair2, part = WritePart.full, StackOperation.write)
		def opcode_r16_exg = Destination(register = registerPair2, part = WritePart.full, StackOperation.exchangeFT)
	}

	private case class Operand(val register: Register.C, val part: OperandPart.C, val selection: OperandSelection.E)
	private object Operand {
		val f = Operand(register = Register.ft, part = OperandPart.high, selection = OperandSelection.register)
		val t = Operand(register = Register.ft, part = OperandPart.low, selection = OperandSelection.register)
		val b = Operand(register = Register.bc, part = OperandPart.high, selection = OperandSelection.register)
		val c = Operand(register = Register.bc, part = OperandPart.low, selection = OperandSelection.register)
		val d = Operand(register = Register.de, part = OperandPart.high, selection = OperandSelection.register)
		val e = Operand(register = Register.de, part = OperandPart.low, selection = OperandSelection.register)
		val h = Operand(register = Register.hl, part = OperandPart.high, selection = OperandSelection.register)
		val l = Operand(register = Register.hl, part = OperandPart.low, selection = OperandSelection.register)

		val ft = Operand(register = Register.ft, part = OperandPart.full, selection = OperandSelection.register)
		val bc = Operand(register = Register.bc, part = OperandPart.full, selection = OperandSelection.register)
		val de = Operand(register = Register.de, part = OperandPart.full, selection = OperandSelection.register)
		val hl = Operand(register = Register.hl, part = OperandPart.full, selection = OperandSelection.register)

		def opcode_r8 = Operand(register = registerPair3, part = registerPair3Part, selection = OperandSelection.register)
		def opcode_r16 = Operand(register = registerPair2, part = OperandPart.full, selection = OperandSelection.register)
		val immediate_byte = Operand(register = Register.ft, part = OperandPart.full, selection = OperandSelection.memory)
		val signed_immediate_byte = Operand(register = Register.ft, part = OperandPart.full, selection = OperandSelection.signed_memory)
		val one = Operand(register = Register.ft, part = OperandPart.full, selection = OperandSelection.one)
		val ones = Operand(register = Register.ft, part = OperandPart.full, selection = OperandSelection.ones)
		val zero = Operand(register = Register.ft, part = OperandPart.full, selection = OperandSelection.zero)
		val pc = Operand(register = Register.ft, part = OperandPart.full, selection = OperandSelection.pc)
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

	setDefaults()

	private val anyActive = io.nmiActive || io.intActive || io.sysActive

	private val reqExtInt = io.intReq && io.intEnable && !io.intActive && !io.nmiActive

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
			is (Opcodes.LD_IO_T)  { ld_IO_T() }
			is (Opcodes.LD_T_CR)  { ld_T_CR() }
			is (Opcodes.LD_T_IO)  { ld_T_IO() }
			is (Opcodes.LS_FT_I)  { shift_FT(ShiftOperation.ls, Operand.immediate_byte) }
			is (Opcodes.NEG_T)    { operation_T(Operand.zero, AluOperation.sub) }
			is (Opcodes.NEG_FT)   { modifyRegisterPair(Operand.zero, AluOperation.sub) }
			is (Opcodes.NOP)      { }
			is (Opcodes.NOT_F)    { not_F() }
			is (Opcodes.OR_T_I)   { operation_T_I(AluOperation.or) }
			is (Opcodes.POPA)     { stackOperation(StackOperation.popAll) }
			is (Opcodes.PUSHA)    { stackOperation(StackOperation.pushAll) }
			is (Opcodes.RETI)     { reti() }
			is (Opcodes.RS_FT_I)  { shift_FT(ShiftOperation.rs, Operand.immediate_byte) }
			is (Opcodes.RSA_FT_I) { shift_FT(ShiftOperation.rsa, Operand.immediate_byte) }
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
			is (Opcodes.LD_MEM_T)   { ld_MEM_T() }
			is (Opcodes.LD_R16_FT)  { ld_R16_FT() }
			is (Opcodes.LD_T_CODE)  { ld_T_CODE() }
			is (Opcodes.LD_T_MEM)   { ld_T_MEM() }
			is (Opcodes.POP)        { stackOperation(StackOperation.pop) }
			is (Opcodes.PUSH)       { stackOperation(StackOperation.push) }
			is (Opcodes.SUB_FT_R16) { operation_FT_R16(AluOperation.sub) }
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
		operand1 := Operand.t
		operand2 := Operand.opcode_r8
		io.output.aluStageControl.operation := AluOperation.operand1
		Destination.opcode_r8_exg := WriteSource.alu
	}

	def exg_FT_R16(): Unit = {
		operand1 := Operand.ft
		operand2 := Operand.opcode_r16
		io.output.aluStageControl.operation := AluOperation.operand1
		Destination.opcode_r16_exg := WriteSource.alu
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

	def ext_T() {
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

	def moveRegister(dest: Destination, source: Operand): Unit = {
		operand1 := source
		io.output.aluStageControl.operation := AluOperation.operand1
		dest := WriteSource.alu
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
		io.output.memoryStageControl.data := dataSource
		io.output.memoryStageControl.address := addressSource
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

	def stackOperation(operation: StackOperation.E): Unit = {
		operand1 := Operand.opcode_r16
		io.output.aluStageControl.operation := AluOperation.operand1
		io.output.writeStageControl.enable   := True
		io.output.writeStageControl.register := registerPair2
		io.output.writeStageControl.part     := WritePart.full
		io.output.writeStageControl.stack    := operation
		io.output.writeStageControl.source   := WriteSource.alu
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
		moveRegister(Destination.opcode_r8, Operand.t)
	}

	def ld_T_R8(): Unit = {
		moveRegister(Destination.t, Operand.opcode_r8)
	}

	def ld_FT_R16(): Unit = {
		moveRegister(Destination.ft, Operand.opcode_r16)
	}

	def ld_R16_FT(): Unit = {
		moveRegister(Destination.opcode_r16, Operand.ft)
	}

	def ld_R8_I(): Unit = {
		loadImmediateByte()
		Destination.opcode_r8 := WriteSource.memory
	}

	def ld_IO_T(): Unit = {
		operand1 := Operand.bc
		operand2 := Operand.t
		writeMemory(ValueSource.register1, ValueSource.register2, doIo = True)
	}

	def ld_T_IO(): Unit = {
		operand1 := Operand.bc
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

	def sys(): Unit = {
		operand1 := Operand.pc
		operand2 := Operand.ones
		io.output.aluStageControl.operation := AluOperation.sub

		readMemory(ValueSource.pc, doIo = False, code = True)
		io.output.pcControl.truePath := PcTruePath.vectorFromMemory

		io.output.writeStageControl.enable    := True
		io.output.writeStageControl.register  := Register.hl
		io.output.writeStageControl.stack     := StackOperation.pushValue
		io.output.writeStageControl.part      := WritePart.full
		io.output.writeStageControl.source    := WriteSource.alu
	}

	def interrupt(vector: Int): Unit = {
		operand1 := Operand.pc
		operand2 := Operand.ones
		io.output.aluStageControl.operation := AluOperation.add

		io.output.pcControl.vector := vector >> 3
		io.output.pcControl.truePath := PcTruePath.vectorFromDecoder

		io.output.writeStageControl.enable    := True
		io.output.writeStageControl.register  := Register.hl
		io.output.writeStageControl.stack     := StackOperation.pushValue
		io.output.writeStageControl.part      := WritePart.full
		io.output.writeStageControl.source    := WriteSource.alu
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

			io.output.writeStageControl.enable    := True
			io.output.writeStageControl.register  := Register.hl
			io.output.writeStageControl.part      := WritePart.full
			io.output.writeStageControl.stack     := StackOperation.pop
		}.otherwise {
			io.output.nmiActive := True
			interrupt(Vectors.IllegalInterrupt)
		}
	}

	def setDefaults(): Unit = {
		io.output.intEnable := io.intEnable
		io.output.nmiActive := io.nmiActive
		io.output.intActive := io.intActive
		io.output.sysActive := io.sysActive
		
		io.output.readStageControl.registers.foreach(_ := Register.ft)
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

		io.output.writeStageControl.enable    := False
		io.output.writeStageControl.register  := Register.ft
		io.output.writeStageControl.part      := WritePart.low
		io.output.writeStageControl.stack     := StackOperation.read
		io.output.writeStageControl.source    := WriteSource.alu

		io.output.pcControl.truePath      := PcTruePath.offsetFromDecoder
		io.output.pcControl.decodedOffset := U(0)
		io.output.pcControl.vector        := U(0)
		io.output.pcControl.condition     := PcCondition.always
	}

}

