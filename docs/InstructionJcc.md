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

| CC | Code | Meaning |
|----|------|---------|
| 0  | LE   | Less or equal signed |
| 1  | GT   | Greater than signed |
| 2  | LT   | Less than signed |
| 3  | GE   | Greater or equal signed |
| 4  | LEU  | Less or equal unsigned |
| 5  | GTU  | Greater than unsigned |
| 6  | LTU  | Less than unsigned |
| 7  | GEU  | Greater or equal unsigned |
| 8  | EQ (or Z) | Equal (or zero) |
| 9  | NE (or NZ) | Not equal (or not zero) |

## Assembler syntax examples

```
J/NZ @+67
J/GE Label
```
