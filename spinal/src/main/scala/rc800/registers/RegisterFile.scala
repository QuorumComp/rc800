package rc800.registers

import spinal.core._
import spinal.lib._

import rc800.decoder.RegisterFileControl
import rc800.decoder.RegisterName


class RegisterFile extends Component {
	val io = new Bundle {
		val readRegisters = in Vec(RegisterName(), 2)
		val dataOut       = out Vec(UInt(16 bits), 2)

		val pointers        = in Vec(UInt(8 bits), 4)
		val registerControl = in Vec(RegisterFileControl(), 4)
		val dataIn          = in UInt(16 bits)
		val dataInExg       = in UInt(16 bits)
	}

	val registers = Array.fill(4)(Register())

	def wireStack(register: Register, control: RegisterFileControl, pointer: UInt): Unit = {
		register.io.control := control.registerControl
		register.io.pointer := pointer

		val dataIn = control.sourceExg ? io.dataInExg | io.dataIn
		register.io.dataIn := control.rot8 ? dataIn.rotateLeft(8) | dataIn
	}

	for (i <- 0 to 3) {
		wireStack(registers(i), io.registerControl(i), io.pointers(i))
	}

	def registerDataOut(r: RegisterName.C) =
		r.mux(
			RegisterName.ft -> registers(0).io.dataOut,
			RegisterName.bc -> registers(1).io.dataOut,
			RegisterName.de -> registers(2).io.dataOut,
			RegisterName.hl -> registers(3).io.dataOut,
		)

	for (i <- 0 to 1) {
		io.dataOut(i) := registerDataOut(io.readRegisters(i))
	}
}
