package rc800.registers

import spinal.core._
import spinal.lib._

import rc800.control.component.RegisterControl


/*
 * One 8 bit half of a register stack.
 *
 * The stack is two 128 entry banks selected by the low bit of the stack
 * pointer and addressed by the rest: bank[parity][index >> 1] holds Stack[index].
 * The banks are block RAM, so their read port is registered; `top` is a
 * flip-flop copy of the entry the pointer selects, reloaded whenever the
 * pointer moves.
 *
 * The memory always holds the whole stack: the top cell is written through on
 * every change of the register. A pointer move therefore needs no repair write,
 * and a stack pointer write only has to reload the register from the new cell.
 *
 * PUSH uses the same write-through. Its writeback control arrives after the
 * pointer has moved down, so the new top cell receives the register's current
 * value; when the instruction also writes the register, that is already the
 * final value.
 *
 * Adjacent entries live in different banks, so SWAP writes the top and the
 * entry below in the same cycle, one write per bank.
 *
 * The RAM is not initialised: the ISA does not define the power-up contents,
 * so software must write every entry it reads. The register's init is only a
 * power-on value; a pointer move loads whatever the RAM holds.
 */
case class Register() extends Component {
	val io = new Bundle {
		val dataIn    = in (Bits(8 bits))
		val dataInExg = in (Bits(8 bits))
		val control   = in (RegisterControl())
		val write     = in (Bool())
		val writeExg  = in (Bool())

		val pick      = in (UInt(8 bits))
		val pointer   = in (UInt(8 bits))

		// Read address for the cycle whose data is used by the writeback
		// stage, one stage later.
		val memReadControl = in (RegisterControl())

		// A stack pointer write in the previous stage: reload the top.
		val stackReload = in (Bool())

		val dataOut   = out (Bits(8 bits))
	}

	private val depth = 128
	private val even = Mem(Bits(8 bits), depth)
	private val odd  = Mem(Bits(8 bits), depth)
	
	private val pointerNext = io.pointer + 1
	private val pointerPick = io.pointer + io.pick
	private val topBank    = io.pointer(0)
	private val topIndex   = io.pointer(7 downto 1)
	private val belowIndex = pointerNext(7 downto 1)

	// ----------------------------------------------------------------------
	// The entry the writeback stage needs: the new top after a pointer move,
	// the entry below for a swap, the picked entry for a pick. PICK counts
	// from the pointer, so the index is added to it.

	private val readAddress = Select(
		io.memReadControl.swap -> pointerNext,
		io.memReadControl.pick -> pointerPick,
		default                -> io.pointer
	)

	// ----------------------------------------------------------------------
	// Read data
	// ----------------------------------------------------------------------

	private val evenReadData = even.readSync(readAddress(7 downto 1))
	private val oddReadData  = odd.readSync(readAddress(7 downto 1))

	// The data is one cycle behind the address, so the bank select has to be
	// delayed by the same amount.
	private val readOddBank = RegNext(readAddress(0))
	private val readValue = readOddBank ? oddReadData | evenReadData

	// Power-on value of the register only; a pointer move loads the RAM.
	private val top = Reg(Bits(8 bits)) init(0)
	io.dataOut := top

	// ----------------------------------------------------------------------
	// Write data
	// ----------------------------------------------------------------------

	// One shared write port: a plain write takes dataIn; EXG writes the same
	// value to both exchanged registers, so it takes dataInExg.
	private val writeValue = io.write ? io.dataIn | io.dataInExg

	// PICK 0 leaves the register alone, so there is nothing to load.
	private val pickLoad = io.control.pick && (io.pick =/= 0)
	private val loadTop  = io.control.pop || io.control.swap || io.stackReload || pickLoad

	// A write wins over a reload: when an instruction both moves the pointer
	// and writes, the written value is the final one.
	private val topNext = Select(
		(io.write || io.writeExg) -> writeValue,
		loadTop                   -> readValue,
		default                   -> top
	)
	top := topNext

	// The top cell follows the register. PUSH uses the same write: its
	// writeback control arrives after the pointer has moved, so the new top
	// cell receives the register's current value.
	val topWrite  = io.write || io.writeExg || io.control.push ||
	                io.control.swap || pickLoad
	val swapWrite = io.control.swap

	// A top write targets the bank selected by the pointer; SWAP's second
	// write targets the other bank and carries the old top.

	even.write(
		address = topBank ? belowIndex | topIndex,
		data    = topBank ? top        | topNext,
		enable  = topBank ? swapWrite  | topWrite)
	odd.write(
		address = topBank ? topIndex   | belowIndex,
		data    = topBank ? topNext    | top,
		enable  = topBank ? topWrite   | swapWrite)
}
