# EXG (8 bit, exchange register contents)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 00010 | r   | Not valid when r = 1 |

## Operation
```
Temp <- T
T <- Reg8[r]
Reg8[r] <- Temp
PC <- PC + 1
```

## Assembler syntax examples
```
EXG T,D
EXG D
```

---
# EXG (16 bit, exchange register contents)

## Opcode
| 765432 | 10 | Notes |
|--------|----|-------|
| 110010 | r  | Not valid when r = 0 |

## Operation
```
Temp <- FT
FT <- Reg16[r]
Reg16[r] <- Temp
PC <- PC + 1
```

## Assembler syntax examples
```
EXG FT,BC
EXG HL
```
