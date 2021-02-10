# LIO (8 bit, load I/O)

## Opcode
| 76543 | 2 | 10 | Notes |
|-------|---|----|-------|
| 00001 | d | r  | d = 0 and r = 0 combination invalid |

## Operation
Performs I/O access. If I/O ports are not supported, accesses memory instead.

```
if d = 0 then
    IO[Reg16[r]] <- T
else
    T <- IO[Reg16[r]]  
PC <- PC + 1
```

## Assembler syntax examples
```
LIO T,(BC)
LIO (DE),T
```
