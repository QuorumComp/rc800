package rc800.alu

import spinal.core._

import rc800.control.component.PcControl


object PcTruePathSource extends SpinalEnum(defaultEncoding = binarySequential) {
	val offsetFromMemory,
		offsetFromDecoder,
		register2,
		vectorFromMemory,
		vectorFromDecoder = newElement()
}


object PcCondition extends SpinalEnum(defaultEncoding = binarySequential) {
	val always,
		whenConditionMet,
		whenResultNotZero = newElement()
}


case class PcCalc() extends Component {
	val io = new Bundle {
		val pc           = in UInt(16 bits)
		val operand2     = in UInt(16 bits)
		val memory       = in Bits(8 bits)
		val conditionMet = in Bool
		val resultZero   = in Bool
		val control      = in (PcControl())

		val nextPc = out UInt(16 bits)
	}

	private val offsetFromMemory = io.memory.asSInt.resize(16 bits).asUInt
	private val offsetFromDecoder = io.control.decodedOffset.resize(16 bits)
	private val offset = (io.control.truePath === PcTruePathSource.offsetFromMemory) ? offsetFromMemory | offsetFromDecoder

	private val truePath = io.control.truePath.mux(
		PcTruePathSource.register2 -> io.operand2,
		PcTruePathSource.vectorFromDecoder -> (io.control.vector << 3).resize(16 bits),
		PcTruePathSource.vectorFromMemory -> (io.memory << 3).resize(16 bits).asUInt,
		// offsetFromMemory, offsetFromDecoder
		default -> (io.pc + offset)
	)
	private val falsePath = io.pc + 1

	private val takeTruePath = io.control.condition.mux (
		PcCondition.always -> True,
		PcCondition.whenConditionMet -> io.conditionMet,
		PcCondition.whenResultNotZero -> !io.resultZero,
	)

	io.nextPc := takeTruePath ? truePath | falsePath
}