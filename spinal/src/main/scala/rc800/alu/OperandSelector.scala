package rc800.alu

import spinal.core._
import spinal.lib._


object OperandSource extends SpinalEnum(defaultEncoding = binarySequential) {
	val zero, ones, register, pc, memory, signed_memory = newElement()
}


case class OperandSelector() extends Component {
	val io = new Bundle {
		val selection = in (OperandSource())
		val register  = in Bits(16 bits)
		val pc        = in UInt(16 bits)
		val memory    = in Bits(8 bits) 

		val dataOut   = out Bits(16 bits)
	}

	io.dataOut := io.selection.mux(
		OperandSource.zero          -> B(0, 16 bits),
		OperandSource.ones          -> B(0xFFFF, 16 bits),
		OperandSource.register      -> io.register,
		OperandSource.pc            -> io.pc.asBits,
		OperandSource.memory        -> (io.memory << 8),
		OperandSource.signed_memory -> io.memory.asSInt.resize(16 bits).asBits
	)
}


