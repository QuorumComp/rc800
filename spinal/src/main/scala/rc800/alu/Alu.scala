package rc800.alu

import spinal.core._
import spinal.lib._

import rc800.control.component.AluControl

/*
 * This is a 16 bit ALU that supports several operations and possibly a final
 * conditional transformation depending on the operation result.
 *
 * To perform 8 bit operations, operands contain the 8 bit values in the most
 * significant bits of the 16 bit operand.
 */

object AluOperation extends SpinalEnum(defaultEncoding = binarySequential) {
	val add,
		sub,
		and,
		or,
		xor,
		shift,
		extend1,
		compare,
		operand1 = newElement()
}


object Condition extends SpinalEnum(defaultEncoding = binarySequential) {
	val	le, gt, lt, ge, leu, gtu, ltu, geu,
		eq, ne,  t,  f = newElement()
}


class Alu extends Component {
	val io = new Bundle {
		val operand1 = in UInt(16 bits)
		val operand2 = in UInt(16 bits)

		val control = in (AluControl())

		val dataOut      = out UInt(16 bits)
		val conditionMet = out Bool
		val highByteZero = out Bool
	}

	private val shifter = Shifter(16 bits)

	shifter.io.operand   <> io.operand1
	shifter.io.amount    <> io.operand2(11 downto 8)
	shifter.io.operation <> io.control.shiftOperation

	val condition = new Area {
		private val overflow = io.operand1(11)
		private val negative = io.operand1(10)
		private val zero     = io.operand1(9)
		private val carry    = io.operand1(8)

		private val cc_le  = (overflow ^ negative || zero)
		private val cc_lt  = (overflow ^ negative)
		private val cc_leu = (carry || zero)
		private val cc_ltu = (carry)
		private val cc_eq  = (zero)

		val met = io.control.condition.mux(
			Condition.le   -> cc_le,
			Condition.gt   -> !cc_le,
			Condition.lt   -> cc_lt,
			Condition.ge   -> !cc_lt,
			Condition.leu  -> cc_leu,
			Condition.gtu  -> !cc_leu,
			Condition.ltu  -> cc_ltu,
			Condition.geu  -> !cc_ltu,
			Condition.eq   -> cc_eq,
			Condition.ne   -> !cc_eq,
			Condition.t    -> True,
			Condition.f    -> False
		)
	}

	private val subtract = new Area {
		private val isAdd = io.control.operation === AluOperation.add
		private val operand1 = io.operand1 ## False
		private val operand2 = (isAdd ? ~io.operand2 | io.operand2) ## isAdd
		private val subResult = operand1.asUInt -^ operand2.asUInt

		val result = subResult(16 downto 1)
		val carry = subResult(17)
	}

	private val result = io.control.operation.mux(
		AluOperation.and       -> (io.operand1 & io.operand2),
		AluOperation.or        -> (io.operand1 | io.operand2),
		AluOperation.xor       -> (io.operand1 ^ io.operand2),
		AluOperation.shift     -> shifter.io.result,
		AluOperation.extend1   -> B(16 bits, default -> io.operand1.msb).asUInt,
		AluOperation.operand1  -> io.operand1,
		/* add, compare, sub */
		default                -> subtract.result
	)

	val flags = new Area {
		private val overflow = (result.msb === io.operand2.msb) && (io.operand1.msb =/= io.operand2.msb)
		private val negative = result.msb
		private val carry    = subtract.carry
		private val zero     = result === U(0)

		io.highByteZero := result(15 downto 8) === U(0)

		val out = B(0, 4 bits) ## overflow ## negative ## zero ## carry ## B(0, 8 bits)
	}

	private val selectFlags = io.control.operation === AluOperation.compare
	
	io.dataOut := selectFlags ? flags.out.asUInt | result
	io.conditionMet := condition.met
}

