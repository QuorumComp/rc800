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

		case class Destination(val register: RegisterName.C) {
			private val control = pipeline.writeStageControl.fileControl
			private val registerControl = control.registerControl(register)
			def := (source: WriteBackValueSource.E): Unit = {
				pipeline.writeStageControl.source := source

				control.write := True
				control.writeRegister := register
			}

			def := (source: Operand): Unit = {
				operand1 := source
				pipeline.aluStageControl.aluControl.operation := AluOperation.operand1
				this := WriteBackValueSource.alu
			}

			def := (source: RegisterName.C): Unit = {
				this := Operand(source, OperandSource.register)
			}

			def exchange(opcodeRegister: PipelineControlPimp#Destination): Unit = {
				this := opcodeRegister.register
				operand2 := register

				val opcodeControl = pipeline.writeStageControl.fileControl
				opcodeControl.writeExg := True
				opcodeControl.writeExgRegister := opcodeRegister.register
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

		def setDefaults(): Unit = {
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

			pipeline.writeStageControl.source    := WriteBackValueSource.alu
			val fileControl = pipeline.writeStageControl.fileControl
			fileControl.write := False
			fileControl.writeRegister := RegisterName.ft
			fileControl.writeExg := False
			fileControl.writeExgRegister := RegisterName.ft
			for (i <- 0 to 3) {
				val registerControl = fileControl.registerControl(i)
				registerControl.push  := False
				registerControl.pop   := False
				registerControl.swap  := False
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