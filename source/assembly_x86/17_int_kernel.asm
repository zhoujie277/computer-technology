;演示中断和平坦模式的内核程序

flat_4gb_code_seg_sel   equ 0x0008      ;平坦模式下的4GB代码段选择子
flat_4gb_data_seg_sel   equ 0x0010      ;平坦模型下的4GB数据段选择子
idt_linear_address      equ 0x8001f000  ;中断描述符表的线性基地址

;以下定义宏
    ;在内核中分配虚拟内存
    %macro alloc_core_linear 0
        mov ebx, [core_tcb + 0x06]
        add dword [core_tcb + 0x06], 0x1000
        call flat_4gb_code_seg_sel:alloc_inst_a_page
    %endmacro

    %macro alloc_user_linear 0
        mov ebx, [esi + 0x06]
        add dword, [esi + 0x06], 0x1000
        call flat_4gb_code_seg_sel:alloc_inst_a_page
    %endmacro

section core vstart=0x80040000
        ;以下是系统核心的头部，用于加载核心程序
        core_length dd  core_end    ;核心程序总长度#00
        core_entry  dd  start       ;核心代码段入口点#04

[bits 32]
;字符串显示例程（适用于平坦内存模型） 
;显示0终止的字符串并移动光标 
;输入：EBX=字符串的线性地址
put_string:
        push ebx
        push ecx
        cli         ;硬件操作期间，关闭中断
    .getc:
        mov cl, [ebx]
        or cl, cl   ;检测串结束标志（0）
        jz .exit    ;显示完毕，返回
        call put_char
        inc ebx
        jmp .getc
    .exit:
        sti         ;硬件操作完毕，开放中断
        pop ecx
        pop ebx
        retf
;在当前光标处显示一个字符,并推进光标。仅用于段内调用 
;输入：CL=字符ASCII码 
put_char:
        pushad
        ;以下取当前光标位置
        mov dx, 0x3d4
        mov al, 0x0e
        out dx, al 
        inc dx
        in al, dx
        mov ah, al

        dec dx
        mov al, 0x0f
        out dx, al
        inc dx                      ;0x3d5
        in al, dx                   ;低字
        mov bx, ax                  ;BX=代表光标位置的16位数
        and ebx, 0x0000ffff         ;准备使用32位寻址方式访问

        cmp cl, rtm_0x70_interrupt_handle
        jnz .put_0a

        mov ax, bx
        mov bl, 80
        div bl
        mul bl
        mov bx, ax
        jmp .set_cursor
    .put_0a:
        cmp cl,0x0a                        ;换行符？
        jnz .put_other
        add bx,80                          ;增加一行 
        jmp .roll_screen

    .put_other:                               ;正常显示字符
        shl bx,1
        mov [0x800b8000+ebx],cl            ;在光标位置处显示字符 

        ;以下将光标位置推进一个字符
        shr bx,1
        inc bx
    .roll_screen:
        cmp bx,2000                        ;光标超出屏幕？滚屏
        jl .set_cursor

        cld
        mov esi,0x800b80a0                 ;小心！32位模式下movsb/w/d 
        mov edi,0x800b8000                 ;使用的是esi/edi/ecx 
        mov ecx,1920
        rep movsd
        mov bx,3840                        ;清除屏幕最底一行
        mov ecx,80                         ;32位程序应该使用ECX
    .cls:
        mov word [0x800b8000+ebx],0x0720
        add bx,2
        loop .cls

        mov bx,1920
    .set_cursor:
        mov dx,0x3d4
        mov al,0x0e
        out dx,al
        inc dx                             ;0x3d5
        mov al,bh
        out dx,al
        dec dx                             ;0x3d4
        mov al,0x0f
        out dx,al
        inc dx                             ;0x3d5
        mov al,bl
        out dx,al
        
        popad
        
        ret
;从硬盘读取一个逻辑扇区（平坦模型） 
;EAX=逻辑扇区号
;EBX=目标缓冲区线性地址
;返回：EBX=EBX+512
read_hard_disk_0:
        cli
        push eax 
        push ecx
        push edx
        push eax
         
        mov dx,0x1f2
        mov al,1
        out dx,al                          ;读取的扇区数

        inc dx                             ;0x1f3
        pop eax
        out dx,al                          ;LBA地址7~0

        inc dx                             ;0x1f4
        mov cl,8
        shr eax,cl
        out dx,al                          ;LBA地址15~8

        inc dx                             ;0x1f5
        shr eax,cl
        out dx,al                          ;LBA地址23~16

        inc dx                             ;0x1f6
        shr eax,cl
        or al,0xe0                         ;第一硬盘  LBA地址27~24
        out dx,al

        inc dx                             ;0x1f7
        mov al,0x20                        ;读命令
        out dx,al
    .waits:
        in al,dx
        and al,0x88
        cmp al,0x08
        jnz .waits                         ;不忙，且硬盘已准备好数据传输 

        mov ecx,256                        ;总共要读取的字数
        mov dx,0x1f0
    .readw:
        in ax,dx
        mov [ebx],ax
        add ebx,2
        loop .readw

        pop edx
        pop ecx
        pop eax
    
        sti
        retf

put_hex_dword:
        pushad
        mov ebx,bin_hex                    ;指向核心地址空间内的转换表
        mov ecx,8
    .xlt:    
        rol edx,4
        mov eax,edx
        and eax,0x0000000f
        xlat
    
        push ecx
        mov cl,al                           
        call put_char
        pop ecx
    
        loop .xlt
    
        popad
        retf

;在GDT内安装一个新的描述符
;输入：EDX:EAX=描述符
;输出：CX=描述符的选择子
set_up_gdt_descriptor:

        push eax
        push ebx
        push edx

        sgdt [pgdt]

        ;GDT的线性基地址+GDT的界限+1=下一个描述符的地址
        movzx ebx, word [pgdt]
        inc bx
        add ebx, [pgdt + 2]         

        mov [ebx], eax
        mov [ebx + 4], edx

        add word [pgdt], 8          ;增加一个描述符的大小
    
        lgdt [pgdt]                 ;对GDT的更改生效

        mov ax, [pgdt]              ;得到GDT新的界限值
        xor dx, dx
        mov bx, 8
        div bx
        mov cx, ax
        shl cx, 3                   ;得到描述符选择子

        pop edx
        pop ebx
        pop eax
        retf

;构造存储器和系统的段描述符
;输入：EAX=线性基地址 EBX=段界限 ECX=属性。各属性位都在原始位置，无关的位清零
;返回：EDX:EAX=描述符
make_seg_descriptor:

        mov edx, eax
        shl eax, 16
        or ax, bx               ;描述符前32位(EAX)构造完毕

        and edx, 0xffff0000     ;清除基地址中无关的位
        rol edx, 8
        bswap edx

        xor bx, bx
        or edx, ebx
        or edx, ecx
        retf

;构造门的描述符（调用门等）
;输入：EAX=门代码在段内偏移地址 BX=门代码所在段的选择子 CX=段类型及属性等（各属性位都在原始位置）
;返回：EDX:EAX=完整的描述符
make_gate_descriptor:
        push ebx
        push ecx
        mov edx, eax
        and edx, 0xffff0000     
        or dx, dx

        and eax, 0x0000ffff
        shl ebx, 16
        or eax, ebx
        pop ecx
        pop ebx
        retf

;分配一个4KB的页
;无输入 输出：EAX=页的物理地址
allocate_a_4k_page:
        push ebx
        push ecx
        push edx

        xor eax, eax
    .b1:
        bts [page_bit_map], eax
        jnc .b2
        inc eax
        cmp eax, page_map_len * 8
        jl .b1
        mov ebx, message_3
        call flat_4gb_code_seg_sel:put_string
        hlt             ;无可分配的页，停机
    .b2:
        shl eax, 12     ;乘以4096
        
        pop edx
        pop ecx
        pop ebx
        ret

;分配一个页，并安装在当前活动的层级分页结构中
;输入 EBX=页的线性地址
alloc_inst_a_page:
        push eax
        push ebx
        push esi
        ;检查该线性地址所对应的页表是否存在
        mov esi, ebx
        and esi, 0xffc00000             ;获取高10位
        shr esi, 20                     ;得到页目录索引，并乘以4
        or esi, 0xfffff000              ;页目录自身的线性地址+表内偏移
        ;fffff000 高10位索引是 3ff，乘以4 -> 0xffc的偏移地址
        ;假设页目录物理地址?????000, 则最后一个表项为 ?????ffc



;------------------------------------------
    pgdt    dw  0   ;用于设置和修改GDT
            dd  0

    pidt    dw  0
            dd  0

    ;任务控制块链
    tcb_chain   dd  0

    core_tcb    times 32 db 0   ;内核（程序管理器）的TCB

    page_bit_map    db  0xff,0xff,0xff,0xff,0xff,0xff,0x55,0x55
                    db  0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff
                    db  0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff
                    db  0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff
                    db  0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55
                    db  0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00
                    db  0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00
                    db  0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00
    page_map_len    equ $ - page_bit_map 

salt:
    salt_1          db  '@PrintString'
                    times 256 - ($ - salt_1) db 0
                    dd  put_string
                    dw  flat_4gb_code_seg_sel

    salt_2          db  '@ReadDiskData'
                times 256 - ($ - salt_2) db 0
                    dd  read_hard_disk_0
                    dw  flat_4gb_code_seg_sel

    salt_3          db  '@PrintDwordAsHexString'
                times 256 - ($ - salt_3) db 0
                    dd  put_hex_dword
                    dw  flat_4gb_code_seg_sel

    salt_4          db  '@TerminateProgram'
                times 256 - ($ - salt_4) db 0
                    dd  terminate_current_task
                    dw  flat_4gb_code_seg_sel

    salt_item_len   equ $ - salt_4
    salt_items      equ ($ - salt) / salt_item_len

    excep_msg       db  '********Exception encounted********',0

    message_0       db  '  Working in system core with protection '
                    db  'and paging are all enabled.System core is mapped '
                    db  'to address 0x80000000.',0x0d,0x0a,0

    message_1       db  '  System wide CALL-GATE mounted.',0x0d,0x0a,0
    
    message_3       db  '********No more pages********',0
    
    core_msg0       db  '  System core task running!',0x0d,0x0a,0
    
    bin_hex         db  '0123456789ABCDEF'
         
    core_buf   times 512 db 0          ;内核用的缓冲区

    cpu_brnd0        db 0x0d, 0x0a, '  ', 0
    cpu_brand  times 52 db 0
    cpu_brnd1        db 0x0d, 0x0a, 0x0d, 0x0a, 0

;在LDT内安装一个新的描述符
;输入：EDX:EAX=描述符 EBX=TCB基地址 
;输出：CX=描述符的选择子
fill_descriptor_in_ldt:
        push eax
        push edx
        push edi
        mov edi, [ebx + 0x0c]       ;获得LDT基地址
        xor ecx, ecx
        mov cx, [ebx + 0x0a]        ;获得LDT界限
        inc cx                      ;LDT总字节数

        mov [edi + ecx + 0x00], eax
        mov [edi + ecx + 0x04], edx ;安装描述符

        add cx, 8
        dec cx                      ;得到新的LDT界限值并更新到TCB
        mov [ebx + 0x0a], cx
        mov ax, cx
        xor dx, dx
        mov cx, 8
        div cx
        mov cx, ax
        shl cx, 3
        or cx, 0000_0000_0000_0100B ;使TI位=1，指向LDT，最后使RPL=00

        pop edi
        pop edx
        pop eax

        ret

;加载并重定位用户程序
;输入 push 逻辑扇区号 push 任务控制块基地址
;无输出
load_relocate_program:

;程序开始运行处
start:
        ;创建中断描述符表IDT
        ;在此之前，禁止调用put_string过程，以及任何含有sti指令的过程
        ;前20个向量时处理器异常使用的
        mov eax, general_exception_handler  ;门代码在段内偏移地址
        mov bx, flat_4gb_code_seg_sel       ;门代码所在段的选择子
        mov cx, 0x8e00                      ;32位中断门，0特权级
        call flat_4gb_code_seg_sel:make_gate_descriptor
        mov ebx, idt_linear_address         ;中断描述符表的线性地址
        xor esi, esi
    .idt0:
        mov [ebx + esi * 8], eax
        mov [ebx + esi * 8 + 4], edx
        inc esi
        cmp esi, 19                 ;安装前20个异常中断处理过程
        jle .idt0

        ;其余为保留或硬件使用的中断向量
        mov eax, general_interrupt_handler  ;门代码在段内偏移地址
        mov bx, flat_4gb_code_seg_sel       ;门代码所在段的选择子
        mov cx, 0x8e00                      ;32位中断门,0特权级
        call flat_4gb_code_seg_sel:make_gate_descriptor
        mov ebx, idt_linear_address         ;中断描述符表的线性地址
    .idt1:
        mov [ebx + esi * 8], eax
        mov [ebx + esi * 8 + 4], edx
        inc esi
        cmp esi, 255                        ;安装普通的中断处理过程
        jle .idt1

        ;设置实时时钟中断处理过程
        mov eax, rtm_0x70_interrupt_handle  ;门代码在段内偏移地址
        mov bx, flat_4gb_code_seg_sel       ;门代码所在段的选择子
        mov cx, 0x8e00                      ;32位中断门，0特权级
        call flat_4gb_code_seg_sel:make_gate_descriptor
        mov ebx, idt_linear_address         ;中断描述符表的线性地址
        mov [ebx + 0x70 * 8], eax
        mov [ebx + 0x70 * 8 + 4], edx

        ;准备开放中断
        mov word [pidt], 256 * 8 - 1        ;IDT的界限
        mov dword [pidt + 2], idt_linear_address
        lidt [pidt]                         ;加载中断描述符表寄存器idtr

        ;设置8259A中断控制器
        mov al,0x11
        out 0x20,al                        ;ICW1：边沿触发/级联方式
        mov al,0x20
        out 0x21,al                        ;ICW2:起始中断向量
        mov al,0x04
        out 0x21,al                        ;ICW3:从片级联到IR2
        mov al,0x01
        out 0x21,al                        ;ICW4:非总线缓冲，全嵌套，正常EOI

        mov al,0x11
        out 0xa0,al                        ;ICW1：边沿触发/级联方式
        mov al,0x70
        out 0xa1,al                        ;ICW2:起始中断向量
        mov al,0x04
        out 0xa1,al                        ;ICW3:从片级联到IR2
        mov al,0x01
        out 0xa1,al                        ;ICW4:非总线缓冲，全嵌套，正常EOI

        ;设置和时钟中断相关的硬件 
        mov al,0x0b                        ;RTC寄存器B
        or al,0x80                         ;阻断NMI
        out 0x70,al
        mov al,0x12                        ;设置寄存器B，禁止周期性中断，开放更
        out 0x71,al                        ;新结束后中断，BCD码，24小时制

        in al,0xa1                         ;读8259从片的IMR寄存器
        and al,0xfe                        ;清除bit 0(此位连接RTC)
        out 0xa1,al                        ;写回此寄存器

        mov al,0x0c
        out 0x70,al
        in al,0x71                         ;读RTC寄存器C，复位未决的中断状态

        sti                                ;开放硬件中断

        mov ebx, message_0
        call flat_4gb_code_seg_sel:put_string

        ;显示处理器品牌信息 
         mov eax,0x80000002
        cpuid
        mov [cpu_brand + 0x00],eax
        mov [cpu_brand + 0x04],ebx
        mov [cpu_brand + 0x08],ecx
        mov [cpu_brand + 0x0c],edx
    
        mov eax,0x80000003
        cpuid
        mov [cpu_brand + 0x10],eax
        mov [cpu_brand + 0x14],ebx
        mov [cpu_brand + 0x18],ecx
        mov [cpu_brand + 0x1c],edx

        mov eax,0x80000004
        cpuid
        mov [cpu_brand + 0x20],eax
        mov [cpu_brand + 0x24],ebx
        mov [cpu_brand + 0x28],ecx
        mov [cpu_brand + 0x2c],edx

        mov ebx,cpu_brnd0                  ;显示处理器品牌信息 
        call flat_4gb_code_seg_sel:put_string
        mov ebx,cpu_brand
        call flat_4gb_code_seg_sel:put_string
        mov ebx,cpu_brnd1
        call flat_4gb_code_seg_sel:put_string

        ;以下开始安装位整个系统服务的调用门.特权级之间的控制转移必须使用门
        mov edi, salt
        mov ecx, salt_items
    .b4:
        push ecx
        mov eax, [edi + 256]            ;该条目入口点的32位偏移地址
        mov bx, [edi + 260]             ;该条目入口点的段选择子
        mov cx, 1_11_0_1100_000_00000B  ;特权级3的调用门（3以上的特权级才允许访问，0个参数，因为此处用寄存器传参，没有用栈）
        call flat_4gb_code_seg_sel:make_gate_descriptor
        call flat_4gb_code_seg_sel:set_up_gdt_descriptor
        mov [edi + 260], cx             ;将返回的门描述符选择子回填
        add edi, salt_item_len          ;指向下一个C-SALT条目
        pop ecx
        loop .b4

        ;对门进行测试
        mov ebx, message_1
        call far [salt_1 + 256]         ;通过门显示信息（偏移量将被忽略）
        
        ;初始化创建程序管理器任务的任务控制块TCB
        mov word [core_tcb + 0x04], 0xffff  ;任务状态：忙碌
        mov dword [core_tcb + 0x06], 0x80100000 ;内核虚拟空间的分配从这里开始
        mov word [core_tcb + 0x0a], 0xffff      ;登记LDT初始的界限到TCB中（未使用）
        mov ecx, core_tcb
        call append_to_tcb_link             ;将此TCB添加到TCB链中

        ;为程序管理器的TSS分配内存空间
        alloc_core_linear                   ;宏 在内核的虚拟地址空间分配内存

        ;在程序管理器设置必要的项目
        mov word [ebx + 0], 0               ;反向链=0
        mov eax, cr3
        mov dword [ebx + 28], eax           ;登记CR3（PDBR）
        mov word [ebx + 96], 0              ;没有LDT。处理器允许没有LDT的任务。
        mov word [ebx + 100], 0             ;T=0
        mov word [ebx + 102], 103           ;没有I/O位图，0特权级事实上不需要

        ;创建程序管理器的TSS描述符，并安装到GDT中
        mov eax, ebx                        ;TSS的起始线性地址
        mov ebx, 103                        ;段长度(界限)
        mov ecx, 0x00408900                 ;TSS描述符,特权级0
        