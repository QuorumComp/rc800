# Configuration registers
The CPU contains a set of configuration registers that control some aspects of the CPU, such as the stack. There is room for up to 256 8 bit registers, but only a few are currently defined.

| Index | Content |
|-------|---------|
| $00   | FT stack pointer |
| $01   | BC stack pointer |
| $02   | DE stack pointer |
| $03   | HL stack pointer |
| $04   | Stack lower bound |
| $05   | Stack upper bound |

Writing a stack pointer register moves the window: the register pair becomes the
entry at the new stack pointer, so it does not keep its previous value. An entry
the pointer has never reached reads as undefined: the stack RAM is not
initialised at power-up, so there is no defined startup value. See
[LCR](InstructionLCR.md).

# Continue reading
[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)

[Calling convention](StandardCallingConvention.md)
