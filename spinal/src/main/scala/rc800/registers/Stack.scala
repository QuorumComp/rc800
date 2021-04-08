package rc800.registers

import spinal.core._
import spinal.lib._


object RegisterOperation extends SpinalEnum(defaultEncoding = binarySequential) {
	// organize write and push so write is enabled when "1-1" and push when "11-"
	val read, pop, swap, _dummy1,
		_dummy2, write, push, pushValue = newElement()
}

class Stack extends Component {
	val io = new Bundle {
		val operation = in (RegisterOperation())
		val writeData = in UInt(16 bits)
		val writeMask = in Bits(2 bits)

		val pointer   = in UInt(8 bits)
		val dataOut   = out UInt(16 bits)
	}

	val memory = Mem(UInt(16 bits), 256)

	val top = Reg(UInt(16 bits))
	val top1 = Reg(UInt(16 bits))
	val popTop2 = memory.readSync(io.pointer + 1)
	val pick = memory.readSync(io.pointer + top(7 downto 0))

	io.dataOut := top

	val memWriteAddress = Reg(UInt(8 bits))
	val memWriteData = Reg(UInt(16 bits))
	val memWriteEnable = RegInit(False)
	memWriteEnable := False

	memory.write(memWriteAddress, memWriteData, memWriteEnable)

	val doWrite = (io.operation === RegisterOperation.write || io.operation === RegisterOperation.pushValue)
	val doPush = (io.operation === RegisterOperation.push || io.operation === RegisterOperation.pushValue)
	val doPop = (io.operation === RegisterOperation.pop)
	val doSwap = (io.operation === RegisterOperation.swap)

	when (doWrite) {
		when (io.writeMask(1)) {
			top(15 downto 8) := io.writeData(15 downto 8)
		}
		when (io.writeMask(0)) {
			top(7 downto 0) := io.writeData(7 downto 0)
		}
	}

	when (doPush) {
		top1 := top

		memWriteAddress := io.pointer + 2
		memWriteData := top1
		memWriteEnable := True
	}

	when (doPop) {
		top := top1
		top1 := popTop2
	}

	when (doSwap) {
		top := top1
		top1 := top
	}
}
