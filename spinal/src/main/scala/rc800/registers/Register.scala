package rc800.registers

import spinal.core._
import spinal.lib._

import rc800.control.component.RegisterControl


case class Register() extends Component {
	val io = new Bundle {
		val dataIn    = in (Bits(8 bits))
		val dataInExg = in (Bits(8 bits))
		val control   = in (RegisterControl())
		val write     = in (Bool())
		val writeExg  = in (Bool())

		val pick      = in (UInt(8 bits))
		val pointer   = in (UInt(8 bits))

		val dataOut   = out (Bits(8 bits))
	}

	val memory = Mem(Bits(8 bits), 256)

	val top = Reg(Bits(8 bits))
	val top1 = Reg(Bits(8 bits))
	val popTop2 = memory.readSync(io.pointer + (io.control.pick ? io.pick | 1))

	io.dataOut := top

	val memWriteAddress = Reg(UInt(8 bits))
	val memWriteData = Reg(Bits(8 bits))
	val memWriteEnable = RegInit(False)
	memWriteEnable := False

	memory.write(memWriteAddress, memWriteData, memWriteEnable)

	when (io.write) {
		top := io.dataIn
	} elsewhen (io.writeExg) {
		top := io.dataInExg
	}

	when (io.control.push) {
		top1 := top

		memWriteAddress := io.pointer + 2
		memWriteData := top1
		memWriteEnable := True
	} elsewhen (io.control.pop) {
		top := top1
		top1 := popTop2
	} elsewhen (io.control.swap) {
		top := top1
		top1 := top
	} elsewhen (io.control.pick) {
		top := io.pick.mux(
			0 -> top,
			1 -> top1,
			default -> popTop2)
	}

}
