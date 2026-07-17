# TST (16 bit, compare register pair with zero)

TST sets F to flags reflecting the result of a 16-bit register minus zero.

## Opcode
| 765432 | 10 |
|--------|----|
| 110101 | r  |

## Operation
```
F <- Flags[Reg16[r] - 0]
PC <- PC + 1
```

## Assembler syntax examples
```
TST	BC
TST	DE
```
