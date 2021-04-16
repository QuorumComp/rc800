package rc800.stage

import spinal.core._
import spinal.lib._

import rc800.decoder.PcCondition
import rc800.decoder.PcControl
import rc800.decoder.PcTruePath

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

	private val pcOffset = ((io.control.truePath === PcTruePath.offsetFromMemory) ? offsetFromMemory | offsetFromDecoder)
	private val truePath = io.control.truePath.mux(
		PcTruePath.register1 -> io.operands(0),
		PcTruePath.register2 -> io.operands(1),
		PcTruePath.vectorFromDecoder -> (io.control.vector << 3).resize(16 bits),
		PcTruePath.vectorFromMemory -> (io.memory << 3).resize(16 bits).asUInt,
		default -> (io.pc + pcOffset)
	)
	private val falsePath = io.pc + 1

	private val takeTruePath = io.control.condition.mux (
		PcCondition.always -> True,
		PcCondition.whenConditionMet -> io.conditionMet,
		PcCondition.whenResultNotZero -> !io.resultZero,
	)

	io.nextPc := takeTruePath ? truePath | falsePath
}