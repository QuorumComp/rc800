# XOR (8 bit, bitwise xor register to register)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 01110 | r   | Not valid when r = 1 |

## Operation
```
T <- T ^ Reg8[r]
PC <- PC + 1
```

## Assembler syntax examples
```
XOR T,F
XOR H
```


---
# XOR (8 bit, bitwise XOR immediate to register)

## Opcode
| 76543210 |
|----------|
| 10110010 |

## Operation
```
PC <- PC + 1
T <- T | Code[PC]
PC <- PC + 1
```

## Assembler syntax examples
```
OR T,$C0
OR $04
```
