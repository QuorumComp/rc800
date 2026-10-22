# J/cc (Jump if condition true)

## Opcode
| 7654 | 3210 | Notes |
|------|------|-------|
| 1001 | CC   | CC < 10 |

## Operation
``` 
PC <- PC + 1
if (F flags satisfy CC) then
    PC <- PC + SignExtend(Code[PC])
else
    PC <- PC + 1
```

## Condition codes

| CC | Code | Meaning | Boolean expression |
|----|------|---------|--------------------|
| 0  | LE   | Less or equal signed | `Z ∨ (N ⊕ V)` |
| 1  | GT   | Greater than signed | `¬Z ∧ ¬(N ⊕ V)` |
| 2  | LT   | Less than signed | `N ⊕ V` |
| 3  | GE   | Greater or equal signed | `¬(N ⊕ V)` |
| 4  | LEU  | Less or equal unsigned | `Z ∨ C` |
| 5  | GTU  | Greater than unsigned | `¬Z ∧ ¬C` |
| 6  | LTU (or CS) | Less than unsigned (or carry set) | `C` |
| 7  | GEU (or CC) | Greater or equal unsigned (or carry clear) | `¬C` |
| 8  | EQ (or Z) | Equal (or zero) | `Z` |
| 9  | NE (or NZ) | Not equal (or not zero) | `¬Z` |

`C`, `Z`, `N` and `V` are the carry, zero, negative and overflow bits of F
(bits 0–3). `∨` is OR, `∧` is AND, `¬` is NOT and `⊕` is XOR.

The assembler accepts `CC` as a synonym for `GEU` (carry clear) and `CS` as a
synonym for `LTU` (carry set), alongside the `Z`/`NZ` aliases. `NC` is not
accepted.

## Assembler syntax examples

```
J/NZ @+67
J/GE Label
J/CS Label
```
