`timescale 1ns/1ns

// Testbench for the RC811 core as it appears in the BOARD Verilog
// (hc800_mega65.v), which produced the hardware bitstream. Uses the board's
// clock-domain wrapper ports (bus_clk / bus_reset / when_ClockDomain_l353_regNext).
// If this simulates `or t,c` correctly, the board Verilog is correct and the
// hardware bug is introduced by Vivado synthesis.

module tb_board_rc811;

    reg clk = 0;
    reg reset = 1;
    always #5 clk = ~clk;

    reg  [7:0]  dataIn = 8'h00;
    wire [7:0]  dataOut;
    wire [15:0] address;
    wire        busEnable;
    wire        ioFlag;
    wire        code;
    wire        write;
    wire        intOut;

    reg [7:0] mem [0:65535];

    wire [1:0]  stage;
    wire [7:0]  opcode;
    wire [3:0]  readIdx0;
    wire [3:0]  readIdx1;
    wire [3:0]  aluOp;
    wire [15:0] dataOut0;
    wire [15:0] dataOut1;

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
        .bus_clk    (clk),
        .bus_reset  (reset),
        .when_ClockDomain_l353_regNext (1'b1)
    );

    assign stage    = dut.stage;
    assign opcode   = dut.decodeArea_opcode;
    assign readIdx0 = dut.decodeArea_decoderUnit_io_output_stageControl_readStageControl_registers_0;
    assign readIdx1 = dut.decodeArea_decoderUnit_io_output_stageControl_readStageControl_registers_1;
    assign aluOp    = dut.decodeArea_decoderUnit_io_output_stageControl_aluStageControl_aluControl_operation;
    assign dataOut0 = dut.registers_registers_io_dataOut_0;
    assign dataOut1 = dut.registers_registers_io_dataOut_1;

    always @(posedge clk) begin
        if (busEnable) begin
            if (write) mem[address] <= dataOut;
            else dataIn <= mem[address];
        end
    end

    integer i;
    initial begin
        for (i = 0; i < 65536; i = i + 1) mem[i] = 8'h00;
        mem[16'hFFFF] = 8'h00;
        mem[16'h0000] = 8'h00;
        mem[16'h0001] = 8'h00;
        mem[16'h0002] = 8'h80; mem[16'h0003] = 8'h01;  // ld f,$01
        mem[16'h0004] = 8'h82; mem[16'h0005] = 8'h40;  // ld b,$40
        mem[16'h0006] = 8'h83; mem[16'h0007] = 8'h10;  // ld c,$10
        mem[16'h0008] = 8'h84; mem[16'h0009] = 8'h20;  // ld d,$20
        mem[16'h000A] = 8'h85; mem[16'h000B] = 8'h30;  // ld e,$30
        mem[16'h000C] = 8'h86; mem[16'h000D] = 8'h50;  // ld h,$50
        mem[16'h000E] = 8'h87; mem[16'h000F] = 8'h60;  // ld l,$60
        mem[16'h0010] = 8'h81; mem[16'h0011] = 8'h00;  // ld t,$00
        mem[16'h0012] = 8'h63;                        // or t,c
        mem[16'h0013] = 8'h00;                        // NOP

        repeat (5) @(negedge clk);
        reset = 0;
        repeat (200) @(negedge clk);
        $finish;
    end

    always @(negedge clk) begin
        if (!reset) begin
            $display("cyc stage=%b opcode=%02h r0=%0d r1=%0d op=%0d dOut0=%04h dOut1=%04h %s",
                     stage, opcode, readIdx0, readIdx1, aluOp, dataOut0, dataOut1,
                     (opcode == 8'h63) ? "<== or t,c" : "");
        end
    end

endmodule
