# RETI (Return from interrupt)

## Opcode
| 76543210 |
|----------|    
| 01011001 |

## Operation
```
PC <- HL
Pop register HL stack
Resume previous interrupt mode
```

## Assembler syntax examples
```
RETI
```
