# NEG (8 bit two's complement)

## Opcode
| 76543210 |
|----------|
| 01010001 |

## Operation
1. T <- -T
2. PC <- PC + 1

## Assembler syntax examples
```
NEG T
```

---
# NEG (16 bit two's complement)

## Opcode
| 76543210 |
|----------|
| 11110000 |

## Operation
1. FT <- -FT
2. PC <- PC + 1

## Assembler syntax examples
```
NEG FT
```
