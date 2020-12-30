# J (Jump)

## Opcode
| 76543210 |
|----------|
| 10011010 |

## Operation
1. PC <- PC + 1
2. PC <- PC + SignExtend(Code[PC])

## Assembler syntax examples
```
J @+67
J Label
```

---
# J (Jump)

## Opcode
| 765432 | 10 |
|--------|----|
| 001111 | r  |

## Operation
1. PC <- Reg16[r]

## Assembler syntax examples
```
J (BC)
J (HL)
```

