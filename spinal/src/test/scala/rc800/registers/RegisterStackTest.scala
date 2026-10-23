package rc800.registers

import spinal.core._
import spinal.core.sim._

/*
 * Behavioural tests for the register stack primitive.
 *
 * The harness drives the block the way the pipeline does:
 *   - PUSH moves the pointer in the memory stage and duplicates the top in the
 *     writeback stage, using the new pointer;
 *   - POP and stack pointer writes are pure pointer moves;
 *   - SWAP and PICK act without moving the pointer.
 */
class RegisterStackTest extends munit.FunSuite {

	private val compiled = SimConfig.compile(new Register)

	private class Harness(dut: Register) {
		var sp: Int = 0xFF

		dut.clockDomain.forkStimulus(period = 10)
		idle()
		dut.io.pointer #= sp
		dut.io.pick #= 0
		dut.clockDomain.waitSampling(2)

		def idle(): Unit = {
			dut.io.control.push #= false
			dut.io.control.pop #= false
			dut.io.control.swap #= false
			dut.io.control.pick #= false
			dut.io.memReadControl.push #= false
			dut.io.memReadControl.pop #= false
			dut.io.memReadControl.swap #= false
			dut.io.memReadControl.pick #= false
			dut.io.write #= false
			dut.io.writeExg #= false
			dut.io.stackReload #= false
		}

		def top: Int = {
			sleep(1)                              // let the edge propagate before reading
			dut.io.dataOut.toInt
		}

		def writeTop(value: Int): Unit = {
			idle()
			dut.io.write #= true
			dut.io.dataIn #= value
			dut.clockDomain.waitSampling()
			idle()
		}

		def push(): Unit = {
			idle()
			sp = (sp - 1) & 0xFF
			dut.io.pointer #= sp
			dut.clockDomain.waitSampling()        // pointer moves in the memory stage
			idle()
			dut.io.control.push #= true          // duplicate in writeback
			dut.clockDomain.waitSampling()
			idle()
		}

		/** Interrupt/SYS push: the writeback stage pushes and writes at once. */
		def pushAndWrite(value: Int): Unit = {
			idle()
			sp = (sp - 1) & 0xFF
			dut.io.pointer #= sp
			dut.clockDomain.waitSampling()        // pointer moves in the memory stage
			idle()
			dut.io.control.push #= true
			dut.io.write #= true
			dut.io.dataIn #= value
			dut.clockDomain.waitSampling()        // the write wins over the duplicate
			idle()
		}

		/** The bank read is registered: present the address, then apply. */
		private def readThenApply(read: => Unit, apply: => Unit): Unit = {
			idle()
			read
			dut.clockDomain.waitSampling()        // address sampled, data latched
			idle()
			apply
			dut.clockDomain.waitSampling()        // value used by the writeback stage
			idle()
		}

		def pop(): Unit = {
			idle()
			sp = (sp + 1) & 0xFF
			dut.io.pointer #= sp
			readThenApply((), dut.io.control.pop #= true)
		}

		def setSp(newSp: Int): Unit = {
			idle()
			sp = newSp
			dut.io.pointer #= newSp
			readThenApply((), dut.io.stackReload #= true)
		}

		def swap(): Unit = {
			readThenApply(dut.io.memReadControl.swap #= true, dut.io.control.swap #= true)
		}

		def pick(depth: Int): Unit = {
			idle()
			dut.io.pick #= depth
			readThenApply(dut.io.memReadControl.pick #= true, dut.io.control.pick #= true)
			idle()
			dut.io.pick #= 0
		}
	}

	private def run(name: String)(body: Harness => Unit): Unit =
		test(name) {
			compiled.doSim { dut => body(new Harness(dut)) }
		}

	run("push duplicates the top and pop restores the entries") { h =>
		h.writeTop(0xAA)
		h.push()
		h.writeTop(0x55)
		h.push()
		assertEquals(h.top, 0x55)

		h.pop()
		assertEquals(h.top, 0x55)

		h.pop()
		assertEquals(h.top, 0xAA)
	}

	run("push and write in the same cycle keep the old top below") { h =>
		h.writeTop(0xAA)
		h.push()
		h.writeTop(0x55)

		h.pushAndWrite(0x77)
		assertEquals(h.top, 0x77)

		h.pop()
		assertEquals(h.top, 0x55)
	}

	run("swap exchanges the top with the entry below") { h =>
		h.writeTop(0xAA)
		h.push()
		h.writeTop(0x55)

		h.swap()
		assertEquals(h.top, 0xAA)

		h.swap()
		assertEquals(h.top, 0x55)
	}

	run("swap exchanges the top and the entry below at an odd pointer") { h =>
		h.setSp(0x40)
		h.writeTop(0x11)
		h.push()                              // sp = $3F, Stack[$3F] = $11
		h.writeTop(0x22)

		h.swap()                              // Stack[$3F] <-> Stack[$40]
		assertEquals(h.top, 0x11)

		h.swap()
		assertEquals(h.top, 0x22)
	}

	run("pick reads an entry by depth and 0 leaves the register unchanged") { h =>
		h.writeTop(0xAA)
		h.push()
		h.writeTop(0x55)
		h.push()

		h.pick(0)
		assertEquals(h.top, 0x55)

		h.pick(2)
		assertEquals(h.top, 0xAA)

		h.pick(0)
		assertEquals(h.top, 0xAA)
	}

	run("pick reads from the top bank and from the other bank") { h =>
		h.setSp(0x20)
		h.writeTop(0xA1)                      // Stack[$20]
		h.push()                              // sp = $1F, Stack[$1F] = $A1
		h.writeTop(0xB2)                      // Stack[$1F]

		h.setSp(0x21)
		h.writeTop(0xC3)                      // Stack[$21]
		h.setSp(0x1F)
		assertEquals(h.top, 0xB2)

		h.pick(2)                             // Stack[$21], same bank as the top
		assertEquals(h.top, 0xC3)

		h.pick(1)                             // Stack[$20], the other bank
		assertEquals(h.top, 0xA1)
	}

	run("a stack pointer write loads the entry at the new pointer") { h =>
		// Power-up contents are undefined, so seed the entries first.
		h.setSp(0x31)
		h.writeTop(0x11)
		h.push()                              // sp = $30, duplicate $11
		h.writeTop(0x22)                      // Stack[$30] = $22

		h.pop()                               // sp = $31, loads Stack[$31]
		assertEquals(h.top, 0x11)

		h.setSp(0x30)                         // loads Stack[$30]
		assertEquals(h.top, 0x22)
	}

	run("rewinding exposes previously used entries") { h =>
		h.writeTop(0xAA)
		h.push()
		h.writeTop(0x55)
		h.push()

		h.setSp(0xFE)
		assertEquals(h.top, 0x55)

		h.setSp(0xFF)
		assertEquals(h.top, 0xAA)
	}

	run("pick after a stack pointer write reads the new stack, not the old window") { h =>
		// Power-up contents are undefined, so seed both destination entries.
		h.setSp(0x11)
		h.writeTop(0x11)
		h.setSp(0x10)
		h.writeTop(0x10)

		h.setSp(0x00)                         // move away
		h.setSp(0x10)                         // new window
		assertEquals(h.top, 0x10)

		h.pick(1)                             // Stack[$11], not the old window
		assertEquals(h.top, 0x11)
	}

	run("push after a stack pointer write materialises the vacated slot") { h =>
		h.writeTop(0xAA)
		h.setSp(0x50)
		// The entry at the new pointer is undefined until written.

		h.writeTop(0x77)
		h.push()
		h.pick(1)
		assertEquals(h.top, 0x77)

		h.pop()
		assertEquals(h.top, 0x77)
	}

	run("a value written through a push is readable after rewinding to it") { h =>
		h.setSp(0x30)
		h.writeTop(0x5A)
		h.push()                              // duplicates the top into Stack[$2F]
		assertEquals(h.top, 0x5A)

		h.setSp(0x31)
		h.setSp(0x30)
		assertEquals(h.top, 0x5A)
	}

	run("push and pop wrap around the stack") { h =>
		h.setSp(0x00)
		h.writeTop(0x33)

		h.push()                              // sp = $FF, Stack[$FF] = $33
		assertEquals(h.top, 0x33)

		h.pop()
		assertEquals(h.top, 0x33)
	}
}

