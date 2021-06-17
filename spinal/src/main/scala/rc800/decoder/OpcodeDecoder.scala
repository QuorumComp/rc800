package rc800.decoder

import spinal.core._
import spinal.core.sim._
import spinal.lib._

import rc800.alu.AluOperation
import rc800.alu.Condition
import rc800.alu.OperandSource
import rc800.alu.PcCondition
import rc800.alu.PcTruePathSource
import rc800.alu.ShiftOperation

import rc800.control.MemoryStageAddressSource
import rc800.control.PipelineControl
import rc800.control.WriteBackValueSource

import rc800.control.component.RegisterControl

import rc800.registers.OperandPart
import rc800.registers.RegisterName
import rc800.registers.WriteMask

import rc800.Vectors

import Pipeline.PipelineControlPimp


case class OpcodeDecoder() extends Component {
	val io = new Bundle {
		val opcode = in Bits(8 bits)
		val output = out (DecoderOutput())

		def controlSignals = output.stageControl
	}

	private val registerPair2 = io.opcode(1 downto 0).as(RegisterName())
	private val registerPair3 = io.opcode(2 downto 1).as(RegisterName())
	private val registerPair3Low = io.opcode(0)
	private val registerPair3Part = registerPair3Low ? OperandPart.low | OperandPart.high

	private val registerPair3WritePart = WriteMask()
	registerPair3WritePart := registerPair3Low ? WriteMask.low | WriteMask.high

	private object Operand {
		def opcode_r8 = Pipeline.Operand(register = registerPair3, part = registerPair3Part, selection = OperandSource.register)
		def opcode_r16 = Pipeline.Operand(register = registerPair2, part = OperandPart.full, selection = OperandSource.register)
	}


	private object Destination {
		def opcode_r8 = io.controlSignals.destination(operand = Operand.opcode_r8, mask = registerPair3WritePart)
		def opcode_r16 = io.controlSignals.destination(operand = Operand.opcode_r16, mask = WriteMask.full)
	}

	io.output.illegal := False
	io.output.stageControl.setDefaults()

	when (Opcodes.illegals.map(io.opcode === _).reduce((v1, v2) => v1 || v2)) { 
		io.output.illegal := True
		io.controlSignals.interrupt(U(Vectors.IllegalInstruction >> 3, 3 bits))
	} otherwise {
		switch (io.opcode) {
			// Opcodes with no fields
			is (Opcodes.AND_T_I)  { operation_T_I(AluOperation.and) }
			is (Opcodes.DI)       { }
			is (Opcodes.EI)       { }
			is (Opcodes.EXT_T)    { ext_T() }
			is (Opcodes.LD_CR_T)  { ld_CR_T() }
			is (Opcodes.LD_T_CR)  { ld_T_CR() }
			is (Opcodes.LS_FT_I)  { shift_FT(AluOperation.ls, Pipeline.Operand.immediate_byte) }
			is (Opcodes.NEG_T)    { operation_T(Pipeline.Operand.zero, AluOperation.sub) }
			is (Opcodes.NEG_FT)   { modifyRegisterPair(Pipeline.Operand.zero, AluOperation.sub) }
			is (Opcodes.NOP)      { }
			is (Opcodes.NOT_F)    { not_F() }
			is (Opcodes.OR_T_I)   { operation_T_I(AluOperation.or) }
			is (Opcodes.POPA)     { stackAll(_.pop := True) }
			is (Opcodes.PUSHA)    { stackAll(_.push := True) }
			is (Opcodes.RETI)     { reti() }
			is (Opcodes.RS_FT_I)  { shift_FT(AluOperation.rs, Pipeline.Operand.immediate_byte) }
			is (Opcodes.RSA_FT_I) { shift_FT(AluOperation.rsa, Pipeline.Operand.immediate_byte) }
			is (Opcodes.SWAPA)    { stackAll(_.swap := True) }
			is (Opcodes.SYS_I)    { sys() }
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
			is (Opcodes.LS_FT_R8)  { shift_FT(AluOperation.ls, Operand.opcode_r8) }
			is (Opcodes.OR_T_R8)   { operation_T_R8(AluOperation.or) }
			is (Opcodes.RS_FT_R8)  { shift_FT(AluOperation.rs, Operand.opcode_r8) }
			is (Opcodes.RSA_FT_R8) { shift_FT(AluOperation.rsa, Operand.opcode_r8) }
			is (Opcodes.SUB_T_R8)  { operation_T_R8(AluOperation.sub) }
			is (Opcodes.XOR_T_R8)  { operation_T_R8(AluOperation.xor) }


			// Opcodes with four-bit field
			is (Opcodes.J_CC_I)    {
				val cc = Condition()
				cc.assignFromBits(io.opcode(3 downto 0))
				jumpLong(cc)
			}
		}
	}

	def exg_T_R8(): Unit = {
		io.controlSignals.Destination.t.exchange(Destination.opcode_r8)
	}

	def exg_FT_R16(): Unit = {
		io.controlSignals.Destination.ft.exchange(Destination.opcode_r16)
	}

	def operation_T_I(operation: AluOperation.E): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.t
		io.controlSignals.operand2 := Pipeline.Operand.immediate_byte
		io.controlSignals.aluStageControl.aluControl.operation := operation
		io.controlSignals.Destination.t := WriteBackValueSource.alu
	}

	def add_R8_I(): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r8
		io.controlSignals.operand2 := Pipeline.Operand.immediate_byte
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.add
		Destination.opcode_r8 := WriteBackValueSource.alu
	}

	def add_R16_I(): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r16
		io.controlSignals.operand2 := Pipeline.Operand.signed_immediate_byte
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.add
		Destination.opcode_r16 := WriteBackValueSource.alu
	}

	def shift_FT(operation: AluOperation.E, operand: Pipeline.Operand): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.ft
		io.controlSignals.operand2 := operand

		io.controlSignals.aluStageControl.aluControl.operation := operation

		io.controlSignals.Destination.ft := WriteBackValueSource.alu
	}

	def ext_T(): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.t

		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.extend1

		io.controlSignals.Destination.f := WriteBackValueSource.alu
	}

	def operation_T(op1: Pipeline.Operand, operation: AluOperation.C): Unit = {
		io.controlSignals.operand1 := op1
		io.controlSignals.operand2 := Pipeline.Operand.t
		
		io.controlSignals.aluStageControl.aluControl.operation := operation

		io.controlSignals.Destination.t := WriteBackValueSource.alu
	}

	def operation_T_R8(operation: AluOperation.C): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.t
		io.controlSignals.operand2 := Operand.opcode_r8
		
		io.controlSignals.aluStageControl.aluControl.operation := operation

		io.controlSignals.Destination.t := WriteBackValueSource.alu
	}
	
	def not_F(): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.ones
		io.controlSignals.operand2 := Pipeline.Operand.f
		
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.xor

		io.controlSignals.Destination.f := WriteBackValueSource.alu
	}
	
	def modifyRegister(op1: Pipeline.Operand, operation: AluOperation.C): Unit = {
		io.controlSignals.operand1 := op1
		io.controlSignals.operand2 := Operand.opcode_r8

		io.controlSignals.aluStageControl.aluControl.operation := operation

		Destination.opcode_r8 := WriteBackValueSource.alu
	}

	def operation_FT_R16(operation: AluOperation.C): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.ft
		io.controlSignals.operand2 := Operand.opcode_r16

		io.controlSignals.aluStageControl.aluControl.operation := operation

		io.controlSignals.Destination.ft := WriteBackValueSource.alu
	}

	def cmp_FT_R16(): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.ft
		io.controlSignals.operand2 := Operand.opcode_r16

		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.compare

		io.controlSignals.Destination.f := WriteBackValueSource.alu
	}

	def cmp_T_R8(): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.t
		io.controlSignals.operand2 := Operand.opcode_r8

		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.compare

		io.controlSignals.Destination.f := WriteBackValueSource.alu
	}

	def cmp_R8_I(): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r8
		io.controlSignals.operand2 := Pipeline.Operand.immediate_byte

		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.compare

		io.controlSignals.Destination.f := WriteBackValueSource.alu
	}

	def tst_R16(): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r16
		io.controlSignals.operand2 := Pipeline.Operand.zero
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.compare

		io.controlSignals.Destination.f := WriteBackValueSource.alu
	}

	def modifyRegisterPair(op1: Pipeline.Operand, operation: AluOperation.C): Unit = {
		io.controlSignals.operand1 := op1
		io.controlSignals.operand2 := Operand.opcode_r16

		io.controlSignals.aluStageControl.aluControl.operation := operation

		Destination.opcode_r16 := WriteBackValueSource.alu
	}

	def jumpLong(condition: Condition.C = Condition.t): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.f
		io.controlSignals.operand2 := Pipeline.Operand.zero
		io.controlSignals.readMemory(MemoryStageAddressSource.pc, doIo = False, code = True)
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.or
		io.controlSignals.aluStageControl.aluControl.condition := condition
		io.controlSignals.aluStageControl.pcControl.truePath  := PcTruePathSource.offsetFromMemory
		io.controlSignals.aluStageControl.pcControl.condition := PcCondition.whenConditionMet
	}

	def dj_R8_I(): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r8
		io.controlSignals.operand2 := Pipeline.Operand.ones
		io.controlSignals.readMemory(MemoryStageAddressSource.pc, doIo = False, code = True)
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.add
		io.controlSignals.aluStageControl.pcControl.truePath  := PcTruePathSource.offsetFromMemory
		io.controlSignals.aluStageControl.pcControl.condition := PcCondition.whenResultNotZero
		Destination.opcode_r8 := WriteBackValueSource.alu
	}

	def stack(setter: RegisterControl => Unit): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r16
		io.controlSignals.operand2 := Pipeline.Operand.zero
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.or

		io.controlSignals.writeStageControl.source := WriteBackValueSource.alu

		val fileControl = io.controlSignals.writeStageControl.fileControl(registerPair2)
		setter(fileControl.registerControl)
	}

	def stackAll(setter: RegisterControl => Unit): Unit = {
		for (i <- 0 to 3) {
			val fileControl = io.controlSignals.writeStageControl.fileControl(i)
			setter(fileControl.registerControl)
		}
	}

	def ld_MEM_T(): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r16
		io.controlSignals.operand2 := Pipeline.Operand.t
		io.controlSignals.writeMemory(MemoryStageAddressSource.register1)
	}

	def ld_MEM_R8(): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.ft
		io.controlSignals.operand2 := Operand.opcode_r8
		io.controlSignals.writeMemory(MemoryStageAddressSource.register1)
	}

	def ld_T_MEM(): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r16
		io.controlSignals.readMemory(MemoryStageAddressSource.register1)
		io.controlSignals.Destination.t := WriteBackValueSource.memory
	}

	def ld_R8_MEM(): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.ft
		io.controlSignals.readMemory(MemoryStageAddressSource.register1)
		Destination.opcode_r8 := WriteBackValueSource.memory
	}

	def ld_T_CODE(): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r16
		io.controlSignals.readMemory(MemoryStageAddressSource.register1, doIo = False, code = True)
		io.controlSignals.Destination.t := WriteBackValueSource.memory
	}

	def jal_R16(): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.pc
		io.controlSignals.operand2 := Operand.opcode_r16
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.operand1
		io.controlSignals.aluStageControl.pcControl.truePath := PcTruePathSource.register2
		io.controlSignals.Destination.hl := WriteBackValueSource.alu
	}

	def j_R16(): Unit = {
		io.controlSignals.operand2 := Operand.opcode_r16
		io.controlSignals.aluStageControl.pcControl.truePath := PcTruePathSource.register2
	}

	def ld_R8_T(): Unit = {
		Destination.opcode_r8 := Pipeline.Operand.t
	}

	def ld_T_R8(): Unit = {
		io.controlSignals.Destination.t := Operand.opcode_r8
	}

	def ld_FT_R16(): Unit = {
		io.controlSignals.Destination.ft := Operand.opcode_r16
	}

	def ld_R16_FT(): Unit = {
		Destination.opcode_r16 := Pipeline.Operand.ft
	}

	def ld_R8_I(): Unit = {
		io.controlSignals.loadImmediateByte()
		Destination.opcode_r8 := WriteBackValueSource.memory
	}

	def ld_IO_T(): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r16
		io.controlSignals.operand2 := Pipeline.Operand.t
		io.controlSignals.writeMemory(MemoryStageAddressSource.register1, doIo = True)
	}

	def ld_T_IO(): Unit = {
		io.controlSignals.operand1 := Operand.opcode_r16
		io.controlSignals.readMemory(MemoryStageAddressSource.register1, doIo = True)
		io.controlSignals.Destination.t := WriteBackValueSource.memory
	}

	def ld_CR_T(): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.c
		io.controlSignals.operand2 := Pipeline.Operand.t
		io.controlSignals.memoryStageControl.enable := True
		io.controlSignals.memoryStageControl.write := True
		io.controlSignals.memoryStageControl.config := True
		io.controlSignals.memoryStageControl.address := MemoryStageAddressSource.register1
	}

	def ld_T_CR(): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.c
		io.controlSignals.memoryStageControl.enable := True
		io.controlSignals.memoryStageControl.config := True
		io.controlSignals.memoryStageControl.write := False
		io.controlSignals.memoryStageControl.address := MemoryStageAddressSource.register1
		io.controlSignals.Destination.t := WriteBackValueSource.memory
	}

	def sys(): Unit = {
		io.controlSignals.operand1 := Pipeline.Operand.pc
		io.controlSignals.operand2 := Pipeline.Operand.ones
		io.controlSignals.aluStageControl.aluControl.operation := AluOperation.sub

		io.controlSignals.readMemory(MemoryStageAddressSource.pc, doIo = False, code = True)
		io.controlSignals.aluStageControl.pcControl.truePath := PcTruePathSource.vectorFromMemory

		io.controlSignals.pushValueHL()
	}

	def reti(): Unit = {
		io.controlSignals.operand2 := Pipeline.Operand.hl
		io.controlSignals.aluStageControl.pcControl.truePath := PcTruePathSource.register2

		io.controlSignals.writeStageControl.fileControl(RegisterName.hl).registerControl.pop := True
	}
}

