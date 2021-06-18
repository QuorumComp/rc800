package rc800.alu

import spinal.core._
import spinal.lib._

import rc800.lpm

/*
 * While this shifter may seem needlessly complicated, the fact that it's only
 * using one dynamic shift implementation means the area needed is drastically
 * reduced.
 */

object ShiftOperation extends SpinalEnum(defaultEncoding = binarySequential) {
	val ls, rs, rsa, swap = newElement()	// must match AluOperation
}


case class Shifter(width: BitCount)(implicit lpmComponents: lpm.Components) extends Component {
	private val amountWidth = log2Up(width.value) bits

	val io = new Bundle {
		val operand   = in  UInt(width)
		val amount    = in  UInt(amountWidth)
		val operation = in  (ShiftOperation())
		val result    = out UInt(width)
	}

	private val rotater = lpmComponents.clShift(lpm.CLShift.ShiftType.rotate, width.value)

	rotater.io.data := io.operand.asBits
	rotater.io.distance := io.operation.mux(
		ShiftOperation.ls   -> (io.amount),
		ShiftOperation.swap -> U(8),
		default             -> ((-io.amount.asSInt).asUInt)	// right shift
	)

	private val fillBit = (io.operation === ShiftOperation.rsa) ? io.operand.msb | False 
	private val fillBits = B(width, default -> fillBit)
	private val mask = B(width, default -> True) |<< rotater.io.distance

	io.result := io.operation.mux (
		ShiftOperation.ls   -> (rotater.io.result & mask).asUInt,
		ShiftOperation.swap -> rotater.io.result.asUInt,
		default             -> ((rotater.io.result & ~mask) | (fillBits & mask)).asUInt	// right shift
	)
}
