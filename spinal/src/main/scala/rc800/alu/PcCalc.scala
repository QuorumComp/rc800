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
		val conditionMet = in Bool()
		val resultZero   = in Bool()
		val control      = in (PcControl())

		val nextPc = out UInt(16 bits)
	}

	private val takeTruePath = io.control.condition.mux (
		PcCondition.always -> True,
		PcCondition.whenConditionMet -> io.conditionMet,
		PcCondition.whenResultNotZero -> !io.resultZero,
	)

	private val offsetFromMemory = io.memory.asSInt.resize(16 bits).asUInt
	private val offsetFromDecoder = io.control.decodedOffset.resize(16 bits)
	private val truePathOffset = (io.control.truePath === PcTruePathSource.offsetFromMemory) ? offsetFromMemory | offsetFromDecoder
	private val offset = takeTruePath ? truePathOffset | 1

	io.nextPc := io.pc + offset

	when (takeTruePath) {
		switch (io.control.truePath) {
			is (PcTruePathSource.register2)         { io.nextPc := io.operand2 }
			is (PcTruePathSource.vectorFromDecoder) { io.nextPc := (io.control.vector << 3).resize(16 bits) }
			is (PcTruePathSource.vectorFromMemory)  { io.nextPc := (io.memory << 3).resize(16 bits).asUInt }
		}
	}
}