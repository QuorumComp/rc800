# EI (Enable interrupt)

## Opcode
| 76543210 |
|----------|
| 01101001 |

Moved from `$08` on 2026-11-12, under `DI` (itself under `RETI`) in the
encoding map; `$08` was `LD F,(FT)` for a day and has been illegal since
2026-11-13, when the hub load went back to `$28`.

## Operation
```
IEF <- 1
PC <- PC + 1
```

## Assembler syntax examples
```
EI
```
