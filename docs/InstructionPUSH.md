# PUSH (Push to register stack)

## Opcode
| 765432 | 10 |
|--------|----|    
| 110000 | r  |

## Operation
```
Push Reg16[r] to register stack
PC <- PC + 1
```

If the selected pair's stack pointer is $00, the stack overflows: the push is
discarded and the [Interrupts](Interrupts.md) fault vector $0020 is taken
instead. See [Faults](Interrupts.md) for the entry semantics and for the case
where the fault is suppressed.

## Assembler syntax examples
```
PUSH BC
```
