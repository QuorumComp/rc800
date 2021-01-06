# Introduction to the RC800 family

The RC800 family is an experiment that explores what a hypothetical 8 bit CPU introduced in the mid to late 1980's might look like, if certain dogmas such as FWIW and RISC techniques had been applied. 

It is inspired by several existing architectures, such as the Z80, MIPS, and 68000.

The ISA has been designed so it will fit into a four stage RISC-like pipeline. The stages are (decode/register read) -> (memory) -> (alu/pc) -> (fetch/register write). Traditionally a RISC processor will have the ALU stage before the memory stage, in order to use more advanced addressing modes. The RC800 architecture benefits from having them reversed, as opcodes are eight bits wide and cannot carry any constant data. A trailing byte of data can instead be accessed in the memory stage and passed on the ALU, enabling many common operations such as ```ADD T,42```

## Opcodes
All opcodes are 8 bits wide. A few instructions may initially appear to be two bytes wide, but the architecture implements them as loading the second byte from memory in the memory stage, it just happens to do so at the next PC location.

Any opcodes that have a second byte has the form ```10------```. This means an instruction prefetcher is as trivial to implement as it is on a classic FWIW architecture, and the instruction is completely decoded just by inspecting the first byte. Thus, the architecture enjoys the usual FWIW benefits, and any discussion as to whether the RC800 ISA is truly FWIW is largely academic.

Instruction encoding space is severely limited by the 8 bits available. Some of the luxuries usually available to RISC architectures are therefore impossible, such as orthogonal addressing modes and operations. RC800 is a load/store-architecture but the available operand types and operations are not completely orthogonal. To get the most out of the scarce encoding space, a more pragmatic approach has been taken instead of following fixed dogmas. However, there is plenty of symmetry in the opcode layout a decoder can utilize.

## Register file
Internally there are eight 8 bit registers. These are named ```F``` (for ```F```lag), ```T``` (```T```emporary), ```B```, ```C```, ```D```, ```E```, ```H```, and ```L```. They can also be combined to form four 16 bit registers, ```FT```, ```BC```, ```DE```, and ```HL```. There's an extensive set of instructions that perform 16 bit operations, such as comparisons, shifts and addition. There's also a program counter, interrupt enable flag, stack pointers and a set of configuration registers.

The T register is often called the accumulator on other architectures. The result of comparison operations are written to the F register, but it is otherwise a normal register available to the programmer.

The architecture features on-chip stacks - one per each register pair. The register pair can be viewed as a window into the stack - when a register is pushed onto its stack, the top value is duplicated and the pointer decremented. Because the stacks are independent, all registers can be pushed at once, enabling very fast interrupt response.

## Memory and I/O
The architecture supports a 16 bit wide address bus and an 8 bit wide data bus, for a total of 64 KiB.

A few signals are also asserted depending on the type of access, these can be used to expand the address space to 384 KiB.

One signal is CODE, which is asserted when the CPU is loading data that will be executed as code, or when the special LCO instruction is used. The hardware designer may choose to use the CODE signal to implement something resembling a Harvard architecture, with separate code and data segments.

Another signal is IO, which is asserted when the special LIO instruction is used. This can be used to implement a special I/O bus in its own address space. Memory mapped I/O may of course also be utilized.

The last signal is SYS, which is asserted when handling an interrupt or using the special ```SYS``` instruction. This can be a way to hide the operating system from a user program.

## Subroutines
The JAL instruction is used with the J instruction to form subroutines. The JAL instructions copy the return address into register HL, they do not automatically perform a push first. A non-leaf subroutine will usually save and restore the HL register itself. To return from a subroutine, the ```J (HL)``` instruction is commonly used.

## Endianness
As the data bus is 8 bits wide, this means the CPU is only able to move one 8 bit value (two, if you count the opcode) to or from memory per instruction. Thus the architecture is neither little or big endian - the system designer or programmer gets to choose which endianness is the most appropriate.

# Microarchitectures

## Naming scheme
The 8 bit family follows the naming scheme RC8```xy```, where ```x``` is the microarchitecture implementation or revision, and ```y``` is the ISA level.

## Implementation 1
The first implementation utilizes a fixed four stage RISC pipeline. However, the design is *not* pipelined. All instructions complete in 4 clock cycles.

All stacks are 256 16 bit words deep.

# Continue reading
[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)
