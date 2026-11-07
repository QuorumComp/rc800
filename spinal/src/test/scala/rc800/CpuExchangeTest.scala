package rc800

import spinal.core._
import spinal.core.sim._

/*
 * Runs real instruction streams through the core to check the two exchange
 * instructions, which are the only other users of the auxiliary write port
 * besides a load that steps its pointer.  Both ports write in the same cycle and
 * both destinations must end up holding their new value in the stack cell too,
 * since the register is the cell - so each stream dumps the exchanged pairs
 * through a window and then reads them back with PUSH/POP, which only shows the
 * cell.  The byte exchange writes bytes on both ports and the pair exchange
 * writes pairs on both, which between them pin the byte-lane rule for equal
 * widths; the stepped load covers the mixed-width case.
 */
class CpuExchangeTest extends munit.FunSuite {

	// Verilator >=5.046 warns (fatally) about simPublic signals blocking
	// optimization; the lint targets the debug-visible stage/pc registers.
	private val compiled = SimConfig
		.addSimulatorFlag("--Wno-UNOPTPUBLIC")
		.compile(new RC811()(lpm.generic.Components))

	private def run(program: Seq[Int], data: Seq[(Int, Int)] = Seq.empty)(check: Array[Int] => Unit): Unit = {
		compiled.doSim { dut =>
			val mem = Array.fill[Int](65536)(0)
			program.zipWithIndex.foreach { case (byte, i) => mem(i) = byte }
			data.foreach { case (address, byte) => mem(address) = byte }
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

			dut.clockDomain.waitSampling(400)

			check(mem)
		}
	}

	test("EXG T,F swaps the hub bytes and both cells follow") {
		run(Seq(
			0x80, 0xAA,		// ld  f,$AA
			0x81, 0x55,		// ld  t,$55
			0x10,			// exg t,f      F = $55, T = $AA
			0xC0,			// push ft      duplicate the hub's top cell...
			0xC4,			// pop  ft      ...and read both bytes back from it
			0x86, 0x03,		// ld  h,$03
			0x87, 0xD0,		// ld  l,$D0
			0x03,			// ld  (hl),t   $03D0 <- T
			0x9F, 0x01,		// add hl,#1
			0x58,			// ld  t,f
			0x03			// ld  (hl),t   $03D1 <- F
		)) { mem =>
			assertEquals(mem(0x3D0), 0xAA, "T took the value that was in F")
			assertEquals(mem(0x3D1), 0x55, "F took the value that was in T")
		}
	}

	test("EXG FT,BC swaps the pairs and both cells follow") {
		run(Seq(
			0x82, 0xAB,		// ld  b,$AB
			0x83, 0xCD,		// ld  c,$CD    BC = $ABCD
			0x80, 0x12,		// ld  f,$12
			0x81, 0x34,		// ld  t,$34    FT = $1234
			0xC9,			// exg ft,bc    FT = $ABCD, BC = $1234
			0xC0,			// push ft      duplicate the hub's top cell...
			0xC4,			// pop  ft      ...and read both bytes back from it
			0xC1,			// push bc      the same for the pair
			0xC5,			// pop  bc
			0x86, 0x03,		// ld  h,$03
			0x87, 0xE0,		// ld  l,$E0
			0x03,			// ld  (hl),t   $03E0 <- T
			0x9F, 0x01,		// add hl,#1
			0x58,			// ld  t,f
			0x03,			// ld  (hl),t   $03E1 <- F
			0x9F, 0x01,		// add hl,#1
			0x5A,			// ld  t,b
			0x03,			// ld  (hl),t   $03E2 <- B
			0x9F, 0x01,		// add hl,#1
			0x5B,			// ld  t,c
			0x03			// ld  (hl),t   $03E3 <- C
		)) { mem =>
			assertEquals(mem(0x3E0), 0xCD, "the hub took the low byte of the pair")
			assertEquals(mem(0x3E1), 0xAB, "the hub took the high byte of the pair")
			assertEquals(mem(0x3E2), 0x12, "the pair took the high byte of the hub")
			assertEquals(mem(0x3E3), 0x34, "the pair took the low byte of the hub")
		}
	}
}
