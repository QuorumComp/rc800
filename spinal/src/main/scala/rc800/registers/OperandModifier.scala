package rc800.registers

import spinal.core._
import spinal.lib._

import rc800.decoder.OperandPart

case class OperandModifier() extends Component {
	val io = new Bundle {
		val operand = in  UInt(16 bits)
		val part    = in  (OperandPart())

		val dataOut = out UInt(16 bits)
	}

	io.dataOut := io.part.mux(
		OperandPart.full   -> io.operand,
		OperandPart.low    -> (io.operand(7 downto 0) << 8),
		OperandPart.high   -> (io.operand(15 downto 8) << 8)
	)
}