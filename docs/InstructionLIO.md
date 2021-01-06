# LIO (8 bit, load I/O)

## Opcode
| 7654321 | 0 |
|---------|---|
| 0000100 | d |

## Operation
Performs I/O access. If I/O ports are not supported, accesses memory instead.

```
if d = 0 then
    IO[BC] <- T
else
    T <- IO[BC]  
PC <- PC + 1
```

## Assembler syntax examples
```
LIO T,(BC)
LIO (BC),T
```
