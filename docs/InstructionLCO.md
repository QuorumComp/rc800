# LCO (8 bit load from code memory)

## Opcode
| 765432 | 10 |
|--------|----|
| 000011 | r  |

## Operation
```
T <- Code[Reg16[r]]
PC <- PC + 1
```

## Assembler syntax examples
```
LCO T,(BC)
```
