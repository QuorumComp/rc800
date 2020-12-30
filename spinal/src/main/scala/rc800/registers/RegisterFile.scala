package rc800.registers

import spinal.core._
import spinal.lib._


object WritePart extends SpinalEnum {
	val low, high, full = newElement()
}

object RegisterOperation extends SpinalEnum {
	val read, write, push, pop, pushValue, writePointer = newElement()
}

object Register extends SpinalEnum {
	val ft, bc, de, hl = newElement()
}

object StackOperation extends SpinalEnum {
	val read, write, push, pop, pushAll, popAll, pushValue, writePointer, exchangeT, exchangeFT = newElement()
}


class RegisterFile extends Component {
	val io = new Bundle {
		val readRegisters = in Vec(Register(), 2)
		val dataOut = out Vec(UInt(16 bits), 2)

		val pointerFT = out UInt(8 bits)
		val pointerBC = out UInt(8 bits)
		val pointerDE = out UInt(8 bits)
		val pointerHL = out UInt(8 bits)

		val writeRegister  = in (Register())
		val writePart      = in (WritePart())
		val writeOperation = in (StackOperation())
		val dataIn         = in UInt(16 bits)
		val dataInExg      = in UInt(16 bits)
	}

	val ftStack, bcStack, deStack, hlStack = new Stack()

	val isExgR8Operation = (io.writeOperation === StackOperation.exchangeT)
	val isExgR16Operation = (io.writeOperation === StackOperation.exchangeFT)
	val isExgOperation = isExgR8Operation || isExgR16Operation

	val writeMask = io.writePart.mux (
		WritePart.low  -> B"16'h00FF",
		WritePart.high -> B"16'hFF00",
		WritePart.full -> B"16'hFFFF"
	)

	val writeData = (io.writePart === WritePart.low) ? io.dataIn(15 downto 8) | io.dataIn
	val exgDataToFT =
		isExgR8Operation ?
			(io.dataIn(15 downto 8) ## io.dataInExg(15 downto 8)).asUInt |
			io.dataInExg
	val exgMaskToFT = (isExgR8Operation && io.writeRegister =/= Register.ft) ? B"16'h00FF" | B"16'hFFFF"

	def wireStack(register: Register.E, stack: Stack, pointer: UInt): Unit = {
		val isSelected = io.writeRegister === register
		val exchangeFT = (isExgOperation && register === Register.ft)
		val exchangeOp = ((exchangeFT || isSelected) ? RegisterOperation.write | RegisterOperation.read)
		stack.io.operation := io.writeOperation.mux[RegisterOperation.C](
			StackOperation.read         -> (RegisterOperation.read),
			StackOperation.write        -> (isSelected ? RegisterOperation.write | RegisterOperation.read),
			StackOperation.push         -> (isSelected ? RegisterOperation.push  | RegisterOperation.read),
			StackOperation.pop          -> (isSelected ? RegisterOperation.pop   | RegisterOperation.read),
			StackOperation.pushAll      -> (RegisterOperation.push),
			StackOperation.popAll       -> (RegisterOperation.pop),
			StackOperation.pushValue    -> (isSelected ? RegisterOperation.pushValue | RegisterOperation.read),
			StackOperation.writePointer -> (isSelected ? RegisterOperation.writePointer | RegisterOperation.read),
			StackOperation.exchangeT    -> exchangeOp,
			StackOperation.exchangeFT   -> exchangeOp
		)

		stack.io.writeData := exchangeFT ? exgDataToFT | writeData
		stack.io.writeMask := exchangeFT ? exgMaskToFT | writeMask
		pointer := stack.io.pointer
	}

	wireStack(Register.ft, ftStack, io.pointerFT)
	wireStack(Register.bc, bcStack, io.pointerBC)
	wireStack(Register.de, deStack, io.pointerDE)
	wireStack(Register.hl, hlStack, io.pointerHL)

	def currentValue(r: Register.C) =
		r.mux(
			Register.ft -> ftStack.io.dataOut,
			Register.bc -> bcStack.io.dataOut,
			Register.de -> deStack.io.dataOut,
			Register.hl -> hlStack.io.dataOut
		)

	for (i <- 0 to 1) {
		io.dataOut(i) := currentValue(io.readRegisters(i))
	}
}
