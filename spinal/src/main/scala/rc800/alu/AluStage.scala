package rc800.alu

import spinal.core._
import spinal.lib._

import rc800.decoder.AluControl

case class AluStage() extends Component {
	val io = new Bundle {
		val registers = in Vec(UInt(16 bits), 2)
		val pc        = in UInt(16 bits)
		val memory    = in UInt(8 bits)
		val control   = in (AluControl())

		val dataOut      = out UInt(16 bits)
		val conditionMet = out Bool
		val highByteZero = out Bool
		val operands     = out Vec(UInt(16 bits), 2)
	}

	private val selectors = Array(OperandSelector(), OperandSelector())

	for (index <- 0 to 1) {
		selectors(index).io.selection := io.control.selection(index)
		selectors(index).io.register  := io.registers(index)
		selectors(index).io.pc        := io.pc
		selectors(index).io.memory    := io.memory

		io.operands(index) := selectors(index).io.dataOut
	}

	private val alu = new Alu()

	alu.io.operation      := io.control.operation
	alu.io.operand1       := selectors(0).io.dataOut
	alu.io.operand2       := selectors(1).io.dataOut
	alu.io.condition      := io.control.condition
	alu.io.shiftOperation := io.control.shiftOperation

	io.dataOut      := alu.io.dataOut
	io.conditionMet := alu.io.conditionMet
	io.highByteZero := alu.io.highByteZero
}
