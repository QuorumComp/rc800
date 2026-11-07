package rc800.lpm.generic

import spinal.core._
import spinal.lib._

import rc800.lpm


/** Read-only memory inferred from its contents: portable, fully simulatable,
  * and the fallback wherever a vendor ROM primitive is not wanted.  The mapper
  * decides whether it becomes block memory, distributed memory or logic -
  * contents loaded by `$readmemb` are not read-only memory to it, which is why
  * a build that wants a guarantee instantiates a primitive instead. */
class Rom(dataWidth: Int, content: Seq[BigInt])
	extends Component
	with lpm.Rom {

	override def width = dataWidth
	override def depth = content.length
	override def init  = content

	require(isPow2(depth), "ROM depth must be a power of two")

	private val signalMem = Mem(init.map(value => B(value, width bits)))

	io.data := signalMem.readSync(io.address, io.enable)
}
