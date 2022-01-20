# Configuration registers
The CPU contains a set of configuration registers that control some aspects of the CPU, such as the stack. There is room for up to 256 8 bit registers, but only a few are currently defined.

| Index | Content |
|-------|---------|
| $00   | AF stack pointer |
| $01   | BC stack pointer |
| $02   | DE stack pointer |
| $03   | HL stack pointer |
| $04   | Stack lower bound |
| $05   | Stack upper bound |

# Continue reading
[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)

[Calling convention](StandardCallingConvention.md)
