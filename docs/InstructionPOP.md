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

## Assembler syntax examples
```
POP BC
```
