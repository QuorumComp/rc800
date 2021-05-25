package rc800.registers

import spinal.core._
import spinal.lib._

import rc800.control.component.RegisterControl


object WriteMask extends SpinalEnum(defaultEncoding = binarySequential) {
	val none, low, high, full = newElement()
}


case class Register() extends Component {
	val io = new Bundle {
		val dataIn  = in UInt(16 bits)
		val control = in (RegisterControl())
		val pointer = in UInt(8 bits)

		val dataOut = out UInt(16 bits)
	}

	val memory = Mem(UInt(16 bits), 256)

	val top = Reg(UInt(16 bits))
	val top1 = Reg(UInt(16 bits))
	val popTop2 = memory.readSync(io.pointer + 1)

	io.dataOut := top

	val memWriteAddress = Reg(UInt(8 bits))
	val memWriteData = Reg(UInt(16 bits))
	val memWriteEnable = RegInit(False)
	memWriteEnable := False

	memory.write(memWriteAddress, memWriteData, memWriteEnable)

	when (io.control.write) {
		when (io.control.mask.asBits(1)) {
			top(15 downto 8) := io.dataIn(15 downto 8)
		}
		when (io.control.mask.asBits(0)) {
			top(7 downto 0) := io.dataIn(7 downto 0)
		}
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
