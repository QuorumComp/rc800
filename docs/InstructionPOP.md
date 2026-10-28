# POP (Pop register stack)

## Opcode
| 765432 | 10 |
|--------|----|    
| 110001 | r  |

## Operation
```
Pop register stack Reg16[r]
PC <- PC + 1
```

There is no underflow fault: a pop is never reported against the empty
stack. If the selected pair's stack pointer is $FF (empty), the pointer
increments and wraps to $00, and the register loads entry $00 - the
deepest cell, undefined after power-up. The stack then looks empty
(depth $FF - SP = 0) but the next PUSH faults as a stack overflow at
$0020, far from the pop that caused it. See
[Interrupts](Interrupts.md#faults).

## Assembler syntax examples
```
POP BC
```
