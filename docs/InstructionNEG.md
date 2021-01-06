# NEG (8 bit two's complement)

## Opcode
| 76543210 |
|----------|
| 01010001 |

## Operation
```
T <- -T
PC <- PC + 1
```

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
```
FT <- -FT
PC <- PC + 1
```

## Assembler syntax examples
```
NEG FT
```
