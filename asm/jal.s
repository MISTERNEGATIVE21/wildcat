#
# jal tests
#
    jal foo # uses x1 per default
    li x2, 0x60
    bne x2, x3, fail
    beq x0, x0, pass

foo:
    li x3, 0x60
    ret

fail:
    addi a0, x0, 2
	ecall
1:  beq   x0, x0, 1b
    nop

pass:
    addi a0, x0, 1
	ecall
1:  beq   x0, x0, 1b




