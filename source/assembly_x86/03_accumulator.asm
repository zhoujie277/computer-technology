                                                        ; 计算从1+2+3+...+100的累加和
    jmp near start
    string db '1+2+3+...+100='
    seg_screen equ 0xB800
    seg_offset equ 0x07c0
start:
    mov ax, seg_screen
    mov es, ax
    mov ax, seg_offset
    mov ds, ax
    xor si, si
    xor di, di
    mov cx, start - string

print:
    mov al, [si + string]
    mov es:[di], al
    inc di
    mov byte es:[di], 0x07
    inc di
    inc si
    loop print
func:
    xor ax, ax
    mov bx, 0x1
accumulation:
    add ax, bx
    inc bx
    cmp bx, 100
    jbe accumulation
init:
    mov bx, 0x0
    mov ss, bx
    xor sp, sp
    mov bx, 10
save:
    xor dx, dx
    div bx
    add dl, 0x30
    push dx
    cmp ax, 0
    jne save
show:
    pop dx
    mov es:[di], dl
    inc di
    mov byte es:[di], 0x07
    inc di
    cmp sp, 0
    jne show

infi:
    jmp near infi

    times 510 - ($ - $$) db 0
    db 0x55, 0xAA