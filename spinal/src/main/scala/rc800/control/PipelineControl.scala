package rc800.control

import spinal.core._
import spinal.lib._

import rc800.alu.AluOperation
import rc800.alu.OperandSource
import rc800.alu.PcTruePathSource

import rc800.registers.OperandPart
import rc800.registers.RegisterName
import rc800.registers.WriteMask


case class PipelineControl() extends Bundle {
	import PipelineControl._

	val readStageControl   = ReadRegisterStageControl()
	val memoryStageControl = MemoryStageControl()
	val aluStageControl    = AluStageControl()
	val writeStageControl  = WriteBackStageControl()

	def loadImmediateByte(): Unit = {
		readMemory(MemoryStageAddressSource.pc, doIo = False, code = True)
		aluStageControl.pcControl.truePath := PcTruePathSource.offsetFromDecoder
		aluStageControl.pcControl.decodedOffset := U(1)
	}

	def writeMemory(addressSource: MemoryStageAddressSource.E, doIo: Bool = False): Unit = {
		memoryStageControl.enable := True
		memoryStageControl.write := True
		memoryStageControl.io := doIo
		memoryStageControl.address := addressSource
	}

	def readMemory(addressSource: MemoryStageAddressSource.E, doIo: Bool = False, code: Bool = False): Unit =  {
		memoryStageControl.enable := True
		memoryStageControl.write := False
		memoryStageControl.io := doIo
		memoryStageControl.address := addressSource
		memoryStageControl.code := code
	}

	def pushValueHL(): Unit = {
		writeStageControl.source := WriteBackValueSource.alu
		val hl = writeStageControl.fileControl(RegisterName.hl)
		hl.registerControl.push := True
		hl.registerControl.write := True
		hl.registerControl.mask := WriteMask.full
	}

	def interrupt(vector: Int): Unit = {
		operand1 := Operand.pc
		operand2 := Operand.ones
		aluStageControl.aluControl.operation := AluOperation.add

		aluStageControl.pcControl.vector := vector >> 3
		aluStageControl.pcControl.truePath := PcTruePathSource.vectorFromDecoder

		pushValueHL()
	}

	case class OperandWriter(index: Int) {
		def := (operand: Operand): Unit = {
			readStageControl.registers(index) := operand.register
			readStageControl.part(index) := operand.part
			aluStageControl.selection(index) := operand.selection
			if (operand.selection == OperandSource.memory || operand.selection == OperandSource.signed_memory) {
				loadImmediateByte()
			}
		}
	}

	val operand1 = OperandWriter(0)
	val operand2 = OperandWriter(1)

	def destination(operand: Operand, mask: WriteMask.C) =
		Destination(operand, mask)

	case class Destination(val operand: Operand, val mask: WriteMask.C) {
		private val control = writeStageControl.fileControl(operand.register)
		def := (source: WriteBackValueSource.E): Unit = {
			writeStageControl.source := source

			control.rot8 := mask === WriteMask.low
			control.sourceExg := False
			control.registerControl.write := True
			control.registerControl.mask := mask
		}

		def := (source: Operand): Unit = {
			operand1 := source
			aluStageControl.aluControl.operation := AluOperation.operand1
			this := WriteBackValueSource.alu
		}

		def exchange(opcodeRegister: Destination): Unit = {
			when (opcodeRegister.operand === Operand.f) {
				operand1 := Operand.ft
				aluStageControl.aluControl.operation := AluOperation.operand1

				writeStageControl.source := WriteBackValueSource.alu

				control.rot8 := True
				control.sourceExg := False
				control.registerControl.write := True
				control.registerControl.mask := WriteMask.full
			} otherwise {
				this := opcodeRegister.operand
				operand2 := operand

				val opcodeControl = writeStageControl.fileControl(opcodeRegister.operand.register)
				opcodeControl.rot8 := opcodeRegister.mask === WriteMask.low
				opcodeControl.sourceExg := True
				opcodeControl.registerControl.write := True
				opcodeControl.registerControl.mask := opcodeRegister.mask
			}
		}
	}

	object Destination {
		def f = new Destination(operand = Operand.f, mask = WriteMask.high)
		def t = new Destination(operand = Operand.t, mask = WriteMask.low)
		def b = new Destination(operand = Operand.b, mask = WriteMask.high)
		def c = new Destination(operand = Operand.c, mask = WriteMask.low)
		def d = new Destination(operand = Operand.d, mask = WriteMask.high)
		def e = new Destination(operand = Operand.e, mask = WriteMask.low)
		def h = new Destination(operand = Operand.h, mask = WriteMask.high)
		def l = new Destination(operand = Operand.l, mask = WriteMask.low)

		def ft = new Destination(operand = Operand.ft, mask = WriteMask.full)
		def bc = new Destination(operand = Operand.bc, mask = WriteMask.full)
		def de = new Destination(operand = Operand.de, mask = WriteMask.full)
		def hl = new Destination(operand = Operand.hl, mask = WriteMask.full)
	}
}


object PipelineControl {
	case class Operand(val register: RegisterName.C, val part: OperandPart.C, val selection: OperandSource.E) {
		def === (rhs: Operand): Bool = {
			(register === rhs.register) && (part === rhs.part) && (selection === rhs.selection)
		}
	}

	object Operand {
		def f = Operand(register = RegisterName.ft, part = OperandPart.high, selection = OperandSource.register)
		def t = Operand(register = RegisterName.ft, part = OperandPart.low, selection = OperandSource.register)
		def b = Operand(register = RegisterName.bc, part = OperandPart.high, selection = OperandSource.register)
		def c = Operand(register = RegisterName.bc, part = OperandPart.low, selection = OperandSource.register)
		def d = Operand(register = RegisterName.de, part = OperandPart.high, selection = OperandSource.register)
		def e = Operand(register = RegisterName.de, part = OperandPart.low, selection = OperandSource.register)
		def h = Operand(register = RegisterName.hl, part = OperandPart.high, selection = OperandSource.register)
		def l = Operand(register = RegisterName.hl, part = OperandPart.low, selection = OperandSource.register)

		def ft = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSource.register)
		def bc = Operand(register = RegisterName.bc, part = OperandPart.full, selection = OperandSource.register)
		def de = Operand(register = RegisterName.de, part = OperandPart.full, selection = OperandSource.register)
		def hl = Operand(register = RegisterName.hl, part = OperandPart.full, selection = OperandSource.register)

		def immediate_byte = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSource.memory)
		def signed_immediate_byte = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSource.signed_memory)
		def ones = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSource.ones)
		def zero = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSource.zero)
		def pc = Operand(register = RegisterName.ft, part = OperandPart.full, selection = OperandSource.pc)
	}
}