# RSA (16 bit, right shift register arithmetic)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 11111 | r   | Not valid when r = 0 or r = 1 |

## Operation
1. FT <- FT >>> (Reg8[r] & 0xF)
2. PC <- PC + 1

## Assembler syntax examples
```
RSA FT,B
RSA C
```

---
# RS (16 bit, right shift register)

## Opcode
| 76543210 |
|----------|
| 10111011 |

## Operation
1. PC <- PC + 1
2. FT <- FT >>> (Code[PC] & 0xF)
3. PC <- PC + 1

## Assembler syntax examples
```
RSA 4
RSA FT,5
```


