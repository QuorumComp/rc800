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

## Assembler syntax examples
```
PUSH BC
```
