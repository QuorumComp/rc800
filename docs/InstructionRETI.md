# RETI (Return from interrupt)

## Opcode
| 76543210 |
|----------|    
| 01011001 |

## Operation
1. PC <- HL
2. Pop register HL stack
3. Resume previous interrupt mode

## Assembler syntax examples
```
RETI
```
