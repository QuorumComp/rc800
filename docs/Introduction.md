# Introduction to the RC800 family

The RC800 family is an experiment that explores what a hypothetical 8 bit CPU introduced in the mid to late 1980's might look like, if certain dogmas such as FWIW and RISC techniques had been applied. 

It is inspired by several existing architectures, such as the Z80, MIPS, and 68000.

Internally it has eight 8 bit registers. These are named ```F``` (for ```F```lag), ```T``` (```T```emporary), ```B```, ```C```, ```D```, ```E```, ```H```, and ```L```. They can also be combined to form four 16 bit register - , ```FT```, ```BC```, ```DE```, and ```HL```. There's an extensive set of instructions that perform 16 bit operations, such as comparisons, shifts and addition. There's also a program counter, interrupt enable flag, stack pointers and a set of configuration registers.

The architecture features on-chip stacks - one per each register pair. The register pair can be viewed as a window into the stack - when a register is pushed onto its stack, the top value is duplicated and the pointer decremented. Because the stacks are independent, all registers can be pushed at once, enabling very fast interrupt handling.

The ISA has been designed so it will fit into a four stage RISC-like pipeline. As the data bus is 8 bits wide, this means the CPU is only able to move one 8 bit value (two, if you count the opcode) to or from memory per instruction. Thus the architecture is neither little or big endian - the system designer or programmer gets to choose which endianness is the most appropriate.

## Opcodes
All opcodes are 8 bits wide. There's a few instructions that may initially appear to be two bytes wide, but the architecture implements them as loading the operand from memory in the memory stage - it just happens to do so at the next PC location.

## Input/Output
I/O is supported through specialized port instructions (LIO), but memory mapped I/O may of course also be utilized.

## Subroutines
The JAL instruction is used with the J instruction to form subroutines. The JAL instructions copy the return address into register HL - they do not automatically perform a push first.

# Microarchitectures

## Naming scheme
The 8 bit family follows the naming scheme RC8```xy```, where ```x``` is the microarchitecture implementation or revision, and ```y``` is the ISA level.

## Implementation 1
The first implementation utilizes a classic RISC fixed four stage pipeline. However, the design is *not* pipelined. All instructions complete in 4 clock cycles.

All stacks are 256 16 bit words deep.

The address bus is 16 bits wide. However, the CPU also provides code/data/sys signals, with which the system designer can choose to implement a Harvard architecture.

The RC811 is implementation 1.
