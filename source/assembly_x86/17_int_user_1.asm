;演示中断切换任务的任务1程序

program_length  dd program_end                      ;程序总长度#00
entry_point     dd start                            ;程序入口点#04
salt_position   dd salt_begin                       ;salt表起始偏移量#08
salt_items      dd (salt_end - salt_begin) / 256    ;salt条目数#0c

salt_begin:
    PrintString db  '@PrintString'
        times 256 - ($ - PrintString) db 0
    TerminateProgram db  '@TerminateProgram'
        times 256 - ($ - TerminateProgram) db 0

    ReadDiskData     db  '@ReadDiskData'
        times 256 - ($ - ReadDiskData) db 0
    
    PrintDwordAsHex  db  '@PrintDwordAsHexString'
        times 256 - ($ - PrintDwordAsHex) db 0
salt_end:

    message_0   db  '  User task A->;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;'
                db  0x0d, 0x0a, 0

[bits 32]

start:
    mov ebx, message_0
    call far [PrintString]
    jmp start

    call far [TerminateProgram]

program_end: