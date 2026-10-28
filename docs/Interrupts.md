# Vectors

## Levels
There are four levels of interrupt states. User, System, Int and NMI. User mode is the usual mode for executing code. System mode is entered by the use of the [SYS](InstructionSYS.md) instruction. Int mode is entered when an external component asserts the INT line. And lastly NMI mode is entered when the NMI line is asserted.

Higher modes may interrupt a lower mode. NMI may interrupt Int, which can interrupt System, which can interrupt User. The RETI instruction will drop to previously active mode. A mode cannot interrupt itself.

Int mode can be disabled by the use of the [DI](InstructionDI.md) instruction.

## Reset, power-up
Upon power-up the PC will be set to $0000. Interrupts will be disabled.

## Interrupts
When an interrupt is handled, the CPU will first perform a register HL push and then store the return PC in HL. PC will be set to according to the vectors below.

The RETI instruction will load the PC from HL, pop the stack and drop to the previously active interrupt mode.

## Faults

The illegal instruction ($0018) and stack overflow ($0020) vectors are *faults*: they enter NMI mode, like the non-maskable interrupt itself. Their entry follows the universal cancel-and-restart semantics: HL receives the cancelled instruction's own address, and RETI re-executes it. The overflowing push is discarded, not executed, so a stack-overflow handler must free stack cells before returning - otherwise the retry faults again.

Faults never nest. While the NMI service runs - including a handler entered through $0018 or $0020 - both faults are suppressed: an illegal opcode is not reported and its effect is undefined, and a push against a full stack (pointer $00) is not reported and wraps the pointer silently to $FF. A fault raised inside the NMI service is undefined; the NMI service itself must be fault-free.

Stack underflow has no fault at all, at any level: POP, POPA, SWAP, SWAPA, PICK and STOW perform no bounds check. A pop against the empty stack (pointer $FF) wraps the pointer to $00 and loads entry $00 - the deepest cell, undefined after power-up - leaving a stack that looks empty but faults on its next push. An overflow fault on a pair that should be nowhere near full is therefore usually a pop-accounting bug, not a depth problem.

## Vectors

The CPU will set the PC to these addresses when certain conditions arise. These are not jump vectors — the CPU sets PC directly to the address, and the code placed there decides what to do (typically jumping to the actual handler).

| Address | Constant | Purpose |
|---------|----------|---------|
| $0000   | `RC8_VECTOR_RESET` | Reset (power-up) |
| $0008   | `RC8_VECTOR_NMI` | Non-maskable external interrupt |
| $0010   | `RC8_VECTOR_ILLEGAL_IRQ` | Illegal interrupt (`SYS` used in SYS handler) |
| $0018   | `RC8_VECTOR_ILLEGAL_OP` | Illegal instruction |
| $0020   | `RC8_VECTOR_STACK_OVFL` | Stack overflow |
| $0028   | `RC8_VECTOR_EXT_INT` | External interrupt |
| $0038 - $003F | — | Reserved |
| $0040 - $07FF | — | SYS entry points (vector byte × 8) |

# Continue reading

[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)

[Calling convention](StandardCallingConvention.md)
