#!/bin/sh
#cd ../ram
#../../asmotor/build/scons/build_gameboy/motorgb -fv -oram_content.hex -mcz test.asm 

cd build
rm -f *.dsn *.vcd

cd ../rtl
iverilog -gio-range-error -gstrict-ca-eval -Wall -Winfloop -o ../build/top.dsn           \
    top_tb.v              \
    top.v                 \
    ram.v                 \
    r8r.v                 \
    r8r_stage1.v          \
    pc.v                  \
    instruction_decoder.v
    

cd ../build
vvp top.dsn

cd ..
