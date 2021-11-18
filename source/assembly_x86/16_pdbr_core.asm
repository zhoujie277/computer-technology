;演示分页机制的核心应用程序

    ;定义常量
    core_data_seg_sel   equ 0x38        ;内核代码段选择子
    core_data_seg_sel   equ 0x30        ;内核数据段选择子
    sys_routine_seg_sel equ 0x28        ;内核数据段选择子
    video_ram_seg_sel   equ 0x20        ;视频显示缓冲区的段选择子
    core_stack_seg_sel  equ 0x18        ;内核栈段选择子
    mem_0_4_gb_seg_sel  equ 0x08        ;整个0-4GB的内存的段选择子

    ;系统核心的头部
    core_length     dd core_end    ;核心程序总长度 #00
    sys_routine_seg dd section.sys_routine.start    ;#0x04
    core_data_seg   dd section.core_data.start      ;#0x08
    core_code_seg   dd section.core_code.start      ;#0x0c
    core_entry      dd start                    ;#0x10
                    dw core_code_seg_sel

[bits 32]
section sys_routine vstart=0
put_string:
        push ecx
    .getc:
        mov cl,[ebx]
        or cl,cl
        jz .exit
        call put_char
        inc ebx
        jmp .getc
    .exit:
        pop ecx
        retf

put_char:

        pushad
        ;以下取当前光标位置
        mov dx,0x3d4
        mov al,0x0e
        out dx,al
        inc dx                             ;0x3d5
        in al,dx                           ;高字
        mov ah,al

        dec dx                             ;0x3d4
        mov al,0x0f
        out dx,al
        inc dx                             ;0x3d5
        in al,dx                           ;低字
        mov bx,ax                          ;BX=代表光标位置的16位数

        cmp cl,0x0d                        ;回车符？
        jnz .put_0a
        mov ax,bx
        mov bl,80
        div bl
        mul bl
        mov bx,ax
        jmp .set_cursor

    put_0a:
        cmp cl,0x0a                        ;换行符？
        jnz .put_other
        add bx,80
        jmp .roll_screen

  .put_other:                               ;正常显示字符
        push es
        mov eax,video_ram_seg_sel          ;0x800b8000段的选择子
        mov es,eax
        shl bx,1
        mov [es:bx],cl
        pop es

        ;以下将光标位置推进一个字符
        shr bx,1
        inc bx
    .roll_screen:
        cmp bx,2000                        ;光标超出屏幕？滚屏
        jl .set_cursor

        push ds
        push es
        mov eax,video_ram_seg_sel
        mov ds,eax
        mov es,eax
        cld
        mov esi,0xa0                       ;小心！32位模式下movsb/w/d 
        mov edi,0x00                       ;使用的是esi/edi/ecx 
        mov ecx,1920
        rep movsd
        mov bx,3840                        ;清除屏幕最底一行
        mov ecx,80                         ;32位程序应该使用ECX
  .cls:
        mov word[es:bx],0x0720
        add bx,2
        loop .cls

        pop es
        pop ds

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

;从硬盘读入一个逻辑扇区
;EAX = 逻辑扇区号 EBX=目标缓冲区地址
;返回：EBX=EBX + 512     
read_hard_disk_0:
        push eax
        push ecx
        push edx

        push eax
        mov dx, 0x1f2
        mov al, 1
        out dx, al

        inc dx
        pop eax
        out dx, al

        inc dx
        mov cl, 8
        shr eax, cl
        out dx, al

        inc dx
        shr eax, cl
        out dx, al
        
        inc dx
        shr eax, cl
        or al, 0xe0
        out dx, al

        inc dx
        mov al, 0x20
        out dx, al
    .waits:
        in al, dx
        and al, 0x88
        cmp al, 0x08
        jnz .waits              ;不忙，且硬盘已准备好数据

        mov ecx, 256
        mov dx, 0x1f0
    .readw:
        in ax, dx
        mov [ebx], ax
        add ebx, 2
        loop .readw

        pop edx
        pop ecx
        pop eax
        retf

;以16进制显示二进制数据
;输入：EDX=要转换并显示的数字
put_hex_dword:

        pushad
        push ds

        mov ax, core_data_seg_sel
        mov ds, ax

        mov ebx, bin_hex
        mov ecx, 8
    .xlt:
        rol edx, 4
        mov eax, edx
        and eax, 0x0000000f
        xlat     ;查表指令，以DS:[BX+AL]为地址，提取存储器中的一个字节送入AL。

        push ecx
        mov cl, al
        call put_char
        pop ecx

        loop .xlt

        pop ds
        popad
        retf

;在GDT内安装一个新的描述符
;输入：EDX:EAX = 描述符
;输出： CX = 描述符的选择子
set_up_gdt_descriptor:

        push eax
        push ebx
        push edx
        push ds
        push es

        mov ebx, core_data_seg_sel  ;切换到核心数据段
        mov ds, ebx

        sgdt [pgdt]

        mov ebx, mem_0_4_gb_seg_sel
        mov es, ebx

        movzx ebx, word [pgdt]      ;GDT界限
        inc bx                      ;GDT总字节数，也是下一个描述符偏移
        add ebx, [pgdt + 2]         ;add操作，下一个描述符的线性地址

        mov [es: ebx], eax
        mov [es: ebx + 4], edx

        add word [pgdt], 8          ;安装完后，增加一个描述符的大小

        lgdt [pgdt]                 ;对GDT的更改生效

        mov ax, [pgdt]              ;得到GDT的界限值
        xor dx, dx
        mov bx, 8
        div bx, 
        mov cx, ax
        shl cx, 3

        pop es
        pop ds
        pop edx
        pop ebx
        pop eax
        retf

;生成存储器和系统的段描述符
;输入：EAX=线性基地址 EBX=段界限 ECX=属性，各属性均在原始位置。无关位清零
;返回：EDX:EAX = 描述符
make_seg_descriptor:
        mov edx, eax
        shl eax, 16
        or ax, bx           ;描述符32位构造完毕

        and edx, 0xffff0000
        rol edx, 8
        bswap edx           ;装配基地址的24-31和16-23位
        xor bx, bx
        or edx, ebx
        or edx, ecx
        retf

;生成门描述符(调用门)
;输入 EAX=门代码在段内偏移地址 BX=门代码所在段的选择子 CX=段类型及属性
;返回 EDX:EAX=完整的描述符
make_gate_descriptor:
        push ebx
        push ecx
        mov edx, eax
        and edx, 0xffff0000         ;得到偏移地址高16位
        or dx, cx                   ;组装属性部分到EDX

        and eax, 0x0000ffff         ;得到偏移地址低16位
        shl ebx, 16
        or eax, ebx                 ;组装段选择子部分

        pop ecx
        pop ebx
        retf

;分配一个4KB的页
;无输入，输出 EAX=页的物理地址
allocate_a_4k_page:
        push ebx
        push ecx
        push edx
        push ds

        mov eax, core_data_seg_sel
        mov ds, eax
        xor eax, eax
    .b1:
        bts [page_bit_map], eax
        jnc .b2
        inc eax
        cmp eax, page_map_len * 8
        jl .b1
        mov ebx, message_3
        call sys_routine_seg_sel:put_string
        hlt
    .b2:
        shl eax, 12

        pop ds
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
        push ds

        mov eax, mem_0_4_gb_seg_sel
        mov ds, eax

        ;检查该线性地址所对应的页表是否存在
        mov esi, ebx
        add esi, 0xffc00000         ;保留高10位
        shr esi, 20                 ;得到页目录索引，并乘以4
        or esi, 0xfffff000          ;页目录自身的线性地址+表内偏移
        
        test dword [esi], 0x00000001    ;P位是否为1，检查该线性地址是否已经有对应的页表
        jnz .b1
        ;不存在，创建该线性地址所对应的页表
        call allocate_a_4k_page         ;分配一个页作为页表
        or eax, 0x00000007
        mov [esi], eax                  ;在页目录中登记该页表
    .b1:
        ;开始访问该线性地址所对应的页表
        mov esi, ebx
        shr esi, 10
        and esi, 0x003ff000             ;或者0xfffff000,因高10为是零
        or esi, 0xffc00000              ;得到给页表的线性地址

        ;得到该线性地址在页表内的对应条目（页表项）
        and ebx, 0x003ff000
        shr ebx, 10                     ;相当于右移12位，再乘以4
        or esi, ebx                     ;页表项的线性地址
        call allocate_a_4k_page
        or eax, 0x00000007
        mov [esi], eax

        pop ds
        pop esi
        pop ebx
        pop eax
        retf

;创建新页目录，并复制当前页目录内容
;无输入
;输出 EAX=新页目录的物理地址
create_copy_cur_pdir:
        push ds
        push es
        push esi
        push edi
        push ebx
        push ecx

        mov ebx, mem_0_4_gb_seg_sel
        mov ds, ebx
        mov es, ebx
        call allocate_a_4k_page
        mov ebx, eax
        or ebx, 0x00000007
        mov [0xfffffff8], ebx

        mov esi, 0xfffff000         ;esi->当前页目录的线性地址
        mov edi, 0xffffe000         ;edi->新页目录的线性地址
        mov ecx, 1024               ;ecx=要复制的目录项数
        cld
        repe movsd

        pop ecx
        pop ebx
        pop edi
        pop esi
        pop es
        pop ds
        retf

;终止当前任务
;注意：执行此例程时，当前任务仍在运行中，此例程其实也是当前任务的一部分
terminate_current_task:
        mov eax, core_data_seg_sel
        mov ds, eax
        pushfd
        pop edx

        test dx, 0100_0000_0000_0000B   ;测试NT位
        jnz .b1
        jmp far [program_man_tss]       ;程序管理器任务
    .b1:
        iretd

sys_routine_end:

;系统核心数据段
section core_data vstart=0

        pgdt            dw  0             ;用于设置和修改GDT 
                        dd  0

        page_bit_map    db  0xff, 0xff, 0xff, 0xff, 0xff, 0x55, 0x55, 0xff
                        db  0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff
                        db  0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff
                        db  0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff
                        db  0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55
                        db  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
                        db  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
                        db  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        page_map_len    equ $ - page_bit_map
        ;符号地址检索表
    salt:
        salt_1          db  '@PrintString'
            times 256 - ($ - salt_1) db 0
                        dd  put_string
                        dw  sys_routine_seg_sel

        salt_2          db  '@ReadDiskData'
            times 256 - ($ - salt_2) db 0
                        dd  read_hard_disk_0
                        dw  sys_routine_seg_sel

        salt_3          db  '@PrintDwordAsHexString'
            times 256 - ($ - salt_3) db 0
                        dd  put_hex_dword
                        dw  sys_routine_seg_sel

        salt_4          db  '@TerminateProgram'
            times 256 - ($ - salt_4) db 0
                        dd  terminate_current_task
                        dw  sys_routine_seg_sel

        salt_item_len   equ $ - salt_4
        salt_items      equ ($ - salt) / salt_item_len

        message_0       db  '  Working in system core,protect mode.'
                        db  0x0d, 0x0a, 0

        message_1       db  '  Paging is enabled.System core is mapped to'
                        db  ' address 0x80000000.', 0x0d, 0x0a, 0
        
        message_2       db  0x0d,0x0a
                        db  '  System wide CALL-GATE mounted.', 0x0d, 0x0a, 0
        
        message_3       db  '********No more pages********', 0
        
        message_4       db  0x0d, 0x0a,'  Task switching...@_@', 0x0d, 0x0a, 0
        
        message_5       db  0x0d, 0x0a,'  Processor HALT.', 0

        ;put_hex_dword子过程用的查找表 
        bin_hex         db '0123456789ABCDEF'
                                            
        core_buf   times 512 db 0          ;内核用的缓冲区

        cpu_brnd0       db 0x0d, 0x0a, '  ', 0
        cpu_brand  times 52 db 0
        cpu_brnd1       db 0x0d, 0x0a, 0x0d, 0x0a, 0

        ;任务控制块链
        tcb_chain       dd  0

        ;内核信息
        core_next_laddr dd  0x80100000    ;内核空间中下一个可分配的线性地址        
        program_man_tss dd  0             ;程序管理器的TSS描述符选择子 
                        dw  0

core_data_end:

section core_code vstart=0
;在LDT内安装一个新描述符
;输入 EDX:EAX=描述符 EBX=TCB基地址
;输出 CX=描述符的选择子
fill_descriptor_in_ldt:
        push eax
        push edx
        push edi
        push ds

        mov ecx, mem_0_4_gb_seg_sel
        mov ds, ecx
        mov edi, [ebx + 0x0c]           ;获得LDT基地址

        xor ecx, ecx
        mov cx, [ebx + 0x0a]            ;获得LDT界限
        inc cx                          ;LDT的总字节数，即新描述符偏移地址

        mov [edi + ecx + 0x00], eax
        mov [edi + ecx + 0x04], edx     ;安装描述符

        add cx, 8
        dec cx

        mov [ebx + 0x0a], cx            ;更新LDT界限值到TCB

        mov ax, cx
        xor dx, dx
        mov cx, 8
        div cx

        mov cx, ax
        shl cx, 3
        or cx, 0000_0000_0000_0100B     ;使TI位=1，指向LDT，最后使RPL=00

        pop ds
        pop edi
        pop edx
        pop eax
        ret

;加载并重定位用户程序
;输入 push 逻辑扇区号 push 任务控制块基地址
;无输出
load_relocate_program:
        pushad
        push ds
        push es
        mov ebp, esp

        mov ecx, mem_0_4_gb_seg_sel
        mov es, ecx

        ;清空当前页目录的前半部分(对应低2GB的局部地址空间)
        mov ebx, 0xfffff000
        xor esi, esi
    .b1:
        mov dword [es: ebx + esi * 4], 0x00000000
        inc esi
        cmp esi, 512
        jl .b1

        ;以下开始分配内存并加载用户程序
        mov eax, core_data_seg_sel
        mov ds, eax

        mov eax, [ebp + 12 * 4]         ;从栈中取出用户程序起始扇区号
        mov ebx, core_buf               ;读取程序头部数据，读到核心缓冲区
        call sys_routine_seg_sel:read_hard_disk_0

        ;以下判断整个程序有多大
        mov eax, [core_buf]             ;程序尺寸
        mov ebx, eax
        and ebx, 0xfffff000             ;使之4KB对齐
        add ebx, 0x1000
        test eax, 0x00000fff            ;程序的大小正好是4KB的倍数
        cmovnz eax, ebx                 ;不是。使用凑整的结果

        mov ecx, eax
        shr ecx, 12                     ;程序占用的总4KB页数

        mov eax, mem_0_4_gb_seg_sel     ;切换DS到0-4GB的段
        mov ds, eax

        mov eax, [ebp + 12 * 4]         ;起始扇区好
        mov esi, [ebp + 11 * 4]         ;从栈中取得TCB的基地址
    .b2:
        mov ebx, [es: esi + 0x06]       ;取得可用的线性地址
        add dword [es: esi + 0x06], 0x1000
        call sys_routine_seg_sel:alloc_inst_a_page

        push ecx
        mov ecx, 8
    .b3:
        call sys_routine_seg_sel:read_hard_disk_0
        inc eax
        loop .b3
        pop ecx
        loop .b2

        ;在内核地址空间内创建用户任务的TSS
        mov eax, core_data_seg_sel      ;切换DS到内核数据段
        mov ds, eax

        mov ebx, [core_next_laddr]      ;用户任务的TSS必须在全局空间上分配
        call sys_routine_seg_sel:alloc_inst_a_page
        add dword [core_next_laddr], 4096

        mov [es: esi + 0x14], ebx       ;在TCB中填写TSS的线性地址
        mov word [es: esi + 0x12], 103  ;在TCB中填写TSS的界限值

        ;在用户任务的局部地址空间内创建LDT
        mov ebx, [es: esi + 0x06]       ;从TCB中取得可用的线性地址
        add dword [es: esi + 0x06], 0x1000
        call sys_routine_seg_sel:alloc_inst_a_page
        mov [es: esi + 0x0c], ebx       ;填写LDT线性地址到TDB中

;在TCB链上追加任务块
;输入 ECX=TCB线性基地址
append_to_tcb_link:
        push eax
        push edx
        push ds
        push es
        mov eax, core_data_seg_sel      ;令DS指向内核数据段
        mov ds, eax
        mov eax, mem_0_4_gb_seg_sel
        mov es, eax

        mov dword [es: ecx + 0x00], 0   ;当前TCB指针域清零，以指示这是最后一个TCB

        mov eax, [tcb_chain]            ;TCB表头指针
        or eax, eax                     ;改变标志寄存器，eax本身不变
        jz .notcb
    .searc:
        mov edx, eax
        mov eax, [es: edx + 0x00]
        or eax, eax
        jnz .searc
        mov [es: edx + 0x00], ecx
        jmp .retpc
    .notcb:
        mov [tcb_chain], ecx
    .retpc:
        pop es
        pop ds
        pop edx
        pop eax
        ret

start:
        mov ecx, core_data_seg_sel
        mov ds, ecx

        mov ecx, mem_0_4_gb_seg_sel
        mov es, ecx

        mov ebx, message_0
        call sys_routine_seg_sel:put_string

        mov eax, 0x80000002
        cpuid
        mov [cpu_brand + 0x00], eax
        mov [cpu_brand + 0x04], ebx
        mov [cpu_brand + 0x08], ecx
        mov [cpu_brand + 0x0c], edx
        mov eax, 0x80000003
        cpuid
        mov [cpu_brand + 0x10], eax
        mov [cpu_brand + 0x14], ebx
        mov [cpu_brand + 0x18], ecx
        mov [cpu_brand + 0x1c], edx
        mov eax, 0x80000004
        cpuid
        mov [cpu_brand + 0x20], eax
        mov [cpu_brand + 0x24], ebx
        mov [cpu_brand + 0x28], ecx
        mov [cpu_brand + 0x2c], edx

        mov ebx, cpu_brnd0
        call sys_routine_seg_sel:put_string
        mov ebx, cpu_brand
        call sys_routine_seg_sel:put_string
        mov ebx, cpu_brnd1
        call sys_routine_seg_sel:put_string

        ;准备打开分页机制
        ;创建系统内核的页目录表PDT
        ;页目录表清零
        mov ecx, 1024               ;1024个目录项
        mov ebx, 0x00020000         ;页目录的物理地址
        xor esi, esi
    .b1:
        mov dword [es: ebx + esi], 0x00000000   ;页目录表项清零
        add esi, 4
        loop .b1

        ;在页目录内创建指向页目录自己的目录项
        mov dword [es: ebx + 4092], 0x00020003

        ;在页目录内创建与线性地址0x00000000对应的目录项
        mov dword [es: ebx + 0], 0x00021003 ;写入目录项(页表的物理地址和属性)
        ;创建与上面那个目录项相对应的页表，初始化页表项
        mov ebx, 0x00021000                 ;页表的物理地址
        xor eax, eax                        ;起始页的物理地址
        xor esi, esi
    .b2:
        mov edx, eax
        or edx, 0x00000003
        mov [es: ebx + esi * 4], edx        ;登记页的物理地址
        add eax, 0x1000                     ;下一个相邻页的物理地址
        inc esi
        cmp esi, 256                        ;仅低端1MB内存对应的页才是有效的
        jl .b2
    .b3:                                    ;其余的页表项设置为无效
        mov dword [es: ebx + esi * 4], 0x00000000
        inc esi
        cmp esi, 1024
        jl .b3

        ;令CR3寄存器指向页目录，并正式开启页功能
        mov eax, 0x00020000                 ;PCD=PWT=0
        mov cr3, eax
        mov eax, cr0
        or eax, 0x80000000
        mov cr0, eax                        ;开启分页机制

        ;在页目录内创建与线性地址0x80000000对应的目录项
        mov ebx, 0xfffff000                 ;页目录自己的线性地址
        mov esi, 0x80000000                 ;映射的起始地址
        shr esi, 22                         ;线性地址的高10为使目录索引
        shl esi, 2
        mov dword [es:ebx + esi], 0x00021003    ;写入目录项（页表的物理地址和属性），目标单元的线性地址为0xFFFFF200
        ;将GDT中的段描述符映射到线性地址 0x80000000
        sgdt [pgdt]

        mov ebx, [pgdt + 2]

        or dword [es: ebx + 0x10 + 4], 0x80000000
        or dword [es: ebx + 0x18 + 4], 0x80000000
        or dword [es: ebx + 0x20 + 4], 0x80000000
        or dword [es: ebx + 0x28 + 4], 0x80000000
        or dword [es: ebx + 0x30 + 4], 0x80000000
        or dword [es: ebx + 0x38 + 4], 0x80000000

        add dword [pgdt + 2], 0x80000000    ;GDTR也用的是线性地址
        
        lgdt [pgdt]

        jmp core_code_seg_sel:flush         ;刷新段寄存器CS，启用高端线性地址

flush:
        mov eax, core_stack_seg_sel
        mov ss, eax

        mov eax, core_data_seg_sel
        mov ds, eax

        mov ebx, message_1
        call sys_routine_seg_sel:put_string

        ;以下开始安装为整个系统服务的调用门，特权级之间的控制转移必须使用门
        mov edi, salt
        mov ecx, salt_items
    .b4:
        push ecx
        mov eax, [edi + 256]            ;该条目入口点的32位偏移地址
        mov bx, [edi + 260]             ;该条目入口点的段选择子
        mov cx, 1_11_0_1100_000_00000B  ;特权级3的调用门

        call sys_routine_seg_sel:make_gate_descriptor
        call sys_routine_seg_sel:set_up_gdt_descriptor
        mov [edi + 260], cx
        add edi, salt_item_len          ;指向下一个C-SAlT项目
        pop ecx
        loop .b4

        ;对门进行测试
        mov ebx, message_2
        call far [salt_1 + 256]         ;通过门显示信息

        ;位程序管理器的TSS分配内存空间
        mov ebx, [core_next_laddr]
        call sys_routine_seg_sel:alloc_inst_a_page
        add dword [core_next_laddr], 4096


core_code_end:

section trail
core_end:
