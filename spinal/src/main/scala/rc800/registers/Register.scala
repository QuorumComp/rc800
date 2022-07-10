package rc800.registers

import spinal.core._
import spinal.lib._

import rc800.control.component.RegisterControl


case class Register() extends Component {
	val io = new Bundle {
		val dataIn  = in Bits(8 bits)
		val control = in (RegisterControl())
		val pointer = in UInt(8 bits)

		val dataOut = out Bits(8 bits)
	}

	val memory = Mem(Bits(8 bits), 256)

	val top = Reg(Bits(8 bits))
	val top1 = Reg(Bits(8 bits))
	val popTop2 = memory.readSync(io.pointer + 1)

	io.dataOut := top

	val memWriteAddress = Reg(UInt(8 bits))
	val memWriteData = Reg(Bits(8 bits))
	val memWriteEnable = RegInit(False)
	memWriteEnable := False

	memory.write(memWriteAddress, memWriteData, memWriteEnable)

	when (io.control.write) {
		top := io.dataIn
	}

	when (io.control.push) {
		top1 := top

		memWriteAddress := io.pointer + 2
		memWriteData := top1
		memWriteEnable := True
	}

	when (io.control.pop) {
		top := top1
		top1 := popTop2
	}

	when (io.control.swap) {
		top := top1
		top1 := top
	}
}
