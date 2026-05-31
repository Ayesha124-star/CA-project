.data
    nums: .word 10, 20, 30, 40, 50

.text
main:
    la   $t0, nums          # load address of array
    lw   $t1, 0($t0)     
    lw   $t1, 0($t0) 
    lw   $t1, 0($t0) 
    lw   $t1, 0($t0)    # t1 = 10   (load)
    add  $t2, $t1, $t1      # t2 = 20   (load-use STALL → red)
    sub  $t3, $t2, $t1      # t3 = 10   (EX forward → green)
    add  $t4, $t3, $t2      # t4 = 30   (MEM forward → green)
    sw   $t4, 4($t0)        # store 30 into nums[1]
    lw   $t5, 4($t0)        # load it back
    add  $t6, $t5, $t4      # load-use STALL again → red
    
    li   $v0, 10
    syscall
