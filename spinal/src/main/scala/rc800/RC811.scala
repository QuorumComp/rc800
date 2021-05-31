package rc800

import spinal.core._
import spinal.lib._
import spinal.core.sim._

import alu.AluOperation
import alu.AluStage
import alu.Condition
import alu.OperandSource
import alu.PcCalc

import control.MemoryStageControl
import control.MemoryStageAddressSource
import control.RC811Control
import control.WriteBackStageControl
import control.WriteBackValueSource

import control.component.RegisterControl
import control.component.RegisterFileControl

import decoder.Decoder

import registers.OperandPartSelector
import registers.Register
import registers.RegisterFile
import registers.WriteMask


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
		val int       = out Bool
	}

	val stage = Reg(UInt(2 bits)) init(1) simPublic()
	stage := stage + 1

	val pc = Reg(UInt(16 bits)) init(0xFFFF) simPublic()
	private val pcPlusOne = pc + 1

	private val intPin = Reg(Bool) init(False)

	io.busEnable := False
	io.write := False
	io.io := False
	io.code := False
	io.int := intPin
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

		private val strobe = Bool(false)
		private val opcode = io.dataIn
		private val intReq = Reg(Bool()) init(False)

		val decoderUnit = new Decoder()
		decoderUnit.io.opcodeAsync := opcode
		decoderUnit.io.strobe := strobe
		decoderUnit.io.nmiReq := False
		decoderUnit.io.intReq := intReq
		decoderUnit.io.intEnable := intEnable
		decoderUnit.io.nmiActive := nmiActive
		decoderUnit.io.intActive := intActive
		decoderUnit.io.sysActive := sysActive

		when (stage === 0) {
			strobe := True
			intReq := io.irq
			intEnable := decoderUnit.io.output.intEnable
			nmiActive := decoderUnit.io.output.nmiActive
			intActive := decoderUnit.io.output.intActive
			sysActive := decoderUnit.io.output.sysActive
		}
		
		val readControl   = decoderUnit.io.output.stageControl.readStageControl
		val aluControl    = decoderUnit.io.output.stageControl.aluStageControl
		val memoryControl = decoderUnit.io.output.stageControl.memoryStageControl
		val writeControl  = decoderUnit.io.output.stageControl.writeStageControl
		val anyIntActive  = decoderUnit.io.output.anyActive
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

		private val modifiers = Array.fill(2)(OperandPartSelector())
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
		val writeControl = Reg(WriteBackStageControl())
		val result = Reg(Bits(8 bits)) init(0)

		for (i <- 0 to 3) {
			val control = writeControl.fileControl(0).registerControl
			control.write init(False)
			control.push  init(False)
			control.pop   init(False)
			control.swap  init(False)
			control.mask  init(WriteMask.none)
		}

		private def selectSourceAddress(source: MemoryStageAddressSource.C): UInt =
			source.mux(
				MemoryStageAddressSource.register1 -> registers.readValues(0),
				MemoryStageAddressSource.pc -> pcPlusOne
			)

		private val memAddress = selectSourceAddress(decodeArea.memoryControl.address)
		private val memData    = registers.readValues(1)	// always operand 2

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

		private def handlePushPop() {
			for (i <- 0 to 3) {
				val sp = registers.stackPointers(i)
				when (decodeArea.writeControl.fileControl(i).registerControl.push) {
					sp := sp - 1
				} elsewhen (decodeArea.writeControl.fileControl(i).registerControl.pop) {
					sp := sp + 1
				}
			}
		}

		when (stage === 1) {
			writeControl := decodeArea.writeControl

			handlePushPop()

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
		alu.io.memory    := memoryIn
		alu.io.control   := decodeArea.aluControl

		val result = alu.io.dataOut
		val pcOut = alu.io.nextPc
	}

	/*
	 * Register writeback/fetch instruction stage
	 */

	val stage3 = new Area {
		private val control = memoryArea.writeControl

		private def fetchInstruction() {
			io.address := aluArea.pcOut
			io.busEnable := True
			io.write := False
			io.io := False
			io.code := True
			io.int := decodeArea.anyIntActive

			intPin := decodeArea.anyIntActive
		}

		when (stage === 3) {
			registers.writeControl := control.fileControl
			registers.writeData := control.source.mux (
				WriteBackValueSource.alu -> aluArea.result,
				WriteBackValueSource.memory -> (memoryArea.result << 8).asUInt
			)
			registers.writeDataExg := registers.readValues(1) // T or FT

			pc := aluArea.pcOut

			fetchInstruction()
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
