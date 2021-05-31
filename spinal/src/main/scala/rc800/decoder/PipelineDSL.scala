package rc800.decoder

import spinal.core._

import rc800.alu.AluOperation
import rc800.alu.OperandSource
import rc800.alu.PcTruePathSource

import rc800.control.MemoryStageAddressSource
import rc800.control.PipelineControl
import rc800.control.WriteBackValueSource

import rc800.registers.OperandPart
import rc800.registers.RegisterName
import rc800.registers.WriteMask


object Pipeline {
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

		def pushValueHL(): Unit = {
			pipeline.writeStageControl.source := WriteBackValueSource.alu
			val hl = pipeline.writeStageControl.fileControl(RegisterName.hl)
			hl.registerControl.push := True
			hl.registerControl.write := True
			hl.registerControl.mask := WriteMask.full
		}

		def interrupt(vector: Int): Unit = {
			operand1 := Operand.pc
			operand2 := Operand.ones
			pipeline.aluStageControl.aluControl.operation := AluOperation.add

			pipeline.aluStageControl.pcControl.vector := vector >> 3
			pipeline.aluStageControl.pcControl.truePath := PcTruePathSource.vectorFromDecoder

			pushValueHL()
		}

		case class OperandWriter(index: Int) {
			def := (operand: Operand): Unit = {
				pipeline.readStageControl.registers(index) := operand.register
				pipeline.readStageControl.part(index) := operand.part
				pipeline.aluStageControl.selection(index) := operand.selection
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
			private val control = pipeline.writeStageControl.fileControl(operand.register)
			def := (source: WriteBackValueSource.E): Unit = {
				pipeline.writeStageControl.source := source

				control.rot8 := mask === WriteMask.low
				control.sourceExg := False
				control.registerControl.write := True
				control.registerControl.mask := mask
			}

			def := (source: Operand): Unit = {
				operand1 := source
				pipeline.aluStageControl.aluControl.operation := AluOperation.operand1
				this := WriteBackValueSource.alu
			}

			def exchange(opcodeRegister: PipelineControlPimp#Destination): Unit = {
				when (opcodeRegister.operand === Operand.f) {
					operand1 := Operand.ft
					pipeline.aluStageControl.aluControl.operation := AluOperation.operand1

					pipeline.writeStageControl.source := WriteBackValueSource.alu

					control.rot8 := True
					control.sourceExg := False
					control.registerControl.write := True
					control.registerControl.mask := WriteMask.full
				} otherwise {
					this := opcodeRegister.operand
					operand2 := operand

					val opcodeControl = pipeline.writeStageControl.fileControl(opcodeRegister.operand.register)
					opcodeControl.rot8 := opcodeRegister.mask === WriteMask.low
					opcodeControl.sourceExg := True
					opcodeControl.registerControl.write := True
					opcodeControl.registerControl.mask := opcodeRegister.mask
				}
			}
		}

		object Destination {
			def f = destination(operand = Operand.f, mask = WriteMask.high)
			def t = destination(operand = Operand.t, mask = WriteMask.low)
			def b = destination(operand = Operand.b, mask = WriteMask.high)
			def c = destination(operand = Operand.c, mask = WriteMask.low)
			def d = destination(operand = Operand.d, mask = WriteMask.high)
			def e = destination(operand = Operand.e, mask = WriteMask.low)
			def h = destination(operand = Operand.h, mask = WriteMask.high)
			def l = destination(operand = Operand.l, mask = WriteMask.low)

			def ft = destination(operand = Operand.ft, mask = WriteMask.full)
			def bc = destination(operand = Operand.bc, mask = WriteMask.full)
			def de = destination(operand = Operand.de, mask = WriteMask.full)
			def hl = destination(operand = Operand.hl, mask = WriteMask.full)
		}
	}


	case class Destination(val operand: Operand, val mask: WriteMask.C)
	
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