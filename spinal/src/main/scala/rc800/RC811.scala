package rc800

import spinal.core._
import spinal.lib._
import spinal.core.sim._

import registers.OperandModifier
import registers.Register
import registers.RegisterFile
import registers.RegisterFileControl
import registers.RegisterName
import registers.WriteMask

import decoder.Decoder
import decoder.ValueSource
import decoder.WriteBackControl
import decoder.WriteSource

import alu.AluOperation
import alu.AluStage
import alu.Condition
import alu.OperandSelection

import stage.PcCalc

import decoder.MemoryStageControl

class RC811() extends Component {
	val io = new Bundle {
		val nmi = in Bool
		val irq = in Bool

		val dataIn  = in  Bits(8 bits)
		val dataOut = out Bits(8 bits)

		val address   = out UInt(16 bits)
		val busEnable = out Bool
		val io        = out Bool
		val code      = out Bool
		val write     = out Bool
		val intActive = out Bool
	}

	val stage = Reg(UInt(2 bits)) init(1) simPublic()
	stage := stage + 1

	val pc = Reg(UInt(16 bits)) init(0xFFFF) simPublic()
	private val pcPlusOne = pc + 1

	private val intActive = Reg(Bool) init(False)

	io.busEnable := False
	io.write := False
	io.io := False
	io.code := False
	io.intActive := intActive
	io.address := 0
	io.dataOut := 0

	/*
	 * Decode/read registers stage
	 */

	private val decodeArea = new Area {
		private val intEnable = Reg(Bool()) init(False)
		private val nmiActive = Reg(Bool()) init(False)
		private val intActive = Reg(Bool()) init(False)
		private val sysActive = Reg(Bool()) init(False)

		private val opcode = Reg(Bits(8 bits)) init(0)
		private val intReq = Reg(Bool()) init(False)

		val decoder = Decoder()
		decoder.io.opcode := opcode
		decoder.io.nmiReq := False
		decoder.io.intReq := intReq
		decoder.io.intEnable := intEnable
		decoder.io.nmiActive := nmiActive
		decoder.io.intActive := intActive
		decoder.io.sysActive := sysActive

		when (stage === 0) {
			opcode := io.dataIn
			intReq := io.irq
			intEnable := decoder.io.output.intEnable
			nmiActive := decoder.io.output.nmiActive
			intActive := decoder.io.output.intActive
			sysActive := decoder.io.output.sysActive
		}
		
		val readControl   = decoder.io.output.readStageControl
		val aluControl    = decoder.io.output.aluStageControl
		val memoryControl = decoder.io.output.memoryStageControl
		val pcControl     = decoder.io.output.pcControl
		val writeControl  = decoder.io.output.writeStageControl
	}

	private val registers = new Area() {
		val writeControl  = Reg(Vec(RegisterFileControl(),4))
		val writeData     = Reg(UInt(16 bits))
		val writeDataExg  = Reg(UInt(16 bits))
		val stackPointers = Vec(Reg(UInt(8 bits)) init(0xFF), 4)

		for (i <- 0 to 3) {
			val ctrl = writeControl(i).registerControl
			ctrl.write init(False)
			ctrl.push init(False)
			ctrl.pop init(False)
			ctrl.swap init(False)
			ctrl.mask init(WriteMask.none)
		}

		private val registers = new RegisterFile()
		private val io = registers.io

		registers.io.readRegisters := decodeArea.readControl.registers

		registers.io.registerControl <> writeControl
		registers.io.dataIn          <> writeData
		registers.io.dataInExg       <> writeDataExg
		registers.io.pointers        <> stackPointers

		private val modifiers = Array.fill(2)(OperandModifier())
		val readValues = Vec(UInt(16 bits), 2)

		for (index <- 0 to 1) {
			modifiers(index).io.operand <> registers.io.dataOut(index)
			modifiers(index).io.part <> decodeArea.readControl.part(index)
			readValues(index) := modifiers(index).io.dataOut
		}
	}


	/*
	 * Memory, configuration registers, I/O stage
	 */

	private val memoryArea = new Area {
		val writeControl = Reg(WriteBackControl())
		val result = Reg(Bits(8 bits)) init(0)

		writeControl.intActive init(False)
		for (i <- 0 to 3) {
			val control = writeControl.fileControl(0).registerControl
			control.write init(False)
			control.push  init(False)
			control.pop   init(False)
			control.swap  init(False)
			control.mask  init(WriteMask.none)
		}

		private def selectSource(source: ValueSource.C): UInt =
			source.mux(
				ValueSource.register1 -> registers.readValues(0),
				ValueSource.register2 -> registers.readValues(1),
				ValueSource.pc -> pcPlusOne
			)

		private val memAddress = selectSource(decodeArea.memoryControl.address)
		private val memData    = selectSource(decodeArea.memoryControl.data)

		private val configRegister = memAddress(15 downto 8)
		private val isStackPointerRegister = configRegister === M"000000--"
		private val stackPointer = registers.stackPointers(configRegister(1 downto 0))

		when (stage === 2) {
			when (decodeArea.memoryControl.config && isStackPointerRegister) {
				result := stackPointer.asBits
			} otherwise {
				result := io.dataIn
			}
		}

		private def handleConfigRegisterOperation(): Unit = {
			when (isStackPointerRegister) {
				// Stack pointer operation
				when (decodeArea.memoryControl.write) {
					stackPointer := memData(15 downto 8)
				}
			}
		}

		private def handleMemoryAndIoOperation(): Unit = {
			io.address := memAddress
			io.dataOut := memData(15 downto 8).asBits
			io.busEnable := decodeArea.memoryControl.enable
			io.write := decodeArea.memoryControl.write
			io.io := decodeArea.memoryControl.io
			io.code := decodeArea.memoryControl.code
		}

		when (stage === 1) {
			writeControl := decodeArea.writeControl

			for (i <- 0 to 3) {
				val sp = registers.stackPointers(i)
				when (decodeArea.writeControl.fileControl(i).registerControl.push) {
					sp := sp - 1
				} elsewhen (decodeArea.writeControl.fileControl(i).registerControl.pop) {
					sp := sp + 1
				}
			}

			when (decodeArea.memoryControl.enable) {
				when (decodeArea.memoryControl.config) {
					handleConfigRegisterOperation()
				} otherwise {
					handleMemoryAndIoOperation()
				}
			}
		}
	}

	/*
	 * ALU stage
	 */

	private val aluArea = new Area {
		private val memoryIn = RegNextWhen(io.dataIn, stage === 2)

		private val alu = AluStage()
		alu.io.registers := registers.readValues
		alu.io.pc        := pcPlusOne
		alu.io.memory    := memoryIn.asUInt
		alu.io.control   := decodeArea.aluControl

		val result       = alu.io.dataOut
		val conditionMet = alu.io.conditionMet
		val highByteZero = alu.io.highByteZero
		val operands     = alu.io.operands

		private val pcCalc = PcCalc()
		pcCalc.io.pc           := pcPlusOne
		pcCalc.io.operands     := registers.readValues
		pcCalc.io.memory       := memoryIn
		pcCalc.io.conditionMet := conditionMet
		pcCalc.io.resultZero   := highByteZero
		pcCalc.io.control      := decodeArea.pcControl

		val pcOut = pcCalc.io.nextPc
	}

	/*
	 * Register writeback/fetch instruction stage
	 */

	val stage3 = new Area {
		val control = memoryArea.writeControl

		when (stage === 3) {
			registers.writeControl := control.fileControl
			registers.writeData := control.source.mux (
				WriteSource.alu -> aluArea.result,
				WriteSource.memory -> (memoryArea.result << 8).asUInt
			)
			registers.writeDataExg := registers.readValues(1) // T or FT

			pc := aluArea.pcOut

			io.address := aluArea.pcOut
			io.busEnable := True
			io.write := False
			io.io := False
			io.code := True
			io.intActive := control.intActive

			intActive := control.intActive
		}.otherwise {
			for (i <- 0 to 3) {
				val ctrl = registers.writeControl(i).registerControl
				ctrl.write := False
				ctrl.push  := False
				ctrl.pop   := False
				ctrl.swap  := False
				ctrl.mask  := WriteMask.none
			}
		}
	}
}


//Generate the MyTopLevel's Verilog
object RC811TopLevel {
	def main(args: Array[String]) {
		SpinalVerilog(new RC811()).printPruned()
	}
}
