package rc800

import spinal.core._
import spinal.core.sim._

/*
 * Runs real instruction streams through the core and checks the stack
 * semantics from the outside, which is what exercises the RC811 wiring: an LCR
 * write to an SP register must leave the pair holding the entry at the new
 * pointer, and PUSH must duplicate the top only after the pointer has moved.
 */
class CpuStackPointerTest extends munit.FunSuite {

	private val compiled = SimConfig.compile(new RC811()(lpm.generic.Components))

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

			dut.clockDomain.waitSampling(400)

			check(mem)
		}
	}

	test("stack pointer writes refresh the register pair") {
		run(Seq(
			0x83, 0x00,		// ld  c,$00   (SP_FT)
			0x81, 0x30,		// ld  t,$30
			0x0A,			// lcr (c),t   SP_FT := $30
			0x81, 0x55,		// ld  t,$55   Stack[$30] low  (power-up is undefined)
			0x80, 0x22,		// ld  f,$22   Stack[$30] high
			0xC0,			// push ft     SP_FT := $2F, duplicate $2255
			0x81, 0x30,		// ld  t,$30   clobbers Stack[$2F] only
			0x0A,			// lcr (c),t   SP_FT := $30, FT := Stack[$30]
			0x86, 0x01,		// ld  h,$01
			0x87, 0x00,		// ld  l,$00
			0x03,			// ld  (hl),t  $0100 <- $55
			0xBF, 0x01,		// add hl,1
			0x58,			// ld  t,f
			0x03,			// ld  (hl),t  $0101 <- $22
			0xBF, 0x01,		// add hl,1
			0x81, 0x5A,		// ld  t,$5A
			0x03			// ld  (hl),t  $0102 <- $5A (program reached the end)
		)) { mem =>
			assertEquals(mem(0x100), 0x55, "register refreshed from the new stack cell")
			assertEquals(mem(0x101), 0x22, "high byte refreshed from the new stack cell")
			assertEquals(mem(0x102), 0x5A, "program ran to completion")
		}
	}

	test("push duplicates the top after the pointer has moved") {
		run(Seq(
			0x80, 0x00,		// ld  f,$00
			0x81, 0xAA,		// ld  t,$AA
			0xC0,			// push ft      Stack[$FE] <- $AA
			0x81, 0x55,		// ld  t,$55
			0xC0,			// push ft      Stack[$FD] <- $55
			0xC4,			// pop ft       T <- Stack[$FE] = $55
			0x86, 0x01,		// ld  h,$01
			0x87, 0x00,		// ld  l,$00
			0x03,			// ld  (hl),t   $0100 <- $55
			0xBF, 0x01,		// add hl,1
			0xC4,			// pop ft       T <- Stack[$FF] = $AA
			0x03,			// ld  (hl),t   $0101 <- $AA
			0xBF, 0x01,		// add hl,1
			0x81, 0x5A,		// ld  t,$5A
			0x03			// ld  (hl),t   $0102 <- $5A
		)) { mem =>
			assertEquals(mem(0x100), 0x55, "entry pushed last")
			assertEquals(mem(0x101), 0xAA, "entry pushed first")
			assertEquals(mem(0x102), 0x5A, "program ran to completion")
		}
	}
}
