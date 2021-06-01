package rc800.alu

import spinal.core._
import spinal.lib._

/*
 * While this shifter may seem needlessly complicated, the fact that it's only
 * using one dynamic shift implementation means the area needed is drastically
 * reduced.
 */

object ShiftOperation extends SpinalEnum(defaultEncoding = binarySequential) {
	val ls, rs, rsa, swap = newElement()
}


case class LeftRotater(width: BitCount) extends Component {
	val io = new Bundle {
		val operand = in  Bits(width)
		val amount  = in  UInt(log2Up(width.value) bits)
		val result  = out Bits(width)
		val mask    = out Bits(width)
	}

	io.result := io.operand.rotateLeft(io.amount)
	io.mask   := B(width, default -> True) |<< io.amount
}


case class Shifter(width: BitCount) extends Component {
	private val amountWidth = log2Up(width.value) bits

	val io = new Bundle {
		val operand   = in  UInt(width)
		val amount    = in  UInt(amountWidth)
		val operation = in  (ShiftOperation())
		val result    = out UInt(width)
	}

	private val rotater = new LeftRotater(width)

	rotater.io.operand := io.operand.asBits
	rotater.io.amount  := io.operation.mux(
		ShiftOperation.ls   -> (io.amount),
		ShiftOperation.swap -> U(8),
		default             -> (~io.amount + 1)	// right shift
	)

	private val fillBit = (io.operation === ShiftOperation.rsa) ? io.operand.msb | False 
	private val fillBits = B(width, default -> fillBit)

	io.result := io.operation.mux (
		ShiftOperation.ls   -> (rotater.io.result & rotater.io.mask).asUInt,
		ShiftOperation.swap -> rotater.io.result.asUInt,
		default             -> ((rotater.io.result & ~rotater.io.mask) | (fillBits & rotater.io.mask)).asUInt	// right shift
	)
}
