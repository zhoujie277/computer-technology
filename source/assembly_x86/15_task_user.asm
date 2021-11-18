;演示用户程序的切换
;用户程序.asm

section header vstart=0

    program_length  dd program_end  ;程序总长度#0x00
    head_len        dd header_end   ;程序头部的长度#0x04
    
    stack_len       dd 0            ;用于接受不了栈段选择子#0x08
    stack_seg       dd 1            ;程序建议的栈大小#0x0c,以4KB为单位

    prgentry        dd start        ;程序入口#0x10
    
    code_seg        dd section.code.start   ;代码段位置#0x14
    code_len        dd code_end             ;代码段长度#0x18
    data_seg        dd section.data.start   ;数据段位置#0x1c
    data_len        dd data_end             ;数据段长度#0x20

    ;符号地址检索表
    salt_items      dd (head_len - salt) / 256   ;0x24   
    salt:                                        ;0x28
        PrintString db '@PrintString'
            times 256 - ($ - PrintString) db 0
        TerminateProgram db '@TerminateProgram'
            times 256 - ($ - TerminateProgram) db 0
        ReadDiskData    db '@ReadDiskData'
            times 256 - ($ - ReadDiskData) db 0

header_end:

section data vstart=0

    message_1   db 0x0d, 0x0a
                db  '[USER TASK]: Hi! nice to meet you,'
                db  'I am run at CPL=', 0
    message_2   db 0
                db '.Now,I must exit...', 0x0d, 0x0a, 0
data_end:

[bits 32]

section code vstart=0
start:
    ;程序启动时，DS指向头部段，也不需要设置栈
    mov eax, ds
    mov fs, eax

    mov eax, [data_seg]
    mov ds, eax

    ;PrintString所在代码段在内核特权0上，而特权0的代码访问了特权3的数据
    mov ebx, message_1
    call far [fs:PrintString]

    mov ax, cs
    and al, 0000_0011B
    or al, 0x0030
    mov [message_2], al
    
    mov ebx, message_2
    call far [fs:PrintString]

    call far [fs:TerminateProgram]  ;退出,并将控制权返回到核心
code_end:

section trail
program_end: