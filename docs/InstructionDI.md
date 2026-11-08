# DI (Disable interrupt)

## Opcode
| 76543210 |
|----------|
| 01100001 |

Moved from `$09` on 2026-11-12, directly under `RETI` in the encoding map;
`$09` was `LD T,(FT)` for a day and has been illegal since 2026-11-13, when
the hub load went back to `$29`.

## Operation
```
IEF <- 0
PC <- PC + 1
```

## Assembler syntax examples
```
DI
```
