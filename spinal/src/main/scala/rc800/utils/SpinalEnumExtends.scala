package rc800.utils

import spinal.core._
import spinal.lib._


class SpinalEnumExtends[T <: SpinalEnum](val base: T) extends SpinalEnum(base.defaultEncoding) {
	base.elements.foreach(e => newElement(s"base_${e.position}_"))

	def asBase(e: C): base.C = e.asBits.resize(base.defaultEncoding.getWidth(base)).as(base())
	def fromBase(e: base.C): this.C = e.asBits.resized.as(this())
	def fromBase(e: base.E): this.E = elements(e.position)
}
