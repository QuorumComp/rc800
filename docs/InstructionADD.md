# ADD (8 bit, register to register)

## Opcode
| 76543 | 210 |
|-------|-----|
| 01000 | r   |

## Operation
1. T <- T + Reg8[r]
2. PC <- PC + 1

## Assembler syntax examples
```
ADD T,T
ADD H
```

---
# ADD (8 bit, immediate to register)

## Opcode
| 76543 | 210 |
|-------|-----|
| 10100 | r   |

## Operation
```
PC <- PC + 1
Reg8[r] <- Reg8[r] + Code[PC]
PC <- PC + 1
```

## Assembler syntax examples
```
ADD T,42
ADD H,-87
```

---
# ADD (16 bit, register to register)

## Opcode
| 765432 | 10 |
|--------|----|
| 111101 | r  |

## Operation
```
FT <- FT + Reg16[r]
PC <- PC + 1
```

## Assembler syntax examples
```
ADD FT,FT
ADD HL
```
---
# ADD (16 bit, immediate to register)

## Opcode
| 765432 | 10 |
|--------|----|
| 101111 | r  |

## Operation
```
PC <- PC + 1
Reg16[r] <- Reg16[r] + SignExtend(Code[PC])
PC <- PC + 1
```

## Assembler syntax examples
```
ADD FT,87
ADD HL,-122
```
