package rc800

object Utils {
	def using[T <: AutoCloseable, R](v: T)(f: T => R): R = {
		try {
			f(v)
		} finally {
			v.close
		}
	}
}