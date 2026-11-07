package rc800

import spinal.core._
import spinal.core.sim._

/*
 * Runs real instruction streams through the core to check the stepped access
 * block ($11-$1F: LD (R16),T and LD T,(R16) with one step of the pointer
 * afterwards) from the outside.  Three things are new there and only the core
 * can show them: a load that writes two registers at once, the loaded byte
 * arriving on the auxiliary write port, and a store whose ALU adds a step while
 * the memory stage reads its data out of the same register lane.  The streams
 * also check that a stepped pointer keeps its stack entry, since the pair write
 * is what duplicates the top cell.
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

	test("LD (HL+),T walks upward and leaves the pointer stepped") {
		run(Seq(
			0x86, 0x01,		// ld  h,$01
			0x87, 0x00,		// ld  l,$00
			0x81, 0xAA,		// ld  t,$AA
			0x13,			// ld  (hl+),t  $0100 <- $AA, HL = $0101
			0x81, 0xBB,		// ld  t,$BB
			0x13,			// ld  (hl+),t  $0101 <- $BB, HL = $0102
			0x5E,			// ld  t,h
			0x03,			// ld  (hl),t   $0102 <- H
			0x9F, 0x01,		// add hl,#1    HL = $0103
			0x5F,			// ld  t,l      the pointer as stepped, not as it was
			0x03			// ld  (hl),t   $0103 <- L
		)) { mem =>
			assertEquals(mem(0x100), 0xAA, "first byte stored")
			assertEquals(mem(0x101), 0xBB, "second byte stored one further")
			assertEquals(mem(0x102), 0x01, "pointer stepped by both stores")
			assertEquals(mem(0x103), 0x03, "low byte stepped too")
		}
	}

	test("LD T,(HL+) walks upward, and the loaded byte keeps the hub's stack entry") {
		run(Seq(
			0x86, 0x02,		// ld  h,$02
			0x87, 0x00,		// ld  l,$00
			0x17,			// ld  t,(hl+)  T = $11, HL = $0201
			0x82, 0x03,		// ld  b,$03    dump window at $03F0
			0x83, 0xF0,		// ld  c,$F0
			0x01,			// ld  (bc),t   $03F0 <- T
			0x9D, 0x01,		// add bc,#1
			0x17,			// ld  t,(hl+)  T = $22, HL = $0202
			0x01,			// ld  (bc),t   $03F1 <- T
			0x9D, 0x01,		// add bc,#1
			0xC0,			// push ft      duplicate the byte the auxiliary port wrote
			0xC4,			// pop  ft      and take it back from the stack cell
			0x01,			// ld  (bc),t   $03F2 <- T
			0x9D, 0x01,		// add bc,#1
			0x5E,			// ld  t,h
			0x01,			// ld  (bc),t   $03F3 <- H
			0x9D, 0x01,		// add bc,#1
			0x5F,			// ld  t,l
			0x01			// ld  (bc),t   $03F4 <- L
		), Seq(0x200 -> 0x11, 0x201 -> 0x22)) { mem =>
			assertEquals(mem(0x3F0), 0x11, "first byte loaded")
			assertEquals(mem(0x3F1), 0x22, "second byte loaded one further")
			assertEquals(mem(0x3F2), 0x22, "the load duplicated the hub's top cell")
			assertEquals(mem(0x3F3), 0x02, "pointer stepped by both loads")
			assertEquals(mem(0x3F4), 0x02, "low byte stepped too")
		}
	}

	test("LD (DE-),T walks downward and steps down") {
		run(Seq(
			0x84, 0x01,		// ld  d,$01
			0x85, 0x00,		// ld  e,$00    DE = $0100
			0x81, 0xAA,		// ld  t,$AA
			0x1A,			// ld  (de-),t  $0100 <- $AA, DE = $00FF
			0x81, 0xBB,		// ld  t,$BB
			0x1A,			// ld  (de-),t  $00FF <- $BB, DE = $00FE
			0x5D,			// ld  t,e
			0x02,			// ld  (de),t   $00FE <- E
			0x9E, 0xFF,		// add de,#-1   DE = $00FD
			0x5C,			// ld  t,d
			0x02			// ld  (de),t   $00FD <- D
		)) { mem =>
			assertEquals(mem(0x100), 0xAA, "first byte stored at the starting address")
			assertEquals(mem(0x0FF), 0xBB, "second byte stored one lower")
			assertEquals(mem(0x0FE), 0xFE, "pointer stepped down by both stores")
			assertEquals(mem(0x0FD), 0x00, "high byte stepped too")
		}
	}

	test("LD T,(BC-) walks downward and steps down") {
		run(Seq(
			0x82, 0x01,		// ld  b,$01
			0x83, 0x00,		// ld  c,$00    BC = $0100
			0x1D,			// ld  t,(bc-)  T = $11, BC = $00FF
			0x86, 0x03,		// ld  h,$03
			0x87, 0xF0,		// ld  l,$F0
			0x03,			// ld  (hl),t   $03F0 <- T
			0x9F, 0x01,		// add hl,#1
			0x1D,			// ld  t,(bc-)  T = $22, BC = $00FE
			0x03,			// ld  (hl),t   $03F1 <- T
			0x9F, 0x01,		// add hl,#1
			0x5A,			// ld  t,b
			0x03,			// ld  (hl),t   $03F2 <- B
			0x9F, 0x01,		// add hl,#1
			0x5B,			// ld  t,c
			0x03			// ld  (hl),t   $03F3 <- C
		), Seq(0x100 -> 0x11, 0x0FF -> 0x22)) { mem =>
			assertEquals(mem(0x3F0), 0x11, "first byte loaded")
			assertEquals(mem(0x3F1), 0x22, "second byte loaded one lower")
			assertEquals(mem(0x3F2), 0x00, "pointer stepped down by both loads")
			assertEquals(mem(0x3F3), 0xFE, "low byte stepped too")
		}
	}

	test("a stepped pointer keeps the entry at its new stack top") {
		run(Seq(
			0x82, 0x01,		// ld  b,$01
			0x83, 0x00,		// ld  c,$00    BC = $0100
			0x81, 0x77,		// ld  t,$77
			0x11,			// ld  (bc+),t  $0100 <- $77, BC = $0101
			0xC1,			// push bc      moves the pointer and duplicates
			0xC5,			// pop  bc      reads the entry back out of the cell
			0x86, 0x03,		// ld  h,$03
			0x87, 0xE0,		// ld  l,$E0
			0x5A,			// ld  t,b
			0x03,			// ld  (hl),t   $03E0 <- B
			0x9F, 0x01,		// add hl,#1
			0x5B,			// ld  t,c
			0x03			// ld  (hl),t   $03E1 <- C
		)) { mem =>
			assertEquals(mem(0x100), 0x77, "byte stored through the pointer")
			// The pop reads the stack cell rather than the register, so this pair is
			// $0101 only if the stepped write also wrote the top cell.
			assertEquals(mem(0x3E0), 0x01, "stepped high byte reached the top cell")
			assertEquals(mem(0x3E1), 0x01, "stepped low byte reached the top cell")
		}
	}
}
