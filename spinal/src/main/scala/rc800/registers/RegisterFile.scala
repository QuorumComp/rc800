package rc800.registers

import spinal.core._
import spinal.lib._

import rc800.control.component._


class RegisterFile extends Component {
	val io = new Bundle {
		val readRegisters = in  (Vec(RegisterName(), 2))
		val dataOut       = out (Vec(Bits(16 bits), 2))

		val pointers  = in (Vec(UInt(8 bits), 4))
		val control   = in (RegisterFileControl())
		val dataIn    = in (Bits(16 bits))
		val dataInExg = in (Bits(16 bits))
	}

	private val nopControl = RegisterControl()
	nopControl.write := False
	nopControl.push  := False
	nopControl.pop   := False
	nopControl.swap  := False

	val registers = Array.fill(8)(Register())

	def wireStack(nameHi: RegisterName.C, nameLo: RegisterName.C, index: RegisterName.E): Unit = {
		val pointer = io.pointers(index.asBits(1 downto 0).asUInt)
		val control = io.control.registerControl(index.position)
		val registerHi = registers(index.position)
		val registerLo = registers(index.position)

		registerHi.io.control := (nameHi === io.control.writeRegister || index() === io.control.writeRegister) ? control | nopControl
		registerHi.io.pointer := pointer
		registerHi.io.dataIn := (nameHi === io.control.writeRegister) ? io.dataIn(15 downto 8) | io.dataIn

		registerLo.io.control := (nameLo === io.control.writeRegister || index() === io.control.writeRegister) ? control | nopControl
		registerLo.io.pointer := pointer
		registerLo.io.dataIn := (nameLo === io.control.writeRegister) ? io.dataIn(15 downto 8) | io.dataIn
	}

	wireStack(RegisterName.f, RegisterName.t, RegisterName.ft)
	wireStack(RegisterName.b, RegisterName.c, RegisterName.bc)
	wireStack(RegisterName.d, RegisterName.e, RegisterName.de)
	wireStack(RegisterName.h, RegisterName.l, RegisterName.hl)

	def registerDataOut(r: RegisterName.C) =
		r.mux(
			RegisterName.f -> (registers(0).io.dataOut ## B(0, 8 bits)),
			RegisterName.t -> (registers(1).io.dataOut ## B(0, 8 bits)),
			RegisterName.b -> (registers(2).io.dataOut ## B(0, 8 bits)),
			RegisterName.c -> (registers(3).io.dataOut ## B(0, 8 bits)),
			RegisterName.d -> (registers(4).io.dataOut ## B(0, 8 bits)),
			RegisterName.e -> (registers(5).io.dataOut ## B(0, 8 bits)),
			RegisterName.h -> (registers(6).io.dataOut ## B(0, 8 bits)),
			RegisterName.l -> (registers(7).io.dataOut ## B(0, 8 bits)),
			RegisterName.ft -> (registers(0).io.dataOut ## registers(1).io.dataOut),
			RegisterName.bc -> (registers(2).io.dataOut ## registers(3).io.dataOut),
			RegisterName.de -> (registers(4).io.dataOut ## registers(5).io.dataOut),
			RegisterName.hl -> (registers(6).io.dataOut ## registers(7).io.dataOut),
		)

	for (i <- 0 to 1) {
		io.dataOut(i) := registerDataOut(io.readRegisters(i))
	}
}
