package rc800.alu

import spinal.core._
import spinal.lib._

import rc800.control.AluStageControl

import rc800.lpm

case class AluStage()(implicit lpmComponents: lpm.Components) extends Component {
	val io = new Bundle {
		val control = in (AluStageControl())

		val registers = in Vec(UInt(16 bits), 2)
		val pc        = in UInt(16 bits)
		val memory    = in Bits(8 bits)

		val dataOut      = out UInt(16 bits)
		val nextPc       = out (UInt(16 bits))
	}

	private val selectors = Array(OperandSelector(), OperandSelector())

	for (index <- 0 to 1) {
		selectors(index).io.selection := io.control.selection(index)
		selectors(index).io.register  := io.registers(index)
		selectors(index).io.pc        := io.pc
		selectors(index).io.memory    := io.memory.asUInt
	}
	
	private val alu = new Alu()

	alu.io.operand1 := selectors(0).io.dataOut
	alu.io.operand2 := selectors(1).io.dataOut
	alu.io.control  := io.control.aluControl

	private val pcCalc = PcCalc()
	pcCalc.io.control      := io.control.pcControl
	pcCalc.io.pc           := io.pc
	pcCalc.io.operand2     := selectors(1).io.dataOut
	pcCalc.io.memory       := io.memory
	pcCalc.io.conditionMet := alu.io.conditionMet
	pcCalc.io.resultZero   := alu.io.highByteZero

	io.dataOut := alu.io.dataOut
	io.nextPc  := pcCalc.io.nextPc
}
