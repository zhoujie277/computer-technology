;05_user_print_cursor.asm
;此程序控制显卡显示一大串文本，并能支持回车换行

;定义用户程序头部段
section header vstart=0
    program_length dd program_end           ;程序总长度
    
    ;用户程序入口点
    code_entry  dw  begin                   ;偏移地址[0x04]
                dd  section.code_1.start    ;段地址[0x06]
    
    ;段重定位表项个数
    realloc_tbl_len dw (header_end - code_1_segment) / 4

    ; 段重定位表
    code_1_segment dd section.code_1.start  ;[0x0c]
    code_2_segment dd section.code_2.start  ;[0x10]
    data_1_segment dd section.data_1.start  ;[0x14]
    data_2_segment dd section.data_2.start  ;[0x18]
    stack_segment  dd section.stack.start   ;[0x1c]
header_end:

;定义代码段1（16字节对齐）
section code_1 align=16 vstart=0
;显示串。输入：DS:BX=串地址
put_string:
        mov cl, [bx]
        or cl, cl               ;更改标志寄存器
        jz .exit
        call put_char
        inc bx
        jmp put_string
    .exit:
        ret

;显示一个字符,输入cl=字符ASCII码
put_char:
        push ax
        push bx
        push cx
        push dx
        push ds
        push es

        ;以下取当前光标位置
        mov dx, 0x3d4
        mov al, 0x0e                ;指定光标位置数值的高8位
        out dx, al
        mov dx, 0x3d5
        in al, dx                   ;向0x3d4位置读取光标位置数值的高8位
        mov ah, al
        
        mov dx, 0x3d4
        mov al, 0x0f                ;指定光标位置数值的低8位
        out dx, al
        mov dx, 0x3d5
        in al, dx                   ;向0x3d4读取光标位置数值的低8位
        mov bx, ax                  ;ax现在拥有代表光标位置数值的16位

        cmp cl, 0x0d                ;输入是不是一个回车符
        jnz .put_0a
        mov bl, 80
        div bl                      ;先除和乘，此操作的结果忽略了除法生成的余数，并随后被乘法覆盖
        mul bl
        mov bx, ax
        jmp set_cursor

    ;显示换行符 
    .put_0a:
        cmp cl, 0x0a                ;输入是不是一个回车符
        jnz .put_other
        add bx, 80
        jmp .roll_screen

    ;正常显示字符
    .put_other:
        mov ax, 0xb800
        mov es, ax
        shl bx, 1
        mov [es:bx], cl

        ;以下将光标位置推进一个字符
        shr bx, 1
        add bx, 1

    ;光标位置超出屏幕，滚屏
    .roll_screen:
        cmp bx, 2000                ;光标超出屏幕？滚屏
        jl set_cursor

        mov ax, 0xb800
        mov ds, ax
        mov es, ax
        cld
        mov si, 0xa0
        mov di, 0x00
        mov cx, 1920
        rep movsw
        mov bx, 3840                
        mov cx, 80
    ;;清除屏幕最底一行
    .cls:
        mov word[es:bx], 0x0720
        add bx, 2
        loop .cls
        mov bx, 1920

    ;重新设置光标
    set_cursor:
        mov dx, 0x3d4
        mov al, 0x0e
        out dx, al
        mov dx, 0x3d5
        mov al, bh
        out dx, al
        mov dx, 0x3d4
        mov al, 0x0f
        out dx, al
        mov dx, 0x3d5
        mov al, bl
        out dx, al

        pop es
        pop ds
        pop dx
        pop cx
        pop bx
        pop ax
        ret

begin:
    ; 初始执行时，DS和ES指向用户程序头部段
    mov ax, [stack_segment]         ;设置用户程序自己的栈
    mov ss, ax
    mov sp, stack_end

    mov ax, [data_1_segment]        ;设置用户程序自己的数据段
    mov ds, ax

    mov bx, msg0
    call put_string                 ;显示第一段信息

    ;push 代码段2的段地址和汇编地址
    push word [es:code_2_segment]
    mov ax, begin2
    push ax

    ;修改CS：IP，使之转移到代码段2执行
    retf

continue:
    ;段寄存器DS切换到数据段2
    mov ax, [es:data_2_segment]
    mov ds, ax
    mov bx, msg1
    call put_string
    ;无限循环
    jmp $

;定义代码段2(16字节对齐)
section code_2 align=16 vstart=0

    begin2:
        push word [es:code_1_segment]
        mov ax, continue
        push ax
        retf

section data_1 align=16 vstart=0

    msg0 db '  This is NASM - the famous Netwide Assembler. '
         db 'Back at SourceForge and in intensive development! '
         db 'Get the current versions from http://www.nasm.us/.'
         db 0x0d,0x0a,0x0d,0x0a
         db '  Example code for calculate 1+2+...+1000:',0x0d,0x0a,0x0d,0x0a
         db '     xor dx,dx',0x0d,0x0a
         db '     xor ax,ax',0x0d,0x0a
         db '     xor cx,cx',0x0d,0x0a
         db '  @@:',0x0d,0x0a
         db '     inc cx',0x0d,0x0a
         db '     add ax,cx',0x0d,0x0a
         db '     adc dx,0',0x0d,0x0a
         db '     inc cx',0x0d,0x0a
         db '     cmp cx,1000',0x0d,0x0a
         db '     jle @@',0x0d,0x0a
         db '     ... ...(Some other codes)',0x0d,0x0a,0x0d,0x0a
         db 0

section data_2 align=16 vstart=0

    msg1 db '  The above contents is written by me. '
         db '2021-10-10'
         db 0

section stack align=16 vstart=0

        resb 256

stack_end:

section trail align=16
program_end:
    