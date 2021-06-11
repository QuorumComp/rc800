package rc800.alu

import spinal.core._
import spinal.lib._

/*
 * While this shifter may seem needlessly complicated, the fact that it's only
 * using one dynamic shift implementation means the area needed is drastically
 * reduced.
 */

class lpm_clshift(shiftType: String, width: Int) extends BlackBox {
    addGeneric("LPM_SHIFTTYPE", shiftType)
    addGeneric("LPM_WIDTH", width)
	addGeneric("LPM_WIDTHDIST", log2Up(width))
	addGeneric("LPM_TYPE", "LPM_CLSHIFT")

	val io = new Bundle {
		val data = in Bits(width bits)
		val distance = in UInt(log2Up(width) bits)

		val result = out Bits(width bits)
	}

	noIoPrefix()
}


object ShiftOperation extends SpinalEnum(defaultEncoding = binarySequential) {
	val ls, rs, rsa, swap = newElement()
}


case class Shifter(width: BitCount) extends Component {
	private val amountWidth = log2Up(width.value) bits

	val io = new Bundle {
		val operand   = in  UInt(width)
		val amount    = in  UInt(amountWidth)
		val operation = in  (ShiftOperation())
		val result    = out UInt(width)
	}

	private val rotater = new lpm_clshift("ROTATE", width.value)
	private val mask = B(width, default -> True) |<< io.amount

	rotater.io.data := io.operand.asBits
	rotater.io.distance := io.operation.mux(
		ShiftOperation.ls   -> (io.amount),
		ShiftOperation.swap -> U(8),
		default             -> (~io.amount + 1)	// right shift
	)

	private val fillBit = (io.operation === ShiftOperation.rsa) ? io.operand.msb | False 
	private val fillBits = B(width, default -> fillBit)

	io.result := io.operation.mux (
		ShiftOperation.ls   -> (rotater.io.result & mask).asUInt,
		ShiftOperation.swap -> rotater.io.result.asUInt,
		default             -> ((rotater.io.result & ~mask) | (fillBits & mask)).asUInt	// right shift
	)
}
