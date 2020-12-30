# Introduction to the RC800 family

The RC800 family is an experiment that explores what a hypothetical 8 bit CPU introduced in the mid to late 1980's might look like, if certain dogmas such as FWIW and RISC techniques had been applied. 

It is inspired by several existing architectures, such as the Z80, MIPS, and 68000.

Internally it has eight 8 bit registers. These are named ```F``` (for ```F```lag), ```T``` (```T```emporary), ```B```, ```C```, ```D```, ```E```, ```H```, and ```L```. They can also be combined to form four 16 bit register - , ```FT```, ```BC```, ```DE```, and ```HL```. There's an extensive set of instructions that perform 16 bit operations, such as comparisons, shifts and addition. There's also a program counter, interrupt enable flag, stack pointers and a set of configuration registers.

The architecture features on-chip stacks - one per each register pair. The register pair can be viewed as a window into the stack - when a register is pushed onto its stack, the top value is duplicated and the pointer decremented. Because the stacks are independent, all registers can be pushed at once, enabling very fast interrupt handling.

The ISA has been designed so it will fit into a classic four stage RISC pipeline. As the data bus is 8 bits wide, this means the CPU is only able to move one 8 bit value (two, if you count the opcode) to or from memory per instruction. Thus the architecture is neither little or big endian - the system designer or programmer gets to choose which endianness is the most appropriate.

# Continue reading
[Introduction](docs/Introduction.md)
[Opcode matrix](docs/OpcodeMatrix.md)
[Alphabetical list of mnemonics](docs/AlphabeticalMnemonics.md)
[Configuration registers](docs/ConfigurationRegisters.md)
[Interrupts](docs/Interrupts.md)
