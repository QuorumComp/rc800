`timescale 1ns/1ns

// iverilog testbench for the standalone RC811 core.
// Stimulates the CPU like TopLevelSim: respond combinationally to each
// busEnable read by driving dataIn from a memory model, drive writes back.
// Probes the internal register read-index + data to localize the operand-2
// mis-read for `or`/`xor`/`sub`.

module tb_rc811;

    reg clk = 0;
    reg reset = 1;
    always #5 clk = ~clk;          // 100 MHz

    // ---- CPU interface ----
    reg  [7:0]  dataIn = 8'h00;
    wire [7:0]  dataOut;
    wire [15:0] address;
    wire        busEnable;
    wire        ioFlag;
    wire        code;
    wire        write;
    wire        intOut;

    // ---- Memory model (code + data) ----
    reg [7:0] mem [0:65535];

    // ---- Key internal signals (hierarchical probes) ----
    wire [1:0]  stage;
    wire [7:0]  opcode;
    wire [3:0]  readIdx0;
    wire [3:0]  readIdx1;
    wire [3:0]  aluOp;
    wire [15:0] dataOut0;
    wire [15:0] dataOut1;
    wire [15:0] dataOut1RegNext;

    RC811 dut (
        .io_nmi     (1'b0),
        .io_irq     (1'b0),
        .io_dataIn  (dataIn),
        .io_dataOut (dataOut),
        .io_address (address),
        .io_busEnable(busEnable),
        .io_io      (ioFlag),
        .io_code    (code),
        .io_write   (write),
        .io_int     (intOut),
        .clk        (clk),
        .reset      (reset)
    );

    assign stage         = dut.stage;
    assign opcode        = dut.decodeArea_opcode;
    assign readIdx0      = dut.decodeArea_decoderUnit_io_output_stageControl_readStageControl_registers_0;
    assign readIdx1      = dut.decodeArea_decoderUnit_io_output_stageControl_readStageControl_registers_1;
    assign aluOp         = dut.decodeArea_decoderUnit_io_output_stageControl_aluStageControl_aluControl_operation;
    assign dataOut0      = dut.registers_registers_io_dataOut_0;
    assign dataOut1      = dut.registers_registers_io_dataOut_1;
    assign dataOut1RegNext = dut.registers_registers_io_dataOut_regNext_1;

    // ---- Bus model with 1-cycle latency (TopLevelSim style) ----
    // The CPU asserts busEnable+address at stage 3 and latches the opcode from
    // io.dataIn at stage 0 (next cycle). So capture dataIn on the fetch and
    // HOLD it; model writes by storing dataOut to mem.
    always @(posedge clk) begin
        if (busEnable) begin
            if (write) begin
                mem[address] <= dataOut;
            end else begin
                dataIn <= mem[address];
            end
        end
        // else: hold dataIn (bus holds the last presented byte)
    end

    integer i;
    initial begin
        // Default: NOP everywhere
        for (i = 0; i < 65536; i = i + 1) mem[i] = 8'h00;

        // Reset vector: first power-on instruction is ignored -> NOP
        mem[16'hFFFF] = 8'h00;

        // Program at 0x0000 (NOP margins, then setup + the failing op)
        mem[16'h0000] = 8'h00;                   // NOP margin
        mem[16'h0001] = 8'h00;                   // NOP margin
        mem[16'h0002] = 8'h80; mem[16'h0003] = 8'h01;  // ld f,$01
        mem[16'h0004] = 8'h82; mem[16'h0005] = 8'h40;  // ld b,$40
        mem[16'h0006] = 8'h83; mem[16'h0007] = 8'h10;  // ld c,$10
        mem[16'h0008] = 8'h84; mem[16'h0009] = 8'h20;  // ld d,$20
        mem[16'h000A] = 8'h85; mem[16'h000B] = 8'h30;  // ld e,$30
        mem[16'h000C] = 8'h86; mem[16'h000D] = 8'h50;  // ld h,$50
        mem[16'h000E] = 8'h87; mem[16'h000F] = 8'h60;  // ld l,$60
        mem[16'h0010] = 8'h81; mem[16'h0011] = 8'h00;  // ld t,$00
        mem[16'h0012] = 8'h63;                        // or t,c  <-- the failing op
        mem[16'h0013] = 8'h00;                        // NOP (halt)

        $dumpfile("rc811.vcd");
        $dumpvars(0, dut);
        $dumpvars(1, tb_rc811.stage, tb_rc811.opcode, tb_rc811.readIdx0,
                  tb_rc811.readIdx1, tb_rc811.aluOp, tb_rc811.dataOut0,
                  tb_rc811.dataOut1, tb_rc811.dataOut1RegNext,
                  tb_rc811.address, tb_rc811.busEnable, tb_rc811.code,
                  tb_rc811.write);

        repeat (5) @(negedge clk);
        reset = 0;

        repeat (200) @(negedge clk);

        $finish;
    end

    // ---- Text monitor: one line per cycle ----
    always @(negedge clk) begin
        if (!reset) begin
            $display("cyc stage=%b opcode=%02h r0=%0d r1=%0d op=%0d dOut0=%04h dOut1=%04h dOut1rn=%04h %s",
                     stage, opcode, readIdx0, readIdx1, aluOp,
                     dataOut0, dataOut1, dataOut1RegNext,
                     (opcode == 8'h63) ? "<== or t,c" : "");
        end
    end

endmodule
