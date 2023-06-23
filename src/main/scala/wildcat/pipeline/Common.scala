package wildcat.pipeline

import chisel3._
import chisel3.util._

import wildcat.AluType._
object Common {
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
