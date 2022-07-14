package rc800.registers

import spinal.core._
import spinal.lib._

import rc800.control.component.RegisterControl


case class RegisterStack() extends Component {
	val io = new Bundle {
		val dataIn  = in (Bits(16 bits))
		val dataOut = in (Bits(16 bits))
		val pointer = in (UInt(8 bits))
		val write   = in (Bool())
	}

	val memory = Mem(Bits(16 bits), 256)
	io.dataOut := memory.readSync(io.pointer)
	memory.write(io.pointer, io.dataIn, io.write)
}
