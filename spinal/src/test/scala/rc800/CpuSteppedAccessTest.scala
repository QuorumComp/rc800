package rc800

import spinal.core._
import spinal.core.sim._

/*
 * Runs real instruction streams through the core to check the stepped byte
 * block ($10-$1F: LD (R16+),F / LD (R16+),T / LD F,(R16+) / LD T,(R16+) - the
 * plain pair-indirect access plus one step of the pointer afterwards) from the
 * outside.  Three things are new there and only the core can show them: the
 * pointer is named by the standard r16 field (BC/DE/HL, never the hub), the
 * byte operand is a hub byte chosen by the opcode's bit 2 (F=0, T=1), and a
 * load delivers that byte on the auxiliary write port while the main port
 * steps the pointer.  The streams also check that a stepped pointer keeps its
 * stack entry, since the pair write is what duplicates the top cell, that a
 * stepped load fills FT directly - the little-endian word load that is the
 * point of the form - and that stepping disturbs neither the flags nor any
 * other state.
 *
 * The hub rows ($22-$27, $2A-$2F: LD (FT+),r8 / LD r8,(FT+)) are the same
 * access with FT itself as the pointer and the r8 field naming the byte; the
 * plain FT access has no encoding, and the two hub-byte loads that cannot
 * step sit at $28/$29.
 * Those streams pin the read-after-write question: the memory stage must see
 * the old FT while the write stage installs FT+1.
 *
 * A stepped byte whose data is a hub byte never rides FT: the byte would land
 * inside the pointer it steps, which is why those cells are illegal.
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

	test("LD (R16+),T walks BC upward storing T, and DE/HL step their own walks") {
		run(Seq(
			0x82, 0x01,		// ld  b,$01
			0x83, 0x00,		// ld  c,$00    BC = $0100
			0x81, 0xAA,		// ld  t,$AA
			0x11,			// ld  (bc+),t  $0100 <- $AA, BC = $0101
			0x81, 0xBB,		// ld  t,$BB
			0x11,			// ld  (bc+),t  $0101 <- $BB, BC = $0102
			0x81, 0xCC,		// ld  t,$CC
			0x11,			// ld  (bc+),t  $0102 <- $CC, BC = $0103
			0x81, 0x99,		// ld  t,$99
			0x11,			// ld  (bc+),t  $0103 <- $99 - the walk really stepped
			0x84, 0x02,		// ld  d,$02
			0x85, 0x00,		// ld  e,$00    DE = $0200
			0x81, 0xDD,		// ld  t,$DD
			0x12,			// ld  (de+),t  $0200 <- $DD through a different pair
			0x86, 0x03,		// ld  h,$03
			0x87, 0x00,		// ld  l,$00    HL = $0300
			0x81, 0xEE,		// ld  t,$EE
			0x13			// ld  (hl+),t  $0300 <- $EE through the third pair
		)) { mem =>
			assertEquals(mem(0x100), 0xAA, "first byte stored")
			assertEquals(mem(0x101), 0xBB, "second byte stored one further")
			assertEquals(mem(0x102), 0xCC, "third byte stored one further still")
			assertEquals(mem(0x103), 0x99, "the pointer really walked onward")
			assertEquals(mem(0x200), 0xDD, "a second pair stores through its own cursor")
			assertEquals(mem(0x300), 0xEE, "the third pair stores too")
		}
	}

	test("LD (R16+),F stores the flags byte as ordinary data") {
		// F is a hub byte like any other: the odd cell of the data field, legal
		// because the stepped pointer is a general pair, not FT.
		run(Seq(
			0x82, 0x04,		// ld  b,$04
			0x83, 0x00,		// ld  c,$00    BC = $0400
			0x80, 0x5A,		// ld  f,$5A    F = data (and flags)
			0x19,			// ld  (bc+),f  $0400 <- $5A, BC = $0401
			0x19			// ld  (bc+),f  $0401 <- $5A - stepped, same byte
		)) { mem =>
			assertEquals(mem(0x400), 0x5A, "the flags byte stored as data")
			assertEquals(mem(0x401), 0x5A, "the pointer stepped under it")
		}
	}

	test("LD T,(R16+) walks DE upward, delivering each byte through the hub") {
		// The three loads run back-to-back; each lands in T on the auxiliary write
		// port while the main port steps DE, and the dump shows the walked bytes.
		run(Seq(
			0x84, 0x02,		// ld  d,$02
			0x85, 0x00,		// ld  e,$00    DE = $0200
			0x16,			// ld  t,(de+)  T = $11, DE = $0201
			0x86, 0x03,		// ld  h,$03
			0x87, 0x00,		// ld  l,$00    HL = $0300 (dump window)
			0x13, 0x9F, 0xFF,			// ld  (hl),t   $0300 <- $11 (stepped + rewind)
			0x9F, 0x01,		// add hl,#1
			0x16,			// ld  t,(de+)  T = $22, DE = $0202
			0x13, 0x9F, 0xFF,			// ld  (hl),t   $0301 <- $22 (stepped + rewind)
			0x9F, 0x01,		// add hl,#1
			0x16,			// ld  t,(de+)  T = $33, DE = $0203
			0x13, 0x9F, 0xFF,			// ld  (hl),t   $0302 <- $33 (stepped + rewind)
			0x9F, 0x01,		// add hl,#1
			0x5C,			// ld  t,d
			0x13, 0x9F, 0xFF			// ld  (hl),t   $0303 <- D = $02 - the stepped high half (stepped + rewind)
		), Seq(0x200 -> 0x11, 0x201 -> 0x22, 0x202 -> 0x33)) { mem =>
			assertEquals(mem(0x300), 0x11, "first byte loaded through T")
			assertEquals(mem(0x301), 0x22, "second byte loaded one further")
			assertEquals(mem(0x302), 0x33, "third byte loaded one further still")
			assertEquals(mem(0x303), 0x02, "DE stepped to $0203 - high half reached the register")
		}
	}

	test("LD T,(BC+) then LD F,(BC+) fill FT - the little-endian word load") {
		// The payoff of the form: a 16-bit little-endian value lands in FT from
		// two hub-byte loads, BC stepping between them - two instructions.
		run(Seq(
			0x82, 0x02,		// ld  b,$02
			0x83, 0x00,		// ld  c,$00    BC = $0200
			0x15,			// ld  t,(bc+)  T = $AB (low),  BC = $0201
			0x1D,			// ld  f,(bc+)  F = $CD (high), BC = $0202   FT = $CDAB
			0x86, 0x03,		// ld  h,$03
			0x87, 0x00,		// ld  l,$00    HL = $0300 (dump window)
			0x13, 0x9F, 0xFF,			// ld  (hl),t   $0300 <- T = $AB (stepped + rewind)
			0x9F, 0x01,		// add hl,#1
			0x58,			// ld  t,f
			0x13, 0x9F, 0xFF,			// ld  (hl),t   $0301 <- F = $CD (stepped + rewind)
			0x9F, 0x01,		// add hl,#1
			0x5B,			// ld  t,c
			0x13, 0x9F, 0xFF			// ld  (hl),t   $0302 <- C = $02 - the stepped pointer (stepped + rewind)
		), Seq(0x200 -> 0xAB, 0x201 -> 0xCD)) { mem =>
			assertEquals(mem(0x300), 0xAB, "low half loaded first, from the lower byte")
			assertEquals(mem(0x301), 0xCD, "high half loaded second, from the higher byte")
			assertEquals(mem(0x302), 0x02, "the pointer stepped by two over the word")
		}
	}

	test("a stepped pointer keeps the entry at its new stack top") {
		// The stepped store writes BC as an ordinary pair write, so the top cell
		// follows the pointer; push/pop through the cell recovers the stepped
		// value, not the pre-step one.  BC starts $0102 and steps to $0103, so
		// the low byte is a distinct $03 that only the stepped write could have
		// left in the cell.
		run(Seq(
			0x82, 0x01,		// ld  b,$01
			0x83, 0x02,		// ld  c,$02    BC = $0102
			0x81, 0xAA,		// ld  t,$AA
			0x11,			// ld  (bc+),t  $0102 <- $AA, BC = $0103
			0xC1,			// push bc      duplicate the stepped pointer
			0xC5,			// pop  bc      read it back out of the cell, BC = $0103
			0x84, 0x03,		// ld  d,$03
			0x85, 0xE0,		// ld  e,$E0    DE = $03E0 (dump window)
			0x5B,			// ld  t,c
			0x12, 0x9E, 0xFF,			// ld  (de),t   $03E0 <- C   (stepped low byte) (stepped + rewind)
			0x9E, 0x01,		// add de,#1
			0x5A,			// ld  t,b
			0x12, 0x9E, 0xFF,			// ld  (de),t   $03E1 <- B   (stepped high byte) (stepped + rewind)
		)) { mem =>
			assertEquals(mem(0x102), 0xAA, "byte stored through the pointer")
			// The pop reads the stack cell rather than the register, so the low byte
			// is $03 only if the stepped write also wrote the top cell.
			assertEquals(mem(0x3E0), 0x03, "stepped low byte reached the top cell")
			assertEquals(mem(0x3E1), 0x01, "stepped high byte reached the top cell")
		}
	}

	test("stepping disturbs no flags") {
		// Only CMP writes the flags; a stepped load between two dumps of F must
		// leave the flag byte exactly as the compare left it.
		run(Seq(
			0x81, 0x00,		// ld  t,$00
			0x82, 0x01,		// ld  b,$01
			0x4A,			// cmp t,b      flags = (0 - 1): borrow, nonzero
			0x84, 0x03,		// ld  d,$03
			0x85, 0x00,		// ld  e,$00    DE = $0300 (dump window)
			0x58,			// ld  t,f
			0x12, 0x9E, 0xFF,			// ld  (de),t   $0300 <- F (before) (stepped + rewind)
			0x9E, 0x01,		// add de,#1
			0x82, 0x02,		// ld  b,$02
			0x83, 0x00,		// ld  c,$00    BC = $0200
			0x15,			// ld  t,(bc+)  T = $77, BC = $0201
			0x12, 0x9E, 0xFF,			// ld  (de),t   $0301 <- T = $77 (stepped + rewind)
			0x9E, 0x01,		// add de,#1
			0x58,			// ld  t,f
			0x12, 0x9E, 0xFF,			// ld  (de),t   $0302 <- F (after) (stepped + rewind)
		), Seq(0x200 -> 0x77)) { mem =>
			assertEquals(mem(0x301), 0x77, "the stepped load delivered its byte")
			assertEquals(mem(0x302), mem(0x300), "the flag byte survived the step untouched")
			assert(clue(mem(0x300)) != 0x77, "the compare really set something other than the loaded byte")
		}
	}

	// --- the hub rows: FT as the stepped pointer ----------------------------------------

	test("LD (FT+),r8 walks FT upward storing B, C, D, H, L") {
		// The read-after-write case of the form: each store lands at the address
		// FT held while the instruction began, even though the same instruction
		// installs FT+1 - the memory stage reads its address at the read stage.
		// The B and C cells ($22/$23) ride the narrow mask; D..L ($24-$27) the wide one.
		run(Seq(
			0x82, 0x01,		// ld  b,$01
			0x83, 0x00,		// ld  c,$00    BC = $0100
			0xD9,			// ld  ft,bc    FT = $0100
			0x82, 0x11,		// ld  b,$11
			0x84, 0xAA,		// ld  d,$AA
			0x86, 0xCC,		// ld  h,$CC
			0x87, 0xDD,		// ld  l,$DD
			0x22,			// ld  (ft+),b  $0100 <- $11, FT = $0101
			0x24,			// ld  (ft+),d  $0101 <- $AA, FT = $0102
			0x26,			// ld  (ft+),h  $0102 <- $CC, FT = $0103
			0x27,			// ld  (ft+),l  $0103 <- $DD, FT = $0104
			0x83, 0xEE,		// ld  c,$EE
			0x23,			// ld  (ft+),c  $0104 <- $EE, FT = $0105
			0x86, 0x03,		// ld  h,$03
			0x87, 0x00,		// ld  l,$00    HL = $0300 (dump window)
			0x1B,			// ld  (hl+),f  $0300 <- F of FT
			0x13			// ld  (hl+),t  $0301 <- T of FT
		)) { mem =>
			assertEquals(mem(0x100), 0x11, "first byte stored at the address FT held at the start")
			assertEquals(mem(0x101), 0xAA, "second byte one further")
			assertEquals(mem(0x102), 0xCC, "third byte one further still")
			assertEquals(mem(0x103), 0xDD, "fourth byte one further still")
			assertEquals(mem(0x104), 0xEE, "fifth byte - the walk really stepped")
			assertEquals(mem(0x300), 0x01, "FT ended at $0105: high byte")
			assertEquals(mem(0x301), 0x05, "FT ended at $0105: low byte")
		}
	}

	test("LD r8,(FT+) walks FT upward, delivering each byte through the auxiliary port") {
		// Six bytes through six different destination registers while FT is the
		// cursor: the loaded byte arrives on the auxiliary write port at r8 while
		// the main port steps FT.  Note the dumps all run *after* the walk: a
		// relay through T mid-walk would overwrite the low half of the cursor.
		run(Seq(
			0x82, 0x02,		// ld  b,$02
			0x83, 0x00,		// ld  c,$00    BC = $0200
			0xD9,			// ld  ft,bc    FT = $0200
			0x2A,			// ld  b,(ft+)  B = $11, FT = $0201
			0x2B,			// ld  c,(ft+)  C = $22, FT = $0202
			0x2C,			// ld  d,(ft+)  D = $33, FT = $0203
			0x2D,			// ld  e,(ft+)  E = $44, FT = $0204
			0x2E,			// ld  h,(ft+)  H = $55, FT = $0205
			0x2F,			// ld  l,(ft+)  L = $66, FT = $0206
			0x86, 0x03,		// ld  h,$03
			0x87, 0x00,		// ld  l,$00    HL = $0300 dump window (this overwrites the loaded H,L)
			0x1B, 0x13,		// ld (hl+),f ; ld (hl+),t  $0300/1 <- FT = $02/$06 - six steps
			0x5A, 0x13,		// ld  t,b ; ld (hl+),t   $0302 <- B
			0x5B, 0x13,		// ld  t,c ; ld (hl+),t   $0303 <- C
			0x5C, 0x13,		// ld  t,d ; ld (hl+),t   $0304 <- D
			0x5D, 0x13		// ld  t,e ; ld (hl+),t   $0305 <- E
		), Seq(0x200 -> 0x11, 0x201 -> 0x22, 0x202 -> 0x33, 0x203 -> 0x44, 0x204 -> 0x55, 0x205 -> 0x66)) { mem =>
			assertEquals(mem(0x300), 0x02, "FT ended at $0206: high byte")
			assertEquals(mem(0x301), 0x06, "FT ended at $0206: low byte")
			assertEquals(mem(0x302), 0x11, "first byte loaded at the address FT held at the start")
			assertEquals(mem(0x303), 0x22, "second byte one further")
			assertEquals(mem(0x304), 0x33, "third byte one further still, into D's high lane")
			assertEquals(mem(0x305), 0x44, "fourth byte one further still, into E's low lane")
		}
	}

	test("a stepped FT store keeps the entry at its new stack top") {
		// The stepped store writes FT as an ordinary pair write, so the FT stack
		// top follows the pointer: push/pop through the cell recovers the stepped
		// $0103, not the pre-step $0102.
		run(Seq(
			0x82, 0x01,		// ld  b,$01
			0x83, 0x02,		// ld  c,$02    BC = $0102
			0xD9,			// ld  ft,bc    FT = $0102
			0x84, 0xAA,		// ld  d,$AA
			0x24,			// ld  (ft+),d  $0102 <- $AA, FT = $0103
			0xC0,			// push ft      duplicate the stepped pointer
			0xC4,			// pop  ft      read it back out of the cell, FT = $0103
			0xD1,			// ld  bc,ft
			0x84, 0x03,		// ld  d,$03
			0x85, 0x00,		// ld  e,$00    DE = $0300 (dump window)
			0x5B,			// ld  t,c
			0x12,			// ld  (de+),t  $0300 <- C (stepped low byte)
			0x5A,			// ld  t,b
			0x12			// ld  (de+),t  $0301 <- B (stepped high byte)
		)) { mem =>
			assertEquals(mem(0x102), 0xAA, "byte stored through the stepped pointer")
			assertEquals(mem(0x300), 0x03, "stepped low byte reached the top cell")
			assertEquals(mem(0x301), 0x01, "stepped high byte reached the top cell")
		}
	}

	test("LD F/T,(FT) load hub bytes and leave the pointer alone") {
		// The two cells that cannot step, the r=0/r=1 exceptions of the hub load
		// row.  A hub load overlays one half of FT - the pointer value it
		// read through is gone either way - so the no-step contract is pinned
		// from the other half: T carries a $34 sentinel through the F load, and
		// a step would have left $35.
		run(Seq(
			0x82, 0x02,		// ld  b,$02
			0x83, 0x34,		// ld  c,$34
			0xD9,			// ld  ft,bc    FT = $0234
			0x29,			// ld  t,(ft)   T = $AB from $0234, F untouched ($29 since the lco-step swap)
			0x84, 0x03,		// ld  d,$03
			0x85, 0x00,		// ld  e,$00    DE = $0300 (dump window)
			0x1A,			// ld  (de+),f  $0300 <- F = $02 - the high half survived
			0x12,			// ld  (de+),t  $0301 <- T = $AB - the loaded byte
			0x82, 0x02,		// ld  b,$02
			0x83, 0x34,		// ld  c,$34
			0xD9,			// ld  ft,bc    FT = $0234 again, T = $34 sentinel
			0x28,			// ld  f,(ft)   F = $AB from $0234 ($28 since the lco-step swap)
			0x1A,			// ld  (de+),f  $0302 <- F = $AB - the loaded byte
			0x12			// ld  (de+),t  $0303 <- T = $34 - stepped, it would be $35
		), Seq(0x200 -> 0xAB, 0x234 -> 0xAB)) { mem =>
			assertEquals(mem(0x300), 0x02, "the F half survived the T load")
			assertEquals(mem(0x301), 0xAB, "T loaded its byte from the address FT held")
			assertEquals(mem(0x302), 0xAB, "F loaded its byte from the address FT held")
			assertEquals(mem(0x303), 0x34, "the T sentinel survived the F load - FT never stepped")
		}
	}

	test("stepping FT disturbs no flags") {
		// One stepped store and one stepped load on the hub pointer between two
		// dumps of F: the flag byte must survive exactly as the compare left it.
		// FT is anchored before the compare - LD FT,r16 writes F, the flag byte.
		run(Seq(
			0x82, 0x02,		// ld  b,$02
			0x83, 0x00,		// ld  c,$00
			0xD9,			// ld  ft,bc    FT = $0200
			0x81, 0x00,		// ld  t,$00
			0x84, 0x01,		// ld  d,$01
			0x4C,			// cmp t,d      flags = (0 - 1): borrow, nonzero
			0x86, 0x03,		// ld  h,$03
			0x87, 0x00,		// ld  l,$00    HL = $0300 (dump window)
			0x58,			// ld  t,f
			0x13,			// ld  (hl+),t  $0300 <- F (before); FT now carries the flags in its low half
			0x84, 0x41,		// ld  d,$41
			0x24,			// ld  (ft+),d  stepped store, wherever FT points
			0x2C,			// ld  d,(ft+)  stepped load of an untouched byte
			0x58,			// ld  t,f
			0x13			// ld  (hl+),t  $0301 <- F (after)
		), Seq(0x200 -> 0x77)) { mem =>
			assertEquals(mem(0x301), mem(0x300), "the flag byte survived the stepped store and load")
			assert(clue(mem(0x300)) != 0x00, "the compare really set something the zero-loaded byte could not mimic")
		}
	}

	/*
	 * The I/O rows ($31-$33 store, $35-$37 load) and the code-space loads
	 * ($0D-$0F) step like the $10-$1F block, and the hub loads sit at
	 * $28/$29.  These streams run the same core
	 * but route the bus by its qualifiers - the io pin to a device array, the
	 * code pin to a code array - so a byte arriving from the wrong space is
	 * a failure, not a coincidence: every shared address is deliberately
	 * filled with different bytes in each space.
	 */
	private def runRouted(program: Seq[Int], data: Seq[(Int, Int)] = Seq.empty,
			ioData: Seq[(Int, Int)] = Seq.empty, codeData: Seq[(Int, Int)] = Seq.empty)
			(check: (Array[Int], Array[Int], Array[Int]) => Unit): Unit = {
		compiled.doSim { dut =>
			val mem = Array.fill[Int](65536)(0)
			val ioMem = Array.fill[Int](65536)(0)
			val codeMem = Array.fill[Int](65536)(0)
			program.zipWithIndex.foreach { case (byte, i) => mem(i) = byte; codeMem(i) = byte }
			data.foreach { case (a, b) => mem(a) = b }
			ioData.foreach { case (a, b) => ioMem(a) = b }
			codeData.foreach { case (a, b) => codeMem(a) = b }
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
						val space =
							if (dut.io.io.toBoolean) ioMem
							else if (dut.io.code.toBoolean) codeMem
							else mem
						if (dut.io.write.toBoolean) space(address) = dut.io.dataOut.toInt
						else dut.io.dataIn #= space(address)
					}
				}
			}

			dut.clockDomain.waitSampling(400)

			check(mem, ioMem, codeMem)
		}
	}

	test("LIO (BC+),T walks the I/O window, and only the device array sees it") {
		runRouted(Seq(
			0x82, 0x40,		// ld  b,$40
			0x83, 0x00,		// ld  c,$00    BC = $4000
			0x81, 0xA1,		// ld  t,$A1
			0x31,			// lio (bc+),t  device $4000 <- $A1, BC = $4001
			0x81, 0xB2,		// ld  t,$B2
			0x31,			// lio (bc+),t  device $4001 <- $B2, BC = $4002
			0x3F			// nop
		)) { (mem, ioMem, _) =>
			assertEquals(ioMem(0x4000), 0xA1, "first byte stored into the device")
			assertEquals(ioMem(0x4001), 0xB2, "second byte stored one further down the window")
			assertEquals(ioMem(0x4002), 0x00, "the walk really stepped the pair")
			assertEquals(mem(0x4000), 0x00, "the store rode the io qualifier, not plain memory")
		}
	}

	test("LIO T,(BC+) walks the I/O window and loads only device-routed bytes") {
		runRouted(Seq(
			0x82, 0x50,		// ld  b,$50
			0x83, 0x00,		// ld  c,$00    BC = $5000
			0x86, 0x60,		// ld  h,$60
			0x87, 0x00,		// ld  l,$00    HL = $6000
			0x35,			// lio t,(bc+)  T <- device $5000 = $11, BC = $5001
			0x13,			// ld  (hl+),t  $6000 <- T
			0x35,			// lio t,(bc+)  T <- device $5001 = $22, BC = $5002
			0x13			// ld  (hl+),t  $6001 <- T
		), Seq(0x5000 -> 0xEE, 0x5001 -> 0xFF),	// plain memory at the same addresses: decoys
		Seq(0x5000 -> 0x11, 0x5001 -> 0x22)) { (mem, ioMem, _) =>
			assertEquals(mem(0x6000), 0x11, "first byte came from the device, not the decoy")
			assertEquals(mem(0x6001), 0x22, "second byte came one further down the window")
			assertEquals(ioMem(0x5000), 0x11, "the device byte survived the reads")
		}
	}

	test("LCO T,(DE+) walks code space and loads only code-routed bytes") {
		runRouted(Seq(
			0x84, 0x70,		// ld  d,$70
			0x85, 0x00,		// ld  e,$00    DE = $7000
			0x86, 0x60,		// ld  h,$60
			0x87, 0x00,		// ld  l,$00    HL = $6000
			0x0E,			// lco t,(de+)  T <- code $7000 = $AA, DE = $7001
			0x13,			// ld  (hl+),t
			0x0E,			// lco t,(de+)  T <- code $7001 = $BB, DE = $7002
			0x13			// ld  (hl+),t
		), Seq(0x7000 -> 0x55, 0x7001 -> 0x66),	// the same addresses in RAM hold different bytes
		Seq.empty, Seq(0x7000 -> 0xAA, 0x7001 -> 0xBB)) { (mem, _, codeMem) =>
			assertEquals(mem(0x6000), 0xAA, "first byte came from code space, not the RAM decoy")
			assertEquals(mem(0x6001), 0xBB, "second byte came one further along the table")
			assertEquals(codeMem(0x7000), 0xAA, "the code byte survived the reads")
		}
	}

	test("LIO T,(FT) and LCO T,(FT) deliver the byte but never step FT") {
		runRouted(Seq(
			0x82, 0x01,		// ld  b,$01
			0x83, 0x00,		// ld  c,$00
			0xD9,			// ld  ft,bc    FT = $0100
			0x86, 0x60,		// ld  h,$60
			0x87, 0x00,		// ld  l,$00    HL = $6000
			0x34,			// lio t,(ft)   T <- device $0100, FT stands still
			0x13,			// ld  (hl+),t
			0x34,			// lio t,(ft)   same device byte again - if FT stepped this differs
			0x13,			// ld  (hl+),t
			0x0C,			// lco t,(ft)   code $0100, FT still stands
			0x13,
			0x0C,			// lco t,(ft)   same code byte again
			0x13			// ld  (hl+),t
		), Seq.empty, Seq(0x0100 -> 0x00), Seq(0x0100 -> 0xCD, 0x01CD -> 0x5C)) { (mem, ioMem, codeMem) =>
			assertEquals(mem(0x6000), 0x00, "first I/O exception read")
			assertEquals(mem(0x6001), 0x00, "repeatable from the same FT - the cell does not step")
			assertEquals(mem(0x6002), 0xCD, "code exception read delivers through T...")
			assertEquals(mem(0x6003), 0x5C, "...and rides the pointer it loads (FT=$01CD, not $0101) - the byte IS the hub")
		}
	}

	test("the vacated $08 hub load traps to the illegal vector $18") {
		runRouted(Seq(
			0x08,			// ld  f,(ft) at $0000: vacated on the lco-step swap - must trap
			0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F,
			0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F,	// $0001-$0010
			0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F,		// $0011-$0017: handler sits at $0018
			0x82, 0x90,		// $0018 (trap vector): ld b,$90
			0x83, 0x00,		// ld  c,$00    BC = $9000
			0x81, 0xEE,		// ld  t,$EE
			0x11			// ld  (bc+),t   $9000 <- $EE: the vector was reached
		)) { (mem, _, _) =>
			assertEquals(mem(0x9000), 0xEE, "the illegal opcode jumped to the trap vector")
		}
	}
}
