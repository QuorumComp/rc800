# EXG (8 bit, exchange register contents)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 00010 | r   | Not valid when r = 1 |

## Operation
1. Temp <- T
2. T <- Reg8[r]
3. Reg8[r] <- Temp
4. PC <- PC + 1

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
1. Temp <- FT
2. FT <- Reg16[r]
3. Reg16[r] <- Temp
4. PC <- PC + 1

## Assembler syntax examples
```
EXG FT,BC
EXG HL
```
