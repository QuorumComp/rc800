package rc800.registers

import spinal.core._
import spinal.lib._


class Stack extends Component {
	val io = new Bundle {
		val operation = in (RegisterOperation())
		val writeData = in UInt(16 bits)
		val writeMask = in Bits(16 bits)

		val pointer   = out UInt(8 bits)
		val dataOut   = out UInt(16 bits)
	}

	val pointer = Reg(UInt(8 bits)) init(255)
	io.pointer := pointer

	val top = Reg(UInt(16 bits))
	val nextTop = Reg(UInt(16 bits))
	io.dataOut := top

	val memory = Mem(UInt(16 bits), 256)

	val memReadTopAddress = Reg(UInt(8 bits))
	val memReadTopEnable = RegInit(False)
	val memReadNextTopAddress = Reg(UInt(8 bits))
	val memReadNextTopEnable = RegInit(False)

	val readAddress = memReadTopEnable ? memReadTopAddress | memReadNextTopAddress
	val readValue = memory.readSync(readAddress)
	when (RegNext(memReadTopEnable, False)) {
		top := readValue
		memReadTopEnable := False
	} elsewhen (RegNext(memReadNextTopEnable, False)) {
		nextTop := readValue
		memReadNextTopEnable := False
	}

	val memWriteAddress = Reg(UInt(8 bits))
	val memWriteData = Reg(UInt(16 bits))
	val memWriteEnable = RegInit(False)
	val memWriteNextAddress = Reg(UInt(8 bits))
	val memWriteNextData = Reg(UInt(16 bits))
	val memWriteNextEnable = RegInit(False)

	memory.write(memWriteAddress, memWriteData, memWriteEnable)

	memWriteAddress := memWriteNextAddress
	memWriteData := memWriteNextData
	memWriteEnable := memWriteNextEnable

	memWriteNextEnable := False

	val pushPointer = pointer - 1

	switch (io.operation) {
		is (RegisterOperation.write) {
			val newTop = (io.writeData & io.writeMask.asUInt) | (top & ~io.writeMask.asUInt)
			top := newTop
			memWriteAddress := pointer
			memWriteData := newTop
			memWriteEnable := True
		}
		is (RegisterOperation.push) {
			memWriteAddress := pushPointer
			memWriteData := top
			memWriteEnable := True
			nextTop := top
			pointer := pushPointer
		}
		is (RegisterOperation.swap) {
			top := nextTop
			nextTop := top

			memWriteAddress := pointer
			memWriteData := nextTop
			memWriteEnable := True
			memWriteNextAddress := pointer + 1
			memWriteNextData := top
			memWriteNextEnable := True
		}
		is (RegisterOperation.pushValue) {
			memWriteAddress := pushPointer
			memWriteData := io.writeData
			memWriteEnable := True
			nextTop := top
			top := io.writeData
			pointer := pushPointer
		}
		is (RegisterOperation.pop) {
			val nextPointer = pointer + 1
			val nextTopPointer = pointer + 2
			top := nextTop
			memReadNextTopAddress := nextTopPointer
			memReadNextTopEnable := True
			pointer := nextPointer
		}
		is (RegisterOperation.writePointer) {
			val newPointer = io.writeData(7 downto 0)
			memReadTopAddress := newPointer
			memReadTopEnable := True
			memReadNextTopAddress := newPointer + 1
			memReadNextTopEnable := True
			pointer := newPointer
		}
	}

}


