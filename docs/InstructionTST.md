# TST (16 bit, compare register pair with zero)

TST sets the flags in F to reflect the result of subtracting zero from the specified 16-bit register pair, without storing the result.

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
