# CMP (8 bit, set flags according to subtraction result)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 01001 | r   | Not valid when r = 1 |

## Operation
1. F <- Flags[T - Reg8[r]]
2. PC <- PC + 1

## Assembler syntax examples
```
CMP T,F
CMP C
```

---
# CMP (8 bit, , set flags according to subtraction result)

## Opcode
| 76543 | 210 |
|-------|-----|
| 10101 | r   |

## Operation
1. PC <- PC + 1
2. Reg8[r] <- Flags[Reg8[r] - Code[PC]]
3. PC <- PC + 1

## Assembler syntax examples
```
CMP T,42
CMP H,-87
```

---
# CMP (16 bit, set flags according to subtraction result)

## Opcode
| 765432 | 10 | Notes |
|--------|----|-------|
| 110011 | r  | Not valid when r = 0 |

## Operation
1. FT <- Flags[FT - Reg16[r]]
2. PC <- PC + 1

## Assembler syntax examples
```
CMP FT,BC
CMP HL
```
