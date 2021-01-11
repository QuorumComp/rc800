# Vectors

## Levels
There are four levels of interrupt states. User, System, Int and NMI. User mode is the usual mode for executing code. System mode is entered by the use of the [SYS](InstructionSYS.md) instruction. Int mode is entered when an external component asserts the INT line. And lastly NMI mode is entered when the NMI line is asserted.

Higher modes may interrupt a lower mode. NMI may interrupt Int, which can interrupt System, which can interrupt User. The RETI instruction will drop to previously active mode. A mode cannot interrupt itself.

Int mode can be disabled by the use of the [DI](InstructionDI.md) instruction.

## Reset, power-up
Upon power-up the PC will be set to $0000. Interupts will be disabled.

## Interrupts
When an interrupt is handled, the CPU will first perform a register HL push and then store the return PC in HL. PC will be set to according to the vectors below.

The RETI instruction will load the PC from HL, pop the stack and drop to the previously active interrupt mode.

## Vectors

The CPU will set the PC to these addresses when certain conditions arise.

| Address | Purpose |
|---------|---------|
| $0000   | Reset |
| $0008   | Non-maskable external interrupt |
| $0010   | Illegal interrupt (SYS used in SYS handler)| 
| $0018   | Illegal instruction |
| $0020   | Stack overflow |
| $0028 - $0037  | External interrupt |
| $0038 - $003F | Reserved | 
| $0040 - $07FF | SYS vectors |

# Continue reading
[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)
