package rc800

import spinal.core._
import spinal.core.sim._

/*
 * Stack overflow (vector $20) through the real core: a PUSH/PUSHA whose
 * stack pointer sits at $00 takes the vector in place of the instruction
 * — the handler runs from $0020, and RETI re-executes the cancelled
 * push, so the handler must free stack cells first (cancel-and-restart,
 * the same semantics as every other interrupt entry: HL holds the
 * cancelled instruction's address).  A push with room left must stay
 * silent.  The observation points are memory markers written by the
 * handler and by the resumed mainline; programs end in a jump-to-self
 * halt so nothing ever slides into the handler through the zero-page
 * NOP sled.  The harness is the synchronous-memory template of
 * CpuStackPointerTest.
 */
class CpuStackOverflowTest extends munit.FunSuite {

	private val compiled = SimConfig.compile(new RC811()(lpm.generic.Components))

	private def run(program: Seq[Int], handler: Seq[Int])(check: Array[Int] => Unit): Unit = {
		compiled.doSim { dut =>
			val mem = Array.fill[Int](65536)(0)
			program.zipWithIndex.foreach { case (byte, i) => mem(i) = byte }
			handler.zipWithIndex.foreach { case (byte, i) => mem(0x20 + i) = byte }
			mem(0xFFFF) = 0		// reset vector fetches one instruction first

			dut.io.nmi #= false
			dut.io.irq #= false
			dut.clockDomain.forkStimulus(period = 10)

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

	// Handler: first free the DE stack (LCR SP_DE := $F0 — RETI
	// re-executes the cancelled instruction, so the fault must be
	// resolved before the retry; LCR itself clobbers DE, so nothing may
	// be stashed there yet), then mark $0100=$EE, then RETI.  RETI loads
	// PC from HL, so the resumed-marker assertion proves HL held the
	// right return address — which means the marker store must not
	// destroy it: HL is stashed in BC *after* the LCR (whose C load would
	// otherwise eat the stash) and the FT stack is avoided entirely
	// (writing FT would clobber a pushed copy — the register *is* the
	// stack top).
	private val handler = Seq(
		0x83, 0x02,		// ld  c,$02
		0x81, 0xF0,		// ld  t,$F0
		0x0A,			// lcr (c),t   SP_DE := $F0 (room again)
		0x5E,			// ld  t,h
		0x7A,			// ld  b,t
		0x5F,			// ld  t,l
		0x7B,			// ld  c,t       BC = return address
		0x86, 0x01,		// ld  h,$01
		0x87, 0x00,		// ld  l,$00
		0x81, 0xEE,		// ld  t,$EE
		0x03,			// ld  (hl),t    $0100 <- $EE  (handler ran)
		0x5A,			// ld  t,b
		0x7E,			// ld  h,t
		0x5B,			// ld  t,c
		0x7F,			// ld  l,t       HL = return address restored
		0x59			// reti
	)

	// Resume: write $0102 <- $CD, then halt by jumping to the $000C loop.
	private val resume = Seq(
		0x86, 0x01,		// ld  h,$01
		0x87, 0x02,		// ld  l,$02
		0x81, 0xCD,		// ld  t,$CD
		0x03,			// ld  (hl),t    $0102 <- $CD  (resumed)
		0x86, 0x00,		// ld  h,$00
		0x87, 0x0C,		// ld  l,$0C
		0x3F			// j  (hl)       halt
	)

	test("push against SP $00 takes vector $20 and resumes after it") {
		run(
			Seq(
				0x83, 0x02,		// ld  c,$02   (CR of SP_DE)
				0x81, 0x00,		// ld  t,$00
				0x0A,			// lcr (c),t   SP_DE := $00
				0xC2			// push de     overflow -> $20, push discarded
			) ++ resume, handler) { mem =>
			assertEquals(mem(0x100), 0xEE, "handler ran")
			assertEquals(mem(0x102), 0xCD, "RETI resumed the mainline")
		}
	}

	test("pusha with any full stack takes vector $20") {
		run(
			Seq(
				0x83, 0x02,		// ld  c,$02
				0x81, 0x00,		// ld  t,$00
				0x0A,			// lcr (c),t   SP_DE := $00
				0xF8			// pusha       overflow -> $20
			) ++ resume, handler) { mem =>
			assertEquals(mem(0x100), 0xEE, "handler ran")
			assertEquals(mem(0x102), 0xCD, "RETI resumed the mainline")
		}
	}

	test("push with room left never reaches the vector") {
		run(
			Seq(
				0x80, 0x12,		// ld  f,$12
				0x81, 0x34,		// ld  t,$34   FT = $1234
				0xC0,			// push ft     SP_FT $FF -> $FE, no fault
				0x86, 0x01,		// ld  h,$01
				0x87, 0x00,		// ld  l,$00
				0x81, 0x11,		// ld  t,$11
				0x03,			// ld  (hl),t  $0100 <- $11 (no handler marker)
				0x86, 0x00,		// ld  h,$00
				0x87, 0x0C,		// ld  l,$0C
				0x3F			// j  (hl)     halt
			), handler) { mem =>
			assertEquals(mem(0x100), 0x11, "handler never ran")
		}
	}
}
