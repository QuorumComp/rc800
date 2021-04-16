package rc800.alu

import spinal.core._
import spinal.lib._

/*
 * This is a 16 bit ALU that supports several operations and possibly a final
 * conditional transformation depending on the operation result.
 *
 * To perform 8 bit operations, operands contain the 8 bit values in the most
 * significant bits of the 16 bit operand.
 */

object AluOperation extends SpinalEnum {
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

object Condition extends SpinalEnum {
	val	le, gt, lt, ge, leu, gtu, ltu, geu,
		eq,  ne,  t,   f = newElement()
}


class Alu extends Component {
	val io = new Bundle {
		val operand1 = in UInt(16 bits)
		val operand2 = in UInt(16 bits)

		val operation      = in (AluOperation())
		val shiftOperation = in (ShiftOperation())
		val condition      = in (Condition())

		val dataOut      = out UInt(16 bits)
		val conditionMet = out Bool
		val highByteZero = out Bool
	}

	private val shifter = Shifter(16 bits)

	shifter.io.operand   <> io.operand1
	shifter.io.amount    <> io.operand2(11 downto 8)
	shifter.io.operation <> io.shiftOperation

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

		val met = io.condition.mux(
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

	private val carryAndResult = io.operation.mux(
		AluOperation.add       -> (io.operand1 +^ io.operand2),
		AluOperation.and       -> (io.operand1 & io.operand2).resize(17 bits),
		AluOperation.or        -> (io.operand1 | io.operand2).resize(17 bits),
		AluOperation.xor       -> (io.operand1 ^ io.operand2).resize(17 bits),
		AluOperation.shift     -> shifter.io.result.resize(17 bits),
		AluOperation.extend1   -> B(17 bits, default -> io.operand1.msb).asUInt,
		AluOperation.operand1  -> io.operand1.resize(17 bits),
		/* compare, sub */
		default                -> (io.operand1 -^ io.operand2)
	)

	private val result = carryAndResult(15 downto 0)

	val flags = new Area {
		private val overflow = (result.msb === io.operand2.msb) && (io.operand1.msb =/= io.operand2.msb)
		private val negative = result.msb
		private val carry    = carryAndResult(16)
		private val zero     = result === U(0)

		io.highByteZero := result(15 downto 8) === U(0)

		val out = B(0, 4 bits) ## overflow ## negative ## zero ## carry ## B(0, 8 bits)
	}

	private val selectFlags = io.operation === AluOperation.compare
	
	io.dataOut := selectFlags ? flags.out.asUInt | result
	io.conditionMet := condition.met
}

