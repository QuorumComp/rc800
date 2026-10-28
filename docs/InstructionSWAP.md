# SWAP (Swap two topmost register stack entries)

## Opcode
| 765432 | 10 |
|--------|----|    
| 110111 | r  |

## Operation
```
Swap the two topmost stack entries (Reg16[r] with the most recently pushed value)
PC <- PC + 1
```

No bounds check is performed. The entry below the top is `Stack[SP+1]` computed
modulo 256: at SP `$FF` (empty) the swap exchanges entries `$FF` and `$00`,
and at SP `$00` it exchanges `$00` and `$01` without the overflow fault a
push there would take. See [Interrupts](Interrupts.md#faults).

## Assembler syntax examples
```
SWAP BC
```
