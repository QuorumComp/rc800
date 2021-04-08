package rc800.registers

import spinal.core._
import spinal.lib._


object WritePart extends SpinalEnum(defaultEncoding = binarySequential) {
	val none, low, high, full = newElement()
}

object Register extends SpinalEnum(defaultEncoding = binarySequential) {
	val ft, bc, de, hl = newElement()
}

object RegisterFileOperation extends SpinalEnum(defaultEncoding = binarySequential) {
	// organize values so read, popAll, swapAll and pushAll
	// share the same lower 3 bit patten as pop, swap and push 
	val read, pop, swap, _dummy1,
		_dummy2, write, push, pushValue ,
		_dummy3, popAll, swapAll, exchangeFT,
		exchangeT, _dummy4, pushAll, _dummy5  = newElement()

	def isSingleRegisterOperation(op: RegisterFileOperation.C): Bool =
		!op.asBits(3)

	def isExchangeOperation(op: RegisterFileOperation.C): Bool =
		(op === exchangeFT) || (op === exchangeT)

	def asRegisterOperation(op: RegisterFileOperation.C): RegisterOperation.C =
		op.asBits(2 downto 0).as(RegisterOperation())	
}


class RegisterFile extends Component {
	val io = new Bundle {
		val readRegisters = in Vec(Register(), 2)
		val dataOut = out Vec(UInt(16 bits), 2)

		val pointerFT = in UInt(8 bits)
		val pointerBC = in UInt(8 bits)
		val pointerDE = in UInt(8 bits)
		val pointerHL = in UInt(8 bits)

		val writeRegister  = in (Register())
		val writePart      = in (WritePart())
		val writeOperation = in (RegisterFileOperation)
		val dataIn         = in UInt(16 bits)
		val dataInExg      = in UInt(16 bits)
	}

	val ftStack, bcStack, deStack, hlStack = new Stack()

	val isExgR8Operation = (io.writeOperation === RegisterFileOperation.exchangeT)
	val isExgR16Operation = (io.writeOperation === RegisterFileOperation.exchangeFT)
	val isExgOperation = isExgR8Operation || isExgR16Operation

	val writeMask = io.writePart.asBits
	val writeData = (io.writePart === WritePart.low) ? io.dataIn(15 downto 8) | io.dataIn
	val exgDataToFT =
		isExgR8Operation ?
			(io.dataIn(15 downto 8) ## io.dataInExg(15 downto 8)).asUInt |
			io.dataInExg
	val exgMaskToFT = (isExgR8Operation && io.writeRegister =/= Register.ft) ? B"01" | B"11"

	def wireStack(register: Register.E, stack: Stack, pointer: UInt): Unit = {
		val isSelected = io.writeRegister === register
		val exchangeFT = (isExgOperation && register === Register.ft)
		val exchangeOp = ((exchangeFT || isSelected) ? RegisterOperation.write | RegisterOperation.read)
		val operation = io.writeOperation

		when (RegisterFileOperation.isSingleRegisterOperation(operation)) {
			stack.io.operation := isSelected ? RegisterFileOperation.asRegisterOperation(operation) | RegisterOperation.read
		} elsewhen (RegisterFileOperation.isExchangeOperation(operation)) {
			stack.io.operation := exchangeOp
		} otherwise {
			stack.io.operation := RegisterFileOperation.asRegisterOperation(operation)
		}

		stack.io.writeData := exchangeFT ? exgDataToFT | writeData
		stack.io.writeMask := exchangeFT ? exgMaskToFT | writeMask
		stack.io.pointer := pointer
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
