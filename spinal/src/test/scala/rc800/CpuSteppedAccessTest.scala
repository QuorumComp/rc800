package rc800

import spinal.core._
import spinal.core.sim._

/*
 * Runs real instruction streams through the core to check the stepped byte
 * block ($10-$1F: LD (FT+),r8 and LD r8,(FT+) - the plain FT-indirect access of
 * $20-$2F plus one step of FT afterwards) from the outside.  Three things are
 * new there and only the core can show them: the byte operand is named by the
 * opcode's r8 field (not the hub), a load delivers that byte on the auxiliary
 * write port while the main port steps FT, and a store reads its byte out of the
 * same register lane the memory stage drives while its ALU adds the step.  The
 * streams also check that a stepped FT keeps its stack entry, since the pair
 * write is what duplicates the top cell, and that two LD r8,(FT+) fill a register
 * pair directly - the little-endian word load that is the point of the form.
 *
 * Note the pointer is FT, whose low half is T: routing a loaded byte through T
 * (to store it) clobbers the cursor, so a walk finishes before any T use.
 */
class CpuSteppedAccessTest extends munit.FunSuite {

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

	test("LD (FT+),r8 walks FT upward, storing the named byte at each step") {
		run(Seq(
			0x80, 0x01,		// ld  f,$01    FT high
			0x81, 0x00,		// ld  t,$00    FT = $0100
			0x82, 0xAA,		// ld  b,$AA
			0x12,			// ld  (ft+),b   $0100 <- $AA, FT = $0101
			0x82, 0xBB,		// ld  b,$BB
			0x12,			// ld  (ft+),b   $0101 <- $BB, FT = $0102
			0x82, 0xCC,		// ld  b,$CC
			0x12,			// ld  (ft+),b   $0102 <- $CC, FT = $0103
			0x84, 0xDD,		// ld  d,$DD
			0x14			// ld  (ft+),d   $0103 <- $DD via a different r8, FT = $0104
		)) { mem =>
			assertEquals(mem(0x100), 0xAA, "first byte stored")
			assertEquals(mem(0x101), 0xBB, "second byte stored one further")
			assertEquals(mem(0x102), 0xCC, "third byte stored one further still")
			assertEquals(mem(0x103), 0xDD, "a different r8 stores through the same cursor")
		}
	}

	test("LD r8,(FT+) walks FT upward, delivering each byte to the named register") {
		// The three loads run back-to-back so the cursor is never clobbered by the
		// T-routing the dump needs; B, C and D show the loaded byte landing in the
		// r8 register the opcode named, on the auxiliary write port.
		run(Seq(
			0x80, 0x02,		// ld  f,$02
			0x81, 0x00,		// ld  t,$00    FT = $0200
			0x1A,			// ld  b,(ft+)  B = $11, FT = $0201
			0x1B,			// ld  c,(ft+)  C = $22, FT = $0202
			0x1C,			// ld  d,(ft+)  D = $33, FT = $0203
			0x86, 0x03,		// ld  h,$03
			0x87, 0x00,		// ld  l,$00    HL = $0300 (dump window)
			0x5A,			// ld  t,b
			0x03,			// ld  (hl),t   $0300 <- B
			0x9F, 0x01,		// add hl,#1
			0x5B,			// ld  t,c
			0x03,			// ld  (hl),t   $0301 <- C
			0x9F, 0x01,		// add hl,#1
			0x5C,			// ld  t,d
			0x03			// ld  (hl),t   $0302 <- D
		), Seq(0x200 -> 0x11, 0x201 -> 0x22, 0x202 -> 0x33)) { mem =>
			assertEquals(mem(0x300), 0x11, "first byte loaded into B")
			assertEquals(mem(0x301), 0x22, "second byte loaded into C one further")
			assertEquals(mem(0x302), 0x33, "third byte loaded into D one further still")
		}
	}

	test("two LD r8,(FT+) fill a pair - the little-endian word load, no exg") {
		// The payoff of the form: a 16-bit little-endian value lands in BC from two
		// byte loads straight into the halves, FT stepping between them.
		run(Seq(
			0x80, 0x02,		// ld  f,$02
			0x81, 0x00,		// ld  t,$00    FT = $0200
			0x1B,			// ld  c,(ft+)  C = $AB (low),  FT = $0201
			0x1A,			// ld  b,(ft+)  B = $CD (high), FT = $0202   BC = $CDAB
			0x86, 0x03,		// ld  h,$03
			0x87, 0x00,		// ld  l,$00    HL = $0300 (dump window)
			0x5A,			// ld  t,b
			0x03,			// ld  (hl),t   $0300 <- B
			0x9F, 0x01,		// add hl,#1
			0x5B,			// ld  t,c
			0x03			// ld  (hl),t   $0301 <- C
		), Seq(0x200 -> 0xAB, 0x201 -> 0xCD)) { mem =>
			assertEquals(mem(0x300), 0xCD, "high half loaded second, from the higher byte")
			assertEquals(mem(0x301), 0xAB, "low half loaded first, from the lower byte")
		}
	}

	test("a stepped FT keeps the entry at its new stack top") {
		// The stepped store writes FT as an ordinary pair write, so the top cell
		// follows the pointer; push/pop through the cell recovers the stepped value,
		// not the pre-step one.  FT starts $0102 and steps to $0103, so the low byte
		// is a distinct $03 that only the stepped write could have left in the cell.
		run(Seq(
			0x80, 0x01,		// ld  f,$01
			0x81, 0x02,		// ld  t,$02    FT = $0102
			0x82, 0xAA,		// ld  b,$AA
			0x12,			// ld  (ft+),b  $0102 <- $AA, FT = $0103
			0xC0,			// push ft      duplicate the stepped pointer
			0xC4,			// pop  ft      read it back out of the cell, FT = $0103
			0x82, 0x03,		// ld  b,$03
			0x83, 0xE0,		// ld  c,$E0    BC = $03E0 (dump window)
			0x01,			// ld  (bc),t   $03E0 <- T   (stepped low byte)
			0x9D, 0x01,		// add bc,#1
			0x58,			// ld  t,f
			0x01			// ld  (bc),t   $03E1 <- F   (stepped high byte)
		)) { mem =>
			assertEquals(mem(0x102), 0xAA, "byte stored through the pointer")
			// The pop reads the stack cell rather than the register, so the low byte
			// is $03 only if the stepped write also wrote the top cell.
			assertEquals(mem(0x3E0), 0x03, "stepped low byte reached the top cell")
			assertEquals(mem(0x3E1), 0x01, "stepped high byte reached the top cell")
		}
	}
}
