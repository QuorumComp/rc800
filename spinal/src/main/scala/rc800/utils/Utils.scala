package rc800

package object utils {
	def using[T <: AutoCloseable, R](v: T)(f: T => R): R = {
		try {
			f(v)
		} finally {
			v.close
		}
	}
}
