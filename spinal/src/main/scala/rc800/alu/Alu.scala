package rc800.alu

import spinal.core._
import spinal.lib._

import rc800.control.component.AluControl

import rc800.lpm

import rc800.utils._


/*
 * This is a 16 bit ALU that supports several operations and possibly a final
 * conditional transformation depending on the operation result.
 *
 * To perform 8 bit operations, operands contain the 8 bit values in the most
 * significant bits of the 16 bit operand.
 */

object AluOperation extends SpinalEnumExtends(ShiftOperation) {
	val add,
		sub,
		and,
		or,
		xor,
		extend1,
		compare,
		operand1 = newElement()

	val shiftOperationMask = M"00--"
}


object Condition extends SpinalEnum(defaultEncoding = binarySequential) {
	val	le, gt, lt, ge, leu, gtu, ltu, geu,
		eq, ne,  t,  f = newElement()
}


class Alu(lpmComponents: lpm.Components) extends Component {
	val io = new Bundle {
		val operand1 = in UInt(16 bits)
		val operand2 = in UInt(16 bits)

		val control = in (AluControl())

		val dataOut      = out UInt(16 bits)
		val conditionMet = out Bool
		val highByteZero = out Bool
	}

	private val shifter = Shifter(16 bits, lpmComponents)

	shifter.io.operand   <> io.operand1
	shifter.io.amount    <> io.operand2(11 downto 8)
	shifter.io.operation <> AluOperation.asBase(io.control.operation)

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

	private val subtract = lpmComponents.addSub(16, lpm.AddSub.Representation.signed, lpm.AddSub.Direction.dynamic)

	subtract.io.add_sub := io.control.operation === AluOperation.add
	subtract.io.dataa := io.operand1.asBits
	subtract.io.datab := io.operand2.asBits

	private val result =
		(io.control.operation.asBits === AluOperation.shiftOperationMask) ? shifter.io.result |
		(io.control.operation.mux(
			AluOperation.and      -> (io.operand1 & io.operand2),
			AluOperation.or       -> (io.operand1 | io.operand2),
			AluOperation.xor      -> (io.operand1 ^ io.operand2),
			AluOperation.extend1  -> B(16 bits, default -> io.operand1.msb).asUInt,
			AluOperation.operand1 -> io.operand1,

			/* add, compare, sub */
			default -> subtract.io.result.asUInt
		))

	val flags = new Area {
		private val overflow = subtract.io.overflow
		private val negative = result.msb
		private val carry    = !subtract.io.cout
		private val zero     = result === U(0)

		io.highByteZero := result(15 downto 8) === U(0)

		val out = B(0, 4 bits) ## overflow ## negative ## zero ## carry ## B(0, 8 bits)
	}

	private val selectFlags = io.control.operation === AluOperation.compare
	
	io.dataOut := selectFlags ? flags.out.asUInt | result
	io.conditionMet := condition.met
}

