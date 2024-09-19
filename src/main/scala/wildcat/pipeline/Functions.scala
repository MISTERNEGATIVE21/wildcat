package wildcat.pipeline

import chisel3._
import chisel3.util._
import wildcat.AluFunct3._
import wildcat.AluType._
import wildcat.BranchFunct._
import wildcat.InstrType._
import wildcat.Opcode._


object Functions {

  def decode(instruction: UInt) = {

    val opcode = instruction(6, 0)
    val decOut = Wire(new DecodedInstr())
    decOut.instrType := R.id.U
    decOut.isImm := false.B
    decOut.isLui := false.B
    decOut.isAuiPc := false.B
    decOut.isLoad := false.B
    decOut.isStore := false.B
    decOut.isJal := false.B
    decOut.isJalr := false.B
    decOut.rfWrite := false.B
    decOut.isECall := false.B
    decOut.rs1Valid := false.B
    decOut.rs2Valid := false.B
    switch(opcode) {
      is(AluImm.U) {
        decOut.instrType := I.id.U
        decOut.isImm := true.B
        decOut.rfWrite := true.B
        decOut.rs1Valid := true.B
      }
      is(Alu.U) {
        decOut.instrType := R.id.U
        decOut.rfWrite := true.B
        decOut.rs1Valid := true.B // TODO: do I need this?
        decOut.rs2Valid := true.B
      }
      is(Branch.U) {
        decOut.instrType := SB.id.U
        decOut.isImm := true.B
      }
      is(Load.U) {
        decOut.instrType := I.id.U
        decOut.rfWrite := true.B
        decOut.isLoad := true.B
      }
      is(Store.U) {
        decOut.instrType := S.id.U
        decOut.isStore := true.B
      }
      is(Lui.U) {
        decOut.instrType := U.id.U
        decOut.rfWrite := true.B
        decOut.isLui := true.B
      }
      is(AuiPc.U) {
        decOut.instrType := U.id.U
        decOut.rfWrite := true.B
        decOut.isAuiPc := true.B
      }
      is(Jal.U) {
        decOut.instrType := UJ.id.U
        decOut.rfWrite := true.B
        decOut.isJal := true.B
      }
      is(JalR.U) {
        decOut.instrType := I.id.U
        decOut.rfWrite := true.B
        decOut.isJalr := true.B
      }
      is(ECall.U) {
        decOut.instrType := I.id.U
        decOut.isECall := true.B
      }
    }
    decOut.aluOp := getAluOp(instruction)
    decOut.imm := getImm(instruction, decOut.instrType)
    decOut
  }

  def getAluOp(instruction: UInt): UInt = {

    val opcode = instruction(6, 0)
    val func3 = instruction(14, 12)
    val func7 = instruction(31, 25)

    val aluOp = WireDefault(ADD.id.U)
    switch(func3) {
      is(F3_ADD_SUB.U) {
        aluOp := ADD.id.U
        when(opcode =/= AluImm.U && func7 =/= 0.U) {
          aluOp := SUB.id.U
        }
      }
      is(F3_SLL.U) {
        aluOp := SLL.id.U
      }
      is(F3_SLT.U) {
        aluOp := SLT.id.U
      }
      is(F3_SLTU.U) {
        aluOp := SLTU.id.U
      }
      is(F3_XOR.U) {
        aluOp := XOR.id.U
      }
      is(F3_SRL_SRA.U) {
        when(func7 === 0.U) {
          aluOp := SRL.id.U
        }.otherwise {
          aluOp := SRA.id.U
        }
      }
      is(F3_OR.U) {
        aluOp := OR.id.U
      }
      is(F3_AND.U) {
        aluOp := AND.id.U
      }
    }
    aluOp
  }

  /*
      def compare(funct3: Int, op1: Int, op2: Int): Boolean = {
      funct3 match {
        case BEQ => op1 == op2
        case BNE => !(op1 == op2)
        case BLT => op1 < op2
        case BGE => op1 >= op2
        case BLTU => (op1 < op2) ^ (op1 < 0) ^ (op2 < 0)
        case BGEU => op1 == op2 || ((op1 > op2) ^ (op1 < 0) ^ (op2 < 0))
      }
    }
   */
  def compare(funct3: UInt, op1: UInt, op2: UInt): Bool = {
    val res = Wire(Bool())
    res := false.B
    switch(funct3) {
      is(BEQ.U) {
        res := op1 === op2
      }
      is(BNE.U) {
        res := op1 =/= op2
      }
      is(BLT.U) {
        res := op1.asSInt < op2.asSInt
      }
      is(BGE.U) {
        res := op1.asSInt >= op2.asSInt
      }
      // How did I come up with this?
      // Better use Tommy's code.
      is(BLTU.U) {
        res := (op1 < op2) ^ (op1(31) ^ op2(31))
      }
      is(BGEU.U) {
        res := op1 === op2 || (op1.asSInt > op2.asSInt)
      }
    }
    res
  }

  def getImm(instruction: UInt, instrType: UInt): SInt = {

    val imm = Wire(SInt(32.W))
    imm := instruction(31, 20).asSInt
    switch(instrType) {
      is(I.id.U) {
        imm := (Fill(20, instruction(31)) ## instruction(31, 20)).asSInt
      }
      is(S.id.U) {
        imm := (Fill(20, instruction(31)) ## instruction(31, 25) ## instruction(11, 7)).asSInt
      }
      is(SB.id.U) {
        imm := (Fill(19, instruction(31)) ## instruction(7) ## instruction(30, 25) ## instruction(11, 8) ## 0.U(1.W)).asSInt
      }
      is(U.id.U) {
        imm := (instruction(31, 12) ## Fill(12, 0.U)).asSInt
      }
      is(UJ.id.U) {
        imm := (Fill(11, instruction(31)) ## instruction(19, 12) ## instruction(20) ## instruction(30, 21) ## 0.U(1.W)).asSInt
      }
    }
    imm
  }

  // Input direct from instruction fetch, the synchronous memory contains the pipeline register
  def registerFile(rs1: UInt, rs2: UInt, rd: UInt, wrData: UInt, wrEna: Bool, useMem: Boolean = true) = {

    if (useMem) {
      val regs = SyncReadMem(32, UInt(32.W), SyncReadMem.WriteFirst)
      val debugRegs = RegInit(VecInit(Seq.fill(32)(0.U(32.W)))) // only for debugging, not used in synthesis
      val rs1Val = regs.read(rs1)
      val rs2Val = regs.read(rs2)
      when(wrEna && rd =/= 0.U) {
        regs.write(rd, wrData)
        debugRegs(rd) := wrData
      }
      (rs1Val, rs2Val, debugRegs)
    } else {
      // non need for forwarding as read address is delayed
      val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
      val rs1Val = regs(RegNext(rs1))
      val rs2Val = regs(RegNext(rs2))
      when(wrEna && rd =/= 0.U) {
        regs(rd) := wrData
      }
      (rs1Val, rs2Val, regs)
    }
  }

  // TODO: something missing? Looks OK now. Wait for the tests.
  def alu(op: UInt, a: UInt, b: UInt): UInt = {
    val res = Wire(UInt(32.W))
    res := DontCare
    switch(op) {
      is(ADD.id.U) {
        res := a + b
      }
      is(SUB.id.U) {
        res := a - b
      }
      is(AND.id.U) {
        res := a & b
      }
      is(OR.id.U) {
        res := a | b
      }
      is(XOR.id.U) {
        res := a ^ b
      }
      is(SLL.id.U) {
        res := a << b(4, 0)
      }
      is(SRL.id.U) {
        res := a >> b(4, 0)
      }
      is(SRA.id.U) {
        res := (a.asSInt >> b(4, 0)).asUInt
      }
      is(SLT.id.U) {
        res := (a.asSInt < b.asSInt).asUInt
      }
      is(SLTU.id.U) {
        res := (a < b).asUInt
      }
    }
    res
  }
}