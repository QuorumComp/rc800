package rc800.alu

import spinal.core._
import spinal.lib._


object OperandSource extends SpinalEnum {
	val zero, ones, register, pc, memory, signed_memory = newElement()
}


case class OperandSelector() extends Component {
	val io = new Bundle {
		val selection = in (OperandSource())
		val register  = in UInt(16 bits)
		val pc        = in UInt(16 bits)
		val memory    = in UInt(8 bits) 

		val dataOut   = out UInt(16 bits)
	}

	io.dataOut := io.selection.mux(
		OperandSource.zero          -> U(0, 16 bits),
		OperandSource.ones          -> U(0xFFFF, 16 bits),
		OperandSource.register      -> io.register,
		OperandSource.pc            -> io.pc,
		OperandSource.memory        -> (io.memory << 8),
		OperandSource.signed_memory -> io.memory.asSInt.resize(16 bits).asUInt
	)
}


