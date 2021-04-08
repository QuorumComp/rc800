package rc800.alu

import spinal.core._
import spinal.lib._


object OperandSelection extends SpinalEnum {
	val zero, ones, register, pc, memory, signed_memory = newElement()
}

case class OperandSelector() extends Component {
	val io = new Bundle {
		val selection = in (OperandSelection())
		val register  = in UInt(16 bits)
		val pc        = in UInt(16 bits)
		val memory    = in UInt(8 bits) 

		val dataOut   = out UInt(16 bits)
	}

	io.dataOut := io.selection.mux(
		OperandSelection.zero          -> U(0, 16 bits),
		OperandSelection.ones          -> U(0xFFFF, 16 bits),
		OperandSelection.register      -> io.register,
		OperandSelection.pc            -> io.pc,
		OperandSelection.memory        -> (io.memory << 8),
		OperandSelection.signed_memory -> io.memory.asSInt.resize(16 bits).asUInt
	)
}


