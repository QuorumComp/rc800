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

## Assembler syntax examples
```
SWAP BC
```
