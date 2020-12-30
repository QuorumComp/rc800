# DJ (Decrement register and jump if result non-zero)

## Opcode
| 76543 | 210 |
|-------|-----|
| 10001 | r   |

## Operation
``` 
PC <- PC + 1
Reg8[r] <- Reg8[r] - 1 
if (Reg8[r] <> 0) then
    PC <- PC + SignExtend(Code[PC])
else
    PC <- PC + 1
```
