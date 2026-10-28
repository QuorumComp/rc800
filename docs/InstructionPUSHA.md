# PUSHA (Push to register register stack)

## Opcode
| 76543210 |
|----------|    
| 11111000 |

## Operation
```
Push FT, BC, DE, HL register stacks
PC <- PC + 1
```

If any pair's stack pointer is $00, the stack overflows: the whole pusha is
discarded and the [Interrupts](Interrupts.md) fault vector $0020 is taken
instead. See [Faults](Interrupts.md) for the entry semantics and for the case
where the fault is suppressed.

## Assembler syntax examples
```
PUSHA
```
