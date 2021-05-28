package rc800.alu

import spinal.core._

import rc800.control.component.PcControl


object PcTruePathSource extends SpinalEnum {
	val offsetFromMemory,
		offsetFromDecoder,
		register2,
		vectorFromMemory,
		vectorFromDecoder = newElement()
}


object PcCondition extends SpinalEnum {
	val always,
		whenConditionMet,
		whenResultNotZero = newElement()
}


case class PcCalc() extends Component {
	val io = new Bundle {
		val pc           = in UInt(16 bits)
		val operands     = in Vec(UInt(16 bits), 2)
		val memory       = in Bits(8 bits)
		val conditionMet = in Bool
		val resultZero   = in Bool
		val control      = in (PcControl())

		val nextPc = out UInt(16 bits)
	}

	private val offsetFromMemory = io.memory.asSInt.resize(16 bits).asUInt
	private val offsetFromDecoder = io.control.decodedOffset.resize(16 bits)

	private val truePath = io.control.truePath.mux(
		PcTruePathSource.register2 -> io.operands(1),
		PcTruePathSource.vectorFromDecoder -> (io.control.vector << 3).resize(16 bits),
		PcTruePathSource.vectorFromMemory -> (io.memory << 3).resize(16 bits).asUInt,
		PcTruePathSource.offsetFromMemory -> (io.pc + offsetFromMemory),
		PcTruePathSource.offsetFromDecoder -> (io.pc + offsetFromDecoder)
	)
	private val falsePath = io.pc + 1

	private val takeTruePath = io.control.condition.mux (
		PcCondition.always -> True,
		PcCondition.whenConditionMet -> io.conditionMet,
		PcCondition.whenResultNotZero -> !io.resultZero,
	)

	io.nextPc := takeTruePath ? truePath | falsePath
}