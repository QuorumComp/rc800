package rc800

import spinal.core._
import spinal.core.sim._

/*
 * Runs the instruction stream that the firmware probe uses: a byte-register
 * logical or arithmetic op against the hub, its result stored, then PUSHA/POPA
 * in between - so the register read port is steered elsewhere between two ops,
 * exactly as it is when the result goes out through a debug print.
 *
 * The point is operand 2.  The ALU takes a registered copy of the read bus
 * while the memory stage takes it directly, so a stream like this is what tells
 * the two paths apart: every store of a byte register here has been observed to
 * write the right value on hardware while the matching ALU op saw a value from
 * an earlier instruction.  Each case pins one operand register, and the last
 * two reload B before using it, which is where a stale operand shows up.
 */
class CpuAluOperandTest extends munit.FunSuite {

	// Verilator >=5.046 warns (fatally) about simPublic signals blocking
	// optimization; the lint targets the debug-visible stage/pc registers.
	private val compiled = SimConfig
		.addSimulatorFlag("--Wno-UNOPTPUBLIC")
		.compile(new RC811()(lpm.generic.Components))

	private def run(program: Seq[Int])(check: Array[Int] => Unit): Unit = {
		compiled.doSim { dut =>
			val mem = Array.fill[Int](65536)(0)
			program.zipWithIndex.foreach { case (byte, i) => mem(i) = byte }
			mem(0xFFFF) = 0		// reset vector fetches one instruction first

			dut.io.nmi #= false
			dut.io.irq #= false
			dut.clockDomain.forkStimulus(period = 10)

			// HC800-style synchronous memory: address presented in one cycle, data
			// sampled by the CPU in the next.
			fork {
				dut.io.dataIn #= 0
				while (true) {
					dut.clockDomain.waitSampling()
					if (dut.io.busEnable.toBoolean) {
						val address = dut.io.address.toInt
						if (dut.io.write.toBoolean) {
							mem(address) = dut.io.dataOut.toInt
						} else {
							dut.io.dataIn #= mem(address)
						}
					}
				}
			}

			dut.clockDomain.waitSampling(4000)

			check(mem)
		}
	}

	// LD T,I, LD R8,I, the T,R8 blocks (f=0 t=1 b=2 c=3 d=4 e=5 h=6 l=7),
	// LD (HL),T, ADD HL,I, PUSHA, POPA, LD T,R8, NOP.
	private def ldT(immediate: Int)       = Seq(0x81, immediate)
	private def ldR8(register: Int, value: Int) = Seq(0x80 + register, value)
	private def alu(operation: Int, register: Int) = Seq(operation + register)
	private val storeT  = Seq(0x03)
	private val bumpHl  = Seq(0x9F, 0x01)
	private val saveAll = Seq(0xE8)
	private val restoreAll = Seq(0xE9)
	private val ldTFrom = (register: Int) => Seq(0x58 + register)

	private val f = 0
	private val b = 2
	private val c = 3
	private val d = 4
	private val e = 5

	private val or = 0x60
	private val and = 0x68
	private val xor = 0x70
	private val sub = 0x50

	/** One probe: give the operand register its value, do the op against T,
	 *  publish the result, then do the bookkeeping a debug print does. */
	private def probe(operandRegister: Int, value: Int, operation: Int, tValue: Int) =
		ldR8(operandRegister, value) ++ ldT(tValue) ++ alu(operation, operandRegister) ++
			storeT ++ bumpHl ++ saveAll ++ restoreAll

	test("operand 2 is the named byte register, op after op, with PUSHA/POPA between") {
		val program =
			// dump pointer: results land from $0200 up
			ldR8(6, 0x02) ++ ldR8(7, 0x00) ++
			// T=$0F against each byte register in turn
			probe(b, 0xF0, or, 0x0F) ++		// $0200: $FF
			probe(c, 0x3C, or, 0x0F) ++		// $0201: $3F
			probe(d, 0xF0, or, 0x0F) ++		// $0202: $FF
			probe(e, 0x0C, or, 0x0F) ++		// $0203: $0F
			probe(f, 0xF0, or, 0x0F) ++		// $0204: $FF
			// B again, twice reloaded: a stale operand shows here
			probe(b, 0x3C, or, 0x0F) ++		// $0205: $3F
			probe(b, 0x01, or, 0x0F) ++		// $0206: $0F
			// the other operations against a reloaded B
			probe(b, 0xF0, and, 0x0F) ++	// $0207: $00
			probe(b, 0xF0, xor, 0x0F) ++	// $0208: $FF
			probe(b, 0xF0, sub, 0x9A) ++	// $0209: $AA
			// LD T,R8: register file to writeback with no ALU in the way
			ldR8(b, 0xF0) ++ ldTFrom(b) ++ storeT ++ bumpHl ++
			Seq(0x00, 0x00)
		val expected = Seq(0xFF, 0x3F, 0xFF, 0x0F, 0xFF, 0x3F, 0x0F, 0x00, 0xFF, 0xAA, 0xF0)

		run(program) { mem =>
			val got = (0 until expected.size).map(i => mem(0x0200 + i))
			val names = Seq(
				"or  t,b  B=$F0", "or  t,c  C=$3C", "or  t,d  D=$F0", "or  t,e  E=$0C",
				"or  t,f  F=$F0", "or  t,b  B=$3C", "or  t,b  B=$01", "and t,b  B=$F0",
				"xor t,b  B=$F0", "sub t,b  B=$F0", "ld  t,b  B=$F0")
			val report = names.zip(expected).zip(got).map { case ((name, want), have) =>
				f"  $name%-16s want $$$want%02X ${if (have == want) "ok" else f"got $$$have%02X"}"
			}.mkString("\n")
			assertEquals(got, expected, "\n" + report)
		}
	}
}
