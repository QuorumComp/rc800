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
		// Second write port's operand bus (writeDataAux): the value an exchange
		// writes to both of its registers, or the PICK/STOW immediate index in its
		// high byte lane.  A load that steps its pointer ignores this bus and takes
		// the memory bus below instead.
		val dataInAux = in (Bits(16 bits))
		// A memory read result: the raw byte, which this component puts in the high
		// byte lane like every other 8-bit value. The auxiliary port takes this
		// instead of the operand bus when a load steps its pointer, since that
		// instruction writes two registers.
		val dataInMemory = in (Bits(8 bits))

		val memReadControl = in (Vec(RegisterControl(), 4))
		val stackReload = in (Vec(Bool(), 4))
	}

	val registers = Array.fill(8)(Register())

	// PICK and STOW take their stack index from the immediate operand
	// byte, which rides the auxiliary data bus (dataInAux) in its high
	// byte lane - the same lane 8-bit writes use.
	private val pickIndex = io.dataInAux(15 downto 8).asUInt

	def wireStack(nameHi: RegisterName.E, nameLo: RegisterName.E, name: RegisterName.E): Unit = {
		val registerIndex = name.asBits(1 downto 0).asUInt
		val pointer = io.pointers(registerIndex)
		val control = io.control.registerControl(registerIndex)
		val is16bit = RegisterName.is16bit(io.control.writeRegister)
		// Each port takes its byte lane from the register it is itself writing,
		// not from the width of the other port's destination: an 8-bit value then
		// rides the high lane on either bus, which is the rule everywhere else in
		// the machine.  The two widths only ever differ on a load that steps its
		// pointer - a pair on the main port, a byte on the auxiliary one - and an
		// exchange always writes the same width on both.
		val is16bitAux = RegisterName.is16bit(io.control.writeAuxRegister)
		// The auxiliary port's source, selected by the same control word that
		// carries its destination and enable: the operand bus for an exchange and
		// for the PICK/STOW index, the memory result for a load that steps its
		// pointer.  The memory result arrives as a bare byte, so it is put in the
		// high byte lane here exactly as the operand bus already holds one, and the
		// lane rule above then covers either source.
		val dataAux = io.control.auxFromMemory ? (io.dataInMemory ## B(0, 8 bits)) | io.dataInAux

		val registerHi = registers(nameHi.position)
		val registerLo = registers(nameLo.position)

		val writeHi = io.control.write && (nameHi === io.control.writeRegister || name() === io.control.writeRegister)
		val writeAuxHi = io.control.writeAux && (nameHi === io.control.writeAuxRegister || name() === io.control.writeAuxRegister)

		registerHi.io.control := control
		registerHi.io.pointer := pointer
		registerHi.io.pickStow := pickIndex
		registerHi.io.memReadControl := io.memReadControl(registerIndex)
		registerHi.io.stackReload := io.stackReload(registerIndex)
		registerHi.io.dataIn := io.dataIn(15 downto 8)
		registerHi.io.dataInAux := dataAux(15 downto 8)
		registerHi.io.write := writeHi
		registerHi.io.writeAux := writeAuxHi

		val writeLo = io.control.write && (nameLo === io.control.writeRegister || name() === io.control.writeRegister)
		val writeAuxLo = io.control.writeAux && (nameLo === io.control.writeAuxRegister || name() === io.control.writeAuxRegister)

		registerLo.io.control := control
		registerLo.io.pointer := pointer
		registerLo.io.pickStow := pickIndex
		registerLo.io.memReadControl := io.memReadControl(registerIndex)
		registerLo.io.stackReload := io.stackReload(registerIndex)
		registerLo.io.dataIn := is16bit ? io.dataIn(7 downto 0) | io.dataIn(15 downto 8)
		registerLo.io.dataInAux := is16bitAux ? dataAux(7 downto 0) | dataAux(15 downto 8)
		registerLo.io.write := writeLo
		registerLo.io.writeAux := writeAuxLo
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
