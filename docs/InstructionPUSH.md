# PUSH (Push to register stack)

## Opcode
| 765432 | 10 |
|--------|----|    
| 110000 | r  |

## Operation
1. Push Reg16[r] to register stack
2. PC <- PC + 1

## Assembler syntax examples
```
PUSH BC
```
